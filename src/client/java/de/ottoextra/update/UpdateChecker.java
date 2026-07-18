package de.ottoextra.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import de.ottoextra.welcome.WelcomeScreenManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prueft einmal pro Minecraft-Start im Hintergrund das neueste stabile GitHub-Release.
 * Fehler oder fehlendes Internet duerfen den Spielstart niemals blockieren.
 */
public final class UpdateChecker {

    public static final String RELEASES_PAGE =
            "https://github.com/Skyfuller1303/OttoExtra/releases";

    private static final URI LATEST_RELEASE_API = URI.create(
            "https://api.github.com/repos/Skyfuller1303/OttoExtra/releases/latest");

    private static final int MAX_RESPONSE_CHARS = 1_000_000;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicReference<UpdateInfo> PENDING = new AtomicReference<>();
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final AtomicBoolean SHOWN_THIS_SESSION = new AtomicBoolean();

    private UpdateChecker() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(UpdateChecker::tick);
        checkAsync();
    }

    public static void checkAsync() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        String currentVersion = installedVersion();
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_API)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "OttoExtra/" + currentVersion)
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> handleResponse(currentVersion, response))
                .exceptionally(error -> {
                    OttoExtra.LOGGER.debug("Update-Pruefung nicht moeglich: {}", rootMessage(error));
                    return null;
                });
    }

    private static void handleResponse(String currentVersion, HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            OttoExtra.LOGGER.debug("GitHub-Update-Pruefung antwortete mit HTTP {}.",
                    response.statusCode());
            return;
        }

        String body = response.body();
        if (body == null || body.isBlank() || body.length() > MAX_RESPONSE_CHARS) {
            OttoExtra.LOGGER.debug("GitHub-Update-Antwort war leer oder unerwartet gross.");
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("tag_name") || json.get("tag_name").isJsonNull()) {
                return;
            }

            String tag = json.get("tag_name").getAsString();
            VersionNumber local = VersionNumber.parse(currentVersion);
            VersionNumber remote = VersionNumber.parse(tag);
            if (!remote.isNewerThan(local)) {
                OttoExtra.LOGGER.debug("OttoExtra ist aktuell (lokal {}, GitHub {}).",
                        local.normalized(), remote.normalized());
                return;
            }

            String releaseUrl = RELEASES_PAGE;
            if (json.has("html_url") && !json.get("html_url").isJsonNull()) {
                releaseUrl = safeReleaseUrl(json.get("html_url").getAsString());
            }

            UpdateInfo info = new UpdateInfo(
                    local.normalized(),
                    remote.normalized(),
                    releaseUrl);

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> PENDING.set(info));
            OttoExtra.LOGGER.info("Neue OttoExtra-Version verfuegbar: {} (installiert: {}).",
                    info.latestVersion(), info.currentVersion());
        } catch (RuntimeException error) {
            OttoExtra.LOGGER.debug("GitHub-Update-Antwort konnte nicht gelesen werden: {}",
                    rootMessage(error));
        }
    }

    private static void tick(MinecraftClient client) {
        if (client == null || SHOWN_THIS_SESSION.get()
                || WelcomeScreenManager.blocksOtherPrompts()) {
            return;
        }

        UpdateInfo info = PENDING.get();
        if (info == null || !(client.currentScreen instanceof TitleScreen)) {
            return;
        }

        if (!SHOWN_THIS_SESSION.compareAndSet(false, true)) {
            return;
        }

        PENDING.set(null);
        client.setScreen(new UpdateAvailableScreen(client.currentScreen, info));
    }

    private static String installedVersion() {
        return FabricLoader.getInstance()
                .getModContainer(OttoExtra.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static String safeReleaseUrl(String candidate) {
        try {
            URI uri = URI.create(candidate);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return RELEASES_PAGE;
            }
            if (!"github.com".equalsIgnoreCase(uri.getHost())) {
                return RELEASES_PAGE;
            }
            String path = uri.getPath();
            if (path == null || !path.startsWith("/Skyfuller1303/OttoExtra/releases")) {
                return RELEASES_PAGE;
            }
            return uri.toString();
        } catch (IllegalArgumentException ignored) {
            return RELEASES_PAGE;
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
