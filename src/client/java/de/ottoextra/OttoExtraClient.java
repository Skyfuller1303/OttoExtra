package de.ottoextra;

import de.ottoextra.api.HttpOttoExtraApiClient;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.animalinfo.AnimalInfoModule;
import de.ottoextra.chat.ChatModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterModule;
import de.ottoextra.horses.HorseRegistryModule;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.map.MapModule;
import de.ottoextra.nametags.NametagModule;
import de.ottoextra.playerlist.PlayerListSortingModule;
import de.ottoextra.regions.RegionsModule;
import de.ottoextra.regions.RegionsServices;
import de.ottoextra.resourcepack.ResourcePackModule;
import de.ottoextra.rpnames.RpNamesModule;
import de.ottoextra.update.UpdateChecker;
import de.ottoextra.verify.AccountLinkModule;
import de.ottoextra.welcome.WelcomeScreenManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.List;

public final class OttoExtraClient implements ClientModInitializer {

    private static volatile OttoExtraApiClient sharedApi;
    private OttoExtraContext context;
    private boolean onlineServicesActive;

    public static OttoExtraApiClient apiClient() {
        return sharedApi;
    }

    @Override
    public void onInitializeClient() {
        de.ottoextra.privacy.LocalDataSession.initialize();
        DebugLog.registerCommand();
        de.ottoextra.ui.hudlayout.HudLayoutSettings.registerCommand();
        de.ottoextra.logging.GuiTrace.register();
        de.ottoextra.ailments.AilmentTrace.register();
        de.ottoextra.ui.complete.TabCompletion.register();
        DebugLog.debug("Initialisiere {} ...", OttoExtra.MOD_NAME);

        de.ottoextra.config.OttoExtraBackupService.ensurePreMigrationBackup();
        OttoExtraConfig config = OttoExtraConfig.load();
        de.ottoextra.chat.SkinCache.load();
        OttoExtraApiClient api = new HttpOttoExtraApiClient(config);
        sharedApi = api;
        this.context = new OttoExtraContext(config, api);
        runRegionsService(() -> RegionsServices.init(
                api, config.regions.activityHudRefreshSeconds), "init");

        List<OttoExtraModule> modules = List.of(
                new ResourcePackModule(),
                new MapModule(),
                new RegionsModule(),
                new RpNamesModule(),
                new PlayerListSortingModule(),
                new NametagModule(),
                new LetterModule(),
                new ChatModule(),
                new AnimalInfoModule(),
                new HorseRegistryModule(),
                new de.ottoextra.tweaks.TweaksModule(),
                new AccountLinkModule(),
                new de.ottoextra.ailments.ConditionsModule()
        );
        context.setModules(modules);

        for (OttoExtraModule module : context.activeModules()) {
            runSafe(module, () -> module.onInitializeClient(context), "init");
        }
        for (OttoExtraModule module : modules) {
            if (!module.enabled(config)) {
                DebugLog.debug("Modul '{}' ist per Config deaktiviert.", module.id());
            }
        }

        registerLifecycle(api);
        registerMenuButton();
        WelcomeScreenManager.initialize(config);
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
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean allowed = context.isOnOttonien() && de.ottoextra.privacy.ConsentPolicy.allowed(
                    context.config(), de.ottoextra.privacy.ConsentPolicy.Purpose.ONLINE_SERVICES);
            if (allowed == onlineServicesActive) return;
            onlineServicesActive = allowed;
            if (allowed) {
                runRegionsService(RegionsServices::onServerJoin, "online-enabled");
            } else {
                runRegionsService(RegionsServices::onDisconnect, "online-disabled");
                if (api instanceof HttpOttoExtraApiClient http) http.invalidateAuthentication();
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            boolean onOttonien = OttoExtraGate.isOnOttonien(client);
            context.setOnOttonien(onOttonien);
            if (!onOttonien) {
                DebugLog.debug("Kein Ottonien-Server erkannt — Online-Features ruhen.");
                return;
            }
            DebugLog.debug("Ottonien-Server beigetreten — aktiviere Module.");
            onlineServicesActive = de.ottoextra.privacy.ConsentPolicy.allowed(
                    context.config(), de.ottoextra.privacy.ConsentPolicy.Purpose.ONLINE_SERVICES);
            if (onlineServicesActive) runRegionsService(RegionsServices::onServerJoin, "join");
            for (OttoExtraModule module : context.activeModules()) {
                runSafe(module, () -> module.onServerJoin(context), "join");
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            runRegionsService(RegionsServices::onDisconnect, "disconnect");
            for (OttoExtraModule module : context.activeModules()) {
                runSafe(module, () -> module.onDisconnect(context), "disconnect");
            }
            context.setOnOttonien(false);
            onlineServicesActive = false;
            context.config().consent.gate(de.ottoextra.privacy.ConsentPolicy.Purpose.ONLINE_SERVICES).invalidate();
            if (api instanceof HttpOttoExtraApiClient http) http.invalidateAuthentication();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            de.ottoextra.privacy.ConsentPolicy.invalidateAll(context.config());
            runRegionsService(RegionsServices::shutdown, "stop");
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

    private static void runRegionsService(Runnable action, String phase) {
        try {
            action.run();
        } catch (Throwable t) {
            OttoExtra.LOGGER.error("Regions-Datendienst Phase '{}' fehlgeschlagen.", phase, t);
        }
    }
}
