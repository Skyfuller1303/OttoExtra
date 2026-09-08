package de.ottoextra.letter.send;

import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.paste.PageSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Plant Befehle gegen Courier 2.1.0; dessen Simulation bestimmt die Buchvorschau. */
public final class LetterV2Planner {

    public static final String EMPTY_LINE = "\u2007";
    /** Historischer Kalibrierwert, wird nicht mehr als Seitengrenze gesendet. */
    public static final String BOUNDARY_PADDING = "§r";
    public static final String LITERAL_NEWLINE_WARNING = "LITERAL_BACKSLASH_N";
    public static final String VISIBLE_PADDING_WARNING = "VISIBLE_BOUNDARY_PADDING";
    public static final String PAGE_CONTENT_WARNING = "PAGE_CONTENT_UNSENDABLE";
    public static final int BOOK_PAGE_WIDTH = 108;
    public static final int BOOK_MAX_LINES = 14;
    public static final int SAFE_CHAT_MESSAGE_LIMIT = 256;

    /** Historische lokale Editorbudgets; keine Regeln der Courier-2.1.0-Pagination. */
    public static final int SERVER_PAGE_MAX_LINES = 10;
    public static final int SERVER_PAGE_EFFECTIVE_CHAR_BUDGET = 248;
    public static final int BOOK_MAX_RENDERED_WIDTH = 816;

    /** Lokale Arbeitsseiten. Courier entscheidet beim Schreiben selbst über Buchseiten. */
    public static List<String> layoutDocumentPages(String command, List<String> sources,
                                                   ToIntFunction<String> width) {
        List<String> pages = new ArrayList<>();
        int limit = SAFE_CHAT_MESSAGE_LIMIT - (command == null ? 0 : command.trim().length()) - 1;
        for (String source : sources == null ? List.<String>of() : sources) {
            pages.addAll(paginate(normalizeDocument(source), width, Math.max(1, limit),
                    new ArrayList<>()));
        }
        return pages.isEmpty() ? List.of("") : List.copyOf(pages);
    }

    private LetterV2Planner() {
    }

    public static LetterCommandPlan plan(String letterCommand, List<String> sourcePages,
                                         ToIntFunction<String> width) {
        return planInternal(letterCommand, sourcePages, width, false, false, List.of());
    }

    public static LetterCommandPlan planFlowing(String letterCommand, List<String> sourcePages,
                                                ToIntFunction<String> width) {
        return planInternal(letterCommand, sourcePages, width, true, false, List.of());
    }

    public static LetterCommandPlan planFlowing(String letterCommand, List<String> sourcePages,
                                                ToIntFunction<String> width,
                                                List<String> initialServerPages) {
        return planInternal(letterCommand, sourcePages, width, true, false,
                initialServerPages);
    }

    /** Plant das kanonische V2-Dokument. Darin sind nur explizit eingegebene Umbrüche. */
    public static LetterCommandPlan planDocument(String letterCommand, List<String> documentPages,
                                                 ToIntFunction<String> width) {
        return planDocument(letterCommand, documentPages, width, List.of());
    }

    public static LetterCommandPlan planDocument(String letterCommand, List<String> documentPages,
                                                 ToIntFunction<String> width,
                                                 List<String> initialServerPages) {
        return planInternal(letterCommand, documentPages, width, true, true,
                initialServerPages);
    }

    private static LetterCommandPlan planInternal(
            String letterCommand, List<String> sourcePages, ToIntFunction<String> width,
            boolean flowing, boolean explicitDocument, List<String> initialServerPages) {
        String command = letterCommand == null ? "" : letterCommand.trim();
        String prefix = command + " ";
        int payloadLimit = SAFE_CHAT_MESSAGE_LIMIT - prefix.length();
        List<String> warnings = new ArrayList<>();

        List<String> sources = sourcePages == null ? List.of() : sourcePages;
        String document = normalizeDocument(String.join("\n", sources));
        if (document.contains("\\n")) {
            warnings.add(LITERAL_NEWLINE_WARNING);
        }
        if (payloadLimit < 1 || command.isEmpty() || command.startsWith("/")
                || command.chars().anyMatch(Character::isWhitespace)
                || document.contains("%prefix%")) {
            warnings.add(PAGE_CONTENT_WARNING);
            return new LetterCommandPlan(List.of(), List.of(), warnings);
        }
        if (!warnings.isEmpty()) {
            return new LetterCommandPlan(List.of(), List.of(document), warnings);
        }
        List<String> pages = new ArrayList<>();
        if (flowing && sources.size() > 1) {
            // Manuelle Arbeitsseiten bleiben in der Redaktion getrennt; Courier kann
            // sie im Buch zusammenführen. Jede beginnt mit eigenem Formatstatus.
            for (String source : sources) {
                String pageDocument = normalizeDocument(source);
                if (!pages.isEmpty() && !LetterFormattingCodes.activePrefixBefore(
                        pages.getLast(), pages.getLast().length()).isEmpty()) {
                    pageDocument = "§r" + pageDocument;
                }
                if (!explicitDocument) {
                    pageDocument = collapseSoftLineBreaks(pageDocument);
                }
                pages.addAll(paginate(pageDocument, width, payloadLimit, warnings));
            }
        } else {
            if (flowing && !explicitDocument) {
                document = collapseSoftLineBreaks(document);
            }
            pages.addAll(paginate(document, width, payloadLimit, warnings));
        }
        List<String> serverPages = initialServerPages == null
                ? new ArrayList<>() : new ArrayList<>(initialServerPages);
        List<LetterCommandPlan.PlannedCommand> entries = new ArrayList<>();

        List<String> groupStartPages = List.copyOf(serverPages);
        String activePrefix = serverPages.isEmpty() ? ""
                : LetterFormattingCodes.activePrefixBefore(serverPages.getLast(),
                serverPages.getLast().length());
        for (String visiblePage : pages) {
            for (String sourceLine : visiblePage.split("\n", -1)) {
                String remaining = materializeEmptyLines(sourceLine);
                while (!remaining.isEmpty()) {
                    String restore = activePrefix.isEmpty() || remaining.startsWith(activePrefix)
                            ? "" : activePrefix;
                    String selected = null;
                    List<String> nextPages = null;
                    int cut = remaining.length();
                    while (cut > 0) {
                        String candidate = restore + remaining.substring(0, cut);
                        String payload = LetterFormattingCodes.sectionToAmpersand(candidate);
                        if (payload.length() <= payloadLimit) {
                            List<String> simulated = CourierNewProtocol.apply(serverPages, payload);
                            String formatted = CourierNewProtocol.format(CourierNewProtocol.rejoinArguments(payload));
                            // A single unit must reach one page intact. If Courier would split
                            // it between words, send the fitting part first and restore the
                            // current style on the next unit/page.
                            // Courier may commit the complete final word before its appended
                            // ASCII space overflows. Only that transport suffix is optional.
                            String withoutSuffix = formatted.endsWith(" ")
                                    ? formatted.substring(0, formatted.length() - 1) : formatted;
                            if (!simulated.isEmpty()
                                    && (simulated.getLast().endsWith(formatted)
                                    || simulated.getLast().endsWith(withoutSuffix))
                                    && simulated.size() <= Math.max(1, serverPages.size()) + 1) {
                                selected = payload;
                                nextPages = simulated;
                                break;
                            }
                        }
                        cut = remaining.lastIndexOf(' ', cut - 1);
                    }
                    if (selected == null) {
                        if (!warnings.contains(PAGE_CONTENT_WARNING)) warnings.add(PAGE_CONTENT_WARNING);
                        break;
                    }
                    String appliedText = restore + remaining.substring(0, cut);
                    activePrefix = activeAfter(activePrefix, appliedText);
                    // Batch only when replay proves exactly the same raw book state. The
                    // explicit space before \n preserves Courier's per-command suffix.
                    boolean merged = false;
                    if (!entries.isEmpty()) {
                        var previous = entries.getLast();
                        String combined = previous.command().substring(prefix.length()) + " \\n" + selected;
                        if (combined.length() <= payloadLimit
                                && CourierNewProtocol.apply(groupStartPages, combined).equals(nextPages)) {
                            entries.set(entries.size() - 1, new LetterCommandPlan.PlannedCommand(
                                    prefix + combined, LetterCommandPlan.Transport.V2_CONTENT,
                                    nextPages.size() - 1));
                            merged = true;
                        }
                    }
                    if (!merged) {
                        groupStartPages = List.copyOf(serverPages);
                        entries.add(new LetterCommandPlan.PlannedCommand(prefix + selected,
                                LetterCommandPlan.Transport.V2_CONTENT, nextPages.size() - 1));
                    }
                    serverPages = nextPages;
                    remaining = remaining.substring(cut);
                    if (remaining.startsWith(" ")) remaining = remaining.substring(1);
                }
            }
        }
        // Reset-Füllbefehle erzeugen im aktuellen Plugin eigene Zeilen/Seiten. Die
        // tatsächliche Pagination (auch bei importierten Briefen) ist die Vorschau.
        return new LetterCommandPlan(entries, displayPages(serverPages), warnings);
    }

    public static List<String> displayPages(List<String> serverPages) {
        return serverPages.stream().map(page -> {
            String[] lines = page.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].stripTrailing();
                if (LetterFormattingCodes.visibleLength(lines[i].replace(EMPTY_LINE, "")) == 0) {
                    lines[i] = lines[i].replace(EMPTY_LINE, "");
                }
            }
            return String.join("\n", lines);
        }).toList();
    }

    private static List<String> paginate(String document, ToIntFunction<String> width,
                                         int payloadLimit, List<String> warnings) {
        PageSplitter wrapper = new PageSplitter(width, BOOK_PAGE_WIDTH, Integer.MAX_VALUE);
        List<String> wrapped = wrapper.wrapLines(document);
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int lines = 0;
        int renderedWidth = 0;
        String activePrefix = "";

        for (String line : wrapped) {
            String firstLine = lines == 0 && !activePrefix.isEmpty()
                    && !line.startsWith(activePrefix) ? activePrefix + line : line;
            String candidate = lines == 0 ? firstLine : page + "\n" + line;
            int lineWidth = width.applyAsInt(materializeEmptyLines(firstLine));
            boolean full = lines >= SERVER_PAGE_MAX_LINES
                    || renderedWidth + lineWidth > BOOK_MAX_RENDERED_WIDTH
                    || encodedLength(candidate) > payloadLimit;
            if (full && lines > 0) {
                pages.add(page.toString());
                page.setLength(0);
                lines = 0;
                renderedWidth = 0;
                firstLine = !activePrefix.isEmpty() && !line.startsWith(activePrefix)
                        ? activePrefix + line : line;
                candidate = firstLine;
                lineWidth = width.applyAsInt(materializeEmptyLines(firstLine));
            }
            if (encodedLength(candidate) > payloadLimit) {
                if (!warnings.contains(PAGE_CONTENT_WARNING)) {
                    warnings.add(PAGE_CONTENT_WARNING);
                }
                continue;
            }
            if (lines > 0) {
                page.append('\n');
            }
            page.append(firstLine);
            lines++;
            renderedWidth += lineWidth;
            activePrefix = activeAfter(activePrefix, line);
        }
        // Eine explizit eingegebene letzte Leerzeile darf auch hinter einer vollen
        // Seite nicht verschwinden. "lines" unterscheidet sie von keinem Inhalt.
        if (lines > 0 || pages.isEmpty()) {
            pages.add(page.toString());
        }
        return List.copyOf(pages);
    }

    private static int encodedLength(String value) {
        return encodePagePayload(LetterFormattingCodes.sectionToAmpersand(
                materializeEmptyLines(value))).length();
    }

    /** CourierNew verwirft echte leere Zeilen; ein Figure Space bleibt optisch leer erhalten. */
    private static String materializeEmptyLines(String value) {
        String[] lines = (value == null ? "" : value).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (LetterFormattingCodes.visibleLength(lines[i]) == 0) {
                lines[i] = lines[i] + EMPTY_LINE;
            }
        }
        return String.join("\n", lines);
    }

    /** Entfernt weiche Einzelumbrüche; Leerzeilen bleiben als Absatzabstand erhalten. */
    public static String collapseSoftLineBreaks(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length();) {
            if (value.charAt(i) != '\n') {
                out.append(value.charAt(i++));
                continue;
            }
            int end = i + 1;
            while (end < value.length() && value.charAt(end) == '\n') {
                end++;
            }
            int count = end - i;
            // Only an internal single break can be a soft wrap. At the
            // document edges it represents an explicitly empty line.
            if (count == 1 && i > 0 && end < value.length()) {
                if (!out.isEmpty() && end < value.length()
                        && out.charAt(out.length() - 1) != ' ') {
                    out.append(' ');
                }
            } else {
                out.append("\n".repeat(count));
            }
            i = end;
        }
        return out.toString();
    }

    public static String normalizeDocument(String raw) {
        String[] lines = (raw == null ? "" : raw).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty() && line.chars().allMatch(ch -> ch == ' ' || ch == 0x2007)) {
                lines[i] = "";
                continue;
            }
            lines[i] = line.trim().replaceAll(" +", " ");
        }
        return String.join("\n", lines);
    }

    private static String activeAfter(String activePrefix, String line) {
        String combined = activePrefix + line;
        return LetterFormattingCodes.activePrefixBefore(combined, combined.length());
    }

    /** CourierNew kennt ausschließlich wörtliches Backslash-n; andere Backslashes bleiben roh. */
    static String encodePagePayload(String raw) {
        return (raw == null ? "" : raw).replace("\n", "\\n");
    }
}
