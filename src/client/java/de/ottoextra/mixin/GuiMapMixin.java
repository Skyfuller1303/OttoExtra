package de.ottoextra.mixin;

import de.ottoextra.map.PaintedWorldMapHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rendert die gemalte Karte auf der Xaero-Weltkarte direkt vor der Element-/
 * Waypoint-Ebene (so verdeckt die Kartentextur für unerkundete Bereiche keine
 * Waypoints).
 *
 * <p>Ziel: {@code xaero.map.element.MapElementRenderHandler#render} — Xaeros
 * EIGENE Methode (Name in dev UND prod identisch, da Xaero nicht durch
 * Intermediary remappt wird). Frühere Variante hookte {@code GuiMap.render},
 * eine geerbte Minecraft-Methode — deren Name ist in prod {@code method_25394},
 * sodass die Injektion mit {@code remap=false} dort still übersprungen wurde
 * (Karte schwarz im echten Modpack, funktionierte aber im Dev). {@code require=0}:
 * fehlt Xaero/ändert sich die Methode, wird still übersprungen.</p>
 */
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
