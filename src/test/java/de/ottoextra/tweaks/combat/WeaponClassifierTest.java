package de.ottoextra.tweaks.combat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeaponClassifierTest {
    @ParameterizedTest
    @ValueSource(strings = {"Streitkolben", "Nobler Streitkolben", "Dornenprügel", "Pilgerstock",
            "Prügel", "Nudelholz", "Streitflegel", "Flegel", "Holderner Dornenprügel",
            "Österlicher Anstifter von Bruch und Zwietracht"})
    void recognizesOttonienBluntWeaponsThatUseSwordItems(String name) {
        assertEquals(WeaponFamily.BLUNT, sword("", "§6" + name, List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spieß", "Hakenspieß", "Speer", "Hellebarde", "Hippe", "Hirtenstab",
            "Holderner Hellebarde", "Holderner Lanze", "Mistgabel", "Verzierter Hirtenstab"})
    void recognizesOttonienPolearmsThatUseSwordItems(String name) {
        assertEquals(WeaponFamily.POLEARM, sword("", name, List.of()));
    }

    @Test
    void modelAndCategoryLoreCanIdentifyRenamedWeapons() {
        assertEquals(WeaponFamily.BLUNT, sword("ottonien:item/streitkolben", "Abendstern", List.of()));
        assertEquals(WeaponFamily.POLEARM, sword("ottonien:item/hakenspiess", "Dorn", List.of()));
        assertEquals(WeaponFamily.POLEARM, sword("", "Dorn", List.of("§7Waffenart: Stangenwaffe")));
        assertEquals(WeaponFamily.BLADE, sword("ottonien:item/bastardschwert", "Streitkolben", List.of()));
    }

    @Test
    void supportsSwordsSpearsBowsAndCrossbowsButRejectsToolsAndMenuIcons() {
        assertEquals(WeaponFamily.BLADE, sword("", "Bastardschwert", List.of()));
        assertEquals(WeaponFamily.POLEARM, WeaponClassifier.classify("minecraft:iron_spear", false, true, "", "", List.of()));
        assertEquals(WeaponFamily.RANGED, WeaponClassifier.classify("minecraft:bow", false, false, "", "Jagdbogen", List.of()));
        assertEquals(WeaponFamily.RANGED, WeaponClassifier.classify("minecraft:crossbow", false, false, "", "Armbrust", List.of()));
        for (String id : List.of("minecraft:paper", "minecraft:air", "minecraft:iron_pickaxe", "minecraft:shield")) {
            assertNull(WeaponClassifier.classify(id, false, false, "ottonien:gui/stumpfwaffenkampf", "Streitkolben", List.of()));
        }
    }

    private static WeaponFamily sword(String model, String name, List<String> lore) {
        return WeaponClassifier.classify("minecraft:diamond_sword", true, false, model, name, lore);
    }
}
