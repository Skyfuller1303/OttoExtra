package de.ottoextra.api.model;

import java.util.UUID;

/** Shared API representation; the addon keeps its own local persistence model. */
public record AnimalBreedingHorse(
        String entityUuid,
        String registryName,
        String origin,
        String currentFarm,
        String animalType,
        String gender,
        String coatColor,
        String marking,
        String life,
        String jumpHeight,
        String speed,
        String mast,
        String breedingReady,
        long observedAtEpochMillis,
        long updatedAtEpochMillis,
        long revision,
        String ownerUuid,
        String ownerName,
        boolean canEdit,
        boolean canManage,
        String updatedByUuid,
        String serverUpdatedAt) {

    public AnimalBreedingHorse {
        entityUuid = UUID.fromString(entityUuid).toString();
        registryName = clean(registryName);
        origin = clean(origin);
        currentFarm = clean(currentFarm);
        animalType = clean(animalType);
        gender = clean(gender);
        coatColor = clean(coatColor);
        marking = clean(marking);
        life = clean(life);
        jumpHeight = clean(jumpHeight);
        speed = clean(speed);
        mast = clean(mast);
        breedingReady = clean(breedingReady);
        ownerUuid = clean(ownerUuid);
        ownerName = clean(ownerName);
        updatedByUuid = clean(updatedByUuid);
        serverUpdatedAt = clean(serverUpdatedAt);
    }

    public AnimalBreedingHorse(
            String entityUuid,
            String registryName,
            String origin,
            String currentFarm,
            String animalType,
            String gender,
            String life,
            String jumpHeight,
            String speed,
            String mast,
            String breedingReady,
            long observedAtEpochMillis,
            long updatedAtEpochMillis,
            long revision,
            String updatedByUuid,
            String serverUpdatedAt) {
        this(entityUuid, registryName, origin, currentFarm, animalType, gender,
                "", "", life, jumpHeight, speed, mast, breedingReady,
                observedAtEpochMillis, updatedAtEpochMillis, revision,
                "", "", false, false,
                updatedByUuid, serverUpdatedAt);
    }

    public AnimalBreedingHorse(
            String entityUuid,
            String registryName,
            String origin,
            String currentFarm,
            String animalType,
            String gender,
            String coatColor,
            String marking,
            String life,
            String jumpHeight,
            String speed,
            String mast,
            String breedingReady,
            long observedAtEpochMillis,
            long updatedAtEpochMillis,
            long revision,
            String updatedByUuid,
            String serverUpdatedAt) {
        this(entityUuid, registryName, origin, currentFarm, animalType, gender,
                coatColor, marking, life, jumpHeight, speed, mast, breedingReady,
                observedAtEpochMillis, updatedAtEpochMillis, revision,
                "", "", false, false, updatedByUuid, serverUpdatedAt);
    }

    public UUID uuid() {
        return UUID.fromString(entityUuid);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
