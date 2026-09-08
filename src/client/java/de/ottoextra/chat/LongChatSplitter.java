package de.ottoextra.chat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Reine, testbare Aufteilung langer Chatnachrichten. */
public final class LongChatSplitter {

    private LongChatSplitter() {
    }

    public static List<String> split(String message, int chunkSize, String marker) {
        return split(message, chunkSize, marker, true);
    }

    public static List<String> split(String message, int chunkSize, String marker,
                                     boolean preserveRpSyntax) {
        List<String> out = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return out;
        }

        int size = Math.max(8, chunkSize);
        String continuation = marker == null ? "" : marker;
        // Always leave room for one complete Unicode code point.
        if (continuation.length() > size - 2) {
            continuation = continuation.substring(0, codePointBoundary(continuation, size - 2));
        }
        int contentMax = Math.max(1, size - continuation.length());
        String remaining = message;

        while (remaining.length() > size) {
            BitSet emoteDelimiters = preserveRpSyntax
                    ? RpChatSyntax.pairedEmoteDelimiters(remaining) : new BitSet();
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
                    cut = codePointBoundary(remaining, Math.min(budget, remaining.length()));
                    if (cut == 0) cut = Character.charCount(remaining.codePointAt(0));
                    part = remaining.substring(0, cut);
                }

                if (preserveRpSyntax) {
                    RpChatSyntax.State state = RpChatSyntax.scan(remaining, part.length(), emoteDelimiters);
                    close = RpChatSyntax.closers(state);
                    reopen = RpChatSyntax.openers(state);
                } else {
                    close = "";
                    reopen = "";
                }

                if (part.length() + close.length() <= contentMax
                        || budget <= 1 || fitAttempts++ >= 12) {
                    break;
                }
                budget = Math.max(1, Math.min(budget - 1, contentMax - close.length()));
            }

            // Deep nesting can reopen every consumed character. Fall back to
            // plain splitting for the rest instead of recreating the input.
            if (part.length() + close.length() > contentMax || reopen.length() >= cut) {
                preserveRpSyntax = false;
                cut = preferredCut(remaining, contentMax);
                part = remaining.substring(0, cut).stripTrailing();
                close = "";
                reopen = "";
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
        return codePointBoundary(text, safeBudget);
    }

    private static int codePointBoundary(String text, int cut) {
        if (cut > 0 && cut < text.length()
                && Character.isHighSurrogate(text.charAt(cut - 1))
                && Character.isLowSurrogate(text.charAt(cut))) {
            return cut - 1;
        }
        return cut;
    }
}
