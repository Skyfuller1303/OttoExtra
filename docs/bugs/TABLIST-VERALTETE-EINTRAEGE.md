# Spieler verschwinden erst nach eigenem Reconnect

Nutzermeldung vom 6. September 2026, zusätzlich zum ursprünglichen Audit und zum Wappen-Fix.

Status: **Offen; Diagnose ergänzt, kein nachgewiesener Fix für den dauerhaften Eintrag.**

## Beobachtung und geprüfte Pfade

Manche Spieler bleiben in der Tabliste vorhanden. Der Nutzer bestätigt: Nach dem eigenen Reconnect ist der betreffende Eintrag weg. Ein konkreter Account mit zeitlich zugehörigen An-/Abmeldepaketen liegt bislang nicht vor; vollständiger Logout gegenüber Wechsel auf einen anderen Proxy-Server ist noch nicht geklärt.

- OttoExtras `PlayerListSortingService` hält Sortiermetadaten, fügt aber keine Spieler in die Minecraft-Spielerliste ein. Der Comparator wird für die jeweils vom Client gelieferte Liste angewendet.
- Minecraft 1.21.11 verwendet eine UUID-Map und zusätzlich ein Set sichtbarer `PlayerListEntry`-Objekte. `onPlayerRemove()` entfernt den Eintrag aus der Map und anschließend dasselbe Objekt aus dem sichtbaren Set. `UPDATE_LISTED=false` entfernt die Sichtbarkeit ohne Entfernung aus der Map. Wiederholtes `ADD_PLAYER` verwendet `putIfAbsent()`; eine bloße Wiederholung erzeugt daher in Vanilla kein zweites Objekt in diesen Listen.
- BetterTab `2.1.5+1.21.11` verwendet dieselbe Liste sichtbarer Spieler. Die lokal installierte Konfiguration des zuletzt aktiven Profils hat `refreshCooldown=250`, `useExamples=false`. Die Darstellung wird regelmäßig vollständig aus den aktuellen Einträgen aufgebaut. Eine normale Cacheverzögerung von 250 ms erklärt einen dauerhaften Eintrag nicht.
- Im aktuellen Profil-Log wurden während der Untersuchung 43 Warnungen zu `UPDATE_DISPLAY_NAME` für bereits unbekannte UUIDs gefunden. Das belegt verspätete oder nicht zuordenbare Updates, aber weder eine fehlende Abmeldung des konkret betroffenen Spielers noch einen verwaisten sichtbaren Eintrag.

Quellen: lokal generierte Minecraft-Quellen zu `ClientPlayNetworkHandler` und `PlayerListHud`; Bytecode und lokal dekompilierte Klassen der vorhandenen BetterTab-JAR (`Tools`, `PlayerListHudMixin`, `TabUpdater`, `TabColumn`); vorhandenes Client-Log. Keine Serveranfragen und keine Änderungen am laufenden Minecraft-Profil.

## Ergänzte Diagnose

[ClientPlayNetworkHandlerMixin.java](../../src/client/java/de/ottoextra/mixin/ClientPlayNetworkHandlerMixin.java) protokolliert die relevanten Pakete nach dem Wechsel auf den Clientthread und am Ende der normalen Verarbeitung. [PlayerListDiagnostics.java](../../src/client/java/de/ottoextra/playerlist/PlayerListDiagnostics.java) verwendet ausschließlich die vorhandene, ausdrücklich aktivierbare Debug-Ausgabe.

Erfasst werden Verbindung, Phase `before`/`after`, Paketaktionen, UUID/Account, tatsächliche Mitgliedschaft in der UUID-Map (`tracked`), sichtbare Einträge mit dieser UUID (`listedMatches`) und sichtbare Objekte, die nicht dem aktuellen Map-Eintrag entsprechen (`orphanedMatches`). Reine Ping-, Spielmodus-, Hat- und Sortierwert-Updates werden nicht protokolliert. Anzeigenamen werden nur als `null` oder `present` erfasst.

Die Diagnose verändert keine Pakete, Spielerlisten, Caches oder Einstellungen. Es werden keine Spieler anhand von fehlender Welt-Entity, Entfernung, API-Online-Status oder Wartezeiten entfernt. Diese Merkmale wären hinter einem Proxy keine zuverlässige Abmeldung.

## Nächster Nachweis im Spiel

1. Die [Diagnose-JAR](../../build/tablist-diagnostics/ottoextra-1.16.3-k.jar) verwenden. Sie enthält auch den vorigen Wappen-Fix. Installation wurde nicht automatisch ausgeführt.
2. Vor dem nächsten betroffenen Logout `/ottoextra debug on` aktivieren.
3. Beim Auftreten Minecraft-Account und Uhrzeit merken; möglichst vor dem eigenen Reconnect den Zustand erhalten. Relevante Zeilen in `logs/debug.log` beginnen mit `[playerlist-packet]`. Danach lässt sich Debug mit `/ottoextra debug off` wieder deaktivieren.

Auswertung:

- `REMOVE before` und anschließend `REMOVE after` mit `tracked=false`, `listedMatches=0`: Minecraft hat entfernt; ein weiterhin sichtbarer Eintrag liegt anschließend in der Darstellung oder wurde durch ein weiteres Paket neu hinzugefügt.
- Nach `REMOVE after` verbleibende `listedMatches`/`orphanedMatches`: belegter inkonsistenter Clientzustand, dessen Entfernung gezielt abgesichert werden kann.
- Bei durchgehend aktiver Diagnose kein zugehöriges `REMOVE` oder `UPDATE_LISTED=false`: Der Client erhält kein Ausblendesignal. Proxy-/Serverpfad und die tatsächliche Verbindung des Spielers müssen geprüft werden.
- Ein neues `ADD_PLAYER`/`UPDATE_LISTED=true` nach erfolgreicher Entfernung macht ein erneutes serverseitiges Einblenden nachvollziehbar.

## Validierung

- Minecraft `1.21.11`, Yarn `1.21.11+build.6`, Java 21 und Loom `1.17.8`; Abhängigkeiten und Versionsnummer unverändert.
- Methoden, Pakettypen und der vorhandene `NetworkThreadUtils.forceMainThread`-Descriptor in lokalen Quellen geprüft. Vier zusätzliche, nicht abbrechende Hooks im bestehenden Client-Mixin; keine neue optionale Modabhängigkeit.
- `.\gradlew.bat test build -x dist --console=plain`: **BUILD SUCCESSFUL**, 22 Sekunden, **428 bestehende Tests in 95 Suites, 0 Fehler, 0 übersprungen**. Keine neuen Tests, die lediglich Logtext nachbilden.
- Remappte JAR: `environment=client`, Diagnoseklasse enthalten, kein Always-Debug-Marker. Die Diagnose ist standardmäßig deaktiviert. Der Mixin wurde kompiliert und remappt, aber noch nicht in einem gestarteten Client ausgeführt.
- Artefakt: [ottoextra-1.16.3-k.jar](../../build/tablist-diagnostics/ottoextra-1.16.3-k.jar), 30.818.245 Bytes.
- SHA-256: `03f76224ad9a916d0cace1cdae57353bb13dcb3016d476293cb2bdbf04c41c5d`.

Die separate Kopie bewahrt das Prüfartefakt vor weiteren Builds. Der vorhandene löschende `dist`-Finalizer wurde ausgeschlossen. Bisherige Worktree-Änderungen bleiben erhalten.
