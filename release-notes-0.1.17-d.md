# OttoExtra 0.1.17-d – Alpha

8. September 2026. Kompakte Symbole im Tierinfo-Toast.

## Installation

Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
Fabric API **ab 0.141.4+1.21.11**. Die bisherige OttoExtra-JAR im Clientprofil durch
**`ottoextra-0.1.17-d.jar`** ersetzen; nur eine OttoExtra-Version gleichzeitig laden.

## Änderungen

- Geschlecht, Mast, Paarungsbereitschaft, Scheren, Melken, Tempo und Sprung
  verwenden 16 × 16-PNGs. Die Basis-Anzeigegröße beträgt kompakte **8 × 8 GUI-Pixel**;
  die jeweilige Skalierung im Layout-Editor wirkt zusätzlich darauf.
- Tempo und Sprung erscheinen mit Symbol, Messwert und Einheit. Beide Angaben
  passen in die Standardspalten.
- Lange Tiernamen erhalten bei engem Platz „…“ und überdecken das Geschlecht
  nicht mehr. Im Editor lässt sich den Namen durch Verschieben der
  Geschlechtselemente mehr Platz geben.
- Gespeicherte Layouts bleiben erhalten. Statusfarben und Ein-/Ausblenden bleiben aktiv.

Die JAR enthält die Briefkorrekturen aus [Alpha B](release-notes-0.1.17-b.md) und
das spätere Verschicken geschriebener Briefe aus [Alpha C](release-notes-0.1.17-c.md).

## Prüfung

**649 Unit-Tests** bestanden. Ein zusätzlicher automatisierter Test im echten
Minecraft-Client prüft alle zwölf Tierlayouts, mehrere Skalierungen, Ausblenden,
Verschieben und den Layout-Editor. Screenshots bei 1920 × 1080 und 1280 × 720
wurden visuell geprüft. Der Test verwendet Tier-Beispieldaten.

Die 89 Courier-Client-/Serverfälle bestanden für Alpha C. Sie wurden für diese
Darstellungsänderung nicht erneut ausgeführt; alle 98 Brief-Klasseneinträge sind
in der gebauten JAR bytegleich zu Alpha C.

[Darstellung und Umsetzung](docs/animal-toast-icons.md),
[Release-Validierung](docs/releases/0.1.17-d.md).
Die offenen Beobachtungen und Grenzen aus Alpha C gelten weiterhin.
