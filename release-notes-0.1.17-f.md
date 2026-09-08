# OttoExtra 0.1.17-f – Alpha

8. September 2026. Tierübersicht mit Eseln, optionalen weiteren Tierarten und
lokal zwischengespeicherten Geschlechtsdaten für ungespeicherte Tiere.

## Installation

Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
Fabric API **ab 0.141.4+1.21.11**. Die bisherige OttoExtra-JAR im Clientprofil durch
**`ottoextra-0.1.17-f.jar`** ersetzen; nur eine OttoExtra-Version gleichzeitig laden.

## Tierübersicht

- Rechts neben der Suche zwischen **Alle, Pferde und Esel** wechseln.
- Weitere Tierarten unter **Einstellungen → Tierinfo → Erweitert → Tierarten
  in der Übersicht** einzeln aktivieren: Rinder, Brakel, Hausschweine, Ziegen,
  Zaupel. Sie erscheinen dann unter Alle und als eigener Filter.
- Zum Eintragen einen Esel beziehungsweise ein anderes unterstütztes Tier
  anvisieren, die Tierinfo abwarten, die Übersicht öffnen und **Tier eintragen…**
  wählen. Die normale Tierakte prüft die Berechtigung und speichert den Namen.
- Namenlose Tiere zeigen **???** und darunter die vollständige UUID. Hofnamen,
  Herkunft und Symbole sind kleiner. Die Suche findet auch UUIDs.

## Nametags und lokaler Cache

Unter **Tierinfo → Grundlagen → Tier-Nametag** ergänzt **Auch ungespeicherte Tiere**
die vorhandenen Optionen für Geschlechtssymbol und Namensfarbe. Zusätzlich muss
mindestens eine dieser beiden Darstellungsoptionen aktiv sein.

Die Daten kommen aus der normalen Tierinfo beim Anvisieren. Ein eigener Tiername
bleibt erhalten; sonst dient die Tierbezeichnung als Name. Registrierte Namen
haben Vorrang. Ohne bekannte Geschlechtsdaten erfolgt keine zusätzliche Anzeige.

Bis zu **1.024 Tiere pro Verbindung** werden anhand ihrer UUID im Arbeitsspeicher
gehalten. Disconnect leert den Cache. Es entstehen keine Dateien, automatischen
Register-Einträge oder zusätzlichen Abfragen für Nametags. Neue Toast-Antworten
aktualisieren die Werte. Das ältere separate AnimalBreeding-Addon behält seine
eigene Pferde-Nametag-Darstellung.

## Validierung

**680 Unit-Tests** sowie **zwei gezielte Tests im echten Minecraft-Client**
bestanden: Nametag-Renderpfad einschließlich Menü und Toast-Ereignis sowie alle
Tierfilter, Esel-Speicherung, UUID-Suche und Darstellung bei zwei Auflösungen.
Der lokale Paper-Server stellt die Welt bereit; die Tierregister-API verwendet
isolierte Testdaten. Produktive Tierzucht-Rechte und Serverantworten müssen beim
Alpha-Test mitgeprüft werden.

Die vorherigen Briefkorrekturen und Toast-Icons sind enthalten. Alle 98
Brief-Klasseneinträge sind bytegleich zu Alpha E; die vollständige Courier-Suite
wurde für diese Tieränderungen nicht erneut ausgeführt.

[Tierübersicht](docs/animal-overview.md), [Nametags](docs/animal-nametags.md),
[Release-Validierung](docs/releases/0.1.17-f.md).
