package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoteBrowserOpenerTest {

    @Test
    void opensEveryValidatedUrlWithoutExternalProcess() {
        List<URI> expected = VoteReminderConfig.ALLOWED_VOTE_URLS.stream()
                .map(URI::create)
                .toList();
        List<URI> opened = new ArrayList<>();
        VoteBrowserOpener opener = new VoteBrowserOpener(expected, opened::add);

        opener.openAll();

        assertEquals(expected, opened);
    }
}
