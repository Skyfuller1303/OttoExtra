package de.ottoextra.rpnames;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.model.RpNameSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public final class RpNamesModule implements OttoExtraModule {

    private static final int SEEN_SYNC_INTERVAL_TICKS = 100;

    private static final int TITLE_SYNC_INTERVAL_TICKS = 600;

    private int tickCounter = 0;
    private int titleTickCounter = 0;
    private net.minecraft.client.option.KeyBinding peopleKey;

    @Override
    public String id() {
        return "rpnames";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.rpnames.enabled;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        RpNamesServices.init(context.config().rpnames);

        registerHoverDebugCommand();

        boolean legacyPresent = FabricLoader.getInstance().isModLoaded("ottochat_rpnames")
                || FabricLoader.getInstance().isModLoaded("ottotalk")
                || FabricLoader.getInstance().isModLoaded("ottonames");
        if (legacyPresent) {
            OttoExtra.LOGGER.warn(
                    "[rpnames] Legacy-Mod (ottochat_rpnames/ottotalk/ottonames) erkannt - "
                            + "OttoExtra-RP-Namen bleiben deaktiviert (Mixin-Konflikte).");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (peopleKey != null && peopleKey.wasPressed()) {
                if (client.currentScreen == null && RpNamesServices.store() != null) {
                    client.setScreen(new de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen(null));
                }
            }
            if (client.player == null || client.getNetworkHandler() == null
                    || !RpNamesServices.isActive()) {
                return;
            }
            if (++titleTickCounter >= TITLE_SYNC_INTERVAL_TICKS) {
                titleTickCounter = 0;
                syncTitlesFromTablist(client);
            }
            if (++tickCounter < SEEN_SYNC_INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;
            syncSeenPlayers(client);
        });

        peopleKey = new net.minecraft.client.option.KeyBinding(
                "key.ottoextra.rpnames_people",
                net.minecraft.client.util.InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.option.KeyBinding.Category.MISC);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(peopleKey);

        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register(
                (player, world, hand, entity, hitResult) -> {
                    boolean meet = RpNamesServices.proactiveMeetEnabled();
                    boolean book = RpNamesServices.openBookOnClickEnabled();
                    if (!world.isClient() || hand != net.minecraft.util.Hand.MAIN_HAND
                            || (!meet && !book) || !player.isSneaking()
                            || !(entity instanceof net.minecraft.entity.player.PlayerEntity target)) {
                        return net.minecraft.util.ActionResult.PASS;
                    }
                    String account = target.getGameProfile().name();
                    String uuid = target.getUuid() != null ? target.getUuid().toString() : null;
                    var p = RpNamesServices.store() != null
                            ? RpNamesServices.store().findByName(account).orElse(null) : null;
                    boolean unknown = p == null || !RpNamesServices.isKnownForDisplay(p);
                    net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                        if (meet && unknown) {

                            net.minecraft.client.MinecraftClient.getInstance().setScreen(
                                    new de.ottoextra.rpnames.ui.MeetPersonScreen(null, account, uuid));
                        } else if (book) {
                            de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen.openFor(null, account, uuid);
                        }
                    });
                    return net.minecraft.util.ActionResult.SUCCESS;
                });

        MeetMarkerRenderer.register();

        OttoExtra.LOGGER.info("[rpnames] initialisiert (lokales Bekanntschaftssystem, {} Personen).",
                RpNamesServices.store().size());
    }

    private void registerHoverDebugCommand() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, access) -> dispatcher.register(
                        net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                .literal("ottoextra")
                                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                        .literal("rpnames")
                                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                .literal("hoverdebug")
                                                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                        .literal("on")
                                                        .executes(c -> {
                                                            de.ottoextra.rpnames.chat.HoverDebug.setEnabled(true);
                                                            feedback("§a[RP-Namen]§7 Hover-Debug §aAN§7 — "
                                                                    + "Chat-Hover werden ins Log (latest.log) geschrieben.");
                                                            return 1;
                                                        }))
                                                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                        .literal("off")
                                                        .executes(c -> {
                                                            de.ottoextra.rpnames.chat.HoverDebug.setEnabled(false);
                                                            feedback("§a[RP-Namen]§7 Hover-Debug §cAUS§7.");
                                                            return 1;
                                                        }))))));
    }

    private static void feedback(String text) {
        var client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(text), false);
        }
    }

    private void syncSeenPlayers(MinecraftClient client) {
        try {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                if (profile == null || profile.name() == null || profile.name().isBlank()) {
                    continue;
                }

                de.ottoextra.chat.SkinCache.remember(profile);
                if (client.player != null
                        && profile.id() != null
                        && profile.id().equals(client.player.getUuid())) {
                    continue;
                }
                RpNamesServices.store().ensureSeen(profile.name(),
                        profile.id() != null ? profile.id().toString() : null,
                        RpNameSource.SEEN_TABLIST);
            }
            de.ottoextra.chat.SkinCache.flush();
        } catch (Throwable t) {
            OttoExtra.LOGGER.debug("[rpnames] Tablist-Sync-Fehler: {}", t.toString());
        }
    }

    private void syncTitlesFromTablist(MinecraftClient client) {
        try {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                if (profile == null || profile.name() == null || profile.name().isBlank()) {
                    continue;
                }

                net.minecraft.text.Text display =
                        ((de.ottoextra.mixin.PlayerListEntryAccessor) (Object) entry)
                                .ottoextra$rawDisplayName();
                if (display == null) {
                    continue;
                }

                String title = de.ottoextra.rpnames.tablist.TablistNameFormatter
                        .extractServerTitle(display, profile.name());
                if (!title.isEmpty()) {

                    RpNamesServices.store().updateTitleIfChanged(profile.name(),
                            profile.id() != null ? profile.id().toString() : null, title);
                }
            }
        } catch (Throwable t) {
            OttoExtra.LOGGER.debug("[rpnames] Titel-Sync-Fehler: {}", t.toString());
        }
    }

    @Override
    public void onServerJoin(OttoExtraContext context) {
        RpNamesServices.setActive(true);

        var store = RpNamesServices.store();
        if (store != null && RpNamesServices.isActive()) {
            de.ottoextra.rpnames.importer.RegionsApiRpNameImporter.runAuto(store)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            OttoExtra.LOGGER.debug("[rpnames] Auto-API-Abgleich fehlgeschlagen: {}",
                                    error.toString());
                        } else {
                            store.saveNow();
                            OttoExtra.LOGGER.info("[rpnames] Auto-API-Abgleich: {}", result);
                        }
                    });
        }
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        RpNamesServices.setActive(false);
        de.ottoextra.chat.SkinCache.flush();
        if (RpNamesServices.store() != null) {
            RpNamesServices.store().saveNow();
        }
    }

    @Override
    public void onClientStop(OttoExtraContext context) {
        RpNamesServices.shutdown();
    }
}
