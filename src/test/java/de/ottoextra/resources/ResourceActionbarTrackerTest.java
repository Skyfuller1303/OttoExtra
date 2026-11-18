package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceActionbarTrackerTest {
    private static final ResourceActionbarTracker.Target MINE =
            new ResourceActionbarTracker.Target("mine", "Eisenmine", "IRON_MINE", "overworld");

    @Test
    void consecutiveEqualPacketsCountDuringContinuousMining() {
        var tracker = new ResourceActionbarTracker();
        tracker.interact(MINE, 0);
        assertNotNull(tracker.receive("§aErfolg!", "overworld", 1));
        for (long now = 2; now < 20_000; now += 100) {
            tracker.interact(MINE, now);
            var result = tracker.receive("  Erfolg!\u00a0", "overworld", now);
            assertNotNull(result);
            assertEquals("Erfolg!", result.message());
            assertEquals(MINE, result.target());
        }
    }

    @Test
    void resultPacketsInSameTickCountSeparatelyAndEmptyPacketsDoNotCount() {
        var tracker = new ResourceActionbarTracker();
        tracker.interact(MINE, 0);
        assertNotNull(tracker.receive("Erfolg", "overworld", 1));
        assertNotNull(tracker.receive("Erfolg", "overworld", 1));
        assertNotNull(tracker.receive("Fehlschlag", "overworld", 2));
        assertNotNull(tracker.receive("Fehlschlag", "overworld", 2));
        assertNotNull(tracker.receive("Erfolg", "overworld", 3));
        assertNull(tracker.receive("", "overworld", 4));
        assertNotNull(tracker.receive("Erfolg", "overworld", 5));
    }

    @Test
    void noInteractionExpiredContextAndOtherDimensionsDoNotCount() {
        var tracker = new ResourceActionbarTracker();
        assertNull(tracker.receive("Vorher", "overworld", 0));
        tracker.interact(MINE, 1);
        assertNull(tracker.receive("Andere Welt", "nether", 2));
        assertNull(tracker.receive("Zu spät", "overworld", 5_002));
        assertNull(tracker.receive("Uhr zurück", "overworld", 0));
    }

    @Test
    void equalTextBeforeMiningDoesNotSuppressANewResultPacket() {
        var tracker = new ResourceActionbarTracker();
        assertNull(tracker.receive("Erfolg", "overworld", 0));
        tracker.interact(MINE, 1);
        assertNotNull(tracker.receive("Erfolg", "overworld", 2));
    }

    @Test
    void reconnectRequiresANewInteraction() {
        var tracker = new ResourceActionbarTracker();
        tracker.interact(MINE, 0);
        assertNotNull(tracker.receive("Erfolg", "overworld", 1));
        tracker.reset();
        assertNull(tracker.receive("Fehlschlag", "overworld", 2));
        tracker.interact(MINE, 3);
        assertNotNull(tracker.receive("Erfolg", "overworld", 4));
    }

    @Test
    void sameResultCountsForNewTargetAndAfterContextExpires() {
        var tracker = new ResourceActionbarTracker();
        tracker.interact(MINE, 0);
        assertNotNull(tracker.receive("Erfolg", "overworld", 1));
        assertNull(tracker.receive("Erfolg", "overworld", 5_001));
        var otherMine = new ResourceActionbarTracker.Target("other", "Andere Mine", "IRON_MINE", "overworld");
        tracker.interact(otherMine, 6_000);
        var result = tracker.receive("Erfolg", "overworld", 6_001);
        assertNotNull(result);
        assertEquals(otherMine, result.target());
    }

    @Test
    void blankAndFormattingOnlyPacketsDoNotCount() {
        var tracker = new ResourceActionbarTracker();
        tracker.interact(MINE, 0);
        assertNull(tracker.receive(null, "overworld", 1));
        assertNull(tracker.receive("§a \u00a0\u200b", "overworld", 2));
    }

    @Test
    void normalizationPreservesNumbersAndWords() {
        assertEquals("Du erhältst 2 Eisen!", ResourceActionbarTracker.normalize("§x§f§f§0§0§0§0Du\n erhältst\u200b 2 Eisen!"));
        assertNotEquals(ResourceActionbarTracker.normalize("1 Eisen"), ResourceActionbarTracker.normalize("2 Eisen"));
    }
}
