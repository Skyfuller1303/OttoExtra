package de.ottoextra.crafting.werkstatt;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WerkstattHudConfigTest {

    @Test
    void newConfigPutsTheBarAtTheTopCentre() {
        OttoExtraConfig c = new OttoExtraConfig();
        c.repair();
        assertEquals("TOP_CENTER", c.crafting.werkstattHudCorner);
        assertEquals(0, c.crafting.werkstattHudOffsetX);
        assertEquals(0, c.crafting.werkstattHudOffsetY);
        assertEquals(1, c.crafting.werkstattHudLayoutVersion);
    }

    @Test
    void neverMovedOldDefaultMovesToTheTopCentreOnce() {
        OttoExtraConfig c = new OttoExtraConfig();
        c.restoreFrom("{\"crafting\":{\"werkstattHudCorner\":\"TOP_LEFT\",\"werkstattHudOffsetX\":4,"
                + "\"werkstattHudOffsetY\":4}}");
        assertEquals("TOP_CENTER", c.crafting.werkstattHudCorner);
        assertEquals(0, c.crafting.werkstattHudOffsetX);
        assertEquals(1, c.crafting.werkstattHudLayoutVersion);
    }

    @Test
    void movedBarAndLaterChoicesStay() {
        OttoExtraConfig moved = new OttoExtraConfig();
        moved.restoreFrom("{\"crafting\":{\"werkstattHudCorner\":\"TOP_LEFT\",\"werkstattHudOffsetX\":30,"
                + "\"werkstattHudOffsetY\":4}}");
        assertEquals("TOP_LEFT", moved.crafting.werkstattHudCorner);
        assertEquals(30, moved.crafting.werkstattHudOffsetX);

        OttoExtraConfig migrated = new OttoExtraConfig();
        migrated.restoreFrom("{\"crafting\":{\"werkstattHudCorner\":\"TOP_LEFT\",\"werkstattHudOffsetX\":4,"
                + "\"werkstattHudOffsetY\":4,\"werkstattHudLayoutVersion\":1}}");
        assertEquals("TOP_LEFT", migrated.crafting.werkstattHudCorner);
    }
}
