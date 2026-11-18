# Vote Reminder

- Quelle: Addon `OttoExtra-Vote` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `vote`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Erinnert täglich an die vier Ottonien-Vote-Seiten.
- Umfang: 1 Tab(s), 1 Karte(n), 5 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Tägliche Erinnerung“

Kartenbeschreibung im GUI: Reminder auf dem konfigurierten Ottonien-Server; Reset nach Berliner Ortszeit.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Vote-Reminder aktivieren | Schalter (An/Aus) | An |  | – | `vote.enabled` |
| 2 | Resetstunde (Europe/Berlin) | Zahlenfeld (ganze Zahl) | `4` | 0 – 23 | – | `vote.resetHour` |
| 3 | Verzögerung nach Join (Sekunden) | Zahlenfeld (ganze Zahl) | `5` | 0 – 60 | – | `vote.joinDelay` |
| 4 | Anzeigedauer (Sekunden) | Zahlenfeld (ganze Zahl) | `10` | 2 – 60 | – | `vote.toastDuration` |
| 5 | Wiederholung (Minuten) | Zahlenfeld (ganze Zahl) | `30` | 1 – 1440 | – | `vote.reminderInterval` |
