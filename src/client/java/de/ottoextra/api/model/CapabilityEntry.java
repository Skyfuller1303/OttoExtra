package de.ottoextra.api.model;

/** Ein gemappter Kapazitäts-Eintrag (z. B. eine Landwirtschaftsart). */
public record CapabilityEntry(
        String key,
        String label,
        String description
) {
}
