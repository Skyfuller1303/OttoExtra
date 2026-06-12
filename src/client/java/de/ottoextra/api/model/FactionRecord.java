package de.ottoextra.api.model;

import java.util.List;

/**
 * Fraktion/Lehen aus der Regions-API.
 *
 * <p>Feldnamen entsprechen exakt dem JSON (snake_case), damit Gson direkt mappt.
 * Parser sind tolerant gegenüber zusätzlichen, hier nicht gelisteten Feldern.</p>
 */
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
    /** Bevorzugter Banner-Pfad: explizites Override vor Standardpfad. */
    public String effectiveBannerPath() {
        if (media_override_path != null && !media_override_path.isBlank()) {
            return media_override_path;
        }
        return banner_path;
    }
}
