package de.ottoextra.tweaks.vote;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Registriert den früher eigenständigen Vote-Reminder als internes Tweaks-Feature. */
public final class VoteReminderFeature {

    private VoteReminderFeature() {
    }

    public static void register(OttoExtraConfig rootConfig) {
        VoteReminderConfig config = rootConfig.tweaks.vote;
        if (config.migrateLegacy(FabricLoader.getInstance().getConfigDir()
                .resolve("ottoextra-vote.json"))) {
            rootConfig.save();
        }

        // Verhindert doppelte Toasts und Browser-Tabs während der Umstellung.
        if (FabricLoader.getInstance().isModLoaded("ottoextra_vote")) {
            OttoExtra.LOGGER.warn(
                    "Eigenständiges OttoExtra-Vote erkannt; integrierter Vote-Reminder bleibt inaktiv.");
            return;
        }

        KeyBinding openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.tweaks.open_vote_pages",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_ENTER,
                KeyBinding.Category.MISC
        ));
        KeyBinding dismissKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.tweaks.dismiss_vote",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                KeyBinding.Category.MISC
        ));
        VoteReminderManager manager = new VoteReminderManager(
                config,
                new VoteBrowserOpener(config.validatedVoteUris()),
                openKey,
                dismissKey,
                rootConfig::save
        );

        ClientTickEvents.END_CLIENT_TICK.register(manager::tick);
        HudRenderCallback.EVENT.register(manager::renderToast);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                manager.connected(client, handler.getServerInfo()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                manager.disconnected(client));
        VoteReminderCommands.register(manager);

        OttoExtra.LOGGER.info("Integrierter Vote-Reminder bereit.");
    }
}
