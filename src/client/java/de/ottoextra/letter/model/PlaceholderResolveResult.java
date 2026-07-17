package de.ottoextra.letter.model;
public record PlaceholderResolveResult(LetterPlaceholder placeholder,
                                       String resolved, String source) {
    public boolean ok() {
        return resolved != null && !resolved.isBlank();
    }
}
