package de.ottoextra.mixin;

import de.ottoextra.nametags.NametagLabelRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        /*
         * EntityRenderer rendert auch Namensschilder von Tieren,
         * Rüstungsständern und anderen benannten Entities.
         *
         * PlayerEntityRenderState deckt den normalen Spielerpfad ab.
         * EntityType.PLAYER ist zusätzlich nötig, weil EntityCulling einen
         * gecullten Spieler teilweise nur als normalen EntityRenderState
         * über diesen Basispfad weitergibt.
         */
        if (!(state instanceof PlayerEntityRenderState)
                && state.entityType != EntityType.PLAYER) {
            return;
        }

        if (NametagLabelRenderer.submit(state, matrices, queue, camera, "ER")) {
            ci.cancel();
        }
    }
}
