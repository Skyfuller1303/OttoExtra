# RP-Verzeichnis: Datenschutz und Aufbewahrung

Stand: **11.09.2026**. Nachtrag zu [P0/P1](release-datenschutz-fahrplan.md), einschließlich gezielter Live-Umsetzung für RP-Daten. **Neuester Stand:** [Upload-Schutz, Löschstatus und verschlüsselte Backups umgesetzt und getestet](rp-upload-sicherheit-2026-09-11.md); [Interessenabwägung vorbereitet](rp-interessenabwaegung.md).

**Aktuelle RP-Namen bleiben für die Mod abrufbar und lokal nutzbar.** Eine Namenszuordnung ist nicht allein deshalb verboten, weil sie auf einem Spielerrechner gespeichert wird. Die zentrale Erfassung, Ausgabe an andere Nutzer und Aufbewahrung benötigen eine passende Rechtsgrundlage und nachvollziehbare Information. Einwilligung ist nicht die einzige mögliche Grundlage; berechtigtes Interesse erfordert insbesondere Zweck, Erforderlichkeit und Abwägung der betroffenen Interessen. [EDPB: rechtmäßige Verarbeitung](https://www.edpb.europa.eu/sme/be-compliant/process-personal-data-lawfully_en)

## Bestätigte Verarbeitung

- Betreiber hat netcup und Serverstandort Deutschland für Website, API und Datenbank bestätigt. Betreibername und Kontakt stehen bereits in der öffentlichen Datenschutzerklärung; hier wird keine zusätzliche Privatadresse kopiert.
- Nachtrag vom 11.09.2026: Laut Betreiber erstellt netcup keine Backups. Die selbst eingerichteten verschlüsselten RP-Backups mit 30-Tage-Rotation bleiben davon unabhängig aktiv. Freigabewunsch und noch zu präzisierende Spielerinformation: [Entscheidungsvermerk](rp-interessenabwaegung.md).
- **Aktuelle Zuordnungen:** Minecraft-UUID, Accountname und RP-Name werden über Spieler-API-Abfragen an die Mod und weitere abrufende Anwendungen ausgegeben. Diese Angaben sind nicht anonym und nicht ausschließlich für den Betreiber sichtbar.
- **Verwaltungsdaten:** RP-Änderungs- und Anfrageprotokolle liegen getrennt im zugriffsgeschützten Backend. Sie können alte/neue RP-Namen, Absender-/Zielaccount, Zeitpunkt, Ergebnis, IP-Adresse und User-Agent enthalten. Betreiberangabe: Verwaltungszugriff nur durch ihn; keine unabhängige vollständige Prüfung der Serverzugriffsrechte.
- **Lokale Daten:** Personenbuch, importierte Zuordnungen, Notizen und Exporte liegen auf Spielergeräten. Serverbereinigung löscht keine bereits heruntergeladenen Kopien. Das bestehende [Offline-Werkzeug](lokale-daten.md) erlaubt die gesonderte Verwaltung von `people`, `cache` und `backups`; die persönlichen Archive haben dadurch keine automatische Sechsmonatsfrist.

## Umgesetzte Fristen

Die sechs Monate sind die hier gewählte Betriebsregel nach Betreiberwunsch, **keine gesetzliche Mindestfrist**. Frühere erforderliche Berichtigung oder Löschung bleibt möglich. Diese Änderung betrifft RP-Zuordnungen und die beiden RP-Protokolltabellen, nicht pauschal sämtliche Account-, Tier-, Webserver- oder Spieldaten.

| Bestand | Ablauf / Verhalten |
|---|---|
| `external_participants.rp_name` | Sechs **Kalendermonate** ab ausdrücklichem Schreiben/Bestätigen des RP-Namens; dann `rp_name` und `rp_name_expires_at` auf `NULL`. UUID, Accountname und übrige Spielerdaten bleiben bestehen. |
| Bestehende RP-Namen ohne belegbaren Zeitpunkt | Einmalig sechs Monate ab erster angewandter Migration. Wiederholte Bereinigung verlängert die Frist nicht. |
| `external_participant_rp_name_log` | Einträge sechs Kalendermonate ab `created_at`, danach löschen. |
| `external_participant_rp_name_request_log` | Gleiche Frist einschließlich dortiger IP-/User-Agent-Daten. |
| Protokolle ohne Zeitpunkt | Einmalige sechsmonatige Übergangsfrist. |
| Abruf / gewöhnlicher Spielersync | Verlängert die RP-Frist nicht. Ein ausdrücklich gelieferter RP-Namenswert aus der Quelle gilt als erneute Aktualisierung. |

Bereinigung täglich um **03:17 Uhr Serverzeit**; zusätzlich beim vorhandenen öffentlichen Sync-Cron. Ablaufdaten werden in UTC berechnet. Kalendergrenzen einschließlich Monatsende/Schaltjahr sind getestet; sechs Monate werden nicht durch 180 Tage ersetzt. Ein Ablaufdatum wird beim nächsten erfolgreichen Lauf umgesetzt, nicht sekundengenau. Ausfälle und verbleibende Chargen müssen überwacht werden.

## Was wurde wo geändert?

| Datei im benachbarten `OttoRegions Web` | Änderung |
|---|---|
| `includes/rp_name_retention.php` | Additive Spalte/Index, gezielte RP-Schreibfunktion, Vorschau und Bereinigung in begrenzten SQL-Chargen. |
| `includes/bootstrap.php` | Hilfsdatei laden und Schema idempotent ergänzen. |
| `api/index.php` | Admin- und bestehender Community-Schreibpfad setzen die RP-Frist. Keine neue Freigabe des Community-Uploads. |
| `includes/public_sync_runner.php` | Interne Ablaufspalte nicht aus Nutzdaten übernehmen; nur ein tatsächlich gelieferter RP-Wert erneuert die Frist. |
| `cron/sync_public_endpoints.php` | RP-Bereinigung ergänzen; bestehende Sync-/Tierfunktion bleibt erhalten. |
| `tools/rp-name-retention.php` | Nur CLI; Standard Vorschau, tatsächliche Verarbeitung mit `--apply`. |
| `tools/rp-name-retention-selftest.php` | Isolierte Datenbanktests; nicht auf den Server hochgeladen. |

Website: `OttoExtra_Web/datenschutz.php`, neuer Abschnitt **6. RP-Namen in OttoExtra und der API**. Veröffentlicht und über HTTPS geprüft: [ottoextra.dev/datenschutz#rp-namen](https://ottoextra.dev/datenschutz#rp-namen).

Die Produktions-CLI verwendet die echte Backend-Konfiguration. Schon das Laden des vorhandenen Bootstraps kann die **additive Schemaänderung** durchführen. `--dry-run` verändert keine RP-Inhalte oder Zeitpunkte; es ist deshalb keine Zusage eines vollständig schreibfreien Datenbankzugriffs.

```sh
# Auf dem Backend, im Projektverzeichnis:
php tools/rp-name-retention.php --dry-run
php tools/rp-name-retention.php --apply
```

Jede Charge ist transaktional; ein Fehler rollt diese Charge zurück. Frühere erfolgreiche Chargen bleiben wirksam. Standard: höchstens 20 Chargen à 500 Datensätze pro Teilaufgabe. Das JSON-Ergebnis enthält ausschließlich Mengen/Zeitpunkt und `remaining`, keine Spielernamen oder Zugangsdaten.

## Nachweis der Umsetzung

- [x] Alle acht neuen/geänderten PHP-Dateien lokal syntaktisch geprüft; produktive Backend-Dateien zusätzlich vor Installation mit Server-PHP 8.4.22 geprüft.
- [x] **26 Prüfungen auf MariaDB 10.11.19 und 26 auf MySQL 8.4.11** bestanden: Vorschau, Altdaten, explizite Bestätigung, Löschung vor Frist, Grenzzeitpunkte, Monatsende/Schaltjahr, UTC/Session-Zeitzone, 501 Datensätze, Wiederholung und Fehler-Rollback. Bestehender External-Change-Log-Selbsttest ebenfalls bestanden.
- [x] Vorherige Live-Dateien gesichert und mit dem lokalen Ausgangszustand verglichen; nur sechs Backend-Dateien und die Datenschutzerklärung installiert. SHA-256 nach Upload zurückgelesen. Keine Mod-JAR, Modrinth-Einstellung oder sonstige Website-Seite veröffentlicht.
- [x] Live-Vorschau und Anwendung am **11.09.2026, 14:10:01 UTC**: **507** alte Zuordnungen initialisiert; **0** Namen abgelaufen, **0** Änderungs-/Anfrageprotokolle gelöscht; keine verbleibenden Chargen. Diese alten Namen laufen ohne spätere Bestätigung am **11.03.2027, 14:10:01 UTC** ab.
- [x] Eigener täglicher Cronjob beim API-Benutzer eingerichtet und zurückgelesen; vorher kein Benutzer-Crontab vorhanden. Cron-Dienst aktiv. Der nächste zeitgesteuerte Lauf liegt nach dieser Abnahme; dessen tatsächlicher Start wurde noch nicht beobachtet.
- [x] Erneuter manueller Live-Lauf mit Ausgabe ins private Cronprotokoll: alle Änderungs- und Restzähler null; Übergangsfristen nicht verlängert. Alle sieben installierten Dateien anschließend erneut per Hash bestätigt.
- [x] Öffentliche Datenschutzseite liefert HTTP 200 und enthält RP-Abschnitt, Empfänger, netcup/Deutschland, Aufbewahrung und Hinweis auf lokale Kopien.

Quellsicherung: `modpack-backups/20260911-155042-rp-retention/`. Deployment-Nachweise und Live-Zähler darunter in `deployment/`. Backend-Originale zusätzlich im privaten Serververzeichnis `rp-retention-backups/`; Website-Original lokal gesichert. Wiederherstellen alter Code-Dateien stellt keine gelöschten Daten wieder her. Es wurde kein Produktions-Datenbankdump angelegt und die neue Ablaufspalte wird beim Code-Rollback nicht automatisch entfernt.

## Verbleibender Fahrplan

- [ ] **P0 – Grundlage und Information:** Der veröffentlichte Hinweis nennt Art. 6 Abs. 1 lit. f DSGVO für das Verzeichnis. Betreiber muss die konkrete Erforderlichkeit und Interessenabwägung belegen, einschließlich Ausgabe an andere Nutzer. Die Veröffentlichung allein beweist diese Voraussetzungen nicht. Auch Spieler ohne Mod müssen erforderliche Informationen erreichen können. [EDPB: Betroffenenrechte und Information](https://www.edpb.europa.eu/sme/be-compliant/respect-individuals-rights_de)
- [x] **P0 – Upload-Zugriff:** Alter Endpunkt live mit 410 geschlossen; neuer V2-Zugang prüft Tokens und Absender, begrenzt Meldungen und behandelt fremde Angaben als private Vorschläge. Auth-Challenge-Replay-Fix ebenfalls live. [Prüfnachweis](rp-upload-sicherheit-2026-09-11.md).
- [ ] **P0 – Rechte praktisch ausführen:** Technischer RP-Prozess mit zwei Testkonten, Protokollen, Restore und echtem Import geprüft; CLI für Sperren/Korrekturen vorhanden. Betreiberverfahren für Antragsprüfung, unbekannte frühere Aliase, zusätzliche Logsysteme/Kopien und Empfängerinformation bleibt abzustimmen. Keine Zusage einer Fernauslöschung beliebiger Spielerexporte.
- [ ] **P1 – Backups:** Eigene verschlüsselte RP-Backups mit 30-Tage-Rotation und geprüftem Restore live eingerichtet. **Hosterfrage beantwortet: Laut Betreiber erstellt netcup keine Backups.** Eventuelle selbst angelegte Datenbank-/Dateisystem-Kopien sind gesondert zu berücksichtigen. Falls künftig vollständige VM-Restores eingesetzt werden, benötigen diese einen separat aktuellen Steuerzustand; die neue RP-Sicherung ersetzt dieses Verfahren nicht.
- [ ] **P1 – Betrieb:** Ersten automatischen Lauf und Fehlerüberwachung prüfen; privates `rp-retention-backups/retention-latest.log` enthält nur den letzten Lauf. netcup-Auftragsverarbeitung und Fristen für übrige Backend-Datentypen einschließlich zusätzlicher Protokolle gesondert belegen.

**Nachtrag:** Die vorsorgliche Projektsperre wurde auf Betreiberauftrag in [0.1.17-n](rp-upload-freigabe-2026-09-11.md) aufgehoben. Nutzer können Online- und RP-Upload-Freigabe einzeln oder zusammen im Assistenten erteilen; ein Update erteilt keine Zustimmung. Der API-Zugang benötigt weiterhin die Minecraft-Authentifizierung. Die Betreibergrundlage für Angaben über andere Spieler bleibt von dieser Client-Auswahl getrennt.

## Konkreter Umsetzungsplan für die Wiederfreigabe

Der folgende Abschnitt hält den ursprünglichen Plan fest. **Die technische Umsetzung ist inzwischen im [neuen Prüfbericht](rp-upload-sicherheit-2026-09-11.md) dokumentiert.** Offene Betreiberpunkte stehen im [Entscheidungsvermerk](rp-interessenabwaegung.md); die alten Plan-Checkboxen sind keine aktuelle Abnahmeübersicht. Sie ergänzen die bereits produktive Aufbewahrung. Technische Arbeiten und die Vorbereitung der Betreiberentscheidung können parallel erfolgen; eine rechtliche Abwägung wird durch bestandene Softwaretests nicht ersetzt.

### 1. P0: Upload authentifizieren und Änderungen begrenzen

- [ ] Den bisherigen `community-participant-rp-name`-Schreibzugang serverseitig schließen bzw. hinter dieselbe Authentifizierung und Berechtigung wie den neuen Zugang stellen. Keine verbleibende unauthentifizierte Rückfallroute für alte Clients. Bestehende Lesezugriffe und der geschützte Adminzugang bleiben verfügbar.
- [ ] Bestehende Mojang-Challenge-/JWT-Prüfung aus `includes/v2/auth.php` wiederverwenden; Absender-UUID ausschließlich aus dem geprüften Token beziehen. Angaben im JSON dürfen keine fremde Absenderidentität auswählen. Vor Integration den bereits lokal behobenen Challenge-Replay-Fall im Live-Auth-Code abgleichen.
- [ ] Authentifizierung vor Speicherung personenbezogener Anfrageinhalte prüfen. Größe, UUIDs, RP-Text und Rate begrenzen; keine Zugangstokens oder vollständigen Request-Bodies protokollieren. Fehlende/ungültige Anmeldung darf weder Namen ändern noch deren Frist verlängern.
- [ ] Eigene Angaben, Admin-Korrekturen und fremde Beobachtungen unterscheiden. Fremdbeobachtungen können Vorschläge liefern, dürfen aber bestätigte Zuordnungen nicht beliebig ersetzen oder fremde Namen löschen. Anmeldung belegt den Absender, nicht die Wahrheit seiner Beobachtung.
- [ ] `RpNameUploadService` an den vorhandenen authentifizierten Transport anbinden. Eigene freiwillige Upload-Freigabe beibehalten; Widerruf stoppt auch wartende Jobs und Wiederholungen. Keine automatischen Uploads alter lokaler Archive bei erstmaliger Aktivierung.
- [ ] Abnahme mit zwei Testkonten: fehlendes/gefälschtes/abgelaufenes Token, behauptete fremde Absender-UUID, unbefugtes Überschreiben/Löschen, gesperrtes Ziel, Wiederholung, Rate-Limit und Widerruf während eines Uploads. Tests zunächst ausschließlich mit synthetischen Datensätzen.

### 2. P0: Interessenabwägung und tatsächliche Spielerinformation

Für den Betreiber einen kurzen, datierten Entscheidungsvermerk vorbereiten: **Zweck → Erforderlichkeit → Abwägung → Schutzmaßnahmen → Ergebnis**. Das Ergebnis ist offen, bis die Tatsachen überprüft sind. Die drei Voraussetzungen für berechtigtes Interesse müssen im konkreten Fall erfüllt sein. [EDPB: Rechtsgrundlagen](https://www.edpb.europa.eu/sme/be-compliant/process-personal-data-lawfully_en)

- [ ] Zweck eng auf die Namenszuordnung für Ottonien und die dafür erforderliche Ausgabe beschränken. Prüfen, ob eine maßgebliche Serverquelle oder selbst bestätigte Angaben die Fremdbeobachtungen ersetzen können.
- [ ] Begründen, weshalb zentrale Speicherung und die jeweilige Reichweite des API-Zugriffs notwendig sind. Sichtbarkeit eines Namens im Spiel bedeutet nicht automatisch, dass ein dauerhaftes, außerhalb des Spiels abrufbares Verzeichnis erwartet wird.
- [ ] Risiken konkret abwägen: falsche Zuordnungen, Verknüpfung verschiedener Namen, unerwartete Weitergabe, gegebenenfalls minderjährige Spieler. Dagegen Schutzmaßnahmen und verbleibende Nachteile dokumentieren; kein vorab angekreuztes positives Ergebnis.
- [ ] Spielerinformation mit Ottonien abstimmen, auch für Spieler ohne Mod: kurzer verständlicher Hinweis an einer tatsächlich erreichten Stelle, dazu Link auf die vollständige Erklärung. Dokumentieren, wer wann wie informiert wurde, soweit dafür erforderlich. Nachrichten werden nicht ohne ausdrücklichen Auftrag versendet.
- [ ] Bei indirekter Erfassung Information grundsätzlich spätestens innerhalb eines Monats, bei früherer Weitergabe bereits dann. Eine bloß vorhandene Website ersetzt nicht automatisch die erforderliche Information; Ausnahmen sind eng. [EDPB: Information und Betroffenenrechte](https://www.edpb.europa.eu/sme/be-compliant/respect-individuals-rights_en)

Wenn sich die Voraussetzungen für automatische Fremdbeobachtungen nicht belegen lassen, bleibt als engerer Betriebsmodus die Veröffentlichung selbst bestätigter Namen mit passender Information und Rechtsgrundlage. Lokale Namenszuordnung bleibt davon getrennt.

### 3. P0/P1: Löschung, Wiederimport und Backups zusammen behandeln

- [ ] Alle tatsächlichen Datenkopien inventarisieren: Datenbankdumps, Server-/netcup-Snapshots, manuelle Downloads und weitere Ablagen. Die Dateisuche im lokalen Backend allein weist keine produktive Backuplösung nach.
- [ ] **Vorschlag für Backupkopien: 30 Tage**, geschützt und verschlüsselt, automatische Rotation mit Ablaufkontrolle. Das verkürzt nicht die vereinbarte sechsmonatige Frist im laufenden RP-Verzeichnis. Tatsächliche Hosterfristen und die notwendige Behandlung von Löschanträgen vor Übernahme prüfen; 30 Tage sind keine gesetzliche Freistellung und noch nicht konfiguriert.
- [ ] Nach überprüftem Löschantrag bzw. akzeptiertem Widerspruch Zuordnung entfernen und eine minimale, zugriffsgeschützte Sperrinformation führen. Alle Import-/Uploadpfade müssen sie beachten. Dafür nur die notwendige Identitätsreferenz und den Sperrstatus speichern, keine Sammlung gelöschter RP-Namen. Zweck und notwendige Dauer dieser ebenfalls personenbezogenen Sperrinformation gesondert festlegen.
- [ ] Wiederherstellung zunächst ohne öffentliche API-Ausgabe und ohne Imports: Schema aktualisieren, **aktuellen** Sperr-/Löschstand aus einer vom alten Datenbackup getrennten Quelle anwenden, abgelaufene Daten bereinigen, prüfen, erst dann freigeben. Alte Backups dürfen weder Löschsperren zurücksetzen noch die einmalige Altdatenfrist erneut um sechs Monate starten.
- [ ] Korrektur-/Löschsignale für künftig synchronisierte API-Zuordnungen vorsehen. Eigene Notizen und beliebige externe Exporte nicht als automatisch fernlöschbar darstellen; erforderliche Empfängerinformation im Betroffenenprozess berücksichtigen. [EDPB: Berichtigung und Betroffenenrechte](https://www.edpb.europa.eu/sme/be-compliant/respect-individuals-rights_en)
- [ ] Restore-Test: synthetischen Spieler sichern, anschließend löschen/sperren, altes Backup zurückspielen, erneut importieren und abrufen. Erwartung: keine Wiederveröffentlichung; andere Spieler bleiben korrekt erhalten. Zusätzlich Backupablauf und Fristberechnung testen.

### Freigabe

Erst serverseitige Zugangssperre/Authentifizierung, Lösch-/Restore-Test und Betreiberentscheidung einschließlich erreichbarer Information abschließen. Danach Backend aktivieren, passenden Client bauen und die automatische Upload-Option mit ausdrücklicher Nutzerfreigabe wieder anbieten. Die neue Funktion nicht durch pauschales Übernehmen alter Zustimmungswerte aktivieren. Die erste technische Aufgabe ist der alte Schreibendpunkt; die bestehende Client-Sperre schützt diesen nicht.
