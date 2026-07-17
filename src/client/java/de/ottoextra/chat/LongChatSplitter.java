package de.ottoextra.chat;

import java.util.ArrayList;
import java.util.List;

/** Reine, testbare Aufteilung langer Chatnachrichten. */
public final class LongChatSplitter {

    private LongChatSplitter() {
    }

    public static List<String> split(String message, int chunkSize, String marker) {
        List<String> out = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return out;
        }

        int size = Math.max(8, chunkSize);
        String continuation = marker == null ? "" : marker;
        if (continuation.length() >= size) {
            continuation = continuation.substring(0, size - 1);
        }
        int contentMax = Math.max(1, size - continuation.length());
        String remaining = message;

        int guard = 0;
        while (remaining.length() > size && guard++ < 1000) {
            int budget = Math.min(contentMax, remaining.length());
            String part;
            String close;
            String reopen;
            int cut;
            int fitAttempts = 0;

            while (true) {
                cut = preferredCut(remaining, budget);
                part = remaining.substring(0, cut).stripTrailing();
                if (part.isEmpty()) {
                    cut = Math.max(1, Math.min(budget, remaining.length()));
                    part = remaining.substring(0, cut);
                }

                RpChatSyntax.State state = RpChatSyntax.scan(part);
                close = RpChatSyntax.closers(state);
                reopen = RpChatSyntax.openers(state);

                if (part.length() + close.length() <= contentMax
                        || budget <= 1 || fitAttempts++ >= 12) {
                    break;
                }
                budget = Math.max(1, Math.min(remaining.length(), contentMax - close.length()));
            }

            out.add(part + close + continuation);
            remaining = reopen + remaining.substring(cut).stripLeading();
        }

        if (!remaining.isEmpty()) {
            out.add(remaining);
        }
        return out;
    }

    private static int preferredCut(String text, int budget) {
        int safeBudget = Math.max(1, Math.min(budget, text.length()));
        if (safeBudget >= text.length()) {
            return text.length();
        }

        int space = text.lastIndexOf(' ', safeBudget);
        // Sehr kleine Fragmente vermeiden; sonst lieber hart am Limit teilen.
        if (space >= Math.max(1, safeBudget / 3)) {
            return space;
        }
        return safeBudget;
    }
}
