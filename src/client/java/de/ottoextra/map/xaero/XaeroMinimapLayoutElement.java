package de.ottoextra.map.xaero;

import de.ottoextra.OttoExtra;
import de.ottoextra.ui.hudlayout.HudLayoutElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xaero.common.HudMod;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.module.HudModule;
import xaero.hud.module.ModuleTransform;

/**
 * The Xaero minimap as a movable layout element. Position is written through Xaero's own
 * {@link ModuleTransform} (the same object its edit mode uses) and saved with Xaero's own settings
 * writer, so no mixin is needed. Size is not exposed as a free scale by Xaero, so it is not scalable.
 */
public final class XaeroMinimapLayoutElement implements HudLayoutElement {
    private final ModuleTransform initial;

    public XaeroMinimapLayoutElement() {
        ModuleTransform current = BuiltInHudModules.MINIMAP.getConfirmedTransform();
        this.initial = current == null ? null : current.copy();
    }

    @Override
    public String id() {
        return "xaero:minimap";
    }

    @Override
    public Text label() {
        return Text.translatable("ottoextra.hudlayout.element.minimap");
    }

    @Override
    public boolean rendersLive() {
        return true;
    }

    @Override
    public Bounds bounds(int screenW, int screenH) {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null || !session.isActive()) {
            return null;
        }
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int w = session.getWidth(guiScale);
        int h = session.getHeight(guiScale);
        if (w <= 0 || h <= 0) {
            return null;
        }
        return new Bounds(session.getEffectiveX(screenW, guiScale), session.getEffectiveY(screenH, guiScale), w, h);
    }

    @Override
    public void place(int x, int y, int w, int h, int screenW, int screenH) {
        HudModule<MinimapSession> module = BuiltInHudModules.MINIMAP;
        ModuleTransform t = module.getConfirmedTransform().copy();
        t.centered = false;
        t.fromOldSystem = false;
        t.fromRight = x + w / 2 > screenW / 2;
        t.fromBottom = y + h / 2 > screenH / 2;
        t.x = t.fromRight ? screenW - x - w : x;
        t.y = t.fromBottom ? screenH - y - h : y;
        module.setTransform(t);
    }

    @Override
    public void reset() {
        if (initial != null) {
            BuiltInHudModules.MINIMAP.setTransform(initial.copy());
        }
    }

    @Override
    public void save() {
        try {
            HudMod hud = HudMod.INSTANCE;
            if (hud != null && hud.getSettings() != null) {
                hud.getSettings().saveSettings();
            }
        } catch (Throwable t) {
            OttoExtra.LOGGER.warn("[hudlayout] Xaero-Minimap-Position konnte nicht gespeichert werden: {}", t.toString());
        }
    }
}
