package de.ottoextra.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Local storage: one JSON file per list below {@code config/ottoextra-resources/}. */
final class ResourceStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 1;
    /** Key of the local marker symbols of older versions, read once for the migration. */
    static final String LEGACY_MARKER_SYMBOLS = "markerSymbols";
    private final Path directory;

    ResourceStore(Path directory) {
        this.directory = directory;
    }

    Path directory() {
        return directory;
    }

    /** Loads every readable list; unreadable files are left untouched and logged. */
    List<ResourceList> loadAll() {
        List<ResourceList> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) return result;
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().toList()) {
                try {
                    JsonObject json = JsonParser.parseString(
                            Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    ResourceList list = fromJson(json, stem(file));
                    // Rewrite once so old local marker symbols live in the zones and old symbol
                    // names (turm, fahne, stecknadel) are stored with their new names from now on.
                    if (json.has(LEGACY_MARKER_SYMBOLS) || usesOldSymbolNames(json)) save(list);
                    result.add(list);
                } catch (Exception error) {
                    OttoExtraResources.LOGGER.warn("Ressourcenliste nicht lesbar, wird ignoriert: {}",
                            file, error);
                }
            }
        } catch (IOException error) {
            OttoExtraResources.LOGGER.warn("Ressourcenlisten konnten nicht gelesen werden: {}",
                    directory, error);
        }
        return result;
    }

    /** Writes the list atomically; returns false when the file could not be written. */
    boolean save(ResourceList list) {
        Path file = file(list);
        try {
            Files.createDirectories(directory);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(toJson(list)), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception error) {
            OttoExtraResources.LOGGER.error("Ressourcenliste konnte nicht gespeichert werden: {}",
                    file, error);
            return false;
        }
    }

    void delete(ResourceList list) {
        try {
            Files.deleteIfExists(file(list));
        } catch (IOException error) {
            OttoExtraResources.LOGGER.warn("Ressourcenliste konnte nicht gelöscht werden: {}",
                    file(list), error);
        }
    }

    private Path file(ResourceList list) {
        return directory.resolve(list.localId + ".json");
    }

    private static String stem(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    static JsonObject toJson(ResourceList list) {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("name", list.name);
        json.addProperty("note", list.note);
        json.addProperty("icon", list.icon);
        json.addProperty("color", list.color);
        json.addProperty("useArms", list.useArms);
        json.addProperty("metaUpdatedAt", list.metaUpdatedAt);
        json.addProperty("metaDirty", list.metaDirty);
        if (list.writeCode != null) json.addProperty("writeCode", list.writeCode);
        if (list.readCode != null) json.addProperty("readCode", list.readCode);
        json.addProperty("since", list.since);
        json.add("zones", zonesJson(list.zones));
        JsonObject timers = new JsonObject();
        list.timers.forEach((id, timer) -> timers.add(id, ResourceWire.timerJson(timer)));
        json.add("timers", timers);
        json.add("dirtyZones", strings(list.dirtyZones));
        json.add("dirtyTimers", strings(list.dirtyTimers));
        JsonArray retired = new JsonArray();
        for (ResourceList.Retired entry : list.retired) {
            JsonObject item = new JsonObject();
            item.addProperty("shareId", entry.shareId());
            item.addProperty("writeCode", entry.writeCode());
            item.add("zones", zonesJson(entry.tombstones()));
            retired.add(item);
        }
        json.add("retired", retired);
        return json;
    }

    static ResourceList fromJson(JsonObject json, String localId) {
        ResourceList list = new ResourceList(localId, ResourceWire.optionalString(json, "name"));
        list.note = ResourceWire.optionalString(json, "note");
        String icon = ResourceWire.optionalString(json, "icon");
        // Old shape names (turm, fahne) are taken over as the new ones (burg, flagge).
        icon = ListMeta.canonicalIcon(icon);
        if (ListMeta.ICONS.contains(icon)) list.icon = icon;
        String color = ResourceWire.optionalString(json, "color");
        if (ListMeta.COLORS.contains(color)) list.color = color;
        // Missing in files of older versions: the coat of arms was always shown then.
        list.useArms = !json.has("useArms") || ResourceWire.optionalBoolean(json, "useArms");
        Long metaAt = ResourceWire.optionalLong(json, "metaUpdatedAt");
        list.metaUpdatedAt = metaAt == null ? 0L : metaAt;
        list.metaDirty = ResourceWire.optionalBoolean(json, "metaDirty");
        if (json.has("writeCode")) list.writeCode = ResourceWire.requiredString(json, "writeCode");
        if (json.has("readCode")) list.readCode = ResourceWire.requiredString(json, "readCode");
        if (list.writeCode != null && list.readCode == null) {
            list.readCode = ResourceCodes.readCode(list.writeCode);
        }
        Long since = ResourceWire.optionalLong(json, "since");
        list.since = since == null ? 0L : since;
        readZones(json, "zones", list.zones);
        migrateMarkerSymbols(json, list, System.currentTimeMillis());
        if (json.has("timers") && json.get("timers").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("timers").entrySet()) {
                try {
                    list.timers.put(entry.getKey(), ResourceWire.timer(entry.getValue().getAsJsonObject()));
                } catch (RuntimeException error) {
                    OttoExtraResources.LOGGER.warn("Ungültiger Timer in Liste {} ignoriert", localId);
                }
            }
        }
        readStrings(json, "dirtyZones", list.dirtyZones);
        readStrings(json, "dirtyTimers", list.dirtyTimers);
        list.dirtyZones.removeIf(id -> !list.zones.containsKey(id));
        list.dirtyTimers.removeIf(id -> !list.timers.containsKey(id));
        if (json.has("retired") && json.get("retired").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("retired")) {
                JsonObject item = element.getAsJsonObject();
                Map<String, ZoneRecord> tombstones = new java.util.LinkedHashMap<>();
                readZones(item, "zones", tombstones);
                list.retired.add(new ResourceList.Retired(ResourceWire.requiredString(item, "shareId"),
                        ResourceWire.requiredString(item, "writeCode"), tombstones));
            }
        }
        return list;
    }

    /**
     * Older versions kept marker symbols only on this computer in {@code "markerSymbols"} (zone id to
     * symbol). They now belong to the zone and are shared: each valid symbol of a live marker without
     * one is moved into the zone. On a writable list the zone gets a newer update time and is marked
     * for sending; a read-only list keeps the time, so the next change from the service wins.
     */
    static void migrateMarkerSymbols(JsonObject json, ResourceList list, long now) {
        if (!json.has(LEGACY_MARKER_SYMBOLS) || !json.get(LEGACY_MARKER_SYMBOLS).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(LEGACY_MARKER_SYMBOLS).entrySet()) {
            ZoneRecord zone = list.zones.get(entry.getKey());
            if (zone == null || zone.deleted() || zone.symbol() != null
                    || !entry.getValue().isJsonPrimitive()) continue;
            String symbol = ZoneRecord.normalizeSymbol(zone.type(), entry.getValue().getAsString());
            if (symbol == null) continue;
            if (list.writable()) {
                list.zones.put(zone.id(), zone.withSymbol(symbol, ResourceRepository.stamp(zone.updatedAt(), now)));
                if (list.shared()) list.dirtyZones.add(zone.id());
            } else {
                list.zones.put(zone.id(), zone.withSymbol(symbol, zone.updatedAt()));
            }
        }
    }

    private static JsonObject zonesJson(Map<String, ZoneRecord> zones) {
        JsonObject json = new JsonObject();
        zones.forEach((id, zone) -> json.add(id, ResourceWire.zoneJson(zone)));
        return json;
    }

    /** True if the stored list symbol or a zone symbol still uses a shape name of symbol set 1. */
    static boolean usesOldSymbolNames(JsonObject json) {
        String icon = ResourceWire.optionalString(json, "icon");
        if (!icon.equals(ListMeta.canonicalIcon(icon))) return true;
        if (!json.has("zones") || !json.get("zones").isJsonObject()) return false;
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("zones").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String symbol = ResourceWire.optionalString(entry.getValue().getAsJsonObject(), "symbol");
            if (!symbol.equals(MarkerSymbols.canonical(symbol))) return true;
        }
        return false;
    }

    private static void readZones(JsonObject json, String key, Map<String, ZoneRecord> target) {
        if (!json.has(key) || !json.get(key).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(key).entrySet()) {
            try {
                ZoneRecord record = ResourceWire.zone(entry.getValue().getAsJsonObject());
                target.put(record.id(), record);
            } catch (RuntimeException error) {
                OttoExtraResources.LOGGER.warn("Ungültiger Eintrag {} ignoriert", entry.getKey());
            }
        }
    }

    private static JsonArray strings(Iterable<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static void readStrings(JsonObject json, String key, java.util.Collection<String> target) {
        if (!json.has(key) || !json.get(key).isJsonArray()) return;
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) target.add(element.getAsString());
        }
    }
}
