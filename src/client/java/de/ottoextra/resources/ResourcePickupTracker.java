package de.ottoextra.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Correlates nearby result packets and item pickups without adding a second attempt. */
final class ResourcePickupTracker {
    static final long MATCH_WINDOW_MILLIS = 2_000;
    private static final int MAX_PENDING = 64;
    private final List<Pending> pending = new ArrayList<>();
    private final Map<Integer, Long> seenEntities = new HashMap<>();

    void actionbar(ResourceStatistics statistics, ResourceActionbarTracker.Observation observation, long now) {
        prune(now);
        var outcome = statistics.outcome(observation.message());
        boolean candidate = outcome == ResourceStatistics.Outcome.UNKNOWN || outcome == ResourceStatistics.Outcome.SUCCESS;
        if (candidate) {
            for (Pending value : pending.reversed()) {
                if (!value.hasActionbar && matches(value.target, observation.target())) {
                    statistics.attachPickupMessage(value.result, observation.message());
                    value.hasActionbar = true;
                    value.message = observation.message();
                    return;
                }
            }
        }
        var result = statistics.record(observation, now);
        if (candidate) add(new Pending(observation.target(), result, observation.message(), now, true, false));
    }

    void pickup(ResourceStatistics statistics, ResourceActionbarTracker.Target target, int entityId, long now) {
        prune(now);
        // Partial pickup packets for the same item entity are one success, not one per stack item.
        if (seenEntities.putIfAbsent(entityId, now) != null) return;
        Pending match = null;
        for (var outcome : List.of(ResourceStatistics.Outcome.SUCCESS, ResourceStatistics.Outcome.UNKNOWN)) {
            for (Pending value : pending.reversed()) {
                if (!value.hasPickup && matches(value.target, target) && statistics.outcome(value.message) == outcome) {
                    match = value;
                    break;
                }
            }
            if (match != null) break;
        }
        if (match != null) {
            statistics.confirmPickup(match.result);
            match.hasPickup = true;
        } else {
            var result = statistics.record(new ResourceActionbarTracker.Observation(target, ResourceStatistics.PICKUP_MESSAGE), now);
            statistics.confirmPickup(result);
            add(new Pending(target, result, ResourceStatistics.PICKUP_MESSAGE, now, false, true));
        }
    }

    private static boolean matches(ResourceActionbarTracker.Target first, ResourceActionbarTracker.Target second) {
        return first.id().equals(second.id()) && first.dimension().equals(second.dimension())
                && ResourceActionbarTracker.normalize(first.region()).equals(ResourceActionbarTracker.normalize(second.region()));
    }

    private void add(Pending value) {
        if (pending.size() >= MAX_PENDING) pending.removeFirst();
        pending.add(value);
    }

    private void prune(long now) {
        pending.removeIf(value -> now < value.time || now - value.time > MATCH_WINDOW_MILLIS);
        seenEntities.entrySet().removeIf(entry -> now < entry.getValue()
                || now - entry.getValue() > ResourceActionbarTracker.INTERACTION_WINDOW_MILLIS);
    }

    void reset() {
        pending.clear();
        seenEntities.clear();
    }

    private static final class Pending {
        final ResourceActionbarTracker.Target target;
        final ResourceStatistics.RecordedResult result;
        final long time;
        String message;
        boolean hasActionbar;
        boolean hasPickup;

        Pending(ResourceActionbarTracker.Target target, ResourceStatistics.RecordedResult result, String message,
                long time, boolean hasActionbar, boolean hasPickup) {
            this.target = target;
            this.result = result;
            this.message = message;
            this.time = time;
            this.hasActionbar = hasActionbar;
            this.hasPickup = hasPickup;
        }
    }
}
