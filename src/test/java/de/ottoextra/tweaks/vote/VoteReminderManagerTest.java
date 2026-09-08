package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteReminderManagerTest {

    @Test
    void openKeyRequiresActiveReminderOnTargetServerWithoutScreen() {
        assertTrue(VoteReminderManager.canOpenVotePages(
                true, true, false, false, true, true));

        assertFalse(VoteReminderManager.canOpenVotePages(
                false, true, false, false, true, true));
        assertFalse(VoteReminderManager.canOpenVotePages(
                true, false, false, false, true, true));
        assertFalse(VoteReminderManager.canOpenVotePages(
                true, true, true, false, true, true));
        assertFalse(VoteReminderManager.canOpenVotePages(
                true, true, false, true, true, true));
        assertFalse(VoteReminderManager.canOpenVotePages(
                true, true, false, false, false, true));
        assertFalse(VoteReminderManager.canOpenVotePages(
                true, true, false, false, true, false));
    }

    @Test
    void copiesUsernameBeforeOpeningVotePages() {
        List<String> actions = new ArrayList<>();

        VoteReminderManager.openVotePages(
                "Skyfuller1303",
                username -> actions.add("clipboard:" + username),
                () -> actions.add("browser")
        );

        assertEquals(List.of("clipboard:Skyfuller1303", "browser"), actions);
    }
}
