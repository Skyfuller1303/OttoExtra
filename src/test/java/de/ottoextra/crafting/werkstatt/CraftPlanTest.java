package de.ottoextra.crafting.werkstatt;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CraftPlanTest {
    private static final RecipeLore EISENBARREN = RecipeLore.parse(RecipeLoreTest.EISENBARREN);
    private static final RecipeLore FEUER = RecipeLore.parse(RecipeLoreTest.FEUER);
    private static final CraftPlan.Fuel STICKS = new CraftPlan.Fuel("Stock", 15, FEUER.groups());
    private static final CraftPlan.Fuel COAL = new CraftPlan.Fuel("Steinkohle", 40,
            List.of(List.of(new RecipeLore.Ingredient(1, "Steinkohle"))));

    @Test void maxWithoutFireIsLimitedByTheScarcestIngredient() {
        Map<String, Integer> inv = Map.of("Eisenerz", 96, "Steinkohle", 44, "Torf", 3);
        // 24 by ore; coal 44/2 = 22, then peat 3 more.
        assertEquals(24, CraftPlan.max(EISENBARREN, inv, null, 0, 10));
        CraftPlan.Result r = CraftPlan.simulate(EISENBARREN, inv, 24, null, 0, 10);
        assertEquals(96, r.used().get("Eisenerz"), "22 crafts on coal and 2 on peat use all 96 ore");
        assertEquals(44, r.used().get("Steinkohle"));
        assertEquals(2, r.used().get("Torf"));
        assertTrue(r.alternatives().contains("Torf"));
    }

    @Test void firstAlternativeIsUsedWhileThereIsEnough() {
        CraftPlan.Result r = CraftPlan.simulate(EISENBARREN, Map.of("Eisenerz", 40, "Steinkohle", 40, "Torf", 10),
                5, null, 0, 10);
        assertEquals(5, r.crafts());
        assertNull(r.used().get("Torf"));
        assertTrue(r.alternatives().isEmpty());
    }

    @Test void fireCostCountsAgainstTheSameStock() {
        // Sticks: one fire (15 s) per craft of 10 s plus the 3 s reserve.
        Map<String, Integer> inv = Map.of("Eisenerz", 400, "Steinkohle", 200, "Stock", 9);
        CraftPlan.Result r = CraftPlan.simulate(EISENBARREN, inv, 100, STICKS, 0, 10);
        assertTrue(r.fuelMissing());
        assertEquals("Stock", r.missing());
        assertEquals(3, r.fires());
        assertTrue(r.crafts() >= 3);
    }

    @Test void coalAsFuelAndIngredientIsFlaggedAndShared() {
        Map<String, Integer> inv = Map.of("Eisenerz", 64, "Steinkohle", 36);
        CraftPlan.Result r = CraftPlan.simulate(EISENBARREN, inv, 16, COAL, 0, 10);
        assertTrue(r.fuelIsIngredient());
        assertEquals(16, r.crafts());
        assertEquals(32 + r.fires(), r.used().get("Steinkohle"));
        assertTrue(r.fires() >= 4);
    }

    @Test void unknownIngredientsGiveNoMax() {
        assertEquals(0, CraftPlan.max(RecipeLore.parse(RecipeLoreTest.STEMMEISEN), Map.of("Eisenerz", 10), null, 0, 10));
        assertEquals(0, CraftPlan.max(null, Map.of(), null, 0, 10));
        assertEquals(0, CraftPlan.max(EISENBARREN, null, null, 0, 10));
    }

    @Test void inventoryNamesAreNormalised() {
        assertEquals(1, CraftPlan.max(EISENBARREN, Map.of("§fEISENERZ", 4, "torf", 1), null, 0, 10));
    }

    @Test void fireClockBurnsDownAndStacks() {
        FireClock f = new FireClock();
        assertTrue(f.needsRefuel(0));
        f.add(15, 0);
        assertEquals(15, f.secondsLeft(0));
        assertFalse(f.needsRefuel(11_000));
        assertTrue(f.needsRefuel(12_500));
        f.add(15, 5_000);
        assertEquals(25, f.secondsLeft(5_000));
        f.reset();
        assertEquals(0, f.secondsLeft(5_000));
    }
}
