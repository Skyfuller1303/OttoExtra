package de.ottoextra.crafting;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure parsing of the server's crafting messages: the 10 step progress action bar, the
 * "Du beginnst die Herstellung von ..." chat line and the completion action bar.
 */
public final class CraftingParser {

    static final char BLOCK = '■';
    private static final int GOLD = Formatting.GOLD.getColorValue();
    private static final int DARK_GRAY = Formatting.DARK_GRAY.getColorValue();

    private static final Pattern START = Pattern.compile(
            "^(?:\\[Info]\\s*)?Du beginnst die Herstellung von ([^.…\\s].*?)\\s*(?:\\.{2,}|…)\\s*$");
    private static final String COMPLETION = "Die Herstellung ist erfolgreich";
    /**
     * The whole completion action bar ("§7Die Herstellung ist erfolgreich!"), optionally with the
     * server's "[Info]" prefix. Anchored, so a chat line that merely quotes the sentence does not count.
     */
    private static final Pattern COMPLETION_LINE = Pattern.compile(
            "^(?:\\[Info]\\s*)?Die Herstellung ist erfolgreich\\s*[.!…]*$");
    /** Failed craft result ("§7Die Herstellung ist schiefgegangen..."), whole line, compared in lower case. */
    private static final Pattern CRAFT_FAILED = Pattern.compile(
            "^(?:\\[info]\\s*)?die herstellung ist (?:schiefgegangen|schief gegangen|fehlgeschlagen|misslungen)\\s*[.!…]*$");
    /** Failure messages are unknown: "Herstellung" plus one of these words (case-insensitive) counts. */
    static final String FAILURE_SUBJECT = "herstellung";
    static final java.util.List<String> FAILURE_KEYWORDS = java.util.List.of(
            "fehlgeschlagen", "abgebrochen", "misslungen", "schiefgegangen", "nicht", "kein");

    private CraftingParser() {
    }

    /** Filled and total step count of a progress bar message. */
    public record Progress(int done, int total) {
        public float fraction() {
            return total <= 0 ? 0.0f : Math.min(1.0f, (float) done / total);
        }
    }

    /**
     * Reads a progress bar: the text must consist only of block characters, gold ones count as done and
     * dark gray ones as remaining. Styles are inherited through siblings, so any splitting works.
     */
    public static Optional<Progress> progress(Text text) {
        if (text == null) return Optional.empty();
        int[] done = {0};
        int[] total = {0};
        boolean[] valid = {true};
        text.visit((Style style, String content) -> {
            if (!valid[0] || content.isEmpty()) return Optional.empty();
            TextColor color = style.getColor();
            int rgb = color == null ? -1 : color.getRgb();
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) != BLOCK || (rgb != GOLD && rgb != DARK_GRAY)) {
                    valid[0] = false;
                    return Optional.empty();
                }
                total[0]++;
                if (rgb == GOLD) done[0]++;
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (!valid[0] || total[0] == 0) return Optional.empty();
        return Optional.of(new Progress(done[0], total[0]));
    }

    /** Item name from "[Info] Du beginnst die Herstellung von Leine...". */
    public static Optional<String> itemName(String chat) {
        if (chat == null) return Optional.empty();
        Matcher m = START.matcher(strip(chat));
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    /** True for the completion action bar ("§7Die Herstellung ist erfolgreich!"), matched as a whole line. */
    public static boolean isCompletion(String message) {
        return message != null && COMPLETION_LINE.matcher(strip(message)).matches();
    }

    /**
     * The server's result line for a craft that ran and failed ("§7Die Herstellung ist
     * schiefgegangen..."). Formatting codes are stripped, case does not matter; "fehlgeschlagen" and
     * "misslungen" count too. Unlike {@link #isFailure} this is a result, not a refusal.
     */
    public static boolean isCraftFailed(String message) {
        if (message == null || isCompletion(message)) return false;
        return CRAFT_FAILED.matcher(strip(message).toLowerCase(java.util.Locale.ROOT)).matches();
    }

    /**
     * Guess for a failed or aborted craft. Success always wins, and the start line
     * ("Du beginnst die Herstellung von ...") is never a failure, even if the item name contains a keyword.
     */
    public static boolean isFailure(String message) {
        if (message == null || strip(message).contains(COMPLETION) || itemName(message).isPresent()) return false;
        String lower = strip(message).toLowerCase(java.util.Locale.ROOT);
        if (!lower.contains(FAILURE_SUBJECT)) return false;
        for (String keyword : FAILURE_KEYWORDS) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private static String strip(String s) {
        String stripped = Formatting.strip(s);
        return (stripped == null ? s : stripped).trim();
    }
}
