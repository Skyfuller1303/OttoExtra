package de.ottoextra.horses;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.AnimalBreedingHorse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class HorseFarmTest {
    private static final UUID HORSE = new UUID(0, 1);
    private static final AnimalBreedingFarm FARM = new AnimalBreedingFarm("apfelhof", "Apfelhof", 0, "", false, "owner", true);
    @AfterEach void reset() { HorseRegistry.shutdown(); }

    @Test void selectedFarmIsAssignedOnlyAfterAnimalSaveSucceeds() {
        var saved = new CompletableFuture<AnimalBreedingHorse>();
        var writes = new java.util.ArrayList<String>();
        initialize((method, args) -> switch (method) {
            case "animalBreedingFarms" -> CompletableFuture.completedFuture(List.of(FARM));
            case "putAnimalBreedingHorseName" -> { writes.add("name"); yield saved; }
            case "putAnimalBreedingHorse" -> {
                writes.add("farm");
                var draft = (AnimalBreedingHorse) args[0];
                assertEquals("Apfelhof", draft.currentFarm());
                yield CompletableFuture.completedFuture(draft);
            }
            default -> throw new AssertionError(method);
        });
        prepareNewAnimal();
        var result = HorseRegistry.saveNameAndFarm(HORSE, "Alrik", null, "", "", "apfelhof");
        assertEquals(List.of("name"), writes);
        assertFalse(result.isDone());
        saved.complete(horse(true));
        assertEquals("Apfelhof", result.join().currentFarm());
        assertEquals(List.of("name", "farm"), writes);
        assertEquals("Apfelhof", HorseRegistry.databaseRecord(HORSE).orElseThrow().currentFarm());
    }

    @Test void failedAnimalSaveNeverAssignsFarm() {
        initialize((method, args) -> switch (method) {
            case "animalBreedingFarms" -> CompletableFuture.completedFuture(List.of(FARM));
            case "putAnimalBreedingHorseName" -> CompletableFuture.failedFuture(new IllegalStateException("offline"));
            default -> throw new AssertionError("Unexpected write " + method);
        });
        prepareNewAnimal();
        assertThrows(Exception.class, () -> HorseRegistry.saveNameAndFarm(HORSE, "Alrik", null, "", "", "apfelhof").join());
        assertTrue(HorseRegistry.databaseRecords().isEmpty());
    }

    @Test void farmFailureReportsPartialSaveAndRetainsRegisteredAnimal() {
        initialize((method, args) -> switch (method) {
            case "animalBreedingFarms" -> CompletableFuture.completedFuture(List.of(FARM));
            case "putAnimalBreedingHorseName" -> CompletableFuture.completedFuture(horse(true));
            case "putAnimalBreedingHorse" -> CompletableFuture.failedFuture(new IllegalStateException("conflict"));
            default -> throw new AssertionError(method);
        });
        prepareNewAnimal();
        var error = assertThrows(java.util.concurrent.CompletionException.class,
                () -> HorseRegistry.saveNameAndFarm(HORSE, "Alrik", null, "", "", "apfelhof").join());
        assertInstanceOf(HorseRegistry.FarmAssignmentException.class, error.getCause());
        assertEquals("Alrik", HorseRegistry.databaseRecord(HORSE).orElseThrow().registryName());
        assertEquals("Alt", HorseRegistry.databaseRecord(HORSE).orElseThrow().currentFarm());
    }

    @Test void noSelectedFarmPreservesExistingFarmWithoutSecondWrite() {
        initialize((method, args) -> {
            assertEquals("putAnimalBreedingHorseName", method);
            return CompletableFuture.completedFuture(horse(true));
        });
        observeNewAnimal();
        assertEquals("Alt", HorseRegistry.saveNameAndFarm(HORSE, "Alrik", null, "", "", null).join().currentFarm());
    }

    @Test void unavailableFarmPreventsBothWrites() {
        initialize((method, args) -> { throw new AssertionError("Unexpected call " + method); });
        observeNewAnimal();
        assertThrows(Exception.class, () -> HorseRegistry.saveNameAndFarm(HORSE, "Alrik", null, "", "", "missing").join());
        assertTrue(HorseRegistry.databaseRecords().isEmpty());
    }

    @Test void disconnectDuringNameSavePreventsFarmAssignment() {
        var saved = new CompletableFuture<AnimalBreedingHorse>();
        initialize((method, args) -> switch (method) {
            case "animalBreedingFarms" -> CompletableFuture.completedFuture(List.of(FARM));
            case "putAnimalBreedingHorseName" -> saved;
            default -> throw new AssertionError("Unexpected call " + method);
        });
        prepareNewAnimal();
        var result = HorseRegistry.saveNameAndFarm(HORSE, "Alrik", null, "", "", "apfelhof");
        HorseRegistry.clearConnection();
        saved.complete(horse(true));
        assertTrue(result.isCompletedExceptionally());
        assertTrue(HorseRegistry.databaseRecords().isEmpty());
    }

    private static void prepareNewAnimal() {
        HorseRegistry.refreshFarms().join();
        observeNewAnimal();
    }

    private static void observeNewAnimal() {
        HorseRegistry.observe(new de.ottoextra.animalinfo.AnimalInfoObservation(HORSE,
                new de.ottoextra.animalinfo.AnimalInfoSnapshot("Kuh", "Weiblich", "Ja"), java.time.Instant.now()));
    }

    @Test void rejectsInvalidNamesAndSeparatesReadOnlyPermissions() {
        for (String name : new String[]{null, "", "  ", "a/b", "a\\b", "a\nb", "a\u007fb", "a".repeat(65)}) {
            assertFalse(AnimalBreedingFarm.validName(name), String.valueOf(name));
        }
        assertTrue(AnimalBreedingFarm.validName("  Mühlengut zur Eiche  "));
        assertTrue(FARM.canEdit());
        assertFalse(new AnimalBreedingFarm("lesehof", "Lesehof", 0, "", false, "viewer", false).canEdit());
    }

    @Test void creatingAnEmptyFarmSurvivesAnOlderListReply() {
        var oldList = new CompletableFuture<List<AnimalBreedingFarm>>();
        initialize((method, args) -> method.equals("animalBreedingFarms") ? oldList : CompletableFuture.completedFuture(FARM));
        HorseRegistry.refreshFarms();
        HorseRegistry.createFarm("Apfelhof").join();
        oldList.complete(List.of());
        assertEquals(List.of(FARM), HorseRegistry.farms());
    }

    @Test void lateFarmRepliesCannotLeakAcrossConnections() {
        var reply = new CompletableFuture<AnimalBreedingFarm>();
        initialize((method, args) -> reply);
        var pending = HorseRegistry.createFarm("Apfelhof");
        HorseRegistry.clearConnection();
        reply.complete(FARM);
        assertTrue(pending.isCompletedExceptionally());
        assertTrue(HorseRegistry.farms().isEmpty());
    }

    @Test void assigningOnlyChangesFarmAndAdvancesTheReadVersionByOne() {
        AnimalBreedingHorse original = horse(true);
        initialize((method, args) -> switch (method) {
            case "animalBreedingFarms" -> CompletableFuture.completedFuture(List.of(FARM));
            case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("editor"));
            case "animalBreedingHorses" -> CompletableFuture.completedFuture(List.of(original));
            case "putAnimalBreedingHorse" -> {
                var draft = (AnimalBreedingHorse) args[0];
                assertEquals("Apfelhof", draft.currentFarm());
                assertEquals(original.updatedAtEpochMillis() + 1, draft.updatedAtEpochMillis());
                assertEquals(original.origin(), draft.origin());
                assertEquals(original.registryName(), draft.registryName());
                assertEquals(original.speed(), draft.speed());
                assertEquals(original.jumpHeight(), draft.jumpHeight());
                assertEquals(original.ownerUuid(), draft.ownerUuid());
                assertEquals(original.observedAtEpochMillis(), draft.observedAtEpochMillis());
                yield CompletableFuture.completedFuture(draft);
            }
            default -> throw new AssertionError(method);
        });
        HorseRegistry.refreshFarms().join(); HorseRegistry.refreshDatabaseRecords().join();
        HorseRegistry.assignFarm(HORSE, "apfelhof", null).join();
        assertEquals("Apfelhof", HorseRegistry.databaseRecord(HORSE).orElseThrow().currentFarm());
        assertEquals(1, HorseRegistry.farms().getFirst().horseCount());
    }

    @Test void noWriteWithoutBothAnimalAndFarmPermission() {
        initialize((method, args) -> switch (method) {
            case "animalBreedingFarms" -> CompletableFuture.completedFuture(List.of(FARM));
            case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("viewer"));
            case "animalBreedingHorses" -> CompletableFuture.completedFuture(List.of(horse(false)));
            default -> throw new AssertionError("Unexpected write " + method);
        });
        HorseRegistry.refreshFarms().join(); HorseRegistry.refreshDatabaseRecords().join();
        assertThrows(Exception.class, () -> HorseRegistry.assignFarm(HORSE, "apfelhof", null).join());
        assertEquals("Alt", HorseRegistry.databaseRecord(HORSE).orElseThrow().currentFarm());
    }

    @Test void failedRefreshDiscardsEarlierFarmPermissions() {
        var reply = new CompletableFuture<List<AnimalBreedingFarm>>();
        initialize((method, args) -> method.equals("createAnimalBreedingFarm")
                ? CompletableFuture.completedFuture(FARM) : reply);
        HorseRegistry.createFarm("Apfelhof").join();
        HorseRegistry.refreshFarms();
        reply.completeExceptionally(new IllegalStateException("Offline"));
        assertTrue(HorseRegistry.farms().isEmpty());
    }

    private static AnimalBreedingHorse horse(boolean editable) {
        return new Gson().fromJson("{\"entityUuid\":\"" + HORSE + "\",\"registryName\":\"Alrik\","
                + "\"origin\":\"Nord\",\"currentFarm\":\"Alt\",\"canEdit\":" + editable
                + ",\"speed\":\"14\",\"jumpHeight\":\"2\",\"updatedAtEpochMillis\":2000,\"observedAtEpochMillis\":1000}", AnimalBreedingHorse.class);
    }

    private static void initialize(BiFunction<String, Object[], Object> call) {
        var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> call.apply(method.getName(), args));
        HorseRegistry.initialize(api, () -> true, true);
    }
}
