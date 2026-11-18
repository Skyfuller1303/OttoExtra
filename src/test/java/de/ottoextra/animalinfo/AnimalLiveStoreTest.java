package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalLiveStoreTest {

    private static final long NOW = 1_800_000_000_000L;
    private final UUID id = UUID.randomUUID();

    @Test
    void updatesCreateAndChangeEntries() {
        AnimalLiveStore store = new AnimalLiveStore();
        long before = store.revision();
        store.update(id, NOW, state -> state.withLoved(NOW));
        assertEquals(NOW, store.get(id).lovedAt());
        store.update(id, NOW + 1, state -> state.withBred(NOW + 1));
        assertEquals(0, store.get(id).lovedAt());
        assertEquals(NOW + 1, store.get(id).bredAt());
        assertTrue(store.revision() > before);
        assertNull(store.get(UUID.randomUUID()));
        assertNull(store.get(null));
    }

    @Test
    void entriesExpireFourteenDaysAfterTheLastChange() {
        AnimalLiveStore store = new AnimalLiveStore();
        store.update(id, NOW, state -> state.withLoved(NOW));
        assertEquals(1, store.snapshot(NOW + AnimalLiveStore.MAX_AGE_MS - 1).size());
        assertEquals(0, store.snapshot(NOW + AnimalLiveStore.MAX_AGE_MS).size());
        assertNull(store.get(id), "Pruned while snapshotting");
    }

    @Test
    void capEvictsTheOldestEntries() {
        AnimalLiveStore store = new AnimalLiveStore();
        UUID oldest = UUID.randomUUID();
        store.update(oldest, NOW - 100_000, state -> state.withLoved(NOW - 100_000));
        UUID newest = null;
        for (int i = 0; i < AnimalLiveStore.MAX_ENTRIES; i++) {
            newest = UUID.randomUUID();
            long at = NOW - 10_000 + i;
            store.update(newest, at, state -> state.withLoved(at));
        }
        assertEquals(AnimalLiveStore.MAX_ENTRIES, store.size());
        assertNull(store.get(oldest));
        assertNotNull(store.get(newest));
    }

    @Test
    void mergingLoadedEntriesKeepsNewerMemoryAndDropsInvalidOnes() {
        AnimalLiveStore store = new AnimalLiveStore();
        store.update(id, NOW, state -> state.withLoved(NOW));
        AnimalLiveState older = AnimalLiveState.empty(id).withBred(NOW - 5_000);
        UUID other = UUID.randomUUID();
        AnimalLiveState expired = AnimalLiveState.empty(UUID.randomUUID())
                .withBred(NOW - AnimalLiveStore.MAX_AGE_MS - 1);
        AnimalLiveState future = AnimalLiveState.empty(UUID.randomUUID()).withBred(NOW + 60_000);
        store.mergeLoaded(List.of(older, AnimalLiveState.empty(other).withBred(NOW - 1), expired, future), NOW);
        assertEquals(NOW, store.get(id).lovedAt(), "Newer in-memory entry survives");
        assertEquals(NOW - 1, store.get(other).bredAt());
        assertNull(store.get(expired.entityUuid()));
        assertNull(store.get(future.entityUuid()));
        store.mergeLoaded(null, NOW);
    }

    @Test
    void observeStoresTheServerAnswer() {
        AnimalLiveStore store = new AnimalLiveStore();
        store.observe(id, new AnimalInfoSnapshot("Rotvieh Rind", "Weiblich", "65/80",
                "in 3h | Mast 0/80", "", ""), NOW);
        AnimalLiveState state = store.get(id);
        assertEquals("Rotvieh Rind", state.species());
        assertEquals(65, state.mast());
        assertEquals(80, state.mastMax());
        assertEquals(NOW + 3 * 3_600_000L, state.breedWaitUntil());
        assertEquals(NOW, state.infoAt());
        store.observe(id, null, NOW);
        assertSame(state, store.get(id));
    }

    @Test
    void endSessionStartsAnEmptyStoreWithoutTouchingTheOldOne() {
        AnimalLiveStore first = AnimalLiveStore.current();
        first.update(id, NOW, state -> state.withLoved(NOW));
        AnimalLiveStore.endSession();
        assertTrue(AnimalLiveStore.current() != first);
        assertEquals(0, AnimalLiveStore.current().size());
        assertEquals(1, first.size(), "A pending save still sees the old data");
        first.clear();
        assertEquals(0, first.size());
    }
}
