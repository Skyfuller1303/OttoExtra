package de.ottoextra.mixin;

import de.ottoextra.map.WorldMapOverlayHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public class WorldMapOverlayMixin {
    // Verified in World Map 1.41.0 / 1.44.2: after the map projection is restored,
    // before map-switching text, buttons, dropdowns and tooltips. Also runs with hidden UI.
    // Both method names are intentional: named dev environment and intermediary runtime.
    @Inject(method = {"render", "method_25394"},
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lxaero/map/element/MapElementRenderHandler;render")),
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;",
                    shift = At.Shift.AFTER), require = 0)
    private void ottoextra$overlayBeforeUi(DrawContext context, int mouseX, int mouseY,
                                          float delta, CallbackInfo ci) {
        WorldMapOverlayHook.render((Screen) (Object) this, context, mouseX, mouseY, delta);
    }
}
