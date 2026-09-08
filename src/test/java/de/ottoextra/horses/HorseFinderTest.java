package de.ottoextra.horses;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorseFinderTest {

    @AfterEach
    void resetFinder() {
        HorseFinder.reset();
    }

    @Test
    void localSelectionIsActiveBeforeApiRoundTrip() {
        UUID horse = UUID.randomUUID();

        HorseFinder.select(horse).join();

        assertTrue(HorseFinder.isTarget(horse));
        assertFalse(HorseFinder.isTarget(UUID.randomUUID()));
    }

    @Test
    void togglingActiveHorseStopsSearch() {
        UUID horse = UUID.randomUUID();
        HorseFinder.select(horse).join();

        HorseFinder.toggle(horse).join();

        assertFalse(HorseFinder.isTarget(horse));
    }

    @Test
    void multipleHorsesCanBeSelectedAndRemovedIndividually() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        HorseFinder.select(first).join();
        HorseFinder.select(second).join();

        assertTrue(HorseFinder.isTarget(first));
        assertTrue(HorseFinder.isTarget(second));
        HorseFinder.remove(first).join();
        assertFalse(HorseFinder.isTarget(first));
        assertTrue(HorseFinder.isTarget(second));
    }
}
