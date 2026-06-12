package de.ottoextra.letter.model;

/**
 * Ausgabe-Modus eines Entwurfs: Brief geht zeilenweise
 * über {@code /letter} + {@code /post}, Verkündung seitenweise an den
 * Discordbot (eine Minecraft-Seite = exakt eine Bot-Nachricht).
 */
public enum LetterOutputMode {
    BRIEF,
    VERKUENDUNG
}
