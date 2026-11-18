package de.ottoextra.resources;

import java.time.Instant;
import java.util.List;

/** Fixed regeneration rules per resource type and the pure status computation built on them. */
final class ResourcePolicy {
    record Stage(int afterMinutes, int percentage) { }

    static final String MARKER = "MARKER";
    static final int MAX_CUSTOM_MINUTES = 10_080;
    static final int MINE_HARVEST_PERCENTAGE = 70;
    /** Order is the order used by the create screen; MARKER is created through the POI command. */
    static final List<String> TYPES = List.of("IRON_MINE", "COAL_MINE", "SALT_MINE",
            "IRON_DEPOSIT", "COAL_DEPOSIT", "SALT_DEPOSIT", "MONASTERY_GARDEN",
            "PEAT_BOG", "APIARY", "CLAY_PIT", MARKER);

    private static final List<Stage> DEPOSIT = List.of(new Stage(120, 100));
    private static final List<Stage> MINE = List.of(new Stage(120, 70), new Stage(240, 100));
    private static final List<Stage> SLOW = List.of(new Stage(240, 100));
    private static final List<Stage> FAST = List.of(new Stage(30, 100));

    private ResourcePolicy() { }

    /** Kinds shown as tiles in the editor; iron, coal and salt come as deposit or mine. */
    static final List<String> BASES = List.of("IRON", "COAL", "SALT", "PEAT", "CLAY", "GARDEN",
            "APIARY", "MARKER");

    static boolean hasScope(String base) {
        return "IRON".equals(base) || "COAL".equals(base) || "SALT".equals(base);
    }

    /** Type identifier for a tile and, where it applies, the deposit/mine switch. */
    static String compose(String base, boolean mine) {
        return switch (base) {
            case "IRON", "COAL", "SALT" -> base + (mine ? "_MINE" : "_DEPOSIT");
            case "PEAT" -> "PEAT_BOG";
            case "CLAY" -> "CLAY_PIT";
            case "GARDEN" -> "MONASTERY_GARDEN";
            default -> base;
        };
    }

    /** Tile of a type identifier, or null for an unknown type. */
    static String baseOf(String type) {
        if (type == null) return null;
        return switch (type) {
            case "IRON_MINE", "IRON_DEPOSIT" -> "IRON";
            case "COAL_MINE", "COAL_DEPOSIT" -> "COAL";
            case "SALT_MINE", "SALT_DEPOSIT" -> "SALT";
            case "PEAT_BOG" -> "PEAT";
            case "CLAY_PIT" -> "CLAY";
            case "MONASTERY_GARDEN" -> "GARDEN";
            case "APIARY" -> "APIARY";
            case "MARKER" -> "MARKER";
            default -> null;
        };
    }

    static boolean isMineType(String type) {
        return type != null && type.endsWith("_MINE");
    }

    static boolean known(String type) {
        return type != null && TYPES.contains(type);
    }

    static boolean isMarker(String type) {
        return MARKER.equals(type);
    }

    /** Stages of a type; a custom duration replaces the fixed policy by a single stage. */
    static List<Stage> stages(String type, Integer customMinutes) {
        if (!known(type) || isMarker(type)) return List.of();
        if (customMinutes != null && customMinutes > 0) {
            return List.of(new Stage(customMinutes, 100));
        }
        return switch (type) {
            case "IRON_DEPOSIT", "COAL_DEPOSIT", "SALT_DEPOSIT" -> DEPOSIT;
            case "IRON_MINE", "COAL_MINE", "SALT_MINE" -> MINE;
            case "MONASTERY_GARDEN", "PEAT_BOG" -> SLOW;
            case "APIARY", "CLAY_PIT" -> FAST;
            default -> List.of();
        };
    }

    /** Minutes until a type is fully regenerated, or 0 without a timer. */
    static int fullMinutes(String type, Integer customMinutes) {
        List<Stage> stages = stages(type, customMinutes);
        return stages.isEmpty() ? 0 : stages.getLast().afterMinutes();
    }

    /**
     * Status at {@code nowMs} for a timer started at {@code depletedAt} (null = never started or
     * reset). Mirrors the former server rules: a stage is reached after whole elapsed minutes.
     */
    static ResourceStatus status(String type, Integer customMinutes, Long depletedAt, long nowMs) {
        if (isMarker(type)) return new ResourceStatus("MARKER", 100, "", "", -1, -1);
        if (!known(type)) return new ResourceStatus("UNKNOWN", 0, "", "", -1, -1);
        if (depletedAt == null) return new ResourceStatus("READY", 100, "", "", -1, 0);
        List<Stage> stages = stages(type, customMinutes);
        long elapsedMinutes = Math.max(0L, nowMs - depletedAt) / 60_000L;
        int percentage = 0;
        Long nextStageAt = null;
        for (Stage stage : stages) {
            long at = depletedAt + stage.afterMinutes() * 60_000L;
            if (elapsedMinutes >= stage.afterMinutes()) {
                percentage = stage.percentage();
                continue;
            }
            nextStageAt = at;
            break;
        }
        long fullAt = depletedAt + stages.getLast().afterMinutes() * 60_000L;
        if (nowMs >= fullAt || percentage >= 100) {
            return new ResourceStatus("READY", 100, "", Instant.ofEpochMilli(fullAt).toString(),
                    -1, 0);
        }
        return new ResourceStatus("REGENERATING", percentage,
                nextStageAt == null ? "" : Instant.ofEpochMilli(nextStageAt).toString(),
                Instant.ofEpochMilli(fullAt).toString(),
                nextStageAt == null ? -1 : seconds(nextStageAt - nowMs), seconds(fullAt - nowMs));
    }

    /** True when a harvest may start: ready, or a mine that reached the harvest percentage. */
    static boolean canStartDepletion(String type, String status, int percentage) {
        return "READY".equals(status) || type != null && type.endsWith("_MINE")
                && "REGENERATING".equals(status) && percentage >= MINE_HARVEST_PERCENTAGE;
    }

    /** Mines and deposits may restart a running timer by hand. */
    static boolean canForceRestart(String type, String status) {
        return "REGENERATING".equals(status) && type != null
                && (type.endsWith("_MINE") || type.endsWith("_DEPOSIT"));
    }

    private static int seconds(long millis) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, (millis + 999L) / 1000L));
    }
}
