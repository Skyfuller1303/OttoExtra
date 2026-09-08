package de.ottoextra.playerlist;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListSortingModuleTest {

    @Test
    void activationWatcherStartsEvenWhenRelatedOptionsAreInitiallyDisabled() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.regions.enabled = false;
        config.rpnames.enabled = false;
        config.rpnames.tablistSortByRegion = false;

        assertTrue(new PlayerListSortingModule().enabled(config));
    }
}
