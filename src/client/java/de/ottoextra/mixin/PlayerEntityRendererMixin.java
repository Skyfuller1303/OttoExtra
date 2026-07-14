package de.ottoextra.mixin;

import de.ottoextra.nametags.NametagLabelRenderer;
import de.ottoextra.nametags.NametagService;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Namensschilder:
 * <ul>
 *   <li>{@code updateRenderState} — Accountname nachreichen (Vanilla setzt
 *       {@code playerName} nicht immer).</li>
 *   <li>{@code hasLabel} — Sichtbarkeit (REALISTIC-Sichtlinie, HIDE_ALL,
 *       Profil-Flag) über {@link NametagService#shouldRender}.</li>
 *   <li>{@code renderLabelIfPresent} — Titel/RP-Name/Account über
 *       {@link NametagLabelRenderer}; Cancel nur, wenn wir zeichnen oder
 *       unterdrücken.</li>
 * </ul>
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;"
            + "Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void ottoextra$captureAccountName(PlayerLikeEntity entity, PlayerEntityRenderState state,
                                              float tickProgress, CallbackInfo ci) {
        // NICHT state.playerName setzen — Vanilla rendert das als eigene
        // Label-Zeile (Doppel-Nametag); Accountname separat merken
        if (entity.getName() != null) {
            NametagService.rememberAccount(state, entity.getName().getString());
        }
    }

    @Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z",
            at = @At("RETURN"), cancellable = true)
    private void ottoextra$nametagVisibility(PlayerLikeEntity entity, double squaredDistance,
                                             CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValueZ() && !NametagService.shouldRender(entity)) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
            // Nametags dürfen nie crashen
        }
    }

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;"
            + "Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
            + "Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void ottoextra$rpLabel(PlayerEntityRenderState state, MatrixStack matrices,
                                   OrderedRenderCommandQueue queue, CameraRenderState camera,
                                   CallbackInfo ci) {
        if (NametagLabelRenderer.submit(state, matrices, queue, camera, "PER")) {
            ci.cancel();
        }
    }
}
