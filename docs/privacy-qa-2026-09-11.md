# P0/P1: ausgeführte technische Abnahme

Stand: **11.09.2026**. Getestet wurde der lokale Arbeitsstand von OttoExtra **0.1.17-l** einschließlich der bereits vorhandenen Änderungen. Die technischen Kernprüfungen bestehen. Das ursprünglich inkompatible Bestands-Modpack wurde anschließend repariert und mit AnimalBreeding erneut erfolgreich geprüft: [Reparaturprotokoll](modpack-reparatur-2026-09-11.md). Keine Veröffentlichung und kein Backend-Deployment.

## Ergebnisse

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| Java-Tests und Release-Build | **783 Tests**, 0 Fehler, 0 übersprungen | `build/privacy-qa/final-build.log`, `build/test-results/test/` |
| Lokale Datenverwaltung | **7 Python-Tests bestanden**: Export/Hashes, Pfade, Vorschauänderungen, Dateisperre, Altersauswahl | `build/privacy-qa/local-data-tests.log` |
| Minecraft ohne optionale Add-ons | **Bestanden**, Java 21 | `build/privacy-qa/minimal-run.log` |
| Minecraft mit Xaero | **Bestanden**, World Map 1.41.0, Minimap 25.3.12, XaeroLib 1.1.15 | `build/privacy-qa/xaero-run.log` |
| Ursprüngliches Bestands-Modpack | **Start fehlgeschlagen**: AnimalBreeding-Version und Java-Anforderung | `build/privacy-qa/modpack-run.log` |
| Repariertes Bestands-Modpack | **Bestanden**, Java 25, AnimalBreeding 0.5.16 einschließlich Hofübersicht und verweigertem manuellen Sync | `build/modpack-repair/modpack-fixed-run.log`, [Details](modpack-reparatur-2026-09-11.md) |
| Vergleichslauf des Modpacks | **Bestanden**, Java 25; 69 lokale Add-on-JARs, AnimalBreeding ausgeschlossen; World Map 1.40.16 zusätzlich über Loom | `build/privacy-qa/modpack-supported-run.log`, dortiges `mods-inventory.json` |
| Dedicated Fabric Server | **Start und sauberer Stopp bestanden**, Java 21; finale Client-JAR im Test-`mods/` | `build/privacy-qa/dedicated-server-run.log` |
| PHP-Selbsttests | **180 Prüfungen** in fünf vorhandenen Skripten bestanden | `build/privacy-qa/backend/*selftest.log` |
| MySQL 8.4.11, Auth/Chat | **21 Integrationsprüfungen bestanden**, nach Replay-Fix | `build/privacy-qa/backend/sql-33316-after.log` |
| MariaDB 10.11.19, Auth/Chat/Aufbewahrung | **26 Integrationsprüfungen bestanden**, nach Replay-Fix | `build/privacy-qa/backend/sql-33317-after.log` |
| AnimalBreeding-Migration und DB-Smoke | **Bestanden auf MariaDB**: Revisionen, territoriale Rechte, Teilen, Eigentum, Finder, Hofzuordnung, Namen und Beobachtungen | `build/privacy-qa/backend/animal-breeding-mariadb.log` |

Die Zahl der JARs ist nicht die Zahl aller geladenen Mods: Fabric API, verschachtelte Bibliotheken und der Testtreiber kommen hinzu. Der Vergleichslauf verwendet Kopien aus `run/mods`, keine Änderungen am ursprünglichen Spielprofil. Clientprüfungen laufen mit dem aktuellen Loom-Quellstand; die fertig remappte Release-JAR wurde zusätzlich inventarisiert und im Dedicated-Server-Test verwendet. Das ersetzt keinen separaten Clientlauf über einen Produktionslauncher.

## Tatsächlich geprüfte Laufzeitfälle

- [x] Veraltete Zustimmungen werden beim Start zurückgesetzt. Alle fünf Online-Zwecke ablehnen; RP-Beobachtungsupload bleibt gesperrt. Späteres Erlauben öffnet zuerst Informationen; Widerruf wirkt sofort und überlebt Settings-Abbrechen.
- [x] Chatlog: eigener Dialog, Ottonien-Scope, erlauben/widerrufen. Vote: ENTER und Abbrechen ohne Browser-/Clipboard-Effekt; bestätigte Aktion einmalig. Die externe Vote-Aktion wird im Test durch einen Zähler ersetzt.
- [x] Sieben englische Dialoge im echten Client gerendert und Screenshots visuell geprüft. Texte und Entscheidungsbuttons bei 1280 × 720 lesbar; ESC erteilt keine Freigabe. Deutsche Screenshots ebenfalls vorhanden.
- [x] Ohne Bildfreigabe erreichen weder Skin- noch Banneranfragen den lokalen HTTP-Server. Mit Freigabe funktionieren echte Übertragung, PNG-Diskcache und native Minecraft-Texturregistrierung.
- [x] Antwortkörper wird nach den ersten 24 PNG-Bytes angehalten. Währenddessen GUI-Wechsel, Widerruf und erneute Freigabe; anschließend restliche Bytes freigegeben. Alte Skin-/Bannerjobs erzeugen weder PNG-/temporäre Dateien noch native Texturen. Erfolgreiche Kontrollfälle schließen einen grundsätzlich defekten Bildpfad aus.
- [x] Ausstehende Pack-Aktivierung mit alter Freigabe bleibt nach Widerruf/erneuter Freigabe blockiert. Eine gültig gestartete Aktivierung durchläuft einen echten Ressourcenreload; ein Marker aus dem ZIP wird danach über Minecrafts ResourceManager gelesen. Bereits autorisiert aktivierte lokale Inhalte bleiben bei späterem Entzug der Downloadfreigabe erhalten, wie im Dialog beschrieben.
- [x] Manuell deaktiviertes Pack wird auch mit altem `respectUserDisable=false` nicht wieder aktiviert oder neu geladen.
- [x] Echte Spielwelt: niedrige Gesundheit löst Effekte aus. Maximalvorschau mit Blur-Stärke 3, acht Blur-Pässen, FOV bis 25°, Vignette und Herzschlag gerendert. Bewegung und zusätzlicher Schaden geprüft. Reduzierter Modus stoppt Blur/FOV/Herzschlag; Hauptschalter stoppt auch die erzwungene Vorschau. Soundaufrufe werden am echten SoundManager gezählt; daraus folgt keine Zusage zur Verträglichkeit für photosensitive Personen.
- [x] Welt verlassen und erneut betreten, während sämtliche Freigaben verweigert sind: unter simuliertem Ottonien-Kontext null Aufrufe des OttoExtra-HTTP-Transports; keine wiederbelebte Zustimmung.
- [x] Xaero-Karte geöffnet; Aufruf der tatsächlichen OttoExtra-Overlay-Injection gemessen, sowohl mit World Map 1.41.0 als auch 1.40.16/XaeroPlus im Vergleichs-Modpack.

Das Skin-Fixture ersetzt ausschließlich die reservierte Test-URL durch Loopback-HTTP; der Ottonien-Kontext wird ausschließlich im Testmod für Einzelspielerwelten simuliert. Netzwerk-/Cache-/Texturcode und Freigabesperren bleiben aktiv. **Kein Beitritt zum öffentlichen Ottonien, kein Paketmitschnitt und keine Aussage über vollständige Netzwerkstille von Minecraft oder Fremdmods.** Die Transportmessung ist keine Prüfung anderer HTTP-Implementierungen. Testmixins gelangen nicht in die Release-JAR.

Der simulierte Ottonien-Kontext löst auch den vorhandenen Chatkanal-Befehl `/s`
aus (`ChatModule`). Der lokale Vanilla-Testserver kennt ihn nicht und zeigt deshalb
eine Befehlsfehlermeldung. Das ist erwartetes Fixture-Verhalten und verdeutlicht:
null OttoExtra-HTTP-Aufrufe bedeutet nicht null Minecraft-Netzwerkpakete.

Screenshots und einzelne Ergebniszeilen liegen unter `build/privacy-qa/<minimal|xaero|modpack-supported>/`. Der letzte Minimal- und Modpack-Vergleichslauf enthalten die erweiterten Maximal-/Reconnect-Fälle; frühere Zwischenläufe wurden nach Korrektur der Test-Fixtures wiederholt. Insbesondere wird Pack-Erfolg jetzt durch tatsächlich geladene Ressourcen belegt, nicht nur durch einen Eintrag in der Pack-Auswahl.

## Gefundene Fehler und Änderungen

### P0: konkurrierender Challenge-Verbrauch im Backend — lokal behoben

Zwei PHP-Prozesse konnten dieselbe Login-Challenge erfolgreich verbrauchen: beide lasen den Datensatz, danach löschten beide ohne Prüfung der betroffenen Zeilenzahl. Der Test hält beide Prozesse nach dem echten SQL-SELECT an und lässt sie anschließend konkurrieren. **Vorher: zwei Gewinner. Nachher: genau einer**, auf MySQL und MariaDB.

Geändert: [Backend-Auth](<../../OttoRegions Web/includes/v2/auth.php>) und [PHP-Regression](<../../OttoRegions Web/tools/v2-selftest.php>). Nur der Request, dessen DELETE genau eine Zeile entfernt, erhält die Challenge. Die normalen Token-, Sperrlisten-, Ablauf- und Replay-Prüfungen bestehen weiterhin.

Das Backendverzeichnis besitzt hier kein eigenes Git-Repository. Originale liegen unter `build/privacy-qa/backend-originals/`; der nachvollziehbare [Patch](../tools/consent-smoke/backend-auth-replay.patch) ist zusätzlich im OttoExtra-Repository abgelegt. **Live-Übernahme bleibt ausstehend.** Die getestete lokale Änderung ist kein Beleg für den Stand des veröffentlichten Dienstes.

### P1: unvollständiges Xaero-Mixin-Ziel — behoben

[WorldMapOverlayMixin](../src/client/java/de/ottoextra/mixin/WorldMapOverlayMixin.java) enthält vollständige INVOKE-Deskriptoren für Entwicklungs- und Produktionsnamen. Signaturen gegen World Map 1.40.16/1.41.0 geprüft. Die vorherige Warnung über das unvollständige Renderziel entfällt; beide Kartenläufe bestätigen die ausgeführte Injection. Der Test misst den Einstieg in den Hook, nicht reale Regionsdaten eines öffentlichen Servers.

### P1: Bestands-Modpack — ursprünglicher Fehler, inzwischen repariert

**Nachtrag:** Die folgenden Punkte beschreiben den ersten Prüflauf. AnimalBreeding
0.5.16 wurde gegen Core 0.1.17-l gebaut, der Clientstart auf Java 25 eingestellt und
die doppelte World-Map-Runtime entfernt. Der vollständige Lauf einschließlich
AnimalBreeding besteht; [Änderungen, Sicherung und verbleibende Warnungen](modpack-reparatur-2026-09-11.md).

1. `run/mods/OttoExtra-AnimalBreeding-0.5.9.jar` verlangte `ottoextra >=1.16.3`; der Core ist `0.1.17-l`. Auch der damalige Add-on-Quellstand nannte eine 1.x-Abhängigkeit. **Erledigt:** tatsächlich verwendete API gegen den Core kompiliert, Add-on neu gebaut und gemeinsam getestet. Keine Loader-Abhängigkeitsübersteuerung.
2. Das enthaltene C2ME-Natives-Modul verlangt Java ≥22. Java 21 bleibt für den Core gültig; das Bestands-Modpack benötigt eine passende höhere Laufzeit. Vergleichslauf mit bereits installiertem Java 25.0.2 bestanden.
3. AmbientSounds/CreativeCore melden ungültige Resource-Pack-Metadaten; malilib meldet beim frischen Profil eine fehlende Konfigurationsdatei. Dazu kommen optionale Mixin-/Integrationswarnungen und ein Accessor-Konflikt zwischen BetterF3 und Fabric-Test-API. Die getesteten Szenarien bestehen trotzdem. **Vor Paketfreigabe:** konkrete Fremdmod-Versionen/Assets bereinigen und erneut prüfen; kein pauschales Kompatibilitätszertifikat.

### P1: Datenbank-Dialekt — dokumentierter Befund

Die AnimalBreeding-Migrationsfolge scheitert auf MySQL 8.4 bei `ADD COLUMN IF NOT EXISTS`; auf MariaDB 10.11 besteht sie. Die Migrationskommentare nennen MariaDB, die Backend-README pauschal MySQL. **Nächster Schritt:** unterstützten Dialekt in der Betriebsdokumentation festlegen oder die Migrationen gezielt portieren. Auth/Chat bestehen auf beiden getesteten Systemen. Keine automatische Änderung produktiver Schemata vorgenommen.

## Datenschutzprüfung: belegt und weiterhin offen

Belegt mit synthetischen Konten: Token-Manipulation abgelehnt, aktive Tokens durch Sperre/Widerruf ungültig, Challenge-Ablauf und paralleler Replay-Schutz, private Chatfreigabe nur für Besitzer/Empfänger, getrennte Inbox, verschlüsseltes Original im SQL-Speicher, Ablauf vor Löschlauf sowie kaskadierende Löschung. AnimalBreeding-DB-Smoke prüft Teilen/Eigentum/territoriale Zugriffe.

Die 60-Tage-Tierbereinigung wurde mit **501 abgelaufenen und einem unmittelbar jüngeren Datensatz** geprüft: mehrere Chargen, Beobachtungen entfernt, Finder-Fremdschlüssel wirksam, junge Akte erhalten, Wiederholung ohne weitere Löschung. Ein SQL-Trigger simuliert einen Fehler nach Beginn der Bereinigung; die Charge wird einschließlich Beobachtungen zurückgerollt.

- [ ] Betreiberangaben, Rechtsgrundlagen und zulässige Fristen bestätigen. Die Tests führen bestehende Regeln aus; sie machen diese nicht automatisch rechtlich ausreichend.
- [ ] Tatsächliche Live-Schemata, Cron-Ausführung, HTTP-Router, Identitätsnachweise und Rollenkonfiguration abnehmen. DB-Funktionstests ersetzen diesen HTTP-/Deployment-Nachweis nicht.
- [ ] Auskunft/Berichtigung/Löschung für RP-Daten, Accountverknüpfungen, historische Zuchtsnapshots, Logs, Exporte, Backups und Restore ausarbeiten. Die geprüfte Tierbereinigung lässt historische Ereignisse bewusst bestehen.
- [ ] Reale Support-/Logexporte auf vertrauliche Daten prüfen; keine privaten Live-Exporte für diese Tests eingelesen.
- [ ] Rechte-/KI-Herkunft und Modrinth-Disclosures bleiben gesonderte Aufgaben. RP-Beobachtungsuploads und zusätzliche Tierumrisse bleiben gesperrt.

## Kandidat und Wiederholung

`build/libs/ottoextra-0.1.17-l.jar`, **31.085.673 Bytes**.

SHA-256: `db910ae1fc74da3c6d1317c33de9ed71236c30b91c4f5e5323e63117cafdc81a`.

Client-Metadaten geprüft. Keine Consent-/Privacy-/Courier-Testtreiber, RuntimeProbe, DevAuth-Klassen, Always-Debug-Markierung oder `.env`-/`.pem`-/`.key`-Dateinamen im JAR gefunden. Dateinameninventar ersetzt keine vollständige Inhaltsprüfung auf Geheimnisse. Die Dedicated-Server-Kopie hat denselben Hash. `dist` wurde nicht ausgeführt, weil es ältere JARs löschen würde.

Ausführbare Werkzeuge und genaue Kommandos: [Consent-/Privacy-QA](../tools/consent-smoke/README.md). Datenbanken ausschließlich in eigenen lokalen Containern mit synthetischen Daten; keine Produktions-`.env`, kein Live-Cron. Die Testcontainer werden nach der Prüfung gestoppt, Logs/Fixtures bleiben für die Nachprüfung erhalten.

Dieser Prüfbericht, Testtreiber und die beschriebenen lokalen Fixes wurden mit generativer KI erstellt.
