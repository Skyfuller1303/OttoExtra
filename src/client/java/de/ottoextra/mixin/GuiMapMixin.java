package de.ottoextra.mixin;

import de.ottoextra.map.PaintedWorldMapHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rendert die gemalte Karte auf der Xaero-Weltkarte (GuiMap) direkt vor der
 * Element-/Waypoint-Ebene, damit Waypoints nicht von der Kartentextur
 * (nicht aufgedeckte Bereiche) überdeckt werden.
 *
 * <p>Ziel: {@code xaero.map.gui.GuiMap#render}; Injektion an der INVOKE-Stelle
 * von {@code MapElementRenderHandler.render} (die alle On-Map-Elemente inkl.
 * Waypoints zeichnet). {@code require = 0}: fehlt Xaero oder ändert sich die
 * Methode, wird die Injektion still übersprungen, kein Crash.</p>
 */
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public class GuiMapMixin {

    // Anker: GETSTATIC von WorldMap.mapElementRenderHandler (direkt vor dem
    // Element-/Waypoint-Render). Nur Xaero-Typen -> stabil in dev UND prod
    // (Intermediary), kein MC-Klassenname im Target.
    @Inject(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/WorldMap;mapElementRenderHandler:"
                            + "Lxaero/map/element/MapElementRenderHandler;",
                    opcode = Opcodes.GETSTATIC,
                    remap = false
            ),
            require = 0
    )
    private void ottoextra$paintedUnderElements(DrawContext context, int mouseX, int mouseY,
                                                float delta, CallbackInfo ci) {
        PaintedWorldMapHook.renderUnderElements((Screen) (Object) this);
    }
}
