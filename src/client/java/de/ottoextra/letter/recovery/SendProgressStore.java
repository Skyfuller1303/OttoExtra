package de.ottoextra.letter.recovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Generischer Recovery-Store: speichert den
 * Sendefortschritt nach JEDEM abgesetzten Command atomar als JSON; nach
 * Join kann der Nutzer fortsetzen/verwerfen. Getrennte Dateien für Brief
 * und Verkündung (Aufrufer wählt den Pfad).
 */
public final class SendProgressStore<T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private final Path file;
    private final Class<T> type;

    public SendProgressStore(Path file, Class<T> type) {
        this.file = file;
        this.type = type;
    }

    public synchronized void save(T progress) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(progress), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            // Recovery ist best effort — Versand läuft weiter
        }
    }

    public synchronized T load() {
        try {
            if (!Files.exists(file)) {
                return null;
            }
            return GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
        }
    }

    public Path file() {
        return file;
    }
}
