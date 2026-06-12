package de.ottoextra.map;

/**
 * Ein Lehen-Polygon in Welt-Blockkoordinaten (Overworld, [x,z]).
 * Immutable; Zentroid und BoundingBox sind vorberechnet (Culling/Labels).
 *
 * <p>Mehrteilige Lehen: JSON-Keys wie {@code lehen_65#2} sind weitere Teile
 * desselben Lehens — {@link #key()} ist der logische Key ohne Suffix,
 * {@code labelOwner} nur beim Hauptteil (Key ohne {@code #}) true.</p>
 */
public record LehenPolygon(
        String key,          // logisch, z. B. "lehen_7" (Suffix #N entfernt)
        double[] xs,
        double[] zs,
        double centroidX,
        double centroidZ,
        double minX,
        double minZ,
        double maxX,
        double maxZ,
        boolean labelOwner   // nur der Hauptteil trägt Wappen + Namen
) {
    public int pointCount() {
        return xs.length;
    }

    public boolean intersects(double qMinX, double qMinZ, double qMaxX, double qMaxZ) {
        return maxX >= qMinX && minX <= qMaxX && maxZ >= qMinZ && minZ <= qMaxZ;
    }
}
