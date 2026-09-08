package de.ottoextra.regions;

import de.ottoextra.api.OttoExtraApiClient;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RegionsServices {

    private static volatile RegionDataService data;
    private static volatile BannerTextureService banners;

    private RegionsServices() {
    }

    public static synchronized void init(OttoExtraApiClient api,
                                         int activityRefreshSeconds) {
        if (data != null && banners != null) {
            return;
        }
        data = new RegionDataService(api, activityRefreshSeconds);
        banners = new BannerTextureService(api);
    }

    public static void onServerJoin() {
        RegionDataService d = data;
        if (d != null) {
            d.onServerJoin();
        }
        BannerTextureService b = banners;
        if (b != null) {
            b.onServerJoin();
        }
    }

    public static void onDisconnect() {
        RegionDataService d = data;
        if (d != null) {
            d.onDisconnect();
        }
    }

    public static synchronized void shutdown() {
        RegionDataService d = data;
        if (d != null) {
            d.shutdown();
        }
        data = null;
        banners = null;
    }

    public static RegionDataService data() {
        return data;
    }

    public static BannerTextureService banners() {
        return banners;
    }

    /** Aktuelle Eigennamen aus dem API-Bootstrap fuer Uebersetzungsfilter. */
    public static Set<String> translationProtectedNames() {
        RegionDataService service = data;
        if (service == null) return Set.of();
        Set<String> names = new LinkedHashSet<>();
        service.allFactions().forEach(faction -> {
            addName(names, faction.name());
            addName(names, faction.region_name());
            addName(names, faction.leader_name());
            addName(names, faction.lord_name());
            addName(names, faction.banner_name());
            if (faction.vassal_names() != null) {
                faction.vassal_names().forEach(value -> addName(names, value));
            }
        });
        service.allRegions().forEach(region -> {
            addName(names, region.name());
            addName(names, region.name_override());
            addName(names, region.original_name());
        });
        return Set.copyOf(names);
    }

    private static void addName(Set<String> names, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.length() < 2 || "-".equals(clean)
                || "Unbekannt".equalsIgnoreCase(clean)
                || "Unknown".equalsIgnoreCase(clean)
                || "Ungelandet".equalsIgnoreCase(clean)) return;
        names.add(clean);
    }
}
