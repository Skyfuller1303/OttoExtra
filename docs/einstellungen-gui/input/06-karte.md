# Karte

- Position: Reiter 6 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `map`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Gemalte Karte, Lehensgrenzen und politisches Overlay für Xaero.
- Umfang: 3 Tab(s), 6 Karte(n), 49 Einträge

## Tab „Basis“

### Karte „Weltkarte“

Kartenbeschreibung im GUI: Was auf der großen Xaero-Weltkarte gezeichnet wird.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Karten-Modul | Schalter (An/Aus) | An |  | – | `map.enabled` |
| 2 | Lehengrenzen | Schalter (An/Aus) | An |  | – | `map.showBorders` |
| 3 | Regionsnamen | Schalter (An/Aus) | An |  | – | `map.showNames` |
| 4 | Wappen | Schalter (An/Aus) | An |  | – | `map.showBanners` |
| 5 | Gemalte Karte | Schalter (An/Aus) | An |  | – | `map.paintedMap` |
| 6 | Politische Karte | Schalter (An/Aus) | An |  | – | `map.politicalFill` |
| 7 | Vollflächige Karte beim Rauszoomen | Schalter (An/Aus) | An |  | Blendet beim Rauszoomen Xaeros Terrain aus und zeigt die gemalte Karte überall zu 100% — sobald nur noch die Wappen der obersten Lehnsherren zu sehen sind. Nur bei aktiver politischer Karte. | `map.paintedFullCoverZoomOut` |
| 8 | Pergament-Kartenmodus | Schalter (An/Aus) | An |  | – | `map.parchmentMode` |
| 9 | Lehen-Infokarte per Klick | Schalter (An/Aus) | An |  | – | `map.clickInfoPanel` |
| 10 | Spieler-Aktivität | Schalter (An/Aus) | An |  | – | `map.showActivity` |
| 11 | NPC-Dörfer anzeigen | Schalter (An/Aus) | An |  | – | `map.showNpcVillages` |
| 12 | Nur auf Ottonien | Schalter (An/Aus) | An |  | An: Overlays nur auf dem Ottonien-Server. Aus: überall (z. B. Singleplayer-Tests). | `map.onlyOnOttonien` |
| 13 | Gefolge-Liste… | Schaltfläche | – | Text auf der Schaltfläche: „Gefolge…“ | – | `map.following` |

### Karte „HUD-Layout“

Kartenbeschreibung im GUI: Minimap, Aktivitäts-HUD, Lehen-Overlay und Addon-Anzeigen gemeinsam anordnen (auch: /ottoextra hud).

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 14 | HUD anordnen | Schaltfläche | – | Text auf der Schaltfläche: „⚠ kein Text für `ottoextra.hudlayout.open.button`“ | *Hinweis: Wird nachträglich von `HudLayoutSettings.register` an den ersten Tab des Karten-Moduls angehängt.* | `hudlayout.open` |

## Tab „Minimap“

### Karte „Minimap-Overlays“

Kartenbeschreibung im GUI: Grenzen, gemalte Karte und Flächen in der runden/eckigen Minimap.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 15 | Grenzen auf Minimap | Schalter (An/Aus) | An |  | – | `map.minimapBorders` |
| 16 | Gemalte Karte auf Minimap | Schalter (An/Aus) | An |  | – | `map.minimapPainted` |
| 17 | Politische Flächen auf Minimap | Schalter (An/Aus) | Aus |  | – | `map.minimapPolitical` |
| 18 | Öffnungsanimation der Weltkarte | Schalter (An/Aus) | An |  | – | `map.worldMapOpenAnimation` |

### Karte „Wappen-HUD“

Kartenbeschreibung im GUI: Wappen, Gefolgename und Stand neben der Minimap — Position per Drag&Drop.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 19 | Wappen an Minimap | Schalter (An/Aus) | An |  | – | `map.minimapBanner` |
| 20 | Wappen: Lehensname | Schalter (An/Aus) | Aus |  | – | `map.minimapBannerShowName` |
| 21 | Wappen: Stand/Hierarchie | Schalter (An/Aus) | An |  | – | `map.minimapBannerShowState` |
| 22 | Gefolgename anzeigen | Schalter (An/Aus) | An |  | Zusätzliche Textzeile mit dem Namen des Gefolges (Faction), das das Lehen hält — eigenes Drag&Drop-Element. | `map.minimapBannerShowFaction` |
| 23 | Obersten Lehnsherrn zeigen | Schalter (An/Aus) | An |  | Aus: direkter Lehnsherr. An: oberster Lehnsherr der Vasallenkette (z. B. statt Mayenburg → Holdstewik). | `map.minimapLiegeTop` |
| 24 | Wappen-Position (Drag&Drop)… | Schaltfläche | – | Text auf der Schaltfläche: „Anordnen…“ | – | `map.bannerEdit` |
| 25 | Gefolge-Farben… | Schaltfläche | – | Text auf der Schaltfläche: „Farben…“ | – | `map.groupColors` |

## Tab „Erweitert“

### Karte „Darstellung“

Kartenbeschreibung im GUI: Farben, Linien und Zoom-Schwellen der Kartenelemente.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 26 | Grenzfarbe (#AARRGGBB) | Farbfeld (Hex `#RRGGBB`) | `#493C30` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `map.borderColor` |
| 27 | Strichgrafik | Schalter (An/Aus) | An |  | – | `map.dashedBorders` |
| 28 | Namen ab Zoom | Zahlenfeld (Kommazahl) | `0.05` | 0.005 – 4 | – | `map.nameMinScale` |
| 29 | Wappen ab Zoom | Zahlenfeld (Kommazahl) | `0.05` | 0.005 – 8 | – | `map.bannerMinScale` |
| 30 | Politische Karte bis Zoom | Zahlenfeld (Kommazahl) | `0.6` | 0.01 – 8 | – | `map.politicalMaxScale` |
| 31 | Pergament-Rand (px) | Zahlenfeld (ganze Zahl) | `5` | 2 – 30 | – | `map.parchmentMarginPx` |
| 32 | Reisehilfe: Fußtempo (Blöcke/s) | Zahlenfeld (Kommazahl) | `4.3` | 0.5 – 20.0 | – | `map.walkBlocksPerSecond` |
| 33 | Reisehilfe: Pferdetempo (Blöcke/s) | Zahlenfeld (Kommazahl) | `8.5` | 0.5 – 30.0 | – | `map.horseBlocksPerSecond` |
| 34 | Politik-Deckkraft | Schieberegler (Prozent) | 90 % | 0 – 100 % | Deckkraft der politischen Flächen tagsüber (0-100%). | `map.politicalOpacity` |
| 35 | Politik-Deckkraft (Nacht) | Schieberegler (Prozent) | 60 % | 0 – 100 % | Deckkraft nachts — automatisch weniger, weil die Karte dunkler ist (0-100%). | `map.politicalOpacityNight` |
| 36 | Kalibrier-Pfeile (Karte verschieben) | Schalter (An/Aus) | Aus |  | Debug-Pfeile zum Verschieben der gemalten Karte auf der Worldmap. Standard: aus. | `map.showCalibrationArrows` |
| 37 | Gemalte Karte: einfacher Modus | Schalter (An/Aus) | Aus |  | Rendert die gemalte Karte ohne Spezial-Shader. Aktivieren, falls die Karte schwarz bleibt (manche Modpacks). Ohne Tag/Nacht-Blending. | `map.paintedMapSimple` |

### Karte „HUD-Feinjustierung“

Kartenbeschreibung im GUI: Breite, Höhe, Schriftgröße und Farben der HUD-Texte.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 38 | HUD Name: Breite | Zahlenfeld (ganze Zahl) | `46` | 20 – 400 | – | `map.nameHudWidth` |
| 39 | HUD Stand: Breite | Zahlenfeld (ganze Zahl) | `91` | 20 – 400 | – | `map.stateHudWidth` |
| 40 | HUD Name: Höhe | Zahlenfeld (ganze Zahl) | `0` | 0 – 160 | Höhe in GUI-Pixeln. 0 behält eine Zeile. Mehr Höhe erlaubt automatische Zeilenumbrüche. | `map.nameHudHeight` |
| 41 | HUD Stand: Höhe | Zahlenfeld (ganze Zahl) | `0` | 0 – 160 | Höhe in GUI-Pixeln. 0 behält eine Zeile. Mehr Höhe erlaubt automatische Zeilenumbrüche. | `map.stateHudHeight` |
| 42 | HUD Name: Skalierung | Zahlenfeld (Kommazahl) | `0.52` | 0.5 – 3 | – | `map.nameHudScale` |
| 43 | HUD Stand: Skalierung | Zahlenfeld (Kommazahl) | `0.52` | 0.5 – 3 | – | `map.stateHudScale` |
| 44 | HUD Name: Farbe (#RRGGBB) | Farbfeld (Hex `#RRGGBB`) | `#E6C8A9` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `map.nameHudColor` |
| 45 | HUD Stand: Farbe (#RRGGBB) | Farbfeld (Hex `#RRGGBB`) | `#B8A88F` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `map.stateHudColor` |
| 46 | HUD Gefolge: Breite | Zahlenfeld (ganze Zahl) | `89` | 20 – 400 | – | `map.factionHudWidth` |
| 47 | HUD Gefolge: Höhe | Zahlenfeld (ganze Zahl) | `0` | 0 – 160 | Höhe in GUI-Pixeln. 0 behält eine Zeile. Mehr Höhe erlaubt automatische Zeilenumbrüche. | `map.factionHudHeight` |
| 48 | HUD Gefolge: Skalierung | Zahlenfeld (Kommazahl) | `0.97` | 0.5 – 3 | – | `map.factionHudScale` |
| 49 | HUD Gefolge: Farbe (#RRGGBB) | Farbfeld (Hex `#RRGGBB`) | `#FFFFFF` | Hex `#RRGGBB`; leeres Feld erlaubt | – | `map.factionHudColor` |
