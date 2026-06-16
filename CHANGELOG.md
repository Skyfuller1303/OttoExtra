# Changelog

Alle nennenswerten Änderungen an OttoExtra. Format angelehnt an
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/); Versionierung nach
[SemVer](https://semver.org/lang/de/).

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

[0.1.0]: https://example.com/ottoextra/releases/0.1.0
