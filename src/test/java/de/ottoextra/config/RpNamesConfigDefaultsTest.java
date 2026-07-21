package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RpNamesConfigDefaultsTest {
    @Test
    void tablistRegionSortingIsOptIn() {
        assertFalse(new OttoExtraConfig().rpnames.tablistSortByRegion);
    }
}
