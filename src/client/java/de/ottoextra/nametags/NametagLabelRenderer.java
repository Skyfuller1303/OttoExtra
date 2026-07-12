package de.ottoextra.nametags;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public final class NametagLabelRenderer {

    private static boolean errorLogged = false;
    private static final java.util.Set<String> DEBUG_DRAWN =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private NametagLabelRenderer() {
    }

    public static boolean submit(EntityRenderState state, MatrixStack matrices,
                                 OrderedRenderCommandQueue queue, CameraRenderState camera) {
        return submit(state, matrices, queue, camera, "?");
    }

    public static boolean submit(EntityRenderState state, MatrixStack matrices,
                                 OrderedRenderCommandQueue queue, CameraRenderState camera,
                                 String source) {
        try {
            if (state.nameLabelPos == null) {
                return false;
            }

            String account = NametagService.accountFor(state);

            PlayerEntity entity = findPlayer(account);
            if (entity != null && !NametagService.shouldRender(entity)) {
                return true;
            }
            NametagService.Lines lines = NametagService.linesFor(account, state.displayName);
            if (lines == null) {
                return false;
            }
            var cfg = NametagService.config();
            int spacing = cfg != null ? Math.max(4, cfg.lineSpacing) : 10;
            float titleScale = cfg != null ? cfg.titleScale : 1.0f;
            float nameScale = cfg != null ? cfg.nameScale : 1.0f;
            float accountScale = cfg != null ? cfg.accountScale : 0.8f;

            int base = lines.account() != null ? -spacing : 0;
            if (!lines.name().getString().isEmpty()) {
                scaledLabel(matrices, queue, camera, state, lines.name(), base, nameScale);
            }
            if (lines.title() != null) {
                scaledLabel(matrices, queue, camera, state, lines.title(), base - spacing, titleScale);
            }
            if (lines.account() != null) {
                scaledLabel(matrices, queue, camera, state, lines.account(), base + spacing, accountScale);
            }
            if (DEBUG_DRAWN.add(account + "@" + source)) {
                OttoExtra.LOGGER.info("[nametags] zeichne {} via {}", account, source);
            }
            return true;
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                OttoExtra.LOGGER.warn("[nametags] Label-Render-Fehler: ", t);
            }
            return false;
        }
    }

    private static PlayerEntity findPlayer(String account) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || account == null) {
            return null;
        }
        for (PlayerEntity p : client.world.getPlayers()) {
            if (account.equals(p.getName().getString())) {
                return p;
            }
        }
        return null;
    }

    private static void scaledLabel(MatrixStack matrices, OrderedRenderCommandQueue queue,
                                    CameraRenderState camera, EntityRenderState state,
                                    net.minecraft.text.Text text, int yOffset, float scale) {
        boolean visible = !state.sneaking;
        if (Math.abs(scale - 1.0f) < 0.01f) {
            queue.submitLabel(matrices, state.nameLabelPos, yOffset, text, visible,
                    state.light, state.squaredDistanceToCamera, camera);
            return;
        }
        var pos = state.nameLabelPos;
        matrices.push();
        try {
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.scale(scale, scale, scale);
            matrices.translate(-pos.x, -pos.y, -pos.z);
            queue.submitLabel(matrices, pos, yOffset, text, visible,
                    state.light, state.squaredDistanceToCamera, camera);
        } finally {
            matrices.pop();
        }
    }
}
