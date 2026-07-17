package de.ottoextra.api.model;
import java.util.List;
public record RegionPolygon(
        String regionId,
        List<PointXZ> points,
        int centroidX,
        int centroidZ
) {
    public record PointXZ(int x, int z) {
    }
}
