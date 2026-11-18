package de.ottoextra.resources;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Shared status text for the native Xaero menu and the fallback map overlay. */
public final class ResourceMapStatus {
    private ResourceMapStatus() { }

    public static String label(ResourceSnapshot resource) {
        return label(resource, Instant.now());
    }

    public static int indicatorColor(String statusLabel) {
        return "Bereit".equals(statusLabel) ? 0xFF42D66B
                : statusLabel.startsWith("Abbaubar") ? 0xFFFFC928 : 0xFFE14949;
    }

    static String label(ResourceSnapshot resource, Instant now) {
        if (resource.markerOnly()) return "Kartenmarkierung";
        if ("READY".equals(resource.status())) return "Bereit";
        if ("DISABLED".equals(resource.status())) return "Deaktiviert";
        if (!"REGENERATING".equals(resource.status())) return resource.status();
        long full = secondsRemaining(resource.fullyRegeneratedAt(), resource.secondsUntilFull(), now);
        if (full == 0) return "Bereit";
        boolean harvestable = resource.type() != null && resource.type().endsWith("_MINE")
                && resource.regenerationPercentage() >= 70;
        if (full < 0) return (harvestable ? "Abbaubar" : "Regeneration")
                + " (" + resource.regenerationPercentage() + " %)";
        return harvestable ? "Abbaubar · voll in " + duration(full) : "Bereit in " + duration(full);
    }

    static long secondsRemaining(String timestamp, int fallback, Instant now) {
        if (timestamp != null && !timestamp.isBlank()) {
            try {
                Instant ready = OffsetDateTime.parse(timestamp).toInstant();
                if (!ready.isAfter(now)) return 0;
                // Round up: never display ready while part of the final second remains.
                var remaining = java.time.Duration.between(now, ready);
                return remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
            } catch (DateTimeParseException ignored) { }
        }
        return fallback;
    }

    static String duration(long seconds) {
        long minutes = Math.max(1, (seconds + 59) / 60);
        long hours = minutes / 60;
        return hours == 0 ? minutes + " min" : hours + " h " + minutes % 60 + " min";
    }
}
