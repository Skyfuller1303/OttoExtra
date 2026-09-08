package de.ottoextra.map;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionCapabilities;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.api.model.RegionInfo;
import de.ottoextra.regions.RegionDataService;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.Function;

/** Facts for the map card. Political relationships come from the API, not overlay colours. */
record MapSelectionInfo(String title, String regionName, String regionId, String rank,
                        String leader, Integer residents, String realm, String realmRank, String directLord,
                        String description, boolean narrativeOnly, boolean independent, String narrativeKey) {
    static MapSelectionInfo resolve(String key, RegionDataService data) {
        RegionRecord region = data == null ? null : data.regionByName(key).orElse(null);
        FactionRecord faction = data == null ? null : data.factionForRegion(key).orElse(null);
        return from(key, region, faction,
                name -> data == null ? null : data.factionByKey(name).orElse(null),
                name -> data == null ? null : data.regionByName(name).orElse(null));
    }

    static MapSelectionInfo from(String key, RegionRecord region, FactionRecord faction,
                                 Function<String, FactionRecord> factions,
                                 Function<String, RegionRecord> regions) {
        String displayName = first(region == null ? null : region.name(), key.replace("lehen_", "Lehen "));
        String regionName = first(region == null ? null : region.original_name(), displayName);
        String title = first(faction == null ? null : faction.name(), displayName);
        RegionCapabilities caps = region != null && region.region_capabilities() != null
                ? region.region_capabilities() : faction == null ? null : faction.region_capabilities();
        RegionInfo regionInfo = region != null && region.region_info() != null ? region.region_info()
                : caps == null ? null : caps.region_info();
        boolean narrativeOnly = faction == null || "local".equalsIgnoreCase(faction.source())
                || regionInfo != null && (Boolean.FALSE.equals(regionInfo.playable())
                || "NPC-Siedlung".equalsIgnoreCase(regionInfo.status_label()));
        String lord = faction == null ? "" : clean(faction.lord_name());
        boolean independent = !narrativeOnly && faction.lord_name() != null && lord.isBlank()
                && (region == null || !region.hasParentRegion());
        String realm = "";
        String realmRank = "";
        var seen = new HashSet<String>();
        seen.add(title.toLowerCase(Locale.ROOT));
        String next = lord;
        while (!next.isBlank()) {
            if (!seen.add(next.toLowerCase(Locale.ROOT))) {
                realm = ""; // Inconsistent/cyclic data must not invent an overlord.
                realmRank = "";
                if (lord.equalsIgnoreCase(title)) lord = "";
                break;
            }
            realm = next;
            FactionRecord parent = factions.apply(next);
            realmRank = parent == null ? "" : clean(parent.rank_name());
            next = parent == null ? "" : clean(parent.lord_name());
        }
        if (lord.isBlank() && region != null && region.hasParentRegion()) {
            seen.clear();
            RegionRecord current = region;
            seen.add(displayName.toLowerCase(Locale.ROOT));
            while (current.hasParentRegion()) {
                RegionRecord parent = regions.apply(first(current.parent_region_ref(), current.parent_region_id()));
                if (parent == null) break;
                String name = first(parent.current_faction() == null ? null : parent.current_faction().name(), parent.name());
                if (name.isBlank() || !seen.add(clean(parent.name()).toLowerCase(Locale.ROOT))) {
                    realm = "";
                    realmRank = "";
                    break;
                }
                if (lord.isBlank()) lord = name;
                realm = name;
                realmRank = parent.current_faction() == null ? "" : clean(parent.current_faction().rank_name());
                current = parent;
            }
        }
        String description = first(
                region == null || region.region_info() == null ? null : region.region_info().note(),
                caps == null || caps.region_info() == null ? null : caps.region_info().note(),
                faction == null ? null : faction.description());
        String regionId = region == null ? "" : first(region.region_id(), region.id());
        String narrativeKey = "";
        // The supplied Holdern text replaces only the old generic NPC fallback.
        // New API descriptions and player settlements keep their own content.
        if (narrativeOnly && ("27".equals(regionId) || "lehen_27".equals(regionId))) {
            if (title.equalsIgnoreCase("Holdern")) title = "Holdern (Ottonien)";
            if (description.isBlank() || description.equals("Die Stadt Holdern ist Sitz des Herzogs von Ottonien.")) {
                narrativeKey = "ottoextra.map.info.story.holdern";
            }
        }
        return new MapSelectionInfo(title, regionName,
                regionId,
                faction == null ? "" : clean(faction.rank_name()),
                faction == null ? "" : clean(faction.leader_name()),
                narrativeOnly || faction.player_count() < 0 ? null : faction.player_count(),
                realm, realmRank, lord, description, narrativeOnly, independent, narrativeKey);
    }

    private static String first(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) return cleaned;
        }
        return "";
    }

    private static String clean(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) return "";
        return value.trim();
    }
}
