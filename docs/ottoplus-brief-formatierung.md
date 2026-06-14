# OttoPlus – Brief-Formatierung (Farben & Stile)

Wie OttoPlus (Mod-ID `ottotalk`, Version 1.6.2) das Schreiben **bunter / formatierter**
Briefe gelöst hat und wie daraus die `/letter`-Befehle entstehen.

> Quelle: `Mods/extracted/decompiled/ottoplus-1.6.2/.../gui/LetterScreen.java`
> (dekompiliert mit CFR). Alle Zeilenangaben beziehen sich auf diese Datei.
> Ergänzend: `PostRecipientScreen.java`, `OttoTalkClient.java`.

---

## 1. Kurzfassung – was passiert beim bunten Schreiben?

OttoPlus hatte **keinen Rich-Text-Editor**. Stattdessen arbeitet das ganze System mit
klassischen **Minecraft-Formatierungscodes** (`§`-Codes wie `§c`, `§l`). Der Trick:

1. **Im Editor** stehen die Codes als sichtbarer `§`-Code mitten im Text (WYSIWYG –
   der Text wird live farbig/fett gerendert, weil das Eingabefeld `§` interpretiert).
2. **Eine Formatierungsleiste** unter dem Brief fügt diese Codes per Klick an der
   Cursor-Position ein – man tippt sie nicht selbst.
3. **Beim Senden** werden alle `§` in `&` umgewandelt und der komplette Text inkl.
   `&c`/`&l`-Codes als **ein Argument** an den Server-Befehl `letter <text>` gehängt.
4. **Der Server** (nicht die Mod) parst die `&`-Codes und baut daraus die farbige
   Discord-Verkündung.

Die Mod selbst „kann" also gar kein Rich-Text – sie schiebt nur Markup-Strings
(`&c…`) durch den `/letter`-Command. Farbe/Fettung entsteht erst serverseitig.

---

## 2. Die Formatierungsleiste

Definiert ganz oben in `LetterScreen.java` (Zeilen 48–51):

```java
// 16 Farben = Vanilla-Farbcodes 0–9, a–f
FMT_COLOR_CODES = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
FMT_COLOR_RGBS  = {0, 0xAA, 0x2A00, 0x2AAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                   0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF,
                   0xFFFF55, 0xFFFFFF};

// 5 Stil-Buttons
FMT_STYLE_CODES  = {'l','o','n','m','r'};   // Code
FMT_STYLE_LABELS = {"B","I","U","S","R"};   // Button-Beschriftung
```

| Button | Code | Wirkung            |
|--------|------|--------------------|
| **B**  | `§l` | Fett (bold)        |
| **I**  | `§o` | Kursiv (italic)    |
| **U**  | `§n` | Unterstrichen      |
| **S**  | `§m` | Durchgestrichen    |
| **R**  | `§r` | Reset (Format aus) |

Dazu 16 Farbflächen (Code `§0`…`§f`). Die `FMT_COLOR_RGBS` dienen nur dazu, das
Farbkästchen in der UI in der richtigen Farbe zu zeichnen – es ist exakt die
Vanilla-Farbpalette.

> Hinweis: `§k` (obfuscated/„magic") hat **keinen** Button, wird aber beim Senden
> als gültiger Code akzeptiert (siehe Regex in §5).

### Rendering der Leiste
`renderFormattingBar()` (Z. 453–481):
- Farbreihe: 16 Kästchen à 6 px, Abstand 7 px, ab `fmtColorsX`.
- Stilreihe: 5 Buttons à 8 px, Abstand 10 px, ab `fmtStylesX`.
- Auf **gesperrten/vollen Seiten** (`lastLocked >= 11`) wird die Leiste
  ausgegraut (`barAlpha` 85 statt 204) und Klicks ignoriert.

### Klick → Code einfügen
`method_25402()` (Maus, Z. 595–611) erkennt den Treffer und ruft
`insertFormattingCode("§" + code)` auf.

`insertFormattingCode()` (Z. 483–505) fügt den `§`-Code an der **Cursor-Position**
der aktuell fokussierten Zeile ein (max. 200 Zeichen/Zeile) und schiebt den Cursor
hinter den Code:

```java
String newText = cur.substring(0, pos) + code + cur.substring(pos);
if (newText.length() <= 200) {
    field.method_1852(newText);
    field.method_1883(pos + code.length(), false);
}
```

---

## 3. Live-Vorschau im Editor: `&` ↔ `§`

Das Brief-Modell speichert die Codes intern mit **`&`** (z. B. `&cHallo`), damit sie
robust persistiert/verschickt werden können. Im Eingabefeld werden sie aber als
echtes **`§`** angezeigt, damit Minecraft sie sofort farbig rendert.

`ampToSection()` (Z. 791–806) macht beim Laden in die Textfelder aus `&x` → `§x`
(nur für gültige Codes `0-9 a-f k l m n o r`):

```java
field.method_1852(LetterScreen.ampToSection(page.get(lineIdx)));
```

→ Deshalb sieht man beim Tippen den Text **direkt bunt/fett**, nicht den rohen Code.
Das ist das „WYSIWYG"-Gefühl, obwohl darunter nur §-Markup liegt.

---

## 4. Vom Editor zum Brief: Seiten, Zeilen, Absätze

`LetterScreen` arbeitet mit:
- **max. 5 Seiten**, jede Seite **12 Zeilen** (`MAX_PAGES=5`, `MAX_LINES=12`).
- Zeilenbreite **114 px** (`LINE_PIXEL_W`) – Wortumbruch `wrapOverflow()` schiebt zu
  lange Zeilen automatisch in die nächste Zeile/Seite.
- `pageNewlines[]`: merkt sich pro Zeile, ob der Spieler dort **Enter** gedrückt hat
  (= bewusster Absatz, Z. 532–540).

### `/letter`-Erzeugung – `startSending()` (Z. 666–751)

Das ist der Kern der Frage „wie wird das mit /letter gemacht?":

**Ein `/letter`-Befehl pro Seite** (nicht pro Zeile!). Pro Seite:

1. **Absätze bauen:** Zeilen werden zu Absätzen zusammengefügt.
   - Normale Folgezeile → mit **Leerzeichen** angehängt (Fließtext).
   - Zeile mit explizitem Enter (`pageNewlines[j]`) → **neuer Absatz**.
   ```java
   if (isExplicitBreak) { paragraphs.add(para...); para = new StringBuilder(); }
   else if (j > startLine && !line.isEmpty()) { para.append(" "); }
   para.append(line);
   ```
2. **Absätze verketten** mit literalem `\n` (zwei Zeichen Backslash-n, Z. 720):
   ```java
   if (p > 0) sb.append("\\n");
   ```
   → Der Server bekommt `\n` als Trennzeichen und macht daraus Zeilenumbrüche.
3. **Leere Absätze / Anker:** Zero-Width-Non-Joiner `‌` füllt leere Absätze und
   markiert Absatzenden (Z. 724–728), damit der Server leere Zeilen nicht verschluckt.
4. **Längenlimit 248 Zeichen** pro Befehl (`sb.setLength(248)`, Z. 731).

### `§` → `&` Rückwandlung beim Senden (Z. 734–738)

Bevor der Befehl rausgeht, werden alle gültigen `§`-Codes wieder zu `&`:

```java
for (int r = 0; r < sb.length() - 1; r++) {
    char nx;
    if (sb.charAt(r) == '§' && istGültigerCode(nx = sb.charAt(r+1)))
        sb.setCharAt(r, '&');
}
```

Grund: `§` ist im Chat/Command-Kanal heikel (wird teils gefiltert/gesperrt),
`&` ist neutraler Transport. Der Server interpretiert `&` als Formatierungszeichen.

### Versand mit Verzögerung

```java
String cmd = "letter " + sb;
long delay = sendIndex * 1200L;          // 1,2 s Abstand pro Seite
SCHEDULER.schedule(() -> mc.execute(() ->
    mc.player.networkHandler.sendCommand(cmd)), delay, MILLISECONDS);
```

- `sendCommand` (`method_45730`) schickt den Befehl **ohne** führenden Slash an den
  Server – `/letter` und `/post` sind also **Server-Commands**, nicht Mod-Commands.
- **1200 ms Pause** zwischen den Seiten gegen Rate-Limit/Spam-Kick.
- Währenddessen Lade-Overlay (`isLoading`, „Wird gesendet…").

---

## 5. Welche Codes überleben? (Whitelist)

Sowohl beim Anzeigen (`ampToSection`) als auch beim Senden gilt dieselbe Whitelist:

```
0-9, a-f   → Farben
k          → obfuscated (magic)
l          → fett
m          → durchgestrichen
n          → unterstrichen
o          → kursiv
r          → reset
```

Alles andere (`&` ohne gültigen Folgecode) bleibt als normales `&`-Zeichen stehen.

---

## 6. Empfängerwahl & `/post`

Nach „Senden" öffnet `PostRecipientScreen` (eigenes Pergament-GUI).
Auswahl eines Empfängers → `sendLetter()` (Z. 258–261):

```java
mc.player.networkHandler.sendCommand("post " + accountName);
```

Ablauf gesamt also:
```
[Seite 1] letter &c&lÜberschrift\nFließtext…‌
[Seite 2] letter &rWeiterer Text…           (1,2 s später)
...
post <Empfängername>                          (nach Empfängerwahl)
```

Der Server sammelt die `letter`-Fragmente zum Brief und verschickt ihn mit `post`
an den Empfänger / als Discord-Verkündung – inkl. der `&`-Farbcodes.

---

## 7. Antwort auf die Ausgangsfrage

> „Mich wundert, wie das mit /letter gemacht wurde, wenn man bunt schreibt oder
> formatiert."

**Es gab nie buntes „rohes" Senden.** Die Farbe ist die ganze Zeit nur ein
**Textcode** (`&c`, `&l` …):

- Die Formatierungsleiste **tippt den Code für dich** an die Cursor-Stelle (`§c`).
- Der Editor zeigt ihn **live farbig** (`§` wird gerendert) – wirkt wie Rich-Text.
- Beim Versand wird `§`→`&` gewandelt und der **komplette Markup-String** als
  Argument in `letter <text>` gepackt – pro Seite ein Befehl, mit `\n` für Absätze
  und `‌` als Leer-/Anker-Marker.
- **Der Server** parst `&`-Codes und macht daraus die farbige Ausgabe.

Die „Magie" steckt also in zwei simplen Umwandlungen (`& ↔ §`) plus dem
seitenweisen Zusammenbauen des Befehls – kein echtes Rich-Text-Format, nur
durchgereichtes Minecraft-Farbmarkup.

---

## 8. Relevante Codestellen (Übersicht)

| Was                          | Methode / Zeile (LetterScreen.java) |
|------------------------------|-------------------------------------|
| Farb-/Stilcode-Tabellen      | Z. 48–51                            |
| Formatierungsleiste zeichnen | `renderFormattingBar` Z. 453–481    |
| Klick → Code einfügen        | `insertFormattingCode` Z. 483–505   |
| Klick-Erkennung Leiste       | `method_25402` Z. 595–611           |
| `&`→`§` für Live-Vorschau    | `ampToSection` Z. 791–806           |
| Brief zusammenbauen + senden | `startSending` Z. 666–751           |
| `§`→`&` Rückwandlung         | Z. 734–738                          |
| Absatz/`\n`-Logik            | Z. 705–729                          |
| Enter = Absatz merken        | `method_25404` Z. 532–540           |
| Empfänger + `post`           | `PostRecipientScreen` Z. 258–261    |
