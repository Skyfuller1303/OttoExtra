package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoResponseCollectorTest {

    @ParameterizedTest
    @MethodSource("newServerSnapshots")
    void collectsNewServerBlocks(AnimalInfoSnapshot expected) {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();

        assertConsumed(collector, "--------------------------------");
        assertConsumed(collector, "- " + expected.animalName());
        assertConsumed(collector, "⚥  Geschlecht: " + expected.gender());
        if (!expected.life().isEmpty()) {
            assertConsumed(collector, "💪 Leben: " + expected.life());
            assertConsumed(collector, "📏 Sprunghöhe: " + expected.jumpHeight());
            assertConsumed(collector, "🏃 Geschwindigkeit: " + expected.speed());
        }
        assertConsumed(collector, "🍽 Mast: " + expected.mast());
        assertConsumed(collector, "⏲  Mastverlust: sobald das Tier gefüttert wurde");
        if (expected.hasShearable()) {
            assertConsumed(collector, "✂ Scherbar: " + expected.shearable());
        }
        if (expected.hasMilkable()) {
            assertConsumed(collector, " Melkbar: " + expected.milkable());
        }
        assertConsumed(collector, "❤ " + (expected.hasHorseStats() ? "Zuchtbereit" : "Paarungsbereit")
                + ": " + expected.breedingReady());

        AnimalInfoResponseCollector.Result result = collector.accept("--------------------------------");
        assertEquals(expected, result.completed());
        assertFalse(result.expectsTrailingDivider());
        assertFalse(collector.accept("Willkommen").consumed());
    }

    private static Stream<AnimalInfoSnapshot> newServerSnapshots() {
        return Stream.of(
                new AnimalInfoSnapshot("Reitpferd", "Hengst (Männlich)", "0/100", "Mast 0/100",
                        "", "", "1.35 Blöcke", "14.66 Blöcke/s", "23.5"),
                new AnimalInfoSnapshot("Reitpferd", "Stute (Weiblich)", "0/100", "Mast 0/100",
                        "", "", "1.38 Blöcke", "14.46 Blöcke/s", "27.5"),
                new AnimalInfoSnapshot("Reitpferd", "Hengst (Männlich)", "0/100", "Mast 0/100",
                        "", "", "1.31 Blöcke", "14.39 Blöcke/s", "22.5"),
                new AnimalInfoSnapshot("Packesel", "Stute (Weiblich)", "0/100", "Mast 0/100",
                        "", "", "1.18 Blöcke", "5.42 Blöcke/s", "10"),
                new AnimalInfoSnapshot("Packesel", "Hengst (Männlich)", "0/100", "Mast 0/100",
                        "", "", "1.03 Blöcke", "4.82 Blöcke/s", "10"),
                new AnimalInfoSnapshot("Rotvieh Rind", "Bulle (Männlich)", "0/80", "Mast 0/80",
                        "", "Nur weiblich"),
                new AnimalInfoSnapshot("Zaupel", "Schaf (Weiblich)", "0/30", "Mast 0/30",
                        "Mast 0/20", ""),
                new AnimalInfoSnapshot("Zaupel", "Bock (Männlich)", "0/30", "Mast 0/30",
                        "Mast 0/20", ""),
                new AnimalInfoSnapshot("Hausschwein", "Eber (Männlich)", "0/50", "Mast 0/50", "", ""),
                new AnimalInfoSnapshot("Hausschwein", "Sau (Weiblich)", "0/50", "Mast 0/50", "", ""),
                new AnimalInfoSnapshot("Brakel", "Hahn (Männlich)", "0/20", "Mast 0/20", "", ""),
                new AnimalInfoSnapshot("Brakel", "Henne (Weiblich)", "0/20", "Mast 0/20", "", ""));
    }

    @Test
    void toleratesChangedDecorationsWhitespaceOrderAndAdditionalFields() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertConsumed(collector, "§8━━━━━━━━━━━━━━━━");
        assertConsumed(collector, "\u00A0\t");
        assertConsumed(collector, "§6– Reitpferd");
        assertConsumed(collector, "❤\uFE0F\u00A0\u00A0ZUCHTBEREIT ： Mast 0/100");
        assertConsumed(collector, "🆕 Neuer Status: in 1h");
        assertConsumed(collector, "🐎\tGender: Stute (Weiblich)");
        assertConsumed(collector, "§a❤️ Leben: 27,5");
        assertConsumed(collector, "  Speed  : 14,46 Blöcke/s");
        assertConsumed(collector, "Jump height: 1,38 Blöcke");
        assertConsumed(collector, "Feeding: 0/100");

        assertEquals(new AnimalInfoSnapshot("Reitpferd", "Stute (Weiblich)", "0/100", "Mast 0/100",
                "", "", "1,38 Blöcke", "14,46 Blöcke/s", "27,5"),
                collector.accept("§8━━━━━━━━━━━━━━━━").completed());
    }

    @Test
    void framedLegacyHorseBlockWaitsForDividerAndKeepsStatsAfterBreedingReady() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertConsumed(collector, "----------------");
        assertConsumed(collector, "Pferdeinfo - Reitpferd");
        assertConsumed(collector, "Geschlecht: Hengst (Männlich)");
        assertConsumed(collector, "Zuchtbereit: Ja");
        assertConsumed(collector, "Leben: 23.5");
        assertConsumed(collector, "Geschwindigkeit: 14.66 Blöcke/s");
        assertEquals("14.66 Blöcke/s", collector.accept("----------------").completed().speed());
    }

    @Test
    void shortHeaderRequiresLeadingDividerAndOrdinaryChatPassesInsideBlock() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertFalse(collector.accept("- Reitpferd").consumed());
        assertFalse(collector.accept("⚥ Geschlecht: Hengst (Männlich)").consumed());
        assertConsumed(collector, "----------------");
        assertConsumed(collector, "- Brakel");
        assertFalse(collector.accept("[Spieler] Hallo zusammen!").consumed());
        assertConsumed(collector, "⚥ Geschlecht: Hahn (Männlich)");
        assertConsumed(collector, "❤ Paarungsbereit: Mast 0/20");
        assertEquals("Brakel", collector.accept("----------------").completed().animalName());
    }

    @Test
    void unrelatedDividerDoesNotStartAnAnimalBlock() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertConsumed(collector, "----------------");
        assertFalse(collector.accept("Serverankündigung").consumed());
        assertFalse(collector.accept("- Eintrag").consumed());
        assertFalse(collector.accept("Geschlecht: Weiblich").consumed());
        assertNull(collector.accept("----------------").completed());
    }

    @Test
    void unknownOrIncompleteBlockNeverPublishesStaleStats() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertConsumed(collector, "Tierinfo - Zaupel");
        assertConsumed(collector, "Geschlecht: Weiblich");
        assertConsumed(collector, "Mast: 0/30");
        assertConsumed(collector, "Tierinfo - Brakel");
        assertConsumed(collector, "Paarungsbereit: Ja");
        assertNull(collector.accept("----------------").completed());

        collector.start();
        assertConsumed(collector, "----------------");
        assertConsumed(collector, "- Zaupel");
        assertConsumed(collector, "Neue Eigenschaft: 42");
        assertNull(collector.accept("----------------").completed());
    }

    @Test
    void blockLimitAndResetStopConsumingChat() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        assertConsumed(collector, "Tierinfo - Zaupel");
        for (int i = 0; i < 64; i++) {
            assertConsumed(collector, "Neuer Status: " + i);
        }
        assertFalse(collector.accept("Weiterer Status: 65").consumed());
        assertFalse(collector.accept("Geschlecht: Weiblich").consumed());

        collector.start();
        assertConsumed(collector, "Tierinfo - Brakel");
        collector.reset();
        assertFalse(collector.accept("Paarungsbereit: Ja").consumed());
    }

    @Test
    void receivesNewServerBlockInSingleMessage() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        AnimalInfoResponseCollector.Result result = collector.acceptMessage("""
                --------------------------------
                - Reitpferd
                ⚥  Geschlecht: Hengst (Männlich)
                 Leben: 23.5
                 Sprunghöhe: 1.35 Blöcke
                 Geschwindigkeit: 14.66 Blöcke/s
                 Mast: 0/100
                ⏲  Mastverlust: sobald das Tier gefüttert wurde
                ❤ Zuchtbereit: Mast 0/100
                --------------------------------
                """);
        assertTrue(result.consumed());
        assertFalse(result.expectsTrailingDivider());
        assertEquals(new AnimalInfoSnapshot("Reitpferd", "Hengst (Männlich)", "0/100", "Mast 0/100",
                "", "", "1.35 Blöcke", "14.66 Blöcke/s", "23.5"), result.completed());
    }

    @Test
    void mixedMessageRemainsVisibleWhileAnimalStatsAreCollected() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        AnimalInfoResponseCollector.Result result = collector.acceptMessage("""
                ----------------
                - Zaupel
                ⚥ Geschlecht: Schaf (Weiblich)
                [Spieler] Hallo zusammen!
                ❤ Paarungsbereit: Mast 0/30
                ----------------
                """);
        assertFalse(result.consumed());
        assertEquals("Zaupel", result.completed().animalName());
        assertEquals("Schaf (Weiblich)", result.completed().gender());
    }

    @Test
    void legacyUnframedHorseConsumesTrailingDividerOnlyOnce() {
        AnimalInfoResponseCollector collector = new AnimalInfoResponseCollector();
        collector.start();
        AnimalInfoResponseCollector.Result result = collector.acceptMessage("""
                Pferdeinfo - Reitpferd
                Geschlecht: Hengst
                Zuchtbereit: Ja
                ────────────────
                """);
        assertTrue(result.consumed());
        assertFalse(result.expectsTrailingDivider());
        assertEquals("Reitpferd", result.completed().animalName());
        assertFalse(collector.acceptMessage("----------------").consumed());
    }

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
