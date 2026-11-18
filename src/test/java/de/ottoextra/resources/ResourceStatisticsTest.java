package de.ottoextra.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ResourceStatisticsTest {
    @TempDir Path directory;

    private static ResourceActionbarTracker.Observation observation(String zone, String message) {
        return new ResourceActionbarTracker.Observation(
                new ResourceActionbarTracker.Target(zone, zone, "IRON_MINE", "overworld"), message);
    }

    @Test
    void consecutiveEqualServerResultsKeepTheirActualRateAcrossRestart() {
        Path file = directory.resolve("consecutive-results.json");
        var stats = new ResourceStatistics(file);
        var tracker = new ResourceActionbarTracker();
        var target = new ResourceActionbarTracker.Target("mine", "Mine", "IRON_MINE", "overworld", "Nord");
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        stats.classify("Daneben", ResourceStatistics.Outcome.FAILURE);

        // Ten server results during continuous mining, without an intervening actionbar clear.
        for (int attempt = 0; attempt < 10; attempt++) {
            long now = attempt * 1_000L;
            tracker.interact(target, now);
            var result = tracker.receive(attempt < 2 ? "Gefunden" : "Daneben", "overworld", now + 1);
            if (result != null) stats.record(result);
        }

        assertEquals(2, stats.totals(null).successes());
        assertEquals(8, stats.totals(null).failures());
        assertEquals(10, stats.totals(null).attempts());
        assertEquals(20.0, stats.totals(null).successRate(), 0.0001);
        assertTrue(stats.save());
        var reloaded = new ResourceStatistics(file);
        assertEquals(stats.totals(null), reloaded.totals("mine", "Nord"));
    }

    @Test
    void classificationIsRetroactiveAndUnknownDoesNotAffectRate() {
        var stats = new ResourceStatistics(directory.resolve("stats.json"));
        stats.record(observation("mine", "Gefunden"));
        stats.record(observation("mine", "Gefunden"));
        stats.record(observation("mine", "Daneben"));
        stats.record(observation("mine", "Hinweis"));
        assertEquals(0, stats.totals(null).attempts());
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        stats.classify("Daneben", ResourceStatistics.Outcome.FAILURE);
        assertEquals(2, stats.totals(null).successes());
        assertEquals(1, stats.totals(null).failures());
        assertEquals(1, stats.totals(null).unknown());
        assertEquals(200.0 / 3, stats.totals(null).successRate(), 0.0001);
        stats.classify("Hinweis", ResourceStatistics.Outcome.IGNORED);
        assertEquals(1, stats.totals(null).ignored());
        assertEquals(3, stats.totals(null).attempts());
    }

    @Test
    void persistenceKeepsCountsFiltersAndClassificationAcrossRestart() {
        Path file = directory.resolve("stats.json");
        var stats = new ResourceStatistics(file);
        stats.record(observation("north", "Gefunden"));
        stats.record(observation("south", "Gefunden"));
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        assertTrue(stats.save());
        var reloaded = new ResourceStatistics(file);
        assertEquals(2, reloaded.totals(null).successes());
        assertEquals(1, reloaded.totals("north").successes());
        assertEquals(1, reloaded.rows("south").size());
        reloaded.record(observation("north", "Gefunden"));
        assertEquals(2, reloaded.rows("north").getFirst().count());
        assertNull(reloaded.storageError());
    }

    @Test
    void regionsCombineDifferentResourcesAndKeepUnmappedBlocksSeparateAcrossRegions() {
        Path file = directory.resolve("regions.json");
        var stats = new ResourceStatistics(file);
        stats.record(new ResourceActionbarTracker.Observation(new ResourceActionbarTracker.Target(
                "mine1", "Mine 1", "IRON_MINE", "overworld", "Nord"), "Gefunden"));
        stats.record(new ResourceActionbarTracker.Observation(new ResourceActionbarTracker.Target(
                "mine2", "Mine 2", "IRON_MINE", "overworld", "Nord"), "Daneben"));
        stats.record(new ResourceActionbarTracker.Observation(new ResourceActionbarTracker.Target(
                "block:iron", "Eisenerz", "IRON_DEPOSIT", "overworld", "Nord"), "Gefunden"));
        stats.record(new ResourceActionbarTracker.Observation(new ResourceActionbarTracker.Target(
                "block:iron", "Eisenerz", "IRON_DEPOSIT", "overworld", "Süd"), "Gefunden"));
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        stats.classify("Daneben", ResourceStatistics.Outcome.FAILURE);
        var reloaded = new ResourceStatistics(file);
        assertEquals(2, reloaded.regions(null).size());
        assertEquals(2, reloaded.totals(null, "Nord").successes());
        assertEquals(1, reloaded.totals(null, "Nord").failures());
        assertEquals(200.0 / 3, reloaded.totals(null, "Nord").successRate(), 0.0001);
        assertEquals(100, reloaded.totals(null, "Süd").successRate());
        assertEquals(1, reloaded.totals("block:iron", "Nord").successes());
        assertEquals(1, reloaded.rows("block:iron", "Süd").size());
    }

    @Test
    void olderEntriesWithoutRegionRemainInUnknownRegion() throws Exception {
        Path file = directory.resolve("old.json");
        Files.writeString(file, """
                {"version":1,"entries":[{"resourceId":"mine","resourceName":"Mine","type":"IRON_MINE",
                "dimension":"overworld","message":"Gefunden","count":3}],"outcomes":{"Gefunden":"SUCCESS"}}
                """);
        var stats = new ResourceStatistics(file);
        assertEquals("", stats.regions(null).getFirst().region());
        assertEquals(3, stats.totals(null, "").successes());
        assertNull(stats.storageError());
    }

    @Test
    void corruptedOrFutureFilesAreNeverOverwritten() throws Exception {
        for (String original : new String[]{"not json", "null", "{\"version\":3}", "{\"entries\":[null]}",
                "{\"version\":2,\"runs\":[null]}", "{\"version\":2,\"runs\":null}"}) {
            Path file = directory.resolve("stats.json");
            Files.writeString(file, original);
            var stats = new ResourceStatistics(file);
            stats.record(observation("mine", "Gefunden"));
            assertFalse(stats.save());
            assertNotNull(stats.storageError());
            assertEquals(original, Files.readString(file));
        }
    }

    @Test
    void runsSeparateCompletionMineSwitchAndIdleTimeAndShowNewestFirst() {
        var stats = new ResourceStatistics(directory.resolve("runs.json"));
        var north = new ResourceActionbarTracker.Target("north", "Nordmine", "IRON_MINE", "overworld", "Nord");
        var south = new ResourceActionbarTracker.Target("south", "Südmine", "IRON_MINE", "overworld", "Süd");
        stats.interact(north, 1_000);
        stats.record(new ResourceActionbarTracker.Observation(north, "Gefunden"), 1_500);
        stats.interact(north, 2_000);
        assertEquals(1, stats.runs(null).size());
        assertTrue(stats.runs(null).getFirst().active());
        assertEquals(1_000, stats.runs(null).getFirst().startedAt());
        stats.finishRun();
        assertFalse(stats.runs(null).getFirst().active());
        assertEquals(2_000, stats.runs(null).getFirst().endedAt());
        stats.record(new ResourceActionbarTracker.Observation(north, "Daneben"), 3_000);
        stats.interact(south, 4_000);
        assertEquals(3, stats.runs(null).size());
        assertEquals("Südmine", stats.runs(null).getFirst().resourceName());
        assertEquals(2, stats.runs("north").size());
        stats.tick(4_000 + ResourceStatistics.RUN_IDLE_MILLIS);
        assertFalse(stats.runs(null).getFirst().active());
        stats.interact(south, 5_000 + ResourceStatistics.RUN_IDLE_MILLIS);
        assertEquals(4, stats.runs(null).size());
    }

    @Test
    void restartingClosesSavedRunAndKeepsPerRunClassification() {
        Path file = directory.resolve("run-restart.json");
        var stats = new ResourceStatistics(file);
        stats.record(observation("mine", "Gefunden"), 1_000);
        String firstId = stats.runs(null).getFirst().id();
        stats.classify("Gefunden", ResourceStatistics.Outcome.SUCCESS);
        var reloaded = new ResourceStatistics(file);
        assertFalse(reloaded.runs(null).getFirst().active());
        reloaded.record(observation("mine", "Gefunden"), 2_000);
        assertEquals(2, reloaded.runs(null).size());
        assertNotEquals(firstId, reloaded.runs(null).getFirst().id());
        assertEquals(1, reloaded.rowsForRun(firstId, "mine").getFirst().count());
        assertEquals(1, reloaded.runs(null).getFirst().totals().successes());
        assertEquals(2, reloaded.totals(null).successes());
    }

    @Test
    void legacyMigrationPreservesCountsAndOriginalFileBackupWithoutInventingRuns() throws Exception {
        Path file = directory.resolve("legacy.json");
        String original = """
                {"version":1,"entries":[{"resourceId":"mine","resourceName":"Mine","type":"IRON_MINE",
                "dimension":"overworld","message":"Gefunden","count":3}],"outcomes":{"Gefunden":"SUCCESS"}}
                """;
        Files.writeString(file, original);
        var stats = new ResourceStatistics(file);
        assertEquals(ResourceStatistics.LEGACY_RUN_ID, stats.runs(null).getFirst().id());
        stats.record(observation("mine", "Gefunden"), 1_000);
        assertTrue(stats.save());
        assertTrue(stats.save());
        try (var files = Files.list(directory)) {
            var backups = files.filter(path -> path.getFileName().toString().endsWith(".bak")).toList();
            assertEquals(1, backups.size());
            assertEquals(original, Files.readString(backups.getFirst()));
        }
        var reloaded = new ResourceStatistics(file);
        assertNull(reloaded.storageError());
        assertEquals(4, reloaded.totals(null).successes());
        assertEquals(2, reloaded.runs(null).size());
        assertEquals(1, reloaded.runs(null).getFirst().totals().successes());
        assertEquals(3, reloaded.runs(null).getLast().totals().successes());
        assertEquals(ResourceStatistics.LEGACY_RUN_ID, reloaded.runs(null).getLast().id());
    }

    @Test
    void pickupConfirmationChangesOnlyOneOccurrenceAndSurvivesReclassification() {
        Path file = directory.resolve("pickup-confirmed.json");
        var stats = new ResourceStatistics(file);
        var confirmed = stats.record(observation("mine", "Ergebnis"), 1_000);
        stats.record(observation("mine", "Ergebnis"), 2_000);
        stats.confirmPickup(confirmed);
        stats.confirmPickup(confirmed);
        stats.classify("Ergebnis", ResourceStatistics.Outcome.FAILURE);
        assertEquals(1, stats.totals(null).successes());
        assertEquals(1, stats.totals(null).failures());
        assertEquals(2, stats.totals(null).attempts());
        var reloaded = new ResourceStatistics(file);
        assertEquals(stats.totals(null), reloaded.totals(null));
        assertEquals(1, reloaded.rows(null).stream().filter(ResourceStatistics.Row::pickupConfirmed).count());
    }
}
