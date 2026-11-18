package de.ottoextra.horses;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * JSON shapes of the farm list protocol and their validation. Only the fields of the protocol are written:
 * the service answers unknown fields with 422. Values of the Ottonien API (Mast, Zucht, Tempo, Sprung,
 * Leben, owner, revision), observations and the local {@code apiKey} are never part of these shapes.
 */
public final class FarmShareWire {
    public static final int NAME_MAX = 64;
    public static final int NOTE_MAX = 200;
    public static final int LABEL_MAX = 64;
    public static final int MAX_FARMS = 300;
    public static final int MAX_ANIMALS = 1_000;
    public static final int MAX_BODY_BYTES = 262_144;
    /** Updates more than this far ahead of the server clock are capped at the server time. */
    public static final long FUTURE_TOLERANCE_MS = 60_000L;
    private static final Pattern ID = Pattern.compile("[a-z0-9-]{8,40}");

    public record Meta(String name, String note, String icon, String color, long updatedAt) {
    }

    public record FarmRec(String id, String name, String note, String symbol, long updatedAt, boolean deleted) {
        FarmRec withUpdatedAt(long at) {
            return new FarmRec(id, name, note, symbol, at, deleted);
        }
    }

    /** @param farmId null for no farm */
    public record AnimalRec(String uuid, String farmId, String note, String label, long updatedAt, boolean deleted) {
        AnimalRec withUpdatedAt(long at) {
            return new AnimalRec(uuid, farmId, note, label, at, deleted);
        }
    }

    /** A list response; {@code meta} is null when the service sent none (keep the local header). */
    public record Remote(long serverTime, Meta meta, Map<String, FarmRec> farms, Map<String, AnimalRec> animals,
                         int invalid) {
    }

    private FarmShareWire() {
    }

    // ---- validation -------------------------------------------------------------------

    static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /** Lower-case 36-character hyphen form of an entity UUID. */
    static boolean validUuid(String uuid) {
        if (uuid == null || uuid.length() != 36 || !uuid.equals(uuid.toLowerCase(Locale.ROOT))) return false;
        try {
            return UUID.fromString(uuid).toString().equals(uuid);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static boolean length(String value, int min, int max) {
        if (value == null) return false;
        int count = value.codePointCount(0, value.length());
        return count >= min && count <= max;
    }

    /** Null for a valid header, otherwise the name of the offending field. */
    public static String problem(Meta meta) {
        if (meta == null) return "meta";
        if (!length(meta.name(), 1, NAME_MAX)) return "name";
        if (!length(meta.note(), 0, NOTE_MAX)) return "note";
        if (!FarmSymbols.SHAPES.contains(meta.icon())) return "icon";
        if (!FarmSymbols.COLORS.contains(meta.color())) return "color";
        if (meta.updatedAt() < 0) return "updatedAt";
        return null;
    }

    public static String problem(FarmRec farm) {
        if (farm == null) return "farm";
        if (!validId(farm.id())) return "id";
        if (!length(farm.name(), 1, NAME_MAX)) return "name";
        if (!length(farm.note(), 0, NOTE_MAX)) return "note";
        if (!FarmSymbols.valid(farm.symbol())) return "symbol";
        if (farm.updatedAt() < 0) return "updatedAt";
        return null;
    }

    public static String problem(AnimalRec animal) {
        if (animal == null) return "animal";
        if (!validUuid(animal.uuid())) return "uuid";
        if (animal.farmId() != null && !validId(animal.farmId())) return "farmId";
        if (!length(animal.note(), 0, NOTE_MAX)) return "note";
        if (!length(animal.label(), 0, LABEL_MAX)) return "label";
        if (animal.updatedAt() < 0) return "updatedAt";
        return null;
    }

    // ---- writing ----------------------------------------------------------------------

    public static JsonObject metaJson(Meta meta) {
        JsonObject json = new JsonObject();
        json.addProperty("name", meta.name());
        json.addProperty("note", meta.note());
        json.addProperty("icon", meta.icon());
        json.addProperty("color", meta.color());
        json.addProperty("updatedAt", meta.updatedAt());
        return json;
    }

    public static JsonObject farmJson(FarmRec farm) {
        JsonObject json = new JsonObject();
        json.addProperty("id", farm.id());
        json.addProperty("name", farm.name());
        json.addProperty("note", farm.note());
        json.addProperty("symbol", farm.symbol());
        json.addProperty("updatedAt", farm.updatedAt());
        json.addProperty("deleted", farm.deleted());
        return json;
    }

    /** The UUID is the key of the entry, not a field. */
    public static JsonObject animalJson(AnimalRec animal) {
        JsonObject json = new JsonObject();
        if (animal.farmId() == null) json.add("farmId", JsonNull.INSTANCE);
        else json.addProperty("farmId", animal.farmId());
        json.addProperty("note", animal.note());
        json.addProperty("label", animal.label());
        json.addProperty("updatedAt", animal.updatedAt());
        json.addProperty("deleted", animal.deleted());
        return json;
    }

    /** PATCH body with the given parts; empty parts are left out. */
    public static JsonObject body(Meta meta, Map<String, FarmRec> farms, Map<String, AnimalRec> animals) {
        JsonObject body = new JsonObject();
        if (meta != null) body.add("meta", metaJson(meta));
        if (farms != null && !farms.isEmpty()) {
            JsonObject json = new JsonObject();
            farms.forEach((id, farm) -> json.add(id, farmJson(farm)));
            body.add("farms", json);
        }
        if (animals != null && !animals.isEmpty()) {
            JsonObject json = new JsonObject();
            animals.forEach((uuid, animal) -> json.add(uuid, animalJson(animal)));
            body.add("animals", json);
        }
        return body;
    }

    // ---- reading ----------------------------------------------------------------------

    public static Meta meta(JsonObject json) {
        Meta meta = new Meta(string(json, "name"), optionalString(json, "note"), string(json, "icon"),
                string(json, "color"), requiredLong(json, "updatedAt"));
        String problem = problem(meta);
        if (problem != null) throw new IllegalArgumentException(problem);
        return meta;
    }

    public static FarmRec farm(String key, JsonObject json) {
        FarmRec farm = new FarmRec(string(json, "id"), string(json, "name"), optionalString(json, "note"),
                string(json, "symbol"), requiredLong(json, "updatedAt"), optionalBoolean(json, "deleted"));
        String problem = problem(farm);
        if (problem != null) throw new IllegalArgumentException(problem);
        if (key != null && !key.equals(farm.id())) throw new IllegalArgumentException("key");
        return farm;
    }

    public static AnimalRec animal(String uuid, JsonObject json) {
        String farmId = json.has("farmId") && !json.get("farmId").isJsonNull() ? string(json, "farmId") : null;
        AnimalRec animal = new AnimalRec(uuid, farmId, optionalString(json, "note"), optionalString(json, "label"),
                requiredLong(json, "updatedAt"), optionalBoolean(json, "deleted"));
        String problem = problem(animal);
        if (problem != null) throw new IllegalArgumentException(problem);
        return animal;
    }

    /** Parses a list response; invalid single entries are skipped and counted. */
    public static Remote remote(JsonObject json) {
        long serverTime = requiredLong(json, "serverTime");
        long cap = serverTime + FUTURE_TOLERANCE_MS;
        int invalid = 0;
        Meta meta = null;
        if (json.has("meta") && json.get("meta").isJsonObject()) {
            try {
                meta = meta(json.getAsJsonObject("meta"));
                if (meta.updatedAt() > cap) {
                    meta = new Meta(meta.name(), meta.note(), meta.icon(), meta.color(), serverTime);
                }
            } catch (RuntimeException error) {
                invalid++;
            }
        }
        Map<String, FarmRec> farms = new LinkedHashMap<>();
        if (json.has("farms") && json.get("farms").isJsonObject()) {
            for (var entry : json.getAsJsonObject("farms").entrySet()) {
                try {
                    FarmRec farm = farm(entry.getKey(), entry.getValue().getAsJsonObject());
                    farms.put(farm.id(), farm.updatedAt() > cap ? farm.withUpdatedAt(serverTime) : farm);
                } catch (RuntimeException error) {
                    invalid++;
                }
            }
        }
        Map<String, AnimalRec> animals = new LinkedHashMap<>();
        if (json.has("animals") && json.get("animals").isJsonObject()) {
            for (var entry : json.getAsJsonObject("animals").entrySet()) {
                try {
                    AnimalRec animal = animal(entry.getKey(), entry.getValue().getAsJsonObject());
                    animals.put(animal.uuid(), animal.updatedAt() > cap ? animal.withUpdatedAt(serverTime) : animal);
                } catch (RuntimeException error) {
                    invalid++;
                }
            }
        }
        return new Remote(serverTime, meta, farms, animals, invalid);
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key);
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : "";
    }

    private static boolean optionalBoolean(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                && value.getAsBoolean();
    }

    static long requiredLong(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key);
        }
        double number = value.getAsDouble();
        if (number != Math.rint(number) || Double.isInfinite(number)) throw new IllegalArgumentException(key);
        return value.getAsLong();
    }
}
