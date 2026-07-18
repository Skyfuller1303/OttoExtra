package de.ottoextra.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VersionNumber implements Comparable<VersionNumber> {

    private final List<Integer> core;
    private final List<String> preRelease;
    private final String normalized;

    private VersionNumber(List<Integer> core, List<String> preRelease, String normalized) {
        this.core = List.copyOf(core);
        this.preRelease = List.copyOf(preRelease);
        this.normalized = normalized;
    }

    public static VersionNumber parse(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }

        int plus = value.indexOf('+');
        if (plus >= 0) {
            value = value.substring(0, plus);
        }

        String corePart = value;
        String prePart = "";
        int dash = value.indexOf('-');
        if (dash >= 0) {
            corePart = value.substring(0, dash);
            prePart = value.substring(dash + 1);
        }

        List<Integer> core = new ArrayList<>();
        for (String token : corePart.split("\\.")) {
            core.add(parseLeadingNumber(token));
        }
        while (core.size() < 3) {
            core.add(0);
        }

        List<String> preRelease = new ArrayList<>();
        if (!prePart.isBlank()) {
            for (String token : prePart.split("\\.")) {
                if (!token.isBlank()) {
                    preRelease.add(token.toLowerCase(Locale.ROOT));
                }
            }
        }

        String normalizedCore = joinCore(core);
        String normalized = preRelease.isEmpty()
                ? normalizedCore
                : normalizedCore + "-" + String.join(".", preRelease);
        return new VersionNumber(core, preRelease, normalized);
    }

    public boolean isNewerThan(VersionNumber other) {
        return compareTo(other) > 0;
    }

    public String normalized() {
        return normalized;
    }

    @Override
    public int compareTo(VersionNumber other) {
        if (other == null) {
            return 1;
        }

        int length = Math.max(core.size(), other.core.size());
        for (int i = 0; i < length; i++) {
            int left = i < core.size() ? core.get(i) : 0;
            int right = i < other.core.size() ? other.core.get(i) : 0;
            int compared = Integer.compare(left, right);
            if (compared != 0) {
                return compared;
            }
        }

        // Gleiche Kernversion: stabil ist neuer als ein Pre-Release.
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) {
            return 0;
        }
        if (preRelease.isEmpty()) {
            return 1;
        }
        if (other.preRelease.isEmpty()) {
            return -1;
        }

        int preLength = Math.max(preRelease.size(), other.preRelease.size());
        for (int i = 0; i < preLength; i++) {
            if (i >= preRelease.size()) {
                return -1;
            }
            if (i >= other.preRelease.size()) {
                return 1;
            }

            String left = preRelease.get(i);
            String right = other.preRelease.get(i);
            boolean leftNumeric = isDigits(left);
            boolean rightNumeric = isDigits(right);

            int compared;
            if (leftNumeric && rightNumeric) {
                compared = Integer.compare(parseSafeInt(left), parseSafeInt(right));
            } else if (leftNumeric != rightNumeric) {
                // SemVer: numerische Bezeichner haben niedrigere Prioritaet.
                compared = leftNumeric ? -1 : 1;
            } else {
                compared = left.compareTo(right);
            }

            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return normalized;
    }

    private static int parseLeadingNumber(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        int end = 0;
        while (end < token.length() && Character.isDigit(token.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        return parseSafeInt(token.substring(0, end));
    }

    private static int parseSafeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static boolean isDigits(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String joinCore(List<Integer> core) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < core.size(); i++) {
            if (i > 0) {
                out.append('.');
            }
            out.append(core.get(i));
        }
        return out.toString();
    }
}
