package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcesConfigTest {
    @Test
    void defaultsKeepSharingAndAutoStartOff() {
        ResourcesConfig resources = new ResourcesConfig();
        assertTrue(resources.enabled);
        assertTrue(resources.hintsEnabled);
        assertFalse(resources.sharingEnabled);
        assertFalse(resources.shareStartedBy);
        assertFalse(resources.autoStartOnBreak);
        assertFalse(resources.hudShowAll);
        assertEquals("https://ottoextra.dev", resources.shareBaseUrl);
    }

    @Test
    void sanitizeRestoresInvalidValues() {
        ResourcesConfig resources = new ResourcesConfig();
        resources.shareBaseUrl = "http://example.org";
        resources.hudMaxRows = 500;
        resources.hudCorner = "MITTE";
        resources.hudOffsetY = 5000;
        resources.hudScale = Float.NaN;
        resources.sanitize();
        assertEquals("https://ottoextra.dev", resources.shareBaseUrl);
        assertEquals(30, resources.hudMaxRows);
        assertEquals("TOP_RIGHT", resources.hudCorner);
        assertEquals(ResourceHudPlacement.MAX_OFFSET, resources.hudOffsetY);
        assertEquals(1.0f, resources.hudScale);
    }

    @Test
    void oldDistanceFromTopBecomesTheOffsetFromTheTopRightCorner() {
        ResourcesConfig resources = new ResourcesConfig();
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("hudTop", 90);
        ResourcesConfig.migrateHudTop(json, resources);
        assertEquals("TOP_RIGHT", resources.hudCorner);
        assertEquals(86, resources.hudOffsetY);

        ResourcesConfig placed = new ResourcesConfig();
        placed.hudCorner = "BOTTOM_LEFT";
        json.addProperty("hudCorner", "BOTTOM_LEFT");
        ResourcesConfig.migrateHudTop(json, placed);
        assertEquals("BOTTOM_LEFT", placed.hudCorner);
        assertEquals(ResourceHudPlacement.DEFAULT_OFFSET_Y, placed.hudOffsetY);
    }
}
