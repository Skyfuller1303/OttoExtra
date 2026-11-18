package de.ottoextra.mixin;

import de.ottoextra.map.WorldMapOpenAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Xaero's world map turns off world rendering while its screen is open. During the map's unroll and
 * roll-up animation the world stays visible beside the map, so it keeps rendering for that time.
 */
@Pseudo
@Mixin(targets = "xaero.map.core.XaeroWorldMapCore", remap = false)
public class XaeroWorldMapCoreMixin {

    @Inject(method = "onRenderCall", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ottoextra$renderWorldDuringMapAnimation(boolean renderingInGame,
                                                                CallbackInfoReturnable<Boolean> cir) {
        if (renderingInGame && !cir.getReturnValueZ() && WorldMapOpenAnimation.wantsLiveWorld()) {
            cir.setReturnValue(true);
        }
    }
}
