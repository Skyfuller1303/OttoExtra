package de.ottoextra.map;
import java.util.List;
public record BorderSegment(
        double x1,
        double z1,
        double x2,
        double z2,
        List<String> ownerKeys
) {
    public boolean intersects(double qMinX, double qMinZ, double qMaxX, double qMaxZ) {
        return Math.max(x1, x2) >= qMinX && Math.min(x1, x2) <= qMaxX
                && Math.max(z1, z2) >= qMinZ && Math.min(z1, z2) <= qMaxZ;
    }
}
