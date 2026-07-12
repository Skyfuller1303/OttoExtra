package de.ottoextra.api.model;

import java.util.List;

public record FactionRecord(
        String uuid,
        String faction_ref,
        String entity_key,
        String name,
        String rank_name,
        String leader_name,
        String lord_name,
        String banner_name,
        String banner_path,
        String media_override_path,
        String source,
        String region_id,
        String region_name,
        int player_count,
        int member_count_raw,
        List<String> vassal_names,
        List<String> vassal_uuids,
        List<String> vassal_recursive_uuids,
        int vassal_count,
        int vassal_recursive_count,
        String description,
        RegionCapabilities region_capabilities
) {

    public String effectiveBannerPath() {
        if (media_override_path != null && !media_override_path.isBlank()) {
            return media_override_path;
        }
        return banner_path;
    }

    public boolean isLanded() {
        boolean hasRegion = (region_name != null && !region_name.isBlank()
                && !"-".equals(region_name.trim()))
                || (region_id != null && !region_id.isBlank());
        return hasRegion && !"Ungelandet".equalsIgnoreCase(rank_name);
    }

    public static FactionRecord better(FactionRecord a, FactionRecord b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.isLanded() != b.isLanded()) {
            return a.isLanded() ? a : b;
        }
        boolean aLord = a.lord_name() != null && !a.lord_name().isBlank();
        boolean bLord = b.lord_name() != null && !b.lord_name().isBlank();
        if (aLord != bLord) {
            return aLord ? a : b;
        }
        return a;
    }
}
