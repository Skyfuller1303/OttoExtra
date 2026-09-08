# XaeroPlus: Unsichtbare Zeichnungen – JAR-Untersuchung

Stand: 6. September 2026. Diagnose mit anschließend vom Nutzer beauftragter **lokaler Ausnahme nur für eigene Linien und Texte auf der Weltkarte**. Server und Xaero-JARs werden nicht verändert; Ingame-Abnahme der Ausnahme steht aus.

**Nachtrag:** Der Nutzer bestätigt inzwischen die Funktion auf der Weltkarte und
beauftragt dieselbe Ausnahme für die Minimap. Umsetzung und neuer Build:
[Minimap-Fix](XAEROPLUS-MINIMAP-FIX.md). Die folgenden Installationsdaten und
Minimap-Ausschlüsse dokumentieren den vorherigen Weltkarten-Build.

## Ergebnis

**Ursache bestätigt: serverseitig erzwungener Fair-Play-Modus der Minimap.** Der Nutzer hat den eindeutigen Tooltip „Disabled by fair-play mode enforced by the server!“ in seiner laufenden Sitzung bestätigt. XaeroPlus blockiert bei diesem Flag seine gesamte Feature-Ausgabe, lässt Zeichen-UI und Speicherung aber aktiv. Der Schreibpfad funktioniert nachweislich; die ursprüngliche Erklärung allein über OttoExtras Renderreihenfolge war für diesen Vorfall unvollständig.

```text
Mausziehen → Zeichnung speichern → DrawManager
                                  ├─ FairPlay = true  → sofort zurück, keine Zeichnungen
                                  └─ FairPlay = false → Features → Linien-Shader → Karte
```

Der entscheidende Gate steht auch im [offiziellen DrawManager-Quelltext](https://github.com/rfresh2/XaeroPlus/blob/1.21.11/common/src/main/java/xaeroplus/feature/render/DrawManager.java): `if (HudMod.INSTANCE.isFairPlay()) return;`. Beide Renderwege – Weltkarte und Minimap – sind betroffen. **Die Aktivierung wurde über den daran gebundenen Live-Tooltip bestätigt, nicht per direktem Zugriff auf den JVM-Speicher.**

## Untersuchte Installation

- Profil: `Ottonien 1.0.0` in Modrinth.
- Minecraft 1.21.11, XaeroPlus 2.34.5, World Map 1.44.2, Minimap 26.4.2, XaeroLib 1.7.1.
- XaeroPlus-JAR SHA-256: `88d3561b3fc570b78ba1862f4b6dbe719dd0d3259dbc351312237236875bf2d2`.
- OttoExtra-JAR des ersten Renderreihenfolge-Fixes, noch ohne FairPlay-Ausnahme, SHA-256: `08cf0631fcccfe3cf92fff7fec56241528b949a3b2cb4117c9813e7c4ded0bb1`.
- Nach Neustart wurde die neue OttoExtra-JAR bestätigt. Der alte Build ist diesmal nicht die Erklärung.
- Die Kombination der drei Xaero-Modversionen entspricht der [offiziellen XaeroPlus-2.34.5-Freigabe](https://github.com/rfresh2/XaeroPlus/releases/tag/2.34.5).

## Beweiskette im Bytecode

Quellenzeilen stammen aus `javap -c -l -v` / `LineNumberTable` der tatsächlich installierten JARs, nicht aus einer anderen Minecraft-Version.

| Ort in der JAR | Nachgewiesenes Verhalten |
| --- | --- |
| `xaeroplus/feature/render/DrawManager.java:67` | Weltkarten-Render bricht bei `HudMod.INSTANCE.isFairPlay()` vor jeder Feature-Iteration ab. |
| `DrawManager.java:41` | Derselbe Abbruch für die Minimap. |
| `xaeroplus/mixin/client/MixinGuiMap.java:278`, `:683`, `:707`, `:747` | Zeichenmodus, Statustext, Mausklick und Release haben keinen FairPlay-Gate. |
| `xaeroplus/module/impl/Drawing.java:153` | `addLine` prüft Mindestlänge und speichert; kein FairPlay-Gate. |
| `xaero/common/HudMod.java:469` | Bei geladenem Client: aktuelle Minimap-Sitzung → `getForcedFairPlay()`. Kein konstanter Editionsschalter in dieser JAR. |
| `xaero/common/minimap/MinimapProcessor.java:263`, `:267` | Getter/Setter für `fairPlayOnlyMessageReceived`. |
| `xaero/common/events/ClientEvents.java:179` | Ein spezielles Systemchat-Steuersignal setzt das FairPlay-Flag. |

Somit ist **UI sichtbar + Daten gespeichert + keine Vorschau/keine Linien** bei aktivem FairPlay vollständig durch Code erklärt. Fehlender Klartext in `latest.log` beweist nicht, dass das Flag aus ist.

## Weitere ausgeführte Prüfungen

- SQLite ausschließlich lesend, nur Aggregate: Zeichnungs-DB ohne aktuelles WAL enthält sechs Overworld-Linien, mit aktuellem WAL sieben. Mindestens eine zusätzliche Linie wurde seit Neustart gespeichert. Keine Koordinaten oder Zeichnungstexte für den Bericht ausgelesen.
- `LINE_SEGMENT` ist **Linkstaste halten → ziehen → loslassen**, kein Zweiklick-Werkzeug. Linien unter zwei Weltblöcken werden verworfen.
- Zeichenmodul wird beim Client-Start ausdrücklich aktiviert; keine passende Initialisierungsfehlermeldung im Log.
- Gespeicherte Linien besitzen Alpha 80. Vorschau verwendet ebenfalls feste, von der gespeicherten Opazitätseinstellung unabhängige Deckkraft 80/255. `Drawing Opacity` allein erklärt fehlende Vorschau nicht.
- F1/`hiddenUI` könnte den DrawManager ebenfalls überspringen, würde jedoch auch normale Kartenbuttons und Drawing-Status ausblenden.
- Fünf aktive Dateipacks und das geprüfte Serverpack enthalten keine Overrides der relevanten XaeroPlus-/Minecraft-Shader. Tactical-Factions-Add-on enthält keine zusätzlichen Karten-Renderhooks.
- Registry-Iteration, relativer Kameraursprung, Vertexformat und QUADS-Indexvertrag wurden geprüft: kein entsprechender belegter Fehler.

## GPU-Gegenprobe: 88 versus 96 Byte

Ein echter portabler Randfehler wurde gefunden: `AbstractLineVertexBuffer` reserviert 88 Byte für `LinesTransforms`; OpenGL meldet für dessen std140-Block 96 Byte. [Allokation](https://github.com/rfresh2/XaeroPlus/blob/1.21.11/common/src/main/java/xaeroplus/feature/render/line/AbstractLineVertexBuffer.java), [Shader-Layout](https://github.com/rfresh2/XaeroPlus/blob/1.21.11/common/src/main/resources/assets/xaeroplus/shaders/include/lines_include.glsl).

Dieser Befund wurde **nicht vorschnell als Ursache behandelt**, sondern isoliert getestet:

- Eigener unsichtbarer GLFW-Kontext 64×64, OpenGL 3.3; kein Anhängen an Minecraft.
- Nutzer-GPU: RTX 4070 Ti SUPER, NVIDIA 610.47.
- Originale `lines.vsh`/`lines.fsh` aus der installierten JAR; Minecraft-Imports durch gleichnamige Uniformdeklarationen ersetzt, Identitätsmatrizen, repräsentative Testlinie, Indizes als GL_TRIANGLES.
- Gemeldete Blockgröße 96; Offsets 0/64/72/80.
- Sowohl 88 als auch 96 Byte: sichtbares rotes Pixel `255,0,0,255`, `glGetError = 0`, jeweils zweimal.

**Damit wurde die beobachtete Unsichtbarkeit nicht reproduziert.** Reale Minecraft-Matrizen, aktiver Framebuffer und Sitzungsflags sind nicht Teil dieses isolierten Tests. Kleinere Uniform-Bindings verletzen zwar den [OpenGL-3.3-Vertrag, §2.11.4](https://registry.khronos.org/OpenGL/specs/gl/glspec33.core.pdf#page=88), sind aber kein belastbarer Kausalnachweis für diesen Vorfall.

Zusätzlich gefunden: Lesen des Index-Typs vor möglichem Indexbuffer-Wachstum. Das betrifft einen großen ersten Buffer-Übergang; es erklärt nicht dauerhaft unsichtbare erste Linien.

## Artefakte und Grenzen

Relevante Klassen mit CFR 0.152 dekompiliert unter:

`build/reports/xaeroplus-investigation/decompiled/`

Enthalten sind unter anderem `Drawing`, `MixinGuiMap`, `DrawManager`, `DrawFeatureRegistry`, die drei Linienbufferklassen sowie `HudMod`, `MinimapProcessor` und `ClientEvents`. Die Java-Dateien sind lesbare Analyseartefakte mit Intermediary-Namen; kein neu kompilierter Ersatz für die Mods.

Der OttoExtra-Modulschalter wird nur beim Start ausgewertet; Abschalten ohne Neustart ist kein vollständiger Ausschluss des Overlays. Die zuletzt gelesene gespeicherte Config hatte `map.enabled=true` und `paintedMap=true`. Das beweist nicht, welchen Zwischenzustand der Nutzer zuvor getestet hat.

Der Live-Nachweis ist erbracht. Der Nutzer hat die angebotene Ausnahme für eigene Linien und Texte anschließend ausdrücklich beauftragt. Die Umsetzung ändert XaeroPlus' bisherige FairPlay-Behandlung ausschließlich für die unten genannten Weltkarten-Features. Sie ist keine Änderung am Server und keine allgemeine FairPlay-Abschaltung.

## Lokale Ausnahme für persönliche Zeichnungen

Der optionale [DrawManager-Mixin](../../src/client/java/de/ottoextra/mixin/compat/xaeroplus/XaeroPlusDrawManagerMixin.java) bleibt ein nicht abbrechender `HEAD`-Hook. Nach OttoExtras Kartenbasis ruft er den [Zeichnungsadapter](../../src/client/java/de/ottoextra/map/xaero/XaeroPlusPersonalDrawings.java) auf. Nur bei aktivem `HudMod.INSTANCE.isFairPlay()` rendert dieser aus der aktuellen Registry exakt:

- `Drawing-lines-saved`: lokal gespeicherte Linien.
- `Drawing-lines-in-progress`: aktuelle Linie beziehungsweise Messvorschau.
- `Drawing-text`: lokal gespeicherter Text.

Kein Präfixfilter: `Drawing-highlights`, Radar, Chunk-Features und sämtliche unbekannten IDs sind ausgeschlossen. Die Ausnahme arbeitet unabhängig vom OttoExtra-Kartenmodul und vom Schalter für die gemalte Karte. Ohne FairPlay erfolgt kein zusätzlicher Zeichenpass; XaeroPlus rendert regulär. Die Zeichenverwaltung und ihre Datenbank bleiben unverändert.

Das globale FairPlay-Flag, der originale DrawManager-Abbruch, Minimap-Renderpfad, Netzwerkbehandlung und andere FairPlay-Sperren bleiben erhalten. Die Matrixkonventionen werden aus XaeroPlus übernommen und der Matrixstack wird auch bei Renderfehlern wiederhergestellt. Fehlende oder inkompatible optionale APIs lassen die Ausnahme ausfallen, nicht den ursprünglichen Renderpfad.

Die Reflection-Signaturen wurden an den JARs von XaeroPlus 2.34.5 und 2.31.5 geprüft: sieben Argumente mit unübersetzter Matrix und Kameraankern beziehungsweise vier Argumente für den älteren `DrawContext`. Nur Reflection-Metadaten werden gecacht; HUD, Registry und Features werden aus dem aktuellen Aufruf gelesen. Eine beim ersten Aufruf noch fehlende HUD-Instanz blockiert spätere Versuche nicht dauerhaft.

### Prüfung und Installation

- Zehn neue [Regressionstests](../../src/test/java/de/ottoextra/map/xaero/XaeroPlusPersonalDrawingsTest.java): exakte Allowlist, kein Zusatzrendern ohne FairPlay, Reihenfolge/Flag-Erhalt, beide Kontextvarianten, Matrix-Restore auch bei Fehler, aktuelle Registry/HUD, leere Daten und fehlende API.
- Finaler Lauf: `.\gradlew.bat test build -x dist --console=plain` → **BUILD SUCCESSFUL**, 19 Sekunden; **388 Tests, 82 Suites, 0 Fehler, 0 übersprungen**. `dist` bleibt ausgeschlossen, um dessen bestehende JARs zu erhalten.
- Unabhängiges lesendes Review: keine konkreten Blocker. Remappte JAR mit `javap` geprüft: MatrixStack-Klassenreferenz `net/minecraft/class_4587`, Zusatzaufruf im Callback, passende Reflection-Konstruktorparameter. Mixin und Helper sind enthalten; Metadaten bleiben client-only, kein Always-Debug-Marker.
- **Am 6. September 2026 um 14:58 Uhr (Europe/Berlin) installiert**, nach erneutem Prozesscheck bei geschlossenem Minecraft. Ziel: `C:\Users\Mattis\AppData\Roaming\ModrinthApp\profiles\Ottonien 1.0.0\mods\ottoextra-1.16.3-k.jar`.
- Neue JAR SHA-256, Build und Installation identisch: `32180e48bf2462c243c6afdfc6e1fb648f7c812b9a1fdd572573d2e309c4a921`.
- Verifiziertes Backup des vorherigen Render-Fixes: `C:\Users\Mattis\AppData\Roaming\ModrinthApp\profiles\Ottonien 1.0.0\ottoextra-backups\jar-20260906-145807-fairplay-drawings\ottoextra-1.16.3-k.jar`, SHA-256 `08cf0631fcccfe3cf92fff7fec56241528b949a3b2cb4117c9813e7c4ded0bb1`.
- XaeroPlus-JAR nach dem Tausch erneut gehasht: unverändert `88d3561b3fc570b78ba1862f4b6dbe719dd0d3259dbc351312237236875bf2d2`. Keine Xaero-Einstellungen, Zeichnungsdaten oder Serverdateien geändert.

Versionsname bleibt `1.16.3-k`; der Hash unterscheidet die Testbuilds. Die Rückmeldung „Linie weiterhin nicht sichtbar“ während dieser Umsetzung betraf nach überprüftem Installationshash noch den alten Build, nicht diese Ausnahme.

Beim ersten erfolgreichen Durchlauf eines erlaubten Features erscheint einmalig im Log:

```text
[map] XaeroPlus: Render-Ausnahme fuer eigene Weltkarten-Linien/Texte aktiv; Server-Flag und andere Sperren bleiben unveraendert.
```

Dies bestätigt den ausgeführten Renderaufruf, nicht die sichtbare GPU-Ausgabe. Bei inkompatibler API oder Renderfehler wird begrenzt `XaeroPlus-Zeichnungen: optionale FairPlay-Ausnahme nicht verfuegbar` protokolliert.

### Akzeptanz im Spiel

1. Mit weiterhin sichtbarem FairPlay-Tooltip Weltkarte öffnen; gespeicherte Linien und Texte müssen erscheinen. Line Segment durch Halten, Ziehen und Loslassen zeichnen: Vorschau und gespeicherte Linie sichtbar.
2. Gemalte Otto-Karte aus/ein; zusätzlich Otto-Kartenmodul aus und Client neu starten. Eigene Zeichnungen bleiben sichtbar und beim Zoomen/Verschieben am Weltpunkt.
3. Radar bleibt gesperrt; andere durch FairPlay gesperrte Features und Minimap-Zeichnungen werden nicht freigeschaltet. Highlights sind ausdrücklich nicht Teil dieser Ausnahme.
4. Ohne FairPlay keine doppelt gezeichneten Linien/Texte. Ohne XaeroPlus normaler Clientstart und bisheriger Karten-Fallback.

Keine visuelle Ingame-Prüfung und keine vollständige Mixin-Transformation durch den automatisierten Testlauf. Clientstart mit/ohne XaeroPlus und die tatsächliche Sichtbarkeit unter Server-FairPlay bleiben manuell abzunehmen; der Server wird für diese Prüfung nicht verändert oder gestartet.

### Eindeutiger Livecheck ohne Neustart

Xaero-Minimap-Einstellungen → **Entity Radar Settings** → Maus über **Entity Radar** halten, ohne den Schalter zu ändern.

Der Tooltip **„Disabled by fair-play mode enforced by the server!“** bestätigt das aktive FairPlay-Flag. Nur „Radar aus“ wäre kein eindeutiger Nachweis.

Beleg: `xaero/hud/minimap/config/option/value/redirect/MinimapConfigOptionClientRedirectors.java:60` bindet die Weiterleitung an `HudMod.INSTANCE.isFairPlay()`; Zeile 63 liefert `FAIRPLAY_TOOLTIP`. Englischer Text im tatsächlich installierten JAR: `assets/xaerominimap/lang/en_us.json`, `gui.xaero_config_redirect_fairplay`.

**Nutzerrückmeldung: Genau dieser Tooltip wird angezeigt.** Damit ist der zuvor nur plausible Codepfad in dieser Sitzung aktiv. Der reine Anzeigecheck hat weder Mods getauscht noch Servervorgaben verändert.
