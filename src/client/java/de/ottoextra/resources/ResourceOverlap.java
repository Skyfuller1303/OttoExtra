package de.ottoextra.resources;

import java.util.Collection;

/** Overlap rule: when several zones apply to a position, the one with the nearest centre wins. */
final class ResourceOverlap {
    private ResourceOverlap() { }

    /** Entry whose centre is nearest to the point; the first one wins a tie. Null if empty. */
    static ResourceSnapshot nearest(Collection<ResourceSnapshot> candidates, double x, double y, double z) {
        ResourceSnapshot best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (ResourceSnapshot candidate : candidates) {
            ResourceZone zone = candidate.zone();
            double dx = ResourceMapOverlay.centerX(zone) - x;
            double dy = ResourceMapOverlay.centerY(zone) - y;
            double dz = ResourceMapOverlay.centerZ(zone) - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }
}
