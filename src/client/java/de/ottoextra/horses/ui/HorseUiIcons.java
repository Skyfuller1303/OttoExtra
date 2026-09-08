package de.ottoextra.horses.ui;

import de.ottoextra.OttoExtra;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Shared, resource-pack-replaceable icons for the animal record and overview. */
final class HorseUiIcons {
    static final Identifier SPEED = icon("speed");
    static final Identifier JUMP = icon("jump");

    private HorseUiIcons() {
    }

    static Identifier icon(String name) {
        return OttoExtra.id("textures/gui/horse_overview/" + name + ".png");
    }

    static void draw(DrawContext context, Identifier icon, int x, int y, int color) {
        draw(context, icon, x, y, 16, 16, color);
    }

    static void draw(DrawContext context, Identifier icon, int x, int y,
                     int size, int textureSize, int color) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon,
                x, y, 0, 0, size, size, textureSize, textureSize,
                textureSize, textureSize, color);
    }

    static void smallText(DrawContext context, TextRenderer renderer, String text,
                          int x, int y, int color, int maxWidth, float scale) {
        String clipped = renderer.trimToWidth(text, Math.max(1, (int) (maxWidth / scale)));
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        try {
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            context.drawTextWithShadow(renderer, clipped, 0, 0, color);
        } finally {
            matrices.popMatrix();
        }
    }

    static void gender(DrawContext context, TextRenderer renderer, String raw,
                       int x, int y, int maxWidth) {
        String label = raw == null || raw.isBlank() ? "–"
                : raw.replace("♀", "").replace("♂", "").trim();
        String normalized = label.toLowerCase(Locale.ROOT);
        boolean female = normalized.contains("stute") || normalized.startsWith("weib")
                || normalized.startsWith("fem") || (raw != null && raw.contains("♀"));
        boolean male = normalized.contains("hengst") || normalized.contains("wallach")
                || normalized.startsWith("männ") || normalized.startsWith("maenn")
                || normalized.equals("male") || (raw != null && raw.contains("♂"));
        int color = female ? 0xFFFF8FB3 : male ? 0xFF72B7FF : 0xFFAA9D90;
        draw(context, icon(female ? "gender_mare" : male ? "gender_stallion" : "gender_unknown"),
                x, y - 2, color);
        smallText(context, renderer, label, x + 17, y, color, Math.max(1, maxWidth - 18), 0.75F);
    }
}
