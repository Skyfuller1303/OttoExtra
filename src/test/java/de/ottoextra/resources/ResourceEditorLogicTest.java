package de.ottoextra.resources;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceEditorLogicTest {
    private static ResourceSnapshot entry(String id, ResourceZone zone) {
        return new ResourceSnapshot(id, id, "IRON_MINE", "Nord", "minecraft:overworld", zone,
                "READY", 100, "", "", -1, 0);
    }

    // ---- cuboid to point ----

    @Test
    void cuboidBecomesPointAtItsCentreWithRadiusFromTheLargerSide() {
        // 49 x 18 x 33 blocks: dx = 49, dz = 33 -> radius ceil(49 / 2) = 25
        ResourceZone box = ResourceZone.cuboid(new BlockPos(-1284, 60, 3402), new BlockPos(-1236, 77, 3434));
        assertEquals(49, box.sizeX());
        assertEquals(33, box.sizeZ());
        assertEquals(18, box.sizeY());
        ResourceZone point = box.asPoint();
        assertEquals(ResourceZone.Type.POINT, point.type());
        assertEquals(-1260, point.x());
        assertEquals(68, point.y());
        assertEquals(3418, point.z());
        assertEquals(25, point.radius());
    }

    @Test
    void conversionRadiusIsClampedToTheAllowedRange() {
        assertEquals(1, ResourceZone.cuboid(new BlockPos(0, 0, 0), new BlockPos(0, 5, 0)).asPoint().radius());
        assertEquals(2, ResourceZone.cuboid(new BlockPos(0, 0, 0), new BlockPos(2, 0, 0)).asPoint().radius());
        assertEquals(64, ResourceZone.cuboid(new BlockPos(0, 0, 0), new BlockPos(500, 0, 10)).asPoint().radius());
        assertEquals(64, ResourceZone.cuboid(new BlockPos(0, 0, 0), new BlockPos(127, 0, 0)).asPoint().radius());
        assertEquals(64, ResourceZone.cuboid(new BlockPos(0, 0, 0), new BlockPos(0, 0, 128)).asPoint().radius());
    }

    @Test
    void negativeCentresRoundDownAndPointsStayUnchanged() {
        ResourceZone box = ResourceZone.cuboid(new BlockPos(-3, -3, -3), new BlockPos(0, 0, 0));
        assertEquals(-2, box.asPoint().x());
        ResourceZone point = ResourceZone.point(1, 2, 3, 4);
        assertSame(point, point.asPoint());
    }

    @Test
    void convertedPointStillContainsTheCentreOfTheBox() {
        ResourceZone box = ResourceZone.cuboid(new BlockPos(10, 0, 10), new BlockPos(30, 40, 20));
        assertTrue(box.asPoint().contains(new BlockPos(20, 20, 15)));
    }

    // ---- kind tiles and scope ----

    @Test
    void scopeMapsToDepositAndMineTypes() {
        assertEquals("IRON_MINE", ResourcePolicy.compose("IRON", true));
        assertEquals("IRON_DEPOSIT", ResourcePolicy.compose("IRON", false));
        assertEquals("COAL_MINE", ResourcePolicy.compose("COAL", true));
        assertEquals("SALT_DEPOSIT", ResourcePolicy.compose("SALT", false));
    }

    @Test
    void kindsWithoutScopeIgnoreTheSwitch() {
        for (boolean mine : new boolean[] {true, false}) {
            assertEquals("PEAT_BOG", ResourcePolicy.compose("PEAT", mine));
            assertEquals("CLAY_PIT", ResourcePolicy.compose("CLAY", mine));
            assertEquals("MONASTERY_GARDEN", ResourcePolicy.compose("GARDEN", mine));
            assertEquals("APIARY", ResourcePolicy.compose("APIARY", mine));
            assertEquals("MARKER", ResourcePolicy.compose("MARKER", mine));
        }
        assertTrue(ResourcePolicy.hasScope("IRON"));
        assertTrue(ResourcePolicy.hasScope("COAL"));
        assertTrue(ResourcePolicy.hasScope("SALT"));
        for (String base : List.of("PEAT", "CLAY", "GARDEN", "APIARY", "MARKER")) {
            assertFalse(ResourcePolicy.hasScope(base));
        }
    }

    @Test
    void everyTypeMapsToATileAndBack() {
        for (String type : ResourcePolicy.TYPES) {
            String base = ResourcePolicy.baseOf(type);
            assertTrue(ResourcePolicy.BASES.contains(base), type);
            assertEquals(type, ResourcePolicy.compose(base, ResourcePolicy.isMineType(type)));
        }
        assertEquals(ResourcePolicy.TYPES.size(), ResourcePolicy.BASES.size() + 3);
        assertNull(ResourcePolicy.baseOf("NOPE"));
        assertNull(ResourcePolicy.baseOf(null));
    }

    // ---- overlap rule ----

    @Test
    void nearestCentreWinsWhenZonesOverlap() {
        ResourceSnapshot wide = entry("wide", ResourceZone.point(0, 64, 0, 40));
        ResourceSnapshot near = entry("near", ResourceZone.point(22, 64, 0, 10));
        ResourceSnapshot far = entry("far", ResourceZone.point(-30, 64, 0, 40));
        // Standing at x=20: all three contain the point, the centre of "near" is closest.
        assertSame(near, ResourceOverlap.nearest(List.of(wide, near, far), 20, 64, 0));
        // Standing at x=-25: "far" is closest, order does not matter.
        assertSame(far, ResourceOverlap.nearest(List.of(near, far, wide), -25, 64, 0));
    }

    @Test
    void nearestUsesCuboidCentreAndFirstWinsATie() {
        ResourceSnapshot box = entry("box", ResourceZone.cuboid(new BlockPos(0, 0, 0), new BlockPos(100, 10, 100)));
        ResourceSnapshot point = entry("point", ResourceZone.point(51, 5, 50, 5));
        assertSame(point, ResourceOverlap.nearest(List.of(box, point), 52, 5, 50));
        ResourceSnapshot a = entry("a", ResourceZone.point(10, 0, 0, 5));
        ResourceSnapshot b = entry("b", ResourceZone.point(-10, 0, 0, 5));
        assertSame(a, ResourceOverlap.nearest(List.of(a, b), 0, 0, 0));
        assertNull(ResourceOverlap.nearest(List.of(), 0, 0, 0));
    }

    // ---- editor helpers ----

    @Test
    void centreFieldAcceptsSpacesAndCommasAndRejectsGarbage() {
        assertArrayEquals(new int[] {-1260, 64, 3418}, ResourceEditScreen.parseCenter("-1260 64 3418"));
        assertArrayEquals(new int[] {1, 2, 3}, ResourceEditScreen.parseCenter(" 1, 2,3 "));
        assertNull(ResourceEditScreen.parseCenter("1 2"));
        assertNull(ResourceEditScreen.parseCenter("1 2 3 4"));
        assertNull(ResourceEditScreen.parseCenter("a b c"));
        assertNull(ResourceEditScreen.parseCenter("30000001 0 0"));
        assertNull(ResourceEditScreen.parseCenter("0 2049 0"));
        assertNull(ResourceEditScreen.parseCenter(""));
        assertNull(ResourceEditScreen.parseCenter(null));
        assertEquals("1 2 3", ResourceEditScreen.centerText(ResourceZone.point(1, 2, 3, 4)));
    }

    @Test
    void recoveryTimesAreShownAsHoursAndMinutes() {
        assertEquals("2:00", ResourceEditScreen.hoursText(120));
        assertEquals("4:00", ResourceEditScreen.hoursText(240));
        assertEquals("0:30", ResourceEditScreen.hoursText(30));
        assertEquals("1:05", ResourceEditScreen.hoursText(65));
    }
}
