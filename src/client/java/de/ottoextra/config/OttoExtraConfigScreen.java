package de.ottoextra.config;

import de.ottoextra.nametags.NameTagMode;
import de.ottoextra.regions.RegionMessageService;
import de.ottoextra.regions.RegionNotificationOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * OttoExtra-Einstellungen (ModMenu): Master-Detail im Pergament-Look.
 *
 * <p>Links die Modulliste (Resourcepack, Regionen, Namensschilder, ...), rechts
 * die Einstellungen des gewählten Moduls. Tiefgehende Werte liegen je Modul
 * unter "Erweitert" ({@link OttoExtraAdvancedScreen}). Farben folgen dem
 * Ottonien-Badge-Styling (ottoregions.json, Light-Theme).</p>
 */
public final class OttoExtraConfigScreen extends Screen {

    // Pergament-Palette (Badge-Styling)
    static final int COL_BG = 0xFFC8AC8E;
    static final int COL_PANEL = 0xFFBFA083;
    static final int COL_SIDEBAR = 0xFFB6967A;
    static final int COL_BORDER = 0xFF513E2A;
    static final int COL_INNER_TL = 0xFFE6C8A9;
    static final int COL_INNER_BR = 0xFFB8926E;
    static final int COL_TITLE = 0xFF503D29;
    static final int COL_TEXT = 0xFF5A4631;
    static final int COL_MUTED = 0xFF6A4D33;
    static final int COL_SELECTED = 0xFF7A5A3A;

    /** Module der linken Liste. */
    private enum Module {
        RESOURCEPACK("ottoextra.module.resourcepack"),
        REGIONS("ottoextra.module.regions"),
        NAMETAGS("ottoextra.module.nametags"),
        MAP("ottoextra.module.map"),
        RPNAMES("ottoextra.module.rpnames"),
        LETTER("ottoextra.module.letter"),
        CHAT("ottoextra.module.chat");

        final String key;

        Module(String key) {
            this.key = key;
        }
    }

    private final Screen parent;
    private final OttoExtraConfig config;
    private Module selected = Module.RESOURCEPACK;
    private final List<ButtonWidget> moduleButtons = new ArrayList<>();
    private final List<ButtonWidget> detailWidgets = new ArrayList<>();
    // Detail-Scrolling: Basis-Positionen + aktueller Offset
    private final List<Integer> detailBaseY = new ArrayList<>();
    private int detailScroll = 0;

    public OttoExtraConfigScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.config.title"));
        this.parent = parent;
        this.config = config;
    }

    // ---- Layout ------------------------------------------------------------

    private int panelX() {
        return Math.max(8, (width - panelW()) / 2);
    }

    private int panelY() {
        return Math.max(8, (height - panelH()) / 2);
    }

    private int panelW() {
        return Math.min(width - 16, 400);
    }

    private int panelH() {
        return Math.min(height - 16, 260);
    }

    private int sidebarW() {
        return 130;
    }

    private int detailX() {
        return panelX() + sidebarW() + 10;
    }

    private int detailY() {
        return panelY() + 26;
    }

    private int detailW() {
        return panelX() + panelW() - 8 - detailX();
    }

    // ---- Init ----------------------------------------------------------------

    @Override
    protected void init() {
        moduleButtons.clear();
        int bx = panelX() + 6;
        int bw = sidebarW() - 12;
        int by = panelY() + 26;
        for (Module m : Module.values()) {
            final Module module = m;
            ButtonWidget btn = ButtonWidget.builder(Text.translatable(m.key), b -> {
                selected = module;
                rebuild();
            }).dimensions(bx, by, bw, 18).build();
            moduleButtons.add(btn);
            addDrawableChild(btn);
            by += 21;
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(bx, panelY() + panelH() - 26, bw, 18).build());

        buildDetail();
        captureAndApplyScroll();
        updateSidebarState();
    }

    private void rebuild() {
        detailWidgets.forEach(this::remove);
        detailWidgets.clear();
        detailScroll = 0;
        buildDetail();
        captureAndApplyScroll();
        updateSidebarState();
    }

    // ---- Detail-Scrolling ------------------------------------------------------

    private int detailViewBottom() {
        return panelY() + panelH() - 8;
    }

    private int detailMaxScroll() {
        int contentBottom = 0;
        for (int i = 0; i < detailWidgets.size(); i++) {
            contentBottom = Math.max(contentBottom, detailBaseY.get(i) + 18);
        }
        return Math.max(0, contentBottom - detailViewBottom());
    }

    /** Nach buildDetail(): Basis-Positionen merken + Offset anwenden. */
    private void captureAndApplyScroll() {
        detailBaseY.clear();
        for (ButtonWidget w : detailWidgets) {
            detailBaseY.add(w.getY());
        }
        applyDetailScroll();
    }

    private void applyDetailScroll() {
        detailScroll = Math.max(0, Math.min(detailScroll, detailMaxScroll()));
        for (int i = 0; i < detailWidgets.size(); i++) {
            ButtonWidget w = detailWidgets.get(i);
            int y = detailBaseY.get(i) - detailScroll;
            w.setY(y);
            w.visible = y >= detailY() - 2 && y + 18 <= detailViewBottom() + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= detailX() && mouseX <= panelX() + panelW()
                && mouseY >= detailY() && mouseY <= detailViewBottom()
                && detailMaxScroll() > 0) {
            detailScroll -= (int) Math.signum(vertical) * 21;
            applyDetailScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void updateSidebarState() {
        Module[] values = Module.values();
        for (int i = 0; i < values.length; i++) {
            moduleButtons.get(i).active = values[i] != selected;
        }
    }

    // ---- Detail-Panels -------------------------------------------------------

    private void buildDetail() {
        int x = detailX();
        int w = detailW();
        int y = detailY();
        switch (selected) {
            case RESOURCEPACK -> {
                y = toggle(x, y, w, "ottoextra.config.rp.enabled",
                        () -> config.resourcepack.enabled, v -> config.resourcepack.enabled = v);
                y = toggle(x, y, w, "ottoextra.config.rp.autoEnable",
                        () -> config.resourcepack.autoEnable, v -> config.resourcepack.autoEnable = v);
                y = toggle(x, y, w, "ottoextra.config.rp.checkOnStartup",
                        () -> config.resourcepack.checkOnStartup, v -> config.resourcepack.checkOnStartup = v);
                y = toggle(x, y, w, "ottoextra.config.rp.respectUserDisable",
                        () -> config.resourcepack.respectUserDisable, v -> config.resourcepack.respectUserDisable = v);
                advanced(x, y, w, OttoExtraAdvancedScreen.resourcepack(this, config));
            }
            case REGIONS -> {
                y = toggle(x, y, w, "ottoextra.config.regions.enabled",
                        () -> config.regions.enabled, v -> config.regions.enabled = v);
                y = toggle(x, y, w, "ottoextra.config.regions.hideActionbar",
                        () -> config.regions.hideOriginalActionbar, v -> config.regions.hideOriginalActionbar = v);
                y = toggle(x, y, w, "ottoextra.config.regions.sound",
                        () -> config.regions.playEnterSound, v -> config.regions.playEnterSound = v);
                y = toggle(x, y, w, "ottoextra.config.regions.banner",
                        () -> config.regions.showBanner, v -> config.regions.showBanner = v);
                y = toggle(x, y, w, "ottoextra.config.regions.hint",
                        () -> config.regions.hintTextEnabled, v -> config.regions.hintTextEnabled = v);
                y = cycle(x, y, w, "ottoextra.config.regions.theme",
                        () -> config.regions.theme, () -> {
                            config.regions.theme = "light".equalsIgnoreCase(config.regions.theme) ? "dark" : "light";
                            return config.regions.theme;
                        });
                y = cycle(x, y, w, "ottoextra.config.regions.position",
                        () -> config.regions.overlayPosition, () -> {
                            String[] order = {"TOP_CENTER", "TOP_RIGHT", "TOP_LEFT", "CENTER"};
                            int idx = 0;
                            for (int i = 0; i < order.length; i++) {
                                if (order[i].equalsIgnoreCase(config.regions.overlayPosition)) {
                                    idx = i;
                                    break;
                                }
                            }
                            config.regions.overlayPosition = order[(idx + 1) % order.length];
                            return config.regions.overlayPosition;
                        });
                y = action(x, y, w, "ottoextra.config.preview", OttoExtraConfigScreen::triggerPreview);
                advanced(x, y, w, OttoExtraAdvancedScreen.regions(this, config));
            }
            case NAMETAGS -> {
                y = toggle(x, y, w, "ottoextra.config.module.nametags",
                        () -> config.nametags.enabled, v -> config.nametags.enabled = v);
                y = cycle(x, y, w, "ottoextra.config.nametags.mode",
                        () -> Text.translatable(config.nametags.mode.translationKey()).getString(), () -> {
                            config.nametags.mode = config.nametags.mode.next();
                            return Text.translatable(config.nametags.mode.translationKey()).getString();
                        });
                y = toggle(x, y, w, "ottoextra.config.nametags.showTitle",
                        () -> config.nametags.showTitle, v -> config.nametags.showTitle = v);
                y = toggle(x, y, w, "ottoextra.config.nametags.showRpName",
                        () -> config.nametags.showRpName, v -> config.nametags.showRpName = v);
                y = toggle(x, y, w, "ottoextra.config.nametags.showPlayerName",
                        () -> config.nametags.showPlayerName, v -> config.nametags.showPlayerName = v);
                advanced(x, y, w, OttoExtraAdvancedScreen.nametags(this, config));
            }
            case MAP -> {
                y = toggle(x, y, w, "ottoextra.config.module.map",
                        () -> config.map.enabled, v -> config.map.enabled = v);
                y = toggle(x, y, w, "ottoextra.config.map.borders",
                        () -> config.map.showBorders, v -> config.map.showBorders = v);
                y = toggle(x, y, w, "ottoextra.config.map.names",
                        () -> config.map.showNames, v -> config.map.showNames = v);
                y = toggle(x, y, w, "ottoextra.config.map.banners",
                        () -> config.map.showBanners, v -> config.map.showBanners = v);
                y = toggle(x, y, w, "ottoextra.config.map.minimap",
                        () -> config.map.minimapBorders, v -> config.map.minimapBorders = v);
                y = toggle(x, y, w, "ottoextra.config.map.minimapPainted",
                        () -> config.map.minimapPainted, v -> config.map.minimapPainted = v);
                y = toggle(x, y, w, "ottoextra.config.map.minimapPolitical",
                        () -> config.map.minimapPolitical, v -> config.map.minimapPolitical = v);
                y = toggle(x, y, w, "ottoextra.config.map.minimapBanner",
                        () -> config.map.minimapBanner, v -> config.map.minimapBanner = v);
                y = toggle(x, y, w, "ottoextra.config.map.minimapBannerRound",
                        () -> config.map.minimapBannerRound, v -> config.map.minimapBannerRound = v);
                y = toggle(x, y, w, "ottoextra.config.map.dashed",
                        () -> config.map.dashedBorders, v -> config.map.dashedBorders = v);
                y = toggle(x, y, w, "ottoextra.config.map.painted",
                        () -> config.map.paintedMap, v -> config.map.paintedMap = v);
                y = toggle(x, y, w, "ottoextra.config.map.political",
                        () -> config.map.politicalFill, v -> config.map.politicalFill = v);
                y = toggle(x, y, w, "ottoextra.config.map.activity",
                        () -> config.map.showActivity, v -> config.map.showActivity = v);
                y = toggle(x, y, w, "ottoextra.config.map.onlyOttonien",
                        () -> config.map.onlyOnOttonien, v -> config.map.onlyOnOttonien = v);
                y = action(x, y, w, "ottoextra.config.map.groupColors", () ->
                        net.minecraft.client.MinecraftClient.getInstance()
                                .setScreen(new MapGroupColorsScreen(this, config)));
                y = toggle(x, y, w, "ottoextra.config.map.bannerName",
                        () -> config.map.minimapBannerShowName, v -> config.map.minimapBannerShowName = v);
                y = toggle(x, y, w, "ottoextra.config.map.bannerState",
                        () -> config.map.minimapBannerShowState, v -> config.map.minimapBannerShowState = v);
                y = action(x, y, w, "ottoextra.config.map.bannerEdit", () ->
                        net.minecraft.client.MinecraftClient.getInstance()
                                .setScreen(new de.ottoextra.map.MinimapBannerEditScreen(this, config)));
                advanced(x, y, w, OttoExtraAdvancedScreen.map(this, config));
            }
            case RPNAMES -> {
                y = toggle(x, y, w, "ottoextra.config.module.rpnames",
                        () -> config.rpnames.enabled, v -> config.rpnames.enabled = v);
                y = toggle(x, y, w, "ottoextra.config.rpnames.unknown",
                        () -> config.rpnames.showUnknownAsUnknown, v -> config.rpnames.showUnknownAsUnknown = v);
                y = toggle(x, y, w, "ottoextra.config.rpnames.unknownAccount",
                        () -> config.rpnames.unknownShowAccount, v -> config.rpnames.unknownShowAccount = v);
                y = cycle(x, y, w, "ottoextra.config.rpnames.unknownPlaceholder",
                        () -> config.rpnames.unknownPlaceholder, () -> {
                            config.rpnames.unknownPlaceholder =
                                    "Unbekannt".equals(config.rpnames.unknownPlaceholder) ? "???" : "Unbekannt";
                            return config.rpnames.unknownPlaceholder;
                        });
                y = toggle(x, y, w, "ottoextra.config.rpnames.tablist",
                        () -> config.rpnames.tablistEnabled, v -> config.rpnames.tablistEnabled = v);
                y = toggle(x, y, w, "ottoextra.config.rpnames.tablistTitles",
                        () -> config.rpnames.tablistTitlesAlways, v -> config.rpnames.tablistTitlesAlways = v);
                action(x, y, w, "ottoextra.config.rpnames.people", () -> {
                    var client = net.minecraft.client.MinecraftClient.getInstance();
                    client.setScreen(new de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen(this));
                });
            }
            case LETTER -> {
                y = toggle(x, y, w, "ottoextra.config.module.letter",
                        () -> config.letter.enabled, v -> config.letter.enabled = v);
                action(x, y, w, "ottoextra.config.letter.open", () ->
                        net.minecraft.client.MinecraftClient.getInstance().setScreen(
                                new de.ottoextra.letter.ui.LetterEditorScreen(this, config)));
            }
            case CHAT -> {
                y = toggle(x, y, w, "ottoextra.config.module.chat",
                        () -> config.chat.enabled, v -> config.chat.enabled = v);
                y = toggle(x, y, w, "ottoextra.config.chat.bang",
                        () -> config.chat.offtopicBangEnabled, v -> config.chat.offtopicBangEnabled = v);
                y = toggle(x, y, w, "ottoextra.config.chat.autoSprechen",
                        () -> config.chat.autoSprechenOnJoin, v -> config.chat.autoSprechenOnJoin = v);
                cycle(x, y, w, "ottoextra.config.chat.bangCount",
                        () -> String.valueOf(config.chat.offtopicBangCount), () -> {
                            config.chat.offtopicBangCount = config.chat.offtopicBangCount % 3 + 1;
                            return String.valueOf(config.chat.offtopicBangCount);
                        });
            }
        }
    }

    private int toggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
        ButtonWidget btn = ButtonWidget.builder(toggleLabel(key, get.getAsBoolean()), b -> {
            boolean next = !get.getAsBoolean();
            set.accept(next);
            config.save();
            b.setMessage(toggleLabel(key, next));
        }).dimensions(x, y, w, 18).build();
        detailWidgets.add(btn);
        addDrawableChild(btn);
        return y + 21;
    }

    /** Button, der durch Werte rotiert (Theme, Position, Modus). */
    private int cycle(int x, int y, int w, String key, Supplier<String> current, Supplier<String> next) {
        ButtonWidget btn = ButtonWidget.builder(cycleLabel(key, current.get()), b -> {
            String value = next.get();
            config.save();
            b.setMessage(cycleLabel(key, value));
        }).dimensions(x, y, w, 18).build();
        detailWidgets.add(btn);
        addDrawableChild(btn);
        return y + 21;
    }

    /** Toast-Vorschau: Beispieldaten anzeigen + Original-Sound (rendert live im Menü). */
    public static void triggerPreview() {
        RegionNotificationOverlay.show("Sankt Aegidius", "Abtei in Grafschaft Holdstewik");
        RegionMessageService.playEnterSound();
    }

    private int action(int x, int y, int w, String key, Runnable run) {
        ButtonWidget btn = ButtonWidget.builder(Text.translatable(key), b -> run.run())
                .dimensions(x, y, w, 18).build();
        detailWidgets.add(btn);
        addDrawableChild(btn);
        return y + 21;
    }

    private void advanced(int x, int y, int w, Screen target) {
        ButtonWidget btn = ButtonWidget.builder(Text.translatable("ottoextra.config.advanced"),
                        b -> client.setScreen(target))
                .dimensions(x, y + 4, w, 18).build();
        detailWidgets.add(btn);
        addDrawableChild(btn);
    }

    private Text toggleLabel(String key, boolean on) {
        return Text.translatable(key).copy().append(": ")
                .append(Text.translatable(on ? "ottoextra.config.on" : "ottoextra.config.off"));
    }

    private Text cycleLabel(String key, String value) {
        return Text.translatable(key).copy().append(": " + value);
    }

    // ---- Render ----------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        // Pergament-Panel mit Doppelrahmen (Badge-Stil)
        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, COL_BORDER);
        ctx.fill(px, py, px + pw, py + ph, COL_PANEL);
        ctx.fill(px, py, px + pw, py + 1, COL_INNER_TL);
        ctx.fill(px, py, px + 1, py + ph, COL_INNER_TL);
        ctx.fill(px, py + ph - 1, px + pw, py + ph, COL_INNER_BR);
        ctx.fill(px + pw - 1, py, px + pw, py + ph, COL_INNER_BR);
        // Sidebar
        ctx.fill(px + 1, py + 1, px + sidebarW(), py + ph - 1, COL_SIDEBAR);
        ctx.fill(px + sidebarW(), py + 1, px + sidebarW() + 1, py + ph - 1, COL_BORDER);

        // Titelzeile
        ctx.drawText(textRenderer, title, px + 8, py + 9, COL_TITLE, false);
        ctx.drawText(textRenderer, Text.translatable(selected.key),
                detailX(), py + 9, COL_TITLE, false);

        super.render(ctx, mouseX, mouseY, delta);

        // Scrollbar rechts im Detailbereich, wenn Inhalt nicht passt
        int maxScroll = detailMaxScroll();
        if (maxScroll > 0) {
            int trackX = panelX() + panelW() - 6;
            int trackTop = detailY();
            int trackH = detailViewBottom() - trackTop;
            ctx.fill(trackX, trackTop, trackX + 3, trackTop + trackH, COL_INNER_BR);
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = trackTop + (trackH - thumbH) * detailScroll / maxScroll;
            ctx.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, COL_BORDER);
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.config.restart_hint"),
                width / 2, py + ph + 6, 0xFF9A8C6A);

        // Toast-Vorschau auch im Menü sichtbar
        RegionNotificationOverlay.render(ctx, null);
    }

    @Override
    public void close() {
        config.save();
        client.setScreen(parent);
    }
}
