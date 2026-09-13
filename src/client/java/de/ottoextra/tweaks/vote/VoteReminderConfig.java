package de.ottoextra.tweaks.vote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VoteReminderConfig {

    public static final String DEFAULT_TIME_ZONE = "Europe/Berlin";
    public static final List<String> DEFAULT_SERVER_ADDRESSES = List.of(
            "ottonien.com",
            "play.ottonien.com"
    );
    public static final List<String> ALLOWED_VOTE_URLS = List.of(
            "https://minecraft-servers.de/server/ottonien-historisches-minecraft-rpg/vote",
            "https://serverliste.net/vote/1560",
            "https://www.minecraft-serverlist.net/vote/59540",
            "https://minecraft-server.eu/vote/index/234EE"
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public int resetHour = 4;
    public String timeZone = DEFAULT_TIME_ZONE;
    public int joinDelaySeconds = 5;
    public int toastDurationSeconds = 10;
    public int reminderIntervalMinutes = 30;
    public List<String> serverAddresses = new ArrayList<>(DEFAULT_SERVER_ADDRESSES);
    public List<String> voteUrls = new ArrayList<>(ALLOWED_VOTE_URLS);
    public boolean openingNoticeAccepted = false;
    public String lastCompletedVoteDay = "";
    public int integrationMigrationVersion = 0;

    /** Übernimmt eine vorhandene Addon-Konfiguration genau einmal in OttoExtra. */
    public boolean migrateLegacy(Path legacyFile) {
        if (integrationMigrationVersion >= 1) {
            return false;
        }
        if (Files.isRegularFile(legacyFile)) {
            try {
                VoteReminderConfig legacy = GSON.fromJson(
                        Files.readString(legacyFile, StandardCharsets.UTF_8),
                        VoteReminderConfig.class
                );
                if (legacy != null) {
                    enabled = legacy.enabled;
                    resetHour = legacy.resetHour;
                    timeZone = legacy.timeZone;
                    joinDelaySeconds = legacy.joinDelaySeconds;
                    toastDurationSeconds = legacy.toastDurationSeconds;
                    reminderIntervalMinutes = legacy.reminderIntervalMinutes;
                    serverAddresses = legacy.serverAddresses;
                    voteUrls = legacy.voteUrls;
                    lastCompletedVoteDay = legacy.lastCompletedVoteDay;
                }
            } catch (Exception exception) {
                OttoExtra.LOGGER.warn(
                        "Alte Vote-Konfiguration konnte nicht übernommen werden: {}",
                        legacyFile, exception);
            }
        }
        integrationMigrationVersion = 1;
        sanitize();
        return true;
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException ignored) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
    }

    public LocalDate voteDay(Instant instant) {
        return VoteDayCalculator.voteDay(instant, zoneId(), resetHour);
    }

    public boolean isCompleted(LocalDate voteDay) {
        return voteDay != null && voteDay.toString().equals(lastCompletedVoteDay);
    }

    public void complete(LocalDate voteDay) {
        lastCompletedVoteDay = voteDay.toString();
    }

    public void resetCompletion() {
        lastCompletedVoteDay = "";
    }

    public boolean requiresOpeningNotice() {
        return !openingNoticeAccepted;
    }

    public void acceptOpeningNotice() {
        openingNoticeAccepted = true;
    }

    public List<URI> validatedVoteUris() {
        List<String> safeUrls = validatedVoteUrlStrings(voteUrls);
        return safeUrls.stream().map(URI::create).toList();
    }

    public void sanitize() {
        resetHour = Math.clamp(resetHour, 0, 23);
        joinDelaySeconds = Math.clamp(joinDelaySeconds, 0, 60);
        toastDurationSeconds = Math.clamp(toastDurationSeconds, 2, 60);
        reminderIntervalMinutes = Math.clamp(reminderIntervalMinutes, 1, 1440);

        try {
            timeZone = ZoneId.of(timeZone == null ? DEFAULT_TIME_ZONE : timeZone).getId();
        } catch (DateTimeException ignored) {
            timeZone = DEFAULT_TIME_ZONE;
        }

        serverAddresses = sanitizeServerAddresses(serverAddresses);
        voteUrls = new ArrayList<>(validatedVoteUrlStrings(voteUrls));

        if (lastCompletedVoteDay == null) {
            lastCompletedVoteDay = "";
        } else if (!lastCompletedVoteDay.isBlank()) {
            try {
                lastCompletedVoteDay = LocalDate.parse(lastCompletedVoteDay).toString();
            } catch (DateTimeException ignored) {
                lastCompletedVoteDay = "";
            }
        }
    }

    private static List<String> sanitizeServerAddresses(List<String> addresses) {
        if (addresses == null) {
            return new ArrayList<>(DEFAULT_SERVER_ADDRESSES);
        }
        List<String> normalized = addresses.stream()
                .map(ServerAddressMatcher::normalize)
                .filter(address -> !address.isEmpty())
                .distinct()
                .toList();
        if (normalized.isEmpty() || normalized.equals(List.of("play.ottonien.com"))) {
            return new ArrayList<>(DEFAULT_SERVER_ADDRESSES);
        }
        return new ArrayList<>(normalized);
    }

    private static List<String> validatedVoteUrlStrings(List<String> urls) {
        if (urls == null || urls.size() != ALLOWED_VOTE_URLS.size()) {
            return ALLOWED_VOTE_URLS;
        }
        Set<String> configured = new HashSet<>(urls);
        if (configured.size() != ALLOWED_VOTE_URLS.size()
                || !configured.equals(new HashSet<>(ALLOWED_VOTE_URLS))) {
            return ALLOWED_VOTE_URLS;
        }
        return List.copyOf(urls);
    }

}
