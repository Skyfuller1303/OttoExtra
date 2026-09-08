package de.ottoextra.horses;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Collections;
import java.util.Map;
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
        if (client.currentScreen != null || !RegistryAnimals.supports(entity)
                || (!HorseRegistry.basicSurfaceActive() && entity instanceof AbstractHorseEntity)
                || !(client.crosshairTarget instanceof EntityHitResult hit)
                || hit.getEntity() != entity
                || state.squaredDistanceToCamera >= MAX_DISTANCE_SQUARED) {
            return;
        }
        HorseRegistry.name(entity.getUuid()).ifPresent(name -> {
            if (state.nameLabelPos == null) {
                state.nameLabelPos = entity.getAttachments().getPointNullable(
                        EntityAttachmentType.NAME_TAG, 0,
                        entity.getLerpedYaw(tickProgress));
            }
            if (state.nameLabelPos != null) {
                LABELS.put(state, Text.literal(name).formatted(Formatting.GOLD));
            }
        });
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
