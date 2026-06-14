package de.ottoextra.config.settings;

import de.ottoextra.config.OttoExtraConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Deklarative Options-Registry fürs Settings-GUI:
 * jede Option ist ein Deskriptor (Modul → Tab → Card → Option) mit Typ,
 * Getter/Setter, Grenzen und Hilfetext-Key. Das GUI rendert generisch,
 * die globale Suche filtert über Label, Key, Beschreibung und Tags.
 */
public final class SettingsRegistry {

    public enum Type { BOOL, INT, FLOAT, COLOR, STRING, COMMAND, ACTION, CYCLE }

    /** Eine Option (typisiert, validierbar). */
    public static final class Option {
        public final String labelKey;
        public final String tooltipKey;     // null = kein ?-Tooltip
        public final Type type;
        public final String configKey;      // z. B. "map.paintedMap" (Suche)
        public final Supplier<String> get;
        public final Consumer<String> set;  // validierter Wert als String
        public final double min;
        public final double max;
        public final Runnable action;       // nur ACTION
        public final String[] cycleValues;  // nur CYCLE
        public final boolean dangerous;

        private Option(String labelKey, String tooltipKey, Type type, String configKey,
                       Supplier<String> get, Consumer<String> set, double min, double max,
                       Runnable action, String[] cycleValues, boolean dangerous) {
            this.labelKey = labelKey;
            this.tooltipKey = tooltipKey;
            this.type = type;
            this.configKey = configKey;
            this.get = get;
            this.set = set;
            this.min = min;
            this.max = max;
            this.action = action;
            this.cycleValues = cycleValues;
            this.dangerous = dangerous;
        }

        public static Option bool(String labelKey, String configKey,
                                  Supplier<Boolean> get, Consumer<Boolean> set) {
            return new Option(labelKey, null, Type.BOOL, configKey,
                    () -> String.valueOf(get.get()), v -> set.accept(Boolean.parseBoolean(v)),
                    0, 0, null, null, false);
        }

        public Option tooltip(String key) {
            return new Option(labelKey, key, type, configKey, get, set, min, max,
                    action, cycleValues, dangerous);
        }

        public Option danger() {
            return new Option(labelKey, tooltipKey, type, configKey, get, set, min, max,
                    action, cycleValues, true);
        }

        public static Option intVal(String labelKey, String configKey,
                                    Supplier<Integer> get, Consumer<Integer> set,
                                    int min, int max) {
            return new Option(labelKey, null, Type.INT, configKey,
                    () -> String.valueOf(get.get()), v -> {
                int parsed = Integer.parseInt(v.trim());
                set.accept((int) Math.max(min, Math.min(max, parsed)));
            }, min, max, null, null, false);
        }

        public static Option floatVal(String labelKey, String configKey,
                                      Supplier<Float> get, Consumer<Float> set,
                                      double min, double max) {
            return new Option(labelKey, null, Type.FLOAT, configKey,
                    () -> String.valueOf(get.get()), v -> {
                float parsed = Float.parseFloat(v.trim().replace(',', '.'));
                set.accept((float) Math.max(min, Math.min(max, parsed)));
            }, min, max, null, null, false);
        }

        public static Option doubleVal(String labelKey, String configKey,
                                       Supplier<Double> get, Consumer<Double> set,
                                       double min, double max) {
            return new Option(labelKey, null, Type.FLOAT, configKey,
                    () -> String.valueOf(get.get()), v -> {
                double parsed = Double.parseDouble(v.trim().replace(',', '.'));
                set.accept(Math.max(min, Math.min(max, parsed)));
            }, min, max, null, null, false);
        }

        public static Option color(String labelKey, String configKey,
                                   Supplier<String> get, Consumer<String> set) {
            return new Option(labelKey, null, Type.COLOR, configKey, get, v -> {
                String s = v.trim().replace("#", "");
                if (s.matches("[0-9a-fA-F]{6}")) {
                    set.accept("#" + s.toUpperCase(Locale.ROOT));
                } else if (s.isEmpty()) {
                    set.accept("");
                } else {
                    throw new IllegalArgumentException("Hex #RRGGBB erwartet");
                }
            }, 0, 0, null, null, false);
        }

        public static Option string(String labelKey, String configKey,
                                    Supplier<String> get, Consumer<String> set) {
            return new Option(labelKey, null, Type.STRING, configKey, get,
                    v -> set.accept(v.trim()), 0, 0, null, null, false);
        }

        /** Command: ohne Slash gespeichert, GUI zeigt mit Slash an. */
        public static Option command(String labelKey, String configKey,
                                     Supplier<String> get, Consumer<String> set) {
            return new Option(labelKey, null, Type.COMMAND, configKey, get, v -> {
                String s = v.trim();
                set.accept(s.startsWith("/") ? s.substring(1) : s);
            }, 0, 0, null, null, false);
        }

        public static Option action(String labelKey, String configKey, Runnable run) {
            return new Option(labelKey, null, Type.ACTION, configKey,
                    () -> "", v -> { }, 0, 0, run, null, false);
        }

        public static Option cycle(String labelKey, String configKey,
                                   Supplier<String> get, Consumer<String> set,
                                   String... values) {
            return new Option(labelKey, null, Type.CYCLE, configKey, get,
                    v -> set.accept(v), 0, 0, null, values, false);
        }
    }

    /** Card = Sektion mit Titel + Erklärung + Optionen. */
    public record Card(String titleKey, String descKey, List<Option> options) {
    }

    /** Tab innerhalb eines Moduls (Basis/Darstellung/Erweitert...). */
    public record Tab(String titleKey, List<Card> cards) {
    }

    /** Modul = Sidebar-Eintrag mit Kopf-Erklärung. */
    public record ModulePage(String id, String titleKey, String descKey, List<Tab> tabs) {
    }

    private final List<ModulePage> modules = new ArrayList<>();

    public List<ModulePage> modules() {
        return modules;
    }

    public ModulePage module(String id, String titleKey, String descKey) {
        ModulePage m = new ModulePage(id, titleKey, descKey, new ArrayList<>());
        modules.add(m);
        return m;
    }

    public static Tab tab(ModulePage module, String titleKey) {
        Tab t = new Tab(titleKey, new ArrayList<>());
        module.tabs().add(t);
        return t;
    }

    public static Card card(Tab tab, String titleKey, String descKey, Option... options) {
        Card c = new Card(titleKey, descKey, new ArrayList<>(List.of(options)));
        tab.cards().add(c);
        return c;
    }

    /** Suchtreffer: Modul + Tab-Index + Card + Option. */
    public record SearchHit(ModulePage module, int tabIndex, Card card, Option option) {
    }

    public List<SearchHit> search(String query, java.util.function.Function<String, String> translate) {
        List<SearchHit> hits = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            return hits;
        }
        for (ModulePage m : modules) {
            for (int t = 0; t < m.tabs().size(); t++) {
                for (Card c : m.tabs().get(t).cards()) {
                    for (Option o : c.options()) {
                        String hay = (translate.apply(o.labelKey) + " " + o.configKey + " "
                                + translate.apply(c.titleKey) + " "
                                + translate.apply(m.titleKey)).toLowerCase(Locale.ROOT);
                        if (hay.contains(q)) {
                            hits.add(new SearchHit(m, t, c, o));
                        }
                    }
                }
            }
        }
        return hits;
    }

    // ---- Komplettaufbau aus der Config --------------------------------

    public static SettingsRegistry build(OttoExtraConfig c, Runnable openPeopleBook,
                                         Runnable openGroupColors, Runnable openBannerEdit,
                                         Runnable openLetterEditor, Runnable openRegionThemes) {
        SettingsRegistry r = new SettingsRegistry();

        // Resourcepack
        var rp = r.module("resourcepack", "ottoextra.module.resourcepack", "ottoextra.set.rp.desc");
        var rpBase = tab(rp, "ottoextra.set.tab.base");
        card(rpBase, "ottoextra.set.rp.card", "ottoextra.set.rp.card.desc",
                Option.bool("ottoextra.config.rp.enabled", "resourcepack.enabled",
                        () -> c.resourcepack.enabled, v -> c.resourcepack.enabled = v),
                Option.bool("ottoextra.config.rp.autoEnable", "resourcepack.autoEnable",
                        () -> c.resourcepack.autoEnable, v -> c.resourcepack.autoEnable = v),
                Option.bool("ottoextra.config.rp.checkOnStartup", "resourcepack.checkOnStartup",
                        () -> c.resourcepack.checkOnStartup, v -> c.resourcepack.checkOnStartup = v),
                Option.bool("ottoextra.config.rp.respectUserDisable", "resourcepack.respectUserDisable",
                        () -> c.resourcepack.respectUserDisable, v -> c.resourcepack.respectUserDisable = v)
                        .tooltip("ottoextra.set.rp.respect.tip"));
        var rpAdv = tab(rp, "ottoextra.set.tab.advanced");
        card(rpAdv, "ottoextra.set.rp.net", "ottoextra.set.rp.net.desc",
                Option.string("ottoextra.adv.manifestUrl", "resourcepack.manifestUrl",
                        () -> c.resourcepack.manifestUrl, v -> c.resourcepack.manifestUrl = v),
                Option.string("ottoextra.adv.assetName", "resourcepack.assetName",
                        () -> c.resourcepack.assetName, v -> c.resourcepack.assetName = v),
                Option.intVal("ottoextra.adv.connectTimeoutMs", "resourcepack.connectTimeoutMs",
                        () -> c.resourcepack.connectTimeoutMs, v -> c.resourcepack.connectTimeoutMs = v,
                        1000, 120000),
                Option.intVal("ottoextra.adv.requestTimeoutMs", "resourcepack.requestTimeoutMs",
                        () -> c.resourcepack.requestTimeoutMs, v -> c.resourcepack.requestTimeoutMs = v,
                        1000, 300000));

        // Chat
        var chat = r.module("chat", "ottoextra.module.chat", "ottoextra.set.chat.desc");
        var chatBase = tab(chat, "ottoextra.set.tab.base");
        card(chatBase, "ottoextra.set.chat.channel", "ottoextra.set.chat.channel.desc",
                Option.bool("ottoextra.config.module.chat", "chat.enabled",
                        () -> c.chat.enabled, v -> c.chat.enabled = v),
                Option.bool("ottoextra.config.chat.autoSprechen", "chat.autoSprechenOnJoin",
                        () -> c.chat.autoSprechenOnJoin, v -> c.chat.autoSprechenOnJoin = v)
                        .tooltip("ottoextra.set.chat.autoSprechen.tip"),
                Option.bool("ottoextra.config.chat.shiftTab", "chat.shiftTabCycleChannels",
                        () -> c.chat.shiftTabCycleChannels, v -> c.chat.shiftTabCycleChannels = v)
                        .tooltip("ottoextra.set.chat.shiftTab.tip"));
        card(chatBase, "ottoextra.set.chat.bang", "ottoextra.set.chat.bang.desc",
                Option.bool("ottoextra.config.chat.bang", "chat.offtopicBangEnabled",
                        () -> c.chat.offtopicBangEnabled, v -> c.chat.offtopicBangEnabled = v),
                Option.intVal("ottoextra.config.chat.bangCount", "chat.offtopicBangCount",
                        () -> c.chat.offtopicBangCount, v -> c.chat.offtopicBangCount = v, 1, 3));
        card(chatBase, "ottoextra.set.chat.longchat", "ottoextra.set.chat.longchat.desc",
                Option.bool("ottoextra.config.chat.longchat", "chat.longChatEnabled",
                        () -> c.chat.longChatEnabled, v -> c.chat.longChatEnabled = v)
                        .tooltip("ottoextra.set.chat.longchat.tip"),
                Option.intVal("ottoextra.config.chat.longchatChunk", "chat.longChatChunk",
                        () -> c.chat.longChatChunk, v -> c.chat.longChatChunk = v, 64, 256),
                Option.intVal("ottoextra.config.chat.longchatInput", "chat.longChatMaxInput",
                        () -> c.chat.longChatMaxInput, v -> c.chat.longChatMaxInput = v, 256, 32500),
                Option.intVal("ottoextra.config.chat.longchatDelay", "chat.longChatDelayTicks",
                        () -> c.chat.longChatDelayTicks, v -> c.chat.longChatDelayTicks = v, 1, 40),
                Option.string("ottoextra.config.chat.longchatMarker", "chat.longChatMarker",
                        () -> c.chat.longChatMarker, v -> c.chat.longChatMarker = v));

        // Regionen
        var reg = r.module("regions", "ottoextra.module.regions", "ottoextra.set.regions.desc");
        var regBase = tab(reg, "ottoextra.set.tab.base");
        card(regBase, "ottoextra.set.regions.toast", "ottoextra.set.regions.toast.desc",
                Option.bool("ottoextra.config.regions.enabled", "regions.enabled",
                        () -> c.regions.enabled, v -> c.regions.enabled = v),
                Option.bool("ottoextra.config.regions.hideActionbar", "regions.hideOriginalActionbar",
                        () -> c.regions.hideOriginalActionbar, v -> c.regions.hideOriginalActionbar = v),
                Option.bool("ottoextra.config.regions.sound", "regions.playEnterSound",
                        () -> c.regions.playEnterSound, v -> c.regions.playEnterSound = v),
                Option.bool("ottoextra.config.regions.banner", "regions.showBanner",
                        () -> c.regions.showBanner, v -> c.regions.showBanner = v),
                Option.cycle("ottoextra.config.regions.position", "regions.overlayPosition",
                        () -> c.regions.overlayPosition, v -> c.regions.overlayPosition = v,
                        "TOP_CENTER", "TOP_RIGHT", "TOP_LEFT", "CENTER"),
                Option.action("ottoextra.config.regions.themes", "regions.themes", openRegionThemes),
                Option.action("ottoextra.config.preview", "regions.preview",
                        de.ottoextra.config.OttoExtraConfigScreen::triggerPreview));
        var regAdv = tab(reg, "ottoextra.set.tab.advanced");
        card(regAdv, "ottoextra.set.regions.badgeLayout", "ottoextra.set.regions.badgeLayout.desc",
                Option.bool("ottoextra.config.regions.hintText", "regions.hintTextEnabled",
                        () -> c.regions.hintTextEnabled, v -> c.regions.hintTextEnabled = v),
                Option.intVal("ottoextra.adv.maxTextWidth", "regions.maxTextWidth",
                        () -> c.regions.maxTextWidth, v -> c.regions.maxTextWidth = v, 50, 800),
                Option.intVal("ottoextra.adv.minToastWidth", "regions.minToastWidth",
                        () -> c.regions.minToastWidth, v -> c.regions.minToastWidth = v, 50, 800),
                Option.intVal("ottoextra.adv.maxToastWidth", "regions.maxToastWidth",
                        () -> c.regions.maxToastWidth, v -> c.regions.maxToastWidth = v, 50, 1000),
                Option.intVal("ottoextra.adv.screenTopMargin", "regions.screenTopMargin",
                        () -> c.regions.screenTopMargin, v -> c.regions.screenTopMargin = v, 0, 200),
                Option.intVal("ottoextra.adv.iconSize", "regions.iconSize",
                        () -> c.regions.iconSize, v -> c.regions.iconSize = v, 8, 64),
                Option.intVal("ottoextra.adv.iconGap", "regions.iconGap",
                        () -> c.regions.iconGap, v -> c.regions.iconGap = v, 0, 32),
                Option.intVal("ottoextra.adv.paddingLeft", "regions.paddingLeft",
                        () -> c.regions.paddingLeft, v -> c.regions.paddingLeft = v, 0, 40),
                Option.intVal("ottoextra.adv.paddingRight", "regions.paddingRight",
                        () -> c.regions.paddingRight, v -> c.regions.paddingRight = v, 0, 40),
                Option.intVal("ottoextra.adv.paddingTop", "regions.paddingTop",
                        () -> c.regions.paddingTop, v -> c.regions.paddingTop = v, 0, 40),
                Option.intVal("ottoextra.adv.paddingBottom", "regions.paddingBottom",
                        () -> c.regions.paddingBottom, v -> c.regions.paddingBottom = v, 0, 40),
                Option.action("ottoextra.config.preview", "regions.preview",
                        de.ottoextra.config.OttoExtraConfigScreen::triggerPreview));
        card(regAdv, "ottoextra.set.regions.badgeText", "ottoextra.set.regions.badgeText.desc",
                Option.floatVal("ottoextra.adv.baseTextScale", "regions.baseTextScale",
                        () -> c.regions.baseTextScale, v -> c.regions.baseTextScale = v, 0.2, 3.0),
                Option.floatVal("ottoextra.adv.titleScale", "regions.titleScale",
                        () -> c.regions.titleScale, v -> c.regions.titleScale = v, 0.2, 3.0),
                Option.floatVal("ottoextra.adv.regionScale", "regions.regionScale",
                        () -> c.regions.regionScale, v -> c.regions.regionScale = v, 0.2, 3.0),
                Option.floatVal("ottoextra.adv.hierarchyScale", "regions.hierarchyScale",
                        () -> c.regions.hierarchyScale, v -> c.regions.hierarchyScale = v, 0.2, 3.0),
                Option.floatVal("ottoextra.adv.hintScale", "regions.hintScale",
                        () -> c.regions.hintScale, v -> c.regions.hintScale = v, 0.1, 3.0));

        // Namensschilder
        var tags = r.module("nametags", "ottoextra.module.nametags", "ottoextra.set.tags.desc");
        var tagsBase = tab(tags, "ottoextra.set.tab.base");
        card(tagsBase, "ottoextra.set.tags.visibility", "ottoextra.set.tags.visibility.desc",
                Option.bool("ottoextra.config.module.nametags", "nametags.enabled",
                        () -> c.nametags.enabled, v -> c.nametags.enabled = v),
                Option.cycle("ottoextra.config.nametags.mode", "nametags.mode",
                        () -> c.nametags.mode.name(), v ->
                                c.nametags.mode = de.ottoextra.nametags.NameTagMode.valueOf(v),
                        "NORMAL", "REALISTIC", "HIDE_ALL"));
        card(tagsBase, "ottoextra.set.tags.lines", "ottoextra.set.tags.lines.desc",
                Option.bool("ottoextra.config.nametags.showTitle", "nametags.showTitle",
                        () -> c.nametags.showTitle, v -> c.nametags.showTitle = v),
                Option.bool("ottoextra.config.nametags.showRpName", "nametags.showRpName",
                        () -> c.nametags.showRpName, v -> c.nametags.showRpName = v),
                Option.bool("ottoextra.config.nametags.showPlayerName", "nametags.showPlayerName",
                        () -> c.nametags.showPlayerName, v -> c.nametags.showPlayerName = v));
        var tagsAdv = tab(tags, "ottoextra.set.tab.advanced");
        card(tagsAdv, "ottoextra.set.tags.scale", "ottoextra.set.tags.scale.desc",
                Option.floatVal("ottoextra.adv.tagTitleScale", "nametags.titleScale",
                        () -> c.nametags.titleScale, v -> c.nametags.titleScale = v, 0.4, 2.5),
                Option.floatVal("ottoextra.adv.tagNameScale", "nametags.nameScale",
                        () -> c.nametags.nameScale, v -> c.nametags.nameScale = v, 0.4, 2.5),
                Option.floatVal("ottoextra.adv.tagAccountScale", "nametags.accountScale",
                        () -> c.nametags.accountScale, v -> c.nametags.accountScale = v, 0.4, 2.5),
                Option.intVal("ottoextra.adv.tagLineSpacing", "nametags.lineSpacing",
                        () -> c.nametags.lineSpacing, v -> c.nametags.lineSpacing = v, 4, 30),
                Option.color("ottoextra.adv.tagAccountColor", "nametags.accountColor",
                        () -> c.nametags.accountColor, v -> c.nametags.accountColor = v));

        // Karte
        var map = r.module("map", "ottoextra.module.map", "ottoextra.set.map.desc");
        var mapBase = tab(map, "ottoextra.set.tab.base");
        card(mapBase, "ottoextra.set.map.visibility", "ottoextra.set.map.visibility.desc",
                Option.bool("ottoextra.config.module.map", "map.enabled",
                        () -> c.map.enabled, v -> c.map.enabled = v),
                Option.bool("ottoextra.config.map.borders", "map.showBorders",
                        () -> c.map.showBorders, v -> c.map.showBorders = v),
                Option.bool("ottoextra.config.map.names", "map.showNames",
                        () -> c.map.showNames, v -> c.map.showNames = v),
                Option.bool("ottoextra.config.map.banners", "map.showBanners",
                        () -> c.map.showBanners, v -> c.map.showBanners = v),
                Option.bool("ottoextra.config.map.painted", "map.paintedMap",
                        () -> c.map.paintedMap, v -> c.map.paintedMap = v),
                Option.bool("ottoextra.config.map.political", "map.politicalFill",
                        () -> c.map.politicalFill, v -> c.map.politicalFill = v),
                Option.bool("ottoextra.config.map.activity", "map.showActivity",
                        () -> c.map.showActivity, v -> c.map.showActivity = v),
                Option.bool("ottoextra.config.map.npcVillages", "map.showNpcVillages",
                        () -> c.map.showNpcVillages, v -> c.map.showNpcVillages = v),
                Option.bool("ottoextra.config.map.onlyOttonien", "map.onlyOnOttonien",
                        () -> c.map.onlyOnOttonien, v -> c.map.onlyOnOttonien = v)
                        .tooltip("ottoextra.set.map.onlyOttonien.tip"));
        var mapMini = tab(map, "ottoextra.set.tab.minimap");
        card(mapMini, "ottoextra.set.map.minimap", "ottoextra.set.map.minimap.desc",
                Option.bool("ottoextra.config.map.minimap", "map.minimapBorders",
                        () -> c.map.minimapBorders, v -> c.map.minimapBorders = v),
                Option.bool("ottoextra.config.map.minimapPainted", "map.minimapPainted",
                        () -> c.map.minimapPainted, v -> c.map.minimapPainted = v),
                Option.bool("ottoextra.config.map.minimapPolitical", "map.minimapPolitical",
                        () -> c.map.minimapPolitical, v -> c.map.minimapPolitical = v));
        card(mapMini, "ottoextra.set.map.hud", "ottoextra.set.map.hud.desc",
                Option.bool("ottoextra.config.map.minimapBanner", "map.minimapBanner",
                        () -> c.map.minimapBanner, v -> c.map.minimapBanner = v),
                Option.bool("ottoextra.config.map.bannerName", "map.minimapBannerShowName",
                        () -> c.map.minimapBannerShowName, v -> c.map.minimapBannerShowName = v),
                Option.bool("ottoextra.config.map.bannerState", "map.minimapBannerShowState",
                        () -> c.map.minimapBannerShowState, v -> c.map.minimapBannerShowState = v),
                Option.bool("ottoextra.config.map.bannerFaction", "map.minimapBannerShowFaction",
                        () -> c.map.minimapBannerShowFaction, v -> c.map.minimapBannerShowFaction = v)
                        .tooltip("ottoextra.set.map.bannerFaction.tip"),
                Option.action("ottoextra.config.map.bannerEdit", "map.bannerEdit", openBannerEdit),
                Option.action("ottoextra.config.map.groupColors", "map.groupColors", openGroupColors));
        var mapAdv = tab(map, "ottoextra.set.tab.advanced");
        card(mapAdv, "ottoextra.set.map.style", "ottoextra.set.map.style.desc",
                Option.color("ottoextra.adv.borderColor", "map.borderColor",
                        () -> c.map.borderColor, v -> c.map.borderColor = v),
                Option.bool("ottoextra.config.map.dashed", "map.dashedBorders",
                        () -> c.map.dashedBorders, v -> c.map.dashedBorders = v),
                Option.doubleVal("ottoextra.adv.nameMinScale", "map.nameMinScale",
                        () -> c.map.nameMinScale, v -> c.map.nameMinScale = v, 0.005, 4),
                Option.doubleVal("ottoextra.adv.bannerMinScale", "map.bannerMinScale",
                        () -> c.map.bannerMinScale, v -> c.map.bannerMinScale = v, 0.005, 8),
                Option.doubleVal("ottoextra.adv.politicalMaxScale", "map.politicalMaxScale",
                        () -> c.map.politicalMaxScale, v -> c.map.politicalMaxScale = v, 0.01, 8),
                Option.bool("ottoextra.config.map.calibrationArrows", "map.showCalibrationArrows",
                        () -> c.map.showCalibrationArrows, v -> c.map.showCalibrationArrows = v)
                        .tooltip("ottoextra.set.map.calibrationArrows.tip"));
        card(mapAdv, "ottoextra.set.map.hudAdv", "ottoextra.set.map.hudAdv.desc",
                Option.intVal("ottoextra.adv.nameHudWidth", "map.nameHudWidth",
                        () -> c.map.nameHudWidth, v -> c.map.nameHudWidth = v, 20, 400),
                Option.intVal("ottoextra.adv.stateHudWidth", "map.stateHudWidth",
                        () -> c.map.stateHudWidth, v -> c.map.stateHudWidth = v, 20, 400),
                Option.floatVal("ottoextra.adv.nameHudScale", "map.nameHudScale",
                        () -> c.map.nameHudScale, v -> c.map.nameHudScale = v, 0.5, 3),
                Option.floatVal("ottoextra.adv.stateHudScale", "map.stateHudScale",
                        () -> c.map.stateHudScale, v -> c.map.stateHudScale = v, 0.5, 3),
                Option.color("ottoextra.adv.nameHudColor", "map.nameHudColor",
                        () -> c.map.nameHudColor, v -> c.map.nameHudColor = v),
                Option.color("ottoextra.adv.stateHudColor", "map.stateHudColor",
                        () -> c.map.stateHudColor, v -> c.map.stateHudColor = v),
                Option.intVal("ottoextra.adv.factionHudWidth", "map.factionHudWidth",
                        () -> c.map.factionHudWidth, v -> c.map.factionHudWidth = v, 20, 400),
                Option.floatVal("ottoextra.adv.factionHudScale", "map.factionHudScale",
                        () -> c.map.factionHudScale, v -> c.map.factionHudScale = v, 0.5, 3),
                Option.color("ottoextra.adv.factionHudColor", "map.factionHudColor",
                        () -> c.map.factionHudColor, v -> c.map.factionHudColor = v));

        // RP-Namen
        var rpn = r.module("rpnames", "ottoextra.module.rpnames", "ottoextra.set.rpn.desc");
        var rpnBase = tab(rpn, "ottoextra.set.tab.base");
        card(rpnBase, "ottoextra.set.rpn.people", "ottoextra.set.rpn.people.desc",
                Option.action("ottoextra.config.rpnames.people", "rpnames.people", openPeopleBook));
        card(rpnBase, "ottoextra.set.rpn.chat", "ottoextra.set.rpn.chat.desc",
                Option.bool("ottoextra.config.module.rpnames", "rpnames.enabled",
                        () -> c.rpnames.enabled, v -> c.rpnames.enabled = v),
                Option.bool("ottoextra.config.rpnames.showInOoc", "rpnames.showInOocChats",
                        () -> c.rpnames.showInOfftopic && c.rpnames.showInHilfe,
                        v -> {
                            c.rpnames.showInOfftopic = v;
                            c.rpnames.showInHilfe = v;
                        })
                        .tooltip("ottoextra.set.rpn.ooc.tip"),
                Option.bool("ottoextra.config.rpnames.unknown", "rpnames.showUnknownAsUnknown",
                        () -> c.rpnames.showUnknownAsUnknown, v -> c.rpnames.showUnknownAsUnknown = v),
                Option.bool("ottoextra.config.rpnames.unknownAccount", "rpnames.unknownShowAccount",
                        () -> c.rpnames.unknownShowAccount, v -> c.rpnames.unknownShowAccount = v),
                Option.cycle("ottoextra.config.rpnames.unknownPlaceholder", "rpnames.unknownPlaceholder",
                        () -> c.rpnames.unknownPlaceholder, v -> c.rpnames.unknownPlaceholder = v,
                        "Unbekannt", "???"));
        card(rpnBase, "ottoextra.set.rpn.click", "ottoextra.set.rpn.click.desc",
                Option.bool("ottoextra.config.rpnames.openBookOnClick", "rpnames.openBookOnClick",
                        () -> c.rpnames.openBookOnClick, v -> c.rpnames.openBookOnClick = v)
                        .tooltip("ottoextra.set.rpn.click.tip"),
                Option.bool("ottoextra.config.rpnames.proactiveMeet", "rpnames.proactiveMeet",
                        () -> c.rpnames.proactiveMeet, v -> c.rpnames.proactiveMeet = v)
                        .tooltip("ottoextra.set.rpn.meet.tip"));
        card(rpnBase, "ottoextra.set.rpn.marker", "ottoextra.set.rpn.marker.desc",
                Option.bool("ottoextra.config.rpnames.markerGlow", "rpnames.meetMarkerGlow",
                        () -> c.rpnames.meetMarkerGlow, v -> c.rpnames.meetMarkerGlow = v)
                        .tooltip("ottoextra.set.rpn.marker.glow.tip"),
                Option.doubleVal("ottoextra.config.rpnames.markerHeight", "rpnames.meetMarkerHeight",
                        () -> c.rpnames.meetMarkerHeight, v -> c.rpnames.meetMarkerHeight = v,
                        -1.0, 3.0).tooltip("ottoextra.set.rpn.marker.height.tip"),
                Option.doubleVal("ottoextra.config.rpnames.markerSize", "rpnames.meetMarkerSize",
                        () -> c.rpnames.meetMarkerSize, v -> c.rpnames.meetMarkerSize = v,
                        0.1, 5.0).tooltip("ottoextra.set.rpn.marker.size.tip"),
                Option.doubleVal("ottoextra.config.rpnames.markerSpin", "rpnames.meetMarkerSpinSpeed",
                        () -> c.rpnames.meetMarkerSpinSpeed, v -> c.rpnames.meetMarkerSpinSpeed = v,
                        0.0, 10.0).tooltip("ottoextra.set.rpn.marker.spin.tip"));
        card(rpnBase, "ottoextra.set.rpn.tablist", "ottoextra.set.rpn.tablist.desc",
                Option.bool("ottoextra.config.rpnames.tablist", "rpnames.tablistEnabled",
                        () -> c.rpnames.tablistEnabled, v -> c.rpnames.tablistEnabled = v),
                Option.bool("ottoextra.config.rpnames.tablistTitles", "rpnames.tablistTitlesAlways",
                        () -> c.rpnames.tablistTitlesAlways, v -> c.rpnames.tablistTitlesAlways = v)
                        .tooltip("ottoextra.set.rpn.tablistTitles.tip"));

        // Brief
        var letter = r.module("letter", "ottoextra.module.letter", "ottoextra.set.letter.desc");
        var letterBase = tab(letter, "ottoextra.set.tab.base");
        card(letterBase, "ottoextra.set.letter.editor", "ottoextra.set.letter.editor.desc",
                Option.bool("ottoextra.config.module.letter", "letter.enabled",
                        () -> c.letter.enabled, v -> c.letter.enabled = v),
                Option.string("ottoextra.set.letter.triggerItem", "letter.triggerItemName",
                        () -> c.letter.triggerItemName, v -> c.letter.triggerItemName = v)
                        .tooltip("ottoextra.set.letter.triggerItem.tip"),
                Option.action("ottoextra.config.letter.open", "letter.open", openLetterEditor));
        var letterAdv = tab(letter, "ottoextra.set.tab.advanced");
        card(letterAdv, "ottoextra.set.letter.commands", "ottoextra.set.letter.commands.desc",
                Option.command("ottoextra.set.letter.letterCommand", "letter.letterCommand",
                        () -> c.letter.letterCommand, v -> c.letter.letterCommand = v),
                Option.command("ottoextra.set.letter.postCommand", "letter.postCommand",
                        () -> c.letter.postCommand, v -> c.letter.postCommand = v),
                Option.command("ottoextra.set.letter.submitCommand", "letter.announcementSubmitCommand",
                        () -> c.letter.announcementSubmitCommand,
                        v -> c.letter.announcementSubmitCommand = v)
                        .tooltip("ottoextra.set.letter.submitCommand.tip"));
        card(letterAdv, "ottoextra.set.letter.layout", "ottoextra.set.letter.layout.desc",
                Option.intVal("ottoextra.set.letter.safeLines", "letter.announcementSafeLinesPerPage",
                        () -> c.letter.announcementSafeLinesPerPage,
                        v -> c.letter.announcementSafeLinesPerPage = v, 4, 12),
                Option.intVal("ottoextra.set.letter.safeChars", "letter.announcementSafeCharsPerLine",
                        () -> c.letter.announcementSafeCharsPerLine,
                        v -> c.letter.announcementSafeCharsPerLine = v, 8, 18),
                Option.intVal("ottoextra.set.letter.delayMin", "letter.letterSendDelayMinMs",
                        () -> c.letter.letterSendDelayMinMs,
                        v -> c.letter.letterSendDelayMinMs = v, 500, 10000),
                Option.intVal("ottoextra.set.letter.delayMax", "letter.letterSendDelayMaxMs",
                        () -> c.letter.letterSendDelayMaxMs,
                        v -> c.letter.letterSendDelayMaxMs = v, 500, 10000));

        return r;
    }
}
