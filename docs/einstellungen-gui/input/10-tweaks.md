# Tweaks

- Position: Reiter 10 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `tweaks`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Optionale clientseitige Effekte und Spiel-Tweaks.
- Umfang: 2 Tab(s), 9 Karte(n), 36 Einträge

## Tab „Basis“

### Karte „Chatprotokoll“

Kartenbeschreibung im GUI: Zusätzliche Originalnachrichten lokal speichern. Standard aus; Aktivierung erklärt Inhalt und Serverumfang.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Chatprotokoll speichern | Schalter (An/Aus) | Aus (Zustimmung nötig) |  | Braucht eigene Freigabe. Auch ausgeblendete Antworten werden gespeichert. 8 MiB/Datei, 64 MiB gesamt; keine automatische Löschung. | `tweaks.chatLog` |
| 2 | Chatlog-Umfang (Änderung braucht neue Freigabe) | Auswahl (Klick schaltet weiter) | `OTTONIEN` | `OTTONIEN`<br>`ALL` | – | `tweaks.chatLogScope` |

### Karte „Kampfmodus“

Kartenbeschreibung im GUI: Zeigt die zuletzt erkannte Kampftechnik rechts neben der Hotbar, solange du eine Waffe in der Haupthand hältst.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 3 | Kampfmodus-Icon anzeigen | Schalter (An/Aus) | An |  | Übernimmt den Modus aus der Actionbar auf Ottonien. Vor der ersten Erkennung erscheint die Waffenart mit einem Fragezeichen. Bei freier Hand wird das Icon ausgeblendet. | `tweaks.combatModeHud` |

### Karte „Herstellungsanzeige“

Kartenbeschreibung im GUI: Ersetzt die Fortschrittsanzeige des Servers an Werkbänken durch ein animiertes Symbol mit eigenem Fortschrittsbalken.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 4 | Eigene Herstellungsanzeige | Schalter (An/Aus) | An |  | Blendet nur die Fortschrittsanzeige (Kästchen) in der Actionbar aus. Alle anderen Actionbar-Meldungen, auch die Abschlussmeldung, bleiben unverändert. | `tweaks.craftingHud` |
| 5 | Herstellungs-Geräusche | Schalter (An/Aus) | An |  | Spielt beim Herstellen einen Schlag pro Animationsdurchlauf sowie einen Ton bei Erfolg oder Fehlschlag. | `tweaks.craftingHudSounds` |

### Karte „Aktivitäts-HUD“

Kartenbeschreibung im GUI: Zeigt aktive Gefolge kompakt mit Wappen und Aktivitätsstufe.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 6 | Aktivitäts-HUD anzeigen | Schalter (An/Aus) | An |  | Es werden nur Gefolge oder Lehen angezeigt, für die aktuell Aktivität gemeldet wird. | `regions.activityHudEnabled` |
| 7 | Aktuelle Uhrzeit anzeigen | Schalter (An/Aus) | An |  | – | `regions.activityHudShowClock` |
| 8 | Letzte Aktualisierung anzeigen | Schalter (An/Aus) | An |  | – | `regions.activityHudShowLastUpdated` |
| 9 | Position des Aktivitäts-HUDs | Auswahl (Klick schaltet weiter) | `MIDDLE_LEFT` | `MIDDLE_LEFT` (Anzeige: „Mitte links“)<br>`TOP_LEFT` (Anzeige: „Oben links“)<br>`BOTTOM_LEFT` (Anzeige: „Unten links“)<br>`MIDDLE_RIGHT` (Anzeige: „Mitte rechts“)<br>`TOP_RIGHT` (Anzeige: „Oben rechts“)<br>`BOTTOM_RIGHT` (Anzeige: „Unten rechts“)<br>`CUSTOM` (Anzeige: „Frei“) | – | `regions.activityHudPosition` |
| 10 | Maximal sichtbare Aktivitäten | Zahlenfeld (ganze Zahl) | `8` | 1 – 20 | – | `regions.activityHudMaxEntries` |
| 11 | Größe des Aktivitäts-HUDs | Zahlenfeld (Kommazahl) | `1.0` | 0.5 – 2.0 | – | `regions.activityHudScale` |
| 12 | Aktivitäts-HUD frei platzieren | Schaltfläche | – | Text auf der Schaltfläche: „Platzieren…“ | – | `regions.activityHudEdit` |

### Karte „Werkzeugschutz“

Kartenbeschreibung im GUI: Bricht Links-/Rechtsklick ab, wenn das gehaltene Werkzeug kurz vor dem Kaputtgehen ist, und warnt einmalig bei niedriger Haltbarkeit.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 13 | Werkzeugschutz aktivieren | Schalter (An/Aus) | An |  | – | `tweaks.toolProtect.enabled` |
| 14 | Blockieren ab (Nutzungen) | Zahlenfeld (ganze Zahl) | `10` | 1 – 50 | Interaktionen werden abgebrochen, wenn dem Werkzeug höchstens so viele Nutzungen bleiben. | `tweaks.toolProtect.blockAtUses` |
| 15 | Warnung unter (%) | Zahlenfeld (ganze Zahl) | `10` | 1 – 50 | Einmalige Actionbar-Warnung, wenn die Haltbarkeit unter diesen Prozentsatz fällt. | `tweaks.toolProtect.warnBelowPercent` |

### Karte „Vote-Reminder“

Kartenbeschreibung im GUI: Erinnerung mit einmaligem Bestätigungsdialog: Vote-Seiten öffnen und Accountnamen in die Zwischenablage kopieren.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 16 | Vote-Reminder aktivieren | Schalter (An/Aus) | An |  | – | `tweaks.vote.enabled` |
| 17 | Resetstunde (Europe/Berlin) | Zahlenfeld (ganze Zahl) | `4` | 0 – 23 | – | `tweaks.vote.resetHour` |
| 18 | Verzögerung nach Join (Sekunden) | Zahlenfeld (ganze Zahl) | `5` | 0 – 60 | – | `tweaks.vote.joinDelaySeconds` |
| 19 | Anzeigedauer (Sekunden) | Zahlenfeld (ganze Zahl) | `10` | 2 – 60 | – | `tweaks.vote.toastDurationSeconds` |
| 20 | Wiederholung (Minuten) | Zahlenfeld (ganze Zahl) | `30` | 1 – 1440 | – | `tweaks.vote.reminderIntervalMinutes` |

### Karte „Litematica-Integration“

Kartenbeschreibung im GUI: Optionale Anpassungen für Litematica. OttoExtra funktioniert auch ohne installierte Litematica-Mod.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 21 | Lokalisierte Blocknamen im HUD | Schalter (An/Aus) | An |  | Ersetzt minecraft:xyz in Litematicas kompakten Blockinfos durch den Inventarnamen aus der aktiven Resourcepack-Sprache. | `tweaks.litematica.localizedBlockNames` |
| 22 | Fachwerk automatisch abstimmen | Schalter (An/Aus) | Aus |  | Ein Rechtsklick mit dem Stemmeisen stimmt einen Fachwerk-Notenblock schrittweise auf den Litematica-Zielzustand ab. | `tweaks.litematica.fachwerkAutoTune` |

### Karte „Verwundung & Adrenalin“

Kartenbeschreibung im GUI: Warnung: Voller Modus kann pulsierende Ränder, Unschärfe, FOV-Bewegung und Herzschlag erzeugen. Standard aus; reduzierter Modus ohne diese Effekte. Abschalten wirkt sofort.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 23 | Effekt aktivieren | Schalter (An/Aus) | Aus |  | – | `tweaks.lowHealth.enabled` |
| 24 | Reduzierte Effekte (ohne Puls, Blur, FOV, Herzschlag) | Schalter (An/Aus) | An |  | – | `tweaks.lowHealth.reducedMotion` |
| 25 | Roter Rand | Schalter (An/Aus) | An |  | – | `tweaks.lowHealth.vignetteEnabled` |
| 26 | Herzschlag-Sound | Schalter (An/Aus) | An |  | – | `tweaks.lowHealth.heartbeatEnabled` |
| 27 | Edge-Blur (Tunnelblick) | Schalter (An/Aus) | An |  | Ränder verschwimmen bei niedriger Health, die Mitte bleibt scharf. HUD bleibt scharf. | `tweaks.lowHealth.blurEnabled` |
| 28 | Blur ab Herzen | Zahlenfeld (Kommazahl) | `7.0` | 1.0 – 10.0 | Ab dieser Herzzahl beginnt der Edge-Blur (1 Herz = 2 Health). | `tweaks.lowHealth.blurStartHearts` |
| 29 | Blur-Stärke | Zahlenfeld (Kommazahl) | `1.0` | 0.0 – 3.0 | Wie stark die Ränder verschwimmen (0 = aus, 3 = stark). | `tweaks.lowHealth.blurStrength` |
| 30 | FOV-Adrenalin | Schalter (An/Aus) | An |  | Leichter FOV-Anstieg bei niedriger Health — Adrenalin-Gefühl. | `tweaks.lowHealth.fovEnabled` |
| 31 | FOV-Zuschlag (Grad) | Zahlenfeld (Kommazahl) | `8.0` | 0.0 – 25.0 | Maximaler FOV-Zuschlag in Grad bei voller Intensität. | `tweaks.lowHealth.fovMaxDegrees` |
| 32 | Start ab Health | Zahlenfeld (Kommazahl) | `20.0` | 2.0 – 40.0 | Effekt beginnt unterhalb dieser Health (Server-Max 40 = 20 Herzen, Start bei 20 = 10 Herzen). | `tweaks.lowHealth.startHealth` |
| 33 | Intensität | Zahlenfeld (Kommazahl) | `1.0` | 0.3 – 2.0 | – | `tweaks.lowHealth.intensityScale` |
| 34 | Beruhigen ohne Schaden | Schalter (An/Aus) | An |  | Herzschlag und starker Blur beruhigen sich nach einigen Sekunden ohne neuen Schaden. | `tweaks.lowHealth.calmAfterNoDamage` |

## Tab „Erweitert“

### Karte „Aktivitäts-HUD verschieben“

Kartenbeschreibung im GUI: Feinjustierung der gewählten HUD-Position in Pixeln.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 35 | Aktivitäts-HUD: X-Versatz | Zahlenfeld (ganze Zahl) | `0` | -1000 – 1000 | – | `regions.activityHudOffsetX` |
| 36 | Aktivitäts-HUD: Y-Versatz | Zahlenfeld (ganze Zahl) | `0` | -1000 – 1000 | – | `regions.activityHudOffsetY` |
