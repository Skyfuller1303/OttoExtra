package de.ottoextra.horses;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class HorseRegistryLifecycleTest {
    private static final UUID HORSE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @AfterEach void clear() { HorseRegistry.shutdown(); }

    @Test
    void failedRoleAndRecordRequestsRetryAfterCooldownWithoutRequestSpam() {
        AtomicLong time = new AtomicLong();
        AtomicInteger roleCalls = new AtomicInteger();
        AtomicInteger horseCalls = new AtomicInteger();
        var first = new CompletableFuture<Optional<String>>();
        AnimalBreedingHorse horse = new Gson().fromJson("{\"entityUuid\":\"" + HORSE + "\",\"registryName\":\"Neu\"}", AnimalBreedingHorse.class);
        HorseRegistry.initialize(api((name, args) -> switch (name) {
            case "animalBreedingRole" -> roleCalls.incrementAndGet() == 1 ? first : CompletableFuture.completedFuture(Optional.of("breeder"));
            case "animalBreedingHorses" -> horseCalls.incrementAndGet() == 1
                    ? CompletableFuture.failedFuture(new IllegalStateException("temporary"))
                    : CompletableFuture.completedFuture(List.of(horse));
            default -> throw new AssertionError(name);
        }), () -> true, true, time::get);
        HorseRegistry.requestDatabaseRecords();
        HorseRegistry.requestDatabaseRecords();
        assertEquals(1, roleCalls.get());
        first.completeExceptionally(new IllegalStateException("temporary"));
        HorseRegistry.requestDatabaseRecords();
        assertEquals(1, roleCalls.get());
        time.addAndGet(HorseRegistry.DATABASE_RETRY_MS);
        HorseRegistry.requestDatabaseRecords();
        assertEquals(1, horseCalls.get());
        time.addAndGet(HorseRegistry.DATABASE_RETRY_MS);
        HorseRegistry.requestDatabaseRecords();
        assertEquals(List.of(horse), HorseRegistry.databaseRecords());
        time.addAndGet(HorseRegistry.DATABASE_RETRY_MS);
        HorseRegistry.requestDatabaseRecords();
        assertEquals(3, roleCalls.get());
    }

    @Test
    void oldNameRepliesCannotOverwriteReconnectOrNewerRefresh() {
        var requests = new ArrayList<CompletableFuture<List<AnimalBreedingHorseName>>>();
        HorseRegistry.initialize(api((name, args) -> {
            assertEquals("animalBreedingHorseNames", name);
            var request = new CompletableFuture<List<AnimalBreedingHorseName>>();
            requests.add(request);
            return request;
        }), () -> true, true);
        HorseRegistry.refresh();
        HorseRegistry.clearConnection();
        HorseRegistry.refresh();
        requests.get(1).complete(names("Neu"));
        requests.get(0).complete(names("Alt"));
        assertEquals("Neu", HorseRegistry.name(HORSE).orElseThrow());
        HorseRegistry.refresh();
        HorseRegistry.refresh();
        requests.get(3).complete(names("Neueste"));
        requests.get(2).complete(names("Überholt"));
        assertEquals("Neueste", HorseRegistry.name(HORSE).orElseThrow());
    }

    @Test
    void oldRoleFailureDoesNotUnlockNewConnectionsPendingRequest() {
        var requests = new ArrayList<CompletableFuture<Optional<String>>>();
        HorseRegistry.initialize(api((name, args) -> {
            assertEquals("animalBreedingRole", name);
            var request = new CompletableFuture<Optional<String>>();
            requests.add(request);
            return request;
        }), () -> true, true);
        HorseRegistry.requestDatabaseRecords();
        HorseRegistry.clearConnection();
        HorseRegistry.requestDatabaseRecords();
        requests.getFirst().completeExceptionally(new IllegalStateException("old connection"));
        HorseRegistry.requestDatabaseRecords();
        assertEquals(2, requests.size());
        requests.get(1).complete(Optional.empty());
        HorseRegistry.requestDatabaseRecords();
        assertEquals(2, requests.size());
    }

    @ParameterizedTest
    @CsvSource({"true, false, true", "false, true, true", "true, true, true", "false, false, false"})
    void editingExistingNamesRequiresRecordPermission(boolean canEdit, boolean canManage,
                                                       boolean expected) {
        AnimalBreedingHorse horse = new Gson().fromJson("{\"entityUuid\":\"" + HORSE
                + "\",\"registryName\":\"Abendrot\",\"canEdit\":" + canEdit
                + ",\"canManage\":" + canManage + "}", AnimalBreedingHorse.class);
        var records = new CompletableFuture<List<AnimalBreedingHorse>>();
        HorseRegistry.initialize(api((name, args) -> switch (name) {
            case "animalBreedingHorseNames" -> CompletableFuture.completedFuture(names("Abendrot"));
            case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("editor"));
            case "animalBreedingHorses" -> records;
            default -> throw new AssertionError(name);
        }), () -> true, true);
        HorseRegistry.refresh().join();
        HorseRegistry.refreshDatabaseRecords();
        assertFalse(HorseRegistry.canEditName(HORSE), "Pending permissions cannot enable editing");

        records.complete(List.of(horse));
        assertEquals(expected, HorseRegistry.canEditName(HORSE));
        assertEquals(expected, HorseRegistry.canEditName(HORSE,
                new de.ottoextra.api.model.AnimalBreedingHorseLocation("minecraft:overworld", 0, 64, 0)),
                "Existing name permissions are independent of initial naming territory");
        HorseRegistry.clearConnection();
        assertFalse(HorseRegistry.canEditName(HORSE));
    }

    @Test
    void publicForeignNameDoesNotGrantEditingDespiteBreederRole() {
        HorseRegistry.initialize(api((name, args) -> switch (name) {
            case "animalBreedingHorseNames" -> CompletableFuture.completedFuture(names("Fremdes Tier"));
            case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("editor"));
            case "animalBreedingHorses" -> CompletableFuture.completedFuture(List.of());
            default -> throw new AssertionError(name);
        }), () -> true, true);
        HorseRegistry.refresh().join();
        HorseRegistry.refreshDatabaseRecords().join();

        assertEquals("Fremdes Tier", HorseRegistry.name(HORSE).orElseThrow());
        assertFalse(HorseRegistry.canEditName(HORSE));
        assertFalse(HorseRegistry.canEditName(UUID.randomUUID()),
                "An unnamed animal must stay read-only without a confirmed own-Lehen location");
    }

    @Test
    void missingNameDirectoryCannotTreatRegisteredAnimalsAsNew() {
        HorseRegistry.initialize(api((name, args) -> switch (name) {
            case "animalBreedingHorseNames" -> CompletableFuture.failedFuture(new IllegalStateException("offline"));
            case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("editor"));
            case "animalBreedingHorses" -> CompletableFuture.completedFuture(List.of());
            default -> throw new AssertionError(name);
        }), () -> true, true);
        HorseRegistry.refreshDatabaseRecords().join();
        assertFalse(HorseRegistry.canEditName(HORSE));
        assertThrows(Exception.class, () -> HorseRegistry.refresh().join());
        assertFalse(HorseRegistry.canEditName(HORSE));
    }

    @Test
    void namingProfileRepliesCannotSurviveReconnectOrOverrideNewerMembership() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        var requests = new ArrayList<CompletableFuture<de.ottoextra.api.model.PlayerRecord>>();
        HorseRegistry.initialize(api((name, args) -> {
            assertEquals("player", name);
            var request = new CompletableFuture<de.ottoextra.api.model.PlayerRecord>();
            requests.add(request);
            return request;
        }), () -> true, true);
        var field = HorseRegistry.class.getDeclaredField("namingPlayer");
        field.setAccessible(true);
        var old = new Gson().fromJson("{\"uuid\":\"" + playerUuid + "\",\"faction\":\"lehen_1\"}",
                de.ottoextra.api.model.PlayerRecord.class);
        var latest = new Gson().fromJson("{\"uuid\":\"" + playerUuid + "\",\"faction\":\"lehen_2\"}",
                de.ottoextra.api.model.PlayerRecord.class);
        HorseRegistry.refreshNamingPlayer(playerUuid);
        HorseRegistry.refreshNamingPlayer(playerUuid);
        requests.get(1).complete(latest);
        requests.get(0).complete(old);
        assertEquals(latest, field.get(null));
        HorseRegistry.refreshNamingPlayer(playerUuid);
        assertNull(field.get(null));
        HorseRegistry.clearConnection();
        requests.get(2).complete(old);
        assertNull(field.get(null));
    }

    private static List<AnimalBreedingHorseName> names(String name) {
        return List.of(new AnimalBreedingHorseName(HORSE.toString(), name, 1));
    }
    private static OttoExtraApiClient api(java.util.function.BiFunction<String, Object[], Object> call) {
        return (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> call.apply(method.getName(), args));
    }
}
