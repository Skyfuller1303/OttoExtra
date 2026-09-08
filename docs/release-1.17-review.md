# Nachprüfung für OttoExtra 1.17

Stand: 7. September 2026. Geprüfter Arbeitsstand: `1.16.3-w` einschließlich
RP-Ersteinrichtung und der hier dokumentierten Korrekturen. Die Version wurde
noch nicht auf 1.17 angehoben und nichts veröffentlicht.

## Behobene Befunde

### P2: XaeroPlus-Minimap-Hook trifft im Dev-Client die falsche Schnittstelle

Mit XaeroPlus 2.34.5 brach der Entwicklungsclient vor dem Hauptmenü mit einer
`InvalidInjectionException` ab. Der String-Selektor für die alte Variante ohne
Zoom fand keinen exakten Treffer. Mixin wich beim Dev-Remapping auf die Suche
nach dem Methodennamen aus und versuchte, den alten Handler in die neue Methode
mit zusätzlichem `double` einzusetzen. `require = 0` verhindert diesen
Signaturfehler nicht.

Die beiden Hooks verwenden jetzt `@Desc` mit exakten Argumenttypen. Diese
Selektoren verlieren beim permissiven Abgleich ihre Signatur nicht. Die
Minecraft-Typen werden weiterhin remappt; der optionale Provider-Typ kommt nur
in Annotation-Metadaten vor. Die eigentlichen Handler verwenden weiter
`@Coerce Object`.

Quelle: `src/client/java/de/ottoextra/mixin/compat/xaeroplus/XaeroPlusDrawManagerMixin.java`.
Der Fehler wurde vor der Änderung mit den tatsächlichen Mod-JARs reproduziert.
Die frühere Aussage, getrennte String-Deskriptoren allein seien ausreichend,
war für den getesteten Dev-Lauf unzutreffend. Ein gleichartiger Absturz im
Produktionsprofil wurde durch diesen Test nicht nachgewiesen.

### P2: Erststart-Buttons außerhalb des Bildschirms

Bei 320 × 240 GUI-Pixeln war das bisher fest 286 Pixel hohe Hinweisfenster
größer als der verfügbare Platz. Bereits der erste Weiter-Button ragte unten
heraus; längere Texte überlappten die Bedienung. Die vorigen RP-Tests hatten
nur die neuen RP-Seiten bei kleiner Auflösung geprüft.

`WelcomeScreen` begrenzt das Panel nun auf die Fensterhöhe und verwendet für
den Inhalt ein natives `ScrollableTextWidget`. Die Buttons stehen außerhalb
des Textbereichs, sämtliche Hinweise lassen sich scrollen. Beim Größenwechsel
bleibt die Leseposition erhalten; eine neue Seite beginnt oben.

### Paket und Dokumentation

- MIT-Lizenzdatei in die gebaute Mod-JAR aufgenommen; zuvor nur im Repository.
- Veraltete Versionsangabe in README und feste alte Upload-JAR in der
  Modrinth-Checkliste berichtigt.
- Changelog ergänzt und Release-Notizen für 1.17 als Entwurf angelegt.

## Noch offen

1. **Veraltete Tablisten-Einträge nach Logout.** Die separate Nutzermeldung hat
   weiterhin keinen belegten Fix. Die Aktualisierung von Sortiermetadaten beim
   Öffnen entfernt keine Spieler aus der vom Server gelieferten Liste und löst
   diesen Befund deshalb nicht nachweislich. Für einen konkreten betroffenen
   Account werden REMOVE-/UPDATE_LISTED-Pakete und der sichtbare Clientzustand
   benötigt. Siehe [Diagnose](bugs/TABLIST-VERALTETE-EINTRAEGE.md).
2. **XaeroPlus-Minimap-Zeichnungen visuell abnehmen.** Der Start und die Auswahl
   der richtigen Hook-Signatur sind geprüft. Das ersetzt keinen Sichttest mit
   gespeicherten Linien/Texten, Zoom, Rotation und der tatsächlichen
   Server-FairPlay-Einstellung. Für den älteren Weltkarten-Fix ist eine
   Nutzerbestätigung dokumentiert; für die Minimap-Erweiterung noch nicht.
3. **Live-Abnahmen aus dem vorigen Audit.** Insbesondere Wappen nach echtem
   Proxy-Rejoin, Briefversand mit Disconnect/Recovery und der tatsächliche
   Ressourcenpaket-Austausch unter Windows sind durch diese Nachprüfung nicht
   zusätzlich abgenommen. Die bestehenden Regressionstests bleiben vorhanden.
   Siehe [Proxy-Rejoin](bugs/TABLIST-PROXY-REJOIN.md),
   [P1-Abnahme](bugs/P1-UMSETZUNG.md) und [P2-Abnahme](bugs/P2-UMSETZUNG.md).
4. **Release-Version und Upload-Artefakt.** Nach Bewertung der offenen Punkte
   `mod_version=1.17` setzen, bauen und genau diese JAR samt Metadaten prüfen.

## Validierung

Wiederholbare Client-Tests und Logs liegen im Workspace unter
`test-results/release-117-review-20260907`. Der Assistent wird vollständig
per tatsächlichen Button-Klicks geprüft: alle fünf Hinweis-Seiten bei kleiner
GUI, Scrollen bis zum Textende, drei RP-Vorschauen, Zurück, Übernehmen/Speichern,
Überspringen, Escape, Rückkehr zum Hauptmenü und kein erneutes Öffnen.

Die Tests berücksichtigen, dass Xaero nach der Rückkehr zum Hauptmenü seinen
eigenen `GuiUpdateAll` anzeigen darf. Die Rückkehr selbst wird über
`ScreenEvents.AFTER_INIT` nachgewiesen. Ein erster Legacy-Testlauf hatte diese
zulässige Folgemeldung fälschlich als Fehler gewertet; dafür war keine weitere
Produktkorrektur nötig.

Die vollständigen Client-Prüfungen bestehen in diesen drei Konfigurationen:

| Konfiguration | Ergebnis |
| --- | --- |
| Ohne Xaero / XaeroPlus | Bestanden |
| XaeroPlus 2.31.5, World Map 1.40.16, Minimap 25.3.12, XaeroLib 1.1.15 | Bestanden |
| XaeroPlus 2.34.5, World Map 1.44.2, Minimap 26.4.2, XaeroLib 1.7.1 | Bestanden |

Exportierter transformierter Bytecode bestätigt: Die alte Variante ruft den
Handler ohne Zoomargument auf; die neue den Handler mit Zoomargument. Das ist
zusätzlich zum Starttest geprüft, damit ein still ausgebliebener optionaler
Hook nicht als Erfolg zählt. Logs: `fixed-no-xaero.log`,
`fixed-legacy-xaeroplus.log`, `fixed-xaeroplus.log`; Bytecode-Nachweise:
`legacy-transformed.javap.txt`, `modern-transformed.javap.txt` im Testordner.

Deutsche und englische Sprachdatei: jeweils 1122 Schlüssel, keine doppelten
Schlüssel und keine Unterschiede im Schlüsselbestand.

Abschließender Build: `gradlew.bat build -x dist --rerun-tasks --console=plain`
erfolgreich; alle 11 Tasks frisch ausgeführt, **565 Tests, keine Fehler und keine
übersprungenen Tests**. Die remappte JAR ist clientseitig, enthält die Lizenz,
keinen Always-Debug-Marker, keine Testklassen und keine gebündelten optionalen
Xaero-/DevAuth-Klassen. Die `@Desc`-Argumenttypen enthalten im Release korrekt
`net.minecraft.class_4587` statt des Yarn-Namens für `MatrixStack`.

Eine unabhängige Artefaktkopie samt maschinenlesbarem Nachweis liegt unter
`test-results/release-117-review-20260907/artifacts/ottoextra-1.16.3-w.jar`.
SHA-256: `e677eee2cf391ba8faeb9ac3b9fba41b5e61914f1b1e47383555d6dbf73a0d83`.
Die vorhandenen Gradle-Deprecation-Warnungen und die bekannte, absichtlich
deskriptorlose Xaero-Render-Slice-Warnung sind weiterhin vorhanden; der Build
und die getesteten Client-Konfigurationen bestehen damit.

Diese Prüfung ersetzt keine vollständige Codeprüfung aller Änderungen im
umfangreichen Arbeitsstand und keinen Live-Multiplayer-Test. Wegen der offenen
Tablisten-Nutzermeldung ist eine uneingeschränkte Freigabe als fehlerbereinigtes
Stable-Release noch nicht belegt.
