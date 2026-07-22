package de.ottoextra.rpnames.importer;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import de.ottoextra.OttoExtra;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RegionsApiRpNameImporter {

    private static final String URL =
            "https://api.ottoextra.dev/api/index.php?action=public-player-compact";
    private static final Gson GSON = new Gson();

    public record Result(int total, int updated, int created, int conflicts, String error) {
        public static Result failure(String error) {
            return new Result(0, 0, 0, 0, error);
        }

        public boolean ok() {
            return error == null;
        }
    }

    private static final class Envelope {
        boolean ok;
        List<Player> players;
    }

    private static final class Player {
        String uuid;
        @SerializedName("minecraft_name")
        String minecraftName;
        @SerializedName("rp_name")
        String rpName;
        String title;
        String state;
    }

    private RegionsApiRpNameImporter() {
    }

    public static CompletableFuture<Result> run(LocalRpIdentityStore store, boolean createMissing) {
        return CompletableFuture.supplyAsync(() -> doImport(store, createMissing, true));
    }

    public static CompletableFuture<Result> runAuto(LocalRpIdentityStore store) {
        return CompletableFuture.supplyAsync(() -> doImport(store, false, false));
    }

    private static Result doImport(LocalRpIdentityStore store, boolean createMissing, boolean backup) {
        Envelope env;
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Result.failure("HTTP " + response.statusCode());
            }
            env = GSON.fromJson(response.body(), Envelope.class);
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] API-Import fehlgeschlagen: {}", e.toString());
            return Result.failure(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (env == null || !env.ok || env.players == null) {
            return Result.failure("Antwort unbrauchbar");
        }

        if (backup) {
            store.backup();
        }

        String selfName = null;
        try {
            selfName = net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername();
        } catch (Throwable ignored) {
        }
        int updated = 0;
        int created = 0;
        int conflicts = 0;
        for (Player p : env.players) {
            String account = clean(p.minecraftName);
            if (account == null) {
                continue;
            }
            boolean isSelf = account.equalsIgnoreCase(selfName);
            String rpName = clean(p.rpName);
            String title = clean(p.title);
            String group = clean(p.state);

            LocalRpProfile existing = store.find(p.uuid, account).orElse(null);
            if (existing != null) {
                boolean hadConflict = existing.apiConflict != null;
                boolean changed = store.importApi(account, p.uuid, rpName, title);
                if (applyGroup(existing, group)) {
                    changed = true;
                }
                if (changed) {
                    updated++;
                }
                if (!hadConflict && existing.apiConflict != null) {
                    conflicts++;
                }
            } else if ((createMissing || isSelf) && (rpName != null || title != null)) {
                LocalRpProfile profile = new LocalRpProfile();
                profile.uuid = p.uuid;
                profile.accountName = account;
                if (rpName != null) {
                    profile.rpName = rpName;
                    profile.apiRpName = rpName;
                }
                if (title != null) {
                    profile.title = title;
                }
                if (group != null) {
                    profile.titleGroup = group;
                }
                profile.knowledgeState = KnowledgeState.API_IMPORTED;
                profile.source = RpNameSource.API_IMPORTED;
                long now = System.currentTimeMillis();
                profile.firstSeenAt = now;
                profile.lastSeenAt = now;
                profile.lastUpdatedAt = now;
                store.insert(profile);
                created++;
            }
        }
        store.saveNow();
        DebugLog.debug("[rpnames] API-Import: {} Spieler, {} ergänzt, {} angelegt, {} Konflikte.",
                env.players.size(), updated, created, conflicts);
        return new Result(env.players.size(), updated, created, conflicts, null);
    }

    private static boolean applyGroup(LocalRpProfile profile, String group) {
        if (group == null || profile.locked || !profile.knowledgeState.allowsAutomaticUpdates()
                || (profile.titleGroup != null && !profile.titleGroup.isBlank())) {
            return false;
        }
        profile.titleGroup = group;
        return true;
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String fixed = s;
        for (int i = 0; i < 2 && (fixed.indexOf('Ã') >= 0 || fixed.indexOf('Â') >= 0); i++) {
            String decoded = new String(fixed.getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);
            if (decoded.indexOf('�') >= 0) {
                break;
            }
            fixed = decoded;
        }
        fixed = fixed.trim();
        return fixed.isEmpty() ? null : fixed;
    }
}
