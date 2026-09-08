# Einzelnes Pergament wurde wegen weiteren Vorrats blockiert

Nutzermeldung vom 8. September 2026, Alpha `0.1.17-a`.

## Ursache und Reproduktion

`LetterWriteItemGuard.check` zählte identische Stacks im gesamten Spielerinventar.
Bei mehr als einem Exemplar lieferte er dieselbe Ablehnung wie für einen Stapel
im aktiven Slot. Die Meldung behauptete deshalb fälschlich, der aktive Slot
enthalte mehrere Items. Im Editor wurde zusätzlich nur die allgemeine Meldung
zu Text/Verbindung angezeigt.

Vor der Korrektur reproduziert mit echtem Fabric-Client und der unveränderten
bereitgestellten Courier-2.1.0-JAR: ein Pergament in Slot 0, ein weiteres in Slot 5.
Der geänderte Positivfall `G05` scheiterte. Der Server erhielt **0 Schreibbefehle**;
beide einzelnen Pergamente blieben im Inventar.
[Nachweis](../../build/letter-supply-fix/before/results.json),
[Clientlog](../../build/letter-supply-fix/before/client.log).

## Korrektur

- Ein neues Schreiben verlangt weiterhin ein einzelnes passendes Item in der
  Haupthand. Leere Hand und ein echter Stapel werden getrennt erkannt.
- Nur ähnliche Materialien **vor** dem aktiven Hotbar-Slot können Courier bei
  der Entnahme zuvorkommen. Spätere Hotbar-Slots, Hauptinventar und Nebenhand
  werden nicht mehr pauschal gesperrt.
- Die Prüfung erfolgt auch direkt vor dem ersten Queue-Befehl erneut.
- Ähnlichkeit folgt dem Bytecode der gelieferten Library: Typ, Schaden,
  formatierter Name und Lore. Ein anderes Modell allein verhindert die
  serverseitige Entnahme nicht.
- Editor und Chat verwenden dieselbe konkrete Materialmeldung. Der Entwurf
  bleibt bei einer Ablehnung geöffnet und erhalten.

Courier entfernt das erste passende Material ab Slot 0. Bleibt die Haupthand
dadurch belegt, legt er das Buch anderswo ab. Diese Serverbesonderheit ist
weiterhin vorhanden. Die Mod verschiebt dafür keine Inventarinhalte.
Eine sichere Anordnung ist **ein einzelnes Pergament im ersten Hotbar-Slot**;
weiterer Vorrat kann rechts davon, im Hauptinventar oder in der Nebenhand liegen.

## Testfälle

Der gezielte echte Client-/Serverlauf besteht mit **10/10 Fällen**:

| Fall | Ergebnis |
| --- | --- |
| G01 / G02 | Leere Hand und echter Stapel blockiert; genaue Meldung direkt im Editor, ohne horizontales Abschneiden |
| G05 | Ein Pergament trotz weiterem Pergament rechts davon beschrieben; Vorrat erhalten |
| G07 | Mehrseitenbrief aus Hotbar-Slot 3, Vorrat im Hauptinventar erhalten |
| G08 | Schreiben aus Hotbar-Slot 9, Vorrat in Nebenhand erhalten |
| G09 | Früheres Material mit eigener Meldung blockiert; nach Auswahl des ersten Slots derselbe Entwurf erfolgreich beschrieben |
| G10 | Anderes Modell im früheren Slot erkannt. Direkter Testbefehl außerhalb der Mod bestätigt: Courier legt das neue Buch tatsächlich in den früheren Slot |
| G11 / G12 | Material mit anderer Lore oder anderem Namen weiter links blockiert nicht und bleibt erhalten |
| G13 | Bei leerer Haupthand kein automatisches Ausweichen auf Vorräte |

[Einzelergebnisse](../../build/letter-supply-fix/focused/results.json),
[sichtbare Editor-Meldung](../../build/letter-supply-fix/focused/earlier-supply-hint.png).
Der Screenshot wurde visuell geprüft; Aktionsbuttons und Hinweis sind sichtbar.
Die Modellabweichung wird ausschließlich in der isolierten Serverfixture erzeugt.
Produktives MythicMobs-Material bleibt weiterhin eine Live-Abnahmegrenze.

Der abschließende Gesamt- und Buildnachweis steht in
[der Release-Validierung](../releases/0.1.17-b.md).
