# RP-Namen

- Position: Reiter 7 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `rpnames`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Lokales RP-Namen-System: Chat, Tabliste und Personenbuch.
- Umfang: 1 Tab(s), 7 Karte(n), 28 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Personenbuch“

Kartenbeschreibung im GUI: Alle bekannten Personen, Titel und Farben verwalten.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Personen verwalten… | Schaltfläche | – | Text auf der Schaltfläche: „Öffnen…“ | – | `rpnames.people` |

### Karte „Chat“

Kartenbeschreibung im GUI: RP-Namen im Chat; Unbekannte als Platzhalter oder Accountname.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 2 | RP-Namen-Modul | Schalter (An/Aus) | An |  | – | `rpnames.enabled` |
| 3 | RP-Namen in OOC-Chats (Offtopic/Hilfe) | Schalter (An/Aus) | Aus |  | RP-Namen auch in den OOC-Kanälen Offtopic und Hilfe anzeigen. Standard: aus.<br>*Hinweis: Setzt zwei Config-Felder gleichzeitig: `rpnames.showInOfftopic` und `rpnames.showInHilfe`.* | `rpnames.showInOocChats` |
| 4 | Unbekannte als „Unbekannt“ zeigen | Schalter (An/Aus) | An |  | – | `rpnames.showUnknownAsUnknown` |
| 5 | Unbekannte: Accountname zeigen | Schalter (An/Aus) | Aus |  | – | `rpnames.unknownShowAccount` |
| 6 | Platzhalter | Auswahl (Klick schaltet weiter) | `Unbekannt` | `Unbekannt`<br>`???` | – | `rpnames.unknownPlaceholder` |

### Karte „Namensfarben“

Kartenbeschreibung im GUI: Globale Farbe für Spieler- und RP-Namen (leer = Standard). Pro Titel im Personenbuch überschreibbar.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 7 | Spielername-Farbe | Farbfeld (Hex `#RRGGBB`) | (leer) | Hex `#RRGGBB`; leeres Feld erlaubt | Globale Farbe für Spieler-/Accountnamen (#RRGGBB, leer = Standard). | `rpnames.globalPlayerNameColor` |
| 8 | RP-Name-Farbe | Farbfeld (Hex `#RRGGBB`) | (leer) | Hex `#RRGGBB`; leeres Feld erlaubt | Globale Farbe für RP-Namen (#RRGGBB, leer = Standard). | `rpnames.globalRpNameColor` |
| 9 | Adelsfarbe auch für andere Titel | Schalter (An/Aus) | Aus |  | Standardmäßig aus: Der angezeigte Titel bestimmt seine Farbe; nur Adelstitel sind adelsfarben. An: Bei adligen Personen färbt der Adelsstand auch andere Titel. Feste Titelfarben und persönliche Farbanpassungen behalten Vorrang. | `rpnames.nobilityColorForAllTitles` |

### Karte „Schnellzugriff“

Kartenbeschreibung im GUI: Shift-Linksklick auf einen Namen im Chat oder Shift-Rechtsklick auf einen Spieler öffnet das Personenbuch beim Eintrag der Person.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 10 | Shift-Klick öffnet Personenbuch | Schalter (An/Aus) | Aus |  | Shift-Linksklick im Chat / Shift-Rechtsklick auf einen Spieler. Bei einer Bossbar mit „IM KAMPF“ ist der Rechtsklick-Schnellzugriff gesperrt. Standard: aus. | `rpnames.openBookOnClick` |
| 11 | Namen kennenlernen (proaktiv) | Schalter (An/Aus) | Aus |  | Unbekannte RP-Sprecher bekommen ein „!“ über dem Kopf. Linksklick auf „Unbekannt“ im Chat oder Shift-Rechtsklick auf den Spieler öffnet das Kennenlernfenster. Bei „IM KAMPF“ bleibt es geschlossen. Standard: an. | `rpnames.proactiveMeet` |

### Karte „RP-Untersuchungsmodus“

Kartenbeschreibung im GUI: Rollenspielgerechte Lupe mit sanftem Zoom, Randunschärfe und verzögerter Untersuchung.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 12 | RP-Untersuchungsmodus aktivieren | Schalter (An/Aus) | An |  | Deaktiviert den gesamten Untersuchungsmodus einschließlich ALT-Taste, Lupe, Texten, Zoom und Randunschärfe. | `rpnames.inspectEnabled` |
| 13 | Sichtbare Gegenstände in den Händen | Schalter (An/Aus) | An |  | Zeigt nach der Untersuchung die offen sichtbaren Gegenstände in Haupt- und Nebenhand eines Spielers. Keine versteckten Itemwerte werden gelesen. | `rpnames.inspectShowPlayerHands` |
| 14 | Sichtbare Rüstung | Schalter (An/Aus) | An |  | Zeigt nach der Untersuchung die offen sichtbaren Rüstungsteile eines Spielers. | `rpnames.inspectShowPlayerArmor` |
| 15 | Sanfter Zoom | Schalter (An/Aus) | An |  | – | `rpnames.inspectZoomEnabled` |
| 16 | Zoomstärke | Zahlenfeld (Kommazahl) | `9.0` | 3.0 – 18.0 | Wie stark beim Untersuchen in die Blickrichtung gezoomt wird. Zusätzliche Zoom-Tasten anderer Mods bleiben nutzbar. | `rpnames.inspectZoomDegrees` |
| 17 | Rand leicht weichzeichnen | Schalter (An/Aus) | An |  | – | `rpnames.inspectEdgeBlurEnabled` |
| 18 | Wechselnde RP-Gedanken | Schalter (An/Aus) | An |  | Zeigt beim Untersuchen einen zufällig gewählten inneren RP-Satz an. Kann vollständig deaktiviert werden. | `rpnames.inspectRoleplayPhrasesEnabled` |
| 19 | Dauer des RP-Gedankens | Zahlenfeld (Kommazahl) | `2.0` | 0.5 – 2.0 | Wie lange der RP-Satz sichtbar bleibt. Maximal zwei Sekunden und niemals länger als die gesamte Untersuchung. | `rpnames.inspectRoleplayPhraseSeconds` |
| 20 | Untersuchungsdauer | Zahlenfeld (Kommazahl) | `2.0` | 0.5 – 2.0 | Bestimmt, wie lange ein Ziel gemustert wird. Die Untersuchung dauert höchstens zwei Sekunden. | `rpnames.inspectRevealSeconds` |

### Karte „Kennenlern-Marker (3D)“

Kartenbeschreibung im GUI: Aussehen des 3D-Ausrufezeichens über unbekannten RP-Sprechern.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 21 | Leuchten (Fullbright) | Schalter (An/Aus) | An |  | Marker leuchtet unabhängig vom Licht. Standard: an. | `rpnames.meetMarkerGlow` |
| 22 | Höhe über Kopf | Zahlenfeld (Kommazahl) | `0.55` | -1.0 – 3.0 | Zusatzhöhe über dem Kopf in Blöcken. Standard: 0.55. | `rpnames.meetMarkerHeight` |
| 23 | Größe | Zahlenfeld (Kommazahl) | `1.0` | 0.1 – 5.0 | Größenfaktor des Markers. Standard: 1.0. | `rpnames.meetMarkerSize` |
| 24 | Drehtempo (Y-Achse) | Zahlenfeld (Kommazahl) | `1.0` | 0.0 – 10.0 | Drehgeschwindigkeit um die Y-Achse. 0 = stillstehend. Standard: 1.0. | `rpnames.meetMarkerSpinSpeed` |

### Karte „Tabliste“

Kartenbeschreibung im GUI: RP-Namen, Titel und Sortierung in der Spielerliste.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 25 | RP-Namen in Tabliste | Schalter (An/Aus) | Aus |  | – | `rpnames.tablistEnabled` |
| 26 | Tabliste nach Lehen sortieren | Schalter (An/Aus) | Aus |  | Gruppiert Online-Spieler nach Lehen. Innerhalb eines Lehens folgen Leitung, Titelhierarchie und Name. Server-Wappen bleiben unverändert. | `rpnames.tablistSortByRegion` |
| 27 | Titel in Tabliste anzeigen | Schalter (An/Aus) | An |  | Blendet vorhandene Titel für alle Spieler in der Tabliste ein oder aus – unabhängig davon, ob die Person bekannt ist. Chat und Namensschilder bleiben davon unberührt. | `rpnames.tablistShowTitle` |
| 28 | Titel automatisch aktuell halten | Schalter (An/Aus) | An |  | Übernimmt regelmäßig den aktuellen Servertitel in das Personenbuch. Die Anzeige kann unabhängig davon ausgeschaltet werden. | `rpnames.tablistTitlesAlways` |
