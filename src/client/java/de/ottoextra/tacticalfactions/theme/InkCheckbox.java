package de.ottoextra.tacticalfactions.theme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;

/** Kästchen mit Tintenbeschriftung auf Pergament; API entspricht dem Vanilla-Kästchen. */
public final class InkCheckbox extends PressableWidget {

    @FunctionalInterface
    public interface Callback {
        void onValueChange(InkCheckbox checkbox, boolean checked);
    }

    private static final int BOX = 9;

    private boolean checked;
    private final Callback callback;

    private InkCheckbox(int x, int y, Text message, TextRenderer textRenderer, boolean checked,
                        Callback callback) {
        super(x, y, BOX + 4 + textRenderer.getWidth(message), BOX, message);
        this.checked = checked;
        this.callback = callback;
    }

    public static Builder builder(Text message, TextRenderer textRenderer) {
        return new Builder(message, textRenderer);
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void onPress(AbstractInput input) {
        checked = !checked;
        callback.onValueChange(this, checked);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        boolean hover = active && isHovered();
        int x = getX();
        int y = getY();
        OttonienTheme.tile(context, x, y, BOX, BOX, hover);
        if (checked) {
            int c = active ? OttonienTheme.PICTO : 0x66514A3A;
            OttonienTheme.rect(context, x + 3, y + 3, x + BOX - 3, y + BOX - 3, c);
        }
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        context.drawText(font, getMessage(), x + BOX + 4, y + 1,
                active ? OttonienTheme.INK : 0x88503D29, false);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    public static final class Builder {
        private final Text message;
        private final TextRenderer textRenderer;
        private int x;
        private int y;
        private boolean checked;
        private Callback callback = (box, value) -> { };

        private Builder(Text message, TextRenderer textRenderer) {
            this.message = message;
            this.textRenderer = textRenderer;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder checked(boolean checked) {
            this.checked = checked;
            return this;
        }

        public Builder callback(Callback callback) {
            this.callback = callback;
            return this;
        }

        public InkCheckbox build() {
            return new InkCheckbox(x, y, message, textRenderer, checked, callback);
        }
    }
}
