package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalStatusTest {

    private static final long H = 3_600_000L;
    private static final long NOW = 1_800_000_000_000L;
    private static final UUID ID = UUID.randomUUID();
    private static final AnimalSpecies COW = AnimalSpecies.byName("Rotvieh Rind");

    private static AnimalLiveState info(String breeding, String shearable) {
        return AnimalLiveState.empty(ID).withInfo(new AnimalInfoSnapshot(
                "Rotvieh Rind", "Weiblich", "65/80", breeding, shearable, ""), NOW);
    }

    @Test
    void serverWaitingTimeIsParsedAndCountsDown() {
        AnimalLiveState state = info("in 2h 15m | Mast 0/80", "");
        assertEquals(NOW + 2 * H + 15 * 60_000L, state.breedWaitUntil());
        AnimalStatus.Status later = AnimalStatus.compute(state, COW, false, NOW + H);
        assertEquals(AnimalStatus.Kind.WAITING, later.kind());
        assertEquals(H + 15 * 60_000L, later.remainingMs());
        assertFalse(later.estimated());
        assertEquals(AnimalStatus.Kind.NEED_MAST,
                AnimalStatus.compute(state, COW, false, NOW + 3 * H).kind(),
                "After the wait only the feeding value from the table comparison remains (65 of 80)");
    }

    @Test
    void readyAndMissingMastAreTold() {
        assertEquals(AnimalStatus.Kind.READY,
                AnimalStatus.compute(info("Ja", ""), COW, false, NOW).kind());
        AnimalStatus.Status need = AnimalStatus.compute(info("Mast 65/80", ""), COW, false, NOW);
        assertEquals(AnimalStatus.Kind.NEED_MAST, need.kind());
        assertEquals(65, need.mast());
        assertEquals(80, need.mastNeeded());
        assertFalse(need.estimated());
    }

    @Test
    void missingMastFallsBackToTheTableWhenTheServerNamesNoRequirement() {
        AnimalLiveState state = AnimalLiveState.empty(ID).withInfo(new AnimalInfoSnapshot(
                "Rotvieh Rind", "Weiblich", "30/100", "Nein", "", ""), NOW);
        AnimalStatus.Status need = AnimalStatus.compute(state, COW, false, NOW);
        assertEquals(AnimalStatus.Kind.NEED_MAST, need.kind());
        assertEquals(30, need.mast());
        assertEquals(100, need.mastNeeded(), "The server maximum wins over the table value 80");
        assertFalse(need.estimated());
    }

    @Test
    void babyShowsServerGrowthTimeOrTableEstimate() {
        AnimalLiveState server = info("wächst noch 1d 16h", "");
        AnimalStatus.Status young = AnimalStatus.compute(server, COW, true, NOW);
        assertEquals(AnimalStatus.Kind.YOUNG, young.kind());
        assertEquals(40 * H, young.remainingMs());
        assertFalse(young.estimated());

        AnimalLiveState born = AnimalLiveState.empty(ID).withBorn(NOW);
        AnimalStatus.Status estimate = AnimalStatus.compute(born, COW, true, NOW + H);
        assertEquals(AnimalStatus.Kind.YOUNG, estimate.kind());
        assertEquals(47 * H, estimate.remainingMs());
        assertTrue(estimate.estimated());
        assertEquals(-1, AnimalStatus.compute(born, null, true, NOW).remainingMs());
    }

    @Test
    void heartsMeanInLoveForThirtySecondsOnlyForAdults() {
        AnimalLiveState loved = info("Ja", "").withLoved(NOW + 1_000);
        assertEquals(AnimalStatus.Kind.IN_LOVE,
                AnimalStatus.compute(loved, COW, false, NOW + 20_000).kind());
        assertEquals(AnimalStatus.Kind.READY,
                AnimalStatus.compute(loved, COW, false, NOW + 31_001).kind());
        assertEquals(AnimalStatus.Kind.YOUNG,
                AnimalStatus.compute(loved, COW, true, NOW + 2_000).kind());
    }

    @Test
    void observedMatingEstimatesTheTableWaitUntilTheNextRealQuery() {
        AnimalLiveState mated = info("Ja", "").withBred(NOW + 5_000);
        AnimalStatus.Status estimate = AnimalStatus.compute(mated, COW, false, NOW + 5_000 + H);
        assertEquals(AnimalStatus.Kind.WAITING, estimate.kind());
        assertEquals(23 * H, estimate.remainingMs());
        assertTrue(estimate.estimated());
        assertEquals(AnimalStatus.Kind.READY,
                AnimalStatus.compute(mated, COW, false, NOW + 5_000 + 25 * H).kind(),
                "Estimated wait over: the animal is considered ready");

        AnimalLiveState requeried = mated.withInfo(new AnimalInfoSnapshot(
                "Rotvieh Rind", "Weiblich", "0/80", "in 20h | Mast 0/80", "", ""), NOW + 10_000);
        AnimalStatus.Status real = AnimalStatus.compute(requeried, COW, false, NOW + 10_000);
        assertEquals(20 * H, real.remainingMs());
        assertFalse(real.estimated(), "Server data replaces the estimate");
        assertEquals(0, requeried.bredAt());
    }

    @Test
    void matingOfASpeciesNotInTheTableCannotBeEstimated() {
        AnimalLiveState mated = AnimalLiveState.empty(ID).withBred(NOW);
        assertEquals(0, AnimalStatus.breedWaitUntil(mated, null));
        assertEquals(AnimalStatus.Kind.UNKNOWN, AnimalStatus.compute(mated, null, false, NOW).kind());
        assertEquals(AnimalStatus.Kind.UNKNOWN, AnimalStatus.compute(null, COW, false, NOW).kind());
    }

    @Test
    void shearingWaitComesFromTheServerOrOurOwnClick() {
        AnimalLiveState server = info("Ja", "in 5h 59m");
        assertEquals(NOW + 5 * H + 59 * 60_000L, AnimalStatus.shearWaitUntil(server));
        AnimalLiveState sheared = info("Ja", "Ja").withSheared(NOW + 1_000);
        assertEquals(NOW + 1_000 + 6 * H, AnimalStatus.shearWaitUntil(sheared));
        AnimalLiveState ready = sheared.withInfo(new AnimalInfoSnapshot(
                "Zaupel", "Weiblich", "0/30", "Ja", "Bereit", ""), NOW + 2_000);
        assertEquals(0, AnimalStatus.shearWaitUntil(ready));
    }

    @Test
    void glowPriorityIsShearedThenBredThenGender() {
        AnimalLiveState both = info("Ja", "").withBred(NOW + 1_000).withSheared(NOW + 1_000);
        long t = NOW + H;
        assertEquals(AnimalStatus.Glow.SHEARED,
                AnimalStatus.glow(true, true, true, false, both, COW, t));
        assertEquals(AnimalStatus.Glow.BREEDING,
                AnimalStatus.glow(false, true, true, false, both, COW, t));
        assertEquals(AnimalStatus.Glow.GENDER_ONLY,
                AnimalStatus.glow(false, false, true, false, both, COW, t));
        assertEquals(AnimalStatus.Glow.BREEDING,
                AnimalStatus.glow(true, true, false, false, both, COW, t),
                "Only sheep can show the shearing glow");
        assertEquals(AnimalStatus.Glow.GENDER_ONLY,
                AnimalStatus.glow(true, true, true, true, both, COW, t),
                "Babies never glow for breeding or shearing");
        assertEquals(AnimalStatus.Glow.BREEDING,
                AnimalStatus.glow(true, true, true, false, both, COW, NOW + 7 * H),
                "Shearing wait over, mating wait (24 h) still running");
        assertEquals(AnimalStatus.Glow.GENDER_ONLY,
                AnimalStatus.glow(true, true, true, false, both, COW, NOW + 25 * H));
        assertEquals(AnimalStatus.Glow.GENDER_ONLY,
                AnimalStatus.glow(true, true, true, false, null, COW, t));
    }

    @Test
    void glowColoursAreDistinctFromTheGenderColours() {
        int female = 0xFF79B8;
        int male = 0x68B8FF;
        for (int state : new int[]{AnimalStatus.BREEDING_COLOR, AnimalStatus.SHEARED_COLOR}) {
            assertNotEquals(female, state);
            assertNotEquals(male, state);
        }
        assertNotEquals(AnimalStatus.BREEDING_COLOR, AnimalStatus.SHEARED_COLOR);
        assertEquals(-1, AnimalStatus.color(AnimalStatus.Glow.GENDER_ONLY));
        assertEquals(-1, AnimalStatus.color(AnimalStatus.Glow.NONE));
        assertEquals(AnimalStatus.BREEDING_COLOR, AnimalStatus.color(AnimalStatus.Glow.BREEDING));
        assertEquals(AnimalStatus.SHEARED_COLOR, AnimalStatus.color(AnimalStatus.Glow.SHEARED));
    }
}
