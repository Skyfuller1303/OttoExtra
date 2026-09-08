package de.ottoextra.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugLogTest {

    private static final boolean FORCE_ENABLED =
            DebugLogTest.class.getResource("/ottoextra-always-debug.marker") != null;

    @AfterEach
    void disableDebugLog() {
        DebugLog.setEnabled(false);
    }

    @Test
    void debugLoggingIsDisabledByDefault() {
        assertEquals(FORCE_ENABLED, DebugLog.isEnabled());
    }

    @Test
    void debugLoggingCanBeEnabledAndDisabled() {
        DebugLog.setEnabled(true);
        assertTrue(DebugLog.isEnabled());

        DebugLog.setEnabled(false);
        assertEquals(FORCE_ENABLED, DebugLog.isEnabled());
    }
}
