package de.ottoextra.tweaks.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.List;

/** Vanilla weapon capability plus Ottonien's item models, lore and legacy CIT names. */
public final class WeaponClassifier {
    private WeaponClassifier() { }

    public static WeaponFamily classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        return classify(id, stack.isIn(ItemTags.SWORDS), stack.isIn(ItemTags.SPEARS),
                model == null ? "" : model.toString(), stack.getName().getString(),
                lore == null ? List.of() : lore.lines().stream().map(line -> line.getString()).toList());
    }

    static WeaponFamily classify(String itemId, boolean sword, boolean spear,
                                  String model, String name, List<String> lore) {
        if ("minecraft:bow".equals(itemId) || "minecraft:crossbow".equals(itemId)) {
            return WeaponFamily.RANGED;
        }
        if ("minecraft:mace".equals(itemId)) return WeaponFamily.BLUNT;
        if (spear || "minecraft:trident".equals(itemId)) return WeaponFamily.POLEARM;
        // A paper menu icon or a renamed tool must never enable the weapon HUD.
        if (!sword) return null;

        WeaponFamily modeled = fromName(model);
        if (modeled != null) return modeled;
        for (String line : lore) {
            String value = CombatMode.normalize(line);
            if (has(value, "stumpfwaffe", "stumpfwaffen", "stumpfe waffe", "stumpfwaffenkampf")) {
                return WeaponFamily.BLUNT;
            }
            if (has(value, "stangenwaffe", "stangenwaffen", "stangenwaffenkampf")) {
                return WeaponFamily.POLEARM;
            }
            if (has(value, "klingenwaffe", "klingenwaffen", "klingenwaffenkampf")) {
                return WeaponFamily.BLADE;
            }
        }
        WeaponFamily named = fromName(name);
        return named == null ? WeaponFamily.BLADE : named;
    }

    private static WeaponFamily fromName(String raw) {
        String value = CombatMode.normalize(raw);
        if (has(value, "streitkolben", "streitflegel", "flegel", "pruegel", "dornenpruegel",
                "pilgerstock", "nudelholz", "keule", "morgenstern", "streithammer", "hammer",
                "anstifter von bruch und zwietracht")) return WeaponFamily.BLUNT;
        if (has(value, "spiess", "hakenspiess", "speer", "lanze", "fluegellanze",
                "hellebarde", "hippe", "hirtenstab", "mistgabel")) return WeaponFamily.POLEARM;
        if (has(value, "schwert", "bastardschwert", "holzschwert", "langschwert", "dolch",
                "nierendolch", "messer", "saebel", "spatha", "bauernwehr", "landwehr",
                "feldsichel")) return WeaponFamily.BLADE;
        return null;
    }

    private static boolean has(String value, String... names) {
        String padded = " " + value + " ";
        for (String name : names) {
            if (padded.contains(" " + name + " ")) return true;
        }
        return false;
    }
}
