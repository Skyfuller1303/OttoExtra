package de.ottoextra.animalinfo;

/*
 * Behaviour adapted from Viehzucht by yuyukey (MIT), https://github.com/yuyukey/OttonienUtils
 * See THIRD_PARTY_NOTICES.md in the repository root.
 */

/** Pure decision logic: what an animal is waiting for, and which glow colour that earns it. */
public final class AnimalStatus {

    /** How long an animal counts as in love after hearts were seen. */
    public static final long LOVE_MS = 30_000L;

    /** Glow colour of an animal that is waiting to breed again. Orchid, distinct from female pink and male blue. */
    public static final int BREEDING_COLOR = 0xE040D0;
    /** Glow colour of a sheep that is waiting to be sheared again. */
    public static final int SHEARED_COLOR = 0xFFC94A;

    public enum Kind { IN_LOVE, YOUNG, WAITING, NEED_MAST, READY, UNKNOWN }

    /**
     * @param remainingMs time left for WAITING and YOUNG, -1 if unknown
     * @param estimated   the time comes from the table, not from the server
     * @param mast        for NEED_MAST: current feeding value
     * @param mastNeeded  for NEED_MAST: feeding value that is required
     */
    public record Status(Kind kind, long remainingMs, boolean estimated, int mast, int mastNeeded) {
    }

    /** What the glow should show. */
    public enum Glow { NONE, GENDER_ONLY, BREEDING, SHEARED }

    private AnimalStatus() {
    }

    /**
     * End of the breeding wait. A mating observed after the last server information is estimated from the
     * table; otherwise the server's own wait counts. 0 if the animal is not waiting.
     */
    public static long breedWaitUntil(AnimalLiveState state, AnimalSpecies species) {
        if (state == null) return 0L;
        if (state.bredAt() > state.infoAt() && species != null) {
            return state.bredAt() + species.cooldownMs();
        }
        return state.breedWaitUntil();
    }

    /** True if {@link #breedWaitUntil} is an estimate from the table. */
    public static boolean breedWaitEstimated(AnimalLiveState state, AnimalSpecies species) {
        return state != null && species != null && state.bredAt() > state.infoAt();
    }

    /** End of the shearing wait: our own shearing after the last information, else the server's value. */
    public static long shearWaitUntil(AnimalLiveState state) {
        if (state == null) return 0L;
        if (state.shearedAt() > state.infoAt()) return state.shearedAt() + AnimalSpecies.SHEAR_COOLDOWN_MS;
        return state.shearUntil();
    }

    public static Status compute(AnimalLiveState state, AnimalSpecies species, boolean baby, long now) {
        if (state == null) return new Status(Kind.UNKNOWN, -1, false, -1, -1);
        if (!baby && state.lovedAt() > 0 && now - state.lovedAt() < LOVE_MS) {
            return new Status(Kind.IN_LOVE, -1, false, -1, -1);
        }
        long waitUntil = breedWaitUntil(state, species);
        boolean estimated = breedWaitEstimated(state, species);
        if (baby) {
            if (waitUntil > now) return new Status(Kind.YOUNG, waitUntil - now, estimated, -1, -1);
            if (species != null && state.bornAt() > state.infoAt()
                    && state.bornAt() + species.growMs() > now) {
                return new Status(Kind.YOUNG, state.bornAt() + species.growMs() - now, true, -1, -1);
            }
            return new Status(Kind.YOUNG, -1, false, -1, -1);
        }
        if (waitUntil > now) return new Status(Kind.WAITING, waitUntil - now, estimated, -1, -1);
        if (state.breedReady() || state.bredAt() > state.infoAt() && waitUntil > 0) {
            return new Status(Kind.READY, -1, false, -1, -1);
        }
        if (state.breedNeed() >= 0 && state.breedNeedMax() > 0) {
            return new Status(Kind.NEED_MAST, -1, false, state.breedNeed(), state.breedNeedMax());
        }
        // The server gave no requirement: compare the feeding value with the table.
        int required = state.mastMax() > 0 ? state.mastMax() : species != null ? species.breedMast() : -1;
        if (state.infoAt() > 0 && !state.breedReady() && state.mast() >= 0 && state.mast() < required) {
            return new Status(Kind.NEED_MAST, -1, state.mastMax() <= 0, state.mast(), required);
        }
        return new Status(Kind.UNKNOWN, -1, false, -1, -1);
    }

    /** Which glow an animal gets. Sheared sheep come first, then bred animals; both beat the gender colour. */
    public static Glow glow(boolean shearEnabled, boolean breedingEnabled, boolean sheep, boolean baby,
                            AnimalLiveState state, AnimalSpecies species, long now) {
        if (state == null) return Glow.GENDER_ONLY;
        if (shearEnabled && sheep && !baby && shearWaitUntil(state) > now) return Glow.SHEARED;
        if (breedingEnabled && !baby && breedWaitUntil(state, species) > now) return Glow.BREEDING;
        return Glow.GENDER_ONLY;
    }

    public static int color(Glow glow) {
        return switch (glow) {
            case BREEDING -> BREEDING_COLOR;
            case SHEARED -> SHEARED_COLOR;
            default -> -1;
        };
    }
}
