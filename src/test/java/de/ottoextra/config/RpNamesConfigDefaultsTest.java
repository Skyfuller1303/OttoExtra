package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpNamesConfigDefaultsTest {
    @Test
    void quickAccessDefaultsRemainStable() {
        OttoExtraConfig.RpNames config = new OttoExtraConfig().rpnames;

        assertFalse(config.tablistSortByRegion);
        assertFalse(config.openBookOnClick);
        assertTrue(config.proactiveMeet);
    }
}
