package de.ottoextra.animalinfo;

import de.ottoextra.regions.RegionActivityHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The compact ("Leiste") and detailed ("Siegel") toast. Both sit on the bevelled plate of the activity HUD
 * and are drawn with fixed pixel measures, so there is nothing to lay out.
 */
final class AnimalToastCards {

    static final int COMPACT_WIDTH = 196;
    static final int COMPACT_HEIGHT = 30;
    static final int DETAILED_WIDTH = 160;
    static final int DETAILED_HEIGHT = 62;

    private static final int OUTLINE = 0xFF141418;
    private static final int D2 = 0xFF2E2D35;
    private static final int D3 = 0xFF3B3A45;
    private static final int NAME = 0xFFF0EBE0;
    private static final int OCHRE = 0xFFD6C194;
    private static final int SUB = 0xFFA9A6B0;
    /** Readable variant of the draft's D4 for the label below the bar; D4 vanishes on the translucent plate. */
    private static final int LABEL = 0xFF8F8C96;
    private static final float INACTIVE_ALPHA = 0.45f;
    private static final int ICON = 9;

    private AnimalToastCards() {
    }

    static int width(AnimalToastStyle style) {
        return style == AnimalToastStyle.COMPACT ? COMPACT_WIDTH : DETAILED_WIDTH;
    }

    static int height(AnimalToastStyle style) {
        return style == AnimalToastStyle.COMPACT ? COMPACT_HEIGHT : DETAILED_HEIGHT;
    }

    static void draw(DrawContext ctx, TextRenderer font, int x, int y, AnimalToastStyle style,
                     AnimalInfoSnapshot value, AnimalEntity entity, String animalType, float alpha) {
        AnimalInfoOverlay.StatusView view =
                AnimalInfoOverlay.statusView(entity, value, System.currentTimeMillis());
        AnimalToastModel model = AnimalToastModel.of(view.status(), value, view.species());
        if (style == AnimalToastStyle.COMPACT) {
            compact(ctx, font, x, y, value, entity, animalType, view, model, alpha);
        } else {
            detailed(ctx, font, x, y, value, entity, animalType, view, model, alpha);
        }
    }

    private static void compact(DrawContext ctx, TextRenderer font, int x, int y,
                                AnimalInfoSnapshot value, AnimalEntity entity, String animalType,
                                AnimalInfoOverlay.StatusView view, AnimalToastModel model, float alpha) {
        int w = COMPACT_WIDTH;
        int h = COMPACT_HEIGHT;
        int[] ramp = AnimalToastModel.ramp(model.status().kind());
        RegionActivityHud.drawPanel(ctx, x, y, w, h, alpha);
        ctx.fill(x + 2, y + 2, x + 5, y + h - 2, a(ramp[1], alpha));
        ctx.fill(x + 2, y + 2, x + 5, y + 3, a(ramp[2], alpha));
        portrait(ctx, x + 9, y + 7, value, entity, animalType, alpha);

        int rightX = x + w - 52;
        int extras = extraCount(value, entity);
        int nameLimit = rightX - 12 * extras - 4 - (x + 31) - ICON - 3;
        String name = font.trimToWidth(value.animalName(), Math.max(10, nameLimit));
        ctx.drawTextWithShadow(font, name, x + 31, y + 6, a(OCHRE, alpha));
        gender(ctx, value, x + 31 + font.getWidth(name) + 3, y + 6, alpha);
        Text label = AnimalInfoOverlay.statusLabel(model.status());
        int textX = x + 31;
        String statusIcon = AnimalToastModel.statusIcon(model.status().kind());
        if (statusIcon != null) {
            icon(ctx, AnimalInfoOverlay.ownIcon(statusIcon), textX, y + 16, 1f, alpha);
            textX += 11;
        }
        ctx.drawTextWithShadow(font, font.trimToWidth(label.getString(), Math.max(10, rightX - 4 - textX)),
                textX, y + 17, a(ramp[2], alpha));

        if (model.hasMast()) {
            icon(ctx, AnimalInfoOverlay.ICON_MAST, rightX, y + 5, 1f, alpha);
            ctx.drawTextWithShadow(font, model.mast() + "/" + model.mastNeed(), rightX + 12, y + 6,
                    a(NAME, alpha));
            bar(ctx, rightX, y + 17, 46, 4, model.mast() / (float) model.mastNeed(),
                    AnimalToastModel.mastRamp(model.mastEnough()), alpha);
        }
        extras(ctx, value, entity, rightX - 12, y + 5, -11, alpha);
    }

    private static void detailed(DrawContext ctx, TextRenderer font, int x, int y,
                                 AnimalInfoSnapshot value, AnimalEntity entity, String animalType,
                                 AnimalInfoOverlay.StatusView view, AnimalToastModel model, float alpha) {
        int w = DETAILED_WIDTH;
        int h = DETAILED_HEIGHT;
        int[] ramp = AnimalToastModel.ramp(model.status().kind());
        RegionActivityHud.drawPanel(ctx, x, y, w, h, alpha);
        ctx.fill(x + 3, y + 3, x + w - 3, y + 15, a(ramp[0], alpha));
        ctx.fill(x + 3, y + 3, x + w - 3, y + 4, a(ramp[1], alpha));
        Text status = AnimalInfoOverlay.statusLabel(model.status());
        String statusIcon = AnimalToastModel.statusIcon(model.status().kind());
        int lead = statusIcon == null ? 0 : ICON + 2;
        String fitted = font.trimToWidth(status.getString(), w - 14 - lead);
        int groupX = x + (w - lead - font.getWidth(fitted)) / 2;
        if (statusIcon != null) {
            icon(ctx, AnimalInfoOverlay.ownIcon(statusIcon), groupX, y + 4, 1f, alpha);
        }
        ctx.drawTextWithShadow(font, fitted, groupX + lead, y + 5, a(NAME, alpha));

        portrait(ctx, x + 8, y + 22, value, entity, animalType, alpha);
        int extras = extraCount(value, entity);
        int nameLimit = w - 31 - 9 - 11 * extras - ICON - 3;
        String name = font.trimToWidth(value.animalName(), Math.max(10, nameLimit));
        ctx.drawTextWithShadow(font, name, x + 31, y + 21, a(OCHRE, alpha));
        gender(ctx, value, x + 31 + font.getWidth(name) + 3, y + 21, alpha);
        String term = AnimalToastModel.genderTerm(value.gender());
        if (!term.isEmpty()) {
            ctx.drawTextWithShadow(font, term, x + 31, y + 31, a(SUB, alpha));
        }
        extras(ctx, value, entity, x + w - 9 - 11 * extras, y + 20, 11, alpha);

        int barWidth = w - 40;
        Text caption;
        switch (model.bar()) {
            case COOLDOWN -> {
                bar(ctx, x + 31, y + 44, barWidth, 5, model.barFraction(), ramp, alpha);
                caption = Text.translatable("ottoextra.animalinfo.card.breeding");
            }
            case GROWTH -> {
                bar(ctx, x + 31, y + 44, barWidth, 5, model.barFraction(), ramp, alpha);
                caption = Text.translatable("ottoextra.animalinfo.card.growth");
            }
            case MAST -> {
                bar(ctx, x + 31, y + 44, barWidth, 5, model.barFraction(),
                        AnimalToastModel.mastRamp(model.mastEnough()), alpha);
                caption = Text.translatable("ottoextra.animalinfo.card.mast", model.mast(), model.mastNeed());
            }
            default -> caption = null;
        }
        if (caption != null) {
            ctx.drawText(font, caption, x + 31, y + 51, a(LABEL, alpha), false);
        }
        if (view.ageMs() != null && caption != null) {
            // Small and right-aligned on the caption row; there is room because the caption is short.
            Text age = AnimalInfoOverlay.ageLabel(view.ageMs());
            int ageWidth = Math.round(font.getWidth(age) * 0.75f);
            if (font.getWidth(caption) + 8 + ageWidth <= barWidth) {
                var matrices = ctx.getMatrices();
                matrices.pushMatrix();
                matrices.translate(x + 31 + barWidth - ageWidth, y + 52);
                matrices.scale(0.75f, 0.75f);
                ctx.drawText(font, age, 0, 0, a(LABEL, alpha), false);
                matrices.popMatrix();
            }
        }
    }

    /** Framed portrait slot: 16x16 content, dark inset, outline. */
    private static void portrait(DrawContext ctx, int x, int y, AnimalInfoSnapshot value,
                                 AnimalEntity entity, String animalType, float alpha) {
        ctx.fill(x - 2, y - 2, x + 18, y + 18, a(OUTLINE, alpha));
        ctx.fill(x - 1, y - 1, x + 17, y + 17, a(D3, alpha));
        AnimalInfoOverlay.drawAnimal(ctx, x, y, 16, entity, value, animalType, alpha);
    }

    private static void gender(DrawContext ctx, AnimalInfoSnapshot value, int x, int y, float alpha) {
        Identifier icon = AnimalInfoOverlay.genderIcon(value.gender());
        if (icon == AnimalInfoOverlay.ICON_FEMALE) {
            icon(ctx, AnimalInfoOverlay.ownIcon("weiblich"), x, y, 1f, alpha);
        } else if (icon == AnimalInfoOverlay.ICON_MALE) {
            icon(ctx, AnimalInfoOverlay.ownIcon("maennlich"), x, y, 1f, alpha);
        }
    }

    private static int extraCount(AnimalInfoSnapshot value, AnimalEntity entity) {
        return AnimalInfoOverlay.extraStatus(value, entity) == null ? 0 : 1;
    }

    /** Milkable / shearable icon without text: ochre when ready, dimmed while waiting. */
    private static void extras(DrawContext ctx, AnimalInfoSnapshot value, AnimalEntity entity,
                               int x, int y, int step, float alpha) {
        AnimalInfoOverlay.ExtraStatus extra = AnimalInfoOverlay.extraStatus(value, entity);
        if (extra != null) {
            icon(ctx, extra.icon(), x, y, extra.positive() ? 1f : INACTIVE_ALPHA, alpha);
        }
    }

    /** Draws a 9x9 icon in its own colours; {@code strength} scales its alpha (inactive icons). */
    private static void icon(DrawContext ctx, Identifier texture, int x, int y, float strength, float alpha) {
        AnimalInfoOverlay.drawOwnIcon(ctx, texture, x, y, a(0xFFFFFFFF, alpha * strength));
    }

    private static void bar(DrawContext ctx, int x, int y, int w, int h, float fraction,
                            int[] ramp, float alpha) {
        ctx.fill(x, y, x + w, y + h, a(OUTLINE, alpha));
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, a(D2, alpha));
        int fill = (int) ((w - 2) * Math.max(0f, Math.min(1f, fraction)));
        if (fill > 0) {
            ctx.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, a(ramp[1], alpha));
            ctx.fill(x + 1, y + 1, x + 1 + fill, y + 2, a(ramp[2], alpha));
        }
    }

    private static int a(int argb, float alpha) {
        return AnimalInfoOverlay.withAlpha(argb, alpha);
    }
}
