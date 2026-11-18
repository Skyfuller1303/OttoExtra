package de.ottoextra.animalinfo;

/*
 * Behaviour adapted from Viehzucht by yuyukey (MIT), https://github.com/yuyukey/OttonienUtils
 * See THIRD_PARTY_NOTICES.md in the repository root.
 */

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Timers and feeding state of one animal as far as this client knows. All times are wall-clock epoch
 * milliseconds so they keep running while the game is closed. Server information (infoAt) and
 * observations made by the client (love, mating, birth, shearing) are kept apart: an observation that
 * is newer than the last server information wins until the next real query.
 *
 * @param species        animal name from the last server information, empty if unknown
 * @param updatedAt      last change of any kind; drives expiry of the persisted entry
 * @param infoAt         time of the last server information, 0 if none
 * @param mast           current feeding value from the last server information, -1 if unknown
 * @param mastMax        feeding maximum from the last server information, -1 if unknown
 * @param breedReady     the last server information said the animal can breed now
 * @param breedWaitUntil end of a waiting time (cooldown or growth) named by the server, 0 if none
 * @param breedNeed      feeding value the server reported as missing for breeding, -1 if none
 * @param breedNeedMax   feeding value needed for breeding, -1 if none
 * @param shearUntil     end of the shearing wait named by the server, 0 if none
 * @param lovedAt        hearts seen above the animal, 0 if never
 * @param bredAt         mating observed after the last server information, 0 if none
 * @param bornAt         seen as a newborn, 0 if never
 * @param shearedAt      sheared by the player after the last server information, 0 if none
 */
public record AnimalLiveState(UUID entityUuid, String species, long updatedAt, long infoAt,
                              int mast, int mastMax, boolean breedReady, long breedWaitUntil,
                              int breedNeed, int breedNeedMax, long shearUntil,
                              long lovedAt, long bredAt, long bornAt, long shearedAt) {

    private static final Pattern NEED = Pattern.compile("(?i)\\bmast\\s+(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern FRACTION = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern PIPE = Pattern.compile("\\s*\\|\\s*");

    public AnimalLiveState {
        species = species == null ? "" : species;
    }

    public static AnimalLiveState empty(UUID entityUuid) {
        return new AnimalLiveState(entityUuid, "", 0L, 0L, -1, -1, false, 0L, -1, -1, 0L,
                0L, 0L, 0L, 0L);
    }

    /** Applies a complete server answer. Observations made before it are superseded. */
    public AnimalLiveState withInfo(AnimalInfoSnapshot snapshot, long now) {
        int[] feed = fraction(snapshot.mast());
        String breeding = snapshot.breedingReady();
        long wait = 0L;
        for (String piece : PIPE.split(breeding)) {
            long duration = AnimalDurations.parse(piece);
            if (duration > 0) {
                wait = now + duration;
                break;
            }
        }
        boolean ready = wait == 0L && AnimalInfoOverlay.isReady(breeding);
        int need = -1;
        int needMax = -1;
        if (wait == 0L && !ready) {
            Matcher matcher = NEED.matcher(breeding);
            if (matcher.find()) {
                need = Integer.parseInt(matcher.group(1));
                needMax = Integer.parseInt(matcher.group(2));
            }
        }
        long shear = shearUntil;
        if (snapshot.hasShearable()) {
            long duration = AnimalDurations.parse(snapshot.shearable());
            if (duration > 0) {
                shear = now + duration;
            } else if (AnimalInfoOverlay.isPositive(snapshot.shearable())) {
                shear = 0L;
            }
        }
        return new AnimalLiveState(entityUuid,
                snapshot.animalName().isEmpty() ? species : snapshot.animalName(),
                now, now, feed[0], feed[1], ready, wait, need, needMax, shear,
                0L, 0L, bornAt, 0L);
    }

    /** Hearts were seen above the animal. */
    public AnimalLiveState withLoved(long now) {
        return new AnimalLiveState(entityUuid, species, now, infoAt, mast, mastMax, breedReady,
                breedWaitUntil, breedNeed, breedNeedMax, shearUntil, now, bredAt, bornAt, shearedAt);
    }

    /** A baby appeared next to the animal while it was in love: the mating is complete. */
    public AnimalLiveState withBred(long now) {
        return new AnimalLiveState(entityUuid, species, now, infoAt, mast, mastMax, breedReady,
                breedWaitUntil, breedNeed, breedNeedMax, shearUntil, 0L, now, bornAt, shearedAt);
    }

    /** The animal was seen as a newborn. */
    public AnimalLiveState withBorn(long now) {
        return new AnimalLiveState(entityUuid, species, now, infoAt, mast, mastMax, breedReady,
                breedWaitUntil, breedNeed, breedNeedMax, shearUntil, lovedAt, bredAt, now, shearedAt);
    }

    /** The player sheared the sheep and it looked sheared afterwards. */
    public AnimalLiveState withSheared(long now) {
        return new AnimalLiveState(entityUuid, species, now, infoAt, mast, mastMax, breedReady,
                breedWaitUntil, breedNeed, breedNeedMax, shearUntil, lovedAt, bredAt, bornAt, now);
    }

    /**
     * Whether a persisted entry may be kept. Rejects expired entries, future timestamps and values that
     * cannot come from this class.
     */
    boolean valid(long now, long maxAgeMs) {
        return entityUuid != null && species.length() <= 128
                && updatedAt > now - maxAgeMs && updatedAt <= now
                && infoAt >= 0 && infoAt <= now && lovedAt >= 0 && bredAt >= 0
                && bornAt >= 0 && shearedAt >= 0 && lovedAt <= now && bredAt <= now
                && bornAt <= now && shearedAt <= now
                && mast >= -1 && mast <= 100_000 && mastMax >= -1 && mastMax <= 100_000
                && breedNeed >= -1 && breedNeed <= 100_000
                && breedNeedMax >= -1 && breedNeedMax <= 100_000
                && breedWaitUntil >= 0 && breedWaitUntil <= now + 2 * maxAgeMs
                && shearUntil >= 0 && shearUntil <= now + 2 * maxAgeMs;
    }

    private static int[] fraction(String value) {
        Matcher matcher = FRACTION.matcher(value == null ? "" : value);
        if (matcher.find()) {
            try {
                return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
            } catch (NumberFormatException tooLarge) {
                return new int[]{-1, -1};
            }
        }
        return new int[]{-1, -1};
    }
}
