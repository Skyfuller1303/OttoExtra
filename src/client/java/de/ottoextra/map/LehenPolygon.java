package de.ottoextra.map;

public record LehenPolygon(
        String key,
        double[] xs,
        double[] zs,
        double centroidX,
        double centroidZ,
        double minX,
        double minZ,
        double maxX,
        double maxZ,
        boolean labelOwner
) {
    public int pointCount() {
        return xs.length;
    }

    public boolean intersects(double qMinX, double qMinZ, double qMaxX, double qMaxZ) {
        return maxX >= qMinX && minX <= qMaxX && maxZ >= qMinZ && minZ <= qMaxZ;
    }
}
