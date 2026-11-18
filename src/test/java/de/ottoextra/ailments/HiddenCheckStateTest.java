package de.ottoextra.ailments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenCheckStateTest {

    private static final String NINE_SIX = "minecraft:generic_9x6";
    private static final String TITLE = "ꈃ쇳";

    @Test
    void interceptsOnlyTheExpectedMenuWhileArmed() {
        HiddenCheckState s = new HiddenCheckState();
        assertFalse(s.shouldIntercept(0, NINE_SIX, TITLE), "idle: user opened the menu himself");
        s.arm(1_000);
        assertTrue(s.shouldIntercept(1_500, NINE_SIX, TITLE));
        assertFalse(s.shouldIntercept(1_500, "minecraft:generic_9x3", TITLE), "other handler type");
        assertFalse(s.shouldIntercept(1_500, NINE_SIX, "Truhe"), "other title");
        assertFalse(s.shouldIntercept(1_500, NINE_SIX, null));
        assertFalse(s.shouldIntercept(1_000 + HiddenCheckState.TIMEOUT_MS + 1, NINE_SIX, TITLE), "window over");
    }

    @Test
    void secondPacketIsNotInterceptedAfterTakeOver() {
        HiddenCheckState s = new HiddenCheckState();
        s.arm(0);
        s.intercepted(7);
        assertEquals(HiddenCheckState.Phase.OPEN, s.phase());
        assertEquals(7, s.syncId());
        assertFalse(s.shouldIntercept(10, NINE_SIX, TITLE));
    }

    @Test
    void readsWhenReadyAndCleansUp() {
        HiddenCheckState s = new HiddenCheckState();
        assertEquals(HiddenCheckState.Step.NONE, s.step(0, false, false, true), "idle does nothing");
        s.arm(0);
        assertEquals(HiddenCheckState.Step.NONE, s.step(100, false, false, false));
        s.intercepted(3);
        assertEquals(HiddenCheckState.Step.NONE, s.step(200, false, false, false), "contents not there yet");
        assertEquals(HiddenCheckState.Step.READ, s.step(300, false, false, true));
        s.finish(300);
        assertFalse(s.active());
        assertEquals(-1, s.syncId());
    }

    @Test
    void timeoutWhenMenuNeverOpensOrNeverFills() {
        HiddenCheckState s = new HiddenCheckState();
        s.arm(0);
        assertEquals(HiddenCheckState.Step.NONE, s.step(HiddenCheckState.TIMEOUT_MS, false, false, false));
        assertEquals(HiddenCheckState.Step.TIMEOUT, s.step(HiddenCheckState.TIMEOUT_MS + 1, false, false, false));
        s.finish(4_000);
        s.arm(10_000);
        s.intercepted(2);
        assertEquals(HiddenCheckState.Step.TIMEOUT, s.step(10_000 + HiddenCheckState.TIMEOUT_MS + 1, false, false, false));
    }

    @Test
    void abortsWhenAScreenOpensOrTheHandlerIsReplaced() {
        HiddenCheckState s = new HiddenCheckState();
        s.arm(0);
        assertEquals(HiddenCheckState.Step.ABORT, s.step(10, true, false, false));
        s.finish(10);
        s.arm(100);
        s.intercepted(5);
        assertEquals(HiddenCheckState.Step.ABORT, s.step(110, false, true, true), "replaced beats ready");
        assertEquals(HiddenCheckState.Step.ABORT, s.step(110, true, false, true), "screen beats ready");
    }

    @Test
    void chatLineIsHiddenOnlyDuringTheCheckAndShortlyAfter() {
        HiddenCheckState s = new HiddenCheckState();
        String line = "§a[Info] §7Du öffnest dein Menü.";
        assertFalse(s.shouldSuppressChat(0, line), "no check: user opened the menu");
        s.arm(1_000);
        assertTrue(s.shouldSuppressChat(1_100, line));
        assertFalse(s.shouldSuppressChat(1_100, "§a[Info] §7Du öffnest dein Menü. Mehr"), "exact match only");
        assertFalse(s.shouldSuppressChat(1_100, "Du öffnest dein Menü."), "exact match incl. prefix");
        assertFalse(s.shouldSuppressChat(1_100, null));
        s.intercepted(1);
        s.finish(2_000);
        assertTrue(s.shouldSuppressChat(2_000 + HiddenCheckState.CHAT_GRACE_MS, line));
        assertFalse(s.shouldSuppressChat(2_000 + HiddenCheckState.CHAT_GRACE_MS + 1, line));
    }

    @Test
    void guardBlocksWhileInventoryOrUseKeyIsPressed() {
        var keys = new AutoCheckGuard.Inputs(true, true, false, 60_000, 60_000, false, false, false, false, false,
                false, true, true);
        assertEquals("inventory/use key pressed", AutoCheckGuard.blockReason(keys));
    }
}
