package de.ottoextra.ui.hudlayout;

import net.minecraft.client.MinecraftClient;

/**
 * Shared column of HUD elements at the top centre of the screen: below the vanilla boss bars come the
 * Werkstatt bar, the animal toast, zone toasts and the vote toast, each {@link #GAP} px apart. Every element calls
 * {@link #claim} once per frame while it is visible and draws at the returned y; {@link #release} when
 * it hides. An element that stops reporting leaves the column after a short moment, so the order of the
 * HUD render callbacks does not matter. Addons may join with an order value of their own.
 */
public final class TopCenterStack {
    public static final int GAP = TopCenterStackState.GAP;
    public static final int ORDER_WERKSTATT = 100;
    public static final int ORDER_ANIMAL_TOAST = 150;
    public static final int ORDER_ZONE_TOAST = 200;
    public static final int ORDER_VOTE = 300;

    private static final TopCenterStackState STATE = new TopCenterStackState();
    private static int lastScreenH;

    private TopCenterStack() {
    }

    /** Reports this frame's height of an element and returns the y at which to draw it. Never throws. */
    public static int claim(String id, int order, int height) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            int sh = client.getWindow().getScaledHeight();
            if (sh != lastScreenH) {
                // GUI scale change: stored heights are in the old pixel space.
                lastScreenH = sh;
                STATE.clear();
            }
            return STATE.claim(id, order, height, top(), System.currentTimeMillis());
        } catch (Throwable t) {
            return TopCenterStackState.MARGIN;
        }
    }

    public static void release(String id) {
        STATE.release(id);
    }

    /** Top of the column: 4 px below the last boss bar that is drawn, or 4 px from the top without bars. */
    public static int top() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            return TopCenterStackState.topBelowBossBars(bossBarCount(client), client.getWindow().getScaledHeight());
        } catch (Throwable t) {
            return TopCenterStackState.MARGIN;
        }
    }

    private static int bossBarCount(MinecraftClient client) {
        if (client == null || client.inGameHud == null || client.inGameHud.getBossBarHud() == null) {
            return 0;
        }
        var bars = ((de.ottoextra.mixin.BossBarHudAccessor) (Object) client.inGameHud.getBossBarHud())
                .ottoextra$bossBars();
        return bars == null ? 0 : bars.size();
    }
}
