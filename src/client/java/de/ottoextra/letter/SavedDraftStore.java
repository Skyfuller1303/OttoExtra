package de.ottoextra.letter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.config.OttoExtraPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Benannte, dauerhaft gespeicherte Brief-/Verkündungsentwürfe unter
 * {@code config/ottoextra/drafts/<draftId>.json} — getrennt vom flüchtigen
 * Arbeits-Cache ({@link LetterDraftCache}). Speichern/Laden ist best effort;
 * defekte Dateien werden beim Listen übersprungen.
 */
public final class SavedDraftStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private SavedDraftStore() {
    }

    private static Path dir() {
        return OttoExtraPaths.draftsDir();
    }

    private static Path fileFor(String id) {
        return dir().resolve(id + ".json");
    }

    /** Kopie des Entwurfs unter dem Namen speichern (eigene draftId bleibt). */
    public static synchronized LetterDraft save(LetterDraft source, String name) {
        LetterDraft copy = deepCopy(source);
        copy.repair();
        if (name != null && !name.isBlank()) {
            copy.meta.name = name.trim();
        }
        copy.meta.updatedAtMs = System.currentTimeMillis();
        try {
            Path f = fileFor(copy.meta.draftId);
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(copy), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            // best effort
        }
        return copy;
    }

    /** Alle gespeicherten Entwürfe, neueste zuerst. */
    public static synchronized List<LetterDraft> list() {
        List<LetterDraft> out = new ArrayList<>();
        Path d = dir();
        if (!Files.isDirectory(d)) {
            return out;
        }
        try (Stream<Path> files = Files.list(d)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            LetterDraft draft = GSON.fromJson(
                                    Files.readString(p, StandardCharsets.UTF_8), LetterDraft.class);
                            if (draft != null) {
                                draft.repair();
                                out.add(draft);
                            }
                        } catch (Exception ignored) {
                            // defekte Datei überspringen
                        }
                    });
        } catch (Exception ignored) {
            // best effort
        }
        out.sort(Comparator.comparingLong((LetterDraft dr) -> dr.meta.updatedAtMs).reversed());
        return out;
    }

    public static synchronized LetterDraft load(String id) {
        try {
            Path f = fileFor(id);
            if (!Files.exists(f)) {
                return null;
            }
            LetterDraft draft = GSON.fromJson(
                    Files.readString(f, StandardCharsets.UTF_8), LetterDraft.class);
            if (draft != null) {
                draft.repair();
            }
            return draft;
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized void delete(String id) {
        try {
            Files.deleteIfExists(fileFor(id));
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static LetterDraft deepCopy(LetterDraft src) {
        return GSON.fromJson(GSON.toJson(src), LetterDraft.class);
    }
}
