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

/**
 * Titelkatalog: konkrete Titel-Einträge mit Varianten, Kategorie,
 * Quelle und optionalem Farb-Override — getrennt von den Titelgruppen
 * (Farbschema/Oberkategorie). Persistiert in
 * {@code config/ottoextra/rpnames/title-catalog.json}; fehlt die Datei, wird
 * der gebündelte Wiki-Import (129 Titel, Stände-und-Titel-Seite) geschrieben.
 *
 * <p>Farbauflösung für einen Titel: Spieler-Override → Titel-{@code colorOverride}
 * → Kategorie-Farbe → Titelgruppen-Farbe (Legacy) → {@code defaultNameColor}.</p>
 */
public final class TitleCatalogStore {

    /** Kategorie (Farbschema). GSON-direkt. */
    public static final class Category {
        public String label = "";
        public String color = "#a17f5f";
    }

    /** Ein Titel-Eintrag. GSON-direkt. */
    public static final class Entry {
        public String id = "";
        public String title = "";
        public List<String> variants = new ArrayList<>();
        public String category = "unclassified";
        public String colorOverride;
        /** Optionale Namensfarbe für Personen mit diesem Titel (Spieler-/RP-Name).
         *  Leer = keine; greift zwischen Personen-Override und globaler Farbe. */
        public String nameColor;
        public String source = "MANUAL";
        public String sourceCategory = "";
        public boolean enabled = true;
        /** Beim Zuweisen dieses Titels die Personen-Titelfarbe überschreiben. */
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
    /** normalisierte Variante -> Eintrag (nur enabled). */
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
        reindex();
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
            List<String> names = e.variants == null || e.variants.isEmpty()
                    ? List.of(e.title) : e.variants;
            for (String v : names) {
                if (v != null && !v.isBlank()) {
                    index.putIfAbsent(TitleRegistry.normalize(v), e);
                }
            }
            if (e.title != null && !e.title.isBlank()) {
                index.putIfAbsent(TitleRegistry.normalize(e.title), e);
            }
        }
        byVariant = Map.copyOf(index);
    }

    public List<Entry> all() {
        return model.titles;
    }

    /** id -> gebündelter Default-Eintrag (Wiki-Auslieferung), lazy geladen. */
    private volatile Map<String, Entry> bundledById;

    /**
     * Original-(Wiki-)Eintrag aus der mitgelieferten {@code title-catalog-default.json}
     * zu einer id — für „auf Werkseinstellung zurücksetzen" im Editor. Leer,
     * wenn die id ein eigener (MANUAL) Titel ist oder das Bundle fehlt.
     */
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

    /**
     * Anzeige-Form eines (Server-)Titels: Bei normalen Titeln ist {@code title}
     * der fixe Standardwert, die <b>Varianten</b> sind die einstellbare
     * Anzeige-Form. Regeln:
     * <ul>
     *   <li>kein Katalog-Treffer -&gt; Roh-Titel unverändert;</li>
     *   <li>der Roh-Titel entspricht genau einer Variante -&gt; diese Variante
     *       behalten (z.B. Geschlechtsform Rüstmann/Rüstfrau);</li>
     *   <li>sonst (Treffer über den Standard-{@code title}) -&gt; die erste
     *       (primäre) Variante als Anzeige-Form; ohne Varianten der {@code title}.</li>
     * </ul>
     * So macht „Variante 1 ändern" den angezeigten Titel aller Träger neu.
     */
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
                    return v; // Träger hat genau diese Variante -> behalten
                }
            }
            for (String v : e.variants) {
                if (v != null && !v.isBlank()) {
                    return v; // Standard-Treffer -> primäre (erste) Variante
                }
            }
        }
        return e.title;
    }

    /** Eintrag zu einem (Hover-)Titel, Varianten-/Umlaut-tolerant. */
    public Optional<Entry> find(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byVariant.get(TitleRegistry.normalize(title)));
    }

    /** Fallback-Titelfarbe: Kategorie "allgemein" (unbekannte/leere Titel). */
    public String fallbackTitleColor() {
        Category c = model.categories.get("allgemein");
        return c != null && c.color != null && !c.color.isBlank() ? c.color : "#a17f5f";
    }

    /** Namensfarbe eines Titels (für Personen mit diesem Titel), oder leer. */
    public Optional<String> titleNameColor(String title) {
        return find(title).map(e -> e.nameColor)
                .filter(c -> c != null && !c.isBlank());
    }

    /** Titel-Farbe aus dem Katalog: Override des Eintrags oder Kategorie-Farbe. */
    public Optional<String> titleColor(String title) {
        return find(title).map(e -> {
            if (e.colorOverride != null && !e.colorOverride.isBlank()) {
                return e.colorOverride;
            }
            Category c = model.categories.get(e.category);
            return c != null ? c.color : null;
        });
    }

    /** Neue Kategorie anlegen/aktualisieren (Schlüssel normalisiert). */
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
