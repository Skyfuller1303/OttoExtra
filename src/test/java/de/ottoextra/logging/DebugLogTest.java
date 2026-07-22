package de.ottoextra.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugLogTest {

    @AfterEach
    void disableDebugLog() {
        DebugLog.setEnabled(false);
    }

    @Test
    void debugLoggingIsDisabledByDefault() {
        assertFalse(DebugLog.isEnabled());
    }

    @Test
    void debugLoggingCanBeEnabledAndDisabled() {
        DebugLog.setEnabled(true);
        assertTrue(DebugLog.isEnabled());

        DebugLog.setEnabled(false);
        assertFalse(DebugLog.isEnabled());
    }
}
