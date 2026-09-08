package de.ottoextra.verify;

import de.ottoextra.api.model.PlayerRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AccountOverviewStateTest {
    @Test
    void lateOlderResponseCannotReplaceNewerDataOrTimestamp() {
        var time = new AtomicReference<>("first");
        var state = new AccountOverviewState(time::get);
        var first = new CompletableFuture<AccountOverviewState.Details>();
        var second = new CompletableFuture<AccountOverviewState.Details>();
        state.refresh(true, () -> first, Runnable::run);
        state.refresh(true, () -> second, Runnable::run);
        var latest = details("newer");
        second.complete(latest);
        time.set("too late");
        first.complete(details("older"));
        assertSame(latest.profile(), state.profile);
        assertSame(latest.account(), state.account);
        assertEquals("first", state.refreshedAt);
        assertFalse(state.loading);
    }

    @Test
    void responseQueuedOnClientThreadIsRejectedAfterAnotherRefreshStarts() {
        var tasks = new ArrayDeque<Runnable>();
        var state = new AccountOverviewState(() -> "now");
        var second = new CompletableFuture<AccountOverviewState.Details>();
        state.refresh(false, () -> CompletableFuture.completedFuture(details("old")), tasks::add);
        state.refresh(false, () -> second, tasks::add);
        tasks.remove().run();
        assertTrue(state.loading);
        assertNull(state.profile);
        second.complete(details("new"));
        tasks.remove().run();
        assertEquals("new", state.profile.name());
    }

    @Test
    void failureKeepsPreviousDataAndTimestampButExposesTheError() {
        var state = new AccountOverviewState(() -> "success");
        var data = details("saved");
        state.refresh(true, () -> CompletableFuture.completedFuture(data), Runnable::run);
        var failure = new IllegalStateException("Offline");
        state.refresh(true, () -> CompletableFuture.failedFuture(failure), Runnable::run);
        assertSame(data.profile(), state.profile);
        assertSame(failure, state.error);
        assertEquals("success", state.refreshedAt);
        assertFalse(state.loading);
    }

    @Test
    void removingScreenRejectsAnOutstandingResponse() {
        var state = new AccountOverviewState(() -> "now");
        var future = new CompletableFuture<AccountOverviewState.Details>();
        state.refresh(false, () -> future, Runnable::run);
        state.invalidate();
        future.complete(details("late"));
        assertNull(state.profile);
        assertEquals("", state.refreshedAt);
    }

    @Test
    void successfulEmptyResponseClearsObsoleteProfile() {
        var state = new AccountOverviewState(() -> "now");
        state.refresh(true, () -> CompletableFuture.completedFuture(details("old")), Runnable::run);
        state.refresh(false, () -> CompletableFuture.completedFuture(new AccountOverviewState.Details(null, null)), Runnable::run);
        assertNull(state.profile);
        assertNull(state.account);
        assertEquals("", state.refreshedAt);
        assertNull(state.error);
    }

    private static AccountOverviewState.Details details(String name) {
        var profile = new PlayerRecord(null, null, name, name, null, null, null, null,
                null, null, 42, 0, 0, List.of(), null, null);
        return new AccountOverviewState.Details(profile,
                new AccountLinkApi.AccountSummary("linked", name, "Discord", profile));
    }
}
