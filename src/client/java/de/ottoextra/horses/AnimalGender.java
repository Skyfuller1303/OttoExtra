package de.ottoextra.horses;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared colors for animal names and outlines; ambiguous data stays uncolored. */
public final class AnimalGender {
    static final int FEMALE_COLOR = 0xFF79B8;
    static final int MALE_COLOR = 0x68B8FF;
    private static final Pattern FEMALE = genderWords("weiblich|female|stute|mare|kuh|sau|henne");
    private static final Pattern MALE = genderWords("männlich|maennlich|male|hengst|stallion|wallach|gelding|bulle|bock|eber|hahn");
    private static final Pattern UNCERTAIN = genderWords("nicht|not|unbekannt|unknown");

    public enum Sex { FEMALE, MALE, UNKNOWN }

    private AnimalGender() { }

    public static Sex parse(String raw) {
        String gender = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (UNCERTAIN.matcher(gender).find()) return Sex.UNKNOWN;
        boolean female = gender.contains("♀") || FEMALE.matcher(gender).find();
        boolean male = gender.contains("♂") || MALE.matcher(gender).find();
        return female == male ? Sex.UNKNOWN : female ? Sex.FEMALE : Sex.MALE;
    }

    public static Integer color(String raw) {
        return switch (parse(raw)) {
            case FEMALE -> FEMALE_COLOR;
            case MALE -> MALE_COLOR;
            case UNKNOWN -> null;
        };
    }

    private static Pattern genderWords(String words) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + words + ")(?![\\p{L}\\p{N}])");
    }
}
