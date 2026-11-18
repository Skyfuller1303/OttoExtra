package de.ottoextra.crafting.werkstatt;

import de.ottoextra.crafting.CraftingStation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationTest {
    @Test void everyKnownCraftingStationIsDetectedByTitle() {
        for (CraftingStation s : CraftingStation.ALL) {
            String id = WerkstattText.stationId(s.title());
            assertNotNull(id, s.title());
            assertEquals(WerkstattText.normalize(s.title()), id);
            assertEquals(s.title(), WerkstattText.stationName(id));
        }
    }

    @Test void titleVariantsMapToTheSameStation() {
        assertEquals("zeugtisch", WerkstattText.stationId("Zeugtisch"));
        assertEquals("zeugtisch", WerkstattText.stationId("§8Zeugstisch"));
        assertEquals("zeugtisch", WerkstattText.stationId("Zeugtisch (Seite 2)"));
        assertEquals("rustbank", WerkstattText.stationId("Rüstbank"));
        assertEquals("kochstelle", WerkstattText.stationId("  KOCHSTELLE "));
        assertNull(WerkstattText.stationId("Truhe"));
        assertNull(WerkstattText.stationId("Zeugtischler"));
        assertNull(WerkstattText.stationId(null));
        assertEquals("Zeugtisch", WerkstattText.stationName(""));
    }

    @Test void subMenuTitles() {
        assertTrue(WerkstattText.isSubMenuTitle("Mehr Sonstiges"));
        assertTrue(WerkstattText.isSubMenuTitle("§8Mehr Pigmente"));
        assertFalse(WerkstattText.isSubMenuTitle("Mehr"));
        assertFalse(WerkstattText.isSubMenuTitle("Zeugtisch"));
    }
}
