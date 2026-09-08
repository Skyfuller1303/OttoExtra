package de.ottoextra.mixin.compat.litematica;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.tweaks.litematica.FachwerkInventoryPicker;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Uses the named Fachwerk inventory item during Litematica's schematic pick-block. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.InventoryUtils", remap = false)
public abstract class LitematicaInventoryUtilsMixin {

    @Shadow(remap = false)
    public static void setPickedItemToHand(int slot, ItemStack stack,
                                           MinecraftClient client) {
        throw new AssertionError();
    }

    @ModifyVariable(
            method = "schematicWorldPickBlock",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private static ItemStack ottoExtra$useNamedFachwerkStack(
            ItemStack original, ItemStack requestedStack, BlockPos pos, World schematicWorld,
            MinecraftClient client) {
        if (!OttoExtraConfig.active().tweaks.litematica.localizedBlockNames
                || client == null || client.player == null
                || schematicWorld == null || pos == null) {
            return original;
        }

        BlockState state = schematicWorld.getBlockState(pos);
        int slot = FachwerkInventoryPicker.matchingSlot(
                state, client.player.getInventory());
        if (slot < 0) {
            return original;
        }

        ItemStack match = client.player.getInventory().getStack(slot).copy();
        setPickedItemToHand(slot, match, client);
        return match;
    }
}
