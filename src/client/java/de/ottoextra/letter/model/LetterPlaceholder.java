package de.ottoextra.letter.model;

/**
 * Ein RP-Platzhalter im Text: {@code {{name:Spieler}}} usw.
 * Offsets beziehen sich auf den Seitentext (inklusive Klammern).
 */
public record LetterPlaceholder(String raw, String type, String playerName,
                                int start, int end) {
}
