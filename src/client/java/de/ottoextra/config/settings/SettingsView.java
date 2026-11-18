package de.ottoextra.config.settings;

/**
 * Which of the two settings views is used. Free of Minecraft classes so the decision can be
 * unit-tested; sizes are scaled GUI pixels.
 */
public final class SettingsView {

    public static final String BOOK = "book";
    public static final String CLASSIC = "classic";

    /** Smallest scaled window in which the book layout is laid out properly. */
    public static final int BOOK_MIN_WIDTH = 440;
    public static final int BOOK_MIN_HEIGHT = 240;

    private SettingsView() {
    }

    /** Known preference, or {@link #BOOK} for null and unknown values. */
    public static String normalize(String preference) {
        return CLASSIC.equals(preference) ? CLASSIC : BOOK;
    }

    public static boolean bookFits(int scaledWidth, int scaledHeight) {
        return scaledWidth >= BOOK_MIN_WIDTH && scaledHeight >= BOOK_MIN_HEIGHT;
    }

    /** The view to open for a stored preference and the current window size. */
    public static String choose(String preference, int scaledWidth, int scaledHeight) {
        return BOOK.equals(normalize(preference)) && bookFits(scaledWidth, scaledHeight)
                ? BOOK : CLASSIC;
    }

    public static String other(String view) {
        return BOOK.equals(normalize(view)) ? CLASSIC : BOOK;
    }
}
