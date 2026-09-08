package de.ottoextra.letter.ui;

import de.ottoextra.letter.format.LetterFormattingCodes;

/** Kleine, formatcode-sichere Textoperationen für den Briefeditor. */
final class LetterEditorTextOperations {

    private LetterEditorTextOperations() {
    }

    static boolean editorShortcut(boolean macOs, boolean control, boolean command) {
        return control || macOs && command;
    }

    static boolean wordShortcut(boolean macOs, boolean control, boolean command,
                                boolean option) {
        return control || macOs && (command || option);
    }

    static Range previousWord(String value, int cursor, int minimum) {
        String text = value == null ? "" : value;
        int lower = Math.max(0, Math.min(minimum, text.length()));
        int i = Math.max(lower, Math.min(cursor, text.length()));
        int deleteEnd = i;

        while (i > lower) {
            if (formattingCodeEndsAt(text, i)) {
                i -= 2;
                deleteEnd = i;
                continue;
            }
            int previous = previousCodePoint(text, i, lower);
            if (isWord(text, previous)) {
                break;
            }
            i = previous;
        }

        boolean foundWord = false;
        while (i > lower) {
            if (formattingCodeEndsAt(text, i)) {
                int codeStart = i - 2;
                int previous = previousCodePoint(text, codeStart, lower);
                if (foundWord && previous < codeStart && isWord(text, previous)) {
                    i = codeStart;
                    continue;
                }
                break;
            }
            int previous = previousCodePoint(text, i, lower);
            if (!isWord(text, previous)) {
                break;
            }
            foundWord = true;
            i = previous;
        }
        return foundWord ? new Range(i, deleteEnd) : new Range(cursor, cursor);
    }

    static Range nextWord(String value, int cursor) {
        String text = value == null ? "" : value;
        int origin = Math.max(0, Math.min(cursor, text.length()));
        int i = origin;
        int deleteStart = origin;

        while (i < text.length()) {
            if (isFormattingCodeAt(text, i)) {
                i += 2;
                deleteStart = i;
                continue;
            }
            if (isWord(text, i)) {
                break;
            }
            i = nextCodePoint(text, i);
        }

        boolean foundWord = false;
        while (i < text.length()) {
            if (isFormattingCodeAt(text, i)) {
                int afterCode = i + 2;
                if (foundWord && afterCode < text.length() && isWord(text, afterCode)) {
                    i = afterCode;
                    continue;
                }
                break;
            }
            if (!isWord(text, i)) {
                break;
            }
            foundWord = true;
            i = nextCodePoint(text, i);
        }
        return foundWord ? new Range(deleteStart, i) : new Range(origin, origin);
    }

    static int previousCharacterStart(String value, int cursor) {
        String text = value == null ? "" : value;
        int end = Math.max(0, Math.min(cursor, text.length()));
        if (formattingCodeEndsAt(text, end)) {
            return end - 2;
        }
        return previousCodePoint(text, end, 0);
    }

    static int nextCharacterEnd(String value, int cursor) {
        String text = value == null ? "" : value;
        int start = Math.max(0, Math.min(cursor, text.length()));
        if (isFormattingCodeAt(text, start)) {
            return start + 2;
        }
        return nextCodePoint(text, start);
    }

    static int cursorBoundary(String value, int proposed, int direction) {
        String text = value == null ? "" : value;
        int target = Math.max(0, Math.min(proposed, text.length()));
        if (target > 0 && isFormattingCodeAt(text, target - 1)) {
            return direction < 0 ? target - 1 : Math.min(text.length(), target + 1);
        }
        if (target > 0 && target < text.length()
                && Character.isHighSurrogate(text.charAt(target - 1))
                && Character.isLowSurrogate(text.charAt(target))) {
            return direction < 0 ? target - 1 : target + 1;
        }
        return target;
    }

    static int editableCursor(String value, int proposed, int minimum, int direction) {
        String text = value == null ? "" : value;
        int lower = cursorBoundary(text, Math.max(0, Math.min(minimum, text.length())), 1);
        return Math.max(lower, cursorBoundary(text, proposed, direction));
    }

    static boolean preservesLockedPrefix(String before, String after, int minimum) {
        int locked = Math.max(0, Math.min(minimum, before.length()));
        return after.length() >= locked && before.regionMatches(0, after, 0, locked);
    }

    static int firstVisibleLine(int current, int cursorLine, int total, int capacity,
                                boolean followCursor) {
        int maximum = Math.max(0, total - Math.max(1, capacity));
        int first = Math.max(0, Math.min(current, maximum));
        if (followCursor) {
            if (cursorLine < first) first = cursorLine;
            if (cursorLine >= first + capacity) first = cursorLine - capacity + 1;
        }
        return Math.max(0, Math.min(first, maximum));
    }

    private static int previousCodePoint(String text, int index, int minimum) {
        if (index <= minimum) {
            return minimum;
        }
        int previous = index - 1;
        if (previous > minimum && Character.isLowSurrogate(text.charAt(previous))
                && Character.isHighSurrogate(text.charAt(previous - 1))) {
            previous--;
        }
        return previous;
    }

    private static int nextCodePoint(String text, int index) {
        if (index >= text.length()) {
            return text.length();
        }
        int next = index + 1;
        if (next < text.length() && Character.isHighSurrogate(text.charAt(index))
                && Character.isLowSurrogate(text.charAt(next))) {
            next++;
        }
        return next;
    }

    private static boolean isWord(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return false;
        }
        int codePoint = text.codePointAt(index);
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }

    private static boolean formattingCodeEndsAt(String value, int index) {
        return index >= 2 && isFormattingCodeAt(value, index - 2);
    }

    private static boolean isFormattingCodeAt(String value, int index) {
        return index >= 0 && index + 1 < value.length()
                && value.charAt(index) == LetterFormattingCodes.SECTION
                && LetterFormattingCodes.isValidCode(value.charAt(index + 1));
    }

    record Range(int start, int end) {
        boolean isEmpty() {
            return start >= end;
        }
    }
}
