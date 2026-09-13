package de.ottoextra.regions;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionActivityHudTest {
    @Test
    void activityHudDefaultsMatchTheCompactSidebar() {
        OttoExtraConfig.Regions regions = new OttoExtraConfig().regions;

        assertTrue(regions.activityHudEnabled);
        assertEquals("MIDDLE_LEFT", regions.activityHudPosition);
        assertEquals(0, regions.activityHudOffsetX);
        assertEquals(0, regions.activityHudOffsetY);
        assertEquals(8, regions.activityHudMaxEntries);
        assertEquals(1.0f, regions.activityHudScale);
        assertTrue(regions.activityHudShowLastUpdated);
        assertTrue(regions.activityHudShowClock);
        assertEquals(0.5f, regions.activityHudCustomX);
        assertEquals(0.5f, regions.activityHudCustomY);
        assertEquals(120, regions.activityHudRefreshSeconds);
    }

    @Test
    void regionsModuleRemainsRegisteredWhenMasterSwitchStartsDisabled() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.regions.enabled = false;

        assertTrue(new RegionsModule().enabled(config));
    }

    @Test
    void clockUsesLocalTwentyFourHourTime() {
        long instant = LocalDate.of(2026, 8, 22).atTime(14, 37)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        assertEquals("14:37", RegionActivityHud.formatTime(instant));
    }

    @Test
    void invalidStoredLayoutValuesAreRepaired() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("""
                {"regions":{"activityHudPosition":"",
                "activityHudOffsetX":2000,"activityHudOffsetY":-2000,
                "activityHudMaxEntries":0,"activityHudScale":9,
                "activityHudCustomX":-1,"activityHudCustomY":2,
                "activityHudRefreshSeconds":1}}
                """);

        assertEquals("MIDDLE_LEFT", config.regions.activityHudPosition);
        assertEquals(0, config.regions.activityHudOffsetX);
        assertEquals(0, config.regions.activityHudOffsetY);
        assertEquals(8, config.regions.activityHudMaxEntries);
        assertEquals(1.0f, config.regions.activityHudScale);
        assertEquals(0.5f, config.regions.activityHudCustomX);
        assertEquals(0.5f, config.regions.activityHudCustomY);
        assertEquals(120, config.regions.activityHudRefreshSeconds);
    }

    @Test
    void activityRefreshRateUsesSafeSecondBounds() {
        assertEquals(5, RegionDataService.sanitizeActivityRefreshSeconds(1));
        assertEquals(45, RegionDataService.sanitizeActivityRefreshSeconds(45));
        assertEquals(3_600,
                RegionDataService.sanitizeActivityRefreshSeconds(10_000));
    }

    @Test
    void positionsAndOffsetsStayInsideTheScreen() {
        OttoExtraConfig.Regions regions = new OttoExtraConfig().regions;
        regions.activityHudPosition = "MIDDLE_RIGHT";
        assertArrayEquals(new int[]{192, 70},
                RegionActivityHud.position(300, 200, 104, 60, regions));

        regions.activityHudOffsetX = 999;
        regions.activityHudOffsetY = -999;
        assertArrayEquals(new int[]{196, 0},
                RegionActivityHud.position(300, 200, 104, 60, regions));
    }

    @Test
    void customPositionUsesResolutionIndependentCoordinates() {
        OttoExtraConfig.Regions regions = new OttoExtraConfig().regions;
        regions.activityHudPosition = "CUSTOM";
        regions.activityHudCustomX = 0.25f;
        regions.activityHudCustomY = 0.8f;

        assertArrayEquals(new int[]{50, 120},
                RegionActivityHud.position(300, 200, 100, 50, regions));
        assertEquals(0.5f, RegionActivityHud.clampScale(0.1f));
        assertEquals(2.0f, RegionActivityHud.clampScale(3.0f));
    }

    @Test
    void lastUpdateAndClockCanBeControlledIndependently() {
        OttoExtraConfig.Regions regions = new OttoExtraConfig().regions;
        assertEquals(RegionActivityHud.FooterMode.UPDATED_AND_CLOCK,
                RegionActivityHud.footerMode(regions));

        regions.activityHudShowLastUpdated = false;
        assertEquals(RegionActivityHud.FooterMode.CLOCK_ONLY,
                RegionActivityHud.footerMode(regions));

        regions.activityHudShowClock = false;
        assertEquals(RegionActivityHud.FooterMode.HIDDEN,
                RegionActivityHud.footerMode(regions));

        regions.activityHudShowLastUpdated = true;
        assertEquals(RegionActivityHud.FooterMode.UPDATED_ONLY,
                RegionActivityHud.footerMode(regions));
    }
}
