package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteReminderConfigTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void sanitizeRestoresSafeDefaultsForInvalidValues() {
        VoteReminderConfig config = new VoteReminderConfig();
        config.resetHour = 40;
        config.joinDelaySeconds = -1;
        config.toastDurationSeconds = 500;
        config.reminderIntervalMinutes = 0;
        config.timeZone = "Invalid/Zone";
        config.serverAddresses = List.of("https://play.ottonien.com");
        config.voteUrls = List.of("https://example.com/vote");
        config.lastCompletedVoteDay = "not-a-date";

        config.sanitize();

        assertEquals(23, config.resetHour);
        assertEquals(0, config.joinDelaySeconds);
        assertEquals(60, config.toastDurationSeconds);
        assertEquals(1, config.reminderIntervalMinutes);
        assertEquals(VoteReminderConfig.DEFAULT_TIME_ZONE, config.timeZone);
        assertEquals(VoteReminderConfig.DEFAULT_SERVER_ADDRESSES, config.serverAddresses);
        assertEquals(VoteReminderConfig.ALLOWED_VOTE_URLS, config.voteUrls);
        assertEquals("", config.lastCompletedVoteDay);
    }

    @Test
    void validatedVoteUrisContainOnlyFourAllowlistedHttpsPages() {
        VoteReminderConfig config = new VoteReminderConfig();
        config.voteUrls = List.of(
                VoteReminderConfig.ALLOWED_VOTE_URLS.get(3),
                VoteReminderConfig.ALLOWED_VOTE_URLS.get(2),
                VoteReminderConfig.ALLOWED_VOTE_URLS.get(1),
                VoteReminderConfig.ALLOWED_VOTE_URLS.get(0)
        );

        var uris = config.validatedVoteUris();

        assertEquals(4, uris.size());
        assertEquals("https", uris.getFirst().getScheme());
        assertEquals(VoteReminderConfig.ALLOWED_VOTE_URLS.get(3), uris.getFirst().toString());
    }

    @Test
    void migratesLegacyAddonConfigOnceWithoutDeletingIt() throws Exception {
        Path legacyFile = temporaryDirectory.resolve("ottoextra-vote.json");
        Files.writeString(legacyFile, """
                {
                  "enabled": false,
                  "resetHour": 7,
                  "joinDelaySeconds": 12,
                  "toastDurationSeconds": 20,
                  "reminderIntervalMinutes": 90,
                  "lastCompletedVoteDay": "2026-08-20"
                }
                """);
        VoteReminderConfig config = new VoteReminderConfig();

        assertTrue(config.migrateLegacy(legacyFile));
        assertFalse(config.enabled);
        assertEquals(7, config.resetHour);
        assertEquals(12, config.joinDelaySeconds);
        assertEquals(20, config.toastDurationSeconds);
        assertEquals(90, config.reminderIntervalMinutes);
        assertEquals("2026-08-20", config.lastCompletedVoteDay);
        assertEquals(1, config.integrationMigrationVersion);
        assertTrue(Files.exists(legacyFile));
        assertFalse(config.migrateLegacy(legacyFile));
    }
}
