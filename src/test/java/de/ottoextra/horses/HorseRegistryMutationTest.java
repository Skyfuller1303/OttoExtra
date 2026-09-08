package de.ottoextra.horses;

import com.google.gson.Gson;
import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class HorseRegistryMutationTest {
    private static final UUID HORSE = UUID.randomUUID();
    private static final AnimalBreedingHorseLocation LOCATION =
            new AnimalBreedingHorseLocation("minecraft:overworld", 20, 64, 20);

    @AfterEach void reset() { HorseRegistry.shutdown(); }

    @Test void lateSaveCannotRepopulateDisconnectedCache() {
        var reply = new CompletableFuture<AnimalBreedingHorse>();
        initialize((method, args) -> reply);
        var saved = HorseRegistry.saveName(HORSE, "Alt", LOCATION);
        HorseRegistry.clearConnection();
        reply.complete(horse(HORSE, "Alt", 1));
        assertTrue(HorseRegistry.names().isEmpty());
        assertTrue(HorseRegistry.databaseRecords().isEmpty());
        assertTrue(saved.isCompletedExceptionally());
    }

    @Test void mismatchedSaveUuidIsRejectedWithoutCacheMutation() {
        initialize((method, args) -> CompletableFuture.completedFuture(horse(UUID.randomUUID(), "Falsch", 1)));
        assertThrows(Exception.class, () -> HorseRegistry.saveName(HORSE, "Name", LOCATION).join());
        assertTrue(HorseRegistry.names().isEmpty());
    }

    @Test void olderSaveResponseCannotOverwriteNewerRevision() {
        var first = new CompletableFuture<AnimalBreedingHorse>();
        var second = new CompletableFuture<AnimalBreedingHorse>();
        initialize((method, args) -> ((AnimalBreedingHorse) args[0]).registryName().equals("Alt") ? first : second);
        HorseRegistry.saveName(HORSE, "Alt", LOCATION);
        HorseRegistry.saveName(HORSE, "Neu", LOCATION);
        second.complete(horse(HORSE, "Neu", 2));
        first.complete(horse(HORSE, "Alt", 1));
        assertEquals("Neu", HorseRegistry.name(HORSE).orElseThrow());
        assertEquals("Neu", HorseRegistry.databaseRecords().getFirst().registryName());
    }

    @Test void listStartedBeforeSaveCannotReplaceConfirmedName() {
        var listing = new CompletableFuture<List<AnimalBreedingHorseName>>();
        initialize((method, args) -> method.equals("animalBreedingHorseNames") ? listing
                : CompletableFuture.completedFuture(horse(HORSE, "Neu", 2)));
        HorseRegistry.refresh();
        HorseRegistry.saveName(HORSE, "Neu", LOCATION).join();
        listing.complete(List.of(new AnimalBreedingHorseName(HORSE.toString(), "Alt", 1)));
        assertEquals("Neu", HorseRegistry.name(HORSE).orElseThrow());
    }

    private static void initialize(BiFunction<String, Object[], Object> call) {
        var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> call.apply(method.getName(), args));
        HorseRegistry.initialize(api, () -> true, true);
        HorseRegistry.observe(new AnimalInfoObservation(HORSE,
                new AnimalInfoSnapshot("Kuh", "Weiblich", "Ja"), Instant.now()));
    }

    private static AnimalBreedingHorse horse(UUID uuid, String name, long revision) {
        return new Gson().fromJson("{\"entityUuid\":\"" + uuid + "\",\"registryName\":\"" + name
                + "\",\"revision\":" + revision + "}", AnimalBreedingHorse.class);
    }
}
