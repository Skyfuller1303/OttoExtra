# Einstellungen außerhalb des Einstellungsbildschirms

Stellen, an denen dauerhaft gespeicherte Einstellungen geändert werden, ohne den Einstellungsbildschirm zu öffnen. Angaben mit „nicht geprüft“ wurden im Quelltext nicht vollständig nachvollzogen.

## Befehle

Alle Befehle beginnen mit `/ottoextra`.

| Befehl | Wirkung | Wird gespeichert | Auch im Einstellungsbildschirm |
|---|---|---|---|
| `/ottoextra hud` | öffnet „HUD anordnen“ | – | ja (Karte › Basis › „HUD anordnen“) |
| `/ottoextra activityhud rate [Sekunden]` | setzt die Abfragerate des Aktivitäts-HUDs (`regions.activityHudRefreshSeconds`, 5 – 3600); ohne Zahl zeigt er den aktuellen Wert | ja | nein |
| `/ottoextra tweaks lowhealth on\|off` | schaltet „Verwundung & Adrenalin“ (`tweaks.lowHealth.enabled`) | ja | ja |
| `/ottoextra letter open` | öffnet den Brief-Editor | – | ja |
| `/ottoextra letter weiter\|abbrechen\|send\|announce\|close` | steuert den Briefversand | – | nein |
| `/ottoextra letter calibrate …` | Kalibrierwerkzeug; ändert zeitweise `letter.sendMode` | ja (wird danach zurückgestellt) | nein |
| `/ottoextra regions refresh\|status` | lädt Kartendaten neu bzw. zeigt den Status | – | „Kartendaten neu laden“ ja |
| `/ottoextra regions banner [Stil [Region]]` | zeigt einen Banner-Stil zur Probe | nein | Stil-Auswahl ja |
| `/ottoextra banderole set\|reset\|show\|replay\|hide\|export` | Entwicklerwerkzeug für den Banderolen-Stil | nicht geprüft | nein |
| `/ottoextra debug [status\|on\|off]` | Debug-Ausgaben | nein | nein |
| `/ottoextra verify [status\|unlink]`, `/ottoextra account` | Kontoverknüpfung | ja (eigene Datei) | nein |

## Tasten

Die Tasten werden in Minecrafts Steuerungs-Einstellungen belegt, nicht im OttoExtra-Einstellungsbildschirm. Im Quelltext sind sie der Kategorie „Verschiedenes“ zugeordnet; ein Text für eine eigene Kategorie „OttoExtra“ existiert in der Sprachdatei, seine Verwendung wurde nicht geprüft.

| Name in der Steuerung | Standardtaste | Ändert dauerhaft |
|---|---|---|
| „Kanal: Sprechen“, „Kanal: Flüstern“, „Kanal: Rufen“, „Kanal: Offtopic“, „Kanal: Hilfe“ | nicht belegt | nicht geprüft |
| „Karten-Overlay umschalten“ | K (`map.toggleKey`) | – |
| „Namensschild-Modus wechseln“ | N (`nametags.toggleKey`) | `nametags.mode` |
| „Lehen-Menü öffnen“ | L (`regions.menuKey`) | – |
| „Aktivitäts-HUD ein-/ausblenden“ | nicht belegt | `regions.activityHudEnabled` |
| „Tier-Glow nach Geschlecht umschalten“ | F8 | `animalInfo.genderGlowEnabled` |
| „Tier benennen“ | B | – |
| „Tierübersicht öffnen“ | J | – |
| „Brief-Editor öffnen“ | nicht belegt | – |
| „RP-Personen öffnen“ | nicht geprüft | – |
| „Untersuchen“ | Alt links | – |
| „Fachwerk-Automatik umschalten“ | nicht belegt | `tweaks.litematica.fachwerkAutoTune` |
| „Ottonien-Vote-Seiten öffnen“ | Enter | – |

Die Config enthält außerdem die Felder `chat.voiceKey` (V), `chat.helpKey` (H) und `chat.offtopicKey` (O); ihre Verwendung wurde nicht geprüft.

## Schaltflächen auf der Karte

| Ort | Element | Feld | Wirkung |
|---|---|---|---|
| Weltkarte | Umschalter „Politische Karte umschalten“ | `map.politicalFill` | wie der Schalter im Einstellungsbildschirm; speichert sofort |
| Weltkarte | Pfeil-Schaltflächen „Gemalte Karte verschieben (Shift = ×5)“ | `map.paintedMapOffsetX`, `map.paintedMapOffsetZ` | Schritt 10, mit Umschalt 50; die mittlere Schaltfläche setzt auf 0; speichert sofort. Der Schalter „Kalibrier-Pfeile (Karte verschieben)“ dazu steht im Einstellungsbildschirm (Karte › Erweitert) |

## Erststart-Assistent

Beim ersten Start fragt die Mod Einwilligungen und eine Rollenspiel-Voreinstellung ab.

| Stelle | Geschriebene Felder |
|---|---|
| Willkommensbildschirm | `consent.onboardingVersion` und die Einwilligungen |
| Rollenspiel-Voreinstellung | `rpnames.enabled`, `rpnames.showInSprechen`, `showInReden`, `showInRufen`, `showInBruellen`, `showInFluestern`, `showInMurmeln` (an), `showInHilfe`, `showInOfftopic`, `showInOoc`, `showInAllChannels` (aus), `nametags.enabled`, `rpnames.showUnknownAsUnknown` |

Die Bedienelemente dieser Bildschirme wurden nicht einzeln aufgenommen.

## Nicht untersuchte Bildschirme

Folgende Bildschirme wurden nicht auf Einstellungen durchgesehen: Update-Hinweis, Brief-Kalibrierung, Tierübersicht und Tier benennen, Kontoübersicht, Kennenlern-Dialog, Regions-Info, Vote-Freigabe sowie alle Bildschirme der Addons (zum Beispiel Politik-Buch, Skript-Bibliothek).
