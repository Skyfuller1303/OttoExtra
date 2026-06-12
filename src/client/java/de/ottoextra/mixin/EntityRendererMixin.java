package de.ottoextra.mixin;

import de.ottoextra.nametags.NametagLabelRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Basis-Label-Pfad: EntityCulling u. a. rendern Labels gecullter
 * Spieler direkt über {@code EntityRenderer.renderLabelIfPresent} — ohne
 * diesen Hook erscheint hinter Wänden wieder das Vanilla-Schild statt
 * unseres RP-Labels bzw. trotz REALISTIC-Sichtlinienregel.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;"
            + "Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
            + "Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void ottoextra$rpLabelBase(EntityRenderState state, MatrixStack matrices,
                                       OrderedRenderCommandQueue queue, CameraRenderState camera,
                                       CallbackInfo ci) {
        // Auch rohe EntityRenderStates: EntityCulling extrahiert gecullte
        // Spieler ohne PlayerEntityRenderState (renderNametagsThroughWalls)
        if (NametagLabelRenderer.submit(state, matrices, queue, camera, "ER")) {
            ci.cancel();
        }
    }
}
