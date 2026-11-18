package de.ottoextra.mixin;

import de.ottoextra.crafting.werkstatt.Werkstatt;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Werkstatt position lock: movement, jump, sneak and sprint keys are ignored while a continuous job runs. */
@Mixin(KeyboardInput.class)
public abstract class WerkstattMovementLockMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ottoextra$werkstattLock(CallbackInfo ci) {
        if (Werkstatt.viewLocked()) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
        }
    }
}
