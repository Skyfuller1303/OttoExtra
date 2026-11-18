package de.ottoextra.horses;

import de.ottoextra.horses.FarmShareWire.AnimalRec;
import de.ottoextra.horses.FarmShareWire.FarmRec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure merge and rebuild rules of the farm lists. Per entry the larger {@code updatedAt} wins and a tie
 * keeps the existing entry, exactly like the service. Nothing here touches files or the network.
 */
final class FarmShareMerge {
    static final long TOMBSTONE_RETENTION_MS = AnimalBookStore.TOMBSTONE_MS;

    /** One register animal as the owner sees it. */
    record RegisterAnimal(String uuid, String farmKey, String name) {
    }

    /** What changed in a merge: ids of entries that were taken from the service. */
    record Merged(Set<String> farms, Set<String> animals, boolean meta) {
        boolean any() {
            return meta || !farms.isEmpty() || !animals.isEmpty();
        }
    }

    private FarmShareMerge() {
    }

    static <T> Set<String> entries(Map<String, T> local, Map<String, T> incoming, Set<String> dirty,
                                   java.util.function.ToLongFunction<T> updatedAt) {
        Set<String> taken = new HashSet<>();
        for (Map.Entry<String, T> entry : incoming.entrySet()) {
            T existing = local.get(entry.getKey());
            if (existing != null && updatedAt.applyAsLong(entry.getValue()) <= updatedAt.applyAsLong(existing)) {
                continue;
            }
            local.put(entry.getKey(), entry.getValue());
            dirty.remove(entry.getKey());
            taken.add(entry.getKey());
        }
        return taken;
    }

    /** Merges a service answer into a list and advances {@code since}. */
    static Merged apply(FarmShareList list, FarmShareWire.Remote remote, long sinceOverlapMs) {
        Set<String> farms = entries(list.farms, remote.farms(), list.dirtyFarms, FarmRec::updatedAt);
        Set<String> animals = entries(list.animals, remote.animals(), list.dirtyAnimals, AnimalRec::updatedAt);
        boolean meta = false;
        if (remote.meta() != null && remote.meta().updatedAt() > list.metaUpdatedAt) {
            list.applyMeta(remote.meta());
            list.metaDirty = false;
            meta = true;
        }
        list.since = Math.max(1L, remote.serverTime() - sinceOverlapMs);
        return new Merged(farms, animals, meta);
    }

    /** Removes tombstones older than the retention time that are not waiting for an upload. */
    static boolean pruneTombstones(FarmShareList list, long now) {
        boolean farms = list.farms.entrySet().removeIf(entry -> entry.getValue().deleted()
                && now - entry.getValue().updatedAt() > TOMBSTONE_RETENTION_MS
                && !list.dirtyFarms.contains(entry.getKey()));
        boolean animals = list.animals.entrySet().removeIf(entry -> entry.getValue().deleted()
                && now - entry.getValue().updatedAt() > TOMBSTONE_RETENTION_MS
                && !list.dirtyAnimals.contains(entry.getKey()));
        return farms || animals;
    }

    /**
     * Rebuilds the shared state of an owned list from the local farm headers, the register and the local
     * notes. Changed entries get {@code now} and are queued; entries that left the list become tombstones.
     *
     * @param headers  local farm headers by local id (deleted headers count as missing)
     * @param register register animals, or null while the register is not loaded (animals stay untouched)
     * @param notes    local animal notes by lower-case UUID
     * @return true when anything changed
     */
    static boolean rebuildOwned(FarmShareList list, Map<String, AnimalBookStore.Farm> headers,
                                List<RegisterAnimal> register, Map<String, String> notes, long now) {
        if (!list.owned || list.readOnly()) return false;
        boolean changed = false;
        Map<String, String> farmIdByKey = new HashMap<>();
        Set<String> live = new HashSet<>();
        for (String id : list.members) {
            AnimalBookStore.Farm header = headers.get(id);
            if (header == null || header.deleted() || !FarmShareWire.validId(id)) continue;
            FarmRec wanted = new FarmRec(id, clip(header.name(), FarmShareWire.NAME_MAX),
                    clip(header.note(), FarmShareWire.NOTE_MAX), FarmSymbols.canonical(header.symbol()), now, false);
            if (wanted.name().isEmpty()) continue;
            FarmRec existing = list.farms.get(id);
            if (existing == null && list.farms.size() >= FarmShareWire.MAX_FARMS) continue;
            live.add(id);
            if (!header.apiKey().isEmpty()) farmIdByKey.put(header.apiKey(), id);
            if (existing != null && !existing.deleted() && existing.name().equals(wanted.name())
                    && existing.note().equals(wanted.note()) && existing.symbol().equals(wanted.symbol())) {
                continue;
            }
            list.farms.put(id, existing == null ? wanted : wanted.withUpdatedAt(after(existing.updatedAt(), now)));
            list.dirtyFarms.add(id);
            changed = true;
        }
        for (FarmRec farm : List.copyOf(list.farms.values())) {
            if (farm.deleted() || live.contains(farm.id())) continue;
            list.farms.put(farm.id(), tombstone(farm, now));
            list.dirtyFarms.add(farm.id());
            changed = true;
        }
        if (register == null) return changed;
        Map<String, AnimalRec> wanted = new LinkedHashMap<>();
        for (RegisterAnimal animal : register) {
            String farmId = farmIdByKey.get(animal.farmKey());
            if (farmId == null || !FarmShareWire.validUuid(animal.uuid())) continue;
            wanted.put(animal.uuid(), new AnimalRec(animal.uuid(), farmId,
                    clip(notes.getOrDefault(animal.uuid(), ""), FarmShareWire.NOTE_MAX),
                    clip(animal.name(), FarmShareWire.LABEL_MAX), now, false));
        }
        for (AnimalRec animal : wanted.values()) {
            AnimalRec existing = list.animals.get(animal.uuid());
            if (existing != null && !existing.deleted() && Objects.equals(existing.farmId(), animal.farmId())
                    && existing.note().equals(animal.note()) && existing.label().equals(animal.label())) {
                continue;
            }
            if (existing == null && list.animals.size() >= FarmShareWire.MAX_ANIMALS) continue;
            list.animals.put(animal.uuid(), existing == null ? animal
                    : animal.withUpdatedAt(after(existing.updatedAt(), now)));
            list.dirtyAnimals.add(animal.uuid());
            changed = true;
        }
        for (AnimalRec animal : List.copyOf(list.animals.values())) {
            if (animal.deleted() || wanted.containsKey(animal.uuid())) continue;
            list.animals.put(animal.uuid(), new AnimalRec(animal.uuid(), null, "", "",
                    after(animal.updatedAt(), now), true));
            list.dirtyAnimals.add(animal.uuid());
            changed = true;
        }
        return changed;
    }

    /** Header change of a shared farm; refused for read-only lists and unknown farms. */
    static boolean editFarm(FarmShareList list, String id, String name, String note, String symbol, long now) {
        if (list.readOnly()) return false;
        FarmRec existing = list.farm(id);
        if (existing == null) return false;
        String cleanName = clip(name, FarmShareWire.NAME_MAX);
        if (cleanName.isEmpty()) cleanName = existing.name();
        FarmRec updated = new FarmRec(id, cleanName, clip(note, FarmShareWire.NOTE_MAX),
                FarmSymbols.canonical(symbol), after(existing.updatedAt(), now), false);
        if (updated.name().equals(existing.name()) && updated.note().equals(existing.note())
                && updated.symbol().equals(existing.symbol())) {
            return false;
        }
        list.farms.put(id, updated);
        list.dirtyFarms.add(id);
        return true;
    }

    /**
     * "Codes erneuern": the list moves to the id of a new write code. Everything is written again under the
     * new id; the old id gets tombstones for every entry.
     */
    static boolean renew(FarmShareList list, String newWriteCode, long now) {
        if (list.readOnly() || FarmShareCodes.kind(newWriteCode) != FarmShareCodes.Kind.WRITE) return false;
        Map<String, FarmRec> farmTombstones = new LinkedHashMap<>();
        list.farms.values().forEach(farm -> farmTombstones.put(farm.id(), tombstone(farm, now)));
        Map<String, AnimalRec> animalTombstones = new LinkedHashMap<>();
        list.animals.values().forEach(animal -> animalTombstones.put(animal.uuid(),
                new AnimalRec(animal.uuid(), null, "", "", after(animal.updatedAt(), now), true)));
        list.retired.add(new FarmShareList.Retired(list.listId, list.writeCode, farmTombstones, animalTombstones));
        list.writeCode = FarmShareCodes.normalize(newWriteCode);
        list.readCode = FarmShareCodes.readCode(list.writeCode);
        list.listId = FarmShareCodes.listId(list.readCode);
        list.since = 0L;
        // Deleted entries need not be repeated under a fresh id.
        list.farms.values().removeIf(FarmRec::deleted);
        list.animals.values().removeIf(AnimalRec::deleted);
        list.dirtyFarms.clear();
        list.dirtyFarms.addAll(list.farms.keySet());
        list.dirtyAnimals.clear();
        list.dirtyAnimals.addAll(list.animals.keySet());
        list.metaDirty = true;
        list.metaUpdatedAt = Math.max(now, list.metaUpdatedAt);
        return true;
    }

    private static FarmRec tombstone(FarmRec farm, long now) {
        return new FarmRec(farm.id(), farm.name(), "", farm.symbol(), after(farm.updatedAt(), now), true);
    }

    /** A local change must win over the entry it replaces, even when the clocks disagree. */
    static long after(long previous, long now) {
        return Math.max(now, previous + 1);
    }

    static String clip(String value, int max) {
        return AnimalBookStore.clip(value, max);
    }
}
