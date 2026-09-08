package de.ottoextra.horses.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.passive.AnimalEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fixed-angle animal preview used by the compact register overview. */
final class HorseModelPreview {

    static final float DEFAULT_YAW_DEGREES = 35.0f;

    private HorseModelPreview() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void draw(DrawContext context, int left, int top,
                     int right, int bottom, int scale,
                     float verticalOffset, float yawDegrees,
                     AnimalEntity horse) {
        EntityRenderer renderer = MinecraftClient.getInstance()
                .getEntityRenderDispatcher().getRenderer(horse);
        EntityRenderState state = renderer.getAndUpdateRenderState(horse, 1.0f);
        state.light = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = EntityRenderState.NO_OUTLINE;
        state.displayName = null;
        state.nameLabelPos = null;

        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = 180.0f + yawDegrees;
            living.relativeHeadYaw = 0.0f;
            living.pitch = 0.0f;
            if (living.baseScale > 0.0f) {
                living.width /= living.baseScale;
                living.height /= living.baseScale;
                living.baseScale = 1.0f;
            }
        }

        Quaternionf orientation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf pitch = new Quaternionf();
        Vector3f translation = new Vector3f(
                0.0f, state.height / 2.0f + verticalOffset, 0.0f);
        context.addEntity(state, scale, translation, orientation, pitch,
                left, top, right, bottom);
    }
}
