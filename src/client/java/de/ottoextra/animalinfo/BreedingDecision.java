package de.ottoextra.animalinfo;

/*
 * Behaviour adapted from Viehzucht by yuyukey (MIT), https://github.com/yuyukey/OttonienUtils
 * See THIRD_PARTY_NOTICES.md in the repository root.
 */

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Pure decision logic for recognising a mating: hearts above two animals, then a baby next to them. */
public final class BreedingDecision {

    /** A baby that appears this soon after hearts were seen belongs to those parents. */
    public static final long BIRTH_AFTER_LOVE_MS = 60_000L;
    /** Parents must be at most this far from the baby (blocks). */
    public static final double BIRTH_RANGE = 8.0;
    /** How long a freshly loaded animal is watched for turning out to be a baby (ticks). */
    public static final int WATCH_TICKS = 40;

    /** An adult animal near a newborn. */
    public record Neighbour(UUID uuid, boolean sameSpecies, boolean adult, double distanceSq, long lovedAt) {
    }

    private BreedingDecision() {
    }

    /** The up to two nearest adults of the same species that were in love shortly before the birth. */
    public static List<UUID> parents(List<Neighbour> neighbours, long now) {
        double rangeSq = BIRTH_RANGE * BIRTH_RANGE;
        return neighbours.stream()
                .filter(n -> n.sameSpecies() && n.adult() && n.distanceSq() <= rangeSq
                        && n.lovedAt() > 0 && n.lovedAt() <= now
                        && now - n.lovedAt() < BIRTH_AFTER_LOVE_MS)
                .sorted(Comparator.comparingDouble(Neighbour::distanceSq))
                .limit(2)
                .map(Neighbour::uuid)
                .toList();
    }

    /**
     * Index of the nearest candidate to a particle, or -1. Heart particles are sent by position, so they
     * are attributed to the closest adult animal.
     */
    public static int nearest(double[] distancesSq) {
        int best = -1;
        for (int i = 0; i < distancesSq.length; i++) {
            if (best < 0 || distancesSq[i] < distancesSq[best]) best = i;
        }
        return best;
    }
}
