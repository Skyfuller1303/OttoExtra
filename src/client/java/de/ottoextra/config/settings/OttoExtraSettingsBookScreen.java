package de.ottoextra.config.settings;

import de.ottoextra.config.OttoExtraBackupService;
import de.ottoextra.ui.theme.InkCheckbox;
import de.ottoextra.ui.theme.InkTextField;
import de.ottoextra.ui.theme.OttonienTheme;
import de.ottoextra.ui.theme.SlateButton;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static de.ottoextra.config.settings.SettingsSession.buttonText;
import static de.ottoextra.config.settings.SettingsSession.cycleLabel;
import static de.ottoextra.config.settings.SettingsSession.defaultText;
import static de.ottoextra.config.settings.SettingsSession.helpText;
import static de.ottoextra.config.settings.SettingsSession.labelText;
import static de.ottoextra.config.settings.SettingsSession.matches;

/**
 * The settings as a book: categories on the dark-green book page, the settings of the selected
 * category on a parchment leaf beside it. Content never scrolls horizontally; long labels wrap.
 * Shares all state and behaviour with the classic view through {@link SettingsSession}.
 */
public final class OttoExtraSettingsBookScreen extends Screen {

    private static final int BOOK_X = 12;
    private static final int BOOK_Y = 30;
    private static final int LEAF_MAX_W = 430;
    private static final int HELP_H = 46;
    private static final int CTL_H = 12;
    private static final int RESET_SIZE = 10;
    /** Width reserved at the right of option rows for the reset tile. */
    private static final int RESET_COL = 18;
    private static final int ROW_MIN_H = 16;
    /** Space above the text and the control inside an option row. */
    private static final int ROW_PAD = 3;
    private static final int LINE_H = 10;

    private static final int COL_FOOT_WARN = 0xFFFF8A80;
    private static final int COL_FOOT_OK = 0xFF9FE08A;
    private static final int COL_FOOT_HINT = OttonienTheme.LABEL_MUTED;
    private static final int COL_INVALID = 0xFFFF5555;

    private record Box(int x, int y, int w, int h) {
        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record Chip(Box box, int page) {
    }

    private record Line(OrderedText text, int x, int dy, int color) {
    }

    /** One block of the leaf content; {@code y} and {@code h} are relative to the content top. */
    private static final class Row {
        int y;
        int h;
        final List<Line> lines = new ArrayList<>();
        final List<ClickableWidget> widgets = new ArrayList<>();
        SettingsRegistry.Option option;
        SettingsLayout.Level level = SettingsLayout.Level.BASIC;
        int crumbH;
        int ctlDy;
        int ctlRight;
        boolean dashed;
        Runnable onClick;
    }

    private final SettingsSession session;

    private List<SettingsModel.Cat> cats = List.of();
    private final List<Row> rows = new ArrayList<>();
    private final List<Box> ribbonBoxes = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();

    private boolean tooSmall;
    private InkTextField searchField;
    private String pendingQuery;
    private int catScroll;
    private int scroll;
    private int contentHeight;

    // geometry: book, book page, leaf and the leaf's content column
    private int bx;
    private int by;
    private int bw;
    private int bh;
    private int lx;
    private int ly;
    private int lw;
    private int lh;
    private int dx;
    private int dy;
    private int dw;
    private int dh;
    private int tx;
    private int innerW;
    private int cw;
    private int ct;
    private int cb;

    // leaf header
    private Text headTitle;
    private int headTitleY;
    private List<OrderedText> headDesc = List.of();
    private int headDescY;
    private int dividerY;

    private Row hoverRow;
    private SlateButton viewButton;
    private SlateButton discardButton;
    private SlateButton saveButton;

    OttoExtraSettingsBookScreen(SettingsSession session) {
        super(Text.translatable("ottoextra.settings.title"));
        this.session = session;
        session.owner = this;
        this.pendingQuery = session.query;
    }

    // ---- layout ----

    private void layout() {
        by = BOOK_Y;
        bw = width < 560 ? 168 : 190;
        bh = height - by - 26;
        int available = width - BOOK_X - bw - 8 - 10;
        dw = Math.min(LEAF_MAX_W, available);
        bx = BOOK_X;
        if (available > LEAF_MAX_W) {
            bx = (width - (bw + 8 + dw)) / 2;
        }
        lx = bx + 10;
        ly = by + 6;
        lw = bw - 16;
        lh = bh - 12;
        dx = bx + bw + 8;
        dy = by + 2;
        dh = bh - 4;
        tx = dx + 10;
        innerW = dw - 20;
        cw = innerW - 6;
        cb = dy + dh - HELP_H - 6;
    }

    private SettingsModel.Cat currentCat() {
        return cats.get(session.selectedModule);
    }

    private boolean onBackupsPage(SettingsModel.Cat cat) {
        return SettingsLayout.BACKUPS_CATEGORY.equals(cat.id())
                && session.selectedTab == cat.pages().size() - 1;
    }

    private int catRowH() {
        return lh >= 220 ? 16 : lh >= 200 ? 15 : 13;
    }

    private int catTop() {
        return ly + 27;
    }

    private int catVisible() {
        return Math.max(1, (ly + lh - 6 - catTop()) / catRowH());
    }

    private int catMaxScroll() {
        return Math.max(0, cats.size() - catVisible());
    }

    private void buildRibbonBoxes() {
        ribbonBoxes.clear();
        int x = bx + 18;
        for (int k = 0; k < 2; k++) {
            int w = textRenderer.getWidth(ribbonLabel(k)) + 10;
            int len = ribbonActive(k) ? 22 : 17;
            ribbonBoxes.add(new Box(x, by - len, w, len + 6));
            x += w + 4;
        }
    }

    private static Text ribbonLabel(int k) {
        return Text.translatable(k == 0 ? "ottoextra.settings.level.basic"
                : "ottoextra.settings.level.expert");
    }

    private static boolean ribbonActive(int k) {
        return (k == 1) == SettingsSession.expertMode;
    }

    // ---- init ----

    @Override
    protected void init() {
        if (searchField != null) {
            pendingQuery = searchField.getText();
        }
        if (!SettingsView.bookFits(width, height)) {
            // Too small for the book: hand the session over to the classic view on the next tick.
            tooSmall = true;
            return;
        }
        tooSmall = false;
        layout();
        cats = SettingsModel.build(session.registry, SettingsSession.expertMode);
        session.selectedModule = Math.max(0, Math.min(session.selectedModule, cats.size() - 1));
        catScroll = Math.max(0, Math.min(catScroll, catMaxScroll()));
        buildRibbonBoxes();

        String hint = Text.translatable("ottoextra.settings.search.book").getString();
        searchField = new InkTextField(textRenderer, lx + 9, ly + 7, lw - 18, 14,
                Text.translatable("ottoextra.settings.search.book"), true, 12);
        searchField.setMaxLength(64);
        searchField.setText(pendingQuery);
        searchField.setSuggestion(pendingQuery.isEmpty() ? hint : "");
        searchField.setChangedListener(q -> {
            searchField.setSuggestion(q.isEmpty() ? hint : "");
            scroll = 0;
            rebuild(false);
        });
        addDrawableChild(searchField);

        int fy = by + bh + 6;
        Text viewLabel = Text.translatable("ottoextra.settings.view.book");
        viewButton = new SlateButton(bx, fy, textRenderer.getWidth(viewLabel) + 14, 16, viewLabel,
                b -> switchToClassic());
        viewButton.setTooltip(Tooltip.of(Text.translatable("ottoextra.settings.view.tooltip")));
        addDrawableChild(viewButton);

        Text saveLabel = Text.translatable("ottoextra.settings.saveQuit");
        Text discardLabel = Text.translatable("ottoextra.settings.discard");
        int saveW = textRenderer.getWidth(saveLabel) + 14;
        int discardW = textRenderer.getWidth(discardLabel) + 14;
        int right = dx + dw;
        saveButton = new SlateButton(right - saveW, fy, saveW, 16, saveLabel,
                b -> session.saveAndClose());
        discardButton = new SlateButton(right - saveW - 4 - discardW, fy, discardW, 16,
                discardLabel, b -> session.discardAndClose());
        addDrawableChild(discardButton);
        addDrawableChild(saveButton);

        rebuild(false);
    }

    private void rememberState() {
        session.query = searchField != null ? searchField.getText() : pendingQuery;
    }

    private void switchToClassic() {
        rememberState();
        session.switchView(SettingsView.CLASSIC);
    }

    @Override
    public void tick() {
        if (tooSmall) {
            tooSmall = false;
            rememberState();
            session.openView(SettingsView.CLASSIC);
        }
    }

    @Override
    public void close() {
        session.saveAndClose();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---- content ----

    private void rebuild(boolean keepStatus) {
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
        boolean searching = !query.isBlank();
        SettingsModel.Cat cat = cats.isEmpty() ? null : currentCat();
        if (cat != null && session.selectedTab >= cat.pages().size()) {
            session.selectedTab = 0;
        }
        layoutHeader(searching ? null : cat);

        int y = 0;
        if (searching) {
            y = buildSearch(query.toLowerCase(Locale.ROOT).trim());
        } else if (cat != null) {
            y = buildCategory(cat);
        }
        contentHeight = y;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (cb - ct))));
        layoutRows();
    }

    /** Title, description, page chips and divider of the leaf; null cat = search mode. */
    private void layoutHeader(SettingsModel.Cat cat) {
        chips.clear();
        headTitle = null;
        headDesc = List.of();
        if (cat == null) {
            ct = dy + 9;
            dividerY = -1;
            return;
        }
        SettingsModel.Pg page = cat.pages().get(session.selectedTab);
        boolean empty = page.sections().isEmpty() && !onBackupsPage(cat);
        int y = dy + 9;
        headTitleY = y;
        headTitle = Text.translatable(cat.titleKey()).formatted(Formatting.BOLD);
        if (cat.pages().size() > 1) {
            int total = 0;
            for (SettingsModel.Pg p : cat.pages()) {
                total += textRenderer.getWidth(Text.translatable(p.titleKey())) + 8 + 3;
            }
            total -= 3;
            boolean ownRow = total + textRenderer.getWidth(headTitle) + 12 > innerW;
            int chipY = ownRow ? y + 11 : y;
            int x = tx + innerW - total;
            for (int i = 0; i < cat.pages().size(); i++) {
                int w = textRenderer.getWidth(Text.translatable(cat.pages().get(i).titleKey())) + 8;
                chips.add(new Chip(new Box(x, chipY - 2, w, 12), i));
                x += w + 3;
            }
            if (ownRow) {
                y += 13;
            }
        }
        y += 11;
        headDescY = y;
        headDesc = textRenderer.wrapLines(Text.translatable(
                empty ? "ottoextra.settings.empty" : cat.descKey()), innerW);
        y += headDesc.size() * LINE_H + 4;
        dividerY = y;
        ct = y + 4;
    }

    private int buildSearch(String q) {
        int y = 0;
        int hits = 0;
        for (SettingsModel.Cat cat : session.allCats) {
            for (SettingsModel.Pg pg : cat.pages()) {
                for (SettingsModel.Sec sec : pg.sections()) {
                    for (SettingsModel.Item item : sec.items()) {
                        SettingsRegistry.Option o = item.option();
                        if (!matches(o, q)) {
                            continue;
                        }
                        String path = Text.translatable(cat.titleKey()).getString()
                                + (cat.other() ? "" : " › "
                                + Text.translatable(sec.titleKey()).getString());
                        y = addOption(y, item, path);
                        hits++;
                    }
                }
            }
        }
        if (hits == 0) {
            y = addTextRow(y, Text.translatable("ottoextra.settings.search.none"));
        }
        return y;
    }

    private int buildCategory(SettingsModel.Cat cat) {
        SettingsModel.Pg page = cat.pages().get(session.selectedTab);
        boolean backups = onBackupsPage(cat);
        int y = 0;
        for (SettingsModel.Sec sec : page.sections()) {
            y = addSectionHead(y, Text.translatable(sec.titleKey()), Text.translatable(sec.descKey()));
            String stateKey = cat.id() + "/" + sec.titleKey();
            boolean open = SettingsModel.isExpanded(sec.items(), session.expanded.get(stateKey));
            SettingsModel.Visible vis = SettingsModel.visible(sec.items(),
                    SettingsSession.expertMode, open);
            for (SettingsModel.Item item : vis.shown()) {
                y = addOption(y, item, null);
            }
            if (vis.toggle()) {
                Row toggle = new Row();
                toggle.y = y;
                toggle.h = ROW_MIN_H;
                toggle.dashed = true;
                Text label = vis.expanded()
                        ? Text.translatable("ottoextra.settings.lessOptions")
                        : Text.translatable("ottoextra.settings.moreOptions", vis.hidden());
                toggle.lines.add(new Line(label.asOrderedText(), 13, 3, OttonienTheme.BRASS));
                toggle.onClick = () -> {
                    session.expanded.put(stateKey, !vis.expanded());
                    rebuild(false);
                };
                rows.add(toggle);
                y += ROW_MIN_H;
            }
            y += 10;
        }
        if (backups) {
            y = buildBackups(y);
        }
        return y;
    }

    private int addSectionHead(int y, Text title, Text desc) {
        Row row = new Row();
        row.y = y;
        row.lines.add(new Line(title.copy().formatted(Formatting.BOLD).asOrderedText(), 0, 0,
                OttonienTheme.TERRACOTTA));
        List<OrderedText> lines = textRenderer.wrapLines(desc, cw - 2);
        for (int i = 0; i < lines.size(); i++) {
            row.lines.add(new Line(lines.get(i), 0, 11 + i * LINE_H, OttonienTheme.INK_MUTED));
        }
        row.h = 11 + lines.size() * LINE_H + 4;
        rows.add(row);
        return y + row.h;
    }

    private int addTextRow(int y, Text text) {
        Row row = new Row();
        row.y = y;
        row.h = ROW_MIN_H;
        row.lines.add(new Line(text.asOrderedText(), 2, 1, OttonienTheme.INK_MUTED));
        rows.add(row);
        return y + row.h;
    }

    private int controlWidth(SettingsRegistry.Option o) {
        return switch (o.type) {
            case BOOL -> 0;
            case ACTION -> Math.min(cw / 2, textRenderer.getWidth(buttonText(o)) + 10);
            case CYCLE -> {
                int max = 0;
                for (String v : o.cycleValues) {
                    max = Math.max(max, textRenderer.getWidth(cycleLabel(o, v)));
                }
                yield Math.min(96, max + 18);
            }
            case COLOR -> 78;
            case STRING, COMMAND -> 70;
            case SLIDER -> 80;
            default -> 36;
        };
    }

    private int addOption(int y, SettingsModel.Item item, String crumb) {
        SettingsRegistry.Option o = item.option();
        Row row = new Row();
        row.y = y;
        row.option = o;
        row.level = item.level();
        row.crumbH = crumb == null ? 0 : LINE_H;
        if (crumb != null) {
            row.lines.add(new Line(Text.literal(crumb).asOrderedText(), 2, 0,
                    OttonienTheme.INK_MUTED));
        }
        boolean bool = o.type == SettingsRegistry.Type.BOOL;
        boolean action = o.type == SettingsRegistry.Type.ACTION;
        int ctlW = controlWidth(o);
        int labX = bool ? 13 : 2;
        int ctlRight = cw - 6 - (action ? 0 : RESET_COL);
        int ctlLeft = ctlRight - ctlW;
        int labelMax = Math.max(40, bool ? cw - labX - RESET_COL - 4 : ctlLeft - 8 - labX);
        List<OrderedText> label = textRenderer.wrapLines(labelText(o), labelMax);
        for (int i = 0; i < label.size(); i++) {
            row.lines.add(new Line(label.get(i), labX, row.crumbH + ROW_PAD + i * LINE_H,
                    OttonienTheme.INK));
        }
        row.h = row.crumbH + Math.max(ROW_MIN_H, label.size() * LINE_H + 2 * ROW_PAD);
        row.ctlRight = ctlRight;
        row.ctlDy = ROW_PAD - 1;

        ClickableWidget control = createControl(o, tx + ctlLeft, ctlW, labX + labelMax);
        if (control != null) {
            row.widgets.add(control);
            addDrawableChild(control);
        }
        rows.add(row);
        return y + row.h;
    }

    private ClickableWidget createControl(SettingsRegistry.Option o, int x, int w, int boolWidth) {
        switch (o.type) {
            case BOOL -> {
                InkCheckbox box = InkCheckbox.builder(Text.empty(), textRenderer)
                        .pos(tx + 1, 0)
                        .checked(Boolean.parseBoolean(o.get.get()))
                        .callback((b, on) -> o.set.accept(String.valueOf(on)))
                        .build();
                // The label is drawn by the row; let a click on its first line toggle too.
                box.setWidth(boolWidth - 1);
                return box;
            }
            case CYCLE -> {
                return new CycleTile(x, w, CTL_H, o, textRenderer);
            }
            case ACTION -> {
                return new SlateButton(x, 0, w, CTL_H, buttonText(o), b -> o.action.run());
            }
            case SLIDER -> {
                return new OptionSlider(x, w, CTL_H, o);
            }
            default -> {
                int fieldW = o.type == SettingsRegistry.Type.COLOR ? w - 16 : w;
                InkTextField f = new InkTextField(textRenderer, x, 0, fieldW, CTL_H, Text.empty(),
                        true);
                f.setMaxLength(256);
                String value = o.get.get();
                f.setText(o.type == SettingsRegistry.Type.COMMAND && !value.isEmpty()
                        ? "/" + value : value);
                f.setChangedListener(text -> {
                    try {
                        o.set.accept(text);
                        f.setEditableColor(OttonienTheme.LABEL);
                    } catch (Exception invalid) {
                        f.setEditableColor(COL_INVALID);
                    }
                });
                return f;
            }
        }
    }

    // ---- backups ----

    private int buildBackups(int y) {
        y = addSectionHead(y, Text.translatable("ottoextra.settings.backups.head"),
                Text.translatable("ottoextra.settings.backups.desc"));
        y = addSectionHead(y, Text.translatable("ottoextra.settings.backups.rpRepair.head"),
                Text.translatable("ottoextra.settings.backups.rpRepair.desc"));
        y = addButtonRow(y, Text.translatable("ottoextra.settings.backups.rpRepair.run"), false,
                () -> {
                    session.runRpRepair();
                    rebuild(true);
                });
        y = addButtonRow(y, Text.translatable("ottoextra.settings.backups.create"), false, () -> {
            session.createBackup();
            rebuild(true);
        });
        for (OttoExtraBackupService.BackupEntry entry : OttoExtraBackupService.listBackups()) {
            Row row = new Row();
            row.y = y;
            row.h = ROW_MIN_H + 2;
            Text label = Text.literal(entry.name() + "  ").append(
                    Text.translatable("ottoextra.settings.backups.files", entry.files()));
            row.lines.add(new Line(label.asOrderedText(), 2, 3, OttonienTheme.INK));
            boolean confirming = entry.dir().equals(session.pendingRestore);
            Text btn = Text.translatable(confirming ? "ottoextra.settings.backups.confirm"
                    : "ottoextra.settings.backups.restore");
            int bwid = textRenderer.getWidth(btn) + 14;
            SlateButton restore = new SlateButton(tx + cw - 6 - bwid, 0, bwid, CTL_H + 1, btn,
                    b -> rebuild(session.pressRestore(entry)));
            if (confirming) {
                restore.danger();
            }
            row.widgets.add(restore);
            addDrawableChild(restore);
            row.ctlDy = 1;
            rows.add(row);
            y += row.h;
        }
        return y + 6;
    }

    private int addButtonRow(int y, Text label, boolean danger, Runnable action) {
        Row row = new Row();
        row.y = y;
        row.h = ROW_MIN_H + 4;
        SlateButton b = new SlateButton(tx + 2, 0, textRenderer.getWidth(label) + 16, CTL_H + 1,
                label, btn -> action.run());
        if (danger) {
            b.danger();
        }
        row.widgets.add(b);
        addDrawableChild(b);
        row.ctlDy = 1;
        rows.add(row);
        return y + row.h;
    }

    // ---- scrolling and hit testing ----

    private void layoutRows() {
        for (Row row : rows) {
            int ry = ct + row.y - scroll;
            for (ClickableWidget w : row.widgets) {
                int wy = ry + row.crumbH + row.ctlDy;
                w.setY(wy);
                w.visible = wy >= ct && wy + w.getHeight() <= cb;
            }
        }
    }

    private boolean inViewport(double mx, double my) {
        return mx >= tx && mx < tx + cw && my >= ct && my < cb;
    }

    private Row rowAt(double mx, double my) {
        if (!inViewport(mx, my)) {
            return null;
        }
        for (Row row : rows) {
            int ry = ct + row.y - scroll;
            if (my >= ry && my < ry + row.h) {
                return row;
            }
        }
        return null;
    }

    private Box resetBox(Row row) {
        int ry = ct + row.y - scroll;
        return new Box(tx + cw - 14, ry + row.crumbH + ROW_PAD, RESET_SIZE, RESET_SIZE);
    }

    private boolean resettable(Row row) {
        return row.option != null && row.option.type != SettingsRegistry.Type.ACTION
                && !session.isDefault(row.option);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal,
                                 double vertical) {
        if (tooSmall) {
            return false;
        }
        if (mouseX >= dx && mouseX < dx + dw && mouseY >= dy && mouseY < dy + dh) {
            int max = Math.max(0, contentHeight - (cb - ct));
            scroll = Math.max(0, Math.min(scroll - (int) (vertical * 16), max));
            layoutRows();
            return true;
        }
        if (mouseX >= lx && mouseX < lx + lw && mouseY >= ly && mouseY < ly + lh) {
            catScroll = Math.max(0, Math.min(catScroll - (int) Math.signum(vertical),
                    catMaxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (tooSmall) {
            return false;
        }
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0) {
            for (int k = 0; k < ribbonBoxes.size(); k++) {
                if (ribbonBoxes.get(k).has(mx, my)) {
                    boolean expert = k == 1;
                    if (expert != SettingsSession.expertMode) {
                        SettingsSession.expertMode = expert;
                        clearAndInit();
                    }
                    return true;
                }
            }
            int visible = catVisible();
            if (mx >= lx + 5 && mx < lx + lw - 5 && my >= catTop()
                    && my < catTop() + visible * catRowH()) {
                int idx = catScroll + (int) ((my - catTop()) / catRowH());
                if (idx < cats.size()) {
                    selectCategory(idx);
                    return true;
                }
            }
            for (Chip chip : chips) {
                if (chip.box().has(mx, my)) {
                    session.selectedTab = chip.page();
                    scroll = 0;
                    rebuild(false);
                    return true;
                }
            }
            Row row = rowAt(mx, my);
            if (row != null) {
                if (resettable(row) && resetBox(row).has(mx, my)) {
                    session.reset(row.option);
                    rebuild(false);
                    return true;
                }
                if (row.onClick != null) {
                    row.onClick.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void selectCategory(int idx) {
        session.selectedModule = idx;
        session.selectedTab = 0;
        scroll = 0;
        if (searchField != null && !searchField.getText().isEmpty()) {
            searchField.setText("");   // the change listener rebuilds
        } else {
            rebuild(false);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_S && input.hasCtrlOrCmd()) {
            session.saveAndClose();
            return true;
        }
        return super.keyPressed(input);
    }

    // ---- rendering ----

    private void ink(DrawContext ctx, Text text, int x, int y, int color) {
        ctx.drawText(textRenderer, text, x, y, color, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (tooSmall) {
            return;
        }
        drawBook(ctx);
        drawLeaf(ctx);
        hoverRow = rowAt(mouseX, mouseY);
        drawContent(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);

        OttonienTheme.pictoSearch(ctx, searchField.getX() - 12, searchField.getY() + 3,
                OttonienTheme.LABEL_MUTED);
        drawScrollBar(ctx);
        drawHelp(ctx);
        drawFooterText(ctx);
        drawTooltips(ctx, mouseX, mouseY);

        de.ottoextra.regions.RegionNotificationOverlay.render(ctx, null);
    }

    private void drawBook(DrawContext ctx) {
        OttonienTheme.greenBook(ctx, bx, by, bw, bh);
        OttonienTheme.parchment(ctx, lx, ly, lw, lh, 47, true, true);
        for (int i = 0; i < 4; i++) {
            ctx.fill(lx + i, ly + 1, lx + i + 1, ly + lh - 1,
                    OttonienTheme.shade(OttonienTheme.PA, .88 + i * .03));
        }
        for (int k = 0; k < ribbonBoxes.size(); k++) {
            boolean on = ribbonActive(k);
            Box box = ribbonBoxes.get(k);
            int len = on ? 22 : 17;
            int color = k == 0 ? OttonienTheme.TERRACOTTA : OttonienTheme.BRASS;
            OttonienTheme.ribbon(ctx, box.x(), by - len, by + 6, box.w(),
                    on ? color : OttonienTheme.shade(color, .85));
            ctx.drawText(textRenderer, ribbonLabel(k), box.x() + 5, by - len + 5,
                    on ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
        }
        drawCategories(ctx);
    }

    private void drawCategories(DrawContext ctx) {
        int rowH = catRowH();
        int top = catTop();
        int visible = catVisible();
        for (int r = 0; r < visible; r++) {
            int i = catScroll + r;
            if (i >= cats.size()) {
                break;
            }
            SettingsModel.Cat cat = cats.get(i);
            int yy = top + r * rowH;
            boolean selected = i == session.selectedModule;
            Text name = Text.translatable(cat.titleKey());
            if (selected) {
                OttonienTheme.rect(ctx, lx + 5, yy - 2, lx + lw - 5, yy + rowH - 3, OttonienTheme.PA_L);
                OttonienTheme.rect(ctx, lx + 5, yy - 2, lx + 7, yy + rowH - 3,
                        OttonienTheme.TERRACOTTA);
                name = name.copy().formatted(Formatting.BOLD);
            }
            int count = settingCount(cat);
            String n = String.valueOf(count);
            int nameW = textRenderer.getWidth(name);
            ink(ctx, name, lx + 11, yy, OttonienTheme.INK);
            if (count > 0 && lx + 13 + nameW < lx + lw - 12 - textRenderer.getWidth(n)) {
                ctx.drawText(textRenderer, n, lx + lw - 9 - textRenderer.getWidth(n), yy,
                        OttonienTheme.INK_MUTED, false);
            }
        }
        if (catMaxScroll() > 0) {
            int trackH = visible * rowH;
            int barH = Math.max(10, trackH * visible / cats.size());
            int barY = top - 2 + (trackH - barH) * catScroll / catMaxScroll();
            OttonienTheme.rect(ctx, lx + lw - 4, barY, lx + lw - 2, barY + barH,
                    OttonienTheme.INK_MUTED);
        }
    }

    private static int settingCount(SettingsModel.Cat cat) {
        int n = 0;
        for (SettingsModel.Pg pg : cat.pages()) {
            for (SettingsModel.Sec sec : pg.sections()) {
                n += sec.items().size();
            }
        }
        return n;
    }

    private void drawLeaf(DrawContext ctx) {
        OttonienTheme.leaf(ctx, dx, dy, dw, dh, 52, false);
        if (headTitle != null) {
            ink(ctx, headTitle, tx, headTitleY, OttonienTheme.INK);
            for (Chip chip : chips) {
                boolean on = chip.page() == session.selectedTab;
                Box b = chip.box();
                OttonienTheme.rect(ctx, b.x(), b.y(), b.x() + b.w(), b.y() + b.h(),
                        on ? OttonienTheme.PA_D : OttonienTheme.PA_S);
                OttonienTheme.rect(ctx, b.x() + 1, b.y() + 1, b.x() + b.w() - 1,
                        b.y() + b.h() - 1, on ? OttonienTheme.PA_L : OttonienTheme.PA);
                SettingsModel.Cat cat = currentCat();
                ink(ctx, Text.translatable(cat.pages().get(chip.page()).titleKey()), b.x() + 4,
                        b.y() + 2, on ? OttonienTheme.INK : OttonienTheme.INK_MUTED);
            }
            for (int i = 0; i < headDesc.size(); i++) {
                ctx.drawText(textRenderer, headDesc.get(i), tx, headDescY + i * LINE_H,
                        OttonienTheme.INK_MUTED, false);
            }
            OttonienTheme.rect(ctx, tx, dividerY, tx + innerW, dividerY + 1, OttonienTheme.PA_S);
        }
        int helpY = dy + dh - HELP_H - 2;
        OttonienTheme.rect(ctx, tx, helpY, tx + innerW, helpY + 1, OttonienTheme.PA_S);
    }

    private void drawContent(DrawContext ctx, int mouseX, int mouseY) {
        ctx.enableScissor(dx + 1, ct, dx + dw - 1, cb);
        for (Row row : rows) {
            int ry = ct + row.y - scroll;
            if (ry + row.h < ct || ry > cb) {
                continue;
            }
            if (row == hoverRow && (row.option != null || row.onClick != null)) {
                OttonienTheme.rect(ctx, tx, ry, tx + cw, ry + row.h, OttonienTheme.PA_L);
            }
            if (row.dashed) {
                OttonienTheme.dashedLine(ctx, tx + 13, ry, cw - 19, OttonienTheme.PA_S);
            }
            for (Line line : row.lines) {
                ctx.drawText(textRenderer, line.text(), tx + line.x(), ry + line.dy(),
                        line.color(), false);
            }
            if (row.option == null) {
                continue;
            }
            if (row.level == SettingsLayout.Level.EXPERT) {
                OttonienTheme.rect(ctx, tx + cw - 3, ry + row.crumbH + ROW_PAD + 1, tx + cw - 1,
                        ry + row.crumbH + ROW_PAD + 5, OttonienTheme.BRASS);
            }
            if (row.option.type == SettingsRegistry.Type.COLOR) {
                drawSwatch(ctx, row, tx + row.ctlRight - 13, ry + row.crumbH + row.ctlDy);
            }
            if (resettable(row)) {
                Box b = resetBox(row);
                OttonienTheme.tile(ctx, b.x(), b.y(), b.w(), b.h(), b.has(mouseX, mouseY));
                OttonienTheme.pictoLeft(ctx, b.x() + 1, b.y(), OttonienTheme.PICTO);
            }
        }
        ctx.disableScissor();
    }

    private void drawSwatch(DrawContext ctx, Row row, int x, int y) {
        String v = row.option.get.get();
        String hex = v == null ? "" : v.replace("#", "").trim();
        int color = OttonienTheme.PA_S;
        if (hex.matches("[0-9a-fA-F]{6}")) {
            color = 0xFF000000 | Integer.parseInt(hex, 16);
        }
        OttonienTheme.rect(ctx, x, y, x + 13, y + CTL_H, 0xFF000000);
        OttonienTheme.rect(ctx, x + 1, y + 1, x + 12, y + CTL_H - 1, color);
    }

    private void drawScrollBar(DrawContext ctx) {
        int view = cb - ct;
        if (view <= 0 || contentHeight <= view) {
            return;
        }
        int sx = dx + dw - 9;
        OttonienTheme.rect(ctx, sx, ct, sx + 3, cb, OttonienTheme.PA_S);
        int barH = Math.max(12, view * view / contentHeight);
        int barY = ct + (view - barH) * scroll / Math.max(1, contentHeight - view);
        OttonienTheme.rect(ctx, sx, barY, sx + 3, barY + barH, OttonienTheme.PA_D);
    }

    // ---- help strip ----

    /** Greedy word wrap by pixel width. */
    private List<String> wrapPlain(String text, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() == 0 || textRenderer.getWidth(candidate) <= maxW) {
                cur = new StringBuilder(candidate);
            } else {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    private String withEllipsis(String line, int maxW) {
        String s = line;
        while (!s.isEmpty() && textRenderer.getWidth(s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s.stripTrailing() + "…";
    }

    /** Help lines for the hovered row, at most three; the last ends in an ellipsis when cut. */
    private List<String> helpLines(SettingsRegistry.Option o) {
        Text help = helpText(o);
        if (help == null) {
            return List.of();
        }
        List<String> lines = wrapPlain(help.getString(), innerW);
        if (lines.size() > 3) {
            lines = new ArrayList<>(lines.subList(0, 3));
            lines.set(2, withEllipsis(lines.get(2), innerW));
        }
        return lines;
    }

    private boolean helpCut(SettingsRegistry.Option o) {
        Text help = helpText(o);
        return help != null && wrapPlain(help.getString(), innerW).size() > 3;
    }

    private void drawHelp(DrawContext ctx) {
        int hy = dy + dh - HELP_H - 2;
        if (hoverRow != null && hoverRow.option != null) {
            SettingsRegistry.Option o = hoverRow.option;
            String label = labelText(o).getString();
            Text bold = Text.literal(label).formatted(Formatting.BOLD);
            while (label.length() > 1 && textRenderer.getWidth(bold) > innerW) {
                label = label.substring(0, label.length() - 1);
                bold = Text.literal(label + "…").formatted(Formatting.BOLD);
            }
            ink(ctx, bold, tx, hy + 4, OttonienTheme.INK);
            List<String> lines = helpLines(o);
            for (int i = 0; i < lines.size(); i++) {
                ctx.drawText(textRenderer, lines.get(i), tx, hy + 15 + i * LINE_H,
                        OttonienTheme.INK_MUTED, false);
            }
        } else {
            ink(ctx, Text.translatable("ottoextra.settings.help.hint1"), tx, hy + 4,
                    OttonienTheme.INK_MUTED);
            ink(ctx, Text.translatable("ottoextra.settings.help.hint2"), tx, hy + 14,
                    OttonienTheme.INK_MUTED);
        }
    }

    private void drawTooltips(DrawContext ctx, int mouseX, int mouseY) {
        Row row = hoverRow;
        if (row == null || row.option == null) {
            return;
        }
        if (resettable(row) && resetBox(row).has(mouseX, mouseY)) {
            String def = session.defaults.get(row.option.configKey);
            ctx.drawTooltip(textRenderer, Text.translatable("ottoextra.settings.reset.tooltip",
                    defaultText(row.option, def)), mouseX, mouseY);
        } else if (helpCut(row.option)) {
            List<Text> lines = new ArrayList<>();
            for (String l : wrapPlain(helpText(row.option).getString(), 200)) {
                lines.add(Text.literal(l));
            }
            ctx.drawTooltip(textRenderer, lines, mouseX, mouseY);
        }
    }

    // ---- footer ----

    private void drawFooterText(DrawContext ctx) {
        int left = viewButton.getX() + viewButton.getWidth() + 8;
        int maxW = discardButton.getX() - 8 - left;
        if (maxW < 40) {
            return;
        }
        List<Text> texts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (!session.statusMessage.isEmpty()) {
            texts.add(Text.literal(session.statusMessage));
            colors.add(session.statusWarn ? COL_FOOT_WARN : COL_FOOT_OK);
        } else if (!OttoExtraBackupService.isBackupOk()) {
            texts.add(Text.translatable("ottoextra.settings.backup.missing"));
            colors.add(COL_FOOT_WARN);
        }
        if (session.isDirty()) {
            texts.add(Text.translatable("ottoextra.settings.dirty"));
            colors.add(COL_FOOT_HINT);
        }
        int y = by + bh + 6 + 8 - (texts.size() * 10 - 2) / 2;
        for (int i = 0; i < texts.size(); i++) {
            String s = texts.get(i).getString();
            if (textRenderer.getWidth(s) > maxW) {
                s = withEllipsis(s, maxW);
            }
            ctx.drawText(textRenderer, s, left, y + i * 10, colors.get(i), false);
        }
    }

    // ---- cycle tile ----

    /** Parchment tile that shows the translated value of a cycle option; a click cycles it. */
    private static final class CycleTile extends PressableWidget {
        private final SettingsRegistry.Option option;
        private final TextRenderer font;

        CycleTile(int x, int w, int h, SettingsRegistry.Option option, TextRenderer font) {
            super(x, 0, w, h, cycleLabel(option));
            this.option = option;
            this.font = font;
        }

        @Override
        public void onPress(AbstractInput input) {
            SettingsSession.cycle(option);
            setMessage(cycleLabel(option));
        }

        @Override
        protected void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            OttonienTheme.tile(ctx, x, y, getWidth(), getHeight(), isHovered());
            String value = getMessage().getString();
            int max = getWidth() - 16;
            while (value.length() > 1 && font.getWidth(value) > max) {
                value = value.substring(0, value.length() - 2) + "…";
            }
            ctx.drawText(font, value, x + 4, y + (getHeight() - 8) / 2, OttonienTheme.INK, false);
            OttonienTheme.pictoDown(ctx, x + getWidth() - 10, y + (getHeight() - 3) / 2,
                    OttonienTheme.PICTO);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
