package de.ottoextra.welcome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Presentation state only. Seeing or skipping a release never grants consent.
 */
final class WelcomeState {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final int SCHEMA = 2;
    private int schemaVersion = SCHEMA;
    private String seenRelease = "";
    private String installedVersion = "";
    private long seenAtEpochMs;

    private WelcomeState() {
    }

    static String releaseSeries(String version) {
        String value = version == null || version.isBlank() ? "0.0.0" : version.trim();
        var match = java.util.regex.Pattern.compile("^(\\d+\\.\\d+\\.\\d+)(?:[-+].*)?$").matcher(value);
        return match.matches() ? match.group(1) : value;
    }

    static boolean wasSeen(String version) {
        return wasSeen(OttoExtraPaths.welcomeState(), version);
    }

    static boolean wasSeen(Path file, String version) {
        if (!Files.isRegularFile(file)) {
            return false;
        }

        try {
            if (Files.size(file) > 64 * 1024) return false;
            WelcomeState state = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8),
                    WelcomeState.class);
            return state != null && state.schemaVersion == SCHEMA
                    && releaseSeries(version).equals(state.seenRelease);
        } catch (Exception error) {
            OttoExtra.LOGGER.warn(
                    "Willkommensstatus konnte nicht gelesen werden; der Hinweis wird erneut gezeigt: {}",
                    error.getMessage());
            return false;
        }
    }

    static void markSeen(String version) {
        markSeen(OttoExtraPaths.welcomeState(), version);
    }

    static void markSeen(Path file, String version) {
        WelcomeState state = new WelcomeState();
        state.seenRelease = releaseSeries(version);
        state.installedVersion = version == null ? "" : version;
        state.seenAtEpochMs = System.currentTimeMillis();

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(tmp, GSON.toJson(state), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomicMoveUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            OttoExtra.LOGGER.warn(
                    "Willkommensstatus konnte nicht gespeichert werden: {}",
                    error.getMessage());
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // Nur eine temporaere Datei; ein zweiter Fehler ist nicht relevant.
            }
        }
    }
}
