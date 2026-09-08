package de.ottoextra.horses;

import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.map.LehenPolygon;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/** UI preflight for initial naming; the server still authorizes every save. */
final class AnimalNamingTerritory {
    private static final Pattern REGION_REF = Pattern.compile("(?:lehen_)?([0-9]+)");

    private AnimalNamingTerritory() {
    }

    static boolean allows(PlayerRecord player, Function<String, FactionRecord> factions,
                          AnimalBreedingHorseLocation location, List<LehenPolygon> polygons) {
        if (player == null || player.faction() == null || player.faction().isBlank()
                || location == null || !"minecraft:overworld".equals(location.dimension())) return false;
        String ownRegion = canonicalRegion(player.faction());
        if (ownRegion.isEmpty()) {
            FactionRecord faction = factions.apply(player.faction());
            ownRegion = faction == null ? "" : canonicalRegion(faction.region_id());
            if (ownRegion.isEmpty() && faction != null) {
                // Region current_faction summaries carry faction_ref instead of region_id.
                ownRegion = canonicalRegion(faction.faction_ref());
            }
        }
        return !ownRegion.isEmpty() && ownRegion.equals(regionAt(location.x(), location.z(), polygons));
    }

    /** Matches the server: use the smallest containing Lehen, including boundary points. */
    static String regionAt(double x, double z, List<LehenPolygon> polygons) {
        String best = "";
        double bestArea = Double.POSITIVE_INFINITY;
        for (LehenPolygon polygon : polygons) {
            if (x < polygon.minX() || x > polygon.maxX() || z < polygon.minZ() || z > polygon.maxZ()) continue;
            String region = canonicalRegion(polygon.key());
            if (region.isEmpty() || !contains(polygon, x, z)) continue;
            double area = area(polygon);
            if (area < bestArea) {
                best = region;
                bestArea = area;
            }
        }
        return best;
    }

    private static String canonicalRegion(String raw) {
        if (raw == null) return "";
        String key = raw.trim().toLowerCase(Locale.ROOT).split("#", 2)[0];
        var match = REGION_REF.matcher(key);
        if (!match.matches()) return "";
        return "lehen_" + match.group(1).replaceFirst("^0+(?!$)", "");
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
