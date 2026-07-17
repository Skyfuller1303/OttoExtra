package de.ottoextra.mixin;
import de.ottoextra.map.PaintedWorldMapHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "xaero.map.element.MapElementRenderHandler", remap = false)
public class GuiMapMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void ottoextra$paintedUnderElements(CallbackInfoReturnable<Object> cir) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null) {
            PaintedWorldMapHook.renderUnderElements(screen);
        }
    }
}
