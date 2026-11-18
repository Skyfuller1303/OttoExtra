package de.ottoextra.resources;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.map.LehenPolygon;
import de.ottoextra.map.LehenPolygonStore;
import de.ottoextra.regions.RegionsServices;

import java.util.List;
import java.util.Optional;

/**
 * Finds the Gefolge at a position: region polygon of the core mod, then the faction landed on
 * that region.
 */
final class ResourceTerritories {
    private static final String OVERWORLD = "minecraft:overworld";

    private ResourceTerritories() { }

    /** Name of the Gefolge landed on the region at the point, or empty (also outside the overworld). */
    static String nameAt(String dimension, double x, double z) {
        if (!OVERWORLD.equals(dimension)) return "";
        LehenPolygonStore.ensureLoaded();
        String key = regionKeyAt(x, z, LehenPolygonStore.territoryPolygons());
        return key.isEmpty() ? "" : displayName(key);
    }

    /** Smallest containing polygon wins, boundary points count as inside. */
    static String regionKeyAt(double x, double z, List<LehenPolygon> polygons) {
        String best = "";
        double bestArea = Double.POSITIVE_INFINITY;
        for (LehenPolygon polygon : polygons) {
            if (x < polygon.minX() || x > polygon.maxX() || z < polygon.minZ()
                    || z > polygon.maxZ() || !contains(polygon, x, z)) continue;
            double area = area(polygon);
            if (area < bestArea) {
                best = polygon.key();
                bestArea = area;
            }
        }
        return best;
    }

    /**
     * Name of the faction that is landed on the region: the {@code current_faction} of the region
     * record. The faction's own {@code isLanded()}, parent/vassal data and the name-resolving
     * helpers of the core mod are not used, they are unreliable. Empty without a holder.
     */
    private static String displayName(String key) {
        var data = RegionsServices.data();
        if (data == null) return "";
        Optional<RegionRecord> region = data.regionByName(key);
        if (region.isEmpty() || region.get().current_faction() == null) return "";
        FactionRecord holder = region.get().current_faction();
        String fromDetail = "";
        if (holder.uuid() != null && !holder.uuid().isBlank()) {
            fromDetail = data.factionByUuid(holder.uuid()).map(FactionRecord::name).orElse("");
        }
        return cleanFactionName(holder.name(), fromDetail);
    }

    /** The region's own faction name wins, then the full record; placeholders count as none. */
    static String cleanFactionName(String regionFactionName, String recordName) {
        for (String candidate : new String[] {regionFactionName, recordName}) {
            if (candidate == null) continue;
            String name = candidate.strip();
            if (name.isEmpty() || name.equals("-") || name.equalsIgnoreCase("Unbekanntes Gefolge")
                    || name.equalsIgnoreCase("Unknown faction") || name.equalsIgnoreCase("Unbekannt")
                    || name.equalsIgnoreCase("Unknown")) continue;
            return name.length() > ResourceWire.TERRITORY_MAX ? name.substring(0, ResourceWire.TERRITORY_MAX) : name;
        }
        return "";
    }

    private static boolean contains(LehenPolygon polygon, double x, double z) {
        boolean inside = false;
        for (int i = 0, j = polygon.pointCount() - 1; i < polygon.pointCount(); j = i++) {
            double xi = polygon.xs()[i], zi = polygon.zs()[i];
            double xj = polygon.xs()[j], zj = polygon.zs()[j];
            double cross = (x - xj) * (zi - zj) - (z - zj) * (xi - xj);
            if (Math.abs(cross) <= 0.000001 && x >= Math.min(xj, xi) && x <= Math.max(xj, xi)
                    && z >= Math.min(zj, zi) && z <= Math.max(zj, zi)) return true;
            if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) inside = !inside;
        }
        return inside;
    }

    private static double area(LehenPolygon polygon) {
        double sum = 0;
        for (int i = 0, j = polygon.pointCount() - 1; i < polygon.pointCount(); j = i++) {
            sum += polygon.xs()[j] * polygon.zs()[i] - polygon.xs()[i] * polygon.zs()[j];
        }
        return Math.abs(sum) / 2;
    }
}
