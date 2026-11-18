package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalToastModelTest {

    private static final long H = 3_600_000L;
    private static final AnimalSpecies COW = AnimalSpecies.byName("Rotvieh Rind");
    private static final AnimalInfoSnapshot COW_INFO =
            new AnimalInfoSnapshot("Rotvieh Rind", "Kuh (Weiblich)", "32/80", "Mast 32/80", "", "");

    private static AnimalStatus.Status status(AnimalStatus.Kind kind, long remaining) {
        return new AnimalStatus.Status(kind, remaining, false, -1, -1);
    }

    @Test
    void everyStatusHasItsOwnColourRamp() {
        assertArrayEquals(new int[]{0xFF2F5426, 0xFF5E9A3E, 0xFF86C25A},
                AnimalToastModel.ramp(AnimalStatus.Kind.READY));
        assertArrayEquals(new int[]{0xFF6E2226, 0xFFB24147, 0xFFC4545A},
                AnimalToastModel.ramp(AnimalStatus.Kind.NEED_MAST));
        assertArrayEquals(new int[]{0xFF96752C, 0xFFCD9F3D, 0xFFDBB451},
                AnimalToastModel.ramp(AnimalStatus.Kind.WAITING));
        assertArrayEquals(new int[]{0xFF214E66, 0xFF349DBA, 0xFF5FB8CF},
                AnimalToastModel.ramp(AnimalStatus.Kind.YOUNG));
        assertArrayEquals(new int[]{0xFF6E2A6A, 0xFFB03AA6, 0xFFE040D0},
                AnimalToastModel.ramp(AnimalStatus.Kind.IN_LOVE));
        assertArrayEquals(new int[]{0xFF3B3A45, 0xFF514F59, 0xFFA9A6B0},
                AnimalToastModel.ramp(AnimalStatus.Kind.UNKNOWN));
        assertEquals(AnimalStatus.Kind.values().length, java.util.Arrays.stream(AnimalStatus.Kind.values())
                .map(k -> java.util.Arrays.hashCode(AnimalToastModel.ramp(k))).distinct().count());
    }

    @Test
    void loveRampUsesTheGlowColour() {
        assertEquals(AnimalStatus.BREEDING_COLOR | 0xFF000000,
                AnimalToastModel.ramp(AnimalStatus.Kind.IN_LOVE)[2]);
    }

    @Test
    void mastBarIsRedUntilEnoughIsFed() {
        AnimalToastModel need = AnimalToastModel.of(status(AnimalStatus.Kind.NEED_MAST, -1), COW_INFO, COW);
        assertEquals(AnimalToastModel.Bar.MAST, need.bar());
        assertEquals(32, need.mast());
        assertEquals(80, need.mastNeed());
        assertEquals(0.4f, need.barFraction(), 1e-6);
        assertFalse(need.mastEnough());
        assertArrayEquals(AnimalToastModel.ramp(AnimalStatus.Kind.NEED_MAST), AnimalToastModel.mastRamp(false));
        AnimalInfoSnapshot full = new AnimalInfoSnapshot("Rotvieh Rind", "Kuh", "80/80", "Ja", "", "");
        AnimalToastModel ready = AnimalToastModel.of(status(AnimalStatus.Kind.READY, -1), full, COW);
        assertTrue(ready.mastEnough());
        assertEquals(1f, ready.barFraction());
        assertArrayEquals(AnimalToastModel.ramp(AnimalStatus.Kind.READY), AnimalToastModel.mastRamp(true));
    }

    @Test
    void serverMaximumOverridesTheTableRequirement() {
        AnimalInfoSnapshot horse = new AnimalInfoSnapshot("Brakel", "Henne", "12/30", "Ja", "", "");
        assertEquals(30, AnimalToastModel.of(status(AnimalStatus.Kind.READY, -1), horse,
                AnimalSpecies.byName("Brakel")).mastNeed(), "Server maximum has priority");
        AnimalInfoSnapshot noMax = new AnimalInfoSnapshot("Brakel", "Henne", "Ja");
        assertEquals(-1, AnimalToastModel.of(status(AnimalStatus.Kind.READY, -1), noMax,
                AnimalSpecies.byName("Brakel")).mastNeed());
        assertEquals(30, AnimalToastModel.of(status(AnimalStatus.Kind.READY, -1), horse, null).mastNeed());
    }

    @Test
    void waitingShowsCooldownProgressWhenTheSpeciesIsKnown() {
        AnimalToastModel model = AnimalToastModel.of(
                status(AnimalStatus.Kind.WAITING, 6 * H), COW_INFO, COW);
        assertEquals(AnimalToastModel.Bar.COOLDOWN, model.bar());
        assertEquals(0.75f, model.barFraction(), 1e-6);
        assertEquals(AnimalToastModel.Bar.MAST,
                AnimalToastModel.of(status(AnimalStatus.Kind.WAITING, 6 * H), COW_INFO, null).bar(),
                "Without table values the cooldown total is unknown");
        assertEquals(0f, AnimalToastModel.of(status(AnimalStatus.Kind.WAITING, 99 * H), COW_INFO, COW)
                .barFraction());
    }

    @Test
    void youngShowsGrowthProgress() {
        AnimalToastModel model = AnimalToastModel.of(status(AnimalStatus.Kind.YOUNG, 12 * H), COW_INFO, COW);
        assertEquals(AnimalToastModel.Bar.GROWTH, model.bar());
        assertEquals(0.75f, model.barFraction(), 1e-6);
    }

    @Test
    void noFeedingValueMeansNoBarAndNoMastElement() {
        AnimalInfoSnapshot bare = new AnimalInfoSnapshot("Rotvieh Rind", "Kuh", "Ja");
        AnimalToastModel model = AnimalToastModel.of(status(AnimalStatus.Kind.READY, -1), bare, COW);
        assertEquals(AnimalToastModel.Bar.NONE, model.bar());
        assertFalse(model.hasMast());
    }

    @Test
    void genderTermIsTheServerWordOnly() {
        assertEquals("Kuh", AnimalToastModel.genderTerm("Kuh (Weiblich)"));
        assertEquals("Bock", AnimalToastModel.genderTerm(" Bock (Männlich) "));
        assertEquals("Hengst", AnimalToastModel.genderTerm("Hengst"));
        assertEquals("", AnimalToastModel.genderTerm("Weiblich"));
        assertEquals("", AnimalToastModel.genderTerm("Männlich (Männlich)"));
        assertEquals("", AnimalToastModel.genderTerm(null));
        assertEquals("", AnimalToastModel.genderTerm(""));
    }
}
