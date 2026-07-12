package de.ottoextra.mixin;

import de.ottoextra.tweaks.TweaksModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
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

@Mixin(GameRenderer.class)
public abstract class GameRendererBlurMixin {

    @Shadow
    @Final
    private ObjectPool pool;

    private static final Identifier OTTOEXTRA$EDGE_BLUR =
            Identifier.of("ottoextra", "low_health_edge_blur");

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/render/GameRenderer;"
                            + "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void ottoextra$lowHealthWorldBlur(RenderTickCounter tickCounter, boolean tick,
                                              CallbackInfo ci) {
        int passes = TweaksModule.lowHealthBlurPasses();
        if (passes <= 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        PostEffectProcessor effect = client.getShaderLoader()
                .loadPostEffect(OTTOEXTRA$EDGE_BLUR, DefaultFramebufferSet.MAIN_ONLY);
        if (effect == null) {
            return;
        }
        for (int i = 0; i < passes; i++) {
            effect.render(client.getFramebuffer(), this.pool);
        }
    }

    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F",
            at = @At("RETURN"), cancellable = true)
    private void ottoextra$lowHealthFov(Camera camera, float tickProgress, boolean changingFov,
                                        CallbackInfoReturnable<Float> cir) {
        if (!changingFov) {
            return;
        }
        float boost = TweaksModule.lowHealthFovBoost();
        if (boost > 0.001f) {
            cir.setReturnValue(cir.getReturnValue() + boost);
        }
    }
}
