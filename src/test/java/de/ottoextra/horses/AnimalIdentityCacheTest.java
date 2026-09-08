package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AnimalIdentityCacheTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1_800_000_000_000L);
    private final UUID uuid = UUID.randomUUID();

    @AfterEach void stop() { HorseRegistry.shutdown(); }

    @Test
    void reconnectRestoresOnlyDisplayIdentityAndCannotSaveStaleBreedingValues() throws Exception {
        var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> {
                    throw new AssertionError("Unexpected request " + method.getName());
                });
        Path file = directory.resolve("animals.json");
        HorseRegistry.initialize(api, () -> true, true, now::get);
        HorseRegistry.openIdentityCache(file, "server-a", true).join();
        HorseRegistry.observe(new AnimalInfoObservation(uuid, new AnimalInfoSnapshot(
                "Rotvieh Rind", "Weiblich", "Voll", "Ja", "Ja", "Ja", "1.5", "14", "20"), Instant.ofEpochMilli(now.get())));
        HorseRegistry.shutdown();
        String json = Files.readString(file);
        assertTrue(json.contains(uuid.toString()));
        for (String value : new String[]{"breedingReady", "mast", "milkable", "shearable", "jumpHeight", "speed", "life"}) {
            assertFalse(json.contains(value), value);
        }
        HorseRegistry.initialize(api, () -> true, true, now::get);
        HorseRegistry.openIdentityCache(file, "server-a", true).join();
        assertEquals("Weiblich", HorseRegistry.knownGender(uuid));
        assertEquals("Rotvieh Rind", HorseRegistry.knownIdentity(uuid).animalType());
        assertTrue(HorseRegistry.observation(uuid).isEmpty());
        assertTrue(HorseRegistry.databaseSnapshot(uuid).isEmpty());
        assertTrue(HorseRegistry.name(uuid).isEmpty());
        assertThrows(java.util.concurrent.CompletionException.class, () -> HorseRegistry.saveName(uuid, "Lotte", null).join());
        var state = new EntityRenderState();
        state.displayName = Text.literal("Lotte");
        var config = new OttoExtraConfig.AnimalInfo();
        config.nametagIncludeUnregistered = config.nametagShowGender = config.nametagGenderColor = true;
        HorseRegistryNametagRenderer.prepareUnregisteredLabel(state, uuid, null, config);
        assertEquals("Lotte ♀", state.displayName.getString());
        assertEquals(0xFF79B8, state.displayName.getStyle().getColor().getRgb());
        HorseRegistry.clearConnection();
        assertEquals("", HorseRegistry.knownGender(uuid), "Closed connection cannot read retained identities");
    }

    @Test
    void expiryIsFourteenDaysSinceObservationAndReadsDoNotExtendIt() {
        var cache = cache("a");
        cache.remember(identity(uuid, "Weiblich", now.get()));
        cache.saveIfDirty();
        long observed = now.get();
        now.addAndGet(AnimalIdentityCache.MAX_AGE_MS - 1);
        var reloaded = cache("a"); reloaded.load();
        assertEquals(observed, reloaded.find(uuid).observedAt());
        assertNotNull(reloaded.find(uuid));
        now.incrementAndGet();
        assertNull(reloaded.find(uuid));
        reloaded.saveIfDirty();
        var expired = cache("a"); expired.load();
        assertNull(expired.find(uuid));
    }

    @Test
    void newObservationRefreshesAgeAndRejectsOlderUpdates() {
        var cache = cache("a");
        long before = now.get();
        cache.remember(identity(uuid, "Weiblich", before));
        now.addAndGet(10_000);
        cache.remember(identity(uuid, "Männlich", now.get()));
        cache.remember(identity(uuid, "Weiblich", before));
        cache.saveIfDirty();
        var restored = cache("a"); restored.load();
        assertEquals("Männlich", restored.find(uuid).gender());
        assertEquals(now.get(), restored.find(uuid).observedAt());
    }

    @Test
    void diskLoadingDoesNotReplaceAnObservationReceivedDuringLoad() {
        var old = cache("a"); old.remember(identity(uuid, "Weiblich", now.get() - 1)); old.saveIfDirty();
        var current = cache("a");
        current.remember(identity(uuid, "Männlich", now.get()));
        current.load();
        assertEquals("Männlich", current.find(uuid).gender());
        var unknown = cache("a");
        unknown.remember(identity(uuid, "Unbekannt", now.get()));
        unknown.load();
        assertNull(unknown.find(uuid), "An older disk entry must not revive invalidated gender");
    }

    @Test
    void maximumEntriesEvictsOldestIdentity() {
        var cache = cache("a");
        cache.remember(identity(uuid, "Weiblich", now.get() - 100_000));
        UUID newest = null;
        for (int i = 0; i < AnimalIdentityCache.MAX_ENTRIES; i++) {
            newest = UUID.randomUUID(); cache.remember(identity(newest, "Männlich", now.get() - 10_000 + i));
        }
        assertNull(cache.find(uuid));
        assertNotNull(cache.find(newest));
        cache.saveIfDirty();
        var restored = cache("a"); restored.load();
        assertNull(restored.find(uuid));
        assertNotNull(restored.find(newest));
    }

    @Test
    void wrongServerCorruptAndOversizedFilesAreIgnored() throws Exception {
        var cache = cache("a"); cache.remember(identity(uuid, "Weiblich", now.get())); cache.saveIfDirty();
        var otherServer = cache("b"); otherServer.load(); assertNull(otherServer.find(uuid));
        Path file = directory.resolve("cache.json");
        Files.writeString(file, "{ broken");
        var broken = cache("a"); assertDoesNotThrow(broken::load); assertNull(broken.find(uuid));
        broken.remember(identity(uuid, "Männlich", now.get())); broken.saveIfDirty();
        var repaired = cache("a"); repaired.load(); assertEquals("Männlich", repaired.find(uuid).gender());
        Files.write(file, new byte[2 * 1024 * 1024 + 1]);
        var oversized = cache("a"); assertDoesNotThrow(oversized::load); assertNull(oversized.find(uuid));
    }

    @Test
    void disablingPurgesCurrentServerFileAndReenablingCannotResurrectIt() throws Exception {
        Path file = directory.resolve("disabled.json");
        HorseRegistry.initialize(null, () -> true, true, now::get);
        HorseRegistry.openIdentityCache(file, "a", true).join();
        HorseRegistry.observe(new AnimalInfoObservation(uuid, new AnimalInfoSnapshot("Kuh", "Weiblich", "Ja"), Instant.ofEpochMilli(now.get())));
        HorseRegistry.shutdown();
        assertTrue(Files.exists(file));
        HorseRegistry.initialize(null, () -> true, true, now::get);
        HorseRegistry.openIdentityCache(file, "a", false).join();
        assertFalse(Files.exists(file));
        HorseRegistry.openIdentityCache(file, "a", true).join();
        assertEquals("", HorseRegistry.knownGender(uuid));
    }

    @Test
    void futureMissingAndAmbiguousDataDoNotBecomeAnIdentity() {
        var cache = cache("a");
        cache.remember(identity(uuid, "Weiblich", now.get() + 1)); assertNull(cache.find(uuid));
        cache.remember(identity(uuid, "♀ ♂", now.get())); assertNull(cache.find(uuid));
        cache.remember(new AnimalIdentityCache.Identity(uuid, "", "Weiblich", now.get())); assertNull(cache.find(uuid));
        cache.remember(new AnimalIdentityCache.Identity(uuid, "x".repeat(129), "Weiblich", now.get())); assertNull(cache.find(uuid));
    }

    private AnimalIdentityCache cache(String server) { return new AnimalIdentityCache(directory.resolve("cache.json"), server, now::get); }
    private static AnimalIdentityCache.Identity identity(UUID uuid, String gender, long at) {
        return new AnimalIdentityCache.Identity(uuid, "Rotvieh Rind", gender, at);
    }
}
