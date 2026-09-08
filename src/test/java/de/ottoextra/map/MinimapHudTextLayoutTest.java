package de.ottoextra.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinimapHudTextLayoutTest {
    @Test
    void legacyHeightKeepsOneLineAtEverySupportedScale() {
        for (float scale : new float[]{0.5F, 0.52F, 0.97F, 1F, 1.25F, 3F}) {
            assertEquals(Math.round(10 * scale), MinimapHudTextLayout.height(0, scale));
            assertEquals(1, MinimapHudTextLayout.maxLines(MinimapHudTextLayout.height(0, scale), scale));
        }
    }

    @Test
    void heightAllowsOnlyCompleteLinesAndRespectsIndependentFontSize() {
        assertEquals(1, MinimapHudTextLayout.maxLines(19, 1F));
        assertEquals(2, MinimapHudTextLayout.maxLines(20, 1F));
        assertEquals(2, MinimapHudTextLayout.maxLines(10, 0.5F));
        assertEquals(1, MinimapHudTextLayout.maxLines(20, 2F));
        assertEquals(60, MinimapHudTextLayout.textWidth(91, 1.5F));
        assertEquals(3, MinimapHudTextLayout.maxLines(45, 1.5F));
    }

    @Test
    void malformedConfigurationStaysWithinUsableBounds() {
        assertEquals(160, MinimapHudTextLayout.height(Integer.MAX_VALUE, 1F));
        assertEquals(30, MinimapHudTextLayout.height(1, 3F));
        assertEquals(10, MinimapHudTextLayout.height(-5, Float.NaN));
        assertEquals(1F, MinimapHudTextLayout.clampScale(Float.POSITIVE_INFINITY));
        assertEquals(1, MinimapHudTextLayout.textWidth(-100, 3F));
    }
}
