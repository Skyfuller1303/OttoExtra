package de.ottoextra.resources;

import java.util.Map;
import java.util.Set;

/** Per-entry merge: the larger {@code updatedAt} wins, a tie keeps the existing entry. */
final class ResourceMerge {
    static final long TOMBSTONE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private ResourceMerge() { }

    /** Merges {@code incoming} into {@code local}; returns true when anything changed. */
    static boolean zones(Map<String, ZoneRecord> local, Map<String, ZoneRecord> incoming,
                         Set<String> dirty) {
        boolean changed = false;
        for (Map.Entry<String, ZoneRecord> entry : incoming.entrySet()) {
            ZoneRecord existing = local.get(entry.getKey());
            if (existing != null && entry.getValue().updatedAt() <= existing.updatedAt()) continue;
            local.put(entry.getKey(), entry.getValue());
            dirty.remove(entry.getKey());
            changed = true;
        }
        return changed;
    }

    static boolean timers(Map<String, TimerRecord> local, Map<String, TimerRecord> incoming,
                          Set<String> dirty) {
        boolean changed = false;
        for (Map.Entry<String, TimerRecord> entry : incoming.entrySet()) {
            TimerRecord existing = local.get(entry.getKey());
            if (existing != null && entry.getValue().updatedAt() <= existing.updatedAt()) continue;
            local.put(entry.getKey(), entry.getValue());
            dirty.remove(entry.getKey());
            changed = true;
        }
        return changed;
    }

    /** Removes tombstones older than the retention time together with their timers. */
    static boolean pruneTombstones(Map<String, ZoneRecord> zones, Map<String, TimerRecord> timers,
                                   Set<String> dirtyZones, Set<String> dirtyTimers, long now) {
        boolean changed = false;
        var iterator = zones.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ZoneRecord> entry = iterator.next();
            ZoneRecord record = entry.getValue();
            if (record.deleted() && now - record.updatedAt() > TOMBSTONE_RETENTION_MILLIS
                    && !dirtyZones.contains(entry.getKey())) {
                iterator.remove();
                timers.remove(entry.getKey());
                dirtyTimers.remove(entry.getKey());
                changed = true;
            }
        }
        return changed;
    }
}
