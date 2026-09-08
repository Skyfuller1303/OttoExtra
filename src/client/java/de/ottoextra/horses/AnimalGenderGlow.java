package de.ottoextra.horses;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
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

    private static int outlineColor(Entity entity) {
        if (!(entity instanceof AnimalEntity) || !OttoExtraConfig.active().animalInfo.genderGlowEnabled
                || !onOttonien.getAsBoolean() || !entity.isAlive()) return EntityRenderState.NO_OUTLINE;
        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || entity.getEntityWorld() != client.world
                || entity.squaredDistanceTo(client.player) >= MAX_DISTANCE_SQUARED) {
            return EntityRenderState.NO_OUTLINE;
        }
        Integer color = AnimalGender.color(HorseRegistry.knownGender(entity.getUuid()));
        return color == null ? EntityRenderState.NO_OUTLINE : 0xFF000000 | color;
    }
}
