package de.ottoextra.letter.placeholder;
import de.ottoextra.letter.model.LetterPlaceholder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public final class LetterPlaceholderParser {
    public static final Pattern PATTERN =
            Pattern.compile("\\{\\{\\s*(name|title|full|mc)\\s*:\\s*([^}]+?)\\s*}}",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OPEN_PATTERN = Pattern.compile("\\{\\{");
    private LetterPlaceholderParser() {
    }
    public static List<LetterPlaceholder> parse(String text) {
        List<LetterPlaceholder> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        Matcher m = PATTERN.matcher(text);
        while (m.find()) {
            out.add(new LetterPlaceholder(m.group(), m.group(1).toLowerCase(Locale.ROOT),
                    m.group(2), m.start(), m.end()));
        }
        return out;
    }
    public static LetterPlaceholder at(String text, int cursor) {
        for (LetterPlaceholder p : parse(text)) {
            if (cursor >= p.start() && cursor <= p.end()) {
                return p;
            }
        }
        return null;
    }
    public static LetterPlaceholder next(String text, int cursor) {
        List<LetterPlaceholder> all = parse(text);
        for (LetterPlaceholder p : all) {
            if (p.start() > cursor) {
                return p;
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }
    public static LetterPlaceholder previous(String text, int cursor) {
        List<LetterPlaceholder> all = parse(text);
        LetterPlaceholder best = null;
        for (LetterPlaceholder p : all) {
            if (p.end() < cursor) {
                best = p;
            }
        }
        if (best != null) {
            return best;
        }
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }
    public static int countInvalidOpenings(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        List<LetterPlaceholder> valid = parse(text);
        Matcher m = OPEN_PATTERN.matcher(text);
        int invalid = 0;
        outer:
        while (m.find()) {
            for (LetterPlaceholder p : valid) {
                if (p.start() == m.start()) {
                    continue outer;
                }
            }
            invalid++;
        }
        return invalid;
    }
}
