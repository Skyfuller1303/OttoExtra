package de.ottoextra.ui.theme;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Textfeld im Buchstil: Tintenlinie auf Pergament oder dunkles Schieferfeld. Das sichtbare
 * Rechteck ist (x, y, width, height); der Text wird 3 px eingerückt und senkrecht zentriert.
 */
public final class InkTextField extends TextFieldWidget {

    private final boolean slate;
    private final int leftPad;

    public InkTextField(TextRenderer textRenderer, int x, int y, int width, int height,
                        Text message, boolean slate) {
        this(textRenderer, x, y, width, height, message, slate, 0);
    }

    /** {@code leftPad} lässt links Platz für ein Symbol (z. B. die Lupe). */
    public InkTextField(TextRenderer textRenderer, int x, int y, int width, int height,
                        Text message, boolean slate, int leftPad) {
        super(textRenderer, x + 3 + leftPad, y, Math.max(8, width - 6 - leftPad), height, message);
        this.slate = slate;
        this.leftPad = leftPad;
        setDrawsBackground(false);
        setTextShadow(false);
        setEditableColor(slate ? OttonienTheme.LABEL : OttonienTheme.INK);
        setUneditableColor(slate ? 0xFF8A8A90 : 0xAA7A6248);
    }

    /** True for the dark slate field, false for the ink line on parchment. */
    public boolean isSlate() {
        return slate;
    }

    /** Width of the drawn box left of the text area (3 px plus the icon padding). */
    public int boxInsetLeft() {
        return 3 + leftPad;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (!isVisible()) {
            return;
        }
        int left = getX() - 3 - leftPad;
        int right = getX() + getWidth() + 3;
        int top = getY();
        int bottom = getY() + getHeight();
        if (slate) {
            OttonienTheme.rect(context, left, top, right, bottom, 0xFF000000);
            OttonienTheme.rect(context, left + 1, top + 1, right - 1, bottom - 1,
                    OttonienTheme.FIELD_DARK);
        } else {
            OttonienTheme.rect(context, left, bottom - 1, right, bottom,
                    isFocused() ? OttonienTheme.INK_MUTED : OttonienTheme.PA_S);
        }
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(0f, (float) ((getHeight() - 8) / 2 - (slate ? 0 : 1)));
        super.renderWidget(context, mouseX, mouseY, deltaTicks);
        matrices.popMatrix();
    }
}
