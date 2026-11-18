package de.ottoextra.crafting.werkstatt;

import de.ottoextra.ui.hudlayout.ActivityHudAnchors;
import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WerkstattBarLayoutTest {
    /** Six pixels per character, like most glyphs of the Minecraft font. */
    private static final ToIntFunction<String> W = s -> s.length() * 6;

    @Test
    void shortTextKeepsTheBossBarWidth() {
        int right = WerkstattBarLayout.right(false, 24, false, 0);
        assertEquals(7 + 2 + 24, right);
        WerkstattBarLayout.Layout l = WerkstattBarLayout.compute(60, right, 640);
        assertEquals(182, l.width());
        assertEquals(182 - 26 - 8 - right - 6, l.textRoom());
        assertEquals(182 - 32, l.barWidth());
    }

    @Test
    void barGrowsWithTextLockAndStopKey() {
        int plain = WerkstattBarLayout.right(false, 24, false, 0);
        int locked = WerkstattBarLayout.right(false, 24, true, 0);
        int withKey = WerkstattBarLayout.right(false, 24, true, 18);
        assertEquals(plain + 9, locked);
        assertEquals(locked + 18 + 4, withKey);
        // "Salz 23/27 · 4 fehlgeschlagen": text plus gaps, wider than 182.
        WerkstattBarLayout.Layout l = WerkstattBarLayout.compute(170, withKey, 640);
        assertEquals(26 + 170 + 8 + withKey + 6, l.width());
        assertEquals(170, l.textRoom());
    }

    @Test
    void finishedShowsOnlyTheCheck() {
        assertEquals(8, WerkstattBarLayout.right(true, 24, true, 18));
    }

    @Test
    void narrowScreenClipsTheText() {
        int right = WerkstattBarLayout.right(false, 24, false, 0);
        WerkstattBarLayout.Layout l = WerkstattBarLayout.compute(400, right, 300);
        assertEquals(300 - 8, l.width());
        assertEquals(292 - 26 - 8 - right - 6, l.textRoom());
        String text = "x".repeat(60);
        String clipped = WerkstattBarLayout.clip(W, text, l.textRoom());
        assertTrue(clipped.endsWith("…"));
        assertTrue(W.applyAsInt(clipped) <= l.textRoom());
        assertEquals("Salz", WerkstattBarLayout.clip(W, "Salz", 24));
        assertEquals("", WerkstattBarLayout.clip(W, "Salz", 3));
        assertEquals("", WerkstattBarLayout.clip(W, null, 50));
    }

    @Test
    void screenNarrowerThanTheMinimumGivesNoNegativeSizes() {
        WerkstattBarLayout.Layout l = WerkstattBarLayout.compute(100, 40, 60);
        assertEquals(52, l.width());
        assertEquals(0, l.textRoom());
        assertEquals(20, l.barWidth());
        assertEquals(0, WerkstattBarLayout.compute(10, 40, 0).width());
    }

    @Test
    void progressFillIsClamped() {
        assertEquals(0, WerkstattBarLayout.filled(150, -1f));
        assertEquals(127, WerkstattBarLayout.filled(150, 23 / 27f));
        assertEquals(150, WerkstattBarLayout.filled(150, 2f));
    }

    @Test
    void topCenterUsesTheColumnYAndCentresHorizontally() {
        assertArrayEquals(new int[]{(640 - 200) / 2, 21},
                WerkstattBarLayout.position("TOP_CENTER", 0, 0, 640, 360, 200, 20, 21));
        assertArrayEquals(new int[]{(640 - 200) / 2 + 10, 26},
                WerkstattBarLayout.position("TOP_CENTER", 10, 5, 640, 360, 200, 20, 21));
        // Corner anchors keep the activity HUD math.
        assertArrayEquals(ActivityHudAnchors.position("TOP_LEFT", 4, 4, 640, 360, 200, 20),
                WerkstattBarLayout.position("TOP_LEFT", 4, 4, 640, 360, 200, 20, 99));
    }

    @Test
    void dragNearTheTopCentreSnapsToIt() {
        ActivityHudAnchors.Placement p = WerkstattBarLayout.fromDrag(222, 8, 200, 20, 640, 360, 4);
        assertEquals("TOP_CENTER", p.anchor());
        assertEquals(2, p.offsetX());
        assertEquals(4, p.offsetY());
        ActivityHudAnchors.Placement corner = WerkstattBarLayout.fromDrag(10, 10, 200, 20, 640, 360, 4);
        assertEquals("TOP_LEFT", corner.anchor());
    }
}
