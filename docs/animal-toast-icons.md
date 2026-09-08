# Kompakte Symbole im Tierinfo-Toast

Stand: 8. September 2026, Alpha 0.1.17-d.

Der Tierinfo-Toast verwendet für seine Statusanzeigen transparente 16 × 16-PNGs.
Die Basisgröße auf dem Bildschirm beträgt 8 × 8 GUI-Pixel. Die vorhandene
Elementskalierung wirkt weiterhin darauf: beispielsweise 4 × 4 bei 0,5 und
16 × 16 bei 2,0. Minecraft multipliziert die GUI-Koordinaten anschließend mit
der eingestellten GUI-Skalierung. Die Standard-Statuszeilen verwenden 0,68;
das Geschlechtssymbol verwendet 1,0.

## Symbole

Alle Dateien liegen unter
`src/main/resources/assets/ottoextra/textures/gui/horse_overview/`.

| Anzeige | Datei | Herkunft |
| --- | --- | --- |
| Weiblich, männlich, unbekannt | `gender_mare.png`, `gender_stallion.png`, `gender_unknown.png` | Vorhandene 16 × 16-Icons |
| Tempo, Sprung | `speed.png`, `jump.png` | Vorhandene 16 × 16-Icons |
| Mast | `feeding.png` | Neue Ähre |
| Paarungsbereitschaft | `breed_ready.png` | Neues Herz |
| Scherbar | `shearable.png` | Neue Schere |
| Melkbar | `milkable.png` | Neuer Eimer |

Die weißen Motive werden wie zuvor nach Geschlecht oder Zustand eingefärbt.
Der Ein-/Ausblendeffekt gilt auch für die PNGs. Die vier neuen Dateien entstehen
über den vorhandenen [Pixelgenerator](../tools/generate-horse-overview-icons.ps1).
Die bestehenden Icons wurden dabei nicht überschrieben.

## Layout

- Der Editor verwendet dieselben Icongrößen und Textbreiten wie die Anzeige.
- Tempo und Sprung zeigen Symbol, Messwert und Einheit. Die langen zusätzlichen
  Beschriftungen entfallen, damit die beiden Standardspalten nebeneinander passen.
- Tiernamen werden in ihrer tatsächlich dargestellten Fettschrift gemessen.
  Bei einem Geschlechtselement rechts daneben endet der Name mit zwei GUI-Pixeln
  Abstand davor; zu lange Namen erhalten „…“. Ausgeblendete Elemente oder Elemente
  in einer anderen Zeile begrenzen den Namen nicht.
- Bestehende Positionen, Größen und Sichtbarkeitseinstellungen bleiben erhalten.
  Für mehr Platz für einen langen Namen lassen sich die Geschlechtselemente im
  Layout-Editor nach rechts oder in eine andere Zeile verschieben.

Die Darstellung des 3D-Tiers und dessen bisheriges Ersatzsymbol bleiben bestehen.
Die Statusicons im Tierregister werden durch diese Toast-Änderung nicht umgestellt.

## Nachweis

Der automatisierte Fall `UI-ANIMAL-ICONS` läuft in einem echten Minecraft-Client
mit dem produktiven Toast-Renderer und Beispielstatus für alle zwölf Tierarten.
Er prüft Iconbegrenzungen bei 0,5/1,0/2,0, die Panelgrenzen, getrennte
Pferdewertspalten, Titelabstände, Titelvergrößerung, ausgeblendete Elemente und
individuell verschobene Icons. Anschließend erstellt er Screenshots bei
1920 × 1080 und 1280 × 720 und öffnet den echten Layout-Editor.

Der Fall besteht. Die Screenshots wurden visuell geprüft. Der Asset-Test prüft
alle neun PNGs auf 16 × 16 Pixel, Transparenz und weiße, harte Pixelkanten.
[Release-Validierung](releases/0.1.17-d.md) und
[lokale kompakte Vorschau](../build/animal-toast-icons/compact.png).

Der lokale Server stellt die Testwelt bereit. Der Fall prüft die Darstellung
mit Beispieldaten; er bildet kein produktives Tierzucht-Plugin nach.
