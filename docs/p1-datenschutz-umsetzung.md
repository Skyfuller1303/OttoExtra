# P1: Clientänderungen und verbleibende Abnahmen

> Aktualisierung 13.09.2026: Gezielte Finder-Umrisse sind wieder aktiv (Suchziel, unterstützte Tierart und aktive Verbindung erforderlich). AnimalOutlinePolicy sperrt weiterhin automatische Geschlechtsumrisse. Die folgenden Aussagen zur vollständigen Finder-Sperre beschreiben den früheren P1-Stand; eine neue Live-/Release-Abnahme wurde hier nicht durchgeführt.

Stand: **11.09.2026**. Fortsetzung des [Release-/Datenschutz-Fahrplans](release-datenschutz-fahrplan.md). Clientänderungen lokal implementiert; **P1 insgesamt bleibt wegen verbleibender Betreiber-, Backend- und Modpack-Abnahme offen**. Die [erweiterte technische Abnahme](privacy-qa-2026-09-11.md) enthält echte Client-, SQL- und Serverläufe sowie die gefundenen Fixes. Späterer gezielter Live-Nachtrag: [RP-Verzeichnis, Datenschutzhinweis und sechsmonatige Bereinigung](rp-verzeichnis-datenschutz.md). Kein Modrinth-Release.

## Umsetzung nach Prioritäts-ID

| ID | Stand | Ergebnis |
|---|---|---|
| P1-01 | Implementiert / getestet | [BoundedBody](../src/client/java/de/ottoextra/privacy/BoundedBody.java) begrenzt echte Empfangsbytes einschließlich Antworten ohne Content-Length. [ConsentHttp](../src/client/java/de/ottoextra/privacy/ConsentHttp.java) beendet auch langsame Antwortkörper. [PngLimits](../src/client/java/de/ottoextra/privacy/PngLimits.java) prüft PNG-Header/Abmessungen vor nativer Bildallokation, auch bei vorhandenen Disk-Caches. |
| P1-02 | Implementiert / getestet | Chatlog neu/ungeklärt aus. Eigener Informationsdialog und Serverumfang; Scope-Wechsel verlangt neue Freigabe. Bestätigte aktuelle Wahl überlebt Neustart, Settings-Abbrechen und Backup-Restore. Alte Backups reaktivieren das Log nicht. `LocalPrivacy`, `LocalChatConsentScreen`, `ChatMessageLog`, `OttoExtraConfig`, `SettingsRegistry`. |
| P1-03 | Offline-Werkzeug implementiert / getestet | [Lokale Datenverwaltung](lokale-daten.md): Kategorien, Bestandsliste, verifizierter ZIP-Export, expliziter Löschplan und Prozesssperre. Log-/Cache-Budgets; Altersauswahl ab 30 Tagen. Persönliche Archive/Backups bleiben getrennt und werden nicht automatisch gelöscht. Keine Ingame-Löschoberfläche. |
| P1-04 | RP-Teil live umgesetzt; Gesamtprozess offen | [RP-Zuordnungen und RP-Protokolle](rp-verzeichnis-datenschutz.md) erhalten sechs Kalendermonate. [Upload-Schutz, Sperren, verschlüsselte RP-Backups und Restore](rp-upload-sicherheit-2026-09-11.md) zusätzlich live umgesetzt und getestet. Laut Betreiber keine netcup-Backups. Andere Datentypen, konkretisierte Betreiberbewertung/Spielerinformation, gegebenenfalls selbst angelegte zusätzliche Kopien und erste zeitgesteuerte Ausführung bleiben offen. |
| P1-05 | Implementiert / getestet | Manuelle Minecraft-Pack-Auswahl ist verbindlich, auch bei altem `respectUserDisable=false`. Der irreführende Abschaltschalter entfällt. `autoEnable` betrifft die Erstinstallation; ein vorhandenes deaktiviertes Pack lässt sich bewusst über Minecrafts Ressourcenpaket-Menü wieder aktivieren. |
| P1-06 | Implementiert / getestet | Vote-Taste öffnet Informationen mit den tatsächlichen Browserzielen und dem zu kopierenden Namen. Erst Bestätigen öffnet Seiten/überschreibt Clipboard. ENTER bestätigt im Dialog nicht; GUI-/Overlay-Eingaben werden bis zum Loslassen unterdrückt. Erinnerungen lassen sich weiter abschalten. |
| P1-07 | Implementiert / im Client getestet | Low-Health bleibt standardmäßig aus. Reduzierter Modus und Hauptschalter funktionieren auch bei maximaler Vorschau, Bewegung und zusätzlichem Schaden. Acht Blur-Pässe, FOV bis 25°, Vignette und echte SoundManager-Aufrufe geprüft; DE-/EN-Dialoge visuell geprüft. |
| P1-08 | Risiko begrenzt; Freigabe offen | [AnimalOutlinePolicy](../src/client/java/de/ottoextra/horses/AnimalOutlinePolicy.java) sperrt zusätzliche Finder-/Geschlechtsumrisse in allen Kontexten. Alte Config/Schalter/F8 können sie nicht aktivieren. Finder-Listen bleiben nutzbar; Vanilla-/andere Modumrisse werden nicht unterdrückt. Reaktivierung braucht belegte Erlaubnis und passende Serverkontrolle. |
| P1-09 | Lokaler Kandidat geprüft; Release-Abnahme offen | README auf tatsächlichen Quellstand gebracht, englische Modbeschreibung, bestehende Client-Metadaten/Abhängigkeiten beibehalten. Build, Unit- und Clienttest sowie JAR-Inventar dokumentiert. Noch keine breite/stabile Freigabe. |

## Konkrete Grenzen

| Empfang / Ablage | Grenze |
|---|---|
| Auth-/Kontoverknüpfungsantworten | 64 KiB |
| Packmanifest / Versionsprüfung | 1 MiB |
| Sonstige API-/Aktivitäts-/RP-JSON-Antworten | 4 MiB |
| Skin-PNG | 1 MiB; maximal 256 × 256 Pixel |
| Banner-PNG | 5 MiB; maximal 1024 × 1024 Pixel |
| HTTP-Zeit | Request-Timeout einschließlich Antwortkörper; ohne Vorgabe 30 s, immer höchstens 120 s. Beim Pack umfasst die Frist auch Redirects und Streaming-Body; Standard-Packkonfiguration 60 s. |
| Parallelität | Höchstens vier Skin-Downloads; Banner-Anfragen ebenfalls begrenzt. Skinfehler/HTTP-Fehler erhalten 45 s Pause. |
| Lokale Daten | Größen-, Datei- und Speichergrenzen siehe [Datenverwaltung](lokale-daten.md#speichergrenzen). |

Größere legitime Antworten/Bilder schlagen kontrolliert fehl und können Fallback-Anzeigen ergeben. Eine PNG-Headerprüfung ersetzt nicht die eigentliche Formatprüfung im Decoder. Laufende Nutzerprozesse und andere Mods unterliegen nicht unseren internen Quoten. Minecrafts eigener Netzwerkverkehr und Vanilla-Logs bleiben unabhängig.

## Backend: vorhandene Ansätze, nächster Arbeitsschritt

Die frühere offene Codezuordnung ist eingegrenzt: [OttoRegions Web](<../../OttoRegions Web/README.md>) enthält `/v2`-Auth, Tierdaten und Synchronisation. Die Landingpage `OttoExtra_Web` allein bildet dieses Backend nicht ab. Für die gezielt geänderten RP-Dateien wurde der Live-Stand inzwischen gesichert, abgeglichen und nach Deployment per Hash geprüft; das ist keine vollständige Abnahme des übrigen Backends.

- [Auth](<../../OttoRegions Web/includes/v2/auth.php>): alte Challenges werden beim Erstellen einer neuen Challenge entfernt; [Konfiguration](<../../OttoRegions Web/includes/v2/config.php>) nennt 120 s Bereinigungsalter, standardmäßig 60 s Challenge-Gültigkeit und bis zu 3600 s JWT-Gültigkeit. Token-Ablauf ist kein vollständiges Datenlöschkonzept.
- [Tierbereinigung](<../../OttoRegions Web/includes/v2/animal_breeding.php>): `v2AnimalBreedingCleanupStaleHorses` löscht seit mindestens 60 Tagen nicht beobachtete Akten/Beobachtungen transaktional in Chargen bis 500; abhängige Editor-/Finder-Einträge sollen per Fremdschlüssel folgen. Historische Zuchtereignis-Snapshots bleiben ausdrücklich erhalten. Ausführung im [Cron](<../../OttoRegions Web/cron/sync_public_endpoints.php>).
- **Isoliert geprüft:** Auth/Chat mit MySQL und MariaDB; Tier-/Hof-/Finder-/Sharing-Prüfungen mit MariaDB. 501 abgelaufene Tierakten, Fremdschlüssel, Fristgrenze, Wiederholung und Rollback nach simuliertem Fehler bestanden. Parallel nutzbare Auth-Challenge gefunden und lokal korrigiert. Einzelheiten im [QA-Protokoll](privacy-qa-2026-09-11.md).
- **Noch zu belegen:** tatsächliche Live-Cron-Ausführung und Live-Schemata; erlaubte Aufbewahrung der verbleibenden Snapshots; Verfahren für Namen/UUIDs/RP-Daten, Accountverknüpfungen, API-/Webserver-Logs, Exporte und Backups. Die SQL-Tests sind keine Datenschutz- oder Deployment-Abnahme.

Der anschließende [RP-Nachtrag](rp-verzeichnis-datenschutz.md) setzt die vom Betreiber gewünschte Frist für RP-Daten einschließlich Vorschau und begrenzter transaktionaler Chargen um. Die übrigen Fristen/Datentypen, historische Kopien, Wiederimport und Restore bleiben offen. Geschützte Chatfreigaben haben bereits einen Ablauf-/Cleanup-Pfad; dieser einschließlich kaskadierender Löschung wurde isoliert geprüft. Beim RP-Nachtrag wurde ausschließlich die neue RP-CLI live ausgeführt; die früheren Tier-/Chat-Prüfungen blieben isoliert.

## Validierung

**Aktueller Stand:** [ausführlicher QA-Bericht](privacy-qa-2026-09-11.md): 783 Java-Tests,
7 Python-Tests, 180 PHP-Selbstprüfungen, echte SQL-Integration auf zwei Datenbanken,
englische Dialoge, verzögerte Bildantworten/GUI-Wechsel, echter Pack-Reload,
Low-Health-Maximum, Join/Reconnect mit simuliertem Ottonien-Kontext und Dedicated
Server bestanden. Das zunächst fehlschlagende Modpack wurde anschließend
[repariert und mit AnimalBreeding geprüft](modpack-reparatur-2026-09-11.md).
Die folgenden Stichpunkte beschreiben den
früheren, kleineren Smoke-Lauf; dessen offene EN-/Weltfälle wurden inzwischen geprüft.

- `./gradlew.bat test build -x dist --console=plain`: **783 Java-Tests, keine Fehler/ausgelassenen Tests**, Build erfolgreich. Bestehende Erwartungen an Default-Logging, Pack-Reaktivierung, Glow und Low-Health-Vorschau wurden an das neue Verhalten angepasst.
- `python -m unittest discover -s tools -p test_local_data.py -v`: **7 Tests bestanden**. Auswahl/Archivtrennung, Exporthashes, Bestandsänderung nach Vorschau, manipulierte Pfade, Altersauswahl, Prozesssperre und fremde Ausgabepfade.
- `./gradlew.bat -I tools/consent-smoke/client.init.gradle runClientGameTest -x dist --console=plain`: **Client-Smoke-Test bestanden**. Alte Online-Freigaben zurückgesetzt, Chatlog bewusst erlauben/widerrufen, Ottonien-Scope, Vote-ENTER/Abbrechen ohne externe Wirkung, explizite Bestätigung einmalig. Der Vote-Test verwendet einen Zähler anstelle echter Browser-/Clipboard-Aufrufe.
- Während des echten Testclients verweigerte `local-data.py ... list` den Zugriff mit der Aufforderung, Minecraft zu schließen. Windows-Java-/Python-Dateisperre damit zusätzlich integriert geprüft.
- Screenshots `build/consent-smoke/client/screenshots/0003_p1-chat-consent-de.png`, `0004_p1-vote-consent-de.png`, `0005_p1-lowhealth-max-hud.png`: DE-Texte und Buttons vollständig sichtbar; maximaler HUD-Rand gerendert. Englisch ist übersetzt, hier nicht separat visuell abgenommen.
- Logs: `build/p1-validation.log`, `build/consent-smoke/p1-run.log`. DevAuth im Smoke-Test ausgeschaltet; kein Serverbeitritt. Erwartete Realms-Meldungen des Fabric-Testkontos bedeuten nicht, dass Minecraft selbst offline war.

Build weiterhin mit `-x dist`: Das bestehende `dist`-Task würde ältere JARs entfernen. Keine dist-Artefakte ersetzt, keine Kontoeinstellungen bei Modrinth geändert.

### Artefakt

`build/libs/ottoextra-0.1.17-l.jar` — lokaler, nicht veröffentlichter Kandidat. **SHA-256:** `db910ae1fc74da3c6d1317c33de9ed71236c30b91c4f5e5323e63117cafdc81a`.
Größe: 31.085.673 Bytes. Metadaten: Client, Minecraft `~1.21.11`, Java `>=21`,
Fabric Loader `>=0.18.1`, Fabric API `>=0.141.4+1.21.11`. Keine Smoke-/E2E-Testklassen,
DevAuth-Klassen, Always-Debug-Markierung oder `.env`-/`.pem`-/`.key`-Dateinamen im JAR
gefunden; diese Inventarprüfung ersetzt keine Prüfung sämtlicher Log-/Dateninhalte.
Das unvollständige Xaero-Renderziel wurde korrigiert und mit zwei World-Map-Versionen
getestet. Veraltete API-/Gradle-Nutzung und konkrete Fremdmod-Befunde bleiben im
[QA-Protokoll](privacy-qa-2026-09-11.md) dokumentiert. Das Offline-Werkzeug liegt separat
im Repository und muss bei einem späteren Release mit seiner Anleitung angeboten werden.

## Vor breiter Verteilung offen

- [ ] P0-Betreiber-/Rechte-/Berechtigungsnachweise schließen; RP-Beobachtungsuploads bleiben gesperrt.
- [ ] P1-04: abgestimmtes Lösch-/Aufbewahrungskonzept und isolierte Backend-Abnahme.
- [x] P1-07: volle Low-Health-Kombination in einer Welt bei maximalen Einstellungen, Bewegung/Schaden und Sound geprüft. Keine Unbedenklichkeitszusage für photosensitive Personen.
- [ ] P1-08: Servererlaubnis/-kontrolle klären, falls Umrisse wieder angeboten werden sollen. Bis dahin bleiben sie gesperrt.
- [ ] P1-09: finalen Kandidaten im Produktionslauncher abnehmen. Die lokale Modpack-Kombination ist repariert: AnimalBreeding 0.5.16, Java 25, passende Xaero-Versionen; vollständiger Clienttest einschließlich verweigertem manuellen Add-on-Sync bestanden. [Reparaturprotokoll](modpack-reparatur-2026-09-11.md); Betreiberfreigaben bleiben offen.

Modrinth-Zulässigkeit bleibt getrennt zu klären: [Einreichungscheckliste](modrinth-submission-checklist.md). Die P1-Änderungen allein entscheiden weder über Rechte/KI-Herkunft noch über Plattformfreigabe.

## Herkunft dieses Beitrags

Die P1-Codeänderungen, Tests und Dokumentation dieses Arbeitslaufs wurden mit generativer KI erstellt. Das ist ein konkreter Herkunftsbeleg für diesen Beitrag, keine Aussage über den KI-Anteil des gesamten Projekts oder die Rechte an vorhandenen Assets. Für eine Veröffentlichung in die Herkunfts-/Disclosure-Prüfung aufnehmen.
