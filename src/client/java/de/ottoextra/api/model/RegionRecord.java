package de.ottoextra.api.model;
import java.util.List;
public record RegionRecord(
        String id,
        String region_ref,
        String region_id,
        String name,
        String name_override,
        String original_name,
        String source,
        FactionRecord current_faction,
        String parent_region_id,
        String parent_region_ref,
        List<String> vassal_region_refs,
        String banner_path,
        String banner_override_path,
        RegionCapabilities region_capabilities,
        RegionInfo region_info
) {
    public String effectiveRegionBannerPath() {
        if (banner_override_path != null && !banner_override_path.isBlank()) {
            return banner_override_path;
        }
        if (banner_path != null && !banner_path.isBlank()) {
            return banner_path;
        }
        return region_info != null ? region_info.suggested_banner_path() : null;
    }
    public boolean hasParentRegion() {
        return (parent_region_id != null && !parent_region_id.isBlank())
                || (parent_region_ref != null && !parent_region_ref.isBlank());
    }
}
