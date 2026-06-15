package de.ottoextra.config.settings;

import de.ottoextra.config.OttoExtraBackupService;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings im Vanilla-Dark-Stil (Vorbild MoreCulling/XaeroPlus):
 * Modul-Tabs oben, Suche, Options-Zeilen mit Label links und
 * Wert-Button/Feld + "Zurücksetzen" rechts, Footer mit
 * "Änderungen verwerfen" / "Speichern &amp; Beenden".
 *
 * <p>Werte ändern sich live im Speicher (Module sehen sie sofort);
 * "Speichern &amp; Beenden" persistiert, "Verwerfen" stellt den Snapshot
 * vom Öffnen wieder her (Feld-Kopie, Referenzen bleiben stabil).
 * Esc = Speichern &amp; Beenden.</p>
 */
public final class OttoExtraSettingsScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int VALUE_W = 100;
    private static final int RESET_W = 80;
    private static final int COL_LABEL = 0xFFFFFFFF;
    private static final int COL_HEADER = 0xFFFFFF99;
    private static final int COL_DESC = 0xFFA0A0A0;
    private static final int COL_STATUS = 0xFF55FF55;
    private static final int COL_WARN = 0xFFFF5555;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final SettingsRegistry registry;
    private final String snapshot;
    /** Defaults: frische Config → gleiche Registry → get() liefert Default. */
    private final Map<String, String> defaults = new HashMap<>();

    private int selectedModule; // 0..n-1 Module, n = Backups
    private int selectedTab;
    private int scroll;
    private int contentHeight;

    private TextFieldWidget searchField;

    /** Modul-Auswahl als Dropdown statt Tab-Reihe, wenn die Reihe zu breit wird
     *  (kleine Monitore). */
    private boolean tabCompact;
    private boolean tabMenuOpen;
    private final List<Text> tabLabels = new ArrayList<>();
    private int tabMenuX;
    private int tabMenuY;
    private int tabMenuW;

    /** Eine Inhaltszeile: Label (gezeichnet) + Widgets (verschoben beim Scroll). */
    private static final class Row {
        int baseY;
        int height = ROW_H;
        Text label;
        Text tooltip;
        boolean header;
        List<OrderedText> descLines = List.of();
        final List<ClickableWidget> widgets = new ArrayList<>();
    }

    private final List<Row> rows = new ArrayList<>();
    private java.nio.file.Path pendingRestore;
    private String statusMessage = "";
    private boolean statusWarn;

    public OttoExtraSettingsScreen(Screen parent) {
        super(Text.translatable("ottoextra.settings.title"));
        this.parent = parent;
        this.config = OttoExtraConfig.active();
        this.snapshot = config.snapshotJson();
        this.registry = SettingsRegistry.build(config,
                () -> open(new de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen(this)),
                () -> open(new de.ottoextra.config.MapGroupColorsScreen(this, config)),
                () -> open(new de.ottoextra.map.MinimapBannerEditScreen(this, config)),
                () -> open(new de.ottoextra.letter.ui.LetterEditorScreen(this, config)),
                () -> open(new de.ottoextra.config.RegionThemeScreen(this, config)),
                () -> open(new de.ottoextra.config.FollowingScreen(this, config)));
        SettingsRegistry defaultRegistry = SettingsRegistry.build(new OttoExtraConfig(),
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { });
        for (SettingsRegistry.ModulePage m : defaultRegistry.modules()) {
            for (SettingsRegistry.Tab t : m.tabs()) {
                for (SettingsRegistry.Card c : t.cards()) {
                    for (SettingsRegistry.Option o : c.options()) {
                        defaults.put(o.configKey, o.get.get());
                    }
                }
            }
        }
    }

    private void open(Screen s) {
        if (client != null) {
            client.setScreen(s);
        }
    }

    // ---- Layout ------------------------------------------------------------

    private int contentW() {
        return Math.min(640, width - 40);
    }

    private int contentX() {
        return (width - contentW()) / 2;
    }

    private int contentTop() {
        return 64;
    }

    private int contentBottom() {
        return height - 36;
    }

    // ---- Init / Seitenaufbau --------------------------------------------------

    @Override
    protected void init() {
        // Modul-Tabs oben (aktiver Tab = ausgegraut wie Personenbuch/MoreCulling).
        // Passt die Reihe nicht in die Breite (kleine Monitore), wird daraus ein
        // Dropdown-Filter.
        tabLabels.clear();
        for (SettingsRegistry.ModulePage m : registry.modules()) {
            tabLabels.add(Text.translatable(m.titleKey()));
        }
        tabLabels.add(Text.translatable("ottoextra.settings.backups"));
        int total = 0;
        int[] widths = new int[tabLabels.size()];
        for (int i = 0; i < tabLabels.size(); i++) {
            widths[i] = Math.max(46, textRenderer.getWidth(tabLabels.get(i)) + 12);
            total += widths[i] + 2;
        }
        tabCompact = total > width - 8;
        if (!tabCompact) {
            tabMenuOpen = false;
            int tx = Math.max(4, (width - total) / 2);
            for (int i = 0; i < tabLabels.size(); i++) {
                final int idx = i;
                ButtonWidget tab = ButtonWidget.builder(tabLabels.get(i), b -> {
                    selectModuleTab(idx);
                }).dimensions(tx, 22, widths[i], 16).build();
                tab.active = selectedModule != idx;
                addDrawableChild(tab);
                tx += widths[i] + 2;
            }
        } else {
            tabMenuW = Math.min(240, width - 8);
            tabMenuX = (width - tabMenuW) / 2;
            tabMenuY = 22;
            int cur = Math.min(selectedModule, tabLabels.size() - 1);
            addDrawableChild(ButtonWidget.builder(
                    Text.empty().append(tabLabels.get(cur)).append(" ▼"),
                    b -> tabMenuOpen = !tabMenuOpen)
                    .dimensions(tabMenuX, tabMenuY, tabMenuW, 16).build());
        }

        // Suche
        searchField = new TextFieldWidget(textRenderer, contentX(), 42, contentW() - 0, 16,
                Text.translatable("ottoextra.settings.search"));
        searchField.setMaxLength(64);
        searchField.setSuggestion(Text.translatable("ottoextra.settings.search").getString());
        searchField.setChangedListener(q -> {
            searchField.setSuggestion(q.isEmpty()
                    ? Text.translatable("ottoextra.settings.search").getString() : "");
            scroll = 0;
            rebuildContent();
        });
        addDrawableChild(searchField);

        // Footer
        int fy = height - 26;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.settings.discard"), b -> {
                    config.restoreFrom(snapshot);
                    if (client != null) {
                        client.setScreen(parent);
                    }
                }).dimensions(width / 2 - 154, fy, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.settings.saveQuit"), b -> saveAndClose())
                .dimensions(width / 2 + 4, fy, 150, 20).build());

        rebuildContent();
    }

    private void saveAndClose() {
        config.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    /** Esc = Speichern &amp; Beenden (Änderungen wirken ohnehin schon live). */
    @Override
    public void close() {
        saveAndClose();
    }

    // ---- Inhalt -----------------------------------------------------------------

    private void rebuildContent() {
        for (Row row : rows) {
            for (ClickableWidget w : row.widgets) {
                remove(w);
            }
        }
        rows.clear();
        statusMessage = "";

        String query = searchField != null ? searchField.getText() : "";
        int y = 0;
        if (!query.isBlank()) {
            List<SettingsRegistry.SearchHit> hits =
                    registry.search(query, k -> Text.translatable(k).getString());
            for (SettingsRegistry.SearchHit hit : hits) {
                Text crumb = Text.literal(
                                Text.translatable(hit.module().titleKey()).getString() + " › ")
                        .formatted(Formatting.GRAY)
                        .append(Text.translatable(hit.option().labelKey)
                                .copy().formatted(Formatting.WHITE));
                y = addOptionRow(y, hit.option(), crumb);
            }
            if (hits.isEmpty()) {
                Row none = new Row();
                none.baseY = y;
                none.header = true;
                none.label = Text.translatable("ottoextra.settings.search.none");
                rows.add(none);
                y += ROW_H;
            }
        } else if (selectedModule >= registry.modules().size()) {
            y = buildBackupsPage(y);
        } else {
            SettingsRegistry.ModulePage module = registry.modules().get(selectedModule);
            if (selectedTab >= module.tabs().size()) {
                selectedTab = 0;
            }
            // Untertabs als Zeile aus Buttons (nur wenn >1)
            if (module.tabs().size() > 1) {
                Row tabRow = new Row();
                tabRow.baseY = y;
                int sx = contentX();
                for (int t = 0; t < module.tabs().size(); t++) {
                    final int tabIdx = t;
                    Text label = Text.translatable(module.tabs().get(t).titleKey());
                    int w = textRenderer.getWidth(label) + 14;
                    ButtonWidget b = ButtonWidget.builder(label, btn -> {
                        selectedTab = tabIdx;
                        scroll = 0;
                        rebuildContent();
                    }).dimensions(sx, 0, w, 16).build();
                    b.active = t != selectedTab;
                    tabRow.widgets.add(b);
                    addDrawableChild(b);
                    sx += w + 4;
                }
                rows.add(tabRow);
                y += ROW_H;
            }
            for (SettingsRegistry.Card card : module.tabs().get(selectedTab).cards()) {
                Row header = new Row();
                header.baseY = y;
                header.header = true;
                header.label = Text.translatable(card.titleKey());
                header.descLines = textRenderer.wrapLines(
                        Text.translatable(card.descKey()).copy().formatted(Formatting.GRAY),
                        contentW());
                header.height = 14 + header.descLines.size() * 10 + 4;
                rows.add(header);
                y += header.height;
                for (SettingsRegistry.Option option : card.options()) {
                    y = addOptionRow(y, option, Text.translatable(option.labelKey));
                }
            }
        }
        contentHeight = y;
        scroll = Math.max(0, Math.min(scroll,
                Math.max(0, contentHeight - (contentBottom() - contentTop()))));
        layoutRows();
    }

    private int addOptionRow(int y, SettingsRegistry.Option option, Text label) {
        Row row = new Row();
        row.baseY = y;
        row.label = label;
        if (option.tooltipKey != null) {
            row.tooltip = Text.translatable(option.tooltipKey);
        }
        int resetX = contentX() + contentW() - RESET_W;
        int valueX = resetX - 4 - VALUE_W;

        ClickableWidget value = createValueWidget(option, valueX, VALUE_W);
        if (option.tooltipKey != null) {
            value.setTooltip(Tooltip.of(Text.translatable(option.tooltipKey)));
        }
        row.widgets.add(value);
        addDrawableChild(value);

        if (option.type != SettingsRegistry.Type.ACTION) {
            String def = defaults.get(option.configKey);
            ButtonWidget reset = ButtonWidget.builder(
                    Text.translatable("ottoextra.settings.reset"), b -> {
                        if (def != null) {
                            try {
                                option.set.accept(def);
                            } catch (Exception ignored) {
                            }
                            rebuildContent();
                        }
                    }).dimensions(resetX, 0, RESET_W, 18).build();
            row.widgets.add(reset);
            addDrawableChild(reset);
        }
        rows.add(row);
        return y + ROW_H;
    }

    private ClickableWidget createValueWidget(SettingsRegistry.Option o, int x, int w) {
        switch (o.type) {
            case BOOL -> {
                ButtonWidget b = ButtonWidget.builder(boolLabel(o), btn -> {
                    boolean on = Boolean.parseBoolean(o.get.get());
                    o.set.accept(String.valueOf(!on));
                    btn.setMessage(boolLabel(o));
                }).dimensions(x, 0, w, 18).build();
                return b;
            }
            case CYCLE -> {
                return ButtonWidget.builder(Text.literal(o.get.get()), btn -> {
                    String value = o.get.get();
                    String[] vals = o.cycleValues;
                    int idx = 0;
                    for (int i = 0; i < vals.length; i++) {
                        if (vals[i].equals(value)) {
                            idx = i;
                            break;
                        }
                    }
                    o.set.accept(vals[(idx + 1) % vals.length]);
                    btn.setMessage(Text.literal(o.get.get()));
                }).dimensions(x, 0, w, 18).build();
            }
            case ACTION -> {
                return ButtonWidget.builder(Text.translatable(o.labelKey + ".button"),
                                btn -> o.action.run())
                        .dimensions(x, 0, w + 4 + RESET_W, 18).build();
            }
            case SLIDER -> {
                return new OptionSlider(x, w, o);
            }
            default -> {
                TextFieldWidget f = new TextFieldWidget(textRenderer, x, 0,
                        w, 16, Text.empty());
                f.setMaxLength(256);
                String value = o.get.get();
                f.setText(o.type == SettingsRegistry.Type.COMMAND && !value.isEmpty()
                        ? "/" + value : value);
                f.setChangedListener(text -> {
                    try {
                        o.set.accept(text);
                        f.setEditableColor(0xFFE0E0E0);
                    } catch (Exception invalid) {
                        f.setEditableColor(0xFFFF5555); // ungültig: rot, Wert bleibt alt
                    }
                });
                return f;
            }
        }
    }

    /** Ganzzahl-Slider (Balken) für eine {@link SettingsRegistry.Option} vom Typ SLIDER. */
    private static final class OptionSlider extends net.minecraft.client.gui.widget.SliderWidget {
        private final SettingsRegistry.Option opt;

        OptionSlider(int x, int w, SettingsRegistry.Option o) {
            super(x, 0, w, 18, Text.empty(), normalized(o));
            this.opt = o;
            updateMessage();
        }

        private static double normalized(SettingsRegistry.Option o) {
            double v;
            try {
                v = Double.parseDouble(o.get.get().trim().replace(',', '.'));
            } catch (Exception e) {
                v = o.min;
            }
            double span = o.max - o.min;
            return span <= 0 ? 0 : Math.max(0, Math.min(1, (v - o.min) / span));
        }

        private int current() {
            return (int) Math.round(opt.min + value * (opt.max - opt.min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(current() + "%"));
        }

        @Override
        protected void applyValue() {
            opt.set.accept(String.valueOf(current()));
        }
    }

    private static Text boolLabel(SettingsRegistry.Option o) {
        boolean on = Boolean.parseBoolean(o.get.get());
        return on
                ? Text.translatable("ottoextra.settings.on").formatted(Formatting.GREEN)
                : Text.translatable("ottoextra.settings.off").formatted(Formatting.RED);
    }

    // ---- Backups ----------------------------------------------------------------

    private int buildBackupsPage(int y) {
        Row head = new Row();
        head.baseY = y;
        head.header = true;
        head.label = Text.translatable("ottoextra.settings.backups.head");
        head.descLines = textRenderer.wrapLines(
                Text.translatable("ottoextra.settings.backups.desc")
                        .copy().formatted(Formatting.GRAY), contentW());
        head.height = 14 + head.descLines.size() * 10 + 4;
        rows.add(head);
        y += head.height;

        Row createRow = new Row();
        createRow.baseY = y;
        ButtonWidget create = ButtonWidget.builder(
                Text.translatable("ottoextra.settings.backups.create"), b -> {
                    try {
                        OttoExtraBackupService.createBackup();
                        status("ottoextra.settings.backups.created", false);
                    } catch (Exception e) {
                        status("ottoextra.settings.backups.failed", true);
                    }
                    rebuildContent();
                }).dimensions(contentX(), 0, 170, 18).build();
        createRow.widgets.add(create);
        addDrawableChild(create);
        rows.add(createRow);
        y += ROW_H + 4;

        for (OttoExtraBackupService.BackupEntry entry : OttoExtraBackupService.listBackups()) {
            Row row = new Row();
            row.baseY = y;
            row.label = Text.literal(entry.name() + "  ").append(
                    Text.translatable("ottoextra.settings.backups.files", entry.files())
                            .formatted(Formatting.GRAY));
            boolean confirming = entry.dir().equals(pendingRestore);
            Text btnLabel = confirming
                    ? Text.translatable("ottoextra.settings.backups.confirm")
                    .formatted(Formatting.RED)
                    : Text.translatable("ottoextra.settings.backups.restore");
            int bw = Math.max(120, textRenderer.getWidth(btnLabel) + 12);
            ButtonWidget restore = ButtonWidget.builder(btnLabel, b -> {
                if (confirming) {
                    boolean ok = OttoExtraBackupService.restoreBackup(entry.dir());
                    status(ok ? "ottoextra.settings.backups.restored"
                            : "ottoextra.settings.backups.failed", !ok);
                    pendingRestore = null;
                } else {
                    pendingRestore = entry.dir();
                }
                rebuildContent();
            }).dimensions(contentX() + contentW() - bw, 0, bw, 18).build();
            row.widgets.add(restore);
            addDrawableChild(restore);
            rows.add(row);
            y += ROW_H;
        }
        return y;
    }

    private void status(String key, boolean warn) {
        statusMessage = Text.translatable(key).getString();
        statusWarn = warn;
    }

    // ---- Scroll / Positionierung ---------------------------------------------------

    private void layoutRows() {
        int top = contentTop();
        int bottom = contentBottom();
        for (Row row : rows) {
            int ry = top + row.baseY - scroll;
            boolean visible = ry >= top - 2 && ry + row.height <= bottom + 4;
            for (ClickableWidget w : row.widgets) {
                w.visible = visible;
                w.setY(ry + (row.height - w.getHeight()) / 2);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal,
                                 double vertical) {
        if (mouseY >= contentTop() && mouseY <= contentBottom()) {
            int max = Math.max(0, contentHeight - (contentBottom() - contentTop()));
            scroll = Math.max(0, Math.min(scroll - (int) (vertical * 16), max));
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_S
                && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(input);
    }

    // ---- Render -----------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);

        int top = contentTop();
        int bottom = contentBottom();
        ctx.enableScissor(0, top, width, bottom);
        for (Row row : rows) {
            int ry = top + row.baseY - scroll;
            if (ry + row.height < top || ry > bottom) {
                continue;
            }
            if (row.header) {
                if (row.label != null) {
                    ctx.drawTextWithShadow(textRenderer, row.label,
                            contentX(), ry + 2, COL_HEADER);
                }
                int dy = ry + 14;
                for (OrderedText line : row.descLines) {
                    ctx.drawTextWithShadow(textRenderer, line, contentX(), dy, COL_DESC);
                    dy += 10;
                }
            } else if (row.label != null) {
                int ly = ry + (row.height - 8) / 2;
                ctx.drawTextWithShadow(textRenderer, row.label, contentX(), ly, COL_LABEL);
                if (row.tooltip != null) {
                    int lw = textRenderer.getWidth(row.label);
                    ctx.drawTextWithShadow(textRenderer, Text.literal("(?)"),
                            contentX() + lw + 4, ly, COL_DESC);
                    if (mouseX >= contentX() && mouseX <= contentX() + lw + 16
                            && mouseY >= ry && mouseY <= ry + row.height) {
                        ctx.drawTooltip(textRenderer, row.tooltip, mouseX, mouseY);
                    }
                }
            }
        }
        ctx.disableScissor();

        // Scrollbalken
        int visible = bottom - top;
        if (contentHeight > visible) {
            int x = contentX() + contentW() + 6;
            int barH = Math.max(16, visible * visible / contentHeight);
            int barY = top + (visible - barH) * scroll
                    / Math.max(1, contentHeight - visible);
            ctx.fill(x, top, x + 4, bottom, 0x66000000);
            ctx.fill(x, barY, x + 4, barY + barH, 0xCC808080);
        }

        // Statuszeile (Backups) + Dirty-Hinweis
        if (!statusMessage.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(statusMessage),
                    contentX(), bottom + 2, statusWarn ? COL_WARN : COL_STATUS);
        } else if (!config.snapshotJson().equals(snapshot)) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.settings.dirty"),
                    width / 2, bottom + 2, COL_DESC);
        }
        if (!OttoExtraBackupService.isBackupOk()) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.settings.backup.missing"),
                    contentX(), 56, COL_WARN);
        }

        // Modul-Dropdown (kleine Monitore) ueber dem Inhalt
        if (tabCompact && tabMenuOpen) {
            int iy = tabMenuY + 18;
            int n = tabLabels.size();
            ctx.fill(tabMenuX - 1, iy - 1, tabMenuX + tabMenuW + 1, iy + n * 16 + 1, 0xF0100010);
            for (int i = 0; i < n; i++) {
                int ry = iy + i * 16;
                boolean cur = i == selectedModule;
                boolean hover = mouseX >= tabMenuX && mouseX <= tabMenuX + tabMenuW
                        && mouseY >= ry && mouseY < ry + 16;
                ctx.fill(tabMenuX, ry, tabMenuX + tabMenuW, ry + 16,
                        hover ? 0x60FFFFFF : (cur ? 0x40FFFFFF : 0x30000000));
                ctx.drawTextWithShadow(textRenderer, tabLabels.get(i), tabMenuX + 6, ry + 4,
                        cur ? COL_LABEL : 0xFFCCCCCC);
            }
        }

        // Region-Toast-Vorschau ZULETZT zeichnen -> liegt ueber dem Menue
        // (sonst verdeckt das Settings-Panel die Vorschau)
        de.ottoextra.regions.RegionNotificationOverlay.render(ctx, null);
    }

    private void selectModuleTab(int idx) {
        selectedModule = idx;
        selectedTab = 0;
        scroll = 0;
        tabMenuOpen = false;
        if (searchField != null) {
            searchField.setText("");
        }
        clearAndInit();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (tabCompact && tabMenuOpen) {
            double mx = click.x();
            double my = click.y();
            int iy = tabMenuY + 18;
            int n = tabLabels.size();
            if (click.button() == 0 && mx >= tabMenuX && mx <= tabMenuX + tabMenuW
                    && my >= iy && my < iy + n * 16) {
                selectModuleTab((int) ((my - iy) / 16));
                return true;
            }
            // Klick auf den Dropdown-Knopf normal behandeln (schließt via Toggle)
            if (mx >= tabMenuX && mx <= tabMenuX + tabMenuW
                    && my >= tabMenuY && my < tabMenuY + 16) {
                return super.mouseClicked(click, doubled);
            }
            // Klick daneben: zuklappen
            tabMenuOpen = false;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
