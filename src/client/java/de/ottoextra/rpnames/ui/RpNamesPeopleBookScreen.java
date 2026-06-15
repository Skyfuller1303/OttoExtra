package de.ottoextra.rpnames.ui;

import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.chat.ChatNameRewriter;
import de.ottoextra.rpnames.importer.RegionsApiRpNameImporter;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.tablist.TablistNameFormatter;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
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

/**
 * RP-Personenbuch: lokales Adressbuch für RP-Wissen.
 *
 * <p>Adaptives Panel, drei Tabs:</p>
 * <ul>
 *   <li><b>Personen</b> — Filter-Chips + Suche + zweizeilige Liste links,
 *       Identitäts-Editor mit Farben pro Anzeigeort (Chat/Tab/Schild) in der
 *       Mitte, Live-Vorschau (echte Formatter) + Konfliktpanel rechts.</li>
 *   <li><b>Gruppen</b> — Titelgruppen-Editor (Label, Farben, Titel).</li>
 *   <li><b>Import</b> — API-Import-Modi mit Spoiler-Warnung, Backup,
 *       Alle vergessen.</li>
 * </ul>
 * Local-first: Speichern = MANUAL(_LOCKED); Import/Lernen überschreibt nie.
 */
public final class RpNamesPeopleBookScreen extends Screen {

    // Vanilla-Dark-Palette (wie Settings-GUI / MoreCulling-Stil).
    // Hintergrund liefert super.render (gedimmt/geblurrt); Karten transluzent.
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

    /**
     * Titel-Tab-Filter: feste Status-Filter + dynamisch ALLE Katalog-Kategorien
     * (inkl. neu angelegter). IDs mit Prefix {@link #TF_CAT_PREFIX} stehen für
     * eine Kategorie. Früher hartkodiertes Enum -> neue Kategorien waren nicht
     * filterbar.
     */
    private static final String TF_ALL = "__all__";
    private static final String TF_ACTIVE = "__active__";
    private static final String TF_INACTIVE = "__inactive__";
    private static final String TF_MANUAL = "__manual__";
    private static final String TF_WIKI = "__wiki__";
    private static final String TF_CAT_PREFIX = "cat:";

    /** Ein Eintrag im Filter-Dropdown (ID + Anzeigename). */
    private record FilterOption(String id, Text label) {
    }

    /** Filter-Chips. */
    private enum Chip {
        ALL("ottoextra.rpbook.chip.all"),
        UNKNOWN("ottoextra.rpbook.chip.unknown"),
        SEEN("ottoextra.rpbook.chip.seen"),
        HEARD("ottoextra.rpbook.chip.heard"),
        MANUAL("ottoextra.rpbook.chip.manual"),
        API("ottoextra.rpbook.chip.api"),
        CONFLICT("ottoextra.rpbook.chip.conflict"),
        ONLINE("ottoextra.rpbook.chip.online");

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

    // Personen-Tab
    private TextFieldWidget searchField;
    private TextFieldWidget rpNameField;
    private TextFieldWidget titleField;
    private TextFieldWidget notesField;
    private CheckboxWidget lockCheckbox;
    private CheckboxWidget titleLockCheckbox;
    /** Unterdrückt das Auto-Sperren während programmatischem Befüllen (select). */
    private boolean suppressLockAuto;
    private ButtonWidget chatFlagButton;
    private ButtonWidget tabFlagButton;
    private ButtonWidget tagFlagButton;
    private final TextFieldWidget[] colorFields = new TextFieldWidget[6];
    private ButtonWidget copyChatColorsButton;
    private ButtonWidget saveButton;
    private ButtonWidget forgetButton;
    private ButtonWidget keepLocalButton;
    private ButtonWidget takeApiButton;
    private final List<ButtonWidget> chipButtons = new ArrayList<>();
    /** Aufklappbarer Filter (statt Chip-Reihen — ragte in die Liste). */
    private boolean filterDropdownOpen;
    private ButtonWidget filterButton;

    private final List<LocalRpProfile> filtered = new ArrayList<>();
    private final Set<String> onlineNamesLower = new HashSet<>();
    private LocalRpProfile selected;

    // 3D-Spielervorschau (gecacht je Account)
    private net.minecraft.client.network.AbstractClientPlayerEntity previewEntity;
    private String previewEntityKey;
    /** Konstante UUID für den fixen Fallback-Skin. */
    private static final java.util.UUID FALLBACK_SKIN_UUID =
            new java.util.UUID(0L, 0L);
    /** Marken-Fallback-Skin-Textur (assets/ottoextra/textures/entity/fallback_skin.png). */
    private static net.minecraft.entity.player.SkinTextures fallbackSkinCache;

    private static net.minecraft.entity.player.SkinTextures fallbackSkin() {
        if (fallbackSkinCache == null) {
            net.minecraft.util.AssetInfo.TextureAsset tex =
                    new net.minecraft.util.AssetInfo.TextureAssetInfo(
                            de.ottoextra.OttoExtra.id("textures/entity/fallback_skin.png"));
            fallbackSkinCache = new net.minecraft.entity.player.SkinTextures(
                    tex, null, null, net.minecraft.entity.player.PlayerSkinType.WIDE, true);
        }
        return fallbackSkinCache;
    }

    // Gruppen-Tab
    private TextFieldWidget groupLabelField;
    private TextFieldWidget groupTitleColorField;
    private TextFieldWidget groupNameColorField;
    private TextFieldWidget groupTitlesField;
    private ButtonWidget groupSaveButton;
    private final List<String> groupKeys = new ArrayList<>();
    private String selectedGroupKey;

    // Titel-Tab
    private String titleFilter = TF_ALL;
    private TextFieldWidget titleSearchField;
    private TextFieldWidget catTitleField;
    private TextFieldWidget catVariant1Field;
    private TextFieldWidget catVariant2Field;
    private TextFieldWidget catColorField;
    private TextFieldWidget catNameColorField;
    private ButtonWidget catCategoryButton;
    private TextFieldWidget catNewCategoryField;
    private CheckboxWidget catEnabledCheckbox;
    private CheckboxWidget catOverrideColorCheckbox;
    private ButtonWidget catSaveButton;
    private ButtonWidget catDeleteButton;
    private final List<ButtonWidget> titleChipButtons = new ArrayList<>();
    private final List<de.ottoextra.rpnames.title.TitleCatalogStore.Entry> filteredTitles = new ArrayList<>();
    private de.ottoextra.rpnames.title.TitleCatalogStore.Entry selectedTitle;
    private String catCategoryValue = "unclassified";

    // Import-Tab
    private ButtonWidget forgetAllButton;
    private boolean forgetAllArmed = false;
    private boolean importRunning = false;

    private int listScroll = 0;
    private volatile String statusLine = "";
    /** Aktueller Autofill-Vorschlag fürs Titel-Feld (Tab übernimmt). */
    private String titleAutofill;

    /** Beim Öffnen vorzuselektierender Account (Shift-Klick), sonst null. */
    private String pendingSelectAccount;

    public RpNamesPeopleBookScreen(Screen parent) {
        super(Text.translatable("ottoextra.rpbook.title"));
        this.parent = parent;
        this.store = RpNamesServices.store();
        this.titles = RpNamesServices.titles();
    }

    public RpNamesPeopleBookScreen(Screen parent, String selectAccount) {
        this(parent);
        this.pendingSelectAccount = selectAccount;
    }

    /**
     * Personenbuch beim Eintrag einer Person öffnen (Shift-Klick im Chat /
     * Shift-Rechtsklick auf Spieler). Legt den Eintrag bei Bedarf an.
     */
    public static void openFor(Screen parent, String rawName, String uuid) {
        LocalRpIdentityStore store = RpNamesServices.store();
        if (store == null || rawName == null || rawName.isBlank()) {
            return;
        }
        LocalRpProfile existing = RpNamesServices.findProfileByAnyName(rawName.trim());
        String account = existing != null ? existing.accountName : rawName.trim();
        store.ensureSeen(account, uuid,
                de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
        MinecraftClient.getInstance().setScreen(new RpNamesPeopleBookScreen(parent, account));
    }

    // ---- Adaptives Layout -------------------------------------

    private int panelW() {
        return Math.max(520 > width - 16 ? width - 16 : 520, Math.min(width - 48, 900));
    }

    private int panelH() {
        return height;
    }

    private int panelX() {
        return Math.max(4, (width - panelW()) / 2);
    }

    private int panelY() {
        return 0; // Vollbild-Layout wie Settings-GUI
    }

    private int contentY() {
        return panelY() + 40;
    }

    private int contentBottom() {
        return height - 36; // Platz für Footer-Button
    }

    private int listX() {
        return panelX() + 8;
    }

    private int listW() {
        return Math.max(180, Math.min(230, panelW() / 4 + 40));
    }

    private int listTop() {
        return contentY() + 42;
    }

    private int listBottom() {
        return contentBottom() - 14;
    }

    private int editX() {
        return listX() + listW() + 10;
    }

    private int editW() {
        int right = previewVisible() ? previewX() - 8 : panelX() + panelW() - 8;
        return right - editX();
    }

    /** Breite der Eingabe-Felder im Editor (gekappt, damit rechts Platz fürs
     *  3D-Modell bleibt). Account-Label nutzt 50px links der Felder. */
    private int peopleFieldsW() {
        int avail = editW() - 50;
        return Math.max(120, Math.min(avail, 210));
    }

    /** Linke Kante der 3D-Modell-Spalte (rechts neben den Feldern). */
    private int modelX() {
        return editX() + 50 + peopleFieldsW() + 12;
    }

    private boolean previewVisible() {
        return panelW() >= 640;
    }

    private int previewX() {
        return panelX() + panelW() - 8 - previewW();
    }

    private int previewW() {
        return Math.max(180, Math.min(260, panelW() / 4));
    }

    // ---- Init ----------------------------------------------------------------

    @Override
    protected void init() {
        chipButtons.clear();
        refreshOnline();

        // Tab-Leiste, zentriert (wie Settings-GUI)
        // Gruppen-Tab entfernt — Gruppen sind titelgebunden (Titelkatalog-Kategorien)
        String[] tabKeys = {"ottoextra.rpbook.tab.people",
                "ottoextra.rpbook.tab.titles", "ottoextra.rpbook.tab.import"};
        int tabsTotal = 0;
        for (String k : tabKeys) {
            tabsTotal += textRenderer.getWidth(Text.translatable(k)) + 14 + 4;
        }
        int tx = Math.max(panelX() + 8, (width - tabsTotal) / 2);
        int ty = panelY() + 20;
        tx = tabButton(tx, ty, "ottoextra.rpbook.tab.people", Tab.PEOPLE);
        tx = tabButton(tx, ty, "ottoextra.rpbook.tab.titles", Tab.TITLES);
        tabButton(tx, ty, "ottoextra.rpbook.tab.import", Tab.IMPORT);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 75, height - 26, 150, 20).build());

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

    /** Vorauswahl per openFor: Person filtern + im Editor selektieren. */
    private void selectPendingIfAny() {
        if (pendingSelectAccount == null) {
            return;
        }
        LocalRpProfile p = store != null ? store.findByName(pendingSelectAccount).orElse(null) : null;
        pendingSelectAccount = null;
        if (p == null) {
            return;
        }
        if (searchField != null) {
            searchField.setText(p.accountName);
        }
        refilter();
        select(p);
    }

    private int tabButton(int x, int y, String key, Tab target) {
        int w = textRenderer.getWidth(Text.translatable(key)) + 14;
        ButtonWidget btn = ButtonWidget.builder(Text.translatable(key), b -> {
            tab = target;
            listScroll = 0;
            clearAndInit();
        }).dimensions(x, y, w, 16).build();
        btn.active = tab != target;
        addDrawableChild(btn);
        return x + w + 4;
    }

    // ---- Personen-Tab ----------------------------------------------------------

    private void initPeople() {
        searchField = new TextFieldWidget(textRenderer, listX(), contentY(), listW(), 16,
                Text.translatable("ottoextra.rpbook.search"));
        searchField.setSuggestion(Text.translatable("ottoextra.rpbook.search").getString());
        searchField.setChangedListener(s -> {
            searchField.setSuggestion(s.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.search").getString() : "");
            listScroll = 0;
            refilter();
        });
        addDrawableChild(searchField);

        // Filter als Dropdown (aufklappbar, ragt nie in die Liste)
        filterDropdownOpen = false;
        filterButton = ButtonWidget.builder(filterLabel(), b ->
                        filterDropdownOpen = !filterDropdownOpen)
                .dimensions(listX(), contentY() + 20, listW(), 16).build();
        addDrawableChild(filterButton);

        int labelW = 50;
        int x = editX() + labelW;
        int w = peopleFieldsW();
        int y = contentY() + 12;

        rpNameField = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        rpNameField.setMaxLength(48);
        rpNameField.setChangedListener(s -> autoLock());
        addDrawableChild(rpNameField);
        y += 21;
        titleField = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        titleField.setMaxLength(48);
        // Autofill aus dem Titelkatalog: grauer Vorschlag, Tab übernimmt
        titleField.setChangedListener(s -> {
            // Titel-Edit sperrt NUR den Titel (nicht den Spieler).
            autoLockTitle();
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
        addDrawableChild(titleField);
        y += 21;

        // Gruppe ist titelgebunden (readonly, steht in der Kopfzeile).
        // Zwei Sperren: ganzer Spieler (links) und nur der Titel (rechts).
        lockCheckbox = CheckboxWidget.builder(Text.translatable("ottoextra.rpbook.lock"), textRenderer)
                .pos(x, y - 1).build();
        addDrawableChild(lockCheckbox);
        titleLockCheckbox = CheckboxWidget.builder(
                Text.translatable("ottoextra.rpbook.titleLock"), textRenderer)
                .pos(x + w / 2 + 2, y - 1).build();
        addDrawableChild(titleLockCheckbox);
        y += 21;

        // Farben pro Anzeigeort: 3 Zeilen (Chat/Tab/Schild) à Titel+Name,
        // vorbefüllt mit der effektiven Default-Farbe; Abweichung = Override
        int fieldW = (w - 16 - 4) / 2 - 12;
        for (int row = 0; row < 3; row++) {
            int fy = y + 12 + row * 19;
            colorFields[row * 2] = colorField(x, fy, fieldW);
            colorFields[row * 2 + 1] = colorField(x + fieldW + 16, fy, fieldW);
        }
        y += 12 + 3 * 19 + 2;

        // "Farben löschen" entfernt — Reset pro Farbe über Icon im Feld (siehe render/mouseClicked)
        copyChatColorsButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.colors.copyChat"), b -> {
            colorFields[2].setText(colorFields[0].getText());
            colorFields[3].setText(colorFields[1].getText());
            colorFields[4].setText(colorFields[0].getText());
            colorFields[5].setText(colorFields[1].getText());
        }).dimensions(x, y, w, 14).build();
        addDrawableChild(copyChatColorsButton);
        y += 17;

        chatFlagButton = flagButton(x, y, w / 3 - 2, "ottoextra.rpbook.flag.chat",
                () -> selected != null && selected.showInChat,
                v -> { if (selected != null) selected.showInChat = v; });
        tabFlagButton = flagButton(x + w / 3, y, w / 3 - 2, "ottoextra.rpbook.flag.tab",
                () -> selected != null && selected.showInTablist,
                v -> { if (selected != null) selected.showInTablist = v; });
        tagFlagButton = flagButton(x + 2 * w / 3, y, w - 2 * (w / 3), "ottoextra.rpbook.flag.tag",
                () -> selected != null && selected.showInNametag,
                v -> { if (selected != null) selected.showInNametag = v; });
        y += 19;

        notesField = field(x, y, w, 200, "ottoextra.rpbook.notes");
        y += 21;

        saveButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.save"), b -> savePerson())
                .dimensions(x, y, w / 2 - 2, 16).build();
        addDrawableChild(saveButton);
        forgetButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.forget"), b -> forget())
                .dimensions(x + w / 2 + 2, y, w / 2 - 2, 16).build();
        addDrawableChild(forgetButton);

        // Konflikt-Aktionen (rechts unten im Vorschau-Bereich)
        if (previewVisible()) {
            int px = previewX();
            int pw = previewW();
            int py = contentBottom() - 36;
            keepLocalButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.conflict.keep"),
                    b -> resolveConflict(false)).dimensions(px, py, pw / 2 - 2, 14).build();
            addDrawableChild(keepLocalButton);
            takeApiButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.conflict.take"),
                    b -> resolveConflict(true)).dimensions(px + pw / 2 + 2, py, pw / 2 - 2, 14).build();
            addDrawableChild(takeApiButton);
        }

        refilter();
        if (selected != null) {
            select(selected);
        } else {
            setPeopleEditEnabled(false);
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

    private TextFieldWidget colorField(int x, int y, int w) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 14, Text.empty());
        f.setMaxLength(7);
        f.setChangedListener(s -> autoLock());
        addDrawableChild(f);
        return f;
    }

    /** Manuelle Änderung an RP-Name/Farbe -> Profil automatisch sperren. */
    private void autoLock() {
        if (suppressLockAuto || tab != Tab.PEOPLE || selected == null || lockCheckbox == null
                || !lockCheckbox.active || lockCheckbox.isChecked()) {
            return;
        }
        lockCheckbox.onPress(null);
    }

    /** Manuelle Titel-Änderung -> nur den Titel sperren (Spieler bleibt aktiv). */
    private void autoLockTitle() {
        if (suppressLockAuto || tab != Tab.PEOPLE || selected == null || titleLockCheckbox == null
                || !titleLockCheckbox.active || titleLockCheckbox.isChecked()) {
            return;
        }
        titleLockCheckbox.onPress(null);
    }

    private ButtonWidget flagButton(int x, int y, int w, String key,
                                    java.util.function.BooleanSupplier get,
                                    java.util.function.Consumer<Boolean> set) {
        ButtonWidget btn = ButtonWidget.builder(flagLabel(key, get.getAsBoolean()), b -> {
            boolean next = !get.getAsBoolean();
            set.accept(next);
            b.setMessage(flagLabel(key, next));
        }).dimensions(x, y, w, 16).build();
        addDrawableChild(btn);
        return btn;
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

    /** Label des Filter-Dropdowns: "Filter: <aktiv>". */
    private Text filterLabel() {
        return Text.translatable("ottoextra.rpbook.filter", currentFilterLabel());
    }

    /** Anzeigename des aktuell gewählten Filters. */
    private Text currentFilterLabel() {
        String cur = tab == Tab.TITLES ? titleFilter : chip.name();
        for (FilterOption o : filterOptions()) {
            if (o.id().equals(cur)) {
                return o.label();
            }
        }
        return Text.literal("?");
    }

    /**
     * Dropdown-Optionen für den aktiven Tab. Titel-Tab: feste Status-Filter +
     * dynamisch alle Katalog-Kategorien (Anzeigename = Kategorie-Label).
     */
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

    private int dropdownX() {
        return listX();
    }

    private int dropdownY() {
        return contentY() + 36;
    }

    private int dropdownRowH() {
        return 13;
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
        // Default-Sortierung: online > Konflikt > manuell > Rest
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

    /**
     * Effektive Default-Farben (ohne Spieler-Override) für die 6 Felder:
     * Titel = Katalog (Override/Kategorie) bzw. Legacy-Gruppe, Name =
     * Titel-Namensfarbe (falls gesetzt) sonst Standard-Namensfarbe (#c7a87f) —
     * Reihenfolge chat/tab/tag × titel/name.
     */
    private String[] defaultColorsFor(String title) {
        var catalog = RpNamesServices.catalog();
        String titleColor = catalog != null ? catalog.titleColor(title).orElse(null) : null;
        if (titleColor == null) {
            titleColor = titles.find(title == null ? "" : title)
                    .map(r -> r.group().titleColor).orElse("");
        }
        // Titel-eigene Namensfarbe ist der Default, sonst globale Default-Farbe.
        String nameColor = catalog != null
                ? catalog.titleNameColor(title).orElse(catalog.defaultNameColor())
                : "#c7a87f";
        return new String[]{titleColor, nameColor, titleColor, nameColor, titleColor, nameColor};
    }

    /** Gruppen-/Kategorienname des Titels (titelgebunden, readonly). */
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
        selected = profile;
        if (profile == null) {
            setPeopleEditEnabled(false);
            return;
        }
        suppressLockAuto = true;
        rpNameField.setText(profile.hasRpName() ? profile.rpName : "");
        titleField.setText(profile.title == null ? "" : profile.title);
        notesField.setText(profile.notes == null ? "" : profile.notes);
        if (lockCheckbox.isChecked() != profile.locked) {
            lockCheckbox.onPress(null);
        }
        if (titleLockCheckbox.isChecked() != profile.titleLocked) {
            titleLockCheckbox.onPress(null);
        }
        // Vorbefüllen: Override falls gesetzt, sonst effektiver Default
        String[] overrides = {profile.colors.chatTitleColor, profile.colors.chatNameColor,
                profile.colors.tabTitleColor, profile.colors.tabNameColor,
                profile.colors.nametagTitleColor, profile.colors.nametagNameColor};
        String[] defaults = defaultColorsFor(profile.title);
        for (int i = 0; i < 6; i++) {
            colorFields[i].setText(overrides[i] != null && !overrides[i].isBlank()
                    ? overrides[i] : defaults[i]);
        }
        chatFlagButton.setMessage(flagLabel("ottoextra.rpbook.flag.chat", profile.showInChat));
        tabFlagButton.setMessage(flagLabel("ottoextra.rpbook.flag.tab", profile.showInTablist));
        tagFlagButton.setMessage(flagLabel("ottoextra.rpbook.flag.tag", profile.showInNametag));
        setPeopleEditEnabled(true);
        suppressLockAuto = false;
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
    }

    private void savePerson() {
        if (selected == null) {
            return;
        }
        String rpName = rpNameField.getText().trim();
        String title = titleField.getText().trim();
        String notes = notesField.getText().trim();
        String group = groupForTitle(title); // titelgebunden, nicht editierbar
        // Feldwert == effektiver Default -> kein Override speichern
        String[] defaults = defaultColorsFor(title);
        String[] hex = new String[6];
        for (int i = 0; i < 6; i++) {
            String v = normalizeHex(colorFields[i].getText());
            hex[i] = v != null && v.equalsIgnoreCase(normalizeHex(defaults[i]) == null
                    ? "" : normalizeHex(defaults[i])) ? null : v;
        }
        // Katalog-Titelfarbe NICHT als Spieler-Override backen — Feld == Default
        // (Katalogfarbe) bleibt null, damit spätere Katalog-Farbänderungen überall
        // automatisch greifen. Manuell abweichende Farbe bleibt als Override erhalten.
        boolean showChat = selected.showInChat;
        boolean showTab = selected.showInTablist;
        boolean showTag = selected.showInNametag;
        boolean lock = lockCheckbox.isChecked();
        LocalRpProfile updated = store.updateManual(selected.accountName, p -> {
            p.rpName = rpName.isEmpty() ? LocalRpProfile.UNKNOWN_NAME : rpName;
            p.title = title;
            // Titel-Sperre aus der Checkbox (auto-an bei Titel-Edit) — schützt
            // NUR den Titel vor Auto-Reset (Server-Hover/Chat, Katalog-Rename),
            // der Spieler bleibt aktiv. Leerer Titel kann nicht gesperrt sein.
            p.titleLocked = titleLockCheckbox.isChecked() && !title.isEmpty();
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
        refilter();
        statusLine = Text.translatable("ottoextra.rpbook.saved").getString();
        setPeopleEditEnabled(true);
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

    /** Konfliktauflösung: behalten (Vermerk löschen) oder API-Namen übernehmen. */
    private void resolveConflict(boolean takeApi) {
        if (selected == null || selected.apiConflict == null) {
            return;
        }
        if (takeApi) {
            String apiName = selected.apiConflict;
            store.updateManual(selected.accountName, p -> {
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

    // ---- Titel-Tab (Titelkatalog) ---------------------------------------

    private void initTitles() {
        titleChipButtons.clear();
        titleSearchField = new TextFieldWidget(textRenderer, listX(), contentY(), listW(), 16,
                Text.translatable("ottoextra.rpbook.search"));
        titleSearchField.setSuggestion(Text.translatable("ottoextra.rpbook.search").getString());
        titleSearchField.setChangedListener(s -> {
            titleSearchField.setSuggestion(s.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.search").getString() : "");
            listScroll = 0;
            refilterTitles();
        });
        addDrawableChild(titleSearchField);

        filterDropdownOpen = false;
        filterButton = ButtonWidget.builder(filterLabel(), b ->
                        filterDropdownOpen = !filterDropdownOpen)
                .dimensions(listX(), contentY() + 20, listW(), 16).build();
        addDrawableChild(filterButton);

        int labelW = 58;
        int x = editX() + labelW;
        int w = editW() + (previewVisible() ? previewW() + 8 : 0) - labelW;
        int y = contentY() + 12;

        catTitleField = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        catTitleField.setMaxLength(48);
        addDrawableChild(catTitleField);
        y += 21;
        catVariant1Field = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        catVariant1Field.setMaxLength(48);
        addDrawableChild(catVariant1Field);
        y += 21;
        catVariant2Field = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        catVariant2Field.setMaxLength(48);
        addDrawableChild(catVariant2Field);
        y += 21;
        catCategoryButton = ButtonWidget.builder(catCategoryLabel(), b -> {
            List<String> keys = new ArrayList<>(catalogCategories());
            int idx = Math.max(0, keys.indexOf(catCategoryValue));
            catCategoryValue = keys.get((idx + 1) % keys.size());
            b.setMessage(catCategoryLabel());
            // Farbe folgt der Kategorie, solange kein eigener Override gesetzt ist
            if (selectedTitle != null
                    && (selectedTitle.colorOverride == null || selectedTitle.colorOverride.isBlank())) {
                catColorField.setText(categoryColor(catCategoryValue));
            }
        }).dimensions(x, y, w / 2 - 2, 16).build();
        addDrawableChild(catCategoryButton);
        catEnabledCheckbox = CheckboxWidget.builder(
                Text.translatable("ottoextra.rpbook.titles.enabled"), textRenderer)
                .pos(x + w / 2 + 2, y - 1).build();
        addDrawableChild(catEnabledCheckbox);
        y += 21;
        // Eigene Kategorie anlegen: Name eingeben + "+"
        catNewCategoryField = new TextFieldWidget(textRenderer, x, y, w / 2 - 18, 16, Text.empty());
        catNewCategoryField.setMaxLength(32);
        catNewCategoryField.setSuggestion(
                Text.translatable("ottoextra.rpbook.titles.newCategory").getString());
        catNewCategoryField.setChangedListener(s -> catNewCategoryField.setSuggestion(
                s.isEmpty() ? Text.translatable("ottoextra.rpbook.titles.newCategory").getString()
                        : ""));
        addDrawableChild(catNewCategoryField);
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> addCategoryFromField())
                .dimensions(x + w / 2 - 16, y, 16, 16)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                        Text.translatable("ottoextra.rpbook.titles.newCategory.tip")))
                .build());
        y += 21;
        catColorField = colorField(x, y, w / 2 - 16);
        catOverrideColorCheckbox = CheckboxWidget.builder(
                Text.translatable("ottoextra.rpbook.titles.overridesColor"), textRenderer)
                .checked(true).pos(x + w / 2 + 2, y - 1).build();
        addDrawableChild(catOverrideColorCheckbox);
        y += 21;
        // Namensfarbe für Personen mit diesem Titel (leer = global/Standard).
        catNameColorField = colorField(x, y, w / 2 - 16);
        y += 21;
        catSaveButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.save"),
                b -> saveTitle()).dimensions(x, y, w / 2 - 2, 16).build();
        addDrawableChild(catSaveButton);
        catDeleteButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.titles.delete"),
                b -> deleteTitle()).dimensions(x + w / 2 + 2, y, w / 2 - 2, 16).build();
        addDrawableChild(catDeleteButton);
        y += 21;
        // Reset pro Feld über Reset-Icon im jeweiligen Feld (siehe render/mouseClicked)
        // "Neuer Titel" breit unter der Titelliste (auffälliger)
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.titles.new"),
                b -> newTitle()).dimensions(listX(), listBottom() + 2, listW(), 18).build());

        refilterTitles();
        if (selectedTitle != null) {
            selectTitle(selectedTitle);
        } else {
            setTitleEditEnabled(false);
        }
    }

    /** Neue Kategorie aus dem Eingabefeld anlegen (Farbe = aktuelles Farbfeld). */
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
            catCategoryButton.setMessage(catCategoryLabel());
            catNewCategoryField.setText("");
        }
    }

    private List<String> catalogCategories() {
        var cats = RpNamesServices.catalog().categories().keySet();
        return cats.isEmpty() ? List.of("unclassified") : new ArrayList<>(cats);
    }

    /** Default-Farbe der Kategorie ("" wenn unbekannt). */
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
        catCategoryButton.setMessage(catCategoryLabel());
        // Farbe automatisch aus Kategorie-Default, Override überschreibt
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
        // Wiki-Titel: nur das Löschen sperren. Der Titel-Text bleibt editierbar —
        // Umbenennen ist gefahrlos: der alte Text wird als Variante behalten
        // (Lookup/Farbe bleiben) und die Umbenennung wird bei allen Trägern
        // durchgeschrieben (siehe saveTitle).
        catDeleteButton.active = !"WIKI_IMPORT".equals(e.source);
    }

    private void setTitleEditEnabled(boolean enabled) {
        catTitleField.setEditable(enabled);
        catVariant1Field.setEditable(enabled);
        catVariant2Field.setEditable(enabled);
        catColorField.setEditable(enabled);
        catNameColorField.setEditable(enabled);
        catCategoryButton.active = enabled;
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
        // Variante 1/2 = einstellbare Anzeige-Form. Den Standard-Titel als
        // Match-Alias mitführen, damit Server-Titel (= title) auf den Eintrag
        // matchen und die Anzeige-Variante bekommen.
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
        // Farbe == Kategorie-Default -> kein Override (folgt künftigen Defaults)
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
        // Anzeige ist live: Tab/Name/Schild/Chat/GUI lösen die Variante beim
        // Rendern via canonicalTitle auf — kein Durchschreiben der Profile nötig.
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

    /** Eigenen Titel anlegen (source MANUAL) und direkt zum Bearbeiten öffnen. */
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

    // ---- Gruppen-Tab -------------------------------------------------------------

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

    // ---- Import-Tab ----------------------------------------------------------------

    private void initImport() {
        int x = listX();
        int w = Math.min(360, panelW() - 16);
        int y = contentY() + 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.import.known"),
                b -> runImport(false)).dimensions(x, y, w, 18).build());
        y += 34;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.import.all"),
                b -> runImport(true)).dimensions(x, y, w, 18).build());
        y += 34;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.import.ottoplusKnown"),
                b -> runOttoPlusImport(false)).dimensions(x, y, w, 18).build());
        y += 34;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.import.ottoplusAll"),
                b -> runOttoPlusImport(true)).dimensions(x, y, w, 18).build());
        y += 34;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.import.backup"), b -> {
            store.backup().ifPresentOrElse(
                    path -> statusLine = Text.translatable("ottoextra.rpbook.import.backupDone",
                            path.getFileName().toString()).getString(),
                    () -> statusLine = Text.translatable("ottoextra.rpbook.import.backupFailed").getString());
        }).dimensions(x, y, w, 18).build());
        y += 34;
        forgetAllArmed = false;
        forgetAllButton = ButtonWidget.builder(Text.translatable("ottoextra.rpbook.forgetAll"),
                b -> forgetAll()).dimensions(x, y, w, 18).build();
        addDrawableChild(forgetAllButton);
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
        forgetAllButton.setMessage(Text.translatable("ottoextra.rpbook.forgetAll"));
        statusLine = Text.translatable("ottoextra.rpbook.forgotAll").getString();
    }

    // ---- Liste / Maus ----------------------------------------------------------

    private int listSize() {
        return switch (tab) {
            case PEOPLE -> filtered.size();
            case TITLES -> filteredTitles.size();
            case GROUPS -> groupKeys.size();
            case IMPORT -> 0;
        };
    }

    private int rowHeight() {
        return tab == Tab.PEOPLE || tab == Tab.TITLES ? ROW_H : 12;
    }

    private int currentListTop() {
        return switch (tab) {
            case PEOPLE -> listTop();
            case TITLES -> listTop(); // Dropdown statt Chip-Reihen
            default -> contentY() + 12;
        };
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - currentListTop()) / rowHeight());
    }

    private int maxScroll() {
        return Math.max(0, listSize() - visibleRows());
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        // Tab übernimmt den Titel-Autofill-Vorschlag
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB
                && tab == Tab.PEOPLE && titleField != null && titleField.isFocused()
                && titleAutofill != null) {
            titleField.setText(titleAutofill);
            titleField.setSuggestion("");
            titleAutofill = null;
            return true;
        }
        // Enter im Titel-Feld: Titelfarbe sofort aus dem Katalog tauschen
        if ((input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
                && tab == Tab.PEOPLE && titleField != null && titleField.isFocused()) {
            applyTitleColorFromCatalog();
            return true;
        }
        return super.keyPressed(input);
    }

    /** Titelfarbe (Chat/Tab/Schild) live aus dem Katalog setzen, wenn der Titel
     *  "Farbe überschreibt" hat. */
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (tab != Tab.IMPORT && mouseX >= listX() && mouseX <= listX() + listW()
                && mouseY >= currentListTop() && mouseY <= listBottom()) {
            listScroll = Math.max(0, Math.min(listScroll - (int) Math.signum(vertical) * 3, maxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (filterDropdownOpen && tab != Tab.IMPORT) {
            int count = filterOptions().size();
            int dx = dropdownX();
            int dy = dropdownY();
            int rh = dropdownRowH();
            if (click.button() == 0 && mx >= dx && mx <= dx + listW()
                    && my >= dy && my <= dy + count * rh) {
                selectFilter((int) ((my - dy) / rh));
                return true;
            }
            // Klick außerhalb (außer auf den Button selbst): zuklappen
            if (filterButton == null || !filterButton.isMouseOver(mx, my)) {
                filterDropdownOpen = false;
                return true;
            }
        }
        // Reset-Icon im jeweiligen Farbfeld -> diese Farbe auf Default
        if (tab == Tab.PEOPLE && selected != null && click.button() == 0) {
            for (int i = 0; i < colorFields.length; i++) {
                if (resetIconHit(colorFields[i], mx, my)) {
                    colorFields[i].setText(defaultColorsFor(selected.title)[i]);
                    return true;
                }
            }
            // RP-Name zurücksetzen: API-Original (apiRpName/apiConflict) bevorzugt,
            // sonst der gespeicherte Name — nicht leeren.
            if (resetIconHit(rpNameField, mx, my)) {
                String original = firstNonBlank(selected.apiRpName,
                        firstNonBlank(selected.apiConflict,
                                selected.hasRpName() ? selected.rpName : null));
                rpNameField.setText(original == null ? "" : original);
                return true;
            }
            if (resetIconHit(titleField, mx, my)) {
                // Titel auf den Original-Server-Titel zurücksetzen (sonst leeren)
                String serverTitle = RpNamesServices.serverTitleFor(selected.accountName);
                titleField.setText(serverTitle != null ? serverTitle : "");
                applyTitleColorFromCatalog();
                return true;
            }
        }
        // Titel-Tab: Reset-Icon je Feld -> dieses Feld auf Werkseinstellung
        // (Wiki-Default aus dem gebündelten Katalog), sonst leeren.
        if (tab == Tab.TITLES && selectedTitle != null && click.button() == 0) {
            var def = RpNamesServices.catalog().bundledDefault(selectedTitle.id).orElse(null);
            if (resetIconHit(catTitleField, mx, my)) {
                catTitleField.setText(def != null && def.title != null ? def.title
                        : (selectedTitle.title == null ? "" : selectedTitle.title));
                return true;
            }
            if (resetIconHit(catVariant1Field, mx, my)) {
                catVariant1Field.setText(def != null && def.variants.size() > 0
                        ? def.variants.get(0) : "");
                return true;
            }
            if (resetIconHit(catVariant2Field, mx, my)) {
                catVariant2Field.setText(def != null && def.variants.size() > 1
                        ? def.variants.get(1) : "");
                return true;
            }
            if (resetIconHit(catColorField, mx, my)) {
                catColorField.setText(categoryColor(catCategoryValue));
                return true;
            }
            if (resetIconHit(catNameColorField, mx, my)) {
                catNameColorField.setText("");
                return true;
            }
        }
        if (tab != Tab.IMPORT && click.button() == 0 && mx >= listX() && mx <= listX() + listW()
                && my >= currentListTop() && my <= listBottom()) {
            int idx = listScroll + (int) ((my - currentListTop()) / rowHeight());
            if (idx >= 0 && idx < listSize()) {
                switch (tab) {
                    case PEOPLE -> select(filtered.get(idx));
                    case TITLES -> selectTitle(filteredTitles.get(idx));
                    case GROUPS -> selectGroup(groupKeys.get(idx));
                    default -> { }
                }
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    // ---- Render ----------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Neues Design: gedimmter/geblurrter Vanilla-Hintergrund + Widgets zuerst
        // (wie OttoExtraSettingsScreen), Inhalt darüber.
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, COL_TITLE);

        if (tab != Tab.IMPORT) {
            ctx.fill(listX() - 2, currentListTop() - 2,
                    listX() + listW() + 2, listBottom() + 2, COL_PANEL);
            renderList(ctx);
        } else {
            renderImportInfo(ctx);
        }
        if (tab == Tab.PEOPLE) {
            renderPeopleLabels(ctx);
            renderPlayerModel(ctx, mouseX, mouseY);
            if (previewVisible()) {
                renderPreview(ctx);
            }
        }
        if (tab == Tab.TITLES) {
            ctx.drawText(textRenderer,
                    selectedTitle != null
                            ? selectedTitle.title + " · " + selectedTitle.source
                            : Text.translatable("ottoextra.rpbook.titles.select").getString(),
                    editX(), contentY(), selectedTitle != null ? COL_TEXT : COL_MUTED, false);
            // Feld-Labels links neben den Eingaben
            String[] fieldLabels = {
                    Text.translatable("ottoextra.rpbook.titles.name").getString(),
                    Text.translatable("ottoextra.rpbook.titles.variant1").getString(),
                    Text.translatable("ottoextra.rpbook.titles.variant2").getString(),
                    null, // Kategorie-Zeile (Button hat eigenes Label)
                    null, // Neue-Kategorie-Zeile
                    Text.translatable("ottoextra.rpbook.titles.color").getString(),
                    Text.translatable("ottoextra.rpbook.titles.nameColor").getString()};
            int ly = contentY() + 12;
            for (String label : fieldLabels) {
                if (label != null) {
                    ctx.drawText(textRenderer, label, editX(), ly + 4, COL_TEXT, false);
                }
                ly += 21;
            }
            drawSwatch(ctx, catColorField);
            drawSwatch(ctx, catNameColorField);
            if (selectedTitle != null) {
                // Reset-Icon je Feld (nur dieses Feld auf Standard)
                drawResetIcon(ctx, catTitleField);
                drawResetIcon(ctx, catVariant1Field);
                drawResetIcon(ctx, catVariant2Field);
                drawResetIcon(ctx, catColorField);
                drawResetIcon(ctx, catNameColorField);
            }
        }
        if (tab == Tab.GROUPS) {
            renderGroupLabels(ctx);
        }

        if (!statusLine.isEmpty()) {
            ctx.drawText(textRenderer, textRenderer.trimToWidth(statusLine, panelW() - 80),
                    listX(), contentBottom() - 10, COL_MUTED, false);
        }

        // Filter-Dropdown-Overlay (liegt über Liste/Editor)
        if (filterDropdownOpen && tab != Tab.IMPORT) {
            List<FilterOption> opts = filterOptions();
            int dx = dropdownX();
            int dy = dropdownY();
            int rh = dropdownRowH();
            int dh = opts.size() * rh;
            ctx.fill(dx - 1, dy - 1, dx + listW() + 1, dy + dh + 1, COL_BORDER);
            ctx.fill(dx, dy, dx + listW(), dy + dh, 0xF8232329);
            String curId = tab == Tab.TITLES ? titleFilter : chip.name();
            for (int i = 0; i < opts.size(); i++) {
                FilterOption o = opts.get(i);
                int ry = dy + i * rh;
                boolean hover = mouseX >= dx && mouseX <= dx + listW()
                        && mouseY >= ry && mouseY < ry + rh;
                if (hover) {
                    ctx.fill(dx, ry, dx + listW(), ry + rh, COL_SELECTED);
                }
                ctx.drawText(textRenderer, o.label(), dx + 4, ry + 3,
                        o.id().equals(curId) ? COL_ONLINE : COL_TEXT, false);
            }
        }
    }

    private void renderList(DrawContext ctx) {
        int rows = visibleRows();
        int top = currentListTop();
        int rh = rowHeight();
        for (int r = 0; r < rows; r++) {
            int idx = listScroll + r;
            if (idx >= listSize()) {
                break;
            }
            int ry = top + r * rh;
            if (tab == Tab.TITLES) {
                var e = filteredTitles.get(idx);
                if (e == selectedTitle) {
                    ctx.fill(listX() - 1, ry, listX() + listW() + 1, ry + rh - 1, COL_SELECTED);
                }
                int nameCol = e.enabled ? COL_TITLE : COL_MUTED;
                ctx.drawText(textRenderer,
                        textRenderer.trimToWidth(String.join(" / ", e.variants), listW() - 18),
                        listX() + 2, ry + 2, nameCol, false);
                String line2 = e.category + " · "
                        + ("WIKI_IMPORT".equals(e.source) ? "Wiki" : "Manuell")
                        + (e.enabled ? "" : " · inaktiv");
                ctx.drawText(textRenderer, textRenderer.trimToWidth(line2, listW() - 18),
                        listX() + 2, ry + 12, COL_MUTED, false);
                // Farb-Marker rechts (Override oder Kategorie)
                String hex = e.colorOverride != null && !e.colorOverride.isBlank()
                        ? e.colorOverride
                        : RpNamesServices.catalog().categories().get(e.category) != null
                                ? RpNamesServices.catalog().categories().get(e.category).color : null;
                TextColor tc = ChatNameRewriter.parseColor(hex);
                if (tc != null) {
                    ctx.fill(listX() + listW() - 8, ry + 3, listX() + listW() - 3, ry + 17,
                            0xFF000000 | tc.getRgb());
                }
                continue;
            }
            if (tab == Tab.PEOPLE) {
                LocalRpProfile p = filtered.get(idx);
                if (p == selected) {
                    ctx.fill(listX() - 1, ry, listX() + listW() + 1, ry + rh - 1, COL_SELECTED);
                }
                // Zeile 1: RP-Name (oder Unbekannt)
                String line1 = p.hasRpName() ? p.rpName : LocalRpProfile.UNKNOWN_NAME;
                ctx.drawText(textRenderer, textRenderer.trimToWidth(line1, listW() - 18),
                        listX() + 2, ry + 2, p.hasRpName() ? COL_TITLE : COL_MUTED, false);
                // Zeile 2: account · STATE
                String line2 = p.accountName + " · " + stateShort(p.knowledgeState)
                        + (p.titleGroup != null && !p.titleGroup.isBlank() ? " · " + p.titleGroup : "");
                ctx.drawText(textRenderer, textRenderer.trimToWidth(line2, listW() - 18),
                        listX() + 2, ry + 12, COL_MUTED, false);
                // Marker rechts: online (grün) / Konflikt (rot)
                int markerX = listX() + listW() - 8;
                if (isOnline(p)) {
                    ctx.fill(markerX, ry + 3, markerX + 5, ry + 8, COL_ONLINE);
                }
                if (p.apiConflict != null && !p.apiConflict.isBlank()) {
                    ctx.fill(markerX, ry + 12, markerX + 5, ry + 17, COL_CONFLICT);
                }
            } else {
                String key = groupKeys.get(idx);
                TitleRegistry.Group g = titles.groups().get(key);
                if (key.equals(selectedGroupKey)) {
                    ctx.fill(listX() - 1, ry, listX() + listW() + 1, ry + rh - 1, COL_SELECTED);
                }
                String label = key + (g != null && g.label != null && !g.label.isBlank()
                        ? " — " + g.label : "");
                ctx.drawText(textRenderer, textRenderer.trimToWidth(label, listW() - 6),
                        listX() + 2, ry + 1, COL_TITLE, false);
            }
        }
        if (maxScroll() > 0) {
            int trackTop = top;
            int trackH = listBottom() - top;
            int barH = Math.max(10, trackH * rows / Math.max(rows, listSize()));
            int barY = trackTop + (trackH - barH) * listScroll / Math.max(1, maxScroll());
            ctx.fill(listX() + listW() + 3, barY, listX() + listW() + 6, barY + barH, 0xCC808080);
        }
        String counter = listSize() + (tab == Tab.PEOPLE ? "/" + store.size() : "");
        ctx.drawText(textRenderer, counter,
                listX() + listW() - textRenderer.getWidth(counter), panelY() + 6, COL_MUTED, false);
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

    private void renderPeopleLabels(DrawContext ctx) {
        int x = editX();
        if (selected != null) {
            String group = groupForTitle(titleField != null ? titleField.getText() : selected.title);
            ctx.drawText(textRenderer,
                    selected.accountName + " · " + stateShort(selected.knowledgeState)
                            + (group.isBlank() ? "" : " · " + group),
                    x, contentY(), COL_TEXT, false);
        } else {
            ctx.drawText(textRenderer,
                    Text.translatable("ottoextra.rpbook.select").getString(),
                    x, contentY(), COL_MUTED, false);
        }
        // Feld-Labels links neben RP-Name/Titel
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.rpname").getString(),
                x, contentY() + 16, COL_TEXT, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.titleField").getString(),
                x, contentY() + 37, COL_TEXT, false);
        // Farb-Zeilen-Labels + Swatches
        String[] rowLabels = {
                Text.translatable("ottoextra.rpbook.colors.chat").getString(),
                Text.translatable("ottoextra.rpbook.colors.tab").getString(),
                Text.translatable("ottoextra.rpbook.colors.tag").getString()};
        int baseY = contentY() + 12 + 21 + 21 + 21 + 12;
        for (int row = 0; row < 3; row++) {
            ctx.drawText(textRenderer, rowLabels[row], x, baseY + row * 19 + 3, COL_TEXT, false);
        }
        for (TextFieldWidget f : colorFields) {
            drawSwatch(ctx, f);
            if (f != null && selected != null) {
                // Reset-Icon im Feld rechts (Klick -> diese Farbe auf Default)
                drawResetIcon(ctx, f);
            }
        }
        // Reset-Icon in RP-Name und Titel (Klick -> auf Serverstandard zurücksetzen)
        if (selected != null) {
            drawResetIcon(ctx, rpNameField);
            drawResetIcon(ctx, titleField);
        }
    }

    /** Reset-Icon-Größe: 70% der 16px-Textur. */
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

    private void renderImportInfo(DrawContext ctx) {
        int x = listX() + Math.min(360, panelW() - 16) + 12;
        int y = contentY() + 24;
        List<String> lines = List.of(
                Text.translatable("ottoextra.rpbook.import.hintKnown").getString(),
                "",
                Text.translatable("ottoextra.rpbook.import.hintAll").getString(),
                Text.translatable("ottoextra.rpbook.import.spoiler").getString(),
                "",
                Text.translatable("ottoextra.rpbook.import.hintBackup").getString());
        for (String line : lines) {
            for (String wrapped : wrap(line, panelX() + panelW() - 12 - x)) {
                ctx.drawText(textRenderer, wrapped, x, y, COL_TEXT, false);
                y += 11;
            }
        }
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

    // ---- 3D-Spielervorschau ----------------------------------------------------

    /** Stehender 3D-Spieler unten in der Vorschau: leichte Drehung + Kopf folgt Maus. */
    private void renderPlayerModel(DrawContext ctx, int mouseX, int mouseY) {
        if (selected == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        int x1 = modelX();
        int x2 = (previewVisible() ? previewX() - 8 : panelX() + panelW() - 8);
        if (x2 - x1 < 60) {
            return; // zu schmal -> kein Platz fürs Modell
        }
        int boxTop = contentY() + 12;
        int boxBottom = listBottom();
        ensurePreviewEntity(client);
        if (previewEntity == null) {
            return;
        }
        ctx.fill(x1, boxTop, x2, boxBottom, 0x50000000);
        // Titel + Spielername über dem Modell, in den Namensschild-Farben,
        // Breite auf die Box begrenzt (skaliert sonst runter, kein Überlauf).
        int cx = (x1 + x2) / 2;
        int maxW = (x2 - x1) - 8;
        int ly = boxTop + 3;
        if (selected.hasTitle()) {
            drawScaledCentered(ctx, selected.title, cx, ly, previewTitleColor(), maxW, 1.2f);
            ly += 12;
        }
        String displayName = selected.hasRpName() ? selected.rpName : selected.accountName;
        drawScaledCentered(ctx, displayName, cx, ly, previewNameColor(), maxW, 1.5f);
        // Skalierung an Höhe UND Breite koppeln (Vollkörper passt)
        int size = (int) Math.min((boxBottom - boxTop) * 0.32f, (x2 - x1) * 0.8f);
        float sway = (float) Math.sin(System.currentTimeMillis() / 1400.0) * 25f;
        try {
            net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                    ctx, x1, boxTop + 24, x2, boxBottom - 6, size, 0.0f,
                    mouseX - sway, mouseY, previewEntity);
        } catch (Throwable ignored) {
            // Render darf das Buch nie brechen
        }
    }

    /** Text zentriert, bei Überbreite auf {@code maxW} herunterskaliert (max {@code baseScale}). */
    private void drawScaledCentered(DrawContext ctx, String s, int cx, int y, int color,
                                    int maxW, float baseScale) {
        if (s == null || s.isEmpty()) {
            return;
        }
        float wScaled = textRenderer.getWidth(s) * baseScale;
        float scale = wScaled > maxW ? baseScale * maxW / wScaled : baseScale;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(scale, scale);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(s), 0, 0, color);
        m.popMatrix();
    }

    private static int previewArgb(String hex, int def) {
        net.minecraft.text.TextColor c =
                de.ottoextra.rpnames.chat.ChatNameRewriter.parseColor(hex);
        return c != null ? (0xFF000000 | c.getRgb()) : def;
    }

    private static String previewFirst(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    /** Namensschild-Titelfarbe der ausgewählten Person (Override -> Katalog -> Gruppe -> Fallback). */
    private int previewTitleColor() {
        var catalog = RpNamesServices.catalog();
        String catalogColor = catalog != null
                ? catalog.titleColor(selected.title).orElse(null) : null;
        String groupColor = RpNamesServices.titles() != null
                ? RpNamesServices.titles().find(selected.title)
                        .map(r -> r.group().titleColor).orElse(null)
                : null;
        String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";
        // „Farbe überschreibt": Katalogfarbe schlägt den Personen-Override.
        String pers = selected.colors.nametagTitleColor;
        String hex = RpNamesServices.titleOverridesColor(selected.title)
                ? previewFirst(catalogColor, previewFirst(pers, previewFirst(groupColor, fallback)))
                : previewFirst(pers, previewFirst(catalogColor, previewFirst(groupColor, fallback)));
        return previewArgb(hex, COL_TITLE);
    }

    /** RP-Namensfarbe der ausgewählten Person: Override -> Titel-Namensfarbe ->
     *  global -> Standard (mit „Farbe überschreibt"). */
    private int previewNameColor() {
        String hex = RpNamesServices.rpNameColor(selected.colors.nametagNameColor, selected.title);
        return previewArgb(hex, COL_TITLE);
    }

    /** Dummy-Spieler (Skin aus UUID/Account) für die 3D-Vorschau, gecacht je Account. */
    private void ensurePreviewEntity(MinecraftClient client) {
        String key = selected.accountName;
        if (key == null || key.isBlank()) {
            previewEntity = null;
            return;
        }
        if (key.equals(previewEntityKey) && previewEntity != null) {
            return;
        }
        previewEntityKey = key;
        previewEntity = null;
        try {
            // Skin vom Server (Player-List-Profil mit Texturen); Mojang nur Backup
            com.mojang.authlib.GameProfile profile = serverProfile(client, key);
            final boolean online = profile != null;
            boolean fallback = false;
            if (profile == null) {
                java.util.UUID uuid;
                if (selected.uuid != null && !selected.uuid.isBlank()) {
                    uuid = java.util.UUID.fromString(selected.uuid); // echte UUID -> lokaler/Mojang-Skin
                } else {
                    uuid = FALLBACK_SKIN_UUID; // nie online / keine Textur -> Marken-Fallback
                    fallback = true;
                }
                profile = new com.mojang.authlib.GameProfile(uuid, key);
            }
            final boolean useFallback = fallback;
            final java.util.UUID previewUuid = profile.id();
            net.minecraft.client.network.OtherClientPlayerEntity entity =
                    new net.minecraft.client.network.OtherClientPlayerEntity(client.world, profile) {
                        @Override
                        public net.minecraft.entity.player.SkinTextures getSkin() {
                            if (useFallback) {
                                return fallbackSkin();
                            }
                            // Offline: lokal gecachten Skin (eigenes PNG) bevorzugen.
                            if (!online) {
                                var local = de.ottoextra.chat.SkinCache.localSkin(previewUuid);
                                if (local != null) {
                                    return local;
                                }
                            }
                            return super.getSkin();
                        }
                    };
            // 2nd-Layer nur bei echtem Skin; Fallback-Textur ist ohne Overlay
            entity.getDataTracker().set(
                    de.ottoextra.mixin.PlayerCustomizationAccessor.ottoextra$customization(),
                    (byte) (useFallback ? 0x00 : 0x7F));
            previewEntity = entity;
        } catch (Throwable t) {
            previewEntity = null;
            de.ottoextra.OttoExtra.LOGGER.warn("[rpnames] 3D-Vorschau fuer {} fehlgeschlagen: {}",
                    key, t.toString());
        }
    }

    /** Server-seitiges Profil (mit Skin-Texturen) aus der Player-Liste, oder null. */
    private com.mojang.authlib.GameProfile serverProfile(MinecraftClient client, String account) {
        var nh = client.getNetworkHandler();
        if (nh == null) {
            return null;
        }
        net.minecraft.client.network.PlayerListEntry entry = null;
        if (selected.uuid != null && !selected.uuid.isBlank()) {
            try {
                entry = nh.getPlayerListEntry(java.util.UUID.fromString(selected.uuid));
            } catch (IllegalArgumentException ignored) {
                // ungültige UUID -> per Name versuchen
            }
        }
        if (entry == null) {
            entry = nh.getPlayerListEntry(account);
        }
        return entry != null ? entry.getProfile() : null;
    }

    // ---- Live-Vorschau (echte Formatter) -------------------------

    private void renderPreview(DrawContext ctx) {
        int x = previewX();
        int w = previewW();
        int y = contentY();
        ctx.fill(x - 2, y - 2, x + w + 2, contentBottom() + 2, COL_PANEL);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.rpbook.preview").getString(),
                x + 2, y, COL_TITLE, false);
        y += 14;
        if (selected == null) {
            return;
        }
        // Chat: echte Rewriter-Pipeline auf konstruierter Zeile
        ctx.drawText(textRenderer, "Chat:", x + 2, y, COL_MUTED, false);
        y += 10;
        Text chatLine = Text.literal("[Reden] ")
                .append(Text.literal(selected.accountName))
                .append(Text.literal(": Seid gegrüßt."));
        Text rewritten = RpNamesServices.processChatMessage(chatLine);
        ctx.drawText(textRenderer, trimText(rewritten == null ? chatLine : rewritten, w - 6),
                x + 2, y, 0xFFFFFFFF, false);
        y += 16;

        // Tabliste: echter Formatter (braucht GameProfile mit UUID)
        ctx.drawText(textRenderer, "Tab:", x + 2, y, COL_MUTED, false);
        y += 10;
        Text tabText = null;
        try {
            if (selected.uuid != null && !selected.uuid.isBlank()) {
                com.mojang.authlib.GameProfile gp = new com.mojang.authlib.GameProfile(
                        java.util.UUID.fromString(selected.uuid), selected.accountName);
                tabText = TablistNameFormatter.format(gp, Text.literal(selected.accountName));
            }
        } catch (Exception ignored) {
            // ungültige UUID -> Fallback unten
        }
        if (tabText == null) {
            tabText = fallbackStyled();
        }
        ctx.drawText(textRenderer, trimText(tabText, w - 6), x + 2, y, 0xFFFFFFFF, false);
        y += 16;

        // Nametag: aus Profilfeldern (Renderer existiert noch nicht)
        ctx.drawText(textRenderer, "Schild:", x + 2, y, COL_MUTED, false);
        y += 10;
        MutableText line1 = fallbackStyled();
        ctx.drawText(textRenderer, trimText(line1, w - 6), x + 2, y, 0xFFFFFFFF, false);
        y += 10;
        ctx.drawText(textRenderer, selected.accountName, x + 2, y, 0xFF9A9A9A, false);
        y += 16;

        // Konfliktpanel
        if (selected.apiConflict != null && !selected.apiConflict.isBlank()) {
            ctx.drawText(textRenderer,
                    Text.translatable("ottoextra.rpbook.conflict.head").getString(),
                    x + 2, y, COL_CONFLICT, false);
            y += 10;
            ctx.drawText(textRenderer, textRenderer.trimToWidth("API: " + selected.apiConflict, w - 6),
                    x + 2, y, COL_TEXT, false);
        }
    }

    /** Titel + Name gefärbt nach der echten Farbkette (Tab-Fallback + Nametag-Vorschau). */
    private MutableText fallbackStyled() {
        String[] defaults = defaultColorsFor(selected.title);
        MutableText out = Text.empty();
        if (selected.hasTitle()) {
            out.append(colored(selected.title + " ",
                    firstNonBlank(selected.colors.chatTitleColor, defaults[0])));
        }
        out.append(colored(selected.displayRpName(),
                firstNonBlank(selected.colors.chatNameColor, defaults[1])));
        return out;
    }

    private static MutableText colored(String s, String hex) {
        MutableText t = Text.literal(s);
        TextColor c = ChatNameRewriter.parseColor(hex);
        if (c != null) {
            t.setStyle(Style.EMPTY.withColor(c));
        }
        return t;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private Text trimText(Text text, int maxW) {
        if (textRenderer.getWidth(text) <= maxW) {
            return text;
        }
        return Text.literal(textRenderer.trimToWidth(text.getString(), maxW)).setStyle(text.getStyle());
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
        store.saveNow();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
