package de.ottoextra.ailments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** JSON of the conditions protocol (docs/zustand-protokoll.md). Pure. */
public final class ConditionWire {

    public static final int MAX_UUIDS_PER_QUERY = 100;

    private ConditionWire() {
    }

    /** A remote player's shared state as delivered by the server. */
    public record Remote(List<Condition> conditions, long observedAt, long updatedAt) {
    }

    /** Body of {@code PUT /api/v1/conditions/me}. */
    public static String putBody(List<Condition> conditions, long observedAtMillis) {
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        int count = 0;
        for (Condition c : conditions) {
            if (count++ >= MenuReader.MAX_CONDITIONS) break;
            JsonObject o = new JsonObject();
            o.addProperty("name", c.name());
            JsonArray effects = new JsonArray();
            for (String e : c.effects()) effects.add(e);
            o.add("effects", effects);
            o.addProperty("kind", c.kind());
            array.add(o);
        }
        body.add("conditions", array);
        body.addProperty("observedAt", observedAtMillis);
        return body.toString();
    }

    /** Query string value for {@code GET /api/v1/conditions?uuids=}; at most 100 are used. */
    public static String uuidQuery(Collection<UUID> uuids) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (UUID uuid : uuids) {
            if (n++ >= MAX_UUIDS_PER_QUERY) break;
            if (sb.length() > 0) sb.append(',');
            sb.append(uuid);
        }
        return sb.toString();
    }

    /** Parses the answer of the GET endpoint; malformed entries are skipped. */
    public static Map<UUID, Remote> parseGet(String json) {
        Map<UUID, Remote> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return out;
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            return out;
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("players")
                || !root.getAsJsonObject().get("players").isJsonObject()) {
            return out;
        }
        for (var entry : root.getAsJsonObject().getAsJsonObject("players").entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject o = entry.getValue().getAsJsonObject();
                List<Condition> conditions = new ArrayList<>();
                if (o.has("conditions") && o.get("conditions").isJsonArray()) {
                    for (JsonElement c : o.getAsJsonArray("conditions")) {
                        parseCondition(c).ifPresent(conditions::add);
                        if (conditions.size() >= MenuReader.MAX_CONDITIONS) break;
                    }
                }
                out.put(uuid, new Remote(List.copyOf(conditions), longOf(o, "observedAt"), longOf(o, "updatedAt")));
            } catch (RuntimeException ignored) {
                // skip this player
            }
        }
        return out;
    }

    private static java.util.Optional<Condition> parseCondition(JsonElement element) {
        if (!element.isJsonObject()) return java.util.Optional.empty();
        JsonObject o = element.getAsJsonObject();
        if (!o.has("name") || !o.get("name").isJsonPrimitive()) return java.util.Optional.empty();
        String name = AilmentNames.plain(o.get("name").getAsString());
        if (name.isEmpty() || name.length() > Condition.MAX_TEXT) return java.util.Optional.empty();
        List<String> effects = new ArrayList<>();
        if (o.has("effects") && o.get("effects").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("effects")) {
                if (!e.isJsonPrimitive()) continue;
                String text = AilmentNames.plain(e.getAsString());
                if (!text.isEmpty() && text.length() <= Condition.MAX_TEXT && effects.size() < Condition.MAX_EFFECTS) {
                    effects.add(text);
                }
            }
        }
        String kind = o.has("kind") && o.get("kind").isJsonPrimitive()
                && Condition.KIND_LEIDEN.equals(o.get("kind").getAsString())
                ? Condition.KIND_LEIDEN : Condition.KIND_STATUS;
        return java.util.Optional.of(new Condition(name, List.copyOf(effects), kind));
    }

    private static long longOf(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsLong() : 0L;
    }
}
