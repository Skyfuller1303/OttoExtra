package de.ottoextra.horses;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/** Hover-only livestock nametag; the addon handles horses while it is loaded. */
public final class HorseRegistryNametagRenderer {

    private static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;
    private static final Map<EntityRenderState, Text> LABELS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private HorseRegistryNametagRenderer() {
    }

    public static void prepare(Entity entity, EntityRenderState state,
                               float tickProgress) {
        LABELS.remove(state);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || !(entity instanceof AnimalEntity)
                || (!HorseRegistry.basicSurfaceActive() && entity instanceof AbstractHorseEntity)
                || !(client.crosshairTarget instanceof EntityHitResult hit)
                || hit.getEntity() != entity
                || state.squaredDistanceToCamera >= MAX_DISTANCE_SQUARED) {
            return;
        }
        var config = OttoExtraConfig.active().animalInfo;
        String registeredName = RegistryAnimals.supports(entity)
                ? HorseRegistry.name(entity.getUuid()).orElse(null) : null;
        if (registeredName == null && (!config.nametagIncludeUnregistered
                || (!config.nametagShowGender && !config.nametagGenderColor))) return;
        if (state.nameLabelPos == null) {
            state.nameLabelPos = entity.getAttachments().getPointNullable(
                    EntityAttachmentType.NAME_TAG, 0, entity.getLerpedYaw(tickProgress));
        }
        if (state.nameLabelPos == null) return;
        if (registeredName != null) {
            prepareLabel(state, registeredName, HorseRegistry.knownGender(entity.getUuid()), config);
        } else {
            prepareUnregisteredLabel(state, entity.getUuid(), entity.getCustomName(), config);
        }
    }

    static void prepareUnregisteredLabel(EntityRenderState state, UUID uuid, Text customName,
                                         OttoExtraConfig.AnimalInfo config) {
        LABELS.remove(state);
        if (!config.nametagIncludeUnregistered || (!config.nametagShowGender && !config.nametagGenderColor)) return;
        var observation = HorseRegistry.observation(uuid).orElse(null);
        if (observation == null || AnimalGender.color(observation.snapshot().gender()) == null) return;
        String gender = observation.snapshot().gender();
        if (state.displayName != null) {
            state.displayName = decorate(state.displayName, gender, config);
        } else {
            Text name = customName != null ? customName
                    : Text.literal(observation.snapshot().animalName()).formatted(Formatting.GOLD);
            LABELS.put(state, decorate(name, gender, config));
        }
    }

    static void prepareLabel(EntityRenderState state, String name, String gender,
                             OttoExtraConfig.AnimalInfo config) {
        LABELS.remove(state);
        Text vanilla = state.displayName;
        if (vanilla != null && vanilla.getString().equalsIgnoreCase(name)) {
            // Decorate the existing label instead of drawing the same animal name twice.
            state.displayName = decorate(vanilla, gender, config);
        } else {
            LABELS.put(state, decorate(Text.literal(name).formatted(Formatting.GOLD), gender, config));
        }
    }

    static Text decorate(Text name, String gender, OttoExtraConfig.AnimalInfo config) {
        Integer color = AnimalGender.color(gender);
        if (color == null || (!config.nametagShowGender && !config.nametagGenderColor)) return name;
        var result = config.nametagGenderColor
                ? tintedCopy(name, color) : name.copy();
        if (config.nametagShowGender) {
            result.append(Text.literal(color == AnimalGender.FEMALE_COLOR ? " ♀" : " ♂").withColor(color));
        }
        return result;
    }

    private static MutableText tintedCopy(Text name, int color) {
        var result = Text.empty().setStyle(name.getStyle().withColor(color));
        name.visit((style, content) -> {
            result.append(Text.literal(content).setStyle(style.withColor(color)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    public static void submit(EntityRenderState state, MatrixStack matrices,
                              OrderedRenderCommandQueue queue,
                              CameraRenderState camera) {
        Text label = LABELS.get(state);
        if (label == null || state.nameLabelPos == null
                || MinecraftClient.getInstance().options.hudHidden) {
            return;
        }
        Text vanillaLabel = state.displayName;
        if (vanillaLabel != null
                && vanillaLabel.getString().equalsIgnoreCase(label.getString())) {
            return;
        }
        queue.submitLabel(matrices, state.nameLabelPos,
                vanillaLabel == null ? 0 : -10, label, !state.sneaking,
                state.light, state.squaredDistanceToCamera, camera);
    }
}
