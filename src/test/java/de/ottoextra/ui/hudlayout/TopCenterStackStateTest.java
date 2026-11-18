package de.ottoextra.ui.hudlayout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopCenterStackStateTest {

    @Test
    void columnStartsFourPixelsBelowTheLastDrawnBossBar() {
        assertEquals(4, TopCenterStackState.topBelowBossBars(0, 360));
        // One bar at y=12, 5 px high, 4 px gap.
        assertEquals(21, TopCenterStackState.topBelowBossBars(1, 360));
        assertEquals(40, TopCenterStackState.topBelowBossBars(2, 360));
        assertEquals(59, TopCenterStackState.topBelowBossBars(3, 360));
    }

    @Test
    void bossBarOffsetStopsWhereVanillaStopsDrawing() {
        // Screen height 120: vanilla draws bars at 12 and 31 and stops once the next y (50) reaches 120/3 = 40.
        assertEquals(40, TopCenterStackState.topBelowBossBars(2, 120));
        assertEquals(40, TopCenterStackState.topBelowBossBars(5, 120));
        // A tiny screen still shows the first bar.
        assertEquals(21, TopCenterStackState.topBelowBossBars(4, 30));
    }

    @Test
    void elementsStackByOrderWithTwoPixelGap() {
        TopCenterStackState s = new TopCenterStackState();
        long now = 1000;
        // Render order vote, zone, Werkstatt: the first frame sees only what has reported so far.
        s.claim("vote", TopCenterStack.ORDER_VOTE, 20, 4, now);
        s.claim("zone", TopCenterStack.ORDER_ZONE_TOAST, 30, 4, now);
        assertEquals(4, s.claim("werkstatt", TopCenterStack.ORDER_WERKSTATT, 20, 4, now));
        // Next frame: Werkstatt bar on top, the zone toast 2 px below, the vote toast 2 px below that.
        now += 16;
        assertEquals(4 + 20 + 2 + 30 + 2, s.claim("vote", TopCenterStack.ORDER_VOTE, 20, 4, now));
        assertEquals(4 + 20 + 2, s.claim("zone", TopCenterStack.ORDER_ZONE_TOAST, 30, 4, now));
        assertEquals(4, s.claim("werkstatt", TopCenterStack.ORDER_WERKSTATT, 20, 4, now));
    }

    @Test
    void voteFlowsDirectlyBelowTheWerkstattBarBelowBossBars() {
        TopCenterStackState s = new TopCenterStackState();
        int top = TopCenterStackState.topBelowBossBars(1, 360);
        assertEquals(21, s.claim("werkstatt", TopCenterStack.ORDER_WERKSTATT, 20, top, 0));
        assertEquals(21 + 20 + 2, s.claim("vote", TopCenterStack.ORDER_VOTE, 20, top, 0));
    }

    @Test
    void releasedOrSilentElementsLeaveTheColumn() {
        TopCenterStackState s = new TopCenterStackState();
        s.claim("werkstatt", TopCenterStack.ORDER_WERKSTATT, 20, 4, 0);
        s.release("werkstatt");
        assertEquals(4, s.claim("vote", TopCenterStack.ORDER_VOTE, 20, 4, 10));

        s.claim("werkstatt", TopCenterStack.ORDER_WERKSTATT, 20, 4, 100);
        assertEquals(26, s.claim("vote", TopCenterStack.ORDER_VOTE, 20, 4, 100 + TopCenterStackState.ENTRY_TTL_MS));
        assertEquals(4, s.claim("vote", TopCenterStack.ORDER_VOTE, 20, 4, 101 + TopCenterStackState.ENTRY_TTL_MS));
    }

    @Test
    void equalOrderIsBrokenByIdAndNegativeHeightCountsAsZero() {
        TopCenterStackState s = new TopCenterStackState();
        s.claim("b", 200, 10, 0, 0);
        assertEquals(0, s.claim("a", 200, 10, 0, 0));
        assertEquals(12, s.claim("b", 200, 10, 0, 0));
        s.claim("x", 100, -5, 0, 0);
        assertEquals(2, s.claim("a", 200, 10, 0, 0));
    }
}
