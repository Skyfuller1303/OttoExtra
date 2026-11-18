# OttoSkript

- Quelle: Addon `OttoExtra-Skript` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `skript`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): MD-Skripte für Messen, Reden und Ankündigungen mit Editor, HUD und Countdown.
- Umfang: 1 Tab(s), 2 Karte(n), 4 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Skripte und Vorschau“

Kartenbeschreibung im GUI: MD-Skripte für Messen, Reden und Ankündigungen mit Editor, HUD und Countdown.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Bibliothek öffnen | Schaltfläche | – | Text auf der Schaltfläche: „Öffnen“ | – | `skript.open` |
| 2 | HUD positionieren | Schaltfläche | – | Text auf der Schaltfläche: „Positionieren“ | – | `skript.hud` |
| 3 | Skriptbutton im Chat anzeigen | Schalter (An/Aus) | An |  | Zeigt rechts in der Chateingabe einen S-Button zur Skriptauswahl, neben der Übersetzungsanzeige. | `skript.chatButtonVisible` |

### Karte „OttoSkript aktiviert“

Kartenbeschreibung im GUI: Schaltet HUD, Chatbutton, Tasten und automatischen Versand gemeinsam aus. Skripte und Fortschritt bleiben gespeichert.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 4 | OttoSkript aktiviert | Schalter (An/Aus) | An |  | Schaltet HUD, Chatbutton, Tasten und automatischen Versand gemeinsam aus. Skripte und Fortschritt bleiben gespeichert. | `skript.enabled` |
