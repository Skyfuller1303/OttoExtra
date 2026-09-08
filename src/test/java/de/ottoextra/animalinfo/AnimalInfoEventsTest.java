package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimalInfoEventsTest {

    @Test
    void publishesUuidSnapshotAndObservationTime() {
        AtomicReference<AnimalInfoObservation> received = new AtomicReference<>();
        AnimalInfoEvents.OBSERVED.register(received::set);
        UUID uuid = UUID.fromString("5f4c96e3-8ef0-4fa0-a90c-efe5831b9a11");
        Instant observedAt = Instant.parse("2026-09-01T16:42:12Z");
        AnimalInfoSnapshot snapshot = new AnimalInfoSnapshot(
                "Reitpferd", "Stute", "72/100", "in 4h 10m", "", "",
                "3,42 Blöcke", "11,87 Blöcke/s", "24/30");

        AnimalInfoService.publishObservation(uuid, snapshot, observedAt);

        assertEquals(new AnimalInfoObservation(uuid, snapshot, observedAt), received.get());
    }
}
