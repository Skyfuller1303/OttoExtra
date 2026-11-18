package de.ottoextra.resources;

/** Read-only view of one entry at one point in time; rebuilt about once per second. */
public record ResourceSnapshot(
        String id,
        String listId,
        String listName,
        String name,
        String type,
        String iconPath,
        String typeLabel,
        String materialLabel,
        boolean markerOnly,
        String lastFarmedAt,
        String startedBy,
        String territoryName,
        String dimension,
        ResourceZone zone,
        String status,
        int regenerationPercentage,
        String nextStageAt,
        String fullyRegeneratedAt,
        int secondsUntilNextStage,
        int secondsUntilFull,
        /** Full recovery time in seconds (custom time or policy), 0 for markers. */
        int totalSeconds,
        boolean writable) {

    /** Compact constructor for tests and callers that only need the status fields. */
    ResourceSnapshot(String id, String name, String type, String territoryName,
                     String dimension, ResourceZone zone, String status,
                     int regenerationPercentage, String nextStageAt,
                     String fullyRegeneratedAt, int secondsUntilNextStage,
                     int secondsUntilFull) {
        this(id, "", "", name, type, "", "", "", ResourcePolicy.isMarker(type), "", "",
                territoryName, dimension, zone, status, regenerationPercentage, nextStageAt,
                fullyRegeneratedAt, secondsUntilNextStage, secondsUntilFull,
                ResourcePolicy.fullMinutes(type, null) * 60, true);
    }
}
