package de.ottoextra.crafting.werkstatt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationMemoryTest {
    @Test void stateIsKeptPerStation() {
        StationMemory m = new StationMemory();
        assertFalse(m.isOpen("zeugtisch"));
        assertEquals(WerkstattPanel.Tab.JOB, m.tab("kochstelle"));
        m.toggle("zeugtisch");
        m.setTab("zeugtisch", WerkstattPanel.Tab.QUEUE);
        m.markFire("zeugtisch");
        assertTrue(m.isOpen("zeugtisch"));
        assertFalse(m.isOpen("schreibstube"), "other stations unchanged");
        assertEquals(WerkstattPanel.Tab.QUEUE, m.tab("zeugtisch"));
        assertEquals(WerkstattPanel.Tab.JOB, m.tab("schreibstube"));
        assertTrue(m.hasFire("zeugtisch"));
        assertFalse(m.hasFire("schreibstube"));
        assertTrue(m.isOpen(""), "jobs without station count as Zeugtisch");
        m.toggle("zeugtisch");
        assertFalse(m.isOpen("zeugtisch"));
    }
}
