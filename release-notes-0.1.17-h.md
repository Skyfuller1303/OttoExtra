# OttoExtra 0.1.17-h – Alpha

8. September 2026. Optionaler Tier-Glow nach Geschlecht mit frei belegbarer Taste.

## Installation

Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
Fabric API **ab 0.141.4+1.21.11**. Die bisherige OttoExtra-JAR im Clientprofil
durch **`ottoextra-0.1.17-h.jar`** ersetzen und Minecraft neu starten.
Nur eine OttoExtra-Version gleichzeitig laden.

## Tier-Glow

**Einstellungen → Tierinfo → Grundlagen → Tier-Glow → Tiere nach Geschlecht
umranden** aktivieren. Weibliche Tiere erhalten einen rosa, männliche Tiere
einen blauen Umriss. Ein Finder-Ziel bleibt gold. Die Funktion ist zunächst aus.

Unter **Steuerung → Tastenbelegung → Verschiedenes** kann **Tier-Glow nach
Geschlecht umschalten** frei belegt werden. Die Taste schaltet denselben
gespeicherten Wert wie das Menü um; zunächst ist sie unbelegt.

Die Markierung funktioniert auch ohne Register-Eintrag, sobald das Geschlecht
über die Tierinfo bekannt ist. Lebende Tiere in weniger als 64 Blöcken Entfernung
werden hervorgehoben, auch durch Wände. Unbekannte Tiere erhalten keinen
zusätzlichen Umriss. Nametag-Schalter sind davon unabhängig.

Das nutzt vorhandene lokale Tierbeobachtungen bzw. bereits geladene
Registerdaten und erzeugt keine zusätzlichen Abfragen. Neue Toast-Daten
aktualisieren die Farbe. Der Beobachtungscache wird beim Disconnect geleert.

Die [verstellbare Minimap-Texthöhe aus Alpha G](release-notes-0.1.17-g.md),
Tierübersicht, Nametag-Erweiterungen und Briefkorrekturen sind enthalten.

## Validierung

**686 Unit-Tests** und die lokalen Clienttests **UI-ANIMAL-GLOW**,
**UI-ANIMAL-NAMETAGS** und **UI-INVENTORY** bestehen. Sichtbare Umrisse,
Finder-Vorrang, Vanilla-Glow, Tastenkürzel, Menü und deaktivierter Zustand
wurden geprüft. Der Inventartest läuft mit der lokal korrigierten
Mouse-Wheelie-Version für Yarn-Dev-Clients.

Alle 98 Brief- und 46 Karten-Klasseneinträge sind bytegleich zu Alpha G.
Die vollständige Courier-Suite wurde für diese Tierdarstellung nicht erneut
ausgeführt.

[Bedienung und technische Details](docs/animal-gender-glow.md),
[Release-Validierung](docs/releases/0.1.17-h.md).
