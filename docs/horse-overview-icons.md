# Pferdeübersicht-Icons

Die austauschbaren Icons liegen unter:

`src/main/resources/assets/ottoextra/textures/gui/horse_overview/`

Vorgabe für alle Dateien:

- exakt **16 × 16 Pixel**
- PNG mit transparentem Hintergrund (RGBA)
- harte Pixelkanten, keine Kantenglättung
- Motiv weiß; OttoExtra färbt es abhängig von Zustand und Geschlecht ein
- Dateinamen unverändert lassen

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

Geschlecht, Tempo und Sprung im Tierregister verwenden dieselben Icons wie die
Übersicht. Mast, Paarung, Scheren und Melken verwenden die vorhandenen Symbole aus
`assets/mattis/textures/font/icons/clean/status/` (8 × 8 Pixel).

Hofnamen/Anzahl anklicken zum Auf-/Zuklappen. Tempo oder Sprung im Hofkopf anklicken:
absteigend → aufsteigend → aus. Mehrere Kriterien bleiben gleichzeitig aktiv;
Auswahlreihenfolge bestimmt Priorität 1/2. Beispiel: Tempo absteigend, bei gleichem
Tempo Sprunghöhe absteigend. Richtung ändern behält die Priorität. Hofsortierung
bleibt unabhängig davon erhalten; die Messwerte werden innerhalb aller Höfe sortiert.
Fehlende Werte stehen für jedes Kriterium in beiden Richtungen am Ende.

Im Sortiermenü lassen sich mehrere Kriterien nacheinander wählen. Ein bereits
aktives Kriterium erneut anklicken entfernt es. Der Pfeil rechts setzt das zweite
Kriterium an erste Stelle. „Sortierung zurücksetzen“ stellt Höfe A–Z und Tiernamen
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
