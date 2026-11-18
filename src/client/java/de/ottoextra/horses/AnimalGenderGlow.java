package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalLiveState;
import de.ottoextra.animalinfo.AnimalLiveStore;
import de.ottoextra.animalinfo.AnimalSpecies;
import de.ottoextra.animalinfo.AnimalStatus;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

/** Local display only: never changes entity flags, scoreboard teams or finder targets. */
public final class AnimalGenderGlow {
    private static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;
    private static BooleanSupplier onOttonien = () -> false;

    private AnimalGenderGlow() { }

    static void initialize(BooleanSupplier serverGate) {
        onOttonien = serverGate;
    }

    static void toggle(MinecraftClient client) {
        if (client.currentScreen != null || client.world == null || client.player == null
                || !onOttonien.getAsBoolean()) return;
        var config = OttoExtraConfig.active();
        config.animalInfo.genderGlowEnabled = !config.animalInfo.genderGlowEnabled;
        config.save();
        client.player.sendMessage(Text.translatable(config.animalInfo.genderGlowEnabled
                ? "ottoextra.animalinfo.glow.enabled" : "ottoextra.animalinfo.glow.disabled"), true);
    }

    public static boolean shouldGlow(Entity entity) {
        return outlineColor(entity) != EntityRenderState.NO_OUTLINE;
    }

    public static void prepare(Entity entity, EntityRenderState state) {
        int color = outlineColor(entity);
        if (color != EntityRenderState.NO_OUTLINE && !HorseFinder.isTarget(entity)) {
            state.outlineColor = color;
        }
    }

    /**
     * Outline colour. Animals that are waiting (sheared sheep gold, bred animals orchid) take precedence over
     * the permanent gender colour: the waiting state is temporary and tells the player what to do next,
     * while the gender colour is the baseline for every other animal.
     */
    private static int outlineColor(Entity entity) {
        var config = OttoExtraConfig.active().animalInfo;
        if (!(entity instanceof AnimalEntity animal)
                || !(config.genderGlowEnabled || config.breedingGlowEnabled || config.shearGlowEnabled)
                || !onOttonien.getAsBoolean() || !entity.isAlive()) return EntityRenderState.NO_OUTLINE;
        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || entity.getEntityWorld() != client.world
                || entity.squaredDistanceTo(client.player) >= MAX_DISTANCE_SQUARED) {
            return EntityRenderState.NO_OUTLINE;
        }
        int waiting = waitingColor(animal, config);
        if (waiting >= 0) return 0xFF000000 | waiting;
        if (!config.genderGlowEnabled) return EntityRenderState.NO_OUTLINE;
        Integer color = AnimalGender.color(HorseRegistry.knownGender(entity.getUuid()));
        return color == null ? EntityRenderState.NO_OUTLINE : 0xFF000000 | color;
    }

    private static int waitingColor(AnimalEntity animal, OttoExtraConfig.AnimalInfo config) {
        if (!config.breedingGlowEnabled && !config.shearGlowEnabled) return -1;
        AnimalLiveState state = AnimalLiveStore.current().get(animal.getUuid());
        if (state == null) return -1;
        String visibleName = animal.getCustomName() == null ? null : animal.getCustomName().getString();
        AnimalSpecies species = AnimalSpecies.resolve(state.species(), visibleName);
        return AnimalStatus.color(AnimalStatus.glow(config.shearGlowEnabled, config.breedingGlowEnabled,
                animal instanceof SheepEntity, animal.isBaby(), state, species, System.currentTimeMillis()));
    }
}
