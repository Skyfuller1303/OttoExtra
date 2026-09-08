package de.ottoextra.letter.e2e.mixin;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces OS clipboard I/O only in the test mod. Editor paste still uses its real input handler. */
@Mixin(Keyboard.class)
public abstract class TestClipboardMixin {
    @Unique private String courierE2e$clipboard = "";
    @Inject(method = "getClipboard()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void courierE2e$get(CallbackInfoReturnable<String> cir) { cir.setReturnValue(courierE2e$clipboard); }
    @Inject(method = "setClipboard(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void courierE2e$set(String text, CallbackInfo ci) { courierE2e$clipboard = text; ci.cancel(); }
}
