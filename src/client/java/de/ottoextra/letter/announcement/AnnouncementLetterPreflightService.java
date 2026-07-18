package de.ottoextra.letter.announcement;

import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.placeholder.LetterPlaceholderParser;

import java.util.ArrayList;
import java.util.List;

public final class AnnouncementLetterPreflightService {

    public record PageCheck(int pageIndex, List<String> lines, int usedLines,
                            int maxLineLength, List<String> warnings, List<String> blockers) {
        public boolean ok() {
            return blockers.isEmpty();
        }

        public boolean clean() {
            return blockers.isEmpty() && warnings.isEmpty();
        }
    }

    public record Result(String draftId, int pageCount, int totalLines, int totalCharacters,
                         List<PageCheck> pages, List<String> blockers, List<String> warnings) {
        public boolean canSend() {
            return blockers.isEmpty();
        }
    }

    private final int safeLines;
    private final int safeChars;
    private final int hardLines;
    private final int hardChars;

    public AnnouncementLetterPreflightService(int safeLines, int safeChars,
                                              int hardLines, int hardChars) {
        this.safeLines = Math.max(1, safeLines);
        this.safeChars = Math.max(4, safeChars);
        this.hardLines = Math.max(this.safeLines, hardLines);
        this.hardChars = Math.max(this.safeChars, hardChars);
    }

    public Result check(String draftId, List<String> pageTexts) {
        PageSplitter wrapper = new PageSplitter(hardChars, Integer.MAX_VALUE);
        List<PageCheck> checks = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int totalLines = 0;
        int totalChars = 0;
        for (int i = 0; i < pageTexts.size(); i++) {
            String text = pageTexts.get(i);
            List<String> lines = wrapper.wrapLines(text);
            List<String> pageWarnings = new ArrayList<>();
            List<String> pageBlockers = new ArrayList<>();
            int maxLen = 0;
            for (int l = 0; l < lines.size(); l++) {
                int len = lines.get(l).length();
                maxLen = Math.max(maxLen, len);
                if (len > hardChars) {
                    pageBlockers.add("Zeile " + (l + 1) + ": " + len + "/" + hardChars
                            + " Zeichen — zu lang");
                } else if (len > safeChars) {
                    pageWarnings.add("Zeile " + (l + 1) + ": " + len + "/" + safeChars
                            + " Zeichen — über sicherer Breite");
                }
            }
            if (text.isBlank()) {
                pageBlockers.add("Seite ist leer");
            }
            int open = LetterPlaceholderParser.parse(text).size();
            if (open > 0) {
                pageBlockers.add(open + " Platzhalter nicht aufgelöst");
            }
            int invalid = LetterPlaceholderParser.countInvalidOpenings(text);
            if (invalid > 0) {
                pageBlockers.add(invalid + " ungültige Platzhalter-Klammern");
            }
            if (lines.size() > hardLines) {
                pageBlockers.add(lines.size() + "/" + hardLines
                        + " Zeilen — über hartem Seitenlimit");
            } else if (lines.size() > safeLines) {
                pageWarnings.add(lines.size() + "/" + safeLines
                        + " Zeilen — sehr voll, wirkt im Discord gedrängt");
            }
            totalLines += lines.size();
            totalChars += text.length();
            checks.add(new PageCheck(i, lines, lines.size(), maxLen, pageWarnings, pageBlockers));
            for (String b : pageBlockers) {
                blockers.add("Seite " + (i + 1) + ": " + b);
            }
            for (String w : pageWarnings) {
                warnings.add("Seite " + (i + 1) + ": " + w);
            }
        }
        return new Result(draftId, pageTexts.size(), totalLines, totalChars,
                checks, blockers, warnings);
    }

    public List<String> optimize(List<String> pageTexts) {
        PageSplitter safe = new PageSplitter(safeChars, safeLines);
        List<String> out = new ArrayList<>();
        for (String text : pageTexts) {
            if (text.isBlank()) {
                out.add(text);
                continue;
            }
            out.addAll(safe.split(text));
        }
        return out;
    }
}
