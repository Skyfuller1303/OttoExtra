package de.ottoextra.animalinfo;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalToastStyleTest {

    @Test
    void parsesIdsAndFallsBackToDetailed() {
        assertArrayEquals(new String[]{"compact", "detailed", "custom"}, AnimalToastStyle.ids());
        assertEquals(AnimalToastStyle.COMPACT, AnimalToastStyle.parse("compact"));
        assertEquals(AnimalToastStyle.CUSTOM, AnimalToastStyle.parse(" Custom "));
        assertEquals(AnimalToastStyle.DETAILED, AnimalToastStyle.parse(null));
        assertEquals(AnimalToastStyle.DETAILED, AnimalToastStyle.parse("bogus"));
        assertTrue(AnimalToastStyle.isKnown("detailed"));
        assertFalse(AnimalToastStyle.isKnown("bogus"));
    }

    @Test
    void freshInstallGetsTheDetailedStyle() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.restoreFrom("{}");
        assertEquals("detailed", config.animalInfo.toastStyle);
        assertEquals(AnimalToastStyle.DETAILED, config.animalInfo.toastStyle());
    }

    @Test
    void untouchedExistingLayoutsMigrateToDetailed() {
        OttoExtraConfig config = new OttoExtraConfig();
        OttoExtraConfig saved = new OttoExtraConfig();
        saved.animalInfo.layout("cow").enabled = false;
        String json = saved.snapshotJson().replaceAll("\"toastStyle\"\\s*:\\s*\"[a-z]+\",?", "");
        config.restoreFrom(json);
        assertEquals("detailed", config.animalInfo.toastStyle,
                "Switching an animal type off is not a layout customisation");
    }

    @Test
    void everyoneStartsOnTheDetailedToastOnce() {
        OttoExtraConfig customised = new OttoExtraConfig();
        customised.restoreFrom("{\"animalInfo\":{\"toastStyle\":\"custom\",\"layouts\":{\"sheep\":{\"titleX\":40}}}}");
        assertEquals("detailed", customised.animalInfo.toastStyle);
        assertEquals(1, customised.animalInfo.toastStyleVersion);
        OttoExtraConfig fresh = new OttoExtraConfig();
        fresh.restoreFrom("{\"animalInfo\":{}}");
        assertEquals("detailed", fresh.animalInfo.toastStyle);
    }

    @Test
    void chosenStyleIsKeptAfterTheSwitchAndUnknownValuesAreReplaced() {
        OttoExtraConfig compact = new OttoExtraConfig();
        compact.restoreFrom("{\"animalInfo\":{\"toastStyle\":\"compact\",\"toastStyleVersion\":1}}");
        assertEquals("compact", compact.animalInfo.toastStyle);
        OttoExtraConfig custom = new OttoExtraConfig();
        custom.restoreFrom("{\"animalInfo\":{\"toastStyle\":\"custom\",\"toastStyleVersion\":1}}");
        assertEquals("custom", custom.animalInfo.toastStyle);
        OttoExtraConfig bogus = new OttoExtraConfig();
        bogus.restoreFrom("{\"animalInfo\":{\"toastStyle\":\"fancy\",\"toastStyleVersion\":1}}");
        assertEquals("detailed", bogus.animalInfo.toastStyle);
    }

    @Test
    void cardsHaveTheDraftSizes() {
        assertEquals(196, AnimalToastCards.width(AnimalToastStyle.COMPACT));
        assertEquals(30, AnimalToastCards.height(AnimalToastStyle.COMPACT));
        assertEquals(160, AnimalToastCards.width(AnimalToastStyle.DETAILED));
        assertEquals(62, AnimalToastCards.height(AnimalToastStyle.DETAILED));
    }
}
