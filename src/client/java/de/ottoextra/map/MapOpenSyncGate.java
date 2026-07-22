package de.ottoextra.map;

import java.time.Duration;
import java.util.function.LongSupplier;

final class MapOpenSyncGate {

    private final long cooldownNanos;
    private final LongSupplier nanoTime;
    private boolean acquired;
    private long lastAcquireNanos;

    MapOpenSyncGate() {
        this(Duration.ofMinutes(1), System::nanoTime);
    }

    MapOpenSyncGate(Duration cooldown, LongSupplier nanoTime) {
        if (cooldown == null || cooldown.isNegative() || cooldown.isZero()) {
            throw new IllegalArgumentException("Cooldown muss positiv sein");
        }
        if (nanoTime == null) {
            throw new IllegalArgumentException("Zeitquelle fehlt");
        }
        this.cooldownNanos = cooldown.toNanos();
        this.nanoTime = nanoTime;
    }

    synchronized boolean tryAcquire() {
        long now = nanoTime.getAsLong();
        if (!acquired || now - lastAcquireNanos >= cooldownNanos) {
            acquired = true;
            lastAcquireNanos = now;
            return true;
        }
        return false;
    }
}
