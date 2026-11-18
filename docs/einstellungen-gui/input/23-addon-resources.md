# Ressourcen

- Quelle: Addon `OttoExtra-Resources` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `resources`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Zonen, Hinweise und Synchronisierung regenerierender Ressourcen.
- Umfang: 1 Tab(s), 2 Karte(n), 5 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Ressourcen-Hinweise“

Kartenbeschreibung im GUI: Steuert lokale Erkennung und API-Cache.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Ressourcenmodul aktivieren | Schalter (An/Aus) | An |  | – | `resources.enabled` |
| 2 | Hinweise beim Abbau anzeigen | Schalter (An/Aus) | An |  | – | `resources.hintsEnabled` |
| 3 | Synchronisierungsradius | Zahlenfeld (ganze Zahl) | `192` | 32 – 256 | – | `resources.nearbyRadius` |
| 4 | Cache-Dauer in Sekunden | Zahlenfeld (ganze Zahl) | `45` | 30 – 60 | – | `resources.cacheSeconds` |

### Karte „Minimap“

Kartenbeschreibung im GUI: Statuspunkt: Grün = bereit, Gelb = abbaubar, Rot = Regeneration.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 5 | Statuspunkt auf der Minimap anzeigen | Schalter (An/Aus) | Aus |  | – | `resources.showMinimapStatusIndicator` |
