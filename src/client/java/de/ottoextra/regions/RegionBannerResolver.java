package de.ottoextra.regions;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Shared region-first banner selection for map and entry toast. */
final class RegionBannerResolver {
    private RegionBannerResolver() { }

    static Optional<Identifier> resolve(
            RegionDataService data, String regionName,
            BiFunction<String, String, Optional<Identifier>> regionTexture,
            Function<FactionRecord, Optional<Identifier>> factionTexture) {
        if (data == null || regionName == null || regionName.isBlank()) {
            return Optional.empty();
        }
        RegionRecord region = data.regionByName(regionName).orElse(null);
        if (region != null) {
            String path = region.effectiveRegionBannerPath();
            if (path != null && !path.isBlank()) {
                Optional<Identifier> ready = regionTexture.apply(cacheKey(region, regionName), path);
                if (ready.isPresent()) return ready;
            }
        }
        return data.factionForRegion(regionName).flatMap(factionTexture);
    }

    private static String cacheKey(RegionRecord region, String requestedName) {
        // Toast names and polygon IDs must reuse the same downloaded texture.
        String key = region.region_ref();
        if (key == null || key.isBlank()) key = region.id();
        if (key == null || key.isBlank()) key = region.name();
        if (key == null || key.isBlank()) key = requestedName;
        return "region-" + key;
    }
}
