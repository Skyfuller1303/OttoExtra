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
    // Verified in World Map 1.40.16 / 1.41.0: after the map projection is restored,
    // before map-switching text, buttons, dropdowns and tooltips. Also runs with hidden UI.
    // Xaero is not remapped by the annotation processor. Keep complete descriptors
    // for both namespaces; a descriptorless INVOKE selector is invalid in Mixin.
    @Inject(method = "render",
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/MinecraftClient;F)Lxaero/map/element/HoveredMapElementHolder;")),
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;",
                    shift = At.Shift.AFTER), require = 0)
    private void ottoextra$overlayBeforeUi(DrawContext context, int mouseX, int mouseY,
                                          float delta, CallbackInfo ci) {
        WorldMapOverlayHook.render((Screen) (Object) this, context, mouseX, mouseY, delta);
    }

    @Inject(method = "method_25394",
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/class_4597$class_4598;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/class_310;F)Lxaero/map/element/HoveredMapElementHolder;")),
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;",
                    shift = At.Shift.AFTER), require = 0)
    private void ottoextra$overlayBeforeUiRuntime(DrawContext context, int mouseX, int mouseY,
                                                 float delta, CallbackInfo ci) {
        WorldMapOverlayHook.render((Screen) (Object) this, context, mouseX, mouseY, delta);
    }
}
