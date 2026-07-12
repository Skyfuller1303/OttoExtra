package de.ottoextra.rpnames;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class MeetMarkerRenderer {

    private static final Identifier TEXTURE =
            de.ottoextra.OttoExtra.id("textures/entity/meet_marker.png");
    private static final float BASE_SCALE = 1.0f;
    private static final double RANGE = 48.0;

    private MeetMarkerRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(MeetMarkerRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        try {
            if (!RpNamesServices.proactiveMeetEnabled()) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) {
                return;
            }
            List<MeetMarkerModel.Quad> quads = MeetMarkerModel.quads();
            if (quads.isEmpty()) {
                return;
            }
            OttoExtraConfig.RpNames cfg = RpNamesServices.config();
            MatrixStack matrices = ctx.matrices();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null || cfg == null) {
                return;
            }
            Camera cam = mc.gameRenderer.getCamera();
            Vec3d camPos = cam.getCameraPos();
            VertexConsumer vc = consumers.getBuffer(RenderLayers.entityCutoutNoCull(TEXTURE));

            float delta = mc.getRenderTickCounter().getTickProgress(true);

            long time = System.currentTimeMillis();
            float spin = (time % 36000L) / 100.0f * (float) cfg.meetMarkerSpinSpeed;
            float bob = (float) Math.sin(time / 380.0) * 0.06f;
            float scale = BASE_SCALE * (float) cfg.meetMarkerSize;

            for (var p : mc.world.getPlayers()) {
                if (p == mc.player) {
                    continue;
                }
                String name = p.getGameProfile() != null ? p.getGameProfile().name() : null;
                if (name == null || !RpNamesServices.isPendingMeet(name)) {
                    continue;
                }
                if (p.squaredDistanceTo(mc.player) > RANGE * RANGE) {
                    continue;
                }
                Vec3d pos = p.getLerpedPos(delta);
                double dx = pos.x - camPos.x;
                double dy = pos.y + p.getHeight() + cfg.meetMarkerHeight + bob - camPos.y;
                double dz = pos.z - camPos.z;
                int light = cfg.meetMarkerGlow ? 0xF000F0
                        : WorldRenderer.getLightmapCoordinates(mc.world, p.getBlockPos().up(2));
                drawModel(quads, matrices, vc, dx, dy, dz, spin, scale, light);
            }
        } catch (Throwable ignored) {

        }
    }

    private static void drawModel(List<MeetMarkerModel.Quad> quads, MatrixStack matrices,
                                  VertexConsumer vc, double dx, double dy, double dz,
                                  float spin, float scale, int light) {
        matrices.push();
        matrices.translate(dx, dy, dz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        matrices.scale(scale, scale, scale);
        MatrixStack.Entry e = matrices.peek();
        for (MeetMarkerModel.Quad q : quads) {
            for (int i = 0; i < 4; i++) {
                float[] v = q.pos()[i];
                float[] uv = q.uv()[i];
                vc.vertex(e, v[0], v[1], v[2])
                        .color(255, 255, 255, 255)
                        .texture(uv[0], uv[1])
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(light)
                        .normal(e, q.nx(), q.ny(), q.nz());
            }
        }
        matrices.pop();
    }
}
