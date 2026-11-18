package de.ottoextra.tacticalfactions;

import de.ottoextra.OttoExtraClient;
import de.ottoextra.api.model.CompactPlayer;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import de.ottoextra.map.PoliticalOverlay;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PoliticalStateService {
    private static final long STALE_AFTER_MS = 15 * 60_000L;
    private static final long REFRESH_INTERVAL_MS = 5 * 60_000L;
    private static final long RETRY_INTERVAL_MS = 30_000L;
    private static final long BOOTSTRAP_WAIT_SECONDS = 10L;
    private final TacticalConfig config;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Snapshot snapshot = Snapshot.empty();
    private volatile long revision = 1L;
    private volatile String lastError = "";
    private volatile long lastAttemptAt;
    private volatile long lastSuccessAt;

    public PoliticalStateService(TacticalConfig config) {
        this.config = config;
    }

    /**
     * Starts a refresh when the last one is old enough: every {@link #REFRESH_INTERVAL_MS} after a
     * success, every {@link #RETRY_INTERVAL_MS} while no usable data arrived.
     */
    public void refreshIfDue(long now) {
        long interval = lastSuccessAt > 0 && lastError.isEmpty() ? REFRESH_INTERVAL_MS : RETRY_INTERVAL_MS;
        if (now - lastAttemptAt < interval) return;
        refresh();
    }

    /** Clears the retry throttle so the next {@link #refreshIfDue(long)} runs right away. */
    public void resetRefreshTimer() {
        lastAttemptAt = 0L;
    }

    public void refresh() {
        if (!refreshing.compareAndSet(false, true)) return;
        lastAttemptAt = System.currentTimeMillis();
        try {
            RegionDataService regions = RegionsServices.data();
            var api = OttoExtraClient.apiClient();
            if (regions == null || api == null) {
                lastError = "services";
                finishRefresh();
                return;
            }
            // The region bootstrap future stays open while the API keeps failing. Do not wait for it
            // forever: after a short grace period continue with the cached faction data.
            regions.bootstrapCompletion().copy()
                    .completeOnTimeout(null, BOOTSTRAP_WAIT_SECONDS, TimeUnit.SECONDS)
                    .whenComplete((ignored, bootstrapError) -> loadPlayers(regions, api));
        } catch (RuntimeException error) {
            lastError = rootMessage(error);
            finishRefresh();
        }
    }

    private void loadPlayers(RegionDataService regions, de.ottoextra.api.OttoExtraApiClient api) {
        try {
            List<FactionRecord> factionRecords = regions.allFactions();
            api.compactPlayers().whenComplete((players, error) -> {
                try {
                    if (error == null && players != null) {
                        snapshot = build(regions, factionRecords, players);
                        lastError = "";
                        lastSuccessAt = System.currentTimeMillis();
                    } else {
                        lastError = error != null ? rootMessage(error) : "players";
                        snapshot = withFactions(snapshot, build(regions, factionRecords, List.of()));
                    }
                } catch (RuntimeException buildError) {
                    lastError = rootMessage(buildError);
                } finally {
                    finishRefresh();
                }
            });
        } catch (RuntimeException error) {
            lastError = rootMessage(error);
            finishRefresh();
        }
    }

    private void finishRefresh() {
        revision++;
        refreshing.set(false);
    }

    /**
     * Player list failed: still show the current factions (so set relations stay visible), but keep
     * the previous player data and its age so protection logic does not treat it as fresh.
     */
    static Snapshot withFactions(Snapshot previous, Snapshot factionsOnly) {
        if (factionsOnly.factions.isEmpty()) return previous;
        return new Snapshot(factionsOnly.factions, factionsOnly.raw, previous.byUuid, previous.byName,
                previous.players, previous.ownFactionId, previous.updatedAt, factionsOnly.landed);
    }

    Snapshot build(RegionDataService regions, List<FactionRecord> records, List<CompactPlayer> players) {
        Set<String> landed = PoliticalHierarchy.landedIds(regions.allRegions());
        Map<String, String> parent = PoliticalHierarchy.parents(records, landed);

        Map<String, FactionNode> factions = new LinkedHashMap<>();
        Map<String, FactionRecord> raw = new LinkedHashMap<>();
        for (FactionRecord faction : records) {
            if (faction.uuid() == null || faction.uuid().isBlank()) continue;
            String name = first(faction.region_name(), faction.name(), faction.uuid());
            factions.put(faction.uuid(), new FactionNode(faction.uuid(), name, parent.get(faction.uuid())));
            raw.put(faction.uuid(), faction);
        }

        Map<String, PlayerAffiliation> byUuid = new HashMap<>();
        Map<String, PlayerAffiliation> byName = new HashMap<>();
        List<PlayerChoice> choices = new ArrayList<>();
        String ownAccount = currentAccount();
        String ownFaction = null;
        for (CompactPlayer player : players) {
            String account = first(player.minecraftName(), player.name());
            FactionRecord faction = regions.factionByKey(first(player.faction(), player.factionName())).orElse(null);
            String factionId = faction != null ? faction.uuid() : null;
            PlayerAffiliation affiliation = new PlayerAffiliation(player.uuid(), account, factionId);
            if (player.uuid() != null) byUuid.put(player.uuid(), affiliation);
            if (account != null) byName.put(normalize(account), affiliation);
            if (account != null) choices.add(new PlayerChoice(player.uuid(), account,
                    first(player.rpName(), account), factionId));
            if (ownAccount != null && account != null && ownAccount.equalsIgnoreCase(account)) {
                ownFaction = factionId;
            }
        }
        choices.sort(Comparator.comparing(PlayerChoice::displayName, String.CASE_INSENSITIVE_ORDER));
        return new Snapshot(factions, raw, byUuid, byName, List.copyOf(choices), ownFaction,
                System.currentTimeMillis(), landed);
    }

    static void addNameBasedParents(List<FactionRecord> records, Map<String, String> parent) {
        PoliticalHierarchy.addNameBasedParents(records, parent, null);
    }

    public Resolution resolve(UUID uuid, String account) {
        return resolve(uuid, account, false);
    }

    /**
     * Read-only UI may keep showing the last successfully resolved relation
     * while a later API refresh is unavailable. Protection code deliberately
     * continues to use {@link #resolve(UUID, String)} and therefore fails safe
     * to neutral once the snapshot is stale.
     */
    public Resolution resolveForDisplay(UUID uuid, String account) {
        return resolve(uuid, account, true);
    }

    private Resolution resolve(UUID uuid, String account, boolean allowStaleSnapshot) {
        // Local squads outrank faction data and manual player relations; the first squad wins.
        Squad squad = squadFor(uuid, account);
        if (squad != null && squad.alwaysAllied) return new Resolution(Relation.ALLIED, "squad:" + squad.name);

        Relation manual = manualRelation(uuid, account);
        if (manual != Relation.INHERIT) return new Resolution(manual, "player");

        Snapshot current = snapshot;
        PlayerAffiliation apiPlayer = uuid != null ? current.byUuid.get(uuid.toString()) : null;
        if (apiPlayer == null && account != null) apiPlayer = current.byName.get(normalize(account));
        String custom = customMembership(uuid != null ? uuid.toString() : null, account);
        if (custom != null && config.customEntities.containsKey(custom)) {
            PlayerAffiliation player = new PlayerAffiliation(uuid != null ? uuid.toString() : null, account,
                    apiPlayer != null ? apiPlayer.apiFactionId() : null, custom);
            return resolverFor(current).resolve(player);
        }
        if (!allowStaleSnapshot
                && System.currentTimeMillis() - current.updatedAt > STALE_AFTER_MS) {
            return new Resolution(Relation.NEUTRAL, "stale");
        }
        if (apiPlayer == null) return new Resolution(Relation.NEUTRAL, "unknown");
        return resolverFor(current).resolve(apiPlayer);
    }

    /** The first squad (list order) containing the player, or null. */
    public Squad squadFor(UUID uuid, String account) {
        return Squad.firstContaining(config.squads, uuid != null ? uuid.toString() : null, account);
    }

    /** The squad colour when the resolution came from a squad, else null (use the ally colour). */
    public String squadColor(Resolution resolution, UUID uuid, String account) {
        if (resolution == null || !resolution.source().startsWith("squad:")) return null;
        Squad squad = squadFor(uuid, account);
        return squad != null ? squad.color : null;
    }

    /** Index of the first squad containing the player when it groups its members first, else -1. */
    public int tablistSquadIndex(UUID uuid, String account) {
        int index = Squad.indexOfFirst(config.squads, uuid != null ? uuid.toString() : null, account);
        return index >= 0 && config.squads.get(index).tablistTop ? index : -1;
    }

    public Resolution resolveFaction(String factionId) {
        Map<String, FactionNode> factions = mergedFactions(snapshot);
        if (!factions.containsKey(factionId)) return new Resolution(Relation.NEUTRAL, "unknown");
        return resolverFor(snapshot).resolve(new PlayerAffiliation(null, null,
                factionId.startsWith("custom:") ? null : factionId,
                factionId.startsWith("custom:") ? factionId : null));
    }

    private PoliticsResolver resolverFor(Snapshot state) {
        Map<String, FactionNode> factions = mergedFactions(state);
        Map<String, Relation> relations = new HashMap<>(config.factionRelations);
        // Legacy name fallbacks belong to API records only. Custom groups always use their stable ID.
        // Lookup by the API name first (that is what setFaction stores), then by the node name,
        // which prefers the region name.
        state.factions.forEach((id, faction) -> {
            FactionRecord record = state.raw.get(id);
            Relation fallback = record != null && record.name() != null
                    ? config.factionNameRelations.get(normalize(record.name())) : null;
            if (fallback == null) fallback = config.factionNameRelations.get(normalize(faction.name()));
            if (fallback != null) relations.putIfAbsent(id, fallback);
        });
        return new PoliticsResolver(factions, relations, Map.of(), activeOwnFaction(state));
    }

    private Map<String, FactionNode> mergedFactions(Snapshot state) {
        Map<String, FactionNode> merged = new LinkedHashMap<>(state.factions);
        config.customEntities.values().forEach(entity -> merged.put(entity.id,
                new FactionNode(entity.id, entity.name.isBlank() ? entity.id : entity.name, entity.parentId)));
        return merged;
    }

    private String activeOwnFaction(Snapshot state) {
        MinecraftClient client = MinecraftClient.getInstance();
        String uuid = client != null && client.getSession() != null
                ? client.getSession().getUuidOrNull() != null ? client.getSession().getUuidOrNull().toString() : null : null;
        String account = currentAccount();
        String custom = customMembership(uuid, account);
        return custom != null ? custom : state.ownFactionId;
    }

    public List<FactionView> factions() {
        Snapshot current = snapshot;
        Map<String, FactionNode> merged = mergedFactions(current);
        Map<String, Integer> memberCounts = memberCounts();
        List<FactionView> result = new ArrayList<>();
        for (FactionNode faction : merged.values()) {
            CustomPoliticalEntity custom = config.customEntities.get(faction.id());
            FactionRecord apiRecord = current.raw.get(faction.id());
            String displayName = custom != null ? custom.name
                    : first(apiRecord != null ? apiRecord.name() : null, faction.name());
            String rootId = rootId(faction.id(), merged);
            FactionNode rootNode = merged.get(rootId);
            FactionRecord rootRecord = current.raw.get(rootId);
            CustomPoliticalEntity rootCustom = config.customEntities.get(rootId);
            String rootName = rootCustom != null ? rootCustom.name : first(
                    rootRecord != null ? rootRecord.name() : null,
                    rootNode != null ? rootNode.name() : null, displayName);
            result.add(new FactionView(faction.id(), displayName, hierarchy(faction.id(), merged),
                    rootName, rootId,
                    resolveFaction(faction.id()), custom != null,
                    custom != null ? custom.type : PoliticalEntityType.API,
                    custom != null ? custom.description : "", memberCounts.getOrDefault(faction.id(), 0),
                    apiRecord, custom != null ? custom.bannerFile : null,
                    rootRecord, rootCustom != null ? rootCustom.bannerFile : null,
                    mapColor(rootRecord, apiRecord, rootName),
                    custom != null || PoliticalHierarchy.isLanded(apiRecord, current.landed),
                    apiRecord != null ? apiRecord.rank_name() : null,
                    hierarchySortPath(faction.id(), merged), hierarchyDepth(faction.id(), merged)));
        }
        result.sort(Comparator.comparing(FactionView::landed).reversed()
                .thenComparing(FactionView::topLiege, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FactionView::sortPath, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<FactionView> availableParents(String childId) {
        Set<String> descendants = descendantsOf(childId);
        return factions().stream().filter(view -> !view.id.equals(childId) && !descendants.contains(view.id)).toList();
    }

    private Set<String> descendantsOf(String id) {
        Map<String, FactionNode> merged = mergedFactions(snapshot);
        Set<String> result = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (FactionNode node : merged.values()) {
                if (id.equals(node.parentId()) || result.contains(node.parentId())) changed |= result.add(node.id());
            }
        } while (changed);
        return result;
    }

    public List<PlayerChoice> players() {
        return snapshot.players;
    }

    public String tablistDisplayName(UUID uuid, String account) {
        String uuidValue = uuid != null ? uuid.toString() : null;
        String normalizedAccount = normalize(account);
        for (PlayerChoice player : snapshot.players) {
            if (uuidValue != null && uuidValue.equals(player.uuid)
                    || !normalizedAccount.isEmpty()
                    && normalizedAccount.equals(normalize(player.account))) {
                return first(player.displayName, account);
            }
        }
        return account;
    }

    public List<MemberRef> members(String entityId) {
        Map<String, MemberRef> result = new LinkedHashMap<>();
        for (PlayerChoice player : snapshot.players) {
            if (entityId.equals(customMembership(player.uuid, player.account))) {
                result.put(normalize(player.account), new MemberRef(player.uuid, player.account, player.displayName));
            }
        }
        config.customAccountMemberships.forEach((account, assigned) -> {
            if (entityId.equals(assigned)) result.putIfAbsent(normalize(account), new MemberRef(null, account, account));
        });
        config.customPlayerMemberships.forEach((uuid, assigned) -> {
            if (entityId.equals(assigned) && result.values().stream().noneMatch(member -> uuid.equals(member.uuid))) {
                result.put("uuid:" + uuid, new MemberRef(uuid, null, uuid));
            }
        });
        return new ArrayList<>(result.values());
    }

    public synchronized void upsertCustom(CustomPoliticalEntity entity, Relation relation, List<MemberRef> members) {
        if (entity == null || entity.id == null || !entity.id.startsWith("custom:")
                || entity.name == null || entity.name.isBlank()) return;
        CustomPoliticalEntity copy = entity.copy();
        copy.name = copy.name.trim();
        copy.description = copy.description == null ? "" : copy.description.trim();
        if (copy.parentId != null && (copy.parentId.equals(copy.id) || descendantsOf(copy.id).contains(copy.parentId))) {
            copy.parentId = null;
        }
        config.customEntities.put(copy.id, copy);
        config.factionRelations.put(copy.id, relation == null ? Relation.INHERIT : relation);
        replaceMembers(copy.id, members);
        config.repair();
        config.save();
        revision++;
    }

    public synchronized void deleteCustom(String id) {
        CustomPoliticalEntity removed = config.customEntities.remove(id);
        if (removed == null) return;
        for (CustomPoliticalEntity child : config.customEntities.values()) {
            if (id.equals(child.parentId)) child.parentId = removed.parentId;
        }
        config.factionRelations.remove(id);
        config.customPlayerMemberships.entrySet().removeIf(entry -> id.equals(entry.getValue()));
        config.customAccountMemberships.entrySet().removeIf(entry -> id.equals(entry.getValue()));
        LocalBannerTextureService.instance().delete(id, removed.bannerFile);
        config.save();
        revision++;
    }

    public synchronized void setFaction(String factionId, String factionName, Relation relation) {
        boolean custom = factionId != null && factionId.startsWith("custom:");
        if (factionId != null && !factionId.isBlank()) config.factionRelations.put(factionId, relation);
        // API factions also keep a name fallback, so the relation survives a changed faction UUID.
        if (!custom && factionName != null && !factionName.isBlank()) {
            config.factionNameRelations.put(normalize(factionName), relation);
        }
        config.save();
        revision++;
    }

    public synchronized void setPlayer(UUID uuid, String account, Relation relation) {
        if (uuid != null) config.playerRelations.put(uuid.toString(), relation);
        if (account != null) config.accountRelations.put(normalize(account), relation);
        config.save();
        revision++;
    }

    private void replaceMembers(String entityId, List<MemberRef> members) {
        config.customPlayerMemberships.entrySet().removeIf(entry -> entityId.equals(entry.getValue()));
        config.customAccountMemberships.entrySet().removeIf(entry -> entityId.equals(entry.getValue()));
        if (members == null) return;
        for (MemberRef member : members) {
            if (member.uuid != null && !member.uuid.isBlank()) {
                config.customPlayerMemberships.put(member.uuid.trim(), entityId);
            }
            if (member.account != null && !member.account.isBlank()) {
                config.customAccountMemberships.put(normalize(member.account), entityId);
            }
        }
    }

    private Relation manualRelation(UUID uuid, String account) {
        Relation relation = uuid != null ? config.playerRelations.get(uuid.toString()) : null;
        if ((relation == null || relation == Relation.INHERIT) && account != null) {
            relation = config.accountRelations.get(normalize(account));
        }
        return relation == null ? Relation.INHERIT : relation;
    }

    private String customMembership(String uuid, String account) {
        String entity = uuid != null ? config.customPlayerMemberships.get(uuid) : null;
        if (entity == null && account != null) entity = config.customAccountMemberships.get(normalize(account));
        return entity;
    }

    private Map<String, Integer> memberCounts() {
        Map<String, Integer> result = new HashMap<>();
        config.customEntities.keySet().forEach(id -> result.put(id, members(id).size()));
        return result;
    }

    public boolean isRefreshing() { return refreshing.get(); }
    public long revision() { return revision; }
    public String lastError() { return lastError; }

    private static String hierarchy(String id, Map<String, FactionNode> factions) {
        List<String> names = new ArrayList<>();
        String current = id;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            FactionNode node = factions.get(current);
            if (node == null) break;
            names.add(node.name());
            current = node.parentId();
        }
        return String.join(" ← ", names);
    }

    private static String rootId(String id, Map<String, FactionNode> factions) {
        String current = id;
        FactionNode top = null;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            FactionNode node = factions.get(current);
            if (node == null) break;
            top = node;
            current = node.parentId();
        }
        return top != null ? top.id() : id;
    }

    private static String hierarchySortPath(String id, Map<String, FactionNode> factions) {
        List<String> names = new ArrayList<>();
        String current = id;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            FactionNode node = factions.get(current);
            if (node == null) break;
            names.add(node.name().toLowerCase(Locale.ROOT));
            current = node.parentId();
        }
        java.util.Collections.reverse(names);
        return String.join("/", names);
    }

    private static int hierarchyDepth(String id, Map<String, FactionNode> factions) {
        int depth = 0;
        String current = id;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            FactionNode node = factions.get(current);
            if (node == null || node.parentId() == null) break;
            depth++;
            current = node.parentId();
        }
        return depth;
    }

    private static int mapColor(FactionRecord root, FactionRecord direct, String rootName) {
        try {
            FactionRecord source = root != null ? root : direct;
            if (source != null && source.region_id() != null && !source.region_id().isBlank()) {
                return 0xFF000000 | (PoliticalOverlay.fillTintFor(source.region_id()) & 0xFFFFFF);
            }
            Integer overview = PoliticalOverlay.groupTintOverview().get(rootName);
            if (overview != null) return 0xFF000000 | (overview & 0xFFFFFF);
        } catch (Throwable ignored) {
            // Politics remains usable when the optional map data is unavailable.
        }
        return 0xFF777777;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private static String currentAccount() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return null;
            if (client.player != null) return client.player.getName().getString();
            return client.getSession() != null ? client.getSession().getUsername() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    public record FactionView(String id, String name, String hierarchy, String topLiege, String rootId,
                              Resolution resolution,
                              boolean custom, PoliticalEntityType type, String description, int memberCount,
                              FactionRecord apiRecord, String bannerFile,
                              FactionRecord rootApiRecord, String rootBannerFile,
                              int mapColor, boolean landed, String rankName,
                              String sortPath, int hierarchyDepth) { }

    public record PlayerChoice(String uuid, String account, String displayName, String apiFactionId) { }
    public record MemberRef(String uuid, String account, String displayName) { }

    record Snapshot(Map<String, FactionNode> factions, Map<String, FactionRecord> raw,
                    Map<String, PlayerAffiliation> byUuid, Map<String, PlayerAffiliation> byName,
                    List<PlayerChoice> players, String ownFactionId, long updatedAt, Set<String> landed) {
        Snapshot(Map<String, FactionNode> factions, Map<String, FactionRecord> raw,
                 Map<String, PlayerAffiliation> byUuid, Map<String, PlayerAffiliation> byName,
                 List<PlayerChoice> players, String ownFactionId, long updatedAt) {
            this(factions, raw, byUuid, byName, players, ownFactionId, updatedAt, null);
        }

        static Snapshot empty() { return new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), List.of(), null, 0); }
    }
}
