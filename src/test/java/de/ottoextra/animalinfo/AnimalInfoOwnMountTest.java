package de.ottoextra.animalinfo;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimalInfoOwnMountTest {
    @Test
    void ownMountIsHiddenByDefault() {
        assertTrue(new OttoExtraConfig.AnimalInfo().hideOwnMount);
        assertTrue(AnimalInfoRequestPlan.ownMountHidden(true, true));
        assertFalse(AnimalInfoRequestPlan.ownMountHidden(true, false));
        assertFalse(AnimalInfoRequestPlan.ownMountHidden(false, true));
    }
}
