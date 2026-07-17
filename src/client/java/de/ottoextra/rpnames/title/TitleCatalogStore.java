package de.ottoextra.rpnames.title;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public final class TitleCatalogStore {
    public static final class Category {
        public String label = "";
        public String color = "#a17f5f";
    }
    public static final class Entry {
        public String id = "";
        public String title = "";
        public List<String> variants = new ArrayList<>();
        public List<String> aliases;
        public String category = "unclassified";
        public String colorOverride;
        public String nameColor;
        public String source = "MANUAL";
        public String sourceCategory = "";
        public boolean enabled = true;
        public boolean overridesColor = true;
        public String matchMode = "NORMALIZED_UMLAUTS";
    }
    private static final class FileModel {
        int schemaVersion = 1;
        String defaultNameColor = "#c7a87f";
        String wikiSource = "";
        Map<String, Category> categories = new LinkedHashMap<>();
        List<Entry> titles = new ArrayList<>();
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String BUNDLED = "/assets/ottoextra/rpnames/title-catalog-default.json";
    private FileModel model = new FileModel();
    private volatile Map<String, Entry> byVariant = Map.of();
    public void load() {
        Path file = catalogFile();
        try {
            if (!Files.exists(file)) {
                writeBundledDefault(file);
            }
            FileModel loaded = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8), FileModel.class);
            if (loaded == null || loaded.titles == null) {
                throw new IllegalStateException("leere title-catalog.json");
            }
            model = loaded;
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] title-catalog.json unlesbar ({}) — nutze Bundled.", e.toString());
            try (InputStream in = TitleCatalogStore.class.getResourceAsStream(BUNDLED)) {
                model = GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), FileModel.class);
            } catch (Exception inner) {
                OttoExtra.LOGGER.error("[rpnames] Bundled Titelkatalog fehlt: {}", inner.toString());
                model = new FileModel();
            }
        }
        if (migrateKnownFixes()) {
            save();
        } else {
            reindex();
        }
    }
    private boolean migrateKnownFixes() {
        boolean changed = false;
        for (Entry e : model.titles) {
            if ("laienbruder".equals(e.id) && "klerus".equals(e.category)) {
                e.category = "allgemein";
                changed = true;
            }
        }
        return changed;
    }
    public synchronized void save() {
        Path file = catalogFile();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(model), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] title-catalog.json speichern fehlgeschlagen: {}", e.toString());
        }
        reindex();
    }
    private static Path catalogFile() {
        return OttoExtraPaths.rpnamesDir().resolve("title-catalog.json");
    }
    private void writeBundledDefault(Path file) throws Exception {
        try (InputStream in = TitleCatalogStore.class.getResourceAsStream(BUNDLED)) {
            if (in == null) {
                throw new IllegalStateException("Bundled default fehlt: " + BUNDLED);
            }
            Files.createDirectories(file.getParent());
            Files.write(file, in.readAllBytes());
        }
    }
    private void reindex() {
        Map<String, Entry> index = new HashMap<>();
        for (Entry e : model.titles) {
            if (!e.enabled) {
                continue;
            }
            seedAliases(e);
            for (String v : matchKeys(e)) {
                if (v != null && !v.isBlank()) {
                    index.putIfAbsent(TitleRegistry.normalize(v), e);
                }
            }
        }
        byVariant = Map.copyOf(index);
    }
    private static List<String> matchKeys(Entry e) {
        List<String> keys = new ArrayList<>();
        if (e.variants != null) {
            keys.addAll(e.variants);
        }
        if (e.aliases != null) {
            keys.addAll(e.aliases);
        }
        if (e.title != null && !e.title.isBlank()) {
            keys.add(e.title);
        }
        return keys;
    }
    private void seedAliases(Entry e) {
        if (e.aliases != null && !e.aliases.isEmpty()) {
            return;
        }
        List<String> seed = new ArrayList<>();
        Entry bundled = bundledDefault(e.id).orElse(null);
        if (bundled != null && bundled.variants != null && !bundled.variants.isEmpty()) {
            seed.addAll(bundled.variants);
        } else if (e.variants != null) {
            seed.addAll(e.variants);
        }
        e.aliases = seed;
    }
    public List<Entry> all() {
        return model.titles;
    }
    private volatile Map<String, Entry> bundledById;
    public Optional<Entry> bundledDefault(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Map<String, Entry> map = bundledById;
        if (map == null) {
            map = new HashMap<>();
            try (InputStream in = TitleCatalogStore.class.getResourceAsStream(BUNDLED)) {
                FileModel fm = GSON.fromJson(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8), FileModel.class);
                if (fm != null && fm.titles != null) {
                    for (Entry e : fm.titles) {
                        if (e.id != null && !e.id.isBlank()) {
                            map.put(e.id, e);
                        }
                    }
                }
            } catch (Exception e) {
                OttoExtra.LOGGER.warn("[rpnames] Bundled Titelkatalog für Reset unlesbar: {}", e.toString());
            }
            bundledById = map;
        }
        return Optional.ofNullable(map.get(id));
    }
    public Map<String, Category> categories() {
        return model.categories;
    }
    public String defaultNameColor() {
        return model.defaultNameColor == null || model.defaultNameColor.isBlank()
                ? "#c7a87f" : model.defaultNameColor;
    }
    public String displayForm(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        Entry e = find(raw).orElse(null);
        if (e == null) {
            return raw;
        }
        String norm = TitleRegistry.normalize(raw);
        if (e.variants != null) {
            for (String v : e.variants) {
                if (v != null && !v.isBlank() && TitleRegistry.normalize(v).equals(norm)) {
                    return v;
                }
            }
        }
        if (e.aliases != null) {
            for (int i = 0; i < e.aliases.size(); i++) {
                String a = e.aliases.get(i);
                if (a != null && !a.isBlank() && TitleRegistry.normalize(a).equals(norm)) {
                    if (e.variants != null && i < e.variants.size()
                            && e.variants.get(i) != null && !e.variants.get(i).isBlank()) {
                        return e.variants.get(i);
                    }
                    break;
                }
            }
        }
        if (e.variants != null) {
            for (String v : e.variants) {
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return e.title;
    }
    public Optional<Entry> find(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byVariant.get(TitleRegistry.normalize(title)));
    }
    public String fallbackTitleColor() {
        Category c = model.categories.get("allgemein");
        return c != null && c.color != null && !c.color.isBlank() ? c.color : "#a17f5f";
    }
    public Optional<String> titleNameColor(String title) {
        return find(title).map(e -> e.nameColor)
                .filter(c -> c != null && !c.isBlank());
    }
    public boolean overridesColor(String title) {
        return find(title).map(e -> e.overridesColor).orElse(false);
    }
    public Optional<String> titleColor(String title) {
        return find(title).map(e -> {
            if (e.colorOverride != null && !e.colorOverride.isBlank()) {
                return e.colorOverride;
            }
            Category c = model.categories.get(e.category);
            return c != null ? c.color : null;
        });
    }
    public synchronized String addCategory(String name, String color) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String label = name.trim();
        String key = label.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (key.isEmpty()) {
            return null;
        }
        Category c = model.categories.getOrDefault(key, new Category());
        c.label = label;
        if (color != null && !color.isBlank()) {
            c.color = color;
        }
        model.categories.put(key, c);
        save();
        return key;
    }
    public synchronized void addOrReplace(Entry entry) {
        model.titles.removeIf(t -> t.id != null && t.id.equals(entry.id));
        model.titles.add(entry);
        save();
    }
    public synchronized void remove(Entry entry) {
        model.titles.remove(entry);
        save();
    }
}
