package de.ottoextra.resources;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceOutlineTest {
    private static double distance(double[] points, int index, double cx, double cz) {
        return Math.hypot(points[2 * index] - cx, points[2 * index + 1] - cz);
    }

    @Test
    void radiusOneGivesTheMinimumRingOnTheCircle() {
        double[] points = ResourceOutline.ring(10.5, -3.5, 1, ResourceOutline.MAX_POINTS);
        assertEquals(8, points.length / 2);
        for (int index = 0; index < 8; index++) assertEquals(1.0, distance(points, index, 10.5, -3.5), 1e-9);
    }

    @Test
    void ringSpacingIsAboutOneBlock() {
        double[] points = ResourceOutline.ring(0, 0, 10, ResourceOutline.MAX_POINTS);
        assertEquals(63, points.length / 2);
        double gap = Math.hypot(points[2] - points[0], points[3] - points[1]);
        assertTrue(gap > 0.9 && gap < 1.1, "gap " + gap);
    }

    @Test
    void radiusSixtyFourIsCappedAtOneHundredSixtyPoints() {
        double[] points = ResourceOutline.ring(0, 0, 64, ResourceOutline.MAX_POINTS);
        assertEquals(ResourceOutline.MAX_POINTS, points.length / 2);
        for (int index = 0; index < points.length / 2; index++) {
            assertEquals(64.0, distance(points, index, 0, 0), 1e-9);
        }
    }

    @Test
    void smallerCapsAreHonouredAndNeverBelowTheMinimum() {
        assertEquals(20, ResourceOutline.ring(0, 0, 30, 20).length / 2);
        assertEquals(8, ResourceOutline.ring(0, 0, 30, 3).length / 2);
    }

    @Test
    void cuboidEdgesStartAtTheCornerAndStayOnTheRectangle() {
        double[] points = ResourceOutline.boxEdges(0, 0, 20, 10, ResourceOutline.MAX_POINTS);
        assertEquals(60, points.length / 2);
        assertEquals(0.0, points[0]);
        assertEquals(0.0, points[1]);
        boolean corner = false;
        for (int index = 0; index < points.length; index += 2) {
            double x = points[index];
            double z = points[index + 1];
            assertTrue(x >= 0 && x <= 20 && z >= 0 && z <= 10);
            assertTrue(x == 0 || x == 20 || z == 0 || z == 10 || Math.abs(x - 20) < 1e-9
                    || Math.abs(z - 10) < 1e-9, x + "," + z);
            if (Math.abs(x - 20) < 1e-9 && Math.abs(z - 10) < 1e-9) corner = true;
        }
        assertTrue(corner);
    }

    @Test
    void largeCuboidIsCappedAndStillCoversAllFourSides() {
        double[] points = ResourceOutline.boxEdges(0, 0, 300, 200, ResourceOutline.MAX_POINTS);
        assertEquals(ResourceOutline.MAX_POINTS, points.length / 2);
        boolean top = false, right = false, bottom = false, left = false;
        for (int index = 0; index < points.length; index += 2) {
            double x = points[index];
            double z = points[index + 1];
            top |= z == 0 && x > 0;
            right |= Math.abs(x - 300) < 1e-9 && z > 0 && z < 200;
            bottom |= Math.abs(z - 200) < 1e-9 && x > 0 && x < 300;
            left |= x == 0 && z > 0 && z < 200;
        }
        assertTrue(top && right && bottom && left);
    }

    @Test
    void zoneOutlinesUseBlockCentresAndBlockEdges() {
        double[] ring = ResourceOutline.of(ResourceZone.point(10, 64, 20, 3));
        assertEquals(3.0, distance(ring, 0, 10.5, 20.5), 1e-9);
        assertEquals(64.1, ResourceOutline.height(ResourceZone.point(10, 64, 20, 3)), 1e-9);
        ResourceZone box = ResourceZone.cuboid(new BlockPos(0, 5, 0), new BlockPos(9, 9, 4));
        double[] edges = ResourceOutline.of(box);
        assertEquals(30, edges.length / 2);
        assertEquals(5.1, ResourceOutline.height(box), 1e-9);
        for (int index = 0; index < edges.length; index += 2) {
            assertTrue(edges[index] >= 0 && edges[index] <= 10 && edges[index + 1] >= 0 && edges[index + 1] <= 5);
        }
    }
}
