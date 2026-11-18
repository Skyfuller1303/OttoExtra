package de.ottoextra.tweaks.vote;

import de.ottoextra.OttoExtra;
import de.ottoextra.regions.RegionActivityHud;
import de.ottoextra.ui.hudlayout.TopCenterStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Vote reminder card (design C) in the palette of the activity HUD: gold header band "Vote-Erinnerung",
 * the seal at its native pixel size in a dark inset square, a rotating title and subtitle from
 * {@link VoteReminderMessageCatalog}, the bound vote key with "Abstimmen", the bound dismiss key with
 * "Später" and a thin gold remaining-time line. It stands in the top-centre column ({@link TopCenterStack})
 * below the boss bars, the Werkstatt bar and zone toasts. No text shadows.
 */
public final class VoteReminderToast {

    public static final String STACK_ID = "ottoextra:vote";
    private static final long FADE_IN_MILLIS = 260L;
    private static final long FADE_OUT_MILLIS = 360L;
    /** Native size of siegelshop.png; drawn 1:1, never scaled by a fraction. */
    private static final int ICON_SIZE = 16;
    private static final Identifier ICON = Identifier.of(
            OttoExtra.MOD_ID,
            "textures/gui/siegelshop.png"
    );

    private static final int OUTL = 0xFF141418;
    private static final int D2 = 0xFF2E2D35;
    private static final int D3 = 0xFF3B3A45;
    private static final int NAME = 0xFFF0EBE0;
    private static final int OCHRE = 0xFFD6C194;
    private static final int SUB = 0xFFA9A6B0;
    private static final int LABEL = 0xFF8F8C96;
    private static final int GOLD_DARK = 0xFF96752C;
    private static final int GOLD = 0xFFCD9F3D;

    private final long shownAt;
    private final long durationMillis;
    private final VoteReminderMessageCatalog.Variant message;
    private final KeyBinding openKey;
    private final KeyBinding dismissKey;
    private final boolean iconAvailable;
    private boolean hidden;

    public VoteReminderToast(
            long durationMillis,
            VoteReminderMessageCatalog.Variant message,
            KeyBinding openKey,
            KeyBinding dismissKey
    ) {
        this.shownAt = System.currentTimeMillis();
        this.durationMillis = Math.max(1_500L, durationMillis);
        this.message = message;
        this.openKey = openKey;
        this.dismissKey = dismissKey;
        MinecraftClient client = MinecraftClient.getInstance();
        this.iconAvailable = client != null
                && client.getResourceManager().getResource(ICON).isPresent();
    }

    public void hide() {
        hidden = true;
        TopCenterStack.release(STACK_ID);
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (hidden) {
            return;
        }

        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed > durationMillis) {
            hide();
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
        float alpha = progress;

        TextRenderer font = client.textRenderer;
        String header = Text.translatable("ottoextra.tweaks.vote.toast.header").getString();
        String title = message == null ? "" : Text.translatable(message.titleKey()).getString();
        String subtitle = message == null ? "" : Text.translatable(message.descriptionKey()).getString();
        String action = Text.translatable("ottoextra.tweaks.vote.toast.action").getString();
        String later = Text.translatable("ottoextra.tweaks.vote.toast.later").getString();
        String openLabel = keyLabel(openKey);
        String laterLabel = keyLabel(dismissKey);
        VoteToastLayout.Layout layout = VoteToastLayout.compute(
                openLabel.isEmpty() ? 0 : font.getWidth(openLabel), font.getWidth(action),
                laterLabel.isEmpty() ? 0 : font.getWidth(laterLabel), font.getWidth(later),
                context.getScaledWindowWidth());
        int width = layout.width();
        int height = VoteToastLayout.H;
        int x = (context.getScaledWindowWidth() - width) / 2;
        int targetY = TopCenterStack.claim(STACK_ID, TopCenterStack.ORDER_VOTE, height);
        int y = Math.round((-height - 4) + (targetY - (-height - 4)) * ease);

        RegionActivityHud.drawPanel(context, x, y, width, height, alpha);
        int inset = VoteToastLayout.BAND_INSET;
        context.fill(x + inset, y + inset, x + width - inset, y + VoteToastLayout.BAND_BOTTOM,
                withAlpha(GOLD_DARK, alpha));
        context.fill(x + inset, y + inset, x + width - inset, y + inset + 1, withAlpha(GOLD, alpha));
        String headerText = clip(font, header, layout.headerRoom());
        context.drawText(font, headerText, x + (width - font.getWidth(headerText)) / 2,
                y + VoteToastLayout.HEADER_TEXT_Y, withAlpha(NAME, alpha), false);

        int bx = x + VoteToastLayout.SEAL_BOX_X;
        int by = y + VoteToastLayout.SEAL_BOX_Y;
        int box = VoteToastLayout.SEAL_BOX;
        context.fill(bx, by, bx + box, by + box, withAlpha(OUTL, alpha));
        context.fill(bx + 1, by + 1, bx + box - 1, by + box - 1, withAlpha(D3, alpha));
        if (iconAvailable) {
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    ICON,
                    x + VoteToastLayout.SEAL_X,
                    y + VoteToastLayout.SEAL_Y,
                    0.0F,
                    0.0F,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    withAlpha(0xFFFFFFFF, alpha)
            );
        }
        int tx = x + VoteToastLayout.TEXT_X;
        context.drawText(font, clip(font, title, layout.textRoom()), tx, y + VoteToastLayout.TITLE_Y,
                withAlpha(OCHRE, alpha), false);
        context.drawText(font, clip(font, subtitle, layout.textRoom()), tx, y + VoteToastLayout.SUBTITLE_Y,
                withAlpha(SUB, alpha), false);

        int rowY = y + VoteToastLayout.ROW_Y;
        if (layout.openChipW() > 0) {
            chip(context, font, tx, rowY, layout.openChipW(), openLabel, alpha);
            context.drawText(font, clip(font, action, layout.actionRoom()),
                    tx + layout.openChipW() + VoteToastLayout.LABEL_GAP, rowY + 1, withAlpha(NAME, alpha), false);
        }
        if (layout.showLater()) {
            int kx = x + layout.laterX();
            chip(context, font, kx, rowY, layout.laterChipW(), laterLabel, alpha);
            context.drawText(font, later, kx + layout.laterChipW() + VoteToastLayout.LABEL_GAP, rowY + 1,
                    withAlpha(LABEL, alpha), false);
        }

        int lineY = y + height - VoteToastLayout.LINE_FROM_BOTTOM;
        int lineW = layout.lineWidth();
        context.fill(x + inset, lineY, x + inset + lineW, lineY + 1, withAlpha(D2, alpha));
        int left = VoteToastLayout.remaining(lineW, elapsed, durationMillis);
        if (left > 0) {
            context.fill(x + inset, lineY, x + inset + left, lineY + 1, withAlpha(GOLD, alpha));
        }
    }

    /** The localized name of a bound key, empty when the key is missing or unbound. */
    private static String keyLabel(KeyBinding key) {
        return key == null || key.isUnbound() ? "" : key.getBoundKeyLocalizedText().getString();
    }

    /** Dark key cap, {@link VoteToastLayout#CHIP_H} high, rounded by leaving out the corner pixels. */
    private static void chip(DrawContext context, TextRenderer font, int kx, int ky, int kw, String label,
                             float alpha) {
        int kh = VoteToastLayout.CHIP_H;
        context.fill(kx + 1, ky, kx + kw - 1, ky + 1, withAlpha(OUTL, alpha));
        context.fill(kx + 1, ky + kh - 1, kx + kw - 1, ky + kh, withAlpha(OUTL, alpha));
        context.fill(kx, ky + 1, kx + 1, ky + kh - 1, withAlpha(OUTL, alpha));
        context.fill(kx + kw - 1, ky + 1, kx + kw, ky + kh - 1, withAlpha(OUTL, alpha));
        context.fill(kx + 1, ky + 1, kx + kw - 1, ky + kh - 2, withAlpha(D3, alpha));
        context.fill(kx + 1, ky + kh - 2, kx + kw - 1, ky + kh - 1, withAlpha(D2, alpha));
        context.drawText(font, label, kx + VoteToastLayout.CHIP_PAD / 2, ky + 1, withAlpha(NAME, alpha), false);
    }

    private static String clip(TextRenderer font, String text, int room) {
        if (font.getWidth(text) <= room) {
            return text;
        }
        int end = text.length();
        while (end > 0 && font.getWidth(text.substring(0, end) + "…") > room) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end).stripTrailing() + "…";
    }

    private static int withAlpha(int argb, float alpha) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int newAlpha = Math.round(baseAlpha * Math.max(0.0F, Math.min(1.0F, alpha)));
        if (newAlpha < 4) {
            newAlpha = 0;
        }
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }
}
