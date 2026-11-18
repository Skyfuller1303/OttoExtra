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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static de.ottoextra.config.settings.SettingsSession.boolLabel;
import static de.ottoextra.config.settings.SettingsSession.buttonText;
import static de.ottoextra.config.settings.SettingsSession.cycleLabel;
import static de.ottoextra.config.settings.SettingsSession.defaultText;
import static de.ottoextra.config.settings.SettingsSession.helpText;
import static de.ottoextra.config.settings.SettingsSession.labelText;
import static de.ottoextra.config.settings.SettingsSession.matches;

public final class OttoExtraSettingsScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int VALUE_W = 100;
    private static final int RESET_W = 80;
    private static final int COL_LABEL = 0xFFFFFFFF;
    private static final int COL_HEADER = 0xFFFFFF99;
    private static final int COL_DESC = 0xFFA0A0A0;
    private static final int COL_STATUS = 0xFF55FF55;
    private static final int COL_WARN = 0xFFFF5555;
    private static final int COL_SECTION_BG = 0x38000000;
    private static final int COL_SECTION_BORDER = 0x665E5E5E;
    private static final int COL_SECTION_ACCENT = 0xCCDDCC66;
    private static final int SECTION_GAP = 8;

    private final SettingsSession session;
    private final OttoExtraConfig config;
    private final SettingsRegistry registry;
    private final Map<String, String> defaults;

    private static final int SIDE_X = 8;
    private static final int SIDE_TOP = 22;
    private static final int SIDE_BTN_H = 18;
    private static final int MIN_CONTENT_W = 330;

    private List<SettingsModel.Cat> cats = List.of();
    private final List<SettingsModel.Cat> allCats;
    private final Map<String, Boolean> expanded;

    private int selectedModule;
    private int selectedTab;
    private int scroll;
    private int contentHeight;

    private TextFieldWidget searchField;
    private String pendingQuery = "";

    private boolean tabCompact;
    private boolean tabMenuOpen;
    private final List<Text> tabLabels = new ArrayList<>();
    private int tabMenuX;
    private int tabMenuY;
    private int tabMenuW;
    private int sideW;
    private int areaLeft;

    private static final class Row {
        int baseY;
        int height = ROW_H;
        Text label;
        Text tooltip;
        boolean header;
        boolean section;
        boolean sectionFirst;
        boolean sectionLast;
        List<OrderedText> descLines = List.of();
        final List<ClickableWidget> widgets = new ArrayList<>();
    }

    private final List<Row> rows = new ArrayList<>();

    /**
     * Opens the settings in the preferred view: the book when it is preferred and the window is
     * large enough, otherwise the classic screen.
     */
    public static Screen create(Screen parent) {
        var window = net.minecraft.client.MinecraftClient.getInstance().getWindow();
        SettingsSession session = new SettingsSession(parent);
        String view = SettingsView.choose(session.config.settingsView,
                window.getScaledWidth(), window.getScaledHeight());
        return SettingsView.BOOK.equals(view)
                ? new OttoExtraSettingsBookScreen(session) : new OttoExtraSettingsScreen(session);
    }

    public OttoExtraSettingsScreen(Screen parent) {
        this(new SettingsSession(parent));
    }

    OttoExtraSettingsScreen(SettingsSession session) {
        super(Text.translatable("ottoextra.settings.title"));
        this.session = session;
        session.owner = this;
        this.config = session.config;
        this.registry = session.registry;
        this.allCats = session.allCats;
        this.defaults = session.defaults;
        this.expanded = session.expanded;
        this.selectedModule = session.selectedModule;
        this.selectedTab = session.selectedTab;
        this.pendingQuery = session.query;
    }

    private void open(Screen s) {
        session.open(s);
    }

    private int contentW() {
        return tabCompact ? Math.min(640, width - 40)
                : Math.min(640, width - areaLeft - 24);
    }

    private int contentX() {
        return tabCompact ? (width - contentW()) / 2
                : areaLeft + (width - areaLeft - contentW()) / 2;
    }

    /** Horizontal centre of the footer buttons: the content area, beside the sidebar. */
    private int footerCenter() {
        return tabCompact ? width / 2 : areaLeft + (width - areaLeft) / 2;
    }

    private Text expertLabel() {
        return Text.translatable("ottoextra.settings.expert",
                Text.translatable(SettingsSession.expertMode
                        ? "ottoextra.settings.on" : "ottoextra.settings.off"));
    }

    private ButtonWidget expertButton(int x, int y, int w) {
        ButtonWidget b = ButtonWidget.builder(expertLabel(), btn -> {
            SettingsSession.expertMode = !SettingsSession.expertMode;
            clearAndInit();
        }).dimensions(x, y, w, SIDE_BTN_H).build();
        b.setTooltip(Tooltip.of(Text.translatable("ottoextra.settings.expert.tooltip")));
        return b;
    }

    /** Switch to the book view; inactive when the window is too small for it. */
    private ButtonWidget viewButton(int x, int y, int w) {
        ButtonWidget b = ButtonWidget.builder(Text.translatable("ottoextra.settings.view.classic"),
                btn -> switchToBook()).dimensions(x, y, w, SIDE_BTN_H).build();
        boolean fits = SettingsView.bookFits(width, height);
        b.active = fits;
        b.setTooltip(Tooltip.of(Text.translatable(fits
                ? "ottoextra.settings.view.tooltip" : "ottoextra.settings.view.tooSmall")));
        return b;
    }

    private void switchToBook() {
        session.selectedModule = selectedModule;
        session.selectedTab = selectedTab;
        session.query = searchField != null ? searchField.getText() : pendingQuery;
        session.switchView(SettingsView.BOOK);
    }

    private int contentTop() {
        return 64;
    }

    private int contentBottom() {
        // The compact layout puts the view button on its own row above the footer.
        return height - (tabCompact ? 56 : 36);
    }

    @Override
    protected void init() {
        if (searchField != null) {
            pendingQuery = searchField.getText();
        }
        cats = SettingsModel.build(registry, SettingsSession.expertMode);
        selectedModule = Math.max(0, Math.min(selectedModule, cats.size() - 1));

        tabLabels.clear();
        int maxLabel = 0;
        for (SettingsModel.Cat c : cats) {
            Text label = Text.translatable(c.titleKey());
            tabLabels.add(label);
            maxLabel = Math.max(maxLabel, textRenderer.getWidth(label));
        }
        sideW = Math.max(90, Math.min(140, maxLabel + 14));
        areaLeft = SIDE_X + sideW + 8;
        boolean sideFits = SIDE_TOP + cats.size() * (SIDE_BTN_H + 2)
                <= height - 26 - 2 * (SIDE_BTN_H + 2) - 2;
        tabCompact = width - areaLeft - 24 < MIN_CONTENT_W || !sideFits;
        if (!tabCompact) {
            tabMenuOpen = false;
            for (int i = 0; i < cats.size(); i++) {
                final int idx = i;
                ButtonWidget tab = ButtonWidget.builder(tabLabels.get(i), b -> {
                    selectModuleTab(idx);
                }).dimensions(SIDE_X, SIDE_TOP + i * (SIDE_BTN_H + 2), sideW, SIDE_BTN_H).build();
                tab.active = selectedModule != i;
                addDrawableChild(tab);
            }
            addDrawableChild(expertButton(SIDE_X, height - 26, sideW));
            addDrawableChild(viewButton(SIDE_X, height - 26 - SIDE_BTN_H - 2, sideW));
        } else {
            int total = Math.min(width - 8, 420);
            int startX = (width - total) / 2;
            tabMenuW = total * 55 / 100;
            tabMenuX = startX;
            tabMenuY = SIDE_TOP;
            addDrawableChild(ButtonWidget.builder(
                    Text.empty().append(tabLabels.get(selectedModule)).append(" ▼"),
                    b -> tabMenuOpen = !tabMenuOpen)
                    .dimensions(tabMenuX, tabMenuY, tabMenuW, SIDE_BTN_H).build());
            addDrawableChild(expertButton(startX + tabMenuW + 4, SIDE_TOP,
                    total - tabMenuW - 4));
            addDrawableChild(viewButton(4, height - 26 - SIDE_BTN_H - 2,
                    Math.min(140, width - 8)));
        }

        searchField = new TextFieldWidget(textRenderer, contentX(), 42, contentW() - 0, 16,
                Text.translatable("ottoextra.settings.search"));
        searchField.setMaxLength(64);
        searchField.setText(pendingQuery);
        searchField.setSuggestion(pendingQuery.isEmpty()
                ? Text.translatable("ottoextra.settings.search").getString() : "");
        searchField.setChangedListener(q -> {
            searchField.setSuggestion(q.isEmpty()
                    ? Text.translatable("ottoextra.settings.search").getString() : "");
            scroll = 0;
            rebuildContent();
        });
        addDrawableChild(searchField);

        int fy = height - 26;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.settings.discard"), b -> session.discardAndClose())
                .dimensions(footerCenter() - 154, fy, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.settings.saveQuit"), b -> session.saveAndClose())
                .dimensions(footerCenter() + 4, fy, 150, 20).build());

        rebuildContent();
    }

    @Override
    public void close() {
        session.saveAndClose();
    }

    private void rebuildContent() {
        rebuildContent(false);
    }

    private void rebuildContent(boolean keepStatus) {
        for (Row row : rows) {
            for (ClickableWidget w : row.widgets) {
                remove(w);
            }
        }
        rows.clear();
        if (!keepStatus) {
            session.statusMessage = "";
        }

        String query = searchField != null ? searchField.getText() : "";
        int y = 0;
        if (!query.isBlank()) {
            String q = query.toLowerCase(Locale.ROOT).trim();
            int hits = 0;
            for (SettingsModel.Cat cat : allCats) {
                for (SettingsModel.Pg pg : cat.pages()) {
                    for (SettingsModel.Sec sec : pg.sections()) {
                        for (SettingsModel.Item item : sec.items()) {
                            SettingsRegistry.Option o = item.option();
                            if (!matches(o, q)) {
                                continue;
                            }
                            String path = Text.translatable(cat.titleKey()).getString() + " › "
                                    + (cat.other() ? "" : Text.translatable(sec.titleKey())
                                    .getString() + " › ");
                            Text crumb = Text.literal(path).formatted(Formatting.GRAY)
                                    .append(labelText(o).copy().formatted(Formatting.WHITE));
                            y = addOptionRow(y, o, crumb);
                            hits++;
                        }
                    }
                }
            }
            if (hits == 0) {
                Row none = new Row();
                none.baseY = y;
                none.header = true;
                none.label = Text.translatable("ottoextra.settings.search.none");
                rows.add(none);
                y += ROW_H;
            }
        } else {
            SettingsModel.Cat cat = cats.get(selectedModule);
            if (selectedTab >= cat.pages().size()) {
                selectedTab = 0;
            }
            SettingsModel.Pg page = cat.pages().get(selectedTab);
            boolean backups = SettingsLayout.BACKUPS_CATEGORY.equals(cat.id())
                    && selectedTab == cat.pages().size() - 1;

            Row intro = new Row();
            intro.baseY = y;
            intro.header = true;
            intro.descLines = textRenderer.wrapLines(
                    Text.translatable(page.sections().isEmpty() && !backups
                            ? "ottoextra.settings.empty" : cat.descKey())
                            .copy().formatted(Formatting.GRAY), contentW());
            intro.height = intro.descLines.size() * 10 + 6;
            rows.add(intro);
            y += intro.height;

            if (cat.pages().size() > 1) {
                Row tabRow = new Row();
                tabRow.baseY = y;
                int sx = contentX();
                for (int t = 0; t < cat.pages().size(); t++) {
                    final int tabIdx = t;
                    Text label = Text.translatable(cat.pages().get(t).titleKey());
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
            for (SettingsModel.Sec sec : page.sections()) {
                y += SECTION_GAP;
                int sectionStart = rows.size();
                Row header = new Row();
                header.baseY = y;
                header.header = true;
                header.label = Text.translatable(sec.titleKey());
                header.descLines = textRenderer.wrapLines(
                        Text.translatable(sec.descKey()).copy().formatted(Formatting.GRAY),
                        contentW());
                header.height = 14 + header.descLines.size() * 10 + 4;
                rows.add(header);
                y += header.height;

                String stateKey = cat.id() + "/" + sec.titleKey();
                boolean open = SettingsModel.isExpanded(sec.items(), expanded.get(stateKey));
                SettingsModel.Visible vis = SettingsModel.visible(sec.items(), SettingsSession.expertMode, open);
                for (SettingsModel.Item item : vis.shown()) {
                    y = addOptionRow(y, item.option(), labelText(item.option()));
                }
                if (vis.toggle()) {
                    Row toggle = new Row();
                    toggle.baseY = y;
                    Text label = vis.expanded()
                            ? Text.translatable("ottoextra.settings.lessOptions")
                            : Text.translatable("ottoextra.settings.moreOptions", vis.hidden());
                    ButtonWidget b = ButtonWidget.builder(label, btn -> {
                        expanded.put(stateKey, !vis.expanded());
                        rebuildContent();
                    }).dimensions(contentX(), 0, contentW() - 4, 18).build();
                    toggle.widgets.add(b);
                    addDrawableChild(b);
                    rows.add(toggle);
                    y += ROW_H;
                }
                for (int i = sectionStart; i < rows.size(); i++) {
                    rows.get(i).section = true;
                }
                rows.get(sectionStart).sectionFirst = true;
                rows.get(rows.size() - 1).sectionLast = true;
            }
            if (backups) {
                y += SECTION_GAP;
                y = buildBackupsPage(y);
            }
            y += SECTION_GAP;
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
        Text help = helpText(option);
        row.tooltip = help;
        int resetX = contentX() + contentW() - RESET_W;
        int valueX = resetX - 4 - VALUE_W;

        ClickableWidget value = createValueWidget(option, valueX, VALUE_W);
        if (help != null) {
            value.setTooltip(Tooltip.of(help));
        }
        row.widgets.add(value);
        addDrawableChild(value);

        if (option.type != SettingsRegistry.Type.ACTION) {
            String def = defaults.get(option.configKey);
            ButtonWidget reset = ButtonWidget.builder(
                    Text.translatable("ottoextra.settings.reset"), b -> {
                        if (def != null) {
                            session.reset(option);
                            rebuildContent();
                        }
                    }).dimensions(resetX, 0, RESET_W, 18).build();
            if (def != null) {
                reset.setTooltip(Tooltip.of(Text.translatable("ottoextra.settings.default",
                        defaultText(option, def))));
            }
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
                return ButtonWidget.builder(cycleLabel(o), btn -> {
                    SettingsSession.cycle(o);
                    btn.setMessage(cycleLabel(o));
                }).dimensions(x, 0, w, 18).build();
            }
            case ACTION -> {
                return ButtonWidget.builder(buttonText(o), btn -> o.action.run())
                        .dimensions(x, 0, w + 4 + RESET_W, 18).build();
            }
            case SLIDER -> {
                return new OptionSlider(x, w, 18, o);
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
                        f.setEditableColor(0xFFFF5555);
                    }
                });
                return f;
            }
        }
    }

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

        Row repairHead = new Row();
        repairHead.baseY = y;
        repairHead.header = true;
        repairHead.label = Text.translatable("ottoextra.settings.backups.rpRepair.head");
        repairHead.descLines = textRenderer.wrapLines(
                Text.translatable("ottoextra.settings.backups.rpRepair.desc")
                        .copy().formatted(Formatting.GRAY), contentW());
        repairHead.height = 14 + repairHead.descLines.size() * 10 + 4;
        rows.add(repairHead);
        y += repairHead.height;

        Row repairRow = new Row();
        repairRow.baseY = y;
        ButtonWidget repair = ButtonWidget.builder(
                Text.translatable("ottoextra.settings.backups.rpRepair.run"), b -> {
                    session.runRpRepair();
                    rebuildContent(true);
                }).dimensions(contentX(), 0, 210, 18).build();
        repairRow.widgets.add(repair);
        addDrawableChild(repair);
        rows.add(repairRow);
        y += ROW_H + 4;

        Row createRow = new Row();
        createRow.baseY = y;
        ButtonWidget create = ButtonWidget.builder(
                Text.translatable("ottoextra.settings.backups.create"), b -> {
                    session.createBackup();
                    rebuildContent(true);
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
            boolean confirming = entry.dir().equals(session.pendingRestore);
            Text btnLabel = confirming
                    ? Text.translatable("ottoextra.settings.backups.confirm")
                    .formatted(Formatting.RED)
                    : Text.translatable("ottoextra.settings.backups.restore");
            int bw = Math.max(120, textRenderer.getWidth(btnLabel) + 12);
            ButtonWidget restore = ButtonWidget.builder(btnLabel, b -> {
                rebuildContent(session.pressRestore(entry));
            }).dimensions(contentX() + contentW() - bw, 0, bw, 18).build();
            row.widgets.add(restore);
            addDrawableChild(restore);
            rows.add(row);
            y += ROW_H;
        }
        return y;
    }

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
        if (input.key() == GLFW.GLFW_KEY_S && input.hasCtrlOrCmd()) {
            session.saveAndClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int top = contentTop();
        int bottom = contentBottom();
        ctx.enableScissor(0, top, width, bottom);
        for (Row row : rows) {
            int ry = top + row.baseY - scroll;
            if (row.section && ry + row.height >= top && ry <= bottom) {
                drawSectionBackground(ctx, row, ry);
            }
        }
        ctx.disableScissor();

        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);

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

        int visible = bottom - top;
        if (contentHeight > visible) {
            int x = contentX() + contentW() + 6;
            int barH = Math.max(16, visible * visible / contentHeight);
            int barY = top + (visible - barH) * scroll
                    / Math.max(1, contentHeight - visible);
            ctx.fill(x, top, x + 4, bottom, 0x66000000);
            ctx.fill(x, barY, x + 4, barY + barH, 0xCC808080);
        }

        if (!session.statusMessage.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(session.statusMessage),
                    contentX(), bottom + 2, session.statusWarn ? COL_WARN : COL_STATUS);
        } else if (session.isDirty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.settings.dirty"),
                    footerCenter(), bottom + 2, COL_DESC);
        }
        if (!OttoExtraBackupService.isBackupOk()) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.settings.backup.missing"),
                    contentX(), 56, COL_WARN);
        }

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

        de.ottoextra.regions.RegionNotificationOverlay.render(ctx, null);
    }

    private void drawSectionBackground(DrawContext ctx, Row row, int rowY) {
        int left = contentX() - 6;
        int right = contentX() + contentW() + 2;
        int rowTop = rowY - (row.sectionFirst ? 2 : 0);
        int rowBottom = rowY + row.height + (row.sectionLast ? 2 : 0);
        ctx.fill(left, rowTop, right, rowBottom, COL_SECTION_BG);
        ctx.fill(left, rowTop, left + 2, rowBottom, COL_SECTION_ACCENT);
        ctx.fill(right - 1, rowTop, right, rowBottom, COL_SECTION_BORDER);
        if (row.sectionFirst) {
            ctx.fill(left, rowTop, right, rowTop + 1, COL_SECTION_BORDER);
        }
        if (row.sectionLast) {
            ctx.fill(left, rowBottom - 1, right, rowBottom, COL_SECTION_BORDER);
        }
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

            if (mx >= tabMenuX && mx <= tabMenuX + tabMenuW
                    && my >= tabMenuY && my < tabMenuY + SIDE_BTN_H) {
                return super.mouseClicked(click, doubled);
            }

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
