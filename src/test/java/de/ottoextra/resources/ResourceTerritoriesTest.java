package de.ottoextra.resources;

import de.ottoextra.map.LehenPolygon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceTerritoriesTest {
    private static LehenPolygon square(String key, double min, double max) {
        double[] xs = {min, max, max, min};
        double[] zs = {min, min, max, max};
        return new LehenPolygon(key, xs, zs, (min + max) / 2, (min + max) / 2, min, min, max, max, true);
    }

    @Test
    void smallestContainingPolygonWins() {
        List<LehenPolygon> polygons = List.of(square("lehen_1", 0, 1000), square("lehen_2", 100, 200));
        assertEquals("lehen_2", ResourceTerritories.regionKeyAt(150, 150, polygons));
        assertEquals("lehen_1", ResourceTerritories.regionKeyAt(500, 500, polygons));
    }

    @Test
    void boundaryCountsAsInsideAndOutsideIsEmpty() {
        List<LehenPolygon> polygons = List.of(square("lehen_1", 0, 100));
        assertEquals("lehen_1", ResourceTerritories.regionKeyAt(100, 50, polygons));
        assertEquals("", ResourceTerritories.regionKeyAt(101, 50, polygons));
        assertEquals("", ResourceTerritories.regionKeyAt(0, 0, List.of()));
    }

    @Test
    void onlyTheOverworldHasLehen() {
        assertEquals("", ResourceTerritories.nameAt("minecraft:the_nether", 10, 10));
    }
}
