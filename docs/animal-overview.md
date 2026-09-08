# Tierübersicht und Tierartenfilter

Ab Alpha 0.1.17-f wechselt der Button rechts neben der Suche zwischen **Alle →
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
2. Die Tierübersicht öffnen und **Tier eintragen…** anklicken.
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

- Ohne eingetragenen Namen erscheint **???**. Darunter steht die vollständige
  Entity-UUID in kleiner Schrift; sie bleibt über die Suche auffindbar.
- Hofnamen im Gruppenkopf und Herkunft neben dem Tiernamen verwenden 75 %
  Schriftgröße. Die UUID verwendet 65 %.
- Die 16 × 16-Icondateien werden in der Übersicht mit **8 × 8 GUI-Pixeln**
  dargestellt. Die 18 × 18-Klickflächen von Finder und Löschen bleiben erhalten.
- Auch nicht geladene Tiere haben eine Vorschau ihrer Tierart: Pferd, Esel,
  Rind, Huhn, Schwein, Ziege oder Schaf.
- Bei namenlosen Tieren nennt die Löschbestätigung zusätzlich die UUID.

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
