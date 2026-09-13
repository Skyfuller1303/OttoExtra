package de.ottoextra.letter;

import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.send.LetterV2Planner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Hält V2-Editorseiten als abgeleitete Ansicht eines kanonischen Dokuments. */
public final class V2DraftFlow {

    public static final int DOCUMENT_VERSION = 2;
    private static final char PAGE_BREAK = '\f';

    private V2DraftFlow() {
    }

    public static void ensureDocument(LetterDraft draft) {
        draft.repair();
        if (draft.meta.flowingDocumentVersion >= DOCUMENT_VERSION
                && draft.meta.flowingDocument != null) {
            return;
        }
        draft.meta.flowingDocument = migratePages(draft.pages);
        draft.meta.flowingDocumentVersion = DOCUMENT_VERSION;
    }

    public static String migratePages(List<String> pages) {
        StringBuilder document = new StringBuilder();
        if (pages != null) {
            for (String page : pages) {
                String part = LetterV2Planner.collapseSoftLineBreaks(
                        LetterV2Planner.normalizeDocument(page));
                if (part.isEmpty()) {
                    continue;
                }
                if (!document.isEmpty() && needsBoundarySpace(document, part)) {
                    document.append(' ');
                }
                document.append(part);
            }
        }
        return document.toString();
    }

    public static EditorPosition reflow(LetterDraft draft, int page, int cursor, int anchor,
                                        String command, ToIntFunction<String> width) {
        ensureDocument(draft);
        Layout before = align(draft.meta.flowingDocument, draft.pages);
        int globalCursor = before.toGlobal(page, cursor);
        int globalAnchor = anchor < 0 ? -1 : before.toGlobal(page, anchor);
        return rebuild(draft, globalCursor, globalAnchor, command, width);
    }

    public static EditorPosition replacePage(
            LetterDraft draft, int page, String value, int cursor, int anchor,
            String command, ToIntFunction<String> width, boolean reflow) {
        ensureDocument(draft);
        Layout before = align(draft.meta.flowingDocument, draft.pages);
        PageSpan span = before.span(page);
        String previous = span.page();
        int commonPrefix = editStart(previous, value, cursor);
        int commonSuffix = commonSuffix(previous, value, commonPrefix);
        int oldChangeEnd = previous.length() - commonSuffix;
        int newChangeEnd = value.length() - commonSuffix;
        int sourceChangeStart = span.toGlobal(commonPrefix);
        int sourceChangeEnd = span.toGlobal(oldChangeEnd);
        String replacement = value.substring(commonPrefix, newChangeEnd);
        String document = draft.meta.flowingDocument;
        draft.meta.flowingDocument = document.substring(0, sourceChangeStart)
                + replacement + document.substring(sourceChangeEnd);

        int globalCursor = mapNewOffset(span, cursor, commonPrefix, oldChangeEnd,
                newChangeEnd, sourceChangeStart, sourceChangeEnd, replacement.length());
        int globalAnchor = anchor < 0 ? -1 : mapNewOffset(span, anchor, commonPrefix,
                oldChangeEnd, newChangeEnd, sourceChangeStart, sourceChangeEnd,
                replacement.length());
        if (!reflow) {
            draft.pages.set(page, value);
            return new EditorPosition(page, cursor, anchor);
        }
        return rebuild(draft, globalCursor, globalAnchor, command, width);
    }

    public static List<String> sourcePages(LetterDraft draft) {
        if (draft != null && draft.meta != null
                && draft.meta.flowingDocumentVersion >= DOCUMENT_VERSION
                && draft.meta.flowingDocument != null) {
            return List.copyOf(java.util.Arrays.asList(
                    draft.meta.flowingDocument.split(String.valueOf(PAGE_BREAK), -1)));
        }
        return draft == null || draft.pages == null ? List.of() : draft.pages;
    }

    public static boolean hasDocument(LetterDraft draft) {
        return draft != null && draft.meta != null
                && draft.meta.flowingDocumentVersion >= DOCUMENT_VERSION
                && draft.meta.flowingDocument != null;
    }

    public static EditorPosition insertPageAfter(
            LetterDraft draft, int page, String command, ToIntFunction<String> width) {
        ensureDocument(draft);
        Layout before = align(draft.meta.flowingDocument, draft.pages);
        int cut = before.span(page).sourceEnd();
        String document = draft.meta.flowingDocument;
        int breaks = cut >= document.length() || document.charAt(cut) == PAGE_BREAK ? 1 : 2;
        draft.meta.flowingDocument = document.substring(0, cut)
                + String.valueOf(PAGE_BREAK).repeat(breaks) + document.substring(cut);
        return rebuild(draft, cut + 1, -1, command, width);
    }

    public static EditorPosition deletePage(
            LetterDraft draft, int page, String command, ToIntFunction<String> width) {
        ensureDocument(draft);
        Layout before = align(draft.meta.flowingDocument, draft.pages);
        PageSpan span = before.span(page);
        String document = draft.meta.flowingDocument;
        int start = span.sourceStart();
        int end = span.sourceEnd();
        if (start > 0 && document.charAt(start - 1) == PAGE_BREAK) {
            start--;
        } else if (end < document.length() && document.charAt(end) == PAGE_BREAK) {
            end++;
        }
        draft.meta.flowingDocument = document.substring(0, start) + document.substring(end);
        return rebuild(draft, start, -1, command, width);
    }

    private static EditorPosition rebuild(LetterDraft draft, int globalCursor, int globalAnchor,
                                          String command, ToIntFunction<String> width) {
        CanonicalDocument canonical = canonicalForLayout(draft.meta.flowingDocument);
        draft.meta.flowingDocument = canonical.value();
        globalCursor = canonical.map(globalCursor);
        globalAnchor = globalAnchor < 0 ? -1 : canonical.map(globalAnchor);
        List<String> pages = new ArrayList<>(LetterV2Planner.layoutDocumentPages(command,
                sourcePages(draft), width));
        int trailingSpaces = trailingSpaces(draft.meta.flowingDocument);
        if (trailingSpaces > 0 && !pages.isEmpty()) {
            pages.set(pages.size() - 1,
                    pages.getLast() + " ".repeat(trailingSpaces));
        }
        draft.pages.clear();
        draft.pages.addAll(pages.isEmpty() ? List.of("") : pages);
        Layout after = align(draft.meta.flowingDocument, draft.pages);
        CursorPosition cursor = after.fromGlobal(globalCursor);
        int anchor = globalAnchor < 0 ? -1
                : after.fromGlobalOnPage(globalAnchor, cursor.page());
        if (anchor == cursor.offset()) {
            anchor = -1;
        }
        return new EditorPosition(cursor.page(), cursor.offset(), anchor);
    }

    /**
     * Der V2-Planer normalisiert Leerraum vor dem Umbruch. Das kanonische
     * Dokument muss dieselbe Darstellung besitzen, sonst zeigen sichtbare
     * Cursorpositionen hinter entfernten Leerzeichen auf den falschen
     * Quelloffset. Manuelle Seitenumbrüche bleiben dabei unangetastet.
     */
    private static CanonicalDocument canonicalForLayout(String raw) {
        String source = raw == null ? "" : raw;
        int trailing = trailingSpaces(source);
        String[] sourcePages = source.split(String.valueOf(PAGE_BREAK), -1);
        StringBuilder normalized = new StringBuilder(source.length());
        for (int i = 0; i < sourcePages.length; i++) {
            if (i > 0) {
                normalized.append(PAGE_BREAK);
            }
            normalized.append(LetterV2Planner.normalizeDocument(sourcePages[i]));
        }
        if (trailing > 0) {
            normalized.append(" ".repeat(trailing));
        }
        String value = normalized.toString();
        if (value.equals(source)) {
            return new CanonicalDocument(value, null);
        }

        // normalizeDocument entfernt ausschließlich Zeichen. Eine Greedy-
        // Teilfolgenabbildung liefert deshalb für jede alte Zeichengrenze den
        // dazugehörigen Offset im normalisierten Dokument.
        boolean[] kept = new boolean[source.length()];
        int sourceIndex = 0;
        for (int targetIndex = 0; targetIndex < value.length(); targetIndex++) {
            char target = value.charAt(targetIndex);
            while (sourceIndex < source.length()
                    && source.charAt(sourceIndex) != target) {
                sourceIndex++;
            }
            if (sourceIndex >= source.length()) {
                // Defensive Rückfallebene; mit der aktuellen Normalisierung
                // kann kein neues Zeichen entstehen.
                return new CanonicalDocument(source, null);
            }
            kept[sourceIndex++] = true;
        }
        int[] boundaries = new int[source.length() + 1];
        int mapped = 0;
        for (int i = 0; i < source.length(); i++) {
            boundaries[i] = mapped;
            if (kept[i]) {
                mapped++;
            }
        }
        boundaries[source.length()] = mapped;
        return new CanonicalDocument(value, boundaries);
    }

    private static Layout align(String document, List<String> pages) {
        String source = document == null ? "" : document;
        String searchable = alignmentText(source);
        List<PageSpan> spans = new ArrayList<>();
        int searchFrom = 0;
        List<String> safePages = pages == null || pages.isEmpty() ? List.of("") : pages;
        for (int i = 0; i < safePages.size(); i++) {
            String rawPage = safePages.get(i);
            String page = rawPage == null ? "" : rawPage;
            if (i > 0 && searchFrom < source.length()
                    && source.charAt(searchFrom) == PAGE_BREAK) {
                searchFrom++;
            }
            Match match = findPage(source, searchable, searchFrom, page,
                    i == safePages.size() - 1);
            spans.add(new PageSpan(page, match.start(), match.end(), match.prefixChars(), match.sourceBoundaries()));
            searchFrom = Math.max(searchFrom, match.end());
        }
        return new Layout(source, spans);
    }

    private static Match findPage(String source, String document, int searchFrom, String page,
                                  boolean lastPage) {
        String semantic = alignmentText(page);
        if (semantic.isEmpty()) {
            int start = Math.min(searchFrom, source.length());
            int nextBreak = source.indexOf(PAGE_BREAK, start);
            int end = nextBreak >= 0 ? nextBreak : lastPage ? source.length() : start;
            return new Match(start, end, 0, null);
        }
        int prefixChars = 0;
        int found = document.indexOf(semantic, Math.min(searchFrom, document.length()));
        while (found < 0 && isFormattingCodeAt(semantic, prefixChars)) {
            prefixChars += 2;
            String withoutPrefix = semantic.substring(prefixChars);
            found = document.indexOf(withoutPrefix, Math.min(searchFrom, document.length()));
        }
        if (found >= 0) {
            return new Match(found, found + semantic.length() - prefixChars, prefixChars, null);
        }

        Match wrapped = findHardWrappedPage(source, searchFrom, page);
        if (wrapped != null) return wrapped;

        int start = Math.min(searchFrom, document.length());
        while (start < document.length() && document.charAt(start) == ' ') {
            start++;
        }
        int length = Math.max(0, semantic.length() - prefixChars);
        return new Match(start, Math.min(document.length(), start + length), prefixChars, null);
    }

    private static Match findHardWrappedPage(String source, int searchFrom, String page) {
        // A wrap inside a long word adds a display newline without consuming a
        // source character. Keep an explicit offset map instead of counting it
        // as a space; otherwise all following edits and the caret drift apart.
        int start = Math.min(searchFrom, source.length());
        do {
            int prefixChars = 0;
            while (true) {
                int[] boundaries = new int[page.length() + 1];
                java.util.Arrays.fill(boundaries, 0, prefixChars + 1, start);
                int sourceOffset = start;
                boolean matches = true;
                for (int local = prefixChars; local < page.length(); local++) {
                    char shown = page.charAt(local);
                    if (sourceOffset < source.length() && (shown == source.charAt(sourceOffset)
                            || shown == '\n' && source.charAt(sourceOffset) == ' ')) {
                        sourceOffset++;
                    } else if (shown != '\n') {
                        matches = false;
                        break;
                    }
                    boundaries[local + 1] = sourceOffset;
                }
                if (matches) return new Match(start, sourceOffset, prefixChars, boundaries);
                if (!isFormattingCodeAt(page, prefixChars)) break;
                prefixChars += 2;
            }
            if (start >= source.length() || source.charAt(start) != ' ' && source.charAt(start) != '\n') break;
            start++;
        } while (true);
        return null;
    }

    private static int alignedPrefixLength(String value, int rawOffset) {
        int end = Math.max(0, Math.min(rawOffset, value.length()));
        return alignmentText(value.substring(0, end)).length();
    }

    private static String alignmentText(String value) {
        return (value == null ? "" : value).replace('\n', ' ').replace(PAGE_BREAK, ' ');
    }

    private static int commonPrefix(String before, String after) {
        int limit = Math.min(before.length(), after.length());
        int i = 0;
        while (i < limit && before.charAt(i) == after.charAt(i)) {
            i++;
        }
        return i;
    }

    private static int editStart(String before, String after, int cursor) {
        int delta = after.length() - before.length();
        int start = cursor - Math.max(0, delta);
        int oldEnd = start + Math.max(0, -delta);
        int newEnd = start + Math.max(0, delta);
        // An entered newline can equal the existing soft wrap beside it. For
        // an exact insertion/deletion, the caret identifies which one changed.
        if (delta != 0 && start >= 0 && oldEnd <= before.length() && newEnd <= after.length()
                && before.regionMatches(0, after, 0, start)
                && before.substring(oldEnd).equals(after.substring(newEnd))) {
            return start;
        }
        return commonPrefix(before, after);
    }

    private static int commonSuffix(String before, String after, int prefix) {
        int limit = Math.min(before.length(), after.length()) - prefix;
        int i = 0;
        while (i < limit && before.charAt(before.length() - 1 - i)
                == after.charAt(after.length() - 1 - i)) {
            i++;
        }
        return i;
    }

    private static int mapNewOffset(PageSpan span, int localOffset, int commonPrefix,
                                    int oldChangeEnd, int newChangeEnd,
                                    int sourceChangeStart, int sourceChangeEnd,
                                    int replacementLength) {
        int local = Math.max(0, Math.min(localOffset,
                commonPrefix + (newChangeEnd - commonPrefix)
                        + (span.page().length() - oldChangeEnd)));
        if (local <= commonPrefix) {
            return span.toGlobal(local);
        }
        if (local >= newChangeEnd) {
            int oldLocal = oldChangeEnd + local - newChangeEnd;
            int delta = replacementLength - (sourceChangeEnd - sourceChangeStart);
            return span.toGlobal(oldLocal) + delta;
        }
        return sourceChangeStart + Math.min(replacementLength, local - commonPrefix);
    }

    private static boolean needsBoundarySpace(StringBuilder left, String right) {
        char a = left.charAt(left.length() - 1);
        char b = right.charAt(0);
        return !Character.isWhitespace(a) && !Character.isWhitespace(b);
    }

    private static int trailingSpaces(String value) {
        int count = 0;
        for (int i = value == null ? -1 : value.length() - 1;
             i >= 0 && value.charAt(i) == ' '; i--) {
            count++;
        }
        return count;
    }

    private static boolean isFormattingCodeAt(String value, int index) {
        return index >= 0 && index + 1 < value.length()
                && value.charAt(index) == LetterFormattingCodes.SECTION
                && LetterFormattingCodes.isValidCode(value.charAt(index + 1));
    }

    public record EditorPosition(int page, int cursor, int anchor) {
    }

    private record CursorPosition(int page, int offset) {
    }

    private record CanonicalDocument(String value, int[] sourceBoundaries) {
        int map(int sourceOffset) {
            if (sourceBoundaries == null) {
                return Math.max(0, Math.min(sourceOffset, value.length()));
            }
            int safe = Math.max(0, Math.min(sourceOffset,
                    sourceBoundaries.length - 1));
            return sourceBoundaries[safe];
        }
    }

    private record Match(int start, int end, int prefixChars, int[] sourceBoundaries) {
    }

    private record PageSpan(String page, int sourceStart, int sourceEnd,
                            int injectedPrefixChars, int[] sourceBoundaries) {
        int toGlobal(int localOffset) {
            if (sourceBoundaries != null) {
                return sourceBoundaries[Math.max(0, Math.min(localOffset, page.length()))];
            }
            int prefix = Math.min(injectedPrefixChars, page.length());
            int local = Math.max(prefix, Math.min(localOffset, page.length()));
            return Math.min(sourceEnd, sourceStart
                    + alignedPrefixLength(page.substring(prefix), local - prefix));
        }

        int fromGlobal(int globalOffset) {
            int target = Math.max(sourceStart, Math.min(globalOffset, sourceEnd));
            int best = Math.min(injectedPrefixChars, page.length());
            for (int local = best; local <= page.length(); local++) {
                int mapped = toGlobal(local);
                if (mapped > target) {
                    break;
                }
                best = local;
            }
            return best;
        }
    }

    private record Layout(String document, List<PageSpan> spans) {
        PageSpan span(int page) {
            return spans.get(Math.max(0, Math.min(page, spans.size() - 1)));
        }

        int toGlobal(int page, int localOffset) {
            return span(page).toGlobal(localOffset);
        }

        CursorPosition fromGlobal(int globalOffset) {
            int target = Math.max(0, Math.min(globalOffset, document.length()));
            for (int i = 0; i < spans.size(); i++) {
                PageSpan span = spans.get(i);
                if (target <= span.sourceEnd() || i == spans.size() - 1) {
                    return new CursorPosition(i, span.fromGlobal(target));
                }
            }
            PageSpan last = spans.getLast();
            return new CursorPosition(spans.size() - 1, last.page().length());
        }

        int fromGlobalOnPage(int globalOffset, int preferredPage) {
            PageSpan span = span(preferredPage);
            if (globalOffset >= span.sourceStart() && globalOffset <= span.sourceEnd()) {
                return span.fromGlobal(globalOffset);
            }
            return -1;
        }
    }
}
