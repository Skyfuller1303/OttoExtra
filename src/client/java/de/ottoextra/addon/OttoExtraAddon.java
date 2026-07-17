package de.ottoextra.addon;
import de.ottoextra.config.settings.SettingsRegistry;
public interface OttoExtraAddon {
    void registerSettings(SettingsRegistry registry);
    default void registerDefaultSettings(SettingsRegistry registry) {
    }
}
