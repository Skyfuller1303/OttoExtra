package de.ottoextra.privacy;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A permission for one operation; a later grant never revives a revoked operation. */
public final class ConsentPermit {
    public static final class Gate {
        private long generation;
        private final Set<Runnable> cancellations = new HashSet<>();

        public synchronized ConsentPermit capture(BooleanSupplier allowed) {
            boolean initiallyAllowed = allowed.getAsBoolean();
            return new ConsentPermit(this, generation, () -> initiallyAllowed && allowed.getAsBoolean());
        }

        public void invalidate() {
            change(() -> { }).run();
        }

        public Runnable change(Runnable mutation) {
            Set<Runnable> old;
            synchronized (this) {
                generation++;
                old = new HashSet<>(cancellations);
                cancellations.clear();
                mutation.run();
            }
            return () -> old.forEach(cancel -> {
                try { cancel.run(); } catch (RuntimeException ignored) { }
            });
        }
    }

    private final Gate gate;
    private final long generation;
    private final BooleanSupplier allowed;

    private ConsentPermit(Gate gate, long generation, BooleanSupplier allowed) {
        this.gate = gate;
        this.generation = generation;
        this.allowed = allowed;
    }

    public boolean valid() {
        synchronized (gate) {
            return gate.generation == generation && allowed.getAsBoolean();
        }
    }

    public void require() {
        if (!valid()) throw new CancellationException("OttoExtra permission is missing or revoked");
    }

    public ConsentPermit restrict(BooleanSupplier condition) {
        boolean initiallyAllowed = valid() && condition.getAsBoolean();
        return new ConsentPermit(gate, generation, () -> initiallyAllowed && allowed.getAsBoolean()
                && condition.getAsBoolean());
    }

    /** Register cleanup for data retained after a request completes. Returns an unregister action. */
    public Runnable onRevoke(Runnable cleanup) {
        synchronized (gate) {
            require();
            gate.cancellations.add(cleanup);
        }
        return () -> { synchronized (gate) { gate.cancellations.remove(cleanup); } };
    }

    /** Serialize a short local commit / request start with revocation. Never block on network here. */
    public void commit(Runnable action) {
        synchronized (gate) {
            require();
            action.run();
        }
    }

    @FunctionalInterface public interface IoAction { void run() throws IOException; }

    public void commitIo(IoAction action) throws IOException {
        synchronized (gate) {
            require();
            action.run();
        }
    }

    public <T> CompletableFuture<T> start(Supplier<CompletableFuture<T>> action) {
        synchronized (gate) {
            if (!valid()) return CompletableFuture.failedFuture(
                    new CancellationException("OttoExtra permission is missing or revoked"));
            CompletableFuture<T> future = action.get();
            Runnable cancel = () -> future.cancel(true);
            gate.cancellations.add(cancel);
            future.whenComplete((value, error) -> {
                synchronized (gate) { gate.cancellations.remove(cancel); }
            });
            return future;
        }
    }

    /** Closing or revoking releases the registration; reads stop as soon as permission is lost. */
    public InputStream guard(InputStream source) {
        class GuardedStream extends FilterInputStream {
            private final Runnable cancel = () -> { try { close(); } catch (IOException ignored) { } };
            GuardedStream() { super(source); }
            @Override public int read() throws IOException { require(); return super.read(); }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                require(); return super.read(bytes, offset, length);
            }
            @Override public void close() throws IOException {
                synchronized (gate) { gate.cancellations.remove(cancel); }
                super.close();
            }
        }
        GuardedStream stream = new GuardedStream();
        synchronized (gate) {
            if (!valid()) {
                stream.cancel.run();
                require();
            }
            gate.cancellations.add(stream.cancel);
        }
        return stream;
    }
}
