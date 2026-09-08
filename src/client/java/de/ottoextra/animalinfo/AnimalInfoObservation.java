package de.ottoextra.animalinfo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Unveränderliche, vollständig geparste Tierbeobachtung für OttoExtra-Addons.
 */
public record AnimalInfoObservation(UUID entityUuid, AnimalInfoSnapshot snapshot,
                                    Instant observedAt) {

    public AnimalInfoObservation {
        entityUuid = Objects.requireNonNull(entityUuid, "entityUuid");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }
}
