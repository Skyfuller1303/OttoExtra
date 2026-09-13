package de.ottoextra.tweaks.vote;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class VoteReminderToast {

    private static final long FADE_IN_MILLIS = 260L;
    private static final long FADE_OUT_MILLIS = 360L;

    private static final int MIN_WIDTH = 220;
    private static final int MAX_WIDTH = 330;
    private static final int MAX_TEXT_WIDTH = 260;
    private static final int PADDING_LEFT = 10;
    private static final int PADDING_RIGHT = 10;
    private static final int PADDING_TOP = 6;
    private static final int PADDING_BOTTOM = 6;
    private static final int TOP_MARGIN = 6;
    private static final int LINE_GAP = 2;
    private static final int ICON_SIZE = 24;
    private static final int ICON_SOURCE_SIZE = 10;
    private static final int ICON_GAP = 6;
    private static final Identifier ICON = Identifier.of(
            OttoExtra.MOD_ID,
            "textures/gui/siegelshop.png"
    );

    private static final Palette PALETTE = new Palette(
            0xFFD0D0D0,
            0xFF202020,
            0xFFF2F2F2,
            0xFF8A8A8A,
            0xFF111111,
            0xFF000000,
            0xFF303030
    );

    private final long shownAt;
    private final long durationMillis;
    private final VoteReminderMessageCatalog.Variant message;
    private final boolean openingNoticeRequired;
    private final boolean iconAvailable;
    private boolean hidden;

    public VoteReminderToast(
            long durationMillis,
            VoteReminderMessageCatalog.Variant message,
            boolean openingNoticeRequired
    ) {
        this.shownAt = System.currentTimeMillis();
        this.durationMillis = Math.max(1_500L, durationMillis);
        this.message = message;
        this.openingNoticeRequired = openingNoticeRequired;
        MinecraftClient client = MinecraftClient.getInstance();
        this.iconAvailable = client != null
                && client.getResourceManager().getResource(ICON).isPresent();
    }

    public void hide() {
        hidden = true;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (hidden) {
            return;
        }

        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed > durationMillis) {
            hidden = true;
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        float progress;
        if (elapsed < FADE_IN_MILLIS) {
            progress = elapsed / (float) FADE_IN_MILLIS;
        } else if (elapsed > durationMillis - FADE_OUT_MILLIS) {
            progress = (durationMillis - elapsed) / (float) FADE_OUT_MILLIS;
        } else {
            progress = 1.0F;
        }
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        float ease = 1.0F - (1.0F - progress) * (1.0F - progress);

        TextRenderer textRenderer = client.textRenderer;
        List<ScaledLine> lines = List.of(
                new ScaledLine(
                        Text.translatable(message.titleKey()).asOrderedText(),
                        0.72F,
                        PALETTE.title()
                ),
                new ScaledLine(
                        Text.translatable(message.descriptionKey()).asOrderedText(),
                        1.0F,
                        PALETTE.body()
                ),
                new ScaledLine(
                        Text.translatable(openingNoticeRequired
                                ? "ottoextra.tweaks.vote.toast.hint"
                                : "ottoextra.tweaks.vote.toast.hint.direct").asOrderedText(),
                        0.62F,
                        PALETTE.hint()
                )
        );

        int textWidth = 0;
        int contentHeight = 0;
        for (ScaledLine line : lines) {
            textWidth = Math.max(textWidth,
                    Math.round(textRenderer.getWidth(line.text()) * line.scale()));
            contentHeight += Math.round(textRenderer.fontHeight * line.scale());
        }
        contentHeight += LINE_GAP * (lines.size() - 1);
        textWidth = Math.min(textWidth, MAX_TEXT_WIDTH);

        int iconArea = iconAvailable ? ICON_SIZE + ICON_GAP : 0;
        int width = Math.max(MIN_WIDTH, Math.min(
                MAX_WIDTH,
                PADDING_LEFT + iconArea + textWidth + PADDING_RIGHT
        ));
        int iconHeight = iconAvailable ? PADDING_TOP + ICON_SIZE + PADDING_BOTTOM : 0;
        int height = Math.max(
                32,
                Math.max(PADDING_TOP + contentHeight + PADDING_BOTTOM, iconHeight)
        );
        int screenWidth = client.getWindow().getScaledWidth();
        int x = (screenWidth - width) / 2;
        int y = Math.round((-height - 4) + (TOP_MARGIN - (-height - 4)) * ease);
        float alpha = progress;

        context.fill(x - 1, y - 1, x + width + 1, y + height + 1,
                withAlpha(PALETTE.borderOuter(), alpha));
        context.fill(x, y, x + width, y + height,
                withAlpha(PALETTE.background(), alpha));
        context.fill(x, y, x + width, y + 1,
                withAlpha(PALETTE.borderTopLeft(), alpha));
        context.fill(x, y, x + 1, y + height,
                withAlpha(PALETTE.borderTopLeft(), alpha));
        context.fill(x, y + height - 1, x + width, y + height,
                withAlpha(PALETTE.borderBottomRight(), alpha));
        context.fill(x + width - 1, y, x + width, y + height,
                withAlpha(PALETTE.borderBottomRight(), alpha));

        int textX = x + PADDING_LEFT;
        var matrices = context.getMatrices();
        if (iconAvailable) {
            int iconY = y + (height - ICON_SIZE) / 2;
            float iconScale = ICON_SIZE / (float) ICON_SOURCE_SIZE;
            matrices.pushMatrix();
            matrices.translate(textX, iconY);
            matrices.scale(iconScale, iconScale);
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    ICON,
                    0,
                    0,
                    0.0F,
                    0.0F,
                    ICON_SOURCE_SIZE,
                    ICON_SOURCE_SIZE,
                    ICON_SOURCE_SIZE,
                    ICON_SOURCE_SIZE,
                    withAlpha(0xFFFFFFFF, alpha)
            );
            matrices.popMatrix();
            textX += ICON_SIZE + ICON_GAP;
        }

        float textY = y + (height - contentHeight) / 2.0F;
        for (ScaledLine line : lines) {
            matrices.pushMatrix();
            matrices.translate(textX, textY);
            matrices.scale(line.scale(), line.scale());
            context.drawText(
                    textRenderer,
                    line.text(),
                    0,
                    0,
                    withAlpha(line.color(), alpha),
                    false
            );
            matrices.popMatrix();
            textY += Math.round(textRenderer.fontHeight * line.scale()) + LINE_GAP;
        }
    }

    private static int withAlpha(int argb, float alpha) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int newAlpha = Math.round(baseAlpha * Math.max(0.0F, Math.min(1.0F, alpha)));
        if (newAlpha < 4) {
            newAlpha = 0;
        }
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }

    private record ScaledLine(OrderedText text, float scale, int color) {
    }

    private record Palette(
            int background,
            int borderOuter,
            int borderTopLeft,
            int borderBottomRight,
            int title,
            int body,
            int hint
    ) {
    }
}
