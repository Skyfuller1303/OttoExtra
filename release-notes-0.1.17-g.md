# OttoExtra 0.1.17-g – Alpha

8. September 2026. Minimap-Textfelder mit verstellbarer Höhe und Zeilenumbruch.

## Installation

Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
Fabric API **ab 0.141.4+1.21.11**. Die bisherige OttoExtra-JAR im Clientprofil durch
**`ottoextra-0.1.17-g.jar`** ersetzen und Minecraft neu starten. Nur eine
OttoExtra-Version gleichzeitig laden.

## Minimap-Texte anpassen

**Einstellungen → Karte → Minimap → Wappen-Position (Drag&Drop)…** öffnen:

- Im Textfeld ziehen: Position ändern.
- Den Griff unten rechts ziehen: Breite und Höhe ändern. Nach unten ziehen
  schafft Platz für weitere Textzeilen.
- Mausrad über dem Textfeld: Schriftgröße unabhängig einstellen.

„Gefolge in Gefolge XYZ“ kann beispielsweise bei Breite 91, Höhe 20 und
Schriftgröße 1 auf zwei Zeilen stehen. Beim Vergrößern bleibt die gegenüberliegende
Ecke fest. Der Griff bleibt auch direkt am Bildschirmrand erreichbar.

Name, Stand/Hierarchie und Gefolge haben eigene Höhenfelder unter
**Karte → Erweitert → HUD-Feinjustierung**. Höhe **0** erhält die bisherige
einzeilige Darstellung. Bei mehr Höhe bricht längerer Text an Wortgrenzen um;
bei zu wenig Gesamtplatz bleibt der Rest ausgeblendet.

Die [Tierübersicht mit Eseln, zusätzlichen Tierarten und lokalem
Nametag-Cache aus Alpha F](release-notes-0.1.17-f.md) sowie alle Briefkorrekturen
sind enthalten.

## Validierung

**684 Unit-Tests** und der gezielte lokale Clienttest **UI-MINIMAP-HEIGHT** bestehen.
Dieser prüft echte Mausereignisse im Editor, zweizeiligen Text ohne Wortverlust,
separate Schriftgröße, Verankerung, Verschieben, erneutes Öffnen, Fenstergrößen
und erreichbare Eckgriffe am Bildschirmrand.

Alle 98 Brief-Klasseneinträge und alle 43 Klassen der Tierinfo/Tierübersicht sind
bytegleich zu Alpha F. Die vollständige Courier-Suite und die beiden Tier-Clientfälle
wurden für diese Minimap-Änderung nicht erneut ausgeführt.

[Bedienung](docs/feature-minimap-wappen.md),
[Release-Validierung](docs/releases/0.1.17-g.md).
