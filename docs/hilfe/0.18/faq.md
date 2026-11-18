# Häufige Fragen und Fehlersuche (0.18.0)

Hier stehen die häufigsten Fragen zu den Neuerungen in OttoExtra 0.18.0. Ausführliches findest du auf den Seiten zu den einzelnen Funktionen. Zurück zur [Übersicht](README.md).

## Installation und Umstieg

**Beim Start meldet Fabric einen Konflikt mit `ottoextra_resources` oder `ottoextra_tacticalfactions`.**
Im `mods`-Ordner liegt noch ein altes Addon-Jar. Resources und Tactical Factions sind jetzt in OttoExtra eingebaut. Lösche die alten Dateien und starte neu. Deine Einstellungen und Daten bleiben erhalten. Mehr: [Integrierte Module](integrierte-module.md).

**Muss ich nach dem Umstieg etwas neu einrichten?**
Nein. Konfiguration und gespeicherte Daten werden übernommen.

**Xaero's World Map oder Minimap ist nicht installiert. Geht OttoExtra trotzdem?**
Ja. Die Xaero-Mods sind empfohlen, aber OttoExtra läuft auch ohne sie. Nur die Karten- und Minimap-Funktionen, die Xaero brauchen, stehen dann nicht zur Verfügung.

**Wo finde ich die Einstellungen?**
Über **ModMenu → OttoExtra** oder ein Tastenkürzel, falls du eines vergeben hast. Manche Optionen sind unter „Mehr Optionen“ eingeklappt, technische Werte erscheinen mit den Experten-Einstellungen.

## Werkstatt

**Darf ich die Werkstatt benutzen?**
Das Panel ja, es macht nur einzelne Klicks für dich. Den **Dauerbetrieb** nur, wenn die Serverregeln es erlauben. Beides ist standardmäßig aus. Mehr: [Werkstatt](werkstatt.md).

**Warum zählt ein Fehlschlag als Durchgang?**
Die Menge zählt Durchgänge. Fehlschläge gehören zum normalen Ablauf und werden nicht nachgeholt. Am Ende steht zum Beispiel „23 von 27 hergestellt, 4 fehlgeschlagen“. Drei Fehlschläge in Folge stoppen den Auftrag.

**Ich kann mich nicht umsehen, während die Werkstatt läuft.**
Das ist die Sichtsperre im Dauerbetrieb. Stoppe mit Esc, dem Befehl `/ottoextra werkstatt stop` oder dem Stopp-Knopf. Du kannst die Sperre auch ausschalten (**Sicht im Dauerbetrieb sperren**).

**Der Auftrag ist von allein gestoppt.**
Die Leiste oben und das Panel nennen den Grund, etwa Schaden, Weggehen, fremdes Fenster, fehlende Zutaten oder Brennstoff.

## Leiden und Zustände

**Warum blitzt das Menü nicht mehr auf?**
Die Prüfung ist jetzt unsichtbar. Möchtest du das alte Verhalten, schalte **Menüprüfung unsichtbar** aus (Allgemein → Leiden und Zustände, unter „Mehr Optionen“).

**Was bedeutet „Prüfung nicht möglich: <Grund>“ nach `/ottoextra leiden check`?**
Die Prüfung läuft gerade nicht. Der Grund wird im Chat auf Englisch angezeigt. Das heißt:

| Grund im Chat | Bedeutung |
| --- | --- |
| `not in world` | Du bist nicht in einer Welt. |
| `not on Ottonien` | Du bist nicht auf dem Ottonien-Server. |
| `combat boss bar` | Eine Kampf-Bossleiste ist aktiv. |
| `recent damage` / `recent attack` | Du hast vor weniger als 10 Sekunden Schaden genommen oder angegriffen. |
| `screen open` | Ein Fenster ist offen. |
| `Werkstatt job running` | Ein Werkstatt-Auftrag läuft. |
| `moving`, `sprinting`, `flying`, `riding` | Du bewegst dich, sprintest, fliegst oder reitest. |
| `window unfocused` | Das Spielfenster ist nicht aktiv. |
| `inventory/use key pressed` | Du hältst gerade eine Bewegungs-, Inventar- oder Benutzen-Taste. |
| `using an item (fishing rod, bow, shield, ...)` | Du benutzt einen Gegenstand, zum Beispiel Angel, Bogen oder Schild. |
| `item cooldown` | Das Menü-Item hat Abklingzeit. |
| `no Menü item in hotbar` | Das Menü-Item des Servers liegt nicht in deiner Schnellleiste. |

Steh still, schließe Fenster und gib den Befehl noch einmal ein. Weitere mögliche Meldungen: „Prüfung abgebrochen: anderes Fenster geöffnet.“ und „Zeitüberschreitung: das Menü hat nicht geantwortet.“

**Wird mein Zustand geteilt, ohne dass ich es weiß?**
Teilen ist standardmäßig an, braucht aber deine Einwilligung für Online-Dienste und ein verknüpftes Konto. Du kannst es jederzeit ausschalten, dann wird dein Stand sofort gelöscht. Ein Stand verfällt nach 20 Minuten. Mehr: [Leiden und Zustände](leiden-und-zustaende.md).

## HUD und Vote

**Meine Anzeigen sitzen anders als vorher.**
Anzeigen in derselben Spalte schieben sich jetzt weg, und oben in der Mitte gibt es einen gemeinsamen Stapel. Mit `/ottoextra hud` ordnest du neu an. Mehr: [HUD](hud.md).

**Die Vote-Karte: „Später“ liegt auf X und stört im Kreativmodus.**
Die Standardtaste für „Später“ ist **X**. Im Kreativmodus liegt auf X in Minecraft standardmäßig „Schnellleiste laden“, sodass sich beide Tasten überschneiden können. Belege **Vote-Erinnerung später** in den Minecraft-Tasteneinstellungen auf eine andere Taste. „Abstimmen“ (Standard Enter) heißt dort **Ottonien-Vote-Seiten öffnen**.

## Tiere und Höfe

**Meine Tierinfo sieht anders aus.**
Alle starten einmalig im Stil „Ausführlich“. Dein eigenes Layout ist unter **Darstellung → Eigenes Layout** weiter wählbar. Mehr: [Tiere und Höfe](tiere-und-hoefe.md).

**Warum zeigt die Tierinfo mein Reittier nicht?**
**Eigenes Reittier ausblenden** ist an (Tiere & Hof → Tierinfo beim Anvisieren, unter „Mehr Optionen“).

**Wie teile ich Höfe?**
Tierübersicht → Hof wählen → **Teilen**. Der Code „Für deine Gruppe“ ist wie ein Passwort, der Lesecode (beginnt mit `H`) ist nur zum Ansehen. Beitreten mit dem Codefeld oder `/ottoextra tiere beitreten <Code>`.

**Mein Tierbuch war defekt.**
Die Datei wird nicht überschrieben, sondern als `….broken-….json` gesichert. Die neuesten drei Sicherungen bleiben erhalten.

## Banner

**Mein Regionsbanner sieht anders aus.**
Der Standard ist jetzt „Plakette“. Den alten Kasten wählst du unter Lehen & Regionen → Aussehen der Einblendung → **Banner-Stil** → **Klassisch (Kasten)**. Mehr: [Regionsbanner](region-banner.md).

## Allgemein

**Wo melde ich Fehler?**
Beim Team oder im GitHub-Projekt von OttoExtra. 0.18.0-alpha ist eine Vorabversion. Noch nicht vollständig im Spiel geprüft sind die Werkstatt auf allen Stationen, die unsichtbare Leiden-Prüfung unter Last und das Zusammenspiel der integrierten Module.

**Tab drückt nichts.**
In Namensfeldern vervollständigt Tab nur bei einem Treffer, sonst wechselt es das Feld. Mehr: [Tab-Vervollständigung](tab-vervollstaendigung.md).
