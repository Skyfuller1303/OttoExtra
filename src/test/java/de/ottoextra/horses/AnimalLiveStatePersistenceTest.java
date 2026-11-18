package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.animalinfo.AnimalLiveState;
import de.ottoextra.animalinfo.AnimalLiveStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalLiveStatePersistenceTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1_800_000_000_000L);
    private final UUID uuid = UUID.randomUUID();
    private Path file() { return directory.resolve("cache.json"); }

    private AnimalIdentityCache cache(AnimalLiveStore store) {
        return new AnimalIdentityCache(file(), "server-a", now::get, store);
    }

    private static AnimalIdentityCache.Identity identity(UUID uuid, long at) {
        return new AnimalIdentityCache.Identity(uuid, "Rotvieh Rind", "Weiblich", at);
    }

    @Test
    void timersSurviveARestart() {
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        cache.remember(identity(uuid, now.get()));
        store.observe(uuid, new AnimalInfoSnapshot("Rotvieh Rind", "Weiblich", "65/80",
                "in 3h | Mast 0/80", "in 5h", ""), now.get());
        store.update(uuid, now.get(), state -> state.withBred(now.get()));
        cache.saveIfDirty();

        AnimalLiveStore restoredStore = new AnimalLiveStore();
        var restored = cache(restoredStore);
        restored.load();
        AnimalLiveState state = restoredStore.get(uuid);
        assertNotNull(state);
        assertEquals("Rotvieh Rind", state.species());
        assertEquals(65, state.mast());
        assertEquals(80, state.mastMax());
        assertEquals(now.get() + 3 * 3_600_000L, state.breedWaitUntil());
        assertEquals(now.get() + 5 * 3_600_000L, state.shearUntil());
        assertEquals(now.get(), state.bredAt());
        assertEquals(now.get(), state.infoAt());
        assertEquals("Weiblich", restored.find(uuid).gender());
    }

    @Test
    void versionOneFileLoadsWithoutTimersAndIsUpgradedOnNextSave() throws Exception {
        Files.writeString(file(), """
                {"version":1,"server":"server-a","animals":[
                {"entityUuid":"%s","animalType":"Rotvieh Rind","gender":"Weiblich","observedAt":%d}]}
                """.formatted(uuid, now.get() - 1_000));
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        cache.load();
        assertEquals("Weiblich", cache.find(uuid).gender());
        assertEquals(0, store.size());

        store.update(uuid, now.get(), state -> state.withLoved(now.get()));
        cache.saveIfDirty();
        String json = Files.readString(file());
        assertTrue(json.contains("\"version\": 2"), json);

        AnimalLiveStore again = new AnimalLiveStore();
        var reloaded = cache(again);
        reloaded.load();
        assertEquals("Weiblich", reloaded.find(uuid).gender());
        assertEquals(now.get(), again.get(uuid).lovedAt());
    }

    @Test
    void unknownFutureVersionIsIgnored() throws Exception {
        Files.writeString(file(), "{\"version\":3,\"server\":\"server-a\",\"animals\":[],\"live\":[]}");
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        cache.load();
        assertEquals(0, store.size());
    }

    @Test
    void timersExpireFourteenDaysAfterTheirLastChangeAndReadsDoNotExtendIt() {
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        store.update(uuid, now.get(), state -> state.withBred(now.get()));
        cache.saveIfDirty();
        long changed = now.get();

        now.addAndGet(AnimalLiveStore.MAX_AGE_MS - 1);
        AnimalLiveStore fresh = new AnimalLiveStore();
        cache(fresh).load();
        assertEquals(changed, fresh.get(uuid).updatedAt());
        assertNotNull(fresh.get(uuid));

        now.incrementAndGet();
        AnimalLiveStore expired = new AnimalLiveStore();
        cache(expired).load();
        assertNull(expired.get(uuid));
    }

    @Test
    void sizeCapAppliesToTimersOnDisk() {
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        UUID oldest = UUID.randomUUID();
        store.update(oldest, now.get() - 100_000, state -> state.withLoved(now.get() - 100_000));
        UUID newest = null;
        for (int i = 0; i < AnimalLiveStore.MAX_ENTRIES; i++) {
            newest = UUID.randomUUID();
            long at = now.get() - 10_000 + i;
            store.update(newest, at, state -> state.withLoved(at));
        }
        cache.saveIfDirty();
        AnimalLiveStore reloaded = new AnimalLiveStore();
        cache(reloaded).load();
        assertEquals(AnimalLiveStore.MAX_ENTRIES, reloaded.size());
        assertNull(reloaded.get(oldest));
        assertNotNull(reloaded.get(newest));
    }

    @Test
    void timersOfOtherServersAndCorruptEntriesAreIgnored() throws Exception {
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        store.update(uuid, now.get(), state -> state.withLoved(now.get()));
        cache.saveIfDirty();
        AnimalLiveStore other = new AnimalLiveStore();
        new AnimalIdentityCache(file(), "server-b", now::get, other).load();
        assertEquals(0, other.size());

        Files.writeString(file(), "{\"version\":2,\"server\":\"server-a\",\"animals\":[],\"live\":[{\"entityUuid\":\""
                + uuid + "\",\"updatedAt\":" + (now.get() + 5_000) + "}, null]}");
        AnimalLiveStore corrupt = new AnimalLiveStore();
        cache(corrupt).load();
        assertEquals(0, corrupt.size(), "Future timestamps and null entries are dropped");
    }

    @Test
    void forgetAllClearsIdentitiesAndTimersAndTheFile() throws Exception {
        AnimalLiveStore store = new AnimalLiveStore();
        var cache = cache(store);
        cache.remember(identity(uuid, now.get()));
        store.update(uuid, now.get(), state -> state.withLoved(now.get()));
        cache.saveIfDirty();
        cache.forgetAll();
        cache.saveIfDirty();
        assertNull(cache.find(uuid));
        assertEquals(0, store.size());
        AnimalLiveStore reloaded = new AnimalLiveStore();
        var restored = cache(reloaded);
        restored.load();
        assertNull(restored.find(uuid));
        assertEquals(0, reloaded.size());
        assertFalse(Files.readString(file()).contains(uuid.toString()));
    }
}
