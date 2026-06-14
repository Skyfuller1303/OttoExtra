package de.ottoextra.rpnames.importer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Lokaler Import aus den OttoPlus/OttoTalk-Caches in den RP-Namen-Store.
 *
 * <p>Primärquelle {@code ottotalk_players.json} — importiert wird NUR der
 * RP-Name; Titel und Titelfarbe werden bewusst ignoriert (machten Probleme).
 * Optional {@code ottoletter-player-cache.json} (UUID je Account). Dateien
 * liegen in {@code config/ottoextra/import/}. Im Gegensatz zum Regions-API-Import
 * überschreibt OttoPlus vorhandene Werte autoritativ; gesperrte Profile bleiben
 * unberührt. Details: {@code docs/ottoplus-import-integration.md}.</p>
 */
public final class OttoPlusImporter {

    public static final String PLAYERS_FILE = "ottotalk_players.json";
    public static final String UUID_FILE = "ottoletter-player-cache.json";

    private static final Gson GSON = new Gson();

    /** Ergebnis-Statistik für die UI. */
    public record Result(int total, int updated, int created, int skippedLocked, String error) {
        public static Result failure(String error) {
            return new Result(0, 0, 0, 0, error);
        }

        public boolean ok() {
            return error == null;
        }
    }

    /** OttoTalk-RP-Profil (ottotalk_players.json). */
    private static final class TalkPlayer {
        String accountName;
        String characterName;
        String characterTitle;
        int characterTitleColor;
    }

    /** OttoLetter-Spielereintrag (ottoletter-player-cache.json) — nur UUID genutzt. */
    private static final class LetterPlayer {
        String uuid;
        String name;
    }

    private OttoPlusImporter() {
    }

    public static CompletableFuture<Result> run(LocalRpIdentityStore store, boolean createMissing) {
        return CompletableFuture.supplyAsync(() -> doImport(store, createMissing));
    }

    private static Result doImport(LocalRpIdentityStore store, boolean createMissing) {
        Path playersFile = OttoExtraPaths.importDir().resolve(PLAYERS_FILE);
        if (!Files.exists(playersFile)) {
            return Result.failure(OttoExtraPaths.importDir().toString());
        }
        List<TalkPlayer> players;
        try {
            Type listType = new TypeToken<List<TalkPlayer>>() {
            }.getType();
            players = GSON.fromJson(
                    Files.readString(playersFile, StandardCharsets.UTF_8), listType);
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] OttoPlus-Import: {} unlesbar: {}",
                    PLAYERS_FILE, e.toString());
            return Result.failure(e.getClass().getSimpleName());
        }
        if (players == null) {
            return Result.failure("leer");
        }

        Map<String, String> uuidByAccount = loadUuidMap();

        store.backup();
        int updated = 0;
        int created = 0;
        int skippedLocked = 0;
        for (TalkPlayer p : players) {
            if (p == null) {
                continue;
            }
            String account = clean(p.accountName);
            if (account == null) {
                continue;
            }
            String rpName = rpName(p.characterName);
            String uuid = uuidByAccount.get(account.toLowerCase(Locale.ROOT));

            LocalRpProfile existing = store.find(uuid, account).orElse(null);
            if (existing != null) {
                if (existing.locked) {
                    skippedLocked++;
                    continue;
                }
                // Nur RP-Name importieren — Titel/Farbe bewusst NICHT (null lassen);
                // importOttoPlus fasst vorhandene Titel/Farben dann nicht an.
                if (store.importOttoPlus(account, uuid, rpName, null, null)) {
                    updated++;
                }
            } else if (createMissing && rpName != null) {
                LocalRpProfile profile = new LocalRpProfile();
                profile.uuid = uuid;
                profile.accountName = account;
                profile.rpName = rpName;
                profile.knowledgeState = KnowledgeState.KNOWN;
                profile.source = RpNameSource.IMPORTED_FROM_OTTOPLUS;
                long now = System.currentTimeMillis();
                profile.firstSeenAt = now;
                profile.lastSeenAt = now;
                profile.lastUpdatedAt = now;
                store.insert(profile);
                created++;
            }
        }
        store.saveNow();
        OttoExtra.LOGGER.info("[rpnames] OttoPlus-Import: {} Einträge, {} aktualisiert, "
                + "{} angelegt, {} gesperrt übersprungen.",
                players.size(), updated, created, skippedLocked);
        return new Result(players.size(), updated, created, skippedLocked, null);
    }

    /** Account → UUID aus der OttoLetter-Cache-Datei (optional). */
    private static Map<String, String> loadUuidMap() {
        Map<String, String> map = new HashMap<>();
        Path file = OttoExtraPaths.importDir().resolve(UUID_FILE);
        if (!Files.exists(file)) {
            return map;
        }
        try {
            Type listType = new TypeToken<List<LetterPlayer>>() {
            }.getType();
            List<LetterPlayer> list = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8), listType);
            if (list != null) {
                for (LetterPlayer lp : list) {
                    if (lp != null && lp.name != null && lp.uuid != null
                            && !lp.name.isBlank() && !lp.uuid.isBlank()) {
                        map.put(lp.name.trim().toLowerCase(Locale.ROOT), lp.uuid.trim());
                    }
                }
            }
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] OttoPlus-Import: {} ignoriert ({}).",
                    UUID_FILE, e.toString());
        }
        return map;
    }

    /** RP-Name säubern; "Unbekannt"/leer → null (keine Daten). */
    private static String rpName(String raw) {
        String s = clean(raw);
        if (s == null || s.equalsIgnoreCase(LocalRpProfile.UNKNOWN_NAME)) {
            return null;
        }
        return s;
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
