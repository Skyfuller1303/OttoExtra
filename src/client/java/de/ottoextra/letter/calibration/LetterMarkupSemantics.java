package de.ottoextra.letter.calibration;

import de.ottoextra.letter.format.LetterFormattingCodes;

import java.util.ArrayList;
import java.util.List;

public final class LetterMarkupSemantics {

    public record Run(String text, Character color, boolean bold, boolean italic,
                      boolean underlined, boolean strikethrough, boolean obfuscated) {
    }

    private LetterMarkupSemantics() {
    }

    public static List<Run> runs(String markup) {
        String input = markup == null ? "" : markup;
        List<Run> out = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        State state = new State();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == LetterFormattingCodes.SECTION && i + 1 < input.length()
                    && LetterFormattingCodes.isValidCode(input.charAt(i + 1))) {
                flush(out, text, state);
                state.apply(Character.toLowerCase(input.charAt(++i)));
            } else {
                text.append(ch);
            }
        }
        flush(out, text, state);
        return List.copyOf(out);
    }

    public static String plain(String markup) {
        StringBuilder out = new StringBuilder();
        for (Run run : runs(markup)) {
            out.append(run.text());
        }
        return out.toString();
    }

    private static void flush(List<Run> out, StringBuilder text, State state) {
        if (text.isEmpty()) {
            return;
        }
        Run next = new Run(text.toString(), state.color, state.bold, state.italic,
                state.underlined, state.strikethrough, state.obfuscated);
        if (!out.isEmpty() && sameStyle(out.getLast(), next)) {
            Run previous = out.removeLast();
            next = new Run(previous.text() + next.text(), next.color(), next.bold(),
                    next.italic(), next.underlined(), next.strikethrough(), next.obfuscated());
        }
        out.add(next);
        text.setLength(0);
    }

    private static boolean sameStyle(Run left, Run right) {
        return java.util.Objects.equals(left.color(), right.color())
                && left.bold() == right.bold()
                && left.italic() == right.italic()
                && left.underlined() == right.underlined()
                && left.strikethrough() == right.strikethrough()
                && left.obfuscated() == right.obfuscated();
    }

    private static final class State {
        Character color;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;

        void apply(char code) {
            if (LetterFormattingCodes.isColor(code)) {
                reset();
                color = code;
                return;
            }
            switch (code) {
                case 'l' -> bold = true;
                case 'o' -> italic = true;
                case 'n' -> underlined = true;
                case 'm' -> strikethrough = true;
                case 'k' -> obfuscated = true;
                case 'r' -> reset();
                default -> { }
            }
        }

        void reset() {
            color = null;
            bold = italic = underlined = strikethrough = obfuscated = false;
        }
    }
}
