package de.ottoextra.animalinfo;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.horses.AnimalGender;
import de.ottoextra.ui.hudlayout.TopCenterStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnimalInfoOverlay {

    public enum Part {
        MODEL, TITLE, GENDER_ICON, GENDER_TEXT, BREEDING, MAST, TIMER, EXTRA,
        JUMP_HEIGHT, SPEED, STATUS
    }

    public record PartBounds(Part part, int x, int y, int width, int height) {
    }

    private record Layout(int width, int height, List<PartBounds> parts) {
    }

    record ExtraStatus(Identifier icon, String label, boolean positive) {
    }

    /** Slot of the toast in the shared top-centre column (below boss bars and the Werkstatt bar). */
    public static final String STACK_ID = "ottoextra:animal_toast";
    private static final long FADE_IN_MS = 140L;
    private static final long FADE_OUT_MS = 180L;

    private static final int COL_BG = 0xF21A1F26;
    private static final int COL_BORDER = 0xFF4B545E;
    private static final int COL_TEXT = 0xFFF4F7FA;
    static final int COL_MUTED = 0xFFB8C0C8;
    private static final int COL_GREEN = 0xFF58C46B;
    private static final int COL_RED = 0xFFE05A5A;
    private static final int COL_AMBER = 0xFFE2B45A;
    private static final int COL_YOUNG = 0xFF55DDFF;
    static final int COL_FEMALE = 0xFFFF79B8;
    static final int COL_MALE = 0xFF68B8FF;

    private static final StyleSpriteSource.Font MENU_FONT =
            new StyleSpriteSource.Font(Identifier.of("mattis", "menu"));

    private static final char ICON_SHEEP = '\uE200';
    private static final char ICON_COW = '\uE201';
    private static final char ICON_PIG = '\uE202';
    private static final char ICON_HORSE = '\uE203';
    private static final char ICON_DONKEY = '\uE204';
    private static final char ICON_GOAT = '\uE205';
    private static final char ICON_CHICKEN = '\uE206';
    private static final char ICON_WOLF = '\uE207';
    private static final char ICON_ANIMAL_UNKNOWN = '\uE400';
    private static final int ICON_TEXTURE_SIZE = 16;
    private static final int ICON_DISPLAY_SIZE = 8;
    private static final int STATUS_TEXT_X = ICON_DISPLAY_SIZE + 4;
    private static final Identifier ICON_GENDER = statusTexture("gender_unknown");
    static final Identifier ICON_FEMALE = statusTexture("gender_mare");
    static final Identifier ICON_MALE = statusTexture("gender_stallion");
    static final Identifier ICON_MAST = ownIcon("mast");
    private static final Identifier OWN_FEMALE = ownIcon("weiblich");
    private static final Identifier OWN_MALE = ownIcon("maennlich");
    private static final Identifier ICON_SHEARABLE = ownIcon("scherbar");
    private static final Identifier ICON_MILKABLE = ownIcon("melkbar");
    private static final Identifier ICON_BREED_READY = statusTexture("breed_ready");
    private static final Identifier ICON_JUMP = statusTexture("jump");
    private static final Identifier ICON_SPEED = statusTexture("speed");

    private static volatile AnimalInfoSnapshot snapshot;
    private static volatile AnimalEntity modelEntity;
    private static volatile long shownAt;
    private static volatile long hiddenAt;
    private static volatile boolean nextShowInstant;

    private AnimalInfoOverlay() {
    }

    public static void show(AnimalInfoSnapshot value, AnimalEntity entity) {
        if (value == null || !value.complete()) {
            return;
        }
        if (!value.equals(snapshot) || entity != modelEntity) {
            snapshot = value;
            modelEntity = entity;
            long now = Util.getMeasuringTimeMs();
            shownAt = nextShowInstant ? now - FADE_IN_MS : now;
        }
        hiddenAt = 0L;
        nextShowInstant = false;
    }

    public static void hide() {
        if (snapshot != null && hiddenAt == 0L) {
            hiddenAt = Util.getMeasuringTimeMs();
        }
    }

    /** Entfernt alte Tierdaten beim direkten Zielwechsel ohne neue Einfahranimation. */
    public static void switchTarget() {
        snapshot = null;
        modelEntity = null;
        shownAt = 0L;
        hiddenAt = 0L;
        nextShowInstant = true;
        TopCenterStack.release(STACK_ID);
    }

    public static void clear() {
        snapshot = null;
        modelEntity = null;
        shownAt = 0L;
        hiddenAt = 0L;
        nextShowInstant = false;
        TopCenterStack.release(STACK_ID);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        AnimalInfoSnapshot value = snapshot;
        if (value == null) {
            TopCenterStack.release(STACK_ID);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || client.options.hudHidden) {
            TopCenterStack.release(STACK_ID);
            return;
        }

        String animalType = animalType(modelEntity, value);
        OttoExtraConfig.AnimalInfoLayout config =
                OttoExtraConfig.active().animalInfo.layout(animalType);
        if (!config.enabled) {
            TopCenterStack.release(STACK_ID);
            return;
        }
        AnimalToastStyle style = OttoExtraConfig.active().animalInfo.toastStyle();
        Layout layout = style == AnimalToastStyle.CUSTOM
                ? layout(client.textRenderer, value, modelEntity, config)
                : new Layout(AnimalToastCards.width(style), AnimalToastCards.height(style), List.of());
        if (style == AnimalToastStyle.CUSTOM && layout.parts().isEmpty()) {
            TopCenterStack.release(STACK_ID);
            return;
        }

        long now = Util.getMeasuringTimeMs();
        float alpha;
        if (hiddenAt > 0L) {
            long elapsed = now - hiddenAt;
            if (elapsed >= FADE_OUT_MS) {
                clear();
                return;
            }
            alpha = 1.0f - elapsed / (float) FADE_OUT_MS;
        } else {
            alpha = Math.min(1.0f, (now - shownAt) / (float) FADE_IN_MS);
        }

        int targetY = TopCenterStack.claim(STACK_ID, TopCenterStack.ORDER_ANIMAL_TOAST, layout.height());
        int y = Math.round(-layout.height()
                + (targetY + layout.height()) * easeOut(alpha));
        int x = (client.getWindow().getScaledWidth() - layout.width()) / 2;
        if (style == AnimalToastStyle.CUSTOM) {
            draw(context, client.textRenderer, x, y, value, modelEntity,
                    animalType, config, layout, alpha);
        } else {
            AnimalToastCards.draw(context, client.textRenderer, x, y, style, value, modelEntity,
                    animalType, alpha);
        }
    }

    public static void renderEditorPreview(DrawContext context, int x, int y,
                                           OttoExtraConfig.AnimalInfo animalInfo,
                                           String animalType) {
        MinecraftClient client = MinecraftClient.getInstance();
        AnimalInfoSnapshot sample = editorSample(animalType);
        AnimalEntity entity = animalType(modelEntity, snapshot).equals(animalType)
                ? modelEntity : null;
        OttoExtraConfig.AnimalInfoLayout config = animalInfo.layout(animalType);
        Layout layout = layout(client.textRenderer, sample, entity, config);
        draw(context, client.textRenderer, x, y, sample, entity,
                animalType, config, layout, 1.0f);
    }

    public static List<PartBounds> editorParts(OttoExtraConfig.AnimalInfo animalInfo,
                                               String animalType) {
        MinecraftClient client = MinecraftClient.getInstance();
        return layout(client.textRenderer, editorSample(animalType), null,
                animalInfo.layout(animalType)).parts();
    }

    public static int editorWidth(OttoExtraConfig.AnimalInfo animalInfo, String animalType) {
        MinecraftClient client = MinecraftClient.getInstance();
        return layout(client.textRenderer, editorSample(animalType), null,
                animalInfo.layout(animalType)).width();
    }

    public static int editorHeight(OttoExtraConfig.AnimalInfo animalInfo, String animalType) {
        MinecraftClient client = MinecraftClient.getInstance();
        return layout(client.textRenderer, editorSample(animalType), null,
                animalInfo.layout(animalType)).height();
    }

    public static boolean partVisible(OttoExtraConfig.AnimalInfoLayout config, Part part) {
        return switch (part) {
            case MODEL -> config.showModel;
            case TITLE -> config.showTitle;
            case GENDER_ICON -> config.showGenderIcon;
            case GENDER_TEXT -> config.showGenderText;
            case BREEDING -> config.showBreeding;
            case MAST -> config.showMast;
            case TIMER -> config.showTimer;
            case EXTRA -> config.showExtra;
            case JUMP_HEIGHT -> Boolean.TRUE.equals(config.showJumpHeight);
            case SPEED -> Boolean.TRUE.equals(config.showSpeed);
            case STATUS -> config.showStatus;
        };
    }

    public static void setPartVisible(OttoExtraConfig.AnimalInfoLayout config,
                                      Part part, boolean visible) {
        switch (part) {
            case MODEL -> config.showModel = visible;
            case TITLE -> config.showTitle = visible;
            case GENDER_ICON -> config.showGenderIcon = visible;
            case GENDER_TEXT -> config.showGenderText = visible;
            case BREEDING -> config.showBreeding = visible;
            case MAST -> config.showMast = visible;
            case TIMER -> config.showTimer = visible;
            case EXTRA -> config.showExtra = visible;
            case JUMP_HEIGHT -> config.showJumpHeight = visible;
            case SPEED -> config.showSpeed = visible;
            case STATUS -> config.showStatus = visible;
        }
    }

    public static void setPartPosition(OttoExtraConfig.AnimalInfoLayout config,
                                       Part part, int x, int y) {
        int px = Math.max(0, Math.min(500, x));
        int py = Math.max(0, Math.min(500, y));
        switch (part) {
            case MODEL -> { config.modelX = px; config.modelY = py; }
            case TITLE -> { config.titleX = px; config.titleY = py; }
            case GENDER_ICON -> { config.genderIconX = px; config.genderIconY = py; }
            case GENDER_TEXT -> { config.genderTextX = px; config.genderTextY = py; }
            case BREEDING -> { config.breedingX = px; config.breedingY = py; }
            case MAST -> { config.mastX = px; config.mastY = py; }
            case TIMER -> { config.timerX = px; config.timerY = py; }
            case EXTRA -> { config.extraX = px; config.extraY = py; }
            case JUMP_HEIGHT -> { config.jumpHeightX = px; config.jumpHeightY = py; }
            case SPEED -> { config.speedX = px; config.speedY = py; }
            case STATUS -> { config.statusX = px; config.statusY = py; }
        }
    }

    public static float partScale(OttoExtraConfig.AnimalInfoLayout config, Part part) {
        return switch (part) {
            case MODEL -> config.modelSize;
            case TITLE -> config.titleScale;
            case GENDER_ICON -> config.genderIconScale;
            case GENDER_TEXT -> config.genderTextScale;
            case BREEDING -> config.breedingScale;
            case MAST -> config.mastScale;
            case TIMER -> config.timerScale;
            case EXTRA -> config.extraScale;
            case JUMP_HEIGHT -> config.jumpHeightScale;
            case SPEED -> config.speedScale;
            case STATUS -> config.statusScale;
        };
    }

    public static void setPartScale(OttoExtraConfig.AnimalInfoLayout config,
                                    Part part, float value) {
        if (part == Part.MODEL) {
            config.modelSize = Math.max(12, Math.min(64, Math.round(value)));
            return;
        }
        float scale = clampScale(value);
        switch (part) {
            case TITLE -> config.titleScale = scale;
            case GENDER_ICON -> config.genderIconScale = scale;
            case GENDER_TEXT -> config.genderTextScale = scale;
            case BREEDING -> config.breedingScale = scale;
            case MAST -> config.mastScale = scale;
            case TIMER -> config.timerScale = scale;
            case EXTRA -> config.extraScale = scale;
            case JUMP_HEIGHT -> config.jumpHeightScale = scale;
            case SPEED -> config.speedScale = scale;
            case STATUS -> config.statusScale = scale;
            case MODEL -> { }
        }
    }

    public static void resetLayout(OttoExtraConfig.AnimalInfo config, String animalType) {
        config.resetLayout(animalType);
    }

    private static Layout layout(TextRenderer renderer, AnimalInfoSnapshot value,
                                 AnimalEntity entity, OttoExtraConfig.AnimalInfoLayout config) {
        List<PartBounds> parts = new ArrayList<>();
        if (config.showModel) {
            parts.add(new PartBounds(Part.MODEL, config.modelX, config.modelY,
                    config.modelSize, config.modelSize));
        }
        if (config.showTitle) {
            Text name = titleText(renderer, value, config);
            addScaled(parts, Part.TITLE, config.titleX, config.titleY,
                    renderer.getWidth(name), 9, config.titleScale);
        }
        if (config.showGenderIcon) {
            addScaled(parts, Part.GENDER_ICON, config.genderIconX, config.genderIconY,
                    ICON_DISPLAY_SIZE, ICON_DISPLAY_SIZE,
                    config.genderIconScale);
        }
        if (config.showGenderText) {
            addScaled(parts, Part.GENDER_TEXT, config.genderTextX, config.genderTextY,
                    renderer.getWidth(genderLabel(value.gender())), 9, config.genderTextScale);
        }
        if (config.showBreeding) {
            addScaled(parts, Part.BREEDING, config.breedingX, config.breedingY,
                    statusWidth(renderer, "Paarungsbereit"), 9, config.breedingScale);
        }
        if (config.showMast) {
            addScaled(parts, Part.MAST, config.mastX, config.mastY,
                    statusWidth(renderer, mastLabel(value)), 9, config.mastScale);
        }
        String cooldown = breedingCooldown(value.breedingReady());
        if (config.showTimer && !cooldown.isEmpty()) {
            addScaled(parts, Part.TIMER, config.timerX, config.timerY,
                    renderer.getWidth(cooldown), 9, config.timerScale);
        }
        ExtraStatus extra = extraStatus(value, entity);
        if (config.showExtra && extra != null) {
            addScaled(parts, Part.EXTRA, config.extraX, config.extraY,
                    statusWidth(renderer, extra.label()), 9, config.extraScale);
        }
        String jumpHeight = jumpHeightLabel(value);
        if (Boolean.TRUE.equals(config.showJumpHeight) && !jumpHeight.isEmpty()) {
            addScaled(parts, Part.JUMP_HEIGHT, config.jumpHeightX, config.jumpHeightY,
                    statusWidth(renderer, jumpHeight), 9, config.jumpHeightScale);
        }
        String speed = speedLabel(value);
        if (Boolean.TRUE.equals(config.showSpeed) && !speed.isEmpty()) {
            addScaled(parts, Part.SPEED, config.speedX, config.speedY,
                    statusWidth(renderer, speed), 9, config.speedScale);
        }
        if (config.showStatus) {
            addScaled(parts, Part.STATUS, config.statusX, config.statusY,
                    renderer.getWidth(statusText(entity, value, System.currentTimeMillis())), 9,
                    config.statusScale);
        }

        int maxX = 1;
        int maxY = 1;
        for (PartBounds part : parts) {
            maxX = Math.max(maxX, part.x() + part.width());
            maxY = Math.max(maxY, part.y() + part.height());
        }
        int padding = Math.max(0, config.padding);
        return new Layout(maxX + padding * 2, maxY + padding * 2, List.copyOf(parts));
    }

    private static void addScaled(List<PartBounds> parts, Part part, int x, int y,
                                  int rawWidth, int rawHeight, float rawScale) {
        float scale = clampScale(rawScale);
        parts.add(new PartBounds(part, x, y,
                Math.max(1, Math.round(rawWidth * scale)),
                Math.max(1, Math.round(rawHeight * scale))));
    }

    private static int statusWidth(TextRenderer renderer, String label) {
        return STATUS_TEXT_X + renderer.getWidth(label);
    }

    private static void draw(DrawContext context, TextRenderer renderer,
                             int x, int y, AnimalInfoSnapshot value, AnimalEntity entity,
                             String animalType, OttoExtraConfig.AnimalInfoLayout config,
                             Layout layout, float alpha) {
        drawCard(context, x, y, layout.width(), layout.height(), alpha);
        int originX = x + Math.max(0, config.padding);
        int originY = y + Math.max(0, config.padding);
        ExtraStatus extra = extraStatus(value, entity);
        for (PartBounds part : layout.parts()) {
            int px = originX + part.x();
            int py = originY + part.y();
            switch (part.part()) {
                case MODEL -> drawAnimal(context, px, py, part.width(), entity,
                        value, animalType, alpha);
                case TITLE -> drawTitle(context, renderer, value, px, py,
                        config, alpha);
                case GENDER_ICON -> drawGenderIcon(context, renderer, value.gender(), px, py,
                        config.genderIconScale, alpha);
                case GENDER_TEXT -> drawGenderText(context, renderer, value.gender(), px, py,
                        config.genderTextScale, alpha);
                case BREEDING -> drawStatus(context, renderer, px, py,
                        ICON_BREED_READY, "Paarungsbereit",
                        isReady(value.breedingReady()) ? COL_GREEN : COL_RED,
                        isReady(value.breedingReady()) ? COL_GREEN : COL_RED,
                        config.breedingScale, alpha);
                case MAST -> drawStatus(context, renderer, px, py,
                        ICON_MAST, mastLabel(value), COL_AMBER, COL_TEXT,
                        config.mastScale, alpha);
                case TIMER -> drawScaledText(context, renderer,
                        breedingCooldown(value.breedingReady()), px, py,
                        COL_MUTED, config.timerScale, alpha);
                case EXTRA -> {
                    if (extra != null) {
                        int color = extra.positive() ? COL_GREEN : COL_RED;
                        // The icon keeps its own colours; an inactive state is shown at about 45 % alpha.
                        drawStatus(context, renderer, px, py, extra.icon(), extra.label(),
                                extra.positive() ? color : (color & 0x00FFFFFF) | 0x73000000, color,
                                config.extraScale, alpha);
                    }
                }
                case JUMP_HEIGHT -> drawStatus(context, renderer, px, py,
                        ICON_JUMP, jumpHeightLabel(value), COL_MUTED, COL_MUTED,
                        config.jumpHeightScale, alpha);
                case SPEED -> drawStatus(context, renderer, px, py,
                        ICON_SPEED, speedLabel(value), COL_MUTED, COL_MUTED,
                        config.speedScale, alpha);
                case STATUS -> drawScaledText(context, renderer,
                        statusText(entity, value, System.currentTimeMillis()), px, py,
                        COL_MUTED, config.statusScale, alpha, true);
            }
        }
    }

    private static void drawCard(DrawContext context, int x, int y,
                                 int width, int height, float alpha) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1,
                withAlpha(0xFF080A0D, alpha));
        context.fill(x, y, x + width, y + height, withAlpha(COL_BG, alpha));
        context.fill(x, y, x + width, y + 1, withAlpha(COL_BORDER, alpha));
    }

    static void drawAnimal(DrawContext context, int x, int y, int size,
                                   AnimalEntity entity, AnimalInfoSnapshot value,
                                   String animalType, float alpha) {
        boolean rendered = false;
        if (entity != null && entity.isAlive() && alpha > 0.05f) {
            try {
                // Three-quarter view filling the box, without name tag (only the render state is changed).
                AnimalPreview.draw(context, x, y, x + size, y + size, entity);
                rendered = true;
            } catch (Throwable ignored) {
            }
        }
        if (!rendered) {
            char glyph = animalIcon(entity, value, animalType);
            float scale = Math.max(0.75f, size / 16.0f);
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(x + Math.max(0, (size - Math.round(8 * scale)) / 2),
                    y + Math.max(0, (size - Math.round(8 * scale)) / 2));
            matrices.scale(scale, scale);
            context.drawText(MinecraftClient.getInstance().textRenderer,
                    icon(glyph), 0, 0, withAlpha(COL_MUTED, alpha), false);
            matrices.popMatrix();
        }
    }

    private static void drawTitle(DrawContext context, TextRenderer renderer,
                                  AnimalInfoSnapshot value, int x, int y,
                                  OttoExtraConfig.AnimalInfoLayout config, float alpha) {
        float scale = clampScale(config.titleScale);
        Text title = titleText(renderer, value, config);
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        context.drawTextWithShadow(renderer, title, 0, 0, withAlpha(COL_TEXT, alpha));
        matrices.popMatrix();
    }

    private static void drawGenderIcon(DrawContext context, TextRenderer renderer,
                                       String genderValue, int x, int y,
                                       float rawScale, float alpha) {
        Identifier gender = genderIcon(genderValue);
        int color = gender == ICON_FEMALE ? COL_FEMALE
                : gender == ICON_MALE ? COL_MALE : COL_MUTED;
        float scale = clampScale(rawScale);
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        try {
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            drawStatusIcon(context, gender == ICON_FEMALE ? OWN_FEMALE : gender == ICON_MALE ? OWN_MALE : gender,
                    withAlpha(color, alpha));
        } finally {
            matrices.popMatrix();
        }
    }

    private static void drawGenderText(DrawContext context, TextRenderer renderer,
                                       String genderValue, int x, int y,
                                       float rawScale, float alpha) {
        Identifier gender = genderIcon(genderValue);
        int color = gender == ICON_FEMALE ? COL_FEMALE
                : gender == ICON_MALE ? COL_MALE : COL_MUTED;
        drawScaledText(context, renderer, Text.literal(genderLabel(genderValue)),
                x, y, color, rawScale, alpha, true);
    }

    private static void drawStatus(DrawContext context, TextRenderer renderer,
                                   int x, int y, Identifier texture, String label,
                                   int iconColor, int textColor,
                                   float rawScale, float alpha) {
        float scale = clampScale(rawScale);
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        try {
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            drawStatusIcon(context, texture, withAlpha(iconColor, alpha));
            context.drawTextWithShadow(renderer, label, STATUS_TEXT_X, 0,
                    withAlpha(textColor, alpha));
        } finally {
            matrices.popMatrix();
        }
    }

    private static Identifier statusTexture(String name) {
        return Identifier.of("ottoextra", "textures/gui/horse_overview/" + name + ".png");
    }

    /** Icon of the animal info set (9x9, own colours, never tinted). */
    static Identifier ownIcon(String name) {
        return Identifier.of("ottoextra", "textures/gui/animalinfo/" + name + ".png");
    }

    static boolean isOwnIcon(Identifier texture) {
        return texture.getPath().startsWith("textures/gui/animalinfo/");
    }

    /** Draws a 9x9 own-colour icon at the current origin; only the alpha of {@code argb} is used. */
    static void drawOwnIcon(DrawContext context, Identifier texture, int x, int y, int argb) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 9, 9, 9, 9, 9, 9,
                0x00FFFFFF | (argb & 0xFF000000));
    }

    private static void drawStatusIcon(DrawContext context, Identifier texture, int color) {
        if (isOwnIcon(texture)) {
            drawOwnIcon(context, texture, 0, 0, color);
            return;
        }
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0,
                ICON_DISPLAY_SIZE, ICON_DISPLAY_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE,
                ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, color);
    }

    private static void drawScaledText(DrawContext context, TextRenderer renderer,
                                       String label, int x, int y, int color,
                                       float rawScale, float alpha) {
        drawScaledText(context, renderer, Text.literal(label),
                x, y, color, rawScale, alpha, true);
    }

    private static void drawScaledText(DrawContext context, TextRenderer renderer,
                                       Text label, int x, int y, int color,
                                       float rawScale, float alpha, boolean shadow) {
        float scale = clampScale(rawScale);
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        if (shadow) {
            context.drawTextWithShadow(renderer, label, 0, 0, withAlpha(color, alpha));
        } else {
            context.drawText(renderer, label, 0, 0, withAlpha(color, alpha), false);
        }
        matrices.popMatrix();
    }

    private static Text titleText(TextRenderer renderer, AnimalInfoSnapshot value,
                                  OttoExtraConfig.AnimalInfoLayout config) {
        int maxWidth = 145;
        if (config.showGenderIcon) {
            maxWidth = titleWidthBefore(config, config.genderIconX, config.genderIconY,
                    ICON_DISPLAY_SIZE * clampScale(config.genderIconScale), maxWidth);
        }
        if (config.showGenderText) {
            maxWidth = titleWidthBefore(config, config.genderTextX, config.genderTextY,
                    9 * clampScale(config.genderTextScale), maxWidth);
        }
        // Measure the bold text that is actually drawn, including an ellipsis when shortened.
        String name = renderer.trimToWidth(value.animalName(), maxWidth);
        Text title = Text.literal(name).setStyle(Style.EMPTY.withBold(true));
        if (name.equals(value.animalName()) && renderer.getWidth(title) <= maxWidth) return title;
        while (!name.isEmpty()) {
            Text shortened = Text.literal(name + "…").setStyle(Style.EMPTY.withBold(true));
            if (renderer.getWidth(shortened) <= maxWidth) return shortened;
            name = name.substring(0, name.offsetByCodePoints(name.length(), -1));
        }
        Text ellipsis = Text.literal("…").setStyle(Style.EMPTY.withBold(true));
        return renderer.getWidth(ellipsis) <= maxWidth ? ellipsis : Text.empty();
    }

    private static int titleWidthBefore(OttoExtraConfig.AnimalInfoLayout config,
                                        int x, int y, float height, int currentWidth) {
        float scale = clampScale(config.titleScale);
        if (x <= config.titleX || y >= config.titleY + 9 * scale
                || y + height <= config.titleY) return currentWidth;
        return Math.min(currentWidth, Math.max(0, (int) Math.floor((x - config.titleX - 2) / scale)));
    }

    private static String mastLabel(AnimalInfoSnapshot value) {
        String mast = value.mast().isEmpty()
                ? compactRequirement(value.breedingReady()) : value.mast();
        return mast.isEmpty() ? "Mast" : "Mast " + mast;
    }

    /**
     * Coloured status line with the age of the data. Live timers come from {@link AnimalLiveStore}, so they
     * keep counting down between server queries. The editor preview passes a sample snapshot and gets a
     * status derived from that sample instead.
     */
    static Text statusText(AnimalEntity entity, AnimalInfoSnapshot value, long now) {
        StatusView view = statusView(entity, value, now);
        return statusText(view.status(), view.ageMs());
    }

    /** Status of the displayed animal, the age of the data (null if unknown) and the table species. */
    record StatusView(AnimalStatus.Status status, Long ageMs, AnimalSpecies species) {
    }

    static StatusView statusView(AnimalEntity entity, AnimalInfoSnapshot value, long now) {
        boolean displayed = value == snapshot;
        AnimalLiveState state = displayed && entity != null
                ? AnimalLiveStore.current().get(entity.getUuid()) : null;
        Long ageMs;
        if (state != null && state.infoAt() > 0) {
            ageMs = Math.max(0L, now - state.infoAt());
        } else {
            state = AnimalLiveState.empty(entity == null ? null : entity.getUuid()).withInfo(value, now);
            ageMs = displayed ? null : 12_000L;
        }
        AnimalSpecies species = AnimalSpecies.resolve(value.animalName(), state.species());
        boolean baby = displayed && entity != null && entity.isBaby();
        return new StatusView(AnimalStatus.compute(state, species, baby, now), ageMs, species);
    }

    /** Uncoloured status wording ("Wartet noch 3h 20m"). */
    static MutableText statusLabel(AnimalStatus.Status status) {
        String time = status.remainingMs() < 0 ? ""
                : (status.estimated() ? "~" : "") + AnimalDurations.format(status.remainingMs());
        return switch (status.kind()) {
            case IN_LOVE -> Text.translatable("ottoextra.animalinfo.status.love");
            case YOUNG -> time.isEmpty() ? Text.translatable("ottoextra.animalinfo.status.young")
                    : Text.translatable("ottoextra.animalinfo.status.young_timer", time);
            case WAITING -> Text.translatable("ottoextra.animalinfo.status.waiting", time);
            case NEED_MAST -> Text.translatable("ottoextra.animalinfo.status.need_mast",
                    status.mast(), status.mastNeeded());
            case READY -> Text.translatable("ottoextra.animalinfo.status.ready");
            case UNKNOWN -> Text.translatable("ottoextra.animalinfo.status.unknown");
        };
    }

    /** "Stand: vor 5m" / "Stand: gerade eben". */
    static Text ageLabel(long ageMs) {
        return ageMs < 10_000L ? Text.translatable("ottoextra.animalinfo.status.age_now")
                : Text.translatable("ottoextra.animalinfo.status.age", AnimalDurations.formatAge(ageMs));
    }

    static Text statusText(AnimalStatus.Status status, Long ageMs) {
        int color = switch (status.kind()) {
            case IN_LOVE -> AnimalStatus.BREEDING_COLOR;
            case YOUNG -> COL_YOUNG;
            case WAITING -> COL_AMBER;
            case NEED_MAST -> COL_RED;
            case READY -> COL_GREEN;
            case UNKNOWN -> COL_MUTED;
        };
        MutableText text = statusLabel(status).withColor(color);
        if (ageMs != null) {
            text.append(Text.literal(" · ").withColor(COL_MUTED))
                    .append(ageLabel(ageMs).copy().withColor(COL_MUTED));
        }
        return text;
    }

    static String jumpHeightLabel(AnimalInfoSnapshot value) {
        return value == null || value.jumpHeight().isEmpty()
                ? "" : value.jumpHeight();
    }

    static String speedLabel(AnimalInfoSnapshot value) {
        return value == null || value.speed().isEmpty()
                ? "" : value.speed();
    }

    static ExtraStatus extraStatus(AnimalInfoSnapshot value, AnimalEntity entity) {
        if (entity instanceof SheepEntity && value.hasShearable()) {
            return new ExtraStatus(ICON_SHEARABLE, "Scherbar", isPositive(value.shearable()));
        }
        if (entity instanceof CowEntity && value.hasMilkable()) {
            return new ExtraStatus(ICON_MILKABLE, "Melkbar", isPositive(value.milkable()));
        }
        if (value.hasShearable()) {
            return new ExtraStatus(ICON_SHEARABLE, "Scherbar", isPositive(value.shearable()));
        }
        if (value.hasMilkable()) {
            return new ExtraStatus(ICON_MILKABLE, "Melkbar", isPositive(value.milkable()));
        }
        return null;
    }

    private static Text icon(char glyph) {
        return Text.literal(String.valueOf(glyph)).setStyle(Style.EMPTY.withFont(MENU_FONT));
    }

    static Identifier genderIcon(String value) {
        return switch (AnimalGender.parse(value)) {
            case FEMALE -> ICON_FEMALE;
            case MALE -> ICON_MALE;
            case UNKNOWN -> ICON_GENDER;
        };
    }

    static String genderLabel(String value) {
        Identifier gender = genderIcon(value);
        return gender == ICON_FEMALE ? "Weiblich" : gender == ICON_MALE ? "Männlich"
                : value == null ? "" : value;
    }

    private static char animalIcon(AnimalEntity entity, AnimalInfoSnapshot value,
                                   String animalType) {
        if (entity instanceof SheepEntity) return ICON_SHEEP;
        if (entity instanceof CowEntity) return ICON_COW;
        if (entity instanceof PigEntity) return ICON_PIG;
        if (entity instanceof HorseEntity) return ICON_HORSE;
        if (entity instanceof DonkeyEntity || entity instanceof MuleEntity) return ICON_DONKEY;
        if (entity instanceof GoatEntity) return ICON_GOAT;
        if (entity instanceof ChickenEntity) return ICON_CHICKEN;
        if (entity instanceof WolfEntity) return ICON_WOLF;
        char configured = switch (animalType) {
            case "sheep" -> ICON_SHEEP;
            case "cow" -> ICON_COW;
            case "pig" -> ICON_PIG;
            case "horse" -> ICON_HORSE;
            case "streitross", "reitpferd", "ackergaul" -> ICON_HORSE;
            case "donkey" -> ICON_DONKEY;
            case "goat" -> ICON_GOAT;
            case "chicken" -> ICON_CHICKEN;
            case "wolf" -> ICON_WOLF;
            default -> '\0';
        };
        if (configured != '\0') return configured;
        if (value != null && value.hasShearable()) return ICON_SHEEP;
        if (value != null && value.hasMilkable()) return ICON_COW;
        return ICON_ANIMAL_UNKNOWN;
    }

    static String animalType(AnimalEntity entity, AnimalInfoSnapshot value) {
        if (entity instanceof SheepEntity) return "sheep";
        if (entity instanceof CowEntity) return "cow";
        if (entity instanceof PigEntity) return "pig";
        if (entity instanceof DonkeyEntity || entity instanceof MuleEntity) return "donkey";
        if (entity instanceof HorseEntity) {
            String name = value != null ? value.animalName()
                    : entity.getName().getString();
            return horseType(name);
        }
        if (entity instanceof GoatEntity) return "goat";
        if (entity instanceof ChickenEntity) return "chicken";
        if (entity instanceof WolfEntity) return "wolf";
        return "other";
    }

    /**
     * Vor der Serverantwort ist die genaue Pferdeart noch unbekannt. Eine
     * Abfrage ist deshalb nur nötig, wenn wenigstens eines der Pferdelayouts
     * aktiv ist. Alle anderen Tierarten lassen sich sofort eindeutig sperren.
     */
    static boolean mayInspect(AnimalEntity entity, OttoExtraConfig.AnimalInfo config) {
        if (entity instanceof HorseEntity) {
            String visibleType = horseType(entity.getName().getString());
            if (!"horse".equals(visibleType)) {
                return config.layout(visibleType).enabled;
            }
            return config.layout("horse").enabled
                    || config.layout("streitross").enabled
                    || config.layout("reitpferd").enabled
                    || config.layout("ackergaul").enabled
                    || config.layout("donkey").enabled;
        }
        return config.layout(animalType(entity, null)).enabled;
    }

    static boolean enabledFor(AnimalEntity entity, AnimalInfoSnapshot value,
                              OttoExtraConfig.AnimalInfo config) {
        return config.layout(animalType(entity, value)).enabled;
    }

    static String horseType(String value) {
        String name = normalize(value);
        if (name.contains("streitross")) return "streitross";
        if (name.contains("reitpferd")) return "reitpferd";
        if (name.contains("ackergaul")) return "ackergaul";
        if (name.contains("packesel") || name.contains("lastesel")
                || name.contains("maultier") || name.contains("donkey")
                || name.contains("mule") || name.equals("esel")) {
            return "donkey";
        }
        return "horse";
    }

    static AnimalInfoSnapshot editorSample(String animalType) {
        return switch (animalType) {
            case "cow" -> new AnimalInfoSnapshot(
                    "Rotvieh Rind", "Weiblich", "24/30",
                    "in 2h 15m | Mast 24/30", "", "Bereit");
            case "pig" -> new AnimalInfoSnapshot(
                    "Hausschwein", "Männlich", "18/30",
                    "in 3h 20m | Mast 18/30", "", "");
            case "streitross" -> new AnimalInfoSnapshot(
                    "Streitross", "Hengst", "72/100",
                    "in 4h 10m | Mast 72/100", "", "",
                    "3,42 Blöcke", "11,87 Blöcke/s");
            case "reitpferd" -> new AnimalInfoSnapshot(
                    "Reitpferd", "Stute", "72/100",
                    "in 4h 10m | Mast 72/100", "", "",
                    "3,42 Blöcke", "11,87 Blöcke/s");
            case "ackergaul" -> new AnimalInfoSnapshot(
                    "Ackergaul", "Hengst", "72/100",
                    "in 4h 10m | Mast 72/100", "", "",
                    "3,42 Blöcke", "11,87 Blöcke/s");
            case "horse" -> new AnimalInfoSnapshot(
                    "Pferd", "Hengst", "72/100",
                    "in 4h 10m | Mast 72/100", "", "",
                    "3,42 Blöcke", "11,87 Blöcke/s");
            case "donkey" -> new AnimalInfoSnapshot(
                    "Packesel", "Stute", "21/100",
                    "in 9h 6m | Mast 21/100", "", "",
                    "2,15 Blöcke", "8,34 Blöcke/s");
            case "goat" -> new AnimalInfoSnapshot(
                    "Ziege", "Weiblich", "17/30",
                    "in 5h 30m | Mast 17/30", "", "");
            case "chicken" -> new AnimalInfoSnapshot(
                    "Brakel", "Weiblich", "15/30",
                    "in 1h 45m | Mast 15/30", "", "");
            case "wolf" -> new AnimalInfoSnapshot(
                    "Wolf", "Männlich", "20/30",
                    "in 6h 5m | Mast 20/30", "", "");
            case "other" -> new AnimalInfoSnapshot(
                    "Tier", "Weiblich", "18/30",
                    "in 3h 30m | Mast 18/30", "", "");
            default -> new AnimalInfoSnapshot(
                    "Zaupel", "Weiblich", "18/30",
                    "in 11h 5m | Mast 18/30", "Bereit", "Nein");
        };
    }

    private static final java.util.regex.Pattern FEED_PROGRESS =
            java.util.regex.Pattern.compile("\\bmast\\s+(\\d{1,9})\\s*/\\s*(\\d{1,9})");

    static boolean isReady(String value) {
        String normalized = normalize(value);
        if (normalized.startsWith("mast ")) {
            // "Zuchtbereit: Mast 0/100" is feeding progress: ready only once x has reached y.
            java.util.regex.Matcher feed = FEED_PROGRESS.matcher(normalized);
            return feed.find() && Long.parseLong(feed.group(1)) >= Long.parseLong(feed.group(2));
        }
        return normalized.equals("ja") || normalized.equals("yes")
                || normalized.equals("bereit") || normalized.equals("ready")
                || normalized.contains("bereit") && !normalized.contains("nicht")
                || normalized.equals("jetzt") || normalized.equals("now")
                || normalized.startsWith("bereit ") || normalized.startsWith("jetzt ")
                || normalized.startsWith("ready ") || normalized.startsWith("now ");
    }

    static boolean isPositive(String value) {
        String normalized = normalize(value);
        return normalized.equals("ja") || normalized.equals("yes")
                || normalized.equals("bereit") || normalized.equals("ready");
    }

    private static String compactRequirement(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "Mast ", 0, 5)) {
            return trimmed.substring(5).trim();
        }
        return isPositive(trimmed) || normalize(trimmed).equals("nein")
                || normalize(trimmed).equals("no") ? "" : trimmed;
    }

    static String breedingCooldown(String value) {
        if (value == null) return "";
        String cooldown = value.split("\\|", 2)[0].trim();
        return normalize(cooldown).startsWith("in ") ? cooldown : "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static float clampScale(float value) {
        return Math.max(0.5f, Math.min(2.0f, value));
    }

    private static float easeOut(float value) {
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        return 1.0f - (1.0f - clamped) * (1.0f - clamped);
    }

    static int withAlpha(int argb, float alpha) {
        int original = (argb >>> 24) & 0xFF;
        int adjusted = Math.round(original * Math.max(0.0f, Math.min(1.0f, alpha)));
        return (adjusted << 24) | (argb & 0x00FFFFFF);
    }
}
