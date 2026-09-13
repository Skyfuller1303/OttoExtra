# OttoExtra: Modrinth-Checkliste

> Aktualisierung 13.09.2026: Gezielte Finder-Umrisse sind wieder aktiv (Suchziel, unterstützte Tierart und aktive Verbindung erforderlich). AnimalOutlinePolicy sperrt weiterhin automatische Geschlechtsumrisse. Die folgenden Aussagen zur vollständigen Finder-Sperre beschreiben den früheren P1-Stand; eine neue Live-/Release-Abnahme wurde hier nicht durchgeführt.

Stand: **11.09.2026**. Geprüft: Arbeitsstand von `OttoExtra`, HEAD `76dae13`, einschließlich vorhandener uncommitteter Änderungen. `gradle.properties`: **0.1.17-l**. Alte Checkliste/README nannten **0.1.17-g**; Dateien in `dist/` belegen keine geprüfte Release-Version.

Grundlagen: bereitgestellte Moderationsnachrichten/Screenshots, aktuelle offizielle Regeln, statische Prüfung relevanter Codepfade. Live-Modrinth-Projektseite, dort hochgeladene JAR, vollständige Backend-Datenhaltung und KI-Herkunft sind **nicht verifiziert**. Die ursprüngliche Bestandsaufnahme war rein lesend. Anschließend wurden P0/P1-Clientänderungen implementiert und getestet; [RP-Hinweis und sechsmonatige RP-Bereinigung](rp-verzeichnis-datenschutz.md) sind inzwischen gezielt live umgesetzt. Maßgebliche Ergebnisse stehen in den verlinkten Protokollen. Modrinth-Einstellungen bleiben unverändert.

**Ergebnis: Öffentliche Zulässigkeit derzeit offen; erneute Einreichung noch nicht ausreichend belegt.** Client-Freigaben, Widerruf und P1-Schutzmaßnahmen sind umgesetzt; zusätzliche Tier-Umrisse sind gesperrt. Offen: KI-/Asset-Herkunft, Betreiber-/Backend-Abnahme und verbleibende Laufzeitfälle.

Technischer Nachtrag: [Abnahme vom 11.09.2026](privacy-qa-2026-09-11.md) mit echten
Client-/SQL-/Servertests und lokalem Replay-Fix. Die anschließende
[Modpack-Reparatur](modpack-reparatur-2026-09-11.md) besteht einschließlich AnimalBreeding;
Produktionslauncher- und Betreiberabnahme bleiben offen.
Diese Tests ändern keine Modrinth-Einstellungen und belegen keine Plattformfreigabe.

**Arbeitsreihenfolge:** [Release- und Datenschutz-Fahrplan mit P0–P3](release-datenschutz-fahrplan.md). Er gilt auch ohne Modrinth und enthält die maßgebliche Priorisierung. Dieses Dokument bleibt die fachliche Bestandsaufnahme und Einreichungscheckliste; „Pflicht vor Einreichung“ bezeichnet eine Voraussetzung für diesen Vertriebskanal, keine globale P0-Einstufung. `[x]` bedeutet im Quellstand gefunden, keine bestätigte Modrinth-Abnahme.

**Umsetzung:** [P0-Clientschutz](p0-umsetzung.md) · [P1-Datenschutz und Nutzerkontrolle](p1-datenschutz-umsetzung.md). Backend-/Rechtenachweise und Modrinth-Abnahme bleiben offen.

## 1. Darf OttoExtra auf Modrinth?

| Situation | Einschätzung |
|---|---|
| KI unterstützt; erhebliche menschlich erstellte Inhalte vorhanden | Öffentliche Veröffentlichung grundsätzlich möglich, sofern übrige Anforderungen erfüllt. |
| Inhalt überwiegend/vollständig KI-generiert; menschlicher Beitrag hauptsächlich Prompten/Testen | Öffentlich nicht zulässig. KI-Disclosure allein behebt das nicht. |
| Überwiegend KI-generiert, Veröffentlichung per Direktlink | Laut KI-Leitfaden häufig als **Unlisted** möglich; keine garantierte Zulassung, keine Befreiung von Sicherheits-/Rechtepflichten. |
| OttoExtra konkret | KI-Anteil und Asset-Herkunft ungeklärt. Commit-Autor und Testumfang beweisen keine menschliche Urheberschaft. |

Quelle: [Disclosure and Usage of AI](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai), „Entirely AI-generated Projects“. Aktuelle [Content Rules, §6](https://modrinth.com/legal/rules#generative-ai), geändert am 13.08.2026, gelten zusätzlich zur älteren Moderationsrückmeldung.

- [ ] **Pflicht vor Einreichung – Herkunft festhalten.** Autor dokumentiert für Kernmodule, Texte, Icon, Karten, Texturen: menschlich erstellt / KI-generiert / fremdes Werk, jeweilige Belege. Keine erfundenen Prozentwerte. Im Release-Nachweis ablegen.
- [ ] **Pflicht vor Einreichung – Veröffentlichungsweg entscheiden.** Bei überwiegender KI-Erstellung Unlisted mit Support klären; bei unklarem Mischprojekt tatsächliche Beiträge beschreiben. Support über Help Center; Moderationsthread benachrichtigt laut bereitgestelltem Hinweis keine Mitarbeitenden.
- [ ] **Pflicht vor Einreichung – Seitenbilder prüfen.** `src/main/resources/assets/ottoextra/icon.png`, Galerie, Banner und Beschreibungsbilder auf KI-Herkunft prüfen. KI-erstellte/-abgeleitete Seitenbilder ersetzen oder entfernen; Kennzeichnung erlaubt deren Upload nicht. [Content Rules, §6.2](https://modrinth.com/legal/rules#generative-ai)

## 2. Moderationsauflagen und konkrete Änderungen

### Downloads – Einwilligung vollständig absichern

Konkrete Moderationsauflage: informierte Zustimmung **vor sämtlichen Dateidownloads**, einschließlich Mod-Updates. Folgende Umsetzungsvorschläge dienen ihrem Nachweis; nicht jede technische Schutzmaßnahme ist wörtlich von Modrinth vorgeschrieben.

Bereits vorhanden:

- [x] Fünf Zustimmungszwecke standardmäßig `false`: [OttoExtraConfig.Consent](../src/client/java/de/ottoextra/config/OttoExtraConfig.java).
- [x] Erststart-Assistent mit getrennten Entscheidungen: [WelcomeScreen](../src/client/java/de/ottoextra/welcome/WelcomeScreen.java), [WelcomeScreenManager](../src/client/java/de/ottoextra/welcome/WelcomeScreenManager.java). Datenschutz-Schalter: [SettingsRegistry](../src/client/java/de/ottoextra/config/settings/SettingsRegistry.java).
- [x] Resourcepack: Zustimmung vor Start, erneute Prüfungen während Lesen und vor Installation, Größenlimit/ZIP-Prüfung: [PackDownloadService](../src/client/java/de/ottoextra/resourcepack/PackDownloadService.java), [ResourcePackUpdater](../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java).
- [x] Versionsprüfung separat erlaubt; geprüfter Update-Pfad lädt keine Mod-JAR herunter. Browserlink nach Klick: [UpdateChecker](../src/client/java/de/ottoextra/update/UpdateChecker.java), [UpdateAvailableScreen](../src/client/java/de/ottoextra/update/UpdateAvailableScreen.java).

| Offen | Wo | Was ändern / Abnahme |
|---|---|---|
| [ ] **Pflicht vor Einreichung: Skin-Widerruf** | [SkinCache](../src/client/java/de/ottoextra/chat/SkinCache.java), `ensurePng` | Freigabe ist nun vor HTTP und Cache-Commit generationsgebunden geprüft. Noch offen: reale verzögerte Skinantwort im laufenden Client. Test: Job verzögern → widerrufen → kein späterer Request/Cache-Commit. |
| [ ] **Pflicht vor Einreichung: Banner-Widerruf** | [BannerTextureService](../src/client/java/de/ottoextra/regions/BannerTextureService.java), `startDownload`, `maybeRefresh`; [HTTP-Client](../src/client/java/de/ottoextra/api/HttpOttoExtraApiClient.java), `downloadBinary` | Generationsgebundene Prüfung bis HTTP, Cache und Texturregistrierung implementiert. Noch offen: reale verzögerte Bannerantwort während GUI-Wechsel. |
| [ ] **Pflicht vor Einreichung: Vorgemerkte Pack-Aktivierung** | [PackInstaller.tick](../src/client/java/de/ottoextra/resourcepack/PackInstaller.java) | Tick verwirft jetzt ungültige Freigaben; Pending-Regressionstest bestanden. Noch offen: Widerruf während eines tatsächlichen Ressourcenreloads; siehe P0-02. |
| [x] **Information bei späterem Aktivieren** | `SettingsRegistry.build`, Datenschutz-Karte; [en_us.json](../src/main/resources/assets/ottoextra/lang/en_us.json), [de_de.json](../src/main/resources/assets/ottoextra/lang/de_de.json) | Einschalten öffnet Informationsdialog mit Quelle, Daten/Dateien, Speicherziel, Folgen und Widerruf. Erst „Erlauben“ erteilt Zustimmung. DE-Ingame-Smoke-Test bestanden; EN-Sichtprüfung offen. |
| [x] **Tatsächliche Quelle** | `OttoExtraConfig.ResourcePack`, `ResourcePackUpdater`, `WelcomeScreenManager.CONSENT_VERSION` | Assistent und Einstellungen zeigen tatsächliche Quelle, Asset, Größenlimit und Aktivierungswunsch. Policy-/Quellenbindung und Restore-Schutz implementiert; Regressionstests bestanden. |
| [ ] **Pflicht vor Einreichung: Erststart/Upgrade** | `OttoExtraClient.onInitializeClient`, `SkinCache.load`, `ResourcePackModule.onInitializeClient` | Migration wird vor Skin-/Pack-Initialisierung geprüft; frische/alte Flags lokal getestet. Für vollständige Abnahme kontrollierten Join-/Reconnect-Netzwerktest ergänzen. |
| [ ] **Release-Prüfung: URLs/Redirects** | `SkinCache.ensurePng`, `PackManifestClient.fetch`, `PackDownloadService.download`, `HttpOttoExtraApiClient.downloadBinary` | Empfängerprüfung vor Kontakt implementiert; Auth/API/Banner ohne automatische Redirects, ZIPs mit begrenzten GitHub-CDNs. Test: verbotenes Redirect-Ziel bleibt unkontaktiert. Backend-/Support-Datenprüfung noch offen. |
| [ ] **Release-Prüfung: Bildgrößen** | `SkinCache.ensurePng`, `HttpOttoExtraApiClient.downloadBinary` | Skin ohne sichtbares Byte-Limit; Banner prüft 5 MiB erst nach vollständigem `ofByteArray()`. Empfang begrenzen, PNG-Inhalt/Abmessungen prüfen. |
| [ ] **Release-Prüfung: Pack-Deaktivierung** | `OttoExtraConfig.ResourcePack.respectUserDisable = true` (Altwerte migriert), `ResourcePackUpdater`, `PackInstaller` | Implementiert: vorhandene manuelle Deaktivierung ist verbindlich, Regressionstests bestanden. Echten Ressourcenreload weiterhin abnehmen. |

Widerruf kann bereits übertragene Bytes nicht zurückholen. Erwartung: keine neuen Jobs; laufende Transfers soweit möglich abbrechen; nach Widerruf keine weitere Installation/Cache-Übernahme. Bereits vorhandene Dateien nur nach angekündigter Löschentscheidung entfernen.

### Datenübertragung vollständig beschreiben

| Tatsächlicher Pfad | Fundstelle | Offener Auftrag |
|---|---|---|
| Join auf Ottonien → Regions-Bootstrap → API-Authentifizierung | [OttoExtraClient](../src/client/java/de/ottoextra/OttoExtraClient.java), [RegionDataService](../src/client/java/de/ottoextra/regions/RegionDataService.java), `HttpOttoExtraApiClient.getString`, [ApiAuthService.handshakeOnce](../src/client/java/de/ottoextra/api/auth/ApiAuthService.java) | [ ] Name + UUID an API-Challenge, Name + Challenge-ID an Verify; unabhängig von `communityRpNameUploads`. Vor Nutzung erklären. Eigene Online-Freigabe und Erklärung sind jetzt eingebaut; tatsächliche Backend-Verarbeitung weiterhin nachweisen. |
| Minecraft-Sessionprüfung | [MinecraftSessionAuth](../src/client/java/de/ottoextra/api/auth/MinecraftSessionAuth.java) | [ ] Mojang-Sessionprüfung erklären. Geprüfter Code verwendet Minecraft-Token für Mojang `joinServer`; er legt es nicht in OttoExtra-Challenge-/Verify-JSON. Nicht als Tokenübertragung an OttoExtra darstellen. |
| RP-Beobachtungen: derzeit Build-Sperre | [RpNameUploadService](../src/client/java/de/ottoextra/rpnames/upload/RpNameUploadService.java) | [ ] Eigenen Accountnamen/UUID, fremden Accountnamen/UUID, RP-Namen, Empfänger und Zweck nennen. Warteschlangen-Widerruf bereits generationsgebunden. Kein pauschales „anonym“. |
| Regionsaktivität | `RegionDataService.ACTIVITY_API` | [ ] `mcp-api.ottonien.com` zusätzlich zu `api.ottoextra.dev` nennen; automatische GETs und lokale Caches berücksichtigen. |
| Tierregister/Finder/Höfe | [HorseRegistryModule](../src/client/java/de/ottoextra/horses/HorseRegistryModule.java), [HorseFinder](../src/client/java/de/ottoextra/horses/HorseFinder.java), [HorseRegistry](../src/client/java/de/ottoextra/horses/HorseRegistry.java) | [ ] Automatisches Laden/Polling von nutzerausgelöstem Speichern trennen. Tier-UUID, Finder-Auswahl und bei Registerspeicherung mögliche Standort-/Besitz-/Beobachtungsdaten anhand konkreter Funktion dokumentieren. |
| Lokales Chatprotokoll | [ChatMessageLog](../src/client/java/de/ottoextra/logging/ChatMessageLog.java), [Chatlog-Doku](chat-log.md) | [x] Aktueller Arbeitsstand: standardmäßig aus; eigene Freigabe mit Serverumfang, Logbudget und Offline-Löschvorschau. Alte Logs bleiben erhalten; [Datenverwaltung](lokale-daten.md). Lokales Logging allein belegt keinen Telemetrie-Upload. |

- [ ] **Pflicht vor Einreichung:** [modrinth-description.md](modrinth-description.md) aktualisieren. Beschreibung nennt vier optionale Entscheidungen und manuelle Kontoverknüpfung, lässt automatische API-Authentifizierung, Regionsaktivität und neue Tierfunktionen unerklärt. Manuelle Kontoverknüpfung und automatische API-Session auseinanderhalten.
- [ ] **Pflicht vor Einreichung:** Betreiber bestätigt gespeicherte Daten, Empfänger/Zugriffsrechte, Aufbewahrung und Löschung. IP-/Request-Protokollierung des Backends/Hosters ist aus Client-Code nicht ableitbar. Keine Löschfristen/Anonymität erfinden.
- [ ] **Pflicht vor Einreichung:** „Telemetry: Opt-in“ erst als Gesamtmodell angeben, wenn alle darunter fallenden Transfers geprüft sind. Erwartbarer Funktionsverkehr ist laut [Disclosure-FAQ](https://support.modrinth.com/en/articles/16567675-content-disclosures) nicht automatisch Telemetrie; zusätzliche/undurchsichtige Datensammlung schon.

### Tier-Glow und Multiplayer-Regeln

**Ausgangsbefund:** Zusätzliche Finder-/Geschlechtsumrisse konnten ohne Sichtlinienprüfung aktiv werden. **P1-Stand:** `AnimalOutlinePolicy` sperrt diese Effekte in allen Kontexten; Serverhostname, Altconfig und UI-Schalter können sie nicht aktivieren. Die folgenden Nachweise sind vor einer Reaktivierung erforderlich.

- [ ] **Pflicht vor Einreichung:** [EntityGlowMixin](../src/client/java/de/ottoextra/mixin/EntityGlowMixin.java), [AnimalGenderGlow](../src/client/java/de/ottoextra/horses/AnimalGenderGlow.java), `HorseFinder.prepare` hinter undurchsichtigen Blöcken testen; begründetes Risiko für Sicht durch Wände, kein durchgeführter Laufzeittest.
- [ ] **Pflicht vor Einreichung:** Bei Sicht-durch-Wände-Verhalten serverseitiges Opt-in erzwingen oder Effekt beschränken/entfernen. [OttoExtraGate](../src/client/java/de/ottoextra/OttoExtraGate.java) prüft nur Hostnamen; kein technisches Server-Opt-in. Modrinths [Content Rules, §3](https://modrinth.com/legal/rules) verlangen für X-Ray/Sicht durch undurchsichtige Blöcke Server-Opt-in; sonstige unfaire Multiplayer-Vorteile benötigen Server-Opt-out.
- [ ] **Release-Prüfung:** Weitere Helfer im finalen Artefakt prüfen: Tierabfragen, Litematica-Fachwerkhilfe, Werkzeugschutz, Combat-HUD. Namen allein belegen weder Cheat noch Zulässigkeit; Verhalten/Serverkontrolle dokumentieren.

## 3. Content Disclosures: Auswahl für OttoExtra

Modrinth-Projekt → Disclosure-Einstellungen. Empfehlungen für geprüften Core; Backend, mitveröffentlichte Add-ons und ältere verfügbare Versionen zusätzlich prüfen. Angaben erst nach Klärung offener Tatsachen speichern.

| Feld | Empfehlung / nötiger Eintrag |
|---|---|
| **Contains AI-generated content → Code** | [ ] Bei erheblichem KI-Codeanteil aktivieren. Herkunft offen; Beiträge aus Abschnitt 1 ergänzen. |
| **… → Assets** | [ ] Bei enthaltenen KI-Assets aktivieren; Icon/Karten/Texturen prüfen. Seitenbilder zusätzlich nach Abschnitt 1 bereinigen. |
| **… → Text** | [ ] Bei KI-generierten Namen, Übersetzungen oder Seiten-/Changelogtexten aktivieren. Werden unten entworfene englische Veröffentlichungstexte übernommen, KI-Textnutzung angeben. Reine Rechtschreibprüfung nicht damit gleichsetzen. |
| **Contains generative AI functionality** | [ ] Im untersuchten Core kein LLM-/Bildgenerierungsaufruf gefunden. Kein belegter Aktivierungsgrund; Backend/Add-ons prüfen. Entwicklungs-KI und feste Namenslisten sind keine Runtime-KI. |
| **Contains advertisements** | [ ] Vote-Reminder prüfen: standardmäßig aktiv, vier Vote-Seiten, Browseröffnung per Aktion. Nicht allein wegen Ottonien-Bezug ausschließen. Unaufdringliche relevante Eigenwerbung oder kennzeichnungspflichtige Promotion? Sponsoring/Money-Bezug beim Betreiber klären. Bei letzterem aktivieren und Vote-Funktion nennen. |
| **Contains paid features** | [ ] Im geprüften Core keine belegte Echtgeld-Freischaltung. Konto-Ränge/Helleranzeige allein reichen nicht. Backend-Zugänge, Cosmetics und Add-ons auf Bezahlschranken prüfen; ggf. konkrete Funktionen nennen. |
| **Contains telemetry** | [ ] Wegen optionalem RP-Beobachtungsupload Aktivierung empfohlen; Daten/Empfänger nennen. Consent-Modell erst nach Abschnitt 2 festlegen. Auth, Funktionssync und Backend-Logs getrennt bewerten. |
| **Contains derivative content** | [ ] Kartenbilder, OttoPlus-Analyse und ggf. übernommener Code: Herkunft/Lizenzen prüfen. Bei relevanter Übernahme Originalname, Original-URL und Ableitung eintragen. Reine Xaero-/Courier-Kompatibilität macht OttoExtra nicht automatisch zum Fork. |
| **Photosensitivity warning** | [ ] Aktivierung für optionalen Low-Health-Effekt empfohlen: optionaler voller Modus mit großflächiger pulsierender roter Vignette; standardmäßig aus, reduzierte Variante vorhanden. [LowHealthHudOverlay](../src/client/java/de/ottoextra/tweaks/lowhealth/LowHealthHudOverlay.java), `OttoExtraConfig.Tweaks.LowHealth`. Laufzeittest durchführen, Abschaltweg nennen; keine medizinische Unbedenklichkeit behaupten. |
| **External system interactions** | [ ] Config, Cache, Resourcepack und normale Exporte allein begründen Aktivierung nicht. [OttoExtraPaths](../src/client/java/de/ottoextra/config/OttoExtraPaths.java) nutzt Spiel-/Config-Verzeichnisse. Vote-Aktion öffnet mehrere Browserseiten und kopiert Accountnamen; vorher klar ankündigen, gegen FAQ-Ausnahme für erwartbares Verhalten prüfen. Überraschende Interaktionen/add-on-seitige Fremdprogramme konkret offenlegen. |
| **Archive project** | [ ] Aus lassen, solange weiterentwickelt. Gezeigter Banner ist Vorschau, kein Archivierungsnachweis. Nicht als Ersatz für Unlisted verwenden. |

Bezug: bereitgestellte Feldbeschreibungen und [Content Disclosures FAQ](https://support.modrinth.com/en/articles/16567675-content-disclosures). Normale Konfiguration/erwartbare Dateiaktionen sind dort von „External System Interactions“ ausgenommen. Sicherheitsangaben auch für ältere noch verteilte Versionen berücksichtigen.

## 4. Rechte, Fremdcode und Assets

[Terms of Use](https://modrinth.com/legal/terms), „About the Service“/„User Contributions“: Rechte und Verantwortung verbleiben beim Uploader; Modrinth benötigt Verbreitungsrechte und kann Beiträge ablehnen. Lokaler MIT-Eintrag ersetzt keine Rechteprüfung fremder Bestandteile.

- [x] [LICENSE](../LICENSE) vorhanden; `fabric.mod.json` nennt MIT; `build.gradle` nimmt Lizenzdatei in JAR auf.
- [ ] **Pflicht vor Einreichung:** Genehmigung für `textures/map/otto-large_map_*.png` belegen: Rechteinhaber, Dateien, Modrinth-Verteilung, Attribution, Lizenzumfang. [Beschreibung](modrinth-description.md) behauptet Genehmigung; im geprüften Material kein eigenständiger Nachweis gefunden.
- [ ] **Pflicht vor Einreichung:** [OttoPlus-Formatierungsanalyse](ottoplus-brief-formatierung.md) nennt dekompilierten Fremdcode. Prüfen, ob nur Verhalten nachvollzogen oder geschützter Code übernommen wurde. Nicht ohne Beleg als Fork/Rechtsverstoß einstufen.
- [ ] **Pflicht vor Einreichung:** Übernommene Drittbestandteile mit Ursprung/Lizenz/Attribution dokumentieren. Karten ggf. vom MIT-Lizenzumfang des eigenen Codes abgrenzen. Nur belegte Rechte zusichern.
- [ ] **Release-Prüfung:** Release-JAR auf fremde gebündelte JARs/Assets prüfen. `modCompileOnly` für Xaero/Mod Menu vorhanden; endgültiges Artefakt separat kontrollieren.

## 5. Projektseite und Release

- [ ] **Pflicht vor Einreichung – English Summary:** Modrinth-Summary speichern. Entwurf: `Client-side Ottonien companion for role-play names, chat, letters, maps, and animal records.`
- [ ] **Pflicht vor Einreichung – English Description:** [modrinth-description.md](modrinth-description.md) enthält Englisch/Deutsch. Nach Abschnitt 2 korrigieren, vollständige englische Fassung auf Modrinth speichern, neu laden und prüfen. Screenshot meldet weiterhin Übersetzungsbedarf; lokale Datei beweist keinen Live-Stand. Deutsche Serverausrichtung hebt konkrete Moderationsauflage nicht auf.
- [ ] **Pflicht vor Einreichung – Funktionsumfang:** Tierregister/Finder, Vote-Reminder, Low-Health, Chatlog und Online-Verkehr beschreiben. Kein pauschales „keine Datenübertragung ohne Zustimmung“, solange Auth-Verhalten aus Abschnitt 2 besteht.
- [ ] **Release-Prüfung – Version:** [README](../README.md), Release-Notizen, `gradle.properties`, JAR-internes `fabric.mod.json` und Modrinth-Version auf geprüften Build abstimmen; aktuell unterschiedliche Stände.
- [ ] **Release-Prüfung – Metadaten:** Mod, Fabric, Minecraft **1.21.11**, clientseitig/Serverinstallation nicht erforderlich, Java **21**. Deklarierter Loader mindestens **0.18.1**, Build verwendet **0.19.3**; Mindestversion prüfen oder anheben. Fabric API mindestens **0.141.4+1.21.11** laut [fabric.mod.json](../src/main/resources/fabric.mod.json).
- [ ] **Release-Prüfung – Dependencies pro Version:** Fabric API erforderlich; Mod Menu, Xaero's Minimap, Xaero's World Map und Litematica als optionale Integrationen prüfen/eintragen. XaeroLib nicht ungeprüft separat fordern: konkrete Xaero-Releases/Bündelung verifizieren. DevAuth/Compile-only-Test-JARs nicht als Release-Abhängigkeit veröffentlichen. Legacy-Importnamen OttoTalk/OttoNames/OttoChat nicht automatisch als Dependencies behandeln. Java/Loader-Anforderungen stehen in Mod-Metadaten, nicht als verlinkte Mod-Projekte.
- [ ] **Release-Prüfung – Links:** Source-/Issues-/Release-Links aus Metadaten ohne Login auf Erreichbarkeit prüfen. Noch nicht als öffentlich geprüft abhaken.
- [ ] **Release-Prüfung – Upload-Datei:** geprüfte remappte `ottoextra-<version>.jar`; keine Sources-/Dev-/`always-debug`-JAR als Hauptdatei. Source-JAR optional korrekt markieren. `OttoExtra-0.1.15.4-player-link.zip` nicht als gewöhnliche Zusatzdatei verwenden. Add-ons ggf. eigene Projekte.
- [ ] **Optional – Galerie:** echte aktuelle Screenshots: Einstellungen/Consent, Briefeditor, Karte. Eine Abbildung als Featured auswählen. [Bildliste](images/README.md) nutzen; keine KI-Bilder/-Bearbeitung, keine privaten Chat-/Accountdaten veröffentlichen.

Englisch beruht auch auf der bereitgestellten Moderationsauflage; Metadaten/Abhängigkeiten auf Repository und [Content Rules, §§2/5](https://modrinth.com/legal/rules).

## 6. Nachweise vor erneuter Einreichung

Tests sind **offen**, keine Ergebnisse dieser Dokumentationsprüfung. Isoliertes Minecraft-Profil mit finalem Release-Build verwenden; Spielverkehr, Mod-Verkehr und Browseraktionen getrennt erfassen.

| Abnahme | Erwartung / Nachweis |
|---|---|
| [ ] Frische Config, alles ablehnen | Vor Entscheidung und danach keine von OttoExtra initiierten Pack-/Skin-/Banner-/Update-Downloads, keine RP-Uploads. Join-/API-Authentifizierung gesondert erfassen; nicht daraus „kein Netzwerk“ folgern. |
| [ ] Pro Zustimmung ein Test | Nur zugehöriger Transfer freigeschaltet. Dialog nennt tatsächliche Quelle, Zweck, Dateityp/Ziel und automatische Folgeaktionen. |
| [ ] Widerruf mit verzögertem Job/Response | Skin, Banner und Pack einzeln; auch erlauben → widerrufen → erneut erlauben. Alte Jobs bleiben ungültig. |
| [ ] Alte Config/Backup/Migration | Fehlende Zustimmung bleibt aus. Neue Zwecke/Quellen erben keine alte Freigabe. Ablehnen bleibt nach Neustart erhalten. |
| [ ] Fehlerfälle | Offline, unerlaubter Redirect, übergroßes/ungültiges PNG/ZIP: kontrollierter Abbruch, vorhandenes Pack bleibt benutzbar. |
| [ ] Privacy/Backend | Gesendete Daten gegen Veröffentlichungstext prüfen; keine Tokens/Secrets im Nachweis veröffentlichen. |
| [ ] Multiplayer/Glow | Ottonien, anderer Server, Singleplayer, Wandtest; ggf. Serverfreigabe verweigert/entzogen → Effekt aus. |
| [ ] Effekte/Vote | Low-Health maximal testen, Abschaltung erreichbar; Vote-Taste und Clipboard-/Browseraktion entsprechen sichtbarem Hinweis. |
| [ ] Release-Smoke-Test | Clientstart/Kernfunktionen ohne optionale Mods; Integrationen separat. Auf Dedicated Server darf clientseitiges JAR keine Clientklassen laden. |

Vorhandene Regressionstests: [PackDownloadLifecycleTest](../src/test/java/de/ottoextra/resourcepack/PackDownloadLifecycleTest.java), [PackRecoveryTest](../src/test/java/de/ottoextra/resourcepack/PackRecoveryTest.java), [RpNameConsentQueueTest](../src/test/java/de/ottoextra/rpnames/upload/RpNameConsentQueueTest.java), [ApiAuthServiceTest](../src/test/java/de/ottoextra/api/auth/ApiAuthServiceTest.java). Bilddownload-/UI-Fälle gezielt ergänzen. Danach Projekt-Wrapper für `test`/`build` nutzen; Laufzeitabnahmen zusätzlich nötig.

- [ ] Release-Nachweis ablegen: Version, Commit/Arbeitsstand, SHA-256 der getesteten JAR, Testdatum/-ergebnisse, bereinigte Screenshots, bekannte Einschränkungen.
- [ ] Pflichtpunkte dieser Einreichungscheckliste erledigen oder mit Modrinth konkret klären; allgemeine Release-Blocker aus dem Fahrplan ebenfalls schließen. Unlisted muss ebenfalls Sicherheits-/Rechtepunkte erfüllen.
- [ ] Disclosures und aktualisierte englische Seite speichern; richtige JAR hochladen.
- [ ] Moderation über Änderungen informieren, dann **Resubmit for review** auslösen. Nachricht allein startet keine erneute Prüfung. Erst nach tatsächlicher Einreichung abhaken.

## 7. Englische Textbausteine

**Entwürfe; erst nach Umsetzung/Prüfung verwenden. Platzhalter ersetzen.** Keine erfolgte Behebung/Freigabe behaupten. Bei Übernahme als Veröffentlichungstext KI-Textnutzung berücksichtigen.

**KI-Erklärung – nur passende, belegte Aussagen übernehmen:**

> Generative AI was used for [specific code, assets, or text]. Human-created contributions include [specific contributions]. The project page uses AI-assisted text. [State the actual asset provenance separately.]

**RP-Upload – nur diese Funktion, nicht sämtliche Backend-Verarbeitung:**

> Optional RP-name sharing sends your Minecraft account name and/or UUID, together with the observed player's RP name, account name and UUID when available, to api.ottoextra.dev. This data is not anonymous. Sharing is disabled by default and can be revoked in Privacy settings. [Add verified retention, access and deletion information; describe other data flows separately.]

**Photosensitivity – falls Effekt enthalten bleibt:**

> An optional low-health effect displays a pulsing red vignette around the screen. It is disabled by default and can be turned off in OttoExtra's low-health settings.

**Erneute Prüfung – nach bestandener Abnahme:**

> The English summary and description have been updated. Version [version, SHA-256] now requires informed opt-in before OttoExtra initiates resource-pack, skin or banner downloads. Declining these permissions keeps those downloads disabled. Revocation and configuration-upgrade behavior were tested. Update checks do not download mod JARs; release links open only after a user click. The project page and disclosures describe the verified network behavior and AI involvement. Evidence: [public links]. Please review this build for [public/unlisted] availability.
