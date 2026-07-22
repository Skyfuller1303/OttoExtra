# OttoExtra 0.1.15.3

Veröffentlicht am 22. Juli 2026 für Minecraft 1.21.11.

## Highlights

- NPCs erhalten in allen RP-Sprechkanälen zuverlässig eigene, konfigurierbare
  Namens- und Nachrichtenfarben. Dies gilt generisch für alle NPCs, ohne
  hardcodierte Namensliste.
- Chatkanal-Nachrichtenfarben sind in der Farbverwaltung mit konkreten Hexwerten
  vorausgefüllt und getrennt editierbar.
- Betreten-Einblendungen zeigen optional den RP-Namen des Lehnsherrn; ohne lokal
  vorhandenen RP-Namen erscheint weiterhin der Accountname.
- Öffnen von Xaeros Weltkarte stößt einen direkten Regionssync an, begrenzt auf
  einen Karten-Sync pro Minute.
- Shift-Rechtsklick öffnet während einer Bossbar mit `IM KAMPF` weder
  Personenbuch noch Kennenlernfenster. Der bestehende Schnellzugriff-Schalter
  unter RP-Namen aktiviert oder deaktiviert die Personenbuch-Aktion.
- Ist die Spieler-Einzelabfrage nicht verfügbar, verwendet das Kennenlernfenster
  lokal gecachte oder im Chat erkannte RP-Daten statt abzubrechen.
- Debug-Ausgaben sind standardmäßig deaktiviert und sitzungsbezogen per Command
  steuerbar.

## Chat und NPCs

- Standard-NPC-Farben:
  - Name: `#C7A87F`
  - Nachricht: `#DFC8A7`
- Standard-Nachrichtenfarben der Kanäle:
  - Sprechen: `#DFC8A7`
  - Flüstern: `#768491`
  - Murmeln: `#58666F`
  - Rufen: `#D2BF6A`
  - Brüllen: `#FCF47E`
  - Offtopic: `#B4BEC6`
  - Hilfe: `#B53764`
- Alte leere oder ungültige Farbwerte werden automatisch migriert; gültige
  eigene Farben bleiben erhalten.
- Strukturierte Minecraft-Texte und Legacy-`§`-Farbcodes werden korrekt
  verarbeitet.
- Hover-, Klick-, Einfüge-, Schrift-/Sprite- und Formatierungsdaten bleiben beim
  Umfärben erhalten.
- NPCs werden nicht als Spieler gelernt, gespeichert oder zur API hochgeladen.
- Emote- und OOC-Farben behalten Vorrang vor der NPC-Nachrichtenfarbe.
- Farbänderungen aktualisieren auch bereits sichtbare Chatzeilen.

## Regionen und Karte

- Neue, standardmäßig aktive Option für RP-Namen des Lehnsherrn im
  Betreten-Toast.
- Lokal vorhandene RP-Namen aus dem API-Abgleich dürfen im Toast erscheinen,
  ohne Sichtbarkeitsregeln von Chat, Tabliste oder Nametags zu verändern.
- Ausschließlich `leader_name` wird als Lehnsherr-Account verwendet;
  `lord_name` bleibt politische Hierarchieinformation.
- Echte Xaero-Weltkartenöffnungen starten einen inkrementellen Regionssync mit
  einer Minute Cooldown. Umgebundene Xaero-Tasten funktionieren ebenfalls.

## Logging und Datenschutz

- Debug-Commands:
  - `/ottoextra debug on`
  - `/ottoextra debug off`
  - `/ottoextra debug status`
- Debug wird nach jedem Neustart wieder deaktiviert.
- Detaillierte Diagnose läuft über Fabrics `logs/debug.log`; keine separate
  OttoExtra-Logdatei.
- Wiederholte Render-, Chat-, Download- und API-Fehler werden gedrosselt.
- OttoExtra schreibt keine sensiblen Chat-, Account-, UUID-, RP-Namens- oder
  API-Nutzdaten mehr in normale Diagnosemeldungen.

## Installation

Vorhandene OttoExtra-JAR durch `ottoextra-0.1.15.3.jar` ersetzen und Minecraft
vollständig neu starten.
