package de.ottoextra.resources;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chooses which entries the side HUD shows. Pure, so it can be tested without the game. */
final class ResourceHudModel {
    /** Entries without a Lehen only show in "all" mode and only this close to the player. */
    static final double UNASSIGNED_RADIUS = 256.0;

    /**
     * @param shown    rows of the full panel, at most maxRows
     * @param more     matching entries beyond {@code shown}
     * @param ready    all matching ready entries (dots of the compact panel)
     * @param upcoming all matching entries that are still recovering, soonest first
     */
    record Result(List<ResourceSnapshot> shown, int more, List<ResourceSnapshot> ready,
                  List<ResourceSnapshot> upcoming) {
        /** Seconds until the next entry is full, or -1 when nothing is recovering. */
        int nextSeconds() {
            return upcoming.isEmpty() ? -1 : upcoming.getFirst().secondsUntilFull();
        }
    }

    private ResourceHudModel() { }

    /**
     * @param territory Lehen the player stands in, empty when unknown
     * @param showAll   false = only entries of {@code territory} from all lists
     */
    static Result build(List<ResourceSnapshot> snapshots, String dimension, String territory,
                        boolean showAll, double playerX, double playerZ, int maxRows) {
        String current = ResourceRepository.territoryKey(territory);
        List<ResourceSnapshot> matching = new ArrayList<>();
        for (ResourceSnapshot snapshot : snapshots) {
            if (snapshot.markerOnly() || !snapshot.dimension().equals(dimension)) continue;
            String own = ResourceRepository.territoryKey(snapshot.territoryName());
            if (showAll) {
                if (own.isEmpty() && !near(snapshot.zone(), playerX, playerZ)) continue;
            } else if (current.isEmpty() || !own.equals(current)) {
                continue;
            }
            matching.add(snapshot);
        }
        matching.sort(Comparator.comparingInt(ResourceSnapshot::secondsUntilFull)
                .thenComparing(ResourceSnapshot::name, String.CASE_INSENSITIVE_ORDER));
        List<ResourceSnapshot> ready = new ArrayList<>();
        List<ResourceSnapshot> upcoming = new ArrayList<>();
        for (ResourceSnapshot snapshot : matching) {
            if ("READY".equals(snapshot.status())) ready.add(snapshot);
            else upcoming.add(snapshot);
        }
        int rows = Math.max(1, maxRows);
        if (matching.size() <= rows) {
            return new Result(List.copyOf(matching), 0, List.copyOf(ready), List.copyOf(upcoming));
        }
        return new Result(List.copyOf(matching.subList(0, rows)), matching.size() - rows, List.copyOf(ready),
                List.copyOf(upcoming));
    }

    private static boolean near(ResourceZone zone, double playerX, double playerZ) {
        double dx = ResourceMapOverlay.centerX(zone) - playerX;
        double dz = ResourceMapOverlay.centerZ(zone) - playerZ;
        return dx * dx + dz * dz <= UNASSIGNED_RADIUS * UNASSIGNED_RADIUS;
    }
}
