package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.PoliticalStateService.FactionView;
import de.ottoextra.tacticalfactions.RealmGrouping.Realm;
import de.ottoextra.tacticalfactions.theme.InkTextField;
import de.ottoextra.tacticalfactions.theme.OttonienTheme;
import de.ottoextra.tacticalfactions.theme.SlateButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Politik tab. View B (default) lists realms grouped by top liege with a detail leaf; view C is a
 * wide diplomacy board with one column per status. Local groups are edited in the leaf.
 */
public final class PoliticsScreen extends BookScreen {
    private static final int HEADER_H = 25;
    private static final int VASSAL_H = 11;
    private static final int FLAT_H = 22;
    private static final int CARD_H = 27;
    private static final int CARD_PITCH = 30;
    private static final Relation[] COLUMNS = {Relation.ALLIED, Relation.NEUTRAL, Relation.HOSTILE};
    private static final Relation[] CHIPS = {Relation.INHERIT, Relation.ALLIED, Relation.NEUTRAL, Relation.HOSTILE};

    private enum RealmFilter { ALL, ALLIED, NEUTRAL, HOSTILE, LOCAL }

    private enum BoardFilter { REALMS, FIEFS, LOCAL }

    private enum RowKind { HEADER, VASSAL, FLAT, GROUP, LOOSE }

    private static final String UNLANDED_ID = "__unlanded__";

    private static final class Row {
        final RowKind kind;
        final FactionView view;
        final Realm realm;
        final boolean open;
        final boolean last;
        final Relation deviates;
        final Relation realmRelation;
        final String sub;
        int y;
        int h;
        Identifier banner;

        Row(RowKind kind, FactionView view, Realm realm, boolean open, boolean last, Relation deviates,
            Relation realmRelation, String sub, int h) {
            this.kind = kind;
            this.view = view;
            this.realm = realm;
            this.open = open;
            this.last = last;
            this.deviates = deviates;
            this.realmRelation = realmRelation;
            this.sub = sub;
            this.h = h;
        }
    }

    private static final class Card {
        final FactionView view;
        final String sub;
        final int[] dots;
        int y;
        Identifier banner;

        Card(FactionView view, String sub, int[] dots) {
            this.view = view;
            this.sub = sub;
            this.dots = dots;
        }
    }

    private PoliticsView view = TacticalFactions.config().politicsView;
    private RealmFilter realmFilter = RealmFilter.ALL;
    private BoardFilter boardFilter = BoardFilter.REALMS;
    private final Set<String> collapsed = new HashSet<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<List<Card>> columns = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    private final int[] columnScroll = new int[3];
    private final Map<String, FactionView> byId = new HashMap<>();
    private final Map<String, FactionView> byName = new HashMap<>();
    private List<FactionView> all = List.of();
    private String query = "";
    private String selectedId;
    private GroupEditorPanel editor;
    private Identifier selectedBanner;
    private int contentHeight;
    private int scroll;
    private int realmCount;
    private int vassalCount;
    private long renderedRevision = -1L;
    private int tickCounter;
    private boolean opened;
    private InkTextField search;
    private SlateButton addLocalButton;
    private Box expandTile;
    private Box listToggle;
    private Box boardToggle;

    public PoliticsScreen(Screen parent) {
        super(parent, Tab.POLITIK, Text.translatable("ottoextra_tacticalfactions.book.title"));
    }

    // ---- BookScreen hooks ----

    @Override boolean wide() { return view == PoliticsView.BOARD; }

    @Override
    List<SideDef> sideDefs() {
        if (view == PoliticsView.BOARD) {
            return List.of(
                    new SideDef(BoardFilter.REALMS.name(), side("board.realms"), OttonienTheme.TERRACOTTA),
                    new SideDef(BoardFilter.FIEFS.name(), side("board.fiefs"), OttonienTheme.BRASS),
                    new SideDef(BoardFilter.LOCAL.name(), side("local"), OttonienTheme.NEUTRAL_TAN));
        }
        return List.of(
                new SideDef(RealmFilter.ALL.name(), side("all"), OttonienTheme.TERRACOTTA),
                new SideDef(RealmFilter.ALLIED.name(), Text.translatable(Relation.ALLIED.translationKey()),
                        OttonienTheme.FRIEND_BLUE),
                new SideDef(RealmFilter.NEUTRAL.name(), Text.translatable(Relation.NEUTRAL.translationKey()),
                        OttonienTheme.NEUTRAL_TAN),
                new SideDef(RealmFilter.HOSTILE.name(), Text.translatable(Relation.HOSTILE.translationKey()),
                        OttonienTheme.FOE_RED),
                new SideDef(RealmFilter.LOCAL.name(), side("local"), OttonienTheme.BRASS));
    }

    private static Text side(String key) {
        return Text.translatable("ottoextra_tacticalfactions.side." + key);
    }

    @Override
    String activeSide() {
        return view == PoliticsView.BOARD ? boardFilter.name() : realmFilter.name();
    }

    @Override
    void onSide(String id) {
        if (view == PoliticsView.BOARD) {
            boardFilter = BoardFilter.valueOf(id);
        } else {
            realmFilter = RealmFilter.valueOf(id);
        }
        scroll = 0;
        java.util.Arrays.fill(columnScroll, 0);
        rebuild();
        applyVisibility();
    }

    @Override
    void initContent() {
        if (!opened) {
            opened = true;
            TacticalFactions.politics().refresh();
        }
        boolean board = view == PoliticsView.BOARD;
        int sx = pageX() + 9;
        int sy = pageY() + 7;
        int sw = pageW() - (board ? 18 : 36) - 34;
        search = page(new InkTextField(textRenderer, sx, sy, sw, 14,
                Text.translatable(board ? "ottoextra_tacticalfactions.book.search_board"
                        : "ottoextra_tacticalfactions.book.search"), true, 12));
        search.setMaxLength(60);
        TabHints.gefolgeAndLehen(search);
        updateSuggestion();
        search.setText(query);
        search.setChangedListener(value -> {
            query = value;
            updateSuggestion();
            scroll = 0;
            java.util.Arrays.fill(columnScroll, 0);
            rebuild();
        });
        int tx = sx + sw + 3;
        expandTile = null;
        if (!board) {
            expandTile = new Box(tx, sy - 1, 16, 16);
            tx += 18;
        }
        listToggle = new Box(tx, sy - 1, 16, 16);
        boardToggle = new Box(tx + 17, sy - 1, 16, 16);

        addLocalButton = page(new SlateButton(pageX() + 9, pageY() + pageH() - 26, pageW() - 18, 18,
                Text.translatable("ottoextra_tacticalfactions.book.add"), b -> startNewGroup()));

        if (!board) {
            if (editor != null) {
                editor.init();
            } else {
                int bw = (lfw - 26) / 2;
                leaf(new SlateButton(lfx + 10, lfy + lfh - 25, bw, 18,
                        Text.translatable("ottoextra_tacticalfactions.book.refresh"),
                        b -> TacticalFactions.politics().refresh()));
                leaf(new SlateButton(lfx + 16 + bw, lfy + lfh - 25, bw, 18,
                        Text.translatable("ottoextra_tacticalfactions.book.add_short"), b -> startNewGroup()));
            }
        }
        rebuild();
    }

    private void updateSuggestion() {
        String hint = Text.translatable(view == PoliticsView.BOARD
                ? "ottoextra_tacticalfactions.book.search_board"
                : "ottoextra_tacticalfactions.book.search").getString();
        search.setSuggestion(query.isEmpty() ? hint : "");
    }

    @Override
    void applyExtraVisibility() {
        if (addLocalButton != null) {
            addLocalButton.visible = pageVisible() && view == PoliticsView.REALMS
                    && realmFilter == RealmFilter.LOCAL;
        }
    }

    @Override
    void tickContent() {
        if (renderedRevision != TacticalFactions.politics().revision()) rebuild();
        if (++tickCounter % 5 == 0) pollBanners();
    }

    // ---- data ----

    private void rebuild() {
        PoliticalStateService politics = TacticalFactions.politics();
        renderedRevision = politics.revision();
        all = politics.factions();
        byId.clear();
        byName.clear();
        for (FactionView entry : all) {
            byId.put(entry.id(), entry);
            byName.putIfAbsent(entry.name().toLowerCase(Locale.ROOT), entry);
        }
        if (selectedId != null && editor == null && !byId.containsKey(selectedId)) selectedId = null;
        rows.clear();
        for (List<Card> column : columns) column.clear();
        realmCount = 0;
        vassalCount = 0;
        contentHeight = 0;
        if (view == PoliticsView.BOARD) buildBoard(); else buildRows();
        clampScroll();
        pollBanners();
    }

    private boolean passes(FactionView entry, RealmFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case ALLIED -> entry.resolution().relation() == Relation.ALLIED;
            case NEUTRAL -> entry.resolution().relation() == Relation.NEUTRAL;
            case HOSTILE -> entry.resolution().relation() == Relation.HOSTILE;
            case LOCAL -> entry.custom();
        };
    }

    private void buildRows() {
        int y = 0;
        if (realmFilter == RealmFilter.LOCAL) {
            for (FactionView entry : all) {
                if (!entry.custom() || !matches(entry, query)) continue;
                Row row = new Row(RowKind.FLAT, entry, null, false, false, null,
                        entry.resolution().relation(), rankLine(entry), FLAT_H);
                row.y = y;
                y += FLAT_H;
                rows.add(row);
            }
            contentHeight = y;
            return;
        }
        Predicate<FactionView> keep = entry -> passes(entry, realmFilter) && matches(entry, query);
        List<Realm> realms = RealmGrouping.filter(RealmGrouping.group(all), keep);
        boolean searching = !query.isBlank();
        List<Realm> loose = new ArrayList<>();
        for (Realm realm : realms) {
            if (RealmGrouping.isLoose(realm)) {
                loose.add(realm);
                continue;
            }
            FactionView root = realm.root();
            Relation realmRelation = root.resolution().relation();
            boolean open = realm.total() > 0 && (searching || !collapsed.contains(root.id()))
                    && !realm.vassals().isEmpty();
            Row header = new Row(RowKind.HEADER, root, realm, open, false, null, realmRelation,
                    headerSub(root, realm), HEADER_H);
            header.y = y;
            y += HEADER_H;
            rows.add(header);
            realmCount++;
            vassalCount += realm.total();
            if (!open) continue;
            for (int i = 0; i < realm.vassals().size(); i++) {
                FactionView vassal = realm.vassals().get(i);
                boolean last = i == realm.vassals().size() - 1;
                Relation own = vassal.resolution().relation();
                Row row = new Row(RowKind.VASSAL, vassal, realm, false, last,
                        own != realmRelation ? own : null, realmRelation, null,
                        VASSAL_H + (last ? 4 : 0));
                row.y = y;
                y += row.h;
                rows.add(row);
            }
        }
        if (!loose.isEmpty()) {
            boolean open = searching || !collapsed.contains(UNLANDED_ID);
            Row group = new Row(RowKind.GROUP, null, null, open, false, null, Relation.NEUTRAL,
                    Text.translatable("ottoextra_tacticalfactions.book.unlanded", loose.size()).getString(), 16);
            group.y = y;
            y += group.h;
            rows.add(group);
            if (open) {
                for (int i = 0; i < loose.size(); i++) {
                    FactionView entry = loose.get(i).root();
                    boolean last = i == loose.size() - 1;
                    Row row = new Row(RowKind.LOOSE, entry, null, false, last, null,
                            entry.resolution().relation(), null, VASSAL_H + (last ? 4 : 0));
                    row.y = y;
                    y += row.h;
                    rows.add(row);
                }
            }
        }
        contentHeight = y;
    }

    private String headerSub(FactionView root, Realm realm) {
        String rank = rankLine(root);
        String vassals = Text.translatable("ottoextra_tacticalfactions.book.vassals", realm.total()).getString();
        return rank.isBlank() ? vassals : rank + " – " + vassals;
    }

    private String rankLine(FactionView entry) {
        if (entry.custom()) return Text.translatable(entry.type().translationKey()).getString();
        return entry.rankName() == null ? "" : entry.rankName();
    }

    private void buildBoard() {
        List<Realm> realms = RealmGrouping.group(all);
        List<FactionView> shown = new ArrayList<>();
        Map<String, Realm> realmOf = new HashMap<>();
        switch (boardFilter) {
            case REALMS -> {
                for (Realm realm : RealmGrouping.filter(realms, entry -> matches(entry, query))) {
                    if (RealmGrouping.isLoose(realm)) continue;
                    shown.add(realm.root());
                    realmOf.put(realm.root().id(), realm);
                }
            }
            case FIEFS -> {
                for (Realm realm : realms) realmOf.put(realm.root().id(), realm);
                for (FactionView entry : all) if (matches(entry, query)) shown.add(entry);
            }
            case LOCAL -> {
                for (FactionView entry : all) if (entry.custom() && matches(entry, query)) shown.add(entry);
            }
        }
        for (FactionView entry : shown) {
            Realm realm = realmOf.get(entry.id());
            String sub;
            int[] dots = new int[0];
            if (boardFilter == BoardFilter.LOCAL || entry.custom() && realm == null) {
                sub = Text.translatable("ottoextra_tacticalfactions.side.local").getString();
            } else if (realm != null) {
                sub = realm.total() > 0
                        ? Text.translatable("ottoextra_tacticalfactions.book.plus_vassals", realm.total()).getString()
                        : Text.translatable(entry.custom() ? "ottoextra_tacticalfactions.side.local"
                                : "ottoextra_tacticalfactions.book.independent").getString();
                if (boardFilter == BoardFilter.REALMS) {
                    dots = new int[Math.min(4, realm.vassals().size())];
                    for (int i = 0; i < dots.length; i++) {
                        dots[i] = BookArt.relationColor(realm.vassals().get(i).resolution().relation());
                    }
                }
            } else {
                sub = Text.translatable("ottoextra_tacticalfactions.book.in_top_simple", entry.topLiege()).getString();
            }
            Relation relation = entry.resolution().relation();
            int column = relation == Relation.ALLIED ? 0 : relation == Relation.HOSTILE ? 2 : 1;
            List<Card> list = columns.get(column);
            Card card = new Card(entry, sub, dots);
            card.y = list.size() * CARD_PITCH;
            list.add(card);
        }
        realmCount = (int) realms.stream().filter(realm -> !RealmGrouping.isLoose(realm)).count();
        vassalCount = RealmGrouping.totalVassals(realms);
    }

    private static boolean matches(FactionView entry, String query) {
        if (query == null || query.isBlank()) return true;
        String needle = query.toLowerCase(Locale.ROOT);
        if (entry.name().toLowerCase(Locale.ROOT).contains(needle)
                || entry.hierarchy().toLowerCase(Locale.ROOT).contains(needle)
                || entry.topLiege().toLowerCase(Locale.ROOT).contains(needle)
                || entry.description().toLowerCase(Locale.ROOT).contains(needle)) return true;
        return entry.custom() && TacticalFactions.politics().members(entry.id()).stream().anyMatch(member ->
                member.displayName().toLowerCase(Locale.ROOT).contains(needle)
                        || member.account() != null && member.account().toLowerCase(Locale.ROOT).contains(needle));
    }

    /** Looks up banner textures for the visible entries only; never blocks and never runs in render. */
    private void pollBanners() {
        LocalBannerTextureService banners = LocalBannerTextureService.instance();
        if (view == PoliticsView.BOARD) {
            for (int c = 0; c < 3; c++) {
                for (Card card : columns.get(c)) {
                    int top = card.y - columnScroll[c];
                    if (card.banner == null && top > -CARD_PITCH && top < columnViewportH()) {
                        card.banner = banners.bannerFor(card.view).orElse(null);
                    }
                }
            }
            return;
        }
        int viewport = listBottom() - listTop();
        for (Row row : rows) {
            int top = row.y - scroll;
            if (row.view != null && row.banner == null && top > -HEADER_H && top < viewport) {
                row.banner = banners.bannerFor(row.view).orElse(null);
            }
        }
        FactionView selected = selectedId != null ? byId.get(selectedId) : null;
        selectedBanner = selected != null ? banners.bannerFor(selected).orElse(null) : null;
    }

    // ---- selection / editing ----

    private void select(FactionView entry) {
        selectedId = entry.id();
        editor = null;
        if (entry.custom() && view == PoliticsView.REALMS) {
            CustomPoliticalEntity custom = TacticalFactions.config().customEntities.get(entry.id());
            if (custom != null) editor = new GroupEditorPanel(this, textRenderer, custom, false, this::editorFinished);
        }
        clearAndInit();
        showLeafIfNarrow();
    }

    private void startNewGroup() {
        if (view != PoliticsView.REALMS) return;
        CustomPoliticalEntity entity = CustomPoliticalEntity.create();
        editor = new GroupEditorPanel(this, textRenderer, entity, true, this::editorFinished);
        selectedId = entity.id;
        realmFilter = RealmFilter.LOCAL;
        scroll = 0;
        clearAndInit();
        showLeafIfNarrow();
    }

    private void editorFinished(String savedId) {
        editor = null;
        selectedId = savedId;
        CustomPoliticalEntity custom = savedId != null ? TacticalFactions.config().customEntities.get(savedId) : null;
        if (custom != null) editor = new GroupEditorPanel(this, textRenderer, custom, false, this::editorFinished);
        clearAndInit();
    }

    private void setView(PoliticsView target, boolean persist) {
        if (view == target) return;
        view = target;
        editor = null;
        scroll = 0;
        java.util.Arrays.fill(columnScroll, 0);
        if (persist) {
            TacticalFactions.config().politicsView = target;
            TacticalFactions.config().save();
        }
        clearAndInit();
    }

    private void toggleExpandAll() {
        if (collapsed.isEmpty()) {
            for (Realm realm : RealmGrouping.group(all)) {
                if (RealmGrouping.isLoose(realm)) continue;
                if (realm.total() > 0) collapsed.add(realm.root().id());
            }
            collapsed.add(UNLANDED_ID);
        } else {
            collapsed.clear();
        }
        rebuild();
    }

    private void setRelation(FactionView entry, Relation relation) {
        TacticalFactions.politics().setFaction(entry.id(), entry.name(), relation);
        rebuild();
    }

    /** Board click: next status (wraps), right click goes back. Own side is locked. */
    private void cycle(FactionView entry, int direction) {
        if ("own".equals(entry.resolution().source())) return;
        int index = switch (entry.resolution().relation()) {
            case ALLIED -> 0;
            case HOSTILE -> 2;
            default -> 1;
        };
        setRelation(entry, COLUMNS[Math.floorMod(index + direction, COLUMNS.length)]);
    }

    // ---- geometry ----

    private int listTop() { return pageY() + 26; }

    private int listBottom() {
        return pageY() + pageH() - (realmFilter == RealmFilter.LOCAL && view == PoliticsView.REALMS ? 30 : 16);
    }

    private int columnViewportH() { return pageY() + pageH() - 22 - (pageY() + 27 + 14); }

    private int columnWidth() { return (pageW() - 24) / 3; }

    private int columnX(int column) { return pageX() + 9 + column * (columnWidth() + 3); }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (listBottom() - listTop()))));
        for (int c = 0; c < 3; c++) {
            int content = columns.get(c).size() * CARD_PITCH;
            columnScroll[c] = Math.max(0, Math.min(columnScroll[c], Math.max(0, content - columnViewportH() + 4)));
        }
    }

    private Box detailChip(int index) {
        int cw = detailChipWidth();
        return new Box(lfx + 10 + (index % 2) * (cw + 3), lfy + 10 + 48 + (index / 2) * 13, cw, 11);
    }

    private int detailChipWidth() {
        int w = 0;
        for (Relation r : CHIPS) w = Math.max(w, textRenderer.getWidth(Text.translatable(r.translationKey())));
        return w + 8;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        if (editor != null && leafVisible() && editor.mouseClicked(mx, my, button)) return true;
        if (button == 0 && pageVisible()) {
            if (listToggle != null && listToggle.has(mx, my)) {
                setView(PoliticsView.REALMS, true);
                return true;
            }
            if (boardToggle != null && boardToggle.has(mx, my)) {
                setView(PoliticsView.BOARD, true);
                return true;
            }
            if (expandTile != null && expandTile.has(mx, my)) {
                toggleExpandAll();
                return true;
            }
        }
        if (pageVisible() && view == PoliticsView.BOARD && boardClick(mx, my, button)) return true;
        if (pageVisible() && view == PoliticsView.REALMS && button == 0 && rowClick(mx, my)) return true;
        if (button == 0 && leafVisible() && editor == null && view == PoliticsView.REALMS) {
            FactionView selected = selectedId != null ? byId.get(selectedId) : null;
            if (selected != null) {
                for (int i = 0; i < CHIPS.length; i++) {
                    if (detailChip(i).has(mx, my)) {
                        setRelation(selected, CHIPS[i]);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean rowClick(double mx, double my) {
        if (mx < pageX() + 5 || mx >= pageX() + pageW() - 5 || my < listTop() || my >= listBottom()) return false;
        double offset = my - listTop() + scroll;
        for (Row row : rows) {
            if (offset < row.y || offset >= row.y + row.h) continue;
            if (row.kind == RowKind.GROUP) {
                if (!collapsed.remove(UNLANDED_ID)) collapsed.add(UNLANDED_ID);
                rebuild();
            } else if (row.kind == RowKind.HEADER && mx < pageX() + 18 && row.realm.total() > 0) {
                if (!collapsed.remove(row.view.id())) collapsed.add(row.view.id());
                rebuild();
            } else {
                select(row.view);
            }
            return true;
        }
        return false;
    }

    private boolean boardClick(double mx, double my, int button) {
        if (button != 0 && button != 1) return false;
        int top = pageY() + 27 + 14;
        for (int c = 0; c < 3; c++) {
            int cx = columnX(c);
            if (mx < cx || mx >= cx + columnWidth() || my < top + 4 || my >= top + columnViewportH()) continue;
            for (Card card : columns.get(c)) {
                int cy = top + 4 + card.y - columnScroll[c];
                if (my < cy || my >= cy + CARD_H) continue;
                selectedId = card.view.id();
                if (card.view.custom() && button == 0 && mx >= cx + columnWidth() - 15 && my < cy + 11) {
                    view = PoliticsView.REALMS;
                    realmFilter = RealmFilter.LOCAL;
                    select(card.view);
                    return true;
                }
                cycle(card.view, button == 1 ? -1 : 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (editor != null && leafVisible() && editor.mouseScrolled(mouseX, mouseY, vertical)) return true;
        int step = vertical > 0 ? -1 : 1;
        if (pageVisible() && view == PoliticsView.REALMS && mouseX >= pageX() && mouseX < pageX() + pageW()
                && mouseY >= listTop() && mouseY < listBottom()) {
            scroll += step * 22;
            clampScroll();
            return true;
        }
        if (pageVisible() && view == PoliticsView.BOARD && mouseX >= pageX() && mouseX < pageX() + pageW()
                && mouseY >= pageY() + 27 + 14 && mouseY < pageY() + pageH() - 22) {
            int column = Math.min(2, Math.max(0, (int) ((mouseX - pageX() - 9) / (columnWidth() + 3))));
            columnScroll[column] += step * CARD_PITCH;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    // ---- rendering: page ----

    @Override
    void drawPage(DrawContext ctx, int mouseX, int mouseY) {
        drawSearchTiles(ctx, mouseX, mouseY);
        if (view == PoliticsView.BOARD) drawBoard(ctx, mouseX, mouseY); else drawRows(ctx, mouseX, mouseY);
    }

    private void drawSearchTiles(DrawContext ctx, int mouseX, int mouseY) {
        if (expandTile != null) {
            boolean hover = expandTile.has(mouseX, mouseY);
            OttonienTheme.tile(ctx, expandTile.x(), expandTile.y(), 16, 16, hover);
            if (collapsed.isEmpty()) {
                BookArt.arrow(ctx, expandTile.x() + 4, expandTile.y() + 6, true);
            } else {
                BookArt.arrow(ctx, expandTile.x() + 4, expandTile.y() + 4, false);
            }
        }
        drawToggleTile(ctx, listToggle, false, view == PoliticsView.REALMS, mouseX, mouseY);
        drawToggleTile(ctx, boardToggle, true, view == PoliticsView.BOARD, mouseX, mouseY);
    }

    private void drawToggleTile(DrawContext ctx, Box box, boolean board, boolean active, int mouseX, int mouseY) {
        if (box == null) return;
        OttonienTheme.tile(ctx, box.x(), box.y(), 16, 16, active || box.has(mouseX, mouseY));
        if (active) ctx.fill(box.x() + 2, box.y() + 14, box.x() + 14, box.y() + 15, OttonienTheme.GOLD_HOVER);
        int c = active ? OttonienTheme.INK : OttonienTheme.PICTO;
        int x = box.x();
        int y = box.y();
        if (!board) {
            for (int r = 0; r < 3; r++) {
                ctx.fill(x + 4, y + 4 + r * 3, x + 6, y + 6 + r * 3, c);
                ctx.fill(x + 7, y + 4 + r * 3, x + 12, y + 5 + r * 3, c);
            }
        } else {
            for (int col = 0; col < 3; col++) {
                ctx.fill(x + 3 + col * 4, y + 4, x + 6 + col * 4, y + 5, c);
                ctx.fill(x + 3 + col * 4, y + 6, x + 6 + col * 4, y + 12,
                        active ? c : OttonienTheme.shade(c, 1.3));
            }
        }
    }

    private void drawRows(DrawContext ctx, int mouseX, int mouseY) {
        int lx = pageX();
        int lw = pageW();
        int top = listTop();
        int bottom = listBottom();
        boolean local = realmFilter == RealmFilter.LOCAL;
        ctx.enableScissor(lx, top, lx + lw, bottom);
        for (Row row : rows) {
            int ry = top + row.y - scroll;
            if (ry + row.h < top || ry > bottom) continue;
            boolean selected = row.view != null && row.view.id().equals(selectedId);
            switch (row.kind) {
                case HEADER -> drawHeader(ctx, row, lx, lw, ry, selected);
                case VASSAL, LOOSE -> drawVassal(ctx, row, lx, lw, ry, selected);
                case GROUP -> drawGroup(ctx, row, lx, lw, ry);
                case FLAT -> drawFlat(ctx, row, lx, lw, ry, selected, row.y + row.h < contentHeight);
            }
        }
        if (local) {
            int hintY = top + contentHeight - scroll + 4;
            drawCentered(ctx, Text.translatable("ottoextra_tacticalfactions.book.local_hint"),
                    lx + lw / 2, hintY, lw - 24);
        } else if (rows.isEmpty()) {
            drawEmptyState(ctx, lx + lw / 2, top + 14, lw - 24);
        }
        ctx.disableScissor();
        if (contentHeight > bottom - top) {
            int trackH = bottom - top;
            int barH = Math.max(10, trackH * trackH / contentHeight);
            int barY = top + (trackH - barH) * scroll / Math.max(1, contentHeight - trackH);
            ctx.fill(lx + lw - 5, barY, lx + lw - 3, barY + barH, 0xCC7A6248);
        }
        if (!local) {
            drawCentered(ctx, Text.translatable("ottoextra_tacticalfactions.book.footer", realmCount, vassalCount),
                    lx + lw / 2, pageY() + pageH() - 13, lw - 20);
        }
    }

    private void drawHeader(DrawContext ctx, Row row, int lx, int lw, int ry, boolean selected) {
        ctx.fill(lx + 6, ry - 1, lx + lw - 6, ry + 21, OttonienTheme.PA_L);
        ctx.fill(lx + 6, ry + 21, lx + lw - 6, ry + 22, OttonienTheme.PA_S);
        if (selected) {
            ctx.fill(lx + 6, ry - 1, lx + lw - 6, ry, OttonienTheme.GOLD_HOVER);
            ctx.fill(lx + 6, ry - 1, lx + 8, ry + 21, OttonienTheme.TERRACOTTA);
        }
        if (row.realm.total() > 0) BookArt.arrow(ctx, lx + 9, ry + 7, row.open);
        FactionView entry = row.view;
        BookArt.arms(ctx, row.banner, lx + 19, ry + 1, 18, entry.mapColor(), entry.id());
        Text pennant = Text.translatable(BookArt.shortRelationKey(row.realmRelation));
        int pennantW = BookArt.tag(ctx, textRenderer, lx + lw - 9, ry + 1, pennant,
                BookArt.relationColor(row.realmRelation));
        int textW = lw - 41 - pennantW - 12;
        ctx.drawText(textRenderer, Text.literal(BookArt.ellipsize(textRenderer, entry.name(), textW))
                .formatted(Formatting.BOLD), lx + 41, ry + 1, OttonienTheme.INK, false);
        ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, row.sub, lw - 41 - 12), lx + 41, ry + 11,
                OttonienTheme.INK_MUTED, false);
    }

    private void drawVassal(DrawContext ctx, Row row, int lx, int lw, int ry, boolean selected) {
        if (selected) {
            ctx.fill(lx + 24, ry - 1, lx + lw - 6, ry + 10, OttonienTheme.PA_L);
            ctx.fill(lx + 24, ry - 1, lx + 26, ry + 10, OttonienTheme.TERRACOTTA);
        }
        if (row.kind == RowKind.VASSAL) {
            ctx.fill(lx + 27, ry - 3, lx + 28, ry + (row.last ? 5 : 11), OttonienTheme.PA_S);
            ctx.fill(lx + 27, ry + 4, lx + 31, ry + 5, OttonienTheme.PA_S);
        }
        FactionView entry = row.view;
        BookArt.arms(ctx, row.banner, lx + 33, ry, 9, entry.mapColor(), entry.id());
        int rightLimit = lx + lw - 14;
        if (row.kind == RowKind.LOOSE && row.realmRelation != Relation.NEUTRAL) {
            Text label = Text.translatable(BookArt.shortRelationKey(row.realmRelation));
            int w = BookArt.tag(ctx, textRenderer, lx + lw - 9, ry, label, BookArt.relationColor(row.realmRelation));
            rightLimit = lx + lw - 9 - w - 4;
        } else if (row.kind == RowKind.LOOSE) {
            ctx.fill(lx + lw - 14, ry + 4, lx + lw - 11, ry + 7, OttonienTheme.shade(OttonienTheme.NEUTRAL_TAN, 1.1));
        } else if (row.deviates != null) {
            Text label = Text.translatable(BookArt.shortRelationKey(row.deviates));
            int w = BookArt.tag(ctx, textRenderer, lx + lw - 9, ry, label, BookArt.relationColor(row.deviates));
            rightLimit = lx + lw - 9 - w - 4;
        } else {
            ctx.fill(lx + lw - 14, ry + 4, lx + lw - 11, ry + 7,
                    OttonienTheme.shade(BookArt.relationColor(row.realmRelation), 1.1));
        }
        ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, entry.name(), rightLimit - (lx + 45)),
                lx + 45, ry + 1, OttonienTheme.INK, false);
    }

    private void drawGroup(DrawContext ctx, Row row, int lx, int lw, int ry) {
        ctx.fill(lx + 6, ry, lx + lw - 6, ry + 14, OttonienTheme.PA_S);
        BookArt.arrow(ctx, lx + 9, ry + 4, row.open);
        ctx.drawText(textRenderer, Text.literal(BookArt.ellipsize(textRenderer, row.sub, lw - 40))
                .formatted(Formatting.BOLD), lx + 20, ry + 3, OttonienTheme.INK, false);
    }

    private void drawFlat(DrawContext ctx, Row row, int lx, int lw, int ry, boolean selected, boolean divider) {
        FactionView entry = row.view;
        if (selected) {
            ctx.fill(lx + 5, ry - 2, lx + lw - 5, ry + 19, OttonienTheme.PA_L);
            ctx.fill(lx + 5, ry - 2, lx + 7, ry + 19, OttonienTheme.TERRACOTTA);
        }
        boolean explicit = !"default".equals(entry.resolution().source())
                && !"unknown".equals(entry.resolution().source());
        ctx.fill(lx + 9, ry, lx + 11, ry + 17, explicit
                ? BookArt.relationColor(entry.resolution().relation()) : OttonienTheme.PA_S);
        BookArt.arms(ctx, row.banner, lx + 13, ry + 1, 16, entry.mapColor(), entry.id());
        Text relation = Text.translatable(BookArt.shortRelationKey(entry.resolution().relation()));
        int tagW = BookArt.tag(ctx, textRenderer, lx + lw - 9, ry, relation,
                BookArt.relationColor(entry.resolution().relation()));
        BookArt.tag(ctx, textRenderer, lx + lw - 9, ry + 11,
                Text.translatable("ottoextra_tacticalfactions.side.local"), BookArt.LOCAL_TAG);
        ctx.drawText(textRenderer, Text.literal(BookArt.ellipsize(textRenderer, entry.name(), lw - 33 - tagW - 14))
                .formatted(Formatting.BOLD), lx + 33, ry, OttonienTheme.INK, false);
        ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, row.sub, lw - 33 - 40), lx + 33, ry + 10,
                OttonienTheme.INK_MUTED, false);
        if (divider) BookArt.line(ctx, lx + 12, lx + lw - 10, ry + 20);
    }

    private void drawBoard(DrawContext ctx, int mouseX, int mouseY) {
        int ly = pageY();
        int lh = pageH();
        int top = ly + 27;
        int fieldBottom = ly + lh - 22;
        int colW = columnWidth();
        for (int c = 0; c < 3; c++) {
            int cx = columnX(c);
            int color = BookArt.relationColor(COLUMNS[c]);
            ctx.fill(cx, top, cx + colW, top + 14, OttonienTheme.shade(color, .6));
            ctx.fill(cx + 1, top + 1, cx + colW - 1, top + 13, color);
            Text label = Text.translatable(COLUMNS[c].translationKey());
            ctx.drawText(textRenderer, label, cx + (colW - textRenderer.getWidth(label)) / 2, top + 3,
                    OttonienTheme.LABEL, false);
            ctx.fill(cx, top + 14, cx + colW, fieldBottom, OttonienTheme.shade(OttonienTheme.PA, .94));
            ctx.fill(cx, top + 14, cx + 1, fieldBottom, OttonienTheme.PA_S);
            ctx.fill(cx + colW - 1, top + 14, cx + colW, fieldBottom, OttonienTheme.PA_S);
            ctx.enableScissor(cx, top + 14, cx + colW, fieldBottom);
            for (Card card : columns.get(c)) {
                int cy = top + 18 + card.y - columnScroll[c];
                if (cy + CARD_H < top + 14 || cy > fieldBottom) continue;
                drawCard(ctx, card, cx, cy, colW, color, mouseX, mouseY);
            }
            ctx.disableScissor();
        }
        if (realmCount == 0 && all.isEmpty()) {
            drawEmptyState(ctx, pageX() + pageW() / 2, top + 40, pageW() - 40);
        }
        drawCentered(ctx, Text.translatable("ottoextra_tacticalfactions.board.hint"),
                pageX() + pageW() / 2, ly + lh - 15, pageW() - 20);
    }

    private void drawCard(DrawContext ctx, Card card, int cx, int cy, int colW, int color, int mouseX, int mouseY) {
        FactionView entry = card.view;
        boolean selected = entry.id().equals(selectedId);
        boolean hover = mouseX >= cx + 3 && mouseX < cx + colW - 3 && mouseY >= cy && mouseY < cy + CARD_H;
        ctx.fill(cx + 3, cy, cx + colW - 3, cy + CARD_H, selected ? OttonienTheme.PA_D : OttonienTheme.PA_S);
        ctx.fill(cx + 4, cy + 1, cx + colW - 4, cy + CARD_H - 1,
                selected || hover ? OttonienTheme.PA_L : OttonienTheme.PA);
        if (selected) ctx.fill(cx + 4, cy + 1, cx + colW - 4, cy + 2, OttonienTheme.GOLD_HOVER);
        BookArt.arms(ctx, card.banner, cx + 7, cy + 4, 20, entry.mapColor(), entry.id());
        boolean quill = entry.custom();
        int textW = colW - 31 - 8 - (quill ? 10 : 0);
        ctx.drawText(textRenderer, Text.literal(BookArt.ellipsize(textRenderer, entry.name(), textW))
                .formatted(Formatting.BOLD), cx + 31, cy + 4, OttonienTheme.INK, false);
        ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, card.sub, colW - 31 - 8 - card.dots.length * 5),
                cx + 31, cy + 15, OttonienTheme.INK_MUTED, false);
        for (int i = 0; i < card.dots.length; i++) {
            ctx.fill(cx + colW - 10 - i * 5, cy + 17, cx + colW - 7 - i * 5, cy + 21, card.dots[i]);
        }
        if (quill) OttonienTheme.pictoQuill(ctx, cx + colW - 15, cy + 2, OttonienTheme.PICTO);
    }

    private void drawEmptyState(DrawContext ctx, int cx, int y, int width) {
        PoliticalStateService politics = TacticalFactions.politics();
        Text status;
        int color;
        if (politics.isRefreshing()) {
            status = Text.translatable("ottoextra_tacticalfactions.book.loading");
            color = OttonienTheme.WARN_OCHRE;
        } else if (!politics.lastError().isBlank()) {
            status = Text.translatable("ottoextra_tacticalfactions.book.error", politics.lastError());
            color = OttonienTheme.WAX_TEXT;
        } else if (all.isEmpty()) {
            status = Text.translatable("ottoextra_tacticalfactions.book.empty");
            color = OttonienTheme.INK_MUTED;
        } else {
            status = Text.translatable("ottoextra_tacticalfactions.book.no_match");
            color = OttonienTheme.INK_MUTED;
        }
        int ty = y;
        for (var line : textRenderer.wrapLines(status, width)) {
            ctx.drawText(textRenderer, line, cx - textRenderer.getWidth(line) / 2, ty, color, false);
            ty += 10;
        }
    }

    private void drawCentered(DrawContext ctx, Text text, int cx, int y, int width) {
        int ty = y;
        for (var line : textRenderer.wrapLines(text, width)) {
            ctx.drawText(textRenderer, line, cx - textRenderer.getWidth(line) / 2, ty,
                    OttonienTheme.INK_MUTED, false);
            ty += 10;
        }
    }

    // ---- rendering: leaf ----

    @Override
    void drawLeaf(DrawContext ctx, int mouseX, int mouseY) {
        if (editor != null) {
            editor.draw(ctx, mouseX, mouseY);
            return;
        }
        FactionView entry = selectedId != null ? byId.get(selectedId) : null;
        if (entry == null) {
            drawCentered(ctx, Text.translatable("ottoextra_tacticalfactions.detail.select"),
                    lfx + lfw / 2, lfy + 20, lfw - 24);
            return;
        }
        drawDetail(ctx, entry);
    }

    private void drawDetail(DrawContext ctx, FactionView entry) {
        int x = lfx + 10;
        int y = lfy + 10;
        int right = lfx + lfw - 10;
        OttonienTheme.goldFrame(ctx, x, y, 30, 30);
        ctx.fill(x + 3, y + 3, x + 27, y + 27, OttonienTheme.PA_L);
        BookArt.arms(ctx, selectedBanner, x + 4, y + 4, 22, entry.mapColor(), entry.id());
        ctx.drawText(textRenderer, Text.literal(BookArt.ellipsize(textRenderer, entry.name(), right - x - 36))
                .formatted(Formatting.BOLD), x + 36, y + 1, OttonienTheme.INK, false);
        ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, detailRank(entry), right - x - 36),
                x + 36, y + 11, OttonienTheme.INK_MUTED, false);
        boolean root = entry.id().equals(entry.rootId());
        String third = root ? (entry.custom() ? entry.description() : "")
                : Text.translatable("ottoextra_tacticalfactions.book.in_top_simple", entry.topLiege()).getString();
        ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, third, right - x - 36),
                x + 36, y + 21, OttonienTheme.INK_MUTED, false);

        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.editor.relation"),
                x, y + 38, OttonienTheme.INK_MUTED, false);
        Relation configured = TacticalFactions.config().factionRelations.getOrDefault(entry.id(), Relation.INHERIT);
        for (int i = 0; i < CHIPS.length; i++) {
            Box box = detailChip(i);
            BookArt.chip(ctx, textRenderer, box.x(), box.y(), box.w(),
                    Text.translatable(CHIPS[i].translationKey()), BookArt.relationColor(CHIPS[i]),
                    configured == CHIPS[i], true);
        }

        int by2 = y + 76;
        Relation effective = entry.resolution().relation();
        int color = BookArt.relationColor(effective);
        List<String> notes = new ArrayList<>();
        notes.add(TacticalFactionsAddon.sourceText(entry.resolution().source()).getString());
        FactionView rootView = byId.get(entry.rootId());
        if (!root && rootView != null && configured != Relation.INHERIT
                && rootView.resolution().relation() != effective) {
            notes.add(Text.translatable("ottoextra_tacticalfactions.detail.deviates", rootView.name(),
                    Text.translatable(rootView.resolution().relation().translationKey()).getString()).getString());
        }
        int boxH = 4 + 10 + notes.size() * 10;
        ctx.fill(x, by2, right, by2 + boxH, OttonienTheme.shade(OttonienTheme.PA, .93));
        ctx.fill(x, by2, x + 2, by2 + boxH, color);
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.detail.effective"),
                x + 6, by2 + 3, OttonienTheme.INK_MUTED, false);
        ctx.drawText(textRenderer, Text.translatable(effective.translationKey()).formatted(Formatting.BOLD),
                x + 8 + textRenderer.getWidth(Text.translatable("ottoextra_tacticalfactions.detail.effective")),
                by2 + 3, color, false);
        int ny = by2 + 13;
        for (String note : notes) {
            ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, note, right - x - 10), x + 6, ny,
                    OttonienTheme.INK_MUTED, false);
            ny += 10;
        }

        int cy = by2 + boxH + 5;
        String[] chain = entry.hierarchy().split(" ← ");
        if (chain.length > 1) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.detail.chain"),
                    x, cy, OttonienTheme.INK_MUTED, false);
            cy += 11;
            int limit = lfy + lfh - 25 - 14;
            for (int d = 0; d < chain.length && cy + 10 <= limit; d++) {
                String name = chain[chain.length - 1 - d];
                int xx = x + 4 + d * 10;
                if (d > 0) {
                    ctx.fill(xx - 6, cy - 4, xx - 5, cy + 4, OttonienTheme.PA_S);
                    ctx.fill(xx - 6, cy + 4, xx - 2, cy + 5, OttonienTheme.PA_S);
                }
                FactionView link = byName.get(name.toLowerCase(Locale.ROOT));
                BookArt.arms(ctx, null, xx, cy, 9, link != null ? link.mapColor() : 0xFF777777,
                        link != null ? link.id() : name);
                Text text = Text.literal(BookArt.ellipsize(textRenderer, name, right - xx - 14));
                ctx.drawText(textRenderer, d == chain.length - 1 ? text.copy().formatted(Formatting.BOLD) : text,
                        xx + 12, cy + 1, OttonienTheme.INK, false);
                cy += 11;
            }
        }
        if (entry.custom() && cy + 10 <= lfy + lfh - 25 - 4) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.book.members_short",
                    entry.memberCount()), x, cy + 1, OttonienTheme.INK_MUTED, false);
        }
    }

    private String detailRank(FactionView entry) {
        if (entry.custom()) return Text.translatable(entry.type().translationKey()).getString();
        String rank = entry.rankName() == null ? "" : entry.rankName();
        String leader = entry.apiRecord() != null ? entry.apiRecord().leader_name() : null;
        if (leader != null && !leader.isBlank()) return rank.isBlank() ? leader : rank + " – " + leader;
        return rank;
    }

    @Override
    void drawOver(DrawContext ctx, int mouseX, int mouseY) {
        if (search != null && pageVisible()) {
            OttonienTheme.pictoSearch(ctx, search.getX() - 12, search.getY() + 3, OttonienTheme.LABEL_MUTED);
        }
        if (editor != null && leafVisible()) editor.drawOver(ctx, mouseX, mouseY);
    }

    @Override
    void beforeLeave() {
        editor = null;
    }
}
