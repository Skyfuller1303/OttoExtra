# Tierinfo

- Quelle: Addon `OttoExtra-AnimalBreeding` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Besonderheit: Das Addon legt keinen eigenen Reiter an, sondern hängt seine Karte an den Tab „Basis“ des vorhandenen Moduls „Tierinfo“ an.
- Modul-ID im Code: `animalinfo`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Zeigt Geschlecht und Paarungsbereitschaft anvisierter Zuchttiere kompakt im HUD.
- Umfang: 1 Tab(s), 1 Karte(n), 1 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Züchtermodus“

Kartenbeschreibung im GUI: Aktiviert Zuchtakten, Hofabgleich und Herkunfts-Autofill. Die Serverfreigabe bleibt zusätzlich erforderlich.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Ich züchte Tiere | Schalter (An/Aus) | Aus |  | Standardmäßig aus. Autofill wird nur mit aktiver Einstellung und serverseitiger Züchterfreigabe freigeschaltet. | `animalInfo.breederModeEnabled` |
