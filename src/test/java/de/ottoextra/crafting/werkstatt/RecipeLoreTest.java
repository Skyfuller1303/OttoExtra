package de.ottoextra.crafting.werkstatt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lore samples are the live ones from MinecraftAgent/qa/0.4.6/live-recipes.json. */
class RecipeLoreTest {
    static final List<String> EISENBARREN = List.of("Gewöhnlich", "", "Ein schwerer Barren aus Eisen.", "",
            "Herstellen:", "4 x Eisenerz", "2 x Steinkohle oder 1 x Torf", "", "Benötigt: Hitze");
    static final List<String> FEUER = List.of("", "Klicke um ein Feuer zu entfachen", "",
            "Entzündet ein Feuer in der Schmiede, um", "Gegenstände herzustellen, die Hitze", "benötigen.", "",
            "+ 0:15 Brenndauer", "", "Benötigt:", "3 x Stock", "");
    static final List<String> STEMMEISEN = List.of("Gewöhnlich", "", "Hebelt Truhen und Türen auf.", "",
            "Siehe Werkstattbuch.");

    @Test void readsRecipeWithAlternativesAndHeat() {
        RecipeLore r = RecipeLore.parse(EISENBARREN);
        assertTrue(r.known());
        assertEquals("Gewöhnlich", r.rarity());
        assertEquals(List.of("Ein schwerer Barren aus Eisen."), r.description());
        assertEquals(2, r.groups().size());
        assertEquals(new RecipeLore.Ingredient(4, "Eisenerz"), r.groups().get(0).get(0));
        assertEquals(List.of(new RecipeLore.Ingredient(2, "Steinkohle"), new RecipeLore.Ingredient(1, "Torf")),
                r.groups().get(1));
        assertEquals(RecipeLore.Heat.YES, r.heat());
        assertFalse(r.isFire());
        assertTrue(r.isCraftable());
    }

    @Test void readsFireButtonBurnTimeAndCost() {
        RecipeLore r = RecipeLore.parse(FEUER);
        assertTrue(r.isFire());
        assertEquals(15, r.burnSeconds());
        assertEquals(List.of(List.of(new RecipeLore.Ingredient(3, "Stock"))), r.groups());
        assertEquals(RecipeLore.Heat.NO, r.heat(), "the word Hitze in the description is no heat requirement");
    }

    @Test void loreWithoutIngredientBlockIsUnknown() {
        RecipeLore r = RecipeLore.parse(STEMMEISEN);
        assertFalse(r.known());
        assertFalse(r.isCraftable());
        assertEquals(RecipeLore.Heat.UNKNOWN, r.heat());
        assertTrue(RecipeLore.parse(List.of()).equals(RecipeLore.EMPTY));
        assertFalse(RecipeLore.parse(null).known());
    }

    @Test void unreadableIngredientLineMakesTheIngredientsUnknown() {
        RecipeLore r = RecipeLore.parse(List.of("Gewöhnlich", "", "Herstellen:", "4 x Eisenerz", "etwas Kohle"));
        assertFalse(r.known());
    }

    @Test void longBurnTimesAndColourCodes() {
        assertEquals(40, RecipeLore.parse(List.of("§7+ 0:40 Brenndauer", "Benötigt:", "1 x Steinkohle")).burnSeconds());
        assertEquals(3723, RecipeLore.parse(List.of("+ 1:02:03 Brenndauer", "Benötigt:", "1 x Torf")).burnSeconds());
    }

    @Test void stationTitlesAndRefusals() {
        assertTrue(WerkstattText.isStationTitle("Zeugtisch"));
        assertTrue(WerkstattText.isStationTitle("§8Zeugstisch"));
        assertTrue(WerkstattText.isStationTitle("Zeugtisch (Seite 2)"));
        assertTrue(WerkstattText.isStationTitle("Rüstbank"));
        assertFalse(WerkstattText.isStationTitle("Truhe"));
        assertTrue(WerkstattText.isRefusal("Du hast nicht genug Eisenerz!"));
        assertTrue(WerkstattText.isRefusal("Kein Feuer in der Schmiede"));
        assertFalse(WerkstattText.isRefusal("Die Herstellung ist erfolgreich!"));
        assertEquals("0:09", WerkstattText.clock(9));
        assertEquals("2:48", WerkstattText.clock(168));
    }
}
