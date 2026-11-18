package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceMergeTest {
    private static ZoneRecord zone(String name, long updatedAt, boolean deleted) {
        return new ZoneRecord("zone-0001", name, "IRON_MINE", "minecraft:overworld", "Nord",
                ResourceZone.point(1, 64, 2, 3), null, "", updatedAt, deleted);
    }

    private static Map<String, ZoneRecord> map(ZoneRecord record) {
        Map<String, ZoneRecord> map = new HashMap<>();
        map.put(record.id(), record);
        return map;
    }

    @Test
    void newerEntryWins() {
        Map<String, ZoneRecord> local = map(zone("alt", 100, false));
        assertTrue(ResourceMerge.zones(local, map(zone("neu", 101, false)), new HashSet<>()));
        assertEquals("neu", local.get("zone-0001").name());
    }

    @Test
    void olderEntryIsIgnored() {
        Map<String, ZoneRecord> local = map(zone("lokal", 200, false));
        assertFalse(ResourceMerge.zones(local, map(zone("alt", 199, false)), new HashSet<>()));
        assertEquals("lokal", local.get("zone-0001").name());
    }

    @Test
    void tieKeepsTheExistingEntry() {
        Map<String, ZoneRecord> local = map(zone("lokal", 200, false));
        assertFalse(ResourceMerge.zones(local, map(zone("fremd", 200, false)), new HashSet<>()));
        assertEquals("lokal", local.get("zone-0001").name());
    }

    @Test
    void unknownEntryIsAdded() {
        Map<String, ZoneRecord> local = new HashMap<>();
        assertTrue(ResourceMerge.zones(local, map(zone("neu", 5, false)), new HashSet<>()));
        assertEquals(1, local.size());
    }

    @Test
    void newerTombstoneBeatsLiveEntryAndStays() {
        Map<String, ZoneRecord> local = map(zone("lebt", 100, false));
        assertTrue(ResourceMerge.zones(local, map(zone("lebt", 101, true)), new HashSet<>()));
        assertTrue(local.get("zone-0001").deleted());
        // An older live copy arriving later must not resurrect the entry.
        assertFalse(ResourceMerge.zones(local, map(zone("lebt", 100, false)), new HashSet<>()));
        assertTrue(local.get("zone-0001").deleted());
    }

    @Test
    void newerLiveEntryBeatsOlderTombstone() {
        Map<String, ZoneRecord> local = map(zone("weg", 100, true));
        assertTrue(ResourceMerge.zones(local, map(zone("zurück", 150, false)), new HashSet<>()));
        assertFalse(local.get("zone-0001").deleted());
    }

    @Test
    void remoteWinClearsThePendingUpload() {
        Map<String, ZoneRecord> local = map(zone("lokal", 100, false));
        Set<String> dirty = new HashSet<>(Set.of("zone-0001"));
        ResourceMerge.zones(local, map(zone("fremd", 101, false)), dirty);
        assertTrue(dirty.isEmpty());
        Set<String> kept = new HashSet<>(Set.of("zone-0001"));
        ResourceMerge.zones(local, map(zone("fremd", 50, false)), kept);
        assertEquals(Set.of("zone-0001"), kept);
    }

    @Test
    void timersMergeByUpdateTime() {
        Map<String, TimerRecord> local = new HashMap<>();
        local.put("zone-0001", new TimerRecord(1_000L, "Anna", 10));
        Map<String, TimerRecord> incoming = new HashMap<>();
        incoming.put("zone-0001", new TimerRecord(2_000L, "Ben", 11));
        assertTrue(ResourceMerge.timers(local, incoming, new HashSet<>()));
        assertEquals(2_000L, local.get("zone-0001").depletedAt());
        // A reset is a timer with a null start and still wins by time.
        incoming.put("zone-0001", new TimerRecord(null, null, 12));
        assertTrue(ResourceMerge.timers(local, incoming, new HashSet<>()));
        assertEquals(null, local.get("zone-0001").depletedAt());
        incoming.put("zone-0001", new TimerRecord(5_000L, null, 12));
        assertFalse(ResourceMerge.timers(local, incoming, new HashSet<>()));
    }

    @Test
    void oldTombstonesAreRemovedWithTheirTimers() {
        long now = 100L * 24 * 60 * 60 * 1000;
        Map<String, ZoneRecord> zones = new HashMap<>();
        zones.put("zone-0001", zone("alt", now - ResourceMerge.TOMBSTONE_RETENTION_MILLIS - 1, true));
        ZoneRecord fresh = new ZoneRecord("zone-0002", "neu", "APIARY", "d", "", ResourceZone.point(0, 0, 0, 1),
                null, "", now - 1000, true);
        zones.put("zone-0002", fresh);
        Map<String, TimerRecord> timers = new HashMap<>();
        timers.put("zone-0001", new TimerRecord(1L, null, 1));
        assertTrue(ResourceMerge.pruneTombstones(zones, timers, new HashSet<>(), new HashSet<>(), now));
        assertEquals(Set.of("zone-0002"), zones.keySet());
        assertTrue(timers.isEmpty());
    }
}
