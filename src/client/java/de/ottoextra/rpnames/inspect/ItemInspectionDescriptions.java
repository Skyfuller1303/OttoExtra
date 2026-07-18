package de.ottoextra.rpnames.inspect;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Liefert kurze, rein clientseitig ableitbare Informationen zu Gegenstaenden.
 * Custom-Name und vorhandene Lore haben immer Vorrang vor allgemeinen
 * OttoExtra-Kategorietexten. Es werden keine versteckten Serverdaten gelesen.
 */
final class ItemInspectionDescriptions {

    private static final int MAX_LORE_LINES = 2;
    private static final int MAX_VISIBLE_CHARS = 54;
    private static final int MAX_DETAIL_LINES = 5;

    private ItemInspectionDescriptions() {
    }

    /**
     * Baut die nach einer abgeschlossenen Untersuchung sichtbaren Zusatzzeilen.
     */
    static List<Text> details(ItemStack stack) {
        List<Text> details = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            details.add(Text.translatable("ottoextra.inspect.item.empty"));
            return details;
        }

        boolean hasVisibleLore = addLore(stack, details);
        addBookInformation(stack, details);

        if (!hasVisibleLore && details.isEmpty()) {
            details.add(describe(stack));
        }

        addCondition(stack, details);
        if (stack.getCount() > 1) {
            details.add(Text.translatable("ottoextra.inspect.item.amount", stack.getCount()));
        }

        if (details.size() > MAX_DETAIL_LINES) {
            return List.copyOf(details.subList(0, MAX_DETAIL_LINES));
        }
        return List.copyOf(details);
    }

    private static boolean addLore(ItemStack stack, List<Text> out) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return false;
        }

        int added = 0;
        for (Text line : lore.lines()) {
            if (line == null || line.getString().isBlank()) {
                continue;
            }
            out.add(shorten(line));
            added++;
            if (added >= MAX_LORE_LINES) {
                break;
            }
        }
        return added > 0;
    }

    private static void addBookInformation(ItemStack stack, List<Text> out) {
        WrittenBookContentComponent written = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            String author = written.author();
            if (author != null && !author.isBlank()) {
                out.add(Text.translatable("ottoextra.inspect.book.author", author));
            }
            out.add(Text.translatable(
                    "ottoextra.inspect.book.pages",
                    written.getPages(false).size()));
            return;
        }

        WritableBookContentComponent writable = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
        if (writable != null) {
            long pages = writable.stream(false).count();
            out.add(Text.translatable("ottoextra.inspect.book.draft"));
            out.add(Text.translatable("ottoextra.inspect.book.pages", pages));
        }
    }

    private static void addCondition(ItemStack stack, List<Text> out) {
        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return;
        }

        double remaining = 1.0 - (double) stack.getDamage() / (double) stack.getMaxDamage();
        String key;
        if (remaining >= 0.90) {
            key = "ottoextra.inspect.condition.new";
        } else if (remaining >= 0.65) {
            key = "ottoextra.inspect.condition.good";
        } else if (remaining >= 0.35) {
            key = "ottoextra.inspect.condition.used";
        } else if (remaining >= 0.12) {
            key = "ottoextra.inspect.condition.worn";
        } else {
            key = "ottoextra.inspect.condition.critical";
        }
        out.add(Text.translatable("ottoextra.inspect.condition", Text.translatable(key)));
    }

    private static Text shorten(Text text) {
        String plain = text.getString().strip();
        if (plain.length() <= MAX_VISIBLE_CHARS) {
            return text.copy();
        }
        return Text.literal(plain.substring(0, MAX_VISIBLE_CHARS - 1) + "…")
                .setStyle(text.getStyle());
    }

    static Text describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Text.translatable("ottoextra.inspect.item.empty");
        }

        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (stack.get(DataComponentTypes.FOOD) != null) {
            return Text.translatable("ottoextra.inspect.item.food");
        }
        if (stack.getItem() instanceof BlockItem) {
            return describeBlockPath(path);
        }
        if (containsAny(path, "sword", "dagger", "knife", "mace", "spear", "halberd")) {
            return Text.translatable("ottoextra.inspect.item.melee_weapon");
        }
        if (containsAny(path, "bow", "crossbow", "trident")) {
            return Text.translatable("ottoextra.inspect.item.ranged_weapon");
        }
        if (path.endsWith("_pickaxe") || path.contains("pickaxe")) {
            return Text.translatable("ottoextra.inspect.item.pickaxe");
        }
        if (path.endsWith("_axe") || path.contains("battle_axe")) {
            return Text.translatable("ottoextra.inspect.item.axe");
        }
        if (path.endsWith("_shovel") || path.contains("shovel")) {
            return Text.translatable("ottoextra.inspect.item.shovel");
        }
        if (path.endsWith("_hoe") || path.contains("scythe")) {
            return Text.translatable("ottoextra.inspect.item.farming_tool");
        }
        if (containsAny(path, "helmet", "chestplate", "leggings", "boots", "elytra")) {
            return Text.translatable("ottoextra.inspect.item.armor");
        }
        if (path.contains("shield")) {
            return Text.translatable("ottoextra.inspect.item.shield");
        }
        if (containsAny(path, "potion", "elixir", "tonic", "milk_bucket", "honey_bottle")) {
            return Text.translatable("ottoextra.inspect.item.drink");
        }
        if (containsAny(path, "book", "paper", "map", "letter", "scroll", "document")) {
            return Text.translatable("ottoextra.inspect.item.document");
        }
        if (containsAny(path, "spawn_egg")) {
            return Text.translatable("ottoextra.inspect.item.spawn_egg");
        }
        if (containsAny(path, "bucket", "bottle")) {
            return Text.translatable("ottoextra.inspect.item.container");
        }
        if (containsAny(path, "boat", "raft", "minecart", "saddle")) {
            return Text.translatable("ottoextra.inspect.item.transport");
        }
        if (containsAny(path, "music_disc", "instrument", "horn")) {
            return Text.translatable("ottoextra.inspect.item.music");
        }
        if (containsAny(path, "banner", "flag", "standard")) {
            return Text.translatable("ottoextra.inspect.item.banner");
        }
        if (containsAny(path, "dye", "paint", "pigment")) {
            return Text.translatable("ottoextra.inspect.item.dye");
        }
        if (containsAny(path, "seed", "sapling", "crop", "wheat", "carrot", "potato")) {
            return Text.translatable("ottoextra.inspect.item.plant");
        }
        if (containsAny(path, "ingot", "nugget", "gem", "diamond", "emerald", "coal",
                "ore", "raw_", "dust", "scrap", "shard", "crystal")) {
            return Text.translatable("ottoextra.inspect.item.material");
        }
        if (containsAny(path, "torch", "lantern", "candle")) {
            return Text.translatable("ottoextra.inspect.item.light");
        }
        if (containsAny(path, "clock", "compass", "spyglass")) {
            return Text.translatable("ottoextra.inspect.item.instrument");
        }

        return Text.translatable("ottoextra.inspect.item.generic");
    }

    static Text describe(BlockState state) {
        if (state == null || state.isAir()) {
            return Text.translatable("ottoextra.inspect.block.generic");
        }
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return describeBlockPath(id.getPath().toLowerCase(Locale.ROOT));
    }

    private static Text describeBlockPath(String path) {
        if (containsAny(path, "door", "trapdoor", "gate")) {
            return Text.translatable("ottoextra.inspect.block.entrance");
        }
        if (containsAny(path, "chest", "barrel", "shulker_box")) {
            return Text.translatable("ottoextra.inspect.block.storage");
        }
        if (containsAny(path, "furnace", "smoker", "blast_furnace", "campfire")) {
            return Text.translatable("ottoextra.inspect.block.workstation");
        }
        if (containsAny(path, "crafting_table", "smithing_table", "anvil", "grindstone",
                "stonecutter", "loom", "cartography_table")) {
            return Text.translatable("ottoextra.inspect.block.crafting");
        }
        if (containsAny(path, "sign", "hanging_sign")) {
            return Text.translatable("ottoextra.inspect.block.sign");
        }
        if (containsAny(path, "banner", "flag")) {
            return Text.translatable("ottoextra.inspect.block.banner");
        }
        if (containsAny(path, "flower", "sapling", "leaves", "grass", "crop", "mushroom")) {
            return Text.translatable("ottoextra.inspect.block.plant");
        }
        if (containsAny(path, "ore")) {
            return Text.translatable("ottoextra.inspect.block.ore");
        }
        if (containsAny(path, "torch", "lantern", "candle", "lamp")) {
            return Text.translatable("ottoextra.inspect.block.light");
        }
        return Text.translatable("ottoextra.inspect.block.generic");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
