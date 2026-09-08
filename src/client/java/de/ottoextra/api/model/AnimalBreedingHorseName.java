package de.ottoextra.api.model;

import java.util.UUID;

/** Minimal public horse identity used by ingame nametags without exposing farm stats. */
public record AnimalBreedingHorseName(
        String entityUuid,
        String registryName,
        long revision
) {
    public AnimalBreedingHorseName {
        entityUuid = entityUuid == null ? "" : entityUuid.trim().toLowerCase();
        registryName = registryName == null ? "" : registryName.trim();
        revision = Math.max(0L, revision);
    }

    public UUID uuid() {
        return UUID.fromString(entityUuid);
    }
}
