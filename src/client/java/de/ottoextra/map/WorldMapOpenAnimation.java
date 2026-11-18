package de.ottoextra.map;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Opening animation for Xaero's world map: the map is unrolled like a parchment over the game view
 * that was visible when the map was opened, while the map zooms out from slightly closer to the
 * user's zoom level.
 *
 * <p>Xaero skips world rendering while its map is open, so the view behind the map is a one-time GPU
 * copy of the main framebuffer ({@link ScreenSnapshotTexture}) taken right after the screen is
 * initialised. Everything is drawn after the screen has rendered, so Xaero's own drawing is
 * untouched: outside a vertical strip around the screen center the snapshot covers the map, and a
 * rolled parchment cylinder (plain fills, no texture files) sits at each strip edge. If the snapshot
 * cannot be created, the older dark curtains are used instead.</p>
 *
 * <p>The zoom reuses Xaero's own interpolation: on the first frame the map's {@code userScale} is
 * set closer and {@code zoomAnim} is replaced by a {@code SinAnimation} toward {@code destScale}.
 * All Xaero access is reflective; if a field is missing, only the zoom part is skipped.</p>
 */
public final class WorldMapOpenAnimation {

    private static final String GUI_MAP = "xaero.map.gui.GuiMap";
    private static final String SIN_ANIMATION = "xaero.map.animation.SinAnimation";

    /**
     * Main framebuffer textures use GL orientation (row 0 = bottom), while GUI quads expect row 0 at
     * the top. Flip this if the snapshot appears upside down.
     */
    static final boolean SNAPSHOT_FLIP_V = true;

    private static final long CURTAIN_MS = 340L;
    private static final long ROLL_IN_MS = 90L;
    private static final long ROLL_END_MS = 560L;
    private static final long ZOOM_MS = 520L;
    private static final double ZOOM_FROM_FACTOR = 1.6;

    private static final int COL_CURTAIN = 0x141418;
    private static final int COL_EDGE = 0xB58D36;
    private static final int COL_EDGE_SHADOW = 0x47391D;

    private static final int SHADOW_PX = 6;
    private static final float SHADOW_ALPHA = 0.35f;

    private static final long CLOSE_SHRINK_MS = 340L;
    private static final long CLOSE_END_MS = 420L;
    /** After the map screen is gone, the world snapshot stays this long before the live world shows. */
    private static final long CLOSE_HOLD_MS = 60L;

    private static final Identifier ROLL_BODY = Identifier.of("ottoextra", "textures/gui/worldmap/map_roll_body.png");
    private static final Identifier ROLL_CAP_TOP = Identifier.of("ottoextra", "textures/gui/worldmap/map_roll_cap_top.png");
    private static final Identifier ROLL_CAP_BOTTOM = Identifier.of("ottoextra", "textures/gui/worldmap/map_roll_cap_bottom.png");
    private static final int ROLL_TEX_W = 12;
    private static final int ROLL_BODY_H = 16;
    private static final int ROLL_CAP_H = 8;

    private static final Identifier SNAPSHOT_ID = Identifier.of("ottoextra", "dynamic/worldmap_open_snapshot");

    private static volatile OttoExtraConfig.Map config;

    private static WeakReference<Screen> animatedScreen = new WeakReference<>(null);
    private static long startedAtMs;

    private static WeakReference<Screen> snapshotScreen = new WeakReference<>(null);
    private static ScreenSnapshotTexture snapshot;

    // Closing animation (render thread only). The map screen stays open while it rolls up live, so
    // Xaero keeps drawing its real tiles; only the fading rolls run after the screen is gone.
    private static WeakReference<Screen> closingScreen = new WeakReference<>(null);
    private static long closeStartedAtMs;
    private static long rollFadeStartedAtMs;
    private static boolean keyLookupTried;
    private static Object openMapKey;

    private static boolean reflectionTried;
    private static Field userScaleField;
    private static Field destScaleField;
    private static Field zoomAnimField;
    private static Constructor<?> sinAnimationCtor;

    private WorldMapOpenAnimation() {
    }

    /** True while the given map screen is still unrolling. */
    private static boolean isOpening(Screen screen) {
        return animatedScreen.get() == screen
                && System.currentTimeMillis() - startedAtMs < ROLL_END_MS;
    }

    /** True while the close animation runs; Xaero's key handler is blocked meanwhile. */
    public static boolean isClosing() {
        long now = System.currentTimeMillis();
        return closingScreen.get() != null
                || (rollFadeStartedAtMs != 0L && now - rollFadeStartedAtMs < CLOSE_END_MS - CLOSE_SHRINK_MS);
    }

    /**
     * True while the open map should let the game world render (Xaero normally skips it): during the
     * unroll and the roll-up, so the world beside the map stays live.
     */
    public static boolean wantsLiveWorld() {
        OttoExtraConfig.Map cfg = config;
        if (cfg == null || !cfg.worldMapOpenAnimation) return false;
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null || snapshot == null || snapshotScreen.get() != screen) return false;
        if (closingScreen.get() == screen) return true;
        return animatedScreen.get() != screen || System.currentTimeMillis() - startedAtMs < ROLL_END_MS;
    }

    /**
     * GUI drawing is deferred, so before the screen renders the framebuffer holds only this frame's
     * world. Copy it into the snapshot texture, which is then drawn beside the map strip.
     */
    private static void beforeRender(Screen screen, DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!wantsLiveWorld()) return;
        try {
            snapshot.refresh(MinecraftClient.getInstance());
        } catch (Throwable t) {
            DebugLog.debug("[map] Live-Welt-Kopie fehlgeschlagen: {}", t.toString());
        }
    }

    /**
     * OttoExtra HUD overlays stay hidden while Xaero's world map is open and while it rolls up, so the
     * live world beside the animating map shows no overlays.
     */
    public static boolean hidesHudOverlays() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null && GUI_MAP.equals(screen.getClass().getName())) return true;
        return rollFadeStartedAtMs != 0L;
    }

    /** True while Xaero's world map screen is the current screen. */
    public static boolean isWorldMapScreenOpen() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen != null && GUI_MAP.equals(screen.getClass().getName());
    }

    private static boolean isLiveClosing(Screen screen) {
        return closingScreen.get() == screen;
    }

    /**
     * ESC or Xaero's map key starts the roll-up instead of closing at once. Every key is ignored
     * while the map unrolls or rolls up.
     */
    private static boolean allowKey(Screen screen, net.minecraft.client.input.KeyInput key) {
        if (isOpening(screen) || isLiveClosing(screen)) return false;
        OttoExtraConfig.Map cfg = config;
        if (cfg == null || !cfg.worldMapOpenAnimation) return true;
        if (snapshot == null || snapshotScreen.get() != screen) return true;
        if (key.key() != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && !matchesOpenMapKey(key)) return true;
        closingScreen = new WeakReference<>(screen);
        closeStartedAtMs = System.currentTimeMillis();
        return false;
    }

    private static boolean matchesOpenMapKey(net.minecraft.client.input.KeyInput key) {
        if (!keyLookupTried) {
            keyLookupTried = true;
            try {
                openMapKey = Class.forName("xaero.map.controls.ControlsRegister")
                        .getField("keyOpenMap").get(null);
            } catch (Throwable t) {
                DebugLog.debug("[map] Xaero-Kartentaste nicht gefunden: {}", t.toString());
            }
        }
        return openMapKey instanceof net.minecraft.client.option.KeyBinding kb && kb.matchesKey(key);
    }

    public static void register(OttoExtraConfig.Map cfg) {
        config = cfg;
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!GUI_MAP.equals(screen.getClass().getName())) return;
            takeSnapshot(client, screen);
            ScreenEvents.beforeRender(screen).register(WorldMapOpenAnimation::beforeRender);
            ScreenEvents.afterRender(screen).register(WorldMapOpenAnimation::afterRender);
            ScreenEvents.remove(screen).register(WorldMapOpenAnimation::onMapRemoved);
            // No key input while the map unrolls, so the map key cannot close it mid-animation.
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.allowKeyPress(screen)
                    .register(WorldMapOpenAnimation::allowKey);
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> drawClosing(ctx));
    }

    /** The map screen is gone: drop the snapshot; after a live roll-up only the rolls fade out. */
    private static void onMapRemoved(Screen screen) {
        if (closingScreen.get() == screen) {
            // Keep the world snapshot for a few frames: the first live world frames after the map
            // (which suppressed world rendering) can differ, so the snapshot hides the switch.
            closingScreen = new WeakReference<>(null);
            rollFadeStartedAtMs = System.currentTimeMillis();
            return;
        }
        if (snapshotScreen.get() == screen) releaseSnapshot();
    }

    /** HUD pass after the map closed: the rolled-up pair lifts and fades. */
    private static void drawClosing(DrawContext ctx) {
        if (rollFadeStartedAtMs == 0L) return;
        MinecraftClient client = MinecraftClient.getInstance();
        long t = System.currentTimeMillis() - rollFadeStartedAtMs;
        long fadeMs = CLOSE_END_MS - CLOSE_SHRINK_MS;
        if (t >= fadeMs || client.currentScreen != null) {
            rollFadeStartedAtMs = 0L;
            releaseSnapshot();
            return;
        }
        try {
            float q = (float) t / fadeMs;
            int w = client.getWindow().getScaledWidth();
            int h = client.getWindow().getScaledHeight();
            if (t < CLOSE_HOLD_MS && snapshot != null) {
                drawSnapshot(ctx, SNAPSHOT_ID, 0, w, w, h, true);
            } else if (snapshot != null) {
                releaseSnapshot();
            }
            drawRolls(ctx, w, h, 0, -Math.round(6f * q), 1f - q);
        } catch (Throwable e) {
            DebugLog.debug("[map] Weltkarten-Schliessanimation fehlgeschlagen: {}", e.toString());
            rollFadeStartedAtMs = 0L;
            releaseSnapshot();
        }
    }

    /** Copies the last completed game frame once per newly opened map (not on resize re-init). */
    private static void takeSnapshot(MinecraftClient client, Screen screen) {
        OttoExtraConfig.Map cfg = config;
        if (cfg == null || !cfg.worldMapOpenAnimation) return;
        if (snapshotScreen.get() == screen) return;
        snapshotScreen = new WeakReference<>(screen);
        releaseSnapshot();
        rollFadeStartedAtMs = 0L;
        try {
            snapshot = ScreenSnapshotTexture.capture(client, SNAPSHOT_ID);
        } catch (Throwable t) {
            snapshot = null;
            DebugLog.debug("[map] Weltkarten-Schnappschuss nicht moeglich, nutze Vorhaenge: {}", t.toString());
        }
    }

    private static void releaseSnapshot() {
        ScreenSnapshotTexture old = snapshot;
        snapshot = null;
        if (old == null) return;
        try {
            old.release(MinecraftClient.getInstance());
        } catch (Throwable t) {
            DebugLog.debug("[map] Weltkarten-Schnappschuss konnte nicht freigegeben werden: {}", t.toString());
        }
    }

    private static void afterRender(Screen screen, DrawContext ctx, int mouseX, int mouseY, float delta) {
        OttoExtraConfig.Map cfg = config;
        if (cfg == null || !cfg.worldMapOpenAnimation) return;

        long now = System.currentTimeMillis();
        if (animatedScreen.get() != screen) {
            // First frame of a newly opened map (re-init on resize keeps the same instance).
            animatedScreen = new WeakReference<>(screen);
            startedAtMs = now;
            startZoom(screen);
        }
        long t = now - startedAtMs;

        boolean haveSnapshot = snapshot != null && snapshotScreen.get() == screen;
        if (!haveSnapshot) {
            drawCurtains(screen, ctx, t);
            return;
        }
        try {
            if (isLiveClosing(screen)) {
                drawRollUp(screen, ctx, now - closeStartedAtMs);
                return;
            }
            if (t >= ROLL_END_MS) return; // snapshot kept for the roll-up on close
            drawUnroll(screen, ctx, t);
        } catch (Throwable e) {
            DebugLog.debug("[map] Weltkarten-Aufrollen fehlgeschlagen: {}", e.toString());
            releaseSnapshot();
        }
    }

    private static void drawUnroll(Screen screen, DrawContext ctx, long t) {
        int w = screen.width;
        int h = screen.height;
        int cx = w / 2;
        int rollW = ROLL_TEX_W * rollScale(h);

        float alpha = 1f;
        int yOff = 0;
        int gap;
        if (t < ROLL_IN_MS) {
            float p = (float) t / ROLL_IN_MS;
            alpha = p;
            yOff = -Math.round(6f * (1f - p));
            gap = 0;
        } else {
            float p = (float) (t - ROLL_IN_MS) / (ROLL_END_MS - ROLL_IN_MS);
            float open = 1f - (1f - p) * (1f - p) * (1f - p); // easeOutCubic
            gap = Math.round((cx + rollW + 8) * open);
        }

        // Snapshot outside the visible map strip [cx - gap, cx + gap].
        int leftEnd = Math.min(w, cx - gap);
        int rightStart = Math.max(0, w - cx + gap);
        if (leftEnd > 0) drawSnapshot(ctx, SNAPSHOT_ID, 0, leftEnd, w, h, true);
        if (rightStart < w) drawSnapshot(ctx, SNAPSHOT_ID, rightStart, w, w, h, true);
        drawRolls(ctx, w, h, gap, yOff, alpha);
    }

    /** Live roll-up: Xaero still renders the map; the world snapshot closes in from both sides. */
    private static void drawRollUp(Screen screen, DrawContext ctx, long t) {
        int w = screen.width;
        int h = screen.height;
        int cx = w / 2;
        int g0 = cx + ROLL_TEX_W * rollScale(h) + 8;
        float p = Math.min(1f, (float) t / CLOSE_SHRINK_MS);
        int gap = g0 - Math.round(g0 * p * p * p); // easeInCubic
        int leftEnd = Math.min(w, cx - gap);
        int rightStart = Math.max(0, w - cx + gap);
        if (leftEnd > 0) drawSnapshot(ctx, SNAPSHOT_ID, 0, leftEnd, w, h, true);
        if (rightStart < w) drawSnapshot(ctx, SNAPSHOT_ID, rightStart, w, w, h, true);
        drawRolls(ctx, w, h, gap, 0, 1f);
        if (p >= 1f) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.currentScreen == screen) screen.close();
            });
        }
    }

    private static int rollScale(int screenH) {
        return Math.max(1, Math.min(3, Math.round(screenH / 180f)));
    }

    /**
     * Draws the snapshot columns [x0, x1) aligned 1:1 with the screen. {@code blackUnder} adds an opaque
     * black base so framebuffer texels with zero alpha do not reveal the map below.
     */
    private static void drawSnapshot(DrawContext ctx, Identifier id, int x0, int x1, int w, int h,
                                     boolean blackUnder) {
        // Own root layer: inside one layer the GUI sorts by pipeline, so the opaque snapshot would land
        // below HUD text (chat etc.) that shares the layer with Xaero's screen.
        ctx.createNewRootLayer();
        if (blackUnder) ctx.fill(x0, 0, x1, h, 0xFF000000);
        if (x1 <= x0) return;
        // The framebuffer copy carries alpha 0, so a blended draw would be invisible. The opaque
        // background pipeline writes the texels without blending. UVs are given in screen units
        // (texture size = screen size); a negative region height flips V for GL's bottom-up rows.
        int span = x1 - x0;
        ctx.drawTexture(RenderPipelines.GUI_OPAQUE_TEX_BG, id, x0, 0,
                (float) x0, SNAPSHOT_FLIP_V ? (float) h : 0f,
                span, h, span, SNAPSHOT_FLIP_V ? -h : h, w, h);
    }

    /**
     * Draws the roll pair around the strip [cx - gap, cx + gap]. Near gap 0 the rolls are pushed apart
     * so the closed pair touches at the center; from a gap of one roll width on they are centered on the
     * strip edge, which hides the cut between snapshot and map.
     */
    private static void drawRolls(DrawContext ctx, int w, int h, int gap, int yOff, float alpha) {
        ctx.createNewRootLayer();
        if (alpha <= 0f) return;
        int k = rollScale(h);
        int rollW = ROLL_TEX_W * k;
        int cx = w / 2;
        int off = Math.round(rollW / 2f * Math.max(0f, Math.min(1f, 1f - (float) gap / rollW)));
        int leftX = cx - gap - off - rollW / 2;
        int rightX = w - cx + gap + off - rollW / 2;
        drawRoll(ctx, leftX, yOff, h, k, alpha, true);
        drawRoll(ctx, rightX, yOff, h, k, alpha, false);
    }

    /** One roll: drop shadow on the world side, then top cap, tiled body and bottom cap. */
    private static void drawRoll(DrawContext ctx, int x, int y0, int h, int k, float alpha, boolean leftRoll) {
        int rollW = ROLL_TEX_W * k;
        int y1 = y0 + h;

        // Soft drop shadow onto the world side (outer side of the roll).
        for (int i = 0; i < SHADOW_PX; i++) {
            int sa = Math.round(255 * SHADOW_ALPHA * alpha * (1f - (float) i / SHADOW_PX));
            if (sa <= 0) continue;
            int sx = leftRoll ? x - 1 - i : x + rollW + i;
            ctx.fill(sx, y0, sx + 1, y1, sa << 24);
        }

        int color = (Math.round(255 * alpha) << 24) | 0xFFFFFF;
        int capH = ROLL_CAP_H * k;
        int tileH = ROLL_BODY_H * k;
        int bodyStart = y0 + capH;
        int bodyEnd = y1 - capH;
        for (int y = bodyStart; y < bodyEnd; y += tileH) {
            int drawH = Math.min(tileH, bodyEnd - y);
            int regionH = Math.min(ROLL_BODY_H, (drawH + k - 1) / k);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ROLL_BODY, x, y, 0f, 0f,
                    rollW, drawH, ROLL_TEX_W, regionH, ROLL_TEX_W, ROLL_BODY_H, color);
        }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ROLL_CAP_TOP, x, y0, 0f, 0f,
                rollW, capH, ROLL_TEX_W, ROLL_CAP_H, ROLL_TEX_W, ROLL_CAP_H, color);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ROLL_CAP_BOTTOM, x, y1 - capH, 0f, 0f,
                rollW, capH, ROLL_TEX_W, ROLL_CAP_H, ROLL_TEX_W, ROLL_CAP_H, color);
    }

    /** Fallback when no snapshot is available: two dark curtains slide from the center. */
    private static void drawCurtains(Screen screen, DrawContext ctx, long t) {
        if (t >= CURTAIN_MS) return;

        float p = (float) t / CURTAIN_MS;
        float open = 1f - (1f - p) * (1f - p) * (1f - p); // easeOutCubic
        int w = screen.width;
        int h = screen.height;
        int half = w / 2;
        int gap = Math.round(half * open);
        int leftEdge = half - gap;
        int rightEdge = w - half + gap;
        // Curtains get slightly transparent toward the end so the reveal does not cut off hard.
        int alpha = Math.round(255 * Math.min(1f, (1f - p) * 2.2f));
        if (alpha < 4) return;
        int curtain = (alpha << 24) | COL_CURTAIN;
        int edge = (alpha << 24) | COL_EDGE;
        int edgeShadow = (alpha << 24) | COL_EDGE_SHADOW;
        if (leftEdge > 0) {
            ctx.fill(0, 0, leftEdge, h, curtain);
            ctx.fill(leftEdge - 1, 0, leftEdge, h, edge);
            if (leftEdge > 1) ctx.fill(leftEdge - 2, 0, leftEdge - 1, h, edgeShadow);
        }
        if (rightEdge < w) {
            ctx.fill(rightEdge, 0, w, h, curtain);
            ctx.fill(rightEdge, 0, rightEdge + 1, h, edge);
            if (rightEdge + 1 < w) ctx.fill(rightEdge + 1, 0, rightEdge + 2, h, edgeShadow);
        }
    }

    private static void startZoom(Screen screen) {
        if (!resolveReflection(screen.getClass())) return;
        try {
            double dest = destScaleField.getDouble(null);
            if (!(dest > 0)) return;
            double from = dest * ZOOM_FROM_FACTOR;
            userScaleField.setDouble(screen, from);
            zoomAnimField.set(screen, sinAnimationCtor.newInstance(from, dest, ZOOM_MS));
        } catch (Exception e) {
            DebugLog.debug("[map] Weltkarten-Zoomanimation uebersprungen: {}", e.toString());
        }
    }

    private static boolean resolveReflection(Class<?> guiMap) {
        if (reflectionTried) return sinAnimationCtor != null;
        reflectionTried = true;
        try {
            userScaleField = guiMap.getDeclaredField("userScale");
            userScaleField.setAccessible(true);
            destScaleField = guiMap.getDeclaredField("destScale");
            destScaleField.setAccessible(true);
            zoomAnimField = guiMap.getDeclaredField("zoomAnim");
            zoomAnimField.setAccessible(true);
            Class<?> sin = Class.forName(SIN_ANIMATION, false, guiMap.getClassLoader());
            sinAnimationCtor = sin.getConstructor(double.class, double.class, long.class);
            return true;
        } catch (Exception e) {
            sinAnimationCtor = null;
            DebugLog.debug("[map] Weltkarten-Zoomanimation nicht verfuegbar: {}", e.toString());
            return false;
        }
    }
}
