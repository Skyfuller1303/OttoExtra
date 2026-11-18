# OttoExtra – Befehle und Konfiguration

## Minecraft-Befehle

Alle Einträge der Tabelle beginnen mit `/ottoextra`. Die Java-Pfade sind relativ zu `src/client/java/de/ottoextra/`. Eckige Klammern markieren optionale Argumente. Die Zeilen stehen in der Reihenfolge der aufgeführten Varianten.

| Befehl | Datei und Zeilen |
| --- | --- |
| `verify [status\|unlink]`, `account` | `verify/AccountLinkModule.java` 46, 51, 55, 62 |
| `hud` | `ui/hudlayout/HudLayoutSettings.java` 28 |
| `debug [status\|on\|off]` | `logging/DebugLog.java` 42, 44, 46, 54 |
| `rpnames buch [<spieler>]`, `rpnames hoverdebug on\|off` | `rpnames/RpNamesModule.java` 335, 338, 345, 354 |
| `tweaks lowhealth test [<intensity>]`, `tweaks lowhealth stop\|on\|off` | `tweaks/TweaksModule.java` 69, 74, 80, 86, 93 |
| `vote test\|reset` | `tweaks/vote/VoteReminderCommands.java` 18, 24 |
| `activityhud rate [<seconds>]` | `regions/RegionsModule.java` 111, 114 |
| `regions refresh`, `regions banner [<style> [<region>]]`, `regions status` | `regions/RegionsModule.java` 130, 136, 138, 146, 153 |
| `banderole`, `banderole set <key> [<value>]`, `banderole reset [<key>]`, `banderole show\|replay\|hide\|export` | `regions/BanderoleDebugCommands.java` 40, 43, 48, 52, 59, 64, 69, 76, 81 |
| `letter open\|weiter\|abbrechen\|send\|announce\|close` | `letter/LetterModule.java` 51, 57, 61, 65, 69, 73 |
| `letter calibrate [<number>]`, `letter calibrate v2 <number>`, `letter calibrate v2 boundary\|final\|refine <number>` | `letter/LetterModule.java` 77, 142, 131, 83, 99, 115 |

- Die Vervollständigung bietet Stilnamen und Banderolen-Schlüssel dynamisch an. Siehe `RegionsModule.java` 140–142 und `BanderoleDebugCommands.java` 44–45, 60–61.
- Der Client sendet die Serverbefehle `/s`, `/f`, `/m`, `/r`, `/b`, `/o` und `/h` für Chatkanäle (`chat/ChatChannelState.java` 12–18, 139). Die konfigurierbaren Standardbefehle für Briefe sind `/letter <Inhalt>`, `/post <Empfänger>` und `/verkünden` (`config/OttoExtraConfig.java` 883–886, `letter/LetterServices.java` 217, 324, 481, 518).
- `/ottoextra resources [status|sync|pos1|pos2]` gehört zum separaten Resources-Add-on (`README.md` 242–249).

## Build und Projektwerkzeuge

- `npm run client` (`package.json` 5) startet Gradles `runClient`. Das Projekt konfiguriert `runClient` und `runClient2` in `build.gradle` 131 sowie die eigenen Aufgaben `generateDebugBuildMarker` und `dist` in Zeile 14 und 148.
- `tools/local-data.py` stellt `list`, `plan`, `export` und `delete` bereit (Zeile 186). Die weiteren Einstiegspunkte sind `tools/test_local_data.py` (95), `tools/dev-client/prepare-mousewheelie.py` (87), `tools/test-courier-contract.ps1` und `tools/generate-horse-overview-icons.ps1` (jeweils 1).
- Test- und Vorbereitungsskripte beginnen jeweils in Zeile 1. Unter `tools/audit-2026-09-08/` liegen `test-long-chat-sender.ps1` und `test-persistence.ps1`, unter `tools/consent-smoke/` liegen `prepare-backend.py`, `rp-http-test.py` und `server-smoke.py`. Courier-E2E startet über `tools/courier-e2e/run.ps1` und nutzt `server/{build,prepare,start}.ps1`.
- Die aufrufbaren Designgeneratoren liegen in `docs/design/generator/` (jeweils ab Zeile 1). Die Dateinamen beginnen mit `gen_` und enden auf `.py`: `baender`, `briefui`, `einstellungen_buch`, `minentimer_hud`, `ottonien_ui`, `personen_band`, `personenui`, `rpbuch`, `rpbuch2`, `tactical`, `toasts`, `trupp_hud`.

## Konfigurationsdateien

| Datei | Wichtigste Schlüssel und Einstellungen |
| --- | --- |
| `build.gradle`, `settings.gradle` | `plugins`, `loom.runs`, `dependencies`, `java`, `ottoextraAlwaysDebug`; `pluginManagement.repositories`, `rootProject.name` |
| `gradle.properties` | `org.gradle.jvmargs`, `org.gradle.parallel`, `modpack_java_version`, `minecraft_version`, `yarn_mappings`, `loader_version`, `loom_version`, `fabric_version`, `modmenu_version`, `mod_version`, `maven_group`, `archives_base_name` |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl`, `networkTimeout`, `validateDistributionUrl`, `distributionBase`, `distributionPath`, `zipStoreBase`, `zipStorePath` |
| `package.json`, `.gitignore`, `.orch/project.json` | `name`, `private`, `scripts.client`; Git-Ausschlussmuster; lokale Orchestrator-Schlüssel `version`, `id`, `name`, `default_base`, `default_models` |
| `src/main/resources/fabric.mod.json` | `id`, `version`, `environment`, `entrypoints`, `mixins`, `depends`, `suggests` |
| `src/main/resources/ottoextra.mixins.json` | `required`, `minVersion`, `package`, `compatibilityLevel`, `client`, `injectors.defaultRequire` |
| `config/ottoextra/ottoextra.json` (Laufzeit) | `api`, `map`, `regions`, `rpnames`, `nametags`, `letter`, `chat`, `resourcepack`, `consent`, `animalInfo`, `tweaks`, `settingsView`. Schema in `config/OttoExtraConfig.java` ab Zeile 24 im Java-Quellbaum. |
| `config/ottoextra-discord.json` (Laufzeit) | `endpointUrl`, `pollIntervalSeconds`, `requestTimeoutSeconds`, `deviceToken`, `linkPending`, `minecraftUuid`, `minecraftName`, `authorizationUrl` |
| `config/ottoextra-vote.json` (Altbestand) | `enabled`, `resetHour`, `timeZone`, `serverAddresses`, `voteUrls`. Die aktuellen Einstellungen liegen unter `tweaks.vote`. |
| `config/ottoextra/rpnames/title-catalog.json`, `title-groups.json` | Katalog `categories`, `entries` mit `id`, `title`, `variants`, `category`, `colorOverride`. Gruppen `label`, `priority`, `titleColor`, `nameColor`, `titles`. Die beiden `*-default.json` liegen unter `src/main/resources/assets/ottoextra/rpnames/`. |
| `tools/chat-log-regression/client.init.gradle`, `tools/consent-smoke/{client,privacy,server}.init.gradle`, `tools/courier-e2e/client.init.gradle` | Test-Source-Sets und Laufprofile, im Consent-Smoke-Test `privacyQaMode`, im Courier-Test `courierE2eFilter` |
| Jeweils `fabric.mod.json` unter `tools/chat-log-regression/resources/`, `tools/consent-smoke/client/resources/` und `tools/courier-e2e/client/resources/` | `id`, `environment`, `entrypoints.fabric-client-gametest`, je nach Datei `depends` und `mixins` |
| `tools/consent-smoke/client/resources/ottoextra-privacy-qa.mixins.json`, `tools/courier-e2e/client/resources/courier-e2e.mixins.json` | `required`, `package`, `compatibilityLevel`, `client`, `injectors.defaultRequire` |
| `tools/courier-e2e/server/plugin.yml`, `tools/courier-e2e/server/server.properties` | `name`, `version`, `main`, `api-version`, `loadbefore`; `server-ip`, `server-port`, `online-mode`, `gamemode`, `level-type` |

Unter `src/main/resources/assets/` liegen zusätzlich die Schriftdefinition `mattis/font/menu.json` (`providers`), die Grafikeffekte `ottoextra/post_effect/{inspect_lens,low_health_edge_blur}.json` (`targets`, `passes`), das Modell `ottoextra/models/entity/meet_marker.json` (`format_version`, `textures`, `elements`) und die Ressourcendaten `ottoextra/names/animals.json` (`mares`, `stallions`, `appearance`), `ottoextra/map/lehen_polygons.json` (`polygons`) sowie `ottoextra/lang/{de_de,en_us}.json` (Übersetzungsschlüssel).

Die Dokumentation verwendet `docs/einstellungen-gui/beschriftungen.json` (`label`, `help`, `level`, `category`, `tab`, `section`) und `docs/design/generator/mcfont/default.json` (`providers`).
