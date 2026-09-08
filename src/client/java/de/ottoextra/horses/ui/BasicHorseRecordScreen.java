package de.ottoextra.horses.ui;

import de.ottoextra.OttoExtra;
import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.api.ApiProblem;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.horses.HorseRegistry;
import de.ottoextra.horses.RegistryAnimals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.concurrent.CompletionException;

/** Compact identity editor shipped by Core when AnimalBreeding is absent. */
public final class BasicHorseRecordScreen extends Screen {

    private static final int PANEL = 0xF018202A;
    private static final int INNER = 0xD0202B35;
    private static final int BORDER = 0xFF6D5A41;
    private static final int TITLE = 0xFFF4EDE5;
    private static final int GOLD = 0xFFDAA84A;
    private static final int BODY = 0xFFD8D0C7;
    private static final int MUTED = 0xFFAA9D90;
    private static final int ERROR = 0xFFFF8E7A;

    private final Screen parent;
    private final AnimalEntity animal;
    private final UUID entityUuid;
    private final AnimalInfoObservation observation;

    private TextFieldWidget nameField;
    private String nameDraft;
    private ButtonWidget saveButton;
    private boolean saving;
    private boolean errorStatus;
    private boolean accessRequested;
    private String status = "";

    public BasicHorseRecordScreen(Screen parent, AnimalEntity animal,
                                  AnimalInfoObservation observation) {
        super(Text.translatable("ottoextra.horses.record.title"));
        this.parent = parent;
        this.animal = animal;
        this.entityUuid = animal.getUuid();
        this.observation = observation;
    }

    @Override
    protected void init() {
        if (nameField != null) nameDraft = nameField.getText();
        if (!accessRequested) {
            accessRequested = true;
            HorseRegistry.refreshNamingPlayer(client == null || client.player == null
                    ? null : client.player.getUuid()).exceptionally(error -> null);
            HorseRegistry.refresh().exceptionally(error -> java.util.List.of());
            HorseRegistry.refreshDatabaseRecords().exceptionally(error -> java.util.List.of());
        }
        int px = panelX();
        int py = panelY();
        int formX = px + 126;
        int formW = panelW() - 140;

        nameField = null;
        saveButton = null;
        if (canEditName()) {
            nameField = new TextFieldWidget(textRenderer, formX, py + 61,
                    formW, 18, Text.translatable("ottoextra.horses.record.name"));
            nameField.setMaxLength(64);
            nameField.setText(nameDraft != null ? nameDraft : HorseRegistry.name(entityUuid).orElse(""));
            nameField.setPlaceholder(Text.translatable("ottoextra.horses.record.name_hint"));
            nameField.setChangedListener(value -> updateSaveButton());
            addDrawableChild(nameField);
        }

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.horses.record.overview"),
                        button -> {
                            if (client != null) {
                                client.setScreen(new BasicHorseOverviewScreen(this));
                            }
                        })
                .dimensions(px + 12, py + panelH() - 29, 76, 18).build());
        if (nameField != null) {
            saveButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.horses.record.save"),
                        button -> save())
                .dimensions(px + panelW() - 154, py + panelH() - 29,
                        68, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"),
                        button -> close())
                .dimensions(px + panelW() - 80, py + panelH() - 29,
                        68, 18).build());
        updateSaveButton();
        if (nameField != null) setInitialFocus(nameField);
    }

    @Override
    public void tick() {
        if (!saving && (nameField != null) != canEditName()) {
            clearAndInit();
        }
    }

    private void updateSaveButton() {
        if (saveButton != null) {
            saveButton.active = !saving && canEditName() && nameField != null
                    && !nameField.getText().trim().isEmpty();
        }
    }

    private void save() {
        if (saving || !canEditName()
                || nameField == null || nameField.getText().trim().isEmpty()) {
            return;
        }
        saving = true;
        errorStatus = false;
        status = Text.translatable("ottoextra.horses.message.saving").getString();
        updateSaveButton();
        String coat = "";
        String marking = "";
        if (animal instanceof HorseEntity vanillaHorse) {
            coat = vanillaHorse.getHorseColor().name();
            marking = vanillaHorse.getMarking().name();
        }
        HorseRegistry.saveName(entityUuid, nameField.getText(), currentLocation(),
                        coat, marking)
                .whenComplete((saved, error) -> {
                    if (client == null) return;
                    client.execute(() -> {
                        saving = false;
                        updateSaveButton();
                        if (client.currentScreen != this) return;
                        if (error != null) {
                            Throwable cause = unwrap(error);
                            errorStatus = true;
                            status = errorMessage(cause);
                            OttoExtra.LOGGER.warn("Tiername {} konnte nicht gespeichert werden: {}",
                                    entityUuid, cause.toString());
                            return;
                        }
                        if (client.player != null) {
                            client.player.sendMessage(Text.translatable(
                                    "ottoextra.horses.message.saved"), true);
                        }
                        close();
                    });
                });
    }

    private boolean canEditName() {
        return HorseRegistry.canEditName(entityUuid, currentLocation());
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB0000000);
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();
        context.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        context.fill(px, py, px + pw, py + ph, PANEL);
        context.drawTextWithShadow(textRenderer, title, px + 12, py + 11, TITLE);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("ottoextra.horses.record.basic"),
                px + 12, py + 25, GOLD);

        context.fill(px + 10, py + 42, px + 116, py + ph - 38, INNER);
        drawAnimal(context, px + 13, py + 44, px + 113, py + ph - 41,
                mouseX, mouseY);

        int x = px + 126;
        context.drawTextWithShadow(textRenderer,
                Text.translatable("ottoextra.horses.record.name"), x, py + 48, MUTED);
        if (nameField == null) {
            context.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(HorseRegistry.name(entityUuid).orElse("–"),
                            panelW() - 140), x, py + 66, BODY);
        }
        int formWidth = panelW() - 140;
        HorseUiIcons.smallText(context, textRenderer, entityUuid.toString(),
                x, py + 87, MUTED, formWidth, 0.65F);
        var value = observation.snapshot();
        int columnWidth = (formWidth - 8) / 2;
        int rightColumn = x + columnWidth + 8;
        context.drawTextWithShadow(textRenderer,
                textRenderer.trimToWidth(value.animalName(), columnWidth), x, py + 103, BODY);
        HorseUiIcons.gender(context, textRenderer, value.gender(),
                rightColumn, py + 103, columnWidth);
        if (value.hasHorseStats()) {
            drawStat(context, HorseUiIcons.SPEED, 16, value.speed(), x, py + 122,
                    columnWidth, "ottoextra.animalinfo.editor.part.speed", mouseX, mouseY);
            drawStat(context, HorseUiIcons.JUMP, 16, value.jumpHeight(), rightColumn, py + 122,
                    columnWidth, "ottoextra.animalinfo.editor.part.jump_height", mouseX, mouseY);
        } else if (value.hasShearable() || value.hasMilkable()) {
            drawStat(context, statusIcon(value.hasShearable() ? "shearable_ee405" : "milkable_ee406"),
                    8, value.hasShearable() ? value.shearable() : value.milkable(), x, py + 122,
                    formWidth, value.hasShearable() ? "ottoextra.horses.record.shearable_label"
                            : "ottoextra.horses.record.milkable_label", mouseX, mouseY);
        }
        drawStat(context, statusIcon("feeding_ee403"), 8, value.mast(), x, py + 141,
                columnWidth, "ottoextra.animalinfo.editor.part.mast", mouseX, mouseY);
        drawStat(context, statusIcon("breed_ready_ee407"), 8, value.breedingReady(),
                rightColumn, py + 141, columnWidth,
                "ottoextra.animalinfo.editor.part.breeding", mouseX, mouseY);
        if (!status.isBlank()) {
            context.drawTextWithShadow(textRenderer, Text.literal(status),
                    x, py + ph - 45, errorStatus ? ERROR : GOLD);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStat(DrawContext context, Identifier icon, int textureSize, String value,
                          int x, int y, int width, String label, int mouseX, int mouseY) {
        HorseUiIcons.draw(context, icon, x, y - 3, 16, textureSize, MUTED);
        HorseUiIcons.smallText(context, textRenderer, emptyDash(value), x + 20, y,
                BODY, width - 20, 0.85F);
        if (mouseX >= x && mouseX < x + width && mouseY >= y - 3 && mouseY < y + 13) {
            context.drawTooltip(textRenderer, Text.translatable(label)
                    .append(": ").append(emptyDash(value)), mouseX, mouseY);
        }
    }

    private static Identifier statusIcon(String name) {
        return Identifier.of("mattis", "textures/font/icons/clean/status/" + name + ".png");
    }

    private void drawAnimal(DrawContext context, int left, int top, int right,
                           int bottom, int mouseX, int mouseY) {
        if (!animal.isAlive()) return;
        boolean nameVisible = animal.isCustomNameVisible();
        Text customName = animal.getCustomName();
        try {
            animal.setCustomNameVisible(false);
            animal.setCustomName(null);
            InventoryScreen.drawEntity(context, left, top, right, bottom,
                    38, 0.0f, mouseX, mouseY, animal);
        } catch (Throwable ignored) {
        } finally {
            animal.setCustomName(customName);
            animal.setCustomNameVisible(nameVisible);
        }
    }

    private static String emptyDash(String value) {
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

    private int panelW() {
        return Math.min(420, Math.max(300, width - 24));
    }

    private int panelH() {
        return Math.min(205, Math.max(188, height - 24));
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    private int panelY() {
        return (height - panelH()) / 2;
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
