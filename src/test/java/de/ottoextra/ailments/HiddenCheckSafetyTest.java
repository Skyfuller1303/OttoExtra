package de.ottoextra.ailments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenCheckSafetyTest {

    private static final String NINE_SIX = "minecraft:generic_9x6";
    private static final String TITLE = "ꈃ쇳";

    @Test
    void closePacketOnlyWhenNothingElseMayHaveOpened() {
        assertTrue(HiddenCheckState.closeAllowed(false, false, false));
        assertFalse(HiddenCheckState.closeAllowed(true, false, false), "arming tick sent a use packet");
        assertFalse(HiddenCheckState.closeAllowed(false, true, false), "user presses use/inventory/attack");
        assertFalse(HiddenCheckState.closeAllowed(false, false, true), "a screen is open");
    }

    @Test
    void lateMenuPacketAfterAbortIsTakenOverOnceAndOnlyWithoutInput() {
        HiddenCheckState s = new HiddenCheckState();
        s.arm(1_000);
        assertEquals(HiddenCheckState.Step.ABORT, s.step(1_100, true, false, false));
        s.finish(1_100, true);
        assertFalse(s.active());
        assertTrue(s.shouldInterceptLate(1_500, NINE_SIX, TITLE, false, false));
        assertFalse(s.shouldInterceptLate(1_500, NINE_SIX, TITLE, true, false), "user input since: pass through");
        assertFalse(s.shouldInterceptLate(1_500, NINE_SIX, TITLE, false, true), "screen open: pass through");
        assertFalse(s.shouldInterceptLate(1_500, "minecraft:generic_9x3", TITLE, false, false));
        assertFalse(s.shouldInterceptLate(1_500, NINE_SIX, "Truhe", false, false));
        assertFalse(s.shouldInterceptLate(1_100 + HiddenCheckState.TIMEOUT_MS + 1, NINE_SIX, TITLE, false, false),
                "window over");
        s.lateConsumed();
        assertFalse(s.shouldInterceptLate(1_500, NINE_SIX, TITLE, false, false), "only once");
    }

    @Test
    void timeoutWhileArmedKeepsTheLateWindowButAReadDoesNot() {
        HiddenCheckState s = new HiddenCheckState();
        s.arm(0);
        s.finish(4_000, true);
        assertTrue(s.shouldInterceptLate(4_500, NINE_SIX, TITLE, false, false));
        HiddenCheckState done = new HiddenCheckState();
        done.arm(0);
        done.intercepted(2);
        done.finish(500, true);
        assertFalse(done.shouldInterceptLate(600, NINE_SIX, TITLE, false, false), "menu already seen");
        HiddenCheckState plain = new HiddenCheckState();
        plain.arm(0);
        plain.finish(500);
        assertFalse(plain.shouldInterceptLate(600, NINE_SIX, TITLE, false, false));
    }

    @Test
    void chatLineStaysHiddenDuringTheLateWindow() {
        HiddenCheckState s = new HiddenCheckState();
        s.arm(0);
        s.finish(1_000, true);
        String line = "§a[Info] §7Du öffnest dein Menü.";
        assertTrue(s.shouldSuppressChat(1_000 + HiddenCheckState.TIMEOUT_MS, line));
        assertFalse(s.shouldSuppressChat(1_000 + HiddenCheckState.TIMEOUT_MS + 1, line));
    }

    @Test
    void manualCheckStateDoesNotSurviveReset() {
        ManualCheck m = new ManualCheck();
        assertTrue(m.request());
        assertFalse(m.request(), "second request while pending");
        m.started();
        assertTrue(m.running());
        assertFalse(m.pending());
        assertFalse(m.request(), "while running");
        m.reset();
        assertFalse(m.any());
        assertTrue(m.request(), "fresh after disconnect");
        m.done();
        assertFalse(m.any());
    }

    @Test
    void guardBlocksWhileUsingAnItemOrOnCooldown() {
        var using = new AutoCheckGuard.Inputs(true, true, false, 60_000, 60_000, false, false, false, false, false,
                false, true, false, true, false);
        assertEquals("using an item (fishing rod, bow, shield, ...)", AutoCheckGuard.blockReason(using));
        var cooldown = new AutoCheckGuard.Inputs(true, true, false, 60_000, 60_000, false, false, false, false,
                false, false, true, false, false, true);
        assertEquals("item cooldown", AutoCheckGuard.blockReason(cooldown));
    }
}
