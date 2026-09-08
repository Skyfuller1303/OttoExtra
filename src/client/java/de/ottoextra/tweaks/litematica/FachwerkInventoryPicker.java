package de.ottoextra.tweaks.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Locale;

/** Resolves Fachwerk note-block states to their named inventory stack. */
public final class FachwerkInventoryPicker {

    private FachwerkInventoryPicker() {
    }

    public static ItemStack matchingStack(BlockState state, PlayerInventory inventory,
                                          ItemStack fallback) {
        int slot = matchingSlot(state, inventory);
        return slot >= 0 ? inventory.getStack(slot).copy() : fallback;
    }

    public static int matchingSlot(BlockState state, PlayerInventory inventory) {
        if (state == null || inventory == null || !state.isOf(Blocks.NOTE_BLOCK)) {
            return -1;
        }

        String key = FachwerkBlockNameResolver.translationKey(
                state.get(NoteBlock.INSTRUMENT).asString(),
                state.get(NoteBlock.NOTE),
                state.get(NoteBlock.POWERED));
        if (key == null) {
            return -1;
        }

        String expectedName = Text.translatable(key).getString();
        for (int slot = 0; slot < inventory.getMainStacks().size(); slot++) {
            ItemStack candidate = inventory.getStack(slot);
            if (!candidate.isEmpty()
                    && candidate.isOf(Blocks.NOTE_BLOCK.asItem())
                    && namesMatch(expectedName, candidate.getName().getString())) {
                return slot;
            }
        }
        return -1;
    }

    static boolean namesMatch(String expected, String actual) {
        return normalizeName(expected).equals(normalizeName(actual));
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT).replace("ß", "ss");
        int separator = normalized.indexOf(' ');
        String first = separator < 0 ? normalized : normalized.substring(0, separator);
        String color = switch (first) {
            case "weiss", "weisse", "weisser", "weisses" -> "weiss";
            case "rot", "rote", "roter", "rotes" -> "rot";
            case "gelb", "gelbe", "gelber", "gelbes" -> "gelb";
            case "braun", "braune", "brauner", "braunes" -> "braun";
            case "grau", "graue", "grauer", "graues" -> "grau";
            default -> first;
        };
        return separator < 0 ? color : color + normalized.substring(separator);
    }
}
