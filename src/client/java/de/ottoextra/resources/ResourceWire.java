package de.ottoextra.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** JSON shapes of the list service protocol, their validation and the plain-text list format. */
final class ResourceWire {
    static final String TEXT_FORMAT = "ottoextra-resources-list";
    static final int MAX_ZONES = 300;
    static final int MAX_TEXT_CHARS = 262_144;
    static final int NAME_MAX = 60;
    static final int NOTE_MAX = 200;
    static final int TERRITORY_MAX = 60;
    static final int DIMENSION_MAX = 100;
    private static final Pattern ID = Pattern.compile("[a-z0-9-]{8,40}");
    private static final Pattern BY = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final int COORD_XZ = 30_000_000;
    private static final int COORD_Y = 2048;

    private ResourceWire() { }

    record ImportedList(String name, List<ZoneRecord> zones, int skipped) { }

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static boolean validBy(String by) {
        return by != null && BY.matcher(by).matches();
    }

    // ---- zones -------------------------------------------------------------------------

    static JsonObject zoneJson(ZoneRecord record) {
        JsonObject json = new JsonObject();
        json.addProperty("id", record.id());
        json.addProperty("name", record.name());
        json.addProperty("type", record.type());
        json.addProperty("dimension", record.dimension());
        json.addProperty("territory", record.territory());
        json.add("zone", areaJson(record.zone()));
        if (record.customMinutes() == null) json.add("customMinutes", JsonNull.INSTANCE);
        else json.addProperty("customMinutes", record.customMinutes());
        json.addProperty("note", record.note());
        json.addProperty("updatedAt", record.updatedAt());
        json.addProperty("deleted", record.deleted());
        // Optional field of the protocol: null for other kinds and for the default pin.
        if (record.symbol() == null) json.add("symbol", JsonNull.INSTANCE);
        else json.addProperty("symbol", record.symbol());
        return json;
    }

    /** Parses and validates one zone; throws {@link IllegalArgumentException} for invalid data. */
    static ZoneRecord zone(JsonObject json) {
        ZoneRecord record = new ZoneRecord(
                requiredString(json, "id"), requiredString(json, "name"),
                requiredString(json, "type"), requiredString(json, "dimension"),
                optionalString(json, "territory"), area(object(json, "zone")),
                optionalInteger(json, "customMinutes"), optionalString(json, "note"),
                requiredLong(json, "updatedAt"), optionalBoolean(json, "deleted"),
                // Missing, null or an unknown value counts as no symbol; the zone stays valid.
                optionalString(json, "symbol"));
        String problem = problem(record);
        if (problem != null) throw new IllegalArgumentException(problem);
        return record;
    }

    /** Returns null for a valid record, otherwise a short English description of the problem. */
    static String problem(ZoneRecord record) {
        if (!ID.matcher(record.id()).matches()) return "id";
        if (record.name().isEmpty() || record.name().length() > NAME_MAX) return "name";
        if (!ResourcePolicy.known(record.type())) return "type";
        if (record.dimension().isEmpty() || record.dimension().length() > DIMENSION_MAX) {
            return "dimension";
        }
        if (record.territory().length() > TERRITORY_MAX) return "territory";
        if (record.note().length() > NOTE_MAX) return "note";
        Integer custom = record.customMinutes();
        if (custom != null && (custom < 1 || custom > ResourcePolicy.MAX_CUSTOM_MINUTES)) {
            return "customMinutes";
        }
        if (record.updatedAt() < 0) return "updatedAt";
        ResourceZone zone = record.zone();
        if (zone.type() == ResourceZone.Type.POINT) {
            if (!coordinates(zone.x(), zone.y(), zone.z())) return "zone";
        } else if (!coordinates(zone.minX(), zone.minY(), zone.minZ())
                || !coordinates(zone.maxX(), zone.maxY(), zone.maxZ())) {
            return "zone";
        }
        return null;
    }

    private static boolean coordinates(int x, int y, int z) {
        return Math.abs((long) x) <= COORD_XZ && Math.abs((long) z) <= COORD_XZ
                && Math.abs((long) y) <= COORD_Y;
    }

    static JsonObject areaJson(ResourceZone zone) {
        JsonObject json = new JsonObject();
        json.addProperty("type", zone.type().name());
        if (zone.type() == ResourceZone.Type.POINT) {
            json.addProperty("x", zone.x());
            json.addProperty("y", zone.y());
            json.addProperty("z", zone.z());
            json.addProperty("radius", zone.radius());
        } else {
            JsonObject min = new JsonObject();
            min.addProperty("x", zone.minX());
            min.addProperty("y", zone.minY());
            min.addProperty("z", zone.minZ());
            JsonObject max = new JsonObject();
            max.addProperty("x", zone.maxX());
            max.addProperty("y", zone.maxY());
            max.addProperty("z", zone.maxZ());
            json.add("min", min);
            json.add("max", max);
        }
        return json;
    }

    static ResourceZone area(JsonObject json) {
        String type = requiredString(json, "type");
        if ("POINT".equals(type)) {
            return ResourceZone.point(requiredInt(json, "x"), requiredInt(json, "y"),
                    requiredInt(json, "z"), requiredInt(json, "radius"));
        }
        if (!"CUBOID".equals(type)) throw new IllegalArgumentException("zone");
        JsonObject min = object(json, "min");
        JsonObject max = object(json, "max");
        return ResourceZone.cuboid(
                new BlockPos(requiredInt(min, "x"), requiredInt(min, "y"), requiredInt(min, "z")),
                new BlockPos(requiredInt(max, "x"), requiredInt(max, "y"), requiredInt(max, "z")));
    }

    // ---- list header -------------------------------------------------------------------

    static JsonObject metaJson(ListMeta meta) {
        JsonObject json = new JsonObject();
        json.addProperty("name", meta.name());
        json.addProperty("note", meta.note());
        json.addProperty("icon", meta.icon());
        json.addProperty("color", meta.color());
        json.addProperty("updatedAt", meta.updatedAt());
        return json;
    }

    /** Parses and validates a list header; throws {@link IllegalArgumentException} when invalid. */
    static ListMeta meta(JsonObject json) {
        ListMeta meta = new ListMeta(requiredString(json, "name"), optionalString(json, "note"),
                requiredString(json, "icon"), requiredString(json, "color"), requiredLong(json, "updatedAt"));
        String problem = ListMeta.problem(meta);
        if (problem != null) throw new IllegalArgumentException(problem);
        return meta;
    }

    // ---- timers ------------------------------------------------------------------------

    static JsonObject timerJson(TimerRecord timer) {
        JsonObject json = new JsonObject();
        if (timer.depletedAt() == null) json.add("depletedAt", JsonNull.INSTANCE);
        else json.addProperty("depletedAt", timer.depletedAt());
        if (timer.by() == null) json.add("by", JsonNull.INSTANCE);
        else json.addProperty("by", timer.by());
        json.addProperty("updatedAt", timer.updatedAt());
        return json;
    }

    static TimerRecord timer(JsonObject json) {
        Long depletedAt = optionalLong(json, "depletedAt");
        String by = json.has("by") && json.get("by").isJsonPrimitive()
                ? json.get("by").getAsString() : null;
        long updatedAt = requiredLong(json, "updatedAt");
        if (depletedAt != null && depletedAt < 0 || updatedAt < 0
                || by != null && !validBy(by)) {
            throw new IllegalArgumentException("timer");
        }
        return new TimerRecord(depletedAt, by, updatedAt);
    }

    // ---- plain-text list (no timers) ---------------------------------------------------

    static String exportText(String listName, Collection<ZoneRecord> zones) {
        JsonObject root = new JsonObject();
        root.addProperty("format", TEXT_FORMAT);
        root.addProperty("version", 1);
        root.addProperty("name", listName);
        JsonArray array = new JsonArray();
        for (ZoneRecord record : zones) {
            if (record.deleted()) continue;
            JsonObject json = zoneJson(record);
            json.remove("id");
            json.remove("updatedAt");
            json.remove("deleted");
            array.add(json);
        }
        root.add("zones", array);
        return root.toString();
    }

    /**
     * Parses exported text. Imported entries get fresh ids and {@code now} as update time, so they
     * never collide with existing lists. Throws {@link IllegalArgumentException} when the text is
     * no list at all; single invalid entries are skipped and counted.
     */
    static ImportedList importText(String text, Supplier<String> ids, long now) {
        if (text == null || text.isBlank() || text.length() > MAX_TEXT_CHARS) {
            throw new IllegalArgumentException("text");
        }
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(text.trim());
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("text");
            root = parsed.getAsJsonObject();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("text", error);
        }
        if (!TEXT_FORMAT.equals(optionalString(root, "format"))
                || !root.has("zones") || !root.get("zones").isJsonArray()) {
            throw new IllegalArgumentException("format");
        }
        String name = optionalString(root, "name").trim();
        if (name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);
        List<ZoneRecord> zones = new ArrayList<>();
        int skipped = 0;
        for (JsonElement element : root.getAsJsonArray("zones")) {
            if (zones.size() >= MAX_ZONES || !element.isJsonObject()) {
                skipped++;
                continue;
            }
            try {
                JsonObject json = element.getAsJsonObject().deepCopy();
                json.addProperty("id", ids.get());
                json.addProperty("updatedAt", now);
                json.addProperty("deleted", false);
                zones.add(zone(json));
            } catch (RuntimeException error) {
                skipped++;
            }
        }
        return new ImportedList(name, List.copyOf(zones), skipped);
    }

    // ---- JSON helpers ------------------------------------------------------------------

    static JsonObject object(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalArgumentException(key);
        }
        return json.getAsJsonObject(key);
    }

    static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()
                || !json.get(key).getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key);
        }
        return json.get(key).getAsString();
    }

    static String optionalString(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : "";
    }

    static boolean optionalBoolean(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                && json.get(key).getAsBoolean();
    }

    static int requiredInt(JsonObject json, String key) {
        long value = requiredLong(json, key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key);
        }
        return (int) value;
    }

    static long requiredLong(JsonObject json, String key) {
        Long value = optionalLong(json, key);
        if (value == null) throw new IllegalArgumentException(key);
        return value;
    }

    static Long optionalLong(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        if (!json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key);
        }
        return json.get(key).getAsLong();
    }

    static Integer optionalInteger(JsonObject json, String key) {
        Long value = optionalLong(json, key);
        if (value == null) return null;
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key);
        }
        return value.intValue();
    }
}
