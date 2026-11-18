package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ResourceMapStatusTest {
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");

    private static ResourceSnapshot resource(String type, String status, int percentage, String full, int seconds) {
        return new ResourceSnapshot("mine", "Eisenmine", type, "Nord", "minecraft:overworld",
                ResourceZone.point(1, 64, 2, 3), status, percentage, "", full, seconds, seconds);
    }

    @Test
    void readyAndDisabledStatesTakePrecedenceOverTimestamps() {
        assertEquals("Bereit", ResourceMapStatus.label(resource("IRON_MINE", "READY", 100, "", -1), NOW));
        assertEquals("Deaktiviert", ResourceMapStatus.label(resource("IRON_MINE", "DISABLED", 0,
                "2026-09-06T10:00:00Z", 0), NOW));
    }

    @Test
    void countdownUsesAbsoluteTimeAndKeepsAdvancingWithoutResync() {
        var resource = resource("IRON_MINE", "REGENERATING", 0, "2026-09-07T14:15:00+02:00", 99_999);
        assertEquals("Bereit in 2 h 15 min", ResourceMapStatus.label(resource, NOW));
        assertEquals("Bereit in 2 h 14 min", ResourceMapStatus.label(resource, NOW.plusSeconds(60)));
        assertEquals("Bereit", ResourceMapStatus.label(resource, NOW.plusSeconds(8_100)));
    }

    @Test
    void partialMinesAreHarvestableButOtherResourcesMustFinishRegenerating() {
        assertEquals("Abbaubar · voll in 1 h 0 min", ResourceMapStatus.label(resource(
                "IRON_MINE", "REGENERATING", 70, "", 3_600), NOW));
        assertEquals("Bereit in 1 h 0 min", ResourceMapStatus.label(resource(
                "IRON_DEPOSIT", "REGENERATING", 70, "", 3_600), NOW));
    }

    @Test
    void absentOrInvalidDeadlinesUseReportedTimeAndUnknownIsNotReady() {
        assertEquals("Bereit in 2 min", ResourceMapStatus.label(resource("CLAY_PIT", "REGENERATING", 0, "bad", 61), NOW));
        assertEquals("Regeneration (0 %)", ResourceMapStatus.label(resource("CLAY_PIT", "REGENERATING", 0, "", -1), NOW));
        assertEquals("Abbaubar (70 %)", ResourceMapStatus.label(resource("IRON_MINE", "REGENERATING", 70, "", -1), NOW));
    }

    @Test
    void finalFractionOfSecondNeverPrematurelyShowsReady() {
        assertEquals("Bereit in 1 min", ResourceMapStatus.label(resource("CLAY_PIT", "REGENERATING", 0,
                "2026-09-07T10:00:00.500Z", 0), NOW));
    }
}
