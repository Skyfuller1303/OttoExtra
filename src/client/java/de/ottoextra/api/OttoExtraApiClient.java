package de.ottoextra.api;

import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.model.CompactPlayer;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.api.model.ProtectedChatInboxEntry;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OttoExtraApiClient {

    default CompletableFuture<com.google.gson.JsonObject> submitRpNameObservation(
            com.google.gson.JsonObject body, de.ottoextra.privacy.ConsentPermit permit) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("RP-Uploads nicht verfügbar"));
    }

    CompletableFuture<ApiEnvelope> bootstrap();

    CompletableFuture<ApiEnvelope> sync(long cursor);

    CompletableFuture<List<RegionRecord>> regionList();

    CompletableFuture<RegionRecord> regionByName(String name);

    CompletableFuture<FactionRecord> faction(UUID uuid);

    CompletableFuture<List<PlayerRecord>> factionPlayers(UUID factionUuid);

    CompletableFuture<PlayerRecord> player(UUID uuid);

    CompletableFuture<List<CompactPlayer>> compactPlayers();

    CompletableFuture<String> createProtectedChatMessage(
            String original, List<String> allowedUsernames);

    CompletableFuture<String> createProtectedChatMessage(
            String original, List<String> allowedUsernames, List<String> translations);

    CompletableFuture<String> protectedChatMessage(String id);

    CompletableFuture<List<ProtectedChatInboxEntry>> protectedChatInbox();

    CompletableFuture<List<AnimalBreedingHorse>> animalBreedingHorses();

    /** Returns the server-side AnimalBreeding role of the current player. */
    CompletableFuture<Optional<String>> animalBreedingRole();

    default CompletableFuture<List<AnimalBreedingFarm>> animalBreedingFarms() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Höfe nicht verfügbar"));
    }

    default CompletableFuture<AnimalBreedingFarm> createAnimalBreedingFarm(String name) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Höfe nicht verfügbar"));
    }

    /** Grants or revokes the global breeder/editor role; the API requires admin. */
    CompletableFuture<Void> setAnimalBreedingBreederAccess(
            String playerName, boolean granted);

    CompletableFuture<List<AnimalBreedingHorseName>> animalBreedingHorseNames();

    CompletableFuture<Optional<UUID>> animalBreedingFinderTarget();

    CompletableFuture<List<UUID>> animalBreedingFinderTargets();

    CompletableFuture<Void> setAnimalBreedingFinderTarget(UUID entityUuid);

    CompletableFuture<Void> addAnimalBreedingFinderTarget(UUID entityUuid);

    CompletableFuture<Void> removeAnimalBreedingFinderTarget(UUID entityUuid);

    CompletableFuture<Void> clearAnimalBreedingFinderTarget();

    default CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorse(
            AnimalBreedingHorse horse) {
        return putAnimalBreedingHorse(horse, null);
    }

    CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorse(
            AnimalBreedingHorse horse, AnimalBreedingHorseLocation location);

    /**
     * Refreshes only the observed animal stats. Identity, name, farm, origin
     * and ownership of an existing record stay untouched.
     */
    CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorseObservation(
            AnimalBreedingHorse horse);

    /**
     * Creates the public horse identity or changes only its registry name.
     * Farm, origin and breeding data of an existing record stay untouched.
     */
    CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorseName(
            AnimalBreedingHorse horse, AnimalBreedingHorseLocation location);

    /** Deletes one horse record; the server verifies ownership. */
    default CompletableFuture<Void> deleteAnimalBreedingHorse(UUID entityUuid) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Pferde können nicht gelöscht werden"));
    }

    CompletableFuture<Void> setAnimalBreedingHorseEditor(
            UUID horseUuid, String playerName, boolean granted);

    CompletableFuture<UUID> createAnimalBreedingEvent(
            UUID mareUuid, UUID stallionUuid, String farmName,
            long pairedAtEpochMillis);

    CompletableFuture<Void> linkAnimalBreedingFoal(
            UUID eventUuid, UUID foalUuid);

    CompletableFuture<byte[]> downloadBinary(URI uri);

    OttoExtraApiRoutes routes();

    void close();
}
