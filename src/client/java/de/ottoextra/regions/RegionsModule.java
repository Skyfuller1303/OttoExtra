package de.ottoextra.regions;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class RegionsModule implements OttoExtraModule {

    private KeyBinding menuKey;
    private KeyBinding activityHudToggleKey;
    private boolean menuKeyNameResolved = false;

    @Override
    public String id() {
        return "regions";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        // Listener und HUD einmalig registrieren. Die aktuelle Config wird in
        // den einzelnen Pfaden geprüft, damit Aus -> An keinen Neustart braucht.
        return true;
    }

    private static volatile OttoExtraConfig.Regions regionsConfig;

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        RegionMessageService.init(context.config());
        RegionNotificationOverlay.configure(context.config().regions);
        regionsConfig = context.config().regions;
        RegionActivityHud.configure(context.config().regions, context::isOnOttonien);

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            if (!de.ottoextra.map.WorldMapOpenAnimation.hidesHudOverlays()) RegionNotificationOverlay.render(ctx, tick);
        });
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            if (!de.ottoextra.map.WorldMapOpenAnimation.hidesHudOverlays()) RegionActivityHud.render(ctx, tick);
        });

        int code = keyCode(context.config().regions.menuKey, GLFW.GLFW_KEY_L);
        menuKey = new KeyBinding(
                "key.ottoextra.region_menu",
                InputUtil.Type.KEYSYM,
                code,
                KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(menuKey);
        activityHudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.activity_hud_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC));

        registerActivityHudRateCommand(context.config());
        registerRegionDataCommands();
        BanderoleDebugCommands.register(() -> regionsConfig);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!menuKeyNameResolved) {
                menuKeyNameResolved = true;
                try {
                    RegionNotificationOverlay.setMenuKeyName(
                            menuKey.getBoundKeyLocalizedText().getString());
                } catch (Throwable ignored) {

                }
            }
            while (menuKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(RegionInfoScreen.current(null));
                }
            }
            while (activityHudToggleKey.wasPressed()) {
                context.config().regions.activityHudEnabled =
                        !context.config().regions.activityHudEnabled;
                context.config().save();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(
                            context.config().regions.activityHudEnabled
                                    ? "ottoextra.activityHud.toggled.on"
                                    : "ottoextra.activityHud.toggled.off"), true);
                }
            }
        });

        DebugLog.debug("[regions] initialisiert (Paket-Abfang + HUD + Lehen-Menue).");
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        RegionMessageService.reset();
        RegionNotificationOverlay.clear();
    }

    private static void registerActivityHudRateCommand(OttoExtraConfig rootConfig) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("activityhud")
                                .then(ClientCommandManager.literal("rate")
                                        .executes(context -> showActivityHudRate(
                                                context.getSource(), rootConfig))
                                        .then(ClientCommandManager.argument("seconds",
                                                        IntegerArgumentType.integer(
                                                                OttoExtraConfig.Regions
                                                                        .MIN_ACTIVITY_HUD_REFRESH_SECONDS,
                                                                OttoExtraConfig.Regions
                                                                        .MAX_ACTIVITY_HUD_REFRESH_SECONDS))
                                                .executes(context -> setActivityHudRate(
                                                        context.getSource(), rootConfig,
                                                        IntegerArgumentType.getInteger(
                                                                context, "seconds"))))))));
    }

    private static void registerRegionDataCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("regions")
                                .then(ClientCommandManager.literal("refresh")
                                        .executes(context -> {
                                            var source = context.getSource();
                                            RegionsServices.refreshWithFeedback(source::sendFeedback);
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("banner")
                                        .executes(context -> replayZoneBanner(null, null))
                                        .then(ClientCommandManager.argument("style",
                                                        com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .suggests((c, b) -> net.minecraft.command.CommandSource
                                                        .suggestMatching(de.ottoextra.regions.zonebanner
                                                                .ZoneBannerStyles.ids(), b))
                                                .executes(context -> replayZoneBanner(
                                                        com.mojang.brigadier.arguments.StringArgumentType
                                                                .getString(context, "style"), null))
                                                .then(ClientCommandManager.argument("region",
                                                                com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                        .executes(context -> replayZoneBanner(
                                                                com.mojang.brigadier.arguments.StringArgumentType
                                                                        .getString(context, "style"),
                                                                com.mojang.brigadier.arguments.StringArgumentType
                                                                        .getString(context, "region"))))))
                                .then(ClientCommandManager.literal("status")
                                        .executes(context -> {
                                            RegionDataService data = RegionsServices.data();
                                            context.getSource().sendFeedback(Text.translatable(
                                                    "ottoextra.regions.status", data == null
                                                            ? "nicht initialisiert"
                                                            : data.diagnosticStatus()));
                                            return 1;
                                        })))));
    }

    /**
     * Dev/test helper: optionally switches the zone banner style (in memory, not saved)
     * and replays the enter banner for the given or the last entered region.
     */
    private static int replayZoneBanner(String style, String region) {
        if (style != null && regionsConfig != null) {
            regionsConfig.zoneBannerStyle = style;
        }
        String target = region;
        String hierarchy = "";
        if (target == null || target.isBlank()) {
            RegionState state = RegionMessageService.current();
            target = state != null ? state.regionName() : "Niederpölt";
            hierarchy = state != null && state.hierarchyLine() != null ? state.hierarchyLine() : "";
        }
        RegionNotificationOverlay.show(target, hierarchy);
        return 1;
    }

    private static int setActivityHudRate(
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            OttoExtraConfig rootConfig, int seconds) {
        rootConfig.regions.activityHudRefreshSeconds = seconds;
        rootConfig.save();
        RegionDataService data = RegionsServices.data();
        if (data != null) {
            data.setActivityRefreshSeconds(seconds);
        }
        source.sendFeedback(Text.translatable(
                "ottoextra.activityHud.rate.set", seconds));
        return 1;
    }

    private static int showActivityHudRate(
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            OttoExtraConfig rootConfig) {
        source.sendFeedback(Text.translatable("ottoextra.activityHud.rate.current",
                rootConfig.regions.activityHudRefreshSeconds));
        return 1;
    }

    private static int keyCode(String translationKey, int fallback) {
        if (translationKey == null || translationKey.isBlank()) {
            return fallback;
        }
        try {
            return InputUtil.fromTranslationKey(translationKey).getCode();
        } catch (Throwable t) {
            return fallback;
        }
    }
}
