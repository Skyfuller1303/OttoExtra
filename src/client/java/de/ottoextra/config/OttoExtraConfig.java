package de.ottoextra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.nametags.NameTagMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class OttoExtraConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public Api api = new Api();
    public Map map = new Map();
    public Regions regions = new Regions();
    public RpNames rpnames = new RpNames();
    public Nametags nametags = new Nametags();
    public Letter letter = new Letter();
    public Chat chat = new Chat();
    public ResourcePack resourcepack = new ResourcePack();
    public Tweaks tweaks = new Tweaks();

    public static final class Tweaks {
        public LowHealth lowHealth = new LowHealth();
        public ToolProtect toolProtect = new ToolProtect();

        public static final class ToolProtect {

            public boolean enabled = true;

            public int blockAtUses = 10;

            public int warnBelowPercent = 10;

            public java.util.List<String> uiBlocks = defaultUiBlocks();

            private static java.util.List<String> defaultUiBlocks() {
                return new java.util.ArrayList<>(java.util.List.of(
                        "minecraft:crafting_table",
                        "minecraft:cartography_table",
                        "minecraft:smithing_table",
                        "minecraft:fletching_table",
                        "minecraft:anvil",
                        "minecraft:chipped_anvil",
                        "minecraft:damaged_anvil",
                        "minecraft:chest",
                        "minecraft:trapped_chest",
                        "minecraft:ender_chest",
                        "minecraft:barrel",
                        "minecraft:furnace",
                        "minecraft:blast_furnace",
                        "minecraft:smoker",
                        "minecraft:loom",
                        "minecraft:grindstone",
                        "minecraft:stonecutter",
                        "minecraft:enchanting_table",
                        "minecraft:brewing_stand",
                        "minecraft:beacon",
                        "minecraft:lectern",
                        "minecraft:crafter",
                        "minecraft:hopper",
                        "minecraft:dispenser",
                        "minecraft:dropper"));
            }
        }

        public static final class LowHealth {

            public boolean enabled = false;
            public boolean vignetteEnabled = true;
            public boolean heartbeatEnabled = true;

            public boolean blurEnabled = true;

            public float blurStartHearts = 7.0f;

            public float blurStrength = 1.0f;

            public boolean fovEnabled = true;

            public float fovMaxDegrees = 8.0f;

            public float startHealth = 20.0f;

            public float intensityScale = 1.0f;
            public float vignetteMinAlpha = 0.06f;
            public float vignetteMaxAlpha = 0.62f;
            public boolean reduceWhenScreenOpen = true;
            public float screenOpenMultiplier = 0.35f;
            public boolean calmAfterNoDamage = true;
            public int calmAfterNoDamageSeconds = 8;
            public float calmMinHealth = 12.0f;
            public float calmVisualMultiplier = 0.35f;
            public float heartbeatMinIntensity = 0.12f;
            public int heartbeatMaxIntervalMs = 1500;
            public int heartbeatMinIntervalMs = 450;
            public float heartbeatMinVolume = 0.18f;
            public float heartbeatMaxVolume = 0.75f;
            public float heartbeatMinPitch = 0.85f;
            public float heartbeatMaxPitch = 1.15f;
            public float fadeSpeed = 0.12f;
        }
    }

    public static final class Api {
        public boolean enabled = true;
        public String baseUrl = "https://regions.skyfuller.de/";
        public int connectTimeoutMs = 10_000;
        public int requestTimeoutMs = 20_000;
        public int syncIntervalSeconds = 1_800;
        public int playerDirectoryIntervalSeconds = 300;

        public boolean useV2Auth = false;

        public boolean requireSignatures = false;

        public boolean tlsPinning = true;
    }

    public static final class Map {
        public boolean enabled = true;
        public boolean showBorders = true;
        public boolean showNames = true;
        public boolean showBanners = true;

        public boolean paintedMap = true;

        public boolean paintedMapSimple = false;

        public boolean politicalFill = true;

        public boolean paintedFullCoverZoomOut = true;

        public int paintedMapOffsetX = 0;
        public int paintedMapOffsetZ = 0;

        public boolean showCalibrationArrows = false;

        public boolean parchmentMode = true;
        public int parchmentMarginPx = 5;

        public boolean clickInfoPanel = true;

        public double walkBlocksPerSecond = 4.3;
        public double horseBlocksPerSecond = 8.5;

        public boolean showActivity = true;

        public boolean showNpcVillages = true;

        public java.util.List<NpcVillage> npcVillages = defaultNpcVillages();

        public boolean onlyOnOttonien = true;

        public boolean minimapBorders = true;

        public boolean minimapPolitical = false;

        public boolean minimapPainted = true;

        public boolean minimapBanner = true;
        public int minimapBannerSize = 16;

        public int minimapBannerOffsetX = 18;
        public int minimapBannerOffsetY = -15;

        public boolean minimapBannerRound = true;
        public int minimapBannerOffsetXRound = 10;
        public int minimapBannerOffsetYRound = -30;

        public boolean minimapLiegeTop = true;

        public String toggleKey = "key.keyboard.k";

        public double nameMinScale = 0.05;

        public double bannerMinScale = 0.05;

        public float labelScale = 1.0f;

        public double politicalMaxScale = 0.6;

        public int politicalOpacity = 90;

        public int politicalOpacityNight = 60;

        public double labelZoomA = 0.06;
        public double labelZoomB = 0.45;
        public float factionScaleA = 0.7f;
        public float factionScaleB = 1.3f;
        public float lehenScaleA = 0.5f;
        public float lehenScaleB = 0.95f;
        public int bannerSizeA = 12;
        public int bannerSizeB = 28;
        public int bannerSizePx = 20;
        public int borderWidthPx = 1;

        public String borderColor = "#493C30";

        public boolean dashedBorders = true;
        public int dashLengthPx = 6;
        public int dashGapPx = 4;

        public java.util.LinkedHashMap<String, String> groupColors = defaultGroupColors();

        public java.util.LinkedHashMap<String, String> groupNameOverrides = new java.util.LinkedHashMap<>();

        public java.util.LinkedHashMap<String, String> lehenColors = new java.util.LinkedHashMap<>();

        public java.util.LinkedHashMap<String, String> factionColors = new java.util.LinkedHashMap<>();

        private static java.util.LinkedHashMap<String, String> defaultGroupColors() {
            java.util.LinkedHashMap<String, String> m = new java.util.LinkedHashMap<>();
            m.put("Holdstewik", "#D5594F");
            m.put("Kalkbruch", "#5D6485");
            m.put("Arctander", "#3493C4");
            m.put("Oberquell", "#91A788");
            m.put("Nordeck", "#B38078");
            m.put("Löwenfels", "#A78C90");
            m.put("Hohenau", "#939180");
            m.put("warglau", "#393939");
            m.put("Westmark", "#C1B355");
            m.put("Glademünde", "#A38696");
            m.put("Hilligsaiwiz", "#B5A26A");
            m.put("Taunestein", "#D9DEE1");
            m.put("Ostowitz", "#4E5873");
            m.put("Rabenstad", "#90A176");
            m.put("Bühlstett", "#848987");
            m.put("Kreuztal", "#224A2F");
            return m;
        }

        public boolean minimapBannerShowName = false;
        public boolean minimapBannerShowState = true;

        public boolean minimapBannerShowFaction = true;

        public int bannerHudX = 78;
        public int bannerHudY = 77;
        public boolean bannerHudFromRight = true;
        public boolean bannerHudFromBottom = false;
        public int nameHudX = 48;
        public int nameHudY = 134;
        public boolean nameHudFromRight = true;
        public boolean nameHudFromBottom = false;
        public int stateHudX = 3;
        public int stateHudY = 121;
        public boolean stateHudFromRight = true;
        public boolean stateHudFromBottom = false;
        public int factionHudX = 5;
        public int factionHudY = 110;
        public boolean factionHudFromRight = true;
        public boolean factionHudFromBottom = false;

        public float nameHudScale = 0.52f;
        public float stateHudScale = 0.52f;
        public float factionHudScale = 0.97f;

        public int nameHudWidth = 46;
        public int stateHudWidth = 91;
        public int factionHudWidth = 89;

        public String nameHudColor = "#E6C8A9";
        public String stateHudColor = "#B8A88F";
        public String factionHudColor = "#FFFFFF";

        private static java.util.List<NpcVillage> defaultNpcVillages() {
            java.util.List<NpcVillage> v = new java.util.ArrayList<>();
            v.add(new NpcVillage("Pfardorf", -630, 3450));
            v.add(new NpcVillage("Pfuhldorf", 2150, 1700));
            v.add(new NpcVillage("Küstgrab", 1860, -1580));
            v.add(new NpcVillage("Mühlsverd", 2570, -4570));
            v.add(new NpcVillage("Quelltal", -1220, -1980));
            return v;
        }
    }

    public static final class NpcVillage {
        public String name = "";
        public int x;
        public int z;

        public NpcVillage() {
        }

        public NpcVillage(String name, int x, int z) {
            this.name = name;
            this.x = x;
            this.z = z;
        }
    }

    public static final class Regions {
        public boolean enabled = true;
        public boolean hideOriginalActionbar = true;
        public boolean playEnterSound = true;
        public boolean showBanner = true;

        public boolean showFaction = true;
        public boolean showLeader = true;
        public boolean showCoordinates = false;

        public int displayDurationMs = 5_200;
        public boolean hintTextEnabled = false;

        public String theme = "light";

        public java.util.List<RegionTheme> customThemes = new java.util.ArrayList<>();

        public String overlayPosition = "TOP_CENTER";
        public String menuKey = "key.keyboard.l";

        public int maxTextWidth = 210;
        public int minToastWidth = 170;
        public int maxToastWidth = 330;
        public int screenTopMargin = 6;
        public int iconSize = 24;
        public int iconGap = 6;
        public int paddingLeft = 8;
        public int paddingRight = 10;
        public int paddingTop = 6;
        public int paddingBottom = 6;

        public float baseTextScale = 1.0f;
        public float titleScale = 0.65f;
        public float regionScale = 1.0f;
        public float hierarchyScale = 0.68f;
        public float hintScale = 0.35f;
    }

    public static final class RegionTheme {
        public String name = "Custom";

        public String bg = "#C8AC8E";
        public String borderOut = "#513E2A";
        public String borderTl = "#E6C8A9";
        public String borderBr = "#B8926E";
        public String title = "#503D29";
        public String region = "#503D29";
        public String hierarchy = "#7A5A3A";
        public String hint = "#6A4D33";

        public float baseTextScale = 1.0f;
        public float titleScale = 0.65f;
        public float regionScale = 1.0f;
        public float hierarchyScale = 0.68f;
        public float hintScale = 0.35f;

        public boolean showBanner = true;
        public boolean showEnteredTitle = true;
        public boolean showHierarchy = true;
        public boolean showHint = false;

        public int maxTextWidth = 210;
        public int minToastWidth = 170;
        public int maxToastWidth = 330;
        public int screenTopMargin = 6;
        public int iconSize = 24;
        public int iconGap = 6;
        public int paddingLeft = 8;
        public int paddingRight = 10;
        public int paddingTop = 6;
        public int paddingBottom = 6;
    }

    public static final class RpNames {
        public boolean enabled = true;

        public boolean showInSprechen = true;
        public boolean showInReden = true;
        public boolean showInRufen = true;
        public boolean showInBruellen = true;
        public boolean showInFluestern = true;
        public boolean showInMurmeln = true;

        public boolean showInHilfe = false;
        public boolean showInOfftopic = false;
        public boolean showInOoc = false;
        public boolean showInAllChannels = false;

        public boolean showUnknownAsUnknown = true;

        public boolean unknownShowAccount = false;

        public String unknownPlaceholder = "Unbekannt";

        public String globalPlayerNameColor = "";

        public String globalRpNameColor = "";

        public boolean tablistEnabled = true;
        public boolean tablistShowTitle = true;

        public boolean tablistTitlesAlways = true;
        public boolean tablistShowAccountForUnknown = true;

        public boolean openBookOnClick = false;

        public boolean proactiveMeet = true;

        public double meetMarkerHeight = 0.55;
        public double meetMarkerSize = 1.0;
        public double meetMarkerSpinSpeed = 1.0;
        public boolean meetMarkerGlow = true;

        public boolean syncFromPublicApi = true;

    }

    public static final class Nametags {
        public boolean enabled = true;
        public NameTagMode mode = NameTagMode.REALISTIC;
        public boolean showTitle = true;
        public boolean showRpName = true;
        public boolean showPlayerName = false;
        public String toggleKey = "key.keyboard.n";

        public float titleScale = 1.0f;
        public float nameScale = 1.0f;
        public float accountScale = 0.8f;
        public int lineSpacing = 10;

        public String accountColor = "#666666";
    }

    public static final class Letter {
        public boolean enabled = true;
        public String triggerItemName = "Pergament und Feder";
        public int maxLinesPerPage = 12;
        public int maxCharsPerLine = 18;

        public int pageModeMaxLinesPerPage = 12;

        public int pageModeEffectiveCharBudget = 248;

        public String sendMode = "PAGE";

        public int pageModeSendDelayMs = 1200;

        public String letterCommand = "letter";
        public String postCommand = "post";

        public String announcementSubmitCommand = "verkünden";

        public int announcementSafeLinesPerPage = 11;
        public int announcementSafeCharsPerLine = 17;
        public int announcementHardLinesPerPage = 12;
        public int announcementHardCharsPerLine = 18;

        public int letterSendDelayMinMs = 500;
        public int letterSendDelayMaxMs = 1200;
        public int letterPageDelayMinMs = 3000;
        public int letterPageDelayMaxMs = 5200;

        public boolean formattingEnabled = true;

        public boolean formattingSidebarVisible = true;

        public boolean formattingConvertAmpersandOnPaste = true;

        public boolean previewEnabled = true;
    }

    public static final class Chat {
        public boolean enabled = true;

        public boolean offtopicBangEnabled = false;

        public int offtopicBangCount = 3;

        public boolean autoSprechenOnJoin = true;

        public boolean shiftTabCycleChannels = true;

        public boolean longChatEnabled = true;

        public int longChatMaxInput = 8192;

        public int longChatChunk = 256;

        public String longChatMarker = " >";

        public int longChatDelayMs = 800;
        public String voiceKey = "key.keyboard.v";
        public String helpKey = "key.keyboard.h";
        public String offtopicKey = "key.keyboard.o";
    }

    public static final class ResourcePack {
        public boolean enabled = true;

        public String manifestUrl = "https://api.github.com/repos/Ottonien/ottonien-reformed/releases/latest";

        public String assetName = "Ottonien.zip";

        public String directZipUrl = "";
        public boolean autoEnable = true;
        public boolean checkOnStartup = true;
        public boolean respectUserDisable = false;
        public boolean priorityTop = true;
        public long maxSizeBytes = 64L * 1024 * 1024;
        public int connectTimeoutMs = 10_000;
        public int requestTimeoutMs = 60_000;
        public boolean showToasts = true;

        public String effectiveSource() {
            if (manifestUrl != null && !manifestUrl.isBlank()) {
                return manifestUrl.trim();
            }
            if (directZipUrl != null && !directZipUrl.isBlank()) {
                return directZipUrl.trim();
            }
            return "";
        }

        public boolean usesManifest() {
            return manifestUrl != null && !manifestUrl.isBlank();
        }
    }

    private static volatile OttoExtraConfig active;

    public static OttoExtraConfig active() {
        OttoExtraConfig a = active;
        return a != null ? a : load();
    }

    public static OttoExtraConfig load() {
        Path file = OttoExtraPaths.configFile();
        OttoExtraConfig config;
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                config = GSON.fromJson(json, OttoExtraConfig.class);
                if (config == null) {
                    config = new OttoExtraConfig();
                }
            } catch (Exception e) {
                OttoExtra.LOGGER.error("Config defekt — verwende Defaults. ({})", e.getMessage());
                config = new OttoExtraConfig();
            }
        } else {
            config = new OttoExtraConfig();
        }
        config.repair();
        config.save();
        active = config;
        return config;
    }

    public void repair() {
        if (api == null) api = new Api();
        if (map == null) map = new Map();
        if (regions == null) regions = new Regions();
        if (rpnames == null) rpnames = new RpNames();
        if (nametags == null) nametags = new Nametags();
        if (letter == null) letter = new Letter();
        if (chat == null) chat = new Chat();
        if (resourcepack == null) resourcepack = new ResourcePack();
        if (tweaks == null) tweaks = new Tweaks();
        if (tweaks.lowHealth == null) tweaks.lowHealth = new Tweaks.LowHealth();
        if (tweaks.toolProtect == null) tweaks.toolProtect = new Tweaks.ToolProtect();
        if (tweaks.toolProtect.uiBlocks == null) {
            tweaks.toolProtect.uiBlocks = Tweaks.ToolProtect.defaultUiBlocks();
        }
        if (resourcepack.maxSizeBytes <= 0) resourcepack.maxSizeBytes = new ResourcePack().maxSizeBytes;
        Regions rd = new Regions();
        if (regions.maxTextWidth < 50) regions.maxTextWidth = rd.maxTextWidth;
        if (regions.minToastWidth < 50) regions.minToastWidth = rd.minToastWidth;
        if (regions.maxToastWidth < regions.minToastWidth) regions.maxToastWidth = Math.max(rd.maxToastWidth, regions.minToastWidth);
        if (regions.screenTopMargin < 0) regions.screenTopMargin = rd.screenTopMargin;
        if (regions.iconSize < 8) regions.iconSize = rd.iconSize;
        if (regions.iconGap < 0) regions.iconGap = rd.iconGap;
        if (regions.paddingLeft < 0) regions.paddingLeft = rd.paddingLeft;
        if (regions.paddingRight < 0) regions.paddingRight = rd.paddingRight;
        if (regions.paddingTop < 0) regions.paddingTop = rd.paddingTop;
        if (regions.paddingBottom < 0) regions.paddingBottom = rd.paddingBottom;
        if (regions.baseTextScale < 0.3f || regions.baseTextScale > 3f) regions.baseTextScale = rd.baseTextScale;
        if (regions.titleScale < 0.3f || regions.titleScale > 3f) regions.titleScale = rd.titleScale;
        if (regions.regionScale < 0.3f || regions.regionScale > 3f) regions.regionScale = rd.regionScale;
        if (regions.hierarchyScale < 0.3f || regions.hierarchyScale > 3f) regions.hierarchyScale = rd.hierarchyScale;
        if (regions.hintScale < 0.3f || regions.hintScale > 3f) regions.hintScale = rd.hintScale;
        if (regions.theme == null || regions.theme.isBlank()) regions.theme = rd.theme;
        if (regions.overlayPosition == null || regions.overlayPosition.isBlank()) regions.overlayPosition = rd.overlayPosition;
        Map md = new Map();
        if (map.nameMinScale <= 0 || map.nameMinScale > 4) map.nameMinScale = md.nameMinScale;
        if (map.bannerMinScale <= 0 || map.bannerMinScale > 8) map.bannerMinScale = md.bannerMinScale;
        if (map.bannerSizePx < 8 || map.bannerSizePx > 64) map.bannerSizePx = md.bannerSizePx;
        if (map.borderWidthPx < 1 || map.borderWidthPx > 4) map.borderWidthPx = md.borderWidthPx;
        if (map.toggleKey == null || map.toggleKey.isBlank()) map.toggleKey = md.toggleKey;
        if (map.dashLengthPx < 2 || map.dashLengthPx > 64) map.dashLengthPx = md.dashLengthPx;
        if (map.dashGapPx < 0 || map.dashGapPx > 64) map.dashGapPx = md.dashGapPx;
        if (nametags.mode == null) nametags.mode = NameTagMode.REALISTIC;


        if (api.baseUrl == null || api.baseUrl.isBlank()) api.baseUrl = new Api().baseUrl;

        if (api.baseUrl.startsWith("http://")) {
            api.baseUrl = "https://" + api.baseUrl.substring("http://".length());
        }
    }

    public String snapshotJson() {
        return GSON.toJson(this);
    }

    public void restoreFrom(String json) {
        OttoExtraConfig parsed = GSON.fromJson(json, OttoExtraConfig.class);
        if (parsed == null) {
            return;
        }
        parsed.repair();
        copyFields(parsed.api, api);
        copyFields(parsed.map, map);
        copyFields(parsed.regions, regions);
        copyFields(parsed.rpnames, rpnames);
        copyFields(parsed.nametags, nametags);
        copyFields(parsed.letter, letter);
        copyFields(parsed.chat, chat);
        copyFields(parsed.resourcepack, resourcepack);
        copyFields(parsed.tweaks.lowHealth, tweaks.lowHealth);
        copyFields(parsed.tweaks.toolProtect, tweaks.toolProtect);
    }

    private static void copyFields(Object from, Object to) {
        for (java.lang.reflect.Field f : from.getClass().getFields()) {
            try {
                f.set(to, f.get(from));
            } catch (ReflectiveOperationException e) {
                OttoExtra.LOGGER.warn("Config-Restore: Feld {} nicht kopierbar", f.getName());
            }
        }
    }

    public void save() {
        Path file = OttoExtraPaths.configFile();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("ottoextra.json.tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            OttoExtra.LOGGER.error("Config konnte nicht gespeichert werden: {}", e.getMessage());
        }
    }
}
