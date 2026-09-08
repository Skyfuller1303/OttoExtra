package de.ottoextra.mixin;

import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseMarking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HorseEntity.class)
public interface HorseEntityVariantInvoker {

    @Invoker("setHorseVariant")
    void ottoextra$setHorseVariant(HorseColor color, HorseMarking marking);
}
