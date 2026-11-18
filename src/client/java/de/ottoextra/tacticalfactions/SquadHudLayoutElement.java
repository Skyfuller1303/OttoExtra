package de.ottoextra.tacticalfactions;

import de.ottoextra.ui.hudlayout.HudLayoutElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

/**
 * The squad HUD as a box in OttoExtra's shared HUD layout editor. Placement stays in
 * {@link TacticalConfig}: corner anchor, pixel offset from it and scale.
 */
final class SquadHudLayoutElement implements HudLayoutElement {
    @Override
    public String id() {
        return "tacticalfactions:squad_hud";
    }

    @Override
    public Text label() {
        return Text.translatable("ottoextra_tacticalfactions.squad_hud.edit.squad");
    }

    private SquadActivityHud.Panel panel(float scale, int screenH) {
        return SquadActivityHud.layout(MinecraftClient.getInstance().textRenderer, SquadActivityHud.currentOrSample(),
                SquadActivityHud.currentOnline(), SquadActivityHud.currentMembers(), Math.round(screenH / scale));
    }

    @Override
    public String flowId() {
        return "tacticalfactions:squad_hud";
    }

    @Override
    public Bounds bounds(int screenW, int screenH) {
        TacticalConfig config = TacticalFactions.config();
        float scale = scale();
        SquadActivityHud.Panel panel = panel(scale, screenH);
        int w = HudPlacement.scaled(panel.width(), scale);
        int h = HudPlacement.scaled(panel.height(), scale);
        HudCorner corner = config.squadHudCorner;
        if (!SquadActivityHud.flowActive() && config.squadHudOffsetX == 0 && config.squadHudOffsetY == 0) {
            corner = SquadActivityHud.effectiveCorner(corner);
        }
        int[] pos = HudPlacement.position(corner, config.squadHudOffsetX, config.squadHudOffsetY,
                screenW, screenH, w, h, SquadActivityHud.margin());
        return new Bounds(pos[0], pos[1], w, h);
    }

    @Override
    public boolean scalable() {
        return true;
    }

    @Override
    public float scale() {
        return HudPlacement.clampScale(TacticalFactions.config().squadHudScale);
    }

    @Override
    public void setScale(float scale) {
        TacticalFactions.config().squadHudScale = HudPlacement.clampScale(scale);
    }

    @Override
    public void place(int x, int y, int w, int h, int screenW, int screenH) {
        HudPlacement.Placement p = HudPlacement.fromDrag(x, y, w, h, screenW, screenH, SquadActivityHud.margin());
        TacticalConfig config = TacticalFactions.config();
        config.squadHudCorner = p.corner();
        config.squadHudOffsetX = p.offsetX();
        config.squadHudOffsetY = p.offsetY();
    }

    @Override
    public void reset() {
        TacticalConfig config = TacticalFactions.config();
        config.squadHudCorner = HudCorner.TOP_RIGHT;
        config.squadHudOffsetX = 0;
        config.squadHudOffsetY = 0;
        config.squadHudScale = 1.0f;
    }

    @Override
    public void save() {
        TacticalFactions.config().save();
        SquadActivityHud.invalidate();
    }

    @Override
    public boolean drawPreview(DrawContext context, TextRenderer font, int x, int y) {
        float scale = scale();
        List<SquadActivityHud.Prepared> lines = SquadActivityHud.currentOrSample();
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        SquadActivityHud.draw(context, font, lines, panel(scale, screenH), x, y, scale,
                SquadActivityHud.currentOnline(), SquadActivityHud.currentMembers());
        return true;
    }
}
