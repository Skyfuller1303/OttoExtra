# Unterbildschirme

Diese Bildschirme öffnen sich über eine Schaltfläche im Einstellungsbildschirm. Sie haben jeweils ein eigenes Aussehen und eigene Speicherregeln. Die Schaltflächen „Verwerfen“ und „Zurücksetzen“ des Einstellungsbildschirms gelten hier nicht.

Angaben mit „nicht geprüft“ wurden im Quelltext nicht vollständig nachvollzogen.

## Übersicht

| Nr. | Bildschirm (Titel im GUI) | Erreichbar über | Speichert | Schreibt in |
|---|---|---|---|---|
| 1 | „Chatkanal-Farben“ | Chatkanäle › Erweitert › „Chatkanal-Farben verwalten…“ | beim Schließen | `chat.channelColors`, `chat.npcNameColor`, `chat.npcMessageColor`, `chat.rpEmoteColor`, `chat.rpOocColor`, `chat.rpEmoteItalic`, `chat.rpOocItalic` |
| 2 | „Gefolge“ | Karte › Basis › „Gefolge-Liste…“ | über eigene Schaltfläche „Speichern“ | `map.groupNameOverrides`, `map.factionColors`, `map.lehenColors` |
| 3 | „Gefolge-Farben (politische Karte)“ | Karte › Minimap › „Gefolge-Farben…“ | sofort bei jeder Eingabe | `map.groupColors` |
| 4 | „Toast-Themes“ | Regionen › Basis › „Toast-Themes“ | sofort bei jeder Änderung | `regions.theme`, `regions.customThemes` |
| 5 | „Aktivitäts-HUD platzieren“ | Tweaks › Basis › „Aktivitäts-HUD frei platzieren“ | sofort / beim Loslassen / beim Schließen | `regions.activityHudPosition`, `regions.activityHudCustomX/Y`, `regions.activityHudScale`, `regions.activityHudOffsetX/Y` |
| 6 | „Tierinfo-Toast bearbeiten“ | Tierinfo › Basis › „Toast-Layout bearbeiten…“ | sofort / beim Loslassen / beim Schließen | `animalInfo.layouts` (je Tierart) |
| 7 | „HUD anordnen“ | Karte › Basis › „HUD anordnen“, auch Befehl `/ottoextra hud` | beim Loslassen, Scrollen, Tastendruck | `map.*HudX/Y`, `regions.activityHud*`, Xaeros eigene Einstellungen, Addon-Speicher |
| 8 | „Wappen-Position“ | Karte › Minimap › „Wappen-Position (Drag&Drop)…“ | beim Loslassen, Scrollen | `map.bannerHudX/Y`, `map.nameHud*`, `map.stateHud*`, `map.factionHud*`, `map.minimapBannerSize` |
| 9 | „OttoExtra – Datenschutz und Downloads“ | Datenschutz › beim Einschalten einer Einwilligung | bei „Erlauben“ / „Ablehnen“ | `consent.*` |
| 10 | „Chatprotokoll“ (Freigabe) | Tweaks › Basis › beim Einschalten von „Chatprotokoll“ | bei „Erlauben“ / „Ablehnen“ | `tweaks.chatLog`, `tweaks.chatLogDecisionVersion`, `tweaks.chatLogAcceptedScope` |
| 11 | „RP-Personenbuch“ | RP-Namen › Basis › „Personen verwalten…“, auch Taste „RP-Personen öffnen“ | eigene Dateien | keine Felder der Config; eigene Dateien unter `config/ottoextra/rpnames/` |
| 12 | Brief-Editor | Briefe › Basis › „Brief-Editor öffnen…“, auch Taste und Befehl `/ottoextra letter open` | – | nur `letter.signature` (Auswahl als Signatur speichern) |

Die Bildschirme 7 und 8 bearbeiten teilweise dieselben Felder (Position der vier Lehen-Elemente). Die Bildschirme 5 und 7 bearbeiten beide das Aktivitäts-HUD, aber auf unterschiedliche Weise: Bildschirm 5 schreibt eine freie Position (`CUSTOM` mit `activityHudCustomX/Y`), Bildschirm 7 schreibt Anker plus Versatz (`activityHudPosition` mit `activityHudOffsetX/Y`).

## 1. Chatkanal-Farben

Farbeingaben sind Textfelder für Hex-Werte (höchstens 7 Zeichen) mit einem Farbfeld daneben. Ungültige Eingaben werden zu einem leeren Feld.

Kanäle (je eine Zeile): Sprechen, Flüstern, Murmeln, Rufen, Brüllen, Offtopic, Hilfe.

| Element | Text im GUI | Art | Feld | Standard / Hinweis |
|---|---|---|---|---|
| Spaltenkopf | „Kanal / Originalton“, „Chatname“, „Nachricht“ | Text | – | – |
| Je Kanal: Farbe des Kanalnamens | Spalte „Chatname“ | Hex-Textfeld mit Farbfeld | `chat.channelColors[kanal].labelColor` | leer = Originalfarbe des Servers |
| Je Kanal: Farbe der Nachricht | Spalte „Nachricht“ | Hex-Textfeld mit Farbfeld | `chat.channelColors[kanal].messageColor` | leer = Standardfarbe des Kanals |
| Je Kanal: Zurücksetzen | „↺“ | Schaltfläche | beide Felder der Zeile | – |
| Abschnitt | „NPC-Namen/-Nachrichten, Emotes und OOC“ | Überschrift | – | – |
| NPC-Name | „NPC-Name“ | Hex-Textfeld | `chat.npcNameColor` | `#C7A87F` |
| NPC-Nachricht | ohne eigene Beschriftung (zweite Spalte derselben Zeile) | Hex-Textfeld | `chat.npcMessageColor` | `#DFC8A7` |
| Emote-Farbe | Vorschautext „*Emotes*“ | Hex-Textfeld | `chat.rpEmoteColor` | `#C6C6C6` |
| Emote kursiv | „Kursiv: An/Aus“ | Schalter | `chat.rpEmoteItalic` | An |
| OOC-Farbe | Vorschautext „(OOC)“ | Hex-Textfeld | `chat.rpOocColor` | `#B4BEC6` |
| OOC kursiv | „Kursiv: An/Aus“ | Schalter | `chat.rpOocItalic` | Aus |
| Zeile zurücksetzen | „↺“ | Schaltfläche | Farbe und Kursiv der Zeile | – |
| Alle Kanäle zurücksetzen | „Alle auf Original“ | Schaltfläche | nur die 7 Kanalzeilen, nicht NPC/Emote/OOC | – |
| Schließen | „Fertig“ | Schaltfläche | speichert | – |

Die vier Felder Emote-Farbe, OOC-Farbe, Emote kursiv und OOC kursiv stehen zusätzlich im Einstellungsbildschirm (Chatkanäle › Basis › „RP-Chatformatierung“).

## 2. Gefolge (Gefolge-Liste)

Liste der Gefolge und Lehen aus den Kartendaten. Eine Zeile anklicken, dann rechts bearbeiten. Ohne Auswahl steht dort: „Zeile wählen zum Bearbeiten (Name/Farbe).“

| Element | Text im GUI | Art | Feld | Hinweis |
|---|---|---|---|---|
| Suche | „Suche…“ | Textfeld | – | filtert die Liste |
| Ansicht | „Hierarchie“ / „Flach“ | Umschalter | – (nicht gespeichert) | Baum nach Lehnsherr oder flache Lehen-Liste |
| Liste | Name, Rang, Lehen | auswählbare Liste | – | nur Anzeige |
| Name | „Name“ (Überschrift „Gefolge: <Name>“) | Textfeld, höchstens 48 Zeichen | `map.groupNameOverrides` | leer entfernt den eigenen Namen |
| Farbe | „Farbe“ | Hex-Textfeld mit Farbfeld und Zurücksetzen-Symbol | `map.factionColors` | leer entfernt die eigene Farbe |
| Lehen-Farbe (nur bei Zeilen mit Lehen) | „Lehen-Farbe“ | Hex-Textfeld mit Farbfeld und Zurücksetzen-Symbol | `map.lehenColors` | – |
| Auf Verband übertragen | „Auf ganzes Gefolge anwenden“ | Schaltfläche | `map.factionColors` für alle Gefolge des Verbands | speichert sofort |
| Übernehmen | „Speichern“ | Schaltfläche | – | erst damit werden Name und Farben gespeichert |
| Zurück zum gespeicherten Stand | „Verwerfen“ | Schaltfläche | – | – |
| Schließen | „Fertig“ | Schaltfläche | – | speichert nicht |

## 3. Gefolge-Farben (politische Karte)

| Element | Text im GUI | Art | Feld | Hinweis |
|---|---|---|---|---|
| Suche | „Suche…“ | Textfeld | – | filtert die Liste |
| Je Gruppe | Gruppenname mit Farbfeld | Hex-Textfeld | `map.groupColors[Gruppe]` | leer entfernt die eigene Farbe; kein Zurücksetzen je Zeile |
| Aktuelle Farben festschreiben | „Aktuelle als Standard speichern“ | Schaltfläche | `map.groupColors` | trägt alle aktuell angezeigten Farben ein, sofern noch kein Eintrag besteht |
| Schließen | „Fertig“ | Schaltfläche | – | – |

Dieser Bildschirm und die Gefolge-Liste (Nr. 2) färben beide die politische Karte, schreiben aber in verschiedene Felder (`map.groupColors` gegenüber `map.factionColors`).

## 4. Toast-Themes

Links eine Liste der Themes, rechts der Editor, dazu eine Live-Vorschau des Regions-Toasts.

| Element | Text im GUI | Art | Feld | Hinweis |
|---|---|---|---|---|
| Theme-Liste | „Themes“; Einträge „Light“, „Dark“ und eigene; aktives Theme mit „✓“ | Schaltflächen | `regions.theme` | Klick aktiviert das Theme |
| Neues Theme | „Custom+ (neu)“ | Schaltfläche | `regions.customThemes` | Kopie der hellen Farben, wird aktiviert |
| Vordefiniertes Theme gewählt | „Duplizieren“, Hinweis „Vordefiniert — zum Anpassen duplizieren.“ | Schaltfläche | `regions.customThemes` | „Light“ und „Dark“ sind nicht bearbeitbar |
| Eigenes Theme löschen | „Löschen“ | Schaltfläche | `regions.customThemes` | war es aktiv, wird „light“ aktiv |
| Schließen | „Fertig“ | Schaltfläche | – | – |

Editor für ein eigenes Theme:

| Abschnitt im GUI | Beschriftung | Art | Feld im Theme | Bereich |
|---|---|---|---|---|
| – | „Name“ | Textfeld, höchstens 24 Zeichen | `name` | nicht leer, nicht „light“/„dark“ |
| „Farben (#RRGGBB)“ | `bg`, `borderOut`, `borderTl`, `borderBr`, `title`, `region`, `hierarchy`, `hint` (englische Feldnamen, nicht übersetzt) | 8 Hex-Textfelder mit Farbfeld | gleichnamig | Hex |
| „Schriftgrößen“ | „Schrift: Basis“, „Schrift: Titel“, „Schrift: Region“, „Schrift: Hierarchie“, „Schrift: Hinweis“ | 5 Zahlenfelder | `baseTextScale`, `titleScale`, `regionScale`, `hierarchyScale`, `hintScale` | 0,1 – 3,0 |
| „Anzeige“ | „Wappen“, „„Du betrittst“-Zeile“, „Lehensname/Hierarchie“, „Tasten-Hinweis“ jeweils mit „AN/AUS“ | 4 Schalter | `showBanner`, `showEnteredTitle`, `showHierarchy`, `showHint` | – |
| „Abstände / Layout“ | „Max. Textbreite“ (50–800), „Min. Toastbreite“ (50–800), „Max. Toastbreite“ (50–1000), „Abstand oben“ (0–200), „Wappengröße“ (8–64), „Wappenabstand“ (0–32), „Innenabstand links/rechts/oben/unten“ (je 0–40) | 10 Zahlenfelder | `maxTextWidth`, `minToastWidth`, `maxToastWidth`, `screenTopMargin`, `iconSize`, `iconGap`, `paddingLeft`, `paddingRight`, `paddingTop`, `paddingBottom` | wie angegeben |

Dieselben Schriftgrößen und Abstände gibt es ein zweites Mal im Einstellungsbildschirm (Regionen › Erweitert, Karten „Badge-Layout“ und „Badge-Schrift“), dort als Felder `regions.*` außerhalb eines Themes.

## 5. Aktivitäts-HUD platzieren

Die Spielwelt bleibt sichtbar. Hinweis im GUI: „Board ziehen = freie Position · Griff unten rechts = Größe“.

| Element | Text im GUI | Art | Feld | Hinweis |
|---|---|---|---|---|
| Position | „Position: Mitte links / Oben links / Unten links / Mitte rechts / Oben rechts / Unten rechts / Frei“ | Auswahl (Klick schaltet weiter) | `regions.activityHudPosition` | Standard „Mitte links“ |
| Vorschau-Tafel | – | Ziehen | `regions.activityHudCustomX/Y` | erstes Ziehen stellt die Position auf „Frei“ |
| Griff unten rechts | – | Ziehen | `regions.activityHudScale` | 0,5 – 2,0 |
| Zurücksetzen | „Zurücksetzen“ | Schaltfläche | Position „Mitte links“, Versatz 0, freie Position Mitte, Größe 1,0 | – |
| Schließen | „Fertig“ | Schaltfläche | – | speichert |

## 6. Tierinfo-Toast bearbeiten

Hinweis im GUI: „Tier wählen • Elemente ziehen • Ecke skalieren • Toast passt sich automatisch an“. Alle Änderungen gelten nur für die gerade gewählte Tierart.

| Element | Text im GUI | Art | Feld (je Tierart) | Hinweis |
|---|---|---|---|---|
| Tierart | „Tier: Schaf / Kuh / Schwein / Streitross / Reitpferd / Ackergaul / Pferd / Packesel / Ziege / Huhn / Wolf / Sonstiges Tier“ | Auswahl (Klick schaltet weiter) | – | wählt, welches Layout bearbeitet wird |
| Tierinfo an/aus | „Tierinfo für diese Tierart: an/aus“ | Schalter | `enabled` | Standard an |
| Bestandteile | „Tier“, „Name“, „Symbol“, „Männlich/Weiblich“, „Paarung“, „Mast“, „Zeit“, „Scherbar/Melkbar“, „Sprunghöhe“, „Geschwindigkeit“, „Statuszeile“ jeweils „an/aus“ | 11 Schalter | `showModel`, `showTitle`, `showGenderIcon`, `showGenderText`, `showBreeding`, `showMast`, `showTimer`, `showExtra`, `showJumpHeight`, `showSpeed`, `showStatus` | Standard an |
| Position der Bestandteile | umrandete Kästen in der Vorschau | Ziehen | `…X`, `…Y` je Bestandteil | 0 – 500 |
| Größe der Bestandteile | Ecke des Kastens | Ziehen | `modelSize` (12–64) und `…Scale` je Bestandteil | – |
| Zurücksetzen | „Zurücksetzen“ | Schaltfläche | Layout der gewählten Tierart | – |
| Schließen | „Fertig“ | Schaltfläche | – | speichert |

Laut Tooltip der Einstellung „Toast-Stil“ gilt dieser Editor nur für den Stil „Eigenes Layout“ (`animalInfo.toastStyle = custom`): „Eigenes Layout: frei anpassbarer Toast; der Layout-Editor gilt nur für diesen Stil.“ Im Editor selbst steht dieser Hinweis nicht. Der Zusammenhang wurde im Quelltext nicht geprüft.

## 7. HUD anordnen

Hinweis im GUI: „Ziehen – Mausrad/Ecke: Größe – Pfeiltasten: 1 px, Umschalt: 10 px – Alt: kein Einrasten – Esc: zurück“. Elemente rasten an Rändern, an der Mitte und aneinander ein.

| Element | Text im GUI | Vorhanden, wenn | Größe änderbar | Speicherort |
|---|---|---|---|---|
| Minimap | „Minimap“ | Xaeros Minimap installiert | nein | Xaeros eigene Einstellungen, nicht die OttoExtra-Config |
| Lehen-Wappen | „Lehen-Wappen“ | Xaeros Minimap installiert | nicht geprüft | `map.bannerHudX/Y` |
| Lehen-Name | „Lehen-Name“ | Xaeros Minimap installiert | nicht geprüft | `map.nameHudX/Y` |
| Lehen-Stand | „Lehen-Stand“ | Xaeros Minimap installiert | nicht geprüft | `map.stateHudX/Y` |
| Lehen-Fraktion | „Lehen-Fraktion“ | Xaeros Minimap installiert | nicht geprüft | `map.factionHudX/Y` |
| Aktivitäts-HUD | „Aktivitäts-HUD“ | immer | ja | `regions.activityHudPosition`, `regions.activityHudOffsetX/Y`, `regions.activityHudScale` |
| Addon-Elemente | je nach Addon (zum Beispiel Trupp-Anzeige von TacticalFactions) | Addon installiert | je nach Addon | Speicher des Addons |

Weitere Bedienelemente: „Zurücksetzen“ (setzt nur das gerade gewählte Element zurück) und „Fertig“.

## 8. Wappen-Position

Benötigt Xaeros Minimap; ohne sie erscheint im Einstellungsbildschirm die Meldung „Für diesen Editor wird Xaeros Minimap benötigt.“ Hinweise im GUI: „Ziehen: Position · Ecke: Breite/Höhe“ und „Mausrad: Schriftgröße · Loslassen speichert“. Ohne erkanntes Lehen: „Kein Lehen erkannt — betritt ein Lehen, um den Block zu sehen.“

| Element | Art | Feld | Bereich |
|---|---|---|---|
| Wappen, Name, Stand, Fraktion | Ziehen | `map.bannerHudX/Y`, `map.nameHudX/Y`, `map.stateHudX/Y`, `map.factionHudX/Y` | – |
| Ecke am Wappen | Ziehen | `map.minimapBannerSize` | 8 – 64 |
| Ecke an Name, Stand, Fraktion | Ziehen | `map.nameHudWidth/Height`, `map.stateHudWidth/Height`, `map.factionHudWidth/Height` | Breite 20 – 400 |
| Mausrad auf Name, Stand, Fraktion | Scrollen, Schritt 0,05 | `map.nameHudScale`, `map.stateHudScale`, `map.factionHudScale` | – |
| „Zurücksetzen“ | Schaltfläche | alle Positionen | – |
| „Fertig“ | Schaltfläche | – | – |

Breite, Höhe und Skalierung von Name, Stand und Fraktion stehen zusätzlich als Zahlenfelder im Einstellungsbildschirm (Karte › Erweitert › „HUD-Feinjustierung“).

## 9. Datenschutz-Freigabe („OttoExtra – Datenschutz und Downloads“)

Der Bildschirm erscheint nur beim Einschalten einer Einwilligung. Ausschalten wirkt sofort und ohne Rückfrage.

| Element | Text im GUI | Art | Wirkung |
|---|---|---|---|
| Erklärtext | Überschrift und Text je Einwilligung: „Automatische Resourcepack-Downloads“, „Externe Bilder“, „Versionsprüfung“, „Beobachtete RP-Namen teilen“, „Optionale Online-Dienste“ | scrollbarer Text | zeigt auch aktuelle Werte (Quelle und Größe des Resourcepacks) |
| Ablehnen | „Ablehnen“ | Schaltfläche | Einwilligung bleibt aus, speichert |
| Erlauben | „Erlauben“ | Schaltfläche | Einwilligung an, speichert; bei „Beobachtete RP-Namen teilen“ gesperrt, solange die Funktion nicht freigegeben ist |

Neben dem jeweiligen Schalter werden `consent.acceptedScopes`, `consent.decidedAt` und `consent.policyVersion` geschrieben.

## 10. Chatprotokoll-Freigabe

Erscheint beim Einschalten von „Chatprotokoll“ (Tweaks › Basis). Zeigt den Erklärtext mit dem gewählten Umfang („nur Ottonien“ oder „alle Server und Einzelspieler“) und die Schaltflächen „Ablehnen“ und „Erlauben“. Der Umfang selbst wird im Einstellungsbildschirm gewählt („Chatlog-Umfang (Änderung braucht neue Freigabe)“).

## 11. RP-Personenbuch

Eigener großer Bildschirm zur Verwaltung bekannter Personen, Kategorien und Titel. Er ändert keine Felder der OttoExtra-Config, sondern eigene Dateien. Er liest die Einstellung `rpnames.globalRpNameColor`. Die einzelnen Bedienelemente wurden nicht aufgenommen.

## 12. Brief-Editor

Eigener Bildschirm zum Schreiben von Briefen. Einzige Einstellung, die er ändert: `letter.signature` (markierten Text als Signatur speichern, Meldung „Auswahl als Signatur gespeichert.“). Er liest die Einstellungen `letter.sendMode`, `letter.letterCommand`, `letter.formattingEnabled`, `letter.formattingSidebarVisible`, `letter.formattingConvertAmpersandOnPaste`, `letter.previewEnabled` und `letter.openRecipientAfterWrite`, hat dafür aber keine eigenen Schalter.

## Altes GUI (nicht mehr erreichbar)

Im Quelltext gibt es noch einen älteren Einstellungsbildschirm im Pergament-Stil (`OttoExtraConfigScreen`, Titel „OttoExtra Einstellungen“) mit einer Seitenleiste für sieben Module und je einem Unterbildschirm „Erweitert …“ (`OttoExtraAdvancedScreen`). Im Quelltext wurde keine Stelle gefunden, die ihn öffnet; der Menüknopf und ModMenu öffnen den neuen Bildschirm.

Das alte GUI enthält Einstellungen, die im neuen Bildschirm fehlen:

| Feld | Beschriftung im alten GUI | Art / Bereich |
|---|---|---|
| `regions.theme` | „Theme“ (hell/dunkel) | Auswahl; im neuen GUI nur über „Toast-Themes“ |
| `map.minimapBannerRound` | „Runde Minimap (Wappen)“ | Schalter |
| `resourcepack.maxSizeBytes` | „Max. Größe (MB)“ | 1 – 512 |
| `map.labelScale` | „Label-Schriftgröße (Faktor)“ | 0,3 – 3 |
| `map.labelZoomA`, `map.labelZoomB` | „Label-Breakpoint A (Zoom)“, „… B“ | 0,005 – 8 |
| `map.factionScaleA`, `map.factionScaleB` | „Gefolgename Größe bei A“, „… bei B“ | 0,3 – 3 |
| `map.lehenScaleA`, `map.lehenScaleB` | „Lehensname Größe bei A“, „… bei B“ | 0,3 – 3 |
| `map.bannerSizeA`, `map.bannerSizeB` | „Wappen-Größe bei A (px)“, „… bei B (px)“ | 4 – 64, 4 – 96 |
| `map.borderWidthPx` | „Linienbreite (px)“ | 1 – 4 |
| `map.dashLengthPx`, `map.dashGapPx` | „Strichlänge (px)“, „Strichlücke (px)“ | 2 – 64, 0 – 64 |
| `map.minimapBannerOffsetX`, `map.minimapBannerOffsetY` | „Minimap-Wappen Versatz X“, „… Y“ | −200 – 200 |
| `map.minimapBannerOffsetXRound`, `map.minimapBannerOffsetYRound` | „Wappen Versatz X (rund)“, „… Y (rund)“ | −200 – 200 |

Hinweis des alten GUI unter dem Fenster: „Modul-Schalter greifen nach einem Neustart.“ Der neue Bildschirm zeigt diesen Hinweis nicht.
