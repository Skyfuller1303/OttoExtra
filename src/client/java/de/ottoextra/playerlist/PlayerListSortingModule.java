package de.ottoextra.playerlist;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.title.TitleRegistry;
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
        return config.regions.enabled && config.rpnames.enabled;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        RegionDataService regions = RegionsServices.data();
        TitleRegistry titles = RpNamesServices.titles();
        if (regions == null || titles == null) {
            OttoExtra.LOGGER.warn("[playerlist] Lehen-Sortierung nicht initialisiert — Dienste fehlen.");
            return;
        }
        service = new PlayerListSortingService(context.api(), regions, titles,
                context.config().rpnames);
        OttoExtra.LOGGER.info("[playerlist] Lehen-/Titel-Sortierung initialisiert.");
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

    private static void deactivate() {
        PlayerListSortingService current = service;
        if (current != null) {
            current.deactivate();
        }
    }
}
