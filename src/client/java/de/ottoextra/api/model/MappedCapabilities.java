package de.ottoextra.api.model;

import java.util.List;

public record MappedCapabilities(
        List<CapabilityEntry> agriculture,
        List<CapabilityEntry> mines,
        List<CapabilityEntry> bonus
) {
}
