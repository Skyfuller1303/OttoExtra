package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePolicyTest {
    private static final long START = 1_790_000_000_000L;
    private static final long MINUTE = 60_000L;

    private static ResourceStatus at(String type, Integer custom, long minutes) {
        return ResourcePolicy.status(type, custom, START, START + minutes * MINUTE);
    }

    @Test
    void neverStartedOrResetEntryIsReady() {
        ResourceStatus status = ResourcePolicy.status("IRON_MINE", null, null, START);
        assertEquals("READY", status.status());
        assertEquals(100, status.regenerationPercentage());
        assertEquals(0, status.secondsUntilFull());
    }

    @Test
    void depositsRegenerateAfterOneStageOfTwoHours() {
        for (String type : new String[] {"IRON_DEPOSIT", "COAL_DEPOSIT", "SALT_DEPOSIT"}) {
            ResourceStatus before = at(type, null, 119);
            assertEquals("REGENERATING", before.status());
            assertEquals(0, before.regenerationPercentage());
            assertEquals(60, before.secondsUntilFull());
            assertEquals(60, before.secondsUntilNextStage());
            assertEquals("READY", at(type, null, 120).status());
        }
    }

    @Test
    void minesReachSeventyPercentAfterTwoHoursAndAreFullAfterFour() {
        for (String type : new String[] {"IRON_MINE", "COAL_MINE", "SALT_MINE"}) {
            ResourceStatus start = at(type, null, 0);
            assertEquals(0, start.regenerationPercentage());
            assertEquals(120 * 60, start.secondsUntilNextStage());
            assertEquals(240 * 60, start.secondsUntilFull());
            assertEquals(0, at(type, null, 119).regenerationPercentage());
            ResourceStatus partial = at(type, null, 120);
            assertEquals("REGENERATING", partial.status());
            assertEquals(70, partial.regenerationPercentage());
            assertEquals(120 * 60, partial.secondsUntilFull());
            assertEquals(70, at(type, null, 239).regenerationPercentage());
            assertEquals("READY", at(type, null, 240).status());
        }
    }

    @Test
    void harvestMayStartAtSeventyPercentOnlyForMines() {
        assertFalse(ResourcePolicy.canStartDepletion("IRON_MINE", "REGENERATING", 69));
        assertTrue(ResourcePolicy.canStartDepletion("IRON_MINE", "REGENERATING", 70));
        assertTrue(ResourcePolicy.canStartDepletion("COAL_DEPOSIT", "READY", 100));
        assertFalse(ResourcePolicy.canStartDepletion("COAL_DEPOSIT", "REGENERATING", 70));
        assertFalse(ResourcePolicy.canStartDepletion("IRON_MINE", "MARKER", 100));
        assertTrue(ResourcePolicy.canForceRestart("IRON_MINE", "REGENERATING"));
        assertTrue(ResourcePolicy.canForceRestart("SALT_DEPOSIT", "REGENERATING"));
        assertFalse(ResourcePolicy.canForceRestart("PEAT_BOG", "REGENERATING"));
        assertFalse(ResourcePolicy.canForceRestart("IRON_MINE", "READY"));
    }

    @Test
    void gardenAndPeatBogTakeFourHours() {
        for (String type : new String[] {"MONASTERY_GARDEN", "PEAT_BOG"}) {
            assertEquals("REGENERATING", at(type, null, 239).status());
            assertEquals("READY", at(type, null, 240).status());
        }
    }

    @Test
    void apiaryAndClayPitTakeThirtyMinutes() {
        for (String type : new String[] {"APIARY", "CLAY_PIT"}) {
            assertEquals("REGENERATING", at(type, null, 29).status());
            assertEquals("READY", at(type, null, 30).status());
        }
    }

    @Test
    void markerHasNoTimer() {
        ResourceStatus status = ResourcePolicy.status("MARKER", null, START, START + MINUTE);
        assertEquals("MARKER", status.status());
        assertEquals(100, status.regenerationPercentage());
        assertEquals(0, ResourcePolicy.fullMinutes("MARKER", null));
    }

    @Test
    void customMinutesReplaceThePolicyWithOneStage() {
        assertEquals("REGENERATING", at("IRON_MINE", 10, 9).status());
        assertEquals(0, at("IRON_MINE", 10, 9).regenerationPercentage());
        assertEquals("READY", at("IRON_MINE", 10, 10).status());
        assertEquals(10, ResourcePolicy.fullMinutes("CLAY_PIT", 10));
        assertEquals(240, ResourcePolicy.fullMinutes("IRON_MINE", null));
    }

    @Test
    void stagesAreReachedAfterWholeElapsedMinutes() {
        long almost = START + 120 * MINUTE - 1;
        assertEquals(0, ResourcePolicy.status("IRON_MINE", null, START, almost).regenerationPercentage());
        assertEquals(1, ResourcePolicy.status("IRON_MINE", null, START, almost).secondsUntilNextStage());
    }

    @Test
    void futureStartCountsAsJustStarted() {
        ResourceStatus status = ResourcePolicy.status("APIARY", null, START + 5 * MINUTE, START);
        assertEquals("REGENERATING", status.status());
        assertEquals(35 * 60, status.secondsUntilFull());
    }

    @Test
    void unknownTypeIsNeitherReadyNorRegenerating() {
        assertEquals("UNKNOWN", at("NOPE", null, 1).status());
        assertFalse(ResourcePolicy.known("NOPE"));
    }
}
