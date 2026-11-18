# Regionen

- Position: Reiter 4 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `regions`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Einblendung beim Betreten von Lehen und Regionen.
- Umfang: 2 Tab(s), 3 Karte(n), 31 Einträge

## Tab „Basis“

### Karte „Regions-Anzeige“

Kartenbeschreibung im GUI: Banner-Einblendung mit Wappen, Name und Hierarchie.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Regionen-Modul | Schalter (An/Aus) | An |  | – | `regions.enabled` |
| 2 | Actionbar ausblenden | Schalter (An/Aus) | An |  | – | `regions.hideOriginalActionbar` |
| 3 | Betreten-Sound | Schalter (An/Aus) | An |  | – | `regions.playEnterSound` |
| 4 | Wappen im Toast | Schalter (An/Aus) | An |  | – | `regions.showBanner` |
| 5 | Herrschaft im Toast | Schalter (An/Aus) | An |  | – | `regions.showFaction` |
| 6 | Lehnsherr im Toast | Schalter (An/Aus) | An |  | – | `regions.showLeader` |
| 7 | RP-Name des Lehnsherrn | Schalter (An/Aus) | An |  | Zeigt einen lokal bekannten RP-Namen. Ohne bekannten RP-Namen bleibt der Spielername sichtbar. Standard: an. | `regions.showLeaderRpName` |
| 8 | Koordinaten im Toast | Schalter (An/Aus) | Aus |  | – | `regions.showCoordinates` |
| 9 | Position | Auswahl (Klick schaltet weiter) | `TOP_CENTER` | `TOP_CENTER`<br>`TOP_RIGHT`<br>`TOP_LEFT`<br>`CENTER` | – | `regions.overlayPosition` |
| 10 | Banner-Stil | Auswahl (Klick schaltet weiter) | `classic` | `classic`<br>`zonentitel`<br>`banderole`<br>`plakette`<br>`minimal` | – | `regions.zoneBannerStyle` |
| 11 | Toast-Themes | Schaltfläche | – | Text auf der Schaltfläche: „Themes verwalten…“ | – | `regions.themes` |
| 12 | Kartendaten neu laden | Schaltfläche | – | Text auf der Schaltfläche: „Neu laden“ | – | `regions.refreshData` |
| 13 | Vorschau anzeigen | Schaltfläche | – | Text auf der Schaltfläche: „Vorschau“ | – | `regions.preview` |

## Tab „Erweitert“

### Karte „Badge-Layout“

Kartenbeschreibung im GUI: Maße des Region-Toasts: Breiten, Rand, Icon, Innenabstände. Vorschau-Knopf zeigt es live.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 14 | Hinweiszeile anzeigen | Schalter (An/Aus) | Aus |  | – | `regions.hintTextEnabled` |
| 15 | Max. Textbreite | Zahlenfeld (ganze Zahl) | `210` | 50 – 800 | – | `regions.maxTextWidth` |
| 16 | Min. Toastbreite | Zahlenfeld (ganze Zahl) | `170` | 50 – 800 | – | `regions.minToastWidth` |
| 17 | Max. Toastbreite | Zahlenfeld (ganze Zahl) | `330` | 50 – 1000 | – | `regions.maxToastWidth` |
| 18 | Abstand oben | Zahlenfeld (ganze Zahl) | `6` | 0 – 200 | – | `regions.screenTopMargin` |
| 19 | Wappengröße | Zahlenfeld (ganze Zahl) | `24` | 8 – 64 | – | `regions.iconSize` |
| 20 | Wappenabstand | Zahlenfeld (ganze Zahl) | `6` | 0 – 32 | – | `regions.iconGap` |
| 21 | Innenabstand links | Zahlenfeld (ganze Zahl) | `8` | 0 – 40 | – | `regions.paddingLeft` |
| 22 | Innenabstand rechts | Zahlenfeld (ganze Zahl) | `10` | 0 – 40 | – | `regions.paddingRight` |
| 23 | Innenabstand oben | Zahlenfeld (ganze Zahl) | `6` | 0 – 40 | – | `regions.paddingTop` |
| 24 | Innenabstand unten | Zahlenfeld (ganze Zahl) | `6` | 0 – 40 | – | `regions.paddingBottom` |
| 25 | Toast-Dauer (ms) | Zahlenfeld (ganze Zahl) | `5200` | 1500 – 15000 | – | `regions.displayDurationMs` |
| 26 | Vorschau anzeigen | Schaltfläche | – | Text auf der Schaltfläche: „Vorschau“ | – | `regions.preview` |

### Karte „Badge-Schrift“

Kartenbeschreibung im GUI: Schriftgrößen im Toast (Titel, Region, Hierarchie, Hinweis).

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 27 | Schrift: Basis | Zahlenfeld (Kommazahl) | `1.0` | 0.2 – 3.0 | – | `regions.baseTextScale` |
| 28 | Schrift: Titel | Zahlenfeld (Kommazahl) | `0.65` | 0.2 – 3.0 | – | `regions.titleScale` |
| 29 | Schrift: Region | Zahlenfeld (Kommazahl) | `1.0` | 0.2 – 3.0 | – | `regions.regionScale` |
| 30 | Schrift: Hierarchie | Zahlenfeld (Kommazahl) | `0.68` | 0.2 – 3.0 | – | `regions.hierarchyScale` |
| 31 | Schrift: Hinweis | Zahlenfeld (Kommazahl) | `0.35` | 0.1 – 3.0 | – | `regions.hintScale` |
