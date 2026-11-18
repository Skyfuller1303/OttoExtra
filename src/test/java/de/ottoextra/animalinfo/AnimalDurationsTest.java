package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimalDurationsTest {

    @Test
    void parsesServerDurations() {
        assertEquals(((24 + 16) * 3600L) * 1000L, AnimalDurations.parse("wächst noch 1d 16h"));
        assertEquals((22 * 60 + 3) * 1000L, AnimalDurations.parse("in 22m 3s"));
        assertEquals(5 * 3_600_000L, AnimalDurations.parse("5h"));
        assertEquals((23 * 3600 + 59 * 60) * 1000L, AnimalDurations.parse("in 23h 59m | Mast 0/80"));
    }

    @Test
    void textWithoutDurationYieldsMinusOne() {
        assertEquals(-1, AnimalDurations.parse("Mast 65/80"));
        assertEquals(-1, AnimalDurations.parse("Bereit"));
        assertEquals(-1, AnimalDurations.parse(""));
        assertEquals(-1, AnimalDurations.parse(null));
    }

    @Test
    void absurdValuesAreCapped() {
        assertEquals(400L * 86_400_000L, AnimalDurations.parse("99999d"));
    }

    @Test
    void formatsRoundingUp() {
        assertEquals("1d 16h", AnimalDurations.format(40 * 3_600_000L));
        assertEquals("3h 20m", AnimalDurations.format((3 * 60 + 20) * 60_000L));
        assertEquals("5m 3s", AnimalDurations.format((5 * 60 + 3) * 1000L));
        assertEquals("1s", AnimalDurations.format(1));
        assertEquals("0s", AnimalDurations.format(-5));
    }

    @Test
    void formatsAgeRoundingDown() {
        assertEquals("42s", AnimalDurations.formatAge(42_999L));
        assertEquals("5m", AnimalDurations.formatAge(5 * 60_000L + 59_000L));
        assertEquals("2h 5m", AnimalDurations.formatAge((2 * 60 + 5) * 60_000L));
        assertEquals("3d 4h", AnimalDurations.formatAge((3 * 24 + 4) * 3_600_000L));
        assertEquals("0s", AnimalDurations.formatAge(-1));
    }
}
