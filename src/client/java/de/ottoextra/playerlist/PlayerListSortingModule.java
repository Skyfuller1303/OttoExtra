package de.ottoextra.playerlist;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Comparator;

public final class PlayerListSortingModule implements OttoExtraModule {
    private static volatile PlayerListSortingService service;

    @Override
    public String id() {
        return "playerlist";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        // Der Waechter muss auch dann starten, wenn die Funktion beim
        // Clientstart ausgeschaltet ist, damit Aus -> An ohne Neustart klappt.
        return true;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        RegionDataService regions = RegionsServices.data();
        if (regions == null) {
            OttoExtra.LOGGER.warn(
                    "[playerlist] Lehen-Sortierung nicht initialisiert — Regionsdienst fehlt.");
            return;
        }
        TitleRegistry titles = RpNamesServices.titles();
        service = new PlayerListSortingService(context.api(), regions, titles,
                context.config().rpnames,
                () -> context.config().regions.enabled
                        && context.config().rpnames.enabled
                        && context.config().rpnames.tablistSortByRegion);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerListSortingService current = service;
            if (current != null) {
                current.tick();
            }
        });
        DebugLog.debug("[playerlist] Lehen-/Titel-Sortierung initialisiert.");
    }

    @Override
    public void onServerJoin(OttoExtraContext context) {
        PlayerListSortingService current = service;
        if (current != null) {
            current.onServerJoin();
        }
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        deactivate();
    }

    @Override
    public void onClientStop(OttoExtraContext context) {
        deactivate();
        service = null;
    }

    public static Comparator<PlayerListEntry> wrapComparator(
            Comparator<PlayerListEntry> vanilla) {
        PlayerListSortingService current = service;
        return current == null ? vanilla : current.wrap(vanilla);
    }

    public static void onTabOpened() {
        PlayerListSortingService current = service;
        if (current != null) {
            current.onTabOpened();
        }
    }

    private static void deactivate() {
        PlayerListSortingService current = service;
        if (current != null) {
            current.deactivate();
        }
    }
}
