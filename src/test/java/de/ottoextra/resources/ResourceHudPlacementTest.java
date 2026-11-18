package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceHudPlacementTest {
    @Test
    void defaultSitsTopRightSixtyPixelsDown() {
        int[] pos = ResourceHudPlacement.position(ResourceHudPlacement.Corner.TOP_RIGHT, 0,
                ResourceHudPlacement.DEFAULT_OFFSET_Y, 480, 270, 150, 60);
        assertArrayEquals(new int[] {480 - 150 - 4, 60}, pos);
    }

    @Test
    void positionStaysOnScreen() {
        assertArrayEquals(new int[] {0, 0},
                ResourceHudPlacement.position(ResourceHudPlacement.Corner.TOP_LEFT, -50, -50, 480, 270, 150, 60));
        assertArrayEquals(new int[] {330, 210},
                ResourceHudPlacement.position(ResourceHudPlacement.Corner.BOTTOM_RIGHT, 50, 50, 480, 270, 150, 60));
    }

    @Test
    void dropPicksTheNearestCornerAndKeepsTheSpot() {
        ResourceHudPlacement.Placement placement = ResourceHudPlacement.fromDrag(20, 190, 150, 60, 480, 270);
        assertEquals(ResourceHudPlacement.Corner.BOTTOM_LEFT, placement.corner());
        assertArrayEquals(new int[] {20, 190}, ResourceHudPlacement.position(placement.corner(),
                placement.offsetX(), placement.offsetY(), 480, 270, 150, 60));
        assertEquals(ResourceHudPlacement.Corner.TOP_RIGHT,
                ResourceHudPlacement.fromDrag(300, 10, 150, 60, 480, 270).corner());
    }

    @Test
    void scaleAndCornerNamesAreClamped() {
        assertEquals(0.5f, ResourceHudPlacement.clampScale(0.1f));
        assertEquals(2.0f, ResourceHudPlacement.clampScale(9f));
        assertEquals(1.0f, ResourceHudPlacement.clampScale(Float.NaN));
        assertEquals(ResourceHudPlacement.Corner.TOP_RIGHT, ResourceHudPlacement.Corner.parse(null));
        assertEquals(ResourceHudPlacement.Corner.BOTTOM_LEFT, ResourceHudPlacement.Corner.parse("BOTTOM_LEFT"));
        assertEquals(75, ResourceHudPlacement.scaled(150, 0.5f));
    }
}
