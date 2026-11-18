package de.ottoextra.animalinfo;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure view model of the compact and detailed toast: colours, progress bar and gender term. */
public record AnimalToastModel(AnimalStatus.Status status, int mast, int mastNeed,
                               Bar bar, float barFraction) {

    public enum Bar { COOLDOWN, GROWTH, MAST, NONE }

    private static final Pattern FRACTION = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    /** Colour ramps {dark, base, light} (ARGB) per status, as in the activity HUD palette. */
    private static final int[] READY = {0xFF2F5426, 0xFF5E9A3E, 0xFF86C25A};
    private static final int[] NEED_MAST = {0xFF6E2226, 0xFFB24147, 0xFFC4545A};
    private static final int[] WAITING = {0xFF96752C, 0xFFCD9F3D, 0xFFDBB451};
    private static final int[] YOUNG = {0xFF214E66, 0xFF349DBA, 0xFF5FB8CF};
    private static final int[] LOVE = {0xFF6E2A6A, 0xFFB03AA6, 0xFFE040D0};
    private static final int[] UNKNOWN = {0xFF3B3A45, 0xFF514F59, 0xFFA9A6B0};

    public static int[] ramp(AnimalStatus.Kind kind) {
        return switch (kind) {
            case READY -> READY;
            case NEED_MAST -> NEED_MAST;
            case WAITING -> WAITING;
            case YOUNG -> YOUNG;
            case IN_LOVE -> LOVE;
            case UNKNOWN -> UNKNOWN;
        };
    }

    /** Ramp for the feeding bar: green once enough is fed, red before. */
    public static int[] mastRamp(boolean enough) {
        return enough ? READY : NEED_MAST;
    }

    /** Name of the status icon in textures/gui/animalinfo, or null for none. */
    public static String statusIcon(AnimalStatus.Kind kind) {
        return switch (kind) {
            case READY, IN_LOVE -> "zucht";
            case WAITING -> "wartet";
            case YOUNG -> "jung";
            case NEED_MAST -> "mast";
            case UNKNOWN -> null;
        };
    }

    public boolean hasMast() {
        return mast >= 0 && mastNeed > 0;
    }

    public boolean mastEnough() {
        return hasMast() && mast >= mastNeed;
    }

    /**
     * @param value   the displayed server answer; supplies the feeding value
     * @param species table values, may be null; supply the feeding requirement and the progress totals
     */
    public static AnimalToastModel of(AnimalStatus.Status status, AnimalInfoSnapshot value,
                                      AnimalSpecies species) {
        int mast = -1;
        int need = -1;
        Matcher matcher = FRACTION.matcher(value.mast());
        if (matcher.find()) {
            try {
                mast = Integer.parseInt(matcher.group(1));
                need = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException tooLarge) {
                mast = -1;
            }
        }
        // The server's own maximum wins; the table only fills in when the server named none.
        if (need <= 0 && species != null && mast >= 0) need = species.breedMast();
        Bar bar = Bar.NONE;
        float fraction = 0f;
        if (status.kind() == AnimalStatus.Kind.WAITING && species != null && status.remainingMs() >= 0) {
            bar = Bar.COOLDOWN;
            fraction = progress(status.remainingMs(), species.cooldownMs());
        } else if (status.kind() == AnimalStatus.Kind.YOUNG && species != null && status.remainingMs() >= 0) {
            bar = Bar.GROWTH;
            fraction = progress(status.remainingMs(), species.growMs());
        } else if (mast >= 0 && need > 0) {
            bar = Bar.MAST;
            fraction = clamp(mast / (float) need);
        }
        return new AnimalToastModel(status, mast, need, bar, fraction);
    }

    private static float progress(long remainingMs, long totalMs) {
        return totalMs <= 0 ? 0f : clamp(1f - remainingMs / (float) totalMs);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /**
     * Species word from the server's gender field ("Kuh (Weiblich)" gives "Kuh", "Hengst" gives "Hengst");
     * empty if the field only says male or female.
     */
    public static String genderTerm(String gender) {
        if (gender == null) return "";
        String word = gender.trim();
        int paren = word.indexOf('(');
        if (paren >= 0) word = word.substring(0, paren).trim();
        String normalized = word.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "weiblich", "männlich", "maennlich", "female", "male", "unbekannt", "unknown" -> "";
            default -> word;
        };
    }
}
