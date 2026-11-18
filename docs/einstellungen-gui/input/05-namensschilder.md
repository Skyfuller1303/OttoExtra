# Namensschilder

- Position: Reiter 5 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `nametags`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Namensschilder über Spielerköpfen: Titel, RP-Name, Account.
- Umfang: 2 Tab(s), 3 Karte(n), 10 Einträge

## Tab „Basis“

### Karte „Sichtbarkeit“

Kartenbeschreibung im GUI: REALISTIC zeigt Schilder nur mit Sichtlinie — wie echtes Erkennen.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Namensschild-Modul | Schalter (An/Aus) | An |  | – | `nametags.enabled` |
| 2 | Namensschilder | Auswahl (Klick schaltet weiter) | `REALISTIC` | `NORMAL`<br>`REALISTIC`<br>`HIDE_ALL` | – | `nametags.mode` |

### Karte „Zeilen“

Kartenbeschreibung im GUI: Welche Zeilen über dem Kopf erscheinen.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 3 | Titel anzeigen | Schalter (An/Aus) | An |  | – | `nametags.showTitle` |
| 4 | RP-Name am Schild | Schalter (An/Aus) | An |  | – | `nametags.showRpName` |
| 5 | Accountname am Schild | Schalter (An/Aus) | Aus |  | – | `nametags.showPlayerName` |

## Tab „Erweitert“

### Karte „Größe & Farbe“

Kartenbeschreibung im GUI: Schriftgrößen je Zeile (1.0 = Vanilla) und Zeilenabstand.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 6 | Titel-Schriftgröße | Zahlenfeld (Kommazahl) | `1.0` | 0.4 – 2.5 | – | `nametags.titleScale` |
| 7 | Name-Schriftgröße | Zahlenfeld (Kommazahl) | `1.0` | 0.4 – 2.5 | – | `nametags.nameScale` |
| 8 | Account-Schriftgröße | Zahlenfeld (Kommazahl) | `0.8` | 0.4 – 2.5 | – | `nametags.accountScale` |
| 9 | Zeilenabstand | Zahlenfeld (ganze Zahl) | `10` | 4 – 30 | – | `nametags.lineSpacing` |
| 10 | Account-Farbe (#RRGGBB) | Farbfeld (Hex `#RRGGBB`) | `#666666` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `nametags.accountColor` |
