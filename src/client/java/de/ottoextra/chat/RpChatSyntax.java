package de.ottoextra.chat;
public final class RpChatSyntax {
    public enum Kind {
        NORMAL,
        EMOTE,
        OOC
    }
    public record State(boolean emote, int oocDepth) {
        public State {
            oocDepth = Math.max(0, oocDepth);
        }
        public static State normal() {
            return new State(false, 0);
        }
    }
    public record Step(Kind kind, State after) {
    }
    private RpChatSyntax() {
    }
    public static Step step(State before, char c) {
        State state = before == null ? State.normal() : before;
        if (state.oocDepth() > 0) {
            int depth = state.oocDepth();
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            return new Step(Kind.OOC, new State(state.emote(), depth));
        }
        if (c == '(') {
            return new Step(Kind.OOC, new State(state.emote(), 1));
        }
        if (c == '*') {
            if (state.emote()) {
                return new Step(Kind.EMOTE, new State(false, 0));
            }
            return new Step(Kind.EMOTE, new State(true, 0));
        }
        return new Step(state.emote() ? Kind.EMOTE : Kind.NORMAL, state);
    }
    public static State scan(String text, State initial) {
        State state = initial == null ? State.normal() : initial;
        if (text == null || text.isEmpty()) {
            return state;
        }
        for (int i = 0; i < text.length(); i++) {
            state = step(state, text.charAt(i)).after();
        }
        return state;
    }
    public static State scan(String text) {
        return scan(text, State.normal());
    }
    public static String closers(State state) {
        State safe = state == null ? State.normal() : state;
        StringBuilder out = new StringBuilder(safe.oocDepth() + (safe.emote() ? 1 : 0));
        out.append(")".repeat(safe.oocDepth()));
        if (safe.emote()) {
            out.append('*');
        }
        return out.toString();
    }
    public static String openers(State state) {
        State safe = state == null ? State.normal() : state;
        StringBuilder out = new StringBuilder(safe.oocDepth() + (safe.emote() ? 1 : 0));
        if (safe.emote()) {
            out.append('*');
        }
        out.append("(".repeat(safe.oocDepth()));
        return out.toString();
    }
}
