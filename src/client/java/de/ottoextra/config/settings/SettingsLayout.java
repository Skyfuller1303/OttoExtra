package de.ottoextra.config.settings;

import java.util.List;

/**
 * Arrangement of all settings as the settings screen shows them: category, tab, section and
 * how prominently each setting appears. Settings are referenced by their config key; the
 * controls themselves still come from {@link SettingsRegistry} and the addons.
 *
 * <p>Generated from docs/einstellungen-gui (vorschlag-kategorien.md, beschriftungen.md).
 */
public final class SettingsLayout {

    /** How prominently a setting is shown. */
    public enum Level {
        /** Always visible. */
        BASIC,
        /** Behind "more options" of its section. */
        MORE,
        /** Only with expert settings switched on. */
        EXPERT
    }

    public record Entry(String configKey, Level level) {
    }

    public record Section(String titleKey, List<Entry> entries) {
        public String descKey() {
            return titleKey + ".desc";
        }
    }

    /** A tab inside a category. {@code titleKey} is null when the category has a single tab. */
    public record Page(String titleKey, List<Section> sections) {
    }

    public record Category(String id, List<Page> pages) {
        public String titleKey() {
            return "ottoextra.cat." + id;
        }

        public String descKey() {
            return titleKey() + ".desc";
        }
    }

    /** Category that ends with the backups section. */
    public static final String BACKUPS_CATEGORY = "privacy";

    private static final Level B = Level.BASIC;
    private static final Level M = Level.MORE;
    private static final Level X = Level.EXPERT;

    public static String labelKey(String configKey) {
        return "ottoextra.opt." + configKey;
    }

    public static String helpKey(String configKey) {
        return labelKey(configKey) + ".help";
    }

    public static String buttonKey(String configKey) {
        return labelKey(configKey) + ".button";
    }

    public static String valueKey(String configKey, String value) {
        return labelKey(configKey) + ".value." + value;
    }

    public static final List<Category> CATEGORIES = List.of(
            new Category("general", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.general.sec.hud", List.of(
                                    new Entry("hudlayout.open", B))),
                            new Section("ottoextra.cat.general.sec.resourcepack", List.of(
                                    new Entry("resourcepack.enabled", B),
                                    new Entry("resourcepack.autoEnable", B),
                                    new Entry("resourcepack.checkOnStartup", B),
                                    new Entry("resourcepack.manifestUrl", X),
                                    new Entry("resourcepack.assetName", X),
                                    new Entry("resourcepack.connectTimeoutMs", X),
                                    new Entry("resourcepack.requestTimeoutMs", X))),
                            new Section("ottoextra.cat.general.sec.vote", List.of(
                                    new Entry("tweaks.vote.enabled", B),
                                    new Entry("tweaks.vote.reminderIntervalMinutes", M),
                                    new Entry("tweaks.vote.toastDurationSeconds", M),
                                    new Entry("tweaks.vote.resetHour", X),
                                    new Entry("tweaks.vote.joinDelaySeconds", X),
                                    new Entry("vote.enabled", B),
                                    new Entry("vote.reminderInterval", M),
                                    new Entry("vote.toastDuration", M),
                                    new Entry("vote.resetHour", X),
                                    new Entry("vote.joinDelay", X))))))),
            new Category("chat", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.chat.sec.channels", List.of(
                                    new Entry("chat.enabled", B),
                                    new Entry("chat.autoSprechenOnJoin", B),
                                    new Entry("chat.shiftTabCycleChannels", B))),
                            new Section("ottoextra.cat.chat.sec.offtopic", List.of(
                                    new Entry("chat.offtopicBangEnabled", B),
                                    new Entry("chat.offtopicBangCount", M))),
                            new Section("ottoextra.cat.chat.sec.long", List.of(
                                    new Entry("chat.longChatEnabled", B),
                                    new Entry("chat.longChatMarker", M),
                                    new Entry("chat.longChatChunk", X),
                                    new Entry("chat.longChatMaxInput", X),
                                    new Entry("chat.longChatDelayMs", X))),
                            new Section("ottoextra.cat.chat.sec.colors", List.of(
                                    new Entry("chat.rpFormattingEnabled", B),
                                    new Entry("chat.channelColors", B),
                                    new Entry("chat.rpEmoteColor", M),
                                    new Entry("chat.rpEmoteItalic", M),
                                    new Entry("chat.rpOocColor", M),
                                    new Entry("chat.rpOocItalic", M))),
                            new Section("ottoextra.cat.chat.sec.translate", List.of(
                                    new Entry("translate.enabled", B),
                                    new Entry("translate.fullTranslationEnabled", B),
                                    new Entry("translate.privacyNotice", B),
                                    new Entry("translate.sourceLanguage", B),
                                    new Entry("translate.targetLanguage", B),
                                    new Entry("translate.incomingSourceLanguage", B),
                                    new Entry("translate.incomingTargetLanguage", B),
                                    new Entry("translate.showLanguageTag", M),
                                    new Entry("translate.showOriginalMessage", M),
                                    new Entry("translate.feedbackMessages", M),
                                    new Entry("translate.protectEmotes", M),
                                    new Entry("translate.protectOoc", M),
                                    new Entry("translate.protectUrls", M),
                                    new Entry("translate.protectBracketTags", M),
                                    new Entry("translate.protectedOriginalMode", X),
                                    new Entry("translate.protectedOriginalUsers", X),
                                    new Entry("translate.protectedOriginalInline", X),
                                    new Entry("translate.clearCache", X),
                                    new Entry("translate.refreshBundle", X),
                                    new Entry("translate.apiUrl", X),
                                    new Entry("translate.apiKey", X),
                                    new Entry("translate.maxCharacters", X),
                                    new Entry("translate.timeoutMillis", X))),
                            new Section("ottoextra.cat.chat.sec.discord", List.of(
                                    new Entry("discord.enabled", B),
                                    new Entry("discord.relayChat", B),
                                    new Entry("discord.relayStatus", B),
                                    new Entry("discord.feedback", M),
                                    new Entry("discord.endpoint", X),
                                    new Entry("discord.pollInterval", X))),
                            new Section("ottoextra.cat.chat.sec.skript", List.of(
                                    new Entry("skript.enabled", B),
                                    new Entry("skript.open", B),
                                    new Entry("skript.chatButtonVisible", B),
                                    new Entry("skript.hud", M))))))),
            new Category("names", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.names.sec.book", List.of(
                                    new Entry("rpnames.people", B))),
                            new Section("ottoextra.cat.names.sec.chat", List.of(
                                    new Entry("rpnames.enabled", B),
                                    new Entry("rpnames.showInOocChats", B),
                                    new Entry("rpnames.openBookOnClick", M))),
                            new Section("ottoextra.cat.names.sec.unknown", List.of(
                                    new Entry("rpnames.showUnknownAsUnknown", B),
                                    new Entry("rpnames.unknownPlaceholder", M),
                                    new Entry("rpnames.unknownShowAccount", M))),
                            new Section("ottoextra.cat.names.sec.nametags", List.of(
                                    new Entry("nametags.enabled", B),
                                    new Entry("nametags.mode", B),
                                    new Entry("nametags.showTitle", B),
                                    new Entry("nametags.showRpName", B),
                                    new Entry("nametags.showPlayerName", B),
                                    new Entry("nametags.titleScale", M),
                                    new Entry("nametags.nameScale", M),
                                    new Entry("nametags.accountScale", M),
                                    new Entry("nametags.accountColor", M),
                                    new Entry("nametags.lineSpacing", X))),
                            new Section("ottoextra.cat.names.sec.tablist", List.of(
                                    new Entry("rpnames.tablistEnabled", B),
                                    new Entry("rpnames.tablistShowTitle", B),
                                    new Entry("rpnames.tablistTitlesAlways", M),
                                    new Entry("rpnames.tablistSortByRegion", M),
                                    new Entry("tactical.tablistHostilesFirst", M))),
                            new Section("ottoextra.cat.names.sec.colors", List.of(
                                    new Entry("rpnames.globalRpNameColor", M),
                                    new Entry("rpnames.globalPlayerNameColor", M),
                                    new Entry("rpnames.nobilityColorForAllTitles", M))),
                            new Section("ottoextra.cat.names.sec.meet", List.of(
                                    new Entry("rpnames.proactiveMeet", B),
                                    new Entry("rpnames.meetMarkerGlow", M),
                                    new Entry("rpnames.meetMarkerHeight", X),
                                    new Entry("rpnames.meetMarkerSize", X),
                                    new Entry("rpnames.meetMarkerSpinSpeed", X))),
                            new Section("ottoextra.cat.names.sec.inspect", List.of(
                                    new Entry("rpnames.inspectEnabled", B),
                                    new Entry("rpnames.inspectShowPlayerHands", M),
                                    new Entry("rpnames.inspectShowPlayerArmor", M),
                                    new Entry("rpnames.inspectZoomEnabled", M),
                                    new Entry("rpnames.inspectEdgeBlurEnabled", M),
                                    new Entry("rpnames.inspectRoleplayPhrasesEnabled", M),
                                    new Entry("rpnames.inspectZoomDegrees", X),
                                    new Entry("rpnames.inspectRoleplayPhraseSeconds", X),
                                    new Entry("rpnames.inspectRevealSeconds", X))))))),
            new Category("map", List.of(
                    new Page("ottoextra.cat.map.tab.worldmap", List.of(
                            new Section("ottoextra.cat.map.sec.general", List.of(
                                    new Entry("map.enabled", B),
                                    new Entry("map.onlyOnOttonien", M))),
                            new Section("ottoextra.cat.map.sec.content", List.of(
                                    new Entry("map.showBorders", B),
                                    new Entry("map.showNames", B),
                                    new Entry("map.showBanners", B),
                                    new Entry("map.showActivity", B),
                                    new Entry("map.showNpcVillages", B),
                                    new Entry("map.clickInfoPanel", B))),
                            new Section("ottoextra.cat.map.sec.style", List.of(
                                    new Entry("map.paintedMap", B),
                                    new Entry("map.parchmentMode", B),
                                    new Entry("map.politicalFill", B),
                                    new Entry("map.worldMapOpenAnimation", B),
                                    new Entry("map.politicalOpacity", M),
                                    new Entry("map.politicalOpacityNight", M),
                                    new Entry("map.paintedFullCoverZoomOut", M),
                                    new Entry("map.paintedMapSimple", M),
                                    new Entry("map.parchmentMarginPx", X))),
                            new Section("ottoextra.cat.map.sec.colors", List.of(
                                    new Entry("map.following", B),
                                    new Entry("map.groupColors", B),
                                    new Entry("map.borderColor", M),
                                    new Entry("map.dashedBorders", M))),
                            new Section("ottoextra.cat.map.sec.travel", List.of(
                                    new Entry("map.walkBlocksPerSecond", M),
                                    new Entry("map.horseBlocksPerSecond", M))),
                            new Section("ottoextra.cat.map.sec.zoom", List.of(
                                    new Entry("map.nameMinScale", X),
                                    new Entry("map.bannerMinScale", X),
                                    new Entry("map.politicalMaxScale", X),
                                    new Entry("map.showCalibrationArrows", X))))),
                    new Page("ottoextra.cat.map.tab.minimap", List.of(
                            new Section("ottoextra.cat.map.sec.minimap", List.of(
                                    new Entry("map.minimapBorders", B),
                                    new Entry("map.minimapPainted", B),
                                    new Entry("map.minimapPolitical", B),
                                    new Entry("resources.showMinimapStatusIndicator", M))),
                            new Section("ottoextra.cat.map.sec.banner", List.of(
                                    new Entry("map.minimapBanner", B),
                                    new Entry("map.minimapBannerShowName", B),
                                    new Entry("map.minimapBannerShowState", B),
                                    new Entry("map.minimapBannerShowFaction", B),
                                    new Entry("map.bannerEdit", B),
                                    new Entry("map.minimapLiegeTop", M))),
                            new Section("ottoextra.cat.map.sec.bannerfine", List.of(
                                    new Entry("map.nameHudColor", M),
                                    new Entry("map.stateHudColor", M),
                                    new Entry("map.factionHudColor", M),
                                    new Entry("map.nameHudScale", X),
                                    new Entry("map.stateHudScale", X),
                                    new Entry("map.factionHudScale", X),
                                    new Entry("map.nameHudWidth", X),
                                    new Entry("map.stateHudWidth", X),
                                    new Entry("map.factionHudWidth", X),
                                    new Entry("map.nameHudHeight", X),
                                    new Entry("map.stateHudHeight", X),
                                    new Entry("map.factionHudHeight", X))))))),
            new Category("regions", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.regions.sec.toast", List.of(
                                    new Entry("regions.enabled", B),
                                    new Entry("regions.playEnterSound", B),
                                    new Entry("regions.preview", B),
                                    new Entry("regions.hideOriginalActionbar", M),
                                    new Entry("regions.displayDurationMs", M))),
                            new Section("ottoextra.cat.regions.sec.content", List.of(
                                    new Entry("regions.showBanner", B),
                                    new Entry("regions.showFaction", B),
                                    new Entry("regions.showLeader", B),
                                    new Entry("regions.showLeaderRpName", B),
                                    new Entry("regions.showCoordinates", B),
                                    new Entry("regions.hintTextEnabled", M))),
                            new Section("ottoextra.cat.regions.sec.look", List.of(
                                    new Entry("regions.zoneBannerStyle", B),
                                    new Entry("regions.overlayPosition", B),
                                    new Entry("regions.themes", B))),
                            new Section("ottoextra.cat.regions.sec.sizes", List.of(
                                    new Entry("regions.baseTextScale", X),
                                    new Entry("regions.titleScale", X),
                                    new Entry("regions.regionScale", X),
                                    new Entry("regions.hierarchyScale", X),
                                    new Entry("regions.hintScale", X),
                                    new Entry("regions.maxTextWidth", X),
                                    new Entry("regions.minToastWidth", X),
                                    new Entry("regions.maxToastWidth", X),
                                    new Entry("regions.screenTopMargin", X),
                                    new Entry("regions.iconSize", X),
                                    new Entry("regions.iconGap", X),
                                    new Entry("regions.paddingLeft", X),
                                    new Entry("regions.paddingRight", X),
                                    new Entry("regions.paddingTop", X),
                                    new Entry("regions.paddingBottom", X))),
                            new Section("ottoextra.cat.regions.sec.activity", List.of(
                                    new Entry("regions.activityHudEnabled", B),
                                    new Entry("regions.activityHudShowClock", B),
                                    new Entry("regions.activityHudShowLastUpdated", B),
                                    new Entry("regions.activityHudMaxEntries", M),
                                    new Entry("regions.activityHudPosition", M),
                                    new Entry("regions.activityHudScale", M),
                                    new Entry("regions.activityHudEdit", M),
                                    new Entry("regions.activityHudOffsetX", X),
                                    new Entry("regions.activityHudOffsetY", X))),
                            new Section("ottoextra.cat.regions.sec.data", List.of(
                                    new Entry("regions.refreshData", B))))))),
            new Category("letters", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.letters.sec.editor", List.of(
                                    new Entry("letter.enabled", B),
                                    new Entry("letter.open", B),
                                    new Entry("letter.previewEnabled", B),
                                    new Entry("letter.openRecipientAfterWrite", B))),
                            new Section("ottoextra.cat.letters.sec.format", List.of(
                                    new Entry("letter.formattingEnabled", B),
                                    new Entry("letter.formattingSidebarVisible", B),
                                    new Entry("letter.signature", B),
                                    new Entry("letter.formattingConvertAmpersandOnPaste", M))),
                            new Section("ottoextra.cat.letters.sec.archive", List.of(
                                    new Entry("letter.exportSubfoldersByPlayer", M))),
                            new Section("ottoextra.cat.letters.sec.announce", List.of(
                                    new Entry("letter.announcementAutoOptimizeEnabled", M),
                                    new Entry("letter.announcementSafeLinesPerPage", X),
                                    new Entry("letter.announcementSafeCharsPerLine", X))),
                            new Section("ottoextra.cat.letters.sec.send", List.of(
                                    new Entry("letter.sendMode", X),
                                    new Entry("letter.triggerItemName", X),
                                    new Entry("letter.letterCommand", X),
                                    new Entry("letter.postCommand", X),
                                    new Entry("letter.announcementSubmitCommand", X),
                                    new Entry("letter.letterSendDelayMinMs", X),
                                    new Entry("letter.letterSendDelayMaxMs", X),
                                    new Entry("letter.pageModeSendDelayMs", X))))))),
            new Category("animals", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.animals.sec.info", List.of(
                                    new Entry("animalInfo.enabled", B),
                                    new Entry("animalInfo.toastStyle", B),
                                    new Entry("animalInfo.layout", M),
                                    new Entry("animalInfo.hideAutomaticChat", M))),
                            new Section("ottoextra.cat.animals.sec.nametags", List.of(
                                    new Entry("animalInfo.nametagShowGender", B),
                                    new Entry("animalInfo.nametagGenderColor", B),
                                    new Entry("animalInfo.nametagIncludeUnregistered", M))),
                            new Section("ottoextra.cat.animals.sec.glow", List.of(
                                    new Entry("animalInfo.genderGlowEnabled", B),
                                    new Entry("animalInfo.breedingGlowEnabled", B),
                                    new Entry("animalInfo.shearGlowEnabled", B))),
                            new Section("ottoextra.cat.animals.sec.breeding", List.of(
                                    new Entry("animalInfo.breederModeEnabled", B))),
                            new Section("ottoextra.cat.animals.sec.overview", List.of(
                                    new Entry("animalInfo.overviewShowCows", M),
                                    new Entry("animalInfo.overviewShowChickens", M),
                                    new Entry("animalInfo.overviewShowPigs", M),
                                    new Entry("animalInfo.overviewShowGoats", M),
                                    new Entry("animalInfo.overviewShowSheep", M))),
                            new Section("ottoextra.cat.animals.sec.farmer", List.of(
                                    new Entry("farmer.feedAssistant", B),
                                    new Entry("farmer.plantingAssistant", B),
                                    new Entry("farmer.plantingSerpentine", M),
                                    new Entry("farmer.feedback", M))),
                            new Section("ottoextra.cat.animals.sec.cache", List.of(
                                    new Entry("animalInfo.rememberAnimalIdentities", M),
                                    new Entry("animalInfo.forgetAnimals", M))),
                            new Section("ottoextra.cat.animals.sec.query", List.of(
                                    new Entry("animalInfo.hoverDelayMs", X),
                                    new Entry("animalInfo.cacheDurationMs", X),
                                    new Entry("animalInfo.responseTimeoutMs", X))))))),
            new Category("combat", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.combat.sec.hud", List.of(
                                    new Entry("tweaks.combatModeHud", B))),
                            new Section("ottoextra.cat.combat.sec.lowhealth", List.of(
                                    new Entry("tweaks.lowHealth.enabled", B),
                                    new Entry("tweaks.lowHealth.reducedMotion", B),
                                    new Entry("tweaks.lowHealth.intensityScale", M),
                                    new Entry("tweaks.lowHealth.vignetteEnabled", M),
                                    new Entry("tweaks.lowHealth.heartbeatEnabled", M),
                                    new Entry("tweaks.lowHealth.blurEnabled", M),
                                    new Entry("tweaks.lowHealth.fovEnabled", M),
                                    new Entry("tweaks.lowHealth.calmAfterNoDamage", M),
                                    new Entry("tweaks.lowHealth.startHealth", X),
                                    new Entry("tweaks.lowHealth.blurStartHearts", X),
                                    new Entry("tweaks.lowHealth.blurStrength", X),
                                    new Entry("tweaks.lowHealth.fovMaxDegrees", X))),
                            new Section("ottoextra.cat.combat.sec.tactical", List.of(
                                    new Entry("tactical.combatMode", B),
                                    new Entry("tactical.showPlayerNames", B),
                                    new Entry("tactical.politicalColors", B),
                                    new Entry("tactical.openBook", B),
                                    new Entry("tactical.retaliationMarkers", M),
                                    new Entry("tactical.preventFriendlyFire", M),
                                    new Entry("tactical.protectNeutralPlayers", M),
                                    new Entry("tactical.allyColor", M),
                                    new Entry("tactical.hostileColor", M))))))),
            new Category("craft", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.craft.sec.hud", List.of(
                                    new Entry("tweaks.craftingHud", B),
                                    new Entry("tweaks.craftingHudSounds", B))),
                            new Section("ottoextra.cat.craft.sec.tools", List.of(
                                    new Entry("tweaks.toolProtect.enabled", B),
                                    new Entry("tweaks.toolProtect.warnBelowPercent", M),
                                    new Entry("tweaks.toolProtect.blockAtUses", M))),
                            new Section("ottoextra.cat.craft.sec.resources", List.of(
                                    new Entry("resources.enabled", B),
                                    new Entry("resources.hintsEnabled", B),
                                    new Entry("resources.nearbyRadius", X),
                                    new Entry("resources.cacheSeconds", X))),
                            new Section("ottoextra.cat.craft.sec.litematica", List.of(
                                    new Entry("tweaks.litematica.localizedBlockNames", B),
                                    new Entry("tweaks.litematica.fachwerkAutoTune", M))))))),
            new Category("privacy", List.of(
                    new Page(null, List.of(
                            new Section("ottoextra.cat.privacy.sec.consent", List.of(
                                    new Entry("consent.onlineServices", B),
                                    new Entry("consent.automaticResourcePackDownloads", B),
                                    new Entry("consent.externalImageDownloads", B),
                                    new Entry("consent.updateChecks", B),
                                    new Entry("consent.communityRpNameUploads", B))),
                            new Section("ottoextra.cat.privacy.sec.chatlog", List.of(
                                    new Entry("tweaks.chatLog", B),
                                    new Entry("tweaks.chatLogScope", B))))))));

    private SettingsLayout() {
    }
}
