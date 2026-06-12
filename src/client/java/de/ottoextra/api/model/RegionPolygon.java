package de.ottoextra.api.model;

import java.util.List;

/** Lokale Lehen-Geometrie für Karten-Overlays (aus lehen_polygons.json). */
public record RegionPolygon(
        String regionId,
        List<PointXZ> points,
        int centroidX,
        int centroidZ
) {
    /** Welt-Koordinatenpunkt (X/Z) eines Polygonzugs. */
    public record PointXZ(int x, int z) {
    }
}
