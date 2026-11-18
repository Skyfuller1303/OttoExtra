package de.ottoextra.crafting.werkstatt;

import de.ottoextra.ui.theme.OttonienTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Werkstatt side panel at a crafting station (design T2, variant C): a frame tab with a gear
 * grows out of the right frame edge of the station GUI, 16 rows below its top, and opens a
 * parchment panel with the tabs Auftrag, Brennstoff and Warteschlange right of it; open, the tab's
 * outline rows and cord bands run on into the panel's cord frame, so both read as one piece.
 * Geometry follows the real GUI (any container size or layout, sub-menus included, see
 * {@link TabGeometry}). Open state and tab are kept per station ({@link StationMemory}). Drawn
 * after the screen but below the station's tooltips (see {@link WerkstattModule#renderPanel});
 * clicks on it never reach the station. Immediate mode: every frame records its click areas.
 */
final class WerkstattPanel {
    enum Tab { JOB, FUEL, QUEUE }

    static final int PANEL_W = 206;
    /** Panel height at least this, even next to a small sub-menu. */
    private static final int MIN_PANEL_H = 222;
    /** Tab row above the panel. */
    private static final int TAB_ROW = 15;
    /** Width of the panel's cord frame. */
    private static final int FRAME = 3;
    private static final int LINE = 10;
    private static final int PAD = 6;

    private static final int GREEN = 0xFF3F7A2E;
    private static final int RED = 0xFFB24147;
    private static final int WARN_BG = 0xFFE3CB93;
    private static final int WARN_EDGE = 0xFF9A6A1E;
    private static final int WARN_TEXT = 0xFF7A4E10;
    private static final int GOLD = 0xFFE2A650;
    private static final int TAB_ACTIVE = 0xFFAA6841;
    private static final int TAB_FUEL = 0xFF6B5A2A;
    private static final int TAB_QUEUE = 0xFF434D66;

    // Frame colours of the T2 tab, sampled from the pack's generic_54.png (see gen_werkstatt.py).
    static final int F_OUT = 0xFF382A25;
    static final int F_BODY = 0xFF493626;
    static final int F_CORD = 0xFF855C2C;
    static final int F_CORD_DARK = 0xFF885633;
    private static final int BADGE_GOLD = 0xFFCD9F3D;
    private static final int BADGE_RUN = 0xFF1E1E24;
    private static final Identifier GEAR = de.ottoextra.OttoExtra.id("textures/gui/werkstatt/zahnrad.png");

    private record Hit(int x0, int y0, int x1, int y1, java.util.function.Consumer<Boolean> action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private final HandledScreen<?> screen;
    private final String station;
    private final List<Hit> hits = new ArrayList<>();
    private int mouseX;
    private int mouseY;

    WerkstattPanel(HandledScreen<?> screen, String station) {
        this.screen = screen;
        this.station = station == null ? "zeugtisch" : station;
    }

    private boolean open() {
        return Werkstatt.memory().isOpen(station);
    }

    private Tab tab() {
        Tab t = Werkstatt.memory().tab(station);
        return t == Tab.FUEL && !showFire() ? Tab.JOB : t;
    }

    private void setTab(Tab t) {
        Werkstatt.memory().setTab(station, t);
    }

    /** Fuel parts only at stations that show fire buttons (now or earlier in this session). */
    private boolean showFire() {
        return Werkstatt.memory().hasFire(station) || !fireButtons(pageSlots()).isEmpty();
    }

    // ------------------------------------------------------------------ geometry

    private int guiX() {
        try {
            return ((de.ottoextra.mixin.WerkstattHandledScreenAccessor) screen).ottoextra$werkstattX();
        } catch (Throwable t) {
            return (screen.width - guiW()) / 2;
        }
    }

    private int guiY() {
        try {
            return ((de.ottoextra.mixin.WerkstattHandledScreenAccessor) screen).ottoextra$werkstattY();
        } catch (Throwable t) {
            return (screen.height - guiH()) / 2;
        }
    }

    private int guiW() {
        try {
            return ((de.ottoextra.mixin.WerkstattHandledScreenAccessor) screen).ottoextra$werkstattWidth();
        } catch (Throwable t) {
            return 176;
        }
    }

    private int guiH() {
        try {
            return ((de.ottoextra.mixin.WerkstattHandledScreenAccessor) screen).ottoextra$werkstattHeight();
        } catch (Throwable t) {
            return 222;
        }
    }

    private TabGeometry.Tab frameTab() {
        return TabGeometry.tab(guiX(), guiY(), guiW(), guiH());
    }

    private boolean panelRight() {
        boolean[] side = new boolean[1];
        TabGeometry.panelX(frameTab(), guiX(), PANEL_W, screen.width, side);
        return side[0];
    }

    private int panelX() {
        return TabGeometry.panelX(frameTab(), guiX(), PANEL_W, screen.width, null);
    }

    /** Panel top = GUI top, so the tab row stands above the GUI; lower only if the screen has no room. */
    private int panelY() {
        return Math.max(TAB_ROW, guiY());
    }

    private int panelH() {
        int h = Math.max(guiH(), MIN_PANEL_H);
        return Math.max(60, Math.min(h, screen.height - panelY() - 2));
    }

    private boolean onPanel(double x, double y) {
        int px = panelX();
        int py = panelY();
        return open() && x >= px && x < px + PANEL_W && y >= py - TAB_ROW && y < py + panelH();
    }

    // ------------------------------------------------------------------ input

    /** Returns false if the click was handled here and must not reach the station. */
    boolean mouseClicked(Click click) {
        double x = click.x();
        double y = click.y();
        if (click.button() == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.contains(x, y)) {
                    h.action().accept(click.hasShift());
                    return false;
                }
            }
            if (onPanel(x, y)) {
                return false;
            }
            Slot slot = hoveredStationSlot();
            if (slot != null) {
                Werkstatt.onStationLeftClick(slot.id, slot.getStack());
            }
            return true;
        }
        if (click.button() == 1) {
            if (onPanel(x, y)) {
                return false;
            }
            Slot slot = hoveredStationSlot();
            if (slot != null && slot.hasStack()) {
                WerkstattJob job = Werkstatt.addFromStack(slot.id, slot.getStack(), pageSlots());
                if (job != null && open()) {
                    setTab(Tab.JOB);
                }
                // Right-click on a recipe is ours: never sent to the server.
                return false;
            }
        }
        return true;
    }

    /** Hovered slot of the station (any layout), never one of the player inventory. */
    private Slot hoveredStationSlot() {
        try {
            Slot slot = ((de.ottoextra.mixin.HandledScreenAccessor) screen).ottoextra$focusedSlot();
            return slot != null && !(slot.inventory instanceof PlayerInventory) ? slot : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private List<JobRunner.SlotView> pageSlots() {
        return Werkstatt.stationSlots(screen.getScreenHandler());
    }

    // ------------------------------------------------------------------ drawing

    void render(DrawContext ctx, int mx, int my) {
        hits.clear();
        mouseX = mx;
        mouseY = my;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        boolean open = open();
        if (open) {
            drawPanel(ctx, font);
        }
        drawFrameTab(ctx, font, open);
    }

    /** Cord pattern of the frame: light where (x + y) mod 4 is 0 or 1, relative to the given origin. */
    private static boolean cordLight(int x, int y, int ox, int oy) {
        return Math.floorMod((x - ox) + (y - oy), 4) < 2;
    }

    /** One row of cord pattern from x0 (inclusive) to x1 (exclusive), drawn in 2-pixel runs. */
    private static void cordRow(DrawContext ctx, int x0, int x1, int y, int ox, int oy, int light, int dark) {
        int x = x0;
        while (x < x1) {
            boolean l = cordLight(x, y, ox, oy);
            int end = x + 1;
            while (end < x1 && cordLight(end, y, ox, oy) == l) {
                end++;
            }
            OttonienTheme.rect(ctx, x, y, end, y + 1, l ? light : dark);
            x = end;
        }
    }

    private static void cordColumn(DrawContext ctx, int x, int y0, int y1, int ox, int oy, int light, int dark) {
        int y = y0;
        while (y < y1) {
            boolean l = cordLight(x, y, ox, oy);
            int end = y + 1;
            while (end < y1 && cordLight(x, end, ox, oy) == l) {
                end++;
            }
            OttonienTheme.rect(ctx, x, y, x + 1, end, l ? light : dark);
            y = end;
        }
    }

    /**
     * T2 frame tab: the frame's outline column (GUI width - 1) opens into the tab; outer corners cut
     * by one pixel; cord bands in the frame's diagonal pattern on the top two, bottom two and right
     * two inner rows/columns; the gear at (x0 + 1, y0 + 3). Hover: gold outline, frame body inside.
     */
    private void drawFrameTab(DrawContext ctx, TextRenderer font, boolean open) {
        TabGeometry.Tab t = frameTab();
        int gx = guiX();
        int gy = guiY();
        int x0 = t.x0();
        int y0 = t.y0();
        int x1 = t.right();
        int y1 = y0 + TabGeometry.HEIGHT - 1;
        boolean hover = mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
        boolean glow = Werkstatt.glowing();
        int out = hover || glow ? GOLD : F_OUT;
        int inner = hover ? F_BODY : F_OUT;
        OttonienTheme.rect(ctx, x0 + 1, y0, x1, y0 + 1, out);
        OttonienTheme.rect(ctx, x0 + 1, y1, x1, y1 + 1, out);
        OttonienTheme.rect(ctx, x1, y0 + 1, x1 + 1, y1, out);
        OttonienTheme.rect(ctx, x0, y0 + 1, x1, y1, inner);
        for (int yy : new int[] {y0 + 1, y0 + 2, y1 - 2, y1 - 1}) {
            cordRow(ctx, x0, x1, yy, gx, gy, F_CORD, F_BODY);
        }
        for (int xx : new int[] {x1 - 2, x1 - 1}) {
            cordColumn(ctx, xx, y0 + 1, y1, gx, gy, F_CORD, F_BODY);
        }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, GEAR, t.iconX(), t.iconY(), 0f, 0f, 16, 16, 16, 16);
        hits.add(new Hit(x0, y0, x1 + 1, y1 + 1, shift -> Werkstatt.memory().toggle(station)));
        if (open) {
            if (panelRight()) {
                bridge(ctx, t, gx, gy);
            }
            return;
        }
        drawBadges(ctx, font, x1 + 2, y0, glow);
    }

    /** Open state: the tab's outline and cord bands run on into the panel's cord frame. */
    private void bridge(DrawContext ctx, TabGeometry.Tab t, int gx, int gy) {
        int x1 = t.right();
        int y0 = t.y0();
        int h = TabGeometry.HEIGHT;
        int px = panelX();
        OttonienTheme.rect(ctx, x1 - 2, y0 + 3, px + FRAME, y0 + h - 3, F_OUT);
        for (int yy : new int[] {y0 + 1, y0 + 2, y0 + h - 3, y0 + h - 2}) {
            cordRow(ctx, x1 - 2, px + FRAME, yy, gx, gy, F_CORD, F_BODY);
        }
    }

    /** Badges 2 px right of the tab, top aligned: progress with flame, red stop dot, waiting count, "+1". */
    private void drawBadges(DrawContext ctx, TextRenderer font, int bx, int y, boolean glow) {
        JobRunner r = Werkstatt.runner();
        if (r != null && r.active() && station.equals(r.job().station)) {
            String progress = progressText(r.job());
            int w = font.getWidth(progress) + 13;
            badge(ctx, bx, y, w, 11, BADGE_RUN);
            flame(ctx, bx + 2, y + 2);
            ctx.drawText(font, progress, bx + 10, y + 2, OttonienTheme.LABEL, false);
            y += 13;
        } else if (Werkstatt.recentlyStopped() && r != null && station.equals(r.job().station)) {
            OttonienTheme.rect(ctx, bx, y + 1, bx + 6, y + 6, 0xFF000000);
            OttonienTheme.rect(ctx, bx + 1, y, bx + 5, y + 7, 0xFF000000);
            OttonienTheme.rect(ctx, bx + 1, y + 1, bx + 5, y + 6, RED);
            y += 9;
        }
        int waiting = Werkstatt.waitingCount(station);
        if (waiting > 0) {
            String n = Integer.toString(waiting);
            int w = Math.max(9, font.getWidth(n) + 5);
            badge(ctx, bx, y, w, 11, BADGE_GOLD);
            ctx.drawText(font, n, bx + (w - font.getWidth(n) + 1) / 2, y + 2, 0xFF2A1E12, false);
            y += 13;
        }
        if (glow) {
            int w = font.getWidth("+1") + 5;
            badge(ctx, bx, y, w, 11, GOLD);
            ctx.drawText(font, "+1", bx + 3, y + 2, 0xFF2A1E12, false);
        }
    }

    /** Cord frame like the pack's GUI border: dark outline, cord ring, frame body inside. */
    private static void cordFrame(DrawContext ctx, int x, int y, int w, int h) {
        int x1 = x + w;
        int y1 = y + h;
        OttonienTheme.rect(ctx, x, y, x1, y + 1, F_OUT);
        OttonienTheme.rect(ctx, x, y1 - 1, x1, y1, F_OUT);
        OttonienTheme.rect(ctx, x, y, x + 1, y1, F_OUT);
        OttonienTheme.rect(ctx, x1 - 1, y, x1, y1, F_OUT);
        cordRow(ctx, x + 1, x1 - 1, y + 1, x, y, F_CORD_DARK, F_CORD);
        cordRow(ctx, x + 1, x1 - 1, y1 - 2, x, y, F_CORD_DARK, F_CORD);
        cordColumn(ctx, x + 1, y + 2, y1 - 2, x, y, F_CORD_DARK, F_CORD);
        cordColumn(ctx, x1 - 2, y + 2, y1 - 2, x, y, F_CORD_DARK, F_CORD);
        OttonienTheme.rect(ctx, x + 2, y + 2, x1 - 2, y + 3, F_BODY);
        OttonienTheme.rect(ctx, x + 2, y1 - 3, x1 - 2, y1 - 2, F_BODY);
        OttonienTheme.rect(ctx, x + 2, y + 3, x + 3, y1 - 3, F_BODY);
        OttonienTheme.rect(ctx, x1 - 3, y + 3, x1 - 2, y1 - 3, F_BODY);
    }

    private void drawPanel(DrawContext ctx, TextRenderer font) {
        int px = panelX();
        int py = panelY();
        int ph = panelH();
        int tx = px;
        Tab current = tab();
        List<Tab> shown = new ArrayList<>(List.of(Tab.values()));
        if (!showFire()) {
            shown.remove(Tab.FUEL);
        }
        Tab[] tabs = shown.toArray(new Tab[0]);
        String[] labels = tabLabels(font, tabs, "");
        TabStrip.Layout strip = TabStrip.layout(widths(font, labels), PANEL_W);
        if (!strip.fits()) {
            labels = tabLabels(font, tabs, ".short");
            strip = TabStrip.layout(widths(font, labels), PANEL_W);
        }
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int w = strip.widths()[i];
            String label = clip(font, labels[i], w - 2 * TabStrip.MIN_PAD);
            boolean active = t == current;
            int color = active ? TAB_ACTIVE : t == Tab.FUEL ? TAB_FUEL : t == Tab.JOB ? 0xFF8A4E30 : TAB_QUEUE;
            int top = py - (active ? TAB_ROW : TAB_ROW - 2);
            OttonienTheme.rect(ctx, tx, top, tx + w, py, 0xFF2A1E14);
            OttonienTheme.rect(ctx, tx + 1, top + 1, tx + w - 1, py, color);
            ctx.drawText(font, label, tx + (w - font.getWidth(label) + 1) / 2, top + 3,
                    active ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
            Tab target = t;
            hits.add(new Hit(tx, top, tx + w, py, shift -> setTab(target)));
            tx += w + strip.gap();
        }
        cordFrame(ctx, px, py, PANEL_W, ph);
        int ix = px + FRAME;
        int iw = PANEL_W - 2 * FRAME;
        OttonienTheme.parchment(ctx, ix, py + FRAME, iw, ph - 2 * FRAME, 7, false, false);
        // Clicks inside the panel never fall through to the station.
        hits.add(0, new Hit(px, py, px + PANEL_W, py + ph, shift -> { }));
        int bottom = py + ph - FRAME - PAD;
        int cx = ix + PAD;
        int cy = py + FRAME + PAD;
        int cw = iw - 2 * PAD;
        switch (current) {
            case JOB -> drawJobTab(ctx, font, cx, cy, cw, bottom);
            case FUEL -> drawFuelTab(ctx, font, cx, cy, cw, bottom);
            case QUEUE -> drawQueueTab(ctx, font, cx, cy, cw, bottom);
        }
    }

    private static String[] tabLabels(TextRenderer font, Tab[] tabs, String suffix) {
        String[] labels = new String[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            labels[i] = tr("tab." + tabs[i].name().toLowerCase(java.util.Locale.ROOT) + suffix);
        }
        return labels;
    }

    private static int[] widths(TextRenderer font, String[] labels) {
        int[] out = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            out[i] = font.getWidth(labels[i]);
        }
        return out;
    }

    // ------------------------------------------------------------------ Auftrag

    private void drawJobTab(DrawContext ctx, TextRenderer font, int x, int y, int w, int bottom) {
        JobRunner r = Werkstatt.runner();
        if (r != null && station.equals(r.job().station)
                && (r.active() || Werkstatt.recentlyStopped() || Werkstatt.recentlyFinished())) {
            drawRunning(ctx, font, x, y, w, bottom, r);
            return;
        }
        WerkstattJob job = Werkstatt.selected(station);
        if (job == null || job.status == WerkstattJob.Status.DONE) {
            y = wrap(ctx, font, tr("hint.rightclick"), x, y, w, OttonienTheme.INK_MUTED);
            y = rule(ctx, x, y + 2, w);
            drawSaved(ctx, font, x, y + 2, w, bottom - LINE - 2);
            footer(ctx, font, x, bottom, w);
            return;
        }
        Map<String, Integer> inv = Werkstatt.inventory(MinecraftClient.getInstance());
        RecipeLore recipe = job.recipe();
        List<JobRunner.SlotView> page = pageSlots();
        CraftPlan.Fuel fuel = fuelFor(job, page);
        // Header: icon, name, path, remember star.
        ctx.drawItem(Werkstatt.icon(job), x, y);
        ctx.drawText(font, clip(font, job.name, w - 34), x + 20, y, OttonienTheme.INK, false);
        ctx.drawText(font, clip(font, job.pathText(), w - 22), x + 20, y + 9, OttonienTheme.INK_MUTED, false);
        boolean saved = Werkstatt.isSaved(job);
        int sx = x + w - 9;
        star(ctx, sx, y + 1, saved ? GOLD : OttonienTheme.PA_S);
        hits.add(new Hit(sx - 1, y, sx + 9, y + 10, shift -> Werkstatt.toggleSaved(job)));
        y += 21;
        if (!recipe.known()) {
            y = warn(ctx, font, tr("unknown"), x, y, w) + 2;
            ctx.drawText(font, tr("heat") + " " + tr("heat." + recipe.heat().name().toLowerCase(java.util.Locale.ROOT)),
                    x, y, OttonienTheme.INK_MUTED, false);
            y += LINE + 2;
        } else {
            ctx.drawText(font, tr("ingredients", job.quantity), x, y, OttonienTheme.INK_MUTED, false);
            y += LINE;
            CraftPlan.Result plan = CraftPlan.simulate(recipe, inv, job.quantity, fuel,
                    Werkstatt.fire().secondsLeft(System.currentTimeMillis()), Werkstatt.craftSeconds());
            for (List<RecipeLore.Ingredient> group : recipe.groups()) {
                RecipeLore.Ingredient use = choose(group, inv, job.quantity);
                int need = use.count() * job.quantity;
                int have = inv.getOrDefault(use.key(), 0);
                String label = use.name() + (use != group.get(0) ? " " + tr("instead", group.get(0).name()) : "");
                ctx.drawText(font, clip(font, label, w - 48), x, y, OttonienTheme.INK, false);
                String count = have + " / " + need;
                ctx.drawText(font, count, x + w - font.getWidth(count), y, have >= need ? GREEN : RED, false);
                y += LINE;
            }
            String heat = recipe.heat() == RecipeLore.Heat.YES && fuel != null
                    ? tr("heat.fires", plan.fires()) : tr("heat." + recipe.heat().name().toLowerCase(java.util.Locale.ROOT));
            ctx.drawText(font, tr("heat") + " " + heat, x, y, OttonienTheme.INK_MUTED, false);
            y += LINE;
            ctx.drawText(font, tr("output", job.quantity, job.quantity * job.outputCount, job.name), x, y,
                    OttonienTheme.INK_MUTED, false);
            y += LINE + 1;
            if (plan.fuelIsIngredient()) {
                y = warn(ctx, font, tr("fuel.shared", fuel.name()), x, y, w) + 2;
            }
        }
        int max = recipe.known() ? CraftPlan.max(recipe, inv, fuel,
                Werkstatt.fire().secondsLeft(System.currentTimeMillis()), Werkstatt.craftSeconds()) : -1;
        y = quantityRow(ctx, font, x, y, w, job, max) + 3;
        if (showFire()) {
            y = fuelRow(ctx, font, x, y, w, job, page) + 3;
        }
        y = autoRunRow(ctx, font, x, y, w) + 3;
        boolean auto = Werkstatt.config().werkstattAutoRun;
        String action = auto ? tr("craft.n", job.quantity) : tr("craft.once");
        button(ctx, font, x, y, w, 16, action, true, true, shift -> Werkstatt.start(job, false));
        y += 20;
        rule(ctx, x, y, w);
        drawSaved(ctx, font, x, y + 3, w, bottom - LINE - 2);
        footer(ctx, font, x, bottom, w);
    }

    private int quantityRow(DrawContext ctx, TextRenderer font, int x, int y, int w, WerkstattJob job, int max) {
        ctx.drawText(font, tr("amount"), x, y + 4, OttonienTheme.INK, false);
        int bx = x + font.getWidth(tr("amount")) + 4;
        button(ctx, font, bx, y, 14, 14, "-", true, false,
                shift -> Werkstatt.setQuantity(job, job.quantity - (shift ? 10 : 1)));
        String value = Integer.toString(job.quantity);
        OttonienTheme.rect(ctx, bx + 16, y, bx + 46, y + 14, OttonienTheme.FIELD_DARK);
        ctx.drawText(font, value, bx + 31 - font.getWidth(value) / 2, y + 3, OttonienTheme.LABEL, false);
        button(ctx, font, bx + 48, y, 14, 14, "+", true, false,
                shift -> Werkstatt.setQuantity(job, job.quantity + (shift ? 10 : 1)));
        int mx = bx + 64;
        String maxLabel = max < 0 ? tr("max.unknown") : tr("max", max);
        button(ctx, font, mx, y, x + w - mx, 14, maxLabel, max > 0, false,
                shift -> Werkstatt.setQuantity(job, max));
        return y + 14;
    }

    private int fuelRow(DrawContext ctx, TextRenderer font, int x, int y, int w, WerkstattJob job,
                        List<JobRunner.SlotView> page) {
        List<JobRunner.SlotView> fires = fireButtons(page);
        int fx = x;
        for (JobRunner.SlotView s : fires) {
            RecipeLore r = s.recipe();
            String fuelName = r.known() ? r.groups().get(0).get(0).name() : s.name();
            boolean chosen = WerkstattText.normalize(fuelName).equals(WerkstattText.normalize(job.fuel));
            OttonienTheme.tile(ctx, fx, y, 18, 18, chosen);
            if (chosen) {
                frame(ctx, fx, y, 18, 18, GOLD);
            }
            ItemStack icon = stackAt(s.slot());
            if (!icon.isEmpty()) {
                ctx.drawItem(icon, fx + 1, y + 1);
            }
            hits.add(new Hit(fx, y, fx + 18, y + 18, shift -> Werkstatt.setFuel(job, fuelName)));
            fx += 20;
        }
        CraftPlan.Fuel fuel = fuelFor(job, page);
        String name = fuel == null ? tr("fuel.none") : fuel.name();
        ctx.drawText(font, clip(font, name, x + w - fx - 34), fx + 2, y, OttonienTheme.INK, false);
        if (fuel != null) {
            ctx.drawText(font, tr("fuel.burn", WerkstattText.clock(fuel.burnSeconds())), fx + 2, y + 9,
                    OttonienTheme.INK_MUTED, false);
        }
        boolean auto = Werkstatt.config().werkstattAutoRefuel;
        int cx = x + w - 10;
        ctx.drawText(font, tr("auto"), cx - font.getWidth(tr("auto")) - 2, y + 5, OttonienTheme.INK_MUTED, false);
        checkbox(ctx, cx, y + 4, auto, shift -> {
            Werkstatt.config().werkstattAutoRefuel = !Werkstatt.config().werkstattAutoRefuel;
            Werkstatt.saveConfig();
        });
        return y + 18;
    }

    /** Dauerbetrieb switch right at the start button; without it a job crafts once per click. */
    private int autoRunRow(DrawContext ctx, TextRenderer font, int x, int y, int w) {
        boolean auto = Werkstatt.config().werkstattAutoRun;
        checkbox(ctx, x, y, auto, shift -> {
            Werkstatt.config().werkstattAutoRun = !Werkstatt.config().werkstattAutoRun;
            Werkstatt.saveConfig();
        });
        String label = auto && Werkstatt.config().werkstattLockView ? tr("autorun.locked") : tr("autorun");
        ctx.drawText(font, clip(font, label, w - 12), x + 12, y, OttonienTheme.INK, false);
        return y + 10;
    }

    private void drawSaved(DrawContext ctx, TextRenderer font, int x, int y, int w, int bottom) {
        List<WerkstattJob> saved = Werkstatt.savedFor(station);
        ctx.drawText(font, tr("saved"), x, y, OttonienTheme.INK, false);
        String n = Integer.toString(saved.size());
        ctx.drawText(font, n, x + w - font.getWidth(n), y, OttonienTheme.INK_MUTED, false);
        y += LINE + 2;
        for (WerkstattJob s : saved) {
            if (y + 16 > bottom) {
                break;
            }
            star(ctx, x, y + 4, GOLD);
            ctx.drawItem(Werkstatt.icon(s), x + 10, y);
            int bw = font.getWidth(tr("new")) + 6;
            ctx.drawText(font, clip(font, s.name, w - 30 - bw - 16), x + 28, y + 4, OttonienTheme.INK, false);
            button(ctx, font, x + w - bw - 14, y + 1, bw, 13, tr("new"), true, false, shift -> {
                Werkstatt.queueSaved(s);
                setTab(Tab.JOB);
            });
            button(ctx, font, x + w - 12, y + 1, 12, 13, "x", true, false, shift -> Werkstatt.forget(s));
            y += 16;
        }
        if (saved.isEmpty() && y + LINE <= bottom) {
            ctx.drawText(font, tr("saved.empty"), x, y, OttonienTheme.INK_MUTED, false);
        }
    }

    private void footer(DrawContext ctx, TextRenderer font, int x, int bottom, int w) {
        rule(ctx, x, bottom - LINE - 2, w);
        String text = Werkstatt.config().werkstattAutoRun
                ? tr("footer.rules", Werkstatt.stopSettings().activeCount()) : tr("footer.manual");
        ctx.drawText(font, clip(font, text, w), x, bottom - LINE + 1, OttonienTheme.INK_MUTED, false);
    }

    private void drawRunning(DrawContext ctx, TextRenderer font, int x, int y, int w, int bottom, JobRunner r) {
        WerkstattJob job = r.job();
        boolean finished = r.phase() == JobRunner.Phase.FINISHED;
        String head = r.phase() == JobRunner.Phase.STOPPED ? tr("run.stopped") : finished ? tr("run.finished")
                : r.phase() == JobRunner.Phase.PAUSED ? tr("run.paused") : tr("run.running");
        ctx.drawText(font, head, x, y, OttonienTheme.INK, false);
        String fire = WerkstattText.clock(Werkstatt.fire().secondsLeft(System.currentTimeMillis()));
        flame(ctx, x + w - font.getWidth(fire) - 9, y + 1);
        ctx.drawText(font, fire, x + w - font.getWidth(fire), y, OttonienTheme.INK, false);
        y += LINE + 3;
        ctx.drawItem(Werkstatt.icon(job), x, y);
        ctx.drawText(font, clip(font, job.name + " " + progressText(job), w - 20), x + 20, y,
                OttonienTheme.INK, false);
        String sub = finished ? summary(job)
                : tr("eta", WerkstattText.clock((long) Werkstatt.remaining(job) * Werkstatt.craftSeconds()));
        ctx.drawText(font, clip(font, sub, w - 20), x + 20, y + 9, OttonienTheme.INK_MUTED, false);
        y += 21;
        bar(ctx, x, y, w, 5, fraction(job), r.phase() == JobRunner.Phase.STOPPED ? RED : finished ? GREEN : GOLD);
        y += 9;
        if (finished) {
            y = wrap(ctx, font, summary(job), x, y, w, OttonienTheme.INK) + 3;
        } else if (r.phase() == JobRunner.Phase.STOPPED && r.stopReason() != null) {
            y = warn(ctx, font, tr("reason", Text.translatable(r.stopReason().translationKey()).getString()), x, y, w)
                    + 3;
        } else {
            ctx.drawText(font, tr("step"), x, y, OttonienTheme.INK, false);
            y += LINE;
            JobRunner.Step current = r.step();
            for (JobRunner.Step s : JobRunner.Step.values()) {
                boolean now = s == current;
                boolean past = s.ordinal() < current.ordinal();
                if (now) {
                    OttonienTheme.rect(ctx, x, y - 1, x + w, y + LINE - 1, OttonienTheme.PA_L);
                    OttonienTheme.rect(ctx, x, y - 1, x + 2, y + LINE - 1, TAB_ACTIVE);
                }
                String mark = past ? "✔" : now ? "▶" : "-";
                ctx.drawText(font, mark + " " + tr("step." + s.name().toLowerCase(java.util.Locale.ROOT)), x + 4, y,
                        past ? GREEN : now ? OttonienTheme.INK : OttonienTheme.INK_MUTED, false);
                y += LINE;
            }
            y += 2;
        }
        RecipeLore recipe = job.recipe();
        if (recipe.known() && y + LINE * 2 < bottom - 56) {
            Map<String, Integer> inv = Werkstatt.inventory(MinecraftClient.getInstance());
            ctx.drawText(font, tr("left"), x, y, OttonienTheme.INK, false);
            y += LINE;
            for (List<RecipeLore.Ingredient> group : recipe.groups()) {
                if (y + LINE > bottom - 56) {
                    break;
                }
                RecipeLore.Ingredient use = choose(group, inv, Werkstatt.remaining(job));
                int have = inv.getOrDefault(use.key(), 0);
                int need = use.count() * Werkstatt.remaining(job);
                ctx.drawText(font, clip(font, use.name(), w - 70), x, y, OttonienTheme.INK, false);
                String c = tr("left.count", have, need);
                ctx.drawText(font, c, x + w - font.getWidth(c), y, have >= need ? GREEN : RED, false);
                y += LINE;
            }
        }
        int by = bottom - 52;
        if (r.active()) {
            button(ctx, font, x, by, w, 16, tr("stop"), true, false, shift -> Werkstatt.stop(StopReason.USER),
                    0xFF8A2A30);
            button(ctx, font, x, by + 18, w, 16, r.phase() == JobRunner.Phase.PAUSED ? tr("resume") : tr("pause"),
                    true, false, shift -> Werkstatt.togglePause());
        } else if (finished) {
            button(ctx, font, x, by + 18, w, 16, tr("dismiss"), true, false, shift -> {
                Werkstatt.select(Werkstatt.queue().indexOf(job));
                dismissStopped();
            });
        } else {
            button(ctx, font, x, by, w, 16, tr("continue"), true, true, shift -> Werkstatt.start(job, false));
            button(ctx, font, x, by + 18, w, 16, tr("dismiss"), true, false, shift -> {
                Werkstatt.select(Werkstatt.queue().indexOf(job));
                dismissStopped();
            });
        }
        WerkstattJob next = nextAfter(job);
        if (next != null) {
            ctx.drawText(font, clip(font, tr("next", next.name, next.quantity), w), x, by + 38,
                    OttonienTheme.INK_MUTED, false);
        }
    }

    private static void dismissStopped() {
        Werkstatt.clearStopped();
    }

    private WerkstattJob nextAfter(WerkstattJob job) {
        List<WerkstattJob> q = Werkstatt.queueFor(station);
        for (int i = q.indexOf(job) + 1; i < q.size(); i++) {
            if (q.get(i).status == WerkstattJob.Status.WAITING) {
                return q.get(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Brennstoff

    private void drawFuelTab(DrawContext ctx, TextRenderer font, int x, int y, int w, int bottom) {
        WerkstattJob job = Werkstatt.selected(station);
        List<JobRunner.SlotView> fires = fireButtons(pageSlots());
        ctx.drawText(font, tr("fuel.title"), x, y, OttonienTheme.INK, false);
        y += LINE + 3;
        Map<String, Integer> inv = Werkstatt.inventory(MinecraftClient.getInstance());
        if (fires.isEmpty()) {
            y = wrap(ctx, font, tr("fuel.nobuttons"), x, y, w, OttonienTheme.INK_MUTED) + 4;
        }
        for (JobRunner.SlotView s : fires) {
            RecipeLore r = s.recipe();
            String fuelName = r.known() ? r.groups().get(0).get(0).name() : s.name();
            boolean chosen = job != null && WerkstattText.normalize(fuelName).equals(WerkstattText.normalize(job.fuel));
            if (chosen) {
                OttonienTheme.rect(ctx, x - 2, y - 1, x + w + 2, y + 19, OttonienTheme.PA_L);
            }
            OttonienTheme.tile(ctx, x, y, 18, 18, chosen);
            ItemStack icon = stackAt(s.slot());
            if (!icon.isEmpty()) {
                ctx.drawItem(icon, x + 1, y + 1);
            }
            ctx.drawText(font, clip(font, fuelName, w - 80), x + 22, y + 1, OttonienTheme.INK, false);
            String cost = r.known() ? r.groups().get(0).get(0).count() + " x " + fuelName : "?";
            ctx.drawText(font, clip(font, cost, w - 80), x + 22, y + 10, OttonienTheme.INK_MUTED, false);
            String burn = "+" + WerkstattText.clock(r.burnSeconds());
            ctx.drawText(font, burn, x + w - font.getWidth(burn), y + 1, OttonienTheme.INK, false);
            String have = tr("fuel.have", inv.getOrDefault(WerkstattText.normalize(fuelName), 0));
            ctx.drawText(font, have, x + w - font.getWidth(have), y + 10, OttonienTheme.INK_MUTED, false);
            if (job != null) {
                hits.add(new Hit(x, y, x + w, y + 18, shift -> Werkstatt.setFuel(job, fuelName)));
            }
            y += 21;
        }
        y += 2;
        boolean auto = Werkstatt.config().werkstattAutoRefuel;
        checkbox(ctx, x, y, auto, shift -> {
            Werkstatt.config().werkstattAutoRefuel = !Werkstatt.config().werkstattAutoRefuel;
            Werkstatt.saveConfig();
        });
        ctx.drawText(font, tr("fuel.auto"), x + 12, y, OttonienTheme.INK, false);
        y += LINE + 4;
        ctx.drawText(font, tr("fuel.estimate", WerkstattText.clock(Werkstatt.fire().secondsLeft(System.currentTimeMillis()))),
                x, y, OttonienTheme.INK_MUTED, false);
        y += LINE;
        ctx.drawText(font, tr("fuel.threshold", WerkstattText.clock(FireClock.REFUEL_BELOW_SECONDS)), x, y,
                OttonienTheme.INK_MUTED, false);
        y += LINE;
        wrap(ctx, font, tr("fuel.note"), x, y + 2, w, OttonienTheme.INK_MUTED);
        footer(ctx, font, x, bottom, w);
    }

    // ------------------------------------------------------------------ Warteschlange

    private void drawQueueTab(DrawContext ctx, TextRenderer font, int x, int y, int w, int bottom) {
        List<WerkstattJob> q = Werkstatt.queueFor(station);
        int rulesTop = bottom - 46;
        int index = 0;
        int listBottom = rulesTop - 62;
        JobRunner r = Werkstatt.runner();
        for (WerkstattJob job : q) {
            if (y + 26 > listBottom) {
                break;
            }
            index++;
            boolean current = r != null && r.active() && r.job() == job;
            if (current) {
                OttonienTheme.rect(ctx, x - 2, y - 1, x + w + 2, y + 25, OttonienTheme.PA_L);
                OttonienTheme.rect(ctx, x - 2, y - 1, x, y + 25, TAB_ACTIVE);
            }
            ctx.drawText(font, Integer.toString(index), x, y + 8, OttonienTheme.INK_MUTED, false);
            ctx.drawItem(Werkstatt.icon(job), x + 8, y + 4);
            ctx.drawText(font, clip(font, job.name + " " + job.quantity + "x", w - 70), x + 28, y + 2,
                    OttonienTheme.INK, false);
            String status = statusText(job, current);
            if (current) {
                bar(ctx, x + 28, y + 13, 40, 3, fraction(job), GOLD);
                ctx.drawText(font, clip(font, status, w - 116), x + 72, y + 11, OttonienTheme.INK_MUTED, false);
            } else {
                ctx.drawText(font, clip(font, status, w - 70), x + 28, y + 12,
                        job.status == WerkstattJob.Status.STOPPED ? RED : OttonienTheme.INK_MUTED, false);
            }
            int rx = x + w - 40;
            button(ctx, font, rx, y, 12, 11, "^", true, false, shift -> Werkstatt.move(job, -1));
            button(ctx, font, rx, y + 12, 12, 11, "v", true, false, shift -> Werkstatt.move(job, 1));
            button(ctx, font, rx + 14, y + 5, 12, 12, "x", !current, false, shift -> Werkstatt.remove(job));
            int sel = Werkstatt.queue().indexOf(job);
            hits.add(new Hit(x, y, rx - 2, y + 24, shift -> {
                Werkstatt.select(sel);
                setTab(Tab.JOB);
            }));
            y += 27;
        }
        if (q.isEmpty()) {
            y = wrap(ctx, font, tr("queue.empty"), x, y, w, OttonienTheme.INK_MUTED) + 4;
        }
        y = Math.max(y, listBottom - 6);
        int total = 0;
        for (WerkstattJob job : q) {
            if (job.status != WerkstattJob.Status.DONE) {
                total += Werkstatt.remaining(job);
            }
        }
        ctx.drawText(font, tr("queue.total", total, WerkstattText.clock((long) total * Werkstatt.craftSeconds())),
                x, y + 4, OttonienTheme.INK_MUTED, false);
        y += 16;
        boolean next = Werkstatt.config().werkstattNextOnError;
        checkbox(ctx, x, y, next, shift -> {
            Werkstatt.config().werkstattNextOnError = !Werkstatt.config().werkstattNextOnError;
            Werkstatt.saveConfig();
        });
        ctx.drawText(font, tr("queue.next"), x + 12, y, OttonienTheme.INK, false);
        y += 14;
        boolean auto = Werkstatt.config().werkstattAutoRun;
        int half = (w - 4) / 2;
        button(ctx, font, x, y, half, 16, tr("queue.start"), auto && !Werkstatt.running() && total > 0, true,
                shift -> Werkstatt.startAll(station));
        button(ctx, font, x + half + 4, y, w - half - 4, 16, tr("stop"), Werkstatt.running(), false,
                shift -> Werkstatt.stop(StopReason.USER), 0xFF8A2A30);
        drawStopRules(ctx, font, x, rulesTop, w);
    }

    private String statusText(WerkstattJob job, boolean current) {
        if (current) {
            return job.failed > 0 ? tr("status.running.failed", job.done, job.quantity, job.failed)
                    : tr("status.running", job.done, job.quantity);
        }
        return switch (job.status) {
            case DONE -> tr("status.done");
            case STOPPED -> job.stopReason == null ? tr("status.stopped")
                    : Text.translatable(job.stopReason.translationKey()).getString();
            case PAUSED -> tr("status.paused");
            default -> clip(MinecraftClient.getInstance().textRenderer,
                    (job.path.isEmpty() ? tr("status.startpage") : job.path.get(job.path.size() - 1).name)
                            + ", " + tr("status.waiting"), 200);
        };
    }

    private void drawStopRules(DrawContext ctx, TextRenderer font, int x, int y, int w) {
        rule(ctx, x, y - 3, w);
        ctx.drawText(font, tr("rules.title"), x, y, OttonienTheme.INK, false);
        String fixed = tr("rules.fixed");
        ctx.drawText(font, fixed, x + w - font.getWidth(fixed), y, OttonienTheme.INK_MUTED, false);
        var c = Werkstatt.config();
        int half = w / 2;
        y += LINE + 1;
        ruleBox(ctx, font, x, y, "rules.move", c.werkstattStopOnMove, () -> c.werkstattStopOnMove = !c.werkstattStopOnMove);
        ruleBox(ctx, font, x + half, y, "rules.damage", c.werkstattStopOnDamage,
                () -> c.werkstattStopOnDamage = !c.werkstattStopOnDamage);
        y += LINE + 1;
        ruleBox(ctx, font, x, y, "rules.gui", c.werkstattStopOnOtherGui,
                () -> c.werkstattStopOnOtherGui = !c.werkstattStopOnOtherGui);
        ruleBox(ctx, font, x + half, y, "rules.full", c.werkstattStopOnFull,
                () -> c.werkstattStopOnFull = !c.werkstattStopOnFull);
        y += LINE + 1;
        ruleBox(ctx, font, x, y, "rules.focus", c.werkstattStopOnUnfocus,
                () -> c.werkstattStopOnUnfocus = !c.werkstattStopOnUnfocus);
        ctx.drawText(font, tr("rules.command"), x + half + 12, y, OttonienTheme.INK_MUTED, false);
    }

    private void ruleBox(DrawContext ctx, TextRenderer font, int x, int y, String key, boolean value, Runnable flip) {
        checkbox(ctx, x, y, value, shift -> {
            flip.run();
            Werkstatt.saveConfig();
        });
        ctx.drawText(font, tr(key), x + 12, y, OttonienTheme.INK, false);
    }

    // ------------------------------------------------------------------ helpers

    private List<JobRunner.SlotView> fireButtons(List<JobRunner.SlotView> page) {
        List<JobRunner.SlotView> out = new ArrayList<>();
        for (JobRunner.SlotView s : page) {
            if (s.recipe().isFire()) {
                out.add(s);
            }
        }
        return out;
    }

    /** The job's fuel as a fire button of this page, or the first one; null without fire buttons. */
    private CraftPlan.Fuel fuelFor(WerkstattJob job, List<JobRunner.SlotView> page) {
        JobRunner.SlotView s = JobRunner.fireButton(new JobRunner.View(JobRunner.ScreenKind.STATION, 0, page,
                Map.of(), true), job.fuel);
        if (s == null) {
            s = JobRunner.fireButton(new JobRunner.View(JobRunner.ScreenKind.STATION, 0, page, Map.of(), true), "");
        }
        if (s == null) {
            return null;
        }
        RecipeLore r = s.recipe();
        String name = r.known() ? r.groups().get(0).get(0).name() : s.name();
        return new CraftPlan.Fuel(name, r.burnSeconds(), r.groups());
    }

    private static RecipeLore.Ingredient choose(List<RecipeLore.Ingredient> group, Map<String, Integer> inv, int n) {
        for (RecipeLore.Ingredient i : group) {
            if (inv.getOrDefault(i.key(), 0) >= i.count() * Math.max(1, n)) {
                return i;
            }
        }
        return group.get(0);
    }

    private ItemStack stackAt(int slot) {
        var handler = screen.getScreenHandler();
        if (slot >= 0 && slot < handler.slots.size()) {
            return handler.getSlot(slot).getStack();
        }
        return ItemStack.EMPTY;
    }

    private boolean inside(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void button(DrawContext ctx, TextRenderer font, int x, int y, int w, int h, String label, boolean enabled,
                        boolean primary, java.util.function.Consumer<Boolean> action) {
        button(ctx, font, x, y, w, h, label, enabled, primary, action, 0);
    }

    private void button(DrawContext ctx, TextRenderer font, int x, int y, int w, int h, String label, boolean enabled,
                        boolean primary, java.util.function.Consumer<Boolean> action, int tint) {
        boolean hover = enabled && inside(x, y, w, h);
        OttonienTheme.slateButton(ctx, x, y, w, h, hover);
        if (tint != 0 && enabled) {
            OttonienTheme.rect(ctx, x + 1, y + 2, x + w - 1, y + h - 2, hover ? OttonienTheme.shade(tint, 1.15) : tint);
        }
        if (primary && enabled && !hover) {
            frame(ctx, x, y, w, h, 0xFFB58D36);
        }
        int color = !enabled ? 0xFF8A8A90 : primary ? GOLD : OttonienTheme.LABEL;
        String shown = clip(font, label, w - 4);
        ctx.drawText(font, shown, x + (w - font.getWidth(shown) + 1) / 2, y + (h - 8) / 2, color, false);
        if (enabled) {
            hits.add(new Hit(x, y, x + w, y + h, action));
        }
    }

    private void checkbox(DrawContext ctx, int x, int y, boolean value, java.util.function.Consumer<Boolean> action) {
        OttonienTheme.rect(ctx, x, y, x + 9, y + 9, 0xFF000000);
        OttonienTheme.rect(ctx, x + 1, y + 1, x + 8, y + 8, OttonienTheme.FIELD_DARK);
        if (value) {
            int c = OttonienTheme.LABEL;
            OttonienTheme.rect(ctx, x + 2, y + 4, x + 3, y + 6, c);
            OttonienTheme.rect(ctx, x + 3, y + 5, x + 4, y + 7, c);
            OttonienTheme.rect(ctx, x + 4, y + 4, x + 5, y + 6, c);
            OttonienTheme.rect(ctx, x + 5, y + 3, x + 6, y + 5, c);
            OttonienTheme.rect(ctx, x + 6, y + 2, x + 7, y + 4, c);
        }
        hits.add(new Hit(x - 1, y - 1, x + 10, y + 10, action));
    }

    private static void frame(DrawContext ctx, int x, int y, int w, int h, int color) {
        OttonienTheme.rect(ctx, x, y, x + w, y + 1, color);
        OttonienTheme.rect(ctx, x, y + h - 1, x + w, y + h, color);
        OttonienTheme.rect(ctx, x, y, x + 1, y + h, color);
        OttonienTheme.rect(ctx, x + w - 1, y, x + w, y + h, color);
    }

    private static void badge(DrawContext ctx, int x, int y, int w, int h, int color) {
        OttonienTheme.rect(ctx, x, y, x + w, y + h, 0xFF000000);
        OttonienTheme.rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, color);
    }


    static void flame(DrawContext ctx, int x, int y) {
        OttonienTheme.rect(ctx, x + 2, y, x + 3, y + 1, 0xFFE2A650);
        OttonienTheme.rect(ctx, x + 1, y + 1, x + 4, y + 3, 0xFFE07A30);
        OttonienTheme.rect(ctx, x, y + 3, x + 5, y + 6, 0xFFC4542A);
        OttonienTheme.rect(ctx, x + 2, y + 3, x + 3, y + 5, 0xFFF2C14E);
    }

    private static void star(DrawContext ctx, int x, int y, int c) {
        OttonienTheme.rect(ctx, x + 3, y, x + 4, y + 2, c);
        OttonienTheme.rect(ctx, x, y + 2, x + 7, y + 4, c);
        OttonienTheme.rect(ctx, x + 1, y + 4, x + 6, y + 5, c);
        OttonienTheme.rect(ctx, x + 1, y + 5, x + 3, y + 7, c);
        OttonienTheme.rect(ctx, x + 4, y + 5, x + 6, y + 7, c);
    }

    static void bar(DrawContext ctx, int x, int y, int w, int h, float fraction, int color) {
        OttonienTheme.rect(ctx, x, y, x + w, y + h, 0xFF5A4532);
        int fill = Math.round(Math.max(0, Math.min(1, fraction)) * (w - 2));
        OttonienTheme.rect(ctx, x + 1, y + 1, x + 1 + fill, y + h - 1, color);
    }

    private static int rule(DrawContext ctx, int x, int y, int w) {
        OttonienTheme.rect(ctx, x, y, x + w, y + 1, OttonienTheme.PA_D);
        return y + 2;
    }

    private static int warn(DrawContext ctx, TextRenderer font, String text, int x, int y, int w) {
        List<String> lines = lines(font, text, w - 8);
        int h = lines.size() * LINE + 4;
        OttonienTheme.rect(ctx, x, y, x + w, y + h, WARN_BG);
        OttonienTheme.rect(ctx, x, y, x + 2, y + h, WARN_EDGE);
        int ly = y + 3;
        for (String line : lines) {
            ctx.drawText(font, line, x + 5, ly, WARN_TEXT, false);
            ly += LINE;
        }
        return y + h;
    }

    private static int wrap(DrawContext ctx, TextRenderer font, String text, int x, int y, int w, int color) {
        for (String line : lines(font, text, w)) {
            ctx.drawText(font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    private static List<String> lines(TextRenderer font, String text, int w) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.getWidth(candidate) <= w || line.length() == 0) {
                line = new StringBuilder(candidate);
            } else {
                out.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    private static String clip(TextRenderer font, String text, int w) {
        if (text == null) {
            return "";
        }
        if (font.getWidth(text) <= w) {
            return text;
        }
        String dots = "…";
        int end = text.length();
        while (end > 0 && font.getWidth(text.substring(0, end) + dots) > w) {
            end--;
        }
        return text.substring(0, end) + dots;
    }

    /** "23/27", or "23/27 · 4 fehlgeschlagen" once a pass failed. */
    static String progressText(WerkstattJob job) {
        String done = job.done + "/" + job.quantity;
        return job.failed > 0 ? done + " · " + tr("failed", job.failed) : done;
    }

    /** "23 von 27 hergestellt, 4 fehlgeschlagen". */
    static String summary(WerkstattJob job) {
        return job.failed > 0 ? tr("summary", job.done, job.quantity, job.failed)
                : tr("summary.ok", job.done, job.quantity);
    }

    /** Progress bar share: passes made (successful or failed) of the quantity. */
    static float fraction(WerkstattJob job) {
        if (job.quantity <= 0) {
            return 0;
        }
        int made = job.quantity - Werkstatt.remaining(job);
        return Math.min(1f, (float) made / job.quantity);
    }

    static String tr(String key, Object... args) {
        return Text.translatable("ottoextra.werkstatt." + key, args).getString();
    }
}
