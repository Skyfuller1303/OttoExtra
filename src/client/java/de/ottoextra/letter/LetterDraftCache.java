package de.ottoextra.letter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.config.OttoExtraPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LetterDraftCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private LetterDraftCache() {
    }

    public static Path file() {
        return OttoExtraPaths.root().resolve("letters").resolve("draft.json");
    }

    public static synchronized void save(LetterDraft draft) {
        try {
            draft.repair();
            draft.meta.updatedAtMs = System.currentTimeMillis();
            Path f = file();
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(draft), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {

        }
    }

    public static synchronized LetterDraft load() {
        try {
            Path f = file();
            if (!Files.exists(f)) {
                return LetterDraft.empty();
            }
            LetterDraft draft = GSON.fromJson(
                    Files.readString(f, StandardCharsets.UTF_8), LetterDraft.class);
            if (draft == null) {
                return LetterDraft.empty();
            }
            draft.repair();
            return draft;
        } catch (Exception e) {
            return LetterDraft.empty();
        }
    }

    public static synchronized void clear() {
        try {
            Files.deleteIfExists(file());
        } catch (Exception ignored) {
        }
    }
}
