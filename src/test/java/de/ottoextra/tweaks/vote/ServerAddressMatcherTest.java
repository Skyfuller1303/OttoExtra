package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerAddressMatcherTest {

    private static final List<String> OTTONIEN = VoteReminderConfig.DEFAULT_SERVER_ADDRESSES;

    @Test
    void matchesConfiguredHostWithCasePortAndTrailingDot() {
        assertTrue(ServerAddressMatcher.matches("PLAY.OTTONIEN.COM.:25565", OTTONIEN));
        assertTrue(ServerAddressMatcher.matches("ottonien.com:25565", OTTONIEN));
    }

    @Test
    void rejectsDifferentHostAndUrlSyntax() {
        assertFalse(ServerAddressMatcher.matches("example.com", OTTONIEN));
        assertFalse(ServerAddressMatcher.matches("https://play.ottonien.com", OTTONIEN));
        assertFalse(ServerAddressMatcher.matches("play.ottonien.com.example.org", OTTONIEN));
    }
}
