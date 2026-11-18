package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetaliationMarkersTest {
    @AfterEach void clearMarkers() {
        RetaliationMarkers.clear();
    }

    @Test void markerExpiresAfterThirtySeconds() {
        UUID player = UUID.randomUUID();
        RetaliationMarkers.mark(player, 1_000L);

        assertTrue(RetaliationMarkers.isMarked(player, 1_000L));
        assertTrue(RetaliationMarkers.isMarked(player, 30_999L));
        assertFalse(RetaliationMarkers.isMarked(player, 31_000L));
    }

    @Test void clearRemovesEveryMarker() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        RetaliationMarkers.mark(first, 0L);
        RetaliationMarkers.mark(second, 0L);

        RetaliationMarkers.clear();

        assertFalse(RetaliationMarkers.isMarked(first, 1L));
        assertFalse(RetaliationMarkers.isMarked(second, 1L));
    }
}
