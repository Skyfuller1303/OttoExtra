package de.ottoextra.horses.ui;

import de.ottoextra.OttoExtra;
import de.ottoextra.animalinfo.AnimalBookStatus;
import de.ottoextra.horses.AnimalGender;
import de.ottoextra.horses.FarmSymbols;
import de.ottoextra.ui.theme.OttonienTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Book look of the animal register and the animal overview: slate-blue cover with two parchment pages, the
 * same style as the resource book of OttoExtra Resources (adapted copy, the core cannot depend on the
 * addon). Colours and geometry follow docs/design/generator/gen_tierbuch.py. No text in the book has a
 * shadow: all text goes through {@link DrawContext#drawText} with {@code shadow = false}, and the book uses
 * its own button and field widgets for the same reason.
 */
final class AnimalBook {
    // Parchment and ink of the sketches.
    static final int PA = 0xFFC9A97C;
    static final int PA_L = 0xFFD8BC92;
    static final int PA_D = 0xFFB08F63;
    static final int PA_S = 0xFFBF9E70;
    static final int INK = 0xFF35281C;
    static final int INK_M = 0xFF6E5A44;
    static final int INK_F = 0xFF8E785E;
    static final int TERRA = 0xFF9E4B32;
    static final int BRASS = 0xFF866C36;
    static final int OK_INK = 0xFF3F6B2C;
    static final int WARN_INK = 0xFF8A6410;
    static final int SOON_INK = 0xFFA0491E;
    static final int RED_INK = 0xFF9A2E2A;
    static final int YOUNG_INK = 0xFF2E6E78;
    static final int LOVE_INK = 0xFF9A3A8C;
    static final int FEMALE = 0xFFB0466E;
    static final int MALE = 0xFF3F6E9E;
    // Slate buttons.
    static final int SLATE = 0xFF2E3A4A;
    static final int SLATE_T = 0xFFE8E2D6;
    static final int PRIMARY = 0xFF35552A;
    static final int DANGER = 0xFF6A2E26;
    private static final int HOVER_OUTLINE = 0xFFE2A650;

    /** Scale of the small caption font of the sketches (3 of 4 pixels). */
    static final float SMALL = 0.75f;
    /** Book size of the sketches without the margin around the cover. */
    static final int BOOK_W = 430;
    static final int BOOK_H = 232;

    private static final int SPINE = 10;
    private static final int EDGE = 5;

    private static final Identifier ICON_FEMALE = OttoExtra.id("textures/gui/animalinfo/weiblich.png");
    private static final Identifier ICON_MALE = OttoExtra.id("textures/gui/animalinfo/maennlich.png");
    private static final Map<String, Identifier> SYMBOLS = new HashMap<>();

    /** Parchment area inside the cover. */
    record Page(int x, int y, int w, int h) { }

    /** The two pages of a double-page book. */
    record Spread(Page left, Page right) { }

    private AnimalBook() { }

    /**
     * Double page of at most maxW x maxH centred on the screen: a narrow left page, a gap with the
     * cover showing through, and a wide right page.
     */
    static Spread spread(int screenWidth, int screenHeight) {
        int w = Math.min(BOOK_W, screenWidth - 8);
        int h = Math.min(BOOK_H, screenHeight - 8);
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;
        Page whole = new Page(x + SPINE, y + EDGE, w - SPINE - EDGE, h - 2 * EDGE);
        int gap = 4;
        int leftW = Math.min(141, whole.w() * 34 / 100);
        Page left = new Page(whole.x(), whole.y(), leftW, whole.h());
        Page right = new Page(whole.x() + leftW + gap, whole.y(), whole.w() - leftW - gap, whole.h());
        return new Spread(left, right);
    }

    /** Dimmed background, cover and two parchment pages. */
    static void drawSpread(DrawContext context, int screenWidth, int screenHeight, Spread spread) {
        context.fill(0, 0, screenWidth, screenHeight, 0xB0101010);
        Page l = spread.left();
        Page r = spread.right();
        cover(context, l.x() - SPINE, l.y() - EDGE, r.x() + r.w() + EDGE - (l.x() - SPINE), l.h() + 2 * EDGE);
        page(context, l, true);
        page(context, r, false);
    }

    /** Parchment page of the sketch: dark edge, light top line, shaded strip at the spine side. */
    private static void page(DrawContext context, Page p, boolean left) {
        rect(context, p.x(), p.y(), p.x() + p.w(), p.y() + p.h(), PA_D);
        rect(context, p.x() + 1, p.y() + 1, p.x() + p.w() - 1, p.y() + p.h() - 1, PA);
        rect(context, p.x() + 1, p.y() + 1, p.x() + p.w() - 1, p.y() + 2, PA_L);
        if (left) rect(context, p.x() + p.w() - 4, p.y() + 1, p.x() + p.w() - 1, p.y() + p.h() - 1, PA_S);
        else rect(context, p.x() + 1, p.y() + 1, p.x() + 4, p.y() + p.h() - 1, PA_S);
    }

    /** Book cover ramp, dark to light (grey-blue leather). */
    private static final int[] COVER = {0xFF141A21, 0xFF222B36, 0xFF313C49, 0xFF3B4756, 0xFF4F5C6D};
    /** Brass studs, same tones as the theme's gold ramp. */
    private static final int[] STUDS = {0xFF47391D, 0xFF866C36, 0xFFB58D36, 0xFFDBB451};

    /** Cover with leather edge, spine with bands, inner lines and two studs. */
    static void cover(DrawContext ctx, int bx, int by, int bw, int bh) {
        int[] ramp = COVER;
        OttonienTheme.rect(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, ramp[0]);
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + bh, ramp[2]);
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + 1, ramp[3]);
        OttonienTheme.rect(ctx, bx, by, bx + 1, by + bh, ramp[3]);
        OttonienTheme.rect(ctx, bx, by + bh - 1, bx + bw, by + bh, ramp[1]);
        OttonienTheme.rect(ctx, bx, by, bx + 7, by + bh, ramp[1]);
        for (int i = by + 10; i < by + bh - 8; i += 22) {
            OttonienTheme.rect(ctx, bx + 1, i, bx + 6, i + 2, ramp[3]);
            OttonienTheme.rect(ctx, bx + 1, i + 2, bx + 6, i + 3, ramp[0]);
        }
        OttonienTheme.rect(ctx, bx + 9, by + 2, bx + bw - 2, by + 3, ramp[1]);
        OttonienTheme.rect(ctx, bx + 9, by + bh - 3, bx + bw - 2, by + bh - 2, ramp[3]);
        int[][] studs = {{bx + bw - 6, by + 3}, {bx + bw - 6, by + bh - 6}};
        for (int[] s : studs) {
            OttonienTheme.rect(ctx, s[0], s[1], s[0] + 3, s[1] + 3, STUDS[1]);
            OttonienTheme.rect(ctx, s[0], s[1], s[0] + 1, s[1] + 1, STUDS[3]);
            OttonienTheme.rect(ctx, s[0] + 2, s[1] + 2, s[0] + 3, s[1] + 3, STUDS[0]);
        }
    }

    // ---- drawing helpers -------------------------------------------------------------

    static void rect(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        context.fill(x0, y0, x1, y1, color);
    }

    /** One-pixel line from x0 to x1. */
    static void rule(DrawContext context, int x0, int x1, int y, int color) {
        rect(context, x0, y, x1, y + 1, color);
    }

    /** Colour times a factor per channel, clamped, opaque. */
    static int shade(int color, double factor) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static void text(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        context.drawText(font, text, x, y, color, false);
    }

    static void text(DrawContext context, TextRenderer font, Text text, int x, int y, int color) {
        context.drawText(font, text, x, y, color, false);
    }

    /** Caption text at three quarters of the normal size. */
    static void small(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float) x, (float) y);
        matrices.scale(SMALL, SMALL);
        context.drawText(font, text, 0, 0, color, false);
        matrices.popMatrix();
    }

    static void small(DrawContext context, TextRenderer font, Text text, int x, int y, int color) {
        small(context, font, text.getString(), x, y, color);
    }

    /** Width of a caption in GUI pixels. */
    static int smallWidth(TextRenderer font, String text) {
        return Math.round(font.getWidth(text) * SMALL);
    }

    /** Caption right-aligned to {@code right}. */
    static void smallRight(DrawContext context, TextRenderer font, String text, int right, int y, int color) {
        small(context, font, text, right - smallWidth(font, text), y, color);
    }

    /** Wrapped caption lines, {@code step} pixels apart, at most {@code maxLines}; returns the y below. */
    static int smallWrapped(DrawContext context, TextRenderer font, Text text, int x, int y, int width, int step,
                            int color, int maxLines) {
        List<OrderedText> lines = font.wrapLines(text, Math.round(width / SMALL));
        int count = 0;
        for (OrderedText line : lines) {
            if (count++ >= maxLines) break;
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate((float) x, (float) y);
            matrices.scale(SMALL, SMALL);
            context.drawText(font, line, 0, 0, color, false);
            matrices.popMatrix();
            y += step;
        }
        return y;
    }

    /** Text shortened with an ellipsis to fit {@code maxWidth}. */
    static String clip(TextRenderer font, String text, int maxWidth) {
        if (text == null) return "";
        if (font.getWidth(text) <= maxWidth) return text;
        return font.trimToWidth(text, Math.max(0, maxWidth - font.getWidth("…"))) + "…";
    }

    /** Caption shortened with an ellipsis to fit {@code maxWidth} GUI pixels at the small size. */
    static String clipSmall(TextRenderer font, String text, int maxWidth) {
        return clip(font, text, Math.round(maxWidth / SMALL));
    }

    /** Header of a page: terracotta title, optional small caption, rule below. */
    static void header(DrawContext context, TextRenderer font, Page page, int inset, Text title, String caption,
                       int titleMax) {
        int x = page.x() + inset;
        String shown = clip(font, title.getString(), titleMax);
        text(context, font, shown, x, page.y() + 7, TERRA);
        if (caption != null && !caption.isEmpty()) {
            int cx = x + 2 + font.getWidth(shown);
            small(context, font, clipSmall(font, caption, Math.max(0, x + titleMax - cx)), cx, page.y() + 9, INK_M);
        }
        rule(context, x, page.x() + page.w() - 8, page.y() + 20, PA_D);
    }

    /** Slate plate of the sketches: dark outline, fill, light top and dark bottom line. */
    static void slate(DrawContext context, int x, int y, int w, int h, int fill, boolean hover) {
        rect(context, x, y, x + w, y + h, hover ? HOVER_OUTLINE : COVER[0]);
        rect(context, x + 1, y + 1, x + w - 1, y + h - 1, fill);
        rect(context, x + 1, y + 1, x + w - 1, y + 2, shade(fill, 1.3));
        rect(context, x + 1, y + h - 2, x + w - 1, y + h - 1, shade(fill, 0.7));
    }

    /** Selection tile of the sketches: terracotta edge and light fill when selected. */
    static void chip(DrawContext context, int x, int y, int w, int h, boolean selected, boolean hover) {
        rect(context, x, y, x + w, y + h, selected ? TERRA : hover ? BRASS : PA_D);
        rect(context, x + 1, y + 1, x + w - 1, y + h - 1, selected ? PA_L : PA);
    }

    /** Button width: the sketch width, or wider when the label needs it. */
    static int buttonWidth(TextRenderer font, Text label, int sketchWidth) {
        return Math.max(sketchWidth, font.getWidth(label) + 10);
    }

    /** Horizontal bar: dark track, coloured fill. */
    static void bar(DrawContext context, int x0, int y, int x1, int h, float fraction, int color) {
        rect(context, x0, y, x1, y + h, PA_D);
        int fill = Math.round((x1 - x0) * Math.max(0f, Math.min(1f, fraction)));
        if (fill > 0) rect(context, x0, y, x0 + fill, y + h, color);
    }

    /** Feeding bar with a notch at the breeding requirement. */
    static void mastBar(DrawContext context, int x0, int y, int x1, int h, AnimalBookStatus.View view) {
        int scale = Math.max(1, view.mastScale());
        int ink = view.mastEnough() ? OK_INK : view.hasMast() ? WARN_INK : INK_F;
        bar(context, x0, y, x1, h, view.mast() < 0 ? 0f : view.mast() / (float) scale, ink);
        if (view.mastNeed() > 0) {
            int nx = x0 + Math.round((x1 - x0) * Math.min(1f, view.mastNeed() / (float) scale));
            nx = Math.min(x1 - 1, nx);
            rect(context, nx, y - 2, nx + 1, y + h + 2, INK);
        }
    }

    /** Ink of a status tone on parchment. */
    static int toneInk(AnimalBookStatus.Tone tone) {
        return switch (tone) {
            case READY -> OK_INK;
            case WAITING -> WARN_INK;
            case SOON -> SOON_INK;
            case NEED_MAST -> RED_INK;
            case YOUNG -> YOUNG_INK;
            case LOVE -> LOVE_INK;
            case UNKNOWN -> INK_F;
        };
    }

    /** Ink of a gender value. */
    static int genderInk(String gender) {
        return switch (AnimalGender.parse(gender)) {
            case FEMALE -> FEMALE;
            case MALE -> MALE;
            case UNKNOWN -> INK_F;
        };
    }

    /** 9x9 gender icon of the animal info set; nothing for an unknown gender. Returns the width used. */
    static int genderIcon(DrawContext context, String gender, int x, int y) {
        Identifier icon = switch (AnimalGender.parse(gender)) {
            case FEMALE -> ICON_FEMALE;
            case MALE -> ICON_MALE;
            case UNKNOWN -> null;
        };
        if (icon == null) return 0;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, 9, 9, 9, 9, 9, 9);
        return 9;
    }

    // ---- farms and previews ------------------------------------------------------------

    /**
     * Key of a farm in the local book: the register API's {@code nameKey} when the farm is one of the
     * player's farms, otherwise a key derived from the name (farms seen only on shared animals).
     */
    static String farmKey(String farmName) {
        if (farmName == null || farmName.isBlank()) return "";
        for (de.ottoextra.api.model.AnimalBreedingFarm farm : de.ottoextra.horses.HorseRegistry.farms()) {
            if (farm.name().equalsIgnoreCase(farmName.trim())) return farm.nameKey();
        }
        return "name:" + farmName.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** Symbol of a farm by name. */
    static String farmSymbol(String farmName) {
        return de.ottoextra.horses.AnimalBookStore.current().symbol(farmKey(farmName));
    }

    /**
     * Entity to render for a register record: the loaded animal when it is in the world, otherwise a
     * cached stand-in of its kind (a horse stand-in takes the stored coat).
     */
    static net.minecraft.entity.passive.AnimalEntity preview(de.ottoextra.api.model.AnimalBreedingHorse horse,
            Map<HorseOverviewView.AnimalKind, net.minecraft.entity.passive.AnimalEntity> cache) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        net.minecraft.entity.Entity loaded = client.world.getEntity(horse.uuid());
        if (loaded instanceof net.minecraft.entity.passive.AnimalEntity animal
                && de.ottoextra.horses.RegistryAnimals.supports(animal)) {
            return animal;
        }
        HorseOverviewView.AnimalKind kind = HorseOverviewView.kind(horse);
        net.minecraft.entity.passive.AnimalEntity preview = cache.get(kind);
        if (preview == null || preview.getEntityWorld() != client.world) {
            preview = switch (kind) {
                case HORSES -> new net.minecraft.entity.passive.HorseEntity(
                        net.minecraft.entity.EntityType.HORSE, client.world);
                case DONKEYS -> new net.minecraft.entity.passive.DonkeyEntity(
                        net.minecraft.entity.EntityType.DONKEY, client.world);
                case COWS -> new net.minecraft.entity.passive.CowEntity(
                        net.minecraft.entity.EntityType.COW, client.world);
                case CHICKENS -> new net.minecraft.entity.passive.ChickenEntity(
                        net.minecraft.entity.EntityType.CHICKEN, client.world);
                case PIGS -> new net.minecraft.entity.passive.PigEntity(
                        net.minecraft.entity.EntityType.PIG, client.world);
                case GOATS -> new net.minecraft.entity.passive.GoatEntity(
                        net.minecraft.entity.EntityType.GOAT, client.world);
                case SHEEP -> new net.minecraft.entity.passive.SheepEntity(
                        net.minecraft.entity.EntityType.SHEEP, client.world);
                default -> null;
            };
            if (preview != null) cache.put(kind, preview);
        }
        if (preview instanceof net.minecraft.entity.passive.HorseEntity entity) {
            HorseAppearance.from(horse).applyTo(entity);
        }
        return preview;
    }

    /**
     * Three-quarter model filling the box, without name tag (same view as the animal info toast); a renderer
     * replaced by another mod must not break the book. {@code scale} is kept for the callers and unused.
     */
    static void model(DrawContext context, net.minecraft.entity.passive.AnimalEntity animal, int left, int top,
                      int right, int bottom, int scale) {
        if (animal == null) return;
        try {
            de.ottoextra.animalinfo.AnimalPreview.draw(context, left, top, right, bottom, animal);
        } catch (Throwable ignored) {
            // Keep the book usable without the model.
        }
    }

    // ---- symbols -----------------------------------------------------------------------

    static Identifier symbolTexture(String key) {
        return SYMBOLS.computeIfAbsent(FarmSymbols.canonical(key),
                known -> OttoExtra.id("textures/gui/symbols/" + known + ".png"));
    }

    /** Draws a farm symbol scaled to {@code size} GUI pixels (16 = 1x). */
    static void symbol(DrawContext context, String key, int x, int y, int size) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, symbolTexture(key), x, y, 0f, 0f, size, size, 16, 16,
                16, 16);
    }

    /** Swatch colour of a colour name for the colour picker. */
    static int swatch(String color) {
        return switch (color) {
            case "rot" -> 0xFFB5482A;
            case "gruen" -> 0xFF4C8A3A;
            case "gold" -> 0xFFA2802E;
            case "violett" -> 0xFF7A4FA3;
            case "grau" -> 0xFF5B5E6B;
            default -> 0xFF3F7AB8;
        };
    }

    /**
     * Symbol picker (adapted from the resource book): the twelve shapes in two rows of six, the six colour
     * swatches to the right of the first row. (x, y) is the top-left corner of the first shape box; the
     * labels sit 8 px above it.
     */
    static final class SymbolPicker {
        static final int BOX = 18;
        static final int STEP = 20;
        static final int PER_ROW = 6;
        static final int SWATCH_STEP = 14;
        static final int HEIGHT = (FarmSymbols.SHAPES.size() + PER_ROW - 1) / PER_ROW * STEP - (STEP - BOX);

        private SymbolPicker() { }

        private static int shapeX(int x, int index) { return x + (index % PER_ROW) * STEP; }
        private static int shapeY(int y, int index) { return y + (index / PER_ROW) * STEP; }
        private static int swatchX(int x, int index) { return x + PER_ROW * STEP + 8 + index * SWATCH_STEP; }

        /** Symbol key after a click, or null when the click missed the picker. */
        static String pick(int x, int y, double mouseX, double mouseY, String current) {
            for (int index = 0; index < FarmSymbols.SHAPES.size(); index++) {
                int sx = shapeX(x, index);
                int sy = shapeY(y, index);
                if (mouseX >= sx && mouseX < sx + BOX && mouseY >= sy && mouseY < sy + BOX) {
                    return FarmSymbols.withShape(current, FarmSymbols.SHAPES.get(index));
                }
            }
            for (int index = 0; index < FarmSymbols.COLORS.size(); index++) {
                int sx = swatchX(x, index);
                if (mouseX >= sx - 1 && mouseX < sx + 12 && mouseY >= y + 2 && mouseY < y + 15) {
                    return FarmSymbols.withColor(current, FarmSymbols.COLORS.get(index));
                }
            }
            return null;
        }

        static void draw(DrawContext context, TextRenderer font, int x, int y, String key) {
            String shape = FarmSymbols.shape(key);
            String color = FarmSymbols.color(key);
            small(context, font, Text.translatable("ottoextra.horses.book.symbol"), x, y - 8, INK_M);
            small(context, font, Text.translatable("ottoextra.horses.book.color"), swatchX(x, 0) - 1, y - 8, INK_M);
            for (int index = 0; index < FarmSymbols.SHAPES.size(); index++) {
                String each = FarmSymbols.SHAPES.get(index);
                boolean on = each.equals(shape);
                int sx = shapeX(x, index);
                int sy = shapeY(y, index);
                rect(context, sx, sy, sx + BOX, sy + BOX, on ? TERRA : PA_D);
                rect(context, sx + 1, sy + 1, sx + BOX - 1, sy + BOX - 1, on ? PA_L : PA);
                symbol(context, FarmSymbols.key(each, color), sx + 1, sy + 1, 16);
            }
            for (int index = 0; index < FarmSymbols.COLORS.size(); index++) {
                String each = FarmSymbols.COLORS.get(index);
                int sx = swatchX(x, index);
                int tone = swatch(each);
                rect(context, sx - 1, y + 2, sx + 12, y + 15, each.equals(color) ? TERRA : PA);
                rect(context, sx, y + 3, sx + 11, y + 14, tone);
                rect(context, sx, y + 3, sx + 11, y + 4, shade(tone, 1.3));
            }
        }
    }

    // ---- widgets -----------------------------------------------------------------------

    enum Tone { SLATE, PRIMARY, DANGER }

    /** Slate button of the sketches: 14 px high, light label, no text shadow. */
    static final class Button extends ButtonWidget {
        private final Tone tone;

        Button(int x, int y, int width, net.minecraft.text.Text message, Tone tone, PressAction onPress) {
            super(x, y, width, 14, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.tone = tone;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            boolean hover = active && (isHovered() || isFocused());
            int fill = switch (tone) {
                case PRIMARY -> PRIMARY;
                case DANGER -> DANGER;
                default -> SLATE;
            };
            slate(context, getX(), getY(), getWidth(), getHeight(), active ? fill : shade(fill, 0.8), hover);
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            net.minecraft.text.Text label = getMessage();
            int color = active ? SLATE_T : 0xFF8A8F98;
            context.drawText(font, label, getX() + (getWidth() - font.getWidth(label)) / 2,
                    getY() + (getHeight() - 8) / 2, color, false);
        }
    }

    /**
     * Text field of the sketches: no background, an underline (terracotta while focused), ink colours and no
     * text shadow. Vanilla {@code TextFieldWidget} draws text, suggestion and cursor with its
     * {@code textShadow} flag, which is switched off here; only its placeholder is always drawn with a
     * shadow, so the field never hands the hint to vanilla and draws it itself.
     */
    static final class Field extends TextFieldWidget {
        private final TextRenderer font;
        private final Text hint;

        Field(TextRenderer font, int x, int y, int width, Text hint) {
            super(font, x + 2, y, Math.max(8, width - 4), 13, hint);
            this.font = font;
            this.hint = hint;
            setDrawsBackground(false);
            setTextShadow(false);
            setEditableColor(INK);
            setUneditableColor(INK_M);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            if (!isVisible()) return;
            rect(context, getX() - 2, getY() + 12, getX() + getWidth() + 2, getY() + 13,
                    isFocused() ? TERRA : INK_M);
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0f, 2f);
            super.renderWidget(context, mouseX, mouseY, deltaTicks);
            if (getText().isEmpty() && !isFocused()) {
                context.drawText(font, clip(font, hint.getString(), getWidth()), getX(), getY(), INK_F, false);
            }
            matrices.popMatrix();
        }
    }
}
