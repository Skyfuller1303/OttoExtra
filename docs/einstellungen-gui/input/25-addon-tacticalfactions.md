# Tactical Factions

- Quelle: Addon `OttoExtra-TacticalFactions` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `tacticalfactions`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Lokale politische Beziehungen, Kampf-Nametags und Friendly-Fire-Schutz.
- Umfang: 1 Tab(s), 3 Karte(n), 10 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Kampfmodus“

Kartenbeschreibung im GUI: (keine)

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Tactical Operation Mode | Schalter (An/Aus) | Aus |  | – | `tactical.combatMode` |
| 2 | Spielernamen im Kampf anzeigen | Schalter (An/Aus) | An |  | – | `tactical.showPlayerNames` |
| 3 | Politische Farben | Schalter (An/Aus) | An |  | – | `tactical.politicalColors` |
| 4 | Friendly Fire verhindern | Schalter (An/Aus) | BuildVariant.friendlyFireProtectionAvailable() |  | Achtung: Das Aktivieren dieser Funktion kann zum Bann führen.<br>*Hinweis: Nur vorhanden, wenn die Build-Variante `friendlyFireProtection` aktiv ist.* | `tactical.preventFriendlyFire` |
| 5 | Neutrale Spieler ebenfalls schützen | Schalter (An/Aus) | Aus |  | *Hinweis: Nur vorhanden, wenn die Build-Variante `friendlyFireProtection` aktiv ist.* | `tactical.protectNeutralPlayers` |
| 6 | Angreifer mit rotem ! markieren | Schalter (An/Aus) | An |  | – | `tactical.retaliationMarkers` |
| 7 | Politik-Buch öffnen | Schaltfläche | – | Text auf der Schaltfläche: „Öffnen“ | – | `tactical.openBook` |

### Karte „Politische Farben“

Kartenbeschreibung im GUI: Farben für Verbündete und Gegner.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 8 | Verbündetenfarbe | Farbfeld (Hex `#RRGGBB`) | `#5555FF` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `tactical.allyColor` |
| 9 | Gegnerfarbe | Farbfeld (Hex `#RRGGBB`) | `#FF5555` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `tactical.hostileColor` |

### Karte „Tabliste“

Kartenbeschreibung im GUI: Gruppierung und Farben nach politischer Beziehung im Kampfmodus.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 10 | Gegner zuerst | Schalter (An/Aus) | Aus |  | An: Gegner, Verbündete, Neutrale. Aus: Verbündete, Gegner, Neutrale. Innerhalb jeder Gruppe bleibt die vorhandene Tablisten-Sortierung erhalten. | `tactical.tablistHostilesFirst` |
