# OttoExtra

Clientseitige Fabric-Mod für das Mittelalter-Rollenspiel-Netzwerk **Ottonien**.

OttoExtra bündelt zahlreiche Komfort- und Rollenspiel-Funktionen in einer gemeinsamen Mod. Dazu gehören RP-Namen, ein lokales Personenbuch, Chat-Kanäle, RP-Chatformatierung, ein Untersuchungsmodus, Regionen-Einblendungen, Kartenfunktionen, ein Briefsystem, Resourcepack-Verwaltung und Importmöglichkeiten für bestehende Daten.

## Projektstatus

OttoExtra befindet sich in aktiver Entwicklung und wird fortlaufend erweitert.

Die Mod arbeitet vollständig clientseitig und ersetzt keine serverseitigen Systeme. Ottonien-spezifische Funktionen werden nur auf dem vorgesehenen Server aktiv.

## Voraussetzungen

| Bereich | Voraussetzung |
|---|---|
| OttoExtra | `0.1.14` |
| Minecraft | `1.21.11` |
| Modloader | Fabric |
| Fabric Loader | `>= 0.18.1` |
| Fabric API | erforderlich |
| ModMenu | optional, aber empfohlen |
| Installation | clientseitig |

## Neu in 0.1.14

### RP-Untersuchungsmodus

Mit der frei belegbaren Taste **„Untersuchen“** könnt ihr Spieler, Gegenstände, Kreaturen und verschiedene Blöcke genauer betrachten.

Standardmäßig liegt die Funktion auf der **linken ALT-Taste**. Die Belegung kann unter den normalen Minecraft-Tastenbelegungen geändert werden.

Beim Start einer Untersuchung erscheint zunächst ein kurzer RP-Gedanke, beispielsweise:

- „Hm … ich sehe mir das genauer an …“
- „Mal sehen, was wir hier haben …“
- „Interessant … ich betrachte es genauer …“
- „Einen Augenblick … das will geprüft sein …“

Nach einer kurzen Verzögerung werden die erkennbaren Informationen eingeblendet. Während ALT gedrückt bleibt, bleibt der gewählte RP-Satz bestehen und wechselt nicht bei jeder kleinen Bewegung.

Untersucht werden können unter anderem:

- RP-Name und Titel eines Spielers
- Gegenstände in Haupt- und Nebenhand
- sichtbare Rüstung
- eigene Namen sichtbarer Waffen und Gegenstände
- herumliegende Gegenstände und Gegenstandsrahmen
- Bücher, Schilder, Banner und Lesepulte
- junge oder gezähmte Kreaturen
- sichtbare Ausrüstung von Rüstungsständern
- Türen, Tore, Lichtquellen, Kerzen und Pflanzen
- Honigstand von Bienenstöcken und Bienennestern

Es werden ausschließlich Informationen verwendet, die dem eigenen Client bereits bekannt sind. Versteckte Inventarinhalte, Lebenspunkte oder geheime Serverwerte werden nicht ausgelesen.

Der Untersuchungsmodus, die RP-Gedanken, die Untersuchungsdauer sowie einzelne Spielerinformationen können in den OttoExtra-Einstellungen angepasst oder vollständig deaktiviert werden.

### RP-Chatformatierung

Die RP-Chatformatierung ist ausschließlich in den **RP-Kanälen** aktiv. Dazu gehören **Murmeln (`/m`)**, **Flüstern (`/f`)**, **Sprechen (`/s`)**, **Rufen (`/r`)** und **Brüllen (`/b`)**. In den OOC-Kanälen **Offtopic (`/o`)** und **Hilfe (`/h`)** werden Nachrichten nicht durch die RP-Formatierung verändert.

In den RP-Kanälen erkennt der Chat automatisch RP-Emotes und OOC-Kommentare:

```text
*blickt den fremden misstrauisch an*
```

Emotes zwischen `*...*` werden standardmäßig **hellgrau und kursiv** dargestellt.

```text
(bin kurz afk)
```

OOC-Kommentare zwischen `(...)` werden standardmäßig **goldgelb** dargestellt.

Auch Kombinationen funktionieren:

```text
*blickt sich um (bin kurz weg) und setzt seinen weg fort*
```

Nach dem OOC-Kommentar wird automatisch wieder der vorherige Emote-Stil verwendet. Normaler Text behält immer die vom Server vorgegebene Farbe des jeweiligen Chatkanals.

Unter **Chatkanäle → RP-Chatformatierung** können ausschließlich die Farben für Emotes und OOC-Kommentare über Hexwerte wie `#FFD45A` selbst festgelegt werden. Die Farbe des normalen Textes wird nicht überschrieben, damit jeder Chatkanal seine serverseitig vorgegebene Farbe behält. Die gesamte Formatierung kann ebenfalls deaktiviert werden. Diese Einstellungen wirken nur in den RP-Kanälen.

### Lange Chatnachrichten

Nachrichten mit mehr als 256 Zeichen werden automatisch in mehrere Chatnachrichten aufgeteilt.

In den RP-Kanälen werden offene Emotes und OOC-Kommentare dabei über die einzelnen Teilnachrichten hinweg korrekt weitergeführt. Lange RP-Texte müssen dadurch nicht mehr von Hand getrennt werden. In den OOC-Kanälen werden keine RP-Farben oder Emote-Formatierungen angewendet.

## Installation

1. Fabric Loader für Minecraft `1.21.11` installieren.
2. Fabric API herunterladen und in den `mods`-Ordner legen.
3. Die OttoExtra-JAR ebenfalls in den `mods`-Ordner legen.
4. Optional ModMenu installieren, um die Einstellungen bequem zu öffnen.
5. Minecraft starten und dem Ottonien-Server beitreten.

Die Mod ist rein clientseitig. Auf anderen Servern bleiben die Ottonien-spezifischen Module inaktiv.

## Einstellungs-GUI

Die zentrale Konfiguration ist über **ModMenu → OttoExtra** oder über das belegbare OttoExtra-Tastenkürzel erreichbar.

Das Menü bietet unter anderem:

- Navigation zwischen allen Modulen
- getrennte Einstellungsbereiche pro Funktion
- Volltextsuche über die Optionen
- Erklärungen und Hover-Hilfen
- Zurücksetzen einzelner Werte
- Backup- und Wiederherstellungsfunktionen
- anpassbare Farben, Positionen und Anzeigeoptionen

## RP-Namen

OttoExtra kann Minecraft-Accountnamen durch **Titel und RP-Namen** ersetzen.

Die Anzeige lässt sich getrennt steuern für:

- Chat
- Tabliste
- Namensschilder über Spielern
- RP-Kanäle
- OOC-Kanäle

Als Datenquellen dienen:

1. manuelle Einträge im Personenbuch
2. gelernte Namen aus Chat-Hoverdaten
3. Daten aus öffentlichen Ottonien-APIs
4. importierte OttoPlus- oder OttoTalk-Daten

Manuelle Einträge haben Vorrang vor automatisch übernommenen Informationen.

### Anzeigeoptionen

- Titel ein- oder ausblenden
- RP-Namen ein- oder ausblenden
- Accountnamen ein- oder ausblenden
- Namensschilder skalieren
- eigene Farben für Chat, Tabliste und Namensschild
- Verhalten bei unbekannten Spielern festlegen
- Anzeige abhängig vom Chatkanal steuern

## Titelkatalog

Der Titelkatalog verwaltet bekannte Titel, Farben und Varianten.

Mögliche Kategorien sind beispielsweise:

- System
- Adel
- Klerus
- Vorkoster
- Fertigkeit
- Allgemein
- Custom
- Spielernamen

Beim Vergeben eines Titels kann die passende Farbe automatisch aus dem Katalog übernommen werden.

## RP-Personenbuch

Das Personenbuch speichert das lokale RP-Wissen über andere Spieler.

Funktionen:

- Personen suchen und filtern
- RP-Namen und Titel bearbeiten
- Accountnamen einsehen
- Farben pro Anzeigeort festlegen
- Notizen verwalten
- Vorschau für Chat, Tabliste und Namensschild
- Konflikte zwischen lokalen und importierten Daten auflösen
- Einträge gegen automatisches Überschreiben sperren
- OttoPlus- und OttoTalk-Daten importieren

### Schnellzugriff

Das Personenbuch kann optional direkt geöffnet werden über:

- Shift-Linksklick auf einen Namen im Chat
- Shift-Rechtsklick auf einen Spieler

## Chat-Kanäle

OttoExtra ergänzt das Chat-Eingabefeld um einen festen Kanal-Button.

| Kanal | Befehl |
|---|---|
| Murmeln | `/m` |
| Flüstern | `/f` |
| Sprechen | `/s` |
| Rufen | `/r` |
| Brüllen | `/b` |
| Offtopic | `/o` |
| Hilfe | `/h` |

Bedienung:

- Linksklick wechselt durch die RP-Kanäle.
- Shift-Linksklick wechselt durch die OOC-Kanäle.
- Ein normaler Linksklick führt wieder zu den RP-Kanälen zurück.
- Beim Wechsel wird der passende Serverbefehl verwendet.

Zusätzliche Optionen:

- automatisches `/s` kurz nach dem Serverbeitritt
- Warnmarker für den Offtopic-Kanal
- eigene Farben pro Kanal
- optionale Hotkeys
- eigene Farben für Emotes und OOC-Kommentare in RP-Kanälen
- RP-Chatformatierung für RP-Kanäle ein- oder ausschalten
- keine automatische RP-Formatierung in Offtopic und Hilfe

## Regionen-Einblendung

Beim Betreten eines Lehens zeigt OttoExtra eine gestaltete Einblendung an.

Mögliche Inhalte:

- Hinweis „Du betrittst“
- Regionsname
- Hierarchie
- Wappen
- Hinweistext
- Sonderbeschreibung

Anpassbar sind unter anderem:

- Position der Einblendung
- Light- oder Dark-Theme
- eigene Themes
- Farben und Abstände
- Schriftgrößen
- Live-Vorschau im Einstellungsmenü

## Karte und Reisehilfe

OttoExtra integriert Ottonien-Daten in **Xaero's World Map** und **Xaero's Minimap**.

Beide Xaero-Mods sind empfohlen, OttoExtra bleibt jedoch auch ohne sie nutzbar.

### World Map

Je nach Konfiguration können angezeigt werden:

- Lehengrenzen
- Lehensnamen
- Wappen
- politische Flächen nach Gefolge
- Vasallen-Schraffuren
- Aktivitätsbereiche
- NPC-Dörfer
- gemalte Ottonien-Übersichtskarte
- anklickbare Lehen mit Detailinformationen

Ein Klick auf ein Lehen kann eine Infokarte mit Herrschaft, Lehnsherr, Stand, Gefolge, Entfernung, Himmelsrichtung und geschätzter Reisezeit öffnen.

Der optionale Pergamentmodus ergänzt Rahmen, Kompass und Maßstabsleiste.

### Minimap

Mögliche Anzeigen:

- Grenzen und politische Flächen
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
- Cursor und Textauswahl
- Kopieren, Ausschneiden und Einfügen
- automatische Seitenerstellung bei längeren eingefügten Texten
- gespeicherte Entwürfe
- Wiederaufnahme nach einem Verbindungsverlust
- Statusanzeige während des Versands
- automatisches Schließen nach erfolgreichem Absenden
- neuer leerer Brief nach abgeschlossenem Versand

### Platzhalter

| Platzhalter | Funktion |
|---|---|
| `{{name:Spieler}}` | RP-Name des Spielers |
| `{{title:Spieler}}` | Titel des Spielers |
| `{{full:Spieler}}` | Titel und RP-Name |
| `{{mc:Spieler}}` | Minecraft-Accountname |

Die Platzhalter können im Editor aufgelöst werden.

## Resourcepack-Downloader

OttoExtra kann das Ottonien-Resourcepack prüfen, herunterladen und aktivieren.

Optionen:

- Prüfung beim Start
- automatische Aktivierung
- manuelles Deaktivieren respektieren
- erneute Prüfung bei Änderungen

## Import aus OttoPlus und OttoTalk

Bestehende RP-Daten können aus OttoPlus beziehungsweise OttoTalk übernommen werden.

Importierbare Angaben:

- RP-Name
- Titel
- Titelfarbe
- UUID-Zuordnung

Importdateien können hier abgelegt werden:

```text
config/ottoextra/import/
```

## Lokale Daten und Backups

OttoExtra speichert persönliche Einstellungen, das Personenbuch und weitere lokale Daten im Minecraft-Konfigurationsordner.

Vor größeren Änderungen können Backups erstellt und bei Bedarf wiederhergestellt werden. Diese Daten bleiben lokal auf dem eigenen Rechner, sofern sie nicht bewusst über eine vorgesehene Import- oder Exportfunktion weitergegeben werden.

## Datenschutz und Server-Interaktion

OttoExtra ist eine clientseitige Community-Erweiterung.

Die Mod verwendet für ihre Funktionen ausschließlich:

- Informationen, die der Minecraft-Client bereits erhalten hat
- normale Chatbefehle
- öffentliche Ottonien-APIs
- lokal gespeicherte Einstellungen und RP-Daten

Der RP-Untersuchungsmodus liest keine versteckten Inventare, Lebenspunkte oder internen Serverdaten aus.

## Aus dem Quellcode bauen

Vorausgesetzt werden ein passendes Java Development Kit und eine funktionierende Gradle-Umgebung.

Unter Windows:

```powershell
.\gradlew.bat clean build
```

Unter Linux oder macOS:

```bash
./gradlew clean build
```

Die erzeugten JAR-Dateien befinden sich anschließend normalerweise unter:

```text
build/libs/
```

## Hinweis

OttoExtra ist eine Community-Erweiterung für Ottonien und kein offizieller Bestandteil von Mojang, Microsoft oder Fabric.
