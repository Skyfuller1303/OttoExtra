# P0: Clientschutz umgesetzt, Release-Nachweise offen

Aktuelle technische Nachprüfung: [Client, Datenbank und Server](privacy-qa-2026-09-11.md). Englische UI, Bild-/Pack-Laufzeitfälle und Low-Health-Weltfälle inzwischen geprüft; frühere Testergebnisse unten sind historisch.

Stand: **11.09.2026**. Umsetzung zu [P0 im Fahrplan](release-datenschutz-fahrplan.md). Änderungen am Client sind vorhanden; **P0 insgesamt bleibt offen**. Insbesondere sind Backend-Berechtigungen, tatsächliche Datenverarbeitung und Fremdasset-Rechte noch nicht freigegeben. Keine Veröffentlichung vorgenommen.

**Fortsetzung:** Dieses Protokoll hält den P0-Prüfstand fest. Neuere Clientänderungen und der aktuelle Kandidaten-Hash stehen im [P1-Protokoll](p1-datenschutz-umsetzung.md); die unten genannte P0-JAR wurde durch spätere lokale Builds ersetzt.

## Was sich jetzt ändert

| Bereich | Verhalten / technische Stelle |
|---|---|
| Neue und alte Installationen | Neue Zwecke bleiben aus. Alte reine `true`-Flags ohne passende Policy/Quelle werden beim Laden verworfen. Der Assistent fragt mit Version 3 neu. `OttoExtraConfig`, `WelcomeScreenManager`. |
| Online-Dienste | Eigene ausdrückliche Wahl für API-Anmeldung, Regionsdaten, Detailabfragen, Tier-/Finder-Requests und Kontofunktionen. Ohne Freigabe kein OttoExtra-Auth-Handshake; keine automatische Regions-/Finder-/Kontostatus-Abfrage. `HttpOttoExtraApiClient`, `ApiAuthService`, `RegionDataService`, `HorseRegistryModule`, `AccountLinkModule`. |
| Widerruf | `ConsentPermit` bindet einen Vorgang an eine Berechtigungsgeneration. Widerruf bricht registrierte Futures/Streams ab und verhindert spätere Übernahmen. Erneutes Erlauben macht alte Vorgänge nicht wieder gültig. Bereits übertragene Daten sind nicht rückholbar. |
| API-Token | Token und zugehörige Freigabe werden gemeinsam gehalten; Widerruf entfernt den lokalen Token-Cache. Auch ein abgebrochener Auth-Vorgang darf später keinen Token nachliefern. Ungefilterte Authlib-Fehlermeldungen werden nicht mehr in Logs übernommen. |
| Skin/Banner | Chatköpfe, Briefempfänger und Personen-Vorschauen nutzen den kontrollierten Skin-Cache statt impliziter Downloads über Minecrafts SkinProvider. Ohne gecachtes Bild erscheint zunächst ein Standardskin; Vorschauen übernehmen keine zusätzlichen Capes aus dem SkinProvider. Freigabe vor HTTP und vor Cache-Schreibzugriff; heruntergeladene Banner werden auch vor späterer Texturregistrierung geprüft. Vorhandene lokale Bilder bleiben nutzbar. |
| Resourcepack | Freigabe umfasst Manifest/ZIP, Installation und vorgemerkte Aktivierung. Eine widerrufene Aktivierung wird im späteren Tick verworfen. Download-/Reload-/Aktivierungsentscheidungen bleiben getrennt. Vorhandene Dateien werden beim Widerruf nicht gelöscht. |
| Versionsprüfung | Eigene Freigabe; späte Ergebnisse eines widerrufenen Checks werden verworfen. Weiterhin keine automatische Mod-JAR-Installation. |
| RP-Beobachtungsuploads | **Im Build gesperrt**, auch bei alten Config-Flags. `ConsentPolicy.COMMUNITY_UPLOADS_AVAILABLE = false`; Sperre zusätzlich am tatsächlichen Sender. Queue-Regressionstests bleiben erhalten. |
| Einstellungen | Einschalten öffnet einen Informationsdialog; erst „Erlauben“ erteilt die Freigabe. Ausschalten widerruft sofort. Assistent und Einstellungen verwenden dieselben DE-/EN-Informationen und zeigen die konfigurierte Quelle. |
| Backup/Abbrechen | Wiederherstellen übernimmt Daten/Einstellungen, überschreibt aber keine aktuellen Zustimmungsentscheidungen mit alten Backup-Werten. Auf Disk wird die zusammengeführte Config gespeichert. Ein allgemeines „Abbrechen“ der Einstellungen nimmt zuvor ausdrücklich erteilte oder widerrufene Freigaben nicht zurück. |

## Quellenbindung und Daten

- `ConsentPolicy.VERSION = 3`; pro Zweck lokale Entscheidung, Zeitstempel und freigegebener Umfang. Keine Chattexte, Spielernamen oder Tokens als Zustimmungsnachweis.
- API-Quelle entspricht der normalisierten Adresse aus `OttoExtraApiRoutes`. Nach Anbieterwechsel sind neue Entscheidung **und Minecraft-Neustart** erforderlich; der alte Client darf die neue Freigabe nicht für seinen alten Host nutzen.
- Resourcepack-Freigabe bindet Quelle, Assetname, Größenlimit und automatische Aktivierung. Änderungen verlangen eine neue Entscheidung. Dieselbe Quelle darf nach Freigabe zukünftige Updates liefern.
- Skins: ausschließlich `textures.minecraft.net/texture/...`; alte HTTP-Texturverweise werden vor Kontakt auf HTTPS umgestellt. Beliebige Profil-Hosts werden abgelehnt.
- Banner: nur Ursprung der freigegebenen OttoExtra-API. Auth-, API-, Banner-, Manifest- und Versionsrequests folgen keinen automatischen Redirects.
- ZIP-Redirects werden **vor** dem nächsten Kontakt geprüft: gleicher HTTPS-Ursprung oder bei GitHub-Quellen die im Dialog benannten GitHub-/Release-CDN-Hosts. Eigene Manifeste können nicht unbemerkt einen anderen Downloadanbieter bestimmen.
- Explizite HTTP-Loopback-Adressen bleiben für lokale API-Tests möglich. Öffentliche HTTP-API-Ziele werden nicht akzeptiert.
- Kontoverknüpfung nutzt den benannten Ursprung `https://ottoextra.dev`; ein frei geänderter Konto-Endpunkt wird abgelehnt. `/ottoextra verify unlink` bleibt als gezielt ausgelöste Beendigung der Verknüpfung auch nach Widerruf der Online-Freigabe verfügbar. Ein vorhandenes Device-Token bleibt hierfür lokal gespeichert; Statusabfragen ruhen. Das ist getrennt vom kurzlebigen API-Token-Cache.

## Prüfung

- **772 JUnit-Tests bestanden, keine Fehler oder übersprungenen Tests; Build erfolgreich.** Befehl: `./gradlew.bat build -x dist --console=plain`. Ergebnisse: `build/reports/tests/test/index.html`, `build/p0-validation.log`.
- Regressionen: keine HTTP-/Session-Abfrage ohne Freigabe; Widerruf während Auth; kein alter Token nach erneutem Erlauben; Quelle gewechselt; verzögerte, nicht abbrechbare Regionsantwort; Cache-Commit nach Widerruf; Stream-Schließen und Teil-Datei-Löschung; alte Pack-Aktivierung; Backup mit gültiger alter Freigabe; Redirect-Ziel bleibt unkontaktiert.
- **Echter Minecraft-Client-Smoke-Test bestanden:** `./gradlew.bat -I tools/consent-smoke/client.init.gradle runClientGameTest -x dist --console=plain`.
- Isoliertes Profil `build/consent-smoke/client`, Testkonto `ConsentAudit`, DevAuth aus, kein Server-Join. Testet Migration alter Flags, alle Ablehnungen, gesperrten RP-Upload-Button sowie späteres Erlauben/Widerrufen im Informationsdialog. Ein gespeichertes Profil mit nicht freigegebener Bildquelle liefert bei Ablehnung weiterhin einen Standardkopf. Der Test deaktiviert Pack-Startprüfungen, bevor er die Pack-Freigabe testweise erteilt.
- DE-Dialoge mit echten Mausklicks und Screenshots bei 1280 × 720 geprüft. Screenshots unter `build/consent-smoke/client/screenshots/`; alle Texte scrollbar, Entscheidungsbuttons erreichbar. EN-Dialoge inzwischen visuell im echten Client geprüft; siehe aktuellen QA-Bericht.
- Lokales Build-Artefakt: `build/libs/ottoextra-0.1.17-l.jar`, SHA-256 `c7ba760d350b147180a4e373f7e8b24734e5e7018d6fd17f2cd6006498b7766d`. Client-Metadaten geprüft; keine Consent-/Courier-Testtreiber, DevAuth- oder offensichtlichen Credential-Dateien im JAR. Dies ist keine vollständige Secret-Prüfung aller Inhalte und keine Veröffentlichungsfreigabe.
- `dist` bewusst ausgeschlossen: Der vorhandene Task löscht ältere Distributions-JARs. Testtreiber bleiben außerhalb des Release-JARs.

## Noch offen vor Freigabe

- [ ] **P0-01/02 Laufzeit-Abnahme:** lokaler Weltbeitritt und Reconnect unter simuliertem Ottonien-Kontext mit verworfenen Freigaben geprüft: null Aufrufe des OttoExtra-HTTP-Transports. Öffentlicher Server, Paketmitschnitt und Trennung sämtlichen Vanilla-/Fremdmod-Verkehrs bleiben nicht abgenommen. [Testgrenzen](privacy-qa-2026-09-11.md).
- [x] **P0-02 Grafik-/Pack-Abnahme:** echte verzögerte Skin-/Bannerantworten, GUI-Wechsel, Widerruf/erneute Freigabe und tatsächlicher Ressourcenreload samt geladenem ZIP-Marker geprüft. Siehe [QA-Bericht](privacy-qa-2026-09-11.md).
- [ ] **P0-04 Betreiber:** Verantwortlicher, Rechtsgrundlage, Empfänger, reale Logs, Aufbewahrung, Auskunft/Berichtigung/Löschung und Backup-Verhalten für die Mod-API bestätigen. Gefundene Website-Datenschutzhinweise allein belegen das Verhalten sämtlicher Mod-Endpunkte nicht.
- [ ] **P0-04 RP-Daten:** Rechtsgrundlage für beobachtete andere Spieler gesondert klären. Bis dahin die Build-Sperre erhalten. Einwilligung des hochladenden Spielers genügt hierfür nicht allein.
- [ ] **P0-05 Backend:** isolierte SQL-Tests für Auth, private Chatfreigaben sowie Tier-/Hof-/Finder-/Sharing-Funktionen bestanden. Parallelen Challenge-Replay lokal korrigiert. HTTP-Router, Live-Rollenkonfiguration und Deployment bleiben separat abzunehmen; [Belege](privacy-qa-2026-09-11.md).
- [ ] **P0-05 Support-/Release-Daten:** reale Log-/Support-Exportpfade auf Tokens, Cookies und private Schlüssel prüfen. Die neue Auth-Fehlerbehandlung ersetzt keine vollständige Backend-/Support-Prüfung.
- [ ] **P0-06 Rechte:** Karten, Icon und übernommene Fremdbestandteile samt Genehmigungen/Attribution dokumentieren oder aus der Veröffentlichung entfernen.

Nächste Arbeit: diese P0-Nachweise und Laufzeitfälle abschließen. Danach P1: Empfangslimits, bewusst aktiviertes Chatlog, lokale Aufbewahrung/Löschen und weitere Punkte des Fahrplans. Modrinth-Einreichung und KI-Zulässigkeit bleiben eine separate Prüfung.
