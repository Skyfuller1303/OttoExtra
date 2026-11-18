package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.theme.OttonienTheme;
import de.ottoextra.tacticalfactions.theme.SlateButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared shell of the blue-grey Politik-Buch: binding, top ribbons (Politik / Trupp / Kampf),
 * optional side ribbons, left page, optional right leaf and the "Fertig" button. The layout
 * follows the OttoExtra people book; tabs are separate screens that share this shell.
 */
abstract class BookScreen extends Screen {
    enum Tab {
        POLITIK("ottoextra_tacticalfactions.tab.politics", OttonienTheme.TERRACOTTA),
        TRUPP("ottoextra_tacticalfactions.tab.squads", OttonienTheme.BRASS),
        KAMPF("ottoextra_tacticalfactions.tab.combat", OttonienTheme.WAX_RED);

        final String key;
        final int color;

        Tab(String key, int color) {
            this.key = key;
            this.color = color;
        }
    }

    record Box(int x, int y, int w, int h) {
        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    record SideDef(String id, Text label, int color) { }

    private record SideBox(SideDef def, Box box, int x0, int x1) { }

    static final int BOOK_W = 182;
    static final int WIDE_W = 380;
    static final int LEAF_W = 186;
    private static final int RIBBON_TOP = 24;

    protected final Screen parent;
    protected final Tab tab;

    // Geometry, valid after layoutBook().
    int bx;
    int by;
    int bookW = BOOK_W;
    int bh = 200;
    int lfx;
    int lfy;
    int lfw = LEAF_W;
    int lfh = 190;
    boolean narrow;
    boolean leafOverlay;
    private boolean sideShown;
    private int compLeft;
    private int compRight;

    private final List<Box> topBoxes = new ArrayList<>();
    private final List<SideBox> sideBoxes = new ArrayList<>();
    private final List<ClickableWidget> pageWidgets = new ArrayList<>();
    private final List<ClickableWidget> leafWidgets = new ArrayList<>();
    private SlateButton backButton;

    BookScreen(Screen parent, Tab tab, Text title) {
        super(title);
        this.parent = parent;
        this.tab = tab;
    }

    // ---- hooks ----

    /** True for the wide book without a right leaf (diplomacy board). */
    boolean wide() { return false; }

    boolean hasLeaf() { return !wide(); }

    List<SideDef> sideDefs() { return List.of(); }

    String activeSide() { return ""; }

    void onSide(String id) { }

    /** Create widgets via {@link #page} / {@link #leaf}. Called from init() after the layout. */
    abstract void initContent();

    void drawPage(DrawContext ctx, int mouseX, int mouseY) { }

    void drawLeaf(DrawContext ctx, int mouseX, int mouseY) { }

    void drawOver(DrawContext ctx, int mouseX, int mouseY) { }

    void tickContent() { }

    /** Height of the leaf; tabs with little content may use a shorter one. */
    int leafHeight() { return lfh; }

    /** Called after widget visibility was applied; tabs may hide individual widgets here. */
    void applyExtraVisibility() { }

    void beforeLeave() { }

    // ---- geometry helpers ----

    int pageX() { return bx + 10; }
    int pageY() { return by + 6; }
    int pageW() { return bookW - 16; }
    int pageH() { return bh - 12; }

    boolean pageVisible() { return !(narrow && leafOverlay); }

    boolean leafVisible() { return hasLeaf() && (!narrow || leafOverlay); }

    <T extends ClickableWidget> T page(T widget) {
        pageWidgets.add(widget);
        return addDrawableChild(widget);
    }

    <T extends ClickableWidget> T leaf(T widget) {
        leafWidgets.add(widget);
        return addDrawableChild(widget);
    }

    void applyVisibility() {
        boolean page = pageVisible();
        boolean leaf = leafVisible();
        pageWidgets.forEach(widget -> widget.visible = page);
        leafWidgets.forEach(widget -> widget.visible = leaf);
        if (backButton != null) backButton.visible = narrow && leafOverlay && hasLeaf();
        applyExtraVisibility();
    }

    /** Narrow windows show either the page or the leaf; selecting an entry flips to the leaf. */
    void showLeafIfNarrow() {
        if (narrow && hasLeaf()) {
            leafOverlay = true;
            applyVisibility();
        }
    }

    // ---- layout ----

    private int sideExtent() {
        int max = 0;
        for (SideDef def : sideDefs()) max = Math.max(max, textRenderer.getWidth(def.label()) + 20);
        return max;
    }

    private void layoutBook() {
        int margin = 4;
        narrow = false;
        boolean leaf = hasLeaf();
        int sideExt = sideExtent();
        int leafW = LEAF_W;
        int bookWidth = wide() ? Math.min(WIDE_W, Math.max(260, width - sideExt - 2 * margin)) : BOOK_W;
        int leafPart = leaf ? 8 + leafW : 0;
        int need = sideExt + bookWidth + leafPart + 2 * margin;
        if (need > width && leaf) {
            leafW = Math.max(150, leafW - (need - width));
            leafPart = 8 + leafW;
            need = sideExt + bookWidth + leafPart + 2 * margin;
        }
        if (need > width && leaf) {
            bookWidth = Math.max(156, bookWidth - (need - width));
            need = sideExt + bookWidth + leafPart + 2 * margin;
        }
        if (need > width && sideExt > 0) {
            sideExt = 0;
            need = bookWidth + leafPart + 2 * margin;
        }
        if (need > width) {
            if (leaf) {
                narrow = true;
                leafW = Math.min(leafW, Math.max(120, width - 2 * margin));
                bookWidth = Math.min(BOOK_W, Math.max(150, width - 2 * margin - 2));
                leafPart = 0;
                need = bookWidth;
            } else {
                bookWidth = Math.max(200, width - 2 * margin);
                need = bookWidth;
            }
        }
        sideShown = sideExt > 0;
        bookW = bookWidth;
        lfw = leafW;
        bh = Math.max(110, Math.min(204, height - RIBBON_TOP - 28));
        by = RIBBON_TOP + Math.max(0, (height - RIBBON_TOP - 28 - bh) / 2);
        int startX = (width - need) / 2;
        bx = narrow ? (width - bookW) / 2 : startX + sideExt;
        lfh = Math.min(200, bh - 4);
        lfy = by + 2;
        lfx = narrow ? bx + (bookW - lfw) / 2 : bx + bookW + 8;
        if (!narrow) leafOverlay = false;
        compLeft = narrow ? bx : bx - sideExt;
        compRight = narrow || !leaf ? bx + bookW : lfx + lfw;
        buildRibbonBoxes();
    }

    private void buildRibbonBoxes() {
        topBoxes.clear();
        int x = bx + 18;
        for (Tab t : Tab.values()) {
            int w = textRenderer.getWidth(Text.translatable(t.key)) + 10;
            int len = t == tab ? 22 : 17;
            topBoxes.add(new Box(x, by - len, w, len + 6));
            x += w + 4;
        }
        sideBoxes.clear();
        if (!sideShown) return;
        int k = 0;
        for (SideDef def : sideDefs()) {
            boolean active = def.id().equals(activeSide());
            int w = textRenderer.getWidth(def.label()) + 15 + (active ? 5 : 0);
            int y = by + 16 + k * 20;
            sideBoxes.add(new SideBox(def, new Box(bx - w, y, w + 6, 15), bx - w, bx + 6));
            k++;
        }
    }

    /** Recomputes the side ribbons after the active filter changed. */
    void refreshRibbons() {
        buildRibbonBoxes();
    }

    @Override
    protected final void init() {
        pageWidgets.clear();
        leafWidgets.clear();
        layoutBook();
        int doneX = (compLeft + compRight) / 2 - 30;
        SlateButton done = new SlateButton(doneX, by + bh + 8, 60, 16, Text.translatable("gui.done"),
                b -> close());
        done.visible = by + bh + 8 + 16 <= height;
        addDrawableChild(done);
        backButton = new SlateButton(doneX - 66, by + bh + 8, 60, 16,
                Text.translatable("ottoextra_tacticalfactions.book.back"), b -> {
            leafOverlay = false;
            applyVisibility();
        });
        backButton.visible = false;
        addDrawableChild(backButton);
        initContent();
        applyVisibility();
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double mx = click.x();
            double my = click.y();
            Tab[] tabs = Tab.values();
            for (int k = 0; k < topBoxes.size(); k++) {
                if (topBoxes.get(k).has(mx, my)) {
                    switchTab(tabs[k]);
                    return true;
                }
            }
            for (SideBox side : sideBoxes) {
                if (side.box().has(mx, my)) {
                    onSide(side.def().id());
                    buildRibbonBoxes();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void switchTab(Tab target) {
        if (target == tab || client == null) return;
        beforeLeave();
        client.setScreen(switch (target) {
            case POLITIK -> new PoliticsScreen(parent);
            case TRUPP -> new SquadScreen(parent);
            case KAMPF -> new CombatScreen(parent);
        });
    }

    @Override
    public void tick() {
        super.tick();
        tickContent();
    }

    // ---- rendering ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawChrome(ctx);
        if (pageVisible()) drawPage(ctx, mouseX, mouseY);
        if (leafVisible()) {
            OttonienTheme.leaf(ctx, lfx, lfy, lfw, leafHeight(), 32 + tab.ordinal(), false);
            drawLeaf(ctx, mouseX, mouseY);
        }
        super.render(ctx, mouseX, mouseY, delta);
        drawOver(ctx, mouseX, mouseY);
    }

    private void drawChrome(DrawContext ctx) {
        BookArt.blueBook(ctx, bx, by, bookW, bh);
        OttonienTheme.parchment(ctx, pageX(), pageY(), pageW(), pageH(), 31 + tab.ordinal(), true, true);
        for (int i = 0; i < 4; i++) {
            ctx.fill(pageX() + i, pageY() + 1, pageX() + i + 1, pageY() + pageH() - 1,
                    OttonienTheme.shade(OttonienTheme.PA, .88 + i * .03));
        }
        Tab[] tabs = Tab.values();
        for (int k = 0; k < tabs.length; k++) {
            boolean active = tabs[k] == tab;
            Box box = topBoxes.get(k);
            int len = active ? 22 : 17;
            OttonienTheme.ribbon(ctx, box.x(), by - len, by + 6, box.w(),
                    active ? tabs[k].color : OttonienTheme.shade(tabs[k].color, .85));
            ctx.drawText(textRenderer, Text.translatable(tabs[k].key), box.x() + 5, by - len + 5,
                    active ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
        }
        for (SideBox side : sideBoxes) {
            boolean active = side.def().id().equals(activeSide());
            int color = side.def().color();
            OttonienTheme.ribbonHorizontal(ctx, side.x0(), side.x1(), side.box().y(), 15,
                    active ? color : OttonienTheme.shade(color, .85));
            int length = side.x1() - side.x0();
            int tx = side.x0() + 6 + (length - 8) / 2 - textRenderer.getWidth(side.def().label()) / 2;
            ctx.drawText(textRenderer, side.def().label(), tx, side.box().y() + 4,
                    active ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
        }
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
