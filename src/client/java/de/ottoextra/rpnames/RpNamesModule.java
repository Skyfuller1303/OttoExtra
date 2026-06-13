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

/**
 * Modul: lokales RP-Bekanntschaftssystem.
 *
 * <p>First seen = lokal "Unbekannt"; RP-Name wird aus Chat-Hover gelernt;
 * lokale/manuelle Daten schlagen API. Chat-Ersetzung via ChatHudMixin,
 * Tabliste via PlayerListEntryMixin — beide laufen über
 * {@link RpNamesServices} und nur auf Ottonien (Server-Gate).</p>
 */
public final class RpNamesModule implements OttoExtraModule {

    /** Tablist-Sync: alle 100 Ticks (5 s) Online-Spieler als gesehen anlegen. */
    private static final int SEEN_SYNC_INTERVAL_TICKS = 100;
    /** Titel-Abgleich aus der Tabliste: alle 600 Ticks (~30 s). */
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

        // Alt-Mods parallel? Dann eigene Ersetzung stilllegen
        boolean legacyPresent = FabricLoader.getInstance().isModLoaded("ottochat_rpnames")
                || FabricLoader.getInstance().isModLoaded("ottotalk")
                || FabricLoader.getInstance().isModLoaded("ottonames");
        if (legacyPresent) {
            OttoExtra.LOGGER.warn(
                    "[rpnames] Legacy-Mod (ottochat_rpnames/ottotalk/ottonames) erkannt - "
                            + "OttoExtra-RP-Namen bleiben deaktiviert (Mixin-Konflikte).");
            return; // setActive bleibt false -> alle Hooks sind No-ops
        }

        // First seen: Tabliste periodisch in den Store synchronisieren
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

        // Hotkey: Personen-Verwaltung (Standard unbelegt — in Steuerung setzen)
        peopleKey = new net.minecraft.client.option.KeyBinding(
                "key.ottoextra.rpnames_people",
                net.minecraft.client.util.InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.option.KeyBinding.Category.MISC);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(peopleKey);

        // Shift-Rechtsklick auf einen Spieler öffnet das RP-Personenbuch
        // (nur wenn aktiviert). Nur Haupthand, um Doppel-Trigger zu vermeiden.
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register(
                (player, world, hand, entity, hitResult) -> {
                    if (!world.isClient() || hand != net.minecraft.util.Hand.MAIN_HAND
                            || !RpNamesServices.openBookOnClickEnabled()
                            || !player.isSneaking()
                            || !(entity instanceof net.minecraft.entity.player.PlayerEntity target)) {
                        return net.minecraft.util.ActionResult.PASS;
                    }
                    String account = target.getGameProfile().name();
                    String uuid = target.getUuid() != null ? target.getUuid().toString() : null;
                    net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                            de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen.openFor(null, account, uuid));
                    return net.minecraft.util.ActionResult.SUCCESS;
                });

        OttoExtra.LOGGER.info("[rpnames] initialisiert (lokales Bekanntschaftssystem, {} Personen).",
                RpNamesServices.store().size());
    }

    private void syncSeenPlayers(MinecraftClient client) {
        try {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                if (profile == null || profile.name() == null || profile.name().isBlank()) {
                    continue;
                }
                if (client.player != null
                        && profile.id() != null
                        && profile.id().equals(client.player.getUuid())) {
                    continue; // sich selbst nicht anlegen
                }
                RpNamesServices.store().ensureSeen(profile.name(),
                        profile.id() != null ? profile.id().toString() : null,
                        RpNameSource.SEEN_TABLIST);
            }
        } catch (Throwable t) {
            OttoExtra.LOGGER.debug("[rpnames] Tablist-Sync-Fehler: {}", t.toString());
        }
    }

    /** Titel der Online-Spieler aus der Tabliste abgleichen (außer lokal gesperrt). */
    private void syncTitlesFromTablist(MinecraftClient client) {
        try {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                if (profile == null || profile.name() == null || profile.name().isBlank()) {
                    continue;
                }
                // ORIGINAL-Displayname vom Server lesen (nicht unseren angepassten)
                net.minecraft.text.Text display =
                        ((de.ottoextra.mixin.PlayerListEntryAccessor) (Object) entry)
                                .ottoextra$rawDisplayName();
                if (display == null) {
                    continue;
                }
                String flat = display.getString();
                int idx = flat.indexOf(profile.name());
                if (idx <= 0) {
                    continue; // kein Titel-Prefix vor dem Account
                }
                String title = flat.substring(0, idx).trim();
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
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        RpNamesServices.setActive(false);
        if (RpNamesServices.store() != null) {
            RpNamesServices.store().saveNow();
        }
    }

    @Override
    public void onClientStop(OttoExtraContext context) {
        RpNamesServices.shutdown();
    }
}
