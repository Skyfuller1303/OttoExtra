package de.ottoextra.horses.ui;

import de.ottoextra.api.ApiProblem;
import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.horses.HorseRegistry;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.concurrent.CompletionException;

/** Core farm creation and selection; all writes remain subject to server permissions. */
public final class HorseFarmScreen extends Screen {
    private final Screen parent;
    private final UUID animal;
    private final AnimalBreedingHorseLocation location;
    private java.util.function.Consumer<AnimalBreedingFarm> selectionHandler;
    private TextFieldWidget nameField;
    private ButtonWidget createButton;
    private ButtonWidget assignButton;
    private ButtonWidget refreshButton;
    private String nameDraft = "";
    private String selected = "";
    private Text status = Text.empty();
    private boolean error;
    private boolean busy;
    private boolean loaded;
    private int scroll;

    public HorseFarmScreen(Screen parent) { this(parent, (UUID) null, null); }

    public static HorseFarmScreen selectForSave(Screen parent, String selectedKey,
                                               java.util.function.Consumer<AnimalBreedingFarm> handler) {
        HorseFarmScreen screen = new HorseFarmScreen(parent);
        screen.selected = selectedKey == null ? "" : selectedKey;
        screen.selectionHandler = java.util.Objects.requireNonNull(handler);
        return screen;
    }

    public HorseFarmScreen(Screen parent, UUID animal, AnimalBreedingHorseLocation location) {
        super(Text.translatable("ottoextra.horses.farms.title"));
        this.parent = parent;
        this.animal = animal;
        this.location = location;
    }

    @Override protected void init() {
        if (nameField != null) nameDraft = nameField.getText();
        nameField = new TextFieldWidget(textRenderer, x() + 12, y() + 53, panelW() - 126, 18,
                Text.translatable("ottoextra.horses.farms.name"));
        nameField.setMaxLength(64);
        nameField.setPlaceholder(Text.translatable("ottoextra.horses.farms.name"));
        nameField.setText(nameDraft);
        nameField.setChangedListener(value -> { nameDraft = value; updateButtons(); });
        addDrawableChild(nameField);
        createButton = addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.horses.farms.create"),
                button -> create()).dimensions(x() + panelW() - 108, y() + 53, 96, 18).build());
        createButton.setTooltip(Tooltip.of(Text.translatable("ottoextra.horses.farms.name.tip")));
        refreshButton = addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.horses.overview.refresh"),
                button -> refresh()).dimensions(x() + 12, y() + panelH() - 27, 76, 18).build());
        if (animal != null || selectionHandler != null) {
            assignButton = addDrawableChild(ButtonWidget.builder(Text.translatable(selectionHandler != null
                            ? "ottoextra.horses.farms.select" : "ottoextra.horses.farms.assign"),
                    button -> assign()).dimensions(x() + panelW() - 184, y() + panelH() - 27, 104, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(x() + panelW() - 72, y() + panelH() - 27, 60, 18).build());
        if (!loaded) refresh();
        updateButtons();
        setInitialFocus(nameField);
    }

    private void updateButtons() {
        if (createButton != null) createButton.active = loaded && !busy && AnimalBreedingFarm.validName(nameDraft);
        if (refreshButton != null) refreshButton.active = !busy;
        if (nameField != null) nameField.setEditable(!busy);
        if (assignButton != null) assignButton.active = loaded && !busy
                && ((selectionHandler != null && selected.isBlank())
                || HorseRegistry.farms().stream().anyMatch(f -> f.nameKey().equals(selected) && f.canEdit()))
                && (selectionHandler != null
                || HorseRegistry.databaseRecord(animal).map(h -> h.canEdit() || h.canManage()).orElse(false));
        if (assignButton != null && selectionHandler != null) assignButton.setMessage(Text.translatable(
                selected.isBlank() ? "ottoextra.horses.farms.keep_current" : "ottoextra.horses.farms.select"));
    }

    private void start(String key) {
        busy = true;
        error = false;
        status = Text.translatable(key);
        updateButtons();
    }

    private void refresh() {
        if (busy) return;
        start("ottoextra.horses.overview.loading");
        HorseRegistry.refreshFarms().whenComplete((farms, failure) -> client.execute(() -> {
            busy = false;
            if (client.currentScreen != this) return;
            loaded = failure == null;
            scroll = Math.min(scroll, maxScroll());
            status = Text.empty();
            if (failure != null) fail(failure, false);
            updateButtons();
        }));
    }

    private void create() {
        if (!createButton.active) return;
        start("ottoextra.horses.farms.creating");
        HorseRegistry.createFarm(nameDraft).whenComplete((farm, failure) -> client.execute(() -> {
            busy = false;
            if (client.currentScreen != this) return;
            if (failure == null) {
                selected = farm.nameKey();
                nameField.setText("");
                scroll = Math.max(0, Math.min(maxScroll(), HorseRegistry.farms().indexOf(farm) * 28));
                status = Text.translatable("ottoextra.horses.farms.created", farm.name());
            } else fail(failure, true);
            updateButtons();
        }));
    }

    private void assign() {
        if (!assignButton.active) return;
        if (selectionHandler != null) {
            if (selected.isBlank()) selectionHandler.accept(null);
            else HorseRegistry.farms().stream().filter(farm -> farm.nameKey().equals(selected) && farm.canEdit())
                    .findFirst().ifPresent(selectionHandler);
            close();
            return;
        }
        start("ottoextra.horses.farms.assigning");
        HorseRegistry.assignFarm(animal, selected, location)
                .thenCompose(horse -> HorseRegistry.refreshDatabaseRecords().handle((records, failure) -> horse))
                .whenComplete((horse, failure) -> client.execute(() -> {
            busy = false;
            if (client.currentScreen != this) return;
            if (failure == null) {
                if (client.player != null) client.player.sendMessage(Text.translatable(
                        "ottoextra.horses.farms.assigned", horse.currentFarm()), true);
                close();
            } else {
                fail(failure, false);
                busy = true;
                // A conflicting write requires a fresh record before trying again.
                HorseRegistry.refreshDatabaseRecords().whenComplete((ignored, refreshFailure) -> client.execute(() -> {
                    busy = false;
                    if (client.currentScreen == this) updateButtons();
                }));
                updateButtons();
            }
        }));
    }

    private void fail(Throwable failure, boolean creating) {
        while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
        String key = "ottoextra.horses.farms.failed";
        if (failure instanceof ApiProblem.ApiException api) {
            if (api.problem().isHttpStatus(403)) key = "ottoextra.horses.farms.forbidden";
            if (api.problem().isHttpStatus(409)) key = creating
                    ? "ottoextra.horses.farms.name_taken" : "ottoextra.horses.farms.conflict";
        }
        error = true;
        status = Text.translatable(key);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && !busy && click.x() >= x() + 12 && click.x() < x() + panelW() - 12
                && click.y() >= listTop() && click.y() < listBottom()) {
            int index = ((int) click.y() - listTop() + scroll) / 28;
            if (index >= 0 && index < HorseRegistry.farms().size()) {
                String key = HorseRegistry.farms().get(index).nameKey();
                selected = selectionHandler != null && selected.equals(key) ? "" : key;
                updateButtons();
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= x() && mouseX < x() + panelW() && mouseY >= listTop() && mouseY < listBottom()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(vertical) * 28));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB0000000);
        context.fill(x() - 1, y() - 1, x() + panelW() + 1, y() + panelH() + 1, 0xFF6D5A41);
        context.fill(x(), y(), x() + panelW(), y() + panelH(), 0xFF10171F);
        context.drawTextWithShadow(textRenderer, title, x() + 12, y() + 11, 0xFFDAA84A);
        Text subtitle = selectionHandler != null ? Text.translatable("ottoextra.horses.farms.select_for_save")
                : animal == null ? Text.translatable("ottoextra.horses.farms.desc")
                : Text.translatable("ottoextra.horses.farms.choose_for", HorseRegistry.name(animal).orElse("???"));
        HorseUiIcons.smallText(context, textRenderer, subtitle.getString(), x() + 12, y() + 29,
                0xFFD8D0C7, panelW() - 24, 0.85F);
        context.fill(x() + 10, listTop(), x() + panelW() - 10, listBottom(), 0xFF131B24);
        context.enableScissor(x() + 10, listTop(), x() + panelW() - 10, listBottom());
        int rowY = listTop() - scroll;
        for (AnimalBreedingFarm farm : HorseRegistry.farms()) {
            if (rowY + 28 > listTop() && rowY < listBottom()) {
                if (farm.nameKey().equals(selected)) context.fill(x() + 12, rowY, x() + panelW() - 12, rowY + 27, 0xFF304153);
                String suffix = (animal != null || selectionHandler != null) && !farm.canEdit() ? " · " + Text.translatable("ottoextra.horses.farms.read_only").getString() : "";
                context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(farm.name(), panelW() - 34),
                        x() + 17, rowY + 3, 0xFFDAA84A);
                HorseUiIcons.smallText(context, textRenderer, Text.translatable("ottoextra.horses.farms.count",
                        farm.horseCount()).getString() + suffix, x() + 17, rowY + 16, 0xFFAA9D90, panelW() - 34, 0.8F);
            }
            rowY += 28;
        }
        if (loaded && HorseRegistry.farms().isEmpty()) context.drawTextWithShadow(textRenderer,
                Text.translatable("ottoextra.horses.farms.empty"), x() + 17, listTop() + 10, 0xFFAA9D90);
        context.disableScissor();
        if (maxScroll() > 0) {
            int track = listBottom() - listTop();
            int thumb = Math.max(12, track * track / (HorseRegistry.farms().size() * 28));
            int top = listTop() + scroll * (track - thumb) / maxScroll();
            context.fill(x() + panelW() - 12, top, x() + panelW() - 10, top + thumb, 0xFFDAA84A);
        }
        int statusY = y() + panelH() - 49;
        for (var line : textRenderer.wrapLines(status, panelW() - 24).stream().limit(2).toList()) {
            context.drawTextWithShadow(textRenderer, line, x() + 12, statusY, error ? 0xFFFF8E7A : 0xFFDAA84A);
            statusY += 9;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private int panelW() { return Math.min(450, Math.max(300, width - 20)); }
    private int panelH() { return Math.min(350, Math.max(210, height - 20)); }
    private int x() { return (width - panelW()) / 2; }
    private int y() { return (height - panelH()) / 2; }
    private int listTop() { return y() + 82; }
    private int listBottom() { return y() + panelH() - 57; }
    private int maxScroll() { return Math.max(0, HorseRegistry.farms().size() * 28 - (listBottom() - listTop())); }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
