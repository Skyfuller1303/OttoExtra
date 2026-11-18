package de.ottoextra.resources;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Notice when entering a zone, starting or resetting its timer and after mining, built after the
 * sketches toast_f..j (gen_zonen_toast_hud.py): the compact "Leiste" (196x30) or the detailed
 * "Siegel" (160x62) on the bevelled plate of the activity HUD, like the core mod's animal toast.
 * All text is drawn without shadow.
 *
 * <p>Position and timing follow the core mod's region notice that this replaces: its display time,
 * fade in/out and position ({@code OttoExtraConfig.regions.overlayPosition}, top margin). The
 * notice moves out of the way of the resource HUD panel when both would overlap. At the top centre it
 * takes a slot in the core's shared column ({@code TopCenterStack}): below the boss bars, the Werkstatt
 * bar and the animal toast, above the vote toast. Older cores without that column keep the plain margin.
 */
final class ResourceNotice {
    static final String STYLE_COMPACT = "compact";
    static final String STYLE_DETAILED = "detailed";

    static final int COMPACT_WIDTH = 196;
    static final int COMPACT_HEIGHT = 30;
    static final int DETAILED_WIDTH = 160;
    static final int DETAILED_HEIGHT = 62;

    // Timing and fallback position of the core region notice (RegionNotificationOverlay).
    private static final long DEFAULT_DISPLAY_MS = 4_200L;
    private static final long FADE_IN_MS = 260L;
    private static final long FADE_OUT_MS = 360L;
    private static final int DEFAULT_MARGIN = 6;
    /** Slot of the notice in the core's top-centre column. */
    static final String STACK_ID = "ottoextra_resources:zone_notice";

    // Colours of gen_zonen_toast_hud.py / AnimalToastCards / the activity HUD panel.
    private static final int PANEL = 0x961E1E24;
    private static final int OUTLINE = 0xD7141418;
    private static final int BEVEL_L = 0xD7514F59;
    private static final int BEVEL_D = 0xD72E2D35;
    private static final int SOLID_OUTLINE = 0xFF141418;
    private static final int D2 = 0xFF2E2D35;
    private static final int D3 = 0xFF3B3A45;
    private static final int NAME = 0xFFF0EBE0;
    private static final int OCHRE = 0xFFD6C194;
    private static final int SUB = 0xFFA9A6B0;
    private static final int LABEL = 0xFF8F8C96;
    private static final float SMALL = 0.75f;

    /** Everything a notice draws, prepared when it is shown. */
    record Content(String zoneId, ResourceNoticeModel model, String name, String detail, String gefolge,
                   Identifier portrait, Identifier seal, String hint) { }

    private record Shown(Content content, long shownAt, boolean sticky) { }

    private final ResourcesConfig config;
    private final ResourceHud hud;
    private volatile Shown shown;

    ResourceNotice(ResourcesConfig config, ResourceHud hud) {
        this.config = config;
        this.hud = hud;
    }

    void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    /** Shows a notice for the configured display time. */
    void show(Content content) {
        shown = new Shown(content, System.currentTimeMillis(), false);
    }

    /** Shows a notice until {@link #clear(String)} or another notice replaces it. */
    void showSticky(Content content) {
        shown = new Shown(content, System.currentTimeMillis(), true);
    }

    /** Hides the notice of this zone, if it is the one shown. */
    void clear(String zoneId) {
        Shown current = shown;
        if (current != null && current.content().zoneId().equals(zoneId)) {
            shown = null;
            releaseStack();
        }
    }

    void clear() {
        shown = null;
        releaseStack();
    }

    /** Coat of arms of the zone's list (if chosen and known), otherwise the list symbol. */
    static Identifier seal(ResourceList list) {
        if (list == null) return null;
        Identifier arms = list.useArms ? ResourceListIcons.coatOfArms(list.name) : null;
        return arms != null ? arms : ResourceListIcons.texture(list.icon, list.color);
    }

    private void render(DrawContext context) {
        Shown current = shown;
        if (current == null) {
            releaseStack();
            return;
        }
        long elapsed = System.currentTimeMillis() - current.shownAt();
        long displayMs = displayMs();
        if (!current.sticky() && elapsed > displayMs) {
            shown = null;
            releaseStack();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.textRenderer == null) {
            releaseStack();
            return;
        }
        float progress = current.sticky() ? 1f : elapsed < FADE_IN_MS ? elapsed / (float) FADE_IN_MS
                : elapsed > displayMs - FADE_OUT_MS ? (displayMs - elapsed) / (float) FADE_OUT_MS : 1f;
        progress = Math.max(0f, Math.min(1f, progress));
        float ease = 1f - (1f - progress) * (1f - progress);
        boolean compact = !STYLE_DETAILED.equals(config.noticeStyle);
        int w = compact ? COMPACT_WIDTH : DETAILED_WIDTH;
        int h = compact ? COMPACT_HEIGHT : DETAILED_HEIGHT;
        int[] pos = position(context.getScaledWindowWidth(), context.getScaledWindowHeight(), w, h, ease);
        TextRenderer font = client.textRenderer;
        if (compact) drawCompact(context, font, current.content(), pos[0], pos[1], progress);
        else drawDetailed(context, font, current.content(), pos[0], pos[1], progress);
    }

    /** Display time of the core region notice; its default when the core config is not readable. */
    private static long displayMs() {
        try {
            var regions = de.ottoextra.config.OttoExtraConfig.active().regions;
            if (regions != null) return Math.max(1_500L, regions.displayDurationMs);
        } catch (Throwable ignored) {
            // Older or missing core config: use the core's own default.
        }
        return DEFAULT_DISPLAY_MS;
    }

    /** Position like the core region notice, slid in with {@code ease}, then moved off the HUD panel. */
    private int[] position(int screenW, int screenH, int w, int h, float ease) {
        String where = "TOP_CENTER";
        int margin = DEFAULT_MARGIN;
        try {
            var regions = de.ottoextra.config.OttoExtraConfig.active().regions;
            if (regions != null) {
                if (regions.overlayPosition != null) where = regions.overlayPosition.trim().toUpperCase(java.util.Locale.ROOT);
                margin = Math.max(4, regions.screenTopMargin);
            }
        } catch (Throwable ignored) {
            // Keep the defaults of the core region notice.
        }
        if (!"TOP_CENTER".equals(where)) releaseStack();
        int x;
        int y;
        switch (where) {
            case "TOP_RIGHT" -> {
                int target = screenW - w - margin;
                x = Math.round((screenW + 4) + (target - (screenW + 4)) * ease);
                y = margin;
            }
            case "TOP_LEFT" -> {
                x = Math.round((-w - 4) + (margin - (-w - 4)) * ease);
                y = margin;
            }
            case "CENTER" -> {
                x = (screenW - w) / 2;
                y = Math.max(margin, screenH - 92 - h);
            }
            default -> {
                x = (screenW - w) / 2;
                int target = "TOP_CENTER".equals(where) ? stackY(h, margin) : margin;
                y = Math.round((-h - 4) + (target - (-h - 4)) * ease);
            }
        }
        return avoid(x, y, w, h, hud == null ? null : hud.lastDrawn(), screenH);
    }

    /** The y the core's top-centre column hands out; {@code fallback} with an older core without it. */
    private static int stackY(int h, int fallback) {
        try {
            return de.ottoextra.ui.hudlayout.TopCenterStack.claim(STACK_ID,
                    de.ottoextra.ui.hudlayout.TopCenterStack.ORDER_ZONE_TOAST, h);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static void releaseStack() {
        try {
            de.ottoextra.ui.hudlayout.TopCenterStack.release(STACK_ID);
        } catch (Throwable ignored) {
            // Older core without the top-centre column.
        }
    }

    /** Moves the notice below (or, without room, above) the HUD panel when both would overlap. */
    static int[] avoid(int x, int y, int w, int h, int[] panel, int screenH) {
        if (panel == null) return new int[] {x, y};
        boolean overlaps = x < panel[0] + panel[2] && panel[0] < x + w && y < panel[1] + panel[3] && panel[1] < y + h;
        if (!overlaps) return new int[] {x, y};
        int below = panel[1] + panel[3] + 2;
        if (below + h <= screenH) return new int[] {x, below};
        return new int[] {x, Math.max(0, panel[1] - h - 2)};
    }

    // ---- cards ---------------------------------------------------------------------------

    private static void drawCompact(DrawContext context, TextRenderer font, Content c, int x, int y, float alpha) {
        int w = COMPACT_WIDTH;
        int h = COMPACT_HEIGHT;
        ResourceNoticeModel m = c.model();
        int[] ramp = m.ramp();
        panel(context, x, y, w, h, alpha);
        context.fill(x + 2, y + 2, x + 5, y + h - 2, a(ramp[1], alpha));
        context.fill(x + 2, y + 2, x + 5, y + 3, a(ramp[2], alpha));
        portrait(context, c.portrait(), x + 9, y + 7, alpha);
        int rightX = x + w - 52;
        String pct = m.percent() + " %";
        context.drawText(font, pct, rightX + 46 - font.getWidth(pct), y + 6, a(NAME, alpha), false);
        bar(context, rightX, y + 17, 46, 4, m.fraction(), ramp, alpha);
        int nameRoom = rightX + 46 - font.getWidth(pct) - 6 - (x + 31);
        context.drawText(font, ResourceHud.truncate(font, c.name(), Math.max(10, nameRoom)),
                x + 31, y + 6, a(OCHRE, alpha), false);
        context.drawText(font, ResourceHud.truncate(font, Text.translatable(m.statusKey()).getString(),
                Math.max(10, rightX - 4 - (x + 31))), x + 31, y + 17, a(ramp[2], alpha), false);
        // Small label row under the bar: time until full, otherwise the key hint.
        String small = m.showsTime() ? fullIn(m) : c.hint();
        if (small != null && !small.isEmpty()) {
            // The label row is free from the end of the status text to the right edge of the bar.
            int statusEnd = x + 31 + font.getWidth(Text.translatable(m.statusKey()).getString()) + 4;
            int room = (int) ((rightX + 46 - Math.min(statusEnd, rightX)) / SMALL);
            String fitted = ResourceHud.truncate(font, small, Math.max(20, room));
            int width = Math.round(font.getWidth(fitted) * SMALL);
            small(context, font, fitted, rightX + 46 - width, y + 23, LABEL, alpha);
        }
    }

    private static void drawDetailed(DrawContext context, TextRenderer font, Content c, int x, int y, float alpha) {
        int w = DETAILED_WIDTH;
        int h = DETAILED_HEIGHT;
        ResourceNoticeModel m = c.model();
        int[] ramp = m.ramp();
        panel(context, x, y, w, h, alpha);
        context.fill(x + 3, y + 3, x + w - 3, y + 15, a(ramp[0], alpha));
        context.fill(x + 3, y + 3, x + w - 3, y + 4, a(ramp[1], alpha));
        String header = ResourceHud.truncate(font, Text.translatable(m.statusKey()).getString(), w - 14);
        context.drawText(font, header, x + (w - font.getWidth(header)) / 2, y + 5, a(NAME, alpha), false);
        portrait(context, c.portrait(), x + 8, y + 22, alpha);
        int textRoom = w - 31 - 9 - (c.seal() == null ? 0 : 18);
        context.drawText(font, ResourceHud.truncate(font, c.name(), textRoom), x + 31, y + 21, a(OCHRE, alpha), false);
        if (c.detail() != null && !c.detail().isEmpty()) {
            context.drawText(font, ResourceHud.truncate(font, c.detail(), textRoom), x + 31, y + 31, a(SUB, alpha), false);
        }
        if (c.seal() != null) icon(context, c.seal(), x + w - 22, y + 20, alpha);
        int barW = w - 40;
        bar(context, x + 31, y + 44, barW, 5, m.fraction(), ramp, alpha);
        // Caption row: the key hint when there is one, otherwise how far the zone has recovered.
        String caption = c.hint() != null && !c.hint().isEmpty() ? c.hint()
                : m.percent() >= 100 ? Text.translatable("ottoextra.resources.notice.fully").getString()
                : Text.translatable("ottoextra.resources.notice.recovered", m.percent()).getString();
        String right = m.showsTime() ? fullIn(m) : c.gefolge();
        int rightW = right == null || right.isEmpty() ? 0 : Math.round(font.getWidth(right) * SMALL);
        String shownCaption = ResourceHud.truncate(font, caption, Math.max(20, barW - (rightW == 0 ? 0 : rightW + 6)));
        context.drawText(font, shownCaption, x + 31, y + 51, a(LABEL, alpha), false);
        if (rightW > 0 && font.getWidth(shownCaption) + 6 + rightW <= barW) {
            small(context, font, right, x + 31 + barW - rightW, y + 52, LABEL, alpha);
        }
    }

    /** "voll in 1:12 h". */
    static String fullIn(ResourceNoticeModel model) {
        return Text.translatable("ottoextra.resources.notice.full_in", model.clock()).getString();
    }

    // ---- drawing helpers -----------------------------------------------------------------

    /** The activity HUD plate: outline without corners, bevel, translucent fill. */
    private static void panel(DrawContext context, int x, int y, int w, int h, float alpha) {
        int outline = a(OUTLINE, alpha);
        int r = x + w;
        int b = y + h;
        context.fill(x + 1, y, r - 1, y + 1, outline);
        context.fill(x + 1, b - 1, r - 1, b, outline);
        context.fill(x, y + 1, x + 1, b - 1, outline);
        context.fill(r - 1, y + 1, r, b - 1, outline);
        context.fill(x + 1, y + 1, x + 2, y + 2, outline);
        context.fill(r - 2, y + 1, r - 1, y + 2, outline);
        context.fill(x + 1, b - 2, x + 2, b - 1, outline);
        context.fill(r - 2, b - 2, r - 1, b - 1, outline);
        context.fill(x + 2, y + 1, r - 2, y + 2, a(BEVEL_L, alpha));
        context.fill(x + 1, y + 2, x + 2, b - 2, a(BEVEL_L, alpha));
        context.fill(x + 2, b - 2, r - 2, b - 1, a(BEVEL_D, alpha));
        context.fill(r - 2, y + 2, r - 1, b - 2, a(BEVEL_D, alpha));
        context.fill(x + 2, y + 2, r - 2, b - 2, a(PANEL, alpha));
    }

    private static void portrait(DrawContext context, Identifier icon, int x, int y, float alpha) {
        context.fill(x - 2, y - 2, x + 18, y + 18, a(SOLID_OUTLINE, alpha));
        context.fill(x - 1, y - 1, x + 17, y + 17, a(D3, alpha));
        if (icon != null) icon(context, icon, x, y, alpha);
    }

    private static void icon(DrawContext context, Identifier icon, int x, int y, float alpha) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, 16, 16, 16, 16,
                a(0xFFFFFFFF, alpha));
    }

    private static void bar(DrawContext context, int x, int y, int w, int h, float fraction, int[] ramp, float alpha) {
        context.fill(x, y, x + w, y + h, a(SOLID_OUTLINE, alpha));
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, a(D2, alpha));
        int fill = (int) ((w - 2) * Math.max(0f, Math.min(1f, fraction)));
        if (fill > 0) {
            context.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, a(ramp[1], alpha));
            context.fill(x + 1, y + 1, x + 1 + fill, y + 2, a(ramp[2], alpha));
        }
    }

    private static void small(DrawContext context, TextRenderer font, String text, int x, int y, int color, float alpha) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(SMALL, SMALL);
        context.drawText(font, text, 0, 0, a(color, alpha), false);
        matrices.popMatrix();
    }

    private static int a(int argb, float alpha) {
        int original = (argb >>> 24) & 0xFF;
        int adjusted = Math.round(original * Math.max(0f, Math.min(1f, alpha)));
        return (adjusted << 24) | (argb & 0x00FFFFFF);
    }
}
