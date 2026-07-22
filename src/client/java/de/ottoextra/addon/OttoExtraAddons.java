package de.ottoextra.addon;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.settings.SettingsRegistry;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.logging.FailureLogGate;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OttoExtraAddons {

    private static final Map<OttoExtraAddon, FailureLogGate> CHAT_FAILURE_LOGS =
            new ConcurrentHashMap<>();

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
                    DebugLog.debug("OttoExtra-Addon geladen: {}",
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

    public static Text processChatMessage(Text message) {
        Text current = message;
        for (OttoExtraAddon addon : addons()) {
            FailureLogGate failureLog = CHAT_FAILURE_LOGS.computeIfAbsent(
                    addon, ignored -> new FailureLogGate());
            try {
                Text changed = addon.processChatMessage(current);
                if (changed != null) current = changed;
                if (failureLog.onSuccess()) {
                    DebugLog.debug("Addon-Chatverarbeitung wieder aktiv: {}",
                            addon.getClass().getName());
                }
            } catch (Throwable t) {
                if (failureLog.onFailure()) {
                    OttoExtra.LOGGER.warn("Addon-Chatverarbeitung fehlgeschlagen ({}): {}",
                            addon.getClass().getName(), t.toString());
                }
            }
        }
        return current;
    }
}
