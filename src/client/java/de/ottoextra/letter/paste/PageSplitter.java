package de.ottoextra.letter.paste;

import java.util.ArrayList;
import java.util.List;

/**
 * Verteilt normalisierten Fließtext auf Brief-Seiten:
 * Wortumbruch an Zeilenbreite, Seitenwechsel nach Zeilenlimit. Es geht NIE
 * Inhalt verloren — lange Wörter werden hart umbrochen statt verworfen.
 */
public final class PageSplitter {

    private final int maxCharsPerLine;
    private final int maxLinesPerPage;

    public PageSplitter(int maxCharsPerLine, int maxLinesPerPage) {
        this.maxCharsPerLine = Math.max(4, maxCharsPerLine);
        this.maxLinesPerPage = Math.max(1, maxLinesPerPage);
    }

    /** Text → Seiten (jede Seite als zusammenhängender String mit \n-Zeilen). */
    public List<String> split(String text) {
        List<String> lines = wrapLines(text);
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int linesOnPage = 0;
        for (String line : lines) {
            if (linesOnPage == maxLinesPerPage) {
                pages.add(page.toString());
                page.setLength(0);
                linesOnPage = 0;
            }
            if (linesOnPage > 0) {
                page.append('\n');
            }
            page.append(line);
            linesOnPage++;
        }
        if (page.length() > 0 || pages.isEmpty()) {
            pages.add(page.toString());
        }
        return pages;
    }

    /** Wortumbruch auf Zeilenbreite; \n bleibt harter Umbruch. */
    public List<String> wrapLines(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\n", -1)) {
            if (raw.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : raw.split(" ", -1)) {
                while (word.length() > maxCharsPerLine) {
                    // Überlanges Wort hart teilen (nichts verwerfen)
                    if (line.length() > 0) {
                        out.add(line.toString());
                        line.setLength(0);
                    }
                    out.add(word.substring(0, maxCharsPerLine));
                    word = word.substring(maxCharsPerLine);
                }
                if (line.length() == 0) {
                    line.append(word);
                } else if (line.length() + 1 + word.length() <= maxCharsPerLine) {
                    line.append(' ').append(word);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            out.add(line.toString());
        }
        return out;
    }
}
