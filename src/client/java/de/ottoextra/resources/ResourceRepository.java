package de.ottoextra.resources;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * All lists in memory plus the mutations on them. Every change updates the entry timestamp,
 * marks the entry for upload (shared lists) and writes the list file. Client thread only.
 */
final class ResourceRepository {
    private final ResourceStore store;
    private final List<ResourceList> lists = new ArrayList<>();

    ResourceRepository(ResourceStore store) {
        this.store = store;
        lists.addAll(store.loadAll());
        long now = System.currentTimeMillis();
        for (ResourceList list : lists) {
            if (ResourceMerge.pruneTombstones(list.zones, list.timers, list.dirtyZones,
                    list.dirtyTimers, now)) {
                store.save(list);
            }
        }
    }

    Path directory() {
        return store.directory();
    }

    /** New local list holding copies of imported entries (fresh ids, no timers). */
    ResourceList importZones(String name, List<ZoneRecord> zones) {
        ResourceList list = createList(name);
        for (ZoneRecord record : zones) list.zones.put(record.id(), record);
        store.save(list);
        return list;
    }

    List<ResourceList> lists() {
        return List.copyOf(lists);
    }

    ResourceList list(String localId) {
        if (localId == null) return null;
        for (ResourceList list : lists) if (list.localId.equals(localId)) return list;
        return null;
    }

    ResourceList listByShareId(String shareId) {
        for (ResourceList list : lists) if (shareId.equals(list.shareId())) return list;
        return null;
    }

    ResourceList listOfZone(String zoneId) {
        for (ResourceList list : lists) {
            ZoneRecord record = list.zones.get(zoneId);
            if (record != null && !record.deleted()) return list;
        }
        return null;
    }

    ResourceList createList(String name) {
        ResourceList list = new ResourceList(ResourceWire.newId(), cleanName(name, "Liste"));
        lists.add(list);
        store.save(list);
        return list;
    }

    /** Local choice between coat of arms and symbol; also allowed for read-only lists. */
    void setUseArms(ResourceList list, boolean useArms) {
        if (list.useArms == useArms) return;
        list.useArms = useArms;
        store.save(list);
    }

    /**
     * Changes the list header. Returns false for a read-only list or an invalid header.
     * A shared list queues the header for upload with a strictly newer update time.
     */
    boolean updateMeta(ResourceList list, String name, String note, String icon, String color, long now) {
        if (!list.writable()) return false;
        ListMeta meta = new ListMeta(clip(name, ListMeta.NAME_MAX), clip(note, ListMeta.NOTE_MAX), icon, color,
                stamp(list.metaUpdatedAt, now));
        if (ListMeta.problem(meta) != null) return false;
        list.applyMeta(meta);
        if (list.shared()) list.metaDirty = true;
        store.save(list);
        return true;
    }

    void rename(ResourceList list, String name) {
        list.name = cleanName(name, list.name);
        if (list.shared() && list.writable()) {
            list.metaUpdatedAt = stamp(list.metaUpdatedAt, System.currentTimeMillis());
            list.metaDirty = true;
        }
        store.save(list);
    }

    /** Removes the list locally. Nothing is sent anywhere. */
    void remove(ResourceList list) {
        lists.remove(list);
        store.delete(list);
    }

    void save(ResourceList list) {
        store.save(list);
    }

    // ---- entries -----------------------------------------------------------------------

    /** Adds a new entry; returns null when the list is read-only or full. */
    ZoneRecord addZone(ResourceList list, String name, String type, String dimension,
                       String territory, ResourceZone zone, Integer customMinutes, String note,
                       long now) {
        return addZone(list, name, type, dimension, territory, zone, customMinutes, note, null, now);
    }

    /** Adds a new entry with a marker symbol (ignored for other kinds); null when read-only or full. */
    ZoneRecord addZone(ResourceList list, String name, String type, String dimension,
                       String territory, ResourceZone zone, Integer customMinutes, String note,
                       String symbol, long now) {
        if (!list.writable() || list.zones.size() >= ResourceWire.MAX_ZONES) return null;
        ZoneRecord record = new ZoneRecord(ResourceWire.newId(), clip(name, ResourceWire.NAME_MAX),
                type, dimension, clip(territory, ResourceWire.TERRITORY_MAX), zone,
                customMinutes, clip(note, ResourceWire.NOTE_MAX), now, false, symbol);
        if (ResourceWire.problem(record) != null) return null;
        list.zones.put(record.id(), record);
        markZone(list, record.id());
        store.save(list);
        return record;
    }

    /** Replaces an entry's data; the update time is advanced past the previous one. */
    boolean updateZone(ResourceList list, ZoneRecord changed, long now) {
        ZoneRecord existing = list.zones.get(changed.id());
        if (!list.writable() || existing == null || existing.deleted()) return false;
        ZoneRecord record = new ZoneRecord(existing.id(), clip(changed.name(), ResourceWire.NAME_MAX),
                changed.type(), changed.dimension(),
                clip(changed.territory(), ResourceWire.TERRITORY_MAX), changed.zone(),
                changed.customMinutes(), clip(changed.note(), ResourceWire.NOTE_MAX),
                stamp(existing.updatedAt(), now), false, changed.symbol());
        if (ResourceWire.problem(record) != null) return false;
        list.zones.put(record.id(), record);
        markZone(list, record.id());
        store.save(list);
        return true;
    }

    boolean deleteZone(ResourceList list, String id, long now) {
        ZoneRecord existing = list.zones.get(id);
        if (!list.writable() || existing == null || existing.deleted()) return false;
        list.zones.put(id, existing.tombstone(stamp(existing.updatedAt(), now)));
        markZone(list, id);
        list.timers.remove(id);
        list.dirtyTimers.remove(id);
        store.save(list);
        return true;
    }

    boolean startTimer(ResourceList list, String id, String by, long now) {
        return setTimer(list, id, now, by);
    }

    boolean resetTimer(ResourceList list, String id, long now) {
        return setTimer(list, id, null, null, now);
    }

    /** Moves the start of a running timer; a positive delta makes the remaining time longer. */
    boolean shiftTimer(ResourceList list, String id, long deltaMillis, long now) {
        TimerRecord timer = list.timers.get(id);
        if (timer == null || timer.depletedAt() == null) return false;
        long moved = Math.min(now, timer.depletedAt() + deltaMillis);
        return setTimer(list, id, moved, timer.by(), now);
    }

    private boolean setTimer(ResourceList list, String id, long now, String by) {
        return setTimer(list, id, now, by, now);
    }

    private boolean setTimer(ResourceList list, String id, Long depletedAt, String by, long now) {
        ZoneRecord zone = list.zones.get(id);
        if (!list.writable() || zone == null || zone.deleted()
                || ResourcePolicy.isMarker(zone.type())) return false;
        TimerRecord existing = list.timers.get(id);
        long updatedAt = stamp(existing == null ? 0L : existing.updatedAt(), now);
        list.timers.put(id, new TimerRecord(depletedAt,
                ResourceWire.validBy(by) ? by : null, updatedAt));
        if (list.shared()) list.dirtyTimers.add(id);
        store.save(list);
        return true;
    }

    private void markZone(ResourceList list, String id) {
        if (list.shared()) list.dirtyZones.add(id);
    }

    /** Update time strictly greater than the previous one, even with a skewed local clock. */
    static long stamp(long previous, long now) {
        return Math.max(now, previous + 1);
    }

    // ---- sharing -----------------------------------------------------------------------

    /** Starts sharing: creates the codes and queues the whole list for upload. */
    void share(ResourceList list) {
        list.writeCode = ResourceCodes.generateWriteCode();
        list.readCode = ResourceCodes.readCode(list.writeCode);
        list.since = 0L;
        queueAll(list);
        queueMeta(list, System.currentTimeMillis());
        store.save(list);
    }

    /**
     * New codes: all live entries go under the new id, tombstones for them are queued under the
     * old id. Returns false when the list has no write code.
     */
    boolean renewCode(ResourceList list, long now) {
        if (list.writeCode == null || list.readCode == null) return false;
        var tombstones = new java.util.LinkedHashMap<String, ZoneRecord>();
        for (ZoneRecord record : list.zones.values()) {
            if (!record.deleted()) tombstones.put(record.id(), record.tombstone(stamp(record.updatedAt(), now)));
        }
        list.retired.add(new ResourceList.Retired(list.shareId(), list.writeCode, tombstones));
        list.writeCode = ResourceCodes.generateWriteCode();
        list.readCode = ResourceCodes.readCode(list.writeCode);
        list.since = 0L;
        queueAll(list);
        queueMeta(list, now);
        store.save(list);
        return true;
    }

    /** Creates a list joined with a code. */
    ResourceList join(String name, String writeCode, String readCode) {
        ResourceList list = new ResourceList(ResourceWire.newId(), cleanName(name, "Liste"));
        list.writeCode = writeCode;
        list.readCode = readCode;
        // Until the shared header arrives a joined list gets the neutral banner symbol.
        list.icon = ListMeta.JOINED_ICON;
        list.color = ListMeta.JOINED_COLOR;
        lists.add(list);
        store.save(list);
        return list;
    }

    /** A read-only list joined earlier becomes writable when its write code is entered. */
    void upgrade(ResourceList list, String writeCode) {
        list.writeCode = writeCode;
        list.readCode = ResourceCodes.readCode(writeCode);
        store.save(list);
    }

    /** Queues the header; a list that never had one gets its first update time now. */
    private static void queueMeta(ResourceList list, long now) {
        if (list.metaUpdatedAt == 0L) list.metaUpdatedAt = now;
        list.metaDirty = true;
    }

    private static void queueAll(ResourceList list) {
        list.dirtyZones.clear();
        list.dirtyTimers.clear();
        for (ZoneRecord record : list.zones.values()) {
            if (!record.deleted()) list.dirtyZones.add(record.id());
        }
        for (String id : list.timers.keySet()) {
            ZoneRecord record = list.zones.get(id);
            if (record != null && !record.deleted()) list.dirtyTimers.add(id);
        }
    }

    // ---- snapshots ---------------------------------------------------------------------

    /** Snapshots of all live entries of all lists, evaluated at {@code now}. */
    List<ResourceSnapshot> snapshots(long now) {
        List<ResourceSnapshot> result = new ArrayList<>();
        for (ResourceList list : lists) {
            for (ZoneRecord record : list.zones.values()) {
                if (!record.deleted()) result.add(snapshot(list, record, list.timers.get(record.id()), now));
            }
        }
        return List.copyOf(result);
    }

    static ResourceSnapshot snapshot(ResourceList list, ZoneRecord record, TimerRecord timer, long now) {
        Long depletedAt = timer == null ? null : timer.depletedAt();
        ResourceStatus status = ResourcePolicy.status(record.type(), record.customMinutes(),
                depletedAt, now);
        return new ResourceSnapshot(record.id(), list.localId, list.name, record.name(),
                record.type(), record.symbol() == null ? "" : record.symbol(), "", "",
                ResourcePolicy.isMarker(record.type()),
                depletedAt == null ? "" : Instant.ofEpochMilli(depletedAt).toString(),
                timer == null || timer.by() == null ? "" : timer.by(),
                record.territory(), record.dimension(), record.zone(), status.status(),
                status.regenerationPercentage(), status.nextStageAt(), status.fullyRegeneratedAt(),
                status.secondsUntilNextStage(), status.secondsUntilFull(),
                ResourcePolicy.fullMinutes(record.type(), record.customMinutes()) * 60, list.writable());
    }

    static String clip(String value, int max) {
        if (value == null) return "";
        String trimmed = value.strip();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private static String cleanName(String name, String fallback) {
        String clipped = clip(name, ListMeta.NAME_MAX);
        return clipped.isEmpty() ? fallback : clipped;
    }

    /** Case-insensitive key used to compare territory names. */
    static String territoryKey(String territory) {
        return territory == null ? "" : de.ottoextra.regions.RegionNameKeys.normalize(territory)
                .toLowerCase(Locale.ROOT);
    }
}
