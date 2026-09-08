package de.ottoextra.letter.calibration;

import de.ottoextra.letter.format.LetterFormattingCodes;

public final class LetterWireCodec {

    private LetterWireCodec() {
    }

    public static String decodePagePayload(String encoded) {
        String input = encoded == null ? "" : encoded;
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'n') {
                    out.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    out.append('\\');
                    i++;
                    continue;
                }
            }
            out.append(ch);
        }
        return LetterFormattingCodes.ampersandToSection(out.toString());
    }
}
