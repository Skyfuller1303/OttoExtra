package de.ottoextra.ui.complete;

import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.horses.AnimalBookStore;
import de.ottoextra.horses.HorseRegistry;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Candidate lists for {@link TabCompletion}, read from data OttoExtra already holds (tab list,
 * people book, region data, Hof records). Nothing here makes a network call, and every method
 * returns an empty list instead of throwing when its source is not ready.
 */
public final class TabCompletionSources {
    private TabCompletionSources() {
    }

    /** Account names of the players currently in the tab list. */
    public static List<String> onlineAccounts() {
        List<String> out = new ArrayList<>();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getNetworkHandler() == null) {
                return out;
            }
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                if (profile != null && profile.name() != null && !profile.name().isBlank()) {
                    out.add(profile.name());
                }
            }
        } catch (Throwable ignored) {
            // Not connected or tab list in flux: no online names.
        }
        return out;
    }

    /** Account names known from the people book. */
    public static List<String> knownAccounts() {
        List<String> out = new ArrayList<>();
        for (LocalRpProfile p : profiles()) {
            if (p.accountName != null && !p.accountName.isBlank()) {
                out.add(p.accountName);
            }
        }
        return out;
    }

    /** RP names known from the people book (without the "Unbekannt" placeholder). */
    public static List<String> rpNames() {
        List<String> out = new ArrayList<>();
        for (LocalRpProfile p : profiles()) {
            if (p.rpName != null && !p.rpName.isBlank() && !LocalRpProfile.UNKNOWN_NAME.equals(p.rpName)) {
                out.add(p.rpName);
            }
        }
        return out;
    }

    /** Account names only (online first, then known), for fields that need a Minecraft account. */
    public static List<String> accounts() {
        List<String> out = onlineAccounts();
        out.addAll(knownAccounts());
        return out;
    }

    /** Everything a person field accepts: online accounts, known accounts and RP names. */
    public static List<String> people() {
        List<String> out = onlineAccounts();
        out.addAll(knownAccounts());
        out.addAll(rpNames());
        return out;
    }

    /** Names of all Gefolge (factions) in the region data. */
    public static List<String> gefolge() {
        List<String> out = new ArrayList<>();
        try {
            RegionDataService data = RegionsServices.data();
            if (data == null) {
                return out;
            }
            for (FactionRecord f : data.allFactions()) {
                if (f != null && f.name() != null && !f.name().isBlank()) {
                    out.add(f.name());
                }
            }
        } catch (Throwable ignored) {
            // Region data not loaded.
        }
        return out;
    }

    /** Names of all Lehen (regions), including local renames. */
    public static List<String> lehen() {
        List<String> out = new ArrayList<>();
        try {
            RegionDataService data = RegionsServices.data();
            if (data == null) {
                return out;
            }
            for (RegionRecord r : data.allRegions()) {
                if (r == null) {
                    continue;
                }
                if (r.name() != null && !r.name().isBlank()) {
                    out.add(r.name());
                }
                if (r.name_override() != null && !r.name_override().isBlank()) {
                    out.add(r.name_override());
                }
            }
        } catch (Throwable ignored) {
            // Region data not loaded.
        }
        return out;
    }

    /** Gefolge and Lehen names together, for fields that search both. */
    public static List<String> gefolgeAndLehen() {
        List<String> out = gefolge();
        out.addAll(lehen());
        return out;
    }

    /** Hof names from the breeding API and the local animal book. */
    public static List<String> hoefe() {
        List<String> out = new ArrayList<>();
        try {
            List<AnimalBreedingFarm> farms = HorseRegistry.farms();
            if (farms != null) {
                for (AnimalBreedingFarm f : farms) {
                    if (f != null && !f.name().isBlank()) {
                        out.add(f.name());
                    }
                }
            }
        } catch (Throwable ignored) {
            // Breeding data not loaded.
        }
        try {
            AnimalBookStore store = AnimalBookStore.current();
            if (store != null) {
                for (AnimalBookStore.Farm f : store.farms()) {
                    if (f != null && !f.deleted() && f.name() != null && !f.name().isBlank()) {
                        out.add(f.name());
                    }
                }
            }
        } catch (Throwable ignored) {
            // Animal book not loaded.
        }
        return out;
    }

    private static List<LocalRpProfile> profiles() {
        try {
            LocalRpIdentityStore store = RpNamesServices.store();
            return store == null ? List.of() : store.all();
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}
