package de.ottoextra.api.model;
public record MetaRecord(
        String server_time,
        Long sync_cursor,
        String version
) {
}
