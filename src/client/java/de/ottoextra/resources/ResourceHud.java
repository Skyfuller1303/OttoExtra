package de.ottoextra.resources;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Timer panel at the right screen edge, built after the sketches hud_a_aktuelles_lehen.png (full,
 * default) and hud_c_knapp.png (compact): dark translucent slate panel, coloured dot per kind,
 * time coloured by how soon the entry is full, no text shadows. {@link #update} prepares the
 * panel about once per second; {@link #render} only draws it.
 *
 * <p>Position is a corner, an offset and a scale from {@link ResourcesConfig}, set in OttoExtra's
 * "HUD anordnen" editor through {@link ResourceHudLayoutElement}. Each frame the panel asks
 * OttoExtra's HudFlow where to draw so it does not overlap other HUD panels.
 */
final class ResourceHud {
    static final String STYLE_FULL = "full";
    static final String STYLE_COMPACT = "compact";

    /** Id of the panel in HudFlow and in the layout editor. */
    static final String FLOW_ID = "ottoextra_resources:timer_hud";
    private static final int PAD_X = 4;
    private static final int HEADER_H = 15;
    private static final int ROW = 14;
    private static final int FOOTER_H = 14;
    private static final int FULL_W = 150;
    private static final int COMPACT_W = 126;
    private static final int COMPACT_NEXT = 3;

    // Colours of gen_minentimer_hud.py (ARGB).
    private static final int COL_PANEL = 0x961E1E24;
    private static final int COL_OUTLINE = 0xD7141418;
    private static final int COL_BEVEL_L = 0xD7514F59;
    private static final int COL_BEVEL_D = 0xD72E2D35;
    private static final int COL_ROW_BG = 0x503B3A45;
    private static final int COL_DIV_D = 0xC8141418;
    private static final int COL_DIV_L = 0xC83B3A45;
    private static final int COL_TITLE = 0xFFD6C194;
    private static final int COL_NAME = 0xFFF0EBE0;
    private static final int COL_NAME_WAIT = 0xFFCFCBD6;
    private static final int COL_DIM = 0xFF8F8C96;
    private static final int COL_READY = 0xFF86C25A;
    private static final int COL_REGEN = 0xFFD9B44A;
    private static final int COL_SOON = 0xFFE08A4A;
    private static final int COL_BAR_BG = 0xC8141418;

    /** Everything the panel draws, prepared by {@link #update}. */
    private record Panel(boolean compact, String title, String right, List<ResourceSnapshot> rows,
                         List<ResourceSnapshot> ready, String footer) { }

    private static volatile ResourceHud instance;

    private final ResourcesConfig config;
    private volatile Panel panel;
    /** x, y, w, h of the panel drawn in the last frame, or null; read by {@link ResourceNotice}. */
    private volatile int[] lastDrawn;

    ResourceHud(ResourcesConfig config) {
        this.config = config;
    }

    void register() {
        instance = this;
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    /** The registered panel, or null before the client entry point ran. */
    static ResourceHud instance() {
        return instance;
    }

    /** Prepares the panel; call on the client thread, not every frame. */
    void update(MinecraftClient client, ResourceHudModel.Result result, String territory) {
        String scope = config.hudShowAll ? Text.translatable("ottoextra.resources.hud.all").getString()
                : territory == null ? "" : territory;
        if (STYLE_COMPACT.equals(config.hudStyle)) {
            if (result.ready().isEmpty() && result.upcoming().isEmpty()) {
                panel = null;
                return;
            }
            String title = scope.isEmpty() ? Text.translatable("ottoextra.resources.hud.title").getString() : scope;
            List<ResourceSnapshot> next = result.upcoming().subList(0, Math.min(COMPACT_NEXT, result.upcoming().size()));
            panel = new Panel(true, title, "", List.copyOf(next), result.ready(), "");
            return;
        }
        if (result.shown().isEmpty()) {
            panel = null;
            return;
        }
        int ready = result.ready().size();
        String footer = result.nextSeconds() < 0
                ? Text.translatable("ottoextra.resources.hud.footer_ready", ready).getString()
                : Text.translatable("ottoextra.resources.hud.footer", ready,
                        ResourceBook.clockText(Math.max(1, (result.nextSeconds() + 59) / 60))).getString();
        if (result.more() > 0) {
            footer += ", " + Text.translatable("ottoextra.resources.hud.more", result.more()).getString();
        }
        panel = new Panel(false, Text.translatable("ottoextra.resources.hud.title").getString(), scope,
                result.shown(), List.of(), footer);
    }

    void clear() {
        panel = null;
    }

    /** Rectangle of the panel in the last frame (x, y, w, h), or null when it was not drawn. */
    int[] lastDrawn() {
        return lastDrawn;
    }

    private void render(DrawContext context) {
        lastDrawn = null;
        Panel current = panel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (current == null || !config.hudEnabled || client.options.hudHidden
                || client.debugHudEntryList.isF3Enabled()) return;
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        float scale = ResourceHudPlacement.clampScale(config.hudScale);
        int[] size = scaledSize(current, scale);
        ResourceHudPlacement.Corner corner = ResourceHudPlacement.Corner.parse(config.hudCorner);
        int[] pos = ResourceHudPlacement.position(corner, config.hudOffsetX, config.hudOffsetY,
                screenW, screenH, size[0], size[1]);
        int[] flowed = flow(pos[0], pos[1], size[0], size[1], corner.bottom);
        if (flowed != null) pos = flowed;
        lastDrawn = new int[] {pos[0], pos[1], size[0], size[1]};
        draw(context, client.textRenderer, current, pos[0], pos[1], scale);
    }

    /** Asks OttoExtra's HudFlow where to draw; null means at the wanted spot (also with an older OttoExtra). */
    private static int[] flow(int x, int y, int w, int h, boolean pushUp) {
        try {
            return de.ottoextra.ui.hudlayout.HudFlow.place(FLOW_ID, x, y, w, h, pushUp,
                    de.ottoextra.ui.hudlayout.HudFlow.PRIORITY_ADDON);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Wanted rectangle of the current panel, or of a sample panel when there is nothing to show. */
    int[] wantedBounds(int screenW, int screenH) {
        float scale = ResourceHudPlacement.clampScale(config.hudScale);
        int[] size = scaledSize(shownOrSample(), scale);
        int[] pos = ResourceHudPlacement.position(ResourceHudPlacement.Corner.parse(config.hudCorner),
                config.hudOffsetX, config.hudOffsetY, screenW, screenH, size[0], size[1]);
        return new int[] {pos[0], pos[1], size[0], size[1]};
    }

    /** Draws the current or a sample panel at (x, y) for the layout editor. */
    void drawPreview(DrawContext context, TextRenderer font, int x, int y) {
        draw(context, font, shownOrSample(), x, y, ResourceHudPlacement.clampScale(config.hudScale));
    }

    private Panel shownOrSample() {
        Panel current = panel;
        return current != null ? current : sample(STYLE_COMPACT.equals(config.hudStyle));
    }

    /** Three made-up entries so the editor shows the panel's real size and look. */
    private static Panel sample(boolean compact) {
        ResourceZone zone = ResourceZone.point(0, 64, 0, 3);
        ResourceSnapshot ready = new ResourceSnapshot("sample-1", "Eisenmine", "IRON_MINE", "", "minecraft:overworld",
                zone, "READY", 100, "", "", 0, 0);
        ResourceSnapshot soon = new ResourceSnapshot("sample-2", "Kohlenflöz", "COAL_DEPOSIT", "",
                "minecraft:overworld", zone, "REGENERATING", 70, "", "", 8 * 60, 8 * 60);
        ResourceSnapshot later = new ResourceSnapshot("sample-3", "Torfmoor", "PEAT_BOG", "", "minecraft:overworld",
                zone, "REGENERATING", 30, "", "", 74 * 60, 74 * 60);
        String title = Text.translatable("ottoextra.resources.hud.title").getString();
        if (compact) return new Panel(true, title, "", List.of(soon, later), List.of(ready), "");
        String footer = Text.translatable("ottoextra.resources.hud.footer", 1, ResourceBook.clockText(8)).getString();
        return new Panel(false, title, "", List.of(ready, soon, later), List.of(), footer);
    }

    private static int width(Panel p) {
        return p.compact() ? COMPACT_W : FULL_W;
    }

    private static int height(Panel p) {
        return p.compact() ? HEADER_H + ROW + p.rows().size() * (ROW - 3) + 3
                : HEADER_H + p.rows().size() * ROW + FOOTER_H;
    }

    private static int[] scaledSize(Panel p, float scale) {
        return new int[] {ResourceHudPlacement.scaled(width(p), scale), ResourceHudPlacement.scaled(height(p), scale)};
    }

    /** Draws the panel with its top-left corner at (x, y), scaled around that corner. */
    private static void draw(DrawContext context, TextRenderer font, Panel p, int x, int y, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        if (p.compact()) drawCompact(context, font, p);
        else drawFull(context, font, p);
        context.getMatrices().popMatrix();
    }

    private static void drawFull(DrawContext context, TextRenderer font, Panel p) {
        int w = width(p);
        int h = height(p);
        int ox = 0;
        int oy = 0;
        frame(context, font, ox, oy, w, h, p.title(), p.right());
        int y = oy + HEADER_H;
        for (ResourceSnapshot row : p.rows()) {
            row(context, font, ox, y, w, row, true);
            y += ROW;
        }
        divider(context, ox, y, w);
        ResourceBook.small(context, font, ResourceBook.clipSmall(font, p.footer(), w - 2 * PAD_X), ox + PAD_X, y + 4,
                COL_DIM);
    }

    private static void drawCompact(DrawContext context, TextRenderer font, Panel p) {
        int w = width(p);
        int h = height(p);
        int ox = 0;
        int oy = 0;
        frame(context, font, ox, oy, w, h, p.title(), null);
        int y = oy + HEADER_H;
        context.fill(ox + 4, y, ox + w - 4, y + ROW - 3, COL_ROW_BG);
        String count = Text.translatable("ottoextra.resources.book.ready", p.ready().size()).getString();
        int room = w - 2 * PAD_X - 2 - 2 - font.getWidth(count);
        int x = ox + PAD_X + 2;
        int dots = Math.min(p.ready().size(), Math.max(0, room / 7));
        for (int index = 0; index < dots; index++) {
            ResourceGem.draw(context, x, y + 3, p.ready().get(index).type(), 1);
            x += 7;
        }
        context.drawText(font, count, x + 2, y + 1, p.ready().isEmpty() ? COL_DIM : COL_READY, false);
        y += ROW;
        for (ResourceSnapshot row : p.rows()) {
            row(context, font, ox, y, w, row, false);
            y += ROW - 3;
        }
    }

    /** Panel with outline, bevel, title and the divider below the header. */
    private static void frame(DrawContext context, TextRenderer font, int ox, int oy, int w, int h, String title,
                              String right) {
        int[][] outline = {{1, 0, w - 1, 1}, {1, h - 1, w - 1, h}, {0, 1, 1, h - 1}, {w - 1, 1, w, h - 1},
                {1, 1, 2, 2}, {w - 2, 1, w - 1, 2}, {1, h - 2, 2, h - 1}, {w - 2, h - 2, w - 1, h - 1}};
        for (int[] r : outline) context.fill(ox + r[0], oy + r[1], ox + r[2], oy + r[3], COL_OUTLINE);
        context.fill(ox + 2, oy + 1, ox + w - 2, oy + 2, COL_BEVEL_L);
        context.fill(ox + 1, oy + 2, ox + 2, oy + h - 2, COL_BEVEL_L);
        context.fill(ox + 2, oy + h - 2, ox + w - 2, oy + h - 1, COL_BEVEL_D);
        context.fill(ox + w - 2, oy + 2, ox + w - 1, oy + h - 2, COL_BEVEL_D);
        context.fill(ox + 2, oy + 2, ox + w - 2, oy + h - 2, COL_PANEL);
        int titleRoom = w - 2 * PAD_X;
        if (right != null && !right.isEmpty()) {
            String shown = truncate(font, right, Math.max(20, w - 2 * PAD_X - font.getWidth(title) - 6));
            int rw = font.getWidth(shown);
            context.drawText(font, shown, ox + w - PAD_X - rw, oy + 3, COL_DIM, false);
            titleRoom -= rw + 6;
        }
        context.drawText(font, truncate(font, title, titleRoom), ox + PAD_X, oy + 3, COL_TITLE, false);
        divider(context, ox, oy + HEADER_H - 3, w);
    }

    private static void divider(DrawContext context, int ox, int y, int w) {
        context.fill(ox + 2, y, ox + w - 2, y + 1, COL_DIV_D);
        context.fill(ox + 2, y + 1, ox + w - 2, y + 2, COL_DIV_L);
    }

    /** Dot, shortened name and time; with {@code bar} a thin progress line under recovering rows. */
    private static void row(DrawContext context, TextRenderer font, int ox, int y, int w, ResourceSnapshot row,
                            boolean bar) {
        ResourceGem.draw(context, ox + PAD_X + 1, y + 3, row.type(), 1);
        int minutes = ResourceBook.minutesLeft(row);
        String time = ResourceBook.timeText(row);
        int timeX = ox + w - PAD_X - font.getWidth(time);
        int room = timeX - (ox + PAD_X + 9) - 4;
        context.drawText(font, truncate(font, row.name(), room), ox + PAD_X + 9, y + 1,
                minutes <= 0 ? COL_NAME : COL_NAME_WAIT, false);
        int color = timeColor(minutes);
        context.drawText(font, time, timeX, y + 1, color, false);
        if (bar && minutes > 0) {
            int x0 = ox + PAD_X + 9;
            int x1 = ox + w - PAD_X;
            context.fill(x0, y + 11, x1, y + 12, COL_BAR_BG);
            context.fill(x0, y + 11, x0 + (int) Math.round((x1 - x0) * ResourceBook.progress(row)), y + 12, color);
        }
    }

    /** Green when ready, orange within ten minutes, amber otherwise. */
    static int timeColor(int minutesLeft) {
        return minutesLeft <= 0 ? COL_READY : minutesLeft <= 10 ? COL_SOON : COL_REGEN;
    }

    /** Shortens by two characters at a time and ends with a dot, like the sketch. */
    static String truncate(TextRenderer font, String text, int room) {
        String shown = text;
        while (font.getWidth(shown) > room && shown.length() > 3) {
            shown = shown.substring(0, shown.length() - 2).stripTrailing() + ".";
        }
        return shown;
    }
}
