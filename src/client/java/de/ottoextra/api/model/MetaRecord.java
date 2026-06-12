package de.ottoextra.api.model;

/** Metadaten des API-Envelopes (tolerant; zusätzliche Felder werden ignoriert). */
public record MetaRecord(
        String server_time,
        Long sync_cursor,
        String version
) {
}
