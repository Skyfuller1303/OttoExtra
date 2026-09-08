package de.ottoextra.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityRendererTest {
    @Test
    void normalActivityUsesOnePingRing() {
        assertEquals(1, ActivityRenderer.pingRingCount(1));
    }

    @Test
    void veryHighActivityUsesTwoPingRings() {
        assertEquals(2, ActivityRenderer.pingRingCount(2));
        assertEquals(2, ActivityRenderer.pingRingCount(5));
    }

    @Test
    void zoomedGroupsKeepOnlyTheirHighestActivityLevel() {
        assertEquals(2, ActivityRenderer.highestActivityLevel(1, 2));
        assertEquals(2, ActivityRenderer.highestActivityLevel(2, 1));
        assertEquals(2, ActivityRenderer.highestActivityLevel(2, 2));
    }

    @Test
    void veryHighPingsAreThirtyPercentSlowerAndSeventyPercentApart() {
        assertEquals(1_200L, ActivityRenderer.animationDurationMs(1));
        assertEquals(1_714L, ActivityRenderer.animationDurationMs(2));
        assertEquals(0.0f, ActivityRenderer.pingProgress(0L, 1_714L, 0.0f), 0.0001f);
        assertEquals(0.3f, ActivityRenderer.pingProgress(0L, 1_714L, 0.70f), 0.0001f);
        assertEquals(0.0f, ActivityRenderer.pingProgress(1_200L, 1_714L, 0.70f), 0.001f);
        assertEquals(0.0f, ActivityRenderer.pingProgress(1_714L, 1_714L, 0.0f), 0.0001f);
    }
}
