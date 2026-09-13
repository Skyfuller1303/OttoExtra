# OttoExtra: priorisierter Release- und Datenschutz-Fahrplan

> Aktualisierung 13.09.2026: Gezielte Finder-Umrisse sind wieder aktiv (Suchziel, unterstützte Tierart und aktive Verbindung erforderlich). AnimalOutlinePolicy sperrt weiterhin automatische Geschlechtsumrisse. Die folgenden Aussagen zur vollständigen Finder-Sperre beschreiben den früheren P1-Stand; eine neue Live-/Release-Abnahme wurde hier nicht durchgeführt.

Aktuelle technische Abnahme: [Prüfbericht vom 11.09.2026](privacy-qa-2026-09-11.md). Die lokalen Runtime-/SQL-Tests schließen viele ursprüngliche Prüfaufträge. [RP-Nachtrag](rp-verzeichnis-datenschutz.md): Betreiber/Hosting präzisiert, Datenschutzhinweis und sechsmonatige RP-Bereinigung gezielt live umgesetzt; übriger Live-Betrieb und Verbreitungsrechte bleiben offen.

Stand: **11.09.2026**. Gilt auch für Verteilung über GitHub, Discord, eigene Website und private Testgruppen. Grundlage: statische Codeprüfung und [Modrinth-Bestandsaufnahme](modrinth-submission-checklist.md). **P0-Clientschutz und P1-Clientänderungen implementiert und lokal getestet; beide Gesamtprioritäten bleiben offen.** [P0-Protokoll](p0-umsetzung.md) · [P1-Protokoll](p1-datenschutz-umsetzung.md). Backend-/Rechtenachweise und weitergehende Laufzeit-Abnahme fehlen; Prüfaufträge sind kein Beweis eines Backend-Fehlers.

**Aktuell: [0.1.17-n erlaubt RP-Uploads nach eigener Zustimmung und bietet eine Gesamtauswahl im Assistenten](rp-upload-freigabe-2026-09-11.md). Backend-Schutz ist live, Standard auf jedem Client bleibt aus.** Betreiber bestätigt: netcup erstellt keine Backups. Die weitere Betriebsdokumentation zur Spielerinformation, Interessenabwägung, gegebenenfalls zusätzlichen eigenen Kopien und Verbreitungsrechten bleibt getrennt von der technischen Freigabe. [Entscheidungsvermerk](rp-interessenabwaegung.md). Modrinth ist kein aktuelles Veröffentlichungsziel.

`[x]` bedeutet nachfolgend implementierter Client-Anteil mit den genannten lokalen Tests; es bedeutet keine vollständige Release-/Backend-Abnahme.

## Prioritäten und Freigaben

| Priorität | Bedeutung | Zeitpunkt |
|---|---|---|
| **P0** | Release-Blocker für betroffene Funktion: fehlende Kontrolle über Transfers, unklare Datenverarbeitung oder Verbreitungsrechte | Vor Weitergabe dieser Funktion beheben oder zuverlässig deaktivieren/aus Release entfernen. |
| **P1** | Wichtiger nächster Fix: Datensparsamkeit, Aufbewahrung, Robustheit, Nutzerkontrolle | Nach P0; vor breiter/stabiler Verteilung abschließen. Begrenzter P0-Hotfix möglich. |
| **P2** | Zusätzliche Modrinth-Voraussetzungen | Nur bei Modrinth-Plan bearbeiten; dort vor Einreichung verbindliche Punkte abschließen. |
| **P3** | Präsentation/Komfort | Nach funktionaler und datenschutzbezogener Abnahme. |

Prioritäten sind eine **Projektentscheidung nach Risiko**, keine gesetzliche Einstufung. Testen eigener isolierter Builds bleibt möglich. Ein anderer Downloadkanal beseitigt weder technische Risiken noch Rechte-/Datenschutzfragen. Vollständige KI-Erstellung ist ohne Modrinth dagegen kein durch dessen Plattformregeln verursachter Release-Blocker.

## P0 – zuerst, unabhängig von Modrinth

### P0-01: Online-Modus und automatische API-Anmeldung kontrollieren

- [x] **Client:** explizite Auswahl „OttoExtra-Onlinedienste“ vor erstem Request. Ablehnen muss lokale Funktionen erhalten; Online-Funktionen dürfen dann fehlen und erklären ihren Bedarf.
- [x] **Alle Einstiegspunkte:** Regions-Bootstrap/-Sync/-Aktivität, Tierregister-/Finder-Polling, Detailabfragen, API-Auth und Retries an dieselbe Freigabe binden. `communityRpNameUploads` regelt nur RP-Beobachtungen, nicht den gesamten API-Verkehr.
- [x] **Abwahl:** neue Requests/Authentifizierung stoppen, wartende Jobs ungültig machen, Polling beenden und API-Token lokal verwerfen. Gezielte Lösch-/Widerrufsaktion zum Backend darf nach eigener Bestätigung weiterhin möglich sein.

**Ausgangsbefund / betroffene Stellen (vor Umsetzung):** [OttoExtraClient.registerLifecycle](../src/client/java/de/ottoextra/OttoExtraClient.java), [RegionDataService.onServerJoin](../src/client/java/de/ottoextra/regions/RegionDataService.java), [HttpOttoExtraApiClient.getString](../src/client/java/de/ottoextra/api/HttpOttoExtraApiClient.java), [ApiAuthService.handshakeOnce](../src/client/java/de/ottoextra/api/auth/ApiAuthService.java), [HorseRegistryModule](../src/client/java/de/ottoextra/horses/HorseRegistryModule.java), [HorseFinder](../src/client/java/de/ottoextra/horses/HorseFinder.java). Join kann Name/UUID an API-Auth senden, ohne RP-Upload-Zustimmung.

**Fertig wenn:** Frische Config → alles ablehnen → Ottonien beitreten → kein OttoExtra-API-Auth-/Polling-Verkehr. Vanilla-, Server-Resourcepack- und Fremdmod-Verkehr separat messen. Ein eigener Online-Schalter ist hier das vorgeschlagene Produktmodell; nicht jede HTTP-Anfrage erfordert rechtlich Einwilligung.

### P0-02: Widerruf bis Download, Speichern und Aktivierung durchsetzen

- [x] **Client/Transport:** Freigabe vor Einreihen, vor HTTP-Start und vor Cache-/Installationsübernahme prüfen. Jobs mit Berechtigungsgeneration versehen; erlauben → widerrufen → erneut erlauben darf alte Jobs nicht reaktivieren.
- [x] **Skin/Banner:** laufende Futures/Streams soweit möglich abbrechen; nach Widerruf keine weiteren Cache-Dateien oder Texturregistrierungen übernehmen.
- [x] **Resourcepack:** bestehende Lese-/Installationsprüfungen erhalten; zusätzlich vorgemerkte Aktivierung im späteren Client-Tick prüfen/entfernen. Ein Reload bereits installierter Daten ist getrennt von einer neu angeforderten Aktivierung zu behandeln.
- [x] **RP-Upload:** vorhandene generationsgebundene Queue als Muster erhalten; prüfen, dass alle neuen Uploadpfade sie verwenden. Threadübergreifende Sichtbarkeit der Freigabe sicherstellen.

**Ausgangsbefund / betroffene Stellen (vor Umsetzung):** [SkinCache.ensurePng](../src/client/java/de/ottoextra/chat/SkinCache.java), [BannerTextureService.startDownload/maybeRefresh](../src/client/java/de/ottoextra/regions/BannerTextureService.java), [PackDownloadService](../src/client/java/de/ottoextra/resourcepack/PackDownloadService.java), [ResourcePackUpdater](../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java), [PackInstaller.tick](../src/client/java/de/ottoextra/resourcepack/PackInstaller.java), [RpNameUploadService](../src/client/java/de/ottoextra/rpnames/upload/RpNameUploadService.java). `PackInstaller.tick` verbraucht vorgemerkte Aktivierung ohne neue Consent-Prüfung.

**Fertig wenn:** gezielt verzögerte Requests/Callbacks und Ressourcenreload testen; Widerruf verhindert neue Starts und spätere Übernahmen. Bereits übertragene Bytes sind nicht rückholbar. Bestehende Nutzerdateien nicht ungefragt löschen.

### P0-03: Einwilligung an Zweck, Quelle und aktuelle Entscheidung binden

- [x] **Config/UI:** aktuelle Quelle, Empfänger, Daten/Dateien, Zielordner, Zweck und Folgeaktionen zeigen; Datenschutz-Schalter brauchen dieselbe Information wie Erststart. Widerruf direkt dort ermöglichen, nicht nur per JSON-Anleitung.
- [x] **Migration/Restore:** alte Config oder Backup darf aktuellen Widerruf nicht rückgängig machen. Fehlende/neue Zwecke bleiben aus; geänderte Anbieter/Zwecke erfordern neue Entscheidung. Harmloser UI-Textfix allein muss keine neue Abfrage auslösen.
- [x] **Startreihenfolge:** Freigaben/Migration auswerten, bevor `SkinCache.load` oder `ResourcePackModule` Netzwerkjobs starten können.
- [x] **Lokaler Nachweis:** Zweck-/Policy-Version, Entscheidung, Quelle und Zeitpunkt sparsam gespeichert; keine Chattexte/Tokens.
- [ ] **Betreibernachweis:** bei personenbezogener Serververarbeitung erforderlichen Nachweis festlegen. Eine lokale Config-Datei allein belegt keine rechtssichere Einwilligung für beliebige Backend-Verarbeitung.

**Ausgangsbefund / betroffene Stellen (vor Umsetzung):** [OttoExtraConfig.Consent/restoreFrom](../src/client/java/de/ottoextra/config/OttoExtraConfig.java), [OttoExtraBackupService.restoreBackup](../src/client/java/de/ottoextra/config/OttoExtraBackupService.java), [WelcomeScreenManager](../src/client/java/de/ottoextra/welcome/WelcomeScreenManager.java), [SettingsRegistry](../src/client/java/de/ottoextra/config/settings/SettingsRegistry.java), [DE-Texte](../src/main/resources/assets/ottoextra/lang/de_de.json), [EN-Texte](../src/main/resources/assets/ottoextra/lang/en_us.json). `restoreFrom` übernimmt derzeit auch gespeicherte Consent-Werte; Pack-Hinweis nennt feste Quelle/64 MB trotz veränderbarer Config.

**Fertig wenn:** Erststart, Upgrade, altes Backup, Anbieterwechsel, defekte Config und erneute Aktivierung behalten die aktuelle freiwillige Entscheidung bei. Einwilligung/Abwahl funktionieren auch ohne erreichbares Backend.

### P0-04: Fremde Spielernamen und Backend-Daten rechtlich/organisatorisch klären

- [ ] **Betreiber:** Datenflussliste aus nächstem Abschnitt vollständig ausfüllen: Verantwortlicher/Kontakt, Zweck, Rechtsgrundlage, Daten, Empfänger/Hoster, Zugriff, Aufbewahrung, Löschung und ggf. Drittlandtransfer. Tatsächliche Logs/Backups einbeziehen.
- [ ] **RP-Verzeichnis:** Rechtsgrundlage für Daten beobachteter Spieler gesondert bestimmen. Zustimmung des hochladenden Spielers ersetzt keine Rechtsgrundlage für Verarbeitung der Daten anderer Personen. Öffentlich sichtbare RP-Namen/UUIDs nicht automatisch als anonym einstufen.
- [ ] **Vor Aktivierung:** erforderliche Informationen und praktikablen Kontakt für Auskunft/Berichtigung/Löschung bereitstellen. Backend-Löschprozess einschließlich Backups/Restore festlegen; anfangs darf ein nachweislich funktionierender manueller Prozess genügen.
- [x] **RP-Sperre:** Beobachtungsuploads sind im Build deaktiviert und durch alte Flags nicht aktivierbar.
- [ ] **Release-Freigabe:** Andere Online-Funktionen nur veröffentlichen, soweit deren eigene Verarbeitung geklärt ist.

**Fertig wenn:** Betreiber hat reale Verarbeitung bestätigt; der Veröffentlichungs-/Ingame-Text entspricht ihr. „Kein Modrinth“ oder „Opt-in vorhanden“ gilt nicht als Datenschutz-Nachweis.

### P0-05: Empfänger und Zugriff vor Übertragung absichern

- [x] **Client:** URLs vor Kontakt geprüft; freigegebene HTTPS-Ziele und erforderliche CDN-Redirects explizit behandeln. Keine beliebigen Zielhosts aus Skin-Profil, Manifest oder Redirect kontaktieren. Bei Auth-/Upload-Requests keine Tokens/Personendaten an fremde Redirect-Ziele weiterreichen.
- [ ] **Backend-Prüfung:** Authentifizierung plus Berechtigung für Tier-/Hof-/Finder-/Chat-Datensätze nachweisen. Zwei Testkonten: fremde IDs, Listen und Rollenänderungen dürfen keine unbefugten Zugriffe ermöglichen. Bis Prüfung keine Freigabe betroffener Funktionen; derzeit kein bestätigter Berechtigungsfehler.
- [ ] **Secret-Schutz:** API-/Minecraft-Token, Cookies und private Schlüssel aus Logs, Exceptions, Support-Dateien und Release-JAR ausschließen. Öffentliche Verifikationsschlüssel sind keine Secrets. Minecraft-Token wird im geprüften Pfad für Mojangs Sessionprüfung verwendet, nicht als OttoExtra-JSON-Feld gesendet.

**Wo:** [HttpOttoExtraApiClient](../src/client/java/de/ottoextra/api/HttpOttoExtraApiClient.java), `ApiAuthService`, `RpNameUploadService`, `SkinCache`, [PackManifestClient](../src/client/java/de/ottoextra/resourcepack/PackManifestClient.java), `ResourcePackUpdater`; zugehöriges Backend separat. Ausgangsbefund war eine Banner-Prüfung erst nach dem Download. Jetzt werden automatische Redirects blockiert und ZIP-Folgeziele vor Kontakt geprüft; Details im Umsetzungsprotokoll.

**Fertig wenn:** unerlaubtes Ziel wird vor Verbindung blockiert; kein Cross-Host-Credential-Transfer; Berechtigungs-Negativtests dokumentiert.

### P0-06: Rechte vor jeder Weitergabe belegen

- [ ] **Autor:** Genehmigungen/Lizenzen für Karten, Icon und tatsächlich übernommene Fremdbestandteile dokumentieren. MIT-Datei des Projekts deckt fremde Rechte nicht automatisch ab.
- [ ] **Prüfen:** [OttoPlus-Analyse](ottoplus-brief-formatierung.md) belegt Untersuchung dekompilierten Codes, nicht automatisch dessen Übernahme. Herkunft klären; unbelegte Bestandteile ersetzen/aus Release nehmen.

**Fertig wenn:** Rechte für den gewählten Verbreitungsweg und notwendige Attribution feststehen. KI-Anteil allein ist außerhalb Modrinths kein Ersatz für diese Prüfung und kein pauschales Verbot.

## P1 – direkt danach, vor breiter/stabiler Verteilung

| ID / Aufgabe | Wo / konkrete Änderung | Fertig wenn |
|---|---|---|
| [x] **P1-01: Größen-/Zeitlimits** | `BoundedBody`, `ConsentHttp`, `PngLimits`; Skin/Banner, Auth/API, Manifest, Aktivität und RP-Requests. Empfangsbytes, vollständiger Body-Timeout und PNG-Abmessungen begrenzt. | Lokal geprüft: exaktes Limit, Content-Length-/Chunked-Übergröße, langsame JSON-/Streaming-Body-Antwort, ungültiger PNG-Header. |
| [x] **P1-02: Chatlog bewusst aktivieren** | `LocalPrivacy`, `LocalChatConsentScreen`, `ChatMessageLog`, Config/Settings. Ungeklärt aus; eigene bestätigte Scope-Wahl (Ottonien/alle). | UI-Freigabe/Widerruf und Restore-/Scope-Regression bestanden; ausgeblendete Antworten und Vanilla-Logs erklärt. |
| [x] **P1-03: Lokale Aufbewahrung/Löschen** | [Offline-Datenverwaltung](lokale-daten.md), `LocalDataSession`, `CacheBudget`, Chatlog. Vorschau, Kategorien, ZIP mit Hashprüfung, expliziter Löschplan; Budgets und Altersauswahl. | Tests mit isolierten Daten bestanden; laufender Client blockiert Offline-Zugriff. Persönliche Archive/Backups getrennt. Ingame-Komfort bleibt P3. |
| [ ] **P1-04: Backend-Prozesse automatisieren** | Backend in `OttoRegions Web`: Auth-/Tierbereinigung vorhanden. [RP-Zuordnungen und RP-Protokolle](rp-verzeichnis-datenschutz.md) nun sechs Kalendermonate, isoliert geprüft und live mit täglichem Cron eingerichtet. | Fristen für übrige Daten, Backups/Restore, Betroffenenprozess und P0-04-Abnahme bleiben offen; erster automatischer RP-Lauf noch zu beobachten. |
| [x] **P1-05: Pack-Auswahl respektieren** | `PackInstaller.decide` respektiert vorhandene Minecraft-Deaktivierung immer; Altwerte können sie nicht überschreiben. Irreführender Schalter entfernt. | Erstinstallation, deaktiviertes Update und aktives Pack mit/ohne Auto-Aktivierung per Regression geprüft. Reaktivierung bewusst in Minecrafts Ressourcenpaket-Menü. |
| [x] **P1-06: Vote-Aktion klar ankündigen** | `VoteConsentScreen`, `VoteReminderManager`, `VoteBrowserOpener`: tatsächliche Ziele, Seitenanzahl, Clipboard-Inhalt. Enter nur zum Informationsdialog. | Clienttest: Enter/Abbrechen ohne Effekt; bestätigte Aktion einmalig. GUI-/Overlay-Eingaben bis Loslassen unterdrückt, Abschaltung erreichbar. |
| [x] **P1-07: Low-Health reduzieren/abschalten** | Reduzierter Modus, Hauptschalter und Maximalvorschau im echten Client geprüft. | Bewegung, zusätzlicher Schaden, acht Blur-Pässe, FOV bis 25°, Vignette und SoundManager-Aufrufe; [Belege](privacy-qa-2026-09-11.md). |
| [ ] **P1-08: Tier-Glow/Serverkontrolle** | Risiko eingegrenzt: `AnimalOutlinePolicy` sperrt sämtliche zusätzlichen Tier-Umrisse; Finder-Listen bleiben. UI/F8/Altconfig können Sperre nicht umgehen. | Vor Reaktivierung echte Servererlaubnis/-kontrolle und Wandtest erforderlich. Hostname bleibt kein Freigabebeleg. |
| [ ] **P1-09: Release konsistent prüfen** | Build, 783 Core-/52 Add-on-Java-Tests, 7 Python-Tests, EN-UI, Minimal-/Xaero-Client, repariertes Modpack einschließlich AnimalBreeding und Dedicated Server geprüft. | Bestands-Modpack auf AnimalBreeding 0.5.16, Java 25 und zusammenpassende Xaero-Versionen umgestellt; [Reparaturprotokoll](modpack-reparatur-2026-09-11.md). Finaler Produktionslauncher-Lauf, Fremdmod-Warnungen und Betreiberfreigaben offen; [QA-Grenzen](privacy-qa-2026-09-11.md). |

## Daten- und Zustimmungsmodell als Umsetzungsziel

Dies beschreibt das **Zielmodell**; den tatsächlich umgesetzten Anteil und offene Nachweise dokumentiert [P0-Umsetzung](p0-umsetzung.md). Benutzerfreigabe für eine optionale Funktion und datenschutzrechtliche Rechtsgrundlage sind getrennt zu dokumentieren.

| Funktion | Freigabe / Default | Daten und Empfänger | Widerruf / Deaktivierung |
|---|---|---|---|
| Lokale RP-/Brief-/GUI-Funktionen | Ohne Online-Erlaubnis nutzbar | Lokale Daten; Spielbefehle nach eigener Aktion | Lokale Speicherung/Archive getrennt steuern. |
| OttoExtra-Onlinedienste | Neue ausdrückliche Wahl, aus | Name/UUID bei Auth, gewählte API-Abfragen; `api.ottoextra.dev`, Regionsaktivität auch `mcp-api.ottonien.com`; Mojang-Sessionprüfung | Auth, Polling, Sync, neue Aufträge stoppen; Tokens verwerfen; bestehende Cache-Nutzung erklären. |
| Resourcepack-Download/-Update | Eigene Wahl, aus | GitHub/erforderliches CDN; ZIP in `resourcepacks/Ottonien.zip` | Transfers/Installationsjobs stoppen; Aktivierung separat, vorhandene Datei bleibt. |
| Externe Bilder | Eigene Wahl, aus; Quellen getrennt benennen | Skinanbieter und OttoExtra-Banner; PNGs im lokalen Cache | Neue Downloads und alte Jobübernahmen stoppen; Cache-Löschung separat. |
| Versionscheck | Eigene Wahl, aus | GitHub-API; IP auf Transportebene, Modversion im User-Agent | Checks stoppen; keine Mod-JAR-Automatik. |
| Community-RP-Upload | Eigene Wahl, aus; erst nach P0-04 | Eigene und beobachtete Account-/RP-Daten → OttoExtra-API | Queue stoppen; Verfahren für bereits gespeicherte Daten erklären. |
| Tier-/Hof-/Finder-Speichern | Online-Freigabe plus erklärte Speichern-Aktion | Tier-UUID, Auswahl, Namen, ggf. Koordinaten/Beobachtungen/Besitzbezug | Künftige Übermittlung stoppen; Datensatzlöschung und gemeinsame Bearbeitungsrechte separat. |
| Konto verknüpfen/anzeigen | Erklärte Benutzeraktion | [AccountLinkApi](../src/client/java/de/ottoextra/verify/AccountLinkApi.java), OttoExtra-Kontodienst; Felder/Empfänger vollständig inventarisieren | Unlink und Token-/Sitzungswiderruf beim Backend prüfen. |
| Zusätzliches Chatlog | Bewusste Wahl vorgeschlagen | Nur lokal `logs/chat/ottoextra-chat-JJJJ-MM-TT-X.log`; keine Uploadfolge aus lokalem Speichern | Neue Einträge sofort stoppen; lokale Dateien gesondert löschen. |
| Geschützte Chatnachrichten | Vor zukünftiger Aktivierung gesondert prüfen | `createProtectedChatMessage` sendet Originaltext, Empfängerliste, optionale Übersetzungen als JSON an API | Im geprüften Core kein Aufrufer außerhalb Interface/Implementierung gefunden; keinen aktiven Upload behaupten. Add-ons prüfen. „Geschützt“ belegt keine Ende-zu-Ende-Verschlüsselung. |

**„Alles optional ablehnen“ bedeutet im Ziel:** keine von OttoExtra initiierten optionalen Web-Requests und kein zusätzlicher Chatlog; lokale Funktionen bleiben nutzbar. Kein Versprechen, Minecraft oder andere Mods seien dadurch vollständig offline.

### Datenschutz: Mindestklärung beim Betreiber

Bei anwendbarer DSGVO benötigt personenbezogene Verarbeitung eine tragfähige Rechtsgrundlage; Einwilligung ist eine mögliche Grundlage. Zweckbindung, Datenminimierung und transparente Verarbeitung bleiben relevant. Für fremde Spielernamen gilt eine eigene Bewertung; Nutzer-Opt-in allein löst diese nicht. [DSGVO, Art. 5/6 – EUR-Lex](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=de)

Eine verwendete Einwilligung muss informiert/freiwillig sein, nachweisbar und einfach widerrufbar. Das Backend muss den Wegfall dieser Grundlage bei weiterer Verarbeitung berücksichtigen. Lokaler Schalter und Löschantrag sind verschiedene Aktionen; daraus folgt kein Recht, Daten ohne weitere Grundlage unbegrenzt zu behalten. [BfDI: DSGVO/BDSG, Einwilligung](https://www.bfdi.bund.de/SharedDocs/Downloads/DE/Broschueren/INFO1.pdf?__blob=publicationFile&v=61)

- [ ] Tatsächlichen Verantwortlichen/Kontakt und zuständige Rollen benennen; Hosting-/Dienstleisterverhältnisse prüfen.
- [ ] Je Datentyp Zwecke, Rechtsgrundlage, Empfänger, Zugriff und begründete Speicherfrist festlegen; keine erfundenen Fristen oder „anonym“-Zusicherung.
- [ ] Betroffeneninformationen und Verfahren für Rechte einschließlich Auskunft/Berichtigung/Löschung festlegen; Daten Dritter, Minderjährige und mögliche Drittlandübermittlungen nach tatsächlichem Dienst prüfen lassen.
- [ ] Schutz-/Löschkonzept betrifft auch Request-Logs, Backups und Wiederherstellung. Widerruf lässt sich nicht allein durch Löschen der lokalen Config erledigen.

Diese Punkte sind Prüf-/Umsetzungsaufträge; der Client-Audit bestätigt keine rechtliche Gesamtkonformität.

## P2 – nur bei gewünschter Modrinth-Veröffentlichung

| ID | Aufgabe | Freigabebedingung |
|---|---|---|
| [ ] **P2-01** | KI-Herkunft, Public/Unlisted und Seitenbilder klären | KI-spezifische Zulässigkeit vor Einreichung geklärt; siehe [Bestandsaufnahme, Abschnitt 1](modrinth-submission-checklist.md#1-darf-ottoextra-auf-modrinth). |
| [ ] **P2-02** | Sämtliche Content Disclosures, englische Summary/Beschreibung | Tatsächlichen Release/Backend-Stand abbilden, Pflichtfelder speichern. Keine pauschale Telemetrie-Opt-in-Angabe ohne vollständige Prüfung. |
| [ ] **P2-03** | Modrinth-Cheat-Regeln, Dependencies, Upload und Moderationsnachweis | Tier-Glow ggf. vor Einreichung zwingend korrigieren; exakte geprüfte JAR verwenden. Erneute Einreichung erst nach Pflichtnachweisen. |

Niedrige Arbeitspriorität bedeutet hier **aufschiebbarer Vertriebskanal**, keine Ausnahme von Plattformregeln. Modrinth-Details und englische Entwürfe bleiben in der verlinkten Bestandsaufnahme. Allgemeine Nutzerhinweise, Lizenzen und korrekte Release-Metadaten gehören schon zu P0/P1.

## P3 – danach

- [ ] **P3-01:** Galerie/Featured-Screenshot, polierte Banner und bebilderte Anleitung.
- [ ] **P3-02:** Komfort bei Privacy-Verwaltung, z. B. verständliche lokale Datenübersicht. Keine zusätzlichen Nutzungsmetriken nur für deren Betrieb einführen.

## Fahrplan: konkrete Reihenfolge ohne Modrinth

| Schritt | Arbeitspaket / verantwortlich | Abschluss |
|---|---|---|
| **1. Eingrenzen** | Client-Entwicklung + Betreiber: P0-01/04/05 inventarisieren; ungeklärte Upload-/Online-Funktionen im verteilten Build zuverlässig deaktivieren. | Deaktivierung am tatsächlichen Einstiegspunkt nachgewiesen; Bestandsnutzer/alte Config berücksichtigt. Keine bloßen Default-Änderungen bei weiterhin aktiven Altwerten. |
| **2. Gemeinsame Freigabe/Widerruf** | Client-Entwicklung: P0-01/02/03, einschließlich Startup, Jobs, Restore und Pack-Tick. | Erststart-/Migration-/Widerrufstests bestanden. Danach darauf aufbauende UI/Transportpfade fertigstellen. |
| **3. Transport und Backend** | Client + Backend-Entwicklung: P0-05, P1-01; Betreiber parallel P0-04/06. | Ziele, Zugriff, Rechtsgrundlagen, Löschkontakt und Verbreitungsrechte belegt. Ungeklärte Funktionen bleiben aus. |
| **4. Datenschutz- und Kontrollfunktionen** | Client + Betreiber: P1-02 bis P1-08; tatsächliche Hinweise in App, README und Distribution aktualisieren. | Chatlog/Dateien, Pack-Auswahl, Vote, Effekte und Serverkontrolle geprüft. |
| **5. Release-Kandidat** | QA/Release-Verantwortlicher: P1-09; finale Negativtests und Smoke-Test. | Exakte JAR + Hash + Ergebnisse; keine offenen P0 der enthaltenen Funktionen. P1 vor breitem/stabilem Release abgeschlossen. |
| **6. Optional Modrinth** | Autor: P2, danach P3. | Bei anderem Vertrieb entfällt dieser Schritt; Schritt 1–5 bleiben relevant. |

### Abnahmefälle für Schritt 2–5

- [ ] Alles ablehnen; Neustart; Ottonien/anderer Server/Singleplayer: kein ungewollter OttoExtra-Transfer/Chatlog.
- [ ] Jede Freigabe einzeln aktivieren: nur zugehörige Funktion; verständliche Folgen bei Ablehnung.
- [ ] Skin, Banner, Pack, RP-Upload, API-Auth/-Sync gezielt verzögern; widerrufen vor Start, während Transfer und vor Callback/Aktivierung. Erneutes Erlauben reaktiviert keine alten Jobs.
- [ ] Backup mit `true` über aktuelles `false` wiederherstellen; Anbieter wechseln; Config beschädigen: keine stillschweigende Freigabe.
- [ ] Unerlaubter Redirect, übergroße Antwort, Offline/Timeout: keine Credential-Abgabe, keine endlosen Retries, kein beschädigtes vorhandenes Pack.
- [ ] Zwei Backend-Testkonten: fremde Datensätze/Rollen nicht unbefugt les-/änderbar; vereinbarte Berichtigung/Löschung praktisch prüfen.
- [ ] Logs/Caches löschen: richtige Auswahl, aktive Dateien, Altdatei `logs/ottoextra-chat.log`, Backups und Löschumfang kontrolliert. Bestehende Nutzerdaten nicht im Audit löschen.
- [ ] Finale `test`-/`build`-Prüfung über Projekt-Wrapper, Clientstart und passende Integrationsprüfungen; Ergebnis pro Artefakt festhalten.

**Aktueller Stand:** P0-Clientschutz und P1-Clientfunktionen einschließlich lokaler Offline-Datenverwaltung umgesetzt; lokale Tests/Build und Ingame-Smoke-Test bestanden. [P1-Ergebnisse](p1-datenschutz-umsetzung.md). P0-Laufzeit-/Betreiber-/Rechtenachweise bleiben offen. Erst danach betroffene Funktionen zur Verteilung freigeben.
