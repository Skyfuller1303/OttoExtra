package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingDecisionTest {

    private static final long NOW = 1_800_000_000_000L;

    private static BreedingDecision.Neighbour adult(UUID id, double distance, long lovedAt) {
        return new BreedingDecision.Neighbour(id, true, true, distance * distance, lovedAt);
    }

    @Test
    void twoNearestLovingAdultsAreTheParents() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        List<UUID> parents = BreedingDecision.parents(List.of(
                adult(c, 6, NOW - 5_000), adult(a, 1, NOW - 10_000), adult(b, 2, NOW - 2_000)), NOW);
        assertEquals(List.of(a, b), parents);
    }

    @Test
    void loveOlderThanSixtySecondsOrNeverSeenDoesNotCount() {
        UUID old = UUID.randomUUID();
        UUID never = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        assertEquals(List.of(fresh), BreedingDecision.parents(List.of(
                adult(old, 1, NOW - BreedingDecision.BIRTH_AFTER_LOVE_MS),
                adult(never, 1, 0), adult(fresh, 3, NOW - BreedingDecision.BIRTH_AFTER_LOVE_MS + 1)), NOW));
    }

    @Test
    void otherSpeciesBabiesAndFarAnimalsAreIgnored() {
        UUID id = UUID.randomUUID();
        assertTrue(BreedingDecision.parents(List.of(
                new BreedingDecision.Neighbour(id, false, true, 1, NOW - 1_000),
                new BreedingDecision.Neighbour(id, true, false, 1, NOW - 1_000),
                adult(id, BreedingDecision.BIRTH_RANGE + 0.5, NOW - 1_000)), NOW).isEmpty());
        assertEquals(List.of(id), BreedingDecision.parents(List.of(
                adult(id, BreedingDecision.BIRTH_RANGE, NOW - 1_000)), NOW));
    }

    @Test
    void loveFromTheFutureIsRejectedAndEmptyInputIsFine() {
        assertTrue(BreedingDecision.parents(List.of(adult(UUID.randomUUID(), 1, NOW + 1)), NOW).isEmpty());
        assertTrue(BreedingDecision.parents(List.of(), NOW).isEmpty());
    }

    @Test
    void particlesGoToTheNearestAnimal() {
        assertEquals(1, BreedingDecision.nearest(new double[]{4.0, 0.5, 2.0}));
        assertEquals(-1, BreedingDecision.nearest(new double[0]));
    }
}
