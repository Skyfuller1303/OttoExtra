package de.ottoextra.resources;

/**
 * Points of a zone outline for particles: a ring for a point zone, the bottom edges for a
 * cuboid. Pure maths so the spacing and the cap can be tested without the game.
 */
final class ResourceOutline {
    static final int MAX_POINTS = 160;
    private static final int MIN_RING_POINTS = 8;

    private ResourceOutline() { }

    /** Flat array x0, z0, x1, z1, ... of a ring, about one block apart, at most {@code cap} points. */
    static double[] ring(double centerX, double centerZ, double radius, int cap) {
        int count = Math.clamp(Math.round(2 * Math.PI * radius), MIN_RING_POINTS, Math.max(MIN_RING_POINTS, cap));
        double[] points = new double[count * 2];
        for (int index = 0; index < count; index++) {
            double angle = 2 * Math.PI * index / count;
            points[2 * index] = centerX + Math.cos(angle) * radius;
            points[2 * index + 1] = centerZ + Math.sin(angle) * radius;
        }
        return points;
    }

    /** Flat x/z array along the rectangle edge, starting at the min corner, about one block apart. */
    static double[] boxEdges(double minX, double minZ, double maxX, double maxZ, int cap) {
        double width = maxX - minX;
        double depth = maxZ - minZ;
        double perimeter = 2 * (width + depth);
        int count = (int) Math.clamp(Math.round(perimeter), 4, Math.max(4, cap));
        double step = perimeter / count;
        double[] points = new double[count * 2];
        for (int index = 0; index < count; index++) {
            double t = index * step;
            double x;
            double z;
            if (t < width) {
                x = minX + t;
                z = minZ;
            } else if (t < width + depth) {
                x = maxX;
                z = minZ + (t - width);
            } else if (t < 2 * width + depth) {
                x = maxX - (t - width - depth);
                z = maxZ;
            } else {
                x = minX;
                z = maxZ - (t - 2 * width - depth);
            }
            points[2 * index] = x;
            points[2 * index + 1] = z;
        }
        return points;
    }

    /** Outline points of a zone in world coordinates (block edges for cuboids). */
    static double[] of(ResourceZone zone) {
        if (zone.type() == ResourceZone.Type.POINT) {
            return ring(zone.x() + 0.5, zone.z() + 0.5, zone.radius(), MAX_POINTS);
        }
        return boxEdges(zone.minX(), zone.minZ(), zone.maxX() + 1.0, zone.maxZ() + 1.0, MAX_POINTS);
    }

    /** Height of the particles: just above the centre of a point, or the bottom of a cuboid. */
    static double height(ResourceZone zone) {
        return (zone.type() == ResourceZone.Type.POINT ? zone.y() : zone.minY()) + 0.1;
    }
}
