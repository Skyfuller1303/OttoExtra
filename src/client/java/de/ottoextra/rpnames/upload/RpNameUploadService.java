package de.ottoextra.rpnames.upload;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiRoutes;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class RpNameUploadService {

    private static final int MAX_RP_NAME_CODE_POINTS = 120;
    private static final int MAX_RESPONSE_CHARS = 16_384;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(
            daemonThreads());

    private static final ConcurrentHashMap<String, String> OBSERVED_IDENTITIES =
            new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private RpNameUploadService() {
    }

    public record Result(boolean attempted, boolean success, int statusCode, String message) {
        public static Result skipped() {
            return new Result(false, true, 0, "");
        }

        public static Result failed(String message) {
            return new Result(true, false, 0, safeMessage(message));
        }
    }

    public static CompletableFuture<Result> uploadObservedIdentity(
            String targetAccountName,
            String targetUuid,
            String rpName) {

        if (rpName == null || rpName.isBlank()) {
            return CompletableFuture.completedFuture(Result.skipped());
        }

        OttoExtraConfig config = OttoExtraConfig.active();
        UploadData data;
        try {
            data = createUploadData(targetAccountName, targetUuid, rpName);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(Result.failed(e.getMessage()));
        }

        String key = data.targetKey();
        String value = data.identityValue();
        String previous = OBSERVED_IDENTITIES.putIfAbsent(key, value);
        if (value.equals(previous)) {
            return CompletableFuture.completedFuture(Result.skipped());
        }
        if (previous != null) {
            OBSERVED_IDENTITIES.put(key, value);
        }

        return CompletableFuture.supplyAsync(() -> send(config, data), EXECUTOR)
                .thenApply(result -> {
                    if (!result.success()) {
                        OBSERVED_IDENTITIES.remove(key, value);
                    }
                    return result;
                })
                .exceptionally(error -> {
                    OBSERVED_IDENTITIES.remove(key, value);
                    Throwable cause = unwrap(error);
                    OttoExtra.LOGGER.warn("[rpnames] Im Chat erkannter RP-Name konnte nicht übertragen werden: {}",
                            cause.toString());
                    return Result.failed(cause.getMessage());
                });
    }

    static UploadData createUploadData(String targetAccountName, String targetUuid, String rpName) {
        MinecraftClient client = MinecraftClient.getInstance();
        String actorUuid = null;
        String actorName = null;

        if (client != null) {
            if (client.player != null) {
                actorUuid = normalizeUuid(client.player.getUuidAsString());
                if (client.player.getGameProfile() != null) {
                    actorName = clean(client.player.getGameProfile().name());
                }
            }
            if (actorName == null && client.getSession() != null) {
                actorName = clean(client.getSession().getUsername());
            }
        }

        return createUploadDataForActor(actorUuid, actorName,
                targetAccountName, targetUuid, rpName);
    }

    static UploadData createUploadDataForActor(
            String actorUuid,
            String actorName,
            String targetAccountName,
            String targetUuid,
            String rpName) {

        String normalizedActorUuid = normalizeUuid(actorUuid);
        String normalizedActorName = clean(actorName);
        if (normalizedActorUuid == null && normalizedActorName == null) {
            throw new IllegalArgumentException("Eigener Minecraft-Account ist nicht verfügbar");
        }

        String targetName = clean(targetAccountName);
        String normalizedTargetUuid = normalizeUuid(targetUuid);
        String normalizedRpName = limitCodePoints(rpName == null ? "" : rpName.trim(),
                MAX_RP_NAME_CODE_POINTS);

        boolean targetsSelf = normalizedActorUuid != null
                && normalizedActorUuid.equalsIgnoreCase(normalizedTargetUuid)
                || normalizedActorName != null && targetName != null
                && normalizedActorName.equalsIgnoreCase(targetName);

        if (!targetsSelf && normalizedTargetUuid == null && targetName == null) {
            throw new IllegalArgumentException("Zielspieler konnte nicht bestimmt werden");
        }

        return new UploadData(normalizedActorUuid, normalizedActorName,
                targetsSelf ? null : normalizedTargetUuid,
                targetsSelf ? null : targetName,
                normalizedRpName);
    }

    private static Result send(OttoExtraConfig config, UploadData data) {
        URI uri = new OttoExtraApiRoutes(config.api.baseUrl).communityParticipantRpName();
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return Result.failed("Unsichere API-Adresse wurde abgelehnt");
        }

        JsonObject json = data.toJson();
        String requestUserAgent = userAgent();
        Duration timeout = Duration.ofMillis(Math.max(1_000, config.api.requestTimeoutMs));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("User-Agent", requestUserAgent)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();

        // Intentionally logged at INFO so the exact outgoing request is visible
        // in latest.log without enabling debug logging. UUIDs and RP name are
        // part of the payload; passwords or tokens are never included.
        OttoExtra.LOGGER.info("[rpnames] Ausgehender RP-Namen-Upload:\n{}",
                formatRequestForLog(uri, requestUserAgent, json));

        try {
            HttpResponse<String> response = HTTP.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() == null ? "" : response.body();
            if (body.length() > MAX_RESPONSE_CHARS) {
                body = body.substring(0, MAX_RESPONSE_CHARS);
            }

            OttoExtra.LOGGER.info("[rpnames] Antwort auf RP-Namen-Upload: HTTP {} | Body: {}",
                    response.statusCode(), responseBodyForLog(body));

            String apiMessage = responseMessage(body);
            boolean httpSuccess = response.statusCode() >= 200 && response.statusCode() < 300;
            boolean apiSuccess = !explicitApiFailure(body);
            if (httpSuccess && apiSuccess) {
                OttoExtra.LOGGER.info("[rpnames] Im RP-Chat erkannten RP-Namen für {} an API übertragen.",
                        data.targetDescription());
                return new Result(true, true, response.statusCode(), apiMessage);
            }

            String message = !apiMessage.isBlank()
                    ? apiMessage
                    : "HTTP " + response.statusCode();
            OttoExtra.LOGGER.warn("[rpnames] RP-Namen-Upload abgelehnt ({}): {}",
                    response.statusCode(), message);
            return new Result(true, false, response.statusCode(), message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            OttoExtra.LOGGER.warn("[rpnames] RP-Namen-Upload wurde unterbrochen.");
            return Result.failed("Übertragung wurde unterbrochen");
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] HTTP-Fehler beim RP-Namen-Upload: {}",
                    e.toString());
            return Result.failed(e.getClass().getSimpleName() + ": " + safeMessage(e.getMessage()));
        }
    }


    static String formatRequestForLog(URI uri, String requestUserAgent, JsonObject json) {
        return "POST " + uri + "\n"
                + "Content-Type: application/json; charset=UTF-8\n"
                + "Accept: application/json\n"
                + "User-Agent: " + requestUserAgent + "\n"
                + "Body: " + json;
    }

    private static String responseBodyForLog(String body) {
        if (body == null || body.isBlank()) {
            return "<leer>";
        }
        String cleaned = body.replace('\r', ' ').replace('\n', ' ').trim();
        return cleaned.length() > 2_000 ? cleaned.substring(0, 2_000) + "…" : cleaned;
    }

    private static boolean explicitApiFailure(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            return parsed.isJsonObject()
                    && parsed.getAsJsonObject().has("ok")
                    && !parsed.getAsJsonObject().get("ok").getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String responseMessage(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return "";
            }
            JsonObject obj = parsed.getAsJsonObject();
            for (String key : new String[]{"message", "error", "detail"}) {
                if (obj.has(key) && !obj.get(key).isJsonNull()) {
                    return safeMessage(obj.get(key).getAsString());
                }
            }
        } catch (Exception ignored) {
            // A successful endpoint is allowed to return an empty/non-JSON body.
        }
        return "";
    }

    private static String userAgent() {
        String version = FabricLoader.getInstance().getModContainer("ottoextra")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("client");
        return "OttoExtra/" + version;
    }

    private static String normalizeUuid(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        try {
            return java.util.UUID.fromString(cleaned).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String limitCodePoints(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        int end = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, end);
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unbekannter Fehler";
        }
        String cleaned = message.replace('\r', ' ').replace('\n', ' ').trim();
        return cleaned.length() > 240 ? cleaned.substring(0, 240) : cleaned;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static ThreadFactory daemonThreads() {
        AtomicInteger index = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable,
                    "ottoextra-rpname-upload-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /** Clears chat-observation de-duplication for a new server connection. */
    public static void resetObservedSession() {
        OBSERVED_IDENTITIES.clear();
    }

    public static void shutdown() {
        resetObservedSession();
        EXECUTOR.shutdownNow();
    }

    record UploadData(
            String actorUuid,
            String actorName,
            String targetUuid,
            String targetName,
            String rpName) {

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (actorUuid != null) {
                json.addProperty("uuid", actorUuid);
            } else {
                json.addProperty("player_name", actorName);
            }
            if (targetUuid != null) {
                json.addProperty("target_uuid", targetUuid);
            } else if (targetName != null) {
                json.addProperty("target_player_name", targetName);
            }
            json.addProperty("rp_name", rpName);
            return json;
        }

        String targetKey() {
            return targetUuid != null ? targetUuid
                    : targetName != null ? targetName.toLowerCase(java.util.Locale.ROOT)
                    : actorUuid != null ? actorUuid
                    : actorName.toLowerCase(java.util.Locale.ROOT);
        }

        String identityValue() {
            return rpName;
        }

        String targetDescription() {
            if (targetName != null) {
                return targetName;
            }
            if (targetUuid != null) {
                return targetUuid;
            }
            return actorName != null ? actorName : actorUuid;
        }
    }

}
