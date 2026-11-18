# Discord

- Quelle: Addon `OttoExtra-Discord` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `discord`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Accountgebundene Brücke zwischen Minecraft und Discord.
- Umfang: 1 Tab(s), 2 Karte(n), 6 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Discord-Brücke“

Kartenbeschreibung im GUI: Steuert übertragene Chat- und Statusereignisse.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Discord-Brücke aktiv | Schalter (An/Aus) | An |  | – | `discord.enabled` |
| 2 | Eigene Chatnachrichten übertragen | Schalter (An/Aus) | An |  | – | `discord.relayChat` |
| 3 | Verbindungsstatus übertragen | Schalter (An/Aus) | An |  | – | `discord.relayStatus` |
| 4 | Statusmeldungen anzeigen | Schalter (An/Aus) | An |  | – | `discord.feedback` |

### Karte „Endpunkt“

Kartenbeschreibung im GUI: Verbindung zum OttoExtra-Discord-Webendpoint.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 5 | API-Endpunkt | Textfeld | `https://ottoextra.dev` |  | Öffentliche HTTPS-Basis-URL ohne /api/v1. HTTP ist nur lokal erlaubt. | `discord.endpoint` |
| 6 | Pollintervall (Sekunden) | Zahlenfeld (ganze Zahl) | `3` | 1 – 60 | – | `discord.pollInterval` |
