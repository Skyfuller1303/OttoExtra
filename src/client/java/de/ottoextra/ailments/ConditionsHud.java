package de.ottoextra.ailments;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.ui.hudlayout.ActivityHudAnchors;
import de.ottoextra.ui.hudlayout.HudFlow;
import de.ottoextra.ui.hudlayout.HudLayoutElement;
import de.ottoextra.ui.theme.OttonienTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Small list of the own conditions, in the style of the activity HUD (no text shadow). Off by default. */
public final class ConditionsHud {
    public static final String FLOW_ID = "ottoextra:conditions";
    private static final int BG = 0xE81B1B22;
    private static final int TITLE = 0xFFE8C77A;
    private static final int LEIDEN = 0xFFE07070;
    private static final int STATUS = 0xFF8CC8E8;
    private static final int MUTED = 0xFF9A968E;
    private static final int MAX_LINES = 8;

    private ConditionsHud() {
    }

    private record Line(String text, int color) {
    }

    private static List<Line> lines(List<Condition> list) {
        List<Line> out = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            out.add(new Line(Text.translatable("ottoextra.conditions.hud.none").getString(), MUTED));
            return out;
        }
        for (Condition c : list) {
            if (out.size() >= MAX_LINES) {
                out.add(new Line("+" + (list.size() - MAX_LINES), MUTED));
                break;
            }
            out.add(new Line(c.name(), c.isLeiden() ? LEIDEN : STATUS));
        }
        return out;
    }

    private static int[] size(TextRenderer font, List<Line> lines, String title) {
        int w = font.getWidth(title);
        for (Line l : lines) w = Math.max(w, font.getWidth(l.text()));
        return new int[]{w + 10, 16 + lines.size() * 10};
    }

    static void render(DrawContext ctx, OttoExtraConfig.Conditions c) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!c.hud || client.player == null || client.options.hudHidden || client.currentScreen != null
                || Conditions.own() == null) {
            return;
        }
        List<Line> lines = lines(Conditions.own());
        String title = Text.translatable("ottoextra.conditions.hud.title").getString();
        int[] s = size(client.textRenderer, lines, title);
        int[] pos = ActivityHudAnchors.position(c.hudCorner, c.hudOffsetX, c.hudOffsetY,
                ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), s[0], s[1]);
        int x = pos[0];
        int y = pos[1];
        try {
            int[] flowed = HudFlow.place(FLOW_ID, x, y, s[0], s[1], c.hudCorner.startsWith("BOTTOM"),
                    HudFlow.PRIORITY_ACTIVITY_HUD + 20);
            if (flowed != null) {
                x = flowed[0];
                y = flowed[1];
            }
        } catch (Throwable ignored) {
            // draw at the wanted spot
        }
        draw(ctx, client.textRenderer, x, y, lines, title, s);
    }

    private static void draw(DrawContext ctx, TextRenderer font, int x, int y, List<Line> lines, String title,
                             int[] s) {
        OttonienTheme.rect(ctx, x, y, x + s[0], y + s[1], 0xFF000000);
        OttonienTheme.rect(ctx, x + 1, y + 1, x + s[0] - 1, y + s[1] - 1, BG);
        ctx.drawText(font, title, x + 5, y + 4, TITLE, false);
        int ty = y + 15;
        for (Line l : lines) {
            ctx.drawText(font, l.text(), x + 5, ty, l.color(), false);
            ty += 10;
        }
    }

    /** Layout editor entry. */
    public static final class Element implements HudLayoutElement {
        private final OttoExtraConfig config;

        public Element(OttoExtraConfig config) {
            this.config = config;
        }

        private OttoExtraConfig.Conditions c() {
            if (config.conditions == null) config.conditions = new OttoExtraConfig.Conditions();
            return config.conditions;
        }

        private List<Line> preview() {
            List<Condition> own = Conditions.own();
            return lines(own != null && !own.isEmpty() ? own
                    : List.of(new Condition("Verdreckt", List.of("Gestank"), Condition.KIND_LEIDEN)));
        }

        @Override
        public String id() {
            return FLOW_ID;
        }

        @Override
        public Text label() {
            return Text.translatable("ottoextra.hudlayout.element.conditions");
        }

        @Override
        public Bounds bounds(int screenW, int screenH) {
            if (!c().hud) return null;
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            int[] s = size(font, preview(), Text.translatable("ottoextra.conditions.hud.title").getString());
            int[] pos = ActivityHudAnchors.position(c().hudCorner, c().hudOffsetX, c().hudOffsetY,
                    screenW, screenH, s[0], s[1]);
            return new Bounds(pos[0], pos[1], s[0], s[1]);
        }

        @Override
        public void place(int x, int y, int w, int h, int screenW, int screenH) {
            ActivityHudAnchors.Placement p = ActivityHudAnchors.fromDrag(x, y, w, h, screenW, screenH);
            c().hudCorner = p.anchor();
            c().hudOffsetX = p.offsetX();
            c().hudOffsetY = p.offsetY();
        }

        @Override
        public void reset() {
            c().hudCorner = "TOP_LEFT";
            c().hudOffsetX = 4;
            c().hudOffsetY = 60;
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
            List<Line> lines = preview();
            String title = Text.translatable("ottoextra.conditions.hud.title").getString();
            draw(context, font, x, y, lines, title, size(font, lines, title));
            return true;
        }
    }
}
