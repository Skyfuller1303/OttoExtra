package de.ottoextra.resources;

/** Timer of one resource entry. {@code depletedAt} is epoch milliseconds, null after a reset. */
record TimerRecord(Long depletedAt, String by, long updatedAt) {
}
