package de.ottoextra.horses.ui;

import de.ottoextra.OttoExtra;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.horses.HorseFinder;
import de.ottoextra.horses.HorseRegistry;
import de.ottoextra.horses.RegistryAnimals;
import de.ottoextra.horses.ui.HorseOverviewView.SortOrder;
import de.ottoextra.horses.ui.HorseOverviewView.SortSelection;
import de.ottoextra.horses.ui.HorseOverviewView.AnimalKind;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/** Compact protected horse database available without the AnimalBreeding addon. */
public final class BasicHorseOverviewScreen extends Screen {

    private static final int PANEL = 0xF018202A;
    private static final int PANEL_INNER = 0xE0202B35;
    private static final int ROW = 0xD025313B;
    private static final int ROW_HOVER = 0xE035424D;
    private static final int BORDER = 0xFF6D5A41;
    private static final int TITLE = 0xFFF4EDE5;
    private static final int GOLD = 0xFFDAA84A;
    private static final int BODY = 0xFFD8D0C7;
    private static final int MUTED = 0xFFAA9D90;
    private static final int ERROR = 0xFFFF8E7A;
    private static final int HEADER_HEIGHT = 20;
    private static final int HORSE_HEIGHT = 44;
    private static final Identifier FINDER_ICON = icon("finder");
    private static final Identifier DELETE_ICON = icon("delete");
    private static final Identifier SPEED_ICON = icon("speed");
    private static final Identifier JUMP_ICON = icon("jump");
    private static final Identifier EXPAND_ICON = icon("expand");
    private static final Identifier COLLAPSE_ICON = icon("collapse");
    private static final Identifier SORT_ASC_ICON = icon("sort_ascending");
    private static final Identifier SORT_DESC_ICON = icon("sort_descending");
    private static final Identifier SORT_NONE_ICON = icon("sort_none");

    private final Screen parent;
    private final List<RowEntry> rows = new ArrayList<>();
    private final Set<UUID> deleting = new HashSet<>();
    private final Set<String> collapsedFarms = new HashSet<>();
    private List<AnimalBreedingHorse> visibleHorses = List.of();
    private TextFieldWidget searchField;
    private ButtonWidget refreshButton;
    private ButtonWidget sortButton;
    private ButtonWidget filterButton;
    private ButtonWidget addButton;
    private AnimalEntity entryTarget;
    private final Map<AnimalKind, AnimalEntity> fallbackAnimals = new EnumMap<>(AnimalKind.class);
    private AnimalKind filter = AnimalKind.ALL;
    private final SortSelection sorting = new SortSelection();
    private boolean sortMenuOpen;
    private boolean refreshing;
    private boolean loadedOnce;
    private int scroll;
    private int contentHeight;
    private String query = "";
    private String status = "";

    public BasicHorseOverviewScreen(Screen parent) {
        super(Text.translatable("ottoextra.horses.overview.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int px = panelX();
        int py = panelY();
        rebuildRows();

        searchField = new TextFieldWidget(textRenderer, px + 12, py + 29,
                panelW() - 130, 18,
                Text.translatable("ottoextra.horses.overview.search"));
        searchField.setPlaceholder(Text.translatable(
                "ottoextra.horses.overview.search"));
        searchField.setMaxLength(64);
        searchField.setText(query);
        searchField.setChangedListener(value -> {
            query = value;
            scroll = 0;
            rebuildRows();
        });
        addDrawableChild(searchField);
        filterButton = addDrawableChild(ButtonWidget.builder(filterButtonText(), button -> {
            var choices = HorseOverviewView.filters(OttoExtraConfig.active().animalInfo);
            filter = choices.get((choices.indexOf(filter) + 1) % choices.size());
            scroll = 0;
            rebuildRows();
            button.setMessage(filterButtonText());
        }).dimensions(px + panelW() - 112, py + 29, 100, 18).build());

        refreshButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.horses.overview.refresh"),
                        button -> refreshRecords())
                .dimensions(px + 12, py + panelH() - 27, 76, 18)
                .build());
        if (entryTarget == null && client != null && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof AnimalEntity animal && RegistryAnimals.supports(animal)) {
            entryTarget = animal;
        }
        addButton = addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.horses.overview.add"), button -> {
            if (entryTarget != null && client != null) {
                HorseRegistry.observation(entryTarget.getUuid()).ifPresent(observation ->
                        client.setScreen(new BasicHorseRecordScreen(this, entryTarget, observation)));
            }
        }).dimensions(px + 94, py + panelH() - 27, 106, 18).build());
        addButton.setTooltip(Tooltip.of(Text.translatable("ottoextra.horses.overview.add.tip")));
        sortMenuOpen = false;
        sortButton = addDrawableChild(ButtonWidget.builder(sortButtonText(),
                        button -> sortMenuOpen = !sortMenuOpen)
                .dimensions(sortX(), sortY(), sortWidth(), 18)
                .build());
        sortButton.setTooltip(Tooltip.of(Text.translatable("ottoextra.horses.overview.sort.combined.tip")));
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.horses.farms.open"),
                        button -> client.setScreen(new HorseFarmScreen(this)))
                .dimensions(px + panelW() - 92, py + 50, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),
                        button -> close())
                .dimensions(px + panelW() - 72, py + panelH() - 27, 60, 18)
                .build());

        if (!loadedOnce) {
            refreshRecords();
        } else {
            updateButtons();
        }
    }

    private void refreshRecords() {
        if (refreshing) {
            return;
        }
        loadedOnce = true;
        refreshing = true;
        status = Text.translatable("ottoextra.horses.overview.loading").getString();
        rebuildRows();
        scroll = 0;
        updateButtons();
        HorseRegistry.refreshDatabaseRecords().whenComplete((records, error) -> {
            if (client == null) {
                return;
            }
            client.execute(() -> {
                refreshing = false;
                status = Text.translatable(error == null
                        ? "ottoextra.horses.overview.current"
                        : "ottoextra.horses.overview.failed").getString();
                if (client.currentScreen != this) {
                    return;
                }
                rebuildRows();
                scroll = Math.min(scroll, maxScroll());
                updateButtons();
            });
        });
    }

    private void rebuildRows() {
        rows.clear();
        var config = OttoExtraConfig.active().animalInfo;
        if (!HorseOverviewView.filters(config).contains(filter)) filter = AnimalKind.ALL;
        if (filterButton != null) filterButton.setMessage(filterButtonText());
        List<HorseOverviewView.FarmGroup> farms = HorseOverviewView.groups(
                HorseOverviewView.filter(HorseRegistry.databaseRecords(), filter, config), query, sorting);
        visibleHorses = farms.stream()
                .flatMap(group -> group.horses().stream())
                .toList();
        for (HorseOverviewView.FarmGroup group : farms) {
            rows.add(RowEntry.header(group.farm(), group.horses().size()));
            if (!collapsedFarms.contains(group.farm())) {
                for (AnimalBreedingHorse horse : group.horses()) {
                    rows.add(RowEntry.horse(horse));
                }
            }
        }
        contentHeight = rows.stream().mapToInt(RowEntry::height).sum();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private void selectSort(SortOrder selected) {
        sorting.select(selected);
        updateSorting();
    }

    private void updateSorting() {
        scroll = 0;
        rebuildRows();
        if (sortButton != null) {
            sortButton.setMessage(sortButtonText());
        }
    }

    private Text sortButtonText() {
        var criteria = sorting.criteria();
        var description = criteria.size() == 2 ? Text.literal("Ø ") : Text.empty();
        if (criteria.isEmpty()) description.append(Text.translatable(sorting.farmOrder().translationKey()));
        for (int i = 0; i < criteria.size(); i++) {
            SortOrder order = criteria.get(i);
            if (i > 0) description.append(" · ");
            description.append(Text.translatable(order.isSpeed()
                    ? "ottoextra.horses.overview.speed" : "ottoextra.horses.overview.jump"))
                    .append(order.ascending() ? " ↓" : " ↑");
        }
        return Text.translatable("ottoextra.horses.overview.sort.label",
                description);
    }

    private void updateButtons() {
        if (refreshButton != null) {
            refreshButton.active = !refreshing;
        }
        if (addButton != null) {
            addButton.active = client != null && entryTarget != null && entryTarget.isAlive()
                    && entryTarget.getEntityWorld() == client.world
                    && HorseRegistry.observation(entryTarget.getUuid()).isPresent();
        }
    }

    private Text filterButtonText() { return Text.translatable(filter.translationKey()); }

    @Override
    public void tick() { updateButtons(); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontal, double vertical) {
        if (sortMenuOpen) return true;
        if (mouseX >= contentX() && mouseX <= contentRight()
                && mouseY >= contentTop() && mouseY <= contentBottom()) {
            scroll = Math.max(0, Math.min(maxScroll(),
                    scroll - (int) Math.signum(vertical) * 30));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && sortMenuOpen) {
            SortOrder selected = sortOptionAt(click.x(), click.y());
            if (selected != null) {
                selectSort(selected);
                return true;
            }
            if (inside(click.x(), click.y(), sortX(), sortResetY(), sortWidth(), 18)) {
                sorting.reset();
                updateSorting();
                return true;
            }
            if (!inside(click.x(), click.y(), sortX(), sortY(), sortWidth(), 18)) {
                sortMenuOpen = false;
                return true;
            }
        }
        if (click.button() == 0 && click.x() >= contentX()
                && click.x() <= contentRight() && click.y() >= contentTop()
                && click.y() <= contentBottom()) {
            int y = contentTop() - scroll;
            for (RowEntry row : rows) {
                if (row.horse() == null && click.y() >= y && click.y() < y + row.height()) {
                    if (click.x() >= speedX() && click.x() < jumpX()) {
                        sorting.cycle(true);
                        updateSorting();
                    } else if (click.x() >= jumpX() && click.x() < finderX() - 4) {
                        sorting.cycle(false);
                        updateSorting();
                    } else {
                        if (!collapsedFarms.add(row.farm())) collapsedFarms.remove(row.farm());
                        rebuildRows();
                    }
                    return true;
                }
                if (row.horse() != null && click.y() >= y
                        && click.y() < y + row.height()) {
                    if (click.x() >= finderX() && click.x() < finderX() + 18
                            && click.y() >= y + 4 && click.y() < y + 22) {
                        AnimalBreedingHorse horse = row.horse();
                        HorseFinder.toggle(horse.uuid());
                        status = HorseFinder.isTarget(horse.uuid())
                                ? Text.translatable("ottoextra.horses.finder.active",
                                        displayName(horse)).getString()
                                : Text.translatable(
                                        "ottoextra.horses.finder.stopped").getString();
                        return true;
                    }
                    if (click.x() >= deleteX() && click.x() < deleteX() + 18
                            && click.y() >= y + 4 && click.y() < y + 22) {
                        AnimalBreedingHorse horse = row.horse();
                        if (horse.canManage() && !deleting.contains(horse.uuid())) {
                            confirmDelete(horse);
                        }
                        return true;
                    }
                }
                y += row.height();
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void confirmDelete(AnimalBreedingHorse horse) {
        if (client == null) {
            return;
        }
        String name = displayName(horse) + (horse.registryName().isBlank() ? " (" + horse.entityUuid() + ")" : "");
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (client == null) {
                return;
            }
            client.setScreen(this);
            if (confirmed) {
                deleteRecord(horse, name);
            }
        }, Text.translatable("ottoextra.horses.overview.delete.title"),
                Text.translatable("ottoextra.horses.overview.delete.confirm", name),
                Text.translatable("ottoextra.horses.overview.delete.yes"),
                Text.translatable("gui.cancel")));
    }

    private void deleteRecord(AnimalBreedingHorse horse, String name) {
        UUID uuid = horse.uuid();
        deleting.add(uuid);
        status = Text.translatable(
                "ottoextra.horses.overview.delete.running").getString();
        HorseRegistry.deleteDatabaseRecord(uuid).whenComplete((ignored, error) -> {
            if (client == null) {
                return;
            }
            client.execute(() -> {
                deleting.remove(uuid);
                if (error == null) {
                    status = Text.translatable(
                            "ottoextra.horses.overview.delete.success", name).getString();
                    rebuildRows();
                    scroll = Math.min(scroll, maxScroll());
                    if (client.player != null) {
                        client.player.sendMessage(Text.translatable(
                                "ottoextra.horses.overview.delete.success", name), true);
                    }
                    return;
                }
                Throwable cause = unwrap(error);
                OttoExtra.LOGGER.warn("Pferdeakte {} konnte nicht geloescht werden: {}",
                        uuid, cause.toString());
                status = Text.translatable(
                        "ottoextra.horses.overview.delete.failed", name).getString();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(
                            "ottoextra.horses.overview.delete.failed", name), true);
                }
            });
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB0000000);
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        context.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        context.fill(px, py, px + pw, py + ph, PANEL);
        context.drawTextWithShadow(textRenderer, title, px + 12, py + 10, TITLE);

        long farmCount = visibleHorses.stream()
                .map(AnimalBreedingHorse::currentFarm)
                .filter(farm -> !farm.isBlank())
                .map(farm -> farm.toLowerCase(Locale.ROOT))
                .distinct().count();
        Text summary = Text.translatable("ottoextra.horses.overview.summary",
                farmCount, rows.stream().filter(row -> row.horse() != null).count());
        context.drawTextWithShadow(textRenderer, summary,
                px + pw - 12 - textRenderer.getWidth(summary), py + 10, GOLD);
        int statusX = sortX() + sortWidth() + 8;
        int statusWidth = px + pw - 12 - statusX;
        if (!status.isBlank() && statusWidth > 20) {
            context.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(status, statusWidth),
                    statusX, py + 55,
                    status.equals(Text.translatable(
                            "ottoextra.horses.overview.failed").getString())
                            ? ERROR : MUTED);
        }

        context.fill(contentX() - 3, contentTop() - 3,
                contentRight() + 3, contentBottom() + 3, PANEL_INNER);
        context.enableScissor(contentX(), contentTop(), contentRight(),
                contentBottom());
        renderRows(context, mouseX, mouseY);
        context.disableScissor();
        renderScrollbar(context);

        if (visibleHorses.isEmpty() && !refreshing) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.horses.overview.empty"),
                    width / 2, contentTop() + 16, MUTED);
        }
        super.render(context, mouseX, mouseY, delta);
        renderSortMenu(context, mouseX, mouseY);
        if (sortMenuOpen) return;
        if (inside(mouseX, mouseY, sortX(), sortY(), sortWidth(), 18)) {
            context.drawTooltip(textRenderer, Text.translatable("ottoextra.horses.overview.sort.help"), mouseX, mouseY);
        }

        AnimalBreedingHorse deleteHorse = horseAtButton(mouseX, mouseY, deleteX());
        if (deleteHorse != null) {
            String key = deleting.contains(deleteHorse.uuid())
                    ? "ottoextra.horses.overview.delete.running"
                    : (deleteHorse.canManage()
                        ? "ottoextra.horses.overview.delete.tooltip"
                        : "ottoextra.horses.overview.delete.forbidden");
            context.drawTooltip(textRenderer, Text.translatable(key), mouseX, mouseY);
            return;
        }
        AnimalBreedingHorse finderHorse = horseAtButton(mouseX, mouseY, finderX());
        if (finderHorse != null) {
            context.drawTooltip(textRenderer, Text.translatable(
                    HorseFinder.isTarget(finderHorse.uuid())
                            ? "ottoextra.horses.finder.stop"
                            : "ottoextra.horses.finder.start"), mouseX, mouseY);
        }
    }

    private void renderRows(DrawContext context, int mouseX, int mouseY) {
        int y = contentTop() - scroll;
        for (RowEntry row : rows) {
            if (y + row.height() >= contentTop() && y <= contentBottom()) {
                if (row.horse() == null) {
                    renderHeader(context, row, y, mouseX, mouseY);
                } else {
                    boolean hovered = mouseX >= contentX()
                            && mouseX <= contentRight()
                            && mouseY >= y && mouseY < y + row.height();
                    renderHorse(context, row.horse(), y, hovered, mouseX, mouseY);
                }
            }
            y += row.height();
        }
    }

    private void renderHeader(DrawContext context, RowEntry row, int y, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, contentX(), y, contentRight() - contentX(), HEADER_HEIGHT)
                && mouseY >= contentTop() && mouseY < contentBottom() && !sortMenuOpen;
        context.fill(contentX(), y + 2, contentRight(), y + HEADER_HEIGHT,
                hovered ? 0xE0483828 : 0xC0322921);
        boolean collapsed = collapsedFarms.contains(row.farm());
        HorseUiIcons.draw(context, collapsed ? EXPAND_ICON : COLLAPSE_ICON,
                contentX() + 5, y + 7, 8, 16, GOLD);
        String farm = row.farm().isBlank()
                ? Text.translatable("ottoextra.horses.overview.no_farm").getString() : row.farm();
        drawSmallText(context, farm, contentX() + 19, y + 8, GOLD, speedX() - contentX() - 24);
        renderMetricHeader(context, SPEED_ICON, speedX(), jumpX() - speedX() - 4,
                y, "ottoextra.horses.overview.speed", sorting.metric(true));
        renderMetricHeader(context, JUMP_ICON, jumpX(), finderX() - jumpX() - 8,
                y, "ottoextra.horses.overview.jump", sorting.metric(false));
        String count = Text.translatable(
                "ottoextra.horses.overview.horse_count", row.count()).getString();
        drawSmallText(context, count,
                contentRight() - 3 - Math.min(43, (int) Math.ceil(textRenderer.getWidth(count) * 0.75)),
                y + 9, MUTED, 43);
        if (hovered) {
            Text tooltip = mouseX >= speedX() && mouseX < jumpX()
                    ? metricTooltip(true)
                    : mouseX >= jumpX() && mouseX < finderX() - 4
                    ? metricTooltip(false)
                    : Text.translatable(collapsed ? "ottoextra.horses.overview.expand"
                            : "ottoextra.horses.overview.collapse", farm);
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private void renderMetricHeader(DrawContext context, Identifier icon, int x, int w,
                                    int y, String label, SortOrder order) {
        boolean active = order != null;
        context.fill(x, y + 3, x + w, y + HEADER_HEIGHT - 1, active ? 0xB080632E : 0x663C3932);
        HorseUiIcons.draw(context, icon, x + 3, y + 7, 8, 16, active ? GOLD : MUTED);
        if (w >= 48) {
            String text = Text.translatable(label).getString();
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(
                    text, w - 32), x + 17, y + 7, BODY);
        }
        Identifier direction = !active ? SORT_NONE_ICON : order.ascending() ? SORT_DESC_ICON : SORT_ASC_ICON;
        HorseUiIcons.draw(context, direction, x + w - 11, y + 7, 8, 16, active ? GOLD : MUTED);
    }

    private Text metricTooltip(boolean speed) {
        return Text.translatable("ottoextra.horses.overview.sort.cycle", Text.translatable(speed
                ? "ottoextra.horses.overview.speed" : "ottoextra.horses.overview.jump"))
                .append("\n").append(Text.translatable("ottoextra.horses.overview.sort.combined.tip"));
    }

    private void renderHorse(DrawContext context, AnimalBreedingHorse horse,
                             int y, boolean hovered, int mouseX, int mouseY) {
        context.fill(contentX(), y + 2, contentRight(), y + HORSE_HEIGHT - 2,
                hovered ? ROW_HOVER : ROW);
        renderHorseModel(context, horse, y);

        int x = contentX() + 43;
        int available = Math.max(20, finderX() - x - 6);
        String name = textRenderer.trimToWidth(displayName(horse), available * 2 / 3);
        context.drawTextWithShadow(textRenderer, name, x, y + 6, TITLE);
        int originX = x + textRenderer.getWidth(name) + 8;
        drawSmallText(context, "· " + value(horse.origin()), originX, y + 7, MUTED,
                Math.max(1, available - (originX - x)));
        if (horse.registryName().isBlank()) {
            HorseUiIcons.smallText(context, textRenderer, horse.entityUuid(), x, y + 17,
                    MUTED, available, 0.65F);
        }

        boolean finderActive = HorseFinder.isTarget(horse.uuid());
        context.fill(finderX(), y + 4, finderX() + 18, y + 22,
                finderActive ? 0xFF80632E
                        : buttonColor(mouseX, mouseY, finderX(), y));
        drawIcon(context, FINDER_ICON, finderX() + 5, y + 9,
                finderActive ? GOLD : MUTED);

        boolean canDelete = horse.canManage();
        int deleteColor = canDelete ? ERROR : 0xFF655E5A;
        context.fill(deleteX(), y + 4, deleteX() + 18, y + 22,
                canDelete && !deleting.contains(horse.uuid())
                        ? buttonColor(mouseX, mouseY, deleteX(), y)
                        : 0xFF282729);
        drawIcon(context, DELETE_ICON, deleteX() + 5, y + 9, deleteColor);

        HorseUiIcons.gender(context, textRenderer, horse.gender(), x, y + 30, speedX() - x, true);
        int speedX = speedX();
        int speedWidth = jumpX() - speedX;
        if (horse.speed().isBlank() && horse.jumpHeight().isBlank()) {
            context.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(horse.animalType(),
                            Math.max(18, finderX() - speedX - 6)),
                    speedX, y + 30, BODY);
            return;
        }
        drawIcon(context, SPEED_ICON, speedX, y + 30, MUTED);
        context.drawTextWithShadow(textRenderer,
                textRenderer.trimToWidth(value(horse.speed()),
                        Math.max(18, speedWidth - 13)),
                speedX + 12, y + 30, BODY);
        int jumpX = speedX + speedWidth;
        drawIcon(context, JUMP_ICON, jumpX, y + 30, MUTED);
        context.drawTextWithShadow(textRenderer,
                textRenderer.trimToWidth(value(horse.jumpHeight()),
                        Math.max(18, finderX() - jumpX - 15)),
                jumpX + 12, y + 30, BODY);
    }

    private void renderHorseModel(DrawContext context,
                                  AnimalBreedingHorse horse, int y) {
        AnimalEntity preview = previewHorse(horse);
        if (preview == null) {
            return;
        }
        int left = contentX() + 3;
        int top = y + 3;
        int right = left + 36;
        int bottom = y + HORSE_HEIGHT - 3;
        context.fill(left, top, right, bottom, 0x55202830);
        try {
            HorseModelPreview.draw(context, left, top, right, bottom, 11,
                    0.25f, HorseModelPreview.DEFAULT_YAW_DEGREES, preview);
        } catch (Throwable ignored) {
            // Keep the database rows usable if another mod replaces the renderer.
        }
    }

    private AnimalEntity previewHorse(AnimalBreedingHorse horse) {
        if (client == null || client.world == null) {
            return null;
        }
        Entity loaded = client.world.getEntity(horse.uuid());
        if (loaded instanceof AnimalEntity animal && RegistryAnimals.supports(animal)) {
            return animal;
        }
        AnimalKind kind = HorseOverviewView.kind(horse);
        AnimalEntity preview = fallbackAnimals.get(kind);
        if (preview == null || preview.getEntityWorld() != client.world) {
            preview = switch (kind) {
                case HORSES -> new HorseEntity(EntityType.HORSE, client.world);
                case DONKEYS -> new DonkeyEntity(EntityType.DONKEY, client.world);
                case COWS -> new CowEntity(EntityType.COW, client.world);
                case CHICKENS -> new ChickenEntity(EntityType.CHICKEN, client.world);
                case PIGS -> new PigEntity(EntityType.PIG, client.world);
                case GOATS -> new GoatEntity(EntityType.GOAT, client.world);
                case SHEEP -> new SheepEntity(EntityType.SHEEP, client.world);
                default -> null;
            };
            if (preview != null) fallbackAnimals.put(kind, preview);
        }
        if (preview instanceof HorseEntity entity) HorseAppearance.from(horse).applyTo(entity);
        return preview;
    }

    private void renderScrollbar(DrawContext context) {
        if (maxScroll() <= 0) {
            return;
        }
        int trackHeight = contentBottom() - contentTop();
        int barHeight = Math.max(14,
                trackHeight * trackHeight / Math.max(trackHeight, contentHeight));
        int barY = contentTop()
                + (trackHeight - barHeight) * scroll / maxScroll();
        context.fill(contentRight() + 5, contentTop(), contentRight() + 7,
                contentBottom(), 0x553E4650);
        context.fill(contentRight() + 4, barY, contentRight() + 8,
                barY + barHeight, BORDER);
    }

    private void renderSortMenu(DrawContext context, int mouseX, int mouseY) {
        if (!sortMenuOpen) {
            return;
        }
        int x = sortX();
        int y = sortY() + 20;
        for (SortOrder option : SortOrder.values()) {
            boolean hovered = inside(mouseX, mouseY, x, y, sortWidth(), 18);
            boolean active = sorting.active(option);
            context.fill(x - 1, y - 1, x + sortWidth() + 1, y + 19, BORDER);
            context.fill(x, y, x + sortWidth(), y + 18,
                    active ? 0xFF80632E
                            : hovered ? 0xFF3A4650 : 0xFF202B25);
            context.fill(x + 4, y + 7, x + 8, y + 11, active ? GOLD : MUTED);
            String label = Text.translatable(option.translationKey()).getString();
            context.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(label, sortWidth() - 35),
                    x + 13, y + 5, active ? GOLD : BODY);
            if (hovered && (option.isSpeed() || option.isJump())) {
                String action = active ? "remove" : "select";
                context.drawTooltip(textRenderer, Text.translatable("ottoextra.horses.overview.sort." + action,
                        Text.translatable(option.translationKey())), mouseX, mouseY);
            }
            y += 20;
        }
        context.fill(x - 1, y - 1, x + sortWidth() + 1, y + 19, BORDER);
        context.fill(x, y, x + sortWidth(), y + 18,
                inside(mouseX, mouseY, x, y, sortWidth(), 18) ? 0xFF3A4650 : 0xFF202B25);
        context.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.horses.overview.sort.reset"),
                x + 6, y + 5, BODY);
    }

    private int sortResetY() {
        return sortY() + 20 + SortOrder.values().length * 20;
    }

    private SortOrder sortOptionAt(double mouseX, double mouseY) {
        int y = sortY() + 20;
        for (SortOrder option : SortOrder.values()) {
            if (inside(mouseX, mouseY, sortX(), y, sortWidth(), 18)) {
                return option;
            }
            y += 20;
        }
        return null;
    }

    private AnimalBreedingHorse horseAtButton(int mouseX, int mouseY,
                                               int buttonX) {
        if (mouseX < buttonX || mouseX >= buttonX + 18
                || mouseY < contentTop() || mouseY > contentBottom()) {
            return null;
        }
        int y = contentTop() - scroll;
        for (RowEntry row : rows) {
            if (row.horse() != null && mouseY >= y + 4 && mouseY < y + 22) {
                return row.horse();
            }
            y += row.height();
        }
        return null;
    }

    private void drawSmallText(DrawContext context, String text, int x, int y,
                               int color, int maxWidth) {
        HorseUiIcons.smallText(context, textRenderer, text, x, y, color, maxWidth, 0.75F);
    }

    private static void drawIcon(DrawContext context, Identifier icon,
                                 int x, int y, int color) {
        HorseUiIcons.draw(context, icon, x, y, 8, 16, color);
    }

    private static Identifier icon(String name) {
        return HorseUiIcons.icon(name);
    }

    private static boolean inside(double mouseX, double mouseY,
                                  int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static int buttonColor(int mouseX, int mouseY, int x, int rowY) {
        return mouseX >= x && mouseX < x + 18
                && mouseY >= rowY + 4 && mouseY < rowY + 22
                ? 0xFF3A4650 : 0xFF202B25;
    }

    private static String displayName(AnimalBreedingHorse horse) {
        return HorseOverviewView.displayName(horse);
    }

    private static String value(String raw) {
        return raw == null || raw.isBlank() ? "–" : raw;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private int panelX() {
        return Math.max(5, (width - panelW()) / 2);
    }

    private int panelY() {
        return Math.max(5, (height - panelH()) / 2);
    }

    private int panelW() {
        return Math.min(680, Math.max(300, width - 20));
    }

    private int panelH() {
        return Math.min(410, Math.max(200, height - 20));
    }

    private int contentX() {
        return panelX() + 12;
    }

    private int contentRight() {
        return panelX() + panelW() - 18;
    }

    private int finderX() {
        return deleteX() - 22;
    }

    private int deleteX() {
        return contentRight() - 22;
    }

    private int sortX() {
        return panelX() + 12;
    }

    private int sortY() {
        return panelY() + 50;
    }

    private int sortWidth() {
        return Math.min(225, panelW() - 112);
    }

    private int speedX() {
        int x = contentX() + 43;
        return x + Math.max(48, Math.max(90, finderX() - x - 6) * 30 / 100);
    }

    private int jumpX() {
        int x = contentX() + 43;
        return speedX() + Math.max(62, Math.max(90, finderX() - x - 6) * 35 / 100);
    }

    private int contentTop() {
        return panelY() + 76;
    }

    private int contentBottom() {
        return panelY() + panelH() - 35;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (contentBottom() - contentTop()));
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record RowEntry(String farm, int count,
                            AnimalBreedingHorse horse, int height) {
        private static RowEntry header(String farm, int count) {
            return new RowEntry(farm, count, null, HEADER_HEIGHT);
        }

        private static RowEntry horse(AnimalBreedingHorse horse) {
            return new RowEntry("", 0, horse, HORSE_HEIGHT);
        }
    }

}
