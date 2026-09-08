package de.ottoextra.horses.ui;

import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.mixin.HorseEntityVariantInvoker;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseMarking;

import java.util.Locale;

/** Applies persisted vanilla coat data to an unloaded horse preview. */
record HorseAppearance(HorseColor color, HorseMarking marking) {

    static HorseAppearance from(AnimalBreedingHorse horse) {
        if (horse == null) {
            return new HorseAppearance(HorseColor.WHITE, HorseMarking.NONE);
        }
        return new HorseAppearance(
                horseColor(horse.coatColor()),
                horseMarking(horse.marking()));
    }

    void applyTo(HorseEntity horse) {
        ((HorseEntityVariantInvoker) horse)
                .ottoextra$setHorseVariant(color, marking);
    }

    private static HorseColor horseColor(String value) {
        try {
            return HorseColor.valueOf(normalize(value));
        } catch (IllegalArgumentException error) {
            return HorseColor.WHITE;
        }
    }

    private static HorseMarking horseMarking(String value) {
        try {
            return HorseMarking.valueOf(normalize(value));
        } catch (IllegalArgumentException error) {
            return HorseMarking.NONE;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
