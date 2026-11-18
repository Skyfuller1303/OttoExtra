package de.ottoextra.resources;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

final class ResourceMapFilterScreen extends Screen {
    private final Screen parent;
    private final ResourcesConfig config;

    ResourceMapFilterScreen(Screen parent, ResourcesConfig config) {
        super(Text.translatable("ottoextra.resources.filter.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        clearChildren();
        int left = width / 2 - 154;
        addDrawableChild(new ResourceBook.Button(left, 48, 150, 20, masterText("ottoextra.resources.filter.resources",
                config.showResourceIcons), ResourceBook.Tone.SLATE, button -> {
            config.setResourceIconsVisible(!config.showResourceIcons);
            init();
        }));
        addDrawableChild(new ResourceBook.Button(left + 158, 48, 150, 20, masterText("ottoextra.resources.filter.markers",
                config.showPoiIcons), ResourceBook.Tone.SLATE, button -> {
            config.setPoiIconsVisible(!config.showPoiIcons);
            init();
        }));
        addDrawableChild(new ResourceBook.Button(width / 2 - 75, height - 30, 150, 20, Text.translatable("gui.done"),
                ResourceBook.Tone.SLATE, button -> close()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xE0101010);
        context.drawText(textRenderer, title, (width - textRenderer.getWidth(title)) / 2, 20, 0xFFE4C477, false);
        super.render(context, mouseX, mouseY, delta);
    }

    private static Text masterText(String key, boolean visible) {
        return Text.translatable("ottoextra.resources.toggle", Text.translatable(key),
                Text.translatable(visible ? "ottoextra.resources.on" : "ottoextra.resources.off"));
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
