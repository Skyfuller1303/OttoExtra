package de.ottoextra.horses.ui;

import de.ottoextra.OttoExtra;
import de.ottoextra.animalinfo.AnimalBookStatus;
import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.animalinfo.AnimalToastModel;
import de.ottoextra.api.ApiProblem;
import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.horses.AnimalBookStore;
import de.ottoextra.horses.AnimalGender;
import de.ottoextra.horses.AnimalNameCatalog;
import de.ottoextra.horses.FarmSymbols;
import de.ottoextra.horses.HorseFinder;
import de.ottoextra.horses.HorseRegistry;
import de.ottoextra.horses.RegistryAnimals;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static de.ottoextra.horses.ui.AnimalBook.BRASS;
import static de.ottoextra.horses.ui.AnimalBook.INK;
import static de.ottoextra.horses.ui.AnimalBook.INK_F;
import static de.ottoextra.horses.ui.AnimalBook.INK_M;
import static de.ottoextra.horses.ui.AnimalBook.PA_D;
import static de.ottoextra.horses.ui.AnimalBook.PA_L;
import static de.ottoextra.horses.ui.AnimalBook.PA_S;
import static de.ottoextra.horses.ui.AnimalBook.TERRA;

/**
 * The register entry of one animal as a book (docs/design/tierbuch/tier_buch_eintrag.png): the animal with
 * its live model on the left page, name, farm, values, feeding bar and a local note on the right page. The
 * farm is chosen on the right page itself (tier_buch_hof_waehlen.png). Name and farm are saved through the
 * register API with its permissions as before; the note stays local ({@link AnimalBookStore}).
 *
 * <p>The screen opens either for an animal in the world (with its current observation, as before) or for a
 * register record only, e.g. from the overview for an animal that is not loaded. Without an observation the
 * name cannot be changed, because the API needs the observed values for that.
 */
public final class BasicHorseRecordScreen extends Screen {
    private static final AnimalNameCatalog NAME_CATALOG = AnimalNameCatalog.loadDefault();
    private static final long CONFIRM_MILLIS = 3_000L;
    private static final int PICK_ROW = 21;

    private enum View { ENTRY, FARM_PICK }

    private record Value(String label, String value, int color) { }

    private final Screen parent;
    private final AnimalEntity animal;
    private final UUID entityUuid;
    private AnimalInfoObservation observation;
    private final Map<HorseOverviewView.AnimalKind, AnimalEntity> fallbackAnimals =
            new EnumMap<>(HorseOverviewView.AnimalKind.class);

    private View view = View.ENTRY;
    private AnimalBook.Spread spread;
    private TextFieldWidget nameField;
    private String nameDraft;
    private String noteDraft;
    private final String savedNote;
    private AnimalBreedingFarm farmDraft;
    private String pickKey;
    private String newFarmName = "";
    private String newFarmSymbol = FarmSymbols.DEFAULT;
    private int pickScroll;
    private ButtonWidget saveButton;
    private ButtonWidget diceButton;
    private ButtonWidget createFarmButton;
    private ButtonWidget deleteButton;
    private ButtonWidget finderButton;
    private boolean saving;
    private boolean busy;
    private boolean errorStatus;
    private boolean accessRequested;
    private long deleteUntil;
    private String status = "";

    public BasicHorseRecordScreen(Screen parent, AnimalEntity animal, AnimalInfoObservation observation) {
        super(Text.translatable("ottoextra.horses.record.title"));
        this.parent = parent;
        this.animal = animal;
        this.entityUuid = animal.getUuid();
        this.observation = observation;
        this.savedNote = AnimalBookStore.current().note(entityUuid);
    }

    /** Entry of a register record without a loaded animal. */
    public BasicHorseRecordScreen(Screen parent, UUID entityUuid) {
        super(Text.translatable("ottoextra.horses.record.title"));
        this.parent = parent;
        this.animal = null;
        this.entityUuid = entityUuid;
        this.observation = HorseRegistry.observation(entityUuid).orElse(null);
        this.savedNote = AnimalBookStore.current().note(entityUuid);
    }

    // ---- layout --------------------------------------------------------------------

    @Override
    protected void init() {
        if (nameField != null) nameDraft = nameField.getText();
        if (noteDraft == null) noteDraft = savedNote;
        if (observation == null) observation = HorseRegistry.observation(entityUuid).orElse(null);
        if (!accessRequested) {
            accessRequested = true;
            HorseRegistry.refreshNamingPlayer(client == null || client.player == null
                    ? null : client.player.getUuid()).exceptionally(error -> null);
            HorseRegistry.refresh().exceptionally(error -> List.of());
            if (HorseRegistry.farms().isEmpty()) {
                HorseRegistry.refreshFarms().exceptionally(error -> List.of());
            }
            if (animal != null) HorseRegistry.refreshDatabaseRecords().exceptionally(error -> List.of());
        }
        spread = AnimalBook.spread(width, height);
        nameField = null;
        diceButton = null;
        saveButton = null;
        createFarmButton = null;
        deleteButton = null;
        finderButton = null;
        AnimalBook.Page left = spread.left();
        AnimalBook.Page right = spread.right();

        // Left page: overview, finder and delete.
        Text overview = Text.translatable("ottoextra.horses.record.overview");
        addDrawableChild(new AnimalBook.Button(left.x() + 10, left.y() + left.h() - 22,
                AnimalBook.buttonWidth(textRenderer, overview, 64), overview, AnimalBook.Tone.SLATE, b -> openOverview()));
        AnimalBreedingHorse record = record();
        if (record != null) {
            Text find = Text.translatable("ottoextra.horses.finder.stop");
            int findW = AnimalBook.buttonWidth(textRenderer, find, 56);
            finderButton = addDrawableChild(new AnimalBook.Button(left.x() + 10, left.y() + left.h() - 40, findW,
                    finderText(), AnimalBook.Tone.SLATE, b -> toggleFinder()));
            if (record.canManage()) {
                Text sure = Text.translatable("ottoextra.horses.book.sure");
                int deleteW = Math.max(AnimalBook.buttonWidth(textRenderer, sure, 40), AnimalBook.buttonWidth(
                        textRenderer, Text.translatable("ottoextra.horses.overview.delete.yes"), 40));
                deleteButton = addDrawableChild(new AnimalBook.Button(left.x() + 10 + findW + 3,
                        left.y() + left.h() - 40, deleteW, Text.translatable("ottoextra.horses.overview.delete.yes"),
                        AnimalBook.Tone.DANGER, b -> deleteRecord()));
            }
        }

        int rx = right.x() + 10;
        int rw = right.w() - 20;
        int edge = right.x() + right.w() - 8;
        int footerY = right.y() + right.h() - 22;
        if (view == View.ENTRY) {
            if (canEditName()) {
                nameField = new AnimalBook.Field(textRenderer, rx, right.y() + 33, rw - AnimalNameDiceButton.WIDTH - 4,
                        Text.translatable("ottoextra.horses.record.name_hint"));
                nameField.setMaxLength(64);
                nameField.setText(nameDraft != null ? nameDraft : HorseRegistry.name(entityUuid).orElse(""));
                nameField.setChangedListener(value -> updateButtons());
                addDrawableChild(nameField);
                diceButton = addDrawableChild(new AnimalNameDiceButton(rx + rw - AnimalNameDiceButton.WIDTH, right.y() + 32, () -> {
                    var unavailable = HorseRegistry.names().stream()
                            .map(de.ottoextra.api.model.AnimalBreedingHorseName::registryName).toList();
                    String coat = animal instanceof HorseEntity horse ? horse.getHorseColor().name() : "";
                    String marking = animal instanceof HorseEntity horse ? horse.getMarking().name() : "";
                    nameField.setText(NAME_CATALOG.next(gender(), coat, marking, nameField.getText(), unavailable));
                }));
            }
            TextFieldWidget note = new AnimalBook.Field(textRenderer, rx, noteY() + 7, rw,
                    Text.translatable("ottoextra.horses.book.note_hint"));
            note.setMaxLength(AnimalBookStore.NOTE_MAX);
            note.setText(noteDraft);
            note.setChangedListener(value -> {
                noteDraft = value;
                updateButtons();
            });
            addDrawableChild(note);
            Text save = Text.translatable("ottoextra.horses.record.save");
            Text cancel = Text.translatable("gui.cancel");
            int saveW = AnimalBook.buttonWidth(textRenderer, save, 60);
            int cancelW = AnimalBook.buttonWidth(textRenderer, cancel, 60);
            saveButton = addDrawableChild(new AnimalBook.Button(edge - saveW, footerY, saveW, save,
                    AnimalBook.Tone.PRIMARY, b -> save()));
            addDrawableChild(new AnimalBook.Button(edge - saveW - 3 - cancelW, footerY, cancelW, cancel,
                    AnimalBook.Tone.SLATE, b -> close()));
            if (nameField != null) setInitialFocus(nameField);
        } else {
            Text create = Text.translatable("ottoextra.horses.book.create");
            int createW = AnimalBook.buttonWidth(textRenderer, create, 60);
            TextFieldWidget farmName = new AnimalBook.Field(textRenderer, rx, newFarmY() + 7, rw - createW - 8,
                    Text.translatable("ottoextra.horses.book.farm_name_hint"));
            farmName.setMaxLength(AnimalBookStore.FARM_NAME_MAX);
            farmName.setText(newFarmName);
            farmName.setChangedListener(value -> {
                newFarmName = value;
                updateButtons();
            });
            addDrawableChild(farmName);
            de.ottoextra.ui.complete.TabCompletion.attach(farmName, de.ottoextra.ui.complete.TabCompletionSources::hoefe, false);
            createFarmButton = addDrawableChild(new AnimalBook.Button(edge - createW, newFarmY() + 5, createW,
                    create, AnimalBook.Tone.SLATE, b -> createFarm()));
            Text done = Text.translatable("ottoextra.horses.book.done");
            int doneW = AnimalBook.buttonWidth(textRenderer, done, 60);
            addDrawableChild(new AnimalBook.Button(edge - doneW, footerY, doneW, done, AnimalBook.Tone.PRIMARY,
                    b -> finishPick()));
        }
        updateButtons();
    }

    @Override
    public void tick() {
        if (observation == null) observation = HorseRegistry.observation(entityUuid).orElse(null);
        if (!saving && view == View.ENTRY && (nameField != null) != canEditName()) clearAndInit();
        updateButtons();
    }

    private void updateButtons() {
        if (diceButton != null) diceButton.active = !saving && canEditName();
        if (saveButton != null) saveButton.active = !saving && (nameReady() || farmDraft != null && canChangeFarm()
                || !noteDraft.trim().equals(savedNote));
        if (createFarmButton != null) createFarmButton.active = !busy && AnimalBreedingFarm.validName(newFarmName);
        if (finderButton != null) finderButton.setMessage(finderText());
        if (deleteButton != null) {
            deleteButton.active = !saving;
            deleteButton.setMessage(Text.translatable(System.currentTimeMillis() < deleteUntil
                    ? "ottoextra.horses.book.sure" : "ottoextra.horses.overview.delete.yes"));
        }
    }

    // ---- state ---------------------------------------------------------------------

    private AnimalBreedingHorse record() {
        return HorseRegistry.databaseRecord(entityUuid).orElse(null);
    }

    private AnimalInfoSnapshot snapshot() {
        return observation == null ? null : observation.snapshot();
    }

    private boolean canEditName() {
        return observation != null && HorseRegistry.canEditName(entityUuid, currentLocation());
    }

    private boolean nameReady() {
        return canEditName() && nameField != null && !nameField.getText().trim().isEmpty();
    }

    /** Farm changes go with the name save, or straight to the record when it may be edited. */
    private boolean canChangeFarm() {
        AnimalBreedingHorse record = record();
        return canEditName() || record != null && (record.canEdit() || record.canManage());
    }

    private String currentFarm() {
        if (farmDraft != null) return farmDraft.name();
        AnimalBreedingHorse record = record();
        return record == null ? "" : record.currentFarm();
    }

    private String currentFarmKey() {
        AnimalBreedingHorse record = record();
        if (record == null || record.currentFarm().isBlank()) return "";
        return HorseRegistry.farms().stream().filter(f -> f.name().equalsIgnoreCase(record.currentFarm()))
                .map(AnimalBreedingFarm::nameKey).findFirst().orElse("");
    }

    private String gender() {
        AnimalInfoSnapshot value = snapshot();
        if (value != null && !value.gender().isBlank()) return value.gender();
        AnimalBreedingHorse record = record();
        return record == null ? "" : record.gender();
    }

    private String species() {
        AnimalInfoSnapshot value = snapshot();
        if (value != null && !value.animalName().isBlank()) return value.animalName();
        AnimalBreedingHorse record = record();
        return record == null ? "" : record.animalType();
    }

    private AnimalEntity loadedAnimal() {
        if (animal != null) return animal.isAlive() ? animal : null;
        if (client == null || client.world == null) return null;
        Entity loaded = client.world.getEntity(entityUuid);
        return loaded instanceof AnimalEntity each && each.isAlive() && RegistryAnimals.supports(each) ? each : null;
    }

    private AnimalBookStatus.View status(long now) {
        AnimalEntity loaded = loadedAnimal();
        Boolean baby = loaded == null ? null : loaded.isBaby();
        AnimalBreedingHorse record = record();
        AnimalInfoSnapshot value = snapshot();
        if (value != null && (record == null || observation.observedAt().toEpochMilli() >= record.observedAtEpochMillis())) {
            return AnimalBookStatus.of(entityUuid, value.animalName(), value.mast(), value.breedingReady(),
                    observation.observedAt().toEpochMilli(), baby, now);
        }
        if (record != null) {
            return AnimalBookStatus.of(entityUuid, record.animalType(), record.mast(), record.breedingReady(),
                    record.observedAtEpochMillis(), baby, now);
        }
        return AnimalBookStatus.of(entityUuid, species(), "", "", 0L, baby, now);
    }

    private AnimalBreedingHorseLocation currentLocation() {
        if (client == null || client.world == null) return null;
        Entity loaded = client.world.getEntity(entityUuid);
        if (!RegistryAnimals.supports(loaded) || !loaded.isAlive()) return null;
        return new AnimalBreedingHorseLocation(
                client.world.getRegistryKey().getValue().toString(),
                (int) Math.floor(loaded.getX()),
                (int) Math.floor(loaded.getY()),
                (int) Math.floor(loaded.getZ()));
    }

    // ---- actions -------------------------------------------------------------------

    private void openOverview() {
        if (client == null) return;
        if (parent instanceof BasicHorseOverviewScreen) client.setScreen(parent);
        else client.setScreen(new BasicHorseOverviewScreen(this));
    }

    private Text finderText() {
        return Text.translatable(HorseFinder.isTarget(entityUuid)
                ? "ottoextra.horses.finder.stop" : "ottoextra.horses.finder.start");
    }

    private void toggleFinder() {
        HorseFinder.toggle(entityUuid);
        String name = HorseRegistry.name(entityUuid).orElse("???");
        status = HorseFinder.isTarget(entityUuid)
                ? Text.translatable("ottoextra.horses.finder.active", name).getString()
                : Text.translatable("ottoextra.horses.finder.stopped").getString();
        errorStatus = false;
        updateButtons();
    }

    private void deleteRecord() {
        AnimalBreedingHorse record = record();
        if (record == null || !record.canManage() || saving) return;
        long now = System.currentTimeMillis();
        if (now >= deleteUntil) {
            deleteUntil = now + CONFIRM_MILLIS;
            updateButtons();
            return;
        }
        deleteUntil = 0L;
        String name = HorseOverviewView.displayName(record)
                + (record.registryName().isBlank() ? " (" + record.entityUuid() + ")" : "");
        saving = true;
        status = Text.translatable("ottoextra.horses.overview.delete.running").getString();
        errorStatus = false;
        updateButtons();
        HorseRegistry.deleteDatabaseRecord(entityUuid).whenComplete((ignored, error) -> {
            if (client == null) return;
            client.execute(() -> {
                saving = false;
                if (error == null) {
                    AnimalBookStore.current().deleteAnimal(entityUuid);
                    if (client.player != null) {
                        client.player.sendMessage(Text.translatable("ottoextra.horses.overview.delete.success", name),
                                true);
                    }
                    if (client.currentScreen == this) close();
                    return;
                }
                OttoExtra.LOGGER.warn("Pferdeakte {} konnte nicht geloescht werden: {}", entityUuid,
                        unwrap(error).toString());
                status = Text.translatable("ottoextra.horses.overview.delete.failed", name).getString();
                errorStatus = true;
                if (client.currentScreen == this) updateButtons();
            });
        });
    }

    private void openPicker() {
        if (!canChangeFarm() || saving) return;
        if (nameField != null) nameDraft = nameField.getText();
        pickKey = farmDraft != null ? farmDraft.nameKey() : currentFarmKey();
        newFarmName = "";
        newFarmSymbol = FarmSymbols.DEFAULT;
        pickScroll = 0;
        view = View.FARM_PICK;
        if (HorseRegistry.farms().isEmpty()) {
            HorseRegistry.refreshFarms().exceptionally(error -> List.of());
        }
        clearAndInit();
    }

    private void finishPick() {
        String current = currentFarmKey();
        if (pickKey == null || pickKey.isEmpty() || pickKey.equals(current)) {
            farmDraft = null;
        } else {
            farmDraft = HorseRegistry.farms().stream().filter(f -> f.nameKey().equals(pickKey) && f.canEdit())
                    .findFirst().orElse(null);
        }
        view = View.ENTRY;
        clearAndInit();
    }

    private void createFarm() {
        if (busy || !AnimalBreedingFarm.validName(newFarmName)) return;
        busy = true;
        status = Text.translatable("ottoextra.horses.farms.creating").getString();
        errorStatus = false;
        updateButtons();
        String symbol = newFarmSymbol;
        HorseRegistry.createFarm(newFarmName).whenComplete((created, failure) -> {
            if (client == null) return;
            client.execute(() -> {
                busy = false;
                if (failure == null) {
                    AnimalBookStore.current().saveFarm(created.nameKey(), created.name(), "", symbol);
                    pickKey = created.nameKey();
                    newFarmName = "";
                    status = Text.translatable("ottoextra.horses.farms.created", created.name()).getString();
                    if (client.currentScreen == this) clearAndInit();
                    return;
                }
                status = BasicHorseOverviewScreen.farmError(failure, true);
                errorStatus = true;
                if (client.currentScreen == this) updateButtons();
            });
        });
    }

    private List<AnimalBreedingFarm> pickFarms() {
        return HorseRegistry.farms();
    }

    private void save() {
        if (saving) return;
        String note = noteDraft == null ? "" : noteDraft;
        if (nameReady()) {
            saveName(note);
        } else if (farmDraft != null && canChangeFarm()) {
            assignFarm(note);
        } else {
            saveLocal(note, currentFarm());
            close();
        }
    }

    private void saveLocal(String note, String farmName) {
        String key = farmName == null || farmName.isBlank() ? "" : AnimalBook.farmKey(farmName);
        AnimalBookStore.current().saveAnimal(entityUuid, key, farmName, note);
    }

    private void saveName(String note) {
        saving = true;
        errorStatus = false;
        status = Text.translatable("ottoextra.horses.message.saving").getString();
        updateButtons();
        String coat = "";
        String marking = "";
        if (animal instanceof HorseEntity vanillaHorse) {
            coat = vanillaHorse.getHorseColor().name();
            marking = vanillaHorse.getMarking().name();
        }
        AnimalBreedingFarm farm = farmDraft;
        HorseRegistry.saveNameAndFarm(entityUuid, nameField.getText(), currentLocation(),
                        coat, marking, farm == null ? null : farm.nameKey())
                .whenComplete((saved, error) -> {
                    if (client == null) return;
                    client.execute(() -> {
                        saving = false;
                        if (error == null || unwrap(error) instanceof HorseRegistry.FarmAssignmentException) {
                            // The name is saved even when only the farm failed; keep the note with it.
                            saveLocal(note, error == null && farm != null ? farm.name()
                                    : saved == null ? currentFarm() : saved.currentFarm());
                        }
                        if (client.currentScreen != this) return;
                        updateButtons();
                        if (error != null) {
                            Throwable cause = unwrap(error);
                            errorStatus = true;
                            status = cause instanceof HorseRegistry.FarmAssignmentException
                                    ? Text.translatable("ottoextra.horses.farms.partial_save").getString()
                                    : errorMessage(cause);
                            OttoExtra.LOGGER.warn("Tiername {} konnte nicht gespeichert werden: {}",
                                    entityUuid, cause.toString());
                            return;
                        }
                        if (client.player != null) {
                            client.player.sendMessage(Text.translatable("ottoextra.horses.message.saved"), true);
                        }
                        close();
                    });
                });
    }

    private void assignFarm(String note) {
        saving = true;
        errorStatus = false;
        status = Text.translatable("ottoextra.horses.farms.assigning").getString();
        updateButtons();
        AnimalBreedingFarm farm = farmDraft;
        HorseRegistry.assignFarm(entityUuid, farm.nameKey(), currentLocation())
                .thenCompose(horse -> HorseRegistry.refreshDatabaseRecords().handle((records, failure) -> horse))
                .whenComplete((horse, failure) -> {
                    if (client == null) return;
                    client.execute(() -> {
                        saving = false;
                        if (failure == null) {
                            saveLocal(note, horse.currentFarm());
                            if (client.player != null) {
                                client.player.sendMessage(Text.translatable("ottoextra.horses.farms.assigned",
                                        horse.currentFarm()), true);
                            }
                            if (client.currentScreen == this) close();
                            return;
                        }
                        status = BasicHorseOverviewScreen.farmError(failure, false);
                        errorStatus = true;
                        if (client.currentScreen != this) return;
                        // A conflicting write requires a fresh record before trying again.
                        busy = true;
                        HorseRegistry.refreshDatabaseRecords().whenComplete((ignored, refreshFailure) ->
                                client.execute(() -> {
                                    busy = false;
                                    if (client.currentScreen == this) updateButtons();
                                }));
                        updateButtons();
                    });
                });
    }

    // ---- geometry ------------------------------------------------------------------

    private int noteY() { return spread.right().y() + spread.right().h() - 46; }
    private int newFarmY() { return spread.right().y() + spread.right().h() - 100; }
    private int pickerY() { return spread.right().y() + spread.right().h() - 66; }
    private int pickTop() { return spread.right().y() + 26; }
    private int pickBottom() { return newFarmY() - 6; }
    private int farmLineY() { return spread.right().y() + 63; }

    /** Rows of the farm picker: the farms, then "Kein Hof" when the animal has none yet. */
    private int pickRows() {
        return pickFarms().size() + (currentFarmKey().isEmpty() ? 1 : 0);
    }

    private int pickVisible() {
        return Math.max(1, (pickBottom() - pickTop()) / PICK_ROW);
    }

    private int pickAt(double mouseX, double mouseY) {
        AnimalBook.Page right = spread.right();
        if (mouseX < right.x() + 6 || mouseX >= right.x() + right.w() - 6 || mouseY < pickTop()
                || mouseY >= pickTop() + pickVisible() * PICK_ROW) return -1;
        int index = pickScroll + (int) ((mouseY - pickTop()) / PICK_ROW);
        return index < pickRows() ? index : -1;
    }

    // ---- input ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            AnimalBook.Page right = spread.right();
            if (view == View.ENTRY) {
                String other = Text.translatable("ottoextra.horses.book.other_farm").getString();
                int otherW = AnimalBook.smallWidth(textRenderer, other);
                int edge = right.x() + right.w() - 10;
                if (canChangeFarm() && click.y() >= farmLineY() - 2 && click.y() < farmLineY() + 12
                        && click.x() >= edge - otherW - 4 && click.x() < edge + 2) {
                    openPicker();
                    return true;
                }
            } else {
                int row = pickAt(click.x(), click.y());
                if (row >= 0) {
                    List<AnimalBreedingFarm> farms = pickFarms();
                    if (row < farms.size()) {
                        if (farms.get(row).canEdit()) pickKey = farms.get(row).nameKey();
                    } else {
                        pickKey = "";
                    }
                    return true;
                }
                String picked = AnimalBook.SymbolPicker.pick(right.x() + 10, pickerY(), click.x(), click.y(),
                        newFarmSymbol);
                if (picked != null) {
                    newFarmSymbol = picked;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (view == View.FARM_PICK && mouseY >= pickTop() && mouseY < pickBottom()) {
            double amount = vertical != 0 ? vertical : horizontal;
            pickScroll = (int) Math.max(0, Math.min(Math.max(0, pickRows() - pickVisible()),
                    pickScroll - (long) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    // ---- drawing -------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AnimalBook.drawSpread(context, width, height, spread);
        drawLeft(context, mouseX, mouseY);
        if (view == View.ENTRY) drawEntry(context, mouseX, mouseY);
        else drawPicker(context, mouseX, mouseY);
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
        int x = left.x() + 10;
        int w = left.w() - 20;
        AnimalBook.header(context, textRenderer, left, 10, Text.translatable("ottoextra.horses.book.register"),
                tr("ottoextra.horses.book.register_caption"), w);
        int fx0 = left.x() + 22;
        int fx1 = left.x() + left.w() - 22;
        int fy0 = left.y() + 28;
        int fy1 = left.y() + 120;
        AnimalBook.rect(context, fx0, fy0, fx1, fy1, PA_D);
        AnimalBook.rect(context, fx0 + 1, fy0 + 1, fx1 - 1, fy1 - 1, PA_S);
        drawModel(context, fx0 + 2, fy0 + 2, fx1 - 2, fy1 - 2, mouseX, mouseY);

        String species = species();
        text(context, clip(species.isBlank() ? "–" : species, w), x, left.y() + 128, INK);
        String gender = gender();
        int iconW = AnimalBook.genderIcon(context, gender, x, left.y() + 141);
        String term = AnimalToastModel.genderTerm(gender);
        if (term.isEmpty()) {
            term = switch (AnimalGender.parse(gender)) {
                case FEMALE -> tr("ottoextra.horses.book.female");
                case MALE -> tr("ottoextra.horses.book.male");
                case UNKNOWN -> gender.isBlank() ? "–" : gender;
            };
        }
        text(context, clip(term, w - iconW - 3), x + (iconW > 0 ? iconW + 3 : 0), left.y() + 141,
                AnimalBook.genderInk(gender));
        small(context, "UUID", x, left.y() + 156, INK_F);
        String[] uuidLines = splitUuid(entityUuid.toString(), w);
        small(context, uuidLines[0], x, left.y() + 164, INK_F);
        if (!uuidLines[1].isEmpty()) small(context, uuidLines[1], x, left.y() + 172, INK_F);
    }

    /** UUID in at most two lines, broken after a hyphen where possible. */
    private String[] splitUuid(String uuid, int width) {
        if (AnimalBook.smallWidth(textRenderer, uuid) <= width) return new String[]{uuid, ""};
        int cut = uuid.length();
        while (cut > 1 && AnimalBook.smallWidth(textRenderer, uuid.substring(0, cut)) > width) cut--;
        int hyphen = uuid.lastIndexOf('-', cut - 1);
        if (hyphen > 0) cut = hyphen + 1;
        return new String[]{uuid.substring(0, cut), AnimalBook.clipSmall(textRenderer, uuid.substring(cut), width)};
    }

    private void drawModel(DrawContext context, int left, int top, int right, int bottom, int mouseX, int mouseY) {
        AnimalEntity loaded = loadedAnimal();
        if (loaded != null) {
            try {
                // Follows the mouse; no name tag, the entity itself is not changed.
                de.ottoextra.animalinfo.AnimalPreview.drawTurning(context, left, top, right, bottom, loaded,
                        mouseX, mouseY);
            } catch (Throwable ignored) {
                // Another mod's renderer must not break the book.
            }
            return;
        }
        AnimalBreedingHorse record = record();
        AnimalEntity preview = record == null ? null : AnimalBook.preview(record, fallbackAnimals);
        if (preview != null) {
            AnimalBook.model(context, preview, left, top, right, bottom - 8, 24);
        }
        small(context, tr("ottoextra.horses.book.model"), left + 2, bottom - 8, INK_F);
    }

    private void drawEntry(DrawContext context, int mouseX, int mouseY) {
        AnimalBook.Page right = spread.right();
        int x = right.x() + 10;
        int w = right.w() - 20;
        int edge = right.x() + right.w() - 10;
        AnimalBook.header(context, textRenderer, right, 10, Text.translatable("ottoextra.horses.book.entry"),
                null, w);
        small(context, tr("ottoextra.horses.book.name"), x, right.y() + 26, INK_M);
        if (nameField == null) {
            text(context, clip(HorseRegistry.name(entityUuid).orElse("–"), w), x, right.y() + 35, INK);
        }

        small(context, tr("ottoextra.horses.book.farm"), x, right.y() + 54, INK_M);
        String farm = currentFarm();
        int fy = farmLineY();
        int textX = x;
        if (!farm.isBlank()) {
            AnimalBook.symbol(context, AnimalBook.farmSymbol(farm), x, fy - 1, 12);
            textX += 15;
        }
        String other = tr("ottoextra.horses.book.other_farm");
        int otherW = AnimalBook.smallWidth(textRenderer, other);
        text(context, clip(farm.isBlank() ? tr("ottoextra.horses.book.no_farm") : farm, edge - otherW - 8 - textX),
                textX, fy + 1, farm.isBlank() ? INK_F : INK);
        if (canChangeFarm()) {
            boolean hover = mouseY >= fy - 2 && mouseY < fy + 12 && mouseX >= edge - otherW - 4 && mouseX < edge + 2;
            small(context, other, edge - otherW, fy + 3, hover ? INK : BRASS);
        }
        AnimalBook.rule(context, x, right.x() + right.w() - 8, fy + 16, PA_S);

        long now = System.currentTimeMillis();
        AnimalBookStatus.View state = status(now);
        int gy = fy + 20;
        small(context, tr("ottoextra.horses.book.values"), x, gy, INK_M);
        gy += 9;
        List<Value> values = values(state);
        int colW = w / 2;
        for (int index = 0; index < values.size() && index < 6; index++) {
            Value each = values.get(index);
            int sx = x + (index % 2) * colW;
            int sy = gy + (index / 2) * 17;
            small(context, each.label(), sx, sy, INK_F);
            text(context, clip(each.value(), colW - 6), sx, sy + 7, each.color());
        }

        int my = noteY() - 32;
        small(context, tr("ottoextra.horses.book.mast"), x, my, INK_M);
        if (state.ageMs() != null) {
            String age = state.ageMs() < 10_000L ? tr("ottoextra.animalinfo.status.age_now")
                    : tr("ottoextra.animalinfo.status.age", AnimalBookStatus.dataAge(state.ageMs()));
            AnimalBook.smallRight(context, textRenderer, age, edge, my, INK_F);
        }
        AnimalBook.mastBar(context, x, my + 9, edge, 4, state);
        String caption;
        if (state.hasMast()) {
            caption = tr("ottoextra.horses.book.mast_of", state.mast(), state.mastScale(), state.mastNeed());
        } else if (state.species() != null) {
            caption = tr("ottoextra.horses.book.mast_table", state.species().breedMast(),
                    AnimalBookStatus.tableWait(state.species()));
        } else {
            caption = tr("ottoextra.horses.book.mast_none");
        }
        small(context, AnimalBook.clipSmall(textRenderer, caption, w), x, my + 17, INK_F);

        small(context, tr("ottoextra.horses.book.note"), x, noteY(), INK_M);
        if (!status.isBlank()) {
            AnimalBook.smallWrapped(context, textRenderer, Text.literal(status), x, right.y() + right.h() - 20,
                    w - 130, 8, errorStatus ? TERRA : INK_M, 2);
        }
    }

    /** Values grid: horse values, breeding status, age, life and shearing or milking when known. */
    private List<Value> values(AnimalBookStatus.View state) {
        List<Value> values = new ArrayList<>();
        AnimalInfoSnapshot value = snapshot();
        AnimalBreedingHorse record = record();
        String speed = value != null && !value.speed().isBlank() ? value.speed() : record == null ? "" : record.speed();
        String jump = value != null && !value.jumpHeight().isBlank() ? value.jumpHeight()
                : record == null ? "" : record.jumpHeight();
        String life = value != null && !value.life().isBlank() ? value.life() : record == null ? "" : record.life();
        if (!speed.isBlank() || !jump.isBlank()) {
            values.add(new Value(tr("ottoextra.horses.overview.speed"), dash(speed), INK));
            values.add(new Value(tr("ottoextra.horses.overview.jump"), dash(jump), INK));
        }
        values.add(new Value(tr("ottoextra.horses.book.breeding"),
                tr(AnimalBookStatus.statusKey(state.status()), AnimalBookStatus.statusArgs(state.status())),
                AnimalBook.toneInk(state.tone())));
        values.add(new Value(tr("ottoextra.horses.book.age"),
                tr(AnimalBookStatus.ageKey(state.age()), AnimalBookStatus.ageArgs(state.age())),
                state.age().kind() == AnimalBookStatus.AgeKind.YOUNG ? AnimalBook.YOUNG_INK : INK));
        if (!life.isBlank()) values.add(new Value(tr("ottoextra.horses.book.life"), life, INK));
        if (value != null && value.hasShearable()) {
            values.add(new Value(tr("ottoextra.horses.record.shearable_label"), extra(value.shearable()),
                    AnimalBookStatus.positive(value.shearable()) ? AnimalBook.OK_INK : AnimalBook.WARN_INK));
        }
        if (value != null && value.hasMilkable()) {
            values.add(new Value(tr("ottoextra.horses.record.milkable_label"), extra(value.milkable()),
                    AnimalBookStatus.positive(value.milkable()) ? AnimalBook.OK_INK : AnimalBook.WARN_INK));
        }
        return values;
    }

    /** Shearing or milking value; a waiting time counts down from the observation. */
    private String extra(String raw) {
        long duration = AnimalBookStatus.duration(raw);
        if (duration > 0 && observation != null) {
            long left = observation.observedAt().toEpochMilli() + duration - System.currentTimeMillis();
            return left > 0 ? tr("ottoextra.horses.book.in", AnimalBookStatus.timer(left, false))
                    : tr("ottoextra.horses.book.now");
        }
        return raw;
    }

    private void drawPicker(DrawContext context, int mouseX, int mouseY) {
        AnimalBook.Page right = spread.right();
        int px = right.x();
        int pw = right.w();
        int x = px + 10;
        int w = pw - 20;
        String name = nameField != null ? nameField.getText() : nameDraft != null ? nameDraft
                : HorseRegistry.name(entityUuid).orElse("");
        AnimalBook.header(context, textRenderer, right, 10, Text.translatable("ottoextra.horses.book.pick_farm"),
                name.isBlank() ? null : tr("ottoextra.horses.book.pick_for", name), w);
        List<AnimalBreedingFarm> farms = pickFarms();
        int hovered = pickAt(mouseX, mouseY);
        int visible = Math.min(pickVisible(), pickRows() - pickScroll);
        for (int index = 0; index < visible; index++) {
            int row = pickScroll + index;
            int cy = pickTop() + index * PICK_ROW;
            boolean none = row >= farms.size();
            AnimalBreedingFarm farm = none ? null : farms.get(row);
            boolean selected = none ? pickKey == null || pickKey.isEmpty() : farm.nameKey().equals(pickKey);
            if (selected || hovered == row) AnimalBook.rect(context, px + 6, cy - 2, px + pw - 6, cy + 17, PA_L);
            if (selected) AnimalBook.rect(context, px + 6, cy - 2, px + 8, cy + 17, TERRA);
            if (none) {
                text(context, tr("ottoextra.horses.book.no_farm"), px + 26, cy + 3, INK_M);
            } else {
                AnimalBook.symbol(context, AnimalBookStore.current().symbol(farm.nameKey()), px + 10, cy, 16);
                String count = farm.canEdit() ? tr("ottoextra.horses.farms.count", farm.horseCount())
                        : tr("ottoextra.horses.farms.read_only");
                int countW = AnimalBook.smallWidth(textRenderer, count);
                AnimalBook.smallRight(context, textRenderer, count, px + pw - 12, cy + 5, INK_M);
                text(context, clip(farm.name(), pw - 12 - countW - 6 - (px + 30) + px), px + 30, cy + 3,
                        farm.canEdit() ? INK : INK_F);
            }
            AnimalBook.rule(context, x, px + pw - 8, cy + 18, PA_S);
        }
        if (farms.isEmpty()) {
            small(context, tr(HorseRegistry.farms().isEmpty() ? "ottoextra.horses.farms.empty"
                    : "ottoextra.horses.overview.loading"), x, pickTop() + PICK_ROW + 4, INK_F);
        }
        small(context, tr("ottoextra.horses.book.new_farm_title"), x, newFarmY(), INK_M);
        AnimalBook.SymbolPicker.draw(context, textRenderer, x, pickerY(), newFarmSymbol);
        if (!status.isBlank()) {
            AnimalBook.smallWrapped(context, textRenderer, Text.literal(status), x, right.y() + right.h() - 20,
                    w - 70, 8, errorStatus ? TERRA : INK_M, 2);
        }
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "–" : value;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof ApiProblem.ApiException api) {
            if (api.problem().isHttpStatus(403)) {
                return Text.translatable("ottoextra.horses.message.forbidden").getString();
            }
            if (api.problem().isHttpStatus(503)) {
                return Text.translatable("ottoextra.horses.message.unavailable").getString();
            }
        }
        return Text.translatable("ottoextra.horses.message.failed").getString();
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
