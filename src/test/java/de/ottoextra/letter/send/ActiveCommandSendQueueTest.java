package de.ottoextra.letter.send;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveCommandSendQueueTest {

    @Test
    void refusesParallelLetterOrRecoveryStartAndClearsAnnouncementOnCompletion() {
        var harness = new CommandSendQueueTest.Harness();
        var active = new ActiveCommandSendQueue(harness::queue);
        assertTrue(active.start(List.of("letter announcement"), null, 0, true, null, null));
        assertTrue(active.isSending());
        assertTrue(active.isAnnouncement());

        assertFalse(active.start(List.of("letter recovery"), null, 0, false, null, null));
        assertEquals(1, harness.scheduled.size());
        harness.step();
        harness.step();

        assertEquals(List.of("letter announcement"), harness.current.commands);
        assertFalse(active.isSending());
        assertFalse(active.isAnnouncement());
        assertTrue(active.start(List.of("letter next"), null, 0, false, null, null));
    }

    @Test
    void disconnectThenRecoveryPreservesProgressAndDropsPendingOldCommand() {
        var harness = new CommandSendQueueTest.Harness();
        var original = harness.current;
        var active = new ActiveCommandSendQueue(harness::queue);
        List<String> commands = List.of("letter A", "letter B");
        List<Integer> progress = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        active.start(commands, null, 0, false, progress::add, completions::incrementAndGet);
        harness.step();
        harness.dispatchNext();

        active.cancel();
        assertFalse(active.isSending());
        harness.reconnect();
        assertTrue(active.start(commands, null, progress.getLast(), false,
                progress::add, completions::incrementAndGet));
        harness.runClientNext();

        assertTrue(active.isSending());
        assertEquals(List.of(1), progress);
        assertEquals(0, completions.get());
        assertTrue(harness.current.commands.isEmpty());
        harness.step();
        harness.step();

        assertEquals(List.of("letter A"), original.commands);
        assertEquals(List.of("letter B"), harness.current.commands);
        assertEquals(List.of(1, 2), progress);
        assertEquals(1, completions.get());
        assertFalse(active.isSending());
    }

    @Test
    void staleProgressAndCompletionCannotMutateNewSendOrItsCache() {
        var harness = new CommandSendQueueTest.Harness();
        List<IntConsumer> progressCallbacks = new ArrayList<>();
        List<Runnable> completionCallbacks = new ArrayList<>();
        var active = new ActiveCommandSendQueue((commands, progress, complete) -> {
            progressCallbacks.add(progress);
            completionCallbacks.add(complete);
            return harness.queue(commands, progress, complete);
        });
        AtomicInteger savedProgress = new AtomicInteger();
        AtomicInteger cacheClears = new AtomicInteger();
        active.start(List.of("letter old"), null, 0, false,
                savedProgress::set, cacheClears::incrementAndGet);
        active.cancel();
        harness.reconnect();
        active.start(List.of("letter new"), null, 0, true,
                savedProgress::set, cacheClears::incrementAndGet);

        progressCallbacks.getFirst().accept(1);
        completionCallbacks.getFirst().run();

        assertEquals(0, savedProgress.get());
        assertEquals(0, cacheClears.get());
        assertTrue(active.isSending());
        assertTrue(active.isAnnouncement());
        harness.dispatchNext();
        assertTrue(harness.clientTasks.isEmpty());
        harness.step();
        harness.step();
        assertEquals(1, savedProgress.get());
        assertEquals(1, cacheClears.get());
        assertFalse(active.isSending());
    }

    @Test
    void lostConnectionReleasesActiveSendWithoutClearingRecoveryProgress() {
        var harness = new CommandSendQueueTest.Harness();
        var active = new ActiveCommandSendQueue(harness::queue);
        AtomicInteger savedProgress = new AtomicInteger();
        AtomicInteger cacheClears = new AtomicInteger();
        active.start(List.of("letter A", "letter B"), null, 0, true,
                savedProgress::set, cacheClears::incrementAndGet);
        harness.step();
        harness.current = null;
        harness.step();

        assertFalse(active.isSending());
        assertFalse(active.isAnnouncement());
        assertEquals(1, savedProgress.get());
        assertEquals(0, cacheClears.get());
        harness.reconnect();
        assertTrue(active.start(List.of("letter A", "letter B"), null, 1, false,
                savedProgress::set, cacheClears::incrementAndGet));
    }

    @Test
    void stopCancelsPostedCompletionWithoutClearingProgress() {
        var harness = new CommandSendQueueTest.Harness();
        var active = new ActiveCommandSendQueue(harness::queue);
        AtomicInteger cacheClears = new AtomicInteger();
        active.start(List.of("letter A"), null, 0, true, null,
                cacheClears::incrementAndGet);
        harness.step();
        harness.dispatchNext();

        active.cancel();
        harness.runClientNext();

        assertFalse(active.isSending());
        assertFalse(active.isAnnouncement());
        assertEquals(0, cacheClears.get());
        assertTrue(harness.scheduled.isEmpty());
    }
}
