package de.ottoextra;

import de.ottoextra.api.HttpOttoExtraApiClient;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.chat.ChatModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterModule;
import de.ottoextra.map.MapModule;
import de.ottoextra.nametags.NametagModule;
import de.ottoextra.playerlist.PlayerListSortingModule;
import de.ottoextra.regions.RegionsModule;
import de.ottoextra.resourcepack.ResourcePackModule;
import de.ottoextra.rpnames.RpNamesModule;
import de.ottoextra.update.UpdateChecker;
import de.ottoextra.welcome.WelcomeScreenManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.List;

public final class OttoExtraClient implements ClientModInitializer {

    private static volatile OttoExtraApiClient sharedApi;
    private OttoExtraContext context;

    public static OttoExtraApiClient apiClient() {
        return sharedApi;
    }

    @Override
    public void onInitializeClient() {
        OttoExtra.LOGGER.info("Initialisiere {} ...", OttoExtra.MOD_NAME);

        de.ottoextra.config.OttoExtraBackupService.ensurePreMigrationBackup();
        OttoExtraConfig config = OttoExtraConfig.load();
        de.ottoextra.chat.SkinCache.load();
        OttoExtraApiClient api = new HttpOttoExtraApiClient(config);
        sharedApi = api;
        this.context = new OttoExtraContext(config, api);

        List<OttoExtraModule> modules = List.of(
                new ResourcePackModule(),
                new MapModule(),
                new RegionsModule(),
                new RpNamesModule(),
                new PlayerListSortingModule(),
                new NametagModule(),
                new LetterModule(),
                new ChatModule(),
                new de.ottoextra.tweaks.TweaksModule()
        );
        context.setModules(modules);

        for (OttoExtraModule module : context.activeModules()) {
            runSafe(module, () -> module.onInitializeClient(context), "init");
        }
        for (OttoExtraModule module : modules) {
            if (!module.enabled(config)) {
                OttoExtra.LOGGER.info("Modul '{}' ist per Config deaktiviert.", module.id());
            }
        }

        registerLifecycle(api);
        registerMenuButton();
        WelcomeScreenManager.initialize();
        UpdateChecker.initialize();

        OttoExtra.LOGGER.info("{} bereit — {} Modul(e) aktiv.",
                OttoExtra.MOD_NAME, context.activeModules().size());
    }

    private void registerMenuButton() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {
                    if (screen instanceof net.minecraft.client.gui.screen.GameMenuScreen) {
                        net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(
                                new de.ottoextra.config.OttoExtraMenuButton(
                                        4, scaledHeight - 34, 30, screen));
                    }
                });
    }

    private void registerLifecycle(OttoExtraApiClient api) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            boolean onOttonien = OttoExtraGate.isOnOttonien(client);
            context.setOnOttonien(onOttonien);
            if (!onOttonien) {
                OttoExtra.LOGGER.info("Kein Ottonien-Server erkannt — Online-Features ruhen.");
                return;
            }
            OttoExtra.LOGGER.info("Ottonien-Server beigetreten — aktiviere Module.");
            for (OttoExtraModule module : context.activeModules()) {
                runSafe(module, () -> module.onServerJoin(context), "join");
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            for (OttoExtraModule module : context.activeModules()) {
                runSafe(module, () -> module.onDisconnect(context), "disconnect");
            }
            context.setOnOttonien(false);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            for (OttoExtraModule module : context.activeModules()) {
                runSafe(module, () -> module.onClientStop(context), "stop");
            }
            api.close();
            sharedApi = null;
        });
    }

    private static void runSafe(OttoExtraModule module, Runnable action, String phase) {
        try {
            action.run();
        } catch (Throwable t) {
            OttoExtra.LOGGER.error("Modul '{}' Phase '{}' fehlgeschlagen — wird uebersprungen.",
                    module.id(), phase, t);
        }
    }
}
