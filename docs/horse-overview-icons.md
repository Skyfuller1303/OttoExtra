# Pferdeübersicht-Icons

Die austauschbaren Icons liegen unter:

`src/main/resources/assets/ottoextra/textures/gui/horse_overview/`

Vorgabe für alle Dateien:

- exakt **16 × 16 Pixel**
- PNG mit transparentem Hintergrund (RGBA)
- harte Pixelkanten, keine Kantenglättung
- Motiv weiß; OttoExtra färbt es abhängig von Zustand und Geschlecht ein
- Dateinamen unverändert lassen

Ab Alpha F stellt die [Tierübersicht](animal-overview.md) diese Icons mit
**8 × 8 GUI-Pixeln** dar, passend zur Höhe der normalen Schrift. Die größeren
Klickflächen bleiben bestehen. Hof- und Herkunftsnamen verwenden 75 %
Schriftgröße. Die Tierakte behält ihre bisherige Symbolgröße.

Dateien:

- `finder.png`
- `delete.png`
- `gender_mare.png`
- `gender_stallion.png`
- `gender_unknown.png`
- `speed.png`
- `jump.png`
- `sort_ascending.png` – aufsteigend
- `sort_descending.png` – absteigend
- `sort_none.png` – Spalte aktuell nicht sortiert
- `expand.png` – eingeklappter Hof, Klick öffnet ihn
- `collapse.png` – aufgeklappter Hof, Klick klappt ihn ein
- `feeding.png` – Mast im Tierinfo-Toast
- `breed_ready.png` – Paarungsbereitschaft im Tierinfo-Toast
- `shearable.png` – Scheren im Tierinfo-Toast
- `milkable.png` – Melken im Tierinfo-Toast

Geschlecht, Tempo und Sprung im Tierregister verwenden dieselben Icons wie die
Übersicht. Mast, Paarung, Scheren und Melken verwenden die vorhandenen Symbole aus
`assets/mattis/textures/font/icons/clean/status/` (8 × 8 Pixel).

Der **Tierinfo-Toast** verwendet ab Alpha D auch für Mast, Paarung, Scheren und
Melken die oben genannten 16 × 16-PNGs. Geschlecht, Tempo und Sprung verwenden
die gemeinsamen 16 × 16-Icons. Im Toast beträgt die Basis-Anzeigegröße **8 × 8
GUI-Pixel**, multipliziert mit der jeweiligen Elementskalierung im Layout-Editor.
[Details und Clientprüfung](animal-toast-icons.md).

Hofnamen/Anzahl anklicken zum Auf-/Zuklappen. Tempo oder Sprung im Hofkopf anklicken:
höchste zuerst **↑** → niedrigste zuerst **↓** → aus. Ein einzelnes Kriterium
sortiert numerisch; zwei Kriterien sortieren nach einem gleich gewichteten
Durchschnitt. Die Auswahlreihenfolge hat keinen Einfluss mehr.

Bei beiden Pfeilen nach oben ist der Wert
`(Tempo / höchstes Tempo + Sprung / höchster Sprung) / 2` maßgeblich.
Die Referenzen stammen aus den vollständig vermessenen Tieren desselben Hofes
im aktiven Tierartenfilter. Eine Textsuche verändert die Referenzen nicht.
Bei einem Pfeil nach unten wird der jeweilige Anteil durch `1 - Anteil` ersetzt;
bei ausschließlich Nullwerten sind die Tiere für dieses Kriterium gleichwertig.
So lassen sich auch hohes Tempo und niedriger Sprung kombinieren. Tiere mit einem
fehlenden Messwert stehen bei kombinierter Sortierung am Ende. Gleichstand wird
nach Name und UUID aufgelöst; Höfe bleiben unabhängig davon nach A–Z bzw. Z–A sortiert.

Im Sortiermenü lassen sich mehrere Kriterien nacheinander wählen. Ein bereits
aktives Kriterium erneut anklicken entfernt es. „Sortierung zurücksetzen“ stellt Höfe A–Z und Tiernamen
A–Z wieder her. Klappzustände bleiben beim Sortieren, Filtern und Abgleichen erhalten,
solange diese Übersicht geöffnet bleibt.

Die Platzhalter können mit
`powershell -ExecutionPolicy Bypass -File tools/generate-horse-overview-icons.ps1`
neu erzeugt werden.

Achtung: Ohne `-Names` überschreibt der Generator auch überarbeitete Icons.
Nur die neuen Platzhalter erzeugen (PowerShell):

```powershell
./tools/generate-horse-overview-icons.ps1 -Names sort_ascending,sort_descending,sort_none,expand,collapse
```

Nur die vier neuen Toast-Symbole erzeugen:

```powershell
./tools/generate-horse-overview-icons.ps1 -Names feeding,breed_ready,shearable,milkable
```
