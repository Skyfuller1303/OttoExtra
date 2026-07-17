package de.ottoextra.addon;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.settings.SettingsRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import java.util.ArrayList;
import java.util.List;
public final class OttoExtraAddons {
    private static List<OttoExtraAddon> addons;
    private OttoExtraAddons() {
    }
    private static synchronized List<OttoExtraAddon> addons() {
        if (addons == null) {
            List<OttoExtraAddon> found = new ArrayList<>();
            for (EntrypointContainer<OttoExtraAddon> container
                    : FabricLoader.getInstance().getEntrypointContainers("ottoextra", OttoExtraAddon.class)) {
                try {
                    found.add(container.getEntrypoint());
                    OttoExtra.LOGGER.info("OttoExtra-Addon geladen: {}",
                            container.getProvider().getMetadata().getId());
                } catch (Throwable t) {
                    OttoExtra.LOGGER.error("OttoExtra-Addon aus '{}' konnte nicht geladen werden",
                            container.getProvider().getMetadata().getId(), t);
                }
            }
            addons = found;
        }
        return addons;
    }
    public static void registerSettings(SettingsRegistry registry) {
        for (OttoExtraAddon addon : addons()) {
            try {
                addon.registerSettings(registry);
            } catch (Throwable t) {
                OttoExtra.LOGGER.error("Addon-Settings konnten nicht registriert werden", t);
            }
        }
    }
    public static void registerDefaultSettings(SettingsRegistry registry) {
        for (OttoExtraAddon addon : addons()) {
            try {
                addon.registerDefaultSettings(registry);
            } catch (Throwable t) {
                OttoExtra.LOGGER.error("Addon-Default-Settings konnten nicht registriert werden", t);
            }
        }
    }
}
