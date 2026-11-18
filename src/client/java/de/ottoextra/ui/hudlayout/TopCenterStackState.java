package de.ottoextra.ui.hudlayout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Pure state of {@link TopCenterStack}: which elements stand in the top-centre column, in which order
 * and how high they are. Free of Minecraft so the stacking and the boss bar offset can be tested.
 */
final class TopCenterStackState {
    /** Space between two elements of the column. */
    static final int GAP = 2;
    /** Top of the column without boss bars. */
    static final int MARGIN = 4;
    /** Vanilla BossBarHud: first bar at y=12, every further bar 19 px lower, bars 5 px high. */
    static final int BOSS_FIRST_Y = 12;
    static final int BOSS_STEP = 19;
    static final int BOSS_BAR_H = 5;
    /** Space between the last boss bar and the column. */
    static final int BOSS_GAP = 4;
    /** An element that did not report for this long has left the column. */
    static final long ENTRY_TTL_MS = 250L;

    private record Entry(int order, int height, long at) {
    }

    private final Map<String, Entry> entries = new HashMap<>();

    /**
     * Reports the height of an element for this frame and returns the y at which to draw it: the top of
     * the column plus every live element with a smaller order (ties by id), each followed by {@link #GAP}.
     */
    synchronized int claim(String id, int order, int height, int top, long now) {
        entries.put(id, new Entry(order, Math.max(0, height), now));
        int y = top;
        Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> other = it.next();
            Entry e = other.getValue();
            if (now - e.at() > ENTRY_TTL_MS) {
                it.remove();
                continue;
            }
            if (e.order() < order || (e.order() == order && other.getKey().compareTo(id) < 0)) {
                y += e.height() + GAP;
            }
        }
        return y;
    }

    synchronized void release(String id) {
        entries.remove(id);
    }

    synchronized void clear() {
        entries.clear();
    }

    /**
     * Top of the column below the boss bars that vanilla actually draws: it stops adding bars once the
     * next one would start at or below a third of the screen height.
     */
    static int topBelowBossBars(int bars, int screenH) {
        if (bars <= 0) {
            return MARGIN;
        }
        int y = BOSS_FIRST_Y;
        int last = y;
        for (int i = 0; i < bars; i++) {
            last = y;
            y += BOSS_STEP;
            if (y >= screenH / 3) {
                break;
            }
        }
        return last + BOSS_BAR_H + BOSS_GAP;
    }
}
