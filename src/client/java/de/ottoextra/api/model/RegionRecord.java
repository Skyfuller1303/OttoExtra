package de.ottoextra.api.model;

import java.util.List;

/** Region/Lehen-Gebiet aus der Regions-API. */
public record RegionRecord(
        String id,
        String region_ref,
        String region_id,
        String name,
        String name_override,
        String original_name,
        String source,
        FactionRecord current_faction,
        // Region-Hierarchie (z. B. Fehde-Verband Mährstein): Kinder zeigen auf
        // parent_region_id, der Root listet vassal_region_refs.
        String parent_region_id,
        String parent_region_ref,
        List<String> vassal_region_refs,
        // Banner auf Region-Ebene (fraktionslose Lehen)
        String banner_path,
        String banner_override_path,
        RegionCapabilities region_capabilities,
        RegionInfo region_info
) {
    /** Bevorzugter Banner-Pfad der Region (Override vor Standard). */
    public String effectiveRegionBannerPath() {
        if (banner_override_path != null && !banner_override_path.isBlank()) {
            return banner_override_path;
        }
        return banner_path;
    }

    /** Hat die Region einen Eltern-Verband (Fehde-/Region-Hierarchie)? */
    public boolean hasParentRegion() {
        return (parent_region_id != null && !parent_region_id.isBlank())
                || (parent_region_ref != null && !parent_region_ref.isBlank());
    }
}
