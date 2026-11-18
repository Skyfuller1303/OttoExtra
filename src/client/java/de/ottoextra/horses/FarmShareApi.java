package de.ottoextra.horses;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.privacy.BoundedBody;
import de.ottoextra.privacy.ConsentHttp;
import de.ottoextra.privacy.ConsentPolicy;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client of the anonymous farm list service ({@code /api/v1/farm-lists/{listId}}). No account, no cookies,
 * no player data; the write code travels only in the {@code X-Farm-List-Write} header of a PATCH and only to
 * {@code https://ottoextra.dev} (or a loopback address for development). Requests go through
 * {@link ConsentHttp} with the consent for online services and a bounded response body.
 */
final class FarmShareApi {
    static final String DEFAULT_BASE_URL = "https://ottoextra.dev";
    static final String WRITE_HEADER = "X-Farm-List-Write";
    private static final String PATH = "/api/v1/farm-lists/";
    /** A full list (300 farms, 1000 animals, all fields at their maximum) stays well below this. */
    static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    /** Result of a failed request; {@code status} is 0 for network errors. */
    static final class ShareException extends RuntimeException {
        final int status;

        ShareException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /**
     * Normalized base URI, or null unless it is {@code https://ottoextra.dev} (port 443) or a loopback address
     * (http or https, for development), without path, query or user. Other hosts are refused because the
     * write code is a password of the ottoextra.dev service.
     */
    static URI baseUri(String value) {
        if (value == null) return null;
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException error) {
            return null;
        }
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            return null;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!path.isEmpty() && !path.equals("/")) return null;
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean allowed = local ? https || "http".equalsIgnoreCase(uri.getScheme())
                : https && "ottoextra.dev".equalsIgnoreCase(host) && (uri.getPort() == -1 || uri.getPort() == 443);
        if (!allowed) return null;
        return URI.create(uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getRawAuthority());
    }

    /** Link page of a code: {@code <base>/h/<code>}. */
    static String joinUrl(String baseUrl, String code) {
        URI base = baseUri(baseUrl);
        return (base == null ? DEFAULT_BASE_URL : base.toString()) + "/h/" + FarmShareCodes.normalize(code);
    }

    CompletableFuture<FarmShareWire.Remote> fetch(URI base, String listId, long since) {
        String path = PATH + enc(listId) + (since > 0 ? "?since=" + since : "");
        if (!allowed(base)) return CompletableFuture.failedFuture(new ShareException(0, "Adresse nicht erlaubt"));
        HttpRequest request = request(base, path).GET().build();
        return send(request).thenApply(response -> FarmShareWire.remote(parse(response.statusCode(), response.body())));
    }

    CompletableFuture<JsonObject> patch(URI base, String listId, String writeCode, JsonObject body) {
        if (writeCode == null || FarmShareCodes.kind(writeCode) != FarmShareCodes.Kind.WRITE) {
            return CompletableFuture.failedFuture(new ShareException(403, "kein Schreibcode"));
        }
        // The write code must never reach another host.
        if (!allowed(base)) return CompletableFuture.failedFuture(new ShareException(0, "Adresse nicht erlaubt"));
        String text = body.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > FarmShareWire.MAX_BODY_BYTES) {
            return CompletableFuture.failedFuture(new ShareException(413, "Liste zu groß"));
        }
        HttpRequest request = request(base, PATH + enc(listId))
                .header("Content-Type", "application/json")
                .header(WRITE_HEADER, FarmShareCodes.normalize(writeCode))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(text)).build();
        return send(request).thenApply(response -> parse(response.statusCode(), response.body()));
    }

    /** True for a base that {@link #baseUri(String)} accepts unchanged. */
    static boolean allowed(URI base) {
        URI checked = base == null ? null : baseUri(base.toString());
        return checked != null && checked.equals(base);
    }

    private CompletableFuture<HttpResponse<String>> send(HttpRequest request) {
        var permit = ConsentPolicy.capture(ConsentPolicy.Purpose.ONLINE_SERVICES);
        return ConsentHttp.send(http, request, BoundedBody.text(MAX_RESPONSE_BYTES), permit);
    }

    private static HttpRequest.Builder request(URI base, String path) {
        return HttpRequest.newBuilder(URI.create(base.toString() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "OttoExtra/client");
    }

    static JsonObject parse(int status, String body) {
        if (body == null || body.length() > MAX_RESPONSE_BYTES) throw new ShareException(status, "Antwort ungültig");
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

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
