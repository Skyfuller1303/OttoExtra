# Chatkanäle

- Position: Reiter 3 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `chat`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Kanal-Anzeige im Chat und Komfortfunktionen.
- Umfang: 2 Tab(s), 6 Karte(n), 16 Einträge

## Tab „Basis“

### Karte „Kanäle“

Kartenbeschreibung im GUI: Aktiver Kanal wird links neben der Eingabe angezeigt; Klick wechselt.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Chat-Modul | Schalter (An/Aus) | An |  | – | `chat.enabled` |
| 2 | Beim Join automatisch /s (Sprechen) | Schalter (An/Aus) | An |  | Sendet beim Betreten des Servers automatisch /s, damit Sprechen der Standardkanal ist. | `chat.autoSprechenOnJoin` |
| 3 | Shift+Tab wechselt Kanal | Schalter (An/Aus) | An |  | Shift+Tab im Chat wechselt den Kanal (statt Vanilla-Autovervollständigung der Spielernamen). Standard: an. | `chat.shiftTabCycleChannels` |

### Karte „Offtopic-Warnung“

Kartenbeschreibung im GUI: Blinkende Ausrufezeichen vor [Offtopic] als Erinnerung: öffentlicher Kanal.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 4 | Offtopic-Warnzeichen (!) | Schalter (An/Aus) | Aus |  | – | `chat.offtopicBangEnabled` |
| 5 | Anzahl ! | Zahlenfeld (ganze Zahl) | `3` | 1 – 3 | – | `chat.offtopicBangCount` |

### Karte „Lange Nachrichten“

Kartenbeschreibung im GUI: Einzeilig unbegrenzt tippen; beim Senden automatisch in mehrere Teilnachrichten splitten.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 6 | Lange Nachrichten splitten | Schalter (An/Aus) | An |  | Lange Eingaben an Wortgrenzen teilen und gestaffelt senden, sodass sie zusammen wie eine große Nachricht wirken. Standard: an. | `chat.longChatEnabled` |
| 7 | Max. Zeichen je Teil | Zahlenfeld (ganze Zahl) | `256` | 64 – 256 | – | `chat.longChatChunk` |
| 8 | Max. Tipplänge | Zahlenfeld (ganze Zahl) | `8192` | 256 – 32500 | – | `chat.longChatMaxInput` |
| 9 | Fortsetzungs-Marker | Textfeld | ` >` |  | – | `chat.longChatMarker` |

### Karte „RP-Chatformatierung“

Kartenbeschreibung im GUI: Hebt Emotes und OOC-Kommentare in RP-Kanälen hervor; normale Kanaltexte können getrennt eingefärbt werden.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 10 | Emotes und OOC farblich darstellen | Schalter (An/Aus) | An |  | Text zwischen *Sternen* wird kursiv; Text in (runden Klammern) wird als OOC markiert. Diese Stile haben Vorrang vor einer eingestellten Nachrichtenfarbe. Standard: an. | `chat.rpFormattingEnabled` |
| 11 | Farbe: *Emotes* | Farbfeld (Hex `#RRGGBB`) | `#C6C6C6` | Hex `#RRGGBB`; leeres Feld erlaubt | Farbe für Text zwischen *Sternen*. Kursiv kann separat eingestellt werden. Standard: #C6C6C6. | `chat.rpEmoteColor` |
| 12 | Farbe: (OOC-Kommentare) | Farbfeld (Hex `#RRGGBB`) | `#B4BEC6` | Hex `#RRGGBB`; leeres Feld erlaubt | Farbe für Text in (runden Klammern). Standard: #B4BEC6. | `chat.rpOocColor` |
| 13 | *Emotes* kursiv | Schalter (An/Aus) | An |  | Legt fest, ob Emote-Passagen zusätzlich zur gewählten Farbe kursiv erscheinen. | `chat.rpEmoteItalic` |
| 14 | (OOC) kursiv | Schalter (An/Aus) | Aus |  | Legt fest, ob OOC-Passagen zusätzlich zur gewählten Farbe kursiv erscheinen. | `chat.rpOocItalic` |

## Tab „Erweitert“

### Karte „Chatkanal-Farben“

Kartenbeschreibung im GUI: Konkrete Nachrichtenfarben je Kanal sowie globale NPC-, Emote- und OOC-Farben. Leere Chatnamenfelder behalten den Serverstil.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 15 | Chatkanal-Farben verwalten… | Schaltfläche | – | Text auf der Schaltfläche: „Öffnen…“ | – | `chat.channelColors` |

### Karte „Lange Nachrichten“

Kartenbeschreibung im GUI: Einzeilig unbegrenzt tippen; beim Senden automatisch in mehrere Teilnachrichten splitten.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 16 | Absende-Intervall (ms) | Zahlenfeld (ganze Zahl) | `800` | 500 – 1500 | Zeit zwischen den Teilnachrichten in Millisekunden (500–1500). Standard: 800. | `chat.longChatDelayMs` |
