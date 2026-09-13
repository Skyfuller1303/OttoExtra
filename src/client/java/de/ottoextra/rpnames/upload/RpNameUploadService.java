package de.ottoextra.rpnames.upload;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.FailureLogGate;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class RpNameUploadService {

    private static final int MAX_RP_NAME_CODE_POINTS = 120;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(
            daemonThreads());

    private static final ConcurrentHashMap<String, ObservedIdentity> OBSERVED_IDENTITIES =
            new ConcurrentHashMap<>();
    private static final FailureLogGate FAILURE_LOG = new FailureLogGate();

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
        if (!de.ottoextra.privacy.ConsentPolicy.allowed(config,
                de.ottoextra.privacy.ConsentPolicy.Purpose.COMMUNITY_UPLOADS)) {
            return CompletableFuture.completedFuture(Result.skipped());
        }
        UploadData data;
        try {
            data = createUploadData(targetAccountName, targetUuid, rpName);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(Result.failed(e.getMessage()));
        }

        return enqueue(config, data, EXECUTOR, RpNameUploadService::send);
    }

    private record ObservedIdentity(String value, OttoExtraConfig.Consent consent, long generation) { }

    @FunctionalInterface
    interface Transport {
        Result send(OttoExtraConfig config, UploadData data, java.util.function.BooleanSupplier allowed);
    }

    static CompletableFuture<Result> enqueue(OttoExtraConfig config, UploadData data,
            java.util.concurrent.Executor executor,
            Transport transport) {
        var consent = config.consent;
        long generation = consent.uploadGeneration();
        if (!consent.allowsUpload(generation)) return CompletableFuture.completedFuture(Result.skipped());
        String key = data.targetKey();
        ObservedIdentity value = new ObservedIdentity(data.identityValue(), consent, generation);
        ObservedIdentity previous = OBSERVED_IDENTITIES.putIfAbsent(key, value);
        if (value.equals(previous)) {
            return CompletableFuture.completedFuture(Result.skipped());
        }
        if (previous != null) {
            OBSERVED_IDENTITIES.put(key, value);
        }

        return CompletableFuture.supplyAsync(() -> {
                    java.util.function.BooleanSupplier allowed = () ->
                            config.consent == consent && consent.allowsUpload(generation);
                    if (!allowed.getAsBoolean()) return Result.skipped();
                    return transport.send(config, data, allowed);
                }, executor)
                .thenApply(result -> {
                    if (!result.attempted() || !result.success()) {
                        OBSERVED_IDENTITIES.remove(key, value);
                    }
                    return result;
                })
                .exceptionally(error -> {
                    OBSERVED_IDENTITIES.remove(key, value);
                    Throwable cause = unwrap(error);
                    if (FAILURE_LOG.onFailure()) {
                        OttoExtra.LOGGER.warn(
                                "[rpnames] RP-Namen-Übertragung unerwartet fehlgeschlagen: {}",
                                cause.getClass().getSimpleName());
                    }
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

    private static Result send(OttoExtraConfig config, UploadData data,
                               java.util.function.BooleanSupplier allowed) {
        var permit = de.ottoextra.privacy.ConsentPolicy.capture(config,
                de.ottoextra.privacy.ConsentPolicy.Purpose.COMMUNITY_UPLOADS).restrict(allowed);
        if (!permit.valid()) return Result.skipped();
        var api = de.ottoextra.OttoExtraClient.apiClient();
        if (api == null) return Result.failed("API nicht verfügbar");
        try {
            permit.require();
            JsonObject result = api.submitRpNameObservation(data.toJson(), permit).join();
            boolean success = result.has("ok") && result.get("ok").getAsBoolean();
            return new Result(true, success, 0, responseMessage(result.toString()));
        } catch (java.util.concurrent.CancellationException e) {
            return Result.skipped();
        } catch (Exception e) {
            if (!allowed.getAsBoolean() || !permit.valid()) return Result.skipped();
            if (FAILURE_LOG.onFailure()) {
                OttoExtra.LOGGER.warn("[rpnames] Authentifizierter Upload fehlgeschlagen: {}", e.getClass().getSimpleName());
            }
            return Result.failed("RP-Upload fehlgeschlagen");
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
