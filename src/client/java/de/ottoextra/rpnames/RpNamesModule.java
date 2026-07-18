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

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpNamesModule implements OttoExtraModule {

    private static final int SEEN_SYNC_INTERVAL_TICKS = 100;

    private static final int TITLE_SYNC_INTERVAL_TICKS = 600;

    private int tickCounter = 0;
    private int titleTickCounter = 0;
    private int historyRefreshTicks = -1;
    private net.minecraft.client.option.KeyBinding peopleKey;
    private final java.util.Set<String> pendingMeetApiRequests =
            ConcurrentHashMap.newKeySet();

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
            if (historyRefreshTicks > 0 && --historyRefreshTicks == 0) {
                de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
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
                    if (meet && unknown) {
                        requestCurrentIdentityAndOpenMeetScreen(context, account, uuid);
                    } else if (book) {
                        net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                                de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen
                                        .openFor(null, account, uuid));
                    }
                    return net.minecraft.util.ActionResult.SUCCESS;
                });

        MeetMarkerRenderer.register();
        de.ottoextra.rpnames.inspect.InspectMode.register(context.config().rpnames);

        OttoExtra.LOGGER.info("[rpnames] initialisiert (lokales Bekanntschaftssystem, {} Personen).",
                RpNamesServices.store().size());
    }

    /**
     * Lädt beim Shift-Rechtsklick zuerst den aktuellen Spielerstand über
     * public-player (beziehungsweise v2 mit Public-Fallback). Erst nachdem die
     * Antwort vorliegt, wird das Kennenlernfenster geöffnet.
     */
    private void requestCurrentIdentityAndOpenMeetScreen(
            OttoExtraContext context, String account, String uuidText) {
        if (account == null || account.isBlank() || uuidText == null || uuidText.isBlank()) {
            showMeetLookupError("Spieler-UUID fehlt");
            return;
        }

        final UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException e) {
            showMeetLookupError("Ungültige Spieler-UUID");
            return;
        }

        String requestKey = uuid.toString().toLowerCase(Locale.ROOT);
        if (!pendingMeetApiRequests.add(requestKey)) {
            return;
        }

        OttoExtra.LOGGER.info(
                "[rpnames] Lade aktuellen API-Stand für {} ({}) vor dem Kennenlernen.",
                account, uuid);

        context.api().player(uuid).whenComplete((profile, error) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                pendingMeetApiRequests.remove(requestKey);

                if (client.world == null || client.player == null
                        || !RpNamesServices.isActive()) {
                    return;
                }

                if (error != null) {
                    OttoExtra.LOGGER.warn(
                            "[rpnames] Spielerprofil für {} konnte nicht geladen werden: {}",
                            account, summarizeError(error));
                    showMeetLookupError("API-Anfrage fehlgeschlagen");
                    return;
                }
                if (profile == null) {
                    OttoExtra.LOGGER.warn(
                            "[rpnames] Spielerprofil für {} ist in der API-Antwort leer.", account);
                    showMeetLookupError("Kein Spielerprofil erhalten");
                    return;
                }

                String apiUuid = cleanApiText(profile.uuid());
                if (apiUuid != null && !apiUuid.equalsIgnoreCase(uuid.toString())) {
                    OttoExtra.LOGGER.warn(
                            "[rpnames] API-Antwort für {} enthält eine fremde UUID: {}",
                            account, apiUuid);
                    showMeetLookupError("API-Antwort gehört nicht zum angeklickten Spieler");
                    return;
                }

                String apiAccount = cleanApiText(profile.minecraft_name());
                if (apiAccount != null && !apiAccount.equalsIgnoreCase(account)) {
                    OttoExtra.LOGGER.warn(
                            "[rpnames] API-Antwort für {} enthält einen fremden Account: {}",
                            account, apiAccount);
                    showMeetLookupError("API-Antwort gehört nicht zum angeklickten Spieler");
                    return;
                }

                String responseUuid = firstNonBlank(apiUuid, uuid.toString());
                String rpName = firstNonBlank(
                        cleanApiText(profile.rp_name()),
                        fallbackProfileName(profile, account));
                String title = cleanApiText(profile.title());

                var store = RpNamesServices.store();
                if (store != null) {
                    store.ensureSeen(account, responseUuid, RpNameSource.SEEN_ONLINE);
                    store.importApi(account, responseUuid, rpName, title);
                }

                // Direkte API-Werte werden an das Fenster übergeben. Dadurch
                // kann kein älterer lokaler Chat-Vorschlag die Anzeige ersetzen.
                client.setScreen(new de.ottoextra.rpnames.ui.MeetPersonScreen(
                        null, account, responseUuid, rpName, title));

                OttoExtra.LOGGER.info(
                        "[rpnames] API-Spielerprofil geladen: account={}, rpName={}, title={}",
                        account, rpName == null ? "<leer>" : rpName,
                        title == null ? "<leer>" : title);
            });
        });
    }

    private static String fallbackProfileName(
            de.ottoextra.api.model.PlayerRecord profile, String targetAccount) {
        String value = cleanApiText(profile.name());
        if (value == null || value.equalsIgnoreCase(targetAccount)) {
            return null;
        }
        String apiAccount = cleanApiText(profile.minecraft_name());
        return apiAccount != null && value.equalsIgnoreCase(apiAccount) ? null : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String cleanApiText(String value) {
        if (value == null) {
            return null;
        }
        String fixed = value;
        for (int i = 0; i < 2
                && (fixed.indexOf('Ã') >= 0 || fixed.indexOf('Â') >= 0); i++) {
            String decoded = new String(
                    fixed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (decoded.indexOf('�') >= 0) {
                break;
            }
            fixed = decoded;
        }
        fixed = fixed.trim();
        return fixed.isEmpty() ? null : fixed;
    }

    private static String summarizeError(Throwable error) {
        Throwable current = error;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return current.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static void showMeetLookupError(String detail) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(
                    "§c[RP-Namen] §7Aktuelle Personendaten konnten nicht geladen werden"
                            + (detail == null || detail.isBlank() ? "." : ": " + detail)), true);
        }
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
        if (!RpNamesServices.config().tablistTitlesAlways) {
            return;
        }
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
        // MoreChatHistory stellt alte Zeilen erst nach dem Join wieder her.
        // Der verzögerte Lauf aktualisiert auch diese Nachrichten mit den
        // inzwischen manuell geänderten RP-Profilen.
        historyRefreshTicks = 100;
        de.ottoextra.rpnames.upload.RpNameUploadService.resetObservedSession();

        var store = RpNamesServices.store();
        if (store != null && RpNamesServices.isActive()) {
            de.ottoextra.rpnames.importer.RegionsApiRpNameImporter.runAuto(store)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            OttoExtra.LOGGER.debug("[rpnames] Auto-API-Abgleich fehlgeschlagen: {}",
                                    error.toString());
                        } else {
                            store.saveNow();
                            de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
                            OttoExtra.LOGGER.info("[rpnames] Auto-API-Abgleich: {}", result);
                        }
                    });
        }
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        RpNamesServices.setActive(false);
        historyRefreshTicks = -1;
        de.ottoextra.rpnames.upload.RpNameUploadService.resetObservedSession();
        de.ottoextra.chat.SkinCache.flush();
        if (RpNamesServices.store() != null) {
            RpNamesServices.store().saveNow();
        }
    }

    @Override
    public void onClientStop(OttoExtraContext context) {
        RpNamesServices.shutdown();
        de.ottoextra.rpnames.upload.RpNameUploadService.shutdown();
    }
}
