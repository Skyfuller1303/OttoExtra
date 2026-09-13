package de.ottoextra.qa.mixin;
import de.ottoextra.map.WorldMapOverlayHook;
import de.ottoextra.qa.RuntimeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = WorldMapOverlayHook.class, remap = false)
public class OverlayProbeMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private static void called(net.minecraft.client.gui.screen.Screen screen, net.minecraft.client.gui.DrawContext ctx,
                               int x, int y, float delta, CallbackInfo ci) {
        RuntimeProbe.overlayCalls.incrementAndGet();
    }
}
