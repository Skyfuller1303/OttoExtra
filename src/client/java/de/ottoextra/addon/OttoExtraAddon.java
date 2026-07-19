package de.ottoextra.addon;

import de.ottoextra.config.settings.SettingsRegistry;
import net.minecraft.text.Text;

public interface OttoExtraAddon {

    void registerSettings(SettingsRegistry registry);

    default void registerDefaultSettings(SettingsRegistry registry) {
    }

    default Text processChatMessage(Text message) {
        return message;
    }
}
