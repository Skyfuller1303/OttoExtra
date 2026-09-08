# LETTER-02: Verkündungsoptimierung verändert den bereits gestarteten Schreibplan nicht

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: UI- und Queue-Aufrufkette statisch bestätigt; optionales Feature standardmäßig aus.

## Auslöser und Folge

Mit `announcementAutoOptimizeEnabled=true` einen noch gültigen Pending-Entwurf über `/ottoextra letter announce` öffnen und im Preflight „Optimieren“ wählen. Der Entwurf muss über die Briefaktionsauswahl vorgemerkt und weniger als zehn Minuten alt sein; eine gespeicherte Datei allein reicht nicht. Die Anzeige kann danach einen passenden Umbruch melden, obwohl bereits der ursprüngliche Text auf den Server geschrieben wurde oder gerade geschrieben wird.

## Codebeleg

- [LetterActionPrompt.java:57](../../../src/client/java/de/ottoextra/letter/ui/LetterActionPrompt.java#L57): `onAnnounce()` beginnt den Schreibvorgang vor dem Öffnen des Preflights; die Befehlsliste steht bereits fest.
- [AnnouncementPreflightScreen.java:65](../../../src/client/java/de/ottoextra/letter/ui/AnnouncementPreflightScreen.java#L65): Optimieren ersetzt nur `draft.pages` und aktualisiert die Prüfung.
- [AnnouncementPreflightScreen.java:56](../../../src/client/java/de/ottoextra/letter/ui/AnnouncementPreflightScreen.java#L56): Abschließendes Senden verschickt nur den Submit-Befehl, keinen neu aufgebauten Schreibplan.

## Reproduktion

Testentwurf mit 18 Zeichen breiten Zeilen bei Sicherheitsbreite 17 verwenden und über die Briefaktionsauswahl vormerken (`LetterActionPrompt.show()` setzt den Pending-Entwurf). Innerhalb von zehn Minuten die Verkündung starten, optimieren und die tatsächlich geplanten Schreibbefehle mit der neuen Vorschau vergleichen: Sie enthalten noch den ursprünglichen Entwurf.

## Fixansatz und Regressionstest

Preflight und Optimierung vor Planbildung und Schreibstart abschließen. Test: Nach Optimierung müssen genau die geprüften finalen Seiten die gesendete Befehlsliste bestimmen.

## Umsetzung am 06.09.2026

Das Öffnen des Preflights startet keine Schreibqueue mehr. Erst die Bestätigung plant und startet Schreiben einschließlich optionalem Submit. Optimierung und tatsächlicher Plan verwenden dieselben finalen Seiten, auch im V2-Modus mit vorhandenem kanonischem Dokument. Die Prüfung berücksichtigt den vorhergesagten Serverumbruch und ungültige Quellseiten.

Regression: [AnnouncementPreflightFlowTest.java](../../../src/test/java/de/ottoextra/letter/ui/AnnouncementPreflightFlowTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
