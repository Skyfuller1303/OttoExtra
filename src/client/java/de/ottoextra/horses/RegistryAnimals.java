package de.ottoextra.horses;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;

/** Animals supported by the shared UUID/name register. */
public final class RegistryAnimals {

    private RegistryAnimals() {
    }

    public static boolean supports(Entity entity) {
        return entity != null && supportsType(entity.getClass());
    }

    static boolean supportsType(Class<? extends Entity> type) {
        return AbstractHorseEntity.class.isAssignableFrom(type)
                || CowEntity.class.isAssignableFrom(type)
                || SheepEntity.class.isAssignableFrom(type)
                || ChickenEntity.class.isAssignableFrom(type)
                || GoatEntity.class.isAssignableFrom(type)
                || PigEntity.class.isAssignableFrom(type);
    }
}
