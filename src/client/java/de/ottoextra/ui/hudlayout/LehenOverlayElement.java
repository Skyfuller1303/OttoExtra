package de.ottoextra.ui.hudlayout;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.map.xaero.MinimapBannerOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xaero.hud.minimap.BuiltInHudModules;

/**
 * One part of the Lehen overlay next to the Xaero minimap (banner, region name, state line or
 * faction name). Only created when Xaero's minimap is loaded.
 */
final class LehenOverlayElement implements HudLayoutElement {
    private final OttoExtraConfig config;
    private final MinimapBannerOverlay.Kind kind;

    LehenOverlayElement(OttoExtraConfig config, MinimapBannerOverlay.Kind kind) {
        this.config = config;
        this.kind = kind;
    }

    @Override
    public String id() {
        return "ottoextra:lehen:" + kind.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public Text label() {
        return Text.translatable("ottoextra.hudlayout.element.lehen." + kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    private MinimapBannerOverlay.Element element() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return null;
        }
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        for (MinimapBannerOverlay.Element e : MinimapBannerOverlay.computeElements(client, config.map, session)) {
            if (e.kind() == kind) {
                return e;
            }
        }
        return null;
    }

    @Override
    public Bounds bounds(int screenW, int screenH) {
        MinimapBannerOverlay.Element e = element();
        return e == null ? null : new Bounds(e.x(), e.y(), e.width(), e.height());
    }

    @Override
    public void place(int x, int y, int w, int h, int screenW, int screenH) {
        MinimapBannerOverlay.savePosition(config.map, kind, x, y, w, h, screenW, screenH);
    }

    @Override
    public void reset() {
        OttoExtraConfig.Map m = config.map;
        switch (kind) {
            case BANNER -> {
                m.bannerHudX = -1;
                m.bannerHudY = -1;
            }
            case NAME -> {
                m.nameHudX = -1;
                m.nameHudY = -1;
            }
            case STATE -> {
                m.stateHudX = -1;
                m.stateHudY = -1;
            }
            case FACTION -> {
                m.factionHudX = -1;
                m.factionHudY = -1;
            }
        }
    }

    @Override
    public void save() {
        config.save();
    }

    @Override
    public boolean rendersLive() {
        return true;
    }
}
