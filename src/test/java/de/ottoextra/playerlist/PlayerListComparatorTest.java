package de.ottoextra.playerlist;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerListComparatorTest {
    record Row(boolean spectator, int order, PlayerListSortKey key) { }

    @Test
    void mixedStatusAndServerOrderFormOneTransitiveOrdering() {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            rows.add(new Row(i % 3 == 2, i % 3 == 1 ? 2 : i % 3 == 2 ? 1 : 0, new PlayerListSortKey(i % 5 != 0,
                    "Lehen" + (i % 4), 0, 0, "", 0, "", "P" + i, "P" + i, new UUID(0, i))));
        }
        Comparator<Row> vanilla = Comparator.comparingInt(Row::order).reversed()
                .thenComparing(Row::spectator).thenComparing(r -> r.key().accountName());
        Comparator<Row> comparator = PlayerListSortingService.comparator(vanilla, Row::spectator, Row::key);
        for (Row a : rows) for (Row b : rows) {
            assertEquals(-Integer.signum(comparator.compare(b, a)), Integer.signum(comparator.compare(a, b)));
            for (Row c : rows) {
                if (comparator.compare(a, b) < 0 && comparator.compare(b, c) < 0)
                    assertTrue(comparator.compare(a, c) < 0);
            }
        }
        Collections.shuffle(rows, new Random(49));
        assertDoesNotThrow(() -> rows.sort(comparator));
        assertFalse(rows.getFirst().spectator());
        assertTrue(rows.getLast().spectator());
    }
}
