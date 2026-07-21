# OttoExtra 0.1.15.1

Veröffentlicht am 21. Juli 2026 für Minecraft 1.21.11.

## Neu

- Optionale Sortierung bestehender Online-Spieler in der Tabliste nach Lehen.
- Innerhalb eines Lehens folgen Leitung, Stellvertretung, Titelgruppe,
  konkreter Titelrang und sichtbarer Name.
- Schalter unter **RP-Namen → Basis → Tabliste → Tabliste nach Lehen sortieren**.
- Direkte Kompatibilität mit BetterTab 2.1.5.

## Behoben

- Frische Installationen laden Lehensnamen und Regionsdaten wieder zuverlässig
  über die signierte, authentifizierte v2-API.
- Wappen werden vom vertrauenswürdigen API-Origin geladen und lokal aktualisiert.
- Fremde Asset-Hosts, Cross-Origin-Weiterleitungen und ungültige Signaturen
  bleiben blockiert.
- Schnelles Trennen und erneutes Verbinden kann keine veralteten
  Regions-Bootstrap-Antworten mehr übernehmen.
- Fraktions-Aliase werden bei Änderungen auf den aktuellen Datensatz gesetzt.

## Tabliste

- Die Standardsortierung bleibt unverändert, solange der neue Schalter aus ist.
- Die Lehenssortierung erzeugt keine Überschriften oder zusätzlichen Zeilen.
- Server-Wappen, BetterTab-Badges, Titel, Farben, Ping und Displaytexte bleiben
  unverändert; ausschließlich die Reihenfolge wird angepasst.

## Installation

Die vorhandene OttoExtra-JAR durch `ottoextra-0.1.15.1.jar` ersetzen und
Minecraft vollständig neu starten.
