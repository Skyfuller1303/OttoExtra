package de.ottoextra.resources;

/**
 * One resource entry as stored locally and sent to the list service.
 *
 * @param symbol own symbol of a marker ({@link MarkerSymbols} key), shared with the list; null for
 *               the default pin and always null for other kinds and for unknown values
 */
record ZoneRecord(
        String id,
        String name,
        String type,
        String dimension,
        String territory,
        ResourceZone zone,
        Integer customMinutes,
        String note,
        long updatedAt,
        boolean deleted,
        String symbol) {

    ZoneRecord {
        symbol = normalizeSymbol(type, symbol);
    }

    /** Entry without an own marker symbol. */
    ZoneRecord(String id, String name, String type, String dimension, String territory, ResourceZone zone,
               Integer customMinutes, String note, long updatedAt, boolean deleted) {
        this(id, name, type, dimension, territory, zone, customMinutes, note, updatedAt, deleted, null);
    }

    /**
     * The symbol a record keeps: a known key on a marker, otherwise null. Old shape names are
     * replaced by the new ones; the default marker is null too.
     */
    static String normalizeSymbol(String type, String symbol) {
        symbol = MarkerSymbols.canonical(symbol);
        if (!ResourcePolicy.isMarker(type) || !MarkerSymbols.valid(symbol) || MarkerSymbols.DEFAULT.equals(symbol)) {
            return null;
        }
        return symbol;
    }

    ZoneRecord withUpdatedAt(long value) {
        return new ZoneRecord(id, name, type, dimension, territory, zone, customMinutes, note,
                value, deleted, symbol);
    }

    ZoneRecord withSymbol(String value, long updated) {
        return new ZoneRecord(id, name, type, dimension, territory, zone, customMinutes, note,
                updated, deleted, value);
    }

    ZoneRecord tombstone(long value) {
        return new ZoneRecord(id, name, type, dimension, territory, zone, customMinutes, note,
                value, true, symbol);
    }
}
