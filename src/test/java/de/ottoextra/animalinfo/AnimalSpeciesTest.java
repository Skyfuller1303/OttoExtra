package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AnimalSpeciesTest {

    private static final long H = 3_600_000L;
    private static final long M = 60_000L;

    @Test
    void tableMatchesTheOttonienBreedingValues() {
        AnimalSpecies cow = AnimalSpecies.byName("Rotvieh Rind");
        assertEquals(24 * H, cow.cooldownMs());
        assertEquals(48 * H, cow.growMs());
        assertEquals(80, cow.breedMast());
        assertEquals(6 * H, AnimalSpecies.byName("Brakel").cooldownMs());
        assertEquals(20, AnimalSpecies.byName("Brakel").breedMast());
        assertEquals(18 * H, AnimalSpecies.byName("Hausschwein").cooldownMs());
        assertEquals(12 * H, AnimalSpecies.byName("Zaupel").cooldownMs());
        assertEquals(30, AnimalSpecies.byName("Graue Bergziege").breedMast());
        assertEquals(2 * H, AnimalSpecies.byName("Merciapferd").cooldownMs());
        assertEquals(40 * M, AnimalSpecies.byName("Standardpferd").cooldownMs());
        assertEquals(20 * M, AnimalSpecies.byName("Standardpferd").growMs());
        assertEquals(7, AnimalSpecies.byName("Standardpferd").breedMast());
        for (String horse : new String[]{"Ackergaul", "Reitpferd", "Streitross", "Packesel"}) {
            assertEquals(100, AnimalSpecies.byName(horse).breedMast(), horse);
            assertEquals(24 * H, AnimalSpecies.byName(horse).cooldownMs(), horse);
        }
    }

    @Test
    void namesAreMatchedLeniently() {
        assertSame(AnimalSpecies.byName("Rotvieh Rind"), AnimalSpecies.byName("rotvieh-rind"));
        assertSame(AnimalSpecies.byName("Rotvieh Rind"), AnimalSpecies.byName("  ROTVIEH   RIND "));
        assertNull(AnimalSpecies.byName("Wolf"));
        assertNull(AnimalSpecies.byName(""));
        assertNull(AnimalSpecies.byName(null));
    }

    @Test
    void storedSpeciesWinsOverTheVisibleName() {
        assertEquals("Hausschwein", AnimalSpecies.resolve("Hausschwein", "Rotvieh Rind").name());
        assertEquals("Rotvieh Rind", AnimalSpecies.resolve("", "Rotvieh Rind").name());
        assertNull(AnimalSpecies.resolve(null, "Lotte"));
    }

    @Test
    void shearingCooldownIsSixHours() {
        assertEquals(6 * H, AnimalSpecies.SHEAR_COOLDOWN_MS);
    }
}
