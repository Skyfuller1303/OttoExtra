package de.ottoextra.letter;

import de.ottoextra.letter.format.LetterFormattingCodes;

import java.util.ArrayList;
import java.util.List;

/** Reine Textoperationen des Briefeditors ohne Minecraft-Abhängigkeit. */
public final class LetterTextTools {

    private static final char FIGURE_SPACE = ' ';

    private LetterTextTools() {
    }

    /** Entfernt Formatcodes (§x); alle anderen Zeichen bleiben unverändert. */
    public static String plainText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            if (isCodeAt(value, i)) {
                i++;
            } else {
                out.append(value.charAt(i));
            }
        }
        return out.toString();
    }

    /** Teilt einen Text am Cursor; ein ungültiger Cursor wird auf den Textbereich begrenzt. */
    public static String[] splitAt(String value, int cursor) {
        String text = value == null ? "" : value;
        int at = Math.max(0, Math.min(cursor, text.length()));
        return new String[]{text.substring(0, at), text.substring(at)};
    }

    /** Teilt die Seite {@code page} am Cursor und fügt den Rest als neue Folgeseite ein. */
    public static List<String> splitPage(List<String> pages, int page, int cursor) {
        List<String> out = new ArrayList<>(pages == null ? List.of() : pages);
        if (out.isEmpty()) {
            out.add("");
        }
        int index = Math.max(0, Math.min(page, out.size() - 1));
        String[] parts = splitAt(out.get(index), cursor);
        out.set(index, parts[0]);
        out.add(index + 1, parts[1]);
        return out;
    }

    /** Sichtbare Zeichen ohne Formatcodes und ohne Leerraum. */
    public static int visibleCount(String value) {
        if (value == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (isCodeAt(value, i)) {
                i++;
            } else if (!isBlank(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Ordnet die vom Server vorhergesagten Buchseitenwechsel den Editorseiten zu. Pro
     * Editorseite wird die Liste der Textoffsets geliefert, an denen eine neue Buchseite
     * beginnt (Index = Buchseitennummer - 2, daher als Paar mit der Nummer). Stimmen die
     * sichtbaren Zeichen beider Seiten nicht überein, ist keine verlässliche Zuordnung möglich
     * und das Ergebnis enthält nur leere Listen.
     */
    public static List<List<int[]>> bookBreaks(List<String> editorPages, List<String> bookPages) {
        List<String> editor = editorPages == null ? List.of() : editorPages;
        List<String> book = bookPages == null ? List.of() : bookPages;
        List<List<int[]>> result = new ArrayList<>();
        for (int i = 0; i < editor.size(); i++) {
            result.add(new ArrayList<>());
        }
        if (editor.isEmpty() || book.size() < 2) {
            return result;
        }
        int editorTotal = 0;
        for (String page : editor) {
            editorTotal += visibleCount(page);
        }
        int bookTotal = 0;
        for (String page : book) {
            bookTotal += visibleCount(page);
        }
        if (editorTotal != bookTotal) {
            return result;
        }
        int boundary = 0;
        for (int k = 1; k < book.size(); k++) {
            boundary += visibleCount(book.get(k - 1));
            if (boundary >= editorTotal) {
                break;
            }
            int running = 0;
            for (int p = 0; p < editor.size(); p++) {
                int count = visibleCount(editor.get(p));
                if (boundary < running + count) {
                    int offset = offsetOfVisible(editor.get(p), boundary - running);
                    if (offset > 0) {
                        result.get(p).add(new int[]{offset, k + 1});
                    }
                    break;
                }
                running += count;
            }
        }
        return result;
    }

    private static int offsetOfVisible(String value, int visibleIndex) {
        int seen = 0;
        for (int i = 0; i < value.length(); i++) {
            if (isCodeAt(value, i)) {
                i++;
            } else if (!isBlank(value.charAt(i))) {
                if (seen == visibleIndex) {
                    return i;
                }
                seen++;
            }
        }
        return value.length();
    }

    private static boolean isBlank(char c) {
        return Character.isWhitespace(c) || c == FIGURE_SPACE;
    }

    private static boolean isCodeAt(String value, int index) {
        return index + 1 < value.length()
                && value.charAt(index) == LetterFormattingCodes.SECTION
                && LetterFormattingCodes.isValidCode(value.charAt(index + 1));
    }
}
