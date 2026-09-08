# P3-Umsetzung vom 6. September 2026

Beide P3-Befunde sind im Code behoben. Die vorhandenen P1-/P2-Fixes und sonstigen Worktree-Änderungen bleiben erhalten. Damit haben alle 23 erfassten Auditbefunde eine Codekorrektur; die dokumentierten Ingame-Abnahmen bleiben davon getrennt.

## LETTER-03: Führende Leerzeilen

Der V2-Planer verwendet die Anzahl der bereits übernommenen Zeilen statt des Inhalts des StringBuilders. Das gilt für Trennumbrüche, Kandidatenbildung, Formatpräfixe und Seitenwechsel. Führende Leerzeilen und vollständige Leerzeilenseiten bleiben dadurch in Vorschau und Versand erhalten. Auch der Fließtextpfad erhält Randumbrüche; interne weiche Einzelumbrüche werden weiterhin zusammengezogen.

Drei neue Tests in [LetterV2PlannerTest.java](../../src/test/java/de/ottoextra/letter/send/LetterV2PlannerTest.java) vergleichen Vorschau, Befehle und das lokale Courier-Protokollmodell: null/eine/zwei führende Leerzeilen, eine volle Leerzeilenseite mit Folgeinhalt und Formatierung nach einem Seitenwechsel. Zwei der Tests schlugen vor der Korrektur am ursprünglichen Verhalten fehl. Der vorhandene [V2DraftFlowTest.java](../../src/test/java/de/ottoextra/letter/V2DraftFlowTest.java) bestätigt nach angepasster Erwartung, dass das Löschen einer anderen Seite die explizite Leerzeile der verbleibenden Seite erhält.

## REGIONS-02: Offenes Regionsfenster aktualisieren

`requestRegionDetailAsync()` bietet eine Completion nach dem Cacheupdate und teilt sie mit bereits laufenden Prefetches. Die bisherige öffentliche Prefetch-Methode bleibt erhalten. Nach erfolgreichem Laden übernimmt das passende Fenster aktuelle Region, Fraktion und Vasallen auf dem Clientthread. Tab-Beschriftungen und Zeilenbuttons werden aktualisiert; ein bereits ausgewählter Spieler-Tab lädt nach, sobald die Fraktion vorliegt.

Screenwechsel und erneutes Öffnen invalidieren alte View-Callbacks. Spieleranfragen prüfen zusätzlich ihre Anfragefolge und Fraktion. Detailantworten aus alten Verbindungen können keine aktuellen Indizes oder neuen Requests überschreiben. Erfolgreiche Requests behalten ihren Cooldown; fehlgeschlagene Requests erlauben einen neuen Versuch. Unveränderte Cachedaten setzen die Scrollposition nicht zurück.

Fünf neue Tests in [RegionDetailRefreshTest.java](../../src/test/java/de/ottoextra/regions/RegionDetailRefreshTest.java) prüfen den Service und die vom Fenster verwendeten Snapshot-/Callback-Helfer mit steuerbaren Futures und einem kontrollierten Client-Executor. Erfasst sind neue Notizen/Fähigkeiten/Fraktion/Vasallen, laufende Prefetches, geschlossene oder wiedergeöffnete Views, andere ausgewählte Regionen, Fehler/Retry und alte Verbindungen. Das Minecraft-Fenster selbst wird dabei nicht gerendert.

## Validierung

- Minecraft `1.21.11`, Yarn `1.21.11+build.6`, Java 21, Loom `1.17.8`; keine Änderungen an Abhängigkeiten, Mixins oder Version.
- Screen-Lifecycle (`removed()` und `setScreen()`) in den lokal generierten Minecraft-Quellen geprüft. UI-Felder werden nur auf dem Clientthread aktualisiert.
- `.\gradlew.bat test --tests 'de.ottoextra.letter.*' --tests 'de.ottoextra.regions.*' --console=plain`: **142 Tests erfolgreich**.
- `.\gradlew.bat test build -x dist --console=plain`: **BUILD SUCCESSFUL**, 19 Sekunden. **422 Tests, 94 Suites, 0 Fehler, 0 übersprungen** laut frisch erzeugten JUnit-XML-Dateien.
- Acht neue P3-Regressionstests. Eigene Änderungen gegen den vor Arbeitsbeginn gesicherten Worktree geprüft; `git diff --check` für die versionierten geänderten Klassen erfolgreich.
- Remappte JAR kontrolliert: `environment=client`, Version `1.16.3-k`, neue Callback-Klassen enthalten. Eine separate Kopie bewahrt dieses Prüfartefakt vor nachfolgenden Builds. `dist` wegen seines vorhandenen Lösch-Finalizers ausgeschlossen. Keine Installation in ein Minecraft-Profil.
- [JAR mit P1-/P2-/P3-Fixes](../../build/p3-fixes/ottoextra-1.16.3-k.jar), 30.814.939 Bytes.
- SHA-256: `510467a791d4531190eaf2455ea194b58bf4bea8088951d24575ca7f01f65a9a`.

## Noch im Spiel prüfen

1. Einen V2-Brief mit einer/zwei führenden Leerzeilen und einem Seitenwechsel öffnen, bearbeiten und im Testprofil schreiben. Abstand, Cursorposition und Buchinhalt vergleichen.
2. Regionsfenster mit unvollständigem Cache öffnen und Detailantwort abwarten. Notizen, Fähigkeiten, Fraktion, Spielerzahl und Vasallen müssen ohne Wiederöffnen wechseln. Währenddessen Spieler-Tab auswählen; anschließend Fenster schließen, andere Region öffnen und zum vorherigen Fenster zurückkehren.

Kein Minecraft-Start und kein Live-API-Aufruf für diese Arbeit. Die bereits dokumentierten P1-/P2-/XaeroPlus-Ingame-Prüfungen bleiben ebenfalls offen.
