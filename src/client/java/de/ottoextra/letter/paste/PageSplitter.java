package de.ottoextra.letter.paste;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Verteilt normalisierten Fließtext auf Brief-Seiten:
 * Wortumbruch an Zeilenbreite, Seitenwechsel nach Zeilenlimit. Es geht NIE
 * Inhalt verloren — lange Wörter werden hart umbrochen statt verworfen.
 *
 * <p>Zwei Breiten-Modi:</p>
 * <ul>
 *   <li><b>Zeichen</b> ({@link #PageSplitter(int, int)}) — feste Zeichen pro Zeile
 *       (Import, Verkündungs-Safety).</li>
 *   <li><b>Pixel</b> ({@link #PageSplitter(ToIntFunction, int, int)}) — Umbruch
 *       nach Pixelbreite wie das echte Minecraft-Buch (WYSIWYG-Editor).</li>
 * </ul>
 */
public final class PageSplitter {

    private final int maxCharsPerLine;
    private final int maxLinesPerPage;
    /** Pixel-Modus, wenn gesetzt; sonst Zeichen-Modus. */
    private final ToIntFunction<String> widthFn;
    private final int maxWidthPx;
    /** Effektives Zeichen-Budget pro Seite (Umbruch zählt 2); 0 = unbegrenzt. */
    private final int maxEffectiveChars;

    public PageSplitter(int maxCharsPerLine, int maxLinesPerPage) {
        this(Math.max(4, maxCharsPerLine), maxLinesPerPage, null, 0, 0);
    }

    /** Pixelbasiert: {@code widthFn} misst Stringbreite, {@code maxWidthPx} = Zeilenbreite. */
    public PageSplitter(ToIntFunction<String> widthFn, int maxWidthPx, int maxLinesPerPage) {
        this(widthFn, maxWidthPx, maxLinesPerPage, 0);
    }

    /** Pixelbasiert + Zeichen-Budget pro Seite ({@code maxEffectiveChars}, 0 = aus). */
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

    /** Passt der String in eine Zeile? */
    private boolean fits(String s) {
        return widthFn != null ? widthFn.applyAsInt(s) <= maxWidthPx : s.length() <= maxCharsPerLine;
    }

    /** Ist das Wort allein zu breit für eine Zeile (-> harter Umbruch)? */
    private boolean wordTooLong(String w) {
        return widthFn != null ? widthFn.applyAsInt(w) > maxWidthPx : w.length() > maxCharsPerLine;
    }

    /** Anzahl Zeichen am Wortanfang, die noch in eine Zeile passen (mind. 1). */
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

    /** Text → Seiten (jede Seite als zusammenhängender String mit \n-Zeilen). */
    public List<String> split(String text) {
        List<String> lines = wrapLines(text);
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int linesOnPage = 0;
        int effOnPage = 0; // effektive Buchzeichen der Seite (\n zählt 2)
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
                while (wordTooLong(word)) {
                    // Überlanges Wort hart teilen (nichts verwerfen)
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
