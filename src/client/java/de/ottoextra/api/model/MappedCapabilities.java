package de.ottoextra.api.model;

import java.util.List;

/** Aufbereitete (gemappte) Kapazitäten einer Region. */
public record MappedCapabilities(
        List<CapabilityEntry> agriculture,
        List<CapabilityEntry> mines,
        List<CapabilityEntry> bonus
) {
}
