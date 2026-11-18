# Werkstatt (Seitenpanel am Zeugtisch)

Die Werkstatt ist ein Seitenpanel für Ottoniens Handwerksstationen. Die erste Station ist der
**Zeugtisch** (auch „Zeugstisch“): ein sechszeiliges Truhenmenü, das sich von einem
`minecraft:cartography_table` öffnet. Der Zeugtisch hat keine echten Brennstoff- oder Zutaten-Slots;
jede Aktion ist ein Klick auf einen Knopf-Gegenstand des Servers. Das Panel liest diese Knöpfe, zeigt,
was ein Rezept braucht, legt Feuer nach und klickt das Rezept.

Die ganze Funktion ist **standardmäßig aus** (`crafting.werkstattEnabled = false`). Einschalten im
Einstellungsbuch unter *Handwerk → Werkstatt (Zeugtisch)*.

## Serverregeln

Die Werkstatt tut nur, was man auch selbst klicken könnte, und das offen: feste Takte, keine
Zufallsverzögerung, nichts wird verschleiert. Der **Dauerbetrieb** (`crafting.werkstattAutoRun`)
wiederholt Aufträge selbstständig: Zeugtisch erneut öffnen, Feuer nachlegen, Rezept klicken. Er ist
standardmäßig aus. **Nur einschalten, falls das laut Serverregeln erlaubt ist.** Das Panel zeigt
diesen Hinweis in seiner Fußzeile. Ohne Dauerbetrieb zeigt das Panel trotzdem Rezepte, Zutaten,
Feuer und die Höchstmenge und bietet „1x herstellen“: genau ein Rezeptklick (wenn nötig vorher ein
Feuerklick) auf der offenen Seite.

## Bedienung

- **Stationen:** Die Werkstatt läuft an allen Herstellungsstationen aus `CraftingStation`
  (Zeugtisch, Rüstbank, Amboss, Bognertisch, Kochstelle, Schreibstube, Laboratorium; dazu die
  Schreibweise „Zeugstisch“ und Titelzusätze wie „(Seite 2)“). Die Station-ID ist der normalisierte
  Name (`WerkstattText.stationId`). Jeder Bildschirm, der aus einer Station heraus geöffnet wird
  (Untermenüs wie „Mehr Sonstiges“ oder „Mehr Pigmente“ mit eigenem Titel und anderer Größe, das
  Lademenü dazwischen), gehört zu dieser Station, bis die GUI geschlossen wird (600 ms Lücke sind
  erlaubt) oder ein fremder Bildschirm öffnet (`StationSession`). Ein Container mit unbekanntem Titel
  zählt nur dann als Untermenü, wenn seine Größe (Zahl der Container-Slots) der Station entspricht;
  sonst ist er fremd, beendet die Sitzung und in ihm wird nie geklickt. Unbekannte Untermenü-Titel
  stehen auf INFO im Log. Der benutzte Block (beliebiger Block, Seite und Trefferpunkt) wird pro Station
  gemerkt; der Dauerbetrieb öffnet nur diesen Block wieder. Aufträge tragen ihre Station und laufen
  nur dort; Warteschlange, gemerkte Rezepte und *Alle starten* zeigen bzw. verketten nur die Aufträge
  der offenen Station.
- **Reiter (Entwurf T2):** ein Rahmenreiter mit Zahnrad wächst aus der rechten Rahmenkante der GUI:
  Die Umrissspalte (Breite − 1) öffnet sich in den Reiter, er ragt 20 px heraus, ist 22 Zeilen hoch
  und sitzt 16 Zeilen unter der GUI-Oberkante (bei sehr flachen GUIs weiter oben, damit er auf dem
  Rahmen bleibt; `TabGeometry`). Äußere Ecken um 1 px abgeschnitten, 2 px Kordelbänder oben, unten und
  rechts im Diagonalmuster des Rahmens, innen #382A25; Zahnrad
  `textures/gui/werkstatt/zahnrad.png` bei (x0 + 1, y0 + 3). Hover: goldener Umriss #E2A650, innen
  #493626 (so im Generator für T2). Offen beginnt das Panel an der rechten Umrissspalte des Reiters;
  Umriss und Kordelbänder laufen in den Kordelrahmen des Panels weiter. Geometrie aus der echten GUI
  (`x`, `y`, `backgroundWidth`, `backgroundHeight`), auch für andere Containerformen. Offen/zu und
  der gewählte Reiter werden pro Station gemerkt (`StationMemory`).
- **Abzeichen** bei geschlossenem Panel, 2 px rechts vom Reiter, oben bündig: `6/20` mit Flamme
  während ein Auftrag dieser Station läuft, roter Punkt nach einem Stopp, Anzahl wartender Aufträge
  (gold), `+1` (gold) nach einem Rechtsklick; der Reiter leuchtet dann golden.
- **Brennstoff** (Reiter und Zeile im Auftrag) erscheint nur an Stationen, die Feuerknöpfe zeigen
  oder in dieser Sitzung gezeigt haben. Ohne Feuerknöpfe wird bei unbekannter Hitze nicht nachgelegt.
- **Tooltips:** Das Panel wird vor den verzögerten Elementen des Bildschirms gezeichnet
  (`WerkstattScreenPanelMixin`, vor `DrawContext.drawDeferredElements`), daher liegen die
  Gegenstands-Tooltips des Zeugtischs immer über dem Panel. Fabrics `afterRender` käme erst danach.
- **Auftrag anlegen:** Rechtsklick auf einen Rezeptknopf der Station oder eines ihrer Untermenüs
  (der Klick auf „Mehr …“ wird als Pfadschritt aufgezeichnet). Ein Rechtsklick bei geschlossenem
  Panel lässt es zu (Option B). Der Rechtsklick wird im Client
  abgefangen und **nie an den Server geschickt**. Linksklicks bleiben unverändert. Das Panel steht
  rechts neben der GUI, links, wenn rechts kein Platz ist.
- **Panel-Reiter** (volle Panelbreite):
  - *Auftrag:* der gewählte Auftrag mit Pfad („Zeugschmieden > Slot 11“), Zutaten für die gewählte
    Menge (vorhanden / nötig, grün oder rot), Hitze und ungefähre Zahl der Feuer, Ertrag
    („20x = 40 Eisenbarren“), Menge `-`/`+` (Shift: Zehnerschritte) und *Max*, Brennstoffwahl,
    der Schalter *Dauerbetrieb*, Herstellen-Knopf und die gemerkten Rezepte (Stern = merken, *Neu* = erneut einreihen). Während ein
    Auftrag läuft: Fortschritt, aktueller Schritt, Rest im Inventar, *Stopp* / *Pausieren*. Ein
    gestoppter Auftrag behält seinen Grund und lässt sich mit *Fortsetzen* weiterführen.
  - *Brennstoff:* die Feuerknöpfe der aktuellen Seite mit Brenndauer, Kosten und Vorrat, der Schalter
    für automatisches Nachlegen und die Feuerschätzung.
  - *Warteschlange:* alle Aufträge mit Umsortieren (`^`/`v`) und Entfernen (`x`), Gesamtzahl,
    *Bei Fehler nächsten Auftrag*, *Alle starten* (nur im Dauerbetrieb), *Stopp* und die Stopp-Regeln.
- **HUD-Leiste (Entwurf P1):** solange ein Auftrag läuft, gerade gestoppt oder fertig ist (15 Sekunden)
  und keine GUI offen ist, zeigt eine schmale dunkle Leiste im Stil der Aktivitätsanzeige: farbiger
  Akzentstreifen links, Item-Symbol, eine Textzeile, rechts Flamme und Feuerzeit, darunter ein 3 Pixel
  hoher Fortschrittsbalken. Zustände: läuft (gold, „Salz 23/27 · 4 fehlgeschlagen“), pausiert (grau,
  „Salz 23/27 · pausiert“), gestoppt (rot, „Salz gestoppt: <Grund>“), fertig (grün, „23 von 27
  hergestellt, 4 fehlgeschlagen“ mit Haken statt Feuerzeit). Bei gesperrter Sicht steht ein Schloss
  links neben der Feuerzeit. Die Stopp-Taste erscheint nur, wenn sie belegt ist; einen Befehlstext gibt
  es in der Leiste nicht (das Panel hat den Stopp-Knopf). Die Leiste ist mindestens 182 Pixel breit
  (eine Bossleiste), wächst mit dem Text bis zur Bildschirmbreite und kürzt sonst mit „…“
  (`WerkstattBarLayout`). Standardplatz ist oben in der Mitte: dort steht sie in der gemeinsamen
  Spalte `TopCenterStack` 4 Pixel unter der letzten sichtbaren Vanilla-Bossleiste (jedes Bild neu
  berechnet), der Vote-Toast folgt 2 Pixel darunter. Sie nimmt an HudFlow teil (`ottoextra:werkstatt`)
  und lässt sich im HUD-Layout-Editor verschieben; nahe der Mitte rastet sie wieder auf TOP_CENTER ein.
  Wer die alte Standardposition (TOP_LEFT, 4/4) nie verändert hat, wird einmalig nach oben Mitte
  umgestellt (`werkstattHudLayoutVersion`); eine selbst gewählte Position bleibt.

## Wie Rezepte gelesen werden

Es gibt keine eingebaute Rezeptliste. `RecipeLore.parse` liest die Lore jedes Knopfs:

- Erste Zeile: Seltenheit. Danach eine Beschreibung.
- `Herstellen:` beginnt den Zutatenblock mit Zeilen `N x Gegenstand`. Alternativen sind mit `oder`
  verbunden („2 x Steinkohle oder 1 x Torf“). Eine Leerzeile beendet den Block.
- `Benötigt: Hitze` kennzeichnet ein Rezept, das Feuer braucht.
- Feuerknöpfe nennen `+ m:ss Brenndauer` und ihre Kosten unter einem alleinstehenden `Benötigt:`
  (Feuer machen: `+ 0:15 Brenndauer`, `3 x Stock`).
- Ist eine Zutatenzeile nicht lesbar oder fehlt der Block, gelten die Zutaten als unbekannt: Das
  Panel zeigt „Zutaten unbekannt – nur Menge, stoppt wenn Server ablehnt“, und nur die Menge zählt.

Der Ertrag pro Durchgang ist die Stapelgröße des Knopfs. Bei Alternativen wird die erste genommen,
solange genug davon da ist; erst wenn sie knapp wird, die nächste, und das Panel zeigt, welche. Die
Höchstmenge wird Durchgang für Durchgang simuliert, daher zählt Kohle, die Zutat und Brennstoff
zugleich ist, nur einmal („Kohle ist auch Zutat“).

## Feuer

Der Server zeigt das Feuer nicht an. Die Werkstatt schätzt es: Sie beginnt bei null, jeder eigene
Feuerklick addiert die Brenndauer aus der Lore, und die Schätzung brennt in Echtzeit ab. Bevor ein
Rezept mit Hitze geklickt wird, wird Feuer gemacht, wenn die Schätzung unter 0:03 liegt
(`FireClock.REFUEL_BELOW_SECONDS`), und zwar mit dem Feuerknopf des gewählten Brennstoffs (Standard
Stock; Kohle oder Torf nur, wenn ein solcher Knopf mit „Brenndauer“ auf der Seite ist). Als
Feuerverbrauch pro Durchgang dient die gemessene Zeit vom Rezeptklick bis „Die Herstellung ist
erfolgreich“ (bis zur ersten Messung 10 s).

## Ablauf im Dauerbetrieb

Für jeden Durchgang eines Auftrags (`JobRunner`, reine Logik):

1. Station öffnen: Der Treffer, mit dem der Spieler den Zeugtisch selbst geöffnet hat (Block, Seite und
   Trefferpunkt), wird unverändert mit `interactBlock` wiederverwendet; das Fadenkreuz spielt keine
   Rolle. Ohne gespeicherten Treffer: das Fadenkreuz, falls es auf einem Kartentisch liegt, sonst die
   Oberseite des zuletzt benutzten. Er muss in Reichweite sein; der Spieler wird nie gedreht oder
   bewegt. Bleibt die Benutzung ohne Antwort (weder Lademenü noch Zeugtisch), wird sie nach 2 s
   wiederholt, höchstens dreimal insgesamt (`OPEN_RETRY_MS`, `OPEN_ATTEMPTS`).
2. Dem gemerkten Pfad folgen: jeder Schritt ist ein Knopfname mit Slot. Der Name entscheidet, der Slot
   ist nur ein Hinweis. Seitenwechsel laufen über Ottoniens 9-Slot-Lademenü (ein namenloses
   `structure_void`), auf das gewartet wird.
3. Wenn nötig Feuer nachlegen.
4. Rezept klicken. Es wird über Namen und Zutatenblock erkannt; der Slot ist nur ein Hinweis. Vor dem
   Klick werden die Zutaten mit dem Inventar abgeglichen.
5. Auf den Server warten: die GUI schließt sich, „Du beginnst die Herstellung von …“ kommt, dann
   „Die Herstellung ist erfolgreich“.
   Meldet der Server „Die Herstellung ist schiefgegangen...“ (auch „fehlgeschlagen“, „misslungen“;
   `CraftingParser.isCraftFailed`), zählt der Durchgang nicht als fertig, sondern als fehlgeschlagen
   (`WerkstattJob.failed`), und der Auftrag macht wie nach einem Erfolg weiter. Fehlschläge sind
   normal und werden nie nachgeholt: Die Menge zählt Durchgänge, der Auftrag ist fertig, sobald
   erfolgreiche plus fehlgeschlagene Durchgänge die Menge erreichen (`WerkstattJob.complete`), mit der
   Zusammenfassung „23 von 27 hergestellt, 4 fehlgeschlagen“. Max, Zutatenbedarf und „noch ca.“ rechnen
   mit den verbleibenden Durchgängen. Der Zutatenabgleich vor einem Rezeptklick stoppt nur, solange
   noch geplante Durchgänge fehlen, mit „Zutaten fehlen“. Kommt 25 s nach der Startmeldung kein
   Ergebnis (`RESULT_UNKNOWN_MS`), gilt der Durchgang ebenso als fehlgeschlagen. Nach drei
   Fehlschlägen in Folge (`MAX_CONSECUTIVE_FAILURES`, ein Erfolg setzt zurück) stoppt der Auftrag mit
   „zu viele Fehlschläge in Folge“. Panel, Warteschlange und HUD-Leiste zeigen „Salz 23/27 · 4
   fehlgeschlagen“.

Feste Takte: mindestens 350 ms zwischen zwei Aktionen, 800 ms nach einem Feuerklick auf einer Seite,
die offen bleibt, 700 ms zwischen zwei Durchgängen. Zeitgrenzen: 6 s für das Öffnen der Station, 4 s
für eine Seite, 6 s für die Startmeldung, 60 s für das Ergebnis. Eine Stationsseite ohne Slots (Inhalt
noch nicht vom Server gesendet) wird bis zu 1,5 s abgewartet, bevor Pfad, Feuerknopf oder Rezept gesucht
werden (`CONTENT_WAIT_MS`). Solange der Chat offen ist, wartet der Auftrag samt allen Zeitgrenzen
(`JobRunner.hold`); Schaden und Weggehen stoppen den Dauerbetrieb aber auch dann.

Ohne Dauerbetrieb (`werkstattAutoRun = false`, Standard) macht *Herstellen* genau einen Durchgang auf
der offenen Seite; danach endet der Auftrag mit `1/N` und wartet auf den nächsten Klick. Der Zeugtisch
wird dann nie neu geöffnet.

### Sicht- und Positionssperre

Mit `werkstattLockView` (Standard an) werden, solange ein Auftrag im Dauerbetrieb läuft und nicht
pausiert ist, Mausbewegungen nicht in Drehungen umgesetzt (`WerkstattMouseLockMixin`) und die Tasten
für Laufen, Springen, Schleichen und Sprinten ignoriert (`WerkstattMovementLockMixin`). Die Sperre
endet sofort bei Stopp, Pause, Ende des Auftrags, Trennung und Tod. Stopp-Taste, Befehl und Chat
bleiben nutzbar. Esc beendet den Auftrag: im Zeugtisch während des Dauerbetriebs, und außerhalb, wenn
dadurch das Pausenmenü öffnet (Grund „mit Esc gestoppt“; öffnet das Menü wegen Fokusverlust, lautet
der Grund „Fenster ohne Fokus“).

### Protokoll

Der Logger `ottoextra/werkstatt` schreibt auf INFO jeden Phasenwechsel, Start, Stopp mit Grund, Pause,
die erkannten Servermeldungen (Beginn, Erfolg, Ablehnung), jeden eigenen Klick und jeden
Öffnungsversuch mit Quelle des Treffers und Ergebnis von `interactBlock`.

Der Pfad wird beim eigenen Navigieren aufgezeichnet: Linksklicks auf Knöpfe, die weder Rezept noch
Feuerknopf sind, zählen als Schritt, sobald die nächste Seite erscheint; „Zurück“ nimmt den letzten
Schritt weg. Wird die Station über den Block geöffnet, beginnt ein neuer Pfad.

## Stoppbedingungen

Ein laufender Auftrag stoppt, behält seinen Fortschritt und zeigt den Grund, wenn

- der Spieler sich mehr als 2 Blöcke vom Startpunkt entfernt (abschaltbar),
- der Spieler Schaden nimmt (abschaltbar),
- eine andere GUI geöffnet wird (abschaltbar),
- das Spielfenster den Fokus verliert (abschaltbar, standardmäßig an),
- das Inventar voll ist (abschaltbar),
- Zutaten oder Brennstoff fehlen,
- der Server binnen 5 s nach einem eigenen Klick ablehnt: nur Meldungen des Servers selbst (Aktionsleiste
  oder Chatzeile mit „[Info]“/„[Fehler]“/„[Hinweis]“), nie Spielerchat. Gezählt werden eine
  Fehlermeldung zur Herstellung oder ein ganzer Ablehnungssatz mit höchstens drei Wörtern vor einer der
  Wendungen „nicht bereit“, „fehlende zutaten“, „zu wenig“, „nicht genug“, „kein feuer“, „feuer
  erloschen“ (dieselbe Liste wie in `WorkshopRules` des Agenten) und ohne Chatzeichen wie „Name:“ oder
  „<Name>“. Erfolgs- und Fehlschlagmeldungen („Die Herstellung ist erfolgreich!“, „… schiefgegangen...“)
  müssen die ganze Zeile sein,
- ein Schritt zu lange dauert, die Station außer Reichweite ist, Pfad oder Rezept nicht gefunden
  werden oder ein Gegenstand am Mauszeiger hängt,
- der Chatbefehl `/ottoextra werkstatt stop` kommt oder die Stopp-Taste (*Werkstatt: Auftrag
  stoppen*, standardmäßig nicht belegt) gedrückt wird,
- der Spieler stirbt oder Esc drückt (siehe Sicht- und Positionssperre),
- die Funktion ausgeschaltet wird oder die Verbindung endet.

Die abschaltbaren Regeln gelten nur im Dauerbetrieb; ein einzelner „1x“-Klick wird nicht überwacht.

## Speicherort

Gemerkte Rezepte und die Warteschlange liegen pro Server und Account in
`config/ottoextra/werkstatt/<sha256(Serveradresse)>.json`; in der Datei sind die Accounts getrennt.
Ein Auftrag, der beim Beenden des Spiels lief, kommt als gestoppt („Verbindung getrennt“) zurück.

## Einstellungen

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `crafting.werkstattEnabled` | false | Knopf und Panel am Zeugtisch |
| `crafting.werkstattAutoRun` | false | Dauerbetrieb, nur falls laut Serverregeln erlaubt |
| `crafting.werkstattLockView` | true | Sicht und Bewegung im Dauerbetrieb sperren |
| `crafting.werkstattAutoRefuel` | true | Feuer automatisch nachlegen |
| `crafting.werkstattStopOnMove` / `OnDamage` / `OnOtherGui` / `OnFull` / `OnUnfocus` | true | Stopp-Regeln |
| `crafting.werkstattNextOnError` | false | Nach einem Stopp mit dem nächsten Auftrag weitermachen |
| `crafting.werkstattHud`, `werkstattHudCorner`, `werkstattHudOffsetX/Y` | true, TOP_CENTER, 0, 0 | HUD-Leiste |

## Code

Paket `de.ottoextra.crafting.werkstatt`:

- Reine Logik mit Unit-Tests: `RecipeLore`, `CraftPlan`, `FireClock`, `JobRunner`, `StopRules`,
  `StopReason`, `WerkstattJob`, `WerkstattStore`, `WerkstattText`, `WerkstattBarLayout`, `TabStrip`.
- Minecraft-Seite: `Werkstatt` (Zustand, Bildschirm lesen, Klicks, Station erneut öffnen),
  `WerkstattPanel` (Knopf und Panel), `WerkstattHud` (Leiste und Layout-Element), `WerkstattModule`
  (Ereignisse, registriert aus `TweaksModule`). Mixins: `WerkstattScreenPanelMixin`,
  `WerkstattMouseLockMixin`, `WerkstattMovementLockMixin`.

## Annahmen, die am Server noch nicht geprüft sind

- Das Feuer ist nicht sichtbar; die Schätzung beginnt bei null, also macht der erste Hitze-Durchgang
  einer Sitzung immer Feuer.
- Ein Durchgang verbraucht Feuer für seine gemessene Dauer; geprüft wird nur vor einem Rezeptklick.
- Nach einem Feuerklick behält oder lädt der Server die Seite neu; schließt er die Station, öffnet der
  Dauerbetrieb sie wieder, der Einzelklick stoppt mit einem Hinweis.
- Zutatennamen in der Lore entsprechen den Anzeigenamen der Gegenstände im Inventar.
- Navigationsknöpfe haben feste Namen; der Pfad wird über die Namen nachgespielt.
