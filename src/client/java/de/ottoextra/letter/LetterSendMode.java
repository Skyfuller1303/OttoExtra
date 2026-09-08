package de.ottoextra.letter;

import java.util.Locale;

public enum LetterSendMode {
    LEGACY,
    PAGE,
    V2;

    public static LetterSendMode from(String value) {
        if (value == null || value.isBlank()) {
            return PAGE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PAGE;
        }
    }

    public boolean usesServerPageLimits() {
        return this == V2;
    }
}
