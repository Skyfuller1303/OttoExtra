package de.ottoextra.welcome;

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
 * Dauerhafter Marker fuer den einmaligen Willkommensbildschirm.
 * Die Datei wird erst nach einem bewussten Klick auf „Los geht's“ angelegt.
 */
final class WelcomeState {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private int schemaVersion = 1;
    private boolean accepted;
    private long acceptedAtEpochMs;
    private String acceptedWithVersion = "";

    private WelcomeState() {
    }

    static boolean wasAccepted() {
        Path file = OttoExtraPaths.welcomeState();
        if (!Files.isRegularFile(file)) {
            return false;
        }

        try {
            WelcomeState state = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8),
                    WelcomeState.class);
            return state != null && state.accepted;
        } catch (Exception error) {
            OttoExtra.LOGGER.warn(
                    "Willkommensstatus konnte nicht gelesen werden; der Hinweis wird erneut gezeigt: {}",
                    error.getMessage());
            return false;
        }
    }

    static void markAccepted(String version) {
        WelcomeState state = new WelcomeState();
        state.accepted = true;
        state.acceptedAtEpochMs = System.currentTimeMillis();
        state.acceptedWithVersion = version == null ? "" : version;

        Path file = OttoExtraPaths.welcomeState();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(tmp, GSON.toJson(state), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
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
