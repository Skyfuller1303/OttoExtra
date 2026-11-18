package de.ottoextra.tacticalfactions.theme;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Schieferplatten-Button im Ottonien-Stil; verhält sich wie jeder andere {@link ButtonWidget}. */
public final class SlateButton extends ButtonWidget {

    private boolean danger;

    public SlateButton(int x, int y, int width, int height, net.minecraft.text.Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    /** Dunkelrote Variante für zerstörerische Aktionen. */
    public SlateButton danger() {
        this.danger = true;
        return this;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        boolean hover = active && isHovered();
        if (danger) {
            OttonienTheme.dangerButton(context, getX(), getY(), getWidth(), getHeight(), hover);
        } else {
            OttonienTheme.slateButton(context, getX(), getY(), getWidth(), getHeight(), hover);
        }
        var font = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        net.minecraft.text.Text label = getMessage();
        int color = !active ? 0xFF6A7078 : hover ? OttonienTheme.GOLD_HOVER : OttonienTheme.LABEL;
        int tx = getX() + (getWidth() - font.getWidth(label)) / 2;
        context.drawText(font, label, tx, getY() + (getHeight() - 8) / 2, color, false);
    }
}
