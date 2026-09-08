package de.ottoextra.map;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

/** Keeps the optional Xaero-backed screen off the settings screen's verification path. */
public final class MinimapBannerScreenFactory {
    private MinimapBannerScreenFactory() { }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("xaerominimap");
    }

    public static Screen create(Screen parent, OttoExtraConfig config) {
        return isAvailable() ? Loaded.create(parent, config) : parent;
    }

    private static final class Loaded {
        private static Screen create(Screen parent, OttoExtraConfig config) {
            return new MinimapBannerEditScreen(parent, config);
        }
    }
}
