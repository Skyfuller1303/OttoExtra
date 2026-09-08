package de.ottoextra.letter.send;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.BooleanSupplier;

/** Verwaltet genau einen Versand; Aufrufe und Callbacks laufen auf dem Client-Thread. */
public final class ActiveCommandSendQueue {

    @FunctionalInterface
    interface QueueFactory {
        CommandSendQueue create(List<String> commands, IntConsumer onProgress, Runnable onComplete);
    }

    private final QueueFactory factory;
    private CommandSendQueue active;
    private boolean announcement;
    private long generation;

    public ActiveCommandSendQueue() {
        this(CommandSendQueue::new);
    }

    ActiveCommandSendQueue(QueueFactory factory) {
        this.factory = factory;
    }

    public boolean isSending() {
        return active != null;
    }

    public boolean isAnnouncement() {
        return active != null && announcement;
    }

    public boolean start(List<String> commands, long[] delays, int startIndex,
                          boolean announcement, IntConsumer onProgress, Runnable onComplete) {
        return start(commands, delays, startIndex, announcement, onProgress, onComplete, null);
    }

    public boolean start(List<String> commands, long[] delays, int startIndex,
                         boolean announcement, IntConsumer onProgress, Runnable onComplete,
                         BooleanSupplier continuationGuard) {
        if (active != null) {
            return false;
        }
        long currentGeneration = ++generation;
        active = factory.create(commands, sent -> {
            if (currentGeneration == generation && active != null && onProgress != null) {
                onProgress.accept(sent);
            }
        }, () -> {
            if (currentGeneration != generation || active == null) {
                return;
            }
            active = null;
            this.announcement = false;
            if (onComplete != null) {
                onComplete.run();
            }
        }).withDelays(delays).whileValid(continuationGuard).onInterrupted(() -> {
            if (currentGeneration == generation) {
                cancel();
            }
        });
        this.announcement = announcement;
        active.start(startIndex);
        return true;
    }

    public void cancel() {
        generation++;
        if (active != null) {
            active.cancel();
            active = null;
        }
        announcement = false;
    }
}
