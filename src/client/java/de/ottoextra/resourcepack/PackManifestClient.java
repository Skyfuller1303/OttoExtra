package de.ottoextra.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.api.ApiProblem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Lädt und parst die Update-Quelle. Unterstützt zwei Schemata:
 *
 * <ul>
 *   <li><b>GitHub Releases</b> ({@code api.github.com/.../releases/latest}) — wird
 *       automatisch erkannt. Liest {@code tag_name} als Version und das passende
 *       Asset ({@code name}, {@code browser_download_url}, {@code digest}).</li>
 *   <li><b>Eigenes Manifest</b> (latest.json) mit {@code url}/{@code sha256}.</li>
 * </ul>
 *
 * <p>GitHub verlangt einen {@code User-Agent}; der wird gesetzt. Ohne Auth gilt das
 * GitHub-Limit von 60 Anfragen/Stunde/IP — bei einem Check pro Start unkritisch.</p>
 */
public final class PackManifestClient {

    private final Gson gson = new Gson();
    private final HttpClient http;
    private final Duration requestTimeout;

    public PackManifestClient(HttpClient http, Duration requestTimeout) {
        this.http = http;
        this.requestTimeout = requestTimeout;
    }

    public CompletableFuture<PackManifest> fetch(URI manifestUri, String assetName) {
        boolean github = isGitHub(manifestUri);
        HttpRequest.Builder builder = HttpRequest.newBuilder(manifestUri)
                .timeout(requestTimeout)
                .header("User-Agent", "OttoExtra-resourcepack")
                .GET();
        if (github) {
            builder.header("Accept", "application/vnd.github+json");
            builder.header("X-GitHub-Api-Version", "2022-11-28");
        } else {
            builder.header("Accept", "application/json");
        }

        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() / 100 != 2) {
                        throw ApiProblem.httpStatus(manifestUri, resp.statusCode()).toException();
                    }
                    try {
                        return github
                                ? parseGitHubRelease(manifestUri, resp.body(), assetName)
                                : parseManifest(manifestUri, resp.body());
                    } catch (ApiProblem.ApiException e) {
                        throw e;
                    } catch (Exception e) {
                        throw ApiProblem.parse(manifestUri, e.getMessage()).toException();
                    }
                });
    }

    private static boolean isGitHub(URI uri) {
        String host = uri.getHost();
        return host != null && host.toLowerCase(Locale.ROOT).endsWith("api.github.com");
    }

    // ---- GitHub Releases -------------------------------------------------

    private PackManifest parseGitHubRelease(URI uri, String body, String assetName) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        String tag = optString(root, "tag_name");

        JsonArray assets = root.has("assets") && root.get("assets").isJsonArray()
                ? root.getAsJsonArray("assets") : null;
        if (assets == null) {
            throw ApiProblem.parse(uri, "Release ohne assets").toException();
        }

        JsonObject match = null;
        for (JsonElement el : assets) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject a = el.getAsJsonObject();
            if (assetName != null && assetName.equals(optString(a, "name"))) {
                match = a;
                break;
            }
        }
        if (match == null) {
            throw ApiProblem.parse(uri, "Asset '" + assetName + "' nicht im Release '" + tag + "'").toException();
        }

        String url = optString(match, "browser_download_url");
        String sha = normalizeDigest(optString(match, "digest"));
        Long size = optLong(match, "size");
        String notes = "GitHub Release " + (tag != null ? tag : "?");
        return new PackManifest(tag, url, sha, size, null, null, notes);
    }

    /** "sha256:abcdef..." -> "abcdef...", sonst durchreichen/null. */
    private static String normalizeDigest(String digest) {
        if (digest == null || digest.isBlank()) {
            return null;
        }
        String d = digest.trim();
        int colon = d.indexOf(':');
        return colon >= 0 ? d.substring(colon + 1) : d;
    }

    // ---- Eigenes latest.json --------------------------------------------

    private PackManifest parseManifest(URI uri, String body) {
        PackManifest m = gson.fromJson(body, PackManifest.class);
        if (m == null || !m.hasUrl()) {
            throw ApiProblem.parse(uri, "Manifest ohne 'url'").toException();
        }
        return m;
    }

    // ---- Helfer ----------------------------------------------------------

    private static String optString(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e != null && !e.isJsonNull()) ? e.getAsString() : null;
    }

    private static Long optLong(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e != null && !e.isJsonNull()) ? e.getAsLong() : null;
    }
}
