# Config-Felder ohne Eintrag im Einstellungsbildschirm

Die Datei `config/ottoextra/ottoextra.json` enthält mehr Felder, als der Einstellungsbildschirm zeigt.
Diese Liste nennt alle Felder der Kern-Mod, die in der Einstellungs-Registry (`SettingsRegistry.java`) nicht vorkommen,
und wo sie stattdessen geändert werden können.

- Felder mit Eintrag im Einstellungsbildschirm: 209
- Felder ohne Eintrag: 151
- Die Spalte „Standard“ zeigt den Ausdruck aus dem Quelltext (`OttoExtraConfig.java`), nicht umgerechnet.
- Die Zuordnung „Änderbar über“ stammt aus der Durchsicht der Unterbildschirme, Befehle und Tasten (siehe `30-unterbildschirme.md`, `31-befehle-tasten-sonstiges.md`). Felder mit „nirgends“ wurden dort nicht gefunden; eine vollständige Suche über alle Bildschirme der Mod wurde nicht durchgeführt.
- Addons haben eigene Config-Dateien; deren Felder sind hier nicht aufgeführt.

## Bereich `api`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `api.enabled` | `boolean` | `true` | nirgends (nur direkt in der Datei) |
| `api.baseUrl` | `String` | `"https://api.ottoextra.dev/"` | nirgends (nur direkt in der Datei) |
| `api.connectTimeoutMs` | `int` | `10_000` | nirgends (nur direkt in der Datei) |
| `api.requestTimeoutMs` | `int` | `20_000` | nirgends (nur direkt in der Datei) |
| `api.syncIntervalSeconds` | `int` | `1_800` | nirgends (nur direkt in der Datei) |
| `api.playerDirectoryIntervalSeconds` | `int` | `300` | nirgends (nur direkt in der Datei) |
| `api.tlsPinning` | `boolean` | `true` | nirgends (nur direkt in der Datei) |

## Bereich `map`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `map.paintedMapOffsetX` | `int` | `0` | Pfeil-Schaltflächen auf der gemalten Karte |
| `map.paintedMapOffsetZ` | `int` | `0` | Pfeil-Schaltflächen auf der gemalten Karte |
| `map.npcVillages` | `java.util.List<NpcVillage>` | `defaultNpcVillages()` | nirgends (nur direkt in der Datei) |
| `map.minimapBannerSize` | `int` | `16` | Unterbildschirm „Wappen-Position“ (Ecke ziehen) |
| `map.minimapBannerOffsetX` | `int` | `18` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.minimapBannerOffsetY` | `int` | `-15` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.minimapBannerRound` | `boolean` | `true` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.minimapBannerOffsetXRound` | `int` | `10` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.minimapBannerOffsetYRound` | `int` | `-30` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.toggleKey` | `String` | `"key.keyboard.k"` | Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen |
| `map.labelScale` | `float` | `1.0f` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.labelZoomA` | `double` | `0.06` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.labelZoomB` | `double` | `0.45` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.factionScaleA` | `float` | `0.7f` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.factionScaleB` | `float` | `1.3f` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.lehenScaleA` | `float` | `0.5f` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.lehenScaleB` | `float` | `0.95f` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.bannerSizeA` | `int` | `12` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.bannerSizeB` | `int` | `28` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.bannerSizePx` | `int` | `20` | nirgends (nur direkt in der Datei) |
| `map.borderWidthPx` | `int` | `1` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.dashLengthPx` | `int` | `6` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.dashGapPx` | `int` | `4` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `map.groupColors` | `java.util.LinkedHashMap<String, String>` | `defaultGroupColors()` | Unterbildschirm „Gefolge-Farben“ |
| `map.groupNameOverrides` | `java.util.LinkedHashMap<String, String>` | `new java.util.LinkedHashMap<>()` | Unterbildschirm „Gefolge“ (Gefolge-Liste) |
| `map.lehenColors` | `java.util.LinkedHashMap<String, String>` | `new java.util.LinkedHashMap<>()` | Unterbildschirm „Gefolge“ (Gefolge-Liste) |
| `map.factionColors` | `java.util.LinkedHashMap<String, String>` | `new java.util.LinkedHashMap<>()` | Unterbildschirm „Gefolge“ (Gefolge-Liste) |
| `map.bannerHudX` | `int` | `78` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.bannerHudY` | `int` | `77` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.bannerHudFromRight` | `boolean` | `true` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.bannerHudFromBottom` | `boolean` | `false` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.nameHudX` | `int` | `48` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.nameHudY` | `int` | `134` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.nameHudFromRight` | `boolean` | `true` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.nameHudFromBottom` | `boolean` | `false` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.stateHudX` | `int` | `3` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.stateHudY` | `int` | `121` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.stateHudFromRight` | `boolean` | `true` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.stateHudFromBottom` | `boolean` | `false` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.factionHudX` | `int` | `5` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.factionHudY` | `int` | `110` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.factionHudFromRight` | `boolean` | `true` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |
| `map.factionHudFromBottom` | `boolean` | `false` | Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) |

## Bereich `regions`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `regions.theme` | `String` | `"light"` | Unterbildschirm „Toast-Themes“ |
| `regions.customThemes` | `java.util.List<RegionTheme>` | `new java.util.ArrayList<>()` | Unterbildschirm „Toast-Themes“ |
| `regions.menuKey` | `String` | `"key.keyboard.l"` | Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen |
| `regions.activityHudCustomX` | `float` | `0.5f` | Unterbildschirm „Aktivitäts-HUD platzieren“ (Ziehen) |
| `regions.activityHudCustomY` | `float` | `0.5f` | Unterbildschirm „Aktivitäts-HUD platzieren“ (Ziehen) |
| `regions.activityHudRefreshSeconds` | `int` | `120` | Befehl `/ottoextra activityhud rate <Sekunden>` |

## Bereich `rpnames`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `rpnames.showInSprechen` | `boolean` | `true` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInReden` | `boolean` | `true` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInRufen` | `boolean` | `true` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInBruellen` | `boolean` | `true` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInFluestern` | `boolean` | `true` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInMurmeln` | `boolean` | `true` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInOoc` | `boolean` | `false` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.showInAllChannels` | `boolean` | `false` | nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) |
| `rpnames.tablistShowAccountForUnknown` | `boolean` | `true` | nirgends (nur direkt in der Datei) |
| `rpnames.meetDefaultVersion` | `Integer` | (nicht gesetzt) | intern (Merker der Mod, keine Einstellung) |
| `rpnames.syncFromPublicApi` | `boolean` | `true` | nirgends (nur direkt in der Datei) |
| `rpnames.inspectShowAccount` | `boolean` | `false` | nirgends (nur direkt in der Datei) |
| `rpnames.inspectMaxDistance` | `double` | `8.0` | nirgends (nur direkt in der Datei) |
| `rpnames.inspectOffsetY` | `int` | `38` | nirgends (nur direkt in der Datei) |

## Bereich `nametags`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `nametags.toggleKey` | `String` | `"key.keyboard.n"` | Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen |

## Bereich `letter`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `letter.maxLinesPerPage` | `int` | `12` | nirgends (nur direkt in der Datei) |
| `letter.maxCharsPerLine` | `int` | `18` | nirgends (nur direkt in der Datei) |
| `letter.pageModeMaxLinesPerPage` | `int` | `12` | nirgends (nur direkt in der Datei) |
| `letter.pageModeEffectiveCharBudget` | `int` | `248` | nirgends (nur direkt in der Datei) |
| `letter.courierV2Migration` | `int` | `1` | intern (Merker der Mod, keine Einstellung) |
| `letter.announcementHardLinesPerPage` | `int` | `12` | nirgends (nur direkt in der Datei) |
| `letter.announcementHardCharsPerLine` | `int` | `18` | nirgends (nur direkt in der Datei) |
| `letter.letterPageDelayMinMs` | `int` | `3000` | nirgends (nur direkt in der Datei) |
| `letter.letterPageDelayMaxMs` | `int` | `5200` | nirgends (nur direkt in der Datei) |

## Bereich `chat`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `chat.npcNameColor` | `String` | `DEFAULT_NPC_NAME_COLOR` | Unterbildschirm „Chatkanal-Farben“ |
| `chat.npcMessageColor` | `String` | `DEFAULT_NPC_MESSAGE_COLOR` | Unterbildschirm „Chatkanal-Farben“ |
| `chat.channelColors` | `java.util.Map<String, ChannelColors>` | `defaultChannelColors()` | Unterbildschirm „Chatkanal-Farben“ |
| `chat.voiceKey` | `String` | `"key.keyboard.v"` | Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen |
| `chat.helpKey` | `String` | `"key.keyboard.h"` | Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen |
| `chat.offtopicKey` | `String` | `"key.keyboard.o"` | Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen |

## Bereich `resourcepack`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `resourcepack.directZipUrl` | `String` | `""` | nirgends (nur direkt in der Datei) |
| `resourcepack.respectUserDisable` | `boolean` | `true` | nirgends (nur direkt in der Datei) |
| `resourcepack.priorityTop` | `boolean` | `true` | nirgends (nur direkt in der Datei) |
| `resourcepack.maxSizeBytes` | `long` | `64L * 1024 * 1024` | nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) |
| `resourcepack.showToasts` | `boolean` | `true` | nirgends (nur direkt in der Datei) |

## Bereich `consent`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `consent.onboardingVersion` | `int` | `0` | intern (Merker der Mod, keine Einstellung) |
| `consent.policyVersion` | `int` | (nicht gesetzt) | intern (Merker der Mod, keine Einstellung) |
| `consent.acceptedScopes` | `java.util.Map<String, String>` | `java.util.Map.of()` | intern (Merker der Mod, keine Einstellung) |
| `consent.decidedAt` | `java.util.Map<String, Long>` | `java.util.Map.of()` | intern (Merker der Mod, keine Einstellung) |

## Bereich `animalInfo`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `animalInfo.breederModeEnabled` | `boolean` | `false` | Addon OttoExtra-AnimalBreeding (Karte „Züchtermodus“) |
| `animalInfo.queryTimingVersion` | `Integer` | (nicht gesetzt) | intern (Merker der Mod, keine Einstellung) |
| `animalInfo.padding` | `int` | `3` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showModel` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showTitle` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showBreeding` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showMast` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showTimer` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showExtra` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.showHorseStats` | `boolean` | `true` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.modelX` | `int` | `0` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.modelY` | `int` | `0` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.modelSize` | `int` | `26` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.titleX` | `int` | `28` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.titleY` | `int` | `0` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.titleScale` | `float` | `1.0f` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.breedingX` | `int` | `28` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.breedingY` | `int` | `10` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.breedingScale` | `float` | `0.68f` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.mastX` | `int` | `100` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.mastY` | `int` | `10` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.mastScale` | `float` | `0.68f` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.timerX` | `int` | `28` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.timerY` | `int` | `19` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.timerScale` | `float` | `0.68f` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.extraX` | `int` | `100` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.extraY` | `int` | `19` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.extraScale` | `float` | `0.68f` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.horseStatsX` | `int` | `28` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.horseStatsY` | `int` | `28` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.horseStatsScale` | `float` | `0.68f` | veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart |
| `animalInfo.layouts` | `java.util.Map<String, AnimalInfoLayout>` | `new java.util.LinkedHashMap<>()` | Unterbildschirm „Tierinfo-Toast bearbeiten“ (je Tierart) |

## Bereich `tweaks`

| Feld | Typ | Standard | Änderbar über |
|---|---|---|---|
| `tweaks.chatLogDecisionVersion` | `int` | (nicht gesetzt) | intern (Merker der Mod, keine Einstellung) |
| `tweaks.chatLogAcceptedScope` | `String` | `""` | intern (Merker der Mod, keine Einstellung) |
| `tweaks.lowHealth.vignetteMinAlpha` | `float` | `0.06f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.vignetteMaxAlpha` | `float` | `0.62f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.reduceWhenScreenOpen` | `boolean` | `true` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.screenOpenMultiplier` | `float` | `0.35f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.calmAfterNoDamageSeconds` | `int` | `8` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.calmMinHealth` | `float` | `12.0f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.calmVisualMultiplier` | `float` | `0.35f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMinIntensity` | `float` | `0.12f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMaxIntervalMs` | `int` | `1500` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMinIntervalMs` | `int` | `450` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMinVolume` | `float` | `0.18f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMaxVolume` | `float` | `0.75f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMinPitch` | `float` | `0.85f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.heartbeatMaxPitch` | `float` | `1.15f` | nirgends (nur direkt in der Datei) |
| `tweaks.lowHealth.fadeSpeed` | `float` | `0.12f` | nirgends (nur direkt in der Datei) |
| `tweaks.toolProtect.uiBlocks` | `java.util.List<String>` | `defaultUiBlocks()` | nirgends (nur direkt in der Datei) |
| `tweaks.vote.timeZone` | `String` | `DEFAULT_TIME_ZONE` | nirgends (nur direkt in der Datei) |
| `tweaks.vote.serverAddresses` | `List<String>` | `new ArrayList<>(DEFAULT_SERVER_ADDRESSES)` | nirgends (nur direkt in der Datei) |
| `tweaks.vote.voteUrls` | `List<String>` | `new ArrayList<>(ALLOWED_VOTE_URLS)` | nirgends (nur direkt in der Datei) |
| `tweaks.vote.openingNoticeAccepted` | `boolean` | `false` | intern (Merker der Mod, keine Einstellung) |
| `tweaks.vote.lastCompletedVoteDay` | `String` | `""` | intern (Merker der Mod, keine Einstellung) |
| `tweaks.vote.integrationMigrationVersion` | `int` | `0` | intern (Merker der Mod, keine Einstellung) |

## Zusammenfassung nach Ort

| Änderbar über | Anzahl Felder |
|---|---|
| nirgends (nur direkt in der Datei) | 45 |
| veraltet: nur noch Quelle für die Umstellung auf Layouts je Tierart | 29 |
| nur im alten, nicht mehr erreichbaren GUI („Erweitert …“) | 18 |
| Unterbildschirme „Wappen-Position“ und „HUD anordnen“ (Ziehen) | 16 |
| intern (Merker der Mod, keine Einstellung) | 12 |
| nur vom Erststart-Assistenten gesetzt (Rollenspiel-Voreinstellung) | 8 |
| Startwert einer Taste; geändert wird über Minecrafts Steuerungs-Einstellungen | 6 |
| Unterbildschirm „Gefolge“ (Gefolge-Liste) | 3 |
| Unterbildschirm „Chatkanal-Farben“ | 3 |
| Pfeil-Schaltflächen auf der gemalten Karte | 2 |
| Unterbildschirm „Toast-Themes“ | 2 |
| Unterbildschirm „Aktivitäts-HUD platzieren“ (Ziehen) | 2 |
| Unterbildschirm „Wappen-Position“ (Ecke ziehen) | 1 |
| Unterbildschirm „Gefolge-Farben“ | 1 |
| Befehl `/ottoextra activityhud rate <Sekunden>` | 1 |
| Addon OttoExtra-AnimalBreeding (Karte „Züchtermodus“) | 1 |
| Unterbildschirm „Tierinfo-Toast bearbeiten“ (je Tierart) | 1 |
