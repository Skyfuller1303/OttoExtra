package de.ottoextra.horses.ui;

import de.ottoextra.animalinfo.AnimalBookStatus;
import de.ottoextra.api.ApiProblem;
import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.horses.AnimalBookStore;
import de.ottoextra.horses.FarmShareCodes;
import de.ottoextra.horses.FarmShareList;
import de.ottoextra.horses.FarmShareService;
import de.ottoextra.horses.FarmShareWire;
import de.ottoextra.horses.FarmSymbols;
import de.ottoextra.horses.HorseRegistry;
import de.ottoextra.horses.RegistryAnimals;
import de.ottoextra.horses.ui.HorseOverviewView.AnimalKind;
import de.ottoextra.horses.ui.HorseOverviewView.BookSort;
import de.ottoextra.horses.ui.HorseOverviewView.SortOrder;
import de.ottoextra.horses.ui.HorseOverviewView.SortSelection;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static de.ottoextra.horses.ui.AnimalBook.BRASS;
import static de.ottoextra.horses.ui.AnimalBook.INK;
import static de.ottoextra.horses.ui.AnimalBook.INK_F;
import static de.ottoextra.horses.ui.AnimalBook.INK_M;
import static de.ottoextra.horses.ui.AnimalBook.OK_INK;
import static de.ottoextra.horses.ui.AnimalBook.PA_D;
import static de.ottoextra.horses.ui.AnimalBook.PA_L;
import static de.ottoextra.horses.ui.AnimalBook.PA_S;
import static de.ottoextra.horses.ui.AnimalBook.SOON_INK;
import static de.ottoextra.horses.ui.AnimalBook.TERRA;

/**
 * The animal overview as a book: farms (Höfe) on the left page, the animals of the selected farm on the
 * right page, or a form for a new farm / the header of a farm. Layout follows
 * docs/design/tierbuch/tier_buch_uebersicht.png and tier_buch_leer.png. Farms and animals come from the
 * register API as before; farm symbols and notes are local ({@link AnimalBookStore}). Farms of lists joined by
 * code ({@link FarmShareList}) follow the own farms on the left page; the share page shows the codes of a list.
 */
public final class BasicHorseOverviewScreen extends Screen {

    /** What the right page shows. */
    enum View { LIST, NEW_FARM, EDIT_FARM, SHARE }

    private static final int FARM_ROW = 26;
    private static final int NEW_ROW = 18;
    private static final int ENTRY_ROW = 26;
    private static final String ALL_SYMBOL = "flagge_grau";
    private static final long CONFIRM_MILLIS = 3_000L;
    private static final String[][] STEPS = {
            {"ottoextra.horses.book.step1", "ottoextra.horses.book.step1_hint"},
            {"ottoextra.horses.book.step2", "ottoextra.horses.book.step2_hint"},
            {"ottoextra.horses.book.step3", "ottoextra.horses.book.step3_hint"},
            {"ottoextra.horses.book.step4", "ottoextra.horses.book.step4_hint"}};

    /**
     * One farm row of the left page; {@code name} null is "all farms", "" is "without farm". Rows of a joined
     * list carry the list and the shared farm id ({@code sharedId} null: the list has no farm yet).
     */
    private record FarmRow(String name, String key, int count, int ready, FarmShareList list, String sharedId) {
        FarmRow(String name, String key, int count, int ready) {
            this(name, key, count, ready, null, null);
        }

        boolean shared() {
            return list != null;
        }
    }

    private final Screen parent;
    private View view = View.LIST;
    private AnimalBook.Spread spread;

    // joined lists and sharing
    private FarmShareList sharedList;
    private String sharedFarm;
    private List<FarmShareWire.AnimalRec> sharedRows = List.of();
    private FarmShareList shareView;
    private String codeText = "";
    private long renewUntil;
    private long leaveUntil;
    private boolean formShared;

    // selection and order
    private String farm;
    private String query = "";
    private AnimalKind filter = AnimalKind.ALL;
    private BookSort sort = BookSort.FARM;
    private final SortSelection sorting = new SortSelection();
    private int leftScroll;
    private int scroll;

    // data
    private List<AnimalBreedingHorse> rows = List.of();
    private final List<FarmRow> farmRows = new ArrayList<>();
    private int totalAnimals;
    private final Map<AnimalKind, AnimalEntity> fallbackAnimals = new EnumMap<>(AnimalKind.class);
    private AnimalEntity entryTarget;
    private boolean refreshing;
    private boolean loadedOnce;
    private String status = "";
    private int ticks;

    // farm form
    private String formName = "";
    private String formNote = "";
    private String formSymbol = FarmSymbols.DEFAULT;
    private String formKey = "";
    private String notice = "";
    private boolean noticeError;
    private boolean busy;

    private ButtonWidget refreshButton;
    private ButtonWidget addButton;
    private ButtonWidget createButton;
    private ButtonWidget shareButton;
    private ButtonWidget renewButton;
    private ButtonWidget leaveButton;

    public BasicHorseOverviewScreen(Screen parent) {
        super(Text.translatable("ottoextra.horses.overview.title"));
        this.parent = parent;
    }

    // ---- layout --------------------------------------------------------------------

    @Override
    protected void init() {
        refreshButton = null;
        addButton = null;
        createButton = null;
        shareButton = null;
        renewButton = null;
        leaveButton = null;
        spread = AnimalBook.spread(width, height);
        if (entryTarget == null && client != null && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof AnimalEntity animal && RegistryAnimals.supports(animal)) {
            entryTarget = animal;
        }
        rebuild();
        AnimalBook.Page left = spread.left();
        AnimalBook.Page right = spread.right();
        TextFieldWidget search = new AnimalBook.Field(textRenderer, left.x() + 8, left.y() + left.h() - 27,
                left.w() - 18, Text.translatable("ottoextra.horses.book.search_hint"));
        search.setMaxLength(64);
        search.setText(query);
        search.setChangedListener(value -> {
            query = value;
            scroll = 0;
            rebuild();
        });
        addDrawableChild(search);
        de.ottoextra.ui.complete.TabCompletion.attach(search, de.ottoextra.ui.complete.TabCompletionSources::hoefe, false);

        // Left page: "Teilen" for the selected farm, joining by code above the search.
        Text share = Text.translatable("ottoextra.horses.share.share");
        int shareW = AnimalBook.buttonWidth(textRenderer, share, 40);
        shareButton = addDrawableChild(new AnimalBook.Button(left.x() + left.w() - 10 - shareW, left.y() + 3, shareW,
                share, AnimalBook.Tone.SLATE, b -> shareSelected()));
        int codeY = codeFooterY();
        Text join = Text.translatable("ottoextra.horses.share.join");
        int joinW = AnimalBook.buttonWidth(textRenderer, join, 44);
        int joinX = left.x() + left.w() - 10 - joinW;
        TextFieldWidget codeField = new AnimalBook.Field(textRenderer, left.x() + 8, codeY + 8,
                joinX - 4 - (left.x() + 8), Text.translatable("ottoextra.horses.share.code_hint"));
        codeField.setMaxLength(40);
        codeField.setText(codeText);
        codeField.setChangedListener(value -> codeText = value);
        addDrawableChild(codeField);
        addDrawableChild(new AnimalBook.Button(joinX, codeY + 7, joinW, join, AnimalBook.Tone.SLATE, b -> joinCode()));

        int edge = right.x() + right.w() - 8;
        int footerY = right.y() + right.h() - 22;
        int rx = right.x() + 10;
        int rw = right.w() - 20;
        if (isEmptyBook() && view == View.LIST) {
            Text create = Text.translatable("ottoextra.horses.book.new_farm");
            int createW = AnimalBook.buttonWidth(textRenderer, create, 70);
            addDrawableChild(new AnimalBook.Button(edge - createW, footerY, createW, create,
                    AnimalBook.Tone.PRIMARY, b -> startNewFarm()));
        } else if (view == View.SHARE) {
            initShare(rx, rw, edge, footerY);
        } else if (view == View.LIST && sharedList != null) {
            Text refresh = Text.translatable("ottoextra.horses.overview.refresh");
            int refreshW = AnimalBook.buttonWidth(textRenderer, refresh, 62);
            refreshButton = addDrawableChild(new AnimalBook.Button(edge - refreshW, right.y() + 4, refreshW, refresh,
                    AnimalBook.Tone.SLATE, b -> FarmShareService.syncSoon(sharedList)));
        } else if (view == View.LIST) {
            Text add = Text.translatable("ottoextra.horses.book.add");
            Text refresh = Text.translatable("ottoextra.horses.overview.refresh");
            int addW = AnimalBook.buttonWidth(textRenderer, add, 50);
            int refreshW = AnimalBook.buttonWidth(textRenderer, refresh, 62);
            addButton = addDrawableChild(new AnimalBook.Button(edge - addW, right.y() + 4, addW, add,
                    AnimalBook.Tone.PRIMARY, b -> addEntry()));
            refreshButton = addDrawableChild(new AnimalBook.Button(edge - addW - 3 - refreshW, right.y() + 4,
                    refreshW, refresh, AnimalBook.Tone.SLATE, b -> refreshRecords()));
        } else {
            boolean editing = view == View.EDIT_FARM;
            if (!editing || formShared) {
                TextFieldWidget nameField = field(rx, right.y() + 33, rw, AnimalBookStore.FARM_NAME_MAX, formName,
                        "ottoextra.horses.book.farm_name_hint", value -> {
                            formName = value;
                            updateButtons();
                        });
                de.ottoextra.ui.complete.TabCompletion.attach(nameField, de.ottoextra.ui.complete.TabCompletionSources::hoefe, false);
                setInitialFocus(nameField);
            }
            field(rx, right.y() + (editing && !formShared ? 55 : 59), rw, AnimalBookStore.NOTE_MAX, formNote,
                    "ottoextra.horses.book.farm_note_hint", value -> formNote = value);
            Text confirm = Text.translatable(editing ? "ottoextra.horses.record.save" : "ottoextra.horses.book.create");
            Text cancel = Text.translatable("gui.cancel");
            int confirmW = AnimalBook.buttonWidth(textRenderer, confirm, 60);
            int cancelW = AnimalBook.buttonWidth(textRenderer, cancel, 60);
            createButton = addDrawableChild(new AnimalBook.Button(edge - confirmW, footerY, confirmW, confirm,
                    AnimalBook.Tone.PRIMARY, b -> {
                        if (editing) saveFarmHeader();
                        else createFarm();
                    }));
            addDrawableChild(new AnimalBook.Button(edge - confirmW - 3 - cancelW, footerY, cancelW, cancel,
                    AnimalBook.Tone.SLATE, b -> show(View.LIST)));
        }
        if (!loadedOnce) refreshRecords();
        updateButtons();
    }

    /** Share page: copy buttons next to each code box, renew, leave and done in the footer. */
    private void initShare(int rx, int rw, int edge, int footerY) {
        FarmShareList list = shareView == null ? null : AnimalBookStore.current().shareList(shareView.localId());
        if (list == null) {
            view = View.LIST;
            return;
        }
        shareView = list;
        int y = right().y() + 28;
        if (list.writeCode() != null) {
            copyButtons(rx, rw, y, list.writeCode());
            y += 40;
        }
        copyButtons(rx, rw, y, list.readCode());
        long now = System.currentTimeMillis();
        Text done = Text.translatable("ottoextra.horses.share.done");
        int doneW = AnimalBook.buttonWidth(textRenderer, done, 50);
        addDrawableChild(new AnimalBook.Button(edge - doneW, footerY, doneW, done, AnimalBook.Tone.PRIMARY,
                b -> show(View.LIST)));
        int x = rx;
        if (!list.readOnly()) {
            Text renew = Text.translatable(now < renewUntil ? "ottoextra.horses.share.renew_confirm"
                    : "ottoextra.horses.share.renew");
            int renewW = AnimalBook.buttonWidth(textRenderer, renew, 70);
            renewButton = addDrawableChild(new AnimalBook.Button(x, footerY, renewW, renew, AnimalBook.Tone.DANGER,
                    b -> renewCodes()));
            x += renewW + 3;
        }
        Text leave = Text.translatable(now < leaveUntil ? "ottoextra.horses.share.leave_confirm"
                : list.owned() ? "ottoextra.horses.share.stop" : "ottoextra.horses.share.leave");
        int leaveW = AnimalBook.buttonWidth(textRenderer, leave, 50);
        leaveButton = addDrawableChild(new AnimalBook.Button(x, footerY, Math.min(leaveW, edge - doneW - 3 - x), leave,
                AnimalBook.Tone.SLATE, b -> leaveList()));
    }

    /** "Kopieren" and "Link" next to a code box that starts at {@code y}. */
    private void copyButtons(int x, int w, int y, String code) {
        Text copy = Text.translatable("ottoextra.horses.share.copy");
        Text link = Text.translatable("ottoextra.horses.share.link");
        int copyW = AnimalBook.buttonWidth(textRenderer, copy, 50);
        int linkW = AnimalBook.buttonWidth(textRenderer, link, 35);
        int linkX = x + w - linkW;
        int copyX = linkX - 3 - copyW;
        addDrawableChild(new AnimalBook.Button(copyX, y + 13, copyW, copy, AnimalBook.Tone.SLATE,
                b -> client.keyboard.setClipboard(FarmShareCodes.display(code))));
        addDrawableChild(new AnimalBook.Button(linkX, y + 13, linkW, link, AnimalBook.Tone.SLATE,
                b -> client.keyboard.setClipboard(FarmShareService.joinUrl(code))));
    }

    private int codeBoxWidth(int w) {
        return w - AnimalBook.buttonWidth(textRenderer, Text.translatable("ottoextra.horses.share.copy"), 50)
                - AnimalBook.buttonWidth(textRenderer, Text.translatable("ottoextra.horses.share.link"), 35) - 3 - 4;
    }

    private AnimalBook.Page right() {
        return spread.right();
    }

    private TextFieldWidget field(int x, int y, int w, int max, String value, String hintKey,
                                  java.util.function.Consumer<String> onChange) {
        TextFieldWidget field = new AnimalBook.Field(textRenderer, x, y, w, Text.translatable(hintKey));
        field.setMaxLength(max);
        field.setText(value);
        field.setChangedListener(onChange);
        addDrawableChild(field);
        return field;
    }

    private void show(View next) {
        view = next;
        if (next == View.LIST) {
            notice = "";
            noticeError = false;
        }
        renewUntil = 0L;
        leaveUntil = 0L;
        clearAndInit();
    }

    private void updateButtons() {
        if (refreshButton != null) refreshButton.active = !refreshing;
        if (addButton != null) {
            addButton.active = client != null && entryTarget != null && entryTarget.isAlive()
                    && entryTarget.getEntityWorld() == client.world
                    && HorseRegistry.observation(entryTarget.getUuid()).isPresent();
        }
        if (createButton != null) {
            createButton.active = !busy && (view == View.EDIT_FARM || AnimalBreedingFarm.validName(formName));
        }
        if (shareButton != null) {
            shareButton.active = sharedList != null || farm != null && !farm.isEmpty();
        }
    }

    @Override
    public void tick() {
        updateButtons();
        // Ready counts follow the live timers; the order only changes on user actions.
        if (++ticks % 20 == 0) {
            recount();
            if (sharedList != null) rebuildShared();
        }
        long now = System.currentTimeMillis();
        if (renewUntil != 0L && now >= renewUntil || leaveUntil != 0L && now >= leaveUntil) {
            renewUntil = 0L;
            leaveUntil = 0L;
            if (view == View.SHARE) clearAndInit();
        }
    }

    // ---- data ----------------------------------------------------------------------

    private List<AnimalBreedingHorse> source() {
        var config = OttoExtraConfig.active().animalInfo;
        if (!HorseOverviewView.filters(config).contains(filter)) filter = AnimalKind.ALL;
        return HorseOverviewView.filter(HorseRegistry.databaseRecords(), filter, config);
    }

    private void rebuild() {
        long now = System.currentTimeMillis();
        Map<UUID, AnimalBookStatus.View> views = new HashMap<>();
        rows = HorseOverviewView.bookRows(source(), query, farm, sort, sorting,
                horse -> AnimalBookStatus.rank(views.computeIfAbsent(horse.uuid(), id -> status(horse, now))),
                horse -> {
                    long remaining = views.computeIfAbsent(horse.uuid(), id -> status(horse, now)).status().remainingMs();
                    return remaining < 0 ? Long.MAX_VALUE : remaining;
                });
        rebuildShared();
        recount();
    }

    /** Animals of the selected farm of a joined list, by label. */
    private void rebuildShared() {
        if (sharedList != null && AnimalBookStore.current().shareList(sharedList.localId()) != sharedList) {
            sharedList = null;
            sharedFarm = null;
        }
        if (sharedList == null || sharedFarm == null) {
            sharedRows = List.of();
        } else {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            List<FarmShareWire.AnimalRec> list = new ArrayList<>();
            for (FarmShareWire.AnimalRec animal : sharedList.animalsOf(sharedFarm)) {
                if (needle.isEmpty() || sharedName(animal).toLowerCase(Locale.ROOT).contains(needle)
                        || animal.note().toLowerCase(Locale.ROOT).contains(needle)) {
                    list.add(animal);
                }
            }
            list.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(sharedName(a), sharedName(b)));
            sharedRows = List.copyOf(list);
        }
        int size = sharedList != null ? sharedRows.size() : rows.size();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, size - visibleEntries())));
    }

    /** Label of a shared animal; the register name when the user has the animal too; else a short UUID. */
    private static String sharedName(FarmShareWire.AnimalRec animal) {
        if (!animal.label().isBlank()) return animal.label();
        var record = HorseRegistry.databaseRecord(UUID.fromString(animal.uuid()));
        if (record.isPresent() && !record.get().registryName().isBlank()) return record.get().registryName();
        return tr("ottoextra.horses.share.unnamed", animal.uuid().substring(0, 8));
    }

    /** Farm rows with animal and ready counts; the search applies to the counts as on the right page. */
    private void recount() {
        long now = System.currentTimeMillis();
        List<AnimalBreedingHorse> all = HorseOverviewView.bookRows(source(), query, null, BookSort.NAME,
                new SortSelection(), horse -> 0, horse -> 0L);
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (AnimalBreedingFarm each : HorseRegistry.farms()) {
            String key = each.name().toLowerCase(Locale.ROOT);
            names.putIfAbsent(key, each.name());
            counts.computeIfAbsent(key, ignored -> new int[2]);
        }
        int ready = 0;
        boolean withoutFarm = false;
        for (AnimalBreedingHorse horse : all) {
            boolean isReady = status(horse, now).tone() == AnimalBookStatus.Tone.READY;
            if (isReady) ready++;
            String name = horse.currentFarm().trim();
            if (name.isEmpty()) {
                withoutFarm = true;
                continue;
            }
            String key = name.toLowerCase(Locale.ROOT);
            names.putIfAbsent(key, name);
            int[] count = counts.computeIfAbsent(key, ignored -> new int[2]);
            count[0]++;
            if (isReady) count[1]++;
        }
        farmRows.clear();
        totalAnimals = all.size();
        farmRows.add(new FarmRow(null, "", all.size(), ready));
        names.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    int[] count = counts.get(entry.getKey());
                    farmRows.add(new FarmRow(entry.getValue(), AnimalBook.farmKey(entry.getValue()),
                            count[0], count[1]));
                });
        if (withoutFarm) {
            int count = 0;
            int readyCount = 0;
            for (AnimalBreedingHorse horse : all) {
                if (!horse.currentFarm().isBlank()) continue;
                count++;
                if (status(horse, now).tone() == AnimalBookStatus.Tone.READY) readyCount++;
            }
            farmRows.add(new FarmRow("", "", count, readyCount));
        }
        for (FarmShareList list : AnimalBookStore.current().shareLists()) {
            if (list.owned()) continue;
            List<FarmShareWire.FarmRec> shared = list.liveFarms();
            if (shared.isEmpty()) {
                farmRows.add(new FarmRow(list.name().isBlank() ? tr("ottoextra.horses.share.list_default") : list.name(),
                        "", 0, 0, list, null));
            }
            for (FarmShareWire.FarmRec each : shared) {
                farmRows.add(new FarmRow(each.name(), each.symbol(), list.animalsOf(each.id()).size(), 0, list, each.id()));
            }
        }
    }

    private AnimalBookStatus.View status(AnimalBreedingHorse horse, long now) {
        Boolean baby = null;
        if (client != null && client.world != null) {
            Entity loaded = client.world.getEntity(horse.uuid());
            if (loaded instanceof AnimalEntity animal && animal.isAlive()) baby = animal.isBaby();
        }
        return AnimalBookStatus.of(horse.uuid(), horse.animalType(), horse.mast(), horse.breedingReady(),
                horse.observedAtEpochMillis(), baby, now);
    }

    /** No farm and no animal: the book shows the short guide. */
    private boolean isEmptyBook() {
        return loadedOnce && !refreshing && HorseRegistry.farms().isEmpty()
                && HorseRegistry.databaseRecords().isEmpty() && AnimalBookStore.current().shareLists().isEmpty();
    }

    private void refreshRecords() {
        if (refreshing) return;
        loadedOnce = true;
        refreshing = true;
        status = Text.translatable("ottoextra.horses.overview.loading").getString();
        updateButtons();
        HorseRegistry.refreshFarms().exceptionally(error -> List.of()).thenCompose(ignored ->
                HorseRegistry.refreshDatabaseRecords()).whenComplete((records, error) -> {
            if (client == null) return;
            client.execute(() -> {
                refreshing = false;
                status = Text.translatable(error == null ? "ottoextra.horses.overview.current"
                        : "ottoextra.horses.overview.failed").getString();
                if (client.currentScreen != this) return;
                rebuild();
                clearAndInit();
            });
        });
    }

    // ---- actions -------------------------------------------------------------------

    private void addEntry() {
        if (entryTarget != null && client != null) {
            HorseRegistry.observation(entryTarget.getUuid()).ifPresent(observation ->
                    client.setScreen(new BasicHorseRecordScreen(this, entryTarget, observation)));
        }
    }

    private void open(AnimalBreedingHorse horse) {
        if (client == null) return;
        Entity loaded = client.world == null ? null : client.world.getEntity(horse.uuid());
        if (loaded instanceof AnimalEntity animal && animal.isAlive() && RegistryAnimals.supports(animal)) {
            var observation = HorseRegistry.observation(animal.getUuid());
            if (observation.isPresent()) {
                client.setScreen(new BasicHorseRecordScreen(this, animal, observation.get()));
                return;
            }
        }
        client.setScreen(new BasicHorseRecordScreen(this, horse.uuid()));
    }

    private void selectFarm(String name) {
        farm = name;
        sharedList = null;
        sharedFarm = null;
        scroll = 0;
        rebuild();
        show(View.LIST);
    }

    private void selectRow(FarmRow row) {
        if (!row.shared()) {
            selectFarm(row.name());
            return;
        }
        farm = null;
        sharedList = row.list();
        sharedFarm = row.sharedId();
        scroll = 0;
        rebuild();
        if (row.sharedId() == null) openShare(row.list());
        else show(View.LIST);
    }

    private boolean selected(FarmRow row) {
        if (view == View.NEW_FARM) return false;
        if (row.shared()) {
            return row.list() == sharedList && java.util.Objects.equals(row.sharedId(), sharedFarm);
        }
        if (sharedList != null) return false;
        return row.name() == null ? farm == null : row.name().equalsIgnoreCase(farm == null ? "\u0000" : farm);
    }

    // ---- sharing -------------------------------------------------------------------

    /** "Teilen" on the Höfe page: the share page of the selected list, or a new list for an own farm. */
    private void shareSelected() {
        if (sharedList != null) {
            openShare(sharedList);
            return;
        }
        if (farm == null || farm.isEmpty()) return;
        FarmShareList list = AnimalBookStore.current().listOfFarm(headerId(farm));
        if (list == null && !FarmShareService.allowed()) {
            status = tr("ottoextra.horses.share.no_consent");
            return;
        }
        if (list == null) list = FarmShareService.share(farm);
        if (list == null) {
            status = tr("ottoextra.horses.share.failed");
            return;
        }
        openShare(list);
    }

    private static String headerId(String farmName) {
        AnimalBookStore.Farm header = AnimalBookStore.current().farmForApi(AnimalBook.farmKey(farmName));
        return header == null ? "" : header.id();
    }

    private void openShare(FarmShareList list) {
        shareView = list;
        FarmShareService.syncSoon(list);
        show(View.SHARE);
    }

    private void joinCode() {
        String code = codeText;
        FarmShareService.JoinResult result = FarmShareService.join(code);
        status = tr("ottoextra.horses.share.join." + result.name().toLowerCase(Locale.ROOT));
        FarmShareList list = FarmShareService.listForCode(code);
        if (list == null) {
            clearAndInit();
            return;
        }
        codeText = "";
        rebuild();
        List<FarmShareWire.FarmRec> farms = list.liveFarms();
        if (farms.isEmpty()) {
            farm = null;
            sharedList = list;
            sharedFarm = null;
            openShare(list);
        } else {
            selectRow(new FarmRow(farms.getFirst().name(), "", 0, 0, list, farms.getFirst().id()));
        }
    }

    private void renewCodes() {
        if (shareView == null || shareView.readOnly()) return;
        long now = System.currentTimeMillis();
        if (now < renewUntil) {
            if (!FarmShareService.allowed()) notice = tr("ottoextra.horses.share.no_consent");
            else if (FarmShareService.renew(shareView)) notice = tr("ottoextra.horses.share.renewed");
            renewUntil = 0L;
        } else {
            renewUntil = now + CONFIRM_MILLIS;
        }
        clearAndInit();
    }

    private void leaveList() {
        if (shareView == null) return;
        long now = System.currentTimeMillis();
        if (now < leaveUntil) {
            if (shareView == sharedList) {
                sharedList = null;
                sharedFarm = null;
            }
            FarmShareService.leave(shareView);
            shareView = null;
            rebuild();
            show(View.LIST);
            return;
        }
        leaveUntil = now + CONFIRM_MILLIS;
        clearAndInit();
    }

    /** Own farm chip on the share page of an owned list: adds or removes the farm. */
    private void toggleMember(String farmName) {
        if (shareView == null) return;
        if (!FarmShareService.toggleMember(shareView, farmName)) {
            notice = tr("ottoextra.horses.share.member_taken");
        } else {
            notice = "";
        }
    }

    private void startNewFarm() {
        formName = "";
        formNote = "";
        formSymbol = FarmSymbols.DEFAULT;
        formKey = "";
        formShared = false;
        notice = "";
        noticeError = false;
        show(View.NEW_FARM);
    }

    private void openFarmHeader() {
        if (sharedList != null) {
            FarmShareWire.FarmRec shared = sharedList.farm(sharedFarm);
            if (shared == null || sharedList.readOnly()) return;
            formShared = true;
            formKey = shared.id();
            formName = shared.name();
            formNote = shared.note();
            formSymbol = FarmSymbols.canonical(shared.symbol());
            notice = "";
            noticeError = false;
            show(View.EDIT_FARM);
            return;
        }
        formShared = false;
        if (farm == null || farm.isEmpty()) return;
        formKey = AnimalBook.farmKey(farm);
        formName = farm;
        formNote = AnimalBookStore.current().farmNote(formKey);
        formSymbol = AnimalBookStore.current().symbol(formKey);
        notice = "";
        noticeError = false;
        show(View.EDIT_FARM);
    }

    private void saveFarmHeader() {
        if (formShared) {
            if (sharedList != null) FarmShareService.editSharedFarm(sharedList, formKey, formName, formNote, formSymbol);
            formShared = false;
            rebuild();
            show(View.LIST);
            return;
        }
        AnimalBookStore.Farm saved = AnimalBookStore.current().saveFarm(formKey, formName, formNote, formSymbol);
        if (saved != null) FarmShareService.syncSoon(AnimalBookStore.current().listOfFarm(saved.id()));
        show(View.LIST);
    }

    private void createFarm() {
        if (busy || !AnimalBreedingFarm.validName(formName)) return;
        busy = true;
        notice = Text.translatable("ottoextra.horses.farms.creating").getString();
        noticeError = false;
        updateButtons();
        String note = formNote;
        String symbol = formSymbol;
        HorseRegistry.createFarm(formName).whenComplete((created, failure) -> {
            if (client == null) return;
            client.execute(() -> {
                busy = false;
                if (failure == null) {
                    AnimalBookStore.current().saveFarm(created.nameKey(), created.name(), note, symbol);
                    if (client.currentScreen == this) selectFarm(created.name());
                    return;
                }
                notice = farmError(failure, true);
                noticeError = true;
                if (client.currentScreen == this) updateButtons();
            });
        });
    }

    /** Message for a failed farm request, same wording as the former farm screen. */
    static String farmError(Throwable failure, boolean creating) {
        while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
        String key = "ottoextra.horses.farms.failed";
        if (failure instanceof ApiProblem.ApiException api) {
            if (api.problem().isHttpStatus(403)) key = "ottoextra.horses.farms.forbidden";
            if (api.problem().isHttpStatus(409)) key = creating
                    ? "ottoextra.horses.farms.name_taken" : "ottoextra.horses.farms.conflict";
        }
        return Text.translatable(key).getString();
    }

    // ---- geometry ------------------------------------------------------------------

    private int rowsTop() { return spread.left().y() + 21; }
    private int rowsBottom() { return spread.left().y() + spread.left().h() - 67; }
    /** Top of the join-by-code block of the left page (rule, label, field). */
    private int codeFooterY() { return spread.left().y() + spread.left().h() - 64; }
    private int entriesTop() { return spread.right().y() + 39; }
    private int entriesBottom() { return spread.right().y() + spread.right().h() - 16; }
    private int visibleEntries() {
        return spread == null ? 1 : Math.max(1, (entriesBottom() - entriesTop()) / ENTRY_ROW);
    }
    private int symbolX() { return spread.right().x() + 10; }
    private int symbolY() { return spread.right().y() + (view == View.NEW_FARM || formShared ? 96 : 88); }

    /** Left row at a position: 0..n-1 for farm rows, n for "+ Neuer Hof", else -1. */
    private int leftRowAt(double mouseX, double mouseY) {
        AnimalBook.Page left = spread.left();
        if (mouseX < left.x() + 5 || mouseX >= left.x() + left.w() - 8 || mouseY < rowsTop()
                || mouseY >= rowsBottom()) {
            return -1;
        }
        boolean empty = isEmptyBook();
        int count = empty ? 0 : farmRows.size();
        int y = rowsTop() + (empty ? 14 : 0);
        for (int index = empty ? 0 : leftScroll; index <= count; index++) {
            int h = index == count ? NEW_ROW : FARM_ROW;
            if (y + h > rowsBottom()) return -1;
            if (mouseY >= y && mouseY < y + h) return index;
            y += h;
        }
        return -1;
    }

    private int entryAt(double mouseX, double mouseY) {
        AnimalBook.Page right = spread.right();
        if (view != View.LIST || isEmptyBook() || mouseX < right.x() + 6 || mouseX >= right.x() + right.w() - 6
                || mouseY < entriesTop() || mouseY >= entriesBottom()) {
            return -1;
        }
        int index = (int) ((mouseY - entriesTop()) / ENTRY_ROW);
        int size = sharedList != null ? sharedRows.size() : rows.size();
        return index < Math.min(visibleEntries(), size - scroll) ? index : -1;
    }

    /** Sort chips: label, x, width; computed the same way for drawing and clicking. */
    private record Chip(String id, String label, int x, int w, boolean selected) { }

    private List<Chip> chips() {
        AnimalBook.Page right = spread.right();
        List<Chip> chips = new ArrayList<>();
        int cx = right.x() + 10 + AnimalBook.smallWidth(textRenderer,
                Text.translatable("ottoextra.horses.book.sort").getString()) + 4;
        String farmLabel = Text.translatable(sorting.farmOrder() == SortOrder.FARMS_DESCENDING
                ? "ottoextra.horses.book.sort_farm_desc" : "ottoextra.horses.book.sort_farm").getString();
        cx = addChip(chips, "farm", farmLabel, cx, sort == BookSort.FARM);
        cx = addChip(chips, "name", Text.translatable("ottoextra.horses.book.sort_name").getString(), cx,
                sort == BookSort.NAME);
        cx = addChip(chips, "state", Text.translatable("ottoextra.horses.book.sort_state").getString(), cx,
                sort == BookSort.STATE);
        if (rows.stream().anyMatch(horse -> !horse.speed().isBlank() || !horse.jumpHeight().isBlank())) {
            cx = addChip(chips, "speed", metricLabel(true), cx, sorting.metric(true) != null);
            addChip(chips, "jump", metricLabel(false), cx, sorting.metric(false) != null);
        }
        String kind = Text.translatable(filter.translationKey()).getString();
        int kindW = AnimalBook.smallWidth(textRenderer, kind) + 8;
        chips.add(new Chip("kind", kind, right.x() + right.w() - 8 - kindW, kindW, filter != AnimalKind.ALL));
        return chips;
    }

    private int addChip(List<Chip> chips, String id, String label, int x, boolean selected) {
        int w = AnimalBook.smallWidth(textRenderer, label) + 8;
        chips.add(new Chip(id, label, x, w, selected));
        return x + w + 3;
    }

    private String metricLabel(boolean speed) {
        SortOrder order = sorting.metric(speed);
        String label = Text.translatable(speed ? "ottoextra.horses.overview.speed"
                : "ottoextra.horses.overview.jump").getString();
        return order == null ? label : label + (order.ascending() ? " ↓" : " ↑");
    }

    private void clickChip(Chip chip) {
        switch (chip.id()) {
            case "farm" -> {
                if (sort == BookSort.FARM) {
                    sorting.select(sorting.farmOrder() == SortOrder.FARMS_DESCENDING
                            ? SortOrder.FARMS_ASCENDING : SortOrder.FARMS_DESCENDING);
                }
                sort = BookSort.FARM;
            }
            case "name" -> sort = BookSort.NAME;
            case "state" -> sort = BookSort.STATE;
            case "speed" -> sorting.cycle(true);
            case "jump" -> sorting.cycle(false);
            case "kind" -> {
                var choices = HorseOverviewView.filters(OttoExtraConfig.active().animalInfo);
                filter = choices.get((choices.indexOf(filter) + 1) % choices.size());
            }
            default -> { }
        }
        scroll = 0;
        rebuild();
    }

    // ---- input ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double x = click.x();
        double y = click.y();
        if (click.button() == 0) {
            int row = leftRowAt(x, y);
            if (row >= 0) {
                int count = isEmptyBook() ? 0 : farmRows.size();
                if (row == count) startNewFarm();
                else selectRow(farmRows.get(row));
                return true;
            }
            AnimalBook.Page right = spread.right();
            if (view == View.SHARE && shareView != null && shareView.owned() && !shareView.readOnly()) {
                for (Member chip : memberChips()) {
                    if (x >= chip.x() && x < chip.x() + chip.w() && y >= chip.y() && y < chip.y() + 11) {
                        toggleMember(chip.id());
                        return true;
                    }
                }
            }
            if (view == View.LIST && !isEmptyBook()) {
                String title = entriesTitle();
                boolean editable = sharedList != null ? !sharedList.readOnly() && sharedList.farm(sharedFarm) != null
                        : farm != null && !farm.isEmpty();
                if (editable && y >= right.y() + 4 && y < right.y() + 18
                        && x >= right.x() + 10 && x < right.x() + 10 + textRenderer.getWidth(title)) {
                    openFarmHeader();
                    return true;
                }
                if (sharedList != null) {
                    int entry = entryAt(x, y);
                    if (entry >= 0) {
                        if (doubled) {
                            UUID uuid = UUID.fromString(sharedRows.get(scroll + entry).uuid());
                            HorseRegistry.databaseRecord(uuid).ifPresent(this::open);
                        }
                        return true;
                    }
                    return super.mouseClicked(click, doubled);
                }
                if (y >= right.y() + 23 && y < right.y() + 34) {
                    for (Chip chip : chips()) {
                        if (x >= chip.x() && x < chip.x() + chip.w()) {
                            clickChip(chip);
                            return true;
                        }
                    }
                }
                int entry = entryAt(x, y);
                if (entry >= 0) {
                    if (doubled) open(rows.get(scroll + entry));
                    return true;
                }
            }
            if (view == View.NEW_FARM || view == View.EDIT_FARM) {
                String picked = AnimalBook.SymbolPicker.pick(symbolX(), symbolY(), x, y, formSymbol);
                if (picked != null) {
                    formSymbol = picked;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double amount = vertical != 0 ? vertical : horizontal;
        AnimalBook.Page right = spread.right();
        if (view == View.LIST && mouseX >= right.x() && mouseX < right.x() + right.w()) {
            int size = sharedList != null ? sharedRows.size() : rows.size();
            scroll = (int) Math.max(0, Math.min(Math.max(0, size - visibleEntries()),
                    scroll - (long) Math.signum(amount)));
            return true;
        }
        if (mouseX < spread.left().x() + spread.left().w()) {
            leftScroll = (int) Math.max(0, Math.min(Math.max(0, farmRows.size() - 1),
                    leftScroll - (long) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    // ---- drawing -------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AnimalBook.drawSpread(context, width, height, spread);
        drawLeft(context, mouseX, mouseY);
        drawRight(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The book draws its own dimmed background.
    }

    private void text(DrawContext context, String text, int x, int y, int color) {
        AnimalBook.text(context, textRenderer, text, x, y, color);
    }

    private void small(DrawContext context, String text, int x, int y, int color) {
        AnimalBook.small(context, textRenderer, text, x, y, color);
    }

    private String clip(String text, int maxWidth) {
        return AnimalBook.clip(textRenderer, text, maxWidth);
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    private void drawLeft(DrawContext context, int mouseX, int mouseY) {
        AnimalBook.Page left = spread.left();
        int x = left.x();
        int w = left.w();
        text(context, tr("ottoextra.horses.book.farms"), x + 8, left.y() + 7, TERRA);
        AnimalBook.rule(context, x + 8, x + w - 10, left.y() + 18, PA_D);
        int hovered = leftRowAt(mouseX, mouseY);
        int top = rowsTop();
        boolean empty = isEmptyBook();
        int count = empty ? 0 : farmRows.size();
        if (empty) {
            small(context, tr("ottoextra.horses.book.no_farms"), x + 11, top + 4, INK_F);
            top += 14;
        }
        int textX = x + 8 + 16 + 4;
        for (int index = empty ? 0 : leftScroll; index <= count; index++) {
            int h = index == count ? NEW_ROW : FARM_ROW;
            if (top + h > rowsBottom()) break;
            int cy = top + 2;
            boolean hover = hovered == index;
            if (index == count) {
                small(context, tr("ottoextra.horses.book.new_farm"), x + 11, cy + 2,
                        hover || view == View.NEW_FARM ? INK : BRASS);
            } else {
                FarmRow row = farmRows.get(index);
                boolean on = selected(row);
                if (on || hover) AnimalBook.rect(context, x + 5, cy - 2, x + w - 8, cy + 21, PA_L);
                if (on) AnimalBook.rect(context, x + 5, cy - 2, x + 7, cy + 21, TERRA);
                if (row.name() == null) AnimalBook.symbol(context, ALL_SYMBOL, x + 8, cy - 1, 16);
                else if (row.shared()) {
                    AnimalBook.symbol(context, FarmSymbols.canonical(row.sharedId() == null ? row.list().symbol()
                            : row.key()), x + 8, cy - 1, 16);
                } else if (!row.name().isEmpty()) {
                    AnimalBook.symbol(context, AnimalBookStore.current().symbol(row.key()), x + 8, cy - 1, 16);
                }
                String ready = row.shared() ? tr(row.list().readOnly() ? "ottoextra.horses.share.read_only"
                        : "ottoextra.horses.share.shared") : tr("ottoextra.horses.book.ready_count", row.ready());
                int readyW = AnimalBook.smallWidth(textRenderer, ready);
                small(context, ready, x + w - 12 - readyW, cy + 2, row.shared() ? BRASS
                        : row.ready() > 0 ? OK_INK : INK_F);
                String name = row.name() == null ? tr("ottoextra.horses.book.all_farms")
                        : row.name().isEmpty() ? tr("ottoextra.horses.overview.no_farm") : row.name();
                text(context, clip(name, w - 12 - readyW - 4 - textX + x), textX, cy, INK);
                small(context, tr("ottoextra.horses.overview.horse_count", row.count()), textX, cy + 11, INK_M);
            }
            top += h;
        }
        AnimalBook.rule(context, x + 8, x + w - 10, codeFooterY(), PA_D);
        small(context, tr("ottoextra.horses.share.code_label"), x + 8, codeFooterY() + 2, INK_M);
        int fy = left.y() + left.h() - 37;
        AnimalBook.rule(context, x + 8, x + w - 10, fy, PA_D);
        small(context, tr("ottoextra.horses.book.search"), x + 8, fy + 4, INK_M);
    }

    private void drawRight(DrawContext context, int mouseX, int mouseY) {
        if (isEmptyBook() && view == View.LIST) {
            drawHowTo(context);
            return;
        }
        switch (view) {
            case LIST -> drawEntries(context, mouseX, mouseY);
            case NEW_FARM, EDIT_FARM -> drawFarmForm(context);
            case SHARE -> drawShare(context, mouseX, mouseY);
        }
    }

    private void drawHowTo(DrawContext context) {
        AnimalBook.Page right = spread.right();
        int x = right.x() + 10;
        int w = right.w() - 20;
        AnimalBook.header(context, textRenderer, right, 10, Text.translatable("ottoextra.horses.book.howto"), null, w);
        int cy = right.y() + 28;
        for (int index = 0; index < STEPS.length; index++) {
            AnimalBook.rect(context, x, cy, x + 12, cy + 12, TERRA);
            text(context, String.valueOf(index + 1), x + 4, cy + 2, PA_L);
            text(context, clip(tr(STEPS[index][0]), w - 18), x + 18, cy, INK);
            small(context, AnimalBook.clipSmall(textRenderer, tr(STEPS[index][1]), w - 18), x + 18, cy + 11, INK_M);
            cy += 30;
        }
    }

    private void drawEntries(DrawContext context, int mouseX, int mouseY) {
        AnimalBook.Page right = spread.right();
        int px = right.x();
        int pw = right.w();
        int x = px + 10;
        int w = pw - 20;
        int buttonsW = AnimalBook.buttonWidth(textRenderer, Text.translatable("ottoextra.horses.book.add"), 50) + 3
                + AnimalBook.buttonWidth(textRenderer, Text.translatable("ottoextra.horses.overview.refresh"), 62)
                + 4;
        if (sharedList != null) {
            drawSharedEntries(context, mouseX, mouseY);
            return;
        }
        String title = entriesTitle();
        String caption = tr("ottoextra.horses.overview.horse_count", rows.size())
                + (status.isEmpty() ? "" : ", " + status);
        AnimalBook.header(context, textRenderer, right, 10, Text.literal(title), caption, w + 2 - buttonsW);

        small(context, tr("ottoextra.horses.book.sort"), x, right.y() + 25, INK_F);
        for (Chip chip : chips()) {
            boolean hover = mouseY >= right.y() + 23 && mouseY < right.y() + 34 && mouseX >= chip.x()
                    && mouseX < chip.x() + chip.w();
            AnimalBook.chip(context, chip.x(), right.y() + 23, chip.w(), 11, chip.selected(), hover);
            small(context, chip.label(), chip.x() + 4, right.y() + 25, chip.selected() ? INK : INK_M);
        }

        if (rows.isEmpty()) {
            AnimalBook.smallWrapped(context, textRenderer, Text.translatable(refreshing
                    ? "ottoextra.horses.overview.loading" : "ottoextra.horses.overview.empty"),
                    x, entriesTop() + 6, w, 8, INK_M, 3);
            return;
        }
        long now = System.currentTimeMillis();
        int hovered = entryAt(mouseX, mouseY);
        int count = Math.min(visibleEntries(), rows.size() - scroll);
        int barX0 = px + pw - 62;
        int barX1 = px + pw - 10;
        for (int index = 0; index < count; index++) {
            AnimalBreedingHorse horse = rows.get(scroll + index);
            int cy = entriesTop() + index * ENTRY_ROW;
            if (index == hovered) AnimalBook.rect(context, px + 6, cy - 2, px + pw - 6, cy + 23, PA_L);
            AnimalBook.rect(context, px + 9, cy + 1, px + 27, cy + 19, PA_D);
            AnimalBook.rect(context, px + 10, cy + 2, px + 26, cy + 18, PA_S);
            AnimalBook.model(context, AnimalBook.preview(horse, fallbackAnimals), px + 10, cy + 2, px + 26, cy + 18, 5);

            AnimalBookStatus.View state = status(horse, now);
            int ink = AnimalBook.toneInk(state.tone());
            String label = tr(AnimalBookStatus.statusKey(state.status()), AnimalBookStatus.statusArgs(state.status()));
            int labelW = textRenderer.getWidth(label);
            text(context, label, px + pw - 10 - labelW, cy, ink);
            int textX = px + 32;
            String name = clip(HorseOverviewView.displayName(horse), px + pw - 10 - labelW - 16 - textX);
            text(context, name, textX, cy, INK);
            AnimalBook.genderIcon(context, horse.gender(), textX + 2 + textRenderer.getWidth(name), cy);
            small(context, AnimalBook.clipSmall(textRenderer, caption(horse), barX0 - 4 - textX), textX, cy + 11,
                    INK_M);
            AnimalBook.bar(context, barX0, cy + 13, barX1, 3, state.progress(), ink);
            AnimalBook.rule(context, px + 10, px + pw - 8, cy + 23, PA_S);
        }
        int more = rows.size() - scroll - count;
        String footer = more > 0 ? tr(more == 1 ? "ottoextra.horses.book.more_one" : "ottoextra.horses.book.more", more)
                : tr("ottoextra.horses.book.open_hint");
        small(context, AnimalBook.clipSmall(textRenderer, footer, w), x, right.y() + right.h() - 12, INK_F);
    }

    private String entriesTitle() {
        if (sharedList != null) {
            FarmShareWire.FarmRec shared = sharedList.farm(sharedFarm);
            return shared != null ? shared.name()
                    : sharedList.name().isBlank() ? tr("ottoextra.horses.share.list_default") : sharedList.name();
        }
        return farm == null ? tr("ottoextra.horses.book.all_farms")
                : farm.isEmpty() ? tr("ottoextra.horses.overview.no_farm") : farm;
    }

    /** Animals of a farm of a joined list: label, note, and the status when the user has the animal too. */
    private void drawSharedEntries(DrawContext context, int mouseX, int mouseY) {
        AnimalBook.Page right = spread.right();
        int px = right.x();
        int pw = right.w();
        int x = px + 10;
        int w = pw - 20;
        int buttonsW = AnimalBook.buttonWidth(textRenderer, Text.translatable("ottoextra.horses.overview.refresh"), 62) + 4;
        String caption = tr("ottoextra.horses.overview.horse_count", sharedRows.size()) + ", "
                + tr(sharedList.readOnly() ? "ottoextra.horses.share.read_only" : "ottoextra.horses.share.shared")
                + (status.isEmpty() ? "" : ", " + status);
        AnimalBook.header(context, textRenderer, right, 10, Text.literal(entriesTitle()), caption, w + 2 - buttonsW);
        FarmShareWire.FarmRec shared = sharedList.farm(sharedFarm);
        String note = shared == null ? "" : shared.note();
        small(context, AnimalBook.clipSmall(textRenderer, note.isEmpty()
                ? tr(sharedList.readOnly() ? "ottoextra.horses.share.read_only_hint" : "ottoextra.horses.share.edit_hint")
                : note, w), x, right.y() + 25, INK_F);
        if (sharedRows.isEmpty()) {
            AnimalBook.smallWrapped(context, textRenderer, Text.translatable("ottoextra.horses.share.no_animals"),
                    x, entriesTop() + 6, w, 8, INK_M, 3);
            return;
        }
        long now = System.currentTimeMillis();
        int hovered = entryAt(mouseX, mouseY);
        int count = Math.min(visibleEntries(), sharedRows.size() - scroll);
        int barX0 = px + pw - 62;
        int barX1 = px + pw - 10;
        for (int index = 0; index < count; index++) {
            FarmShareWire.AnimalRec animal = sharedRows.get(scroll + index);
            int cy = entriesTop() + index * ENTRY_ROW;
            if (index == hovered) AnimalBook.rect(context, px + 6, cy - 2, px + pw - 6, cy + 23, PA_L);
            AnimalBook.rect(context, px + 9, cy + 1, px + 27, cy + 19, PA_D);
            AnimalBook.rect(context, px + 10, cy + 2, px + 26, cy + 18, PA_S);
            var record = HorseRegistry.databaseRecord(UUID.fromString(animal.uuid()));
            int labelW = 0;
            if (record.isPresent()) {
                AnimalBreedingHorse horse = record.get();
                AnimalBook.model(context, AnimalBook.preview(horse, fallbackAnimals), px + 10, cy + 2, px + 26, cy + 18, 5);
                AnimalBookStatus.View state = status(horse, now);
                int ink = AnimalBook.toneInk(state.tone());
                String label = tr(AnimalBookStatus.statusKey(state.status()), AnimalBookStatus.statusArgs(state.status()));
                labelW = textRenderer.getWidth(label);
                text(context, label, px + pw - 10 - labelW, cy, ink);
                AnimalBook.bar(context, barX0, cy + 13, barX1, 3, state.progress(), ink);
            } else if (shared != null) {
                AnimalBook.symbol(context, FarmSymbols.canonical(shared.symbol()), px + 10, cy + 2, 16);
            }
            int textX = px + 32;
            text(context, clip(sharedName(animal), px + pw - 10 - labelW - 6 - textX), textX, cy, INK);
            String line = animal.note().isEmpty() ? tr("ottoextra.horses.share.no_note") : animal.note();
            small(context, AnimalBook.clipSmall(textRenderer, line, (record.isPresent() ? barX0 - 4 : px + pw - 10) - textX),
                    textX, cy + 11, INK_M);
            AnimalBook.rule(context, px + 10, px + pw - 8, cy + 23, PA_S);
        }
        int more = sharedRows.size() - scroll - count;
        if (more > 0) {
            small(context, AnimalBook.clipSmall(textRenderer, tr(more == 1 ? "ottoextra.horses.book.more_one"
                    : "ottoextra.horses.book.more", more), w), x, right.y() + right.h() - 12, INK_F);
        }
    }

    /** Own farm chip on the share page of an owned list. */
    private record Member(String id, String label, int x, int y, int w, boolean selected) { }

    private int membersY() {
        return spread.right().y() + 144;
    }

    private List<Member> memberChips() {
        List<Member> chips = new ArrayList<>();
        if (shareView == null || !shareView.owned()) return chips;
        AnimalBook.Page right = spread.right();
        int x0 = right.x() + 10;
        int x1 = right.x() + right.w() - 10;
        int cx = x0;
        int cy = membersY() + 9;
        int bottom = right.y() + right.h() - 38;
        var members = shareView.members();
        for (FarmRow row : farmRows) {
            if (row.shared() || row.name() == null || row.name().isEmpty()) continue;
            String label = AnimalBook.clipSmall(textRenderer, row.name(), x1 - x0 - 8);
            int w = AnimalBook.smallWidth(textRenderer, label) + 8;
            if (cx + w > x1 && cx > x0) {
                cx = x0;
                cy += 13;
            }
            if (cy + 11 > bottom) break;
            chips.add(new Member(row.name(), label, cx, cy, w, members.contains(headerId(row.name()))));
            cx += w + 3;
        }
        return chips;
    }

    private void drawShare(DrawContext context, int mouseX, int mouseY) {
        AnimalBook.Page right = spread.right();
        FarmShareList list = shareView;
        int x = right.x() + 10;
        int w = right.w() - 20;
        String listName = list == null ? "" : list.name().isBlank() ? tr("ottoextra.horses.share.list_default") : list.name();
        AnimalBook.header(context, textRenderer, right, 10, Text.translatable("ottoextra.horses.share.title"), listName, w);
        if (list == null) return;
        int boxW = codeBoxWidth(w);
        int cy = right.y() + 28;
        if (list.writeCode() != null) {
            codeBox(context, x, cy, boxW, tr("ottoextra.horses.share.for_group"),
                    tr("ottoextra.horses.share.for_group_hint"), list.writeCode());
            cy += 40;
        }
        codeBox(context, x, cy, boxW, tr("ottoextra.horses.share.for_others"),
                tr("ottoextra.horses.share.for_others_hint"), list.readCode());
        cy += 40;
        int top = cy;
        int bottom = cy;
        if (list.writeCode() != null) {
            bottom = AnimalBook.smallWrapped(context, textRenderer,
                    Text.translatable("ottoextra.horses.share.password_warning"), x + 7, cy + 1, w - 7, 8, SOON_INK, 3);
        } else {
            bottom = AnimalBook.smallWrapped(context, textRenderer,
                    Text.translatable("ottoextra.horses.share.read_only_hint"), x + 7, cy + 1, w - 7, 8, INK_M, 2);
        }
        bottom = AnimalBook.smallWrapped(context, textRenderer, Text.translatable("ottoextra.horses.share.links_hint"),
                x + 7, bottom, w - 7, 8, INK_M, 1);
        AnimalBook.rect(context, x, top, x + 2, bottom - 2, list.writeCode() != null ? SOON_INK : PA_D);
        if (list.owned() && !list.readOnly()) {
            small(context, tr("ottoextra.horses.share.members"), x, membersY(), INK_M);
            for (Member chip : memberChips()) {
                boolean hover = mouseX >= chip.x() && mouseX < chip.x() + chip.w() && mouseY >= chip.y()
                        && mouseY < chip.y() + 11;
                AnimalBook.chip(context, chip.x(), chip.y(), chip.w(), 11, chip.selected(), hover);
                small(context, chip.label(), chip.x() + 4, chip.y() + 2, chip.selected() ? INK : INK_M);
            }
        }
        String line;
        int ink = INK_F;
        String error = FarmShareService.error(list);
        if (!notice.isEmpty()) {
            line = notice;
            ink = TERRA;
        } else if (!FarmShareService.allowed()) {
            line = tr("ottoextra.horses.share.no_consent");
            ink = TERRA;
        } else if (!error.isEmpty()) {
            line = error.equals("403") ? tr("ottoextra.horses.share.forbidden") : tr("ottoextra.horses.share.error", error);
            ink = TERRA;
        } else if (FarmShareService.lastSync(list) > 0) {
            line = tr("ottoextra.horses.share.synced");
        } else {
            line = tr("ottoextra.horses.share.syncing");
        }
        small(context, AnimalBook.clipSmall(textRenderer, line, w), x, right.y() + right.h() - 34, ink);
    }

    /** Title with caption and a light box with the code; the buttons sit right of the box. */
    private void codeBox(DrawContext context, int x, int y, int boxW, String title, String hint, String code) {
        text(context, title, x, y, INK);
        small(context, AnimalBook.clipSmall(textRenderer, hint, Math.max(0, boxW - textRenderer.getWidth(title) - 4)),
                x + 4 + textRenderer.getWidth(title), y + 2, INK_M);
        int by = y + 12;
        AnimalBook.rect(context, x, by, x + boxW, by + 16, PA_D);
        AnimalBook.rect(context, x + 1, by + 1, x + boxW - 1, by + 15, PA_L);
        text(context, clip(FarmShareCodes.display(code), boxW - 10), x + 5, by + 4, INK);
    }

    /** "Reitpferd, Hof Eichengrund", plus "melkbar"/"scherbar" when the last answer said so. */
    private String caption(AnimalBreedingHorse horse) {
        String species = horse.animalType().isBlank() ? "–" : horse.animalType();
        String text = horse.currentFarm().isBlank() ? species
                : tr("ottoextra.horses.book.caption", species, horse.currentFarm());
        var observation = HorseRegistry.observation(horse.uuid());
        if (observation.isPresent()) {
            var value = observation.get().snapshot();
            if (value.hasMilkable() && AnimalBookStatus.positive(value.milkable())) {
                text += ", " + tr("ottoextra.horses.book.milkable");
            }
            if (value.hasShearable() && AnimalBookStatus.positive(value.shearable())) {
                text += ", " + tr("ottoextra.horses.book.shearable");
            }
        }
        if (de.ottoextra.horses.HorseFinder.isTarget(horse.uuid())) {
            text += ", " + tr("ottoextra.horses.book.searching");
        }
        return text;
    }

    private void drawFarmForm(DrawContext context) {
        AnimalBook.Page right = spread.right();
        int x = right.x() + 10;
        int w = right.w() - 20;
        boolean editing = view == View.EDIT_FARM;
        AnimalBook.header(context, textRenderer, right, 10, Text.translatable(editing
                ? "ottoextra.horses.book.edit_farm" : "ottoextra.horses.book.new_farm_title"),
                editing ? formName : null, w);
        small(context, tr("ottoextra.horses.book.name"), x, right.y() + 26, INK_M);
        if (editing && !formShared) {
            text(context, clip(formName, w), x, right.y() + 34, INK);
            small(context, tr("ottoextra.horses.book.note"), x, right.y() + 48, INK_M);
        } else {
            small(context, tr("ottoextra.horses.book.note"), x, right.y() + 52, INK_M);
        }
        AnimalBook.SymbolPicker.draw(context, textRenderer, symbolX(), symbolY(), formSymbol);
        int hintY = symbolY() + AnimalBook.SymbolPicker.HEIGHT + 6;
        AnimalBook.smallWrapped(context, textRenderer, Text.translatable(formShared
                ? "ottoextra.horses.share.shared_hint" : "ottoextra.horses.book.local_hint"), x, hintY, w, 8, INK_F, 3);
        if (!notice.isEmpty()) {
            AnimalBook.smallWrapped(context, textRenderer, Text.literal(notice), x, right.y() + right.h() - 40, w, 8,
                    noticeError ? TERRA : INK_M, 2);
        }
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
