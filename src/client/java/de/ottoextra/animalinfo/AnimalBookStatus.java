package de.ottoextra.animalinfo;

import java.util.UUID;

/**
 * Pure view model of one animal in the animal book (register and overview). It combines what the client
 * knows live ({@link AnimalLiveStore}: server answers and own observations such as matings and births)
 * with the last values stored in the register API, so the status, timers and the feeding bar also work
 * for animals that are not loaded. Times count down from wall-clock time, so a waiting animal's timer keeps
 * running while the book is open.
 */
public final class AnimalBookStatus {

    /** A waiting time this short is shown in the "soon" colour. */
    public static final long SOON_MS = 15 * 60_000L;
    /** Scale of the feeding bar when the server names a requirement up to this value. */
    public static final int MAST_SCALE = 100;

    /** Colour class of the status on parchment. */
    public enum Tone { READY, WAITING, SOON, NEED_MAST, YOUNG, LOVE, UNKNOWN }

    /** Age of an animal: still growing (with the time left when known), adult, or not known. */
    public enum AgeKind { YOUNG, ADULT, UNKNOWN }

    public record Age(AgeKind kind, long remainingMs, boolean estimated) {
    }

    /**
     * @param status      breeding status
     * @param tone        colour class
     * @param progress    fill of the status bar, 0..1
     * @param mast        current feeding value, -1 if unknown
     * @param mastNeed    feeding value needed for breeding, -1 if unknown
     * @param mastScale   full width of the feeding bar
     * @param ageMs       age of the information, null if unknown
     * @param species     table values, null if the species is unknown
     * @param age         growth state
     */
    public record View(AnimalStatus.Status status, Tone tone, float progress, int mast, int mastNeed,
                       int mastScale, Long ageMs, AnimalSpecies species, Age age) {

        public boolean hasMast() {
            return mast >= 0 && mastNeed > 0;
        }

        public boolean mastEnough() {
            return hasMast() && mast >= mastNeed;
        }
    }

    private AnimalBookStatus() {
    }

    /** Live view of an animal from the current store and its register values. */
    public static View of(UUID uuid, String species, String mast, String breedingReady, long observedAt,
                          Boolean baby, long now) {
        return of(AnimalLiveStore.current().get(uuid), uuid, species, mast, breedingReady, observedAt, baby, now);
    }

    /**
     * @param live          live state of the animal, may be null
     * @param species       species name of the register record ("Reitpferd"), may be empty
     * @param mast          register value "x/y", may be empty
     * @param breedingReady register value ("Bereit", "in 3h | Mast 8/100"), may be empty
     * @param observedAt    when the register values were observed, 0 if unknown
     * @param baby          true/false when the entity is loaded, null when unknown
     */
    public static View of(AnimalLiveState live, UUID uuid, String species, String mast, String breedingReady,
                          long observedAt, Boolean baby, long now) {
        AnimalLiveState state = merge(live, uuid, species, mast, breedingReady, observedAt, now);
        AnimalSpecies table = AnimalSpecies.resolve(state == null ? "" : state.species(), species);
        boolean young = Boolean.TRUE.equals(baby) || baby == null && growing(state, table, now);
        AnimalStatus.Status status = AnimalStatus.compute(state, table, young, now);
        int mastValue = state == null ? -1 : state.mast();
        int need = state == null ? -1 : state.breedNeedMax() > 0 ? state.breedNeedMax() : state.mastMax();
        if (status.kind() == AnimalStatus.Kind.NEED_MAST && status.mastNeeded() > 0) {
            mastValue = status.mast();
            need = status.mastNeeded();
        }
        if (need <= 0 && table != null && mastValue >= 0) need = table.breedMast();
        int scale = Math.max(MAST_SCALE, Math.max(need, mastValue));
        Long ageMs = state != null && state.infoAt() > 0 && (observedAt > 0 || live != null && live.infoAt() > 0)
                ? Math.max(0L, now - state.infoAt()) : null;
        return new View(status, tone(status), progress(status, table, mastValue, need), mastValue, need, scale,
                ageMs, table, age(status, state, table, baby, now));
    }

    /** Uses the newer of live state and register values; own observations newer than both are kept. */
    static AnimalLiveState merge(AnimalLiveState live, UUID uuid, String species, String mast, String breedingReady,
                                 long observedAt, long now) {
        boolean registerData = !blank(mast) || !blank(breedingReady);
        if (live != null && (live.infoAt() > 0 && live.infoAt() >= observedAt || !registerData)) return live;
        if (!registerData) return null;
        long at = observedAt > 0 && observedAt <= now ? observedAt : now;
        AnimalInfoSnapshot snapshot = new AnimalInfoSnapshot(species, "", mast, breedingReady, "", "");
        AnimalLiveState base = AnimalLiveState.empty(uuid).withInfo(snapshot, at);
        if (live == null) return base;
        return new AnimalLiveState(uuid, base.species().isEmpty() ? live.species() : base.species(),
                Math.max(base.updatedAt(), live.updatedAt()), base.infoAt(), base.mast(), base.mastMax(),
                base.breedReady(), base.breedWaitUntil(), base.breedNeed(), base.breedNeedMax(), base.shearUntil(),
                live.lovedAt() > at ? live.lovedAt() : 0L, live.bredAt() > at ? live.bredAt() : 0L,
                live.bornAt(), live.shearedAt() > at ? live.shearedAt() : 0L);
    }

    private static boolean growing(AnimalLiveState state, AnimalSpecies species, long now) {
        return state != null && species != null && state.bornAt() > 0 && state.bornAt() + species.growMs() > now;
    }

    static Tone tone(AnimalStatus.Status status) {
        return switch (status.kind()) {
            case READY -> Tone.READY;
            case WAITING -> status.remainingMs() >= 0 && status.remainingMs() <= SOON_MS ? Tone.SOON : Tone.WAITING;
            case NEED_MAST -> Tone.NEED_MAST;
            case YOUNG -> Tone.YOUNG;
            case IN_LOVE -> Tone.LOVE;
            case UNKNOWN -> Tone.UNKNOWN;
        };
    }

    static float progress(AnimalStatus.Status status, AnimalSpecies species, int mast, int need) {
        return switch (status.kind()) {
            case READY, IN_LOVE -> 1f;
            case WAITING -> species == null || status.remainingMs() < 0 ? 0f
                    : clamp(1f - status.remainingMs() / (float) species.cooldownMs());
            case YOUNG -> species == null || status.remainingMs() < 0 ? 0f
                    : clamp(1f - status.remainingMs() / (float) species.growMs());
            case NEED_MAST, UNKNOWN -> mast >= 0 && need > 0 ? clamp(mast / (float) need) : 0f;
        };
    }

    static Age age(AnimalStatus.Status status, AnimalLiveState state, AnimalSpecies species, Boolean baby,
                   long now) {
        boolean growing = growing(state, species, now);
        if (Boolean.TRUE.equals(baby) || baby == null && growing) {
            if (status.kind() == AnimalStatus.Kind.YOUNG && status.remainingMs() >= 0) {
                return new Age(AgeKind.YOUNG, status.remainingMs(), status.estimated());
            }
            if (growing) return new Age(AgeKind.YOUNG, state.bornAt() + species.growMs() - now, true);
            return new Age(AgeKind.YOUNG, -1, false);
        }
        if (Boolean.FALSE.equals(baby)) return new Age(AgeKind.ADULT, -1, false);
        if (state != null && species != null && state.bornAt() > 0) return new Age(AgeKind.ADULT, -1, false);
        return new Age(AgeKind.UNKNOWN, -1, false);
    }

    // ---- wording (translation keys and their arguments) --------------------------------------

    /** Translation key of the short status of a row ("zuchtbereit", "wartet %s", ...). */
    public static String statusKey(AnimalStatus.Status status) {
        return switch (status.kind()) {
            case READY -> "ottoextra.horses.book.status.ready";
            case WAITING -> status.remainingMs() >= 0 ? "ottoextra.horses.book.status.waiting_timer"
                    : "ottoextra.horses.book.status.waiting";
            case NEED_MAST -> "ottoextra.horses.book.status.need_mast";
            case YOUNG -> status.remainingMs() >= 0 ? "ottoextra.horses.book.status.young_timer"
                    : "ottoextra.horses.book.status.young";
            case IN_LOVE -> "ottoextra.horses.book.status.love";
            case UNKNOWN -> "ottoextra.horses.book.status.unknown";
        };
    }

    /** Arguments of {@link #statusKey}: the live timer or the feeding values. */
    public static Object[] statusArgs(AnimalStatus.Status status) {
        return switch (status.kind()) {
            case WAITING, YOUNG -> status.remainingMs() >= 0
                    ? new Object[]{timer(status.remainingMs(), status.estimated())} : new Object[0];
            case NEED_MAST -> new Object[]{status.mast(), status.mastNeeded()};
            default -> new Object[0];
        };
    }

    /** Translation key of the age line. */
    public static String ageKey(Age age) {
        return switch (age.kind()) {
            case YOUNG -> age.remainingMs() >= 0 ? "ottoextra.horses.book.age.young_timer"
                    : "ottoextra.horses.book.age.young";
            case ADULT -> "ottoextra.horses.book.age.adult";
            case UNKNOWN -> "ottoextra.horses.book.age.unknown";
        };
    }

    public static Object[] ageArgs(Age age) {
        return age.kind() == AgeKind.YOUNG && age.remainingMs() >= 0
                ? new Object[]{timer(age.remainingMs(), age.estimated())} : new Object[0];
    }

    /** "3h 20m", with "~" for an estimate from the table. */
    public static String timer(long remainingMs, boolean estimated) {
        return (estimated ? "~" : "") + AnimalDurations.format(remainingMs);
    }

    /** "5m", "2h 3m": age of a piece of information. */
    public static String dataAge(long ageMs) {
        return AnimalDurations.formatAge(ageMs);
    }

    /** Table waiting time of a species, "24h" or "40m". */
    public static String tableWait(AnimalSpecies species) {
        return species == null ? "" : AnimalDurations.formatAge(species.cooldownMs());
    }

    /** True when a shearing or milking value means "now possible". */
    public static boolean positive(String value) {
        return AnimalInfoOverlay.isPositive(value);
    }

    /** Remaining time of a value such as "in 3h 20m", or -1. */
    public static long duration(String value) {
        return AnimalDurations.parse(value);
    }

    /** Sort rank: ready first, then soon, waiting, needs feed, young, in love, unknown. */
    public static int rank(View view) {
        return switch (view.tone()) {
            case READY -> 0;
            case SOON -> 1;
            case WAITING -> 2;
            case NEED_MAST -> 3;
            case LOVE -> 4;
            case YOUNG -> 5;
            case UNKNOWN -> 6;
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
