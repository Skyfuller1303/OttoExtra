package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentRecipientsTest {

    @Test
    void recordMovesNameToFrontWithoutDuplicates() {
        assertEquals(List.of("cara", "Anna", "Ben"),
                RecentRecipients.record(List.of("Anna", "Cara", "Ben"), "cara"));
        assertEquals(List.of("Anna"), RecentRecipients.record(List.of(), " Anna "));
        assertEquals(List.of("Anna"), RecentRecipients.record(List.of("Anna"), " "));
    }

    @Test
    void recordKeepsAtMostTwentyEntries() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            names = RecentRecipients.record(names, "p" + i);
        }
        assertEquals(RecentRecipients.MAX, names.size());
        assertEquals("p24", names.getFirst());
        assertEquals("p5", names.getLast());
    }

    @Test
    void orderPutsRecentFirstThenOnlineThenAlphabetical() {
        List<String> accounts = List.of("Zoe", "anna", "Ben", "Cara", "Dirk");
        Set<String> online = Set.of("Dirk", "Zoe");
        assertEquals(List.of("Ben", "Cara", "Dirk", "Zoe", "anna"),
                RecentRecipients.order(accounts, List.of("ben", "CARA"), online::contains));
        assertEquals(List.of("Dirk", "Zoe", "anna", "Ben", "Cara"),
                RecentRecipients.order(accounts, List.of(), online::contains));
    }
}
