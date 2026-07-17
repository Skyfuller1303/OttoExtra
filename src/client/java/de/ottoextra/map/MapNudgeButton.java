package de.ottoextra.map;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
public final class MapNudgeButton extends ClickableWidget {
    private static final int STEP = 10;
    private final OttoExtraConfig config;
    private final int dirX;
    private final int dirZ;
    public MapNudgeButton(int x, int y, int size, int dirX, int dirZ, OttoExtraConfig config) {
        super(x, y, size, size, Text.translatable("ottoextra.map.nudge"));
        this.config = config;
        this.dirX = dirX;
        this.dirZ = dirZ;
        setTooltip(Tooltip.of(Text.translatable("ottoextra.map.nudge")));
    }
    @Override
    public void onClick(Click click, boolean doubled) {
        if (dirX == 0 && dirZ == 0) {
            config.map.paintedMapOffsetX = 0;
            config.map.paintedMapOffsetZ = 0;
            config.save();
            return;
        }
        boolean shift = (click.modifiers() & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0;
        int step = shift ? STEP * 5 : STEP;
        config.map.paintedMapOffsetX += dirX * step;
        config.map.paintedMapOffsetZ += dirZ * step;
        config.save();
    }
    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int s = getWidth();
        int col = isHovered() ? 0xFFFFFFFF : 0xCCDDDDDD;
        ctx.fill(x, y, x + s, y + s, isHovered() ? 0x66000000 : 0x44000000);
        if (dirX == 0 && dirZ == 0) {
            int m = s / 2;
            ctx.fill(x + m - 2, y + m - 2, x + m + 2, y + m + 2, col);
            return;
        }
        int cx = x + s / 2;
        int cy = y + s / 2;
        int len = s / 2 - 2;
        for (int i = 0; i < len; i++) {
            int half = Math.max(0, len - 1 - i);
            if (dirZ != 0) {
                int yy = cy + dirZ * (i - len / 2);
                ctx.fill(cx - half, yy, cx + half + 1, yy + 1, col);
            } else {
                int xx = cx + dirX * (i - len / 2);
                ctx.fill(xx, cy - half, xx + 1, cy + half + 1, col);
            }
        }
    }
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
