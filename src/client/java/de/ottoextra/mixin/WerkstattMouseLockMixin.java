package de.ottoextra.mixin;

import de.ottoextra.crafting.werkstatt.Werkstatt;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Werkstatt view lock: mouse movement does not turn the camera while a continuous job runs. */
@Mixin(Mouse.class)
public abstract class WerkstattMouseLockMixin {
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void ottoextra$werkstattLock(double timeDelta, CallbackInfo ci) {
        if (Werkstatt.viewLocked()) {
            ci.cancel();
        }
    }
}
