package de.ottoextra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.nametags.NameTagMode;
import de.ottoextra.tweaks.vote.VoteReminderConfig;

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

    private transient boolean persistenceBlocked;

    public Api api = new Api();
    public Map map = new Map();
    public Regions regions = new Regions();
    public RpNames rpnames = new RpNames();
    public Nametags nametags = new Nametags();
    public Letter letter = new Letter();
    public Chat chat = new Chat();
    public ResourcePack resourcepack = new ResourcePack();
    public Consent consent = new Consent();
    public AnimalInfo animalInfo = new AnimalInfo();
    public Tweaks tweaks = new Tweaks();

    /**
     * Ausdrückliche Entscheidungen aus dem Erststart-Assistenten.
     * Fehlende/alte Werte bleiben absichtlich auf false und gelten nie als Zustimmung.
     */
    public static final class Consent {
        public int onboardingVersion = 0;
        public boolean automaticResourcePackDownloads = false;
        public boolean externalImageDownloads = false;
        public boolean updateChecks = false;
        public volatile boolean communityRpNameUploads = false;
        private transient volatile long uploadGeneration;

        public synchronized void setCommunityRpNameUploads(boolean allowed) {
            if (communityRpNameUploads && !allowed) uploadGeneration++;
            communityRpNameUploads = allowed;
        }

        public synchronized long uploadGeneration() {
            return uploadGeneration;
        }

        public synchronized boolean allowsUpload(long generation) {
            return communityRpNameUploads && generation == uploadGeneration;
        }
    }

    public static final class AnimalInfo {
        public static final java.util.List<String> ANIMAL_TYPES = java.util.List.of(
                "sheep", "cow", "pig", "streitross", "reitpferd", "ackergaul",
                "horse", "donkey",
                "goat", "chicken", "wolf", "other");

        public boolean enabled = true;
        public boolean hideAutomaticChat = true;
        /** Lokaler Opt-in fuer Zuchtakte, Hofsync und Zucht-Autofill. */
        public boolean breederModeEnabled = false;
        public int hoverDelayMs = 100;
        public int cacheDurationMs = 5_000;
        public int responseTimeoutMs = 3_000;
        /** Interne, einmalige Migration des alten 300-ms-Standardwerts. */
        public Integer queryTimingVersion;

        public int padding = 3;
        public boolean showModel = true;
        public boolean showTitle = true;
        public boolean showBreeding = true;
        public boolean showMast = true;
        public boolean showTimer = true;
        public boolean showExtra = true;
        public boolean showHorseStats = true;

        public int modelX = 0;
        public int modelY = 0;
        public int modelSize = 26;
        public int titleX = 28;
        public int titleY = 0;
        public float titleScale = 1.0f;
        public int breedingX = 28;
        public int breedingY = 10;
        public float breedingScale = 0.68f;
        public int mastX = 100;
        public int mastY = 10;
        public float mastScale = 0.68f;
        public int timerX = 28;
        public int timerY = 19;
        public float timerScale = 0.68f;
        public int extraX = 100;
        public int extraY = 19;
        public float extraScale = 0.68f;
        public int horseStatsX = 28;
        public int horseStatsY = 28;
        public float horseStatsScale = 0.68f;

        public java.util.Map<String, AnimalInfoLayout> layouts = new java.util.LinkedHashMap<>();

        public AnimalInfoLayout layout(String animalType) {
            if (layouts == null) {
                layouts = new java.util.LinkedHashMap<>();
            }
            String key = normalizeAnimalType(animalType);
            return layouts.computeIfAbsent(key, AnimalInfoLayout::defaults);
        }

        public void resetLayout(String animalType) {
            String key = normalizeAnimalType(animalType);
            if (layouts == null) {
                layouts = new java.util.LinkedHashMap<>();
            }
            boolean enabled = layout(key).enabled;
            AnimalInfoLayout reset = AnimalInfoLayout.defaults(key);
            reset.enabled = enabled;
            layouts.put(key, reset);
        }

        private void repairLayouts() {
            if (layouts == null) {
                layouts = new java.util.LinkedHashMap<>();
            }
            for (String type : ANIMAL_TYPES) {
                AnimalInfoLayout value = layouts.get(type);
                if (value == null) {
                    layouts.put(type, AnimalInfoLayout.fromLegacy(this, type));
                } else {
                    value.repair(type);
                }
            }
        }

        private static String normalizeAnimalType(String value) {
            String normalized = value == null ? "other"
                    : value.trim().toLowerCase(java.util.Locale.ROOT);
            return ANIMAL_TYPES.contains(normalized) ? normalized : "other";
        }
    }

    public static final class AnimalInfoLayout {
        /** Ob die automatische Tierinfo für diese Tierart angezeigt wird. */
        public boolean enabled = true;
        public int padding = 3;
        public boolean showModel = true;
        public boolean showTitle = true;
        public boolean showGenderIcon = true;
        public boolean showGenderText = true;
        public boolean showBreeding = true;
        public boolean showMast = true;
        public boolean showTimer = true;
        public boolean showExtra = true;
        /** Alte Sammeloption; dient nur noch als Migrationsquelle. */
        public boolean showHorseStats = true;
        public Boolean showJumpHeight;
        public Boolean showSpeed;

        public int modelX = 0;
        public int modelY = 0;
        public int modelSize = 26;
        public int titleX = 28;
        public int titleY = 0;
        public float titleScale = 1.0f;
        public int genderIconX = 70;
        public int genderIconY = 0;
        public float genderIconScale = 1.0f;
        public int genderTextX = 81;
        public int genderTextY = 1;
        public float genderTextScale = 0.68f;
        public int breedingX = 28;
        public int breedingY = 10;
        public float breedingScale = 0.68f;
        public int mastX = 100;
        public int mastY = 10;
        public float mastScale = 0.68f;
        public int timerX = 28;
        public int timerY = 19;
        public float timerScale = 0.68f;
        public int extraX = 100;
        public int extraY = 19;
        public float extraScale = 0.68f;
        public int horseStatsX = 28;
        public int horseStatsY = 28;
        public float horseStatsScale = 0.68f;
        public int jumpHeightX = -1;
        public int jumpHeightY = -1;
        public float jumpHeightScale = -1.0f;
        public int speedX = -1;
        public int speedY = -1;
        public float speedScale = -1.0f;

        public static AnimalInfoLayout defaults(String animalType) {
            AnimalInfoLayout layout = new AnimalInfoLayout();
            layout.genderIconX = switch (animalType) {
                case "cow", "goat", "chicken", "wolf" -> 60;
                case "horse" -> 64;
                case "streitross" -> 86;
                case "reitpferd", "ackergaul" -> 82;
                case "donkey" -> 76;
                case "pig" -> 68;
                default -> 70;
            };
            layout.genderTextX = layout.genderIconX + 11;
            layout.showJumpHeight = true;
            layout.showSpeed = true;
            layout.jumpHeightX = 28;
            layout.jumpHeightY = 28;
            layout.jumpHeightScale = 0.68f;
            layout.speedX = 100;
            layout.speedY = 28;
            layout.speedScale = 0.68f;
            return layout;
        }

        private static AnimalInfoLayout fromLegacy(AnimalInfo source, String animalType) {
            AnimalInfoLayout layout = defaults(animalType);
            layout.padding = source.padding;
            layout.showModel = source.showModel;
            layout.showTitle = source.showTitle;
            layout.showBreeding = source.showBreeding;
            layout.showMast = source.showMast;
            layout.showTimer = source.showTimer;
            layout.showExtra = source.showExtra;
            layout.showHorseStats = source.showHorseStats;
            layout.showJumpHeight = source.showHorseStats;
            layout.showSpeed = source.showHorseStats;
            layout.modelX = source.modelX;
            layout.modelY = source.modelY;
            layout.modelSize = source.modelSize;
            layout.titleX = source.titleX;
            layout.titleY = source.titleY;
            layout.titleScale = source.titleScale;
            layout.breedingX = source.breedingX;
            layout.breedingY = source.breedingY;
            layout.breedingScale = source.breedingScale;
            layout.mastX = source.mastX;
            layout.mastY = source.mastY;
            layout.mastScale = source.mastScale;
            layout.timerX = source.timerX;
            layout.timerY = source.timerY;
            layout.timerScale = source.timerScale;
            layout.extraX = source.extraX;
            layout.extraY = source.extraY;
            layout.extraScale = source.extraScale;
            layout.horseStatsX = source.horseStatsX;
            layout.horseStatsY = source.horseStatsY;
            layout.horseStatsScale = source.horseStatsScale;
            layout.jumpHeightX = source.horseStatsX;
            layout.jumpHeightY = source.horseStatsY;
            layout.jumpHeightScale = source.horseStatsScale;
            layout.speedX = Math.min(500, source.horseStatsX + 72);
            layout.speedY = source.horseStatsY;
            layout.speedScale = source.horseStatsScale;
            return layout;
        }

        private void repair(String animalType) {
            AnimalInfoLayout defaults = defaults(animalType);
            if (showJumpHeight == null) showJumpHeight = showHorseStats;
            if (showSpeed == null) showSpeed = showHorseStats;
            if (jumpHeightX < 0) jumpHeightX = horseStatsX;
            if (jumpHeightY < 0) jumpHeightY = horseStatsY;
            if (jumpHeightScale < 0.0f) jumpHeightScale = horseStatsScale;
            if (speedX < 0) speedX = Math.min(500, horseStatsX + 72);
            if (speedY < 0) speedY = horseStatsY;
            if (speedScale < 0.0f) speedScale = horseStatsScale;
            if (padding < 0 || padding > 20) padding = defaults.padding;
            modelX = repairLayoutPosition(modelX, defaults.modelX);
            modelY = repairLayoutPosition(modelY, defaults.modelY);
            titleX = repairLayoutPosition(titleX, defaults.titleX);
            titleY = repairLayoutPosition(titleY, defaults.titleY);
            genderIconX = repairLayoutPosition(genderIconX, defaults.genderIconX);
            genderIconY = repairLayoutPosition(genderIconY, defaults.genderIconY);
            genderTextX = repairLayoutPosition(genderTextX, defaults.genderTextX);
            genderTextY = repairLayoutPosition(genderTextY, defaults.genderTextY);
            breedingX = repairLayoutPosition(breedingX, defaults.breedingX);
            breedingY = repairLayoutPosition(breedingY, defaults.breedingY);
            mastX = repairLayoutPosition(mastX, defaults.mastX);
            mastY = repairLayoutPosition(mastY, defaults.mastY);
            timerX = repairLayoutPosition(timerX, defaults.timerX);
            timerY = repairLayoutPosition(timerY, defaults.timerY);
            extraX = repairLayoutPosition(extraX, defaults.extraX);
            extraY = repairLayoutPosition(extraY, defaults.extraY);
            horseStatsX = repairLayoutPosition(horseStatsX, defaults.horseStatsX);
            horseStatsY = repairLayoutPosition(horseStatsY, defaults.horseStatsY);
            jumpHeightX = repairLayoutPosition(jumpHeightX, defaults.jumpHeightX);
            jumpHeightY = repairLayoutPosition(jumpHeightY, defaults.jumpHeightY);
            speedX = repairLayoutPosition(speedX, defaults.speedX);
            speedY = repairLayoutPosition(speedY, defaults.speedY);
            if (modelSize < 12 || modelSize > 64) modelSize = defaults.modelSize;
            titleScale = repairAnimalInfoScale(titleScale, defaults.titleScale);
            genderIconScale = repairAnimalInfoScale(
                    genderIconScale, defaults.genderIconScale);
            genderTextScale = repairAnimalInfoScale(
                    genderTextScale, defaults.genderTextScale);
            breedingScale = repairAnimalInfoScale(breedingScale, defaults.breedingScale);
            mastScale = repairAnimalInfoScale(mastScale, defaults.mastScale);
            timerScale = repairAnimalInfoScale(timerScale, defaults.timerScale);
            extraScale = repairAnimalInfoScale(extraScale, defaults.extraScale);
            horseStatsScale = repairAnimalInfoScale(
                    horseStatsScale, defaults.horseStatsScale);
            jumpHeightScale = repairAnimalInfoScale(
                    jumpHeightScale, defaults.jumpHeightScale);
            speedScale = repairAnimalInfoScale(speedScale, defaults.speedScale);
        }
    }

    public static final class Tweaks {
        public boolean chatLog = true;
        public boolean combatModeHud = true;
        public LowHealth lowHealth = new LowHealth();
        public ToolProtect toolProtect = new ToolProtect();
        public VoteReminderConfig vote = new VoteReminderConfig();
        public Litematica litematica = new Litematica();

        public static final class Litematica {
            public boolean localizedBlockNames = true;
            public boolean fachwerkAutoTune = false;
        }

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
        public String baseUrl = "https://api.ottoextra.dev/";
        public int connectTimeoutMs = 10_000;
        public int requestTimeoutMs = 20_000;
        public int syncIntervalSeconds = 1_800;
        public int playerDirectoryIntervalSeconds = 300;

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
        public static final int MIN_ACTIVITY_HUD_REFRESH_SECONDS = 5;
        public static final int MAX_ACTIVITY_HUD_REFRESH_SECONDS = 3_600;

        public boolean enabled = true;
        public boolean hideOriginalActionbar = true;
        public boolean playEnterSound = true;
        public boolean showBanner = true;

        public boolean showFaction = true;
        public boolean showLeader = true;
        public boolean showLeaderRpName = true;
        public boolean showCoordinates = false;

        public int displayDurationMs = 5_200;
        public boolean hintTextEnabled = false;

        public String theme = "light";

        public java.util.List<RegionTheme> customThemes = new java.util.ArrayList<>();

        public String overlayPosition = "TOP_CENTER";
        public String menuKey = "key.keyboard.l";

        public boolean activityHudEnabled = true;
        public String activityHudPosition = "MIDDLE_LEFT";
        public int activityHudOffsetX = 0;
        public int activityHudOffsetY = 0;
        public int activityHudMaxEntries = 8;
        public float activityHudScale = 1.0f;
        public boolean activityHudShowLastUpdated = true;
        public boolean activityHudShowClock = true;
        public float activityHudCustomX = 0.5f;
        public float activityHudCustomY = 0.5f;
        public int activityHudRefreshSeconds = 120;

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

        /** Optional legacy behavior: use a noble person's estate color for other titles too. */
        public boolean nobilityColorForAllTitles = false;

        /** Neue Installationen zeigen in der Tabliste zunaechst Accountnamen. */
        public boolean tablistEnabled = false;
        public boolean tablistShowTitle = true;
        public boolean tablistSortByRegion = false;
        /** Titel von noch unbekannten Personen nur optional in der Tabliste zeigen. */
        public boolean tablistTitlesAlways = true;
        public boolean tablistShowAccountForUnknown = true;

        public boolean openBookOnClick = false;

        public boolean proactiveMeet = true;

        public double meetMarkerHeight = 0.55;
        public double meetMarkerSize = 1.0;
        public double meetMarkerSpinSpeed = 1.0;
        public boolean meetMarkerGlow = true;

        public boolean syncFromPublicApi = true;

        public boolean inspectEnabled = true;
        public boolean inspectShowPlayerHands = true;
        public boolean inspectShowPlayerArmor = true;
        public boolean inspectShowAccount = false;
        public double inspectMaxDistance = 8.0;
        public int inspectOffsetY = 38;
        public boolean inspectZoomEnabled = true;
        public double inspectZoomDegrees = 9.0;
        public boolean inspectEdgeBlurEnabled = true;
        public double inspectRevealSeconds = 2.0;
        public boolean inspectRoleplayPhrasesEnabled = true;
        public double inspectRoleplayPhraseSeconds = 2.0;

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

        /** Historische V2-Migration; neue Konfigurationen starten direkt im PAGE-Modus. */
        public int courierV2Migration = 1;

        public int pageModeSendDelayMs = 1200;

        public String letterCommand = "letter";
        public String postCommand = "post";

        public String announcementSubmitCommand = "verkünden";

        public int announcementSafeLinesPerPage = 11;
        public int announcementSafeCharsPerLine = 17;
        public int announcementHardLinesPerPage = 12;
        public int announcementHardCharsPerLine = 18;

        public boolean announcementAutoOptimizeEnabled = false;

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
        public static final String DEFAULT_NPC_NAME_COLOR = "#C7A87F";
        public static final String DEFAULT_NPC_MESSAGE_COLOR = "#DFC8A7";

        public boolean enabled = true;

        public boolean offtopicBangEnabled = false;

        public int offtopicBangCount = 3;

        public boolean autoSprechenOnJoin = true;

        public boolean shiftTabCycleChannels = true;

        public boolean longChatEnabled = true;

        public boolean rpFormattingEnabled = true;
        public String npcNameColor = DEFAULT_NPC_NAME_COLOR;
        public String npcMessageColor = DEFAULT_NPC_MESSAGE_COLOR;
        public String rpEmoteColor = "#C6C6C6";
        public String rpOocColor = "#B4BEC6";
        public boolean rpEmoteItalic = true;
        public boolean rpOocItalic = false;

        /** Optionale lokale Farben je Server-Chatkanal; leer = Serverstil. */
        public java.util.Map<String, ChannelColors> channelColors = defaultChannelColors();

        public int longChatMaxInput = 8192;

        public int longChatChunk = 256;

        public String longChatMarker = " >";

        public int longChatDelayMs = 800;
        public String voiceKey = "key.keyboard.v";
        public String helpKey = "key.keyboard.h";
        public String offtopicKey = "key.keyboard.o";

        public ChannelColors channelColors(String channel) {
            if (channelColors == null) {
                channelColors = defaultChannelColors();
            }
            return channelColors.computeIfAbsent(channel,
                    key -> new ChannelColors(defaultMessageColor(key)));
        }

        public static String defaultMessageColor(String channel) {
            return switch (channel == null ? "" : channel) {
                case "sprechen" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.SPRECHEN);
                case "fluestern" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.FLUESTERN);
                case "murmeln" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.MURMELN);
                case "rufen" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.RUFEN);
                case "bruellen" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.BRUELLEN);
                case "offtopic" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.OFFTOPIC);
                case "hilfe" -> de.ottoextra.chat.ChatChannelButton.originalHex(
                        de.ottoextra.chat.ChatChannelState.ChatChannel.HILFE);
                default -> "#FFFFFF";
            };
        }

        private static java.util.Map<String, ChannelColors> defaultChannelColors() {
            java.util.Map<String, ChannelColors> colors = new java.util.LinkedHashMap<>();
            for (String channel : java.util.List.of(
                    "sprechen", "fluestern", "murmeln", "rufen", "bruellen",
                    "offtopic", "hilfe")) {
                colors.put(channel, new ChannelColors(defaultMessageColor(channel)));
            }
            return colors;
        }
    }

    public static final class ChannelColors {
        /** Farbe von [Kanalname], leer behaelt den Serverstil. */
        public String labelColor = "";
        /** Lokale Standardfarbe des Nachrichtentextes nach dem Sprecher. */
        public String messageColor = "";

        public ChannelColors() {
        }

        private ChannelColors(String messageColor) {
            this.messageColor = messageColor;
        }
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
        OttoExtraConfig config = load(OttoExtraPaths.configFile());
        active = config;
        return config;
    }

    static OttoExtraConfig load(Path file) {
        return load(file, source -> {
            Path backup = Files.createTempFile(source.toAbsolutePath().getParent(),
                    source.getFileName() + ".broken-", ".json");
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            if (Files.mismatch(source, backup) != -1) {
                throw new IOException("Fehlersicherung stimmt nicht mit Original überein");
            }
            OttoExtra.LOGGER.warn("Defekte Config gesichert: {}", backup);
        });
    }

    @FunctionalInterface
    interface BrokenConfigBackup {
        void preserve(Path file) throws IOException;
    }

    static OttoExtraConfig load(Path file, BrokenConfigBackup backup) {
        OttoExtraConfig config;
        if (!Files.notExists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                config = GSON.fromJson(json, OttoExtraConfig.class);
                if (config == null) {
                    throw new IOException("Leere Config");
                }
            } catch (Exception e) {
                OttoExtra.LOGGER.error("Config defekt — verwende Defaults. ({})", e.getMessage());
                config = new OttoExtraConfig();
                try {
                    backup.preserve(file);
                } catch (Exception backupError) {
                    config.persistenceBlocked = true;
                    OttoExtra.LOGGER.error("Config-Sicherung fehlgeschlagen; Original bleibt erhalten: {}",
                            backupError.getMessage());
                }
            }
        } else {
            config = new OttoExtraConfig();
        }
        config.repair();
        config.save(file);
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
        if (consent == null) consent = new Consent();
        if (animalInfo == null) animalInfo = new AnimalInfo();
        if (tweaks == null) tweaks = new Tweaks();
        if (tweaks.lowHealth == null) tweaks.lowHealth = new Tweaks.LowHealth();
        if (tweaks.toolProtect == null) tweaks.toolProtect = new Tweaks.ToolProtect();
        if (tweaks.vote == null) tweaks.vote = new VoteReminderConfig();
        if (tweaks.litematica == null) tweaks.litematica = new Tweaks.Litematica();
        tweaks.vote.sanitize();
        if (tweaks.toolProtect.uiBlocks == null) {
            tweaks.toolProtect.uiBlocks = Tweaks.ToolProtect.defaultUiBlocks();
        }
        AnimalInfo aid = new AnimalInfo();
        if (animalInfo.queryTimingVersion == null
                || animalInfo.queryTimingVersion < 1) {
            // Nur den bisherigen Default migrieren; bewusst gewaehlte
            // langsamere Hover-Zeiten bleiben erhalten.
            if (animalInfo.hoverDelayMs == 300) {
                animalInfo.hoverDelayMs = aid.hoverDelayMs;
            }
            animalInfo.queryTimingVersion = 1;
        }
        if (animalInfo.hoverDelayMs < 100 || animalInfo.hoverDelayMs > 2_000) {
            animalInfo.hoverDelayMs = aid.hoverDelayMs;
        }
        if (animalInfo.cacheDurationMs < 1_000 || animalInfo.cacheDurationMs > 60_000) {
            animalInfo.cacheDurationMs = aid.cacheDurationMs;
        }
        if (animalInfo.responseTimeoutMs < 1_000 || animalInfo.responseTimeoutMs > 10_000) {
            animalInfo.responseTimeoutMs = aid.responseTimeoutMs;
        }
        if (animalInfo.padding < 0 || animalInfo.padding > 20) {
            animalInfo.padding = aid.padding;
        }
        animalInfo.modelX = repairLayoutPosition(animalInfo.modelX, aid.modelX);
        animalInfo.modelY = repairLayoutPosition(animalInfo.modelY, aid.modelY);
        animalInfo.titleX = repairLayoutPosition(animalInfo.titleX, aid.titleX);
        animalInfo.titleY = repairLayoutPosition(animalInfo.titleY, aid.titleY);
        animalInfo.breedingX = repairLayoutPosition(animalInfo.breedingX, aid.breedingX);
        animalInfo.breedingY = repairLayoutPosition(animalInfo.breedingY, aid.breedingY);
        animalInfo.mastX = repairLayoutPosition(animalInfo.mastX, aid.mastX);
        animalInfo.mastY = repairLayoutPosition(animalInfo.mastY, aid.mastY);
        animalInfo.timerX = repairLayoutPosition(animalInfo.timerX, aid.timerX);
        animalInfo.timerY = repairLayoutPosition(animalInfo.timerY, aid.timerY);
        animalInfo.extraX = repairLayoutPosition(animalInfo.extraX, aid.extraX);
        animalInfo.extraY = repairLayoutPosition(animalInfo.extraY, aid.extraY);
        if (animalInfo.modelSize < 12 || animalInfo.modelSize > 64) {
            animalInfo.modelSize = aid.modelSize;
        }
        animalInfo.titleScale = repairAnimalInfoScale(animalInfo.titleScale, aid.titleScale);
        animalInfo.breedingScale = repairAnimalInfoScale(
                animalInfo.breedingScale, aid.breedingScale);
        animalInfo.mastScale = repairAnimalInfoScale(animalInfo.mastScale, aid.mastScale);
        animalInfo.timerScale = repairAnimalInfoScale(animalInfo.timerScale, aid.timerScale);
        animalInfo.extraScale = repairAnimalInfoScale(animalInfo.extraScale, aid.extraScale);
        animalInfo.repairLayouts();
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
        if (regions.activityHudPosition == null || regions.activityHudPosition.isBlank()) {
            regions.activityHudPosition = rd.activityHudPosition;
        }
        if (regions.activityHudOffsetX < -1000 || regions.activityHudOffsetX > 1000) {
            regions.activityHudOffsetX = rd.activityHudOffsetX;
        }
        if (regions.activityHudOffsetY < -1000 || regions.activityHudOffsetY > 1000) {
            regions.activityHudOffsetY = rd.activityHudOffsetY;
        }
        if (regions.activityHudMaxEntries < 1 || regions.activityHudMaxEntries > 20) {
            regions.activityHudMaxEntries = rd.activityHudMaxEntries;
        }
        if (regions.activityHudScale < 0.5f || regions.activityHudScale > 2.0f) {
            regions.activityHudScale = rd.activityHudScale;
        }
        if (regions.activityHudCustomX < 0.0f || regions.activityHudCustomX > 1.0f) {
            regions.activityHudCustomX = rd.activityHudCustomX;
        }
        if (regions.activityHudCustomY < 0.0f || regions.activityHudCustomY > 1.0f) {
            regions.activityHudCustomY = rd.activityHudCustomY;
        }
        if (regions.activityHudRefreshSeconds
                < Regions.MIN_ACTIVITY_HUD_REFRESH_SECONDS
                || regions.activityHudRefreshSeconds
                > Regions.MAX_ACTIVITY_HUD_REFRESH_SECONDS) {
            regions.activityHudRefreshSeconds = rd.activityHudRefreshSeconds;
        }
        Map md = new Map();
        if (map.nameMinScale <= 0 || map.nameMinScale > 4) map.nameMinScale = md.nameMinScale;
        if (map.bannerMinScale <= 0 || map.bannerMinScale > 8) map.bannerMinScale = md.bannerMinScale;
        if (map.bannerSizePx < 8 || map.bannerSizePx > 64) map.bannerSizePx = md.bannerSizePx;
        if (map.borderWidthPx < 1 || map.borderWidthPx > 4) map.borderWidthPx = md.borderWidthPx;
        if (map.toggleKey == null || map.toggleKey.isBlank()) map.toggleKey = md.toggleKey;
        if (map.dashLengthPx < 2 || map.dashLengthPx > 64) map.dashLengthPx = md.dashLengthPx;
        if (map.dashGapPx < 0 || map.dashGapPx > 64) map.dashGapPx = md.dashGapPx;
        if (nametags.mode == null) nametags.mode = NameTagMode.REALISTIC;
        if (letter.courierV2Migration < 1) {
            letter.sendMode = "V2";
            letter.courierV2Migration = 1;
        }
        RpNames rpn = new RpNames();
        if (rpnames.inspectMaxDistance < 1.0 || rpnames.inspectMaxDistance > 32.0) {
            rpnames.inspectMaxDistance = rpn.inspectMaxDistance;
        }
        if (rpnames.inspectZoomDegrees < 0.0 || rpnames.inspectZoomDegrees > 30.0) {
            rpnames.inspectZoomDegrees = rpn.inspectZoomDegrees;
        }
        if (rpnames.inspectRevealSeconds < 0.5 || rpnames.inspectRevealSeconds > 4.0) {
            rpnames.inspectRevealSeconds = rpn.inspectRevealSeconds;
        }
        if (rpnames.inspectRoleplayPhraseSeconds < 0.5
                || rpnames.inspectRoleplayPhraseSeconds > 4.0) {
            rpnames.inspectRoleplayPhraseSeconds = rpn.inspectRoleplayPhraseSeconds;
        }
        Chat cd = new Chat();
        chat.npcNameColor = requiredHexColor(chat.npcNameColor, cd.npcNameColor);
        chat.npcMessageColor = requiredHexColor(
                chat.npcMessageColor, cd.npcMessageColor);
        if (chat.rpEmoteColor == null || chat.rpEmoteColor.isBlank()) {
            chat.rpEmoteColor = cd.rpEmoteColor;
        }
        if (chat.rpOocColor == null || chat.rpOocColor.isBlank()) {
            chat.rpOocColor = cd.rpOocColor;
        }
        if (chat.channelColors == null) {
            chat.channelColors = cd.channelColors;
        } else {
            for (String channel : cd.channelColors.keySet()) {
                ChannelColors colors = chat.channelColors(channel);
                colors.labelColor = optionalHexColor(colors.labelColor);
                colors.messageColor = requiredHexColor(
                        colors.messageColor, Chat.defaultMessageColor(channel));
            }
        }


        if (api.baseUrl == null || api.baseUrl.isBlank()) api.baseUrl = new Api().baseUrl;

        if (api.baseUrl.startsWith("http://")) {
            api.baseUrl = "https://" + api.baseUrl.substring("http://".length());
        }
        if (api.baseUrl.equalsIgnoreCase("https://regions.skyfuller.de")
                || api.baseUrl.equalsIgnoreCase("https://regions.skyfuller.de/")) {
            api.baseUrl = new Api().baseUrl;
        }
    }

    private static String requiredHexColor(String value, String fallback) {
        String normalized = optionalHexColor(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static int repairLayoutPosition(int value, int fallback) {
        return value < 0 || value > 500 ? fallback : value;
    }

    private static float repairAnimalInfoScale(float value, float fallback) {
        return value < 0.5f || value > 2.0f ? fallback : value;
    }

    private static String optionalHexColor(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        return normalized.matches("#[0-9a-fA-F]{6}")
                ? normalized.toUpperCase(java.util.Locale.ROOT) : "";
    }

    public String snapshotJson() {
        return GSON.toJson(this);
    }

    public synchronized void restoreFrom(String json) {
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
        consent.setCommunityRpNameUploads(parsed.consent.communityRpNameUploads);
        copyFields(parsed.consent, consent);
        copyFields(parsed.animalInfo, animalInfo);
        tweaks.combatModeHud = parsed.tweaks.combatModeHud;
        copyFields(parsed.tweaks.lowHealth, tweaks.lowHealth);
        copyFields(parsed.tweaks.toolProtect, tweaks.toolProtect);
        copyFields(parsed.tweaks.vote, tweaks.vote);
        copyFields(parsed.tweaks.litematica, tweaks.litematica);
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

    public synchronized void save() {
        save(OttoExtraPaths.configFile());
    }

    synchronized void save(Path file) {
        if (persistenceBlocked) return;
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
