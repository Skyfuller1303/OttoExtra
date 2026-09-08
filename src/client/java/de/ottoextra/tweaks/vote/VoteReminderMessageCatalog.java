package de.ottoextra.tweaks.vote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class VoteReminderMessageCatalog {

    private static final List<Variant> VARIANTS = List.of(
            variant(1),
            variant(2),
            variant(3),
            variant(4),
            variant(5),
            variant(6),
            variant(7),
            variant(8)
    );

    private final Random random;
    private final List<Variant> remaining = new ArrayList<>();
    private Variant previous;

    public VoteReminderMessageCatalog() {
        this(new Random());
    }

    VoteReminderMessageCatalog(Random random) {
        this.random = random;
    }

    public synchronized Variant next() {
        if (remaining.isEmpty()) {
            refill();
        }
        Variant selected = remaining.removeLast();
        previous = selected;
        return selected;
    }

    static int size() {
        return VARIANTS.size();
    }

    private void refill() {
        remaining.addAll(VARIANTS);
        Collections.shuffle(remaining, random);
        if (previous != null && remaining.size() > 1 && previous.equals(remaining.getLast())) {
            Collections.swap(remaining, 0, remaining.size() - 1);
        }
    }

    private static Variant variant(int number) {
        return new Variant(
                "ottoextra.tweaks.vote.toast." + number + ".title",
                "ottoextra.tweaks.vote.toast." + number + ".description"
        );
    }

    public record Variant(String titleKey, String descriptionKey) {
    }
}
