# OttoExtra

Clientseitiger Fabric-Mod für das Mittelalter-Rollenspiel-Netzwerk **Ottonien**.

OttoExtra bündelt mehrere Komfort- und Rollenspiel-Funktionen in einer zentralen Mod: RP-Namen, Chat-Kanäle, Karten-Overlay, Regionen-Einblendungen, Briefsystem, Resourcepack-Downloader, Importfunktionen und ein gemeinsames Einstellungs-GUI.

## Projektstatus

OttoExtra befindet sich in aktiver Entwicklung.  
Die Mod ist als clientseitige Community-Erweiterung für Ottonien gedacht und ersetzt keine serverseitigen Systeme.

## Überblick

| Bereich | Beschreibung |
|---|---|
| Minecraft | `1.21.11` |
| Modloader | Fabric |
| Fabric Loader | `>= 0.18.1` |
| Fabric API | erforderlich |
| Seite | clientseitig |
| Version | `0.1.15.3` |
| Server-Gate | Funktionen werden nur auf Ottonien aktiv |
| Server-Interaktion | ausschließlich über normale Chat-Befehle und öffentliche APIs |

## Features

- Zentrales Einstellungs-GUI für alle Module
- RP-Namen in Chat, Tabliste und Namensschildern
- Lokale Titel können den serverseitigen Titel in der Tabliste ersetzen, ohne das Lehenswappen zu entfernen
- Lokales RP-Personenbuch mit Titel-, Farb-, Notiz- und Importverwaltung
- Chat-Kanal-Umschaltung direkt am Chat-Eingabefeld
- Regionen-Einblendung beim Betreten eines Lehens
- Pergamentmodus, gemalte Übersichtskarte und anklickbare Lehen für Xaero's World Map und Minimap
- Brief- und Verkündungseditor mit Seitenverwaltung, Formatierung und Vorschau
- Nach dem Absenden startet der nächste neue Brief mit einer leeren Seite
- Resourcepack-Downloader für das Ottonien-Resourcepack
- Import bestehender Daten aus OttoPlus/OttoTalk
- Backup- und Recovery-Funktionen für wichtige lokale Daten

## Installation

1. Fabric Loader für Minecraft `1.21.11` installieren.
2. Fabric API in den `mods`-Ordner legen.
3. OttoExtra in den `mods`-Ordner legen.
4. Optional: ModMenu installieren, um die Einstellungen komfortabel zu öffnen.
5. Minecraft starten und dem Ottonien-Server beitreten.

Die Mod ist rein clientseitig. Auf anderen Servern bleiben die Ottonien-spezifischen Module still.

## Einstellungs-GUI

Die zentrale Konfiguration ist über **ModMenu → OttoExtra** oder ein konfigurierbares Tastenkürzel erreichbar.

Geplant beziehungsweise enthalten:

- Modul-Navigation in der Kopfzeile
- Tabs pro Modul, zum Beispiel Basis und Erweitert
- Volltextsuche über alle Optionen
- Optionserklärungen und Hover-Hilfen
- Zurücksetzen einzelner Optionen auf Standardwerte
- Backup-Erstellung vor größeren Änderungen
- Wiederherstellung lokaler Backups
- übersichtliche Gruppierung pro Funktionsbereich

## Diagnose-Logs

OttoExtra-Diagnosen sind nach jedem Start deaktiviert. Bei Bedarf lassen sie sich
für die laufende Sitzung einschalten:

```text
/ottoextra debug on
/ottoextra debug status
/ottoextra debug off
```

Aktive Diagnosemeldungen stehen in `logs/debug.log`. Sie können Chat-, Account-
und RP-Identitätsdaten enthalten und sollten nur gezielt zur Fehlersuche aktiviert
werden. Ein Neustart setzt den Schalter automatisch auf `off`. Wichtige,
gedrosselte Warnungen und Fehler bleiben unabhängig davon in `latest.log` sichtbar.

## RP-Namen

OttoExtra ersetzt Minecraft-Accountnamen durch **Titel + RP-Name**.

Die Anzeige kann getrennt gesteuert werden für:

- Chat
- Tabliste
- Namensschilder über Spielern
- RP-Kanäle
- OOC-Kanäle

Datenquellen:

1. manuelle Einträge im Personenbuch
2. gelernte Namen aus Chat-Hoverdaten
3. Import aus der Ottonien-Regions-API
4. Import aus OttoPlus/OttoTalk-Caches

Manuelle Einträge haben Vorrang vor automatisch importierten Daten.

### Anzeigeoptionen

- Titel anzeigen oder ausblenden
- RP-Name anzeigen oder ausblenden
- Accountname anzeigen oder ausblenden
- Skalierung der Namensschilder
- eigene Farben für Chat, Tabliste und Namensschild
- Verhalten bei unbekannten Spielern
- kanalabhängige Aktivierung

## Titelkatalog

Der Titelkatalog verwaltet bekannte Titel mit Kategorie, Farbe und Varianten.

Kategorien können unter anderem sein:

- System
- Adel
- Klerus
- Vorkoster
- Fertigkeit
- Allgemein
- Custom
- Spielernamen

Beim Vergeben eines Titels kann die Titelfarbe automatisch aus dem Katalog übernommen werden.

## RP-Personenbuch

Das Personenbuch ist das lokale Adressbuch für RP-Wissen.

Funktionen:

- Personen suchen und filtern
- RP-Name bearbeiten
- Titel bearbeiten
- Accountname einsehen
- Farben pro Anzeigeort setzen
- Live-Vorschau für Chat, Tabliste und Namensschild
- Konflikte zwischen lokalen Daten und API-Daten auflösen
- Einträge sperren, damit Imports sie nicht überschreiben
- Importbereich für OttoPlus/OttoTalk-Daten

### Schnellzugriff

Optional kann das Personenbuch direkt geöffnet werden über:

- Shift-Linksklick auf einen Namen im Chat
- Shift-Rechtsklick auf einen Spieler

## Chat-Kanäle

OttoExtra ergänzt den Chat um einen festen Kanal-Button links unten am Eingabefeld.

Unterstützte Kanäle:

| Kanal | Befehl |
|---|---|
| Sprechen | `/s` |
| Flüstern | `/f` |
| Rufen | `/r` |
| Offtopic | `/o` |
| Hilfe | `/h` |

Bedienung:

- Linksklick wechselt durch die RP-Kanäle
- Shift-Linksklick wechselt durch die OOC-Kanäle
- Normaler Linksklick führt zurück in die RP-Kanäle
- Der Wechsel sendet den passenden Serverbefehl
- OOC-Kanäle werden bewusst nur per Shift-Wechsel aktiviert

Zusätzliche Optionen:

- Auto-`/s` kurz nach dem Serverbeitritt
- Warnmarker für Offtopic
- eigene Farben pro Kanal
- optionale Hotkeys pro Kanal

## Karte und Reisehilfe

Die Xaero-Weltkarte verwendet wieder ein kontinuierliches Zoomverhalten. Beim
Herauszoomen werden einzelne Lehen schrittweise zugunsten der Gefolge-Übersicht
reduziert; optional kann die gemalte Ottonien-Karte das Terrain vollständig
überblenden.

Ein Klick auf ein Lehen öffnet eine Infokarte mit Herrschaft, Lehnsherr,
Stand, Gefolge, Entfernung, Himmelsrichtung und geschätzter Reisezeit. Der
Pergamentmodus ergänzt Rahmen, Kompass und Maßstabsleiste. Alle Bestandteile
lassen sich in den Karteneinstellungen einzeln anpassen.

## Regionen-Einblendung

Beim Betreten eines Lehens zeigt OttoExtra eine gestaltete Einblendung an.

Inhalte:

- Hinweis „Du betrittst“
- Regionsname
- Hierarchie
- Wappen
- optionaler Hinweistext
- optionale Sonderbeschreibung

Optionen:

- Position oben rechts
- Position oben links
- Position oben mittig
- Position zentriert
- Light-Theme
- Dark-Theme
- Custom-Themes
- Live-Vorschau im Einstellungsmenü
- anpassbare Farben, Abstände und Schriftgrößen

## Karte

OttoExtra integriert Ottonien-Daten in Xaero's World Map und Xaero's Minimap.

Xaero's World Map und Xaero's Minimap sind empfohlen, aber die Mod soll ohne sie nicht abstürzen.

### Worldmap

Geplante beziehungsweise enthaltene Anzeigen:

- Lehengrenzen
- Lehensnamen
- Wappen
- politische Flächen nach Gefolge
- Vasallen-Schraffuren
- Aktivitätskreise bei Spielerversammlungen
- NPC-Dörfer
- gemalte Ottonien-Karte über unerkundetem Terrain
- Debug-Kalibrierung für Kartenpositionen

### Minimap

Geplante beziehungsweise enthaltene Anzeigen:

- Grenzen
- politische Flächen
- gemalte Karte
- Wappen-HUD
- Name, Stand und Fraktion des aktuellen Bereichs

## Briefsystem

OttoExtra enthält einen Editor zum Schreiben und Versenden von Briefen und Verkündungen.

Unterstützte Serverbefehle:

- `/letter`
- `/post`

Funktionen:

- mehrseitiger Editor
- automatischer Wortumbruch
- Cursor und Selektion
- Copy, Cut und Paste
- Paste mit automatischer Seitenerstellung
- gespeicherte Entwürfe
- Versand-Recovery nach Verbindungsverlust
- Actionbar-Status während des Versands
- automatisches Schließen nach erfolgreichem Absenden

### Platzhalter

Im Editor können Platzhalter genutzt werden:

| Platzhalter | Funktion |
|---|---|
| `{{name:Spieler}}` | wird zum RP-Namen des Spielers |
| `{{title:Spieler}}` | wird zum Titel des Spielers |
| `{{full:Spieler}}` | wird zu Titel + RP-Name |
| `{{mc:Spieler}}` | wird zum Minecraft-Accountnamen |

Die Auflösung kann per Tab ausgelöst werden.

### Brief oder Verkündung

Am Ende des Schreibprozesses wird unterschieden zwischen:

- Brief an einen Empfänger
- Verkündung

Bei Verkündungen soll besonders darauf geachtet werden, dass Minecraft-Seiten sauber in Discord-Nachrichten übertragen werden und keine unnötig unschönen Umbrüche entstehen.

## Resourcepack-Downloader

OttoExtra kann das Ottonien-Server-Resourcepack automatisch herunterladen und aktivieren.

Optionen:

- Prüfung beim Start
- automatische Aktivierung
- manuelles Deaktivieren respektieren
- erneute Prüfung bei Änderungen

## Import aus OttoPlus

OttoExtra kann bestehende RP-Daten aus OttoPlus beziehungsweise OttoTalk übernehmen.

Importierbare Daten:

- RP-Name
- Titel
- Titelfarbe
- UUID-Zuordnung

Ablage für Importdateien:

```text
config/ottoextra/import/
