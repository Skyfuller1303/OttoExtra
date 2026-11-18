# Tierinfo

- Position: Reiter 9 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `animalinfo`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Zeigt Geschlecht und Paarungsbereitschaft anvisierter Zuchttiere kompakt im HUD.
- Umfang: 2 Tab(s), 7 Karte(n), 20 Einträge

## Tab „Basis“

### Karte „Tierinfo beim Anvisieren“

Kartenbeschreibung im GUI: Fragt die Tierinfo nach kurzem Anvisieren automatisch ab und zeigt nur die wichtigen Werte oben an.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Automatische Tierinfo | Schalter (An/Aus) | An |  | – | `animalInfo.enabled` |
| 2 | Automatische Antwort ausblenden | Schalter (An/Aus) | An |  | Blendet nur von OttoExtra angeforderte Tierinfo-Blöcke aus Chat und Client-Log aus. Manuelle Abfragen bleiben sichtbar. | `animalInfo.hideAutomaticChat` |
| 3 | Toast-Layout bearbeiten… | Schaltfläche | – | Text auf der Schaltfläche: „Bearbeiten…“ | – | `animalInfo.layout` |

### Karte „Toast-Stil“

Kartenbeschreibung im GUI: Aussehen der Tierinfo beim Anvisieren: kompakte Leiste, ausführliche Karte oder das eigene Layout.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 4 | Stil | Auswahl (Klick schaltet weiter) | `detailed` | `compact` (Anzeige: „Kompakt“)<br>`detailed` (Anzeige: „Ausführlich“)<br>`custom` (Anzeige: „Eigenes Layout“) | Kompakt: schmale Leiste mit Statusstreifen. Ausführlich: Karte mit Statusband und Fortschrittsbalken. Eigenes Layout: frei anpassbarer Toast; der Layout-Editor gilt nur für diesen Stil. | `animalInfo.toastStyle` |

### Karte „Tier-Nametag“

Kartenbeschreibung im GUI: Darstellung der Namen über anvisierten Tieren.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 5 | Geschlecht im Nametag anzeigen | Schalter (An/Aus) | Aus |  | Ergänzt ♀ oder ♂, sobald das Geschlecht aus Tierinfo oder Tierregister bekannt ist. Gilt für OttoExtras Tier-Nametags. | `animalInfo.nametagShowGender` |
| 6 | Tiernamen nach Geschlecht färben | Schalter (An/Aus) | Aus |  | Weiblich: Rosa. Männlich: Blau. Unbekannt: bisherige Farbe. Funktioniert auch ohne Geschlechtssymbol. | `animalInfo.nametagGenderColor` |
| 7 | Auch ungespeicherte Tiere | Schalter (An/Aus) | Aus |  | Nutzt bekannte Toast-Daten auch ohne Tierregister-Eintrag. Geschlecht oder Namensfarbe zusätzlich aktivieren. Mit lokalem Tiercache bleiben Tierart und Geschlecht bis zu 14 Tage verfügbar. Keine zusätzlichen Abfragen oder automatischen Register-Einträge. | `animalInfo.nametagIncludeUnregistered` |

### Karte „Tier-Glow“

Kartenbeschreibung im GUI: Tiere auf Ottonien nach bekanntem Geschlecht umranden: Weiblich rosa, männlich blau.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 8 | Tiere nach Geschlecht umranden | Schalter (An/Aus) | Aus |  | Im Menü oder mit der Tier-Glow-Taste (standardmäßig F8) umschalten. Tierinfos zuerst abfragen; Tiere mit unbekanntem Geschlecht erhalten keinen Umriss. Reichweite: unter 64 Blöcken. | `animalInfo.genderGlowEnabled` |
| 9 | Gepaarte Tiere umranden | Schalter (An/Aus) | An |  | Tiere, die nach einer Paarung warten, leuchten orchideenfarben (Magenta-Violett), bis sie wieder paaren können. Gilt auch ohne Geschlechts-Glow und hat Vorrang vor der Geschlechtsfarbe. Die Paarung wird erkannt, wenn zwei Tiere Herzchen zeigen und danach ein Jungtier daneben erscheint; die Wartezeit ist dann ein Schätzwert aus der Ottonien-Tabelle, bis du das Tier wieder anvisierst. | `animalInfo.breedingGlowEnabled` |
| 10 | Geschorene Schafe umranden | Schalter (An/Aus) | An |  | Selbst geschorene Schafe leuchten golden, bis sie wieder scherbar sind (6 Stunden oder die Zeit aus der Tierinfo). Hat Vorrang vor allen anderen Umrissfarben. | `animalInfo.shearGlowEnabled` |

## Tab „Erweitert“

### Karte „Lokaler Tiercache“

Kartenbeschreibung im GUI: Tierart, Geschlecht und Wartezeiten nach dem Wiederverbinden für Glow, Nametags und Statuszeile verwenden.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 11 | Tiergeschlecht und Wartezeiten 14 Tage lokal merken | Schalter (An/Aus) | An |  | Speichert bis zu 1.024 Tiere je Server auf diesem Rechner. Ablauf 14 Tage nach der letzten erhaltenen Tierinfo. Zusätzlich werden die letzte Zuchtbereitschaft, der Mast-Stand, Wartezeiten (Paarung, Scheren) und beobachtete Paarungen gespeichert, damit sie nach einem Neustart weiterlaufen. Hof, Besitzer und Register-Daten werden nicht gespeichert. Ausschalten und Einstellungen speichern löscht den Cache des aktuellen Servers; keine Datenbank-Einträge. | `animalInfo.rememberAnimalIdentities` |
| 12 | Alle Tiere dieses Servers vergessen | Schaltfläche | – | Text auf der Schaltfläche: „Vergessen“ | Löscht die lokal gemerkten Tiere und Zeiten des aktuell verbundenen Servers (Geschlecht, Paarungs-, Schur- und Mastzeiten). Nur verbunden möglich; der Cache baut sich beim nächsten Anvisieren wieder auf. | `animalInfo.forgetAnimals` |

### Karte „Tierarten in der Übersicht“

Kartenbeschreibung im GUI: Zusätzliche Tierarten als eigene Filter anbieten. Alle zeigt sämtliche gespeicherten Tierarten. Pferde und Esel sind immer als Filter verfügbar.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 13 | Rinder | Schalter (An/Aus) | Aus |  | – | `animalInfo.overviewShowCows` |
| 14 | Brakel | Schalter (An/Aus) | Aus |  | – | `animalInfo.overviewShowChickens` |
| 15 | Hausschweine | Schalter (An/Aus) | Aus |  | – | `animalInfo.overviewShowPigs` |
| 16 | Ziegen | Schalter (An/Aus) | Aus |  | – | `animalInfo.overviewShowGoats` |
| 17 | Zaupel | Schalter (An/Aus) | Aus |  | – | `animalInfo.overviewShowSheep` |

### Karte „Abfrageverhalten“

Kartenbeschreibung im GUI: Verzögerung, Auffrischintervall (Standard 60 s, nach dem Füttern sofort) und Zeitlimit der automatischen Tierinfo-Abfrage.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 18 | Anvisierverzögerung (ms) | Zahlenfeld (ganze Zahl) | `100` | 100 – 2000 | – | `animalInfo.hoverDelayMs` |
| 19 | Auffrischintervall (ms) | Zahlenfeld (ganze Zahl) | `60000` | 1000 – 60000 | – | `animalInfo.cacheDurationMs` |
| 20 | Antwort-Zeitlimit (ms) | Zahlenfeld (ganze Zahl) | `3000` | 1000 – 10000 | – | `animalInfo.responseTimeoutMs` |
