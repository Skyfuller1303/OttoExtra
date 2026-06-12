package de.ottoextra.letter.send;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Gemeinsame Versand-Maschine: sendet Commands mit Delay
 * nacheinander, meldet nach JEDEM Command den Fortschritt (Recovery-Store)
 * und am Ende Completion. Bricht ab, wenn der Spieler die Verbindung verliert
 * — der persistierte Fortschritt erlaubt Fortsetzen nach Join.
 */
public final class CommandSendQueue {

    public static final long COMMAND_DELAY_MS = 1500L;

    /** Optionale per-Command-Verzögerungen (Index i = Pause VOR Command i). */
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
    private volatile int sent = 0;
    private final List<String> commands;
    private final IntConsumer onProgress;
    private final Runnable onComplete;

    public CommandSendQueue(List<String> commands, IntConsumer onProgress, Runnable onComplete) {
        this.commands = List.copyOf(commands);
        this.onProgress = onProgress;
        this.onComplete = onComplete;
    }

    public void start(int startIndex) {
        sent = Math.max(0, startIndex);
        scheduleNext(0L);
    }

    private void scheduleNext(long delayMs) {
        SCHEDULER.schedule(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (cancelled) {
                    return;
                }
                if (sent >= commands.size()) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    return;
                }
                if (client.player == null || client.getNetworkHandler() == null) {
                    // Verbindung weg -> stehen lassen; Recovery übernimmt nach Join
                    OttoExtra.LOGGER.warn("[letter] Versand unterbrochen bei {}/{}",
                            sent, commands.size());
                    return;
                }
                client.getNetworkHandler().sendChatCommand(commands.get(sent));
                sent++;
                if (onProgress != null) {
                    onProgress.accept(sent);
                }
                long next = delays != null && sent < delays.length
                        ? delays[sent] : COMMAND_DELAY_MS;
                scheduleNext(next);
            });
        }, delayMs, TimeUnit.MILLISECONDS);
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
