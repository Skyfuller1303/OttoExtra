package de.ottoextra.playerlist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListSortingServiceTest {

    @Test
    void retriesOnlyMissingEnabledSortingDataAfterInterval() {
        long dueAt = PlayerListSortingService.EMPTY_RETRY_INTERVAL_MS;

        assertTrue(PlayerListSortingService.refreshDue(
                true, true, true, false, dueAt, dueAt));
        assertFalse(PlayerListSortingService.refreshDue(
                false, true, true, false, dueAt, dueAt));
        assertFalse(PlayerListSortingService.refreshDue(
                true, false, true, false, dueAt, dueAt));
        assertFalse(PlayerListSortingService.refreshDue(
                true, true, false, false, dueAt, dueAt));
        assertFalse(PlayerListSortingService.refreshDue(
                true, true, true, true, dueAt, dueAt));
        assertFalse(PlayerListSortingService.refreshDue(
                true, true, true, false, dueAt - 1, dueAt));
    }
}
