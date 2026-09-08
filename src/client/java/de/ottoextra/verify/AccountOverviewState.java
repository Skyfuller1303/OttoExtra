package de.ottoextra.verify;

import de.ottoextra.api.model.PlayerRecord;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/** Screen-owned state; requests are completed on the client executor. */
final class AccountOverviewState {
    record Details(PlayerRecord profile, AccountLinkApi.AccountSummary account) { }

    PlayerRecord profile;
    AccountLinkApi.AccountSummary account;
    boolean loading;
    Throwable error;
    String refreshedAt = "";
    private long generation;
    private final Supplier<String> timestamp;

    AccountOverviewState(Supplier<String> timestamp) {
        this.timestamp = timestamp;
    }

    void refresh(boolean linked, Supplier<CompletableFuture<Details>> request, Executor executor) {
        long current = ++generation;
        loading = true;
        error = null;
        if (!linked) account = null;
        try {
            request.get().whenCompleteAsync((value, failure) -> complete(current, value, failure), executor);
        } catch (RuntimeException failure) {
            complete(current, null, failure);
        }
    }

    private void complete(long current, Details value, Throwable failure) {
        if (current != generation) return;
        loading = false;
        error = failure;
        if (failure != null) return; // Keep useful old data and its original timestamp.
        profile = value == null ? null : value.profile();
        account = value == null ? null : value.account();
        refreshedAt = profile == null ? "" : timestamp.get();
    }

    void invalidate() {
        generation++;
        loading = false;
    }
}
