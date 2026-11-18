package de.ottoextra.ui.hudlayout;

import de.ottoextra.ui.hudlayout.HudSnap.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HudSnapTest {
    private static final int W = 400;
    private static final int H = 240;
    private static final int M = 4;

    private static HudSnap.Result snap(Rect moving, List<Rect> others, boolean off) {
        return HudSnap.snap(moving, others, W, H, M, 4, off);
    }

    @Test void snapsToScreenEdgeAndMarginLines() {
        var left = snap(new Rect(5, 100, 50, 30), List.of(), false);
        assertEquals(4, left.x());
        assertEquals(List.of(4), left.guideX());
        assertEquals(W - 50 - M, snap(new Rect(W - 50 - M - 3, 100, 50, 30), List.of(), false).x());
        var bottom = snap(new Rect(100, H - 30 - M + 2, 50, 30), List.of(), false);
        assertEquals(H - 30 - M, bottom.y());
        assertEquals(List.of(H - M), bottom.guideY());
    }

    @Test void snapsCentreToScreenCentreLines() {
        var r = snap(new Rect(W / 2 - 50 + 3, H / 2 - 15 - 2, 100, 30), List.of(), false);
        assertEquals(W / 2 - 50, r.x());
        assertEquals(H / 2 - 15, r.y());
        assertEquals(List.of(W / 2), r.guideX());
        assertEquals(List.of(H / 2), r.guideY());
    }

    @Test void snapsToEdgesAndCentresOfOtherBoxes() {
        Rect other = new Rect(100, 60, 80, 40);
        var edge = snap(new Rect(182, 102, 60, 30), List.of(other), false);
        assertEquals(180, edge.x());
        assertEquals(100, edge.y());
        var centre = snap(new Rect(140 - 30 + 2, 150, 60, 30), List.of(other), false);
        assertEquals(110, centre.x());
        assertEquals(List.of(140), centre.guideX());
    }

    @Test void altDisablesSnappingCompletely() {
        var r = snap(new Rect(5, 3, 50, 30), List.of(new Rect(60, 0, 10, 10)), true);
        assertEquals(5, r.x());
        assertEquals(3, r.y());
        assertTrue(r.guideX().isEmpty());
        assertTrue(r.guideY().isEmpty());
    }

    @Test void nothingSnapsOutsideTheThreshold() {
        var r = snap(new Rect(20, 100, 50, 30), List.of(), false);
        assertEquals(20, r.x());
        assertEquals(100, r.y());
        assertTrue(r.guideX().isEmpty());
        assertEquals(M, snap(new Rect(M + 4, 100, 50, 30), List.of(), false).x());
        assertEquals(M + 5, snap(new Rect(M + 5, 100, 50, 30), List.of(), false).x());
    }

    @Test void scaleIsClampedAndStepped() {
        assertEquals(0.5f, HudSnap.clampScale(0.1f));
        assertEquals(2.0f, HudSnap.clampScale(9f));
        assertEquals(1.0f, HudSnap.clampScale(Float.NaN));
        assertEquals(1.1f, HudSnap.stepScale(1.0999f), 0.0001f);
        assertEquals(1.15f, HudSnap.stepScale(1.14f), 0.0001f);
        assertEquals(150, HudSnap.scaled(100, 1.5f));
    }

    @Test void activityAnchorsMatchRegionActivityHudScheme() {
        int w = 100;
        int h = 60;
        assertArrayEquals(new int[]{4, 90}, ActivityHudAnchors.position("MIDDLE_LEFT", 0, 0, W, H, w, h));
        assertArrayEquals(new int[]{296, 90}, ActivityHudAnchors.position("middle_right", 0, 0, W, H, w, h));
        assertArrayEquals(new int[]{296, 176}, ActivityHudAnchors.position("BOTTOM_RIGHT", 0, 0, W, H, w, h));
        assertArrayEquals(new int[]{14, 100}, ActivityHudAnchors.position(null, 10, 10, W, H, w, h));
        assertArrayEquals(new int[]{300, 180}, ActivityHudAnchors.position("BOTTOM_RIGHT", 999, 999, W, H, w, h));
        var left = ActivityHudAnchors.fromDrag(10, 95, w, h, W, H);
        assertEquals("MIDDLE_LEFT", left.anchor());
        assertEquals(6, left.offsetX());
        assertEquals(5, left.offsetY());
        var corner = ActivityHudAnchors.fromDrag(280, 170, w, h, W, H);
        assertEquals("BOTTOM_RIGHT", corner.anchor());
        assertArrayEquals(new int[]{280, 170}, ActivityHudAnchors.position(corner.anchor(), corner.offsetX(),
                corner.offsetY(), W, H, w, h));
    }

    @Test void droppedActivityHudStaysAtItsYAfterSaveAndReloadForEveryAnchorAndScale() {
        int baseW = 120;
        int baseH = 77;
        for (float scale : new float[]{0.75f, 1.0f, 1.5f}) {
            int w = HudSnap.scaled(baseW, scale);
            int h = HudSnap.scaled(baseH, scale);
            for (int y = 0; y <= H - h; y += 17) {
                for (int x : new int[]{4, 120, W - w - 4}) {
                    var placement = ActivityHudAnchors.fromDrag(x, y, w, h, W, H);
                    // "Reload": stored anchor string and offsets are all that survives.
                    int[] reloaded = ActivityHudAnchors.position(placement.anchor(), placement.offsetX(),
                            placement.offsetY(), W, H, w, h);
                    assertArrayEquals(new int[]{x, y}, reloaded,
                            "scale " + scale + " drop " + x + "," + y + " via " + placement.anchor());
                }
            }
            // Explicit MIDDLE_LEFT drop.
            int mid = (H - h) / 2 + 9;
            var p = ActivityHudAnchors.fromDrag(4, mid, w, h, W, H);
            assertEquals("MIDDLE_LEFT", p.anchor());
            assertEquals(9, p.offsetY());
        }
    }
}
