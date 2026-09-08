# Serververtrag der gelieferten Courier-Version

## Referenz

- JAR: `input/courier-2.1.0.jar`
- SHA-256: `9310a0acaeaed0123a8fd2007502065090c0864dbfe277e779d266601f789b4f`
- Quellen: `input/Courier-main/Courier-main/`
- Maven-Version 2.1.0, Java 21, Paper-API 1.21.11-R0.1-SNAPSHOT;
  `plugin.yml` deklariert API-Version 1.20.
- Abhängigkeiten: Ottonien Library, VaultAPI, DiscordHook und Commons Lang;
  das Standardmaterial wird über MythicItem `Pergament_und_Feder` aufgelöst.
- JAR und Quellen enthalten bytegleiche `config.yml` und `messages.yml`.
  Briefchecker/Creation wurden auch am Bytecode abgeglichen.

Quellen: [pom.xml](../../../input/Courier-main/Courier-main/pom.xml),
[plugin.yml](../../../input/Courier-main/Courier-main/src/main/resources/plugin.yml),
[config.yml](../../../input/Courier-main/Courier-main/src/main/resources/config.yml).
Diese Dateien belegen die ausgelieferten Defaults. Abweichende Live-Konfiguration,
Permissions und globale Befehlsaliase sind dadurch nicht bekannt.

## Befehle

| Befehl | Verhalten |
| --- | --- |
| `/letter <Text>` | Permission `courier.letter`; unversandten Brief in der Haupthand erweitern, sonst einen neuen Brief aus Schreibmaterial erzeugen. |
| `/post <Spieler>` | Spieler dürfen genau ein Argument übergeben. Kein Nachrichtentext nach dem Empfänger. |
| `/post a,b`, `/post *`, `/post **` | Serverseitig Sonderfälle für mehrere Empfänger, alle Online-Spieler bzw. alle bekannten Spieler, mit separaten Permissions. OttoExtras normale Empfängerwahl bleibt auf einen Account beschränkt. |
| `/postas ...` | Ausschließlich Konsole. |
| `/unread` | Wartende Brieftaube rufen; Default-Cooldown 30 Sekunden (`remove-delay / 20`). |
| `/announce`, `/verkünden` | Gehaltenen Brief zur Verkündung einreichen. Default Kosten 20, Verzögerung und Cooldown je 3600 Sekunden. |
| `/announcements`, `/verkündungen` | Verkündungen anzeigen. |

`letter`, `post` und `unread` haben im Plugin keine deutschen Aliase.
Welten und Spielmodi können blockiert werden. Quelle:
[CommandExec.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/commands/CommandExec.java),
insbesondere Zeilen 52–79, 155–179, 189–260.

## Item, Titel und Versandstatus

Ein Brief ist `WRITTEN_BOOK`; `BookMeta.title` enthält standardmäßig den Marker
`§7Brief von §6`. Der Autor ist der Spielername. Courier prüft keine Autorenrechte,
keine PDC-ID und kein Itemmodell bei der Briefidentifikation.

Eine Lorezeile mit `§8Zu ` markiert den Brief als versandt. Ein beliebiges Vorkommen
des unformatierten Wortes „Zu“ genügt nicht. Der erzeugte Brief besitzt standardmäßig
das Modell `ottonien:item/brief`, abgeleitet vom Configwert `item/brief` durch den
gebündelten `CustomModelUtil`.

Neuanlage erfordert ein zur Serverkonfiguration `isSimilar` passendes Material in
der Haupthand. `ItemUtil.removeItem` entfernt allerdings das erste passende Exemplar
im Inventar ab Slot 0. Gibt es weiteres Material, kann dadurch die Haupthand belegt
bleiben und das erzeugte Buch in einem anderen Slot landen. OttoExtra verlangt
deshalb für einen neuen automatischen Schreibvorgang genau ein entsprechendes
Materialexemplar im Inventar. Der Materialname im Client ist eine Vorprüfung;
die verbindliche MythicItem-Identität besitzt nur der Server.

Quellen: [LetterChecker.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/letter/LetterChecker.java)
Zeilen 32–65, [LetterCreation.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/letter/LetterCreation.java)
Zeilen 38–92 sowie `ItemUtil`-/`CustomModelUtil`-Bytecode aus der JAR.

## Textverarbeitung und Seiten

1. Paper trennt Befehlsargumente an ASCII-Leerzeichen; Courier fügt nach jedem Argument
   ein Leerzeichen an. Tabs und andere Zeichen sind nicht pauschal `.trim()`-Leerraum.
2. Der MessageManager ersetzt `%prefix%`, die wörtliche Folge `\n` durch LF und
   gültige ASCII-`&`-Farbcodes durch `§`-Codes. Beispielsweise ist `&K` kein Bukkit-Farbcode.
3. Create und Edit verwenden dieselbe `appendMessageToBook`-Methode.
4. Die Nachricht wird an LF in logische Zeilen, jede Zeile an Leerzeichen in Wörter zerlegt.
   Leere Zeilen und führende Leerzeichen werden vom Algorithmus verworfen.
5. Eine übernommene Zeile wird auf nichtleerer letzter Seite mit **einem LF** angehängt.
6. Bei Überlauf werden Wörter auf weitere Zeilen/Seiten verteilt.

Für den Überlauf gilt exakt:

```text
estimatedLines(s) = Summe max(1, ceil(Zeilenlänge / 18)) über LF-getrennte Zeilen
overflow(page, line) =
    (page leer ? 0 : estimatedLines(page))
    + estimatedLines(line) + (page leer ? 0 : 0.5) > 13
    ODER page.length + line.length + 1 > 255
```

Es zählen Java-UTF-16-Einheiten einschließlich Formatcodes. Die zusätzliche 1 gilt
auch auf einer leeren Seite. Kurze Zeilen passen daher üblicherweise zwölfmal auf
eine Seite. Die Werte 108 Pixel, 10 Arbeitszeilen und 816 Gesamtpixel sind keine
Courier-Serverregeln.

Quellen: [LetterCreation.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/letter/LetterCreation.java)
Zeilen 27–29 und 95–162; `MessageManager.replacePlaceholdersToParameters` und
Paper `SimpleCommandMap.dispatch` im lokalen JAR-Bytecode.

## Antworten, Lesen und Zustellung

Die üblichen Antworten beginnen mit `[Courier]`. Schreiben meldet
„Du hast einen Brief geschrieben.“, Anhängen „Du hast den Brief bearbeitet.“.
Permission-, Material-, Geld-, Welt- und Cooldownfehler werden separat im Chat ausgegeben.

`/post` leert bei Erfolg den Haupthandstack. Verkünden verbraucht ein Exemplar.
Courier speichert die Zustellung asynchron als Bukkit-ItemStack in einer UUID-Queue.
Die Brieftaube wird per Entity-Rechtsklick angenommen, nicht durch einen Annahmebefehl.
OttoExtra hat keinen zuverlässigen Zustell-ACK-Kanal; „Befehl gesendet“ ist keine
Bestätigung, dass ein Empfänger den Brief erhalten hat.

Das Öffnen eines Buches wird durch `BookListener` abgefangen. `BookManager` öffnet ein
temporäres Written Book ohne ursprüngliche Lore/Itemmodell. Leseskills können dessen
Text verändern. Defaultwerte mischen 50 % der Wörter und ersetzen 80 % der Buchstaben;
`skills.reading.6` setzt beide Werte auf 0. Für Textabgleiche deshalb den tatsächlichen
Inventarinhalt erfassen oder mit maximalem Leseskill vergleichen.

Quellen: [messages.yml](../../../input/Courier-main/Courier-main/src/main/resources/messages.yml),
[LetterSender.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/letter/LetterSender.java),
[BookListener.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/listener/BookListener.java),
[BookManager.java](../../../input/Courier-main/Courier-main/src/main/java/de/ottonien/courier/book/BookManager.java).
