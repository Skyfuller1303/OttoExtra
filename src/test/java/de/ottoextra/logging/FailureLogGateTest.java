package de.ottoextra.logging;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureLogGateTest {

    @Test
    void reportsOnlyFirstFailureUntilRecovery() {
        FailureLogGate gate = new FailureLogGate(Duration.ZERO, () -> 0L);

        assertTrue(gate.onFailure());
        assertFalse(gate.onFailure());
        assertTrue(gate.onSuccess());
        assertFalse(gate.onSuccess());
        assertTrue(gate.onFailure());
    }

    @Test
    void suppressesFlappingDuringCooldown() {
        AtomicLong now = new AtomicLong();
        FailureLogGate gate = new FailureLogGate(Duration.ofSeconds(10), now::get);

        assertTrue(gate.onFailure());
        assertTrue(gate.onSuccess());

        now.set(Duration.ofSeconds(5).toNanos());
        assertFalse(gate.onFailure());
        assertFalse(gate.onFailure());
        assertFalse(gate.onSuccess());

        now.set(Duration.ofSeconds(10).toNanos());
        assertTrue(gate.onFailure());
    }

    @Test
    void firstFailureIsReportedWithNegativeNanoTimeOrigin() {
        FailureLogGate gate = new FailureLogGate(Duration.ofSeconds(10), () -> -1L);

        assertTrue(gate.onFailure());
    }

    @Test
    void successWithoutFailureDoesNothing() {
        FailureLogGate gate = new FailureLogGate();

        assertFalse(gate.onSuccess());
    }
}
