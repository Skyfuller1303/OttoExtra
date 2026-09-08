package de.ottoextra.animalinfo;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AnimalInfoEditScreen extends Screen {

    private static final int HANDLE_SIZE = 6;
    private static final int ANIMAL_ENABLED_Y = 45;
    private static final int TOGGLE_START_Y = 67;
    private static final int TOGGLE_ROW_HEIGHT = 20;
    private static final int HINT_Y = 152;
    private static final int PREVIEW_MIN_Y = 164;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final Map<AnimalInfoOverlay.Part, ButtonWidget> toggleButtons =
            new EnumMap<>(AnimalInfoOverlay.Part.class);
    private String animalType = "sheep";
    private ButtonWidget animalTypeButton;
    private ButtonWidget animalEnabledButton;

    private AnimalInfoOverlay.Part dragging;
    private AnimalInfoOverlay.Part resizing;
    private double dragOffsetX;
    private double dragOffsetY;
    private double resizeStartX;
    private double resizeStartY;
    private float resizeStartScale;

    public AnimalInfoEditScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.animalinfo.editor.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        toggleButtons.clear();
        animalTypeButton = ButtonWidget.builder(animalTypeLabel(), ignored -> {
            int current = OttoExtraConfig.AnimalInfo.ANIMAL_TYPES.indexOf(animalType);
            animalType = OttoExtraConfig.AnimalInfo.ANIMAL_TYPES.get(
                    (current + 1) % OttoExtraConfig.AnimalInfo.ANIMAL_TYPES.size());
            dragging = null;
            resizing = null;
            animalTypeButton.setMessage(animalTypeLabel());
            updateToggleLabels();
        }).dimensions(width / 2 - 80, 23, 160, 18).build();
        addDrawableChild(animalTypeButton);

        animalEnabledButton = ButtonWidget.builder(animalEnabledLabel(), ignored -> {
            currentLayout().enabled = !currentLayout().enabled;
            if (!currentLayout().enabled) {
                AnimalInfoOverlay.clear();
            }
            updateToggleLabels();
            config.save();
        }).dimensions(width / 2 - 80, ANIMAL_ENABLED_Y, 160, 18).build();
        addDrawableChild(animalEnabledButton);

        AnimalInfoOverlay.Part[] parts = AnimalInfoOverlay.Part.values();
        int buttonWidth = 96;
        int gap = 4;
        int rowWidth = buttonWidth * 3 + gap * 2;
        int startX = (width - rowWidth) / 2;
        for (int i = 0; i < parts.length; i++) {
            AnimalInfoOverlay.Part part = parts[i];
            int x = startX + (i % 3) * (buttonWidth + gap);
            int y = TOGGLE_START_Y + (i / 3) * TOGGLE_ROW_HEIGHT;
            ButtonWidget button = ButtonWidget.builder(toggleLabel(part), ignored -> {
                boolean visible = !AnimalInfoOverlay.partVisible(currentLayout(), part);
                AnimalInfoOverlay.setPartVisible(currentLayout(), part, visible);
                updateToggleLabels();
                config.save();
            }).dimensions(x, y, buttonWidth, 18).build();
            toggleButtons.put(part, button);
            addDrawableChild(button);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), ignored -> close())
                .dimensions(width / 2 - 80, height - 26, 76, 18).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.animalinfo.editor.reset"), ignored -> {
                    AnimalInfoOverlay.resetLayout(config.animalInfo, animalType);
                    updateToggleLabels();
                    config.save();
                }).dimensions(width / 2 + 4, height - 26, 76, 18).build());
    }

    private Text toggleLabel(AnimalInfoOverlay.Part part) {
        return Text.translatable(partKey(part)).append(": ").append(Text.translatable(
                AnimalInfoOverlay.partVisible(currentLayout(), part)
                        ? "ottoextra.animalinfo.editor.on"
                        : "ottoextra.animalinfo.editor.off"));
    }

    private Text animalTypeLabel() {
        return Text.translatable("ottoextra.animalinfo.editor.animal").append(": ")
                .append(Text.translatable("ottoextra.animalinfo.editor.type." + animalType));
    }

    private Text animalEnabledLabel() {
        return Text.translatable("ottoextra.animalinfo.editor.enabled").append(": ")
                .append(Text.translatable(currentLayout().enabled
                        ? "ottoextra.animalinfo.editor.on"
                        : "ottoextra.animalinfo.editor.off"));
    }

    private OttoExtraConfig.AnimalInfoLayout currentLayout() {
        return config.animalInfo.layout(animalType);
    }

    private void updateToggleLabels() {
        if (animalEnabledButton != null) {
            animalEnabledButton.setMessage(animalEnabledLabel());
        }
        for (var entry : toggleButtons.entrySet()) {
            entry.getValue().setMessage(toggleLabel(entry.getKey()));
        }
    }

    private static String partKey(AnimalInfoOverlay.Part part) {
        return "ottoextra.animalinfo.editor.part."
                + part.name().toLowerCase(java.util.Locale.ROOT);
    }

    private int previewX() {
        return Math.max(4, Math.min(width
                        - AnimalInfoOverlay.editorWidth(config.animalInfo, animalType) - 4,
                width / 2 - 112));
    }

    private int previewY() {
        int toastHeight = AnimalInfoOverlay.editorHeight(config.animalInfo, animalType);
        return Math.max(PREVIEW_MIN_Y, Math.min(height - toastHeight - 34,
                (height - toastHeight) / 2 + 12));
    }

    private List<AnimalInfoOverlay.PartBounds> parts() {
        return AnimalInfoOverlay.editorParts(config.animalInfo, animalType);
    }

    private AnimalInfoOverlay.PartBounds hoveredPart(double mouseX, double mouseY) {
        int ox = previewX() + currentLayout().padding;
        int oy = previewY() + currentLayout().padding;
        List<AnimalInfoOverlay.PartBounds> parts = parts();
        for (int i = parts.size() - 1; i >= 0; i--) {
            AnimalInfoOverlay.PartBounds part = parts.get(i);
            if (mouseX >= ox + part.x() && mouseX <= ox + part.x() + part.width()
                    && mouseY >= oy + part.y() && mouseY <= oy + part.y() + part.height()) {
                return part;
            }
        }
        return null;
    }

    private boolean overResizeHandle(AnimalInfoOverlay.PartBounds part,
                                     double mouseX, double mouseY) {
        int x = previewX() + currentLayout().padding + part.x() + part.width() + 1;
        int y = previewY() + currentLayout().padding + part.y() + part.height() + 1;
        return mouseX >= x && mouseX <= x + HANDLE_SIZE
                && mouseY >= y && mouseY <= y + HANDLE_SIZE;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            for (AnimalInfoOverlay.PartBounds part : parts()) {
                if (overResizeHandle(part, click.x(), click.y())) {
                    resizing = part.part();
                    resizeStartX = click.x();
                    resizeStartY = click.y();
                    resizeStartScale = AnimalInfoOverlay.partScale(
                            currentLayout(), resizing);
                    return true;
                }
            }
            AnimalInfoOverlay.PartBounds part = hoveredPart(click.x(), click.y());
            if (part != null) {
                dragging = part.part();
                dragOffsetX = click.x()
                        - (previewX() + currentLayout().padding + part.x());
                dragOffsetY = click.y()
                        - (previewY() + currentLayout().padding + part.y());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (resizing != null) {
            double delta = (click.x() - resizeStartX + click.y() - resizeStartY) / 2.0;
            float next = resizing == AnimalInfoOverlay.Part.MODEL
                    ? resizeStartScale + (float) delta
                    : resizeStartScale + (float) delta * 0.02f;
            AnimalInfoOverlay.setPartScale(currentLayout(), resizing, next);
            return true;
        }
        if (dragging != null) {
            int x = (int) Math.round(click.x() - previewX()
                    - currentLayout().padding - dragOffsetX);
            int y = (int) Math.round(click.y() - previewY()
                    - currentLayout().padding - dragOffsetY);
            AnimalInfoOverlay.setPartPosition(currentLayout(), dragging, x, y);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null || resizing != null) {
            dragging = null;
            resizing = null;
            config.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 7, 0xFFF4F7FA);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.animalinfo.editor.hint"),
                width / 2, HINT_Y, 0xFFB8C0C8);

        int x = previewX();
        int y = previewY();
        AnimalInfoOverlay.renderEditorPreview(
                context, x, y, config.animalInfo, animalType);
        AnimalInfoOverlay.PartBounds hovered = hoveredPart(mouseX, mouseY);
        for (AnimalInfoOverlay.PartBounds part : parts()) {
            int px = x + currentLayout().padding + part.x();
            int py = y + currentLayout().padding + part.y();
            boolean active = part.part() == dragging || part.part() == resizing;
            boolean hot = hovered != null && hovered.part() == part.part();
            int color = active ? 0xFFFFD479 : hot ? 0xFFE6C8A9 : 0xFF796A55;
            outline(context, px - 1, py - 1,
                    px + part.width() + 1, py + part.height() + 1, color);

            int hx = px + part.width() + 1;
            int hy = py + part.height() + 1;
            context.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE,
                    active ? 0xFFFFD479 : 0xFF796A55);
        }
    }

    private static void outline(DrawContext context, int left, int top,
                                int right, int bottom, int color) {
        context.fill(left, top, right, top + 1, color);
        context.fill(left, bottom - 1, right, bottom, color);
        context.fill(left, top, left + 1, bottom, color);
        context.fill(right - 1, top, right, bottom, color);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        config.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
