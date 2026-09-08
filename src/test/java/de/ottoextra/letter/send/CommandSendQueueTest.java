package de.ottoextra.letter.send;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandSendQueueTest {

    @Test
    void sendFailureInterruptsWithoutAdvancingRecoveryOrReportingCompletion() {
        Harness harness = new Harness();
        harness.current.failSend = true;
        AtomicInteger progress = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A"), progress::set,
                completions::incrementAndGet).onInterrupted(interruptions::incrementAndGet);
        queue.start(0);
        harness.step();

        assertEquals(0, queue.sent());
        assertEquals(0, progress.get());
        assertEquals(0, completions.get());
        assertEquals(1, interruptions.get());
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    void changedHeldLetterStopsRemainingCommandsAndDoesNotComplete() {
        Harness harness = new Harness();
        AtomicBoolean sameLetter = new AtomicBoolean(true);
        AtomicInteger interruptions = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A", "post Alice"), null,
                completions::incrementAndGet).whileValid(sameLetter::get)
                .onInterrupted(interruptions::incrementAndGet);
        queue.start(0);
        harness.step();
        sameLetter.set(false);
        harness.step();

        assertEquals(List.of("letter A"), harness.current.commands);
        assertEquals(1, queue.sent());
        assertEquals(1, interruptions.get());
        assertEquals(0, completions.get());
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    void rejectedFinalServerWriteCannotReportSuccess() {
        Harness harness = new Harness();
        AtomicBoolean sameLetter = new AtomicBoolean(true);
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A"), null,
                completions::incrementAndGet).whileValid(sameLetter::get)
                .onInterrupted(interruptions::incrementAndGet);
        queue.start(0);
        harness.step();
        sameLetter.set(false);
        harness.step();

        assertEquals(1, interruptions.get());
        assertEquals(0, completions.get());
    }

    @Test
    void sendsInOrderWithConfiguredDelaysAndCompletesOnce() {
        Harness harness = new Harness();
        List<Integer> progress = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A", "letter B"),
                progress::add, completions::incrementAndGet).withDelays(new long[]{0, 5000});

        queue.start(0);
        queue.start(0);
        harness.step();
        harness.step();
        harness.step();
        queue.start(0);

        assertEquals(List.of("letter A", "letter B"), harness.current.commands);
        assertEquals(List.of(1, 2), progress);
        assertEquals(List.of(0L, 5000L, CommandSendQueue.COMMAND_DELAY_MS), harness.delays);
        assertEquals(1, completions.get());
        assertEquals(2, queue.sent());
        assertEquals(2, queue.total());
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    void handlerChangeStopsAlreadyDispatchedCommandAndPreservesProgress() {
        Harness harness = new Harness();
        Harness.Session original = harness.current;
        List<Integer> progress = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A", "letter B"),
                progress::add, completions::incrementAndGet)
                .onInterrupted(interruptions::incrementAndGet);
        queue.start(0);
        harness.step();
        harness.dispatchNext();

        harness.reconnect();
        harness.runClientNext();

        assertEquals(List.of("letter A"), original.commands);
        assertTrue(harness.current.commands.isEmpty());
        assertEquals(List.of(1), progress);
        assertEquals(1, queue.sent());
        assertEquals(0, completions.get());
        assertEquals(1, interruptions.get());
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    void cancelDuringPauseDoesNotDispatchMoreClientWork() {
        Harness harness = new Harness();
        CommandSendQueue queue = harness.queue(List.of("letter A", "letter B"), null, null);
        queue.start(0);
        harness.step();

        queue.cancel();
        harness.reconnect();
        harness.dispatchNext();

        assertTrue(harness.clientTasks.isEmpty());
        assertTrue(harness.current.commands.isEmpty());
        assertEquals(1, queue.sent());
    }

    @Test
    void cancelBlocksAlreadyDispatchedCommandAndCallbacks() {
        Harness harness = new Harness();
        AtomicInteger progress = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A"), progress::set,
                completions::incrementAndGet);
        queue.start(0);
        harness.dispatchNext();

        queue.cancel();
        harness.runClientNext();

        assertTrue(harness.current.commands.isEmpty());
        assertEquals(0, progress.get());
        assertEquals(0, completions.get());
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    void cancelBlocksAlreadyDispatchedCompletion() {
        Harness harness = new Harness();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A"), null,
                completions::incrementAndGet);
        queue.start(0);
        harness.step();
        harness.dispatchNext();

        queue.cancel();
        harness.runClientNext();

        assertEquals(1, queue.sent());
        assertEquals(0, completions.get());
    }

    @Test
    void handlerChangeBlocksCompletionFromPreviousSession() {
        Harness harness = new Harness();
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A"), null,
                completions::incrementAndGet).onInterrupted(interruptions::incrementAndGet);
        queue.start(0);
        harness.step();
        harness.dispatchNext();

        harness.reconnect();
        harness.runClientNext();

        assertEquals(0, completions.get());
        assertEquals(1, interruptions.get());
    }

    @Test
    void capturesConnectionAtStartAndResumesOnlyRemainingCommands() {
        Harness harness = new Harness();
        Harness.Session original = harness.current;
        List<Integer> progress = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A", "letter B", "post Alice"),
                progress::add, completions::incrementAndGet);

        harness.reconnect();
        queue.start(1);
        harness.step();
        harness.step();
        harness.step();

        assertTrue(original.commands.isEmpty());
        assertEquals(List.of("letter B", "post Alice"), harness.current.commands);
        assertEquals(List.of(2, 3), progress);
        assertEquals(1, completions.get());
    }

    @Test
    void startWithoutConnectionDoesNotAdoptLaterConnection() {
        Harness harness = new Harness();
        harness.current = null;
        AtomicInteger interruptions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of("letter A"), null, null)
                .onInterrupted(interruptions::incrementAndGet);
        queue.start(0);

        harness.reconnect();
        harness.step();

        assertTrue(harness.current.commands.isEmpty());
        assertEquals(1, interruptions.get());
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    void emptyPlanCompletesWithoutSendingOrProgress() {
        Harness harness = new Harness();
        List<Integer> progress = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        CommandSendQueue queue = harness.queue(List.of(), progress::add,
                completions::incrementAndGet);
        queue.start(0);
        harness.step();

        assertTrue(harness.current.commands.isEmpty());
        assertTrue(progress.isEmpty());
        assertEquals(1, completions.get());
    }

    static final class Harness {
        final Deque<Runnable> scheduled = new ArrayDeque<>();
        final Deque<Runnable> clientTasks = new ArrayDeque<>();
        final List<Long> delays = new ArrayList<>();
        Session current = new Session();

        CommandSendQueue queue(List<String> commands, IntConsumer onProgress, Runnable onComplete) {
            return new CommandSendQueue(commands, onProgress, onComplete, (task, delay) -> {
                delays.add(delay);
                scheduled.add(task);
            }, clientTasks::add, () -> current);
        }

        void reconnect() {
            current = new Session();
        }

        void dispatchNext() {
            scheduled.removeFirst().run();
        }

        void runClientNext() {
            clientTasks.removeFirst().run();
        }

        void step() {
            dispatchNext();
            runClientNext();
        }

        final class Session implements CommandSendQueue.Connection {
            final List<String> commands = new ArrayList<>();
            boolean failSend;

            @Override
            public boolean isCurrent() {
                return current == this;
            }

            @Override
            public void send(String command) {
                if (failSend) {
                    throw new IllegalStateException("test connection rejected command");
                }
                commands.add(command);
            }
        }
    }
}
