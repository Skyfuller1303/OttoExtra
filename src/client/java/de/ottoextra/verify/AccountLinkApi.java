package de.ottoextra.verify;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.privacy.ConsentPermit;
import de.ottoextra.privacy.ConsentHttp;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

final class AccountLinkApi {

    private static final Gson GSON = new Gson();
    private static final int MAX_RESPONSE_CHARS = 262_144;

    private final AccountLinkConfig config;
    private final OttoExtraConfig rootConfig;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    AccountLinkApi(AccountLinkConfig config, OttoExtraConfig rootConfig) {
        this.config = config;
        this.rootConfig = rootConfig;
    }

    CompletableFuture<LinkStart> start(String minecraftUuid, String minecraftName) {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", minecraftUuid);
        body.addProperty("minecraft_name", minecraftName);
        body.addProperty("mod_version", version());
        return send("POST", "/api/v1/link/start", body, false).thenApply(json -> {
            String authorizationUrl = requiredString(json, "authorization_url");
            validateAuthorizationUrl(authorizationUrl);
            return new LinkStart(authorizationUrl, requiredString(json, "device_token"));
        });
    }

    CompletableFuture<String> status() {
        return send("GET", "/api/v1/link/status", null, true)
                .thenApply(json -> requiredString(json, "status"));
    }

    CompletableFuture<Void> unlink() {
        return send("DELETE", "/api/v1/link", null, true).thenApply(ignored -> null);
    }

    CompletableFuture<AccountSummary> summary() {
        return send("GET", "/api/v1/account", null, true).thenApply(json ->
                new AccountSummary(
                        requiredString(json, "status"),
                        optionalString(json, "minecraft_name"),
                        optionalString(json, "discord_name"),
                        json.has("profile") && json.get("profile").isJsonObject()
                                ? GSON.fromJson(json.get("profile"), PlayerRecord.class)
                                : null));
    }

    private CompletableFuture<JsonObject> send(
            String method, String path, JsonObject body, boolean authenticated) {
        // Unlink is an explicit user request to end processing, available even after a withdrawal.
        ConsentPermit permit = "DELETE".equals(method)
                ? new ConsentPermit.Gate().capture(() -> true)
                : ConsentPolicy.capture(rootConfig, ConsentPolicy.Purpose.ONLINE_SERVICES);
        URI uri;
        try {
            uri = endpoint().resolve(path);
        } catch (IllegalArgumentException error) {
            return CompletableFuture.failedFuture(error);
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
                .header("Accept", "application/json")
                .header("User-Agent", "OttoExtra/" + version());
        if (authenticated) {
            if (!config.hasDeviceToken()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Kein Device-Token"));
            }
            request.header("Authorization", "Bearer " + config.deviceToken);
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
        }

        return ConsentHttp.send(http, request.build(), de.ottoextra.privacy.BoundedBody.text(64 * 1024), permit)
                .thenApply(response -> parse(response.statusCode(), response.body()));
    }

    private URI endpoint() {
        URI endpoint = URI.create(config.endpointUrl);
        if (!ConsentHttp.sameOrigin(endpoint, URI.create("https://ottoextra.dev"))) {
            throw new IllegalArgumentException("Die Konto-Freigabe gilt nur für https://ottoextra.dev");
        }
        String host = endpoint.getHost();
        boolean local = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
        if (host == null || (!"https".equalsIgnoreCase(endpoint.getScheme())
                && !(local && "http".equalsIgnoreCase(endpoint.getScheme())))
                || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("Ungültiger OttoExtra-Endpunkt");
        }
        return endpoint;
    }

    private static void validateAuthorizationUrl(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"ottoextra.dev".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null || uri.getFragment() != null
                || uri.getPort() != -1 && uri.getPort() != 443
                || !uri.getPath().startsWith("/link/")) {
            throw new IllegalStateException("API lieferte einen ungültigen Anmeldelink");
        }
    }

    private static JsonObject parse(int status, String body) {
        if (body != null && body.length() > MAX_RESPONSE_CHARS) {
            throw new IllegalStateException("API-Antwort zu groß");
        }
        JsonObject json = new JsonObject();
        if (body != null && !body.isBlank()) {
            JsonElement parsed;
            try {
                parsed = GSON.fromJson(body, JsonElement.class);
            } catch (RuntimeException error) {
                throw new IllegalStateException("API-Antwort ist kein JSON", error);
            }
            if (parsed != null && parsed.isJsonObject()) {
                json = parsed.getAsJsonObject();
            }
        }
        if (status < 200 || status >= 300) {
            String message = json.has("error") ? json.get("error").getAsString() : "HTTP " + status;
            throw new ApiException(status, message);
        }
        return json;
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("API-Antwort ohne '" + key + "'");
        }
        return json.get(key).getAsString();
    }

    private static String optionalString(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                ? json.get(key).getAsString() : "";
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("ottoextra")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    record LinkStart(String authorizationUrl, String deviceToken) {
    }

    record AccountSummary(
            String status,
            String minecraftName,
            String discordName,
            PlayerRecord profile) {
    }

    static final class ApiException extends RuntimeException {
        private final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        int status() {
            return status;
        }
    }
}
