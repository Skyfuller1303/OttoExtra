package de.ottoextra.ui.hudlayout;

import java.util.ArrayList;
import java.util.List;

/** Pure snapping and scale math of the HUD layout editor. */
public final class HudSnap {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.0f;

    private HudSnap() {
    }

    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public int centerX() {
            return x + w / 2;
        }

        public int centerY() {
            return y + h / 2;
        }
    }

    /** Snapped top-left position plus the guide lines (screen x / y) that were hit. */
    public record Result(int x, int y, List<Integer> guideX, List<Integer> guideY) {
    }

    public static float clampScale(float value) {
        if (Float.isNaN(value)) {
            return 1.0f;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    /** Rounds to 0.05 and clamps so repeated wheel steps never drift. */
    public static float stepScale(float value) {
        return clampScale(Math.round(value * 20f) / 20f);
    }

    public static int scaled(int size, float scale) {
        return Math.round(size * scale);
    }

    /**
     * Snaps the left/centre/right and top/centre/bottom lines of {@code moving} to the screen
     * edges, the margin lines, the screen centre lines and the same lines of the other boxes when
     * one is within {@code threshold} pixels. {@code disabled} (Alt held) returns the input.
     */
    public static Result snap(Rect moving, List<Rect> others, int screenW, int screenH, int margin,
                              int threshold, boolean disabled) {
        List<Integer> guideX = new ArrayList<>();
        List<Integer> guideY = new ArrayList<>();
        if (disabled) {
            return new Result(moving.x(), moving.y(), guideX, guideY);
        }
        List<Integer> targetsX = new ArrayList<>(List.of(0, margin, screenW / 2, screenW - margin, screenW));
        List<Integer> targetsY = new ArrayList<>(List.of(0, margin, screenH / 2, screenH - margin, screenH));
        for (Rect other : others) {
            targetsX.addAll(List.of(other.x(), other.centerX(), other.right()));
            targetsY.addAll(List.of(other.y(), other.centerY(), other.bottom()));
        }
        int dx = best(new int[]{moving.x(), moving.centerX(), moving.right()}, targetsX, threshold, guideX);
        int dy = best(new int[]{moving.y(), moving.centerY(), moving.bottom()}, targetsY, threshold, guideY);
        return new Result(moving.x() + dx, moving.y() + dy, guideX, guideY);
    }

    private static int best(int[] lines, List<Integer> targets, int threshold, List<Integer> guides) {
        int bestDelta = 0;
        int bestAbs = Integer.MAX_VALUE;
        int guide = 0;
        for (int line : lines) {
            for (int target : targets) {
                int delta = target - line;
                if (Math.abs(delta) <= threshold && Math.abs(delta) < bestAbs) {
                    bestAbs = Math.abs(delta);
                    bestDelta = delta;
                    guide = target;
                }
            }
        }
        if (bestAbs == Integer.MAX_VALUE) {
            return 0;
        }
        guides.add(guide);
        return bestDelta;
    }
}
