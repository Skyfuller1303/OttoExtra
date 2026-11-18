# Neue Kategorien für die OttoExtra-Einstellungen

Grundlage ist die Bestandsaufnahme in `input/`. Dieser Vorschlag ordnet jede vorhandene Einstellung genau einmal neu zu. Er ändert keine Funktion und keinen Config-Schlüssel, nur den Ort im GUI. Neue Beschriftungen und Hilfetexte sind ein eigener, späterer Schritt.

## Leitgedanken

1. **Kategorien nach dem, was der Spieler sieht, nicht nach Modulen der Mod.** Wer die Namensschilder ändern will, sucht unter „Namen“, nicht unter „Nametags“ oder „RP-Namen“. Der Sammelreiter „Tweaks“ entfällt; seine acht Funktionen wandern in die Kategorie, zu der sie inhaltlich gehören.
2. **Drei Stufen statt „Basis“ und „Erweitert“.** Jede Einstellung bekommt eine Stufe:
   - **Einfach** – immer sichtbar. Schalter und Auswahlen, die jeder versteht.
   - **Mehr** – im selben Abschnitt hinter „Mehr Optionen“ eingeklappt. Farben, Größen, Feinheiten.
   - **Experte** – nur sichtbar, wenn in der Fußzeile „Experten-Einstellungen anzeigen“ eingeschaltet ist. Zeitwerte in Millisekunden, Pixelmaße, Adressen, Befehle.
   
   Die Tabs „Basis“ und „Erweitert“ entfallen damit. Eine Einstellung bleibt bei ihrem Thema, statt in einen anderen Tab zu wandern. Tabs gibt es nur noch dort, wo eine Kategorie zwei wirklich verschiedene Dinge enthält (Weltkarte und Minimap).
3. **Der Hauptschalter steht oben im Abschnitt.** Ist er aus, werden die übrigen Einstellungen des Abschnitts ausgegraut.
4. **Addons docken an bestehende Kategorien an.** Ein Addon bekommt keinen eigenen Reiter mehr, sondern einen Abschnitt in der passenden Kategorie, mit einem kleinen Kennzeichen „Addon“. Das Addon AnimalBreeding macht das heute schon so. Für Addons ohne passende Kategorie bleibt eine Sammelkategorie „Addons“ als Rückfallebene.
5. **Jede Einstellung nur an einem Ort.** Doppelte Einträge (zweimal „Lange Nachrichten“, zweimal „Vorschau“, zwei Vote-Erinnerungen) werden zusammengelegt.
6. **Kategorien in einer Seitenleiste links statt in einer Reiterleiste oben.** Zehn Einträge untereinander bleiben bei jeder Fensterbreite lesbar; die Reiterleiste klappt heute bei schmalen Fenstern zu einem Menü zusammen.

## Die neue Struktur auf einen Blick

Zehn Kategorien ersetzen die heutigen 18 Reiter (10 Kern, 7 Addon, Backups).

| Nr. | Kategorie | Frage des Spielers | Enthält (bisheriger Ort) |
|---|---|---|---|
| 1 | Allgemein | „Wo stelle ich Grundsätzliches ein?“ | Server-Resourcepack; Vote-Reminder (aus Tweaks und Addon Vote); HUD anordnen (aus Karte) |
| 2 | Chat | „Wie sieht mein Chat aus und wie schreibe ich?“ | Chatkanäle; Addons TranslateUtils, Discord, Skript |
| 3 | Namen & Personen | „Wie heißen die anderen und wo steht ihr Name?“ | RP-Namen; Namensschilder; Tabliste (Addon TacticalFactions: ein Eintrag) |
| 4 | Karte & Minimap | „Was zeigt meine Karte?“ | Karte (ohne HUD anordnen); Addon Resources: ein Eintrag |
| 5 | Lehen & Regionen | „Was erscheint, wenn ich ein Gebiet betrete?“ | Regionen; Aktivitäts-HUD (aus Tweaks) |
| 6 | Briefe | „Wie schreibe und verschicke ich Briefe?“ | Briefe |
| 7 | Tiere & Hof | „Was sehe ich bei Tieren?“ | Tierinfo; Addons AnimalBreeding, Farmer |
| 8 | Kampf | „Was ändert sich im Kampf?“ | Kampfmodus und Verwundung (aus Tweaks); Addon TacticalFactions |
| 9 | Handwerk & Bauen | „Was hilft mir beim Herstellen und Bauen?“ | Herstellungsanzeige, Werkzeugschutz, Litematica (aus Tweaks); Addon Resources |
| 10 | Datenschutz & Sicherung | „Was verlässt meinen Rechner, was wird gespeichert?“ | Datenschutz; Chatprotokoll (aus Tweaks); Backups |

Verteilung der Einstellungen auf die Stufen (Kern und alle Addons zusammen):

| Nr. | Kategorie | Einfach | Mehr | Experte | Summe |
|---|---|---|---|---|---|
| 1 | Allgemein | 6 | 4 | 8 | 18 |
| 2 | Chat | 20 | 15 | 14 | 49 |
| 3 | Namen & Personen | 13 | 19 | 7 | 39 |
| 4 | Karte & Minimap | 21 | 14 | 14 | 49 |
| 5 | Lehen & Regionen | 15 | 7 | 17 | 39 |
| 6 | Briefe | 7 | 3 | 10 | 20 |
| 7 | Tiere & Hof | 10 | 12 | 3 | 25 |
| 8 | Kampf | 7 | 11 | 4 | 22 |
| 9 | Handwerk & Bauen | 6 | 3 | 2 | 11 |
| 10 | Datenschutz & Sicherung | 7 | 0 | 0 | 7 |
| | Summe | 112 | 88 | 79 | 279 |

Zum Vergleich: Heute stehen in der Kern-Mod 137 Einträge in den Tabs „Basis“, 11 im Tab „Minimap“ und 74 in den Tabs „Erweitert“. Mit dem Vorschlag sind ohne Aufklappen 112 von 279 Einträgen sichtbar, verteilt auf zehn Kategorien.

## Zuordnung aller Einstellungen

Die Spalte „Bisher“ nennt Reiter › Tab › Karte im heutigen GUI. Die Reihenfolge in den Tabellen ist die vorgeschlagene Reihenfolge im Abschnitt.

### 1. Allgemein

Dinge, die die ganze Mod betreffen.

**Abschnitt „Anzeigen auf dem Bildschirm anordnen“** – Ein Einstieg für alle verschiebbaren Anzeigen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| HUD anordnen (Schaltfläche) | Einfach | Karte › Basis › HUD-Layout |

**Abschnitt „Server-Resourcepack“** – Lädt das Ottonien-Resourcepack und hält es aktuell.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Resourcepack-Download | Einfach | Server-Resourcepack › Basis › Automatik |
| Pack auto-aktivieren | Einfach | Server-Resourcepack › Basis › Automatik |
| Beim Start prüfen | Einfach | Server-Resourcepack › Basis › Automatik |
| Manifest-/Release-URL | Experte | Server-Resourcepack › Erweitert › Quelle & Netzwerk |
| Asset-Name | Experte | Server-Resourcepack › Erweitert › Quelle & Netzwerk |
| Connect-Timeout (ms) | Experte | Server-Resourcepack › Erweitert › Quelle & Netzwerk |
| Request-Timeout (ms) | Experte | Server-Resourcepack › Erweitert › Quelle & Netzwerk |

**Abschnitt „Vote-Erinnerung“** – Erinnert einmal am Tag ans Voten.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Vote-Reminder aktivieren | Einfach | Tweaks › Basis › Vote-Reminder |
| Wiederholung (Minuten) | Mehr | Tweaks › Basis › Vote-Reminder |
| Anzeigedauer (Sekunden) | Mehr | Tweaks › Basis › Vote-Reminder |
| Resetstunde (Europe/Berlin) | Experte | Tweaks › Basis › Vote-Reminder |
| Verzögerung nach Join (Sekunden) | Experte | Tweaks › Basis › Vote-Reminder |
| Vote-Reminder aktivieren | Einfach | Addon Vote: Vote Reminder › Basis › Tägliche Erinnerung |
| Wiederholung (Minuten) | Mehr | Addon Vote: Vote Reminder › Basis › Tägliche Erinnerung |
| Anzeigedauer (Sekunden) | Mehr | Addon Vote: Vote Reminder › Basis › Tägliche Erinnerung |
| Resetstunde (Europe/Berlin) | Experte | Addon Vote: Vote Reminder › Basis › Tägliche Erinnerung |
| Verzögerung nach Join (Sekunden) | Experte | Addon Vote: Vote Reminder › Basis › Tägliche Erinnerung |

### 2. Chat

Alles rund ums Schreiben und Lesen im Chat.

**Abschnitt „Kanäle“** – Kanalanzeige neben der Eingabe und Kanalwechsel.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Chat-Modul | Einfach | Chatkanäle › Basis › Kanäle |
| Beim Join automatisch /s (Sprechen) | Einfach | Chatkanäle › Basis › Kanäle |
| Shift+Tab wechselt Kanal | Einfach | Chatkanäle › Basis › Kanäle |

**Abschnitt „Offtopic-Warnung“** – Warnzeichen vor dem öffentlichen Kanal.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Offtopic-Warnzeichen (!) | Einfach | Chatkanäle › Basis › Offtopic-Warnung |
| Anzahl ! | Mehr | Chatkanäle › Basis › Offtopic-Warnung |

**Abschnitt „Lange Nachrichten“** – Lange Texte werden automatisch in mehrere Nachrichten geteilt.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Lange Nachrichten splitten | Einfach | Chatkanäle › Basis › Lange Nachrichten |
| Fortsetzungs-Marker | Mehr | Chatkanäle › Basis › Lange Nachrichten |
| Max. Zeichen je Teil | Experte | Chatkanäle › Basis › Lange Nachrichten |
| Max. Tipplänge | Experte | Chatkanäle › Basis › Lange Nachrichten |
| Absende-Intervall (ms) | Experte | Chatkanäle › Erweitert › Lange Nachrichten |

**Abschnitt „Farben & Hervorhebung“** – Farben der Kanäle, von Emotes und von OOC-Text.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Emotes und OOC farblich darstellen | Einfach | Chatkanäle › Basis › RP-Chatformatierung |
| Chatkanal-Farben verwalten… (Schaltfläche) | Einfach | Chatkanäle › Erweitert › Chatkanal-Farben |
| Farbe: *Emotes* | Mehr | Chatkanäle › Basis › RP-Chatformatierung |
| *Emotes* kursiv | Mehr | Chatkanäle › Basis › RP-Chatformatierung |
| Farbe: (OOC-Kommentare) | Mehr | Chatkanäle › Basis › RP-Chatformatierung |
| (OOC) kursiv | Mehr | Chatkanäle › Basis › RP-Chatformatierung |

**Abschnitt „Übersetzung“** – Addon TranslateUtils.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Eigene Nachrichten übersetzen | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Serveranzeigen übersetzen | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Datenschutzhinweis (Schaltfläche) | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Ausgehende Eingabesprache | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Ausgehende Zielsprache | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Server-Eingabesprache | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Anzeige-Zielsprache | Einfach | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Sprachkürzel an der Eingabe anzeigen | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Original als OOC-Nachricht senden | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Statusmeldung beim Umschalten | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Emotes *...* schützen | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Geschützte Inhalte |
| OOC-Passagen (...) schützen | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Geschützte Inhalte |
| URLs schützen | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Geschützte Inhalte |
| Eckige Tags schützen | Mehr | Addon TranslateUtils: TranslateUtils › Basis › Geschützte Inhalte |
| Original geschützt per Hover | Experte | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Erlaubte Usernamen (Schaltfläche) | Experte | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Freigegebenes Original als Chatzeile | Experte | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Lokaler Übersetzungscache (Schaltfläche) | Experte | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| Freigegebenes Übersetzungsbundle (Schaltfläche) | Experte | Addon TranslateUtils: TranslateUtils › Basis › Übersetzung |
| API-URL | Experte | Addon TranslateUtils: TranslateUtils › Erweitert › Übersetzungs-API |
| API-Key | Experte | Addon TranslateUtils: TranslateUtils › Erweitert › Übersetzungs-API |
| Maximale Zeichen | Experte | Addon TranslateUtils: TranslateUtils › Erweitert › Übersetzungs-API |
| Timeout (ms) | Experte | Addon TranslateUtils: TranslateUtils › Erweitert › Übersetzungs-API |

**Abschnitt „Discord-Brücke“** – Addon Discord.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Discord-Brücke aktiv | Einfach | Addon Discord: Discord › Basis › Discord-Brücke |
| Eigene Chatnachrichten übertragen | Einfach | Addon Discord: Discord › Basis › Discord-Brücke |
| Verbindungsstatus übertragen | Einfach | Addon Discord: Discord › Basis › Discord-Brücke |
| Statusmeldungen anzeigen | Mehr | Addon Discord: Discord › Basis › Discord-Brücke |
| API-Endpunkt | Experte | Addon Discord: Discord › Basis › Endpunkt |
| Pollintervall (Sekunden) | Experte | Addon Discord: Discord › Basis › Endpunkt |

**Abschnitt „Skripte“** – Addon Skript. Zuordnung zu „Chat“ ist eine Annahme und zu prüfen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| OttoSkript aktiviert | Einfach | Addon Skript: OttoSkript › Basis › OttoSkript aktiviert |
| Bibliothek öffnen (Schaltfläche) | Einfach | Addon Skript: OttoSkript › Basis › Skripte und Vorschau |
| Skriptbutton im Chat anzeigen | Einfach | Addon Skript: OttoSkript › Basis › Skripte und Vorschau |
| HUD positionieren (Schaltfläche) | Mehr | Addon Skript: OttoSkript › Basis › Skripte und Vorschau |

### 3. Namen & Personen

Wie andere Spieler heißen und wo ihr Name erscheint.

**Abschnitt „Personenbuch“** – Bekannte Personen ansehen und verwalten.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Personen verwalten… (Schaltfläche) | Einfach | RP-Namen › Basis › Personenbuch |

**Abschnitt „Namen im Chat“** – RP-Namen statt Accountnamen im Chat.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| RP-Namen-Modul | Einfach | RP-Namen › Basis › Chat |
| RP-Namen in OOC-Chats (Offtopic/Hilfe) | Einfach | RP-Namen › Basis › Chat |
| Shift-Klick öffnet Personenbuch | Mehr | RP-Namen › Basis › Schnellzugriff |

**Abschnitt „Unbekannte Personen“** – Was bei Personen steht, die die eigene Figur noch nicht kennt.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Unbekannte als „Unbekannt“ zeigen | Einfach | RP-Namen › Basis › Chat |
| Platzhalter | Mehr | RP-Namen › Basis › Chat |
| Unbekannte: Accountname zeigen | Mehr | RP-Namen › Basis › Chat |

**Abschnitt „Namen über dem Kopf“** – Namensschilder über anderen Spielern.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Namensschild-Modul | Einfach | Namensschilder › Basis › Sichtbarkeit |
| Namensschilder | Einfach | Namensschilder › Basis › Sichtbarkeit |
| Titel anzeigen | Einfach | Namensschilder › Basis › Zeilen |
| RP-Name am Schild | Einfach | Namensschilder › Basis › Zeilen |
| Accountname am Schild | Einfach | Namensschilder › Basis › Zeilen |
| Titel-Schriftgröße | Mehr | Namensschilder › Erweitert › Größe & Farbe |
| Name-Schriftgröße | Mehr | Namensschilder › Erweitert › Größe & Farbe |
| Account-Schriftgröße | Mehr | Namensschilder › Erweitert › Größe & Farbe |
| Account-Farbe (#RRGGBB) | Mehr | Namensschilder › Erweitert › Größe & Farbe |
| Zeilenabstand | Experte | Namensschilder › Erweitert › Größe & Farbe |

**Abschnitt „Namen in der Tabliste“** – Die Spielerliste auf der Tab-Taste.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| RP-Namen in Tabliste | Einfach | RP-Namen › Basis › Tabliste |
| Titel in Tabliste anzeigen | Einfach | RP-Namen › Basis › Tabliste |
| Titel automatisch aktuell halten | Mehr | RP-Namen › Basis › Tabliste |
| Tabliste nach Lehen sortieren | Mehr | RP-Namen › Basis › Tabliste |
| Gegner zuerst | Mehr | Addon TacticalFactions: Tactical Factions › Basis › Tabliste |

**Abschnitt „Namensfarben“** – Eigene Farben für RP-Namen und Accountnamen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| RP-Name-Farbe | Mehr | RP-Namen › Basis › Namensfarben |
| Spielername-Farbe | Mehr | RP-Namen › Basis › Namensfarben |
| Adelsfarbe auch für andere Titel | Mehr | RP-Namen › Basis › Namensfarben |

**Abschnitt „Kennenlernen“** – Dialog und Markierung, wenn man jemanden zum ersten Mal trifft.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Namen kennenlernen (proaktiv) | Einfach | RP-Namen › Basis › Schnellzugriff |
| Leuchten (Fullbright) | Mehr | RP-Namen › Basis › Kennenlern-Marker (3D) |
| Höhe über Kopf | Experte | RP-Namen › Basis › Kennenlern-Marker (3D) |
| Größe | Experte | RP-Namen › Basis › Kennenlern-Marker (3D) |
| Drehtempo (Y-Achse) | Experte | RP-Namen › Basis › Kennenlern-Marker (3D) |

**Abschnitt „Personen untersuchen“** – Genaues Ansehen einer Person mit der Untersuchen-Taste.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| RP-Untersuchungsmodus aktivieren | Einfach | RP-Namen › Basis › RP-Untersuchungsmodus |
| Sichtbare Gegenstände in den Händen | Mehr | RP-Namen › Basis › RP-Untersuchungsmodus |
| Sichtbare Rüstung | Mehr | RP-Namen › Basis › RP-Untersuchungsmodus |
| Sanfter Zoom | Mehr | RP-Namen › Basis › RP-Untersuchungsmodus |
| Rand leicht weichzeichnen | Mehr | RP-Namen › Basis › RP-Untersuchungsmodus |
| Wechselnde RP-Gedanken | Mehr | RP-Namen › Basis › RP-Untersuchungsmodus |
| Zoomstärke | Experte | RP-Namen › Basis › RP-Untersuchungsmodus |
| Dauer des RP-Gedankens | Experte | RP-Namen › Basis › RP-Untersuchungsmodus |
| Untersuchungsdauer | Experte | RP-Namen › Basis › RP-Untersuchungsmodus |

### 4. Karte & Minimap

Weltkarte und Minimap (Xaero) mit den Ottonien-Ergänzungen.

#### Tab „Weltkarte“

**Abschnitt „Allgemein“** – Hauptschalter der Kartenergänzungen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Karten-Modul | Einfach | Karte › Basis › Weltkarte |
| Nur auf Ottonien | Mehr | Karte › Basis › Weltkarte |

**Abschnitt „Was die Karte zeigt“** – Welche Informationen auf der Weltkarte erscheinen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Lehengrenzen | Einfach | Karte › Basis › Weltkarte |
| Regionsnamen | Einfach | Karte › Basis › Weltkarte |
| Wappen | Einfach | Karte › Basis › Weltkarte |
| Spieler-Aktivität | Einfach | Karte › Basis › Weltkarte |
| NPC-Dörfer anzeigen | Einfach | Karte › Basis › Weltkarte |
| Lehen-Infokarte per Klick | Einfach | Karte › Basis › Weltkarte |

**Abschnitt „Kartenstil“** – Gemalte Karte, Pergament und politische Einfärbung.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Gemalte Karte | Einfach | Karte › Basis › Weltkarte |
| Pergament-Kartenmodus | Einfach | Karte › Basis › Weltkarte |
| Politische Karte | Einfach | Karte › Basis › Weltkarte |
| Öffnungsanimation der Weltkarte | Einfach | Karte › Minimap › Minimap-Overlays |
| Politik-Deckkraft | Mehr | Karte › Erweitert › Darstellung |
| Politik-Deckkraft (Nacht) | Mehr | Karte › Erweitert › Darstellung |
| Vollflächige Karte beim Rauszoomen | Mehr | Karte › Basis › Weltkarte |
| Gemalte Karte: einfacher Modus | Mehr | Karte › Erweitert › Darstellung |
| Pergament-Rand (px) | Experte | Karte › Erweitert › Darstellung |

**Abschnitt „Farben & Grenzen“** – Farben der Gefolge und Aussehen der Grenzlinien.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Gefolge-Liste… (Schaltfläche) | Einfach | Karte › Basis › Weltkarte |
| Gefolge-Farben… (Schaltfläche) | Einfach | Karte › Minimap › Wappen-HUD |
| Grenzfarbe (#AARRGGBB) | Mehr | Karte › Erweitert › Darstellung |
| Strichgrafik | Mehr | Karte › Erweitert › Darstellung |

**Abschnitt „Reisezeit“** – Geschwindigkeiten für die Reisezeit-Schätzung.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Reisehilfe: Fußtempo (Blöcke/s) | Mehr | Karte › Erweitert › Darstellung |
| Reisehilfe: Pferdetempo (Blöcke/s) | Mehr | Karte › Erweitert › Darstellung |

**Abschnitt „Zoom & Ausrichtung“** – Ab welchem Zoom etwas erscheint; Ausrichtung der gemalten Karte.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Namen ab Zoom | Experte | Karte › Erweitert › Darstellung |
| Wappen ab Zoom | Experte | Karte › Erweitert › Darstellung |
| Politische Karte bis Zoom | Experte | Karte › Erweitert › Darstellung |
| Kalibrier-Pfeile (Karte verschieben) | Experte | Karte › Erweitert › Darstellung |

#### Tab „Minimap“

**Abschnitt „Minimap-Darstellung“** – Was die Minimap zusätzlich zeigt.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Grenzen auf Minimap | Einfach | Karte › Minimap › Minimap-Overlays |
| Gemalte Karte auf Minimap | Einfach | Karte › Minimap › Minimap-Overlays |
| Politische Flächen auf Minimap | Einfach | Karte › Minimap › Minimap-Overlays |
| Statuspunkt auf der Minimap anzeigen | Mehr | Addon Resources: Ressourcen › Basis › Minimap |

**Abschnitt „Lehen-Anzeige an der Minimap“** – Wappen, Name, Stand und Fraktion des Lehens, in dem man steht.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Wappen an Minimap | Einfach | Karte › Minimap › Wappen-HUD |
| Wappen: Lehensname | Einfach | Karte › Minimap › Wappen-HUD |
| Wappen: Stand/Hierarchie | Einfach | Karte › Minimap › Wappen-HUD |
| Gefolgename anzeigen | Einfach | Karte › Minimap › Wappen-HUD |
| Wappen-Position (Drag&Drop)… (Schaltfläche) | Einfach | Karte › Minimap › Wappen-HUD |
| Obersten Lehnsherrn zeigen | Mehr | Karte › Minimap › Wappen-HUD |

**Abschnitt „Feinabstimmung der Lehen-Anzeige“** – Farben und genaue Maße der drei Textzeilen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| HUD Name: Farbe (#RRGGBB) | Mehr | Karte › Erweitert › HUD-Feinjustierung |
| HUD Stand: Farbe (#RRGGBB) | Mehr | Karte › Erweitert › HUD-Feinjustierung |
| HUD Gefolge: Farbe (#RRGGBB) | Mehr | Karte › Erweitert › HUD-Feinjustierung |
| HUD Name: Skalierung | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Stand: Skalierung | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Gefolge: Skalierung | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Name: Breite | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Stand: Breite | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Gefolge: Breite | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Name: Höhe | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Stand: Höhe | Experte | Karte › Erweitert › HUD-Feinjustierung |
| HUD Gefolge: Höhe | Experte | Karte › Erweitert › HUD-Feinjustierung |

### 5. Lehen & Regionen

Einblendungen und Listen zu dem Gebiet, in dem man sich bewegt.

**Abschnitt „Einblendung beim Betreten“** – Die Meldung, wenn man ein Lehen betritt.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Regionen-Modul | Einfach | Regionen › Basis › Regions-Anzeige |
| Betreten-Sound | Einfach | Regionen › Basis › Regions-Anzeige |
| Vorschau anzeigen (Schaltfläche) | Einfach | Regionen › Basis › Regions-Anzeige; Regionen › Erweitert › Badge-Layout |
| Actionbar ausblenden | Mehr | Regionen › Basis › Regions-Anzeige |
| Toast-Dauer (ms) | Mehr | Regionen › Erweitert › Badge-Layout |

**Abschnitt „Inhalt der Einblendung“** – Was in der Meldung steht.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Wappen im Toast | Einfach | Regionen › Basis › Regions-Anzeige |
| Herrschaft im Toast | Einfach | Regionen › Basis › Regions-Anzeige |
| Lehnsherr im Toast | Einfach | Regionen › Basis › Regions-Anzeige |
| RP-Name des Lehnsherrn | Einfach | Regionen › Basis › Regions-Anzeige |
| Koordinaten im Toast | Einfach | Regionen › Basis › Regions-Anzeige |
| Hinweiszeile anzeigen | Mehr | Regionen › Erweitert › Badge-Layout |

**Abschnitt „Aussehen der Einblendung“** – Stil, Platz auf dem Bildschirm und Farbschema.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Banner-Stil | Einfach | Regionen › Basis › Regions-Anzeige |
| Position | Einfach | Regionen › Basis › Regions-Anzeige |
| Toast-Themes (Schaltfläche) | Einfach | Regionen › Basis › Regions-Anzeige |

**Abschnitt „Maße & Schrift der Einblendung“** – Genaue Größen. Langfristig nur noch im Farbschema-Editor (siehe offene Punkte).

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Schrift: Basis | Experte | Regionen › Erweitert › Badge-Schrift |
| Schrift: Titel | Experte | Regionen › Erweitert › Badge-Schrift |
| Schrift: Region | Experte | Regionen › Erweitert › Badge-Schrift |
| Schrift: Hierarchie | Experte | Regionen › Erweitert › Badge-Schrift |
| Schrift: Hinweis | Experte | Regionen › Erweitert › Badge-Schrift |
| Max. Textbreite | Experte | Regionen › Erweitert › Badge-Layout |
| Min. Toastbreite | Experte | Regionen › Erweitert › Badge-Layout |
| Max. Toastbreite | Experte | Regionen › Erweitert › Badge-Layout |
| Abstand oben | Experte | Regionen › Erweitert › Badge-Layout |
| Wappengröße | Experte | Regionen › Erweitert › Badge-Layout |
| Wappenabstand | Experte | Regionen › Erweitert › Badge-Layout |
| Innenabstand links | Experte | Regionen › Erweitert › Badge-Layout |
| Innenabstand rechts | Experte | Regionen › Erweitert › Badge-Layout |
| Innenabstand oben | Experte | Regionen › Erweitert › Badge-Layout |
| Innenabstand unten | Experte | Regionen › Erweitert › Badge-Layout |

**Abschnitt „Aktivitäts-Anzeige“** – Liste aktueller Aktivitäten am Bildschirmrand.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Aktivitäts-HUD anzeigen | Einfach | Tweaks › Basis › Aktivitäts-HUD |
| Aktuelle Uhrzeit anzeigen | Einfach | Tweaks › Basis › Aktivitäts-HUD |
| Letzte Aktualisierung anzeigen | Einfach | Tweaks › Basis › Aktivitäts-HUD |
| Maximal sichtbare Aktivitäten | Mehr | Tweaks › Basis › Aktivitäts-HUD |
| Position des Aktivitäts-HUDs | Mehr | Tweaks › Basis › Aktivitäts-HUD |
| Größe des Aktivitäts-HUDs | Mehr | Tweaks › Basis › Aktivitäts-HUD |
| Aktivitäts-HUD frei platzieren (Schaltfläche) | Mehr | Tweaks › Basis › Aktivitäts-HUD |
| Aktivitäts-HUD: X-Versatz | Experte | Tweaks › Erweitert › Aktivitäts-HUD verschieben |
| Aktivitäts-HUD: Y-Versatz | Experte | Tweaks › Erweitert › Aktivitäts-HUD verschieben |

**Abschnitt „Kartendaten“** – Daten zu Lehen und Gefolgen neu vom Server holen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Kartendaten neu laden (Schaltfläche) | Einfach | Regionen › Basis › Regions-Anzeige |

### 6. Briefe

Briefe und Verkündungen schreiben.

**Abschnitt „Brief-Editor“** – Der Editor zum Schreiben.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Brief-Modul | Einfach | Briefe › Basis › Editor |
| Brief-Editor öffnen… (Schaltfläche) | Einfach | Briefe › Basis › Editor |
| Briefvorschau aktivieren | Einfach | Briefe › Basis › Formatierung |
| Versandmenü nach dem Schreiben öffnen | Einfach | Briefe › Basis › Editor |

**Abschnitt „Schreiben & Formatieren“** – Formatierung, Seitenleiste und Signatur.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Formatierung aktivieren | Einfach | Briefe › Basis › Formatierung |
| Leiste rechts anzeigen | Einfach | Briefe › Basis › Formatierung |
| Signatur | Einfach | Briefe › Basis › Formatierung |
| &-Codes beim Einfügen umwandeln | Mehr | Briefe › Basis › Formatierung |

**Abschnitt „Archiv“** – Ablage gespeicherter Briefe.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Briefexporte nach Spielernamen ablegen | Mehr | Briefe › Basis › Editor |

**Abschnitt „Verkündungen“** – Seitenaufteilung für Verkündungen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Automatische Optimierung erlauben | Mehr | Briefe › Erweitert › Layout & Timing |
| Sichere Zeilen/Seite | Experte | Briefe › Erweitert › Layout & Timing |
| Sichere Zeichen/Zeile | Experte | Briefe › Erweitert › Layout & Timing |

**Abschnitt „Versand“** – Technik des Versands. Nur ändern, wenn der Versand nicht funktioniert.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Versandmodus | Experte | Briefe › Basis › Editor |
| Auslöse-Item (Name) | Experte | Briefe › Basis › Editor |
| Zeilen-Befehl | Experte | Briefe › Erweitert › Serverbefehle |
| Versand-Befehl | Experte | Briefe › Erweitert › Serverbefehle |
| Verkündungs-Abschluss | Experte | Briefe › Erweitert › Serverbefehle |
| Zeilen-Delay min (ms) | Experte | Briefe › Erweitert › Layout & Timing |
| Zeilen-Delay max (ms) | Experte | Briefe › Erweitert › Layout & Timing |
| Seiten-Delay (PAGE, ms) | Experte | Briefe › Erweitert › Layout & Timing |

### 7. Tiere & Hof

Tierinfo, Zucht und Hilfen für die Landwirtschaft.

**Abschnitt „Tierinfo beim Anvisieren“** – Die Einblendung, wenn man ein Tier ansieht.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Automatische Tierinfo | Einfach | Tierinfo › Basis › Tierinfo beim Anvisieren |
| Stil | Einfach | Tierinfo › Basis › Toast-Stil |
| Toast-Layout bearbeiten… (Schaltfläche) | Mehr | Tierinfo › Basis › Tierinfo beim Anvisieren |
| Automatische Antwort ausblenden | Mehr | Tierinfo › Basis › Tierinfo beim Anvisieren |

**Abschnitt „Namen über Tieren“** – Geschlecht am Namensschild der Tiere.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Geschlecht im Nametag anzeigen | Einfach | Tierinfo › Basis › Tier-Nametag |
| Tiernamen nach Geschlecht färben | Einfach | Tierinfo › Basis › Tier-Nametag |
| Auch ungespeicherte Tiere | Mehr | Tierinfo › Basis › Tier-Nametag |

**Abschnitt „Tiere hervorheben“** – Leuchtende Umrisse nach Geschlecht, Zuchtpause und Schur.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Tiere nach Geschlecht umranden | Einfach | Tierinfo › Basis › Tier-Glow |
| Gepaarte Tiere umranden | Einfach | Tierinfo › Basis › Tier-Glow |
| Geschorene Schafe umranden | Einfach | Tierinfo › Basis › Tier-Glow |

**Abschnitt „Zucht“** – Addon AnimalBreeding.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Ich züchte Tiere | Einfach | Addon AnimalBreeding: Tierinfo › Basis › Züchtermodus |

**Abschnitt „Tierübersicht“** – Welche Tierarten die Übersicht zeigt.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Rinder | Mehr | Tierinfo › Erweitert › Tierarten in der Übersicht |
| Brakel | Mehr | Tierinfo › Erweitert › Tierarten in der Übersicht |
| Hausschweine | Mehr | Tierinfo › Erweitert › Tierarten in der Übersicht |
| Ziegen | Mehr | Tierinfo › Erweitert › Tierarten in der Übersicht |
| Zaupel | Mehr | Tierinfo › Erweitert › Tierarten in der Übersicht |

**Abschnitt „Fütter- und Pflanzhilfe“** – Addon Farmer.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Fütterhilfe aktivieren | Einfach | Addon Farmer: Farmer › Basis › Farmer-Assistenten |
| Pflanzhilfe aktivieren | Einfach | Addon Farmer: Farmer › Basis › Farmer-Assistenten |
| Schlangenlinien-Modus | Mehr | Addon Farmer: Farmer › Basis › Farmer-Assistenten |
| Statusmeldungen anzeigen | Mehr | Addon Farmer: Farmer › Basis › Farmer-Assistenten |

**Abschnitt „Gemerkte Tiere“** – Lokal gespeicherte Tierdaten.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Tiergeschlecht und Wartezeiten 14 Tage lokal merken | Mehr | Tierinfo › Erweitert › Lokaler Tiercache |
| Alle Tiere dieses Servers vergessen (Schaltfläche) | Mehr | Tierinfo › Erweitert › Lokaler Tiercache |

**Abschnitt „Abfrage“** – Zeitwerte der Serverabfrage.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Anvisierverzögerung (ms) | Experte | Tierinfo › Erweitert › Abfrageverhalten |
| Auffrischintervall (ms) | Experte | Tierinfo › Erweitert › Abfrageverhalten |
| Antwort-Zeitlimit (ms) | Experte | Tierinfo › Erweitert › Abfrageverhalten |

### 8. Kampf

Anzeigen und Effekte im Kampf.

**Abschnitt „Kampfmodus-Anzeige“** – Zeigt an, ob der Kampfmodus aktiv ist.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Kampfmodus-Icon anzeigen | Einfach | Tweaks › Basis › Kampfmodus |

**Abschnitt „Verwundung & Adrenalin“** – Bildeffekte bei wenig Leben.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Effekt aktivieren | Einfach | Tweaks › Basis › Verwundung & Adrenalin |
| Reduzierte Effekte (ohne Puls, Blur, FOV, Herzschlag) | Einfach | Tweaks › Basis › Verwundung & Adrenalin |
| Intensität | Mehr | Tweaks › Basis › Verwundung & Adrenalin |
| Roter Rand | Mehr | Tweaks › Basis › Verwundung & Adrenalin |
| Herzschlag-Sound | Mehr | Tweaks › Basis › Verwundung & Adrenalin |
| Edge-Blur (Tunnelblick) | Mehr | Tweaks › Basis › Verwundung & Adrenalin |
| FOV-Adrenalin | Mehr | Tweaks › Basis › Verwundung & Adrenalin |
| Beruhigen ohne Schaden | Mehr | Tweaks › Basis › Verwundung & Adrenalin |
| Start ab Health | Experte | Tweaks › Basis › Verwundung & Adrenalin |
| Blur ab Herzen | Experte | Tweaks › Basis › Verwundung & Adrenalin |
| Blur-Stärke | Experte | Tweaks › Basis › Verwundung & Adrenalin |
| FOV-Zuschlag (Grad) | Experte | Tweaks › Basis › Verwundung & Adrenalin |

**Abschnitt „Verbündete & Gegner“** – Addon TacticalFactions.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Tactical Operation Mode | Einfach | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Spielernamen im Kampf anzeigen | Einfach | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Politische Farben | Einfach | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Politik-Buch öffnen (Schaltfläche) | Einfach | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Angreifer mit rotem ! markieren | Mehr | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Friendly Fire verhindern | Mehr | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Neutrale Spieler ebenfalls schützen | Mehr | Addon TacticalFactions: Tactical Factions › Basis › Kampfmodus |
| Verbündetenfarbe | Mehr | Addon TacticalFactions: Tactical Factions › Basis › Politische Farben |
| Gegnerfarbe | Mehr | Addon TacticalFactions: Tactical Factions › Basis › Politische Farben |

### 9. Handwerk & Bauen

Hilfen beim Herstellen, Abbauen und Bauen.

**Abschnitt „Herstellungsanzeige“** – Eigene Fortschrittsanzeige beim Herstellen.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Eigene Herstellungsanzeige | Einfach | Tweaks › Basis › Herstellungsanzeige |
| Herstellungs-Geräusche | Einfach | Tweaks › Basis › Herstellungsanzeige |

**Abschnitt „Werkzeugschutz“** – Verhindert, dass ein Werkzeug versehentlich zerbricht.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Werkzeugschutz aktivieren | Einfach | Tweaks › Basis › Werkzeugschutz |
| Warnung unter (%) | Mehr | Tweaks › Basis › Werkzeugschutz |
| Blockieren ab (Nutzungen) | Mehr | Tweaks › Basis › Werkzeugschutz |

**Abschnitt „Ressourcen-Hinweise“** – Addon Resources.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Ressourcenmodul aktivieren | Einfach | Addon Resources: Ressourcen › Basis › Ressourcen-Hinweise |
| Hinweise beim Abbau anzeigen | Einfach | Addon Resources: Ressourcen › Basis › Ressourcen-Hinweise |
| Synchronisierungsradius | Experte | Addon Resources: Ressourcen › Basis › Ressourcen-Hinweise |
| Cache-Dauer in Sekunden | Experte | Addon Resources: Ressourcen › Basis › Ressourcen-Hinweise |

**Abschnitt „Bauen mit Litematica“** – Ergänzungen für die Mod Litematica.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Lokalisierte Blocknamen im HUD | Einfach | Tweaks › Basis › Litematica-Integration |
| Fachwerk automatisch abstimmen | Mehr | Tweaks › Basis › Litematica-Integration |

### 10. Datenschutz & Sicherung

Was die Mod ins Internet schickt, lokal speichert und sichert.

**Abschnitt „Einwilligungen“** – Jede Verbindung nach außen ist einzeln erlaubt oder verboten.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Online-Dienste erlauben | Einfach | Datenschutz › Basis › Einwilligungen |
| Automatische Resourcepack-Downloads | Einfach | Datenschutz › Basis › Einwilligungen |
| Externe Skin- und Bannerbilder | Einfach | Datenschutz › Basis › Einwilligungen |
| GitHub-Versionsprüfung | Einfach | Datenschutz › Basis › Einwilligungen |
| Beobachtete RP-Namen an API senden | Einfach | Datenschutz › Basis › Einwilligungen |

**Abschnitt „Chatprotokoll“** – Speichert den Chat lokal in einer Datei.

| Einstellung (heutige Beschriftung) | Stufe | Bisher |
|---|---|---|
| Chatprotokoll speichern | Einfach | Tweaks › Basis › Chatprotokoll |
| Chatlog-Umfang (Änderung braucht neue Freigabe) | Einfach | Tweaks › Basis › Chatprotokoll |

**Abschnitt „Backups“** – Sicherungen anlegen und wiederherstellen.

Inhalt des heutigen Reiters „Backups“ unverändert: „Backup jetzt erstellen“, Liste mit „Wiederherstellen“, „RP-Namen prüfen und reparieren“.

## Was sich gegenüber heute ändert

| Heute | Vorschlag |
|---|---|
| Reiter „Tweaks“ mit 8 Funktionen | aufgelöst: Chatprotokoll → Datenschutz & Sicherung; Kampfmodus, Verwundung → Kampf; Herstellungsanzeige, Werkzeugschutz, Litematica → Handwerk & Bauen; Aktivitäts-HUD → Lehen & Regionen; Vote-Reminder → Allgemein |
| Reiter „RP-Namen“ (28 Einträge in einem Tab) und Reiter „Namensschilder“ | eine Kategorie „Namen & Personen“ mit 8 Abschnitten, gegliedert nach dem Ort, an dem der Name erscheint |
| Reiter „Karte“ mit Tabs Basis, Minimap, Erweitert | Kategorie „Karte & Minimap“ mit Tabs Weltkarte und Minimap; „Öffnungsanimation der Weltkarte“ und „Gefolge-Farben…“ wandern vom Tab Minimap zur Weltkarte |
| „HUD anordnen“ unter Karte › Basis | unter Allgemein, als ein Einstieg für alle verschiebbaren Anzeigen |
| Reiter „Datenschutz“ und Reiter „Backups“ getrennt, Chatprotokoll unter Tweaks | eine Kategorie „Datenschutz & Sicherung“ |
| 7 eigene Addon-Reiter | Addon-Abschnitte in den passenden Kategorien |
| Vote-Reminder doppelt (Kern und Addon) | ein Abschnitt „Vote-Erinnerung“ |
| Karte „Lange Nachrichten“ zweimal, „Vorschau anzeigen“ zweimal | je einmal |

## Entscheidungen zur Gliederung

Die Kategorien gelten wie oben beschrieben. Die Abwägungen dazu:

1. **Namen im Chat stehen unter „Namen & Personen“.** Dort stehen alle Orte beieinander, an denen ein Name erscheint (Chat, Kopf, Tabliste). In „Chat“ steht im Abschnitt „Farben & Hervorhebung“ ein Verweis „Namen im Chat findest du unter Namen & Personen“.
2. **„Kampf“ und „Handwerk & Bauen“ bleiben getrennt.** Eine gemeinsame Kategorie „Spielhilfen“ wäre wieder ein Sammelbegriff wie „Tweaks“. Dass „Kampf“ ohne das Addon TacticalFactions nur zwei Abschnitte hat, ist vertretbar.
3. **Die Vote-Erinnerung ist ein einziger Abschnitt unter „Allgemein“.** Im Quelltext der Kern-Mod wurde keine Abstimmung mit dem Addon Vote gefunden; heute erscheinen bei installiertem Addon voraussichtlich beide. Ziel: Ist das Addon installiert, zeigt der Abschnitt dessen Einstellungen, sonst die der Kern-Mod.
4. **Das Addon Skript steht unter „Chat“.** Laut seiner Beschreibung dient es „MD-Skripten für Messen, Reden und Ankündigungen“, die im Chat vorgetragen werden.
5. **Maße und Schrift der Regions-Einblendung bleiben vorerst ein Experten-Abschnitt.** Das Zusammenlegen mit dem Farbschema-Editor ist eine Änderung an der Funktion und gehört nicht zur Neugliederung.
6. **Die vier Wege, Anzeigen zu verschieben, bleiben vorerst bestehen** und werden nur geordnet: „HUD anordnen“ unter Allgemein als Haupteinstieg, die übrigen bei ihrem Thema in den Stufen „Mehr“ und „Experte“.
7. **Config-Felder ohne GUI werden nicht aufgenommen.** Die Neugliederung fügt keine Einstellungen hinzu. Ausnahme, die sich anbietet, sobald die Struktur steht: „Runde Minimap (Wappen)“ (`map.minimapBannerRound`), weil sie im alten GUI ein gewöhnlicher Schalter war.

## Noch offen

1. **Stufen im Einzelnen.** Die Einteilung Einfach / Mehr / Experte folgt dem Typ der Einstellung (Schalter eher einfach, Maße und Zeitwerte eher Experte) und ist nicht im Spiel erprobt. Einzelne Einträge können nach einem Test wandern.
2. **Vote-Erinnerung im Code.** Geklärt am 18.11.2026: Ist das Addon Vote installiert (`ottoextra_vote`), registriert die Kern-Mod ihre eigene Erinnerung nicht (`VoteReminderFeature.register`). Der Einstellungsbildschirm zeigt dann nur die Einstellungen des Addons.

## Nächste Schritte

1. Beschriftungen und Hilfetexte überarbeiten: Fachbegriffe ersetzen (Toast, Badge, HUD, OOC), technische Auswahlwerte übersetzen (`TOP_CENTER`, `REALISTIC`, `PAGE`), fehlende Hilfetexte ergänzen (144 von 222 Einträgen der Kern-Mod haben keinen).
2. Aufbau des Bildschirms entwerfen: Seitenleiste, „Mehr Optionen“, Experten-Schalter, ausgegraute abhängige Einstellungen.
3. Umsetzung in `SettingsRegistry` und in der Addon-Schnittstelle (Andocken an Kategorien).
