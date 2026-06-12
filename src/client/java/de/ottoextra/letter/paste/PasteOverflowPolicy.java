package de.ottoextra.letter.paste;

/**
 * Verhalten bei Überlauf: NIE still abschneiden — entweder
 * automatisch neue Seiten oder nur nach bestätigter Vorschau kürzen.
 */
public enum PasteOverflowPolicy {
    AUTO_PAGES,
    TRUNCATE_CONFIRMED
}
