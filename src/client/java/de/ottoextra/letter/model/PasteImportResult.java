package de.ottoextra.letter.model;

/**
 * Ergebnis eines Clipboard-/Buch-Imports: wie viel Text auf wie
 * viele Seiten verteilt wurde; {@code truncated} nur nach bestätigter Kürzung.
 */
public record PasteImportResult(int characters, int pagesCreated, int startPage,
                                boolean truncated) {
}
