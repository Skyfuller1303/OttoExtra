package de.ottoextra.tweaks.vote;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class VoteReminderManager {

    private static final long NEVER = Long.MAX_VALUE;
    private static final long CALENDAR_CHECK_INTERVAL_MILLIS = 1_000L;

    private final VoteReminderConfig config;
    private final VoteBrowserOpener browserOpener;
    private final KeyBinding openKey;
    private final Clock clock;
    private final LongSupplier measuringTime;
    private final Runnable save;
    private final VoteReminderMessageCatalog messageCatalog = new VoteReminderMessageCatalog();

    private boolean targetServerConnected;
    private boolean testToast;
    private boolean enterKeyDown;
    private boolean suppressOpenKeyUntilRelease;
    private VoteReminderToast activeToast;
    private LocalDate voteDay;
    private long toastEndsAt = NEVER;
    private long nextReminderAt = NEVER;
    private long nextCalendarCheckAt = NEVER;

    public VoteReminderManager(
            VoteReminderConfig config,
            VoteBrowserOpener browserOpener,
            KeyBinding openKey,
            Runnable save
    ) {
        this(config, browserOpener, openKey, Clock.systemUTC(), Util::getMeasuringTimeMs, save);
    }

    VoteReminderManager(
            VoteReminderConfig config,
            VoteBrowserOpener browserOpener,
            KeyBinding openKey,
            Clock clock,
            LongSupplier measuringTime,
            Runnable save
    ) {
        this.config = config;
        this.browserOpener = browserOpener;
        this.openKey = openKey;
        this.clock = clock;
        this.measuringTime = measuringTime;
        this.save = save;
    }

    public void connected(MinecraftClient client, ServerInfo serverInfo) {
        hideToast();
        clearRuntimeState();
        targetServerConnected = serverInfo != null
                && ServerAddressMatcher.matches(serverInfo.address, config.serverAddresses);
        OttoExtra.LOGGER.info(
                "Vote-Reminder Serverprüfung: {} -> {}",
                serverInfo == null ? "unbekannt" : serverInfo.address,
                targetServerConnected ? "aktiv" : "inaktiv"
        );
        if (!targetServerConnected) {
            return;
        }

        long now = measuringTime.getAsLong();
        refreshVoteDay(now, true);
        if (config.enabled && !config.isCompleted(voteDay)) {
            nextReminderAt = now + secondsToMillis(config.joinDelaySeconds);
        }
    }

    public void disconnected(MinecraftClient client) {
        hideToast();
        clearRuntimeState();
    }

    public void tick(MinecraftClient client) {
        boolean openKeyPressed = consumeOpenKeyPress(client);
        long now = measuringTime.getAsLong();

        if (activeToast != null && now >= toastEndsAt) {
            boolean expiredTestToast = testToast;
            hideToast();
            if (!expiredTestToast && targetServerConnected) {
                nextReminderAt = now + minutesToMillis(config.reminderIntervalMinutes);
            }
        }

        if (activeToast != null && openKeyPressed && canOpenVotePages(client)) {
            OttoExtra.LOGGER.info("Enter erkannt, öffne {} Vote-Seiten.",
                    config.validatedVoteUris().size());
            openVotePages(
                    client.player.getGameProfile().name(),
                    client.keyboard::setClipboard,
                    browserOpener::openAll
            );
            config.complete(voteDay);
            save.run();
            hideToast();
            nextReminderAt = NEVER;
            client.player.sendMessage(Text.translatable("ottoextra.tweaks.vote.completed"), false);
            return;
        }

        if (testToast) {
            return;
        }
        if (!targetServerConnected || !config.enabled || client.world == null || client.player == null) {
            hideToast();
            nextReminderAt = NEVER;
            return;
        }

        refreshVoteDay(now, false);
        if (config.isCompleted(voteDay)) {
            hideToast();
            nextReminderAt = NEVER;
            return;
        }

        if (activeToast != null) {
            return;
        }

        if (nextReminderAt == NEVER) {
            nextReminderAt = now + secondsToMillis(config.joinDelaySeconds);
        }
        if (now >= nextReminderAt) {
            showToast(client, false, now);
        }
    }

    public void showTest(MinecraftClient client) {
        long now = measuringTime.getAsLong();
        showToast(client, true, now);
    }

    public void reset(MinecraftClient client) {
        config.resetCompletion();
        save.run();
        long now = measuringTime.getAsLong();
        refreshVoteDay(now, true);
        hideToast();
        showToast(client, !targetServerConnected || !config.enabled, now);
    }

    boolean isToastActive() {
        return activeToast != null;
    }

    private boolean consumeOpenKeyPress(MinecraftClient client) {
        boolean queuedPress = false;
        while (openKey.wasPressed()) {
            queuedPress = true;
        }

        boolean keyDown = isOpenKeyDown(client);
        if (suppressOpenKeyUntilRelease) {
            enterKeyDown = keyDown;
            if (!keyDown) {
                suppressOpenKeyUntilRelease = false;
            }
            return false;
        }

        boolean physicalPress = keyDown && !enterKeyDown;
        enterKeyDown = keyDown;
        return queuedPress || physicalPress;
    }

    private boolean isOpenKeyDown(MinecraftClient client) {
        return openKey.isPressed()
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_ENTER)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_KP_ENTER);
    }

    private boolean canOpenVotePages(MinecraftClient client) {
        return canOpenVotePages(
                targetServerConnected,
                activeToast != null,
                client.currentScreen != null,
                client.getOverlay() != null,
                client.world != null,
                client.player != null
        );
    }

    static boolean canOpenVotePages(
            boolean targetServerConnected,
            boolean toastActive,
            boolean screenOpen,
            boolean overlayOpen,
            boolean worldAvailable,
            boolean playerAvailable
    ) {
        return targetServerConnected
                && toastActive
                && !screenOpen
                && !overlayOpen
                && worldAvailable
                && playerAvailable;
    }

    static void openVotePages(
            String username,
            Consumer<String> clipboard,
            Runnable browserOpener
    ) {
        if (username != null && !username.isBlank()) {
            clipboard.accept(username);
        }
        browserOpener.run();
    }

    private void refreshVoteDay(long now, boolean force) {
        if (!force && now < nextCalendarCheckAt) {
            return;
        }
        nextCalendarCheckAt = now + CALENDAR_CHECK_INTERVAL_MILLIS;

        LocalDate currentVoteDay = config.voteDay(clock.instant());
        if (!currentVoteDay.equals(voteDay)) {
            voteDay = currentVoteDay;
            hideToast();
            nextReminderAt = now + secondsToMillis(config.joinDelaySeconds);
        }
    }

    public void renderToast(DrawContext context, RenderTickCounter tickCounter) {
        if (activeToast != null) {
            activeToast.render(context, tickCounter);
        }
    }

    private void showToast(MinecraftClient client, boolean test, long now) {
        hideToast();
        while (openKey.wasPressed()) {
            // Enter used to submit a client command must not activate the new toast.
        }
        enterKeyDown = isOpenKeyDown(client);
        suppressOpenKeyUntilRelease = true;

        long durationMillis = secondsToMillis(config.toastDurationSeconds);
        activeToast = new VoteReminderToast(durationMillis, messageCatalog.next());
        testToast = test;
        toastEndsAt = now + durationMillis;
        nextReminderAt = NEVER;
        OttoExtra.LOGGER.info("Vote-Reminder-Toast angezeigt{}.", test ? " (Test)" : "");
    }

    private void hideToast() {
        if (activeToast != null) {
            activeToast.hide();
        }
        activeToast = null;
        testToast = false;
        suppressOpenKeyUntilRelease = false;
        toastEndsAt = NEVER;
    }

    private void clearRuntimeState() {
        targetServerConnected = false;
        voteDay = null;
        nextReminderAt = NEVER;
        nextCalendarCheckAt = NEVER;
    }

    private static long secondsToMillis(int seconds) {
        return seconds * 1_000L;
    }

    private static long minutesToMillis(int minutes) {
        return minutes * 60_000L;
    }
}
