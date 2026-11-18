package de.ottoextra.tacticalfactions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TacticalConfig {
    static final int CURRENT_SCHEMA = 5;
    static final String LEGACY_ALERT_SQUAD_NAME = "Wache";
    static final String[] SQUAD_COLORS = {"#55C46A", "#4FA3C7", "#D9B44A", "#B07AC9",
            "#E06C5A", "#E8913A", "#8FA3B8", "#C9577F"};
    private static final int MAX_SQUAD_NAME = 32;
    static final String DEFAULT_ALLY_COLOR = "#5555FF";
    static final String DEFAULT_HOSTILE_COLOR = "#FF5555";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static TacticalConfig active;

    public int schemaVersion = CURRENT_SCHEMA;
    public boolean combatMode = false;
    public boolean showPlayerNames = true;
    public boolean politicalColors = true;
    public boolean tablistHostilesFirst = false;
    public String allyColor = DEFAULT_ALLY_COLOR;
    public String hostileColor = DEFAULT_HOSTILE_COLOR;
    public boolean preventFriendlyFire = BuildVariant.friendlyFireProtectionAvailable();
    public boolean protectNeutralPlayers = false;
    public boolean retaliationMarkers = true;
    public Map<String, Relation> factionRelations = new LinkedHashMap<>();
    public Map<String, Relation> factionNameRelations = new LinkedHashMap<>();
    public Map<String, Relation> playerRelations = new LinkedHashMap<>();
    public Map<String, Relation> accountRelations = new LinkedHashMap<>();
    public Map<String, CustomPoliticalEntity> customEntities = new LinkedHashMap<>();
    public Map<String, String> customPlayerMemberships = new LinkedHashMap<>();
    public Map<String, String> customAccountMemberships = new LinkedHashMap<>();
    public PoliticsView politicsView = PoliticsView.REALMS;
    public List<Squad> squads = new ArrayList<>();
    public boolean squadHudEnabled = false;
    public boolean squadHudMembers = true;
    public boolean squadHudOnlyOnline = false;
    public HudCorner squadHudCorner = HudCorner.TOP_RIGHT;
    public int squadHudMaxMembers = 0;
    public float squadHudScale = 1.0f;
    public int squadHudOffsetX = 0;
    public int squadHudOffsetY = 0;
    /**
     * Schema 4 and older stored a login-alert list here. It is only read to migrate it into a squad
     * during {@link #repair()} and is then set to null, so it is never written again.
     */
    public Map<String, String> playerLoginAlerts;

    public static synchronized TacticalConfig active() {
        if (active == null) active = load(defaultPath());
        return active;
    }

    static Path defaultPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("ottoextra-tacticalfactions.json");
    }

    public static Path bannerDirectory() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("ottoextra-tacticalfactions").resolve("banners");
    }

    public static TacticalConfig load(Path file) {
        TacticalConfig config = new TacticalConfig();
        if (file != null && Files.exists(file)) {
            try {
                TacticalConfig loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                        TacticalConfig.class);
                if (loaded != null) config = loaded;
            } catch (Exception ignored) {
                // Keep defaults; saving below repairs the file without failing startup.
            }
        }
        config.repair();
        config.save(file);
        return config;
    }

    public synchronized void save() {
        save(defaultPath());
    }

    synchronized void save(Path file) {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    void repair() {
        schemaVersion = CURRENT_SCHEMA;
        if (!BuildVariant.friendlyFireProtectionAvailable()) {
            preventFriendlyFire = false;
            protectNeutralPlayers = false;
        }
        allyColor = validColor(allyColor, DEFAULT_ALLY_COLOR);
        hostileColor = validColor(hostileColor, DEFAULT_HOSTILE_COLOR);
        factionRelations = repairMap(factionRelations, false);
        factionNameRelations = repairMap(factionNameRelations, true);
        playerRelations = repairMap(playerRelations, false);
        accountRelations = repairMap(accountRelations, true);
        repairCustomEntities();
        customPlayerMemberships = repairMemberships(customPlayerMemberships, false);
        customAccountMemberships = repairMemberships(customAccountMemberships, true);
        if (politicsView == null) politicsView = PoliticsView.REALMS;
        if (squads == null) squads = new ArrayList<>();
        if (squadHudCorner == null) squadHudCorner = HudCorner.TOP_RIGHT;
        squadHudScale = HudPlacement.clampScale(squadHudScale);
        squadHudMaxMembers = Math.max(0, Math.min(20, squadHudMaxMembers));
        squadHudOffsetX = HudPlacement.clampOffset(squadHudOffsetX);
        squadHudOffsetY = HudPlacement.clampOffset(squadHudOffsetY);
        migrateLoginAlerts();
        repairSquads();
    }

    public static boolean validAccountName(String accountName) {
        return cleanAccountName(accountName) != null;
    }

    public Squad findSquad(String id) {
        if (id == null) return null;
        for (Squad squad : squads) if (id.equals(squad.id)) return squad;
        return null;
    }

    /** Converts a pre-schema-5 login-alert list into one squad so no watched player is lost. */
    private void migrateLoginAlerts() {
        Map<String, String> legacy = playerLoginAlerts;
        playerLoginAlerts = null;
        if (legacy == null || legacy.isEmpty()) return;
        Squad wache = Squad.create(LEGACY_ALERT_SQUAD_NAME, SQUAD_COLORS[squads.size() % SQUAD_COLORS.length]);
        // The old feature only announced logins. The converted squad must not silently turn those
        // players into allies or reorder the tab list, so every squad effect starts disabled.
        wache.alwaysAllied = false;
        wache.tablistTop = false;
        wache.friendlyFireProtection = false;
        legacy.forEach((key, value) -> {
            String account = cleanAccountName(first(value, key));
            if (account != null) wache.members.add(new SquadMember(account, null, false));
        });
        if (!wache.members.isEmpty()) squads.add(wache);
    }

    private void repairSquads() {
        List<Squad> clean = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Squad squad : squads) {
            if (squad == null) continue;
            String id = cleanId(squad.id);
            if (id == null || ids.contains(id)) id = "squad:" + java.util.UUID.randomUUID();
            ids.add(id);
            squad.id = id;
            String name = squad.name == null ? "" : squad.name.trim();
            if (name.length() > MAX_SQUAD_NAME) name = name.substring(0, MAX_SQUAD_NAME).trim();
            squad.name = name.isEmpty() ? "Trupp " + (clean.size() + 1) : name;
            squad.color = validColor(squad.color, SQUAD_COLORS[clean.size() % SQUAD_COLORS.length]);
            squad.members = repairSquadMembers(squad.members);
            clean.add(squad);
        }
        squads = clean;
    }

    private static List<SquadMember> repairSquadMembers(List<SquadMember> source) {
        Map<String, SquadMember> clean = new LinkedHashMap<>();
        if (source != null) for (SquadMember member : source) {
            if (member == null) continue;
            String account = cleanAccountName(member.account);
            if (account == null) continue;
            member.account = account;
            member.uuid = cleanUuid(member.uuid);
            clean.putIfAbsent(account.toLowerCase(Locale.ROOT), member);
        }
        return new ArrayList<>(clean.values());
    }

    private static String cleanUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.util.UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static String cleanAccountName(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.matches("[A-Za-z0-9_]{3,16}") ? clean : null;
    }

    private void repairCustomEntities() {
        Map<String, CustomPoliticalEntity> clean = new LinkedHashMap<>();
        if (customEntities != null) customEntities.forEach((key, value) -> {
            if (value == null) return;
            String id = cleanCustomId(first(value.id, key));
            if (id == null) return;
            value.id = id;
            if (value.type == null || value.type == PoliticalEntityType.API) value.type = PoliticalEntityType.FIEF;
            value.name = value.name == null ? "" : value.name.trim();
            value.description = value.description == null ? "" : value.description.trim();
            value.parentId = cleanId(value.parentId);
            if (id.equals(value.parentId)) value.parentId = null;
            value.bannerFile = safeBannerFile(value.bannerFile);
            clean.put(id, value);
        });
        // Break custom-only cycles without discarding otherwise valid groups.
        for (CustomPoliticalEntity entity : clean.values()) {
            Set<String> seen = new HashSet<>();
            String current = entity.id;
            while (current != null && seen.add(current)) {
                CustomPoliticalEntity node = clean.get(current);
                current = node != null ? node.parentId : null;
            }
            if (current != null) entity.parentId = null;
        }
        customEntities = clean;
    }

    private Map<String, String> repairMemberships(Map<String, String> source, boolean lower) {
        Map<String, String> clean = new LinkedHashMap<>();
        if (source != null) source.forEach((key, entityId) -> {
            if (key == null || key.isBlank() || !customEntities.containsKey(entityId)) return;
            String cleanKey = lower ? key.trim().toLowerCase(Locale.ROOT) : key.trim();
            clean.put(cleanKey, entityId);
        });
        return clean;
    }

    private static String cleanCustomId(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        return clean.startsWith("custom:") && clean.length() > 7 ? clean : null;
    }

    private static String cleanId(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeBannerFile(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String file = Path.of(value).getFileName().toString();
            return file.toLowerCase(Locale.ROOT).endsWith(".png") ? file : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String first(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static Map<String, Relation> repairMap(Map<String, Relation> source, boolean lower) {
        Map<String, Relation> clean = new LinkedHashMap<>();
        if (source != null) source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                clean.put(lower ? key.trim().toLowerCase(Locale.ROOT) : key.trim(), value);
            }
        });
        return clean;
    }

    static String validColor(String value, String fallback) {
        if (value == null) return fallback;
        String clean = value.trim();
        if (!clean.matches("#[0-9a-fA-F]{6}")) return fallback;
        return clean.toUpperCase(Locale.ROOT);
    }
}
