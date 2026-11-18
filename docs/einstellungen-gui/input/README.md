# Einstellungen von OttoExtra – Bestandsaufnahme

Diese Dateien halten fest, welche Einstellungen OttoExtra heute hat, wie sie beschriftet sind und wo sie im GUI stehen. Sie sind die Grundlage für eine Neuordnung des Einstellungsbildschirms. Sie enthalten keinen Entwurf für das neue GUI.

- Stand: 18.11.2026, Arbeitsverzeichnis `OttoExtra` (Branch `fix/worldmap-api-banners` mit nicht eingecheckten Änderungen, `mod_version=0.17.1`)
- Sprache der Beschriftungen: Deutsch (`de_de.json`)
- Quellen: `SettingsRegistry.java`, `OttoExtraSettingsScreen.java`, `OttoExtraConfig.java`, `HudLayoutSettings.java`, die Addon-Klassen der Nachbarverzeichnisse `OttoExtra-*` und die jeweiligen Sprachdateien
- Die Moduldateien 01–10 und 20–27 sowie Datei 32 wurden per Skript aus dem Quelltext erzeugt, die übrigen Dateien von Hand aus der Durchsicht des Quelltexts geschrieben. Im Spiel selbst wurde nichts nachgeprüft.

## Dateien

| Datei | Inhalt |
|---|---|
| `01-datenschutz.md` bis `10-tweaks.md` | je ein Reiter der Kern-Mod: Tabs, Karten, jede Einstellung mit Beschriftung, Bedienelement, Standard, Bereich, Hilfetext und Config-Schlüssel |
| `11-backups.md` | der fest eingebaute Reiter „Backups“ |
| `20-addon-…md` bis `27-addon-…md` | Reiter und Karten, die Addons hinzufügen |
| `30-unterbildschirme.md` | Bildschirme hinter Schaltflächen (Farben, Themes, HUD-Editoren, Freigaben) und das alte, nicht mehr erreichbare GUI |
| `31-befehle-tasten-sonstiges.md` | Befehle, Tasten, Schaltflächen auf der Karte und der Erststart-Assistent, soweit sie Einstellungen ändern |
| `32-config-felder-ohne-gui.md` | Felder der Config-Datei, die im Einstellungsbildschirm fehlen |

## So erreicht man die Einstellungen

- über den OttoExtra-Menüknopf (`OttoExtraMenuButton`)
- über ModMenu (Zahnrad bei OttoExtra)

Beide öffnen denselben Bildschirm mit dem Titel „OttoExtra — Einstellungen“. Die Einstellungen liegen in `config/ottoextra/ottoextra.json`; Addons haben eigene Dateien.

## Aufbau des Bildschirms (Ist-Zustand)

Von oben nach unten:

1. Titel „OttoExtra — Einstellungen“
2. Reiterleiste: ein Reiter je Modul, danach die Reiter der Addons, zuletzt „Backups“. Passt die Leiste nicht in die Fensterbreite, wird sie durch ein Aufklappmenü „<Modul> ▼“ ersetzt.
3. Suchfeld „Suchen…“. Die Suche prüft Beschriftung, Config-Schlüssel, Kartentitel und Modulname. Treffer erscheinen als flache Liste „Modul › Beschriftung“ ohne Karten und Beschreibungen. Ohne Treffer: „Keine Treffer.“ Hilfetexte und Kartenbeschreibungen werden nicht durchsucht.
4. Tab-Leiste des Moduls („Basis“, „Erweitert“, bei der Karte zusätzlich „Minimap“). Sie erscheint nur bei Modulen mit mehr als einem Tab.
5. Karten: gelber Titel, graue Beschreibung, darunter die Einstellungszeilen. Jede Karte hat einen Rahmen mit farbigem Streifen links.
6. Fußzeile: „Verwerfen“ und „Speichern & Beenden“.

Eine Einstellungszeile besteht aus:

- Beschriftung links, dahinter „(?)“, wenn es einen Hilfetext gibt (erscheint beim Überfahren mit der Maus)
- Bedienelement rechts (100 Pixel breit)
- Schaltfläche „Zurücksetzen“ ganz rechts, bei jeder Zeile außer bei Schaltflächen

Bedienelemente:

| Art | Aussehen | Anzahl in der Kern-Mod |
|---|---|---|
| Schalter | Schaltfläche mit grünem „An“ oder rotem „Aus“ | 111 |
| Zahlenfeld | Textfeld; ungültige Eingabe wird rot und nicht übernommen, zu große oder zu kleine Werte werden auf den Bereich begrenzt | 70 |
| Farbfeld | Textfeld für `#RRGGBB`, ohne Farbvorschau und ohne Farbwähler | 9 |
| Textfeld | freier Text, höchstens 256 Zeichen | 5 |
| Befehlsfeld | Textfeld, zeigt den Befehl mit führendem `/` | 3 |
| Auswahl | Schaltfläche, jeder Klick schaltet zum nächsten Wert | 8 |
| Schieberegler | Regler mit Prozentanzeige | 2 |
| Schaltfläche | breite Schaltfläche, öffnet einen Unterbildschirm oder löst eine Aktion aus | 14 |

Speichern:

- „Speichern & Beenden“, die Esc-Taste und Strg+S speichern und schließen.
- „Verwerfen“ stellt den Stand vom Öffnen des Bildschirms wieder her und schließt.
- Bei ungespeicherten Änderungen steht unten: „Ungespeicherte Änderungen — Strg+S speichert“.
- Einwilligungen (Datenschutz, Chatprotokoll) und fast alle Unterbildschirme speichern sofort und unabhängig von „Verwerfen“ (siehe `30-unterbildschirme.md`).
- Ohne vorhandenes Backup steht oben in Rot: „Kein Backup! Bitte unter Backups eines erstellen.“

## Umfang

### Kern-Mod

| Reiter | Datei | Tabs | Karten | Einträge |
|---|---|---|---|---|
| Datenschutz | `01-datenschutz.md` | 1 | 1 | 5 |
| Server-Resourcepack | `02-server-resourcepack.md` | 2 | 2 | 7 |
| Chatkanäle | `03-chatkanaele.md` | 2 | 6 | 16 |
| Regionen | `04-regionen.md` | 2 | 3 | 31 |
| Namensschilder | `05-namensschilder.md` | 2 | 3 | 10 |
| Karte | `06-karte.md` | 3 | 6 | 49 |
| RP-Namen | `07-rp-namen.md` | 1 | 7 | 28 |
| Briefe | `08-briefe.md` | 2 | 4 | 20 |
| Tierinfo | `09-tierinfo.md` | 2 | 7 | 20 |
| Tweaks | `10-tweaks.md` | 2 | 9 | 36 |
| Backups | `11-backups.md` | – | – | 3 Schaltflächen und die Backup-Liste |
| Summe (ohne Backups) | | 19 | 48 | 222 |

### Addons

Addons sind eigene Mods. Ihr Reiter erscheint nur, wenn das Addon installiert ist. Die Reihenfolge der Addon-Reiter hängt von der Ladereihenfolge ab.

| Addon | Reiter | Datei | Tabs | Karten | Einträge |
|---|---|---|---|---|---|
| OttoExtra-AnimalBreeding | kein eigener; Karte im Reiter „Tierinfo“ | `20-addon-animalbreeding.md` | – | 1 | 1 |
| OttoExtra-Discord | Discord | `21-addon-discord.md` | 1 | 2 | 6 |
| OttoExtra-Farmer | Farmer | `22-addon-farmer.md` | 1 | 1 | 4 |
| OttoExtra-Resources | Ressourcen | `23-addon-resources.md` | 1 | 2 | 5 |
| OttoExtra-Skript | OttoSkript | `24-addon-skript.md` | 1 | 2 | 4 |
| OttoExtra-TacticalFactions | Tactical Factions | `25-addon-tacticalfactions.md` | 1 | 3 | 10 |
| OttoExtra-TranslateUtils | TranslateUtils | `26-addon-translateutils.md` | 2 | 3 | 23 |
| OttoExtra-Vote | Vote Reminder | `27-addon-vote.md` | 1 | 1 | 5 |
| Summe | | | | 15 | 58 |

Mit allen Addons hat die Reiterleiste 18 Reiter (10 Kern, 7 Addon, Backups).

### Config-Datei

Von den Feldern der Kern-Config haben 209 einen Eintrag im Einstellungsbildschirm und 151 keinen (siehe `32-config-felder-ohne-gui.md`).

## Alle Karten im Überblick

### Kern-Mod

| Reiter | Tab | Karte | Einträge |
|---|---|---|---|
| Datenschutz | Basis | Einwilligungen | 5 |
| Server-Resourcepack | Basis | Automatik | 3 |
| Server-Resourcepack | Erweitert | Quelle & Netzwerk | 4 |
| Chatkanäle | Basis | Kanäle | 3 |
| Chatkanäle | Basis | Offtopic-Warnung | 2 |
| Chatkanäle | Basis | Lange Nachrichten | 4 |
| Chatkanäle | Basis | RP-Chatformatierung | 5 |
| Chatkanäle | Erweitert | Chatkanal-Farben | 1 |
| Chatkanäle | Erweitert | Lange Nachrichten | 1 |
| Regionen | Basis | Regions-Anzeige | 13 |
| Regionen | Erweitert | Badge-Layout | 13 |
| Regionen | Erweitert | Badge-Schrift | 5 |
| Namensschilder | Basis | Sichtbarkeit | 2 |
| Namensschilder | Basis | Zeilen | 3 |
| Namensschilder | Erweitert | Größe & Farbe | 5 |
| Karte | Basis | Weltkarte | 13 |
| Karte | Basis | HUD-Layout | 1 |
| Karte | Minimap | Minimap-Overlays | 4 |
| Karte | Minimap | Wappen-HUD | 7 |
| Karte | Erweitert | Darstellung | 12 |
| Karte | Erweitert | HUD-Feinjustierung | 12 |
| RP-Namen | Basis | Personenbuch | 1 |
| RP-Namen | Basis | Chat | 5 |
| RP-Namen | Basis | Namensfarben | 3 |
| RP-Namen | Basis | Schnellzugriff | 2 |
| RP-Namen | Basis | RP-Untersuchungsmodus | 9 |
| RP-Namen | Basis | Kennenlern-Marker (3D) | 4 |
| RP-Namen | Basis | Tabliste | 4 |
| Briefe | Basis | Editor | 6 |
| Briefe | Basis | Formatierung | 5 |
| Briefe | Erweitert | Serverbefehle | 3 |
| Briefe | Erweitert | Layout & Timing | 6 |
| Tierinfo | Basis | Tierinfo beim Anvisieren | 3 |
| Tierinfo | Basis | Toast-Stil | 1 |
| Tierinfo | Basis | Tier-Nametag | 3 |
| Tierinfo | Basis | Tier-Glow | 3 |
| Tierinfo | Erweitert | Lokaler Tiercache | 2 |
| Tierinfo | Erweitert | Tierarten in der Übersicht | 5 |
| Tierinfo | Erweitert | Abfrageverhalten | 3 |
| Tweaks | Basis | Chatprotokoll | 2 |
| Tweaks | Basis | Kampfmodus | 1 |
| Tweaks | Basis | Herstellungsanzeige | 2 |
| Tweaks | Basis | Aktivitäts-HUD | 7 |
| Tweaks | Basis | Werkzeugschutz | 3 |
| Tweaks | Basis | Vote-Reminder | 5 |
| Tweaks | Basis | Litematica-Integration | 2 |
| Tweaks | Basis | Verwundung & Adrenalin | 12 |
| Tweaks | Erweitert | Aktivitäts-HUD verschieben | 2 |

### Addons

| Reiter | Tab | Karte | Einträge |
|---|---|---|---|
| Tierinfo (AnimalBreeding) | Basis | Züchtermodus | 1 |
| Discord | Basis | Discord-Brücke | 4 |
| Discord | Basis | Endpunkt | 2 |
| Farmer | Basis | Farmer-Assistenten | 4 |
| Ressourcen | Basis | Ressourcen-Hinweise | 4 |
| Ressourcen | Basis | Minimap | 1 |
| OttoSkript | Basis | Skripte und Vorschau | 3 |
| OttoSkript | Basis | OttoSkript aktiviert | 1 |
| Tactical Factions | Basis | Kampfmodus | 7 |
| Tactical Factions | Basis | Politische Farben | 2 |
| Tactical Factions | Basis | Tabliste | 1 |
| TranslateUtils | Basis | Übersetzung | 15 |
| TranslateUtils | Basis | Geschützte Inhalte | 4 |
| TranslateUtils | Erweitert | Übersetzungs-API | 4 |
| Vote Reminder | Basis | Tägliche Erinnerung | 5 |

## Beobachtungen zum Ist-Zustand

Reine Feststellungen aus dem Quelltext, als Ausgangspunkt für die Neuordnung.

Gliederung:

1. Die Reiter folgen den technischen Modulen der Mod (Chat, Regionen, Karte, RP-Namen …), nicht den Fragen eines Spielers.
2. Der Reiter „Tweaks“ sammelt acht voneinander unabhängige Funktionen: Chatprotokoll, Kampfmodus, Herstellungsanzeige, Aktivitäts-HUD, Werkzeugschutz, Vote-Reminder, Litematica, Verwundung.
3. Das Aktivitäts-HUD steht unter „Tweaks“, seine Config-Schlüssel heißen aber `regions.*`. Seine Einstellungen verteilen sich auf zwei Karten („Aktivitäts-HUD“ in Basis, „Aktivitäts-HUD verschieben“ in Erweitert) und zwei Unterbildschirme („Aktivitäts-HUD platzieren“, „HUD anordnen“).
4. HUD-Positionen lassen sich an vier Stellen einstellen: „HUD anordnen“, „Wappen-Position“, „Aktivitäts-HUD platzieren“ und als Zahlenfelder unter Karte › Erweitert › „HUD-Feinjustierung“.
5. Farben der politischen Karte lassen sich an zwei Stellen einstellen („Gefolge-Liste…“ unter Basis, „Gefolge-Farben…“ unter Minimap), die in verschiedene Felder schreiben. Beide Schaltflächen betreffen die Weltkarte; eine davon steht in der Karte „Wappen-HUD“ des Tabs „Minimap“.
6. Der Schalter „Öffnungsanimation der Weltkarte“ (`map.worldMapOpenAnimation`) steht im Tab „Minimap“.
7. RP-Namen hat 28 Einträge in einem einzigen Tab; Tweaks › Basis hat 34 Einträge in 8 Karten; die Karte „Verwundung & Adrenalin“ allein hat 12 Einträge.
8. Die Tabs heißen überall „Basis“ und „Erweitert“. In „Basis“ stehen auch Feinwerte in Millisekunden, Sekunden und Grad (zum Beispiel beim Untersuchungsmodus, beim Vote-Reminder und bei der Verwundung).
9. In der Chat-Rubrik gibt es zwei Karten mit demselben Titel „Lange Nachrichten“ (Basis und Erweitert). Die Schaltfläche „Vorschau anzeigen“ gibt es bei Regionen zweimal.
10. Schriftgrößen und Abstände des Regions-Toasts gibt es doppelt: als Felder unter Regionen › Erweitert und je eigenem Theme im Unterbildschirm „Toast-Themes“.
11. Den Vote-Reminder gibt es zweimal mit denselben fünf Einstellungen: als Karte unter Tweaks (`tweaks.vote.*`) und als Addon-Reiter „Vote Reminder“ (`vote.*`). Ob beide gleichzeitig erscheinen, wurde nicht geprüft.

Beschriftung und Hilfe:

12. Die Beschreibung eines Moduls ist im Code hinterlegt, wird im Bildschirm aber nirgends angezeigt.
13. Von 222 Einträgen der Kern-Mod haben 78 einen Hilfetext, 144 keinen.
14. Auswahlfelder zeigen technische Werte statt Text: `TOP_CENTER`, `REALISTIC`, `HIDE_ALL`, `LEGACY`/`PAGE`/`V2`, `OTTONIEN`/`ALL`, `classic`/`zonentitel`/`banderole`/`plakette`/`minimal`. Übersetzt werden nur die Position des Aktivitäts-HUDs und der Toast-Stil der Tierinfo. Für die Namensschild-Modi gibt es deutsche Texte in der Sprachdatei („Normal“, „Realistisch (Sichtlinie)“, „Alle ausblenden“), die dieser Bildschirm nicht verwendet.
15. Die Schaltfläche „HUD anordnen“ (Karte › Basis) hat keinen Text in der Sprachdatei: Der Schlüssel `ottoextra.hudlayout.open.button` fehlt in `de_de.json` und `en_us.json`. Auf der Schaltfläche steht deshalb voraussichtlich der rohe Schlüssel.
16. Fachbegriffe ohne Erklärung in Beschriftungen: Toast, Badge, HUD, OOC, Manifest, Asset, Timeout, Overlay.
17. Im Unterbildschirm „Toast-Themes“ sind die acht Farbfelder mit englischen Feldnamen beschriftet (`bg`, `borderOut`, `borderTl` …).

Bedienung:

18. Farben werden im Einstellungsbildschirm als Hex-Text getippt; es gibt dort weder Farbvorschau noch Farbwähler. Die Unterbildschirme zeigen ein Farbfeld neben der Eingabe.
19. Zahlen werden frei getippt; den erlaubten Bereich sieht man nur, wenn der Hilfetext ihn nennt.
20. Abhängige Einstellungen bleiben bedienbar, wenn der Hauptschalter aus ist (zum Beispiel „Anzahl !“ bei ausgeschalteter Offtopic-Warnung). Der Bildschirm kennt keine Abhängigkeiten zwischen Einträgen.
21. Jede Zeile hat eine eigene Schaltfläche „Zurücksetzen“; ein Zurücksetzen je Karte oder je Modul gibt es nicht.
22. Das Speicherverhalten ist uneinheitlich: Der Einstellungsbildschirm speichert beim Schließen und kann verwerfen; die Unterbildschirme speichern je nach Bildschirm sofort, beim Loslassen, beim Schließen oder über eine eigene Schaltfläche „Speichern“.
23. Ob eine Einstellung erst nach einem Neustart wirkt, zeigt der Bildschirm nicht an. Das alte GUI hatte dazu den Hinweis „Modul-Schalter greifen nach einem Neustart.“

Nicht erreichbar:

24. 45 Config-Felder lassen sich nur direkt in der Datei ändern, 18 weitere nur im alten, nicht mehr erreichbaren GUI (siehe `32-config-felder-ohne-gui.md`).
