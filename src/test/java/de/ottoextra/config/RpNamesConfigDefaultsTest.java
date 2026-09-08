package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpNamesConfigDefaultsTest {
    @Test
    void quickAccessDefaultsRemainStable() {
        OttoExtraConfig.RpNames config = new OttoExtraConfig().rpnames;

        assertFalse(config.tablistEnabled);
        assertFalse(config.tablistSortByRegion);
        assertFalse(config.openBookOnClick);
        assertTrue(config.proactiveMeet);
    }

    @Test
    void existingExplicitTablistChoiceIsPreserved() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("{\"rpnames\":{\"tablistEnabled\":true}}");

        assertTrue(config.rpnames.tablistEnabled);
    }
}
