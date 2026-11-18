package de.ottoextra.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ResourcesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean hintsEnabled = true;
    /** Zone notice: "compact" (Leiste, default) or "detailed" (Siegel). */
    public String noticeStyle = ResourceNotice.STYLE_COMPACT;
    /** Notice when leaving a zone whose timer was started during the visit. */
    public boolean leaveNotice = true;
    public boolean showResourceIcons = true;
    public boolean showPoiIcons = true;
    public boolean showMinimapStatusIndicator = false;
    /** Start the timer on the first block broken inside an entry, without the confirm key. */
    public boolean autoStartOnBreak = false;
    public boolean hudEnabled = true;
    /** Show entries of every Lehen instead of only the one the player is in. */
    public boolean hudShowAll = false;
    /** "full" (title, rows with progress line, footer) or "compact" (ready count and the next three). */
    public String hudStyle = ResourceHud.STYLE_FULL;
    public int hudMaxRows = 8;
    /** Corner the panel is anchored to ({@link ResourceHudPlacement.Corner} name); set in "HUD anordnen". */
    public String hudCorner = ResourceHudPlacement.Corner.TOP_RIGHT.name();
    /** Pixel offset of the panel from its corner. */
    public int hudOffsetX = 0;
    public int hudOffsetY = ResourceHudPlacement.DEFAULT_OFFSET_Y;
    /** Panel scale, 0.5 to 2. */
    public float hudScale = 1.0f;
    /** Show zone outlines in the world with particles while editing, proposing or on request. */
    public boolean showOutlineParticles = true;
    public boolean readySound = true;
    /** Sharing through the list service; off by default, nothing is sent while off. */
    public boolean sharingEnabled = false;
    public String shareBaseUrl = ResourceShareApi.DEFAULT_BASE_URL;
    /** Send the Minecraft name with started timers. The name is unverified and visible to all. */
    public boolean shareStartedBy = false;
    /** Local id of the list new entries are added to. */
    public String activeListId = "";

    private static ResourcesConfig active;

    public static synchronized ResourcesConfig active() {
        if (active == null) active = load();
        return active;
    }

    private static ResourcesConfig load() {
        Path file = configFile();
        ResourcesConfig config = null;
        if (Files.isRegularFile(file)) {
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(
                        Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                config = GSON.fromJson(json, ResourcesConfig.class);
                migrateHudTop(json, config);
            } catch (Exception error) {
                OttoExtraResources.LOGGER.warn("Config nicht lesbar, verwende Defaults: {}", file,
                        error);
            }
        }
        if (config == null) config = new ResourcesConfig();
        config.sanitize();
        config.save();
        return config;
    }

    public synchronized void save() {
        sanitize();
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(this), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception error) {
            OttoExtraResources.LOGGER.error("Config konnte nicht gespeichert werden: {}", file,
                    error);
        }
    }

    /**
     * Older versions only had {@code hudTop}, the distance of the top-right panel from the screen
     * top. It becomes the vertical offset from the top-right corner once.
     */
    static void migrateHudTop(com.google.gson.JsonObject json, ResourcesConfig config) {
        if (config == null || !json.has("hudTop") || json.has("hudCorner")) return;
        try {
            config.hudCorner = ResourceHudPlacement.Corner.TOP_RIGHT.name();
            config.hudOffsetX = 0;
            config.hudOffsetY = Math.clamp(json.get("hudTop").getAsInt(), 0, 400) - ResourceHudPlacement.MARGIN;
        } catch (RuntimeException ignored) {
            // Unreadable old value: keep the default position.
        }
    }

    void sanitize() {
        hudMaxRows = Math.clamp(hudMaxRows, 1, 30);
        hudCorner = ResourceHudPlacement.Corner.parse(hudCorner).name();
        hudOffsetX = ResourceHudPlacement.clampOffset(hudOffsetX);
        hudOffsetY = ResourceHudPlacement.clampOffset(hudOffsetY);
        hudScale = ResourceHudPlacement.clampScale(hudScale);
        if (shareBaseUrl == null || ResourceShareApi.baseUri(shareBaseUrl) == null) {
            shareBaseUrl = ResourceShareApi.DEFAULT_BASE_URL;
        }
        if (activeListId == null) activeListId = "";
        if (!ResourceHud.STYLE_COMPACT.equals(hudStyle)) hudStyle = ResourceHud.STYLE_FULL;
        if (!ResourceNotice.STYLE_DETAILED.equals(noticeStyle)) noticeStyle = ResourceNotice.STYLE_COMPACT;
    }

    boolean showsOnMap(ResourceSnapshot resource) {
        return resource.markerOnly() ? showPoiIcons : showResourceIcons;
    }

    synchronized void setResourceIconsVisible(boolean visible) {
        showResourceIcons = visible;
        save();
    }

    synchronized void setPoiIconsVisible(boolean visible) {
        showPoiIcons = visible;
        save();
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("ottoextra-resources.json");
    }
}
