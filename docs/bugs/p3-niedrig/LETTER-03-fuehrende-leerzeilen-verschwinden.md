# LETTER-03: V2-Briefplaner entfernt führende Leerzeilen

Priorität: **P3 – niedrig**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Originaler V2-Planer lokal mit Java 21 reproduziert.

## Auslöser und Folge

Ein V2-Briefdokument beginnt mit einer oder mehreren Leerzeilen. Die gewünschten Abstände verschwinden schon beim Erstellen des Schreibplans. Eingaben `"\nHallo"` und `"\n\nHallo"` ergeben beide `predictedPages=[Hallo]` und `commands=[letter Hallo]`.

## Codebeleg

- [LetterV2Planner.java:179](../../../src/client/java/de/ottoextra/letter/send/LetterV2Planner.java#L179): Verarbeitung zählt bereits vorhandene Zeilen, einschließlich leerer Zeilen.
- [LetterV2Planner.java:175](../../../src/client/java/de/ottoextra/letter/send/LetterV2Planner.java#L175): Der nächste Trennumbruch wird jedoch von `!page.isEmpty()` abhängig gemacht. Nach einer leeren ersten Zeile ist der StringBuilder weiterhin leer; der Umbruch entfällt.
- Das nachfolgende Materialisieren leerer Zeilen kann diese bereits verlorene Information nicht rekonstruieren.

## Fixansatz und Regressionstest

Vorherige Zeilen anhand des Zeilenzählers erkennen und diese Logik auch bei Kandidatenbildung und Seitenumbrüchen verwenden. Regression: keine, eine und zwei führende Leerzeilen sowie ein Seitenwechsel müssen im Plan unterscheidbar bleiben. Lokales Beispiel unter [Validierung](../VALIDIERUNG.md).

## Umsetzung am 06.09.2026

Der V2-Planer verwendet den Zeilenzähler für Kandidatenbildung, Formatpräfixe, Seitenwechsel und Trennumbrüche. Damit bleiben führende Leerzeilen und vollständig leere Seiten erhalten. Auch der Fließtextpfad erhält einen einzelnen Umbruch am Dokumentanfang/-ende; nur innere weiche Umbrüche werden weiterhin zusammengezogen.

Drei ergänzte Tests in [LetterV2PlannerTest.java](../../../src/test/java/de/ottoextra/letter/send/LetterV2PlannerTest.java) prüfen null/eine/zwei führende Leerzeilen, eine volle Leerzeilenseite sowie Formatierung nach einem Seitenwechsel. Vorschau, Befehle und das lokale Courier-Protokollmodell werden verglichen. Zwei dieser Tests schlugen vor dem Fix fehl. [V2DraftFlowTest.java](../../../src/test/java/de/ottoextra/letter/V2DraftFlowTest.java) erwartet beim Löschen einer anderen Seite jetzt korrekt die erhaltene Leerzeile der verbleibenden Seite.

Buildnachweis und verbleibende Ingame-Abnahme: [P3-UMSETZUNG.md](../P3-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor dem Fix.
