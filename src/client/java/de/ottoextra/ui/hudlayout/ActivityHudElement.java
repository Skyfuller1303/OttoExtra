package de.ottoextra.ui.hudlayout;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionActivityHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** The region activity HUD; placement lives in {@code OttoExtraConfig.regions.activityHud*}. */
final class ActivityHudElement implements HudLayoutElement {
    private static final int SAMPLE_W = 120;
    private static final int SAMPLE_H = 15 + 4 * 16 + 14;

    private final OttoExtraConfig config;

    ActivityHudElement(OttoExtraConfig config) {
        this.config = config;
    }

    @Override
    public String id() {
        return "ottoextra:activity";
    }

    @Override
    public Text label() {
        return Text.translatable("ottoextra.hudlayout.element.activity");
    }

    private int[] baseSize() {
        int[] last = RegionActivityHud.lastPanelSize();
        return last != null ? last : new int[]{SAMPLE_W, SAMPLE_H};
    }

    @Override
    public String flowId() {
        return "ottoextra:activity";
    }

    @Override
    public Bounds bounds(int screenW, int screenH) {
        int[] base = baseSize();
        float scale = scale();
        int w = HudSnap.scaled(base[0], scale);
        int h = HudSnap.scaled(base[1], scale);
        OttoExtraConfig.Regions r = config.regions;
        int[] pos = ActivityHudAnchors.position(r.activityHudPosition, r.activityHudOffsetX, r.activityHudOffsetY,
                screenW, screenH, w, h);
        return new Bounds(pos[0], pos[1], w, h);
    }

    @Override
    public boolean scalable() {
        return true;
    }

    @Override
    public float scale() {
        return HudSnap.clampScale(config.regions.activityHudScale);
    }

    @Override
    public void setScale(float scale) {
        config.regions.activityHudScale = HudSnap.clampScale(scale);
    }

    @Override
    public void place(int x, int y, int w, int h, int screenW, int screenH) {
        ActivityHudAnchors.Placement p = ActivityHudAnchors.fromDrag(x, y, w, h, screenW, screenH);
        config.regions.activityHudPosition = p.anchor();
        config.regions.activityHudOffsetX = p.offsetX();
        config.regions.activityHudOffsetY = p.offsetY();
    }

    @Override
    public void reset() {
        config.regions.activityHudPosition = "MIDDLE_LEFT";
        config.regions.activityHudOffsetX = 0;
        config.regions.activityHudOffsetY = 0;
        config.regions.activityHudScale = 1.0f;
    }

    @Override
    public void save() {
        config.save();
    }

    @Override
    public boolean drawPreview(DrawContext context, TextRenderer font, int x, int y) {
        int[] base = baseSize();
        float scale = scale();
        var m = context.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        RegionActivityHud.drawPanel(context, 0, 0, base[0], base[1], 1.0f);
        context.drawTextWithShadow(font, Text.translatable("ottoextra.activityHud.title"), 4, 3, 0xFFD6C194);
        m.popMatrix();
        return true;
    }
}
