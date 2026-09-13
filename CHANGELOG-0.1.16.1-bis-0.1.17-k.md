# OttoExtra – Changelog 0.1.16.1 bis 0.1.17-k

**Zeitraum: 21. August bis 8. September 2026 · Zielversion: 0.1.17-k Alpha**

Zusammenfassung einschließlich der Änderungen aus 0.1.16.1 und der historischen
Zwischenstände `1.16.3` bis `1.16.3-w`. Die folgenden Abschnitte beschreiben den
Stand von 0.1.17-k; die Versionsübersicht ordnet die einzelnen Schritte ein.

## Neue Features

### Briefe und TXT-Archiv

- **Geschriebene Briefe später verschicken:** Ein unversendeter Brief öffnet über
  **Verschicken…** erneut die Empfängerauswahl. Das funktioniert auch nach einem
  erneuten Login oder über `/ottoextra letter send`.
- **Automatisches Versandarchiv:** Nach Couriers Versandbestätigung wird der
  tatsächliche Buchinhalt lokal als `letter_Datum_Uhrzeit.txt` gespeichert.
  Auch manuelles `/post Spielername` wird berücksichtigt.
- **Empfangene Briefe exportieren:** **TXT exportieren** im geöffneten Brief
  speichert eine Datei als `Spielername_Datum_Uhrzeit.txt`. Maßgeblich sind der
  im Buch gespeicherte Autor und der Exportzeitpunkt.
- **TXT wieder importieren:** Unter **Gespeicherte Entwürfe → Briefe als TXT
  importieren** lassen sich archivierte oder eigene UTF-8-Dateien als neue,
  vollständig bearbeitbare Briefe öffnen. Ein vorhandener Entwurf wird vorher
  gesichert.
- **Lokaler Letters-Ordner:** Dateien liegen unter `config/ottoextra/letters/`.
  Farben, Schriftstile und Seitenwechsel bleiben im Archiv erhalten; gleiche
  Dateinamen werden durch einen zusätzlichen Zähler unterschieden.

### Tierübersicht und Nametags

- **Tierübersicht direkt in OttoExtra:** Eigene und ausdrücklich freigegebene
  Tierakten mit Suche nach Name, Hof, Herkunft und UUID, Modellvorschau, Finder
  sowie Löschen nach Bestätigung und Berechtigungsprüfung.
- **Höfe gruppieren und sortieren:** Höfe lassen sich auf- und zuklappen.
  Tempo und Sprunghöhe können gleichzeitig mit eigener Richtung und Priorität
  sortiert werden; fehlende Werte stehen hinten.
- **Weitere Tierarten:** Esel können eingetragen werden. Die Übersicht bietet
  **Alle**, **Pferde** und **Esel**; Rinder, Brakel, Hausschweine, Ziegen und Zaupel
  sind zusätzlich in den Einstellungen zuschaltbar.
- **Benennen im eigenen Lehen:** Die Tierakte unterstützt auch Nutztiere und
  berücksichtigt die vorhandenen Rechte für Erstbenennung und Bearbeitung.
- **Geschlecht im Nametag:** Geschlechtssymbol und Geschlechtsfarbe des Namens
  lassen sich unabhängig voneinander aktivieren – auch für nicht eingetragene
  Tiere, deren Tierinfo bereits bekannt ist.
- **Lokaler 14-Tage-Tiercache:** Erkannte Tierart und Geschlecht bleiben über
  Disconnects und Neustarts erhalten, höchstens 1.024 Tiere je Serveradresse.
  Neue Tierinfos erneuern die Frist. Die Funktion ist abschaltbar und erzeugt
  keine automatischen Einträge in der gemeinsamen Tierdatenbank.

### Karte, RP-Einrichtung und Komfort

- **Minimap-Texthöhe per Drag & Drop:** Breite und Höhe von Name, Stand und
  Gefolge lassen sich unabhängig von der Schriftgröße einstellen. Längere
  Angaben wie „Gefolge in Gefolge XYZ“ können dadurch mehrzeilig erscheinen.
- **Lehen-Details auf Pergament:** Die Weltkarte zeigt Wappen, Herrschaft,
  Führung, Bewohner, Lehnsherren, Beschreibungen und Koordinaten in einer
  scrollbaren Detailansicht. Lehen-Klicks bewegen die Karte weich zum Ziel.
- **RP-Ersteinrichtung:** Die Profile **Komfort**, **Ausgewogen** und **Immersiv**
  bieten einen schnellen Einstieg. Elf Anzeigeoptionen lassen sich vor dem
  Übernehmen einzeln anpassen; die Einrichtung ist überspringbar.
- **Lokales Chatprotokoll:** Chat- und Systemnachrichten werden standardmäßig
  mit Zeitstempel in `logs/ottoextra-chat.log` gespeichert, unabhängig vom
  Debug-Modus. Abschaltbar unter **Tweaks → Basis → Chatprotokoll**.
- **Kampfmodus-Anzeige:** Ein optionales Icon neben der Hotbar zeigt die
  erkannte Kampftechnik. Zwölf Techniken und unterschiedliche Waffenklassen
  werden unterstützt.
- **Voting integriert:** Konfigurierbare Vote-Erinnerung direkt in OttoExtra;
  beim Öffnen der Vote-Seiten wird der Minecraft-Name in die Zwischenablage
  kopiert. Alte Einstellungen werden übernommen.
- **Litematica-Komfort:** Lokalisierte Inventarnamen aus dem Resourcepack und
  Erkennung der 28 Fachwerkvarianten mit passender Farbe und Bauform.
- **Aktivitäts-HUD:** Frei belegbare Taste zum Ein-/Ausblenden, verständlichere
  Positionsnamen sowie getrennte Schalter für Uhrzeit und letzte Aktualisierung.

## Verbesserungen

- **Courier-V2 überarbeitet:** Die Briefplanung ist auf die bereitgestellte,
  angepasste Courier-Version 2.1.0 abgestimmt. Vorschau und Schreibbefehle
  verwenden denselben Plan für Seiten, Formatierung und Leerraum.
- **Klarere Seitenangaben:** Der Editor unterscheidet Arbeitsseiten von den
  voraussichtlichen Buchseiten. Schreibschutz- und Nur-Lesen-Hinweise haben
  eigenen Platz und bleiben auch beim Scrollen verständlich.
- **Kompaktere Tieranzeige:** Austauschbare 16 × 16-PNGs werden standardmäßig
  mit 8 × 8 GUI-Pixeln dargestellt. Tempo und Sprung zeigen Symbol, Wert und
  Einheit; gespeicherte Layouts bleiben erhalten.
- **Lesbarere Tierliste:** Fehlende Tiernamen erscheinen als `???`, darunter
  klein die UUID. Hofnamen, Herkunft und Symbole sind kompakter. Lange Namen
  werden gekürzt, bevor sie andere Angaben überdecken.
- **Schnellere Tierinfos:** Der frühere Standard für stabiles Anvisieren wurde
  von 300 auf 100 ms verkürzt; der globale Abstand bei Zielwechseln von 750 auf
  350 ms. Bewusst eingestellte andere Hover-Zeiten bleiben erhalten.
- **Titel und Farben:** Titelfarben richten sich standardmäßig nach dem
  angezeigten Titel. Die Übernahme der Adelsfarbe ist optional; der aktualisierte
  Titelkatalog erhält eigene Farben und Varianten bei der Migration.
- **Bessere Bedienbarkeit:** Erststart-Seiten passen auch bei hoher
  GUI-Skalierung ins Fenster. Lange Texte sind scrollbar; Buttons bleiben
  erreichbar und verwenden einen einheitlichen dunkelblauen und goldenen Stil.
- **Kartenintegration:** Grenzen, Namen und Wappen liegen unter Xaeros Buttons,
  Menüs und Tooltips. Eigene XaeroPlus-Zeichnungen werden bei der Darstellung
  und der Behandlung bereits verbrauchter Mausklicks berücksichtigt.

## Fixes

### Briefe und Chat

- **Falsche Pergament-Fehlermeldung behoben:** Ein einzelnes Pergament im aktiven
  Slot wird auch mit weiterem Vorrat rechts davon, im Hauptinventar oder in der
  Nebenhand beschrieben. Material weiter links wird nach Couriers tatsächlicher
  Entnahmereihenfolge erkannt und verständlich gemeldet.
- **Cursor im V2-Editor korrigiert:** Manuelle Umbrüche auf späteren Seiten und
  unsichtbare Umbruch-Leerzeichen verschieben nachfolgende Eingaben nicht mehr.
- **Sicheres Fortsetzen:** Briefinhalt, Spieler, Verbindung und Slot werden
  geprüft. Ein anderes Buch oder ein alter Entwurf wird beim Wiederöffnen
  nicht versehentlich übernommen oder erneut beschrieben.
- **Empfangene Briefe korrekt erkannt:** Sie erhalten keinen Bearbeiten- oder
  Versandbutton, können aber als TXT exportiert werden.
- **TXT ohne Datenverlust:** Fehlgeschlagene Sendungen erzeugen kein
  Versandarchiv. Beschädigte Importdateien verdrängen keinen Entwurf; ein alter
  Exportbutton übernimmt kein zwischenzeitlich ausgetauschtes Buch.
- **Langchat an die Verbindung gebunden:** Wartende Nachrichtenteile werden
  beim Disconnect oder Verbindungswechsel verworfen. Weitere Eingaben umgehen
  die Sendepause nicht mehr.
- **Vollständiger Chatverlauf:** Lange Nachrichten bleiben als vollständige
  Eingabe über Pfeil hoch erreichbar. RP-Formatierung berücksichtigt den
  gewählten Kanal und die aktivierte Einstellung; verzögerte Nachrichten werden
  nicht doppelt protokolliert.

### Tiere, Tabliste und Karte

- Tierinfo-Abfragen werden beim Reiten auf Pferden, Eseln und Maultieren
  unterbrochen, damit keine unbeabsichtigten Interaktionen ausgelöst werden.
- Pferdeinfos verwenden den abgesicherten Sneak-Abfragepfad. Slot-Synchronisierung,
  Wiederholungen nach Timeouts und die Verarbeitung unterschiedlicher
  Servernachrichten wurden korrigiert.
- Tierantworten werden im Clientthread und ohne doppelte Auswertung verarbeitet;
  Toasts bleiben dadurch mit dem Anfragezustand synchron.
- Die persönliche Tierübersicht zeigt nicht mehr pauschal sämtliche Tiere
  eines eigenen oder freigegebenen Hofes an.
- Die Benennung durch Lehensmitglieder funktioniert auch dann, wenn die
  Lehenszuordnung nur über die alternative Fraktionsreferenz geliefert wird.
- Tablisten-Sortierdaten werden beim Öffnen sowie nach erneutem Aktivieren
  aktualisiert. Während des Ladens bleibt die vorhandene Sortierung erhalten.
- Fehlende Tablisten-Anzeigenamen nach Proxy-Rejoin können auf aktuelle
  Scoreboard-Teamdaten zurückgreifen; Wappen und Abstände bleiben dabei erhalten.
- Feste Regionswappen haben im Regionstoast Vorrang, auch bei Lehen ohne
  Spielerfraktion. Karten- und Toastanzeige verwenden dieselbe Wappenauswahl.
- Minimap-Texte behalten beim Vergrößern ihre Verankerung; der Größenanfasser
  bleibt auch am Bildschirmrand erreichbar.
- Kopierte Kartentexturen und ihre Ansichten werden vollständig freigegeben.

### Einstellungen, Daten und Kompatibilität

- Backup und Wiederherstellung übernehmen aktive RP-Daten, Briefentwürfe und
  die Chatprotokoll-Einstellung korrekt.
- RP-Daten bleiben bei einer leeren Erstinstallation und nach einer Reparatur
  schreibbar; wartende Änderungen werden nicht verworfen.
- Rückmeldungen nach Backup, Reparatur oder Wiederherstellung bleiben beim
  Neuaufbau des Einstellungsfensters sichtbar.
- Veraltete Kontoantworten überschreiben keine neueren Ergebnisse mehr.
  Fehlerhinweise bleiben auch bei weiterhin angezeigten alten Daten sichtbar.
- Parallele Zugriffe auf die Kontoverknüpfung beschädigen den gespeicherten
  Device-Token nicht mehr durch konkurrierendes Zurückschreiben.
- Fehlende Resourcepacks und Änderungen der Downloadquelle werden erkannt;
  ein alter Downloadcache verhindert dann keine erneute Installation.
- Einstellungen lassen sich auch ohne Xaeros Minimap öffnen. Optionale
  Karten- und Litematica-Integrationen bleiben optional.
- Ein nachgewiesener XaeroPlus-Startabsturz im Entwicklungsclient wurde durch
  die eindeutige Unterscheidung der unterstützten Schnittstellen behoben.
- Die Einstellung zum Abschalten der Unschärfe bei wenig Leben wird beachtet.
- Die MIT-Lizenzdatei ist in der Mod-JAR enthalten.

## Versionsübersicht

| Version | Schwerpunkt |
| --- | --- |
| **0.1.16.1** | Vote- und Litematica-Integration, Fachwerkerkennung, HUD-Taste und keine automatischen Tierabfragen beim Reiten. |
| **1.16.3 bis 1.16.3-w** | Tierübersicht, Hof-/Wertesortierung, Tierabfrage-Fixes, Kampfmodus, Tablisten- und Kartenkorrekturen. |
| **0.1.17-a** | Erste weitergebbare Alpha des überarbeiteten Stands; Courier-V2-Abgleich, RP-Einrichtung und zwölf Auditkorrekturen. |
| **0.1.17-b** | Korrekte Prüfung von Pergament und zusätzlichem Vorrat. |
| **0.1.17-c** | Geschriebene, unversendete Briefe später erneut zum Versand öffnen. |
| **0.1.17-d** | Kleine 16 × 16-Tiericons und korrigierte Toast-Titelbreite. |
| **0.1.17-e** | Geschlechtssymbol und Namensfarbe im Tier-Nametag getrennt einstellbar. |
| **0.1.17-f** | Esel, Tierartenfilter, kompakte Liste und Nametags für lokal bekannte, ungespeicherte Tiere. |
| **0.1.17-g** | Minimap-Texthöhe und mehrzeilige Angaben per Drag & Drop. |
| **0.1.17-j** | Lokaler Tiercache über Disconnects und Neustarts, bis zu 14 Tage. |
| **0.1.17-k** | Automatisches TXT-Versandarchiv, Export empfangener Briefe und erneuter TXT-Import. |

## Teststand und Hinweise

- **0.1.17-k:** 703 Unit-Tests und alle 100 lokalen Server-/Client-Testfälle
  bestanden, einschließlich eines vollständigen vierseitigen TXT-Rundlaufs.
- Basis bleibt **Minecraft Java 1.21.11, Java 21 und Fabric**. OttoExtra wird
  ausschließlich im Client installiert; die Briefprüfungen verwenden die
  angepasste Courier-2.1.0-Version auf dem lokalen Testserver.
- Der Inventar-Crash mit Mouse Wheelie wurde im **lokalen Yarn-Devclient**
  durch eine korrigierte Drittanbieter-Kopie behoben. Diese Korrektur ist kein
  Bestandteil der weitergegebenen OttoExtra-JAR.
- Die bekannte Meldung über veraltete Tablisteneinträge nach Logout ist
  weiterhin nicht als behoben belegt. Frühere vereinzelte Renderbeobachtungen
  und die vollständige Abnahme aller optionalen Modkombinationen bleiben offen.

Grundlagen: [chronologischer Changelog](CHANGELOG.md),
[Auditkorrekturen](docs/audit-2026-09-08/UMSETZUNG.md),
[Alpha-A-Hinweise](release-notes-0.1.17-a.md),
[Alpha-K-Validierung](docs/releases/0.1.17-k.md),
[lokaler Inventar-Fix](docs/dev-client/inventory-mousewheelie.md).
