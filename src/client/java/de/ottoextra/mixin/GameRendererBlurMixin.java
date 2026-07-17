package de.ottoextra.mixin;

import de.ottoextra.rpnames.inspect.InspectMode;
import de.ottoextra.tweaks.TweaksModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.memory.ObjectPool;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4f;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererBlurMixin {

    @Shadow
    @Final
    private ObjectPool pool;

    private static final Identifier OTTOEXTRA$LOW_HEALTH_EDGE_BLUR =
            Identifier.of("ottoextra", "low_health_edge_blur");
    private static final Identifier OTTOEXTRA$INSPECT_LENS =
            Identifier.of("ottoextra", "inspect_lens");

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/render/GameRenderer;"
                            + "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void ottoextra$worldPostEffects(RenderTickCounter tickCounter, boolean tick,
                                            CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        int lowHealthPasses = TweaksModule.lowHealthBlurPasses();
        if (lowHealthPasses > 0) {
            renderEffect(client, OTTOEXTRA$LOW_HEALTH_EDGE_BLUR, lowHealthPasses);
        }

        // Die Untersuchungslinse ist bewusst nur ein einzelner, leichter Pass.
        if (InspectMode.edgeBlurActive()) {
            renderEffect(client, OTTOEXTRA$INSPECT_LENS, 1);
        }
    }

    private void renderEffect(MinecraftClient client, Identifier id, int passes) {
        PostEffectProcessor effect = client.getShaderLoader()
                .loadPostEffect(id, DefaultFramebufferSet.MAIN_ONLY);
        if (effect == null) {
            return;
        }
        for (int i = 0; i < passes; i++) {
            effect.render(client.getFramebuffer(), this.pool);
        }
    }

    @Inject(method = "getProjectionMatrix(F)Lorg/joml/Matrix4f;",
            at = @At("RETURN"))
    private void ottoextra$combinedProjectionEffects(float tickProgress,
                                                       CallbackInfoReturnable<Matrix4f> cir) {
        float inspectMultiplier = InspectMode.fovZoomMultiplier();
        float lowHealthBoost = TweaksModule.lowHealthFovBoost();
        if (inspectMultiplier >= 0.9999f && Math.abs(lowHealthBoost) < 0.001f) {
            return;
        }

        Matrix4f projection = cir.getReturnValue();
        if (projection == null) {
            return;
        }

        // OttoExtra greift nicht mehr in GameRenderer#getFov ein. Genau dort setzen
        // OptiFine und viele Zoom-Mods ihren C-Zoom um. Stattdessen wird erst die
        // vollstaendig berechnete Welt-Projektionsmatrix skaliert. So bleiben alle
        // vorherigen Zoom-Effekte erhalten und OttoExtra wird nur daruebergelegt.
        float scale = projectionScaleForInspect(inspectMultiplier)
                * projectionScaleForLowHealth(lowHealthBoost);
        if (Math.abs(scale - 1.0f) < 0.0001f) {
            return;
        }

        projection.m00(projection.m00() * scale);
        projection.m11(projection.m11() * scale);
    }

    private static float projectionScaleForInspect(float multiplier) {
        float clamped = Math.max(0.10f, Math.min(1.0f, multiplier));
        if (clamped >= 0.9999f) {
            return 1.0f;
        }

        double referenceFov = 70.0;
        double targetFov = Math.max(1.0, referenceFov * clamped);
        return (float) (Math.tan(Math.toRadians(referenceFov * 0.5))
                / Math.tan(Math.toRadians(targetFov * 0.5)));
    }

    private static float projectionScaleForLowHealth(float boostDegrees) {
        if (Math.abs(boostDegrees) < 0.001f) {
            return 1.0f;
        }

        double referenceFov = 70.0;
        double targetFov = Math.max(1.0, Math.min(170.0, referenceFov + boostDegrees));
        return (float) (Math.tan(Math.toRadians(referenceFov * 0.5))
                / Math.tan(Math.toRadians(targetFov * 0.5)));
    }

}
