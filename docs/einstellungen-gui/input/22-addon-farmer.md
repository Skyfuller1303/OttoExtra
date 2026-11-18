# Farmer

- Quelle: Addon `OttoExtra-Farmer` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `farmer`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Hilfen für Füttern und automatisch geführtes Bepflanzen.
- Umfang: 1 Tab(s), 1 Karte(n), 4 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Farmer-Assistenten“

Kartenbeschreibung im GUI: Client-Hilfen mit begrenzter Blicksteuerung, aber ohne Laufbewegung.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Fütterhilfe aktivieren | Schalter (An/Aus) | An |  | – | `farmer.feedAssistant` |
| 2 | Pflanzhilfe aktivieren | Schalter (An/Aus) | An |  | – | `farmer.plantingAssistant` |
| 3 | Schlangenlinien-Modus | Schalter (An/Aus) | An |  | An: zusammenhängende Reihen abwechselnd bepflanzen. Aus: immer nächstes Farmland vor dir wählen. | `farmer.plantingSerpentine` |
| 4 | Statusmeldungen anzeigen | Schalter (An/Aus) | An |  | – | `farmer.feedback` |
