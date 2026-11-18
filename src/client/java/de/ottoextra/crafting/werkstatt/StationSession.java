package de.ottoextra.crafting.werkstatt;

/**
 * Which station the open screens belong to, free of Minecraft. A screen with a station title starts
 * (or switches) the session. Every further container screen opened from it (sub-menus such as
 * "Mehr Sonstiges" with their own title, the 9-slot loading menu in between, other layouts) stays
 * part of that station until the player closes the GUI or an unrelated, non-container screen opens.
 * A container with a title that is neither a station, a known sub-menu nor the loading menu only counts
 * when its layout (number of container slots) matches the station screen's signature; any other
 * container is foreign and ends the session, so nothing in it is ever clicked.
 * A short gap without any screen ({@link #GRACE_MS}) is allowed, because the server may close one
 * inventory before it opens the next.
 */
final class StationSession {
    /** Gap without a screen that still continues the session. */
    static final long GRACE_MS = 600L;

    enum ScreenType { NONE, CONTAINER, OTHER }

    /**
     * @param station      station id of the current screen, null if it is not part of a station
     * @param subMenu      the screen belongs to the station without a station title
     * @param unknownTitle a sub-menu whose title matches neither the station nor a known sub-menu pattern
     */
    record State(String station, boolean subMenu, boolean unknownTitle) {
        static final State NONE = new State(null, false, false);
    }

    private String station;
    /** Container slot count of the station screen that started the session; -1 while unknown. */
    private int signature = -1;
    private long noScreenSince = -1;

    String station() {
        return station;
    }

    /**
     * Call for every screen change and every tick. {@code loading} is Ottonien's 9-slot loading menu,
     * {@code size} the number of container slots of the screen (without the player inventory).
     */
    State update(ScreenType type, String title, boolean loading, int size, long now) {
        if (type == ScreenType.OTHER) {
            station = null;
            noScreenSince = -1;
            return State.NONE;
        }
        if (type == ScreenType.NONE) {
            if (station != null) {
                if (noScreenSince < 0) {
                    noScreenSince = now;
                } else if (now - noScreenSince > GRACE_MS) {
                    station = null;
                    noScreenSince = -1;
                }
            }
            return State.NONE;
        }
        if (station != null && noScreenSince >= 0 && now - noScreenSince > GRACE_MS) {
            station = null;
        }
        noScreenSince = -1;
        String id = WerkstattText.stationId(title);
        if (id != null) {
            station = id;
            signature = size;
            return new State(id, false, false);
        }
        if (station == null) {
            return State.NONE;
        }
        boolean known = loading || title == null || title.isBlank() || WerkstattText.isSubMenuTitle(title);
        if (!known && !matchesSignature(size)) {
            // A foreign container (other title and layout) inside the session: not part of the station.
            reset();
            return State.NONE;
        }
        return new State(station, true, !known);
    }

    /** The layout of a container equals the station screen's layout. */
    boolean matchesSignature(int size) {
        return signature > 0 && size == signature;
    }

    void reset() {
        station = null;
        signature = -1;
        noScreenSince = -1;
    }
}
