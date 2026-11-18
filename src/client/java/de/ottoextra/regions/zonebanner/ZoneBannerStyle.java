package de.ottoextra.regions.zonebanner;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * A procedurally drawn "zone entered" banner. Implementations draw everything with
 * {@link DrawContext} primitives (fill, text, the banner texture) and own their animation.
 */
public interface ZoneBannerStyle {

    /** Stable config id, lower case. */
    String id();

    /**
     * Draws one frame.
     *
     * @param elapsedMs time since the banner was triggered
     * @param displayMs total display time; the banner must be fully faded out at this point
     *                  unless {@code frame.sticky()} is true (then hold the fully shown state)
     */
    void render(DrawContext ctx, TextRenderer tr, ZoneBannerFrame frame, long elapsedMs, long displayMs);
}
