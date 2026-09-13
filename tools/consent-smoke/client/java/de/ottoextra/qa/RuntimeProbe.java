package de.ottoextra.qa;

/** Test-only probes. Never included in the release jar. */
public final class RuntimeProbe {
    public static volatile java.net.URI skinEndpoint;
    public static final java.util.concurrent.atomic.AtomicInteger overlayCalls = new java.util.concurrent.atomic.AtomicInteger();
    public static final java.util.concurrent.atomic.AtomicInteger heartbeats = new java.util.concurrent.atomic.AtomicInteger();
    public static volatile boolean simulateOttonien;
    public static final java.util.concurrent.atomic.AtomicInteger deniedHttpAttempts = new java.util.concurrent.atomic.AtomicInteger();
    private RuntimeProbe() { }
}
