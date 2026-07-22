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

final class PlayerListSortingService {
    private static final int UNKNOWN_TITLE_ORDER = Integer.MAX_VALUE;

    private final OttoExtraApiClient api;
    private final RegionDataService regions;
    private final TitleRegistry titles;
    private final OttoExtraConfig.RpNames config;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean activationLogged = new AtomicBoolean();

    private volatile Snapshot snapshot = Snapshot.inactive();

    PlayerListSortingService(OttoExtraApiClient api, RegionDataService regions,
                             TitleRegistry titles, OttoExtraConfig.RpNames config) {
        this.api = api;
        this.regions = regions;
        this.titles = titles;
        this.config = config;
    }

    void onServerJoin() {
        long currentGeneration = generation.incrementAndGet();
        activationLogged.set(false);
        snapshot = new Snapshot(true, Map.of());
        regions.bootstrapCompletion()
                .thenCompose(ignored -> generation.get() == currentGeneration
                        ? api.compactPlayers()
                        : CompletableFuture.completedFuture(List.of()))
                .whenComplete((players, error) -> {
                    if (generation.get() != currentGeneration) {
                        return;
                    }
                    if (error != null) {
                        DebugLog.debug("[playerlist] Sortierdaten nicht verfügbar: {}",
                                rootMessage(error));
                        return;
                    }
                    Map<UUID, PlayerData> indexed = index(players);
                    snapshot = new Snapshot(true, indexed);
                    DebugLog.debug("[playerlist] {} Spieler für Lehen-Sortierung geladen.",
                            indexed.size());
                });
    }

    void deactivate() {
        generation.incrementAndGet();
        activationLogged.set(false);
        snapshot = Snapshot.inactive();
    }

    Comparator<PlayerListEntry> wrap(Comparator<PlayerListEntry> vanilla) {
        Snapshot current = snapshot;
        if (!config.tablistSortByRegion || !current.active || current.players.isEmpty()) {
            return vanilla;
        }
        if (activationLogged.compareAndSet(false, true)) {
            DebugLog.debug("[playerlist] Lehen-Sortierung aktiv; Server-listOrder wird ersetzt.");
        }
        Map<PlayerListEntry, PlayerListSortKey> keys = new IdentityHashMap<>();
        return (left, right) -> {
            if (isSpectator(left) != isSpectator(right)) {
                return vanilla.compare(left, right);
            }
            PlayerListSortKey leftKey = keys.computeIfAbsent(left,
                    entry -> sortKey(entry, current));
            PlayerListSortKey rightKey = keys.computeIfAbsent(right,
                    entry -> sortKey(entry, current));
            int result = PlayerListSortKey.compare(leftKey, rightKey);
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
        TitleRegistry.ResolvedTitle resolved = titles.find(title).orElse(null);
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
