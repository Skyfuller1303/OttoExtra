package de.ottoextra.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePickupTrackerTest {
    @TempDir Path directory;
    private static final ResourceActionbarTracker.Target MINE =
            new ResourceActionbarTracker.Target("mine", "Mine", "IRON_MINE", "overworld", "Nord");

    private ResourceStatistics statistics() { return new ResourceStatistics(directory.resolve("stats.json")); }
    private static ResourceActionbarTracker.Observation message(String text) {
        return new ResourceActionbarTracker.Observation(MINE, text);
    }

    @Test
    void actionbarThenPickupIsOneConfirmedSuccess() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        tracker.actionbar(stats, message("Gefunden"), 1_000);
        tracker.pickup(stats, MINE, 10, 1_300);
        tracker.pickup(stats, MINE, 10, 1_400);
        assertEquals(1, stats.totals(null).successes());
        assertEquals(1, stats.totals(null).attempts());
        assertEquals(0, stats.totals(null).unknown());
        assertEquals("Gefunden", stats.rows(null).getFirst().message());
    }

    @Test
    void pickupBeforeActionbarIsMergedInsteadOfCountedTwice() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        tracker.pickup(stats, MINE, 10, 1_000);
        assertEquals(1, stats.totals(null).successes());
        tracker.actionbar(stats, message("Gefunden"), 1_200);
        assertEquals(1, stats.totals(null).successes());
        assertEquals(0, stats.totals(null).unknown());
        assertEquals("Gefunden", stats.rows(null).getFirst().message());
    }

    @Test
    void classifiedSuccessAndPickupAreNotAddedAndFailuresAreNotOverridden() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        stats.classify("Daneben", ResourceStatistics.Outcome.FAILURE);
        tracker.actionbar(stats, message("Gefunden"), 1_000);
        tracker.actionbar(stats, message("Daneben"), 1_100);
        tracker.pickup(stats, MINE, 10, 1_300);
        assertEquals(1, stats.totals(null).successes());
        assertEquals(1, stats.totals(null).failures());
        assertEquals(2, stats.totals(null).attempts());
    }

    @Test
    void unknownNoticeDoesNotStealPickupFromAKnownSuccess() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        tracker.actionbar(stats, message("Gefunden"), 1_000);
        tracker.actionbar(stats, message("Hinweis"), 1_100);
        tracker.pickup(stats, MINE, 10, 1_300);
        assertEquals(1, stats.totals(null).successes());
        assertEquals(1, stats.totals(null).unknown());
        assertTrue(stats.rows(null).stream().filter(row -> row.message().equals("Gefunden")).findFirst().orElseThrow().pickupConfirmed());
    }

    @Test
    void repeatedSuccessMessagesAndSeparatePickupsRemainSeparateAttempts() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        for (int attempt = 0; attempt < 5; attempt++) {
            tracker.actionbar(stats, message("Gefunden"), attempt * 500L);
            tracker.pickup(stats, MINE, attempt, attempt * 500L + 100);
        }
        assertEquals(5, stats.totals(null).successes());
        assertEquals(0, stats.totals(null).unknown());
        assertEquals(5, stats.rows(null).getFirst().count());
    }

    @Test
    void twoPickedUpSuccessesAndEightFailuresProduceTwentyPercentInRunTable() {
        var stats = statistics();
        var actionbars = new ResourceActionbarTracker();
        var pickups = new ResourcePickupTracker();
        stats.classify("Daneben", ResourceStatistics.Outcome.FAILURE);
        for (int attempt = 0; attempt < 10; attempt++) {
            long now = 1_000 + attempt * 1_000L;
            actionbars.interact(MINE, now);
            var result = actionbars.receive(attempt < 2 ? "Gefunden" : "Daneben", "overworld", now + 1);
            assertNotNull(result);
            pickups.actionbar(stats, result, now + 1);
            if (attempt < 2) pickups.pickup(stats, MINE, attempt, now + 100);
        }
        stats.finishRun();
        assertTrue(stats.save());
        var reloaded = statistics();
        assertEquals(1, reloaded.runs(null).size());
        var totals = reloaded.runs(null).getFirst().totals();
        assertEquals(2, totals.successes());
        assertEquals(8, totals.failures());
        assertEquals(10, totals.attempts());
        assertEquals(20.0, totals.successRate(), 0.0001);
        assertEquals(0, totals.unknown());
    }

    @Test
    void differentRegionsAndExpiredPacketsCannotBeMerged() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        tracker.actionbar(stats, message("Alt"), 1_000);
        var south = new ResourceActionbarTracker.Target("mine", "Mine", "IRON_MINE", "overworld", "Süd");
        tracker.pickup(stats, south, 10, 1_100);
        tracker.actionbar(stats, new ResourceActionbarTracker.Observation(south, "Später"), 4_000);
        assertEquals(1, stats.totals(null, "Nord").unknown());
        assertEquals(1, stats.totals(null, "Süd").successes());
        assertEquals(1, stats.totals(null, "Süd").unknown());
    }

    @Test
    void resetPreventsMatchingAnEarlierRunAndAllowsReusedEntityIds() {
        var stats = statistics();
        var tracker = new ResourcePickupTracker();
        tracker.pickup(stats, MINE, 10, 1_000);
        tracker.reset();
        stats.finishRun();
        tracker.actionbar(stats, message("Neu"), 1_100);
        assertEquals(1, stats.totals(null).unknown());
        tracker.pickup(stats, MINE, 10, 1_200);
        assertEquals(2, stats.totals(null).successes());
        assertEquals(2, stats.runs(null).size());
    }
}
