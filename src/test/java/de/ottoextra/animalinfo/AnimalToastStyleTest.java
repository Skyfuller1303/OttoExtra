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
    void customisedLayoutsKeepTheirOwnToast() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.restoreFrom("{\"animalInfo\":{\"layouts\":{\"sheep\":{\"titleX\":40}}}}");
        assertEquals("custom", config.animalInfo.toastStyle);
        OttoExtraConfig toggled = new OttoExtraConfig();
        toggled.restoreFrom("{\"animalInfo\":{\"layouts\":{\"pig\":{\"showTimer\":false}}}}");
        assertEquals("custom", toggled.animalInfo.toastStyle);
        OttoExtraConfig scaled = new OttoExtraConfig();
        scaled.restoreFrom("{\"animalInfo\":{\"layouts\":{\"cow\":{\"mastScale\":1.2}}}}");
        assertEquals("custom", scaled.animalInfo.toastStyle);
    }

    @Test
    void chosenStyleIsKeptAndUnknownValuesAreReplaced() {
        OttoExtraConfig compact = new OttoExtraConfig();
        compact.restoreFrom("{\"animalInfo\":{\"toastStyle\":\"compact\"}}");
        assertEquals("compact", compact.animalInfo.toastStyle);
        OttoExtraConfig bogus = new OttoExtraConfig();
        bogus.restoreFrom("{\"animalInfo\":{\"toastStyle\":\"fancy\"}}");
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
