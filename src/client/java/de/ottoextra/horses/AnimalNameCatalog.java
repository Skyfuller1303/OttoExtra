package de.ottoextra.horses;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class AnimalNameCatalog {

    private static final String RESOURCE =
            "/assets/ottoextra/names/animals.json";

    private final List<String> mares;
    private final List<String> stallions;
    private final Map<String, List<String>> coatNames;
    private final Map<String, List<String>> markingNames;

    public AnimalNameCatalog(List<String> mares, List<String> stallions) {
        this(mares, stallions, Map.of(), Map.of());
    }

    AnimalNameCatalog(List<String> mares, List<String> stallions,
                Map<String, List<String>> coatNames,
                Map<String, List<String>> markingNames) {
        this.mares = sanitize(mares);
        this.stallions = sanitize(stallions);
        this.coatNames = sanitizeMap(coatNames);
        this.markingNames = sanitizeMap(markingNames);
        if (this.mares.isEmpty() || this.stallions.isEmpty()) {
            throw new IllegalArgumentException("Beide Namenspools müssen Einträge enthalten.");
        }
    }

    public static AnimalNameCatalog loadDefault() {
        try (InputStream stream = AnimalNameCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Ressource fehlt: " + RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject appearance = object(root.get("appearance"));
            return new AnimalNameCatalog(read(root.getAsJsonArray("mares")),
                    read(root.getAsJsonArray("stallions")),
                    readMap(object(appearance.get("coatColors"))),
                    readMap(object(appearance.get("markings"))));
        } catch (Exception error) {
            OttoExtra.LOGGER.warn("Namensliste konnte nicht geladen werden: {}",
                    error.toString());
            return new AnimalNameCatalog(
                    List.of("Abendrot", "Silberlinde", "Falkenfeder"),
                    List.of("Sturmfels", "Nordwind", "Falkenflug"));
        }
    }

    public String next(String gender, String current) {
        return next(gender, current, ThreadLocalRandom.current());
    }

    public String next(String gender, String current, Random random) {
        return next(gender, "", "", current, List.of(), random);
    }

    public String next(String gender, String coatColor, String marking,
                       String current, Collection<String> unavailable) {
        return next(gender, coatColor, marking, current, unavailable,
                ThreadLocalRandom.current());
    }

    String next(String gender, String coatColor, String marking,
                String current, Collection<String> unavailable, Random random) {
        Objects.requireNonNull(random, "random");
        List<String> base = pool(gender);
        List<String> appearance = appearancePool(coatColor, marking);
        String cleanCurrent = current == null ? "" : current.trim();
        Set<String> blocked = normalized(unavailable);
        boolean preferAppearance = !appearance.isEmpty() && random.nextInt(4) != 0;
        String candidate = pick(preferAppearance ? appearance : base,
                cleanCurrent, blocked, random);
        if (candidate == null) candidate = pick(preferAppearance ? base : appearance,
                cleanCurrent, blocked, random);
        if (candidate == null) candidate = pick(appearance, cleanCurrent, Set.of(), random);
        if (candidate == null) candidate = pick(base, cleanCurrent, Set.of(), random);
        return candidate == null ? base.getFirst() : candidate;
    }

    public int sizeFor(String gender) {
        return pool(gender).size();
    }

    public int appearanceSize(String coatColor, String marking) {
        return appearancePool(coatColor, marking).size();
    }

    private List<String> pool(String gender) {
        String value = gender == null ? "" : gender.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("stute") || value.contains("♀") || value.startsWith("weib")
                || value.startsWith("fem") || value.equals("mare")) {
            return mares;
        }
        if (value.contains("♂") || value.startsWith("hengst") || value.startsWith("wallach")
                || value.startsWith("männ") || value.startsWith("maenn")
                || value.startsWith("male") || value.equals("stallion")
                || value.equals("gelding")) {
            return stallions;
        }
        List<String> all = new ArrayList<>(mares.size() + stallions.size());
        all.addAll(mares);
        all.addAll(stallions);
        return all;
    }

    private List<String> appearancePool(String coatColor, String marking) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(coatNames.getOrDefault(key(coatColor), List.of()));
        result.addAll(markingNames.getOrDefault(key(marking), List.of()));
        return List.copyOf(result);
    }

    private static String pick(List<String> pool, String current,
                               Set<String> blocked, Random random) {
        if (pool.isEmpty()) return null;
        int start = random.nextInt(pool.size());
        for (int offset = 0; offset < pool.size(); offset++) {
            String candidate = pool.get((start + offset) % pool.size());
            if (!candidate.equalsIgnoreCase(current)
                    && !blocked.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private static Set<String> normalized(Collection<String> values) {
        Set<String> result = new HashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    result.add(value.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return result;
    }

    private static List<String> read(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement value : array) {
            if (value.isJsonPrimitive()) {
                result.add(value.getAsString());
            }
        }
        return result;
    }

    private static JsonObject object(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject()
                : new JsonObject();
    }

    private static Map<String, List<String>> readMap(JsonObject object) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                result.put(key(entry.getKey()), read(entry.getValue().getAsJsonArray()));
            }
        }
        return result;
    }

    private static Map<String, List<String>> sanitizeMap(
            Map<String, List<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, values) -> {
                List<String> clean = sanitize(values);
                if (!clean.isEmpty()) result.put(key(name), clean);
            });
        }
        return Map.copyOf(result);
    }

    private static String key(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> sanitize(List<String> source) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (source != null) {
            for (String value : source) {
                if (value != null && !value.trim().isEmpty()) {
                    unique.add(value.trim());
                }
            }
        }
        return List.copyOf(unique);
    }
}
