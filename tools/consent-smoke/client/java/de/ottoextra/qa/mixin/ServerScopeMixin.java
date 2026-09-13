package de.ottoextra.qa.mixin;
import de.ottoextra.OttoExtraGate;
import de.ottoextra.qa.RuntimeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = OttoExtraGate.class, remap = false)
public class ServerScopeMixin {
    @Inject(method = "isOnOttonien", at = @At("HEAD"), cancellable = true)
    private static void fixture(net.minecraft.client.MinecraftClient client, CallbackInfoReturnable<Boolean> cir) {
        if (RuntimeProbe.simulateOttonien && client != null && client.isInSingleplayer()) cir.setReturnValue(true);
    }
}
