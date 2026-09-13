package de.ottoextra.qa.mixin;
import de.ottoextra.privacy.ConsentHttp;
import de.ottoextra.qa.RuntimeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = ConsentHttp.class, remap = false)
public class SkinEndpointMixin {
    @Inject(method = "send", at = @At("HEAD"))
    private static void audit(CallbackInfoReturnable<?> cir) {
        if (RuntimeProbe.simulateOttonien) RuntimeProbe.deniedHttpAttempts.incrementAndGet();
    }
    @Inject(method = "skinTarget", at = @At("HEAD"), cancellable = true)
    private static void fixture(String url, CallbackInfoReturnable<java.net.URI> cir) {
        // Only the reserved fixture URL is redirected. All production validation stays intact.
        if (RuntimeProbe.skinEndpoint != null && url.equals("https://textures.minecraft.net/texture/PrivacyQa"))
            cir.setReturnValue(RuntimeProbe.skinEndpoint);
    }
}
