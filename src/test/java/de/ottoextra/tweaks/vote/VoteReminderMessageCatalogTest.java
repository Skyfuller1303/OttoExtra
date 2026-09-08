package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteReminderMessageCatalogTest {

    @Test
    void everyRoundUsesEachVariantOnceWithoutDirectRepeats() {
        VoteReminderMessageCatalog catalog = new VoteReminderMessageCatalog(new Random(1303L));
        VoteReminderMessageCatalog.Variant previous = null;

        for (int round = 0; round < 20; round++) {
            Set<VoteReminderMessageCatalog.Variant> roundVariants = new HashSet<>();
            for (int index = 0; index < VoteReminderMessageCatalog.size(); index++) {
                VoteReminderMessageCatalog.Variant current = catalog.next();
                if (previous != null) {
                    assertNotEquals(previous, current);
                }
                assertTrue(roundVariants.add(current));
                previous = current;
            }
            assertEquals(VoteReminderMessageCatalog.size(), roundVariants.size());
        }
    }
}
