package de.ottoextra.horses.ui;

import de.ottoextra.OttoExtra;
import de.ottoextra.horses.AnimalGender;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

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
        gender(context, renderer, raw, x, y, maxWidth, false);
    }

    static void gender(DrawContext context, TextRenderer renderer, String raw,
                       int x, int y, int maxWidth, boolean compact) {
        String label = raw == null || raw.isBlank() ? "–"
                : raw.replace("♀", "").replace("♂", "").trim();
        AnimalGender.Sex sex = AnimalGender.parse(raw);
        boolean female = sex == AnimalGender.Sex.FEMALE;
        boolean male = sex == AnimalGender.Sex.MALE;
        int color = female ? 0xFFFF8FB3 : male ? 0xFF72B7FF : 0xFFAA9D90;
        draw(context, icon(female ? "gender_mare" : male ? "gender_stallion" : "gender_unknown"),
                x, compact ? y : y - 2, compact ? 8 : 16, 16, color);
        int textOffset = compact ? 11 : 17;
        smallText(context, renderer, label, x + textOffset, y, color,
                Math.max(1, maxWidth - textOffset - 1), compact ? 0.85F : 0.75F);
    }
}
