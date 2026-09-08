# Changelog

## 0.1.17-d – Alpha – 2026-09-08

- Der Tierinfo-Toast verwendet transparente 16 × 16-Symbole für Geschlecht,
  Mast, Paarung, Scheren, Melken, Tempo und Sprung, mit 8 × 8 GUI-Pixeln Basisgröße.
- Tempo und Sprung zeigen Symbol, Wert und Einheit und passen damit in ihre
  Standardspalten. Die gespeicherten Layout-Einstellungen bleiben erhalten.
- Die Titelbreite berücksichtigt Fettschrift und Geschlechtselemente in derselben
  Zeile. Zu lange Namen werden mit „…“ gekürzt, statt die Symbole zu überdecken.
- [Darstellung und Clientprüfung](docs/animal-toast-icons.md).
- [Installation und Testerhinweise](release-notes-0.1.17-d.md).

## 0.1.17-c – Alpha – 2026-09-08

- Die Buchansicht unversendeter Briefe bietet neben **Bearbeiten** den Button
  **Verschicken…** zum erneuten Öffnen der Empfängerauswahl.
- `/ottoextra letter send` verwendet einen gehaltenen unversendeten Brief auch
  nach erneutem Login oder nach Ablauf des alten Chat-Hinweises.
- Das Wiederöffnen erhält Buchseiten und separate Entwürfe; Materialverbrauch
  oder erneute Schreibbefehle werden dadurch nicht ausgelöst.
- Buch-, Slot- und Sitzungsprüfung bleiben aktiv; andere angezeigte Bücher und
  empfangene Briefe bieten den neuen Versandbutton nicht an.
- [Installation und Testerhinweise](release-notes-0.1.17-c.md).

## 0.1.17-b – Alpha – 2026-09-08

- Briefschreiben wird nicht mehr wegen weiteren Pergaments rechts vom aktiven
  Hotbar-Slot, im Hauptinventar oder in der Nebenhand blockiert.
- Material weiter links wird entsprechend Couriers tatsächlicher Entnahmereihenfolge
  erkannt. Der Hinweis nennt eine sichere Anordnung, statt einen Stapel im aktiven
  Slot zu behaupten. Abweichende Modelle allein umgehen diese Prüfung nicht.
- Konkreter Materialfehler bleibt direkt im Editor sichtbar; der Entwurf bleibt erhalten.
- Reproduktion vor der Korrektur und zusätzliche echte Inventar-/Versandtests:
  [Nachweis](docs/briefmod/pergament-slot-fix-0.1.17-b.md).
- [Installation und Testerhinweise](release-notes-0.1.17-b.md).

## 0.1.17-a – Alpha – 2026-09-08

- Erste weitergebbare Alpha des aktuellen Entwicklungsstands mit Courier-V2,
  Tierinfo/Pferdeübersicht, RP-Ersteinrichtung, Karten- und Chat-Erweiterungen.
- Zwölf Auditpunkte vom 8. September umgesetzt: verbindungsgebundener Langchat
  mit stabiler Sendepause und Verlauf, sichere Config-/RP-Wiederherstellung,
  Texturfreigabe, aktuelle Kontoantworten und quellenabhängiger Resourcepack-Cache.
- Briefeditor unterscheidet Arbeits- und Buchseiten; Vorschau und Versand verwenden
  dieselbe Planung. Sperrhinweise und Backup-Rückmeldungen bleiben erkennbar.
- Alpha-Verteilung und bekannte offene Beobachtungen:
  [Release-Notizen](release-notes-0.1.17-a.md).
- Nachweise zu allen zwölf Punkten:
  [Audit-Umsetzung](docs/audit-2026-09-08/UMSETZUNG.md).

### Weitere enthaltene Änderungen aus der Vorbereitung

- Lehen-Klick auf der Worldmap nutzt dieselbe weiche Kamerabewegung wie Wegpunkte.
  Der neue Pergament-Infokasten zeigt Wappen, Herrschaft, Lehen und Koordinaten,
  ausgeschriebene Angaben zu Führung, Bewohnern und Lehnsherren sowie vorhandene
  Kurzbeschreibungen. Längere Texte sind scrollbar; Entfernung und Reisezeiten bleiben sichtbar.
- Lehen-Texte unterscheiden unabhängige Herrschaften, Vasallen und NPC-Lehen.
  Vogteien und Abteien werden korrekt ausgeschrieben, der Rang der übergeordneten
  Herrschaft wird genannt. NPC-Texte stehen ohne künstliche Bewohner-/Vasallenangaben;
  Holdern erhält die ausführliche Beschreibung. Anzeigename und ursprünglicher
  Lehensname bleiben getrennt (Mährstein / Heldweg).
- Titelfarbe richtet sich standardmäßig nach dem angezeigten Titel. Die bisherige
  Farbübernahme vom Adelsstand ist unter RP-Namen optional zuschaltbar.
  Vorkoster/in, Legende/Legendin und Chronist/in behalten ihre festen Standardfarben.
- Titelkatalog mit der aktuellen Wiki-Liste abgeglichen, fehlende Titel ergänzt
  und Standzuordnungen korrigiert; bestehende Kataloge werden unter Erhalt eigener
  Farben und Titelvarianten aktualisiert.

- Einstellungen lassen sich wieder ohne Xaeros Minimap öffnen; der optionale
  Wappen-Editor wird erst bei vorhandener Kartenmod geladen.
- Standardmäßig aktives Chatprotokoll unter Tweaks: alle Chat-/Systemnachrichten
  mit Zeitstempel in `logs/ottoextra-chat.log`, unabhängig vom Debug-Modus und
  über Neustarts erhalten; verzögerte Nachrichten ohne Doppeleinträge.
- Überspringbare RP-Einrichtung nach den Erststart-Hinweisen: Komfort,
  Ausgewogen und Immersiv als drei Karten mit Auswahlbuttons nebeneinander.
  Elf An/Aus-Schalter für Chat/Tabliste, Namensschilder und Kennenlernen erlauben
  individuelle Details vor dem Übernehmen; Zurück erhält die Entwürfe.
- Einheitlicher dunkelblauer und goldener Stil für alle Erststart-Seiten;
  Zurück, Ablehnen und Erlauben stehen auch bei den Grundeinstellungen nebeneinander.
- Erststart-Hinweise passen auch bei hoher GUI-Skalierung vollständig ins
  Fenster. Längere Texte sind scrollbar; die Auswahl-Buttons bleiben erreichbar.
- XaeroPlus-Minimap-Hooks unterscheiden die Schnittstellen mit und ohne
  Zoomargument auch beim Mixin-Remapping im Entwicklungsclient exakt.
- Die gebaute Mod-JAR enthält jetzt die MIT-Lizenzdatei.

## 1.16.3-w – Mehrere Sortierkriterien in der Tierübersicht – 2026-09-07

- Tempo und Sprunghöhe gleichzeitig sortieren: Auswahlreihenfolge bestimmt die
  Priorität; weitere Kriterien entscheiden bei Gleichstand. Richtung je Kriterium
  einzeln ändern, ohne andere Kriterien oder die Hofreihenfolge zurückzusetzen.
- Priorität 1/2 direkt im Hofkopf und Sortiermenü. Der Menü-Pfeil setzt ein
  Kriterium nach vorne; erneute Auswahl entfernt es. Separate Reset-Option.
- Hofkopf-Klick wechselt absteigend → aufsteigend → aus. Klappzustände bleiben erhalten.

## 1.16.3-v – Tiermenü und sortierbare Pferdeübersicht – 2026-09-07

- Tierregister: kleinere UUID und gemeinsame Icons für Geschlecht, Tempo,
  Sprung, Mast, Paarung sowie Scheren/Melken; Werte mit erklärenden Tooltips.
- Tempo und Sprunghöhe innerhalb der Höfe numerisch auf-/absteigend sortieren,
  über den Hofkopf oder das Sortiermenü. Fehlende Werte bleiben hinten.
- Höfe lassen sich einzeln auf-/zuklappen; Zustand bleibt beim Sortieren,
  Filtern und Abgleichen erhalten. Neue Pfeil-Icons liegen als austauschbare PNGs vor.

## 1.16.3-u – Karten-Overlays unter der Bedienoberfläche – 2026-09-07

- Grenzen, Namen, Wappen, politische Flächen und Karteninformationen werden
  vor Xaeros Buttons, Dropdowns, Kontextmenüs und Tooltips gezeichnet.
- Eigene GUI-Ebenen sichern die Reihenfolge auch beim gebündelten Rendering.

## 1.16.3-t – Tiernamen für Lehensmitglieder – 2026-09-07

- Bei der ersten Benennung eines Tieres wird die Lehenszuordnung auch aus
  `faction_ref` erkannt, wenn die Regionsdaten kein `region_id` enthalten.
  Mitglieder sehen dadurch im eigenen Lehen das Namensfeld statt nur „–“.

## 1.16.3-s – Tabliste beim Öffnen aktualisieren – 2026-09-06

- Bei jedem erneuten Öffnen mit Tab werden die Lehen- und Rangdaten für die
  aktivierte Tablisten-Sortierung neu geladen und anschließend berücksichtigt.
- Während des Ladens und bei Abruffehlern bleibt die bisherige Sortierung
  erhalten. Gedrückthalten löst keine weiteren Abrufe aus; laufende Anfragen
  werden geteilt und Antworten aus vorherigen Sitzungen verworfen.

## 1.16.3-n – Mährsteiner Wappen im Regionstoast – 2026-09-06

- Der Betreten-Toast und das Vorladen berücksichtigen jetzt feste Regionswappen
  vor Fraktionswappen. Damit erscheint das Mährsteiner Banner auch bei Lehen
  ohne Spielerfraktion, etwa Klepburg, Eberfeld, Miessteig, Riedorf und Berhausen.
- Toast und Karte verwenden dieselbe Wappenauswahl und denselben Cacheeintrag
  für Regionsnamen und Polygon-IDs. Bereits geladene Wappen werden wiederverwendet.

## 1.16.3-m – Kampfmodus neben der Hotbar – 2026-09-06

- Unter Tweaks lässt sich das neue Kampfmodus-Icon ein- und ausschalten.
  Es erscheint auf Ottonien nur mit einer Waffe in der Haupthand, rechts neben
  der Hotbar im gespiegelten Abstand des Offhand-Slots.
- Alle zwölf Kampftechniken werden aus beiden Actionbar-Paketformen erkannt.
  Der zuletzt erkannte Modus bleibt pro Waffenklasse für die laufende Sitzung
  gespeichert. Vor der ersten Erkennung erscheint das Waffenklassen-Icon mit `?`.
- Stumpf- und Stangenwaffen werden auch dann erkannt, wenn sie technisch
  Schwert-Items sind: über Itemmodell, Kategorie in der Lore oder vorhandene
  Ottonien-Waffennamen. Bögen, Armbrüste und Speer-Items werden ebenfalls erkannt.
- Die Anzeige verwendet die vorhandenen Talent-Icons aus dem Ressourcenpaket
  und lässt Platz für eine rechte Offhand oder die Hotbar-Angriffsanzeige.

## 1.16.3-l – Tiernamen für Nutztiere – 2026-09-06

- Das Benennungsmenü auf `B` unterstützt jetzt zusätzlich Kühe, Schafe,
  Hühner, Ziegen und Schweine; Pferde, Esel und Maultiere bleiben unterstützt.
- Tierregister mit passendem Tiermodell, Tiernamen und Scher-/Melkinfos;
  Tierübersicht, Namensanzeige und Finder unterstützen die zusätzlichen Tiere.
- AnimalBreeding 0.5.12 öffnet für die zusätzlichen Nutztiere das gemeinsame
  Benennungsmenü, auch ohne Züchtermodus. Die vorhandenen serverseitigen
  Regeln für Erstbenennung und Bearbeitungsfreigaben gelten weiterhin.

## Lokale Tablisten-Diagnose – 2026-09-06

- Für Spieler, die erst nach einem eigenen Reconnect aus der Tabliste verschwinden,
  protokolliert der vorhandene Debug-Modus jetzt An-/Abmeldungen und Sichtbarkeits-
  bzw. Anzeigenamen-Updates vor und nach der Paketverarbeitung. Dieser Nachtrag
  dient der Ursachenklärung; der gemeldete dauerhafte Eintrag ist noch nicht behoben.

## Lokaler Tablisten-Fix – 2026-09-06

- Fehlt nach einem Proxy-Rejoin der separate Tablisten-Anzeigename, erhält die
  RP-Namensanzeige jetzt Wappen, Präfix und Suffix aus dem aktuellen Scoreboard-Team.
  Später eintreffende Team-Daten werden beim nächsten Anzeigen berücksichtigt.
- Bei übersetzten Titelkomponenten bleibt der Abstand zwischen Wappen und Name
  auch nach Titelersetzung oder ausgeblendeten Titeln erhalten.

## Lokaler XaeroPlus-Testbuild – 2026-09-06

### Behoben

- XaeroPlus-Zeichnungen werden nach OttoExtras gemalter Kartenbasis ausgegeben;
  bereits konsumierte Zeichen-Klicks lösen keine OttoExtra-Gebietsauswahl aus.
- Auf Nutzerwunsch werden bei aktivem FairPlay ausschließlich eigene Linien,
  Zeichenvorschau und Texte auf Weltkarte und Minimap zusätzlich gerendert, auch ohne
  aktives OttoExtra-Kartenmodul. Server-FairPlay, Radar und alle anderen
  gesperrten Features bleiben unverändert; Highlights sind nicht freigeschaltet.
- Minimap-Erweiterung: eigener Renderhook für XaeroPlus 2.34.5 (mit Zoom) und
  2.31.5 (ohne Zoom), mit Minimap-Kontext und passendem Kameraursprung.

### Kompatibilität

- Optionale Integration für XaeroPlus unter Minecraft Fabric 1.21.11; keine neue
  Pflichtabhängigkeit. Ingame-Abnahme und Nachweise: `docs/bugs/XAEROPLUS-ANALYSE.md`.

## [1.16.3-k] – 2026-09-04

### Behoben

- Die Tablisten-Sortierung nach Lehen/Gefolgen erkennt das Aus- und erneute
  Einschalten während einer laufenden Sitzung und lädt ihren Snapshot sofort
  neu, statt im alten deaktivierten Zustand zu bleiben.
- Fehlen die Sortierdaten oder scheitert ihr API-Abruf, folgt alle fünf Sekunden
  ein neuer Versuch. Nach einem erfolgreichen Snapshot enden die Netzwerk-Retries;
  die Tabliste selbst wird bei jeder Darstellung mit den aktuellen Daten sortiert.
- Der Aktivierungswächter wird auch dann initialisiert, wenn Regions- oder
  RP-Namen-Funktionen beim Clientstart ausgeschaltet sind.

## [1.16.3-j] – 2026-09-04

### Geändert

- Der bisherige Tierinfo-Standard von 300 ms stabilem Hover wird einmalig auf
  100 ms migriert. Bewusst gewählte andere Hover-Zeiten bleiben erhalten.
- Der globale Abstand bei schnellen Zielwechseln sinkt von 750 auf 350 ms.
  Laufende Serverantworten werden weiterhin nicht parallelisiert, damit weder
  Paketspam noch vermischte Tierinfo-Blöcke entstehen.

## [1.16.3-i] – 2026-09-04

### Behoben

- Alle Pferde verwenden nun sofort den expliziten Sneak-Abfragepfad. Die
  Funktion hängt damit nicht mehr davon ab, ob Minecraft den Leinenzustand des
  anvisierten Pferdes bereits clientseitig synchronisiert hat.
- Tierinfo-Antworten werden an der gemeinsamen Chat-HUD-Grenze ausgewertet und
  funktionieren dadurch auch bei signierten oder vom Proxy anders verpackten
  Nachrichten. Doppelte Auswertung über mehrere Paketpfade wurde entfernt.
- Der Debug-Modus protokolliert Zielwechsel, Abfragepfad, vollständige Antworten
  und Timeouts der Tierinfo gezielt in `logs/debug.log`.

## [1.16.3-h] – 2026-09-04

### Behoben

- Angeleinte Pferde verwenden sofort den expliziten Sneak-Abfragepfad, damit
  der Server den automatischen Klick nicht als Aufsteigeversuch behandelt.
- Eine serverseitige Aufforderung zum Losbinden beendet die laufende Anfrage
  sofort. Nach einem endgültigen Fehlschlag bleibt der Pferde-Toast über die
  UUID-basierte Datenbank oder die letzte Beobachtung verfügbar, ohne Abfragen
  anderer Tiere zu blockieren.

## [1.16.3-g] – 2026-09-04

### Behoben

- Tierinfo-Antworten werden erst nach Minecrafts Wechsel vom Netzwerk- in den
  Clientthread ausgewertet. Dadurch bleiben Anfragezustand, Cache und Toast
  synchron und die Anzeige kann nicht mehr sporadisch nur kurz erscheinen oder
  nach einer Antwort aussetzen.

## [1.16.3-f] – 2026-09-04

### Behoben

- Automatische Tier- und Pferdeinfos werden auch verarbeitet, wenn der Server
  sie als profillose Systemchat-Nachrichten statt als normale Game-Message
  sendet. Manuelle Tierinfos ohne laufende OttoExtra-Abfrage bleiben sichtbar.

## [1.16.3-e] – 2026-09-03

### Behoben

- Tierinfo-Abfragen synchronisieren vor dem unsichtbaren Shift-Rechtsklick den
  ausgewählten Hotbar-Slot. Bleibt eine Antwort aus, folgt ein abgesicherter
  Sneak-Fallback; nach dem Backoff kann auch dasselbe anvisierte Tier erneut
  abgefragt werden.
- Die persönliche J-Übersicht zeigt nicht mehr automatisch alle Tiere eines
  eigenen oder freigegebenen Hofes. Sichtbar sind nur selbst gespeicherte bzw.
  besessene Pferde und ausdrücklich freigegebene Pferdeakten.
- Die Pferdemodelle sitzen in den kompakten Zeilen etwas tiefer.
- Die Hofsortierung befindet sich oberhalb der Liste und öffnet über
  „Sortieren nach“ eine Auswahl für A–Z oder Z–A.
- Lupe, Mülleimer, Geschlecht, Tempo und Sprunghöhe verwenden austauschbare
  16×16-PNG-Ressourcen statt fest im Java-Code gezeichneter Symbole.

## [1.16.3-d] – 2026-09-03

### Hinzugefügt

- Die J-Pferdeübersicht ist jetzt vollständig in OttoExtra enthalten und zeigt
  ausschließlich eigene sowie über Höfe oder Pferdeakten freigegebene Tiere.
- Pferde werden kompakt nach Höfen gruppiert und wahlweise A–Z oder Z–A
  sortiert. Die Suche berücksichtigt Name, Hof, Herkunft und UUID.
- Jede Zeile zeigt ein echtes Pferdemodell mit gespeicherter Fellfarbe und
  Zeichnung, Name und Herkunft sowie farbiges Geschlecht, Tempo und Sprunghöhe.
- Pixelgenaue Lupen- und Mülleimer-Symbole ersetzen problematische Schriftzeichen.
  Berechtigte Besitzer können Pferdeakten nach einer Bestätigung löschen.

## [1.16.3-c] – 2026-09-03

### Hinzugefügt

- Die AnimalBreeding-API kann eine Pferdeakte über ihre Entity-UUID löschen;
  Besitz und Berechtigung werden weiterhin serverseitig geprüft.

## [1.16.3-b] – 2026-09-02

### Behoben

- Parallele Leser der Minecraft-/Discord-Verknüpfung schreiben die
  Zugangsdaten nicht mehr beim Laden zurück. Eindeutige temporäre Dateien und
  ein gemeinsamer Dateilock verhindern, dass ein gültiger Device-Token durch
  konkurrierende Addons verloren geht.
- Der Schreibschutz prüft die Anzahl von „Pergament und Feder“ nur noch im
  aktiven Hotbar-Slot. Weitere Exemplare im übrigen Inventar blockieren den
  Schreibvorgang nicht; beim Fortsetzen muss weiterhin der angefangene Brief in
  der Haupthand liegen.

## [1.16.3-a] – 2026-09-02

### Behoben

- Im V2-Briefeditor bleibt der Cursor nach einem manuellen Umbruch auf späteren
  Seiten exakt an der sichtbaren Position. Unsichtbare Leerzeichen von
  automatischen Zeilen- und Seitenumbrüchen verschieben nachfolgende Eingaben
  nicht mehr um ein Zeichen oder in den vorherigen Absatz.

## [1.16.3] – 2026-09-02

### Geändert

- Die festen Positionen des Aktivitäts-HUDs werden im Platzierungsdialog
  verständlich als „Mitte links“, „Oben links“ und weitere Positionen angezeigt.
- „Letzte Aktualisierung“ und die aktuelle Uhrzeit lassen sich getrennt
  ein- und ausblenden; dadurch ist auch eine reine Uhrzeitanzeige möglich.

## [0.1.16.1] – 2026-08-21

### Tweaks – Neu

- **Voting:** Der Vote-Reminder ist neu in OttoExtra integriert und befindet
  sich mit seinen Einstellungen unter „Tweaks“. Beim Öffnen der Vote-Seiten wird
  der eigene Minecraft-Name direkt in die Zwischenablage kopiert.
- **Litematica:** Die neue optionale Integration zeigt in der Blockanzeige
  zusätzlich zum technischen Blockzustand den gelben, lokalisierten Inventarnamen
  aus dem aktiven Resourcepack.
- **Litematica – Fachwerk:** Fachwerk-Notenblöcke werden anhand von Instrument,
  Note und Redstone-Zustand erkannt. Litematica zeigt für alle 28 Fachwerkvarianten
  die passende Farbe und Bauform an, beispielsweise „Weißer Fachwerk Balken“
  oder „Graues Fachwerk Zierde“. Normale und unbelegte Notenblöcke bleiben
  unverändert.

### Tweaks – Geändert

- Die separaten Vote- und Litematica-Erweiterungen werden für diese Funktionen
  nicht mehr benötigt. OttoExtra vermeidet beim Übergang doppelte
  Vote-Erinnerungen.

### Weitere Änderungen

- Der Briefeditor verwendet bei neuen Briefen standardmäßig die seitenbasierte
  Briefversion `PAGE`.
- Das Aktivitäts-HUD kann über einen frei belegbaren Tastenschlüssel direkt
  ein- und ausgeblendet werden. Der gewählte Zustand bleibt gespeichert.
- Tactical Factions besitzt im Politik-Buch den neuen Tab „Spieler-Wache“.
  Minecraft-Namen können dort dauerhaft eingetragen und entfernt werden; bei
  einem späteren Onlinekommen erscheint eine Chatnachricht. Bereits beim eigenen
  Serverbeitritt anwesende Spieler lösen keine Meldung aus.

### Behoben

- Automatische Tierinfo-Abfragen werden vollständig unterbrochen, solange der
  Spieler auf einem Pferd, Esel oder Maultier reitet. Dadurch sendet OttoExtra in
  diesem Zustand keinen simulierten Shift-Rechtsklick auf andere Tiere und keine
  nachgelagerte Sneak-Flanke.
- Alte Vote-Einstellungen werden einmalig in die zentrale OttoExtra-Konfiguration
  übernommen, ohne die bisherige Konfigurationsdatei zu löschen.
- Litematica bleibt auch ohne installierte optionale Integration startfähig;
  unbekannte Blockzustände fallen zuverlässig auf den normalen Inventarnamen
  zurück.

## [0.1.16] – 2026-08-10

Diese Fassung bündelt alle Änderungen seit `0.1.15.3`, einschließlich des
zwischenzeitlichen `0.1.15.4`-Teststands.

### Hinzugefügt

- Automatische, kompakte Tierinfos beim Anvisieren von Zuchttieren mit Modell,
  Name, Geschlecht, Paarungsstatus, Mast, Restzeit sowie tierabhängigen Werten
  wie Scherbarkeit, Melkbarkeit, Sprunghöhe und Geschwindigkeit.
- Eigener Tierinfo-Editor mit getrennten Layouts für Schaf, Kuh, Schwein,
  Pferdearten, Packesel, Ziege, Huhn, Wolf und sonstige Tiere. Jede Tierart und
  jedes Anzeigeelement kann separat aktiviert oder deaktiviert, verschoben und
  skaliert werden.
- Minecraft-/Discord-Kontoverknüpfung über `/ottoextra verify` sowie eine
  Kontoübersicht über `/ottoextra account` mit RP-Profil, Stand, Rang, Gefolge
  und Hellerbestand.
- Verpflichtender Erststart-Assistent für optionale Netzwerkzugriffe. Er fragt
  Resourcepack-Downloads, externe Bilder, Versionsprüfung und das Teilen
  beobachteter RP-Namen getrennt ab; keine Erlaubnis ist vorausgewählt.
- Verschiebbares und skalierbares Aktivitäts-HUD mit Wappen, Gefolgenamen,
  Aktivitätsstufe, festen Positionen und frei platzierbarem Modus.
- Aktivitätspings auf Xaeros Weltkarte. Hohe Aktivität nutzt zwei zeitversetzte,
  langsamere Ringe; beim Herauszoomen zählt pro zusammengefasstem Lehen nur die
  höchste Aktivitätsstufe.
- Neuer optionaler Brief-Versandmodus `V2`, der Fließtext und notwendige
  Legacy-Zeilen kombiniert und die tatsächliche Server-Paginierung berücksichtigt.
- Interne Brief-Kalibrierung mit abgesicherten Testfällen für Seiten-, Pixel-,
  Leerraum-, Escape- und Formatierungsgrenzen.
- Titelfarben können aus dem API-Stand abgeleitet werden. Persönliche Farben,
  Stand, Titelkatalog, Titelgruppe und Fallback besitzen eine feste Priorität.
- Generische, rückwärtskompatible Addon-Hooks für synchrone und asynchrone
  Chatverarbeitung, dynamische Anzeigetexte, Nametag-Sichtbarkeit und
  -darstellung sowie beschriftete Aktionen im RP-Personenbuch.

### Geändert

- Automatisch abgefragte Tierinfo-Blöcke erscheinen weder im Chat noch im
  normalen Client-Log. Manuelle Tierinfo-Abfragen bleiben sichtbar.
- Tierinfos werden nach Füttern, Melken oder vergleichbaren Interaktionen neu
  geladen; Blockinteraktionen pausieren automatische Abfragen kurzzeitig.
- Nametags behandeln Titel, RP-Name und Accountname als getrennte Zeilen. Der
  Titel wird nicht mehr zusätzlich in die RP-Namenszeile geschrieben.
- Das RP-Personenbuch reserviert im ausgewählten Spielerprofil Platz für
  Addon-Felder und deren anklickbare Aktionen.
- Der Regionsdienst stellt Fraktionen, Hierarchien, Wappen, Spielerzuordnungen
  und Aktivitätsdaten über wiederverwendbare Schnittstellen für Addons bereit.
- Briefvorschau und Versand verwenden dieselbe V2-Planung; Leerzeilen,
  Formatierungen und serverseitige Seitenlimits werden konsistenter behandelt.
- Der Update-Dialog öffnet nur die Release-Seite im Browser und ersetzt keine
  Mod-Datei selbstständig.
- Ottonien-spezifische Funktionen akzeptieren ausschließlich die offizielle
  Domain und deren echte Subdomains; ähnlich aussehende Hosts werden abgelehnt.
- Addon-Fehler werden pro Hook isoliert, damit OttoExtra und andere Addons
  weiterarbeiten. Für Addons wird keine feste OttoExtra-Mindestversion erzwungen.

### Behoben

- Verschachtelte Emotes in OOC-Klammern können sichtbaren Nachrichtentext nicht
  mehr verdoppeln oder beschädigen; das beobachtete Servermuster wird repariert
  und unsichere Stilumbauten fallen aufs Original zurück.
- Bei verschachtelter RP-Syntax bestimmt der innerste Bereich den Stil: Emotes
  in OOC nutzen Emote-Farbe, OOC in Emotes nutzt OOC-Farbe.
- Automatische Tierinfo-Abfragen bereinigen den simulierten Sneak-Zustand sofort
  und lassen serverseitig keine kombinierte Sneak-/Sprint-Erkennung zurück.
- Pferde- und Esel-Tierinfos mit abweichender Serverausgabe werden vollständig
  erkannt und nicht mehr teilweise in Chat oder Log stehen gelassen.
- Alte Tierinfo-Konfigurationen werden schadensfrei ergänzt; deaktivierte
  Tierarten bleiben auch beim Zurücksetzen ihres Layouts deaktiviert.
- API-Importe aktualisieren Stand und andere öffentliche Metadaten auch bei
  gesperrten RP-Profilen, ohne geschützte Namen, Titel oder Farben zu überschreiben.
- Titelvarianten und entfernte Katalogtitel werden in Personenbuch, Vorschau
  und Anzeige konsistent ersetzt.
- Addon-erzwungene Nametags funktionieren auch bei deaktiviertem normalem
  OttoExtra-Nametag-Modul; mindestens der Minecraft-Accountname bleibt nutzbar.

### OttoExtra-TranslateUtils

#### Hinzugefügt

- Neuer Modus „Serveranzeigen übersetzen“ mit eigener Sprachrichtung,
  standardmäßig Deutsch → Englisch. Die ausgehende Chatübersetzung behält ihr
  separates Sprachpaar.
- Clientseitige Übersetzung für eingehenden Chat, Chat-Hovertexte, Actionbar,
  Titel und Untertitel, Bossbars, Scoreboard, Tab-Kopf und -Fuß,
  Inventartitel, gelesene Buchseiten, Custom-Itemnamen, Tooltip- und Lorezeilen
  sowie dynamische OttoExtra-Regionsanzeigen.
- Übersetzte Chatnachrichten zeigen den vollständigen Originaltext lokal im
  Hover, ohne Kanal-, Sprecher-, Farb- oder Klickinformationen zu ersetzen.
- Persistenter lokaler Cache unter
  `config/ottoextra-translateutils/translations.json` für wiederverwendbare
  NPC-, System-, HUD-, Regions- und Itemtexte. Spielerchat, Buchseiten,
  Chat-Hovertexte und unklare Chatzeilen werden nicht auf Platte gespeichert.
- Eigene Aktion zum Leeren des Übersetzungscaches sowie eine priorisierte,
  gebündelte Übersetzungswarteschlange. Chat hat Vorrang vor Item-Vorladungen;
  fehlerhafte Batches werden verkleinert wiederholt und HTTP 429 respektiert.
- Schutz für Minecraft-Accountnamen, bekannte RP-Namen, Gefolge, Fraktionen,
  Regionen und Lehen. Übersetzungen mit veränderten oder neu eingefügten
  geschützten Eigennamen werden verworfen.
- Vorladen sichtbarer Itemübersetzungen und lokale Übersetzungen für häufige
  dynamische Tooltipteile wie Haltbarkeit und Briefabsender.
- Verpflichtende, versionierte Datenschutzeinwilligung für Serveranzeigen,
  Custom-Itemnamen, Lore und die Regeln des persistenten Caches.

#### Geändert

- Ausgehende Übersetzung bleibt in Offtopic und Hilfe aktiv; der
  Sprachindikator zeigt dort keinen deaktivierten Zustand mehr an.
- Ein- und ausgehende Übersetzungen laufen asynchron. Bis zum Ergebnis bleibt
  das Original sichtbar, anschließend wird nur die betroffene Anzeige erneuert.
- Identische gleichzeitige Anfragen werden zusammengeführt und API-Aufrufe
  gedrosselt, statt dieselben Texte pro Renderdurchlauf erneut anzufragen.
- Minecraft-Textstile, Komponenten, Farbcodes und mehrzeilige Hover-Lore
  bleiben beim Austausch des sichtbaren Textes erhalten.
- Durch das Ottonien-Resourcepack ersetzte deutsche Itemtexte werden auf ihre
  deutsche Quelle zurückgeführt, damit dynamische Itemnamen zuverlässig
  übersetzt werden können.
- TranslateUtils verwendet die kontextreichen synchronen und asynchronen
  OttoExtra-Addon-Hooks, bleibt jedoch ohne fest deklarierte
  OttoExtra-Mindestversion.

#### Datenschutz und Fehlerbehebungen

- Nur wiederverwendbare Nicht-Spieler-Texte werden persistent gespeichert;
  private oder nicht sicher klassifizierbare Inhalte bleiben im Arbeitsspeicher.
- Cache-Dateien werden atomar ersetzt. Beschädigte Dateien werden nicht
  überschrieben und Übersetzungsfehler fallen auf das Original zurück.
- Scoreboard-Einträge, die echten Online-Spielernamen entsprechen, werden nicht
  übersetzt.
- Veraltete Cache-Einträge mit veränderten Eigennamen werden nicht angezeigt
  und automatisch entfernt.
- Batchantworten mit veränderten Trennern werden nur bei eindeutiger Zuordnung
  übernommen; unvollständige Antworten landen nicht beim falschen Text.

## [0.1.15.4] – 2026-07-30

### Neu

- Beim kurzen Anvisieren eines Zuchttiers erscheint oben eine schlanke Tierinfo
  mit kleinem 3D-Tiermodell, Name und Geschlecht, farblicher Paarungsbereitschaft,
  Mast sowie der verbleibenden Zuchtzeit. Bei Schafen erscheint zusätzlich
  „Scherbar“, bei Kühen „Melkbar“ jeweils grün oder rot.
- Der Tierinfo-Layouteditor speichert ein eigenes Layout je Tierart. Modell, Name,
  Geschlechtssymbol, Geschlechtstext, Paarungsstatus, Mast, Zeit und Zusatzstatus
  lassen sich einzeln verschieben, skalieren oder ausblenden. Der dunkle
  Toast-Hintergrund passt seine Größe samt festem Innenabstand automatisch an.
- Die Abfrage benötigt weder Shift noch Rechtsklick und wird pro Tier gedrosselt
  sowie kurz zwischengespeichert.
- Spieler können ihren aktuellen Minecraft-Account direkt in der OttoExtra-Haupt-JAR
  mit `/ottoextra verify` mit ihrem OttoExtra-/Discord-Konto verbinden.
- `/ottoextra account` öffnet eine übersichtliche Kontoansicht mit Verbindungsstatus,
  Discord-Name, RP-Profil, Stand, Rang, Gefolge und aktuellem Hellerbestand.
- Der sichere Browser-Flow läuft vollständig über `ottoextra.dev`; nach der
  Discord-Anmeldung wird die Verknüpfung im OttoExtra-Konto angezeigt.

### Sicherheit

- Discord-Zugriffstokens werden nicht im Client gespeichert. Lokal bleibt nur
  ein widerrufbarer Device-Token in `config/ottoextra-discord.json`.
- Minecraft-UUID und Accountname stammen in dieser ersten Spielerversion aus
  der angemeldeten Minecraft-Clientsession.

### Geändert

- Block-Rechtsklicks sperren automatische Tierinfo-Abfragen kurzzeitig. Dadurch
  kollidiert die simulierte Tierinteraktion nicht mehr mit Türen oder anderen
  interaktiven Blöcken.
- Nach Füttern, Melken oder anderen Rechtsklick-Interaktionen wird die angezeigte
  Tierinfo automatisch neu abgefragt. Sprunghöhe und Geschwindigkeit sind im
  Pferde-Toast nun getrennt verschiebbar, skalierbar und ausblendbar.
- Von OttoExtra automatisch angeforderte Tierinfo-Blöcke werden vor dem Chat-HUD
  abgefangen. Dadurch erscheinen die neun Systemzeilen weder im Chat noch als
  wiederholte `[CHAT]`-Einträge im Client-Log. Manuelle Tierinfo-Abfragen bleiben
  unverändert sichtbar.
- Das abweichende Pferde-/Esel-Format mit „Pferdeinfo“ und „Zuchtbereit“ wird
  ebenfalls als Toast verarbeitet und vollständig aus Chat und Log entfernt.

## [0.1.15.3] – 2026-07-22

### Hinzugefügt

- Sitzungsbezogene Debug-Steuerung über `/ottoextra debug on`,
  `/ottoextra debug off` und `/ottoextra debug status`. Debug ist nach jedem
  Clientstart standardmäßig deaktiviert.
- NPC-Namensfarbe und NPC-Nachrichtenfarbe lassen sich getrennt in der
  Chatfarben-Verwaltung konfigurieren. Standardwerte sind `#C7A87F` für Namen
  und `#DFC8A7` für Nachrichten.
- Betreten-Einblendungen können beim Lehnsherrn den lokal vorhandenen RP-Namen
  anzeigen. Die Option ist standardmäßig aktiv; ohne RP-Namen bleibt der
  Accountname sichtbar.

### Geändert

- Nachrichtenfarben aller Chatkanäle sind als konkrete Hexwerte vorausgefüllt:
  Sprechen `#DFC8A7`, Flüstern `#768491`, Murmeln `#58666F`, Rufen `#D2BF6A`,
  Brüllen `#FCF47E`, Offtopic `#B4BEC6` und Hilfe `#B53764`.
- Bestehende Konfigurationen mit leeren oder ungültigen Nachrichtenfarben werden
  automatisch auf den jeweiligen Kanalstandard migriert. Eigene gültige Farben
  bleiben erhalten.
- Beim Öffnen der echten Xaero-Weltkarte wird sofort ein inkrementeller
  Ottonien-Regionssync angestoßen. Nur dieser Karten-Trigger besitzt einen
  Cooldown von einer Minute; Join- und periodische Synchronisierung bleiben
  unverändert.
- Änderungen an Chatkanal-, NPC-, Emote- und OOC-Farben werden auch auf bereits
  sichtbare Chatzeilen angewendet.
- Normale Logs wurden entschlackt. Detaillierte Diagnoseausgaben erscheinen nur
  bei aktivierter Debug-Sitzung in Fabrics `logs/debug.log`; wiederholte Render-,
  Chat-, Download- und API-Fehler werden gedrosselt.
- Chat-, Account-, UUID-, RP-Namens- und API-Nutzdaten werden nicht mehr in
  OttoExtras normalen Diagnosemeldungen ausgegeben.

### Behoben

- Shift-Rechtsklick auf Spieler öffnet bei einer aktiven Bossbar mit `IM KAMPF`
  weder Personenbuch noch Kennenlernfenster. Prüfung ignoriert Groß-/Kleinschreibung
  und wird direkt vor verzögerten Screen-Öffnungen wiederholt.
- Kann der aktuelle Spielerstand beim Kennenlernen nicht über die API geladen
  werden, öffnet das Kennenlernfenster mit lokal gecachten oder im Chat gesehenen
  RP-Daten statt mit einer Fehlermeldung. Vorhandene lokale Namen vermeiden die
  unnötige Einzelabfrage vollständig.
- NPCs in Sprechen, Reden und weiteren RP-Sprechkanälen bleiben nicht länger in
  grauer Serverfarbe. Erkennung funktioniert generisch ohne hardcodierte
  NPC-Namensliste.
- Strukturierte Minecraft-Textkomponenten und alte `§`-Farbcodes werden bei der
  NPC-Einfärbung korrekt verarbeitet, ohne sichtbare Steuerzeichen oder
  verschobene Textbereiche.
- Hover-, Klick-, Einfüge-, Schrift-/Sprite- und Formatierungsinformationen der
  ursprünglichen Chatzeile bleiben beim Umfärben erhalten.
- NPCs durchlaufen weder Spieler-Kennenlernen noch RP-Namensspeicherung oder
  API-Upload.
- NPC-Nachrichtenfarbe überschreibt die jeweilige Kanal-Nachrichtenfarbe;
  `*Emotes*` und `(OOC)` behalten weiterhin ihre eigenen, höher priorisierten
  Farben.
- Lehnsherr-RP-Namen aus dem lokalen API-Abgleich werden im Betreten-Toast nicht
  mehr durch den Kennenlernstatus ausgeblendet. Chat, Tabliste und Nametags
  behalten ihre bestehenden Sichtbarkeitsregeln.
- Politische `lord_name`-Daten werden nicht mehr fälschlich als Spielerfallback
  für den Lehnsherrn verwendet.

## [0.1.15] – 2026-07-19

### Hinzugefügt

- Neuer RP-Untersuchungsmodus über die frei belegbare, standardmäßig linke
  Alt-Taste für Spieler, Gegenstände, Kreaturen und ausgewählte Blöcke.
- Animierte Lupe mit sanftem Zoom, Randunschärfe, Fortschrittsring und
  wechselnden RP-Gedanken.
- Spieleruntersuchung mit RP-Name, Titel, sichtbaren Handgegenständen und
  sichtbaren Rüstungsteilen; versteckte Inventarinhalte werden nicht gelesen.
- Gegenstandsinformationen wie Custom-Name, sichtbare Lore, Stapelgröße,
  Zustandsbeschreibung sowie Autor und Seitenzahl bei Büchern.
- Sichtbare Informationen zu Schildern, Türen und Toren, Lichtquellen, Kerzen,
  Bienenstöcken, Pflanzen, Lesepulten, Bannern, Rahmen und Rüstungsständern.
- Automatische RP-Chatformatierung für `*Emotes*` und `(OOC-Kommentare)`.
- Für Sprechen, Flüstern, Murmeln, Rufen, Brüllen, Offtopic und Hilfe lassen
  sich Kanalname und Nachrichtentext jeweils getrennt lokal einfärben.
- Farbe und Kursivdarstellung lassen sich für `*Emotes*` und `(OOC)` jeweils
  unabhängig konfigurieren.
- Die kompakte Chatfarben-Verwaltung zeigt für jedes Hexfeld direkt daneben
  eine Live-Farbvorschau und bietet Einzelreset sowie „Alle auf Original“.
- Authentifizierte API-Anbindung für geschützte Chat-Originaltexte mit
  Besitzer- und Empfängerprüfung.
- Addon-Hook zur verlustfreien Erweiterung eingehender Chatnachrichten, etwa
  um freigegebene Originaltexte als Hover anzuzeigen.
- API-Vertrag für die benötigten PHP-Endpunkte inklusive UUID-Allowlist, TTL,
  Rate-Limits und sicherer Speicherung.
- Autorisierte Chat-Inbox zur markerlosen Zuordnung öffentlicher Übersetzungen
  zu geschützten Originaltexten.

### Geändert

- Untersuchungsdauer, RP-Gedanken, Zoom, Randunschärfe, Accountname,
  Handgegenstände und Rüstung sind getrennt konfigurierbar.
- RP-Chatformatierung betrifft ausschließlich den Nachrichtentext in den
  RP-Kanälen; Kanal, Titel, Name, Kopf sowie Klick- und Hoverdaten bleiben erhalten.
- Lange Nachrichten werden automatisch geteilt. Offene Emote- und OOC-Bereiche
  werden pro RP-Teilnachricht geschlossen und in der nächsten wieder geöffnet.
- Offtopic und Hilfe verändern beim Aufteilen weder Sternchen noch Klammern.
- OttoExtra gibt RP-Chatfarben und -formatierung jetzt zentral vor; Addons wie
  TranslateUtils verwenden dieselben Regeln auch für Hover-Lore und lokale Originalzeilen.
- Die Kanalanzeige neben der Chat-Eingabe übernimmt die konfigurierte Farbe
  des jeweiligen Chatnamens.
- Die Chatkanal-Farbverwaltung verwendet nun das dunkle, halbtransparente
  Design des RP-Personenbuchs.
- Emote- und OOC-Farbe sowie die beiden Kursiv-Schalter befinden sich gemeinsam
  mit den Kanaloptionen in der kompakten Farbverwaltung.
- „Titel in der Tabliste“ gilt jetzt einheitlich für bekannte und unbekannte
  Spieler; der separate Unbekannten-Schalter entfällt.
- Verschachtelte OOC- und Emote-Bereiche bleiben auch bei langen Nachrichten
  und Kombinationen wie `(*...*)` vollständig ausgeglichen.
- Die OOC-Standardfarbe der Chatformatierung entspricht `#B4BEC6`.
- OttoExtra stellt geladenen Addons den bestehenden Mojang-verifizierten
  API-Client zur Verfügung.
- Bereits sichtbare Chatzeilen berücksichtigen beim Neuaufbau auch
  nachträgliche Addon-Verarbeitung.
- „Automatisch optimieren“ für Verkündungen ist standardmäßig deaktiviert und
  kann unter „Briefe → Erweitert → Layout & Timing“ bewusst aktiviert werden.
  Der Einstellungshinweis warnt vor möglichen Umbruch-, Formatierungs- und
  Seitenlayoutfehlern.

### Behoben

- Die im Untersuchungsmodus referenzierte Lupentextur ist wieder vollständig
  in der OttoExtra-JAR enthalten und wird durch Asset-Tests abgesichert.
- Der Button zum Öffnen der Chatkanal-Farbverwaltung besitzt wieder die
  sichtbare Beschriftung „Öffnen…“.
- Das Farbmenü löst keinen zweiten Screen-Blur mehr aus und crasht nicht mehr
  mit „Can only blur once per frame“.
- OttoExtra überschreibt die normale Chatfarbe nicht mehr automatisch.
- Jeder RP-Kanal behält wieder seine vom Server vorgegebene Farbe.
- Nach einem `*Emote*` oder `(OOC-Kommentar)` kehrt der Text zuverlässig zum
  ursprünglichen Stil des jeweiligen Chatkanals zurück.
- Die automatische RP-Formatierung bleibt auf Murmeln, Flüstern, Sprechen,
  Rufen und Brüllen beschränkt; Offtopic und Hilfe bleiben unverändert.
- Geschützte Übersetzungen benötigen keine sichtbaren `[Ü:…]`-Marker oder
  unsichtbaren Unicode-Zeichen mehr im öffentlichen Chat.

## [0.1.13-2a] – 2026-07-15

### Hinzugefügt

- Beim Öffnen der Xaero-Weltkarte wird sofort ein inkrementeller Regions-Sync ausgelöst.
- Bereits vorhandene Chatzeilen werden nach Änderungen im RP-Namensbuch neu aufgebaut.
- Titelanzeige und automatische Titelaktualisierung der Tabliste sind getrennt schaltbar.
- Gemeinsame Chat-Eingabefläche für Addons; TranslateUtils kann seinen Sprachschalter anzeigen, ohne den eingegebenen Text zu überdecken.

### Geändert

- Die Regions-API wurde von `regions.skyfuller.de` auf `api.ottoextra.dev` umgestellt.
- Bereits gespeicherte Konfigurationen mit der alten Regions-Domain werden automatisch migriert.
- Parallele Regions-Syncs während eines laufenden Bootstraps oder Syncs werden vermieden.
- RP-Name und Titel werden im Ottonien-Rich-Text-Chat ersetzt, ohne Wappen, Spielerkopf, Hover-Informationen oder Nachrichtenfarben zu verlieren.
- Die eindeutige Accountzuordnung der Spieler-Kopfkomponente hat Vorrang vor gleichnamigen RP-Profilen.
- Persönliche Farben aus dem RP-Namensbuch haben in Chat, Tabliste und Nametag Vorrang vor Katalog- und Standardfarben.
- Bei aktiver Titelanzeige behalten unbekannte Spieler ihren originalen Servertitel in der Tabliste; bekannte Spieler erhalten den lokal angepassten Titel.
- OttoExtra ist mit TranslateUtils `0.1.1` kompatibel.

### Behoben

- Manuelle Änderungen im RP-Namensbuch werden beim Personenwechsel, Tabwechsel und Schließen automatisch übernommen und zuverlässig auf Platte gespeichert.
- Angepasste RP-Namen, Titel und Farben erschienen teilweise nur in Nametags oder der Tabliste, nicht aber im Chat.
- Gleichnamige Profile konnten fälschlich als `Unbekannt` angezeigt werden und den Titel verlieren.
- Beim Chat-Umschreiben entstanden sichtbare Ressourcenpfade, falsche Farben oder große Leerbereiche.
- Die Einstellung „Titel in Tabliste“ wurde zuvor mit der automatischen Titelaktualisierung vermischt.
- Der Platzhalter `Unbekannt` wird nur auf echte Spieler angewendet, nicht auf NPCs, Tiere oder andere Entitäten.

## [0.1.12] – 2026-07-12

### Hinzugefügt

#### Briefeditor
- Text kann mit der Maus markiert und nur innerhalb der Auswahl formatiert werden.
- Farben sowie Fett, Kursiv, Unterstrichen und Durchgestrichen können auf einzelne Textbereiche angewendet werden.
- `Strg + Z` macht Änderungen rückgängig.
- `Strg + Y` stellt Änderungen wieder her.
- Neuer `Tx`-Button entfernt sämtliche Formatierungen ausschließlich aus dem markierten Bereich.
- Neue Briefvorschau zeigt alle Seiten vor dem Versenden mit Farben, Formatierungen und Zeilenumbrüchen an.

#### Karte
- Neuer Pergamentmodus mit Rahmen, Kompassrose und Maßstabsleiste.
- Lehen können auf Xaeros Weltkarte angeklickt werden.
- Das Informationsfenster zeigt Herrschaft, Lehnsherr, Stand, Gefolge und Koordinaten.
- Entfernung, Himmelsrichtung sowie geschätzte Geh- und Reitzeit werden angezeigt.

#### Regionen
- Beim Betreten von Lehen werden zusätzliche Informationen angezeigt.

### Geändert

#### Chat und Tabliste
- Bei unbekannten Personen werden ausschließlich Titel und Name durch `Unbekannt` ersetzt.
- Lehenswappen werden in der Tabliste wieder als echte Wappen angezeigt und nicht als sichtbare Ressourcenpfade.

#### Briefe
- Nach dem erfolgreichen Versenden eines Briefes wird der Editor vollständig geleert.
- Ein neuer Brief enthält keinen ausgegrauten Text des zuvor versendeten Briefes.
- Nach einer Bereichsformatierung wird die zuvor aktive Farbe und Formatierung hinter der Auswahl automatisch wiederhergestellt.
- Ein Klick auf einen Button im Briefeditor führt nicht mehr dazu, dass die Leertaste die letzte Button-Aktion erneut ausführt.

## [0.1.11] – 2026-07-03

### Hinzugefügt
- **Addon-Schnittstelle**: Andere Mods können sich per Fabric-Entrypoint
`"ottoextra"` (Interface `de.ottoextra.addon.OttoExtraAddon`) als eigenes
Modul in die OttoExtra-Einstellungen einhängen. Fehler einzelner Addons
werden geloggt und reißen weder OttoExtra noch andere Addons mit. Erstes
Addon: **OttoExtra-TranslateUtils** (eigenes Repository, eigene JAR).
- **Verbandserkennung übers Wappen** (politische Karte): Fraktionslose Lehen
  mit dem Wappen einer anderen Region (Wappen-Dateiname ↦ gleichnamige
  Region, z. B. `maehrstein.png` ↦ Mährstein) zählen als deren Verband —
  Fallback, wenn die API weder Fraktion noch Region-Hierarchie liefert.
  Mitglieder bekommen Verbandsfarbe und Vasallen-Streifen, nur das
  namensgebende Lehen ist Wurzel; Labels zeigen einzeilig den Lehensnamen.
  Region-Wappen fallen zusätzlich auf `suggested_banner_path` zurück.

### Behoben
- **Doppelte Verbands-Labels auf der politischen Karte** (2× „Mährstein",
  2× „Kreuztal"): Verbände konnten in zwei Gruppen mit zwei Sammel-Labels
  zerfallen — der Region-Detail-Endpoint überschreibt Records zur Laufzeit
  ohne Hierarchie-Felder, und Gefolge-Wurzeln führen selbst keine
  `vassal_uuids` (Gefolge hängt nur am `lord_name` der Kinder). Alle
  Erkennungswege (Lehnsherr-Kette, Region-Hierarchie, Verbandswappen)
  münden jetzt in derselben Gruppe.
- **Wurzel-Lehen beim Rauszoomen doppelt beschriftet**: Trägt ein Lehen exakt
  den Verbandsnamen (z. B. Mährstein selbst), wird sein Einzel-Label
  ausgeblendet, sobald das Sammel-Label einblendet.

## [0.1.10] – 2026-07-02

### Hinzugefügt
- **Vollflächige Karte beim Rauszoomen** (optional, Standard aus): Blendet auf
  der Worldmap Xaeros Terrain aus und zeigt die gemalte Karte überall zu
  100 % — sobald das politische Overlay nur noch die Wappen der obersten
  Lehnsherren zeigt (gleiche Zoom-Stufe wie die Gruppen-Labels). Nur bei
  aktiver politischer Karte. Einstellungen → Karte. Die HUD-Schutzbänder des
  Karten-Shaders (Xaero-Koordinaten/Zoom-Anzeige) bleiben im normalen Modus
  erhalten und reißen die Vollabdeckung nicht mehr auf.

### Geändert
- **Kanal-Zyklus neu sortiert**: Durchschalten (Kanal-Button und Shift+Tab)
  läuft jetzt Murmeln → Flüstern → Sprechen → Rufen → Brüllen → Offtopic →
  Hilfe (Button nur RP-Kanäle, Shift+Tab alle). Neu: **Alt+Linksklick** auf
  den Kanalnamen schaltet einen Kanal zurück (z. B. Offtopic → Brüllen).

### Behoben
- **Wappen/Name/Stand-HUD verschwand bei F3+B (Hitboxen)**: Seit MC 1.21.11
  meldet `shouldShowDebugHud()` auch aktive Einzel-Debug-Einträge (z. B. den
  Hitbox-Toggle) — das HUD hielt das für den F3-Screen und versteckte sich,
  obwohl die Minimap sichtbar blieb. Jetzt zählt wie bei Xaero selbst nur der
  echte F3-Screen (`debugHudEntryList.isF3Enabled()`).

## [0.1.9] – 2026-06-30

### Hinzugefügt
- **Tweak „Werkzeugschutz"** (Standard an, abschaltbar): Bricht Links- und
  Rechtsklick-Interaktionen clientseitig ab, wenn dem gehaltenen Werkzeug nur
  noch wenige Nutzungen bleiben (einstellbar, Default 10) — statt dessen
  erscheint eine Actionbar-Meldung, dass es kurz vor dem Kaputtgehen ist.
  Zusätzlich einmalige Actionbar-Warnung, wenn die Haltbarkeit unter einen
  Prozentsatz fällt (einstellbar, Default 10 %) — auch beim Auswählen eines
  bereits fast kaputten Items; erneut nach Item-Wechsel oder Reparatur.
  Gilt für beide Hände. Einstellungen → Tweaks → Werkzeugschutz.
- **Neue Chat-Kanäle**: **Murmeln** (`/m`, Farbe `#58666f`) und **Brüllen**
  (`/b`, Farbe `#fcf47e`) ergänzen die RP-Sprechkanäle. Kanal-Button-Zyklus
  (links) läuft jetzt lautstärke-aufsteigend Flüstern → Murmeln → Sprechen →
  Rufen → Brüllen. Eingehende `[Murmeln]`/`[Brüllen]`-Zeilen bekommen RP-Namen
  und werden fürs proaktive Kennenlernen erkannt.

### Geändert
- **Shift+Tab** im Chat schaltet den Kanal durch **alle** Kanäle
  (Sprechen → Flüstern → Murmeln → Rufen → Brüllen → Offtopic → Hilfe).

### Entfernt
- **Spielerköpfe im Chat**: Das Modul rückte die Chatzeile ein und brach damit
  die serverseitigen Kopf-Token (z. B. `[flizko head]` bei Teamlern) — Köpfe
  erschienen als roher Text. Feature inkl. Einstellung entfernt; die
  Server-Köpfe rendern wieder selbst. Skin-Auflösung für GUIs (Empfängerliste)
  bleibt erhalten.

### Behoben
- **Politisches Overlay: Lehen fälschlich als unabhängig gruppiert**: Führt die
  API denselben Fraktionsnamen doppelt (z. B. ein stale „Ungelandet"-Eintrag
  neben dem echten Lehen, wie bei Schaffenberg), gewann beim Namens-Lookup der
  erste Treffer — die Lehnsherr-Kette riss ab und das Lehen samt Vasallen
  (Rosenhain) erschien als eigene, unabhängige Gruppe statt beim obersten
  Lehnsherrn. Bei Duplikaten gewinnt jetzt der aussagekräftigere Datensatz
  (gelandet vor ungelandet, dann mit Lehnsherr vor ohne) — in Worldmap-Gruppen,
  Minimap-Lehnsherr-HUD und Gefolge-Liste.
- **Lange Nachrichten: RP-Spans überleben den Split**: Wurde eine lange
  Nachricht mitten in einem `*…*`- (Darstellung) oder `(…)`-Span (Offtopic)
  geteilt, verlor der Server die Einfärbung — er färbt nur, wenn Stern/Klammer
  in derselben Nachricht geschlossen wird. Offene Spans werden jetzt am Ende
  des Teilstücks (vor dem Fortsetzungs-Marker) geschlossen und im nächsten
  Teilstück direkt wieder geöffnet; auch verschachtelt (`(bla *foo` →
  `…*)` / `(*…`) und über beliebig viele Teilstücke.
- **Wortweise Cursor-Navigation und Markierung im Brief-Editor**: Strg+Pfeil
  (Wortsprung) und Strg+Shift+Pfeil (wortweise markieren) waren auf keiner
  Plattform implementiert — die Pfeiltasten-Handler ignorierten die Modifier.
  Jetzt plattform-korrekt: **Strg+Pfeil** (Windows/Linux) bzw. **Option+Pfeil**
  (macOS) springt wortweise, **Cmd+Pfeil** (macOS) springt an Zeilenanfang/-ende;
  jeweils mit Shift wird markiert. Hinweis macOS: Ctrl+Pfeil fängt oft das
  System ab (Mission Control) — dort Option+Pfeil nutzen.
- **macOS: Einfügen im Brief-Editor ging nicht**: Der Editor prüfte nur Strg;
  auf macOS liegt Kopieren/Einfügen/Ausschneiden/Alles-markieren auf **Cmd**.
  Nutzt jetzt die plattform-korrekte Prüfung (`hasCtrlOrCmd`); ebenso
  Cmd/Strg+S im Einstellungs-Screen.
- **Teamler-/Server-Ränge verloren ihre Farbe im Chat**: Server-Ränge wie
  „Gamemaster" wurden als Titel behandelt, der farbige Server-Titel geblankt und
  durch einen (weißen) Ersatz überschrieben. Es werden jetzt nur noch **eigene
  Katalog-/Gruppen-Titel** ersetzt; unbekannte Server-Ränge bleiben mit ihrer
  Server-Farbe stehen (Chat).

## [0.1.8] – Unveröffentlicht

### Geändert
- **RP-Titel-Editor**: Der Standard-Titel (oberes Feld) ist nur noch in der
  Kategorie „custom" frei editierbar. Bei Katalog-/Wiki-Titeln bleibt er fest,
  nur die Anzeige-Varianten (Variante 1/2) dürfen angepasst werden.

### Behoben
- **RP-Titel-Varianten brachen den Treffer**: Eine Anzeige-Variante umzubenennen
  (z. B. „Dame" → „Edeldame") hat dazu geführt, dass die vom Server gesendete
  Form nicht mehr zum Katalog-Eintrag passte — die Träger verloren Farbe und
  Anzeige (fielen auf die allgemeine Default-Farbe zurück). Match-Aliase (die
  echten Server-Formen) werden jetzt getrennt von den Anzeige-Varianten geführt:
  die Server-Form matcht weiterhin und zeigt die zugehörige (umbenannte)
  Variante. Bestehende Einträge heilen sich beim Laden automatisch aus dem
  Wiki-Default.

### Hinzugefügt
- **Neues Modul „Tweaks"** mit eigenem Einstellungstab — optionale,
  clientseitige Effekte und Spiel-Tweaks.
- **Low-Health-Adrenalin-Effekt** (unter 10 Herzen, rein clientseitig, opt-in):
  - **Rote Blut-Vignette** am Bildschirmrand, prozedural gezeichnet und leicht
    pulsierend; stärker je niedriger die Health, bei offenem Screen reduziert.
  - **Warden-Herzschlag** lokal als wiederholter Einzel-Sound — Tempo,
    Lautstärke und Tonhöhe skalieren mit der Intensität (kein Loop, kein Spam).
  - **Edge-Blur / Tunnelblick** über einen eigenen Posteffekt: die Bildmitte
    bleibt scharf, die Ränder verschwimmen zunehmend. Das HUD bleibt scharf
    (Blur läuft nach dem Welt-Render, vor der GUI). Unabhängig von der
    Menü-Hintergrund-Unschärfe; funktioniert auch mit Iris/Shaderpacks (best
    effort, sonst bleiben Vignette + Herzschlag aktiv).
  - **FOV-Adrenalin**: leichter, additiver FOV-Anstieg bei niedriger Health.
  - **Beruhigung**: Herzschlag und starker Blur beruhigen sich nach einigen
    Sekunden ohne neuen Schaden; die rote Vignette bleibt health-basiert.
  - Einstellbar pro Effekt: an/aus, Start-Health bzw. Herz-Schwelle (Blur),
    Intensität, Blur-Stärke, FOV-Zuschlag.
  - **Test-Commands** (Single-/Multiplayer): `/ottoextra tweaks lowhealth
    test [0..1]` erzwingt eine Intensität, `… stop` hebt das auf, `… on`/`… off`
    schalten den automatischen Effekt.

## [0.1.7] – 2026-06-16

### Hinzugefügt
- **Chat-Aktionsprompt nach dem Schreiben**: „Schreiben" schreibt den Brief
  sofort per `/letter` (das beschriebene Buch entsteht) und schließt den Editor;
  danach erscheint im Chat eine klickbare Nachricht
  `[Verschicken] [Verkünden] [Schließen]`. Die Empfängerliste öffnet **nur** noch
  auf **[Verschicken]** (sendet dann `/post <Empfänger>`), **[Verkünden]** führt
  über die Preflight-Bestätigung (`/verkünden`); **[Schließen]** lässt den Brief
  ungesendet. Kein erzwungener Moduswechsel/keine erzwungene Playerliste mehr.
- **Beschriebene Briefe bearbeiten**: Eigene beschriebene Briefe (erkannt am
  Item-Namen „… von <Spielername>", Account über die Spieler-UUID) bekommen in
  der Buch-Lese-GUI einen **[Bearbeiten]**-Button (mittig unter „Fertig"). Der
  Inhalt wird in den Editor übernommen und zu vollen 12-Zeilen-Seiten
  umgebrochen; der alte Text ist **gesperrt** und wird **entsättigt mit seiner
  Original-Formatierung** angezeigt (man sieht, ob/wie vorher Farben/Stile
  gesetzt waren). Nach einer Leerzeile schreibt man **inline** weiter — nur der
  neue Teil wird per `/letter` angehängt.
- **Spielerköpfe im Chat** (optional, Standard aus): zeigt links zwischen Kanal
  und Titel den Spielerkopf des Sprechers. Einstellbar unter Chat → Kanal
  („Spielerköpfe im Chat"). Der Sprecher wird über die Namenszone (Account oder
  RP-Name) aufgelöst, der Skin aus der Tabliste bzw. dem Default-Skin gezogen;
  die Auflösung ist je Name gecacht. Zeichnen über zwei ChatHud-Backend-Mixins
  (offener und geschlossener Chat), mit Zeilen-Deckkraft (Fade). Der Kopf wird
  nur in die tatsächliche Leerzeichen-Lücke hinter dem Kanal gezeichnet
  (zentriert) — ist sie zu klein, erscheint kein Kopf statt einer Überlappung
  mit dem Titel. Der Sprecher wird **per UUID** bestimmt (Account aus dem
  Chat-Hover → UUID), der RP-Name dient nur als Fallback — so erscheint auch bei
  gleichlautenden RP-Namen der richtige Kopf.
- **Persistenter Skin-Cache**: Skins der online gesehenen Spieler werden lokal
  je UUID gespeichert (`config/ottoextra/cache/skins.json` für die signierte
  Textur-Property, `cache/skins/<uuid>.png` als Bild) und beim Start geladen.
  Befüllt sich beim Server-Join über die Tabliste. Offline werden die GUIs
  (Chat-Kopf, Empfängerliste, RP-Buch-3D-Vorschau) direkt aus dem **lokalen PNG**
  bedient (als Runtime-Textur), statt Mojang/Default — der echte Skin bleibt
  also auch ohne Online-Status erhalten.

### Geändert
- **Brief-Versand: Schreiben und Abschluss getrennt.** Der frühere Auto-Dialog
  (Brief/Verkündung) wird durch den Chat-Aktionsprompt ersetzt (bleibt als
  Fallback im Code). „Senden…" heißt jetzt **„Schreiben"**. Verschicken sendet
  nur noch `/post`, Verkünden nur den Submit-Befehl (`/verkünden`) — kein
  doppeltes Schreiben mehr.
- **Brief-Import** füllt die Editor-Seiten (12 Zeilen) statt die Buchseiten 1:1
  zu übernehmen; Trailing-Spaces der Serverzeilen werden entfernt (keine
  Phantom-Leerzeilen mehr), zwischen altem und neuem Text steht eine Leerzeile.
- **Server-Briefhinweise im Chat ausgeblendet** („Beschreibe den Brief mit
  /letter …", „Du hast den Brief bearbeitet") — der Chat-Prompt und die
  Bearbeiten-GUI ersetzen sie.
- **Modul-Tabs der Einstellungen** werden auf schmalen Fenstern/kleinen Monitoren
  zu einem **Dropdown-Filter** (aktuelles Modul + Liste) statt einer überlaufenden
  Tab-Reihe; bei genug Breite bleibt die normale Reihe.

## [0.1.6] – 2026-06-15

### Hinzugefügt
- **„Titel fest"** (RP-Personenbuch, neben „Gesperrt"): sperrt **nur den Titel**
  einer Person gegen automatische Änderung (Server-Hover/Chat, 30s-Tablist-Sync,
  Katalog-Umbenennung) — der Spieler bleibt aktiv (RP-Name etc. werden weiter
  gelernt). Wird beim manuellen Bearbeiten des Titels automatisch gesetzt.

### Geändert
- **Anzeige-Titel über Varianten einstellbar**: Bei einem Titel ist das Feld
  **Titel** der feste Standardwert (Server-/Wiki-Name); **Variante 1/2**
  bestimmen die **angezeigte** Form. Ein Spieler, dessen Server-Titel auf einen
  Katalog-Eintrag matcht, wird überall (Tab, Namensschild, Chat, Kennenlern-GUI)
  mit der Variante angezeigt — passt eine Variante genau zum Server-Titel (z. B.
  Geschlechtsform Rüstmann/Rüstfrau), bleibt diese erhalten, sonst greift die
  erste Variante. Die Auflösung passiert **live beim Rendern**: eine Änderung an
  Variante 1/2 wirkt sofort bei allen Trägern, ohne Neustart und ohne dass der
  30s-Tablist-Sync sie zurücksetzt (Server-Titel werden roh gespeichert).
- **Reset-Icon im Titel-Editor** setzt Titel/Varianten jetzt auf die
  **Werkseinstellung** (gebündelter Wiki-Default) statt das Feld zu leeren.
- **Titel-Feld im Editor** ist auch für Standard-(Wiki-)Titel editierbar (nur
  das Löschen bleibt gesperrt).
- **Gefolge-Editor** (Karte → Gefolge…) übernimmt Name + Farben nicht mehr
  live, sondern erst über **Speichern**; **Verwerfen** lädt den gespeicherten
  Stand neu.
- **OttoExtra-Button** im Pause-Menü (unten links) ist jetzt 30×30 (vorher 20×20).
- **Automatischer API-Abgleich beim Server-Join** (RP-Namen): cached den
  API-RP-Namen je Spieler und ergänzt leere Felder — legt keine neuen Spieler an
  und schreibt keine Backup-Datei. Läuft asynchron im Hintergrund.
- **Titel „Laienbruder/Laienschwester"** ist jetzt Kategorie **allgemein**
  (vorher fälschlich Klerus). Bestehende Kataloge werden beim Laden automatisch
  korrigiert (nur wenn noch der alte Default-Wert gesetzt ist).

### Behoben
- **Gemalte Karte verschwand beim Shader-Wechsel**: Ein Iris-Shader-Wechsel baut
  die GPU-Pipeline neu auf (ohne Resource-Reload), wodurch die gecachte
  Composite-Pipeline veraltete und die Karte bis zum Client-Neustart wegblieb.
  Der frühere dauerhafte Kill-Switch ist durch einen transienten Fehlerzustand
  mit Selbstheilung ersetzt (GPU-Ressourcen verwerfen + Neuaufbau nach kurzer
  Pause). Worldmap und Minimap heilen unabhängig; die Xaero-Bridge löst ihre
  Reflection-Handles bei Klassenwechsel (Reconnect/Reload) neu auf.
- **Manuell gesetzter Person-Titel wurde zurückgesetzt**: Der 30s-Tablist-Sync
  und eingehende Chat-Hover überschrieben von Hand gesetzte Titel wieder mit dem
  Server-Titel. Jetzt schützen sowohl der MANUAL-Status als auch „Titel fest"
  alle automatischen Titel-Schreibpfade.
- **Overlay-Toggle (K)** kippt nur noch auf dem Xaero-Worldmap-Screen; ein
  versehentlicher K-Druck im Spiel blendet das Lehen-Overlay nicht mehr
  unbemerkt für die ganze Session aus (mit Actionbar-Hinweis).
- **Gefolge-Anzeigename griff nicht überall**: Ein geänderter Gefolge-Name
  erschien nur in der Minimap-HUD, nicht auf der Großkarte (Einzel-Lehen-Label)
  und nicht in der Region-Benachrichtigung. Ursache: der Setter baute den
  Label-Cache nicht neu (`invalidateGroups` fehlte) und beide Stellen lasen den
  Roh-Namen statt des Overrides.
- **Minimap-Wappen-Overlay überlappte die Tabliste**: Das Wappen-/Namens-Overlay
  wird jetzt ausgeblendet, solange die Spielerliste (Tab) gehalten wird.
- **„Farbe überschreibt" wirkte nicht**: Hatte ein Titel „Farbe überschreibt"
  aktiv, schlug die Titel-/Namensfarbe trotzdem nicht den Personen-Override —
  in Chat, Tabliste und Namensschild gewann immer die individuelle Spielerfarbe.
  Jetzt setzt sich bei aktivem Flag die Katalog-Titel- und -Namensfarbe durch.
- **Titel-Namensfarbe griff nicht in den GUIs**: Die eigene Namensfarbe eines
  Titels ist jetzt der Default im RP-Buch und erscheint auch in der
  Kennenlern-GUI und der Namensschild-Vorschau (vorher nur die globale
  Standard-Namensfarbe).
- **RP-Name zurücksetzen leerte das Feld**: Das Reset-Icon stellt jetzt den
  API-Original-Namen wieder her (lokal gecacht via `apiRpName`, auch wenn ein
  eigener Name gesetzt ist), sonst den gespeicherten Namen — statt das Feld zu
  leeren.

## [0.1.5] – 2026-06-14

### Hinzugefügt
- **Brief-Formatierung (Farben & Stile)**: optionale Formatierungs-Leiste rechts
  neben dem Brief-Editor — schmaler Pergament-Streifen mit den 16 Minecraft-Farben
  (8 Reihen × 2 Spalten) und Stilbuttons **B/I/U/S/R** (fett, kursiv,
  unterstrichen, durchgestrichen, Reset). Klick fügt den Code an der
  Cursorposition ein; der Text wird **live formatiert** angezeigt (auch über
  automatische Zeilenumbrüche hinweg). Tooltips nennen Farbname und Code.
  Aus der Zwischenablage eingefügte `&`-Codes werden optional zu `§` umgewandelt,
  damit sie sofort formatiert erscheinen. Abschaltbar (Einstellungen → Brief →
  „Formatierung").

### Geändert
- **`§`→`&`-Umwandlung beim Senden** ist jetzt zentralisiert und greift auch im
  **LEGACY-Zeilenmodus** (vorher nur PAGE) — formatierte Briefe wurden dort sonst
  mit rohen `§`-Codes verschickt.

## [0.1.4] – 2026-06-14

### Hinzugefügt
- **Gefolge-Liste** (Einstellungen → Karte → Basis → „Gefolge…"): alle Gefolge
  als Liste, hierarchisch (Lehnsherr + alle Vasallen eingerückt) oder flach
  (alle Lehen), mit Suche, Wappen und 3-zeiliger Anzeige (Gefolge / Titel /
  Lehen). Pro Gefolge **Anzeigename** (lokaler Override — greift überall, auch
  auf Karte/Minimap, und ist gegen automatische Updates fest) und **politische
  Farbe** (pro Fraktion); zusätzlich **Farbe pro einzelnem Lehen**. Button
  „Auf ganzes Gefolge anwenden" (Lehnsherr + Vasallen), Live-Farbvorschau,
  Reset auf die ausgelieferte Standardfarbe.
- **Brief – Seiten-Modus (PAGE)**: pro Buchseite genau ein `/letter` statt pro
  Zeile (eine Seite = eine Discord-Verkündung). Editor mit **echtem Buch-Umbruch**
  (pixelgenau wie das Vanilla-Buch), 12 Zeilen/Seite, Auto-Split zu langer
  Seiten. Umschaltbar (LEGACY bleibt erhalten); seit dieser Version Standard.
- **Empfängerliste** (Brief): Online-Spieler zuerst, mit Spielerkopf je Eintrag.
- **Namensfarben** für Spieler- und RP-Namen global einstellbar (RP-Namen) und
  **pro Titel** überschreibbar; einheitlich in Chat, Tabliste und Namensschild.
- **Politisches Overlay**: Deckkraft per Schieberegler (Tag/Nacht getrennt);
  nachts automatisch reduziert.
- Pause-Menü: **Mod-Icon-Button** unten links öffnet die OttoExtra-Einstellungen.
- Titel-Filter im Personenbuch zeigt jetzt **alle (auch neue) Kategorien** dynamisch.

### Geändert
- **Tab- und Namensschild-Namen** werden immer in der OttoExtra-Farbe angezeigt
  (keine ungefärbten/Vanilla-Namen mehr; unsere Farbe schlägt Server-/Team-Farbe).
- **OttoPlus-Import** übernimmt nur noch den RP-Namen (kein Titel/keine Farbe).
- Mehrere aktive Einstellungen als neue Standardwerte übernommen.

### Behoben
- **Gemalte Karte schwarz in Modpacks**: Die Worldmap-Painted-Map wurde im
  echten Modpack (z. B. Modrinth) gar nicht gezeichnet (schwarzer Hintergrund),
  funktionierte aber im Dev. Ursache: Der Render-Hook hing an Xaeros geerbter
  Minecraft-Methode (`GuiMap.render`), deren Name in der ausgelieferten Mod
  `method_25394` heißt — die Injektion wurde dort still übersprungen. Hängt jetzt
  an Xaeros eigener `MapElementRenderHandler.render` (Name überall gleich).
  Zusätzlich: Karten-Texturen direkt aus dem Mod-JAR (kein Override durch den
  Server-Resourcepack), Neuaufbau der Karten-Pipeline nach Resource-Reload und
  ein Schalter „Gemalte Karte: einfacher Modus" (Karte → Erweitert) als Fallback.
- **Langer Chat bei Befehlen**: Das angehobene Zeichenlimit griff auch bei
  Befehlen (`/…`), sodass man überlange, vom Server abgelehnte Befehle tippen
  konnte. Beginnt die Eingabe mit `/`, gilt wieder das Vanilla-Limit (256).

## [0.1.3] – 2026-06-14

### Hinzugefügt
- Titelkatalog: **eigene Kategorien** anlegbar (Name + Farbe), erscheinen sofort
  im Kategorie-Wechsel und Filter.
- Titel-Editor: **Reset-Icon je Feld** (Titel, Variante 1/2, Farbe) — setzt nur
  das jeweilige Feld auf Standard zurück.
- „Neuer Titel" als breiter Button direkt unter der Titelliste (auffälliger).
- Lange Nachrichten: einstellbares **Absende-Intervall in Millisekunden**
  (500–1500, Standard 800) unter Chat → Erweitert.
- Minimap-Lehnsherr **einstellbar**: direkter Lehnsherr oder oberster der
  Vasallenkette (z. B. statt Mayenburg → Holdstewik). Karte → Minimap → HUD.

### Behoben
- **Titelfarbe griff nicht überall**: Beim Speichern wurde die Katalog-Titelfarbe
  als Spieler-Override gebacken, sodass spätere Farbänderungen am Titel nicht mehr
  durchschlugen (man musste die Farben beim Spieler manuell zurücksetzen). Jetzt
  wird bei Default-Farbe kein Override gespeichert — die Farbe folgt dem Katalog
  live; eine manuell abweichende Farbe bleibt als Override erhalten.
- **Realistische Namensschilder**: Die Sichtprüfung verband vier Körperpunkte mit
  UND, sodass das Schild schon bei Teilverdeckung (~50 %) verschwand. Jetzt
  ODER-Verknüpfung — ein sichtbarer Punkt (Kopf oder Körper) genügt; das Schild
  verschwindet erst bei voller Verdeckung.
- Label-Versatz im Titel-Tab (durch die neue Kategorie-Zeile) korrigiert.

## [0.1.2] – 2026-06-14

### Hinzugefügt
- **Proaktives Kennenlernen** (optional, Standard aus): redet eine unbekannte
  Person im RP-Chat (Sprechen/Flüstern/Rufen), schwebt ein 3D-Ausrufezeichen über
  ihrem Kopf. Shift-Rechtsklick öffnet ein Kennenlern-GUI „Kennst du <RP-Name>?"
  mit 3D-Charakter, Beispiel-Schild (Titel/Name) und Ja-speichern / Nein /
  Bearbeiten. Im proaktiven Modus werden RP-Namen nicht automatisch gelernt,
  nur als Prefill vorgeschlagen.
- 3D-Marker aus Blockbench-Model: leuchtet (Fullbright), dreht sich um die
  Y-Achse, wippt; Debug-Einstellungen für Höhe, Größe, Drehtempo und Glow.
- **Shift+Tab** im Chat wechselt den Kanal (statt Spielernamen-Autovervollständigung)
  — abschaltbar, Standard an.
- **Lange Nachrichten**: einzeilig unbegrenzt tippen; beim Senden automatisch an
  Wortgrenzen in mehrere Teile (≤ Limit) splitten und gestaffelt senden, sodass
  sie wie eine große Nachricht wirken (Fortsetzungs-Marker, einstellbar).
- Kennenlern-/Personenbuch-Vorschau zeigt den Titel über dem Namen in den
  Namensschild-Farben; Name wird auf die Boxbreite begrenzt (kein Überlauf).

### Geändert
- „Personen verwalten" steht in den Einstellungen ganz oben unter RP-Namen.
- Personenbuch: Änderung an RP-Name, Titel oder Farbe sperrt das Profil
  automatisch (kein versehentliches Überschreiben durch Auto-Sync).

### Behoben
- Weltkarte: gemalte Karte (PaintedMap) wird vor der Waypoint-Ebene gerendert —
  Waypoints werden nicht mehr von nicht aufgedecktem Gebiet überdeckt.
- Chat: kein Titel mehr vor unbekannten Personen in RP-Kanälen; Server-Titel auch
  im OOC ausgeblendet (kein Doppeltitel); doppelte Leerzeichen kollabieren.
- Unbekannte Personen ohne RP-Namen: kein Titel im Namensschild.
- Kennenlern-GUI: Titel/Name erst vorausgefüllt, wenn die Person geredet hat
  (kein Tablist-Prefill); Editor-Felder Titel über Name, Live-Vorschau beim Tippen.

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

[0.1.9]: https://github.com/Skyfuller1303/OttoExtra/releases/tag/0.1.9
[0.1.0]: https://example.com/ottoextra/releases/0.1.0
