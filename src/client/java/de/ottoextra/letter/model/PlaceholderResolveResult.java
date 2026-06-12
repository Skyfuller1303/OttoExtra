package de.ottoextra.letter.model;

/**
 * Aufgelöster Platzhalter: {@code resolved} null = unauflösbar
 * (Spieler unbekannt / Typ ohne Daten) — Editor markiert das rot.
 */
public record PlaceholderResolveResult(LetterPlaceholder placeholder,
                                       String resolved, String source) {
    public boolean ok() {
        return resolved != null && !resolved.isBlank();
    }
}
