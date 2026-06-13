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

/**
 * Zentrale, getypte Konfiguration von OttoExtra.
 *
 * <p>Spiegelt {@code config/ottoextra/ottoextra.json}.
 * Bewusst <b>ohne</b> KI-Felder (apiKey, apiProvider, model, temperature, ...) —
 * diese sind laut no-ai-text-helper-policy verboten.</p>
 *
 * <p>Lesen ist tolerant (fehlende Sektionen werden mit Defaults aufgefüllt),
 * Schreiben ist streng und atomar (Temp-Datei + Move).</p>
 */
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

    // ---- Sektionen -------------------------------------------------------

    public static final class Api {
        public boolean enabled = true;
        public String baseUrl = "https://regions.skyfuller.de/";
        public int connectTimeoutMs = 10_000;
        public int requestTimeoutMs = 20_000;
        public int syncIntervalSeconds = 1_800;
        public int playerDirectoryIntervalSeconds = 300;
        /**
         * v2-Auth (Mojang-Handshake + Bearer-Token) nutzen; bei nicht
         * erreichbarem /v2 fällt der Client automatisch auf die alten
         * public-*-Routen zurück.
         */
        public boolean useV2Auth = true;
        /**
         * Antworten ohne gültige Ed25519-Signatur verwerfen. Default false,
         * bis der Server-Rollout abgeschlossen ist.
         */
        public boolean requireSignatures = false;
        /** SPKI-Pinning — greift nur, wenn Pins einkompiliert sind. */
        public boolean tlsPinning = true;
    }

    public static final class Map {
        public boolean enabled = true;
        public boolean showBorders = true;
        public boolean showNames = true;
        public boolean showBanners = true;
        /** Gemalte Ottonien-Karte über unerkundetem Terrain (Worldmap). */
        public boolean paintedMap = true;
        /** Politisches Overlay: Lehen-Flächen nach Gefolge eingefärbt (Klick = fokussieren). */
        public boolean politicalFill = true;
        /** Manueller Versatz der gemalten Karte in Blöcken (Pfeil-Buttons auf der Worldmap). */
        public int paintedMapOffsetX = 0;
        public int paintedMapOffsetZ = 0;
        /** Kalibrier-Pfeile (Karte verschieben) auf der Worldmap einblenden (Debug). */
        public boolean showCalibrationArrows = false;
        /** Spieler-Aktivität (pulsierender Ring bei Versammlungen, Quelle: player_gathering). */
        public boolean showActivity = true;
        /** Overlay nur auf dem Ottonien-Server (false = auf allen Servern/Welten). */
        public boolean onlyOnOttonien = true;
        /** Grenzlinien auch auf der Minimap. */
        public boolean minimapBorders = true;
        /** Politische Flächen auch auf der Minimap. */
        public boolean minimapPolitical = false;
        /** Gemalte Karte über unerkundetem Terrain auch auf der Minimap. */
        public boolean minimapPainted = true;
        /** Wappen des aktuellen Lehens unten rechts an der Minimap. */
        public boolean minimapBanner = true;
        public int minimapBannerSize = 16;
        /** Feinjustierung der Wappen-Position relativ zur Minimap-Ecke (px). */
        public int minimapBannerOffsetX = 18;
        public int minimapBannerOffsetY = -15;
        /** Profil "runde Minimap": eigener Versatz (Toggle im Karten-Menü). */
        public boolean minimapBannerRound = true;
        public int minimapBannerOffsetXRound = 10;
        public int minimapBannerOffsetYRound = -30;
        /** Hotkey: Overlay auf der Worldmap an/aus. */
        public String toggleKey = "key.keyboard.k";
        // --- Erweitert ---
        /** Ab dieser Zoomstufe (px/Block) erscheinen Namen. */
        public double nameMinScale = 0.05;
        /** Ab dieser Zoomstufe erscheinen Wappen. */
        public double bannerMinScale = 0.05;
        /** Schriftgröße der Kartenlabels (globaler Faktor; 1.0 = Standard). */
        public float labelScale = 1.0f;
        /** Politische Flächen blenden oberhalb dieser Zoomstufe aus (Xaero-Anzeige unten). */
        public double politicalMaxScale = 0.6;
        // Dynamische Label-/Wappen-Größen: Werte an zwei Zoom-Breakpoints
        // (A = weit draussen, B = nah dran), dazwischen smooth interpoliert.
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
        /** Grenzlinien-Farbe als Hex (#AARRGGBB oder #RRGGBB). */
        public String borderColor = "#493C30";
        /** Grenzen als Strichgrafik (gestrichelt) statt durchgezogen. */
        public boolean dashedBorders = true;
        public int dashLengthPx = 6;
        public int dashGapPx = 4;
        /** Gefolge-Farb-Overrides: Gruppen-Anzeigename -> "#RRGGBB". */
        public java.util.LinkedHashMap<String, String> groupColors = defaultGroupColors();

        /** Ingame abgestimmte Gefolge-Farben als Auslieferungs-Default. */
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
        /** HUD-Elemente einzeln: Gefolgename + Stand (Hierarchie) als eigene Blöcke. */
        public boolean minimapBannerShowName = false;
        public boolean minimapBannerShowState = true;
        /** Zusatzzeile: Gefolgename (Faction) unter dem Lehensnamen. */
        public boolean minimapBannerShowFaction = true;
        /** Freie HUD-Positionen (Drag&Drop), je Element. -1 = Default-Andockung. */
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
        /** Skalierung der Text-Elemente (Icon skaliert über minimapBannerSize). */
        public float nameHudScale = 0.52f;
        public float stateHudScale = 0.52f;
        public float factionHudScale = 0.97f;
        /** Feste Boxbreite der Text-Elemente (px); Text linksbündig, Überlauf gekürzt. */
        public int nameHudWidth = 46;
        public int stateHudWidth = 91;
        public int factionHudWidth = 89;
        /** Farben der Text-Elemente ("#RRGGBB"). */
        public String nameHudColor = "#E6C8A9";
        public String stateHudColor = "#B8A88F";
        public String factionHudColor = "#FFFFFF";
    }

    public static final class Regions {
        public boolean enabled = true;
        public boolean hideOriginalActionbar = true;
        public boolean playEnterSound = true;
        public boolean showBanner = true;
        public boolean hintTextEnabled = false;
        /** Aktives Theme: "light", "dark" oder Name eines Custom-Themes. */
        public String theme = "light";
        /** Eigene Toast-Themes (Name + 8 Farben), per GUI anlegbar/editierbar. */
        public java.util.List<RegionTheme> customThemes = new java.util.ArrayList<>();
        /** TOP_CENTER (Server-Standard), TOP_RIGHT, TOP_LEFT, CENTER. */
        public String overlayPosition = "TOP_CENTER";
        public String menuKey = "key.keyboard.l";

        // --- Erweitert: Toast-Layout (Werte aus ottoregions.json) ---
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
        // Schrift-Skalierungen: effektiv = baseTextScale * Zeilen-Scale
        public float baseTextScale = 1.0f;
        public float titleScale = 0.65f;
        public float regionScale = 1.0f;
        public float hierarchyScale = 0.68f;
        public float hintScale = 0.35f;
    }

    /**
     * Benutzerdefiniertes Toast-Theme: Name + 8 Panel-Farben als "#RRGGBB".
     * Defaults = Light-Palette. GSON-direkt, daher mutable.
     */
    public static final class RegionTheme {
        public String name = "Custom";
        // Farben
        public String bg = "#C8AC8E";
        public String borderOut = "#513E2A";
        public String borderTl = "#E6C8A9";
        public String borderBr = "#B8926E";
        public String title = "#503D29";
        public String region = "#503D29";
        public String hierarchy = "#7A5A3A";
        public String hint = "#6A4D33";
        // Schrift (baseTextScale * Zeilen-Scale)
        public float baseTextScale = 1.0f;
        public float titleScale = 0.65f;
        public float regionScale = 1.0f;
        public float hierarchyScale = 0.68f;
        public float hintScale = 0.35f;
        // Sichtbarkeit der Elemente
        public boolean showBanner = true;
        public boolean showEnteredTitle = true; // "Du betrittst"-Zeile
        public boolean showHierarchy = true;    // Hierarchie/Lehensname-Zeile
        public boolean showHint = false;        // Tasten-Hinweis
        // Abstände / Layout
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
        // Chat-Anzeige je Kanal (RP-Kanäle an, OOC-artige aus)
        public boolean showInSprechen = true;
        public boolean showInReden = true;
        public boolean showInRufen = true;
        public boolean showInFluestern = true;
        // OOC-Kanäle (Offtopic + Hilfe): RP-Namen standardmäßig aus, per Setting opt-in
        public boolean showInHilfe = false;
        public boolean showInOfftopic = false;
        public boolean showInOoc = false;
        public boolean showInAllChannels = false;
        /** Unbekannte Sprecher als "Unbekannt" zeigen statt Accountname. */
        public boolean showUnknownAsUnknown = true;
        /** Unbekannte: Accountname zeigen (true) statt Platzhalter (false). */
        public boolean unknownShowAccount = false;
        /** Platzhalter-Text für unbekannte RP-Namen ("Unbekannt", "???", ...). */
        public String unknownPlaceholder = "Unbekannt";
        // Tabliste
        public boolean tablistEnabled = true;
        public boolean tablistShowTitle = true;
        /** Titel auch ohne RP-Namen-Ersetzung in der Tabliste voranstellen. */
        public boolean tablistTitlesAlways = true;
        public boolean tablistShowAccountForUnknown = true;
        /** Shift-Klick auf Spielernamen im Chat / Shift-Rechtsklick auf Spieler
         *  öffnet das RP-Personenbuch beim Eintrag der Person. */
        public boolean openBookOnClick = false;
        // API ist optional — Default aus (lokales Bekanntschaftssystem)
        public boolean syncFromPublicApi = true;
        /** Standardmäßig aus: keine personenbezogenen Daten ungefragt hochladen. */
        public boolean uploadLearnedNames = false;
    }

    public static final class Nametags {
        public boolean enabled = true;
        public NameTagMode mode = NameTagMode.REALISTIC;
        public boolean showTitle = true;
        public boolean showRpName = true;
        public boolean showPlayerName = false;
        public String toggleKey = "key.keyboard.n";
        /** Schriftgrößen je Zeile (1.0 = Vanilla) + Zeilenabstand in Label-Pixeln. */
        public float titleScale = 1.0f;
        public float nameScale = 1.0f;
        public float accountScale = 0.8f;
        public int lineSpacing = 10;
        /** Farbe des Accountnamens über dem Kopf ("#RRGGBB"). */
        public String accountColor = "#666666";
    }

    public static final class Letter {
        public boolean enabled = true;
        public String triggerItemName = "Pergament und Feder";
        public int maxLinesPerPage = 12;
        public int maxCharsPerLine = 18;
        /** Serverbefehle (ohne Slash) — abstrahiert, falls der Server andere nutzt. */
        public String letterCommand = "letter";
        public String postCommand = "post";
        /** Abschlussbefehl der Verkündung (leer = manuell ausführen). */
        public String announcementSubmitCommand = "";
        // Layout-Guard für Verkündungen: sichere vs. harte Grenzen
        public int announcementSafeLinesPerPage = 11;
        public int announcementSafeCharsPerLine = 17;
        public int announcementHardLinesPerPage = 12;
        public int announcementHardCharsPerLine = 18;
        // Anti-Spam-Timing (randomisiert, Reihenfolge bleibt)
        public int letterSendDelayMinMs = 1100;
        public int letterSendDelayMaxMs = 1900;
        public int letterPageDelayMinMs = 3000;
        public int letterPageDelayMaxMs = 5200;
    }

    public static final class Chat {
        public boolean enabled = true;
        /** Blinkende "!" vor [Offtopic] (Signal: öffentlicher Kanal). */
        public boolean offtopicBangEnabled = true;
        /** Anzahl der "!" (1-3). */
        public int offtopicBangCount = 3;
        /** Beim Server-Join automatisch /s senden (Sprechen als Standard). */
        public boolean autoSprechenOnJoin = true;
        public String voiceKey = "key.keyboard.v";
        public String helpKey = "key.keyboard.h";
        public String offtopicKey = "key.keyboard.o";
    }

    /** Automatischer Server-Resourcepack-Downloader. */
    public static final class ResourcePack {
        public boolean enabled = true;
        /**
         * Bevorzugt: GitHub-Releases-API ({@code .../releases/latest}) ODER eine
         * eigene latest.json (version + url + sha256). GitHub wird automatisch erkannt
         * (Host {@code api.github.com}).
         */
        public String manifestUrl = "https://api.github.com/repos/Ottonien/ottonien-reformed/releases/latest";
        /** Name des Release-Assets (nur GitHub-Modus). */
        public String assetName = "Ottonien.zip";
        /** Fallback: direkte ZIP-URL (nur wenn manifestUrl leer). */
        public String directZipUrl = "";
        public boolean autoEnable = true;
        public boolean checkOnStartup = true;
        public boolean respectUserDisable = false;
        public boolean priorityTop = true;
        public long maxSizeBytes = 64L * 1024 * 1024;
        public int connectTimeoutMs = 10_000;
        public int requestTimeoutMs = 60_000;
        public boolean showToasts = true;

        /** Aktive Bezugsqülle (Manifest bevorzugt). Leer => Feature inaktiv. */
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

    // ---- Laden / Speichern ----------------------------------------------

    /** Aktive Instanz (gesetzt von {@link #load()}) — für ModMenu-Screen. */
    private static volatile OttoExtraConfig active;

    public static OttoExtraConfig active() {
        OttoExtraConfig a = active;
        return a != null ? a : load();
    }

    /** Lädt die Config oder erzeugt sie mit Defaults. Wirft nie. */
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

    /** Füllt fehlende (null) Sektionen mit Defaults auf. */
    public void repair() {
        if (api == null) api = new Api();
        if (map == null) map = new Map();
        if (regions == null) regions = new Regions();
        if (rpnames == null) rpnames = new RpNames();
        if (nametags == null) nametags = new Nametags();
        if (letter == null) letter = new Letter();
        if (chat == null) chat = new Chat();
        if (resourcepack == null) resourcepack = new ResourcePack();
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
        // https ist Pflicht — http:// hart reparieren
        if (api.baseUrl.startsWith("http://")) {
            api.baseUrl = "https://" + api.baseUrl.substring("http://".length());
        }
    }

    /** JSON-Snapshot fürs Settings-GUI (Dirty-Erkennung + Verwerfen). */
    public String snapshotJson() {
        return GSON.toJson(this);
    }

    /**
     * Stellt alle Sektionen aus einem Snapshot wieder her (Verwerfen).
     * WICHTIG: kopiert Feldwerte IN die bestehenden Sektions-Objekte —
     * Module (z. B. NametagService) halten Referenzen darauf und würden
     * bei einem Objekt-Tausch eingefrorene Kopien lesen.
     */
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

    /** Schreibt atomar (Temp-Datei + ATOMIC_MOVE). */
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
