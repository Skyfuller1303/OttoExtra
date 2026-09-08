package de.ottoextra.mixin;

import de.ottoextra.horses.HorseFinder;
import de.ottoextra.horses.AnimalGenderGlow;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void ottoextra$showHorseFinderTarget(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (HorseFinder.isTarget(entity) || AnimalGenderGlow.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
