package de.ottoextra.mixin;

import de.ottoextra.map.WorldMapOpenAnimation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the world map animation lets the world render behind Xaero's map, the in-game HUD would be
 * drawn too (and on top of the map). Skip it while the world map screen is open.
 */
@Mixin(InGameHud.class)
public class InGameHudWorldMapMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ottoextra$hideHudBehindWorldMap(DrawContext context, RenderTickCounter tickCounter,
                                                CallbackInfo ci) {
        if (WorldMapOpenAnimation.isWorldMapScreenOpen()) {
            ci.cancel();
        }
    }
}
