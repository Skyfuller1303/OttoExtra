package de.ottoextra.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One list of resource entries with timers, optionally shared through codes. */
final class ResourceList {
    /** Old identity of a list after "renew code": tombstones still to be written under it. */
    record Retired(String shareId, String writeCode, Map<String, ZoneRecord> tombstones) { }

    final String localId;
    String name;
    String note = "";
    String icon = ListMeta.DEFAULT_ICON;
    String color = ListMeta.DEFAULT_COLOR;
    /**
     * Show the coat of arms of the Gefolge with the list's name instead of the symbol, when there
     * is one. Kept on this computer only, not part of the shared header.
     */
    boolean useArms = true;
    /** Update time of the header; 0 = never set or received. */
    long metaUpdatedAt;
    boolean metaDirty;
    /** Null for a local list and for a list joined with a read code. */
    String writeCode;
    /** Null for a local list. */
    String readCode;
    /** Lower bound for the next incremental fetch; 0 forces a full fetch. */
    long since;
    final Map<String, ZoneRecord> zones = new LinkedHashMap<>();
    final Map<String, TimerRecord> timers = new LinkedHashMap<>();
    final Set<String> dirtyZones = new HashSet<>();
    final Set<String> dirtyTimers = new HashSet<>();
    final List<Retired> retired = new ArrayList<>();

    ResourceList(String localId, String name) {
        this.localId = localId;
        this.name = name;
    }

    ListMeta meta() {
        return new ListMeta(name, note, icon, color, metaUpdatedAt);
    }

    void applyMeta(ListMeta meta) {
        name = meta.name();
        note = meta.note();
        icon = meta.icon();
        color = meta.color();
        metaUpdatedAt = meta.updatedAt();
    }

    boolean shared() {
        return readCode != null;
    }

    /** Local lists and lists joined with the write code can be changed. */
    boolean writable() {
        return readCode == null || writeCode != null;
    }

    /** Identifier at the list service, or null for a local list. */
    String shareId() {
        return readCode == null ? null : ResourceCodes.listId(readCode);
    }

    int liveZoneCount() {
        int count = 0;
        for (ZoneRecord record : zones.values()) if (!record.deleted()) count++;
        return count;
    }

    boolean hasPendingChanges() {
        return !dirtyZones.isEmpty() || !dirtyTimers.isEmpty() || !retired.isEmpty() || metaDirty;
    }
}
