package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HorseObservationCacheTest {
    @AfterEach
    void clear() { HorseRegistry.shutdown(); }

    @Test
    void cacheKeepsMostRecentlyObservedAnimalsWithinItsLimit() {
        HorseRegistry.initialize(null, () -> true, true);
        for (int i = 0; i < HorseRegistry.MAX_OBSERVATIONS; i++) observe(i, "Weiblich", i);
        observe(0, "Männlich", 10_000);
        observe(HorseRegistry.MAX_OBSERVATIONS, "Weiblich", 20_000);
        assertEquals("Männlich", HorseRegistry.observation(id(0)).orElseThrow().snapshot().gender());
        assertTrue(HorseRegistry.observation(id(1)).isEmpty(), "Evict oldest observation, not a refreshed entry");
        int retained = 0;
        for (int i = 0; i <= HorseRegistry.MAX_OBSERVATIONS; i++) {
            if (HorseRegistry.observation(id(i)).isPresent()) retained++;
        }
        assertEquals(HorseRegistry.MAX_OBSERVATIONS, retained);
        assertTrue(HorseRegistry.name(id(0)).isEmpty());
        assertTrue(HorseRegistry.names().isEmpty());
        assertTrue(HorseRegistry.databaseRecords().isEmpty());
    }

    @Test
    void observationsStaySeparateRejectOlderOrIncompleteDataAndClearOnDisconnect() {
        HorseRegistry.initialize(null, () -> true, true);
        observe(1, "Weiblich", 2_000);
        observe(2, "Männlich", 2_000);
        observe(1, "Männlich", 1_000);
        HorseRegistry.observe(new AnimalInfoObservation(id(1),
                new AnimalInfoSnapshot("Kuh", "", ""), Instant.ofEpochMilli(3_000)));
        assertEquals("Weiblich", HorseRegistry.observation(id(1)).orElseThrow().snapshot().gender());
        assertEquals("Männlich", HorseRegistry.observation(id(2)).orElseThrow().snapshot().gender());
        HorseRegistry.clearConnection();
        assertTrue(HorseRegistry.observation(id(1)).isEmpty());
        assertTrue(HorseRegistry.observation(id(2)).isEmpty());
    }

    private static void observe(int id, String gender, long time) {
        HorseRegistry.observe(new AnimalInfoObservation(id(id),
                new AnimalInfoSnapshot("Kuh", gender, "Ja"), Instant.ofEpochMilli(time)));
    }

    private static UUID id(int id) { return new UUID(0, id + 1L); }
}
