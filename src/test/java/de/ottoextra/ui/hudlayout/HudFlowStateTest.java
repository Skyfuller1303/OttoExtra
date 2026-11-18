package de.ottoextra.ui.hudlayout;

import de.ottoextra.ui.hudlayout.HudSnap.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HudFlowStateTest {
    private static final int W = 400;
    private static final int H = 240;
    private static final int G = HudFlowState.GAP;

    private HudFlowState state = new HudFlowState();

    private Rect place(String id, Rect desired, boolean up, int prio, long now) {
        return state.place(id, desired, up, prio, now, List.of(), W, H);
    }

    @Test void nonOverlappingHudsStayWhereTheyWant() {
        place("a", new Rect(10, 10, 100, 50), false, 100, 0);
        assertEquals(new Rect(10, 100, 100, 50), place("b", new Rect(10, 100, 100, 50), false, 200, 10));
    }

    @Test void overlappingHudIsPushedDownBelowTheHigherRanked() {
        place("a", new Rect(10, 10, 100, 50), false, 100, 0);
        Rect b = place("b", new Rect(20, 30, 100, 40), false, 200, 10);
        assertEquals(10 + 50 + G, b.y());
        assertEquals(20, b.x());
    }

    @Test void bottomAnchoredHudIsPushedUp() {
        place("a", new Rect(10, 100, 100, 80), false, 100, 0);
        Rect b = place("b", new Rect(10, 150, 100, 40), true, 200, 10);
        // a's top edge (100) lies above b's bottom edge (190), so a stays first and b goes below it.
        assertEquals(100 + 80 + G, b.y());
        // A bottom-anchored panel at the screen bottom stays; the column is pulled up above it.
        place("low", new Rect(200, 190, 100, 50), true, 200, 20);
        Rect high = place("high", new Rect(200, 150, 100, 60), false, 100, 20);
        assertEquals(new Rect(200, 190 - G - 60, 100, 60), high);
        assertEquals(190, state.resolved("low", 20).y());
    }

    @Test void higherRankedHudNeverMoves() {
        place("b", new Rect(20, 30, 100, 40), false, 200, 0);
        assertEquals(new Rect(10, 10, 100, 50), place("a", new Rect(10, 10, 100, 50), false, 100, 10));
    }

    @Test void chainOfThreeStacksInPriorityOrder() {
        // One-frame lag: each frame uses the others' last positions, so the chain settles within a few frames.
        Rect desiredA = new Rect(10, 10, 100, 40);
        Rect desiredB = new Rect(10, 20, 100, 30);
        Rect desiredC = new Rect(10, 30, 100, 20);
        Rect c = null;
        Rect b = null;
        for (int frame = 0; frame < 5; frame++) {
            long now = frame * 16L;
            place("a", desiredA, false, 100, now);
            b = place("b", desiredB, false, 200, now);
            c = place("c", desiredC, false, 300, now);
        }
        assertEquals(10 + 40 + G, b.y());
        assertEquals(b.y() + 30 + G, c.y());
    }

    @Test void hudsWithNonOverlappingXRangesDoNotPush() {
        place("a", new Rect(10, 10, 100, 50), false, 100, 0);
        assertEquals(new Rect(120, 20, 100, 50), place("b", new Rect(120, 20, 100, 50), false, 200, 10));
    }

    @Test void clampsAtTheScreenEdgeAndAllowsOverlapAsLastResort() {
        place("a", new Rect(10, 150, 100, 85), false, 100, 0);
        Rect b = place("b", new Rect(10, 160, 100, 40), false, 200, 10);
        // Stacking b below a would leave the screen, so the column ends at the screen bottom and a moves up.
        assertEquals(H - 40, b.y());
        assertEquals(H - 40 - G - 85, state.resolved("a", 10).y());
        // Desired position off screen is clamped too.
        assertEquals(0, place("c", new Rect(300, -20, 50, 20), false, 300, 20).y());
    }

    @Test void obstaclesAlwaysWinRegardlessOfPriority() {
        Rect minimap = new Rect(0, 0, 120, 100);
        Rect r = state.place("a", new Rect(10, 20, 100, 50), false, 1, 0, List.of(minimap), W, H);
        assertEquals(100 + G, r.y());
        Rect up = state.place("b", new Rect(10, 80, 100, 50), true, 1, 0, List.of(new Rect(0, 100, 120, 100)), W, H);
        assertEquals(100 - G - 50, up.y());
    }

    @Test void staleEntriesExpireSoTheOtherHudSnapsBack() {
        place("a", new Rect(10, 10, 100, 50), false, 100, 0);
        assertEquals(10 + 50 + G, place("b", new Rect(10, 20, 100, 40), false, 200, 100).y());
        // a stops reporting (hidden HUD); after the expiry window b returns to its wanted spot.
        assertEquals(20, place("b", new Rect(10, 20, 100, 40), false, 200, 100 + HudFlowState.EXPIRY_MS + 100).y());
        assertNull(state.resolved("a", 100 + HudFlowState.EXPIRY_MS + 100));
    }

    @Test void resultDoesNotDependOnReportOrder() {
        HudFlowState one = new HudFlowState();
        HudFlowState two = new HudFlowState();
        Rect a = new Rect(10, 10, 100, 40);
        Rect b = new Rect(10, 20, 100, 30);
        for (int frame = 0; frame < 4; frame++) {
            long now = frame * 16L;
            one.place("a", a, false, 100, now, List.of(), W, H);
            one.place("b", b, false, 100, now, List.of(), W, H);
            two.place("b", b, false, 100, now, List.of(), W, H);
            two.place("a", a, false, 100, now, List.of(), W, H);
        }
        // Equal priority: the smaller id ("a") ranks first in both runs.
        assertEquals(one.resolved("a", 48), two.resolved("a", 48));
        assertEquals(one.resolved("b", 48), two.resolved("b", 48));
        assertEquals(10, one.resolved("a", 48).y());
        assertEquals(10 + 40 + G, one.resolved("b", 48).y());
    }

    @Test void pushThatWouldLeaveTheScreenKeepsTheWantedPositionInsteadOfJumpingToTheEdge() {
        // A tall blocker near the top; b is bottom anchored and sits just inside it.
        place("a", new Rect(10, 0, 100, 150), false, 100, 0);
        Rect b = place("b", new Rect(10, 100, 100, 60), true, 200, 10);
        // b orders after a (bottom edge 160 > top edge 0) and goes below it, never to y = 0.
        assertEquals(150 + G, b.y());
        // Column taller than the screen: overlap as last resort, the first panel stays fully visible at
        // the top and the second ends at the screen bottom.
        HudFlowState tight = new HudFlowState();
        tight.place("a", new Rect(10, 0, 100, 235), false, 100, 0, List.of(), W, H);
        Rect c = tight.place("c", new Rect(10, 120, 100, 60), true, 200, 10, List.of(), W, H);
        assertEquals(H - 60, c.y());
        assertEquals(0, tight.resolved("a", 10).y());
    }

    @Test void zeroSizedReportsNeverCollapseToTheEdge() {
        // A degenerate rect must not produce a position at 0 or NaN-like jumps.
        Rect r = place("z", new Rect(10, 120, 0, 0), true, 100, 0);
        assertEquals(120, r.y());
        place("a", new Rect(10, 100, 100, 60), false, 100, 10);
        assertEquals(120, place("z", new Rect(10, 120, 0, 0), false, 200, 20).y());
    }

    @Test void elementGrowingFrameByFrameNextToAnotherMovesSmoothlyWithoutJumps() {
        int previous = -1;
        for (int frame = 0; frame < 30; frame++) {
            long now = frame * 16L;
            int rows = 1 + frame / 3;
            place("a", new Rect(10, 40, 100, 15 + rows * 16), false, 100, now);
            Rect b = place("b", new Rect(10, 200, 100, 40), true, 200, now);
            // b is bottom anchored: it only ever moves up when a grows into it, never to the screen edge.
            assertTrue(b.y() > 0, "jumped to top at frame " + frame);
            if (previous >= 0) {
                assertTrue(b.y() <= previous, "moved down while the other grew");
                assertTrue(previous - b.y() <= 16 + G, "jump of " + (previous - b.y()) + " px");
            }
            previous = b.y();
        }
    }

    @Test void droppedFlushBelowAnotherHudStaysBelowEvenWhenBottomAnchored() {
        place("a", new Rect(10, 100, 100, 50), false, 100, 0);
        // Snapped flush (gap 0) under a: the short way down wins over jumping above a.
        assertEquals(150 + G, place("b", new Rect(10, 150, 100, 40), true, 200, 10).y());
    }

    @Test void pinnedHudKeepsExactlyTheWantedPosition() {
        place("a", new Rect(10, 100, 100, 50), false, 100, 0);
        Rect pinned = state.place("b", new Rect(10, 110, 100, 40), true, 200, 10, List.of(), W, H, true);
        assertEquals(110, pinned.y());
        // Others make room for the pinned rect: a leaves it, c stacks below a.
        assertEquals(110 + 40 + G, state.resolved("a", 10).y());
        assertEquals(110 + 40 + G + 50 + G, place("c", new Rect(10, 120, 100, 30), false, 300, 20).y());
        assertEquals(110, state.resolved("b", 20).y());
    }

    @Test
    void lowerPanelMovesWhenUpperGrowsRegardlessOfPriority() {
        // Addon HUD (priority 200) above the activity HUD (priority 100), both top anchored.
        place("squad", new Rect(10, 10, 100, 40), false, 200, 0);
        assertEquals(new Rect(10, 60, 100, 50), place("activity", new Rect(10, 60, 100, 50), false, 100, 0));
        // Squad panel grows by 30 px: the activity HUD below has to move down, the squad HUD stays.
        assertEquals(new Rect(10, 10, 100, 70), place("squad", new Rect(10, 10, 100, 70), false, 200, 10));
        assertEquals(new Rect(10, 82, 100, 50), place("activity", new Rect(10, 60, 100, 50), false, 100, 10));
        // Shrinks again: activity returns to its wanted spot.
        place("squad", new Rect(10, 10, 100, 40), false, 200, 20);
        assertEquals(new Rect(10, 60, 100, 50), place("activity", new Rect(10, 60, 100, 50), false, 100, 20));
    }

    @Test
    void bottomAnchoredStackMovesUpperPanelUp() {
        place("low", new Rect(10, 200, 100, 40), true, 200, 0);
        assertEquals(new Rect(10, 140, 100, 50), place("high", new Rect(10, 140, 100, 50), true, 100, 0));
        place("low", new Rect(10, 170, 100, 70), true, 200, 10);
        assertEquals(new Rect(10, 118, 100, 50), place("high", new Rect(10, 140, 100, 50), true, 100, 10));
    }

    // Dev config of the bug report: activity HUD MIDDLE_RIGHT (offset -1/33, scale 0.6, 8 rows plus
    // footer) and the TF squad HUD BOTTOM_RIGHT (offset -1/-55, pushUp, PRIORITY_ADDON) overlapped by
    // about the squad header height.
    private static final Rect ACTIVITY = new Rect(W - 1 - 96, H / 2 + 33 - 35, 96, 70);
    private static final Rect SQUAD = new Rect(W - 1 - 86, H - 55 - 60, 86, 60);

    @Test void screenshotScenarioActivityAndSquadHudNoLongerOverlap() {
        for (int frame = 0; frame < 3; frame++) {
            long now = frame * 16L;
            place("ottoextra:activity", ACTIVITY, false, HudFlow.PRIORITY_ACTIVITY_HUD, now);
            place("tacticalfactions:squad_hud", SQUAD, true, HudFlow.PRIORITY_ADDON, now);
        }
        Rect activity = state.resolved("ottoextra:activity", 32);
        Rect squad = state.resolved("tacticalfactions:squad_hud", 32);
        assertTrue(activity.bottom() + G <= squad.y(), activity + " overlaps " + squad);
        assertTrue(squad.bottom() <= H);
        assertTrue(activity.y() >= 0);
    }

    @Test void threePanelsInOneColumnStackWithoutOverlap() {
        Rect a = new Rect(10, 20, 100, 40);
        Rect b = new Rect(30, 40, 100, 30);
        Rect c = new Rect(20, 150, 100, 50);
        place("a", a, false, 300, 0);
        place("c", c, true, 100, 0);
        place("b", b, false, 200, 0);
        assertEquals(20, state.resolved("a", 0).y());
        assertEquals(20 + 40 + G, state.resolved("b", 0).y());
        assertEquals(150, state.resolved("c", 0).y());
    }

    @Test void overflowPropagatesUpwardFromTheScreenBottom() {
        place("a", new Rect(10, 60, 100, 60), false, 100, 0);
        place("b", new Rect(10, 80, 100, 100), false, 100, 0);
        place("c", new Rect(10, 200, 100, 40), true, 100, 0);
        // Packed downward: 60, 122, 224; c would end at 264 > 240, so the column is pulled up from the bottom.
        Rect c = state.resolved("c", 0);
        Rect b = state.resolved("b", 0);
        Rect a = state.resolved("a", 0);
        assertEquals(H - 40, c.y());
        assertEquals(c.y() - G - 100, b.y());
        assertEquals(Math.min(60, b.y() - G - 60), a.y());
        assertTrue(a.bottom() + G <= b.y() && b.bottom() + G <= c.y());
    }

    @Test void pinnedPanelStaysAndTheColumnFlowsAroundIt() {
        state.place("drag", new Rect(10, 100, 100, 40), false, 999, 0, List.of(), W, H, true);
        place("upper", new Rect(10, 80, 100, 30), false, 100, 0);
        place("lower", new Rect(10, 130, 100, 30), false, 100, 0);
        assertEquals(100, state.resolved("drag", 0).y());
        // upper wants 80..110 and overlaps the pinned rect: it jumps below it, lower follows.
        assertEquals(140 + G, state.resolved("upper", 0).y());
        assertEquals(140 + G + 30 + G, state.resolved("lower", 0).y());
    }

    @Test void fixedObstacleIsSkippedOnBothPasses() {
        List<Rect> minimap = List.of(new Rect(0, 100, 120, 60));
        // Downward: a panel wanting 90 jumps below the obstacle.
        assertEquals(160 + G, state.place("a", new Rect(10, 90, 100, 30), false, 100, 0, minimap, W, H).y());
        // Upward: a column ending past the screen bottom is pulled up over the obstacle, not into it.
        HudFlowState s = new HudFlowState();
        s.place("x", new Rect(10, 170, 100, 50), false, 100, 0, minimap, W, H);
        s.place("y", new Rect(10, 200, 100, 40), true, 100, 0, minimap, W, H);
        Rect x = s.resolved("x", 0);
        Rect y = s.resolved("y", 0);
        assertEquals(H - 40, y.y());
        assertEquals(100 - G - 50, x.y());
        assertTrue(x.bottom() + G <= 100);
    }

    @Test void layoutDoesNotDependOnReportOrder() {
        String[] ids = {"ottoextra:activity", "tacticalfactions:squad_hud", "ottoextra_resources:timer_hud"};
        Rect[] wants = {ACTIVITY, SQUAD, new Rect(W - 1 - 90, 10, 90, 100)};
        boolean[] up = {false, true, false};
        int[][] orders = {{0, 1, 2}, {2, 1, 0}, {1, 0, 2}, {1, 2, 0}};
        Rect[] reference = null;
        for (int[] order : orders) {
            HudFlowState s = new HudFlowState();
            for (int frame = 0; frame < 3; frame++) {
                for (int k : order) {
                    s.place(ids[k], wants[k], up[k], 100 + k, frame * 16L, List.of(), W, H);
                }
            }
            Rect[] got = new Rect[ids.length];
            for (int k = 0; k < ids.length; k++) {
                got[k] = s.resolved(ids[k], 32);
            }
            if (reference == null) {
                reference = got;
                // A single report already yields the final layout of the whole column.
                for (int k = 0; k < got.length; k++) {
                    for (int l = k + 1; l < got.length; l++) {
                        assertFalse(got[k].y() < got[l].bottom() && got[l].y() < got[k].bottom(),
                                got[k] + " overlaps " + got[l]);
                    }
                }
            } else {
                assertArrayEquals(reference, got);
            }
        }
    }

    @Test void repeatedFramesDoNotOscillate() {
        Rect previousActivity = null;
        Rect previousSquad = null;
        for (int frame = 0; frame < 20; frame++) {
            long now = frame * 16L;
            Rect squad = place("tacticalfactions:squad_hud", SQUAD, true, HudFlow.PRIORITY_ADDON, now);
            Rect activity = place("ottoextra:activity", ACTIVITY, false, HudFlow.PRIORITY_ACTIVITY_HUD, now);
            if (frame > 0) {
                assertEquals(previousActivity, activity, "activity moved in frame " + frame);
                assertEquals(previousSquad, state.resolved("tacticalfactions:squad_hud", now));
            }
            previousActivity = activity;
            previousSquad = state.resolved("tacticalfactions:squad_hud", now);
            assertNotNull(squad);
        }
    }
}
