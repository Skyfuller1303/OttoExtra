package de.ottoextra.letter.send;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

public final class CommandSendQueue {

    public static final long COMMAND_DELAY_MS = 1500L;

    private long[] delays;

    public CommandSendQueue withDelays(long[] perCommandDelays) {
        this.delays = perCommandDelays;
        return this;
    }

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ottoextra-letter-send");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean cancelled = false;
    private boolean started = false;
    private volatile int sent = 0;
    private final List<String> commands;
    private final IntConsumer onProgress;
    private final Runnable onComplete;
    private final BiConsumer<Runnable, Long> scheduler;
    private final Consumer<Runnable> clientExecutor;
    private final Supplier<Connection> connectionSupplier;
    private Connection connection;
    private Runnable onInterrupted;
    private BooleanSupplier continuationGuard = () -> true;

    interface Connection {
        boolean isCurrent();

        void send(String command);
    }

    public CommandSendQueue(List<String> commands, IntConsumer onProgress, Runnable onComplete) {
        this(commands, onProgress, onComplete,
                (task, delay) -> SCHEDULER.schedule(task, delay, TimeUnit.MILLISECONDS),
                task -> MinecraftClient.getInstance().execute(task),
                CommandSendQueue::currentConnection);
    }

    CommandSendQueue(List<String> commands, IntConsumer onProgress, Runnable onComplete,
                     BiConsumer<Runnable, Long> scheduler, Consumer<Runnable> clientExecutor,
                     Supplier<Connection> connectionSupplier) {
        this.commands = List.copyOf(commands);
        this.onProgress = onProgress;
        this.onComplete = onComplete;
        this.scheduler = scheduler;
        this.clientExecutor = clientExecutor;
        this.connectionSupplier = connectionSupplier;
    }

    public CommandSendQueue onInterrupted(Runnable callback) {
        onInterrupted = callback;
        return this;
    }

    /** Rechecks the held letter before each command and before reporting completion. */
    public CommandSendQueue whileValid(BooleanSupplier guard) {
        continuationGuard = guard == null ? () -> true : guard;
        return this;
    }

    private static Connection currentConnection() {
        MinecraftClient client = MinecraftClient.getInstance();
        var handler = client.getNetworkHandler();
        if (client.player == null || handler == null) {
            return null;
        }
        return new Connection() {
            @Override
            public boolean isCurrent() {
                return client.player != null && client.getNetworkHandler() == handler;
            }

            @Override
            public void send(String command) {
                handler.sendChatCommand(command);
            }
        };
    }

    public void start(int startIndex) {
        if (started || cancelled) {
            return;
        }
        started = true;
        connection = connectionSupplier.get();
        sent = Math.max(0, Math.min(startIndex, commands.size()));
        scheduleNext(0L);
    }

    private void scheduleNext(long delayMs) {
        scheduler.accept(() -> {
            if (cancelled) {
                return;
            }
            clientExecutor.accept(() -> {
                if (cancelled) {
                    return;
                }
                if (connection == null || !connection.isCurrent() || !continuationGuard.getAsBoolean()) {
                    cancelled = true;
                    OttoExtra.LOGGER.warn("[letter] Versand unterbrochen bei {}/{}",
                            sent, commands.size());
                    if (onInterrupted != null) {
                        onInterrupted.run();
                    }
                    return;
                }
                if (sent >= commands.size()) {
                    cancelled = true;
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    return;
                }
                try {
                    connection.send(commands.get(sent));
                } catch (RuntimeException failure) {
                    cancelled = true;
                    OttoExtra.LOGGER.warn("[letter] Befehl konnte nicht gesendet werden", failure);
                    if (onInterrupted != null) {
                        onInterrupted.run();
                    }
                    return;
                }
                sent++;
                if (onProgress != null) {
                    onProgress.accept(sent);
                }
                long next = delays != null && sent < delays.length
                        ? delays[sent] : COMMAND_DELAY_MS;
                if (!cancelled) {
                    scheduleNext(next);
                }
            });
        }, delayMs);
    }

    public void cancel() {
        cancelled = true;
    }

    public int sent() {
        return sent;
    }

    public int total() {
        return commands.size();
    }
}
