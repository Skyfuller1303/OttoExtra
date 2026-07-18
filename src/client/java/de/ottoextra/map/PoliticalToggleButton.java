package de.ottoextra.map;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class PoliticalToggleButton extends ClickableWidget {

    private final int size;
    private final OttoExtraConfig config;

    public PoliticalToggleButton(int x, int y, int size, OttoExtraConfig config) {
        super(x, y, size, size, Text.translatable("ottoextra.map.politicalToggle"));
        this.size = size;
        this.config = config;
        setTooltip(Tooltip.of(Text.translatable("ottoextra.map.politicalToggle")));
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        config.map.politicalFill = !config.map.politicalFill;
        config.save();
    }

    private static final net.minecraft.util.Identifier ICON =
            de.ottoextra.OttoExtra.id("textures/gui/political_toggle.png");
    private static Boolean iconAvailable;

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean on = config.map.politicalFill;

        int ix = getX() + (size - 16) / 2;
        int iy = getY() + (size - 16) / 2;
        if (iconAvailable == null) {
            iconAvailable = net.minecraft.client.MinecraftClient.getInstance()
                    .getResourceManager().getResource(ICON).isPresent();
        }
        int tint = on ? (isHovered() ? 0xFFFFFFFF : 0xE6FFFFFF)
                : (isHovered() ? 0x99CCCCCC : 0x66AAAAAA);
        if (iconAvailable) {
            ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, ICON,
                    ix, iy, 0f, 0f, 16, 16, 16, 16, tint);
        } else {

            int a = on ? 0xFF : 0x55;
            ctx.fill(ix + 1, iy + 1, ix + 8, iy + 8, (a << 24) | 0xB0524E);
            ctx.fill(ix + 8, iy + 1, ix + 15, iy + 8, (a << 24) | 0x5470B0);
            ctx.fill(ix + 1, iy + 8, ix + 8, iy + 15, (a << 24) | 0x6FA963);
            ctx.fill(ix + 8, iy + 8, ix + 15, iy + 15, (a << 24) | 0xB09A3F);
        }
        if (!on) {

            for (int i = 0; i < 14; i++) {
                ctx.fill(ix + 1 + i, iy + 1 + i, ix + 2 + i, iy + 2 + i, 0xFFE0E0E0);
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
