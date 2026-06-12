package de.ottoextra.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persistenter lokaler Zustand des installierten Server-Packs.
 *
 * <p>Tolerant gelesen (fehlende Felder => erzwingt Neuabgleich), streng/atomar
 * geschrieben. Liegt in {@code config/ottoextra/resourcepack/state.json}.</p>
 */
public final class PackState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String version;
    public String sha256;
    public String etag;
    /** Zeitpunkt der letzten erfolgreichen Installation (ISO-8601 UTC). */
    public String installedAt;
    /** Zeitpunkt der letzten API-Prüfung (ISO-8601 UTC), unabhängig vom Ergebnis. */
    public String lastCheckedAt;
    /** Zuletzt von der API gemeldete Version/Tag (auch wenn nicht installiert). */
    public String remoteVersion;
    /** Ob der Pack zuletzt vom Nutzer aktiv gehalten wurde (für respectUserDisable). */
    public boolean enabled = true;

    public static PackState load() {
        Path file = OttoExtraPaths.resourcepackState();
        if (Files.exists(file)) {
            try {
                PackState s = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), PackState.class);
                if (s != null) {
                    return s;
                }
            } catch (Exception e) {
                OttoExtra.LOGGER.warn("[resourcepack] state.json unlesbar — Neuabgleich. ({})", e.getMessage());
            }
        }
        return new PackState();
    }

    public void save() {
        Path file = OttoExtraPaths.resourcepackState();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("state.json.tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            OttoExtra.LOGGER.error("[resourcepack] state.json speichern fehlgeschlagen: {}", e.getMessage());
        }
    }

    public boolean matchesSha(String otherSha) {
        return sha256 != null && otherSha != null && sha256.equalsIgnoreCase(otherSha);
    }
}
