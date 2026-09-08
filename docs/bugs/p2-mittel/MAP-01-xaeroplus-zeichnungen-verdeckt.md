# MAP-01: Gemalte Karte verdeckt XaeroPlus-Schrift und Zeichnungen

- Priorität: **P2 – mittel**, auf Nutzerwunsch vorgezogen.
- Status: **Fix implementiert; visuelle Ingame-Abnahme offen**.

Nachtest: Der Nutzer meldet auch mit installierter Fix-JAR unsichtbare Linien. Inzwischen **bestätigt der Live-Tooltip serverseitig erzwungenes FairPlay**: XaeroPlus überspringt dadurch sämtliche Zeichnungen unabhängig vom OttoExtra-Overlay. Der Renderreihenfolge-Patch ist deshalb **kein vollständiger Fix dieses Vorfalls**. JAR-Untersuchung, GPU-Gegenprobe und bestätigte Ursache: [XAEROPLUS-ANALYSE.md](../XAEROPLUS-ANALYSE.md).

Folgepatch implementiert und am 6. September um 14:58 Uhr mit Backup installiert: lokale FairPlay-Ausnahme ausschließlich für eigene gespeicherte Linien, Zeichenvorschau und Text auf der Weltkarte. Der Server-FairPlay-Zustand und andere Sperren bleiben unverändert. Gesamtbuild: 388 Tests erfolgreich; neue JAR beginnt mit SHA-256 `32180e48`. Umfang, vollständiger Hash und offene Ingame-Abnahme: [Lokale Ausnahme](../XAEROPLUS-ANALYSE.md#lokale-ausnahme-für-persönliche-zeichnungen).

- Auslöser: OttoExtras gemalte Weltkarte und XaeroPlus-Zeichnungen gleichzeitig aktiv, insbesondere bei flächendeckender Karte im herausgezoomten Zustand.
- Folge: Linien/Text sind verdeckt. Zusätzlich konnten bereits konsumierte Zeichen-Klicks OttoExtras Gebietsauswahl oder Kamerazentrierung auslösen.

## Ursache und Nachweis

Die installierten JARs wurden mit `javap -c -v -l` untersucht, ohne Spielstart:

- `run/mods/XaeroPlus-2.31.5+fabric-1.21.11-WM1.40.16-MM25.3.12.jar`
- World Map `1.40.16`, Minimap `25.3.12`, Minecraft `1.21.11`.

Die ursprüngliche Reihenfolge war: Terrain → XaeroPlus-Features im `primaryScaleFBO` → FBO auf Hauptbild → OttoExtra-Kartenfläche → Xaero-Kartenelemente. OttoExtras `GuiMapMixin` hängt an `MapElementRenderHandler.render` und kam somit zu spät. Der Shader kann die zuvor gezeichnete XaeroPlus-Schrift vollständig überdecken.

Bytecodebelege (Quellenzeilen aus `LineNumberTable`, keine lokalen Java-Dateien):

- XaeroPlus `MixinGuiMap.java:446`: `drawWorldMapFeatures` nach `XaeroBufferProvider.endBatch`, noch vor dem Wiederherstellen des Hauptframebuffers.
- World Map `GuiMap.java:1241`, `:1242`, `:1265`, `:1276`: Batch-Ende, FBO-Restore, FBO-Blit, Element-Render.
- XaeroPlus `Drawing.java:69`, `:78`, `:87`, `:95`: Linien, Vorschauen, Highlights und Text sind DrawManager-Features.
- XaeroPlus `MixinGuiMap.java:914`, `:918`, `:952`: Zeichen-Klicks/-Releases liefern `true`. Fabric reicht dieses Ergebnis an die AFTER-Mauscallbacks weiter; OttoExtra ignorierte es bisher.
- Gewöhnliche Kartenklicks wurden in World Map `1.40.16` und `1.41.0` geprüft: ohne konsumierendes Widget/Dropdown/Keybinding kein unbedingtes `true`. Deshalb bleibt normale OttoExtra-Auswahl mit dem Klickschutz möglich.

## Umsetzung

- [Optionaler DrawManager-Mixin](../../../src/client/java/de/ottoextra/mixin/compat/xaeroplus/XaeroPlusDrawManagerMixin.java): malt bei `HEAD` von `drawWorldMapFeatures` vor XaeroPlus. `@Pseudo`, `remap = false`, `require = 0`; keine neue Pflichtabhängigkeit.
- [Render-Hook](../../../src/client/java/de/ottoextra/map/PaintedWorldMapHook.java) und [Frame-Guard](../../../src/client/java/de/ottoextra/map/WorldMapRenderPass.java): späteren Paint im selben Frame unterdrücken, auch bei frühem Renderfehler. Frame-Reset über Fabric `ScreenEvents.beforeRender`. Ohne XaeroPlus bleibt der bisherige Renderpfad.
- [Matrix-Umrechnung](../../../src/client/java/de/ottoextra/map/XaeroFramebufferView.java): übernimmt Xaeros Originalmatrix und Kameraanker einschließlich Pixelrundung. Xaeros Matrix liegt bereits in GUI-Einheiten vor; ihre Y-Achse ist gespiegelt.
- [Renderer](../../../src/client/java/de/ottoextra/map/PaintedMapRenderer.java): gespiegelte UVs im Zwischenpuffer, auch im Simple-Modus; Zoom-Fades bleiben an den tatsächlichen Bildschirmzoom gekoppelt. Keine Änderung an Minimappfaden oder Kartentexturen.
- [Klick-Guard](../../../src/client/java/de/ottoextra/map/MapClickGesture.java): nur unverbrauchte linke Press-/Release-Paare mit höchstens vier Pixel Bewegung lösen OttoExtras Kartenauswahl aus. Verbrauchten Zustand anschließend verwerfen.

Der erste Patch wurde gegen die Workspace-Versionen untersucht. Der spätere Profilabgleich bestätigte XaeroPlus `2.34.5`, World Map `1.44.2`, Minimap `26.4.2` und XaeroLib `1.7.1`; die Hook-Signatur und Matrixkonventionen passen auch dort. Die OttoExtra-JAR wurde anschließend auf ausdrücklichen Nutzerwunsch mit Backup ersetzt. Xaero-Mods blieben unverändert.

## Automatisierte Prüfung

Die folgenden Zahlen und Artefaktdaten beschreiben den **ersten Renderreihenfolge-Fix**. Der ergänzte FairPlay-Zeichenpfad mit zehn weiteren Tests und neuem Installationshash ist im verlinkten Folgepatch dokumentiert.

21 neue Regressionstests prüfen Matrix-/Zoomumrechnung, negative und fraktionale Koordinaten, GUI-Größen, Y-Spiegelung, ungültige Matrizen, Frame-Neustart/Fallback und konsumierte Klickfolgen.

- `.\gradlew.bat test --tests de.ottoextra.map.XaeroFramebufferViewTest --tests de.ottoextra.map.WorldMapRenderPassTest --console=plain`: erfolgreich, 11 gezielte Tests.
- `.\gradlew.bat test build -x dist --console=plain`: **BUILD SUCCESSFUL**, 22 Sekunden; **378 Tests, 81 Suites, 0 Fehler, 0 übersprungen**. `dist` bewusst ausgeschlossen, damit dessen vorhandene JARs nicht automatisch gelöscht werden.
- Unabhängiges, lesendes Code-Review: kein belegter Fehler im Patch. Kein Spielstart durch das Review.
- Fertige JAR geprüft: client-only, Mixin registriert, neue Klassen enthalten. Remappter Callback verwendet `net/minecraft/class_4587`, passend zum installierten XaeroPlus-Descriptor; der optionale BufferProvider-Parameter ist `@Coerce Object`.
- Artefakt: `build/libs/ottoextra-1.16.3-k.jar`.
- SHA-256: `08cf0631fcccfe3cf92fff7fec56241528b949a3b2cb4117c9813e7c4ded0bb1`.

Bestehende Deprecation-Hinweise für MapModule/Fabric und Gradle bleiben unverändert; keine neuen Buildfehler. Der spätere Austausch der OttoExtra-JAR erfolgte bei beendetem Minecraft mit Backup und SHA-256-Prüfung.

## Noch im Spiel prüfen

1. Zusammenpassende XaeroPlus-/World-Map-/Minimap-Versionen starten; Log auf neue Mixin-/PaintedMap-Fehler prüfen.
2. Gemalte Karte einschalten. Linie und Text platzieren; Schrift muss sichtbar bleiben. Texteingabefeld, Bestätigung und Abbruch prüfen. Highlights nur ohne FairPlay prüfen; sie sind nicht Teil der ausdrücklich vereinbarten Ausnahme.
3. Zoomen und verschieben, auch bei negativen Koordinaten; Markierung muss am selben Weltpunkt bleiben. GUI-Skalierung und Fenstergröße ändern.
4. Normalen Shaderpfad und `paintedMapSimple` testen. Weit herauszoomen bis Vollabdeckung; Karte darf weder gespiegelt noch versetzt sein.
5. Zeichnungsklick über einem OttoExtra-Auswahlpanel: keine fremde Auswahl/Kamerazentrierung. Zeichenmodus beenden: normale Gebietsauswahl und Drag-Panning bleiben funktionsfähig.
6. Karte schließen/öffnen, Overlay aus/ein, Ressourcen neu laden. Ohne XaeroPlus und ohne Xaero-Mods jeweils Start/Fallback prüfen.

Die Unit-Tests ersetzen weder GPU-/Mixin-Anwendung im laufenden Spiel noch die visuelle Prüfung dieser Kombination.
