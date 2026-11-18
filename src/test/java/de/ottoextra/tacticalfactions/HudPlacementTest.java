package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudPlacementTest {
    private static final int W = 400;
    private static final int H = 240;
    private static final int M = 4;

    @Test void zeroOffsetSitsInTheAnchorCorner() {
        assertArrayEquals(new int[]{4, 4}, HudPlacement.position(HudCorner.TOP_LEFT, 0, 0, W, H, 100, 50, M));
        assertArrayEquals(new int[]{296, 4}, HudPlacement.position(HudCorner.TOP_RIGHT, 0, 0, W, H, 100, 50, M));
        assertArrayEquals(new int[]{4, 186}, HudPlacement.position(HudCorner.BOTTOM_LEFT, 0, 0, W, H, 100, 50, M));
        assertArrayEquals(new int[]{296, 186}, HudPlacement.position(HudCorner.BOTTOM_RIGHT, 0, 0, W, H, 100, 50, M));
    }

    @Test void offsetsMoveRelativeToTheCornerAndAreClampedOnScreen() {
        assertArrayEquals(new int[]{276, 24}, HudPlacement.position(HudCorner.TOP_RIGHT, -20, 20, W, H, 100, 50, M));
        assertArrayEquals(new int[]{300, 0}, HudPlacement.position(HudCorner.TOP_RIGHT, 500, -500, W, H, 100, 50, M));
        assertArrayEquals(new int[]{0, 190}, HudPlacement.position(HudCorner.BOTTOM_LEFT, -500, 500, W, H, 100, 50, M));
        // Panel larger than the screen never gives negative coordinates.
        assertArrayEquals(new int[]{0, 0}, HudPlacement.position(HudCorner.BOTTOM_RIGHT, 0, 0, 100, 40, 200, 90, M));
    }

    @Test void dragPicksTheNearestCornerFromThePanelCentre() {
        assertEquals(HudCorner.TOP_LEFT, HudPlacement.fromDrag(10, 10, 100, 50, W, H, M).corner());
        assertEquals(HudCorner.TOP_RIGHT, HudPlacement.fromDrag(250, 10, 100, 50, W, H, M).corner());
        assertEquals(HudCorner.BOTTOM_LEFT, HudPlacement.fromDrag(10, 150, 100, 50, W, H, M).corner());
        assertEquals(HudCorner.BOTTOM_RIGHT, HudPlacement.fromDrag(250, 150, 100, 50, W, H, M).corner());
    }

    @Test void dragStoresTheOffsetFromTheChosenCornerAndRoundTrips() {
        HudPlacement.Placement p = HudPlacement.fromDrag(270, 30, 100, 50, W, H, M);
        assertEquals(HudCorner.TOP_RIGHT, p.corner());
        assertEquals(-26, p.offsetX());
        assertEquals(26, p.offsetY());
        assertArrayEquals(new int[]{270, 30},
                HudPlacement.position(p.corner(), p.offsetX(), p.offsetY(), W, H, 100, 50, M));

        HudPlacement.Placement q = HudPlacement.fromDrag(40, 160, 100, 50, W, H, M);
        assertEquals(HudCorner.BOTTOM_LEFT, q.corner());
        assertArrayEquals(new int[]{40, 160},
                HudPlacement.position(q.corner(), q.offsetX(), q.offsetY(), W, H, 100, 50, M));
    }

    @Test void dragOutsideTheScreenIsClampedBeforeComputingTheOffset() {
        HudPlacement.Placement p = HudPlacement.fromDrag(-80, -30, 100, 50, W, H, M);
        assertEquals(HudCorner.TOP_LEFT, p.corner());
        assertEquals(-4, p.offsetX());
        assertEquals(-4, p.offsetY());
        assertEquals(1000, HudPlacement.clampOffset(5000));
        assertEquals(-1000, HudPlacement.clampOffset(-5000));
    }

    @Test void configRepairClampsStoredOffsets() {
        TacticalConfig config = new TacticalConfig();
        config.squadHudOffsetX = 99999;
        config.squadHudOffsetY = -99999;
        config.repair();
        assertEquals(1000, config.squadHudOffsetX);
        assertEquals(-1000, config.squadHudOffsetY);
        assertEquals(0, new TacticalConfig().squadHudOffsetX);
    }

    @Test void scaleIsClampedAndConfigRepairsIt() {
        assertEquals(0.5f, HudPlacement.clampScale(0.1f));
        assertEquals(2.0f, HudPlacement.clampScale(9f));
        assertEquals(1.0f, HudPlacement.clampScale(Float.NaN));
        assertEquals(150, HudPlacement.scaled(100, 1.5f));
        TacticalConfig config = new TacticalConfig();
        assertEquals(1.0f, config.squadHudScale);
        config.squadHudScale = 7f;
        config.repair();
        assertEquals(2.0f, config.squadHudScale);
        config.squadHudScale = Float.NaN;
        config.repair();
        assertEquals(1.0f, config.squadHudScale);
    }

    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temp;

    @Test void droppedSquadHudKeepsItsPositionAfterSaveAndReloadForEveryCornerAndScale() {
        java.nio.file.Path file = temp.resolve("config.json");
        for (float scale : new float[]{0.75f, 1.0f, 1.5f}) {
            int w = HudPlacement.scaled(110, scale);
            int h = HudPlacement.scaled(70, scale);
            for (int y = 0; y <= H - h; y += 19) {
                for (int x = 0; x <= W - w; x += 41) {
                    HudPlacement.Placement p = HudPlacement.fromDrag(x, y, w, h, W, H, M);
                    TacticalConfig config = new TacticalConfig();
                    config.squadHudScale = scale;
                    config.squadHudCorner = p.corner();
                    config.squadHudOffsetX = p.offsetX();
                    config.squadHudOffsetY = p.offsetY();
                    config.save(file);
                    TacticalConfig loaded = TacticalConfig.load(file);
                    assertEquals(scale, loaded.squadHudScale);
                    assertArrayEquals(new int[]{x, y}, HudPlacement.position(loaded.squadHudCorner,
                            loaded.squadHudOffsetX, loaded.squadHudOffsetY, W, H, w, h, M),
                            "scale " + scale + " drop " + x + "," + y);
                }
            }
        }
    }
}
