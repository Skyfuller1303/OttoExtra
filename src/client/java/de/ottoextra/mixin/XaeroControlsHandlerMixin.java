package de.ottoextra.mixin;

import de.ottoextra.map.WorldMapOpenAnimation;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ignores Xaero world map key presses (e.g. reopening the map) while the close animation runs. */
@Pseudo
@Mixin(targets = "xaero.map.controls.ControlsHandler", remap = false)
public class XaeroControlsHandlerMixin {

    @Inject(method = "keyDown", at = @At("HEAD"), cancellable = true, require = 0)
    private void ottoextra$blockDuringMapClose(KeyBinding key, boolean tickEnd, boolean repeat,
                                               CallbackInfo ci) {
        if (WorldMapOpenAnimation.isClosing()) {
            ci.cancel();
        }
    }
}
