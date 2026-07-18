package de.ottoextra.rpnames.title;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TitleRegistry {

    public static final class Group {
        public String label = "";
        public int priority = 0;
        @SerializedName("defaultTitleColor")
        public String titleColor = "#AAAAAA";
        @SerializedName("defaultNameColor")
        public String nameColor = "#DDDDDD";
        public List<String> titles = new ArrayList<>();
    }

    private static final class FileModel {
        int schemaVersion = 1;
        Map<String, Group> groups = new LinkedHashMap<>();
    }

    public record ResolvedTitle(String title, String groupKey, Group group) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String BUNDLED = "/assets/ottoextra/rpnames/title-groups-default.json";

    private volatile Map<String, Group> groups = new LinkedHashMap<>();

    private volatile Map<String, ResolvedTitle> byNormalizedTitle = Map.of();

    private volatile List<String> normalizedByLength = List.of();

    public void load() {
        Path file = OttoExtraPaths.rpnamesTitleGroups();
        try {
            if (!Files.exists(file)) {
                writeBundledDefault(file);
            }
            FileModel model = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8), FileModel.class);
            if (model == null || model.groups == null || model.groups.isEmpty()) {
                throw new IllegalStateException("leere title-groups.json");
            }
            apply(model.groups);
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] title-groups.json unlesbar ({}) — nutze Defaults.", e.toString());
            try (InputStream in = TitleRegistry.class.getResourceAsStream(BUNDLED)) {
                FileModel model = GSON.fromJson(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8), FileModel.class);
                apply(model.groups);
            } catch (Exception inner) {
                OttoExtra.LOGGER.error("[rpnames] Default-Titelgruppen fehlen: {}", inner.toString());
                apply(new LinkedHashMap<>());
            }
        }
    }

    private void writeBundledDefault(Path file) throws Exception {
        try (InputStream in = TitleRegistry.class.getResourceAsStream(BUNDLED)) {
            if (in == null) {
                throw new IllegalStateException("Bundled default fehlt: " + BUNDLED);
            }
            Files.createDirectories(file.getParent());
            Files.write(file, in.readAllBytes());
        }
    }

    private void apply(Map<String, Group> loaded) {
        Map<String, ResolvedTitle> index = new HashMap<>();
        for (Map.Entry<String, Group> e : loaded.entrySet()) {
            for (String title : e.getValue().titles) {
                if (title == null || title.isBlank()) {
                    continue;
                }
                index.putIfAbsent(normalize(title), new ResolvedTitle(title, e.getKey(), e.getValue()));
            }
        }
        List<String> sorted = new ArrayList<>(index.keySet());
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));
        this.groups = loaded;
        this.byNormalizedTitle = Map.copyOf(index);
        this.normalizedByLength = List.copyOf(sorted);
    }

    public Map<String, Group> groups() {
        return groups;
    }

    public synchronized void save() {
        Path file = OttoExtraPaths.rpnamesTitleGroups();
        try {
            FileModel model = new FileModel();
            model.groups = new LinkedHashMap<>(groups);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(model), StandardCharsets.UTF_8);
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            apply(new LinkedHashMap<>(groups));
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] title-groups.json speichern fehlgeschlagen: {}", e.toString());
        }
    }

    public Optional<ResolvedTitle> find(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byNormalizedTitle.get(normalize(title)));
    }

    public Optional<ResolvedTitle> findPrefix(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(line);
        for (String key : normalizedByLength) {
            if (normalized.equals(key) || normalized.startsWith(key + " ")) {
                return Optional.ofNullable(byNormalizedTitle.get(key));
            }
        }
        return Optional.empty();
    }

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("Ä", "Ae").replace("ä", "ae")
                .replace("Ö", "Oe").replace("ö", "oe")
                .replace("Ü", "Ue").replace("ü", "ue")
                .replace("ß", "ss")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
