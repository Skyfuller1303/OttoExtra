package de.ottoextra.crafting.werkstatt;

import de.ottoextra.crafting.werkstatt.RecipeLore.Ingredient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simulates crafts against an inventory, craft by craft, so shared items (coal as ingredient and
 * fuel) and alternatives are counted right. For every ingredient group the first alternative is
 * used while there is enough of it; only when it runs short the next one is taken.
 *
 * <p>Fire: a recipe that needs heat gets a fire click whenever the estimated fire is below
 * {@link FireClock#REFUEL_BELOW_SECONDS} before a craft; each craft is assumed to burn
 * {@code craftSeconds} of fire.
 */
public final class CraftPlan {
    /** Upper bound of the "Max" button. */
    public static final int MAX_CRAFTS = 999;

    /** A fire button: its name (fuel item), burn time and cost per click. */
    public record Fuel(String name, int burnSeconds, List<List<Ingredient>> cost) {
        public boolean usable() {
            return burnSeconds > 0 && cost != null && !cost.isEmpty();
        }
    }

    /**
     * @param crafts         number of crafts that fit (at most the wanted number)
     * @param fires          fire clicks needed for them
     * @param used           items used, by display name, in lore order
     * @param alternatives   display names of items used instead of a group's first choice
     * @param missing        display name of the item that ran short first, null if all wanted crafts fit
     * @param fuelMissing    true if the fuel ran short first
     * @param fuelIsIngredient true if the fuel is also an ingredient of the recipe
     */
    public record Result(int crafts, int fires, Map<String, Integer> used, Set<String> alternatives,
                         String missing, boolean fuelMissing, boolean fuelIsIngredient) {
    }

    private CraftPlan() {
    }

    /** Largest number of crafts (up to {@link #MAX_CRAFTS}); 0 for unknown ingredients. */
    public static int max(RecipeLore recipe, Map<String, Integer> inventory, Fuel fuel, int fireSecondsLeft,
                          int craftSeconds) {
        return simulate(recipe, inventory, MAX_CRAFTS, fuel, fireSecondsLeft, craftSeconds).crafts();
    }

    /**
     * Runs up to {@code wanted} crafts. {@code inventory} maps normalised names
     * ({@link WerkstattText#normalize}) to counts; it is not modified. {@code fuel} may be null (no
     * fire accounting). Unknown ingredients give 0 crafts.
     */
    public static Result simulate(RecipeLore recipe, Map<String, Integer> inventory, int wanted, Fuel fuel,
                                  int fireSecondsLeft, int craftSeconds) {
        Map<String, Integer> used = new LinkedHashMap<>();
        Set<String> alternatives = new LinkedHashSet<>();
        if (recipe == null || !recipe.known() || wanted <= 0) {
            return new Result(0, 0, used, alternatives, null, false, false);
        }
        Map<String, Integer> left = new HashMap<>();
        if (inventory != null) {
            for (Map.Entry<String, Integer> e : inventory.entrySet()) {
                left.merge(WerkstattText.normalize(e.getKey()), Math.max(0, e.getValue() == null ? 0 : e.getValue()),
                        Integer::sum);
            }
        }
        boolean heat = recipe.heat() == RecipeLore.Heat.YES && fuel != null && fuel.usable();
        boolean fuelIsIngredient = heat && shares(recipe.groups(), fuel.cost());
        int fire = Math.max(0, fireSecondsLeft);
        int crafts = 0;
        int fires = 0;
        String missing = null;
        boolean fuelMissing = false;
        while (crafts < wanted) {
            Map<String, Integer> trial = new HashMap<>(left);
            Map<String, Integer> trialUsed = new LinkedHashMap<>();
            Set<String> trialAlt = new LinkedHashSet<>();
            int trialFire = fire;
            int trialFires = 0;
            if (heat) {
                while (trialFire < FireClock.REFUEL_BELOW_SECONDS) {
                    String short1 = take(fuel.cost(), trial, trialUsed, trialAlt);
                    if (short1 != null) {
                        fuelMissing = true;
                        missing = short1;
                        break;
                    }
                    trialFire += fuel.burnSeconds();
                    trialFires++;
                }
                if (fuelMissing) {
                    break;
                }
            }
            String short2 = take(recipe.groups(), trial, trialUsed, trialAlt);
            if (short2 != null) {
                missing = short2;
                break;
            }
            left = trial;
            trialUsed.forEach((k, v) -> used.merge(k, v, Integer::sum));
            alternatives.addAll(trialAlt);
            fire = heat ? Math.max(0, trialFire - Math.max(0, craftSeconds)) : fire;
            fires += trialFires;
            crafts++;
        }
        return new Result(crafts, fires, used, alternatives, missing, fuelMissing, fuelIsIngredient);
    }

    /** Takes one set of a cost list from {@code stock}; returns the short item's name or null. */
    private static String take(List<List<Ingredient>> groups, Map<String, Integer> stock, Map<String, Integer> used,
                               Set<String> alternatives) {
        for (List<Ingredient> group : groups) {
            Ingredient chosen = null;
            for (Ingredient option : group) {
                if (stock.getOrDefault(option.key(), 0) >= option.count()) {
                    chosen = option;
                    break;
                }
            }
            if (chosen == null) {
                return group.get(0).name();
            }
            if (chosen != group.get(0)) {
                alternatives.add(chosen.name());
            }
            stock.merge(chosen.key(), -chosen.count(), Integer::sum);
            used.merge(chosen.name(), chosen.count(), Integer::sum);
        }
        return null;
    }

    private static boolean shares(List<List<Ingredient>> recipe, List<List<Ingredient>> fuel) {
        Set<String> keys = new java.util.HashSet<>();
        for (List<Ingredient> g : recipe) {
            for (Ingredient i : g) {
                keys.add(i.key());
            }
        }
        for (List<Ingredient> g : fuel) {
            for (Ingredient i : g) {
                if (keys.contains(i.key())) {
                    return true;
                }
            }
        }
        return false;
    }
}
