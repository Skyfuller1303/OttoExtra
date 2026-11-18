package de.ottoextra.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** HTTP client of the anonymous list service. No account, no cookies, no player position. */
final class ResourceShareApi {
    static final String DEFAULT_BASE_URL = "https://ottoextra.dev";
    static final String WRITE_HEADER = "X-Resource-List-Write";
    private static final int MAX_RESPONSE_CHARS = 524_288;
    private static final int MAX_BODY_BYTES = 262_144;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** {@code meta} is null when the server sent no header (keep the local one). */
    record Remote(long serverTime, Map<String, ZoneRecord> zones, Map<String, TimerRecord> timers,
                  ListMeta meta, int invalid) { }

    /** Result of a failed request; {@code status} is 0 for network errors. */
    static final class ShareException extends RuntimeException {
        final int status;

        ShareException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /** Returns the normalized base URI, or null unless it is https (or http for localhost). */
    static URI baseUri(String value) {
        if (value == null) return null;
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException error) {
            return null;
        }
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null) return null;
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!path.isEmpty() && !path.equals("/")) return null;
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "[::1]".equals(host);
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        if (!https && !(local && "http".equalsIgnoreCase(uri.getScheme()))) return null;
        return URI.create(uri.getScheme().toLowerCase(java.util.Locale.ROOT) + "://"
                + uri.getRawAuthority());
    }

    static String joinUrl(String baseUrl, String code) {
        URI base = baseUri(baseUrl);
        return (base == null ? DEFAULT_BASE_URL : base.toString()) + "/r/" + code;
    }

    CompletableFuture<Remote> fetch(URI base, String listId, long since) {
        String path = "/api/v1/resource-lists/" + enc(listId)
                + (since > 0 ? "?since=" + since : "");
        HttpRequest request = request(base, path).GET().build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> remote(parse(response.statusCode(), response.body())));
    }

    CompletableFuture<JsonObject> patch(URI base, String listId, String writeCode, JsonObject body) {
        String text = body.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) {
            return CompletableFuture.failedFuture(new ShareException(413, "Liste zu groß"));
        }
        HttpRequest request = request(base, "/api/v1/resource-lists/" + enc(listId))
                .header("Content-Type", "application/json")
                .header(WRITE_HEADER, writeCode)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(text)).build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(response.statusCode(), response.body()));
    }

    private static HttpRequest.Builder request(URI base, String path) {
        return HttpRequest.newBuilder(URI.create(base.toString() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "OttoExtra-Resources/" + version());
    }

    static JsonObject parse(int status, String body) {
        if (body == null || body.length() > MAX_RESPONSE_CHARS) {
            throw new ShareException(status, "Antwort ungültig");
        }
        JsonObject json;
        try {
            var parsed = JsonParser.parseString(body);
            json = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException error) {
            if (status >= 200 && status < 300) throw new ShareException(status, "Antwort ist kein JSON");
            json = new JsonObject();
        }
        if (status < 200 || status >= 300) {
            String message = json.has("error") && json.get("error").isJsonPrimitive()
                    ? json.get("error").getAsString() : "HTTP " + status;
            throw new ShareException(status, message);
        }
        return json;
    }

    /** Parses a list response; invalid single entries are skipped and counted. */
    static Remote remote(JsonObject json) {
        long serverTime = ResourceWire.requiredLong(json, "serverTime");
        Map<String, ZoneRecord> zones = new LinkedHashMap<>();
        Map<String, TimerRecord> timers = new LinkedHashMap<>();
        int invalid = 0;
        if (json.has("zones") && json.get("zones").isJsonObject()) {
            for (var entry : json.getAsJsonObject("zones").entrySet()) {
                try {
                    ZoneRecord record = ResourceWire.zone(entry.getValue().getAsJsonObject());
                    // Updates from the future would win every merge for a long time.
                    if (record.updatedAt() > serverTime + 60_000L) record = record.withUpdatedAt(serverTime);
                    zones.put(record.id(), record);
                } catch (RuntimeException error) {
                    invalid++;
                }
            }
        }
        if (json.has("timers") && json.get("timers").isJsonObject()) {
            for (var entry : json.getAsJsonObject("timers").entrySet()) {
                try {
                    TimerRecord timer = ResourceWire.timer(entry.getValue().getAsJsonObject());
                    if (timer.updatedAt() > serverTime + 60_000L) {
                        timer = new TimerRecord(timer.depletedAt(), timer.by(), serverTime);
                    }
                    timers.put(entry.getKey(), timer);
                } catch (RuntimeException error) {
                    invalid++;
                }
            }
        }
        ListMeta meta = null;
        if (json.has("meta") && json.get("meta").isJsonObject()) {
            try {
                meta = ResourceWire.meta(json.getAsJsonObject("meta"));
            } catch (RuntimeException error) {
                invalid++;
            }
        }
        return new Remote(serverTime, zones, timers, meta, invalid);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("ottoextra")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }
}
