package de.ottoextra.ui.hudlayout;

import de.ottoextra.OttoExtra;
import de.ottoextra.addon.OttoExtraAddons;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/** Collects the elements of the HUD layout editor: built-ins, Xaero-backed ones and addon ones. */
public final class HudLayoutRegistry {
    private HudLayoutRegistry() {
    }

    /** Fresh element instances for one editor session; bottom-most element first. */
    public static List<HudLayoutElement> elements(OttoExtraConfig config) {
        List<HudLayoutElement> list = new ArrayList<>();
        if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            try {
                XaeroElements.addTo(list, config);
            } catch (Throwable t) {
                OttoExtra.LOGGER.warn("[hudlayout] Xaero-Elemente nicht verfügbar: {}", t.toString());
            }
        }
        list.add(new ActivityHudElement(config));
        list.add(new de.ottoextra.crafting.werkstatt.WerkstattHud.Element(config));
        list.add(new de.ottoextra.ailments.ConditionsHud.Element(config));
        list.addAll(OttoExtraAddons.hudLayoutElements());
        return list;
    }

    /** Keeps Xaero class references off the verification path when Xaero is absent. */
    private static final class XaeroElements {
        static void addTo(List<HudLayoutElement> list, OttoExtraConfig config) {
            list.add(new de.ottoextra.map.xaero.XaeroMinimapLayoutElement());
            for (de.ottoextra.map.xaero.MinimapBannerOverlay.Kind kind
                    : de.ottoextra.map.xaero.MinimapBannerOverlay.Kind.values()) {
                list.add(new LehenOverlayElement(config, kind));
            }
        }
    }
}
