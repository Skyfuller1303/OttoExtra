package de.ottoextra.api.model;

/** Position of the horse at the moment a player edits its registry record. */
public record AnimalBreedingHorseLocation(
        String dimension,
        int x,
        int y,
        int z) {

    public AnimalBreedingHorseLocation {
        dimension = dimension == null ? "" : dimension.trim().toLowerCase();
        if (dimension.isBlank()) {
            throw new IllegalArgumentException("dimension fehlt");
        }
    }
}
