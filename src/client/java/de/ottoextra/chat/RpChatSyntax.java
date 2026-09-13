package de.ottoextra.chat;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/** Gemeinsame, verschachtelbare Zustandsmaschine für RP-Chat-Syntax. */
public final class RpChatSyntax {

    public enum Kind {
        NORMAL,
        EMOTE,
        OOC
    }

    /** Zuletzt geöffneter Bereich bestimmt den sichtbaren Stil. */
    public record State(List<Kind> stack) {
        public State {
            stack = stack == null ? List.of() : stack.stream()
                    .filter(kind -> kind == Kind.EMOTE || kind == Kind.OOC)
                    .toList();
        }

        /** Rückwärtskompatibler Zustand ohne bekannte Verschachtelungsreihenfolge. */
        public State(boolean emote, int oocDepth) {
            this(legacyStack(emote, oocDepth));
        }

        public static State normal() {
            return new State(List.of());
        }

        public boolean emote() {
            return stack.contains(Kind.EMOTE);
        }

        public int oocDepth() {
            return (int) stack.stream().filter(kind -> kind == Kind.OOC).count();
        }

        Kind currentKind() {
            return stack.isEmpty() ? Kind.NORMAL : stack.getLast();
        }

        private static List<Kind> legacyStack(boolean emote, int oocDepth) {
            List<Kind> stack = new ArrayList<>();
            if (emote) {
                stack.add(Kind.EMOTE);
            }
            for (int index = 0; index < Math.max(0, oocDepth); index++) {
                stack.add(Kind.OOC);
            }
            return stack;
        }
    }

    public record Step(Kind kind, State after) {
    }

    private RpChatSyntax() {
    }

    /** Bestimmt Stil und Folgezustand für genau ein sichtbares Zeichen. */
    public static Step step(State before, char c) {
        State state = before == null ? State.normal() : before;
        List<Kind> stack = new ArrayList<>(state.stack());
        Kind current = state.currentKind();

        if (c == '*') {
            if (current == Kind.EMOTE) {
                stack.removeLast();
                return new Step(Kind.EMOTE, new State(stack));
            }
            if (!stack.contains(Kind.EMOTE)) {
                stack.add(Kind.EMOTE);
                return new Step(Kind.EMOTE, new State(stack));
            }
            // Ungültig überkreuzte Markierung bleibt Teil des inneren Bereichs.
            return new Step(current, state);
        }

        if (c == '(') {
            stack.add(Kind.OOC);
            return new Step(Kind.OOC, new State(stack));
        }

        if (c == ')' && current == Kind.OOC) {
            stack.removeLast();
            return new Step(Kind.OOC, new State(stack));
        }

        return new Step(current, state);
    }

    /** Einzelne Sternchen sind Text; nur vorab bestätigte Paare ändern den Zustand. */
    static Step step(State before, char c, boolean pairedEmoteDelimiter) {
        State state = before == null ? State.normal() : before;
        if (c == '*' && !pairedEmoteDelimiter) {
            return new Step(state.currentKind(), state);
        }
        return step(state, c);
    }

    /** Findet vollständig geschlossene Emotes in der gesamten Nachricht. */
    static BitSet pairedEmoteDelimiters(String text) {
        BitSet delimiters = new BitSet();
        int[] closingStars = closingStars(text);
        State state = State.normal();
        int opening = -1;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == '§' && index + 1 < text.length()) {
                index++;
                continue;
            }
            boolean delimiter = state.emote()
                    ? isClosingEmoteDelimiter(text, index)
                    : closingStars[index] >= 0;
            State after = step(state, c, delimiter).after();
            if (!state.emote() && after.emote()) {
                opening = index;
            } else if (state.emote() && !after.emote()) {
                delimiters.set(opening);
                delimiters.set(index);
                opening = -1;
            }
            state = after;
        }
        return delimiters;
    }

    /** Look ahead in linear time; stars inside nested OOC cannot close an outer emote. */
    private static int[] closingStars(String text) {
        int[] closing = new int[text.length()];
        Arrays.fill(closing, -1);
        BitSet formattingCodes = new BitSet();
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '§') {
                formattingCodes.set(i, i + 2);
                i++;
            }
        }
        var afterParenthesis = new ArrayDeque<Integer>();
        int nextStar = -1;
        for (int i = text.length() - 1; i >= 0; i--) {
            if (formattingCodes.get(i)) continue;
            switch (text.charAt(i)) {
                case '*' -> {
                    if (isOpeningEmoteDelimiter(text, i)) {
                        closing[i] = nextStar;
                    }
                    if (isClosingEmoteDelimiter(text, i)) {
                        nextStar = i;
                    }
                }
                case ')' -> afterParenthesis.push(nextStar);
                case '(' -> nextStar = afterParenthesis.isEmpty() ? -1 : afterParenthesis.pop();
                default -> { }
            }
        }
        return closing;
    }

    /** Öffner stehen nicht direkt hinter einem Wort und benötigen sichtbaren Inhalt. */
    private static boolean isOpeningEmoteDelimiter(String text, int index) {
        int next = nextVisibleIndex(text, index);
        if (next < 0 || Character.isWhitespace(text.charAt(next))
                || text.charAt(next) == '*') {
            return false;
        }
        int previous = previousVisibleIndex(text, index);
        return previous < 0 || !isWordCharacter(text.charAt(previous));
    }

    /** Schließer folgen sichtbarem Inhalt und stehen nicht mitten in einem Wort. */
    private static boolean isClosingEmoteDelimiter(String text, int index) {
        int previous = previousVisibleIndex(text, index);
        if (previous < 0 || Character.isWhitespace(text.charAt(previous))
                || text.charAt(previous) == '*') {
            return false;
        }
        int next = nextVisibleIndex(text, index);
        return next < 0 || !isWordCharacter(text.charAt(next));
    }

    private static boolean isWordCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int previousVisibleIndex(String text, int index) {
        int cursor = index - 1;
        while (cursor >= 1 && text.charAt(cursor - 1) == '§') {
            cursor -= 2;
        }
        return cursor;
    }

    private static int nextVisibleIndex(String text, int index) {
        int cursor = index + 1;
        while (cursor + 1 < text.length() && text.charAt(cursor) == '§') {
            cursor += 2;
        }
        return cursor < text.length() ? cursor : -1;
    }

    /** Liest einen Präfix, dessen Emote-Paare anhand der ganzen Nachricht feststehen. */
    static State scan(String text, int end, BitSet pairedEmoteDelimiters) {
        State state = State.normal();
        for (int index = 0; index < end; index++) {
            char c = text.charAt(index);
            if (c == '§' && index + 1 < end) {
                index++;
                continue;
            }
            state = step(state, c, pairedEmoteDelimiters.get(index)).after();
        }
        return state;
    }

    /** Liest einen Text ab dem angegebenen Zustand vollständig ein. */
    public static State scan(String text, State initial) {
        State state = initial == null ? State.normal() : initial;
        if (text == null || text.isEmpty()) {
            return state;
        }
        for (int index = 0; index < text.length(); index++) {
            state = step(state, text.charAt(index)).after();
        }
        return state;
    }

    public static State scan(String text) {
        return scan(text, State.normal());
    }

    /** Schließt offene Bereiche für eine eigenständige Teilnachricht. */
    public static String closers(State state) {
        State safe = state == null ? State.normal() : state;
        StringBuilder out = new StringBuilder(safe.stack().size());
        for (int index = safe.stack().size() - 1; index >= 0; index--) {
            out.append(safe.stack().get(index) == Kind.EMOTE ? '*' : ')');
        }
        return out.toString();
    }

    /** Öffnet denselben Verschachtelungszustand in der Folgemeldung. */
    public static String openers(State state) {
        State safe = state == null ? State.normal() : state;
        StringBuilder out = new StringBuilder(safe.stack().size());
        for (Kind kind : safe.stack()) {
            out.append(kind == Kind.EMOTE ? '*' : '(');
        }
        return out.toString();
    }
}
