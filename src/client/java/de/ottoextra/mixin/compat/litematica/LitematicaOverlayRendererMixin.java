package de.ottoextra.mixin.compat.litematica;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.tweaks.litematica.FachwerkBlockNameResolver;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optionale Litematica-Anbindung; bleibt ohne installierte Zielmod wirkungslos. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.render.OverlayRenderer", remap = false)
public abstract class LitematicaOverlayRendererMixin {

    @Unique
    private BlockState ottoExtra$blockState;

    @Inject(method = "addBlockInfoLines", at = @At("HEAD"), remap = false)
    private void ottoExtra$captureBlockState(BlockState state, CallbackInfo ci) {
        ottoExtra$blockState = state;
    }

    @Inject(method = "addBlockInfoLines", at = @At("RETURN"), remap = false)
    private void ottoExtra$clearBlockState(BlockState state, CallbackInfo ci) {
        ottoExtra$blockState = null;
    }

    @ModifyArg(
            method = "addBlockInfoLines",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            ),
            index = 0,
            remap = false
    )
    private Object ottoExtra$useLocalizedInventoryName(Object original) {
        if (!OttoExtraConfig.active().tweaks.litematica.localizedBlockNames
                || !(original instanceof String registryName)) {
            return original;
        }

        Identifier id = Identifier.tryParse(registryName);
        if (id == null) return original;

        Block block = Registries.BLOCK.getOptionalValue(id).orElse(null);
        if (block == null) return original;

        Text displayName = fachwerkName(ottoExtra$blockState);
        if (displayName == null) {
            ItemStack stack = block.asItem().getDefaultStack();
            displayName = stack.isEmpty() ? block.getName() : stack.getName();
        }
        String name = displayName.getString();
        if (name.isBlank()) return original;

        return Formatting.YELLOW + name + Formatting.RESET;
    }

    @Unique
    private static Text fachwerkName(BlockState state) {
        if (state == null || !state.isOf(Blocks.NOTE_BLOCK)) {
            return null;
        }
        String key = FachwerkBlockNameResolver.translationKey(
                state.get(NoteBlock.INSTRUMENT).asString(),
                state.get(NoteBlock.NOTE),
                state.get(NoteBlock.POWERED));
        return key == null ? null : Text.translatable(key);
    }
}
