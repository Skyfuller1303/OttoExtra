# Consent-Smoke-Test

```powershell
./gradlew.bat -I tools/consent-smoke/client.init.gradle runClientGameTest -x dist --console=plain
```

Eigenes Profil: `build/consent-smoke/client`. Der Lauf überschreibt ausschließlich dessen Test-Config und `options.txt`, setzt veraltete Zustimmungsflags und prüft deren Migration. Kein Server-Join, DevAuth aus. Für die testweise Pack-Freigabe sind Startprüfungen ausgeschaltet.

Prüft echte Dialoge/Mausklicks: erste Übersicht mit „Alles erlauben“, „Alles ablehnen“ und Einzelentscheidungen; direkter Abschluss der Gesamtauswahl; unveränderte RP-Einstellungen; keine Freigabe nachträglich geänderter Quellen; Erststart ablehnen; spätere RP-Upload-Freigabe nach Information und sofortiger Widerruf. Screenshots: `build/consent-smoke/client/screenshots/`. Ergänzende HTTP-/Widerrufs-/Backup-Tests: `./gradlew.bat test -x dist`.

Bei der Gesamtauswahl erfasst ein Test-Elternbildschirm die Entscheidung und widerruft
sie synchron beim Öffnen, bevor der nächste Tick einen echten Update-Check starten kann.
Resourcepack-Startprüfungen sind zusätzlich abgeschaltet. Die Tests wählen keine
echten Online-Zustimmungen für das Produktionsprofil.

Grenzen und offene Freigaben: [P0-Umsetzung](../../docs/p0-umsetzung.md). Keine Aussage über vollständige Netzwerkstille von Minecraft/Fremdmods oder serverseitige Berechtigungen.

## P1-Erweiterung

Prüft zusätzlich den separaten Chatlog-Dialog (Ottonien-Scope, erlauben/widerrufen),
Vote-Bestätigung (ENTER/Abbrechen ohne Effekt; bestätigte Aktion genau einmal)
und rendert den maximalen Low-Health-HUD-Rand als Screenshot. Vote-Effekte sind
im Fixture ausschließlich ein Zähler: kein echter Browser und keine Clipboard-Änderung.
Die neuen Screenshots heißen `0003_p1-chat-consent-de.png`,
`0004_p1-vote-consent-de.png`, `0005_p1-lowhealth-max-hud.png`.
Kein vollständiger Low-Health-Welt-/Sound-/FOV-Test. Siehe
[P1-Protokoll](../../docs/p1-datenschutz-umsetzung.md).

## Erweiterte technische Abnahme

[Ergebnisse und Grenzen vom 11.09.2026](../../docs/privacy-qa-2026-09-11.md).
Der zusätzliche `PrivacyRuntimeTest` läuft nur mit `privacy.init.gradle`.

```powershell
./gradlew.bat test build -x dist --console=plain
python -m unittest discover -s tools -p test_local_data.py -v
./gradlew.bat -I tools/consent-smoke/privacy.init.gradle -PprivacyQaMode=minimal runClientGameTest -x dist --console=plain
./gradlew.bat -I tools/consent-smoke/privacy.init.gradle -PprivacyQaMode=xaero runClientGameTest -x dist --console=plain
# Vergleich mit run/mods: Java 25, ohne das inkompatible AnimalBreeding-Add-on.
./gradlew.bat -I tools/consent-smoke/privacy.init.gradle -PprivacyQaMode=modpack-supported runClientGameTest -x dist --console=plain
# Repariertes Modpack: Java 25, mit AnimalBreeding 0.5.16 und World Map aus run/mods.
./gradlew.bat -I tools/consent-smoke/privacy.init.gradle -PprivacyQaMode=modpack-fixed runClientGameTest -x dist --console=plain
python tools/consent-smoke/server-smoke.py
```

`privacyQaMode=modpack` prüft den aktuellen Inhalt von `run/mods` auf Java 21;
das enthaltene C2ME benötigt eine höhere Laufzeit. Es rekonstruiert nach der
[Modpack-Reparatur](../../docs/modpack-reparatur-2026-09-11.md) nicht mehr die alte
AnimalBreeding-JAR. `modpack-supported` bleibt ein Vergleich ohne AnimalBreeding;
`modpack-fixed` prüft das reparierte Gesamtpaket einschließlich Hofübersicht,
manuellem Sync ohne Zustimmung und Xaero-Karte.
Alle Profile liegen separat unter `build/privacy-qa/`. Deren Test-Konfiguration,
`options.txt` und das erzeugte Test-Pack werden beim Wiederholen zurückgesetzt.
Das ursprüngliche `run/mods` wird nur gelesen. Screenshots und
`privacy-qa-results.txt` bleiben in den Profilen. Der Server-Smoke benötigt einen
vorherigen Build und verwendet die bereits vorhandene EULA-Zustimmung des lokalen
Courier-Testservers. Er bindet nur Loopback, wählt einen freien Port und stoppt
nach erfolgreichem Start über die Konsole.

Die erweiterten Tests verwenden echte HTTP-Bodies, NativeImage/Texturen, Reloads,
SoundManager und eine Spielwelt. Nur die reservierte Skin-Test-URL, die Erkennung
des Ottonien-Kontexts in Einzelspielerwelten und beobachtende Zähler sind Testmixins.
Keine öffentlichen Server, reale Browseraktionen oder echten Zugangstokens.

## Isolierte Backend-Integration

Voraussetzungen: lokaler PHP-CLI mit PDO-MySQL, mbstring und sodium; Docker;
benachbartes `OttoRegions Web`. Die folgenden Testzugangsdaten sind ausschließlich
für diese wegwerfbaren, an Loopback gebundenen Datenbanken bestimmt.

```powershell
docker run --detach --name ottoextra-privacy-qa-20260911 --publish 127.0.0.1:33316:3306 --env MYSQL_ROOT_PASSWORD=local-qa-only --env MYSQL_DATABASE=ottoextra_privacy_qa mysql:8.4
docker run --detach --name ottoextra-privacy-qa-mariadb-20260911 --publish 127.0.0.1:33317:3306 --env MARIADB_ROOT_PASSWORD=local-qa-only --env MARIADB_DATABASE=ottoextra_privacy_qa mariadb:10.11
# Für bereits vorhandene gestoppte Testcontainer stattdessen docker start NAME.
python tools/consent-smoke/prepare-backend.py
```

Erst fortfahren, wenn die Testdatenbanken bereit sind (`mysqladmin ping` bzw.
`mariadb-admin ping` im jeweiligen Container). PHP-Erweiterungen sind auf diesem
Rechner vorhanden; ein globales `php.ini` wurde nicht geändert:

```powershell
$qaPhpArgs = @('-n', '-d', 'extension_dir=C:/php/ext', '-d', 'extension=php_pdo_mysql.dll', '-d', 'extension=php_mbstring.dll', '-d', 'extension=php_sodium.dll')
& C:/php/php.exe @qaPhpArgs build/privacy-qa/backend-fixture/setup.php 33317
& C:/php/php.exe @qaPhpArgs build/privacy-qa/backend-fixture/tools/v2-animal-breeding-db.php --apply --smoke
& C:/php/php.exe @qaPhpArgs tools/consent-smoke/backend-mysql.php 33316
& C:/php/php.exe @qaPhpArgs tools/consent-smoke/backend-mysql.php 33317
docker stop ottoextra-privacy-qa-20260911 ottoextra-privacy-qa-mariadb-20260911
```

Die Integrationstests schreiben und löschen ausschließlich ihre synthetischen
Datensätze in `ottoextra_privacy_qa`. Sie lesen keine Produktions-`.env` und starten
keine Live-Cron-Jobs. Die Kopie des AnimalBreeding-Runners erhält eine eigene
Fixture-`.env`. Auth-Tests benutzen konstante Testschlüssel. Der parallele Replay-Test
startet zwei PHP-Prozesse mit einer Barriere nach dem SQL-SELECT; ein SQL-Trigger
simuliert einen Abbruch der Tierbereinigung. Logs unter `build/privacy-qa/backend/`.

Der lokal angewandte [Backend-Patch](backend-auth-replay.patch) dokumentiert den
Replay-Fix zusätzlich zum geänderten Backendverzeichnis. Kein automatisches Deployment.

## RP-Namen: sechs Kalendermonate

Der [RP-Nachtrag](../../docs/rp-verzeichnis-datenschutz.md) dokumentiert die getrennte,
gezielte Live-Umsetzung vom 11.09.2026. Der [Quellpatch](rp-name-retention.patch)
enthält Backend und Datenschutzhinweis, relativ zum gemeinsamen Ottonien-Workspace.
Die Testdatei wird nicht auf den Server veröffentlicht.

Mit den beiden oben beschriebenen, bereiten Loopback-Testdatenbanken:

```powershell
$rpQaArgs = @('-n', '-d', 'extension_dir=C:/php/ext', '-d', 'extension=php_pdo_mysql.dll')
& C:/php/php.exe @rpQaArgs '../OttoRegions Web/tools/rp-name-retention-selftest.php' 33316
& C:/php/php.exe @rpQaArgs '../OttoRegions Web/tools/rp-name-retention-selftest.php' 33317
```

Jeweils 26 Prüfungen. Der Runner liest keine produktive `.env`; er erstellt und
entfernt ausschließlich seine eigene Datenbank `ottoextra_rp_qa_<Prozess-ID>`.
Die Produktions-CLI `tools/rp-name-retention.php --dry-run` ist davon getrennt:
Sie liest die echte Backend-Konfiguration und kann beim Bootstrap das Schema
ergänzen. Nur `--apply` initialisiert Fristen bzw. bereinigt fällige RP-Daten.

## RP-Upload, Sperren und verschlüsselte Sicherungen

[Umsetzung und Live-Nachweise](../../docs/rp-upload-sicherheit-2026-09-11.md),
[Backend-/Website-Patch](rp-security.patch). Bei laufenden Loopback-Datenbanken:

```powershell
$rpSecurityArgs = @('-n', '-d', 'memory_limit=64M', '-d', 'extension_dir=C:/php/ext', '-d', 'extension=php_pdo_mysql.dll', '-d', 'extension=php_mbstring.dll', '-d', 'extension=php_sodium.dll')
& C:/php/php.exe @rpSecurityArgs '../OttoRegions Web/tools/rp-security-selftest.php' 33316
& C:/php/php.exe @rpSecurityArgs '../OttoRegions Web/tools/rp-security-selftest.php' 33317
python tools/consent-smoke/rp-http-test.py
```

Je 47 Sicherheits-/Backupprüfungen; zusätzlich 15 vollständige PHP-HTTP-/CLI-/Importprüfungen.
Der HTTP-Test kopiert ausschließlich PHP-Quellen ohne `config.local.php`, erzeugt
eigene Testkonfiguration/Schlüssel und eine eigene Datenbank. Produktionskonfiguration
wird nicht gelesen; alle HTTP-Requests bleiben auf Loopback. Der gestartete PHP-Server
wird im `finally` beendet; nur die selbst erzeugte Testdatenbank wird gelöscht.
Synthetische Sicherungsdateien bleiben in eindeutig benannten Testverzeichnissen.

Die Java-Prüfungen `RpNameAuthenticatedTransportTest` ergänzen die bestehende Queue-
und Payload-Prüfung. Seit 0.1.17-n verwenden sie die öffentliche Upload-Methode mit
beiden aktuellen Freigaben. Geprüft werden fehlende Online-/Upload-Freigabe, Widerruf
beider Zwecke, alte Jobs nach erneuter Freigabe, echte signierte Antworten und
Token-Erneuerung. Keine Reflexion zum Umgehen einer Veröffentlichungssperre.

## Banner-Cache

`BannerCacheClientTest` läuft im bestehenden `runClientGameTest` mit. Er erzeugt
80 kleine PNG-Fixtures ausschließlich im isolierten Testprofil, lädt und rendert
sie bei verweigerter Bilddownload-Zustimmung und verwirft jeden versuchten API-Aufruf.
Ein kleiner separater Cache prüft native Texturfreigabe und erneutes Laden nach
Verdrängung. Die erzeugten Dateien und Texturen werden anschließend entfernt.

`BannerTextureBudgetTest` prüft zusätzlich Anzahl, dekodiertes Pixelbudget,
Verwendungsreihenfolge und Aktualisierung vorhandener Einträge.
