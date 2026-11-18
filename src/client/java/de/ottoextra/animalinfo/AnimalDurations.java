package de.ottoextra.animalinfo;

/*
 * Behaviour adapted from Viehzucht by yuyukey (MIT), https://github.com/yuyukey/OttonienUtils
 * See THIRD_PARTY_NOTICES.md in the repository root.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats the durations Ottonien writes ("1d 16h", "22m 3s", "5h"). */
final class AnimalDurations {

    private static final Pattern DURATION =
            Pattern.compile("\\b\\d+[dhms]\\b(?:\\s+\\d+[dhms]\\b)*");
    private static final Pattern PART = Pattern.compile("(\\d+)([dhms])");
    private static final long MAX_MS = 400L * 86_400_000L;

    private AnimalDurations() {
    }

    /** Milliseconds of the first duration in the text, or -1 if there is none. */
    static long parse(String text) {
        if (text == null) return -1;
        Matcher block = DURATION.matcher(text);
        if (!block.find()) return -1;
        long ms = 0;
        Matcher part = PART.matcher(block.group());
        while (part.find()) {
            long amount = Long.parseLong(part.group(1));
            ms += switch (part.group(2)) {
                case "d" -> amount * 86_400_000L;
                case "h" -> amount * 3_600_000L;
                case "m" -> amount * 60_000L;
                default -> amount * 1_000L;
            };
            if (ms > MAX_MS) return MAX_MS;
        }
        return ms;
    }

    /** "1d 16h", "3h 20m", "5m 3s" or "42s"; rounds up so a running timer never shows 0s early. */
    static String format(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        long d = s / 86_400;
        long h = s % 86_400 / 3_600;
        long m = s % 3_600 / 60;
        long sec = s % 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + sec + "s";
        return sec + "s";
    }

    /** Age of a piece of information; rounds down and drops the seconds once it is a minute old. */
    static String formatAge(long ms) {
        long s = Math.max(0, ms / 1000);
        long d = s / 86_400;
        long h = s % 86_400 / 3_600;
        long m = s % 3_600 / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m";
        return s + "s";
    }
}
