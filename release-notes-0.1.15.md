# OttoExtra 0.1.15

Veröffentlicht am 19. Juli 2026 für Minecraft 1.21.11.

## Neu

- Neuer RP-Untersuchungsmodus über die frei belegbare, standardmäßig linke
  Alt-Taste mit Lupe, Zoom, Randunschärfe, Fortschrittsring und RP-Gedanken.
- Sichtbare Informationen zu Spielern, Händen, Rüstung, Gegenständen,
  Kreaturen, Schildern, Rahmen, Lesepulten, Bannern und ausgewählten Blöcken.
- Automatische RP-Chatformatierung für `*Emotes*` und `(OOC-Kommentare)`.
- Getrennt konfigurierbare Farben für Kanalname und Nachrichtentext in
  Sprechen, Flüstern, Murmeln, Rufen, Brüllen, Offtopic und Hilfe.
- Farbe und Kursivdarstellung für Emotes und OOC können unabhängig voneinander
  eingestellt werden.
- Authentifizierte API- und Addon-Schnittstelle für zeitlich begrenzte,
  empfängergebundene Chat-Originaltexte.

## Geändert

- Die kompakte Chatfarben-Verwaltung verwendet das dunkle Design des
  RP-Personenbuchs und zeigt Live-Farbfelder direkt neben den Hexfeldern.
- Jeder Farbwert besitzt einen eigenen Reset; zusätzlich lassen sich alle
  Kanalwerte gemeinsam auf ihre Originaldarstellung zurücksetzen.
- Leere Kanal-Farbfelder behalten den unveränderten Serverstil.
- OttoExtra gibt Chatfarben und RP-Formatierung zentral vor. Addons wie
  TranslateUtils verwenden dieselben Regeln für Übersetzungen, Hover-Lore und
  lokale Originalzeilen.
- Lange Nachrichten führen offene Emote- und OOC-Bereiche korrekt über alle
  automatisch aufgeteilten Teilnachrichten fort.
- „Titel in der Tabliste“ zeigt vorhandene Titel bei allen Spielern,
  unabhängig vom Bekanntschaftsstatus. Chat und Nametags bleiben davon
  unberührt.
- Die Standardfarbe für OOC-Text ist `#B4BEC6`.

## Behoben

- Kanal-, Spieler- und Titelbestandteile behalten bei der RP-Formatierung ihre
  vorhandenen Farben, Hoverdaten und Klickaktionen.
- Nach Emotes und OOC-Kommentaren wird zuverlässig der vorherige Kanalstil
  wiederhergestellt.
- Die Lupentextur sowie Post-Effect und Shader werden vollständig in die JAR
  gepackt und durch Asset-Tests abgesichert.
- Der Button zum Öffnen der Chatfarben-Verwaltung besitzt wieder eine sichtbare
  Beschriftung.
- Das Farbmenü löst keinen zweiten Screen-Blur mehr aus und crasht nicht mehr
  mit „Can only blur once per frame“.
- Bereits sichtbare Chatzeilen behalten beim Neuaufbau nachträgliche
  Addon-Verarbeitung und geschützte Originaldaten.

## Kompatibilität

- Minecraft 1.21.11
- Fabric Loader
- TranslateUtils 0.1.3

## Installation

Die vorhandene OttoExtra-JAR durch `ottoextra-0.1.15.jar` ersetzen und
Minecraft vollständig neu starten.

Der ausführliche Verlauf aller Änderungen befindet sich in
`CHANGELOG-since-0.1.13.md`.
