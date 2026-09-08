package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoResponseCollectorTest {

    @Test
    void collectsShownServerBlockAndKeepsExactBreedingValue() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        assertConsumed(collector, "--------------------------------");
        assertConsumed(collector, "Tierinfo - Zaupel");
        assertConsumed(collector, "Geschlecht: Weiblich");
        assertConsumed(collector, "Mast: 0/30");
        assertConsumed(collector, "Nächster Mastverlust: sobald das Tier gefüttert wurde");
        assertConsumed(collector, "Scherbar: Mast 0/20");
        assertConsumed(collector, "Melkbar: Nein");
        assertConsumed(collector, "Paarungsbereit: Mast 0/30");

        AnimalInfoResponseCollector.Result end =
                collector.accept("--------------------------------");
        assertTrue(end.consumed());
        assertFalse(end.expectsTrailingDivider());
        assertEquals(new AnimalInfoSnapshot(
                        "Zaupel", "Weiblich", "0/30", "Mast 0/30",
                        "Mast 0/20", "Nein"),
                end.completed());
    }

    @Test
    void startsAtHeaderAndAcceptsAlternativeDash() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        assertConsumed(collector, "Tierinfo – Schaf");
        assertConsumed(collector, "Geschlecht: Männlich");
        assertConsumed(collector, "Paarungsbereit: Ja");

        AnimalInfoResponseCollector.Result end = collector.accept("----------------");
        assertEquals(new AnimalInfoSnapshot("Schaf", "Männlich", "", "Ja", "", ""),
                end.completed());
    }

    @Test
    void unrelatedMessagesPassBeforeBlockStarts() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        AnimalInfoResponseCollector.Result result = collector.accept("Willkommen auf Ottonien");

        assertFalse(result.consumed());
        assertNull(result.completed());
    }

    @Test
    void incompleteBlockIsConsumedButProducesNoToast() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertConsumed(collector, "Tierinfo - Kuh");
        assertConsumed(collector, "Geschlecht: Weiblich");

        AnimalInfoResponseCollector.Result end = collector.accept("----------------");

        assertTrue(end.consumed());
        assertNull(end.completed());
    }

    @Test
    void collectsHorseInfoWithoutDividersAndClosesAtZuchtbereit() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        assertConsumed(collector, "Pferdeinfo - Packesel");
        assertConsumed(collector, "Geschlecht: Stute");
        assertConsumed(collector, "Leben: /");
        assertConsumed(collector, "Sprunghöhe: 3,42 Blöcke");
        assertConsumed(collector, "Geschwindigkeit: 11,87 Blöcke/s");
        assertConsumed(collector, "Mast: 71/100");
        assertConsumed(collector, "Nächster Mastverlust: in 5m 35s");

        AnimalInfoResponseCollector.Result end =
                collector.accept("Zuchtbereit: Mast 71/100");

        assertTrue(end.consumed());
        assertTrue(end.expectsTrailingDivider());
        assertEquals(new AnimalInfoSnapshot(
                        "Packesel", "Stute", "71/100", "Mast 71/100", "", "",
                        "3,42 Blöcke", "11,87 Blöcke/s", "/"),
                end.completed());
    }

    @Test
    void acceptsEnglishHorseStatKeys() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        assertConsumed(collector, "Pferdeinfo: Horse");
        assertConsumed(collector, "Gender: Male");
        assertConsumed(collector, "Jump height: 2.91 blocks");
        assertConsumed(collector, "Speed: 10.24 blocks/s");

        AnimalInfoResponseCollector.Result end = collector.accept("Ready to breed: Yes");

        assertEquals("2.91 blocks", end.completed().jumpHeight());
        assertEquals("10.24 blocks/s", end.completed().speed());
    }

    @Test
    void collectsNumericHorseLife() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        assertConsumed(collector, "Pferdeinfo - Reitpferd");
        assertConsumed(collector, "Geschlecht: Hengst");
        assertConsumed(collector, "Leben: 24/30");

        AnimalInfoResponseCollector.Result end = collector.accept("Zuchtbereit: Ja");

        assertEquals("24/30", end.completed().life());
    }

    private static void assertConsumed(AnimalInfoResponseCollector collector, String line) {
        AnimalInfoResponseCollector.Result result = collector.accept(line);
        assertTrue(result.consumed(), line);
        assertNull(result.completed(), line);
    }
}
