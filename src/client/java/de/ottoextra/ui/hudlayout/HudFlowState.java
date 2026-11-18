package de.ottoextra.ui.hudlayout;

import de.ottoextra.ui.hudlayout.HudSnap.Rect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure core of {@link HudFlow}: keeps the latest wanted rectangle of every participating HUD and
 * lays out each column of HUDs so that they never overlap while their total height fits on screen.
 *
 * <p>A column is every live HUD whose x-range touches another one in it (in practice one per screen
 * side; panels left and right never affect each other). Pinned HUDs (dragged in the layout editor)
 * and fixed obstacles (Xaero minimap, Lehen overlay) keep their rectangle. The other HUDs are sorted
 * by wanted position ({@link #sortKey}; ties by priority, then id) and packed in one dimension:
 * top-down each HUD is placed at its wanted y, but below every HUD placed before it and below fixed
 * blocks it overlaps; then bottom-up each HUD is pulled up as far as needed so the stack ends on
 * screen, again jumping over fixed blocks. Only a column that is taller than the screen overlaps.
 *
 * <p>The layout only depends on the set of wanted rectangles, never on the order in which HUDs
 * report, so it is the same for every HUD of a column and does not oscillate from frame to frame.
 * The reporting HUD's new size counts immediately; the others count with their latest report.
 */
public final class HudFlowState {
    public static final long EXPIRY_MS = 250L;
    public static final int GAP = 2;

    private record Entry(Rect rect, Rect desired, boolean pushUp, int priority, long reportedAt, boolean pinned) {
    }

    private final Map<String, Entry> entries = new HashMap<>();

    /**
     * Records where a HUD wants to be this frame and returns where it is drawn.
     *
     * @param pushUp true if the HUD is bottom anchored (ordered by its bottom edge), false otherwise
     */
    public Rect place(String id, Rect desired, boolean pushUp, int priority, long nowMs,
                      List<Rect> obstacles, int screenW, int screenH) {
        return place(id, desired, pushUp, priority, nowMs, obstacles, screenW, screenH, false);
    }

    /**
     * As above; a {@code pinned} HUD (the one being dragged in the layout editor) is never moved
     * and keeps exactly the wanted position, while the others make room for it.
     */
    public Rect place(String id, Rect desired, boolean pushUp, int priority, long nowMs,
                      List<Rect> obstacles, int screenW, int screenH, boolean pinned) {
        entries.values().removeIf(e -> nowMs - e.reportedAt() > EXPIRY_MS);
        Rect clamped = clampToScreen(desired, screenH);
        Entry previous = entries.get(id);
        entries.put(id, new Entry(previous == null ? clamped : previous.rect(), desired, pushUp, priority, nowMs,
                pinned));
        Map<String, Rect> layout = layoutColumn(id, obstacles, screenH);
        for (Map.Entry<String, Rect> slot : layout.entrySet()) {
            Entry e = entries.get(slot.getKey());
            entries.put(slot.getKey(), new Entry(slot.getValue(), e.desired(), e.pushUp(), e.priority(),
                    e.reportedAt(), e.pinned()));
        }
        return layout.get(id);
    }

    /** Last resolved rectangle of a HUD, or null if it is not (or no longer) reporting. */
    public Rect resolved(String id, long nowMs) {
        Entry e = entries.get(id);
        return e == null || nowMs - e.reportedAt() > EXPIRY_MS ? null : e.rect();
    }

    public void clear() {
        entries.clear();
    }

    /** Lays out the column that contains {@code id}; returns the slot of every HUD in it. */
    private Map<String, Rect> layoutColumn(String id, List<Rect> obstacles, int screenH) {
        List<String> column = column(id);
        Map<String, Rect> result = new HashMap<>();
        List<Rect> fixed = new ArrayList<>(obstacles);
        List<String> movable = new ArrayList<>();
        for (String member : column) {
            Entry e = entries.get(member);
            if (e.pinned()) {
                Rect r = clampToScreen(e.desired(), screenH);
                result.put(member, r);
                fixed.add(r);
            } else {
                movable.add(member);
            }
        }
        movable.sort(Comparator.<String>comparingInt(m -> sortKey(entries.get(m)))
                .thenComparingInt(m -> entries.get(m).priority())
                .thenComparing(Comparator.naturalOrder()));

        // Top-down: wanted y, but below everything placed before and below overlapping fixed blocks.
        List<Rect> placed = new ArrayList<>();
        for (String member : movable) {
            Rect want = clampToScreen(entries.get(member).desired(), screenH);
            int y = want.y();
            for (int guard = 0; guard <= placed.size() + fixed.size(); guard++) {
                Rect probe = new Rect(want.x(), y, want.w(), want.h());
                Rect hit = firstHit(probe, placed, fixed);
                if (hit == null) {
                    break;
                }
                y = Math.max(y, hit.bottom() + GAP);
            }
            placed.add(new Rect(want.x(), y, want.w(), want.h()));
        }

        // Bottom-up: pull the stack back on screen, never down, jumping over fixed blocks.
        for (int i = placed.size() - 1; i >= 0; i--) {
            Rect r = placed.get(i);
            int y = Math.min(r.y(), screenH - r.h());
            for (int j = i + 1; j < placed.size(); j++) {
                Rect below = placed.get(j);
                if (xOverlap(r, below) && below.y() >= r.y()) {
                    y = Math.min(y, below.y() - GAP - r.h());
                }
            }
            // The top-down spot is clear of fixed blocks; a panel pulled up from it jumps over any
            // fixed block it now overlaps.
            for (int guard = 0; y < r.y() && guard <= fixed.size(); guard++) {
                Rect hit = lowestHit(new Rect(r.x(), y, r.w(), r.h()), fixed);
                if (hit == null) {
                    break;
                }
                y = hit.y() - GAP - r.h();
            }
            // Clamp at the top; the column is taller than the screen if this overlaps (last resort).
            placed.set(i, new Rect(r.x(), Math.max(0, y), r.w(), r.h()));
        }
        for (int i = 0; i < movable.size(); i++) {
            result.put(movable.get(i), placed.get(i));
        }
        return result;
    }

    /** Ids of all live HUDs connected to {@code id} by intersecting x-ranges, sorted by id. */
    private List<String> column(String id) {
        List<String> column = new ArrayList<>();
        column.add(id);
        for (int i = 0; i < column.size(); i++) {
            Rect current = entries.get(column.get(i)).desired();
            for (Map.Entry<String, Entry> other : entries.entrySet()) {
                if (!column.contains(other.getKey()) && xOverlap(current, other.getValue().desired())) {
                    column.add(other.getKey());
                }
            }
        }
        column.sort(Comparator.naturalOrder());
        return column;
    }

    /**
     * Vertical order of a HUD in its column: top edge for top-anchored HUDs (also middle anchors,
     * which report {@code pushUp = false}), bottom edge for bottom-anchored ones.
     */
    private static int sortKey(Entry e) {
        return e.pushUp() ? e.desired().bottom() : e.desired().y();
    }

    /** First rectangle (placed panels first, then fixed blocks) that overlaps {@code r} including the gap. */
    private static Rect firstHit(Rect r, List<Rect> placed, List<Rect> fixed) {
        Rect best = null;
        for (List<Rect> list : List.of(placed, fixed)) {
            for (Rect b : list) {
                if (xOverlap(r, b) && r.y() < b.bottom() + GAP && b.y() < r.bottom() + GAP
                        && (best == null || b.y() < best.y())) {
                    best = b;
                }
            }
        }
        return best;
    }

    /** Fixed block overlapping {@code r} (including the gap) with the lowest top edge, or null. */
    private static Rect lowestHit(Rect r, List<Rect> fixed) {
        Rect best = null;
        for (Rect b : fixed) {
            if (xOverlap(r, b) && r.y() < b.bottom() + GAP && b.y() < r.bottom() + GAP
                    && (best == null || b.y() > best.y())) {
                best = b;
            }
        }
        return best;
    }

    private static boolean xOverlap(Rect a, Rect b) {
        return a.x() < b.right() && b.x() < a.right();
    }

    private static Rect clampToScreen(Rect r, int screenH) {
        return new Rect(r.x(), Math.max(0, Math.min(Math.max(0, screenH - r.h()), r.y())), r.w(), r.h());
    }
}
