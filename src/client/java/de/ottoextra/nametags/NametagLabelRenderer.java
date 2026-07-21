package de.ottoextra.nametags;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Gemeinsamer Label-Zeichner für beide Render-Pfade:
 * {@code PlayerEntityRenderer.renderLabelIfPresent} (normal) und
 * {@code EntityRenderer.renderLabelIfPresent} (z. B. EntityCulling rendert
 * Labels gecullter Spieler direkt über die Basisklasse — sonst erscheint
 * hinter Wänden wieder das Vanilla-Schild).
 *
 * <p>Rückgabe true = Aufrufer soll cancel(): entweder haben wir gezeichnet
 * oder die Sichtbarkeitsregel (REALISTIC/HIDE_ALL/Profil) unterdrückt das
 * Label komplett.</p>
 */
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
                return false; // Vanilla überspringt dann ebenfalls
            }
            // Accountname aus der State-Map (updateRenderState-Capture);
            // Fallback playerName/displayName (EntityCulling-Roh-States).
            String account = NametagService.accountFor(state);

            /*
             * Der Hook in EntityRenderer sieht auch Tiere, Ruestungsstaender,
             * Villager und serverseitige NPCs. Ein displayName allein beweist
             * daher nicht, dass dieser State zu einem echten Spieler gehoert.
             * Nur eine PlayerEntity mit aktivem PlayerListEntry darf durch die
             * RP-Namenslogik laufen; alle anderen Labels bleiben bei Vanilla.
             */
            PlayerEntity entity = findOnlinePlayer(account);
            if (entity == null) {
                return false;
            }
            if (MinecraftClient.getInstance().options.hudHidden) {
                return true; // F1 unterdrückt auch OttoExtra-Nametags
            }

            // Sichtbarkeit auch auf diesem Pfad durchsetzen (EntityCulling
            // umgeht hasLabel teilweise)
            if (!NametagService.shouldRender(entity)) {
                return true; // unterdrücken
            }
            NametagService.Lines lines = NametagService.linesFor(account, state.displayName);
            if (lines == null) {
                return false; // Vanilla
            }
            var cfg = NametagService.config();
            int spacing = cfg != null ? Math.max(4, cfg.lineSpacing) : 10;
            float titleScale = cfg != null ? cfg.titleScale : 1.0f;
            float nameScale = cfg != null ? cfg.nameScale : 1.0f;
            float accountScale = cfg != null ? cfg.accountScale : 0.8f;
            // Mit Account-Zeile alles eine Zeile hochschieben (positive
            // Offsets gehen nach unten), sonst hängt die dritte Zeile im Kopf
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
            return false; // Vanilla weiterzeichnen lassen
        }
    }

    private static PlayerEntity findOnlinePlayer(String account) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.getNetworkHandler() == null || account == null) {
            return null;
        }
        for (PlayerEntity p : client.world.getPlayers()) {
            if (account.equalsIgnoreCase(p.getName().getString())
                    && NametagService.isOnlinePlayer(p)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Eine Label-Zeile mit Skalierung um den Label-Ankerpunkt zeichnen.
     * Flag/Light wie Vanilla: {@code !sneaking} (false = matte Sneak-Optik)
     * und {@code state.light} — sonst wirken die Schilder ausgewaschen.
     */
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
            matrices.pop(); // Pose-Stack MUSS leer zurückbleiben (WorldRenderer.checkEmpty)
        }
    }
}
