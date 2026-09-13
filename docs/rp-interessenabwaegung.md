# RP-Verzeichnis: Entscheidungsvermerk für den Betreiber

Datum: **11.09.2026**. **Auf Betreiberauftrag technisch wieder freigegeben:** [0.1.17-n, individuelle Freigaben und Gesamtauswahl](rp-upload-freigabe-2026-09-11.md). Die frühere vorsorgliche Projektsperre ist aufgehoben; dies ist keine Bescheinigung einer abgeschlossenen rechtlichen Prüfung. Der folgende Arbeitsvermerk hält Betreiberangaben, Quellencheck und noch zu konkretisierende Bewertung fest. Der Datenschutzhinweis benennt berechtigtes Interesse für das Verzeichnis. Die technische Sicherheitsumsetzung steht im [Prüfbericht](rp-upload-sicherheit-2026-09-11.md).

## Rückmeldung und Bedeutung der Freigabe

Der Betreiber hat am 11.09.2026 im Projektgespräch bestätigt: **netcup erstellt keine Backups**; das Sammeln von Spielerinformationen sei erlaubt und jeder Nutzer solle die Funktion selbst freigeben können. Die Hoster-Backupfrage ist damit auf Grundlage seiner Auskunft beantwortet. Die von OttoExtra eingerichteten verschlüsselten RP-Backups mit 30-Tage-Rotation bestehen davon unabhängig weiter. Die Auskunft über netcup sagt nichts über gegebenenfalls selbst angelegte Kopien aus.

„Betreiberfreigabe“ bezeichnet hier die interne Entscheidung über den Betrieb der gemeinsamen API, keinen Antrag bei netcup oder Modrinth und keine Freischaltung einzelner Nutzer durch den Betreiber. Es wurde dafür keine besondere Anmeldemaske gebaut. Die Nachricht dokumentiert den Freigabewunsch; eine erneute pauschale Erlaubnisfrage ist nicht nötig. Die pauschale Aussage zum erlaubten Sammeln beschreibt noch nicht den konkreten Informationsweg, die Empfänger oder die zugrunde liegende Abwägung.

Davon getrennt entscheidet jeder Mod-Nutzer nach Information freiwillig über seine eigene Upload-Funktion und kann sie wieder abschalten. Dieser Schalter ist technisch bereits vorgesehen, aktuell aber durch die globale Projektsperre deaktiviert. Die Zustimmung eines Absenders ist keine Zustimmung der Spieler, deren Namen er meldet. Eine andere passende Rechtsgrundlage für fremde Angaben bleibt möglich.

Mit **Spielerinformation** ist der Datenschutzhinweis **an die betroffenen Spieler** gemeint, einschließlich Personen ohne OttoExtra, nicht die Erlaubnis zum Sammeln ihrer Informationen. Noch festzuhalten ist, wo und seit wann sie den Hinweis auf das zentrale RP-Verzeichnis, seine API-Empfänger und ihre Rechte erhalten. Das ist eine fehlende Sachinformation, keine zusätzliche technische Betreiber-Anmeldung.

Möglicher kurzer Hinweis zur Einbindung an einer tatsächlich erreichten Stelle; vor Verwendung mit dem konkreten Informationsweg abstimmen:

> OttoExtra führt ein gemeinsames Verzeichnis aus Minecraft-UUID, Accountname und RP-Name. Aktuelle Zuordnungen sind über die API auch für andere Anwendungen abrufbar. RP-Namen werden grundsätzlich sechs Kalendermonate ab ausdrücklicher Aktualisierung oder Bestätigung gespeichert; erforderliche frühere Löschungen bleiben möglich. Informationen zu Quellen, Rechtsgrundlage, Empfängern, Fristen sowie Auskunft, Berichtigung, Löschung und Widerspruch: https://ottoextra.dev/datenschutz#rp-namen

Der Hinweis wurde in diesem Arbeitsschritt weder an Spieler versendet noch auf einem Ottonien-Kanal veröffentlicht. Client und Server wurden nicht aktiviert.

### Präzisierung: Freigabe der Client-Aktionen

Der Betreiber präzisiert: Die Daten kommen vom Minecraft-Server; Spieler akzeptieren dort Bestimmungen. Die zusätzliche Mod-Auswahl soll regeln, ob **der eigene Client Abfragen ausführt und beobachtete Zuordnungen an die OttoExtra-Datenbank sendet**. Es ist keine stellvertretende Erklärung im Namen sämtlicher beobachteter Spieler und keine Einzelzulassung des Clients durch einen Administrator vorgesehen.

Die vorhandene technische Trennung passt dazu: `ONLINE_SERVICES` erlaubt die beschriebenen Online-Abfragen und Anmeldung; `COMMUNITY_UPLOADS` erlaubt zusätzlich RP-Meldungen. Beide brauchen ihre eigene aktuelle Nutzerentscheidung. Abschalten der Uploads stoppt künftige Übermittlungen und Wiederholungen, löscht aber nicht automatisch bereits übermittelte Datensätze. Authentifizierung, Eingabeprüfung, private Fremdvorschläge und Löschsperren bleiben eigenständige Schutzmaßnahmen.

Ein separates Einwilligungs-Häkchen jedes beobachteten Spielers ist **keine pauschale Voraussetzung** dieses Entwurfs: Eine passende andere Rechtsgrundlage ist möglich. Ebenso wenig wird allein aus der Datenquelle „Minecraft-Server“ oder dem Akzeptieren beliebiger Serverbestimmungen eine umfassende externe Verarbeitungsbefugnis abgeleitet. [EDPB: Rechtsgrundlagen und zweckbezogene Einwilligung](https://www.edpb.europa.eu/sme/be-compliant/process-personal-data-lawfully_en)

Öffentlich geprüft am 11.09.2026: [Ottonien-Regeln](https://ottonien.com/regeln/) und [Ottonien-Datenschutzerklärung](https://ottonien.com/datenschutz/). Darin wurde keine ausdrückliche Beschreibung des OttoExtra-RP-Verzeichnisses oder dieser Übermittlung an dessen Betreiber gefunden. Das ist eine Grenze dieses Quellenchecks, keine Feststellung eines Verbots und kein Ausschluss gesonderter Ingame-/API-Bestimmungen. Die vom Betreiber angesprochene konkrete Bestimmung liegt hier bislang nicht vor. Die globale Client-/Serversperre ist durch diese begriffliche Präzisierung noch nicht geändert.

### Veröffentlichung des präzisierten Datenschutzhinweises

Auf ausdrücklichen Betreiberauftrag am **11.09.2026, 17:43:24 UTC** wurde Abschnitt 6 der [OttoExtra-Datenschutzerklärung](https://ottoextra.dev/datenschutz#client-freigaben) erweitert und veröffentlicht: getrennte freiwillige Client-Freigaben für Online-Abfragen und RP-Uploads, Anmeldungs-/Übermittlungsdaten, Empfänger, lokale Speicherung der Entscheidung, Abwahl, Datenlöschung, private Fremdvorschläge und fehlende zusätzliche netcup-Backups. Der Text beschreibt die geplante Upload-Funktion ausdrücklich als derzeit technisch deaktiviert. Er enthält keine Einwilligungserklärung im Namen beobachteter Spieler.

PHP-Prüfung, HTML-Struktur und unveränderte Abschnitte außerhalb von Abschnitt 6 geprüft. Die veröffentlichte Quelldatei wurde per SHA-256 zurückgelesen; die gerenderte HTTPS-Seite lieferte 200 und stimmt mit der lokalen PHP-Ausgabe überein. [Änderungspatch](../tools/consent-smoke/privacy-notice-client-consent.patch). Originaldatei und Deployment-Nachweis: `modpack-backups/20260911-194058-datenschutz-clientfreigabe/`.

Die Veröffentlichung erfüllt den Arbeitsauftrag zur Ergänzung des OttoExtra-Hinweises. Sie ändert weder Ottonien-Bestimmungen noch belegt sie, dass sämtliche Betroffenen den Hinweis bereits erhalten haben. In diesem Schritt wurden keine Mod-/Backend-Funktionen aktiviert und keine neue JAR gebaut.

## Zweck und betrachtete Vorgänge

OttoExtra ordnet Minecraft-Konten ihren Ottonien-RP-Namen zu. Betrachtet werden getrennt: bestehende API-Ausgabe aktueller Zuordnungen, eigene Namensbestätigung, Erfassung fremder Beobachtungen als private Vorschläge, Prüfung/Korrektur und erforderliche Aufbewahrung/Löschsperren. Nicht vom Zweck umfasst sind Werbung, fremde Nutzungsprofile oder ein öffentliches historisches Namensarchiv.

Bestätigt: Hosting bei netcup in Deutschland; aktuelle Zuordnungen über die API abrufbar; Verwaltungsdaten zugriffsgeschützt. Die zentrale API-Ausgabe erreicht auch abrufende Anwendungen außerhalb der Mod. Minecraft-Namen und UUIDs sind personenbezogene/pseudonyme Zuordnungen, keine anonymen Statistiken.

## Erforderlichkeit und Alternativen

Eine gemeinsame Zuordnung erleichtert die konsistente Anzeige von RP-Namen. Daraus folgt noch nicht, dass jede fremde Beobachtung zentral erfasst werden muss. Zu prüfen sind eine verlässliche Ottonien-Quelle, ausschließlich selbst bestätigte Angaben und engere Empfängerkreise. Noch nicht belegt ist, welche dieser Alternativen den tatsächlichen Bedarf ausreichend erfüllt.

Für berechtigtes Interesse sind ein rechtmäßiger konkreter Zweck, notwendige Verarbeitung und die Abwägung gegenüber den Interessen/Rechten der betroffenen Personen erforderlich. Die Entscheidung muss die konkreten Umstände einschließlich vernünftiger Erwartungen berücksichtigen. [EDPB: rechtmäßige Verarbeitung](https://www.edpb.europa.eu/sme/be-compliant/process-personal-data-lawfully_en)

## Abwägung

| Gesichtspunkt | Tatsächlicher Stand / Bewertung |
|---|---|
| Nutzen | Gemeinsame, korrigierbare Namenszuordnung für die Ottonien-Funktionen. Ein rein lokales Verzeichnis ist technisch weiter möglich. |
| Erwartung | Sichtbarkeit im Spiel belegt noch keine Erwartung dauerhafter zentraler Ausgabe außerhalb des Spiels. Tatsächliche Ottonien-Hinweise sind nicht nachgewiesen. |
| Nachteile | Falsche Meldungen, Verknüpfung unterschiedlicher Namen, unerwartete Abrufe und fortbestehende lokale Kopien. Mögliche minderjährige Nutzer bei der Bewertung berücksichtigen. |
| Begrenzung | Verifizierter Absender; fremde Meldungen nur privat zur Prüfung; keine beliebige Überschreibung bestätigter Namen; begrenzte Eingaben, Übermittlungsrate und Speicherdauer. |
| Kontrolle | Auskunfts-/Berichtigungs-/Löschkontakt vorhanden. Technische Sperre verhindert Wiederimport und berücksichtigt aktuelle Löschungen bei verwalteten Restores. Antragsprüfung und erforderliche Empfängerinformation organisatorisch durchführen. |
| Restfragen | Reichweite der tatsächlichen Spielerinformation, ausreichend wirksame mildere Alternativen, weitere Backup-/Logkopien und praktische Bearbeitung von Anträgen. |

Die technischen Maßnahmen verringern Risiken. **Sie belegen für sich allein kein überwiegendes Interesse.** Für den bisherigen öffentlich abrufbaren Bestand ist die Bewertung ebenfalls erforderlich; es geht nicht ausschließlich um neue Uploads.

## Information und Entscheidung

Der vorhandene Website-Hinweis beschreibt Daten, Empfänger, Fristen und Kontakt. Noch zu belegen ist ein tatsächlich erreichender Informationsweg für Spieler ohne OttoExtra, etwa ein mit Ottonien abgestimmter Einstiegshinweis. Bei indirekter Erhebung gilt grundsätzlich spätestens ein Monat, bei früherer Weitergabe bereits deren Zeitpunkt. Eine Ausnahme wegen unverhältnismäßigen Aufwands darf nicht einfach unterstellt werden. [EDPB: Information und Betroffenenrechte](https://www.edpb.europa.eu/sme/be-compliant/respect-individuals-rights_en)

Für die Betriebsdokumentation weiterhin konkret festhalten:

- [ ] Betreiber bestätigt Zweck und den notwendigen Umfang einschließlich API-Empfänger.
- [ ] Geprüfte Alternativen und konkrete Gründe dokumentiert.
- [ ] Ottonien-Informationsweg, Inhalt, Einführungstermin und verantwortliche Person festgelegt; erforderliche Information tatsächlich erfolgt.
- [ ] Verfahren für verifizierte Auskunft/Korrektur/Löschung/Widerspruch einschließlich zusätzlicher Kopien umgesetzt; Speicherfristen und Notwendigkeit der Sperrliste überprüft.
- [ ] Ergebnis mit Datum und Begründung vom Betreiber festgehalten: Freigabe im beschriebenen Umfang oder engerer Betrieb. Falls diese Grundlage nicht trägt, die Verarbeitung entsprechend einschränken und eine passende Grundlage für selbst bestätigte Angaben festlegen.

**Betriebsentscheidung:** Der Betreiber hat die individuelle technische Aktivierung und eine gemeinsame Freigabeoption im Assistenten beauftragt. Die Umsetzung ist in 0.1.17-n verfügbar; nichts wird allein durch das Update für den einzelnen Nutzer eingeschaltet. Die tatsächliche Reichweite der Spielerinformation und die konkrete Abwägung werden durch diese Softwareentscheidung nicht nachgewiesen. Die Nutzerfreigabe steuert die Aktion des eigenen Clients; sie bleibt getrennt von der Grundlage für Angaben über andere Spieler.
