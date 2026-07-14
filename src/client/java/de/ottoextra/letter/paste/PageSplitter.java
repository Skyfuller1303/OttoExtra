package de.ottoextra.letter.paste;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class PageSplitter {

    private final int maxCharsPerLine;
    private final int maxLinesPerPage;

    private final ToIntFunction<String> widthFn;
    private final int maxWidthPx;

    private final int maxEffectiveChars;

    public PageSplitter(int maxCharsPerLine, int maxLinesPerPage) {
        this(Math.max(4, maxCharsPerLine), maxLinesPerPage, null, 0, 0);
    }

    public PageSplitter(ToIntFunction<String> widthFn, int maxWidthPx, int maxLinesPerPage) {
        this(widthFn, maxWidthPx, maxLinesPerPage, 0);
    }

    public PageSplitter(ToIntFunction<String> widthFn, int maxWidthPx, int maxLinesPerPage,
                        int maxEffectiveChars) {
        this(Integer.MAX_VALUE, maxLinesPerPage, widthFn, Math.max(8, maxWidthPx),
                maxEffectiveChars);
    }

    private PageSplitter(int maxCharsPerLine, int maxLinesPerPage,
                         ToIntFunction<String> widthFn, int maxWidthPx, int maxEffectiveChars) {
        this.maxCharsPerLine = maxCharsPerLine;
        this.maxLinesPerPage = Math.max(1, maxLinesPerPage);
        this.widthFn = widthFn;
        this.maxWidthPx = maxWidthPx;
        this.maxEffectiveChars = maxEffectiveChars;
    }

    private boolean fits(String s) {
        return widthFn != null ? widthFn.applyAsInt(s) <= maxWidthPx : s.length() <= maxCharsPerLine;
    }

    private boolean wordTooLong(String w) {
        return widthFn != null ? widthFn.applyAsInt(w) > maxWidthPx : w.length() > maxCharsPerLine;
    }

    private int hardCut(String word) {
        if (widthFn == null) {
            return Math.min(maxCharsPerLine, word.length());
        }
        int i = 1;
        while (i < word.length() && widthFn.applyAsInt(word.substring(0, i + 1)) <= maxWidthPx) {
            i++;
        }
        return Math.max(1, i);
    }

    public List<String> split(String text) {
        List<String> lines = wrapLines(text);
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int linesOnPage = 0;
        int effOnPage = 0;
        for (String line : lines) {
            int add = (linesOnPage > 0 ? 2 : 0) + line.length();
            boolean lineFull = linesOnPage == maxLinesPerPage;
            boolean charFull = maxEffectiveChars > 0 && linesOnPage > 0
                    && effOnPage + add > maxEffectiveChars;
            if (lineFull || charFull) {
                pages.add(page.toString());
                page.setLength(0);
                linesOnPage = 0;
                effOnPage = 0;
                add = line.length();
            }
            if (linesOnPage > 0) {
                page.append('\n');
            }
            page.append(line);
            effOnPage += add;
            linesOnPage++;
        }
        if (page.length() > 0 || pages.isEmpty()) {
            pages.add(page.toString());
        }
        return pages;
    }

    public List<String> wrapLines(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\n", -1)) {
            if (raw.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : raw.split(" ", -1)) {
                while (wordTooLong(word)) {

                    if (line.length() > 0) {
                        out.add(line.toString());
                        line.setLength(0);
                    }
                    int cut = hardCut(word);
                    out.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                if (line.length() == 0) {
                    line.append(word);
                } else if (fits(line + " " + word)) {
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
