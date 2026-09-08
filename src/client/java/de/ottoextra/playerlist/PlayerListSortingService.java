package de.ottoextra.playerlist;

import com.mojang.authlib.GameProfile;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.CompactPlayer;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.rpnames.tablist.TablistNameFormatter;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;

import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

final class PlayerListSortingService {
    private static final int UNKNOWN_TITLE_ORDER = Integer.MAX_VALUE;
    static final long EMPTY_RETRY_INTERVAL_MS = 5_000L;

    private final OttoExtraApiClient api;
    private final RegionDataService regions;
    private final TitleRegistry titles;
    private final OttoExtraConfig.RpNames config;
    private final BooleanSupplier sortingRequested;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicLong refreshInFlightGeneration = new AtomicLong(-1L);
    private final AtomicBoolean activationLogged = new AtomicBoolean();

    private volatile Snapshot snapshot = Snapshot.inactive();
    private volatile boolean joined;
    private volatile boolean sortingWasRequested;
    private volatile long nextRefreshAt;

    PlayerListSortingService(OttoExtraApiClient api, RegionDataService regions,
                             TitleRegistry titles, OttoExtraConfig.RpNames config,
                             BooleanSupplier sortingRequested) {
        this.api = api;
        this.regions = regions;
        this.titles = titles;
        this.config = config;
        this.sortingRequested = sortingRequested;
    }

    synchronized void onServerJoin() {
        generation.incrementAndGet();
        refreshInFlightGeneration.set(-1L);
        activationLogged.set(false);
        snapshot = Snapshot.inactive();
        joined = true;
        sortingWasRequested = false;
        nextRefreshAt = 0L;
        tick();
    }

    /**
     * Erkennt Konfigurationswechsel und wiederholt ausschließlich fehlende
     * Sortierdaten. Bei einem erfolgreichen Snapshot entstehen keine
     * periodischen API-Aufrufe. Erneutes Öffnen der Tabliste lädt die Daten neu.
     */
    void tick() {
        tick(false);
    }

    void onTabOpened() {
        tick(true);
    }

    private synchronized void tick(boolean tabOpened) {
        long now = Util.getMeasuringTimeMs();
        boolean requested = isSortingRequested();
        if (!joined) {
            return;
        }
        if (!requested) {
            if (sortingWasRequested) {
                generation.incrementAndGet();
                refreshInFlightGeneration.set(-1L);
                activationLogged.set(false);
                snapshot = Snapshot.inactive();
                sortingWasRequested = false;
                nextRefreshAt = 0L;
                DebugLog.debug("[playerlist] Lehen-Sortierung deaktiviert.");
            }
            return;
        }
        if (!sortingWasRequested) {
            generation.incrementAndGet();
            refreshInFlightGeneration.set(-1L);
            activationLogged.set(false);
            snapshot = new Snapshot(true, Map.of());
            sortingWasRequested = true;
            nextRefreshAt = 0L;
            DebugLog.debug(
                    "[playerlist] Lehen-Sortierung aktiviert — lade Sortierdaten neu.");
        }

        long currentGeneration = generation.get();
        Snapshot current = snapshot;
        boolean inFlight = refreshInFlightGeneration.get() == currentGeneration;
        if ((tabOpened && !inFlight) || refreshDue(joined, requested, current.players().isEmpty(),
                inFlight, now, nextRefreshAt)) {
            refresh(currentGeneration, now);
        }
    }

    private void refresh(long currentGeneration, long now) {
        if (!refreshInFlightGeneration.compareAndSet(-1L, currentGeneration)) {
            return;
        }
        nextRefreshAt = now + EMPTY_RETRY_INTERVAL_MS;
        regions.bootstrapCompletion()
                .thenCompose(ignored -> generation.get() == currentGeneration
                                && joined && isSortingRequested()
                        ? api.compactPlayers()
                        : CompletableFuture.completedFuture(List.of()))
                .whenComplete((players, error) -> {
                    synchronized (this) {
                        try {
                            if (generation.get() != currentGeneration
                                    || !joined || !isSortingRequested()) {
                                return;
                            }
                            if (error != null) {
                                DebugLog.debug("[playerlist] Sortierdaten nicht verfügbar: {}",
                                        rootMessage(error));
                                nextRefreshAt = Util.getMeasuringTimeMs()
                                        + EMPTY_RETRY_INTERVAL_MS;
                                return;
                            }
                            Map<UUID, PlayerData> indexed = index(players);
                            snapshot = new Snapshot(true, indexed);
                            nextRefreshAt = indexed.isEmpty()
                                    ? Util.getMeasuringTimeMs() + EMPTY_RETRY_INTERVAL_MS
                                    : Long.MAX_VALUE;
                            DebugLog.debug("[playerlist] {} Spieler für Lehen-Sortierung geladen.",
                                    indexed.size());
                        } finally {
                            // Erst nach dem Cacheupdate darf die nächste Tab-Anfrage starten.
                            refreshInFlightGeneration.compareAndSet(currentGeneration, -1L);
                        }
                    }
                });
    }

    synchronized void deactivate() {
        generation.incrementAndGet();
        refreshInFlightGeneration.set(-1L);
        activationLogged.set(false);
        snapshot = Snapshot.inactive();
        joined = false;
        sortingWasRequested = false;
        nextRefreshAt = 0L;
    }

    Comparator<PlayerListEntry> wrap(Comparator<PlayerListEntry> vanilla) {
        Snapshot current = snapshot;
        if (!isSortingRequested() || !current.active || current.players.isEmpty()) {
            return vanilla;
        }
        if (activationLogged.compareAndSet(false, true)) {
            DebugLog.debug("[playerlist] Lehen-Sortierung aktiv; Server-listOrder wird ersetzt.");
        }
        return comparator(vanilla, PlayerListSortingService::isSpectator,
                entry -> sortKey(entry, current));
    }

    static <T> Comparator<T> comparator(Comparator<T> vanilla,
                                        java.util.function.Predicate<T> spectator,
                                        java.util.function.Function<T, PlayerListSortKey> key) {
        Map<T, PlayerListSortKey> keys = new IdentityHashMap<>();
        return (left, right) -> {
            int status = Boolean.compare(spectator.test(left), spectator.test(right));
            if (status != 0) return status;
            int result = PlayerListSortKey.compare(
                    keys.computeIfAbsent(left, key), keys.computeIfAbsent(right, key));
            return result != 0 ? result : vanilla.compare(left, right);
        };
    }

    private Map<UUID, PlayerData> index(List<CompactPlayer> players) {
        if (players == null || players.isEmpty()) {
            return Map.of();
        }
        Map<UUID, PlayerData> indexed = new HashMap<>();
        for (CompactPlayer player : players) {
            UUID uuid = parseUuid(player.uuid());
            if (uuid == null) {
                continue;
            }
            FactionRecord faction = faction(player);
            if (faction == null || !faction.isLanded()) {
                continue;
            }
            String lehen = firstNonBlank(faction.region_name(), faction.region_id());
            String normalizedLehen = normalize(lehen);
            if (normalizedLehen.isEmpty()) {
                continue;
            }
            String account = firstNonBlank(player.minecraftName(), player.name());
            int roleOrder = roleOrder(player.rank(), account, faction.leader_name());
            indexed.put(uuid, new PlayerData(normalizedLehen, roleOrder, player.title()));
        }
        return Map.copyOf(indexed);
    }

    private FactionRecord faction(CompactPlayer player) {
        String key = firstNonBlank(player.faction(), player.factionName());
        return key == null ? null : regions.factionByKey(key).orElse(null);
    }

    private PlayerListSortKey sortKey(PlayerListEntry entry, Snapshot current) {
        GameProfile profile = entry.getProfile();
        UUID uuid = profile != null ? profile.id() : null;
        PlayerData data = uuid != null ? current.players.get(uuid) : null;
        if (profile == null || uuid == null || data == null) {
            return unknownKey(uuid);
        }

        String title = TablistNameFormatter.visibleSortTitle(profile, data.apiTitle);
        TitleRegistry.ResolvedTitle resolved = titles == null
                ? null : titles.find(title).orElse(null);
        int groupPriority = resolved != null ? resolved.group().priority : Integer.MIN_VALUE;
        String group = resolved != null ? normalize(resolved.groupKey()) : "";
        int titleOrder = resolved != null ? resolved.titleIndex() : UNKNOWN_TITLE_ORDER;
        String visibleName = TablistNameFormatter.visibleSortName(profile);

        return new PlayerListSortKey(
                true,
                data.lehen,
                data.roleOrder,
                groupPriority,
                group,
                titleOrder,
                normalize(title),
                normalize(visibleName),
                normalize(profile.name()),
                uuid
        );
    }

    private static PlayerListSortKey unknownKey(UUID uuid) {
        return new PlayerListSortKey(false, "", Integer.MAX_VALUE, Integer.MIN_VALUE,
                "", UNKNOWN_TITLE_ORDER, "", "", "",
                uuid != null ? uuid : new UUID(0, 0));
    }

    private static boolean isSpectator(PlayerListEntry entry) {
        return entry.getGameMode() == GameMode.SPECTATOR;
    }

    private static int roleOrder(String rank, String account, String leaderName) {
        String normalizedRank = rank == null ? "" : rank.trim().toUpperCase(Locale.ROOT);
        if ("LEADER".equals(normalizedRank)
                || (account != null && leaderName != null
                && account.equalsIgnoreCase(leaderName.trim()))) {
            return 0;
        }
        if ("DEPUTY".equals(normalizedRank)) {
            return 1;
        }
        return 2;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return TitleRegistry.normalize(value == null ? "" : value);
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName() : message;
    }

    private boolean isSortingRequested() {
        try {
            return sortingRequested != null
                    ? sortingRequested.getAsBoolean()
                    : config.tablistSortByRegion;
        } catch (RuntimeException error) {
            return false;
        }
    }

    static boolean refreshDue(boolean joined, boolean requested,
                              boolean snapshotEmpty, boolean inFlight,
                              long now, long nextRefreshAt) {
        return joined && requested && snapshotEmpty && !inFlight
                && now >= nextRefreshAt;
    }

    private record PlayerData(String lehen, int roleOrder, String apiTitle) {
    }

    private record Snapshot(boolean active, Map<UUID, PlayerData> players) {
        private Snapshot {
            players = Map.copyOf(players);
        }

        static Snapshot inactive() {
            return new Snapshot(false, Map.of());
        }
    }
}
