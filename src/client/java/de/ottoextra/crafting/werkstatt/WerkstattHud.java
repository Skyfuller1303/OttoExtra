package de.ottoextra.crafting.werkstatt;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionActivityHud;
import de.ottoextra.ui.hudlayout.ActivityHudAnchors;
import de.ottoextra.ui.hudlayout.HudFlow;
import de.ottoextra.ui.hudlayout.HudLayoutElement;
import de.ottoextra.ui.hudlayout.TopCenterStack;
import de.ottoextra.ui.theme.OttonienTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Slim bar of the running, paused, just stopped or just finished job while the station is closed (design
 * P1): accent stripe, item icon, one text line, fire time on the right and a thin progress bar. By default
 * it stands at the top centre below the boss bars ({@link TopCenterStack}); it takes part in
 * {@link HudFlow} and in the HUD layout editor. No text shadows.
 */
public final class WerkstattHud {
    public static final String FLOW_ID = "ottoextra:werkstatt";
    // Palette of the activity HUD and the approved HUD toasts.
    private static final int OUTL = 0xFF141418;
    private static final int D2 = 0xFF2E2D35;
    private static final int NAME = 0xFFF0EBE0;
    private static final int OCHRE = 0xFFD6C194;
    private static final int SUB = 0xFFA9A6B0;
    private static final int LABEL = 0xFF8F8C96;
    /** Colour ramps: dark, base, light. */
    private static final int[] GOLD = {0xFF96752C, 0xFFCD9F3D, 0xFFDBB451};
    private static final int[] GREY = {0xFF3B3A45, 0xFF514F59, 0xFFA9A6B0};
    private static final int[] LOW = {0xFF6E2226, 0xFFB24147, 0xFFC4545A};
    private static final int[] READY = {0xFF2F5426, 0xFF5E9A3E, 0xFF86C25A};
    private static final int[] FIRE = {0xFF7A3A1E, 0xFFC0602E, 0xFFE08A4A};
    private static final String[] FLAME = {"...o...", "..oo...", "..ooo..", ".oooo..", ".ooiio.", "ooiiioo",
            "oiiiiio", ".ooooo."};
    private static final int[][] CHECK = {{0, 3}, {1, 4}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {6, 1}};

    enum State { RUNNING, PAUSED, STOPPED, FINISHED }

    private WerkstattHud() {
    }

    public static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null
                || !Werkstatt.enabled() || !Werkstatt.config().werkstattHud) {
            TopCenterStack.release(FLOW_ID);
            return;
        }
        JobRunner r = Werkstatt.runner();
        if (r == null || !(r.active() || Werkstatt.recentlyStopped() || Werkstatt.recentlyFinished())) {
            TopCenterStack.release(FLOW_ID);
            return;
        }
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        OttoExtraConfig.Crafting c = Werkstatt.config();
        Bar bar = bar(client.textRenderer, r.job(), r.phase(), r.stopReason(), Werkstatt.viewLocked(), sw);
        int w = bar.layout().width();
        int h = WerkstattBarLayout.H;
        int columnY;
        if (WerkstattBarLayout.TOP_CENTER.equals(c.werkstattHudCorner)) {
            columnY = TopCenterStack.claim(FLOW_ID, TopCenterStack.ORDER_WERKSTATT, h);
        } else {
            TopCenterStack.release(FLOW_ID);
            columnY = 0;
        }
        int[] pos = WerkstattBarLayout.position(c.werkstattHudCorner, c.werkstattHudOffsetX, c.werkstattHudOffsetY,
                sw, sh, w, h, columnY);
        int x = pos[0];
        int y = pos[1];
        try {
            int[] flowed = HudFlow.place(FLOW_ID, x, y, w, h, c.werkstattHudCorner.startsWith("BOTTOM"),
                    HudFlow.PRIORITY_ACTIVITY_HUD + 10);
            if (flowed != null) {
                x = flowed[0];
                y = flowed[1];
            }
        } catch (Throwable ignored) {
            // Draw at the wanted spot.
        }
        draw(ctx, client.textRenderer, x, y, bar);
    }

    /** All texts of the bar and its measured layout. */
    record Bar(WerkstattJob job, State state, String text, String clock, String key, boolean locked,
               WerkstattBarLayout.Layout layout) {
    }

    static Bar bar(TextRenderer font, WerkstattJob job, JobRunner.Phase phase, StopReason reason, boolean locked,
                   int screenW) {
        State state = phase == JobRunner.Phase.STOPPED ? State.STOPPED
                : phase == JobRunner.Phase.PAUSED ? State.PAUSED
                : phase == JobRunner.Phase.FINISHED && job != null ? State.FINISHED
                : State.RUNNING;
        String text = text(job, state, reason);
        String clock = WerkstattText.clock(Werkstatt.fire().secondsLeft(System.currentTimeMillis()));
        // Only a bound stop key is shown; the panel has the stop button, so no command text here.
        String key = state == State.RUNNING || state == State.PAUSED ? WerkstattModule.stopKeyLabel() : "";
        boolean lock = locked && state == State.RUNNING;
        int right = WerkstattBarLayout.right(state == State.FINISHED, font.getWidth(clock), lock,
                key.isEmpty() ? 0 : font.getWidth(key));
        WerkstattBarLayout.Layout layout = WerkstattBarLayout.compute(font.getWidth(text), right, screenW);
        return new Bar(job, state, text, clock, key, lock, layout);
    }

    /** "Salz 23/27 · 4 fehlgeschlagen", "Salz 23/27 · pausiert", "Salz gestoppt: weggegangen" or the summary. */
    static String text(WerkstattJob job, State state, StopReason reason) {
        String name = job == null ? "" : job.name;
        return switch (state) {
            case FINISHED -> WerkstattPanel.summary(job);
            case STOPPED -> WerkstattPanel.tr("hud.bar.stopped", name, reason == null ? ""
                    : Text.translatable(reason.translationKey()).getString());
            case PAUSED -> WerkstattPanel.tr("hud.bar.paused", name,
                    job == null ? "" : job.done + "/" + job.quantity);
            case RUNNING -> WerkstattPanel.tr("hud.bar.running", name,
                    job == null ? "" : WerkstattPanel.progressText(job));
        };
    }

    static void draw(DrawContext ctx, TextRenderer font, int x, int y, Bar bar) {
        WerkstattBarLayout.Layout l = bar.layout();
        int w = l.width();
        int h = WerkstattBarLayout.H;
        int[] ramp = switch (bar.state()) {
            case RUNNING -> GOLD;
            case PAUSED -> GREY;
            case STOPPED -> LOW;
            case FINISHED -> READY;
        };
        int textColor = switch (bar.state()) {
            case RUNNING -> NAME;
            case PAUSED -> SUB;
            case STOPPED -> LOW[2];
            case FINISHED -> READY[2];
        };
        RegionActivityHud.drawPanel(ctx, x, y, w, h, 1.0f);
        OttonienTheme.rect(ctx, x + 2, y + 2, x + 5, y + h - 2, ramp[1]);
        OttonienTheme.rect(ctx, x + 2, y + 2, x + 5, y + 3, ramp[2]);
        ItemStack icon = bar.job() == null ? new ItemStack(Items.ANVIL) : Werkstatt.icon(bar.job());
        ctx.drawItem(icon, x + WerkstattBarLayout.ICON_X, y + WerkstattBarLayout.ICON_Y);
        int tx = x + WerkstattBarLayout.TEXT_X;
        int ty = y + WerkstattBarLayout.TEXT_Y;
        ctx.drawText(font, WerkstattBarLayout.clip(font::getWidth, bar.text(), l.textRoom()), tx, ty, textColor, false);
        int rx = x + w - WerkstattBarLayout.RIGHT_PAD;
        if (bar.state() == State.FINISHED) {
            for (int[] p : CHECK) {
                int px = rx - 7 + p[0];
                int py = ty + p[1];
                OttonienTheme.rect(ctx, px, py, px + 1, py + 2, READY[2]);
            }
        } else {
            int clockW = font.getWidth(bar.clock());
            int fx = rx - clockW - WerkstattBarLayout.FLAME_GAP - WerkstattBarLayout.FLAME_W;
            flame(ctx, fx, ty, bar.state() == State.RUNNING ? FIRE : GREY);
            ctx.drawText(font, bar.clock(), rx - clockW, ty, bar.state() == State.STOPPED ? LABEL : SUB, false);
            int left = fx;
            if (bar.locked()) {
                left -= WerkstattBarLayout.LOCK_W;
                lock(ctx, left, ty, OCHRE);
            }
            if (!bar.key().isEmpty()) {
                left -= WerkstattBarLayout.KEY_GAP + font.getWidth(bar.key());
                ctx.drawText(font, bar.key(), left, ty, LABEL, false);
            }
        }
        int bx = tx;
        int bw = l.barWidth();
        int by = y + WerkstattBarLayout.BAR_Y;
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + WerkstattBarLayout.BAR_H, OUTL);
        OttonienTheme.rect(ctx, bx + 1, by + 1, bx + bw - 1, by + WerkstattBarLayout.BAR_H - 1, D2);
        float fraction = bar.state() == State.FINISHED ? 1f : bar.job() == null ? 0f : WerkstattPanel.fraction(bar.job());
        int fill = WerkstattBarLayout.filled(bw - 2, fraction);
        if (fill > 0) {
            OttonienTheme.rect(ctx, bx + 1, by + 1, bx + 1 + fill, by + WerkstattBarLayout.BAR_H - 1, ramp[2]);
        }
    }

    private static void flame(DrawContext ctx, int x, int y, int[] ramp) {
        for (int j = 0; j < FLAME.length; j++) {
            String row = FLAME[j];
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                if (ch != '.') {
                    OttonienTheme.rect(ctx, x + i, y + j, x + i + 1, y + j + 1, ch == 'o' ? ramp[1] : ramp[2]);
                }
            }
        }
    }

    private static void lock(DrawContext ctx, int x, int y, int color) {
        OttonienTheme.rect(ctx, x + 1, y, x + 5, y + 1, color);
        OttonienTheme.rect(ctx, x + 1, y, x + 2, y + 3, color);
        OttonienTheme.rect(ctx, x + 4, y, x + 5, y + 3, color);
        OttonienTheme.rect(ctx, x, y + 3, x + 6, y + 8, color);
    }

    /** Layout editor entry of the bar. */
    public static final class Element implements HudLayoutElement {
        private final OttoExtraConfig config;

        public Element(OttoExtraConfig config) {
            this.config = config;
        }

        @Override
        public String id() {
            return FLOW_ID;
        }

        @Override
        public Text label() {
            return Text.translatable("ottoextra.hudlayout.element.werkstatt");
        }

        @Override
        public Bounds bounds(int screenW, int screenH) {
            if (config.crafting == null || !config.crafting.werkstattEnabled || !config.crafting.werkstattHud) {
                return null;
            }
            OttoExtraConfig.Crafting c = config.crafting;
            int w = previewBar(MinecraftClient.getInstance().textRenderer, screenW).layout().width();
            int[] pos = WerkstattBarLayout.position(c.werkstattHudCorner, c.werkstattHudOffsetX,
                    c.werkstattHudOffsetY, screenW, screenH, w, WerkstattBarLayout.H, TopCenterStack.top());
            return new Bounds(pos[0], pos[1], w, WerkstattBarLayout.H);
        }

        @Override
        public void place(int x, int y, int w, int h, int screenW, int screenH) {
            ActivityHudAnchors.Placement p = WerkstattBarLayout.fromDrag(x, y, w, h, screenW, screenH,
                    TopCenterStack.top());
            config.crafting.werkstattHudCorner = p.anchor();
            config.crafting.werkstattHudOffsetX = p.offsetX();
            config.crafting.werkstattHudOffsetY = p.offsetY();
        }

        @Override
        public void reset() {
            config.crafting.werkstattHudCorner = WerkstattBarLayout.TOP_CENTER;
            config.crafting.werkstattHudOffsetX = 0;
            config.crafting.werkstattHudOffsetY = 0;
        }

        @Override
        public String flowId() {
            return FLOW_ID;
        }

        @Override
        public void save() {
            config.save();
        }

        @Override
        public boolean drawPreview(DrawContext context, TextRenderer font, int x, int y) {
            draw(context, font, x, y, previewBar(font, context.getScaledWindowWidth()));
            return true;
        }

        private static Bar previewBar(TextRenderer font, int screenW) {
            JobRunner r = Werkstatt.runner();
            if (r != null) {
                return bar(font, r.job(), r.phase(), r.stopReason(), Werkstatt.viewLocked(), screenW);
            }
            return bar(font, sample(), JobRunner.Phase.WAITING_RESULT, null,
                    Werkstatt.config().werkstattLockView && Werkstatt.config().werkstattAutoRun, screenW);
        }

        private static WerkstattJob sample() {
            WerkstattJob j = new WerkstattJob();
            j.name = "Eisenbarren";
            j.itemId = "minecraft:iron_ingot";
            j.quantity = 20;
            j.done = 6;
            return j;
        }
    }
}
