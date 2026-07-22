package de.ottoextra.logging;

import java.time.Duration;
import java.util.function.LongSupplier;

public final class FailureLogGate {

    private static final Duration DEFAULT_COOLDOWN = Duration.ofMinutes(1);

    private final long cooldownNanos;
    private final LongSupplier nanoTime;

    private volatile boolean failing;
    private boolean reported;
    private long nextReportNanos = Long.MIN_VALUE;

    public FailureLogGate() {
        this(DEFAULT_COOLDOWN);
    }

    public FailureLogGate(Duration cooldown) {
        this(cooldown, System::nanoTime);
    }

    FailureLogGate(Duration cooldown, LongSupplier nanoTime) {
        this.cooldownNanos = Math.max(0L, cooldown.toNanos());
        this.nanoTime = nanoTime;
    }

    public synchronized boolean onFailure() {
        failing = true;
        if (reported || nanoTime.getAsLong() < nextReportNanos) {
            return false;
        }
        reported = true;
        return true;
    }

    public boolean onSuccess() {
        if (!failing) {
            return false;
        }
        synchronized (this) {
            if (!failing) {
                return false;
            }
            failing = false;
            boolean recovered = reported;
            reported = false;
            if (recovered) {
                nextReportNanos = nanoTime.getAsLong() + cooldownNanos;
            }
            return recovered;
        }
    }
}
