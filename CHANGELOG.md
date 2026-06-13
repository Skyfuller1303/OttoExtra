# Changelog

Alle nennenswerten Änderungen an OttoExtra. Format angelehnt an
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/); Versionierung nach
[SemVer](https://semver.org/lang/de/).

## [0.1.1] – 2026-06-13

### Hinzugefügt
- Automatischer Titel-Abgleich: Titel werden gegen den Serverstand geprüft und
  lokal angepasst — aus der Tabliste (~30 s) und bei jeder Chat-Nachricht. Lokal
  gesperrte Personen bleiben unverändert.

### Behoben
- Titel-Sync las den bereits angepassten Tablist-Namen statt des Server-Originals;
  liest jetzt über einen Accessor das Roh-`displayName`-Feld (Server-Stand).

## [0.1.0] – 2026-06-13

Erste Version für Minecraft 1.21.11 (Fabric).

### Hinzugefügt

**RP-Namen & Personenbuch**
- Lokales RP-Bekanntschaftssystem: Account → Titel + RP-Name in Chat, Tabliste und Namensschild.
- RP-Personenbuch im neuen GUI-Design: Personen suchen/filtern, Identität + Farben pro Anzeigeort bearbeiten, Live-Vorschau, Konfliktauflösung.
- 3D-Spielervorschau im Personenbuch: Modell mit Maus-Blickfolge, Server-Skin (Mojang als Backup), Marken-Fallback-Skin für Unbekannte, sichtbarer 2nd-Layer.
- Titelkatalog mit Kategorien/Farben und Flag „Farbe überschreibt" (Standard an); Titelfarbe wird beim Vergeben übernommen (Live-Tausch bei Enter).
- Titelfarbe-Override greift in Chat, Tabliste und Namensschild.
- Schnellzugriff (optional): Shift-Linksklick auf Chat-Namen / Shift-Rechtsklick auf Spieler öffnet das Personenbuch beim Eintrag.
- Import aus OttoPlus/OttoTalk (RP-Name/Titel/Farbe) über das Import-Tab.
- Reset-Icons: RP-Name/Titel auf Serverstandard, einzelne Farben auf Default.

**Chat**
- Kanal-Prefix-Button (Sprechen/Flüstern/Rufen/Offtopic/Hilfe) mit eigenen Farben, OOC-Warnmarker, optionalem Auto-`/s` nach Join.
- Kanal-Hotkeys (standardmäßig unbelegt, manuell bindbar).
- RP-Namen in OOC-Kanälen (Offtopic/Hilfe) standardmäßig aus, per Setting aktivierbar.

**Regionen**
- Betreten-Toast mit Wappen, Name, Hierarchie und Tasten-Hinweis; Position einstellbar.
- Theme-System: Light, Dark und eigene Custom-Themes mit Editor (Farben, Schriftgrößen, Sichtbarkeit, Abstände) und Live-Vorschau.

**Karte**
- Worldmap-Overlay: Lehengrenzen, zoomabhängige Lehensnamen/Wappen (Lehnsherr ↔ Lehen), politische Gefolge-Flächen mit Klick-Fokus.
- Aktivitätskreise bei Spielerversammlungen, zoomabhängig am Lehnsherrn bzw. Lehen.
- NPC-Dörfer als kleine, editierbare Labels (ein-/ausschaltbar).
- Gemalte Karte über unerkundetem Terrain + Kalibrier-Pfeile (optional/Debug).
- Minimap-Overlay: Grenzen, gemalte Karte, politische Flächen, Wappen-HUD.

**Briefe**
- Editor mit Seiten/Wortumbruch, Paste-zu-Seiten, RP-Platzhaltern; Brief vs. Verkündung.
- Versand mit Anti-Spam-Timing und Recovery; Actionbar-Status; UI schließt nach Absenden.
- Gespeicherte Entwürfe (benennen/laden/weiter editieren/löschen).

**Allgemein**
- Zentrales Einstellungs-GUI (ModMenu) mit Suche, Reset je Option und Backups.
- Resourcepack-Downloader (Auto-Download/-Aktivierung des Server-Packs).
- Rein clientseitig; alle Module nur auf dem Ottonien-Server aktiv.

### Behoben
- Doppelte Titel in Chat/Tabliste (Server-Titel + Mod-Titel) bei aktivem RP-Namen-Modul.
- Kanal-Button-Farben pro Kanal.
- Brief-Versand schließt das UI nicht / alter Entwurf blieb erhalten.

### Geändert
- Personenbuch-, Brief- und Theme-Oberflächen an das neue, einheitliche GUI-Design angeglichen.
- Brief-Editor: Buch-Import entfernt, „Parameter prüfen" als Icon, „Gespeicherte Entwürfe" als Button.
- Z-Reihenfolge der Karte: Lehnsnamen/Wappen als oberste Overlay-Ebene.

[0.1.0]: https://example.com/ottoextra/releases/0.1.0
