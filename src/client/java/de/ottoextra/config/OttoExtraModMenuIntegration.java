package de.ottoextra.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu-Anbindung (optional). Wird nur geladen, wenn ModMenu installiert ist.
 */
public final class OttoExtraModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new de.ottoextra.config.settings.OttoExtraSettingsScreen(parent);
    }
}
