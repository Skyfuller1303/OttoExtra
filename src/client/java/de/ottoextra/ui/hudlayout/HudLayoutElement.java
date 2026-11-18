package de.ottoextra.ui.hudlayout;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * One movable HUD element in the shared layout editor. Built-in elements cover the region activity
 * HUD, the Lehen overlay and the Xaero minimap; addons contribute their own through
 * {@link de.ottoextra.addon.OttoExtraAddon#hudLayoutElements()}.
 */
public interface HudLayoutElement {

    /** Screen rectangle in scaled GUI pixels. */
    record Bounds(int x, int y, int w, int h) {
    }

    String id();

    Text label();

    /** Current on-screen bounds, or null if the element is not available right now. */
    Bounds bounds(int screenW, int screenH);

    default boolean movable() {
        return true;
    }

    default boolean scalable() {
        return false;
    }

    /** Current scale; only meaningful if {@link #scalable()}. */
    default float scale() {
        return 1.0f;
    }

    /** Stores a new scale (already clamped to 0.5..2.0 by the editor). Bounds change accordingly. */
    default void setScale(float scale) {
    }

    /**
     * Moves the element so that its top-left corner is at (x, y). {@code w} and {@code h} are its
     * current size. The element decides how to store this (anchor and offset, or absolute values).
     */
    void place(int x, int y, int w, int h, int screenW, int screenH);

    /** Back to the default placement (or the placement at editor start where there is no default). */
    void reset();

    /**
     * True if the element is drawn by the game HUD itself behind the editor (Xaero minimap, Lehen
     * overlay), so the editor must not draw a copy.
     */
    default boolean rendersLive() {
        return false;
    }

    /** Id this element uses with {@link HudFlow}, or null if it does not take part. */
    default String flowId() {
        return null;
    }

    /** Called after an edit finished; persist the settings here. */
    default void save() {
    }

    /**
     * Draws the element's real look with its top-left corner at (x, y). Return false to let the
     * editor draw a plain labelled box instead.
     */
    default boolean drawPreview(DrawContext context, TextRenderer font, int x, int y) {
        return false;
    }
}
