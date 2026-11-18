package de.ottoextra.rpnames.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.addon.OttoExtraAddons;
import de.ottoextra.addon.RpBookPlayerContext;
import de.ottoextra.addon.RpBookPlayerField;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.chat.ChatNameRewriter;
import de.ottoextra.rpnames.importer.RegionsApiRpNameImporter;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.Relation;
import de.ottoextra.ui.theme.InkCheckbox;
import de.ottoextra.ui.theme.InkTextField;
import de.ottoextra.ui.theme.OttonienTheme;
import de.ottoextra.ui.theme.SlateButton;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.title.TitleRegistry;
import de.ottoextra.rpnames.title.TitleColorResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RpNamesPeopleBookScreen extends Screen {

    private static final int COL_PANEL = 0xC8141418;
    private static final int COL_BORDER = 0xFF000000;
    private static final int COL_TITLE = 0xFFFFFFFF;
    private static final int COL_TEXT = 0xFFE0E0E0;
    private static final int COL_MUTED = 0xFF9A9A9A;
    private static final int COL_SELECTED = 0x40FFFFFF;
    private static final int COL_CONFLICT = 0xFFFF5555;
    private static final int COL_ONLINE = 0xFF55FF55;
    private static final int ROW_H = 22;

    private enum Tab { PEOPLE, TITLES, GROUPS, IMPORT }

    private static final String TF_ALL = "__all__";
    private static final String TF_ACTIVE = "__active__";
    private static final String TF_INACTIVE = "__inactive__";
    private static final String TF_MANUAL = "__manual__";
    private static final String TF_WIKI = "__wiki__";
    private static final String TF_CAT_PREFIX = "cat:";

    private record FilterOption(String id, Text label) {
    }

    private enum Chip {
        ALL("ottoextra.rpbook.chip.all"),
        UNKNOWN("ottoextra.rpbook.chip.unknown"),
        SEEN("ottoextra.rpbook.chip.seen"),
        HEARD("ottoextra.rpbook.chip.heard"),
        MANUAL("ottoextra.rpbook.chip.manual"),
        API("ottoextra.rpbook.chip.api"),
        CONFLICT("ottoextra.rpbook.chip.conflict"),
        ONLINE("ottoextra.rpbook.chip.online"),
        FRIEND("ottoextra.rpbook.chip.friend"),
        FOE("ottoextra.rpbook.chip.foe");

        final String key;

        Chip(String key) {
            this.key = key;
        }
    }

    private final Screen parent;
    private final LocalRpIdentityStore store;
    private final TitleRegistry titles;

    private Tab tab = Tab.PEOPLE;
    private Chip chip = Chip.ALL;

    private TextFieldWidget searchField;
    private TextFieldWidget rpNameField;
    private TextFieldWidget titleField;
    private TextFieldWidget notesField;
    private InkCheckbox lockCheckbox;
    private InkCheckbox titleLockCheckbox;

    private boolean loadingPerson;
    private ButtonWidget chatFlagButton;
    private ButtonWidget tabFlagButton;
    private ButtonWidget tagFlagButton;
    private final TextFieldWidget[] colorFields = new TextFieldWidget[6];
    private final boolean[] forceBaseColorOverride = new boolean[6];
    private ButtonWidget copyChatColorsButton;
    private ButtonWidget saveButton;
    /** Meet mode ({@link #openForMeet}): one-click "Ja, kenne ich" and "Nein" for this account. */
    private String meetAccount;
    private ButtonWidget meetYesButton;
    private ButtonWidget meetNoButton;
    private ButtonWidget forgetButton;
    private ButtonWidget keepLocalButton;
    private ButtonWidget takeApiButton;
    private final List<ButtonWidget> chipButtons = new ArrayList<>();
    private final List<ButtonWidget> addonFieldButtons = new ArrayList<>();
    private List<RpBookPlayerField> addonPlayerFields = List.of();

    private boolean filterDropdownOpen;
    private ButtonWidget filterButton;

    private final List<LocalRpProfile> filtered = new ArrayList<>();
    private final Set<String> onlineNamesLower = new HashSet<>();
    private LocalRpProfile selected;

    private TextFieldWidget groupLabelField;
    private TextFieldWidget groupTitleColorField;
    private TextFieldWidget groupNameColorField;
    private TextFieldWidget groupTitlesField;
    private ButtonWidget groupSaveButton;
    private final List<String> groupKeys = new ArrayList<>();
    private String selectedGroupKey;

    private String titleFilter = TF_ALL;
    private TextFieldWidget titleSearchField;
    private TextFieldWidget catTitleField;
    private TextFieldWidget catVariant1Field;
    private TextFieldWidget catVariant2Field;
    private TextFieldWidget catColorField;
    private TextFieldWidget catNameColorField;
    private TextFieldWidget catNewCategoryField;
    private InkCheckbox catEnabledCheckbox;
    private InkCheckbox catOverrideColorCheckbox;
    private ButtonWidget catSaveButton;
    private ButtonWidget catAddCategoryButton;
    private ButtonWidget newTitleButton;
    private ButtonWidget catDeleteButton;
    private final List<ButtonWidget> titleChipButtons = new ArrayList<>();
    private final List<de.ottoextra.rpnames.title.TitleCatalogStore.Entry> filteredTitles = new ArrayList<>();
    private de.ottoextra.rpnames.title.TitleCatalogStore.Entry selectedTitle;
    private String catCategoryValue = "unclassified";

    private ButtonWidget forgetAllButton;
    private boolean forgetAllArmed = false;
    private boolean importRunning = false;

    private int listScroll = 0;
    private volatile String statusLine = "";
    private boolean personDirty = false;
    private boolean forceRawTitleDisplay = false;

    private String titleAutofill;

    private String pendingSelectAccount;
    /** Suggested RP name and title for an unknown person ("kennenlernen"); shown, not saved. */
    private String pendingMeetName;
    private String pendingMeetTitle;
    private String pendingSelectUuid;

    public RpNamesPeopleBookScreen(Screen parent) {
        super(Text.translatable("ottoextra.rpbook.title"));
        this.parent = parent;

        RpNamesServices.ensureInitialized(OttoExtraConfig.active().rpnames);
        this.store = RpNamesServices.store();
        this.titles = RpNamesServices.titles();
    }

    public RpNamesPeopleBookScreen(Screen parent, String selectAccount) {
        this(parent, selectAccount, null);
    }

    private RpNamesPeopleBookScreen(Screen parent, String selectAccount, String selectUuid) {
        this(parent);
        this.pendingSelectAccount = selectAccount;
        this.pendingSelectUuid = selectUuid;
    }

    public static void openFor(Screen parent, String rawName, String uuid) {
        RpNamesServices.ensureInitialized(OttoExtraConfig.active().rpnames);
        LocalRpProfile profile = resolveOpenProfile(rawName, uuid);
        if (profile != null) {
            MinecraftClient.getInstance().setScreen(
                    new RpNamesPeopleBookScreen(parent, profile.accountName, profile.uuid));
        }
    }

    /**
     * Opens the book on an unknown person to get to know them, replacing the old meet dialog
     * ({@link MeetPersonScreen}). The suggested RP name and title (from the API or from chat) are
     * filled into the person's fields but not saved: "Speichern" accepts them, closing the book
     * leaves the person unknown, and the fields can be edited before saving.
     */
    public static void openForMeet(Screen parent, String account, String uuid, String rpName, String title) {
        RpNamesServices.ensureInitialized(OttoExtraConfig.active().rpnames);
        LocalRpProfile profile = resolveOpenProfile(account, uuid);
        if (profile == null) {
            return;
        }
        RpNamesPeopleBookScreen screen = new RpNamesPeopleBookScreen(parent, profile.accountName, profile.uuid);
        screen.meetAccount = profile.accountName;
        // Same rules as the old dialog: only a real RP name counts, the title is canonical.
        MeetPersonScreen.Prefill prefill = MeetPersonScreen.apiPrefill(rpName, title);
        screen.pendingMeetName = prefill.name();
        screen.pendingMeetTitle = prefill.title();
        MinecraftClient.getInstance().setScreen(screen);
    }

    /** Opens the book to meet a person with the local suggestion of the old meet dialog. */
    public static void openForMeet(Screen parent, String account, String uuid) {
        MeetPersonScreen.Prefill prefill = MeetPersonScreen.localPrefill(account, uuid);
        openForMeet(parent, account, uuid, prefill.name(), prefill.title());
    }

    static LocalRpProfile resolveOpenProfile(String rawName, String uuid) {
        LocalRpIdentityStore store = RpNamesServices.store();
        if (store == null || rawName == null || rawName.isBlank()) {
            return null;
        }
        LocalRpProfile existing = uuid != null && !uuid.isBlank()
                ? store.find(uuid, rawName.trim()).orElse(null)
                : RpNamesServices.findProfileByAnyName(rawName.trim());
        String account = existing != null ? existing.accountName : rawName.trim();
        String targetUuid = uuid != null && !uuid.isBlank() ? uuid : existing == null ? null : existing.uuid;
        return RpNamesServices.displayProfile(targetUuid, account)
                .orElseGet(() -> store.ensureSeen(account, targetUuid,
                        de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE));
    }

    // ---- Layout (Buch links, Dossierblatt rechts; wird in layoutBook() berechnet) ----

    private static final int BOOK_W_MAX = 182;
    private static final int LEAF_W_MAX = 186;
    private static final int RIBBON_TOP = 24;

    private record Box(int x, int y, int w, int h) {
        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record SideRibbon(String id, Text label, int color, Box box, int x0, int x1, int y) {
    }

    private enum Page { PERSON, COLORS }

    private Page page = Page.PERSON;
    private int bx;
    private int by;
    private int bookW = BOOK_W_MAX;
    private int bh = 200;
    private int lfx;
    private int lfy;
    private int lfw = LEAF_W_MAX;
    private int lfh = 190;
    private boolean narrow;
    private boolean leafOverlay;
    private boolean sideRibbonsShown;
    private final List<Box> topRibbonBoxes = new ArrayList<>();
    private final List<SideRibbon> sideRibbons = new ArrayList<>();
    private Box filterTile;
    private Box colorsToggle;
    private Box relationFriendBox;
    private Box relationFoeBox;
    private Box newCategoryChip;
    private final List<Box> categoryChipBoxes = new ArrayList<>();
    private final List<String> categoryChipKeys = new ArrayList<>();
    private boolean newCategoryOpen;
    private int titlePreviewY = -1;
    private String lastBackupText = "";
    private ButtonWidget doneButton;
    private int compLeft;
    private int compRight;
    private int doneX;

    private static final Tab[] RIBBON_TABS = {Tab.PEOPLE, Tab.TITLES, Tab.IMPORT};
    private static final int[] RIBBON_COLORS = {OttonienTheme.TERRACOTTA, OttonienTheme.BRASS,
            OttonienTheme.SLATE_TAB};

    private int panelX() {
        return bx;
    }

    private int panelW() {
        return bookW;
    }

    private int pageX() {
        return bx + 10;
    }

    private int pageY() {
        return by + 6;
    }

    private int pageW() {
        return bookW - 16;
    }

    private int pageH() {
        return bh - 12;
    }

    private int contentY() {
        return lfy + 10;
    }

    private int contentBottom() {
        return lfy + lfh;
    }

    private int listX() {
        return pageX() + 6;
    }

    private int listW() {
        return pageW() - 12;
    }

    private int rowH() {
        return tab == Tab.TITLES ? 18 : 22;
    }

    private int listTop() {
        return pageY() + 26;
    }

    private int listBottom() {
        return tab == Tab.TITLES ? pageY() + pageH() - 26 : pageY() + pageH() - 18;
    }

    private int editX() {
        return lfx + 9;
    }

    private int editW() {
        return lfw - 18;
    }

    private boolean previewVisible() {
        return false;
    }

    private int previewW() {
        return 0;
    }

    private int dropdownX() {
        return pageX() + pageW() - 9 - dropdownW();
    }

    private int dropdownY() {
        return pageY() + 24;
    }

    private int dropdownW() {
        int w = 60;
        for (FilterOption o : filterOptions()) {
            w = Math.max(w, textRenderer.getWidth(o.label()) + 10);
        }
        return w;
    }

    private int dropdownRowH() {
        return 13;
    }

    private boolean leafVisible() {
        return !narrow || leafOverlay;
    }

    private boolean bookContentVisible() {
        return !(narrow && leafOverlay);
    }

    private void layoutBook() {
        int rope = 1;
        narrow = false;
        int leafW = LEAF_W_MAX;
        int bookWidth = BOOK_W_MAX;
        int sideExt = tab == Tab.IMPORT ? 0 : sideRibbonExtent();
        int margin = 4;
        int need = sideExt + bookWidth + 8 + leafW + 2 * margin;
        if (need > width) {
            leafW = Math.max(150, leafW - (need - width));
            need = sideExt + bookWidth + 8 + leafW + 2 * margin;
        }
        if (need > width) {
            bookWidth = Math.max(156, bookWidth - (need - width));
            need = sideExt + bookWidth + 8 + leafW + 2 * margin;
        }
        if (need > width && sideExt > 0) {
            sideExt = 0;
            need = bookWidth + 8 + leafW + 2 * margin;
        }
        if (need > width) {
            narrow = true;
            leafW = Math.min(leafW, Math.max(120, width - 2 * margin));
            bookWidth = Math.min(BOOK_W_MAX, Math.max(150, width - 2 * margin - 2 * rope));
            need = bookWidth;
        }
        sideRibbonsShown = sideExt > 0 && tab != Tab.IMPORT;
        bookW = bookWidth;
        lfw = leafW;
        bh = Math.max(110, Math.min(204, height - RIBBON_TOP - 28));
        by = RIBBON_TOP + Math.max(0, (height - RIBBON_TOP - 28 - bh) / 2);
        int startX = (width - need) / 2;
        bx = narrow ? (width - bookW) / 2 : startX + sideExt;
        lfh = Math.min(tab == Tab.IMPORT ? 152 : 200, bh - 4);
        lfy = by + 2;
        lfx = narrow ? bx + (bookW - lfw) / 2 : bx + bookW + 8;
        if (!narrow || tab == Tab.IMPORT) {
            leafOverlay = false;
        }
        compLeft = narrow ? bx : bx - sideExt;
        compRight = narrow ? bx + bookW : lfx + lfw;
        buildRibbonBoxes();
    }

    private int sideRibbonExtent() {
        int max = 0;
        for (String[] def : sideRibbonDefs()) {
            for (FilterOption o : filterOptions()) {
                if (o.id().equals(def[0])) {
                    max = Math.max(max, textRenderer.getWidth(o.label()) + 20);
                }
            }
        }
        return Math.max(max, 0);
    }

    /** {id, farbe} der linken Filterbänder je Tab. */
    private String[][] sideRibbonDefs() {
        if (tab == Tab.TITLES) {
            return new String[][]{{TF_ALL, "0"}, {TF_ACTIVE, "1"}, {TF_INACTIVE, "2"},
                    {TF_MANUAL, "3"}, {TF_WIKI, "4"}};
        }
        return new String[][]{{Chip.ALL.name(), "0"}, {Chip.ONLINE.name(), "1"},
                {Chip.UNKNOWN.name(), "2"}, {Chip.FRIEND.name(), "5"}, {Chip.FOE.name(), "6"}};
    }

    private static int ribbonColorFor(String code) {
        return switch (code) {
            case "0" -> OttonienTheme.TERRACOTTA;
            case "1" -> OttonienTheme.GREEN;
            case "2" -> OttonienTheme.NEUTRAL_TAN;
            case "3" -> OttonienTheme.BRASS;
            case "4" -> OttonienTheme.SLATE_TAB;
            case "5" -> OttonienTheme.FRIEND_BLUE;
            default -> OttonienTheme.FOE_RED;
        };
    }

    private String currentFilterId() {
        return tab == Tab.TITLES ? titleFilter : chip.name();
    }

    private void buildRibbonBoxes() {
        topRibbonBoxes.clear();
        int x = bx + 18;
        for (int k = 0; k < RIBBON_TABS.length; k++) {
            Text label = Text.translatable(TAB_KEYS[k]);
            int w = textRenderer.getWidth(label) + 10;
            int len = RIBBON_TABS[k] == tab ? 22 : 17;
            topRibbonBoxes.add(new Box(x, by - len, w, len + 6));
            x += w + 4;
        }
        sideRibbons.clear();
        if (!sideRibbonsShown) {
            return;
        }
        String[][] defs = sideRibbonDefs();
        for (int k = 0; k < defs.length; k++) {
            Text label = null;
            for (FilterOption o : filterOptions()) {
                if (o.id().equals(defs[k][0])) {
                    label = o.label();
                }
            }
            if (label == null) {
                continue;
            }
            boolean active = defs[k][0].equals(currentFilterId());
            int w = textRenderer.getWidth(label) + 15 + (active ? 5 : 0);
            int y = by + 16 + k * 20;
            sideRibbons.add(new SideRibbon(defs[k][0], label, ribbonColorFor(defs[k][1]),
                    new Box(bx - w, y, w + 6, 15), bx - w, bx + 6, y));
        }
    }

    private static final String[] TAB_KEYS = {"ottoextra.rpbook.tab.people",
            "ottoextra.rpbook.tab.titles", "ottoextra.rpbook.tab.import"};

    @Override
    protected void init() {
        chipButtons.clear();
        addonFieldButtons.clear();
        addonPlayerFields = fieldsFor(selected);
        refreshOnline();
        layoutBook();
        refreshBackupInfo();

        doneX = (compLeft + compRight) / 2 - 30;
        doneButton = new SlateButton(doneX, by + bh + 8, 60, 16, Text.translatable("gui.done"),
                b -> close());
        doneButton.visible = by + bh + 8 + 16 <= height;
        addDrawableChild(doneButton);

        switch (tab) {
            case PEOPLE -> {
                initPeople();
                selectPendingIfAny();
            }
            case TITLES -> initTitles();
            case GROUPS -> initGroups();
            case IMPORT -> initImport();
        }
    }

    private void selectPendingIfAny() {
        if (pendingSelectAccount == null) {
            return;
        }
        LocalRpProfile p = store != null ? store.find(pendingSelectUuid, pendingSelectAccount).orElse(null) : null;
        pendingSelectAccount = null;
        pendingSelectUuid = null;
        if (p == null) {
            return;
        }
        if (searchField != null) {
            searchField.setText(p.accountName);
        }
        refilter();
        select(p);
        applyPendingMeet(p);
        if (narrow) {
            leafOverlay = true;
            applyPeopleVisibility();
        }
    }

    /** Fills the suggestion of {@link #openForMeet} into an unknown person's fields without saving. */
    private void applyPendingMeet(LocalRpProfile p) {
        String name = pendingMeetName;
        String title = pendingMeetTitle;
        pendingMeetName = null;
        pendingMeetTitle = null;
        if (name == null || p.hasRpName() || rpNameField == null || titleField == null) {
            return;
        }
        loadingPerson = true;
        if (!name.isEmpty()) {
            rpNameField.setText(name);
            titleField.setText(title == null ? "" : title);
        }
        loadingPerson = false;
        statusLine = Text.translatable(name.isEmpty() ? "ottoextra.rpbook.meet.hint_empty"
                : "ottoextra.rpbook.meet.hint").getString();
        applyPeopleVisibility();
    }

    /** True while the book shows the person it was opened to meet, still without a known name. */
    private boolean meetActive() {
        return meetAccount != null && selected != null && !selected.hasRpName()
                && meetAccount.equalsIgnoreCase(selected.accountName);
    }

    /**
     * "Ja, kenne ich": saves the shown name and title in one click like the old meet dialog
     * ({@link MeetPersonScreen#saveIdentity}, which keeps an existing lock) and returns to chat.
     */
    private void acceptMeet() {
        if (!meetActive() || !LocalRpProfile.isKnownRpName(rpNameField.getText().trim())) {
            return;
        }
        MeetPersonScreen.saveIdentity(selected.accountName, selected.uuid,
                rpNameField.getText().trim(), titleField.getText().trim());
        personDirty = false;
        meetAccount = null;
        close();
    }

    /** "Nein": closes without saving anything. */
    private void declineMeet() {
        personDirty = false;
        meetAccount = null;
        close();
    }

    private void switchTab(Tab target) {
        if (target == tab) {
            return;
        }
        savePendingPerson();
        tab = target;
        listScroll = 0;
        leafOverlay = false;
        filterDropdownOpen = false;
        clearAndInit();
    }

    private InkTextField inkField(int x, int y, int w, int maxLen) {
        InkTextField f = new InkTextField(textRenderer, x, y, w, 12, Text.empty(), false);
        f.setMaxLength(maxLen);
        addDrawableChild(f);
        return f;
    }

    private InkTextField searchWidget(java.util.function.Consumer<String> onChange) {
        int sx = pageX() + 9;
        int sy = pageY() + 7;
        int sw = pageW() - 36;
        // Links 12 px Polster für die Lupe, damit Text und Symbol sich nie überlagern.
        InkTextField f = new InkTextField(textRenderer, sx, sy, sw, 14,
                Text.translatable("ottoextra.rpbook.search"), true, 12);
        f.setSuggestion(Text.translatable("ottoextra.rpbook.search").getString());
        f.setChangedListener(s -> {
            f.setSuggestion(s.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.search").getString() : "");
            listScroll = 0;
            onChange.accept(s);
        });
        addDrawableChild(f);
        filterDropdownOpen = false;
        filterTile = new Box(sx + sw + 3, sy - 1, 16, 16);
        return f;
    }

    private void initPeople() {
        searchField = searchWidget(s -> refilter());
        de.ottoextra.ui.complete.TabCompletion.attach(searchField, de.ottoextra.ui.complete.TabCompletionSources::people);

        int x = lfx + 10;
        int w = lfw - 20;
        int fy = lfy + 10 + 36 + 21;
        int labW = 52;

        rpNameField = inkField(x + labW, fy - 1, w - labW, 48);
        rpNameField.setChangedListener(s -> markPersonDirty());
        de.ottoextra.ui.complete.TabCompletion.attach(rpNameField, de.ottoextra.ui.complete.TabCompletionSources::rpNames);
        titleField = inkField(x + labW, fy + 17, w - labW, 48);
        titleField.setChangedListener(s -> {

            markPersonDirty();
            if (!loadingPerson) {
                forceRawTitleDisplay = false;
            }
            titleAutofill = null;
            titleField.setSuggestion("");
            String typed = s.trim();
            if (typed.isEmpty()) {
                return;
            }
            String lower = typed.toLowerCase(Locale.ROOT);
            for (var e : RpNamesServices.catalog().all()) {
                if (!e.enabled) {
                    continue;
                }
                for (String v : e.variants.isEmpty() ? List.of(e.title) : e.variants) {
                    if (v != null && v.length() > typed.length()
                            && v.toLowerCase(Locale.ROOT).startsWith(lower)) {
                        titleAutofill = v;
                        titleField.setSuggestion(v.substring(typed.length()));
                        return;
                    }
                }
            }
        });
        notesField = inkField(x + labW, fy + 35, w - labW, 200);
        notesField.setSuggestion(Text.translatable("ottoextra.rpbook.notes").getString());
        notesField.setChangedListener(s -> {
            notesField.setSuggestion(s.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.notes").getString() : "");
            markPersonDirty();
        });

        lockCheckbox = InkCheckbox.builder(Text.translatable("ottoextra.rpbook.lock"), textRenderer)
                .pos(x, fy + 58).callback((checkbox, checked) -> markPersonDirty()).build();
        addDrawableChild(lockCheckbox);
        titleLockCheckbox = InkCheckbox.builder(
                Text.translatable("ottoextra.rpbook.titleLock"), textRenderer)
                .pos(x + 80, fy + 58).callback((checkbox, checked) -> markPersonDirty()).build();
        addDrawableChild(titleLockCheckbox);

        // API-Abweichung: übernehmen / behalten (nur bei Abweichung sichtbar)
        takeApiButton = new SlateButton(x + w - 30, lfy + 10 + 19, 14, 12, Text.literal("✓"),
                b -> resolveConflict(true));
        takeApiButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.translatable("ottoextra.rpbook.conflict.take")));
        addDrawableChild(takeApiButton);
        keepLocalButton = new SlateButton(x + w - 14, lfy + 10 + 19, 14, 12, Text.literal("✗"),
                b -> resolveConflict(false));
        keepLocalButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.translatable("ottoextra.rpbook.conflict.keep")));
        addDrawableChild(keepLocalButton);

        // Farbseite
        int cx = x + 44;
        int cw = w - 44 - 14;
        int cy = lfy + 36;
        for (int row = 0; row < 3; row++) {
            colorFields[row * 2] = colorField(cx, cy + row * 28, cw);
            colorFields[row * 2 + 1] = colorField(cx, cy + row * 28 + 13, cw);
        }
        int y = cy + 3 * 28 + 2;
        copyChatColorsButton = new SlateButton(x, y, w, 12,
                Text.translatable("ottoextra.rpbook.colors.copyChat"), b -> {
            colorFields[2].setText(colorFields[0].getText());
            colorFields[3].setText(colorFields[1].getText());
            colorFields[4].setText(colorFields[0].getText());
            colorFields[5].setText(colorFields[1].getText());
        });
        addDrawableChild(copyChatColorsButton);
        y += 14;
        chatFlagButton = flagButton(x, y, w / 3 - 1, "ottoextra.rpbook.flag.chat",
                () -> selected != null && selected.showInChat,
                v -> { if (selected != null) selected.showInChat = v; });
        tabFlagButton = flagButton(x + w / 3, y, w / 3 - 1, "ottoextra.rpbook.flag.tab",
                () -> selected != null && selected.showInTablist,
                v -> { if (selected != null) selected.showInTablist = v; });
        tagFlagButton = flagButton(x + 2 * w / 3, y, w - 2 * (w / 3), "ottoextra.rpbook.flag.tag",
                () -> selected != null && selected.showInNametag,
                v -> { if (selected != null) selected.showInNametag = v; });
        y += 14;
        for (int i = 0; i < addonPlayerFields.size(); i++) {
            final int fieldIndex = i;
            ButtonWidget button = new SlateButton(x, y, w, 12, addonFieldLabel(addonPlayerFields.get(i)), b -> {
                if (fieldIndex < addonPlayerFields.size()) {
                    addonPlayerFields.get(fieldIndex).action().run();
                }
            });
            addonFieldButtons.add(button);
            addDrawableChild(button);
            y += 14;
        }

        int bw = (lfw - 26) / 2;
        saveButton = new SlateButton(lfx + 10, lfy + lfh - 25, bw, 18,
                Text.translatable("ottoextra.rpbook.save"), b -> savePerson());
        addDrawableChild(saveButton);
        forgetButton = new SlateButton(lfx + 16 + bw, lfy + lfh - 25, bw, 18,
                Text.translatable("ottoextra.rpbook.forget"), b -> forget());
        addDrawableChild(forgetButton);
        // Meet mode: [Ja, kenne ich] [Speichern] [Nein] share the bottom row; "Vergessen" is hidden.
        int row = lfw - 20 - 8;
        int yesW = row * 44 / 100;
        int saveW = row * 32 / 100;
        int noW = row - yesW - saveW;
        meetYesButton = new SlateButton(lfx + 10, lfy + lfh - 25, yesW, 18,
                Text.translatable("ottoextra.rpbook.meet.yes"), b -> acceptMeet()).primary();
        addDrawableChild(meetYesButton);
        meetNoButton = new SlateButton(lfx + 10 + yesW + 4 + saveW + 4, lfy + lfh - 25, noW, 18,
                Text.translatable("ottoextra.rpbook.meet.no"), b -> declineMeet());
        addDrawableChild(meetNoButton);

        refilter();
        if (selected != null) {
            select(selected);
        } else {
            setPeopleEditEnabled(false);
        }
        applyPeopleVisibility();
    }

    /** Sichtbarkeit je Seite (Person/Farben), Überlagerung (schmale Fenster) und Konflikt. */
    private void applyPeopleVisibility() {
        if (tab != Tab.PEOPLE || saveButton == null) {
            return;
        }
        boolean leaf = leafVisible();
        boolean personPage = leaf && page == Page.PERSON;
        boolean colorPage = leaf && page == Page.COLORS;
        if (searchField != null) {
            searchField.visible = bookContentVisible();
        }
        rpNameField.visible = personPage;
        titleField.visible = personPage;
        notesField.visible = personPage;
        lockCheckbox.visible = personPage;
        titleLockCheckbox.visible = personPage;
        boolean conflict = selected != null && selected.apiConflict != null
                && !selected.apiConflict.isBlank();
        takeApiButton.visible = personPage && conflict;
        keepLocalButton.visible = personPage && conflict;
        for (TextFieldWidget f : colorFields) {
            if (f != null) {
                f.visible = colorPage;
            }
        }
        copyChatColorsButton.visible = colorPage;
        chatFlagButton.visible = colorPage;
        tabFlagButton.visible = colorPage;
        tagFlagButton.visible = colorPage;
        addonFieldButtons.forEach(b -> b.visible = colorPage);
        saveButton.visible = leaf;
        forgetButton.visible = leaf;
        if (meetYesButton != null && meetNoButton != null) {
            boolean meet = leaf && meetActive();
            meetYesButton.visible = meet;
            meetNoButton.visible = meet;
            forgetButton.visible = leaf && !meet;
            int bw = (lfw - 26) / 2;
            int row = lfw - 20 - 8;
            int yesW = row * 44 / 100;
            saveButton.setX(meet ? lfx + 10 + yesW + 4 : lfx + 10);
            saveButton.setWidth(meet ? row * 32 / 100 : bw);
        }
    }

    private TextFieldWidget colorField(int x, int y, int w) {
        InkTextField f = new InkTextField(textRenderer, x, y, w, 12, Text.empty(), false);
        f.setMaxLength(7);
        f.setChangedListener(s -> markPersonDirty());
        addDrawableChild(f);
        return f;
    }

    private ButtonWidget flagButton(int x, int y, int w, String key,
                                    java.util.function.BooleanSupplier get,
                                    java.util.function.Consumer<Boolean> set) {
        ButtonWidget btn = new SlateButton(x, y, w, 12, flagLabel(key, get.getAsBoolean()), b -> {
            boolean next = !get.getAsBoolean();
            set.accept(next);
            markPersonDirty();
            b.setMessage(flagLabel(key, next));
        });
        addDrawableChild(btn);
        return btn;
    }

    private void refreshBackupInfo() {
        lastBackupText = "";
        try {
            java.nio.file.Path dir = de.ottoextra.config.OttoExtraPaths.rpnamesBackups();
            if (!java.nio.file.Files.isDirectory(dir)) {
                return;
            }
            long newest = 0L;
            try (var stream = java.nio.file.Files.list(dir)) {
                for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) stream::iterator) {
                    newest = Math.max(newest, java.nio.file.Files.getLastModifiedTime(p).toMillis());
                }
            }
            if (newest > 0) {
                lastBackupText = java.time.format.DateTimeFormatter.ofPattern("dd.MM. HH:mm")
                        .format(java.time.LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(newest), java.time.ZoneId.systemDefault()));
            }
        } catch (Exception ignored) {
            lastBackupText = "";
        }
    }

    private TextFieldWidget field(int x, int y, int w, int maxLen, String suggestionKey) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        f.setMaxLength(maxLen);
        f.setSuggestion(Text.translatable(suggestionKey).getString());
        f.setChangedListener(s -> f.setSuggestion(s.isEmpty()
                ? Text.translatable(suggestionKey).getString() : ""));
        addDrawableChild(f);
        return f;
    }

    private Text flagLabel(String key, boolean on) {
        return Text.translatable(key).copy().append(": ")
                .append(Text.translatable(on ? "ottoextra.rpbook.flag.on"
                        : "ottoextra.rpbook.flag.off"));
    }

    private void updateChipState() {
        if (filterButton != null) {
            filterButton.setMessage(filterLabel());
        }
    }

    private Text filterLabel() {
        return Text.translatable("ottoextra.rpbook.filter", currentFilterLabel());
    }

    private Text currentFilterLabel() {
        String cur = tab == Tab.TITLES ? titleFilter : chip.name();
        for (FilterOption o : filterOptions()) {
            if (o.id().equals(cur)) {
                return o.label();
            }
        }
        return Text.literal("?");
    }

    private List<FilterOption> filterOptions() {
        List<FilterOption> opts = new ArrayList<>();
        if (tab == Tab.TITLES) {
            opts.add(new FilterOption(TF_ALL, Text.translatable("ottoextra.rpbook.chip.all")));
            opts.add(new FilterOption(TF_ACTIVE, Text.translatable("ottoextra.rpbook.tchip.active")));
            opts.add(new FilterOption(TF_INACTIVE, Text.translatable("ottoextra.rpbook.tchip.inactive")));
            opts.add(new FilterOption(TF_MANUAL, Text.translatable("ottoextra.rpbook.tchip.manual")));
            opts.add(new FilterOption(TF_WIKI, Text.translatable("ottoextra.rpbook.tchip.wiki")));
            var catalog = RpNamesServices.catalog();
            if (catalog != null) {
                for (var entry : catalog.categories().entrySet()) {
                    var cat = entry.getValue();
                    String label = cat != null && cat.label != null && !cat.label.isBlank()
                            ? cat.label : entry.getKey();
                    opts.add(new FilterOption(TF_CAT_PREFIX + entry.getKey(), Text.literal(label)));
                }
            }
            return opts;
        }
        for (Chip c : Chip.values()) {
            opts.add(new FilterOption(c.name(), Text.translatable(c.key)));
        }
        return opts;
    }

    private void selectFilter(int index) {
        List<FilterOption> opts = filterOptions();
        if (index < 0 || index >= opts.size()) {
            return;
        }
        String id = opts.get(index).id();
        if (tab == Tab.TITLES) {
            titleFilter = id;
            refilterTitles();
        } else {
            chip = Chip.valueOf(id);
            refilter();
        }
        listScroll = 0;
        filterDropdownOpen = false;
        updateChipState();
    }

    private void refreshOnline() {
        onlineNamesLower.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().name() != null) {
                onlineNamesLower.add(entry.getProfile().name().toLowerCase(Locale.ROOT));
            }
        }
    }

    private boolean isOnline(LocalRpProfile p) {
        return p.accountName != null && onlineNamesLower.contains(p.accountName.toLowerCase(Locale.ROOT));
    }

    private boolean matchesChip(LocalRpProfile p) {
        return switch (chip) {
            case ALL -> true;
            case UNKNOWN -> !p.hasRpName();
            case SEEN -> p.knowledgeState == KnowledgeState.SEEN;
            case HEARD -> p.knowledgeState == KnowledgeState.HEARD_NAME;
            case MANUAL -> p.knowledgeState == KnowledgeState.MANUAL
                    || p.knowledgeState == KnowledgeState.MANUAL_LOCKED;
            case API -> p.knowledgeState == KnowledgeState.API_IMPORTED;
            case CONFLICT -> p.apiConflict != null && !p.apiConflict.isBlank();
            case ONLINE -> isOnline(p);
            case FRIEND -> p.relation == Relation.FRIEND;
            case FOE -> p.relation == Relation.FOE;
        };
    }

    private void refilter() {
        filtered.clear();
        String q = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        for (LocalRpProfile p : store.all()) {
            if (!matchesChip(p)) {
                continue;
            }
            if (q.isEmpty()
                    || contains(p.accountName, q) || contains(p.rpName, q)
                    || contains(p.title, q) || contains(p.titleGroup, q)
                    || contains(p.uuid, q) || contains(p.apiConflict, q)) {
                filtered.add(p);
            }
        }

        filtered.sort(Comparator
                .comparing((LocalRpProfile p) -> !isOnline(p))
                .thenComparing(p -> p.apiConflict == null || p.apiConflict.isBlank())
                .thenComparing(p -> p.knowledgeState != KnowledgeState.MANUAL
                        && p.knowledgeState != KnowledgeState.MANUAL_LOCKED)
                .thenComparing(p -> !p.hasRpName())
                .thenComparing(p -> p.accountName == null ? "" : p.accountName.toLowerCase(Locale.ROOT)));
    }

    private static boolean contains(String hay, String needle) {
        return hay != null && hay.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String[] defaultColorsFor(LocalRpProfile profile, String title) {
        var catalog = RpNamesServices.catalog();
        String titleColor = TitleColorResolver.resolve(profile, null, title,
                catalog, titles);

        String nameColor = catalog != null
                ? catalog.titleNameColor(title).orElse(catalog.defaultNameColor())
                : "#c7a87f";
        return new String[]{titleColor, nameColor, titleColor, nameColor, titleColor, nameColor};
    }

    /**
     * Unveränderte Basisfarben für einen Titel. Anders als
     * {@link #defaultColorsFor(LocalRpProfile, String)}
     * werden titelbezogene Farb-Overrides hier bewusst ignoriert. Ein Reset soll sichtbar
     * zur Kategorie-/Grundfarbe zurückkehren und nicht wieder den gerade angepassten
     * Titel-Override einsetzen.
     */
    private String[] baseColorsFor(String title) {
        var catalog = RpNamesServices.catalog();
        String titleColor = null;
        if (catalog != null) {
            var entry = catalog.find(title == null ? "" : title).orElse(null);
            if (entry != null) {
                var category = catalog.categories().get(entry.category);
                titleColor = category != null ? category.color : null;
            }
        }
        if (titleColor == null || titleColor.isBlank()) {
            titleColor = titles.find(title == null ? "" : title)
                    .map(r -> r.group().titleColor).orElse("");
        }

        String globalNameColor = OttoExtraConfig.active().rpnames.globalRpNameColor;
        String nameColor = globalNameColor != null && !globalNameColor.isBlank()
                ? globalNameColor
                : catalog != null ? catalog.defaultNameColor() : "#c7a87f";
        return new String[]{titleColor, nameColor, titleColor, nameColor, titleColor, nameColor};
    }

    private String groupForTitle(String title) {
        var catalog = RpNamesServices.catalog();
        if (catalog != null) {
            var entry = catalog.find(title == null ? "" : title).orElse(null);
            if (entry != null) {
                var cat = catalog.categories().get(entry.category);
                return cat != null && cat.label != null && !cat.label.isBlank()
                        ? cat.label : entry.category;
            }
        }
        return titles.find(title == null ? "" : title)
                .map(TitleRegistry.ResolvedTitle::groupKey).orElse("");
    }

    private void select(LocalRpProfile profile) {
        if (selected != null && selected != profile) {
            savePendingPerson();
        }
        if (meetAccount != null && profile != null && !meetAccount.equalsIgnoreCase(profile.accountName)) {
            meetAccount = null;
        }
        selected = profile;
        if (profile == null) {
            setPeopleEditEnabled(false);
            return;
        }
        loadingPerson = true;
        rpNameField.setText(profile.hasRpName() ? profile.rpName : "");
        titleField.setText(profile.title == null ? "" : profile.title);
        forceRawTitleDisplay = profile.rawTitleDisplay;
        notesField.setText(profile.notes == null ? "" : profile.notes);
        if (lockCheckbox.isChecked() != profile.locked) {
            lockCheckbox.onPress(null);
        }
        if (titleLockCheckbox.isChecked() != profile.titleLocked) {
            titleLockCheckbox.onPress(null);
        }

        String[] overrides = {profile.colors.chatTitleColor, profile.colors.chatNameColor,
                profile.colors.tabTitleColor, profile.colors.tabNameColor,
                profile.colors.nametagTitleColor, profile.colors.nametagNameColor};
        String[] defaults = defaultColorsFor(profile, profile.title);
        for (int i = 0; i < 6; i++) {
            colorFields[i].setText(overrides[i] != null && !overrides[i].isBlank()
                    ? overrides[i] : defaults[i]);
            forceBaseColorOverride[i] = false;
        }
        chatFlagButton.setMessage(flagLabel("ottoextra.rpbook.flag.chat", profile.showInChat));
        tabFlagButton.setMessage(flagLabel("ottoextra.rpbook.flag.tab", profile.showInTablist));
        tagFlagButton.setMessage(flagLabel("ottoextra.rpbook.flag.tag", profile.showInNametag));
        refreshAddonFields();
        setPeopleEditEnabled(true);
        loadingPerson = false;
        personDirty = false;
        applyPeopleVisibility();
    }

    private void markPersonDirty() {
        if (!loadingPerson && tab == Tab.PEOPLE && selected != null) {
            personDirty = true;
        }
    }

    private void savePendingPerson() {
        if (personDirty && tab == Tab.PEOPLE && selected != null
                && rpNameField != null && titleField != null && notesField != null) {
            savePerson();
        }
    }

    private void setPeopleEditEnabled(boolean enabled) {
        rpNameField.setEditable(enabled);
        titleField.setEditable(enabled);
        notesField.setEditable(enabled);
        for (TextFieldWidget f : colorFields) {
            if (f != null) {
                f.setEditable(enabled);
            }
        }
        lockCheckbox.active = enabled;
        titleLockCheckbox.active = enabled;
        chatFlagButton.active = enabled;
        tabFlagButton.active = enabled;
        tagFlagButton.active = enabled;
        addonFieldButtons.forEach(button -> button.active = enabled);
        copyChatColorsButton.active = enabled;
        saveButton.active = enabled;
        forgetButton.active = enabled;
        boolean conflict = enabled && selected != null
                && selected.apiConflict != null && !selected.apiConflict.isBlank();
        if (keepLocalButton != null) {
            keepLocalButton.active = conflict;
            keepLocalButton.visible = conflict;
        }
        if (takeApiButton != null) {
            takeApiButton.active = conflict;
            takeApiButton.visible = conflict;
        }
        applyPeopleVisibility();
    }

    private List<RpBookPlayerField> fieldsFor(LocalRpProfile profile) {
        if (profile == null) return List.of();
        java.util.UUID uuid = null;
        try {
            if (profile.uuid != null && !profile.uuid.isBlank()) uuid = java.util.UUID.fromString(profile.uuid);
        } catch (IllegalArgumentException ignored) {
        }
        return OttoExtraAddons.rpBookPlayerFields(new RpBookPlayerContext(
                uuid, profile.accountName, profile.rpName, profile.title, this));
    }

    private void refreshAddonFields() {
        List<RpBookPlayerField> fresh = fieldsFor(selected);
        if (fresh.size() != addonFieldButtons.size()) {
            addonPlayerFields = fresh;
            clearAndInit();
            return;
        }
        addonPlayerFields = fresh;
        for (int i = 0; i < fresh.size(); i++) {
            addonFieldButtons.get(i).setMessage(addonFieldLabel(fresh.get(i)));
        }
    }

    private static Text addonFieldLabel(RpBookPlayerField field) {
        return field.value();
    }

    private void savePerson() {
        if (selected == null) {
            return;
        }
        String rpName = rpNameField.getText().trim();
        String title = titleField.getText().trim();
        String notes = notesField.getText().trim();
        String group = groupForTitle(title);

        String[] defaults = defaultColorsFor(selected, title);
        String[] baseColors = baseColorsFor(title);
        String[] hex = new String[6];
        for (int i = 0; i < 6; i++) {
            String v = normalizeHex(colorFields[i].getText());
            if (forceBaseColorOverride[i]) {
                // Die Basisfarbe muss als persönlicher Wert erhalten bleiben, solange der
                // Titelkatalog selbst eine abweichende Farbe vorgibt.
                hex[i] = normalizeHex(baseColors[i]);
            } else {
                hex[i] = v != null && v.equalsIgnoreCase(normalizeHex(defaults[i]) == null
                        ? "" : normalizeHex(defaults[i])) ? null : v;
            }
        }

        boolean showChat = selected.showInChat;
        boolean showTab = selected.showInTablist;
        boolean showTag = selected.showInNametag;
        boolean lock = lockCheckbox.isChecked();
        // The server tablist and API keep re-sending their own title and overwrite unlocked titles.
        // A title the user changed here therefore locks itself, or the edit would silently revert.
        String previousTitle = selected.title == null ? "" : selected.title;
        if (!title.isEmpty() && !title.equals(previousTitle) && !titleLockCheckbox.isChecked()) {
            loadingPerson = true;
            titleLockCheckbox.onPress(null);
            loadingPerson = false;
        }
        LocalRpProfile updated = store.updateManual(selected.accountName, selected.uuid, p -> {
            p.rpName = rpName.isEmpty() ? LocalRpProfile.UNKNOWN_NAME : rpName;
            p.title = title;
            p.rawTitleDisplay = forceRawTitleDisplay;

            p.titleLocked = titleLockCheckbox.isChecked();
            p.titleGroup = group;
            p.notes = notes;
            p.colors.chatTitleColor = hex[0];
            p.colors.chatNameColor = hex[1];
            p.colors.tabTitleColor = hex[2];
            p.colors.tabNameColor = hex[3];
            p.colors.nametagTitleColor = hex[4];
            p.colors.nametagNameColor = hex[5];
            p.showInChat = showChat;
            p.showInTablist = showTab;
            p.showInNametag = showTag;
        }, lock);
        selected = updated;
        java.util.Arrays.fill(forceBaseColorOverride, false);
        personDirty = false;
        refilter();
        setPeopleEditEnabled(true);

        statusLine = Text.translatable("ottoextra.rpbook.saved").getString();
    }

    private void forget() {
        if (selected == null) {
            return;
        }
        store.remove(selected);
        selected = null;
        setPeopleEditEnabled(false);
        refilter();
    }

    private void resolveConflict(boolean takeApi) {
        if (selected == null || selected.apiConflict == null) {
            return;
        }
        if (takeApi) {
            String apiName = selected.apiConflict;
            store.updateManual(selected.accountName, selected.uuid, p -> {
                p.rpName = apiName;
                p.apiConflict = null;
            }, selected.locked);
        } else {
            selected.apiConflict = null;
            store.saveSoon();
        }
        select(selected);
        refilter();
    }

    private static String normalizeHex(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replace("#", "");
        if (s.length() != 6 || !s.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        return "#" + s.toUpperCase(Locale.ROOT);
    }

    private void initTitles() {
        titleChipButtons.clear();
        titleSearchField = searchWidget(s -> refilterTitles());

        int x = lfx + 9;
        int w = lfw - 18;
        int labW = 50;
        int fy = lfy + 10 + 14;

        catTitleField = inkField(x + labW, fy - 1, w - labW, 48);
        catVariant1Field = inkField(x + labW, fy + 13, w - labW, 48);
        catVariant2Field = inkField(x + labW, fy + 27, w - labW, 48);
        catTitleField.setChangedListener(s -> { });

        catEnabledCheckbox = InkCheckbox.builder(
                Text.translatable("ottoextra.rpbook.titles.enabled"), textRenderer)
                .pos(x, fy + 70).build();
        addDrawableChild(catEnabledCheckbox);

        catNewCategoryField = new InkTextField(textRenderer, x, fy + 66, w - 22, 12,
                Text.empty(), false);
        catNewCategoryField.setMaxLength(32);
        catNewCategoryField.setSuggestion(
                Text.translatable("ottoextra.rpbook.titles.newCategory").getString());
        catNewCategoryField.setChangedListener(s -> catNewCategoryField.setSuggestion(
                s.isEmpty() ? Text.translatable("ottoextra.rpbook.titles.newCategory").getString()
                        : ""));
        addDrawableChild(catNewCategoryField);
        catAddCategoryButton = new SlateButton(x + w - 20, fy + 66, 20, 12, Text.literal("OK"),
                b -> addCategoryFromField());
        catAddCategoryButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.translatable("ottoextra.rpbook.titles.newCategory.tip")));
        addDrawableChild(catAddCategoryButton);

        catColorField = colorField(x + labW, fy + 86, w - labW - 14);
        catNameColorField = colorField(x + labW, fy + 100, w - labW - 14);
        catOverrideColorCheckbox = InkCheckbox.builder(
                Text.translatable("ottoextra.rpbook.titles.overridesColor"), textRenderer)
                .checked(true).pos(x, fy + 114).build();
        addDrawableChild(catOverrideColorCheckbox);

        int bw = (lfw - 16 - 2) / 2;
        catSaveButton = new SlateButton(lfx + 8, lfy + lfh - 24, bw, 18,
                Text.translatable("ottoextra.rpbook.save"), b -> saveTitle());
        addDrawableChild(catSaveButton);
        catDeleteButton = new SlateButton(lfx + 8 + bw + 2, lfy + lfh - 24, bw, 18,
                Text.translatable("ottoextra.rpbook.titles.delete"), b -> deleteTitle());
        addDrawableChild(catDeleteButton);

        newTitleButton = new SlateButton(pageX() + 10, pageY() + pageH() - 22, pageW() - 20, 16,
                Text.literal("+ ").append(Text.translatable("ottoextra.rpbook.titles.new")),
                b -> newTitle());
        addDrawableChild(newTitleButton);

        refilterTitles();
        if (selectedTitle != null) {
            selectTitle(selectedTitle);
        } else {
            setTitleEditEnabled(false);
        }
        layoutTitleLeaf();
    }

    /** Kategorie-Chips umbrechen; darunter rutschen Aktiv, Farben und Vorschau nach. */
    private void layoutTitleLeaf() {
        if (catTitleField == null || tab != Tab.TITLES) {
            return;
        }
        int x = lfx + 9;
        int w = lfw - 18;
        int fy = lfy + 10 + 14;
        categoryChipBoxes.clear();
        categoryChipKeys.clear();
        List<String> keys = new ArrayList<>(catalogCategories());
        int[] rowsOut = new int[1];
        while (true) {
            List<String> attempt = new ArrayList<>(keys);
            attempt.add("+");
            categoryChipBoxes.clear();
            categoryChipKeys.clear();
            int cx = x;
            int cyy = fy + 54;
            int rows = 1;
            for (String key : attempt) {
                String label = "+".equals(key) ? "+" : categoryLabel(key);
                int cw = Math.min(64, textRenderer.getWidth(label) + 8);
                if (cx + cw > x + w && cx > x) {
                    rows++;
                    cx = x;
                    cyy += 12;
                }
                categoryChipBoxes.add(new Box(cx, cyy, cw, 11));
                categoryChipKeys.add(key);
                cx += cw + 2;
            }
            rowsOut[0] = cyy;
            if (rows <= 2 || keys.size() <= 1) {
                break;
            }
            keys.remove(keys.size() - 1);
        }
        int cy = rowsOut[0];
        int c = cy + 13;
        catNewCategoryField.visible = newCategoryOpen && leafVisible();
        catAddCategoryButton.visible = newCategoryOpen && leafVisible();
        if (newCategoryOpen) {
            catNewCategoryField.setPosition(x + 3, c);
            catAddCategoryButton.setPosition(x + w - 20, c);
            c += 14;
        }
        catEnabledCheckbox.setPosition(x, c);
        c += 13;
        catColorField.setPosition(x + 50 + 3, c);
        c += 14;
        catNameColorField.setPosition(x + 50 + 3, c);
        c += 14;
        catOverrideColorCheckbox.setPosition(x, c);
        c += 13;
        titlePreviewY = c + 2 + 12 <= lfy + lfh - 26 ? c + 2 : -1;
        applyTitleVisibility();
    }

    private void applyTitleVisibility() {
        if (tab != Tab.TITLES || catTitleField == null) {
            return;
        }
        boolean leaf = leafVisible();
        for (var widget : new net.minecraft.client.gui.widget.ClickableWidget[]{catTitleField,
                catVariant1Field, catVariant2Field, catEnabledCheckbox, catColorField,
                catNameColorField, catOverrideColorCheckbox, catSaveButton, catDeleteButton}) {
            widget.visible = leaf;
        }
        catNewCategoryField.visible = leaf && newCategoryOpen;
        catAddCategoryButton.visible = leaf && newCategoryOpen;
        titleSearchField.visible = bookContentVisible();
        newTitleButton.visible = bookContentVisible();
    }

    private String categoryLabel(String key) {
        var cat = RpNamesServices.catalog().categories().get(key);
        return cat != null && cat.label != null && !cat.label.isBlank() ? cat.label : key;
    }

    private void chooseCategory(String key) {
        if (selectedTitle == null || key == null) {
            return;
        }
        catCategoryValue = key;
        if (selectedTitle.colorOverride == null || selectedTitle.colorOverride.isBlank()) {
            catColorField.setText(categoryColor(catCategoryValue));
        }
    }

    private void addCategoryFromField() {
        if (catNewCategoryField == null) {
            return;
        }
        String name = catNewCategoryField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        String key = RpNamesServices.catalog().addCategory(name, catColorField.getText().trim());
        if (key != null) {
            catCategoryValue = key;
            catNewCategoryField.setText("");
            newCategoryOpen = false;
            layoutTitleLeaf();
        }
    }

    private List<String> catalogCategories() {
        var cats = RpNamesServices.catalog().categories().keySet();
        return cats.isEmpty() ? List.of("unclassified") : new ArrayList<>(cats);
    }

    private String categoryColor(String key) {
        var c = RpNamesServices.catalog().categories().get(key);
        return c != null && c.color != null ? c.color : "";
    }

    private Text catCategoryLabel() {
        return Text.translatable("ottoextra.rpbook.titles.category").copy()
                .append(": " + catCategoryValue);
    }

    private boolean matchesTitleFilter(de.ottoextra.rpnames.title.TitleCatalogStore.Entry e) {
        return switch (titleFilter) {
            case TF_ALL -> true;
            case TF_ACTIVE -> e.enabled;
            case TF_INACTIVE -> !e.enabled;
            case TF_MANUAL -> !"WIKI_IMPORT".equals(e.source);
            case TF_WIKI -> "WIKI_IMPORT".equals(e.source);
            default -> titleFilter.startsWith(TF_CAT_PREFIX)
                    && titleFilter.substring(TF_CAT_PREFIX.length()).equals(e.category);
        };
    }

    private void refilterTitles() {
        filteredTitles.clear();
        String q = titleSearchField == null ? ""
                : titleSearchField.getText().toLowerCase(Locale.ROOT).trim();
        for (var e : RpNamesServices.catalog().all()) {
            if (!matchesTitleFilter(e)) {
                continue;
            }
            if (q.isEmpty()
                    || contains(e.title, q) || contains(e.category, q)
                    || e.variants.stream().anyMatch(v -> contains(v, q))) {
                filteredTitles.add(e);
            }
        }
        filteredTitles.sort(Comparator
                .comparing((de.ottoextra.rpnames.title.TitleCatalogStore.Entry e) -> e.category)
                .thenComparing(e -> e.title == null ? "" : e.title.toLowerCase(Locale.ROOT)));
    }

    private void selectTitle(de.ottoextra.rpnames.title.TitleCatalogStore.Entry e) {
        selectedTitle = e;
        if (e == null) {
            setTitleEditEnabled(false);
            return;
        }
        catTitleField.setText(e.title == null ? "" : e.title);
        catVariant1Field.setText(e.variants.size() > 0 ? e.variants.get(0) : "");
        catVariant2Field.setText(e.variants.size() > 1 ? e.variants.get(1) : "");
        catCategoryValue = e.category == null || e.category.isBlank() ? "unclassified" : e.category;

        catColorField.setText(e.colorOverride != null && !e.colorOverride.isBlank()
                ? e.colorOverride : categoryColor(catCategoryValue));
        catNameColorField.setText(e.nameColor == null ? "" : e.nameColor);
        if (catEnabledCheckbox.isChecked() != e.enabled) {
            catEnabledCheckbox.onPress(null);
        }
        if (catOverrideColorCheckbox.isChecked() != e.overridesColor) {
            catOverrideColorCheckbox.onPress(null);
        }
        setTitleEditEnabled(true);

        catDeleteButton.active = !"WIKI_IMPORT".equals(e.source);
        layoutTitleLeaf();
    }

    private void setTitleEditEnabled(boolean enabled) {
        catTitleField.setEditable(enabled);
        catVariant1Field.setEditable(enabled);
        catVariant2Field.setEditable(enabled);
        catColorField.setEditable(enabled);
        catNameColorField.setEditable(enabled);
        catEnabledCheckbox.active = enabled;
        catOverrideColorCheckbox.active = enabled;
        catSaveButton.active = enabled;
        catDeleteButton.active = enabled;
    }

    private void saveTitle() {
        if (selectedTitle == null) {
            return;
        }
        var e = selectedTitle;
        e.title = catTitleField.getText().trim();

        List<String> variants = new ArrayList<>();
        for (String t : new String[]{catVariant1Field.getText().trim(),
                catVariant2Field.getText().trim()}) {
            if (!t.isEmpty()) {
                variants.add(t);
            }
        }
        if (variants.isEmpty()) {
            variants.add(e.title);
        }
        e.variants = variants;
        e.category = catCategoryValue;

        String hex = normalizeHex(catColorField.getText());
        e.colorOverride = hex != null && hex.equalsIgnoreCase(categoryColor(catCategoryValue))
                ? null : hex;
        e.nameColor = normalizeHex(catNameColorField.getText());
        e.enabled = catEnabledCheckbox.isChecked();
        e.overridesColor = catOverrideColorCheckbox.isChecked();
        if (e.id == null || e.id.isBlank()) {
            e.id = TitleRegistry.normalize(e.title);
        }
        RpNamesServices.catalog().save();
        de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();

        refilterTitles();
        statusLine = Text.translatable("ottoextra.rpbook.saved").getString();
    }

    private void deleteTitle() {
        if (selectedTitle == null || "WIKI_IMPORT".equals(selectedTitle.source)) {
            return;
        }
        RpNamesServices.catalog().remove(selectedTitle);
        selectedTitle = null;
        setTitleEditEnabled(false);
        refilterTitles();
    }

    private void newTitle() {
        var e = new de.ottoextra.rpnames.title.TitleCatalogStore.Entry();
        e.title = "";
        e.variants = new ArrayList<>();
        e.category = "custom";
        e.source = "MANUAL";
        e.enabled = true;
        RpNamesServices.catalog().all().add(e);
        titleFilter = TF_ALL;
        if (titleSearchField != null) {
            titleSearchField.setText("");
        }
        refilterTitles();
        selectTitle(e);
        catTitleField.setFocused(true);
    }

    private void initGroups() {
        groupKeys.clear();
        groupKeys.addAll(titles.groups().keySet());
        if (selectedGroupKey == null && !groupKeys.isEmpty()) {
            selectedGroupKey = groupKeys.get(0);
        }
        int x = editX();
        int w = editW() + (previewVisible() ? previewW() + 8 : 0);
        int y = contentY() + 12;

        groupLabelField = field(x, y, w, 48, "ottoextra.rpbook.groupLabel");
        y += 21;
        groupTitleColorField = colorField(x, y, w / 2 - 16);
        groupNameColorField = colorField(x + w / 2 + 2, y, w / 2 - 16);
        y += 19;
        groupTitlesField = field(x, y, w, 4000, "ottoextra.rpbook.groupTitles");
        y += 21;
        groupSaveButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.save"),
                b -> saveGroup()).dimensions(x, y, w / 2 - 2, 16).build();
        addDrawableChild(groupSaveButton);

        selectGroup(selectedGroupKey);
    }

    private void selectGroup(String key) {
        selectedGroupKey = key;
        TitleRegistry.Group g = key == null ? null : titles.groups().get(key);
        boolean enabled = g != null;
        groupLabelField.setEditable(enabled);
        groupTitleColorField.setEditable(enabled);
        groupNameColorField.setEditable(enabled);
        groupTitlesField.setEditable(enabled);
        groupSaveButton.active = enabled;
        if (g == null) {
            return;
        }
        groupLabelField.setText(g.label == null ? "" : g.label);
        groupTitleColorField.setText(g.titleColor == null ? "" : g.titleColor);
        groupNameColorField.setText(g.nameColor == null ? "" : g.nameColor);
        groupTitlesField.setText(String.join(", ", g.titles));
    }

    private void saveGroup() {
        TitleRegistry.Group g = selectedGroupKey == null ? null : titles.groups().get(selectedGroupKey);
        if (g == null) {
            return;
        }
        g.label = groupLabelField.getText().trim();
        String tc = normalizeHex(groupTitleColorField.getText());
        String nc = normalizeHex(groupNameColorField.getText());
        if (tc != null) {
            g.titleColor = tc;
        }
        if (nc != null) {
            g.nameColor = nc;
        }
        List<String> list = new ArrayList<>();
        for (String t : groupTitlesField.getText().split(",")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        g.titles = list;
        titles.save();
        statusLine = Text.translatable("ottoextra.rpbook.saved").getString();
    }

    private void initImport() {
        int x = pageX() + 10;
        int w = pageW() - 20;
        int step = importStep();
        int y1 = pageY() + 24;
        int y2 = y1 + step;
        int y3 = y2 + step;

        importButton(x, y1 + 22, "ottoextra.rpbook.import.short.known", () -> runImport(false), true);
        importButton(x + importButtonW("ottoextra.rpbook.import.short.known") + 4, y1 + 22,
                "ottoextra.rpbook.import.short.all", () -> runImport(true), false);
        importButton(x, y2 + 22, "ottoextra.rpbook.import.short.known",
                () -> runOttoPlusImport(false), false);
        importButton(x + importButtonW("ottoextra.rpbook.import.short.known") + 4, y2 + 22,
                "ottoextra.rpbook.import.short.all", () -> runOttoPlusImport(true), false);
        importButton(x, y3 + 22, "ottoextra.rpbook.import.backupMake", () -> {
            store.backup().ifPresentOrElse(
                    path -> statusLine = Text.translatable("ottoextra.rpbook.import.backupDone",
                            path.getFileName().toString()).getString(),
                    () -> statusLine = Text.translatable("ottoextra.rpbook.import.backupFailed").getString());
            refreshBackupInfo();
        }, false);

        forgetAllArmed = false;
        if (narrow) {
            forgetAllButton = new SlateButton(x, pageY() + pageH() - 32, w, 14, forgetAllLabel(),
                    b -> forgetAll()).danger();
        } else {
            forgetAllButton = new SlateButton(lfx, lfy + lfh + 8, lfw, 20, forgetAllLabel(),
                    b -> forgetAll()).danger();
        }
        addDrawableChild(forgetAllButton);
    }

    private int importStep() {
        return Math.max(40, Math.min(48, (pageH() - 24 - 34) / 3));
    }

    private Text forgetAllLabel() {
        return Text.translatable("ottoextra.rpbook.forgetAll").append("…");
    }

    private int importButtonW(String key) {
        return textRenderer.getWidth(Text.translatable(key)) + 14;
    }

    private void importButton(int x, int y, String key, Runnable action, boolean primary) {
        addDrawableChild(new SlateButton(x, y, importButtonW(key), 16, Text.translatable(key),
                b -> action.run()));
    }

    private void runImport(boolean createMissing) {
        if (importRunning) {
            return;
        }
        importRunning = true;
        statusLine = Text.translatable("ottoextra.rpbook.import.running").getString();
        MinecraftClient client = MinecraftClient.getInstance();
        RegionsApiRpNameImporter.run(store, createMissing).whenComplete((result, error) ->
                client.execute(() -> {
                    importRunning = false;
                    if (error != null || result == null || !result.ok()) {
                        String msg = error != null ? error.getMessage()
                                : result != null ? result.error() : "?";
                        statusLine = Text.translatable("ottoextra.rpbook.import.failed", msg).getString();
                    } else {
                        statusLine = Text.translatable("ottoextra.rpbook.import.done",
                                result.updated(), result.created(), result.conflicts()).getString();
                        refilter();
                    }
                }));
    }

    private void runOttoPlusImport(boolean createMissing) {
        if (importRunning) {
            return;
        }
        importRunning = true;
        statusLine = Text.translatable("ottoextra.rpbook.import.running").getString();
        MinecraftClient client = MinecraftClient.getInstance();
        de.ottoextra.rpnames.importer.OttoPlusImporter.run(store, createMissing)
                .whenComplete((result, error) -> client.execute(() -> {
                    importRunning = false;
                    if (error != null || result == null || !result.ok()) {
                        String msg = error != null ? error.getMessage()
                                : result != null ? result.error() : "?";
                        statusLine = Text.translatable(
                                "ottoextra.rpbook.import.ottoplusFailed", msg).getString();
                    } else {
                        statusLine = Text.translatable("ottoextra.rpbook.import.ottoplusDone",
                                result.updated(), result.created(), result.skippedLocked())
                                .getString();
                        refilter();
                    }
                }));
    }

    private void forgetAll() {
        if (!forgetAllArmed) {
            forgetAllArmed = true;
            forgetAllButton.setMessage(Text.translatable("ottoextra.rpbook.forgetAllConfirm"));
            statusLine = Text.translatable("ottoextra.rpbook.forgetAllHint").getString();
            return;
        }
        store.backup();
        for (LocalRpProfile p : new ArrayList<>(store.all())) {
            store.remove(p);
        }
        store.saveNow();
        selected = null;
        forgetAllArmed = false;
        forgetAllButton.setMessage(forgetAllLabel());
        statusLine = Text.translatable("ottoextra.rpbook.forgotAll").getString();
    }

    private int listSize() {
        return switch (tab) {
            case PEOPLE -> filtered.size();
            case TITLES -> filteredTitles.size();
            case GROUPS -> groupKeys.size();
            case IMPORT -> 0;
        };
    }

    private int rowHeight() {
        return tab == Tab.PEOPLE || tab == Tab.TITLES ? rowH() : 12;
    }

    private int currentListTop() {
        return listTop();
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - currentListTop()) / rowHeight());
    }

    private int maxScroll() {
        return Math.max(0, listSize() - visibleRows());
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {

        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB
                && tab == Tab.PEOPLE && titleField != null && titleField.isFocused()
                && titleAutofill != null) {
            titleField.setText(titleAutofill);
            titleField.setSuggestion("");
            titleAutofill = null;
            return true;
        }

        if ((input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
                && tab == Tab.PEOPLE && titleField != null && titleField.isFocused()) {
            applyTitleColorFromCatalog();
            return true;
        }
        return super.keyPressed(input);
    }

    private void applyTitleColorFromCatalog() {
        String t = titleField.getText().trim();
        if (t.isEmpty()) {
            return;
        }
        var catalog = RpNamesServices.catalog();
        if (catalog == null) {
            return;
        }
        var entry = catalog.find(t).orElse(null);
        if (entry == null || !entry.overridesColor) {
            return;
        }
        String c = catalog.titleColor(t).orElse(null);
        if (c != null && !c.isBlank()) {
            colorFields[0].setText(c);
            colorFields[2].setText(c);
            colorFields[4].setText(c);
        }
    }

    private void applyFilterId(String id) {
        if (tab == Tab.TITLES) {
            titleFilter = id;
            refilterTitles();
        } else {
            try {
                chip = Chip.valueOf(id);
            } catch (IllegalArgumentException e) {
                return;
            }
            refilter();
        }
        listScroll = 0;
        filterDropdownOpen = false;
        updateChipState();
        buildRibbonBoxes();
    }

    private void toggleRelation(Relation target) {
        if (selected == null) {
            return;
        }
        selected.relation = selected.relation == target ? Relation.NONE : target;
        markPersonDirty();
    }

    private List<String> categoryKeys() {
        return new ArrayList<>(catalogCategories());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (tab != Tab.IMPORT && bookContentVisible() && mouseX >= listX() && mouseX <= listX() + listW()
                && mouseY >= currentListTop() && mouseY <= listBottom()) {
            listScroll = Math.max(0, Math.min(listScroll - (int) Math.signum(vertical) * 3, maxScroll()));
            return true;
        }
        if (tab == Tab.TITLES && leafVisible() && selectedTitle != null) {
            for (Box b : categoryChipBoxes) {
                if (b.has(mouseX, mouseY)) {
                    // Zusätzliche Kategorien jenseits der zwei Chip-Zeilen: Mausrad wechselt.
                    List<String> keys = categoryKeys();
                    int idx = Math.max(0, keys.indexOf(catCategoryValue));
                    idx = Math.floorMod(idx - (int) Math.signum(vertical), keys.size());
                    chooseCategory(keys.get(idx));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (filterDropdownOpen && tab != Tab.IMPORT && bookContentVisible()) {
            int count = filterOptions().size();
            int dx = dropdownX();
            int dy = dropdownY();
            int rh = dropdownRowH();
            if (click.button() == 0 && mx >= dx && mx <= dx + dropdownW()
                    && my >= dy && my <= dy + count * rh) {
                selectFilter((int) ((my - dy) / rh));
                buildRibbonBoxes();
                return true;
            }
            if (filterTile == null || !filterTile.has(mx, my)) {
                filterDropdownOpen = false;
                return true;
            }
        }

        if (click.button() == 0) {
            for (int k = 0; k < topRibbonBoxes.size(); k++) {
                if (topRibbonBoxes.get(k).has(mx, my)) {
                    switchTab(RIBBON_TABS[k]);
                    return true;
                }
            }
            for (SideRibbon r : sideRibbons) {
                if (r.box().has(mx, my)) {
                    applyFilterId(r.id());
                    return true;
                }
            }
            if (tab != Tab.IMPORT && bookContentVisible() && filterTile != null
                    && filterTile.has(mx, my)) {
                filterDropdownOpen = !filterDropdownOpen;
                return true;
            }
            if (narrow && leafOverlay && backBox().has(mx, my)) {
                leafOverlay = false;
                applyPeopleVisibility();
                applyTitleVisibility();
                return true;
            }
            if (tab == Tab.PEOPLE && leafVisible() && selected != null) {
                if (page == Page.PERSON && relationFriendBox != null && relationFriendBox.has(mx, my)) {
                    toggleRelation(Relation.FRIEND);
                    return true;
                }
                if (page == Page.PERSON && relationFoeBox != null && relationFoeBox.has(mx, my)) {
                    toggleRelation(Relation.FOE);
                    return true;
                }
                if (colorsToggle != null && colorsToggle.has(mx, my)) {
                    page = page == Page.PERSON ? Page.COLORS : Page.PERSON;
                    applyPeopleVisibility();
                    return true;
                }
            }
            if (tab == Tab.TITLES && leafVisible() && selectedTitle != null) {
                for (int i = 0; i < categoryChipBoxes.size(); i++) {
                    if (categoryChipBoxes.get(i).has(mx, my)) {
                        String key = categoryChipKeys.get(i);
                        if ("+".equals(key)) {
                            newCategoryOpen = !newCategoryOpen;
                            layoutTitleLeaf();
                        } else {
                            chooseCategory(key);
                        }
                        return true;
                    }
                }
            }
        }

        if (tab == Tab.PEOPLE && selected != null && click.button() == 0) {
            for (int i = 0; i < colorFields.length; i++) {
                if (resetIconHit(colorFields[i], mx, my)) {
                    if (!backupBeforeReset()) {
                        return true;
                    }
                    String liveTitle = titleField != null ? titleField.getText().trim() : selected.title;
                    colorFields[i].setText(baseColorsFor(liveTitle)[i]);
                    forceBaseColorOverride[i] = true;
                    savePerson();
                    return true;
                }
            }

            if (resetIconHit(rpNameField, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }
                String original = firstNonBlank(selected.apiRpName,
                        firstNonBlank(selected.apiConflict,
                                selected.hasRpName() ? selected.rpName : null));
                rpNameField.setText(original == null ? "" : original);
                savePerson();
                return true;
            }
            if (resetIconHit(titleField, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }

                String serverTitle = RpNamesServices.serverTitleFor(selected.accountName);
                titleField.setText(serverTitle != null && !serverTitle.isBlank()
                        ? serverTitle : selected.title);
                forceRawTitleDisplay = true;
                applyTitleColorFromCatalog();
                savePerson();
                return true;
            }
        }

        if (tab == Tab.TITLES && selectedTitle != null && click.button() == 0) {
            var def = RpNamesServices.catalog().bundledDefault(selectedTitle.id).orElse(null);
            if (resetIconHit(catTitleField, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }
                catTitleField.setText(def != null && def.title != null ? def.title
                        : (selectedTitle.title == null ? "" : selectedTitle.title));
                saveTitle();
                return true;
            }
            if (resetIconHit(catVariant1Field, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }
                catVariant1Field.setText(def != null && def.variants.size() > 0
                        ? def.variants.get(0) : "");
                saveTitle();
                return true;
            }
            if (resetIconHit(catVariant2Field, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }
                catVariant2Field.setText(def != null && def.variants.size() > 1
                        ? def.variants.get(1) : "");
                saveTitle();
                return true;
            }
            if (resetIconHit(catColorField, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }
                String bundledColor = def != null && def.colorOverride != null
                        && !def.colorOverride.isBlank()
                        ? def.colorOverride
                        : categoryColor(def != null ? def.category : catCategoryValue);
                catColorField.setText(bundledColor);
                saveTitle();
                return true;
            }
            if (resetIconHit(catNameColorField, mx, my)) {
                if (!backupBeforeReset()) {
                    return true;
                }
                catNameColorField.setText(def != null && def.nameColor != null
                        ? def.nameColor : "");
                saveTitle();
                return true;
            }
        }
        if (tab != Tab.IMPORT && bookContentVisible() && click.button() == 0
                && mx >= listX() && mx <= listX() + listW()
                && my >= currentListTop() && my <= listBottom()) {
            int idx = listScroll + (int) ((my - currentListTop()) / rowHeight());
            if (idx >= 0 && idx < listSize()) {
                switch (tab) {
                    case PEOPLE -> {
                        select(filtered.get(idx));
                        if (narrow) {
                            leafOverlay = true;
                            applyPeopleVisibility();
                        }
                    }
                    case TITLES -> {
                        selectTitle(filteredTitles.get(idx));
                        if (narrow) {
                            leafOverlay = true;
                        }
                        layoutTitleLeaf();
                    }
                    case GROUPS -> selectGroup(groupKeys.get(idx));
                    default -> { }
                }
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean backupBeforeReset() {
        var backup = store.backup();
        if (backup.isEmpty()) {
            statusLine = Text.translatable("ottoextra.rpbook.import.backupFailed").getString();
            return false;
        }
        statusLine = Text.translatable("ottoextra.rpbook.import.backupDone",
                backup.get().getFileName().toString()).getString();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (meetYesButton != null && rpNameField != null) {
            meetYesButton.active = meetActive() && LocalRpProfile.isKnownRpName(rpNameField.getText().trim());
        }
        renderBookChrome(ctx);
        if (bookContentVisible()) {
            switch (tab) {
                case PEOPLE -> renderPeopleList(ctx, mouseX, mouseY);
                case TITLES -> renderTitleList(ctx);
                case IMPORT -> renderImportPage(ctx);
                default -> { }
            }
        }
        if (leafVisible()) {
            renderLeafBackground(ctx);
        }
        super.render(ctx, mouseX, mouseY, delta);
        if (tab != Tab.IMPORT && bookContentVisible()) {
            InkTextField field = tab == Tab.TITLES ? titleSearchFieldInk() : searchFieldInk();
            if (field != null) {
                OttonienTheme.pictoSearch(ctx, field.getX() - 12, field.getY() + 3,
                        OttonienTheme.LABEL_MUTED);
            }
        }

        if (leafVisible()) {
            switch (tab) {
                case PEOPLE -> renderPersonLeaf(ctx, mouseX, mouseY);
                case TITLES -> renderTitleLeaf(ctx);
                case IMPORT -> renderImportLeaf(ctx);
                default -> { }
            }
        }
        if (tab != Tab.IMPORT && !statusLine.isEmpty() && doneButton != null && doneButton.visible) {
            ctx.drawText(textRenderer, textRenderer.trimToWidth(statusLine, Math.max(20, doneX - bx - 8)),
                    bx, by + bh + 12, 0xFFD6C194, false);
        }
        if (filterDropdownOpen && tab != Tab.IMPORT && bookContentVisible()) {
            renderDropdown(ctx, mouseX, mouseY);
        }
    }

    private void renderBookChrome(DrawContext ctx) {
        OttonienTheme.redBook(ctx, bx, by, bookW, bh);
        OttonienTheme.parchment(ctx, pageX(), pageY(), pageW(), pageH(), 31 + tab.ordinal(), true, true);
        for (int i = 0; i < 4; i++) {
            ctx.fill(pageX() + i, pageY() + 1, pageX() + i + 1, pageY() + pageH() - 1,
                    OttonienTheme.shade(OttonienTheme.PA, .88 + i * .03));
        }
        for (int k = 0; k < RIBBON_TABS.length; k++) {
            boolean active = RIBBON_TABS[k] == tab;
            Box box = topRibbonBoxes.get(k);
            int len = active ? 22 : 17;
            int color = RIBBON_COLORS[k];
            OttonienTheme.ribbon(ctx, box.x(), by - len, by + 6, box.w(),
                    active ? color : OttonienTheme.shade(color, .85));
            ctx.drawText(textRenderer, Text.translatable(TAB_KEYS[k]), box.x() + 5, by - len + 5,
                    active ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
        }
        for (SideRibbon r : sideRibbons) {
            boolean active = r.id().equals(currentFilterId());
            OttonienTheme.ribbonHorizontal(ctx, r.x0(), r.x1(), r.y(), 15,
                    active ? r.color() : OttonienTheme.shade(r.color(), .85));
            int length = r.x1() - r.x0();
            int tx = r.x0() + 6 + (length - 8) / 2 - textRenderer.getWidth(r.label()) / 2;
            ctx.drawText(textRenderer, r.label(), tx, r.y() + 4,
                    active ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
        }
    }

    private void renderSearchChrome(DrawContext ctx, int mouseX, int mouseY) {
        if (filterTile != null) {
            boolean hover = filterTile.has(mouseX, mouseY) || filterDropdownOpen;
            OttonienTheme.tile(ctx, filterTile.x(), filterTile.y(), filterTile.w(), filterTile.h(), hover);
            OttonienTheme.pictoDown(ctx, filterTile.x() + 4, filterTile.y() + 6, OttonienTheme.PICTO);
        }
    }

    private InkTextField searchFieldInk() {
        return searchField instanceof InkTextField f ? f : null;
    }

    private InkTextField titleSearchFieldInk() {
        return titleSearchField instanceof InkTextField f ? f : null;
    }

    private void renderScrollbar(DrawContext ctx) {
        if (maxScroll() > 0) {
            int top = currentListTop();
            int trackH = listBottom() - top;
            int rows = visibleRows();
            int barH = Math.max(10, trackH * rows / Math.max(rows, listSize()));
            int barY = top + (trackH - barH) * listScroll / Math.max(1, maxScroll());
            ctx.fill(pageX() + pageW() - 5, barY, pageX() + pageW() - 3, barY + barH, 0xCC7A6248);
        }
    }

    private static int relationColor(LocalRpProfile p) {
        return switch (p.relation == null ? Relation.NONE : p.relation) {
            case FRIEND -> OttonienTheme.FRIEND_BLUE;
            case FOE -> OttonienTheme.FOE_RED;
            default -> 0;
        };
    }

    private net.minecraft.entity.player.SkinTextures skinFor(LocalRpProfile p) {
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        net.minecraft.client.network.PlayerListEntry entry = null;
        if (nh != null) {
            if (p.uuid != null && !p.uuid.isBlank()) {
                try {
                    entry = nh.getPlayerListEntry(java.util.UUID.fromString(p.uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (entry == null && p.accountName != null) {
                entry = nh.getPlayerListEntry(p.accountName);
            }
        }
        if (entry != null) {
            return de.ottoextra.chat.ChatHeads.skinForUuid(entry.getProfile().id(), entry.getProfile().name());
        }
        if (p.uuid != null && !p.uuid.isBlank()) {
            try {
                return de.ottoextra.chat.ChatHeads.skinForUuid(
                        java.util.UUID.fromString(p.uuid), p.accountName);
            } catch (IllegalArgumentException ignored) {
            }
        }
        java.util.UUID uuid = net.minecraft.util.Uuids.getOfflinePlayerUuid(
                p.accountName == null ? "" : p.accountName);
        return net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(uuid);
    }

    private Text stateLabel(LocalRpProfile p) {
        if (p.locked) {
            return Text.translatable("ottoextra.rpbook.lock");
        }
        return switch (p.knowledgeState) {
            case SEEN -> Text.translatable("ottoextra.rpbook.chip.seen");
            case HEARD_NAME -> Text.translatable("ottoextra.rpbook.chip.heard");
            case KNOWN -> Text.translatable("ottoextra.rpbook.state.known");
            case API_IMPORTED -> Text.translatable("ottoextra.rpbook.chip.api");
            case MANUAL, MANUAL_LOCKED -> Text.translatable("ottoextra.rpbook.chip.manual");
        };
    }

    private static int stateColor(LocalRpProfile p) {
        if (p.locked) {
            return OttonienTheme.WAX_DARK;
        }
        return switch (p.knowledgeState) {
            case SEEN, HEARD_NAME -> OttonienTheme.NEUTRAL_TAN;
            case KNOWN -> OttonienTheme.GREEN;
            case API_IMPORTED -> OttonienTheme.SLATE_TAB;
            case MANUAL, MANUAL_LOCKED -> OttonienTheme.BRASS;
        };
    }

    /** Gefülltes Zustandsetikett, rechtsbündig bei {@code xRight}; liefert die Breite. */
    private int drawStateTag(DrawContext ctx, int xRight, int y, LocalRpProfile p) {
        Text label = stateLabel(p);
        int w = textRenderer.getWidth(label) + 6;
        ctx.fill(xRight - w, y, xRight, y + 9, stateColor(p));
        ctx.drawText(textRenderer, label, xRight - w + 3, y + 1, OttonienTheme.LABEL, false);
        return w;
    }

    private String ellipsize(String value, int maxW) {
        if (textRenderer.getWidth(value) <= maxW) {
            return value;
        }
        return textRenderer.trimToWidth(value, Math.max(0, maxW - textRenderer.getWidth("…"))) + "…";
    }

    private void renderPeopleList(DrawContext ctx, int mouseX, int mouseY) {
        renderSearchChrome(ctx, mouseX, mouseY);
        int lx = pageX();
        int lw = pageW();
        int rows = visibleRows();
        int top = currentListTop();
        for (int r = 0; r < rows; r++) {
            int idx = listScroll + r;
            if (idx >= filtered.size()) {
                break;
            }
            LocalRpProfile p = filtered.get(idx);
            int yy = top + r * 22;
            if (p == selected) {
                ctx.fill(lx + 5, yy - 2, lx + lw - 5, yy + 19, OttonienTheme.PA_L);
                ctx.fill(lx + 5, yy - 2, lx + 7, yy + 19, OttonienTheme.TERRACOTTA);
            }
            int stripe = relationColor(p);
            if (stripe != 0) {
                ctx.fill(lx + 9, yy, lx + 11, yy + 17, stripe);
            }
            ctx.fill(lx + 13, yy, lx + 31, yy + 18, OttonienTheme.PA_D);
            net.minecraft.client.gui.PlayerSkinDrawer.draw(ctx, skinFor(p), lx + 14, yy + 1, 16);
            if (isOnline(p)) {
                ctx.fill(lx + 28, yy + 14, lx + 32, yy + 18, OttonienTheme.GREEN_DARK);
                ctx.fill(lx + 29, yy + 15, lx + 31, yy + 17, OttonienTheme.GREEN_LIGHT);
            }
            String line1 = p.hasRpName() ? p.rpName : LocalRpProfile.UNKNOWN_NAME;
            ctx.drawText(textRenderer, ellipsize(line1, lw - 36 - 12), lx + 36, yy,
                    p.hasRpName() ? OttonienTheme.INK : OttonienTheme.INK_MUTED, false);
            int tagW = drawStateTag(ctx, lx + lw - 9, yy + 9, p);
            int limit = lx + lw - 9 - tagW - 4;
            boolean conflict = p.apiConflict != null && !p.apiConflict.isBlank();
            if (conflict) {
                limit -= 9;
                OttonienTheme.pictoWarning(ctx, lx + lw - 14 - tagW, yy + 10, OttonienTheme.WARN_OCHRE);
            }
            ctx.drawText(textRenderer, ellipsize(p.accountName, limit - (lx + 36)), lx + 36, yy + 10,
                    OttonienTheme.INK_MUTED, false);
            if (r < rows - 1 && idx < filtered.size() - 1) {
                ctx.fill(lx + 12, yy + 20, lx + lw - 10, yy + 21, OttonienTheme.PA_S);
            }
        }
        renderScrollbar(ctx);
        Text count = filtered.size() == store.size()
                ? Text.translatable("ottoextra.rpbook.count.people", filtered.size())
                : Text.translatable("ottoextra.rpbook.count.peopleOf", filtered.size(), store.size());
        ctx.drawText(textRenderer, count,
                lx + (lw - textRenderer.getWidth(count)) / 2, pageY() + pageH() - 13,
                OttonienTheme.INK_MUTED, false);
        for (int r = 0; r < rows; r++) {
            int idx = listScroll + r;
            if (idx >= filtered.size()) {
                break;
            }
            LocalRpProfile p = filtered.get(idx);
            int yy = top + r * 22;
            if (p.apiConflict != null && !p.apiConflict.isBlank()) {
                int tagW = textRenderer.getWidth(stateLabel(p)) + 6;
                int tx = lx + lw - 14 - tagW;
                if (mouseX >= tx - 4 && mouseX < tx + 6 && mouseY >= yy + 8 && mouseY < yy + 18) {
                    ctx.drawTooltip(textRenderer, List.of(
                            Text.translatable("ottoextra.rpbook.conflict.apiName", p.apiConflict)),
                            mouseX, mouseY);
                }
            }
        }
    }

    private void renderTitleList(DrawContext ctx) {
        renderSearchChrome(ctx, -1, -1);
        int lx = pageX();
        int lw = pageW();
        int rows = visibleRows();
        int top = currentListTop();
        for (int r = 0; r < rows; r++) {
            int idx = listScroll + r;
            if (idx >= filteredTitles.size()) {
                break;
            }
            var e = filteredTitles.get(idx);
            int yy = top + r * 18;
            if (e == selectedTitle) {
                ctx.fill(lx + 6, yy - 1, lx + lw - 6, yy + 16, OttonienTheme.PA_L);
                ctx.fill(lx + 6, yy - 1, lx + 8, yy + 16, OttonienTheme.TERRACOTTA);
            }
            String hex = e.colorOverride != null && !e.colorOverride.isBlank()
                    ? e.colorOverride
                    : RpNamesServices.catalog().categories().get(e.category) != null
                            ? RpNamesServices.catalog().categories().get(e.category).color : null;
            TextColor tc = ChatNameRewriter.parseColor(hex);
            ctx.fill(lx + 12, yy + 2, lx + 19, yy + 9, OttonienTheme.PA_D);
            if (tc != null) {
                ctx.fill(lx + 13, yy + 3, lx + 18, yy + 8, 0xFF000000 | tc.getRgb());
            }
            int textW = lw - 23 - 10;
            ctx.drawText(textRenderer,
                    textRenderer.trimToWidth(String.join(" / ", e.variants), textW),
                    lx + 23, yy, e.enabled ? OttonienTheme.INK : OttonienTheme.INK_MUTED, false);
            String line2 = e.category + (e.enabled ? ""
                    : " - " + Text.translatable("ottoextra.rpbook.tchip.inactive").getString().toLowerCase(Locale.ROOT));
            ctx.drawText(textRenderer, textRenderer.trimToWidth(line2, textW), lx + 23, yy + 8,
                    OttonienTheme.INK_MUTED, false);
            if (r < rows - 1 && idx < filteredTitles.size() - 1) {
                ctx.fill(lx + 10, yy + 16, lx + lw - 10, yy + 17, OttonienTheme.PA_S);
            }
        }
        renderScrollbar(ctx);
    }

    private void renderDropdown(DrawContext ctx, int mouseX, int mouseY) {
        List<FilterOption> opts = filterOptions();
        int dx = dropdownX();
        int dy = dropdownY();
        int rh = dropdownRowH();
        int dw = dropdownW();
        int dh = opts.size() * rh;
        ctx.fill(dx - 1, dy - 1, dx + dw + 1, dy + dh + 1, COL_BORDER);
        ctx.fill(dx, dy, dx + dw, dy + dh, 0xF8232329);
        String curId = currentFilterId();
        for (int i = 0; i < opts.size(); i++) {
            FilterOption o = opts.get(i);
            int ry = dy + i * rh;
            boolean hover = mouseX >= dx && mouseX <= dx + dw && mouseY >= ry && mouseY < ry + rh;
            if (hover) {
                ctx.fill(dx, ry, dx + dw, ry + rh, COL_SELECTED);
            }
            ctx.drawText(textRenderer, o.label(), dx + 4, ry + 3,
                    o.id().equals(curId) ? COL_ONLINE : COL_TEXT, false);
        }
    }

    private void renderLeafBackground(DrawContext ctx) {
        OttonienTheme.leaf(ctx, lfx, lfy, lfw, lfh, 32 + tab.ordinal(), tab != Tab.IMPORT);
    }

    private Box backBox() {
        return new Box(lfx + lfw - 32, lfy + 22, 22, 12);
    }

    private void renderBackTile(DrawContext ctx, int mouseX, int mouseY) {
        if (!narrow || !leafOverlay) {
            return;
        }
        Box b = backBox();
        OttonienTheme.tile(ctx, b.x(), b.y(), b.w(), b.h(), b.has(mouseX, mouseY));
        ctx.drawText(textRenderer, "◂", b.x() + 8, b.y() + 2, OttonienTheme.PICTO, false);
    }

    private void drawBold(DrawContext ctx, String s, int x, int y, int color, int maxW) {
        String shown = textRenderer.trimToWidth(s, maxW);
        ctx.drawText(textRenderer, Text.literal(shown).formatted(net.minecraft.util.Formatting.BOLD),
                x, y, color, false);
    }

    private void renderPersonLeaf(DrawContext ctx, int mouseX, int mouseY) {
        int x = lfx + 10;
        int y = lfy + 10;
        int w = lfw - 20;
        relationFriendBox = null;
        relationFoeBox = null;
        colorsToggle = null;
        if (selected == null) {
            ctx.drawText(textRenderer, ellipsize(
                    Text.translatable("ottoextra.rpbook.select").getString(), w - 16),
                    x, y + 2, OttonienTheme.INK_MUTED, false);
            return;
        }
        renderBackTile(ctx, mouseX, mouseY);
        String name = selected.hasRpName() ? selected.rpName : LocalRpProfile.UNKNOWN_NAME;
        if (page == Page.COLORS) {
            drawBold(ctx, name, x, y + 1, OttonienTheme.INK, w - 20);
            colorsToggle = new Box(x, y + 11, 56, 11);
            OttonienTheme.chip(ctx, colorsToggle.x(), colorsToggle.y(), colorsToggle.w(), 11,
                    OttonienTheme.BRASS, false);
            ctx.drawText(textRenderer, "‹ " + Text.translatable("ottoextra.rpbook.colors.back").getString(),
                    x + 4, y + 13, OttonienTheme.INK_MUTED, false);
            String[] cats = {Text.translatable("ottoextra.rpbook.colors.chat").getString(),
                    Text.translatable("ottoextra.rpbook.colors.tab").getString(),
                    Text.translatable("ottoextra.rpbook.colors.tag").getString()};
            int cy = lfy + 36;
            for (int row = 0; row < 3; row++) {
                ctx.drawText(textRenderer, cats[row], x, cy + row * 28 + 2, OttonienTheme.INK, false);
                ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.colors.nameShort"),
                        x + 4, cy + row * 28 + 15, OttonienTheme.INK_MUTED, false);
            }
            for (TextFieldWidget f : colorFields) {
                drawSwatch(ctx, f);
                drawResetIcon(ctx, f);
            }
            return;
        }
        // Kopf: Porträt, Name, Account, Zustandsetikett
        OttonienTheme.goldFrame(ctx, x, y, 28, 28);
        net.minecraft.client.gui.PlayerSkinDrawer.draw(ctx, skinFor(selected), x + 2, y + 2, 24);
        drawBold(ctx, name, x + 34, y + 1, OttonienTheme.INK, w - 34 - 20);
        ctx.drawText(textRenderer, ellipsize(selected.accountName, w - 34 - 4), x + 34, y + 11,
                OttonienTheme.INK_MUTED, false);
        int tagW = drawStateTag(ctx, x + 34 + textRenderer.getWidth(stateLabel(selected)) + 6, y + 20,
                selected);
        boolean conflict = selected.apiConflict != null && !selected.apiConflict.isBlank();
        if (conflict) {
            int cx = x + 34 + tagW + 6;
            int room = x + w - 32 - cx;
            if (room > 20) {
                ctx.drawText(textRenderer, ellipsize("⚠ API: " + selected.apiConflict, room), cx,
                        y + 21, OttonienTheme.WARN_OCHRE, false);
            }
        }
        // Beziehung
        int fy = y + 36;
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.relation.label"), x, fy + 1,
                OttonienTheme.INK_MUTED, false);
        Relation rel = selected.relation == null ? Relation.NONE : selected.relation;
        int cx = x + 52;
        for (Relation r : new Relation[]{Relation.FRIEND, Relation.FOE}) {
            String label = Text.translatable(r == Relation.FRIEND
                    ? "ottoextra.rpbook.relation.friend" : "ottoextra.rpbook.relation.foe").getString();
            int cw = textRenderer.getWidth(label) + 8;
            boolean on = rel == r;
            int color = r == Relation.FRIEND ? OttonienTheme.FRIEND_BLUE : OttonienTheme.FOE_RED;
            OttonienTheme.chip(ctx, cx, fy, cw, 11, color, on);
            ctx.drawText(textRenderer, label, cx + 4, fy + 2,
                    on ? OttonienTheme.LABEL : OttonienTheme.INK_MUTED, false);
            Box box = new Box(cx, fy, cw, 11);
            if (r == Relation.FRIEND) {
                relationFriendBox = box;
            } else {
                relationFoeBox = box;
            }
            cx += cw + 3;
        }
        ctx.fill(x, fy + 15, x + w, fy + 16, OttonienTheme.PA_S);
        fy += 21;
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.rpname"), x, fy,
                OttonienTheme.INK_MUTED, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titleField"), x, fy + 18,
                OttonienTheme.INK_MUTED, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.notes"), x, fy + 36,
                OttonienTheme.INK_MUTED, false);
        drawResetIcon(ctx, rpNameField);
        drawResetIcon(ctx, titleField);

        // "Farben ›" öffnet die Farbseite (rechtsbündig unter den Kästchen).
        String toggle = Text.translatable("ottoextra.rpbook.colors.page").getString() + " ›";
        int tw = textRenderer.getWidth(toggle) + 8;
        colorsToggle = new Box(x + w - tw, fy + 78, tw, 11);
        OttonienTheme.chip(ctx, colorsToggle.x(), colorsToggle.y(), tw, 11, OttonienTheme.BRASS, false);
        ctx.drawText(textRenderer, toggle, colorsToggle.x() + 4, colorsToggle.y() + 2,
                OttonienTheme.INK_MUTED, false);
    }

    private void renderTitleLeaf(DrawContext ctx) {
        int x = lfx + 9;
        int y = lfy + 10;
        int w = lfw - 18;
        int fy = y + 14;
        renderBackTile(ctx, -1, -1);
        String head = selectedTitle != null
                ? (selectedTitle.title == null || selectedTitle.title.isBlank() ? "…" : selectedTitle.title)
                : Text.translatable("ottoextra.rpbook.titles.select").getString();
        if (selectedTitle != null) {
            drawBold(ctx, head, x, y, OttonienTheme.INK, w - 40);
            String source = "WIKI_IMPORT".equals(selectedTitle.source) ? "Wiki" : "Manuell";
            int sourceX = x + textRenderer.getWidth(head) + 8;
            if (sourceX + textRenderer.getWidth(source) <= x + w - 14) {
                ctx.drawText(textRenderer, source, sourceX, y, OttonienTheme.INK_MUTED, false);
            }
        } else {
            ctx.drawText(textRenderer, head, x, y, OttonienTheme.INK_MUTED, false);
            return;
        }
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titles.nameLabel"), x, fy,
                OttonienTheme.INK_MUTED, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titles.variantLabel"), x, fy + 14,
                OttonienTheme.INK_MUTED, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titles.variantLabel"), x, fy + 28,
                OttonienTheme.INK_MUTED, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titles.category"), x, fy + 43,
                OttonienTheme.INK_MUTED, false);
        for (int i = 0; i < categoryChipBoxes.size(); i++) {
            Box b = categoryChipBoxes.get(i);
            String key = categoryChipKeys.get(i);
            boolean plus = "+".equals(key);
            boolean on = plus ? newCategoryOpen : key.equals(catCategoryValue);
            OttonienTheme.chip(ctx, b.x(), b.y(), b.w(), b.h(), OttonienTheme.BRASS, on);
            String label = plus ? "+" : textRenderer.trimToWidth(categoryLabel(key), b.w() - 6);
            int tx = b.x() + (b.w() - textRenderer.getWidth(label)) / 2;
            ctx.drawText(textRenderer, label, tx, b.y() + 2,
                    on ? OttonienTheme.LABEL : OttonienTheme.INK_MUTED, false);
        }
        int colorY = catColorField.getY();
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titles.colorTitle"), x, colorY + 2,
                OttonienTheme.INK, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titles.colorName"), x,
                catNameColorField.getY() + 2, OttonienTheme.INK, false);
        drawSwatch(ctx, catColorField);
        drawSwatch(ctx, catNameColorField);
        drawResetIcon(ctx, catTitleField);
        drawResetIcon(ctx, catVariant1Field);
        drawResetIcon(ctx, catVariant2Field);
        drawResetIcon(ctx, catColorField);
        drawResetIcon(ctx, catNameColorField);
        if (titlePreviewY >= 0) {
            ctx.fill(x, titlePreviewY, x + w, titlePreviewY + 12, OttonienTheme.PREVIEW_DARK);
            String variant = catVariant1Field.getText().isBlank() ? catTitleField.getText()
                    : catVariant1Field.getText();
            int titleColor = previewArgb(normalizeHex(catColorField.getText()), COL_TITLE);
            int nameColor = previewArgb(normalizeHex(catNameColorField.getText()), COL_TITLE);
            ctx.drawText(textRenderer, variant, x + 3, titlePreviewY + 2, titleColor, true);
            ctx.drawText(textRenderer, "Ansgar", x + 3 + textRenderer.getWidth(variant + " "),
                    titlePreviewY + 2, nameColor, true);
        }
    }

    private void renderImportPage(DrawContext ctx) {
        int x = pageX() + 10;
        int w = pageW() - 20;
        int ly = pageY();
        drawBold(ctx, Text.translatable("ottoextra.rpbook.import.heading").getString(), x, ly + 7,
                OttonienTheme.INK, w);
        ctx.fill(x, ly + 17, x + w, ly + 18, OttonienTheme.PA_S);
        int step = importStep();
        String[][] sections = {
                {"ottoextra.rpbook.import.sec.api", "ottoextra.rpbook.import.sec.api.hint"},
                {"ottoextra.rpbook.import.sec.ottoplus", "ottoextra.rpbook.import.sec.ottoplus.hint"},
                {"ottoextra.rpbook.import.sec.backup", null}};
        for (int i = 0; i < 3; i++) {
            int y = ly + 24 + i * step;
            OttonienTheme.tile(ctx, x, y, 16, 16, false);
            switch (i) {
                case 0 -> OttonienTheme.pictoCloud(ctx, x + 4, y + 4, OttonienTheme.PICTO);
                case 1 -> OttonienTheme.pictoFile(ctx, x + 4, y + 4, OttonienTheme.PICTO);
                default -> OttonienTheme.pictoChest(ctx, x + 4, y + 4, OttonienTheme.PICTO);
            }
            drawBold(ctx, Text.translatable(sections[i][0]).getString(), x + 20, y + 1,
                    OttonienTheme.INK, w - 20);
            String hint = sections[i][1] != null ? Text.translatable(sections[i][1]).getString()
                    : lastBackupText.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.import.sec.backup.none").getString()
                    : Text.translatable("ottoextra.rpbook.import.sec.backup.last", lastBackupText).getString();
            ctx.drawText(textRenderer, textRenderer.trimToWidth(hint, w - 20), x + 20, y + 10,
                    OttonienTheme.INK_MUTED, false);
            if (i < 2 || true) {
                ctx.fill(x, y + step - 4, x + w, y + step - 3, OttonienTheme.PA_S);
            }
        }
        if (!statusLine.isEmpty()) {
            ctx.drawText(textRenderer, textRenderer.trimToWidth(statusLine, w), x,
                    ly + pageH() - 14, OttonienTheme.INK_MUTED, false);
        }
    }

    private void renderImportLeaf(DrawContext ctx) {
        if (narrow) {
            return;
        }
        int x = lfx + 9;
        int w = lfw - 18;
        OttonienTheme.waxSeal(ctx, x + 6, lfy + 14, 6);
        drawBold(ctx, Text.translatable("ottoextra.rpbook.import.warn").getString(), x + 16, lfy + 10,
                OttonienTheme.WAX_TEXT, w - 16);
        String spoiler = Text.translatable("ottoextra.rpbook.import.spoiler").getString();
        int colon = spoiler.indexOf(": ");
        if (colon > 0 && colon < 14) {
            spoiler = spoiler.substring(colon + 2);
        }
        int y = lfy + 26;
        int limit = lfy + lfh - 34;
        y = drawWrapped(ctx, spoiler, x, y, w, OttonienTheme.INK, limit);
        y += 6;
        y = drawWrapped(ctx, Text.translatable("ottoextra.rpbook.import.hintKnown").getString(), x, y, w,
                OttonienTheme.INK_MUTED, limit);
        y = drawWrapped(ctx, Text.translatable("ottoextra.rpbook.import.hintAll").getString(), x, y, w,
                OttonienTheme.INK_MUTED, limit);
        if (y + 6 < lfy + lfh - 12) {
            ctx.fill(x, y + 3, x + w, y + 4, OttonienTheme.PA_S);
            drawWrapped(ctx, Text.translatable("ottoextra.rpbook.import.hintBackup").getString(), x,
                    y + 8, w, OttonienTheme.INK_MUTED, lfy + lfh - 4);
        }
    }

    private int drawWrapped(DrawContext ctx, String text, int x, int y, int w, int color, int limitY) {
        for (String line : wrap(text, w)) {
            if (y + 9 > limitY) {
                return y;
            }
            ctx.drawText(textRenderer, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private static String stateShort(KnowledgeState s) {
        return switch (s) {
            case SEEN -> "Gesehen";
            case HEARD_NAME -> "Gehört";
            case KNOWN -> "Bekannt";
            case API_IMPORTED -> "API";
            case MANUAL -> "Manuell";
            case MANUAL_LOCKED -> "Gesperrt";
        };
    }

    private static final int RESET_ICON_SIZE = 11;

    private int resetIconX(TextFieldWidget f) {
        return f.getX() + f.getWidth() - RESET_ICON_SIZE - 1;
    }

    private int resetIconY(TextFieldWidget f) {
        return f.getY() + (f.getHeight() - RESET_ICON_SIZE) / 2;
    }

    private boolean resetIconHit(TextFieldWidget f, double mx, double my) {
        if (f == null || !f.visible) {
            return false;
        }
        int ix = resetIconX(f);
        int iy = resetIconY(f);
        return mx >= ix && mx <= ix + RESET_ICON_SIZE && my >= iy && my <= iy + RESET_ICON_SIZE;
    }

    private void drawResetIcon(DrawContext ctx, TextFieldWidget f) {
        if (f == null) {
            return;
        }
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(resetIconX(f), resetIconY(f));
        m.scale(0.7f, 0.7f);
        ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, RESET_ICON,
                0, 0, 0f, 0f, 16, 16, 16, 16);
        m.popMatrix();
    }

    private static final net.minecraft.util.Identifier RESET_ICON =
            de.ottoextra.OttoExtra.id("textures/gui/reset.png");

    private void renderGroupLabels(DrawContext ctx) {
        drawSwatch(ctx, groupTitleColorField);
        drawSwatch(ctx, groupNameColorField);
        ctx.drawText(textRenderer,
                Text.translatable("ottoextra.rpbook.groups.hint").getString(),
                editX(), contentY(), COL_MUTED, false);
    }

    private List<String> wrap(String line, int maxW) {
        if (line.isEmpty()) {
            return List.of("");
        }
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textRenderer.getWidth(candidate) > maxW && !current.isEmpty()) {
                out.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        out.add(current.toString());
        return out;
    }

    private static int previewArgb(String hex, int def) {
        net.minecraft.text.TextColor c = ChatNameRewriter.parseColor(hex);
        return c != null ? (0xFF000000 | c.getRgb()) : def;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private void drawSwatch(DrawContext ctx, TextFieldWidget field) {
        if (field == null || !field.visible) {
            return;
        }
        TextColor color = ChatNameRewriter.parseColor(field.getText().trim());
        int rgb = color == null ? 0xFF888888 : 0xFF000000 | color.getRgb();
        int sx = field.getX() + field.getWidth() + 2;
        int sy = field.getY() + 2;
        ctx.fill(sx - 1, sy - 1, sx + 11, sy + 11, COL_BORDER);
        ctx.fill(sx, sy, sx + 10, sy + 10, rgb);
    }

    @Override
    public void close() {
        savePendingPerson();
        store.saveNow();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
