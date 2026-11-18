# Tierübersicht und Tierartenfilter

Das Feld ganz rechts in der Sortierzeile wechselt zwischen **Alle →
Pferde → Esel → Alle**. Esel und Maultiere werden der Esel-Gruppe zugeordnet.
Die Suche nach Name, Hof, Herkunft oder UUID und die Sortierung funktionieren
innerhalb der ausgewählten Tierart. Die Auswahl gilt für die geöffnete Übersicht.

Unter **Einstellungen → Tierinfo → Erweitert → Tierarten in der Übersicht** lassen
sich **Rinder, Brakel, Hausschweine, Ziegen und Zaupel** einzeln freischalten.
Diese fünf Optionen sind zunächst aus. Aktivierte Tierarten erhalten einen
eigenen Filter und erscheinen zusätzlich unter **Alle**. Die Optionen werden in
der Konfiguration gespeichert; ausgeblendete Register-Einträge werden nicht
gelöscht. Nicht zuordenbare ältere Einträge bleiben unter **Alle** sichtbar.

## Esel und andere Tiere eintragen

1. Das Tier anvisieren und warten, bis seine Tierinfo angezeigt wurde.
2. Die Tierübersicht öffnen und **+ Tier** anklicken.
3. In der Tierakte den Namen eingeben und speichern.

Der Button verwendet das beim Öffnen anvisierte Tier und dessen bekannte
Tierinfo. Er wird erst aktiv, wenn ein unterstütztes Tier mit Beobachtung in
derselben Welt vorhanden ist. Die bestehende Tierakte übernimmt Speicherung,
Standort, Berechtigungen und Fehleranzeige. Dafür gelten weiterhin die
serverseitigen Zugriffsrechte. Ein Esel wird mit seiner Tierart gespeichert;
Pferdefell-Daten werden nicht auf ihn übertragen.

Das ist ein bewusster Register-Eintrag. Der separate
[lokale Nametag-Zwischenspeicher](animal-nametags.md) legt selbst keine Akten an.

## Darstellung

- Die Übersicht ist ein aufgeschlagenes Buch ohne Textschatten. Die linke Seite
  zeigt **Alle Höfe**, die einzelnen Höfe mit Symbol, Tierzahl und Zahl der
  bereiten Tiere, **Ohne Hof** und darunter die Höfe beigetretener Listen
  (Kennzeichen „geteilt“ oder „nur lesen“). Darunter folgen das Codefeld mit
  **Beitreten** und die Suche. **Teilen** rechts oben auf der Höfe-Seite teilt den gewählten Hof.
- Die rechte Seite zeigt die Tiere des gewählten Hofs in zweizeiligen Zeilen:
  Tiermodell, Name mit Geschlechtszeichen, Tierart und Hof, rechts der Status
  (bereit, wartet, bald, braucht Mast, Jungtier, verliebt) mit Fortschrittsbalken.
  Die Sortierchips **Hof**, **Name** und **Status**, auf Wunsch Tempo und Sprung,
  sowie der Tierartenfilter stehen über der Liste.
- Ohne eingetragenen Namen erscheint **???**; die UUID bleibt über die Suche
  auffindbar.
- Auch nicht geladene Tiere haben eine Vorschau ihrer Tierart: Pferd, Esel,
  Rind, Huhn, Schwein, Ziege oder Schaf.
- Ohne Hof und ohne Tier zeigt die rechte Seite eine kurze Anleitung in vier
  Schritten. Ein Doppelklick auf eine Zeile öffnet die Tierakte; Löschen und
  Finder liegen dort auf der linken Seite.
- Bei Tieren beigetretener Listen stehen Beschriftung und Notiz der Liste. Den
  Status gibt es nur, wenn das Tier auch im eigenen Register steht.

## Prüfung

Unit-Tests prüfen Serverbezeichnungen, Standardfilter, unabhängige zusätzliche
Tierarten, unveränderte Quelldaten, UUID-Suche sowie Konfigurationsspeicherung.
`UI-ANIMAL-OVERVIEW` öffnet die echte Oberfläche im lokalen Minecraft-Client,
wechselt alle Filter, speichert einen Esel über die Tierakte und prüft die Suche.
Screenshots bei 1920 × 1080 und 1280 × 720 mit GUI-Skalierung 3 zeigen Namen,
UUID, Herkunft, Symbole und Tiermodelle.

Der lokale Paper-Server stellt die Welt bereit; die Register-API wird für diese
Tests isoliert ersetzt. Produktive Tierregister-Daten werden nicht verändert.
[Release-Validierung](releases/0.1.17-f.md).
