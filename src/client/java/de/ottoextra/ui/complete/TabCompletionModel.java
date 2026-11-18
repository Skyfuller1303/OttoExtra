package de.ottoextra.ui.complete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure matching and cycling logic behind {@link TabCompletion}, free of Minecraft classes.
 *
 * <p>The whole field is completed first; only when nothing matches the whole text and it contains a
 * space, the last word is completed instead. Matches are case-insensitive: prefix matches first,
 * then substring matches, each group in alphabetical order. The first Tab applies the first match,
 * every further Tab cycles (Shift+Tab backwards). As soon as the text differs from what the last Tab
 * put in (typing, deleting, pasting), the cycle starts over from the new text.
 */
public final class TabCompletionModel {
    /** Number of matches shown at once in the suggestion line. */
    public static final int MAX_SHOWN = 5;

    /** Matches to show: a window of the full match list, {@code selected} is -1 while just typing. */
    public record Shown(List<String> items, int selected, int total) {
        public static final Shown NONE = new Shown(List.of(), -1, 0);

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }

    private record Query(String keep, List<String> matches, String typed) {
    }

    private String base;
    private String keep = "";
    private List<String> matches = List.of();
    private int index = -1;
    private String applied;

    /**
     * Candidates matching {@code query}: case-insensitive prefix matches, then substring matches.
     * Blank candidates and case-insensitive duplicates are dropped; a blank query matches nothing.
     */
    public static List<String> matches(String query, Collection<String> candidates) {
        if (query == null || candidates == null) {
            return List.of();
        }
        String q = query.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String c : candidates) {
            if (c == null) {
                continue;
            }
            String name = c.strip();
            if (!name.isEmpty()) {
                unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
            }
        }
        List<String> prefix = new ArrayList<>();
        List<String> inner = new ArrayList<>();
        for (Map.Entry<String, String> e : unique.entrySet()) {
            if (e.getKey().startsWith(q)) {
                prefix.add(e.getValue());
            } else if (e.getKey().contains(q)) {
                inner.add(e.getValue());
            }
        }
        Comparator<String> order = Comparator.comparing((String s) -> s.toLowerCase(Locale.ROOT))
                .thenComparing(Comparator.naturalOrder());
        prefix.sort(order);
        inner.sort(order);
        prefix.addAll(inner);
        return prefix;
    }

    /**
     * Handles a Tab press on a field holding {@code text}. Returns the new field text, or null if
     * there is nothing to complete; the caller then lets Tab change the focus as usual.
     */
    public String cycle(String text, Collection<String> candidates, boolean backward) {
        String current = text == null ? "" : text;
        if (cycling(current)) {
            int n = matches.size();
            index = Math.floorMod(index + (backward ? -1 : 1), n);
            applied = keep + matches.get(index);
            return applied;
        }
        reset();
        Query q = query(current, candidates);
        if (q.matches().isEmpty()) {
            return null;
        }
        int n = q.matches().size();
        int start = backward ? n - 1 : 0;
        String first = q.keep() + q.matches().get(start);
        if (first.equals(current)) {
            if (n == 1) {
                // Already complete: let Tab move the focus on.
                return null;
            }
            start = Math.floorMod(start + (backward ? -1 : 1), n);
        }
        base = current;
        keep = q.keep();
        matches = q.matches();
        index = start;
        applied = keep + matches.get(index);
        return applied;
    }

    /**
     * Tells the model what the field actually holds after applying a completion (for example cut by
     * the field's maximum length), so the next Tab still continues the cycle.
     */
    public void applied(String actual) {
        if (applied != null && actual != null) {
            applied = actual;
        }
    }

    /** True while the field still holds exactly what the last Tab put in. */
    public boolean cycling(String text) {
        return applied != null && applied.equals(text) && !matches.isEmpty();
    }

    /** Ends a cycle and returns the text typed before the first Tab, or null if not cycling. */
    public String cancel(String text) {
        if (!cycling(text)) {
            return null;
        }
        String typed = base;
        reset();
        return typed;
    }

    public void reset() {
        base = null;
        keep = "";
        matches = List.of();
        index = -1;
        applied = null;
    }

    /**
     * The matches for the suggestion line: while cycling a window around the selected match, else the
     * live matches of the typed text. Nothing is shown when the only match is already in the field.
     */
    public Shown shown(String text, Collection<String> candidates) {
        String current = text == null ? "" : text;
        if (cycling(current)) {
            return window(matches, index);
        }
        Query q = query(current, candidates);
        if (q.matches().isEmpty()
                || q.matches().size() == 1 && q.matches().get(0).equalsIgnoreCase(q.typed().strip())) {
            return Shown.NONE;
        }
        return window(q.matches(), -1);
    }

    private static Shown window(List<String> all, int selected) {
        int n = all.size();
        int start = selected < 0 ? 0 : Math.max(0, Math.min(selected - MAX_SHOWN / 2, n - MAX_SHOWN));
        int end = Math.min(n, start + MAX_SHOWN);
        return new Shown(List.copyOf(all.subList(start, end)), selected < 0 ? -1 : selected - start, n);
    }

    /** Whole text first; if nothing matches and the text has a space, only its last word. */
    private static Query query(String text, Collection<String> candidates) {
        String whole = text.stripLeading();
        List<String> found = matches(whole, candidates);
        if (!found.isEmpty()) {
            return new Query(text.substring(0, text.length() - whole.length()), found, whole);
        }
        int space = text.lastIndexOf(' ');
        if (space >= 0 && space < text.length() - 1) {
            String word = text.substring(space + 1);
            return new Query(text.substring(0, space + 1), matches(word, candidates), word);
        }
        return new Query("", List.of(), whole);
    }
}
