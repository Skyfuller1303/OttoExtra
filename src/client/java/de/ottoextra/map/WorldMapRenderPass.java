package de.ottoextra.map;

/** Render-thread-only guard: the painted base must not cover features drawn in an earlier stage. */
final class WorldMapRenderPass {
    private Object screen;
    private boolean earlyClaimed;

    void begin(Object screen) {
        this.screen = screen;
        earlyClaimed = false;
    }

    boolean claimEarly(Object screen) {
        if (screen == null || this.screen != screen || earlyClaimed) {
            return false;
        }
        earlyClaimed = true;
        return true;
    }

    boolean shouldRenderLate(Object screen) {
        return this.screen != screen || !earlyClaimed;
    }
}
