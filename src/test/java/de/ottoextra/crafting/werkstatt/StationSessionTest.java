package de.ottoextra.crafting.werkstatt;

import de.ottoextra.crafting.werkstatt.StationSession.ScreenType;
import de.ottoextra.crafting.werkstatt.StationSession.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationSessionTest {
    @Test void subMenusWithOtherTitlesStayPartOfTheStation() {
        StationSession s = new StationSession();
        State st = s.update(ScreenType.CONTAINER, "Bognertisch", false, 54, 0);
        assertEquals("bognertisch", st.station());
        assertFalse(st.subMenu());
        // Navigation click: loading menu, then a sub-menu with its own title and fewer rows.
        st = s.update(ScreenType.CONTAINER, "", true, 9, 100);
        assertEquals("bognertisch", st.station());
        assertTrue(st.subMenu());
        assertFalse(st.unknownTitle());
        st = s.update(ScreenType.CONTAINER, "Mehr Pigmente", false, 27, 200);
        assertEquals("bognertisch", st.station());
        assertTrue(st.subMenu());
        assertFalse(st.unknownTitle());
        st = s.update(ScreenType.CONTAINER, "Farben", false, 54, 300);
        assertEquals("bognertisch", st.station());
        assertTrue(st.unknownTitle(), "unmatched titles are reported for logging");
    }

    @Test void shortGapBetweenInventoriesKeepsTheSession() {
        StationSession s = new StationSession();
        s.update(ScreenType.CONTAINER, "Kochstelle", false, 54, 0);
        assertNull(s.update(ScreenType.NONE, "", false, 0, 100).station());
        assertEquals("kochstelle", s.station());
        State st = s.update(ScreenType.CONTAINER, "Mehr Sonstiges", false, 54, 100 + StationSession.GRACE_MS);
        assertEquals("kochstelle", st.station());
    }

    @Test void closingTheGuiOrAnUnrelatedScreenEndsTheSession() {
        StationSession s = new StationSession();
        s.update(ScreenType.CONTAINER, "Zeugtisch", false, 54, 0);
        s.update(ScreenType.NONE, "", false, 0, 100);
        s.update(ScreenType.NONE, "", false, 0, 100 + StationSession.GRACE_MS + 1);
        assertNull(s.station());
        assertNull(s.update(ScreenType.CONTAINER, "Mehr Sonstiges", false, 54, 2000).station(),
                "a sub-menu title alone never starts a session");

        s.update(ScreenType.CONTAINER, "Zeugtisch", false, 54, 3000);
        assertNull(s.update(ScreenType.OTHER, "Chat", false, 0, 3100).station());
        assertNull(s.update(ScreenType.CONTAINER, "Truhe", false, 54, 3200).station());
    }

    @Test void lateReopenAfterTheGraceGapIsNotASubMenu() {
        StationSession s = new StationSession();
        s.update(ScreenType.CONTAINER, "Zeugtisch", false, 54, 0);
        s.update(ScreenType.NONE, "", false, 0, 100);
        assertNull(s.update(ScreenType.CONTAINER, "Truhe", false, 54, 100 + StationSession.GRACE_MS + 50).station());
    }

    @Test void foreignContainerWithOtherLayoutIsNeverPartOfTheStation() {
        StationSession s = new StationSession();
        s.update(ScreenType.CONTAINER, "Zeugtisch", false, 54, 0);
        // Unknown title, other layout (a chest the server opened): foreign, the session ends.
        assertNull(s.update(ScreenType.CONTAINER, "Truhe", false, 27, 100).station());
        assertNull(s.station());
        // A sub-menu title after that does not revive the session.
        assertNull(s.update(ScreenType.CONTAINER, "Mehr Sonstiges", false, 54, 200).station());
        // Unknown title with the station's layout stays a (logged) sub-menu.
        s.update(ScreenType.CONTAINER, "Zeugtisch", false, 54, 300);
        assertTrue(s.matchesSignature(54));
        assertFalse(s.matchesSignature(27));
        State st = s.update(ScreenType.CONTAINER, "Werkzeuge", false, 54, 400);
        assertEquals("zeugtisch", st.station());
        assertTrue(st.unknownTitle());
    }

    @Test void anotherStationTitleSwitchesTheSession() {
        StationSession s = new StationSession();
        s.update(ScreenType.CONTAINER, "Zeugtisch", false, 54, 0);
        State st = s.update(ScreenType.CONTAINER, "Rüstbank", false, 54, 100);
        assertEquals("rustbank", st.station());
        assertFalse(st.subMenu());
    }
}
