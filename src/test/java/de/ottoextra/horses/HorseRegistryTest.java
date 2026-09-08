package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.OttoExtraApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HorseRegistryTest {

    @AfterEach
    void resetRegistry() {
        HorseRegistry.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kuh", "Schaf", "Huhn", "Esel", "Packesel", "Maultier", "Ziege", "Schwein"})
    void savesFarmAnimalNamesWithoutRequiringHorseStats(String animalType) {
        UUID uuid = UUID.randomUUID();
        AnimalBreedingHorseLocation location = new AnimalBreedingHorseLocation(
                "minecraft:overworld", 1739, 70, 4811);
        OttoExtraApiClient api = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> {
                    assertEquals("putAnimalBreedingHorseName", method.getName());
                    AnimalBreedingHorse draft = (AnimalBreedingHorse) args[0];
                    assertEquals(uuid, draft.uuid());
                    assertEquals(animalType, draft.animalType());
                    assertEquals("Lotte", draft.registryName());
                    assertEquals("", draft.speed());
                    assertEquals("", draft.jumpHeight());
                    assertEquals("", draft.origin());
                    assertEquals("", draft.currentFarm());
                    assertEquals(location, args[1]);
                    return CompletableFuture.completedFuture(draft);
                });
        HorseRegistry.initialize(api, () -> true, true);
        AnimalInfoObservation observation = new AnimalInfoObservation(uuid,
                new AnimalInfoSnapshot(animalType, "Weiblich", "72/100", "Ja", "", ""),
                Instant.ofEpochMilli(1_000));
        HorseRegistry.observe(observation);

        assertEquals(observation, HorseRegistry.observation(uuid).orElseThrow());
        HorseRegistry.saveName(uuid, " Lotte ", location).join();

        assertEquals("Lotte", HorseRegistry.name(uuid).orElseThrow());
        assertEquals(animalType, HorseRegistry.databaseRecords().getFirst().animalType());
    }

    @Test
    void mapsAnimalInfoIntoNonDestructiveBasicDraft() {
        UUID uuid = UUID.fromString("5f4c96e3-8ef0-4fa0-a90c-efe5831b9a11");
        AnimalInfoObservation observation = new AnimalInfoObservation(uuid,
                new AnimalInfoSnapshot("Reitpferd", "Stute", "72/100",
                        "in 4h", "", "", "1,42 Blöcke",
                        "14,2 Blöcke/s", "24/30"), Instant.ofEpochMilli(1_000));

        var horse = HorseRegistry.buildHorse(uuid, "Abendrot", observation,
                "CHESTNUT", "WHITE_DOTS", 2_000);

        assertEquals("Abendrot", horse.registryName());
        assertEquals("", horse.origin());
        assertEquals("", horse.currentFarm());
        assertEquals("Reitpferd", horse.animalType());
        assertEquals("CHESTNUT", horse.coatColor());
        assertEquals("1,42 Blöcke", horse.jumpHeight());
        assertEquals(1_000, horse.observedAtEpochMillis());
        assertEquals(2_000, horse.updatedAtEpochMillis());
    }

    @Test
    void mapsProtectedDatabaseRecordIntoAnimalInfoToast() {
        AnimalBreedingHorse horse = new AnimalBreedingHorse(
                "5f4c96e3-8ef0-4fa0-a90c-efe5831b9a11",
                "Abendrot", "Nordmark", "Gestuet Morgenrot",
                "Reitpferd", "Stute", "CHESTNUT", "WHITE_DOTS",
                "24/30", "1,42 Bloecke", "14,2 Bloecke/s",
                "72/100", "in 4h", 1_000, 2_000, 7,
                "", "", true, false, "", "");

        AnimalInfoSnapshot snapshot = HorseRegistry.databaseSnapshot(horse);

        assertEquals("Abendrot \u00b7 Reitpferd", snapshot.animalName());
        assertEquals("Stute", snapshot.gender());
        assertEquals("72/100", snapshot.mast());
        assertEquals("in 4h", snapshot.breedingReady());
        assertEquals("1,42 Bloecke", snapshot.jumpHeight());
        assertEquals("14,2 Bloecke/s", snapshot.speed());
        assertEquals("24/30", snapshot.life());
    }
}
