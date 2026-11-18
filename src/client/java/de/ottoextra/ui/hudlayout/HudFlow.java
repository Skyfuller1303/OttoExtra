package de.ottoextra.ui.hudlayout;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.ui.hudlayout.HudSnap.Rect;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps HUD panels from drawing over each other. Every participating HUD calls {@link #place} once
 * per frame before drawing and draws at the returned position. Addons may call it too (priority
 * above 200 is meant for addons). All rectangles are in scaled GUI pixels, already multiplied by
 * the HUD's own scale.
 */
public final class HudFlow {
    public static final int PRIORITY_ACTIVITY_HUD = 100;
    public static final int PRIORITY_ADDON = 200;
    private static final long OBSTACLE_REFRESH_MS = 200L;

    private static final HudFlowState STATE = new HudFlowState();
    private static List<Rect> obstacles = List.of();
    private static long obstaclesAt;
    private static volatile String pinned;
    private static int lastScreenW;
    private static int lastScreenH;

    private HudFlow() {
    }

    /** The layout editor pins the HUD being dragged so it is drawn exactly where the mouse puts it. */
    public static void pin(String id) {
        pinned = id;
    }

    public static boolean isEnabled() {
        try {
            return OttoExtraConfig.active().hudFlowEnabled;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Returns the top-left position at which to draw, {x, y}, or null if HUD flow is disabled (draw
     * at the desired position then). Never throws.
     *
     * @param pushUp true for bottom anchored HUDs, which move up; others move down
     */
    public static int[] place(String id, int x, int y, int w, int h, boolean pushUp, int priority) {
        try {
            if (w <= 0 || h <= 0) {
                // Unknown or transient size (content refresh): neither report nor move; the last
                // report stays valid for a moment and the HUD is drawn where it wants to be.
                return null;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            long now = System.currentTimeMillis();
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();
            if (sw != lastScreenW || sh != lastScreenH) {
                // Resize or GUI scale change: stored rects are in the old pixel space.
                lastScreenW = sw;
                lastScreenH = sh;
                STATE.clear();
                obstaclesAt = 0L;
            }
            if (!isEnabled()) {
                // Still record where the HUD is drawn (the editor uses it), but never move it.
                STATE.place(id, new Rect(x, y, w, h), pushUp, priority, now, List.of(),
                        client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), true);
                return null;
            }
            Rect r = STATE.place(id, new Rect(x, y, w, h), pushUp, priority, now, obstacles(now),
                    client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), id.equals(pinned));
            return new int[]{r.x(), r.y()};
        } catch (Throwable t) {
            return null;
        }
    }

    /** Where a HUD was actually drawn recently ({x, y, w, h}), or null. Used by the layout editor. */
    public static int[] resolved(String id) {
        Rect r = STATE.resolved(id, System.currentTimeMillis());
        return r == null ? null : new int[]{r.x(), r.y(), r.w(), r.h()};
    }

    /** Fixed rectangles that always win: the Xaero minimap and the Lehen overlay boxes. */
    private static List<Rect> obstacles(long now) {
        if (now - obstaclesAt > OBSTACLE_REFRESH_MS) {
            obstaclesAt = now;
            List<Rect> list = new ArrayList<>();
            if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
                try {
                    XaeroObstacles.collect(list);
                } catch (Throwable ignored) {
                    // Xaero API changed: no static obstacles.
                }
            }
            obstacles = List.copyOf(list);
        }
        return obstacles;
    }

    /** Keeps Xaero class references off the verification path when Xaero is absent. */
    private static final class XaeroObstacles {
        static void collect(List<Rect> out) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) {
                return;
            }
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();
            HudLayoutElement.Bounds minimap = new de.ottoextra.map.xaero.XaeroMinimapLayoutElement().bounds(sw, sh);
            if (minimap != null) {
                out.add(new Rect(minimap.x(), minimap.y(), minimap.w(), minimap.h()));
            }
            var session = xaero.hud.minimap.BuiltInHudModules.MINIMAP.getCurrentSession();
            if (session == null || !session.isActive()) {
                return;
            }
            for (var e : de.ottoextra.map.xaero.MinimapBannerOverlay.computeElements(client,
                    OttoExtraConfig.active().map, session)) {
                out.add(new Rect(e.x(), e.y(), e.width(), e.height()));
            }
        }
    }
}
