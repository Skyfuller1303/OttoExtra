package de.ottoextra.map;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapOpenSyncGateTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void firstAttemptIsAcceptedImmediately() {
        AtomicLong clock = new AtomicLong(25 * SECOND);
        MapOpenSyncGate gate = new MapOpenSyncGate(
                Duration.ofMinutes(1), clock::get);

        assertTrue(gate.tryAcquire());
    }

    @Test
    void attemptsInsideCooldownAreRejectedAndExactBoundaryIsAccepted() {
        AtomicLong clock = new AtomicLong();
        MapOpenSyncGate gate = new MapOpenSyncGate(
                Duration.ofMinutes(1), clock::get);

        assertTrue(gate.tryAcquire());
        clock.set(59 * SECOND);
        assertFalse(gate.tryAcquire());
        clock.set(60 * SECOND);
        assertTrue(gate.tryAcquire());
    }

    @Test
    void rejectedAttemptDoesNotExtendCooldown() {
        AtomicLong clock = new AtomicLong(100 * SECOND);
        MapOpenSyncGate gate = new MapOpenSyncGate(
                Duration.ofMinutes(1), clock::get);

        assertTrue(gate.tryAcquire());
        clock.set(159 * SECOND);
        assertFalse(gate.tryAcquire());
        clock.set(160 * SECOND);
        assertTrue(gate.tryAcquire());
    }

    @Test
    void negativeNanoTimeOriginIsHandled() {
        AtomicLong clock = new AtomicLong(-100 * SECOND);
        MapOpenSyncGate gate = new MapOpenSyncGate(
                Duration.ofMinutes(1), clock::get);

        assertTrue(gate.tryAcquire());
        clock.set(-41 * SECOND);
        assertFalse(gate.tryAcquire());
        clock.set(-40 * SECOND);
        assertTrue(gate.tryAcquire());
    }
}
