package de.ottoextra.api.model;

import java.util.List;

/** Wirtschaftliche/strukturelle Kapazitäten einer Region (aus public-bootstrap). */
public record RegionCapabilities(
        String id,
        String name,
        String agriculture,
        String mines,
        String bonus,
        Integer fertility,
        List<String> connected_regions,
        Boolean enabled,
        Integer player_gathering,
        MappedCapabilities mapped,
        RegionInfo region_info
) {
}
