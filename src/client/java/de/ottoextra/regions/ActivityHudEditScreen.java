package de.ottoextra.regions;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ActivityHudEditScreen extends Screen {
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 88;
    private static final int HANDLE_SIZE = 9;
    private static final String[] POSITIONS = {
            "MIDDLE_LEFT", "TOP_LEFT", "BOTTOM_LEFT",
            "MIDDLE_RIGHT", "TOP_RIGHT", "BOTTOM_RIGHT", "CUSTOM"
    };

    private final Screen parent;
    private final OttoExtraConfig config;
    private ButtonWidget positionButton;
    private boolean dragging;
    private boolean resizing;
    private double dragOffsetX;
    private double dragOffsetY;
    private double resizeStartX;
    private double resizeStartY;
    private float resizeStartScale;

    public ActivityHudEditScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.activityHud.edit.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        positionButton = addDrawableChild(ButtonWidget.builder(positionLabel(),
                button -> cyclePosition()).dimensions(width / 2 - 80, height - 50, 160, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 80, height - 28, 76, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.activityHud.edit.reset"),
                button -> reset()).dimensions(width / 2 + 4, height - 28, 76, 18).build());
    }

    private Text positionLabel() {
        return Text.translatable("ottoextra.activityHud.edit.position",
                Text.translatable("ottoextra.activityHud.position."
                        + normalizedPosition().toLowerCase(java.util.Locale.ROOT)));
    }

    private String normalizedPosition() {
        String current = config.regions.activityHudPosition;
        if (current != null) {
            for (String position : POSITIONS) {
                if (position.equalsIgnoreCase(current)) {
                    return position;
                }
            }
        }
        return "MIDDLE_LEFT";
    }

    private void cyclePosition() {
        String current = normalizedPosition();
        int index = 0;
        for (int i = 0; i < POSITIONS.length; i++) {
            if (POSITIONS[i].equals(current)) {
                index = i;
                break;
            }
        }
        config.regions.activityHudPosition = POSITIONS[(index + 1) % POSITIONS.length];
        positionButton.setMessage(positionLabel());
        config.save();
    }

    private void reset() {
        config.regions.activityHudPosition = "MIDDLE_LEFT";
        config.regions.activityHudOffsetX = 0;
        config.regions.activityHudOffsetY = 0;
        config.regions.activityHudCustomX = 0.5f;
        config.regions.activityHudCustomY = 0.5f;
        config.regions.activityHudScale = 1.0f;
        positionButton.setMessage(positionLabel());
        config.save();
    }

    private int[] bounds() {
        float scale = RegionActivityHud.clampScale(config.regions.activityHudScale);
        int renderedWidth = Math.round(PREVIEW_WIDTH * scale);
        int renderedHeight = Math.round(PREVIEW_HEIGHT * scale);
        int[] pos = RegionActivityHud.position(width, height, renderedWidth, renderedHeight,
                config.regions);
        return new int[]{pos[0], pos[1], renderedWidth, renderedHeight};
    }

    private int[] resizeHandle() {
        int[] bounds = bounds();
        return new int[]{bounds[0] + bounds[2] - HANDLE_SIZE - 2,
                bounds[1] + bounds[3] - HANDLE_SIZE - 2, HANDLE_SIZE};
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        int[] handle = resizeHandle();
        if (inside(click.x(), click.y(), handle[0], handle[1], handle[2], handle[2])) {
            resizing = true;
            resizeStartX = click.x();
            resizeStartY = click.y();
            resizeStartScale = config.regions.activityHudScale;
            return true;
        }
        int[] bounds = bounds();
        if (inside(click.x(), click.y(), bounds[0], bounds[1], bounds[2], bounds[3])) {
            if (!"CUSTOM".equals(normalizedPosition())) {
                saveCustomPosition(bounds[0], bounds[1], bounds[2], bounds[3]);
                config.regions.activityHudPosition = "CUSTOM";
                positionButton.setMessage(positionLabel());
            }
            dragging = true;
            dragOffsetX = click.x() - bounds[0];
            dragOffsetY = click.y() - bounds[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (resizing) {
            double movement = (click.x() - resizeStartX + click.y() - resizeStartY) / 2.0;
            config.regions.activityHudScale = RegionActivityHud.clampScale(
                    resizeStartScale + (float) (movement / 100.0));
            return true;
        }
        if (dragging) {
            int[] bounds = bounds();
            int x = Math.max(0, Math.min((int) Math.round(click.x() - dragOffsetX),
                    width - bounds[2]));
            int y = Math.max(0, Math.min((int) Math.round(click.y() - dragOffsetY),
                    height - bounds[3]));
            saveCustomPosition(x, y, bounds[2], bounds[3]);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging || resizing) {
            dragging = false;
            resizing = false;
            config.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void saveCustomPosition(int x, int y, int renderedWidth, int renderedHeight) {
        int availableX = Math.max(0, width - renderedWidth);
        int availableY = Math.max(0, height - renderedHeight);
        config.regions.activityHudCustomX = availableX == 0 ? 0.0f
                : RegionActivityHud.clampUnit(x / (float) availableX);
        config.regions.activityHudCustomY = availableY == 0 ? 0.0f
                : RegionActivityHud.clampUnit(y / (float) availableY);
    }

    private static boolean inside(double mouseX, double mouseY,
                                  int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.activityHud.edit.hint"), width / 2, 10, 0xFFE6C8A9);

        int[] bounds = bounds();
        int border = dragging ? 0xFFFFD479 : 0xFFB08D57;
        ctx.fill(bounds[0] - 2, bounds[1] - 2,
                bounds[0] + bounds[2] + 2, bounds[1] - 1, border);
        ctx.fill(bounds[0] - 2, bounds[1] + bounds[3] + 1,
                bounds[0] + bounds[2] + 2, bounds[1] + bounds[3] + 2, border);
        ctx.fill(bounds[0] - 2, bounds[1] - 1,
                bounds[0] - 1, bounds[1] + bounds[3] + 1, border);
        ctx.fill(bounds[0] + bounds[2] + 1, bounds[1] - 1,
                bounds[0] + bounds[2] + 2, bounds[1] + bounds[3] + 1, border);
        drawPreview(ctx, bounds[0], bounds[1]);

        int[] handle = resizeHandle();
        int handleColor = resizing ? 0xFFFFD479 : 0xFFB08D57;
        ctx.fill(handle[0], handle[1], handle[0] + handle[2], handle[1] + handle[2],
                0xCC000000);
        for (int i = 1; i < handle[2] - 1; i++) {
            ctx.fill(handle[0] + i, handle[1] + i,
                    handle[0] + i + 1, handle[1] + i + 1, handleColor);
        }
    }

    private void drawPreview(DrawContext ctx, int x, int y) {
        float scale = RegionActivityHud.clampScale(config.regions.activityHudScale);
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        try {
            ctx.fill(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT, 0x78000000);
            ctx.drawTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.activityHud.title"), 3, 2, 0xFFE8E8E8);
            String[] names = {"Holdstewik", "Nordeck", "Ährstein", "Eden"};
            for (int i = 0; i < names.length; i++) {
                int rowY = 13 + i * 16;
                ctx.fill(1, rowY, PREVIEW_WIDTH - 1, rowY + 16, 0x36000000);
                ctx.fill(1, rowY + 1, 3, rowY + 15,
                        i == 0 ? 0xFFFF5252 : 0xFFFFD54F);
                ctx.fill(5, rowY + 2, 17, rowY + 14, 0xFF59462F);
                ctx.drawTextWithShadow(textRenderer, names[i], 20, rowY + 3, 0xFFFFFFFF);
            }
            String footer = RegionActivityHud.footerText(config.regions,
                    System.currentTimeMillis() - 120_000L,
                    System.currentTimeMillis());
            if (!footer.isEmpty()) {
                int footerX = Math.max(3,
                        PREVIEW_WIDTH - 3 - textRenderer.getWidth(footer));
                ctx.drawText(textRenderer, footer, footerX,
                        PREVIEW_HEIGHT - 9, 0xFFAAAAAA, false);
            }
        } finally {
            matrices.popMatrix();
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
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
