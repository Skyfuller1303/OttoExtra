package de.ottoextra.animalinfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Animal model in a GUI box: a three-quarter view (head to the front left, seen slightly from above), scaled
 * so the entity's bounding box fills the box, without name tag, custom name, leash, outline or shadow. The
 * model is drawn as a picture-in-picture element, which clips it to the box.
 *
 * <p>Only the render state of this frame is changed; the entity itself is never touched. The name tag of the
 * register ({@code HorseRegistryNametagRenderer}) is suppressed as well, because it needs
 * {@code nameLabelPos}.
 */
public final class AnimalPreview {
    /** Body yaw of the three-quarter view; 180 faces the viewer. */
    public static final float VIEW_YAW = 225.0f;
    /** Tilt of the model; negative shows the back. */
    public static final float VIEW_PITCH = -10.0f;
    public static final int PADDING = 2;
    /**
     * Quadruped models are longer than their square bounding box (horse, cow, sheep about 1.5 times the box
     * width, head included); the fit uses this length so the head is not cut off.
     */
    public static final float LENGTH_FACTOR = 1.5f;

    /** Pixels per block and the projected size of the model. */
    public record Fit(float scale, float width, float height) {
    }

    private AnimalPreview() {
    }

    /**
     * Scale that fits a bounding box of {@code width} x {@code height} blocks, turned by {@code yawDegrees}
     * around the vertical axis (relative to facing the viewer) and tilted by {@code pitchDegrees}, into a box
     * of {@code boxW} x {@code boxH} pixels with {@code padding} pixels on every side. The model is assumed to
     * be {@link #LENGTH_FACTOR} times as long as the box is wide.
     */
    public static Fit fit(float width, float height, float yawDegrees, float pitchDegrees,
                          int boxW, int boxH, int padding) {
        float w = Math.max(0.1f, width);
        float h = Math.max(0.1f, height);
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        float length = w * LENGTH_FACTOR;
        float projectedW = (float) (w * Math.abs(Math.cos(yaw)) + length * Math.abs(Math.sin(yaw)));
        float depth = (float) (w * Math.abs(Math.sin(yaw)) + length * Math.abs(Math.cos(yaw)));
        float projectedH = (float) (h * Math.abs(Math.cos(pitch)) + depth * Math.abs(Math.sin(pitch)));
        float innerW = Math.max(1, boxW - 2 * padding);
        float innerH = Math.max(1, boxH - 2 * padding);
        float scale = Math.max(1.0f, Math.min(innerW / projectedW, innerH / projectedH));
        return new Fit(scale, projectedW * scale, projectedH * scale);
    }

    /** Three-quarter view filling the box. */
    public static void draw(DrawContext context, int left, int top, int right, int bottom, LivingEntity entity) {
        draw(context, left, top, right, bottom, entity, VIEW_YAW, VIEW_PITCH);
    }

    /**
     * Model that follows the mouse like the inventory player, without name tag. The size is fitted for the
     * three-quarter view, so it does not change while the model turns.
     */
    public static void drawTurning(DrawContext context, int left, int top, int right, int bottom, LivingEntity entity,
                                   float mouseX, float mouseY) {
        float turn = (float) Math.atan(((left + right) / 2.0f - mouseX) / 40.0f);
        float tilt = (float) Math.atan(((top + bottom) / 2.0f - mouseY) / 40.0f);
        draw(context, left, top, right, bottom, entity, 180.0f + turn * 20.0f, tilt * 20.0f, true);
    }

    /**
     * Model at the given body yaw (180 faces the viewer) and tilt, filling the box. Throws when another mod's
     * renderer fails; callers keep their fallback.
     */
    public static void draw(DrawContext context, int left, int top, int right, int bottom, LivingEntity entity,
                            float bodyYaw, float pitchDegrees) {
        draw(context, left, top, right, bottom, entity, bodyYaw, pitchDegrees, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void draw(DrawContext context, int left, int top, int right, int bottom, LivingEntity entity,
                             float bodyYaw, float pitchDegrees, boolean fitForView) {
        if (entity == null || right - left < 2 || bottom - top < 2) return;
        EntityRenderer renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = renderer.getAndUpdateRenderState(entity, 1.0f);
        state.light = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = EntityRenderState.NO_OUTLINE;
        state.displayName = null;
        state.nameLabelPos = null;
        state.leashDatas = null;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = bodyYaw;
            living.relativeHeadYaw = 0.0f;
            living.pitch = 0.0f;
            // Like the inventory: the scale attribute is undone, the baby size is kept and fills the box.
            if (living.baseScale > 0.0f) {
                living.width /= living.baseScale;
                living.height /= living.baseScale;
                living.baseScale = 1.0f;
            }
        }
        Fit fit = fitForView
                ? fit(state.width, state.height, VIEW_YAW - 180.0f, VIEW_PITCH, right - left, bottom - top, PADDING)
                : fit(state.width, state.height, bodyYaw - 180.0f, pitchDegrees, right - left, bottom - top, PADDING);
        Quaternionf tilt = new Quaternionf().rotateX((float) Math.toRadians(pitchDegrees));
        Quaternionf orientation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        Vector3f translation = new Vector3f(0.0f, state.height / 2.0f, 0.0f);
        context.addEntity(state, fit.scale(), translation, orientation, tilt, left, top, right, bottom);
    }
}
