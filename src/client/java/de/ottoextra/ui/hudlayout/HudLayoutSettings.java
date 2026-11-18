package de.ottoextra.ui.hudlayout;

import de.ottoextra.config.settings.SettingsRegistry;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

/** Entry points of the HUD layout editor: a settings button (map module) and {@code /ottoextra hud}. */
public final class HudLayoutSettings {
    private HudLayoutSettings() {
    }

    /** Registration without a live config (layout tests): the options act on a throw-away config. */
    public static void register(SettingsRegistry registry, Runnable open) {
        register(registry, new de.ottoextra.config.OttoExtraConfig(), open);
    }

    /** Adds an "arrange HUDs" button to the first tab of the map module. */
    public static void register(SettingsRegistry registry, de.ottoextra.config.OttoExtraConfig config,
                                Runnable open) {
        for (SettingsRegistry.ModulePage module : registry.modules()) {
            if ("map".equals(module.id()) && !module.tabs().isEmpty()) {
                SettingsRegistry.card(module.tabs().get(0), "ottoextra.hudlayout.card",
                        "ottoextra.hudlayout.card.desc",
                        SettingsRegistry.Option.action("ottoextra.hudlayout.open", "hudlayout.open", open),
                        SettingsRegistry.Option.bool("ottoextra.hudlayout.flow", "hudlayout.flow",
                                () -> config.hudFlowEnabled, v -> config.hudFlowEnabled = v)
                                .tooltip("ottoextra.hudlayout.flow.tip"));
                return;
            }
        }
    }

    public static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("hud").executes(ctx -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            // Open after the chat screen closed, otherwise it would replace the editor.
                            client.send(() -> client.setScreen(HudLayoutEditorScreen.create(null)));
                            return 1;
                        }))));
    }
}
