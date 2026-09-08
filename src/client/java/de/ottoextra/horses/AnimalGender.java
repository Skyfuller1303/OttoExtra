package de.ottoextra.horses;

import java.util.Locale;

/** Shared colors for animal names and outlines; ambiguous data stays uncolored. */
final class AnimalGender {
    static final int FEMALE_COLOR = 0xFF79B8;
    static final int MALE_COLOR = 0x68B8FF;

    private AnimalGender() { }

    static Integer color(String raw) {
        String gender = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        boolean female = gender.contains("♀") || gender.startsWith("weib")
                || gender.startsWith("fem") || gender.equals("stute");
        boolean male = gender.contains("♂") || gender.startsWith("männ")
                || gender.startsWith("maenn") || gender.equals("male")
                || gender.equals("hengst") || gender.equals("wallach");
        return female == male ? null : female ? FEMALE_COLOR : MALE_COLOR;
    }
}
