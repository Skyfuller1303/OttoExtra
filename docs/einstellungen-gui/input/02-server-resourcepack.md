# Server-Resourcepack

- Position: Reiter 2 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `resourcepack`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Hält das Ottonien-Resourcepack automatisch aktuell.
- Umfang: 2 Tab(s), 2 Karte(n), 7 Einträge

## Tab „Basis“

### Karte „Automatik“

Kartenbeschreibung im GUI: Downloads brauchen eigene Freigabe. Automatisches Aktivieren gilt nur bei Erstinstallation; deaktivierte Packs bleiben aus.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Resourcepack-Download | Schalter (An/Aus) | An |  | – | `resourcepack.enabled` |
| 2 | Pack auto-aktivieren | Schalter (An/Aus) | An |  | – | `resourcepack.autoEnable` |
| 3 | Beim Start prüfen | Schalter (An/Aus) | An |  | Ein in Minecraft deaktiviertes Pack bleibt auch nach Updates deaktiviert. Erneut aktivieren: Minecraft → Ressourcenpakete. | `resourcepack.checkOnStartup` |

## Tab „Erweitert“

### Karte „Quelle & Netzwerk“

Kartenbeschreibung im GUI: Woher das Pack kommt und wie lange Downloads warten dürfen.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 4 | Manifest-/Release-URL | Textfeld | `https://api.github.com/repos/Ottonien/ottonien-reformed/releases/latest` |  | – | `resourcepack.manifestUrl` |
| 5 | Asset-Name | Textfeld | `Ottonien.zip` |  | – | `resourcepack.assetName` |
| 6 | Connect-Timeout (ms) | Zahlenfeld (ganze Zahl) | `10000` | 1000 – 120000 | – | `resourcepack.connectTimeoutMs` |
| 7 | Request-Timeout (ms) | Zahlenfeld (ganze Zahl) | `60000` | 1000 – 300000 | – | `resourcepack.requestTimeoutMs` |
