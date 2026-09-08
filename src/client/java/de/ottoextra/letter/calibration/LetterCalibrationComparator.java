package de.ottoextra.letter.calibration;

import java.util.ArrayList;
import java.util.List;

public final class LetterCalibrationComparator {

    private LetterCalibrationComparator() {
    }

    public static List<String> compare(List<String> expected, List<String> observed) {
        List<String> diff = new ArrayList<>();
        if (expected.size() != observed.size()) {
            diff.add("PAGE_COUNT expected=" + expected.size() + " observed=" + observed.size());
        }
        int count = Math.max(expected.size(), observed.size());
        for (int i = 0; i < count; i++) {
            String exp = canonical(i < expected.size() ? expected.get(i) : "");
            String obs = canonical(i < observed.size() ? observed.get(i) : "");
            if (!LetterMarkupSemantics.plain(exp).equals(LetterMarkupSemantics.plain(obs))) {
                diff.add("PAGE_" + (i + 1) + "_TEXT");
            } else if (!LetterMarkupSemantics.runs(exp).equals(LetterMarkupSemantics.runs(obs))) {
                diff.add("PAGE_" + (i + 1) + "_STYLE");
            }
        }
        return List.copyOf(diff);
    }

    private static String canonical(String value) {
        String text = value == null ? "" : value;
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int end = line.length();
            while (end > 0 && (line.charAt(end - 1) == ' '
                    || line.charAt(end - 1) == '\u2007')) {
                end--;
            }
            lines[i] = end == 0 ? "" : line.substring(0, end);
        }
        return String.join("\n", lines);
    }
}
