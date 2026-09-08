package de.ottoextra.letter.send;

import java.util.ArrayList;
import java.util.List;

/** Modell der angepassten Courier-2.1.0-/letter-Logik aus input/. */
public final class CourierNewProtocol {

    public static final int MAX_CHARS_PER_PAGE = 255;
    public static final int SAFE_MAX_VISUAL_LINES = 13;
    public static final int AVG_CHARS_PER_VISUAL_LINE = 18;
    /** Nur für alte gespeicherte Kalibrierungen; keine Seitengrenze in Courier 2.1.0. */
    @Deprecated public static final int NEW_PAGE_THRESHOLD = 256;

    private static final String COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private CourierNewProtocol() {
    }

    /**
     * Führt einen Payload ohne Befehlsnamen so aus, wie CommandExec und LetterCreation ihn
     * verarbeiten. Die übergebene Liste wird nicht verändert.
     */
    public static List<String> apply(List<String> existingPages, String commandPayload) {
        List<String> pages = existingPages == null
                ? new ArrayList<>() : new ArrayList<>(existingPages);
        String formatted = format(rejoinArguments(commandPayload));
        if (formatted.isEmpty()) {
            return pages;
        }
        if (pages.isEmpty()) {
            pages.add("");
        }
        for (String line : formatted.split("\n", -1)) {
            String currentLine = "";
            for (String word : line.split(" ", -1)) {
                String next = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (wouldOverflow(pages.getLast(), next)) {
                    if (!currentLine.isEmpty()) {
                        commitLine(pages, currentLine);
                    } else {
                        pages.add("");
                    }
                    currentLine = word;
                } else {
                    currentLine = next;
                }
            }
            if (!currentLine.isEmpty()) {
                commitLine(pages, currentLine);
            }
        }
        return pages;
    }

    private static void commitLine(List<String> pages, String line) {
        String page = pages.getLast();
        if (wouldOverflow(page, line) && !page.isEmpty()) {
            pages.add("");
            page = "";
        }
        pages.set(pages.size() - 1, page.isEmpty() ? line : page + "\n" + line);
    }

    public static boolean wouldOverflow(String page, String line) {
        double existing = page.isEmpty() ? 0 : countRenderedLines(page);
        double added = countRenderedLines(line) + (page.isEmpty() ? 0 : 0.5);
        return existing + added > SAFE_MAX_VISUAL_LINES
                || page.length() + line.length() + 1 > MAX_CHARS_PER_PAGE;
    }

    public static int countRenderedLines(String text) {
        int count = 0;
        for (String line : text.split("\n", -1)) {
            count += Math.max(1, (int) Math.ceil(line.length()
                    / (double) AVG_CHARS_PER_VISUAL_LINE));
        }
        return count;
    }

    /** Bukkit liefert einzelne Argumente; CourierNew hängt hinter jedes wieder ein Leerzeichen. */
    public static String rejoinArguments(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (String argument : payload.split(" +")) {
            if (!argument.isEmpty()) {
                joined.append(argument).append(' ');
            }
        }
        return joined.toString();
    }

    /** Message.format: wörtliches Backslash-n und Bukkit-&-Farbcodes. */
    public static String format(String value) {
        String withLines = (value == null ? "" : value)
                .replace("%prefix%", "&7[&6Courier&7]").replace("\\n", "\n");
        StringBuilder out = new StringBuilder(withLines.length());
        for (int i = 0; i < withLines.length(); i++) {
            char ch = withLines.charAt(i);
            if (ch == '&' && i + 1 < withLines.length()
                    && COLOR_CODES.indexOf(withLines.charAt(i + 1)) >= 0) {
                out.append('§').append(Character.toLowerCase(withLines.charAt(++i)));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
