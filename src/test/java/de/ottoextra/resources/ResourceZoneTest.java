package de.ottoextra.resources;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceZoneTest {
    @Test
    void pointIncludesRadiusBoundary() {
        ResourceZone zone = ResourceZone.point(10, 64, -5, 3);
        assertTrue(zone.contains(new BlockPos(13, 64, -5)));
        assertFalse(zone.contains(new BlockPos(14, 64, -5)));
    }

    @Test
    void cuboidNormalizesCornersAndIncludesEdges() {
        ResourceZone zone = ResourceZone.cuboid(new BlockPos(20, 80, 10),
                new BlockPos(5, 60, -4));
        assertEquals(5, zone.minX());
        assertEquals(20, zone.maxX());
        assertTrue(zone.contains(new BlockPos(5, 60, -4)));
        assertTrue(zone.contains(new BlockPos(20, 80, 10)));
        assertFalse(zone.contains(new BlockPos(21, 80, 10)));
    }

    @Test
    void confirmationPaddingIncludesPlayerNextToSelectedBlocks() {
        ResourceZone zone = ResourceZone.cuboid(new BlockPos(-555, 55, 3178),
                new BlockPos(-540, 60, 3194));
        assertFalse(zone.contains(new BlockPos(-547, 61, 3186)));
        assertTrue(zone.contains(new BlockPos(-547, 61, 3186), 6));
        assertFalse(zone.contains(new BlockPos(-547, 67, 3186), 6));
    }
}
