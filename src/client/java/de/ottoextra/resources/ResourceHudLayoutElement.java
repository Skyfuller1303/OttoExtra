package de.ottoextra.resources;

import de.ottoextra.ui.hudlayout.HudLayoutElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The timer panel as a box in OttoExtra's shared "HUD anordnen" editor. The placement stays in
 * {@link ResourcesConfig}: corner, pixel offset from it and scale. While the game HUD draws the
 * panel and reports it to HudFlow, the editor shows the live panel; otherwise it draws a preview
 * with sample entries.
 */
final class ResourceHudLayoutElement implements HudLayoutElement {
    @Override
    public String id() {
        return ResourceHud.FLOW_ID;
    }

    @Override
    public Text label() {
        return Text.translatable("ottoextra.resources.hud.layout");
    }

    @Override
    public String flowId() {
        return ResourceHud.FLOW_ID;
    }

    @Override
    public Bounds bounds(int screenW, int screenH) {
        ResourceHud hud = ResourceHud.instance();
        if (hud == null) return null;
        int[] box = hud.wantedBounds(screenW, screenH);
        return new Bounds(box[0], box[1], box[2], box[3]);
    }

    @Override
    public boolean scalable() {
        return true;
    }

    @Override
    public float scale() {
        return ResourceHudPlacement.clampScale(ResourcesConfig.active().hudScale);
    }

    @Override
    public void setScale(float scale) {
        ResourcesConfig.active().hudScale = ResourceHudPlacement.clampScale(scale);
    }

    @Override
    public void place(int x, int y, int w, int h, int screenW, int screenH) {
        ResourceHudPlacement.Placement placement = ResourceHudPlacement.fromDrag(x, y, w, h, screenW, screenH);
        ResourcesConfig config = ResourcesConfig.active();
        config.hudCorner = placement.corner().name();
        config.hudOffsetX = placement.offsetX();
        config.hudOffsetY = placement.offsetY();
    }

    @Override
    public void reset() {
        ResourcesConfig config = ResourcesConfig.active();
        config.hudCorner = ResourceHudPlacement.Corner.TOP_RIGHT.name();
        config.hudOffsetX = 0;
        config.hudOffsetY = ResourceHudPlacement.DEFAULT_OFFSET_Y;
        config.hudScale = 1.0f;
    }

    @Override
    public void save() {
        ResourcesConfig.active().save();
    }

    @Override
    public boolean drawPreview(DrawContext context, TextRenderer font, int x, int y) {
        ResourceHud hud = ResourceHud.instance();
        if (hud == null) return false;
        hud.drawPreview(context, font, x, y);
        return true;
    }
}
