package de.ottoextra.mixin;

import de.ottoextra.crafting.werkstatt.WerkstattModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the Werkstatt panel after the screen but before its deferred elements, so the station's item
 * tooltips (deferred since 1.21.6) end up above the panel. Fabric's afterRender runs after them.
 */
@Mixin(Screen.class)
public abstract class WerkstattScreenPanelMixin {
    @Inject(method = "renderWithTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawDeferredElements()V"))
    private void ottoextra$werkstattPanel(DrawContext context, int mouseX, int mouseY, float deltaTicks,
                                          CallbackInfo ci) {
        WerkstattModule.renderPanel((Screen) (Object) this, context, mouseX, mouseY);
    }
}
