package de.ottoextra.tweaks.vote;

/**
 * Size of the vote reminder card (design C), free of Minecraft: gold header band, seal in a dark inset
 * square, title and subtitle, a bottom row with the vote key plus "Abstimmen" on the left and the dismiss
 * key plus "Später" on the right, and a thin remaining-time line. The card is {@link #MIN_W} wide and grows
 * only when the bottom row needs it, up to the screen width; texts that still do not fit are clipped, and
 * "Später" is left out when it would run into the left part.
 */
final class VoteToastLayout {
    static final int MIN_W = 196;
    static final int H = 58;
    /** Header band: inset 3 px, 12 px high, title 2 px below its top. */
    static final int BAND_INSET = 3;
    static final int BAND_BOTTOM = 15;
    static final int HEADER_TEXT_Y = 5;
    /** Dark square around the seal (20x20 with outline) and the seal itself at native 16x16. */
    static final int SEAL_BOX_X = 7;
    static final int SEAL_BOX_Y = 20;
    static final int SEAL_BOX = 20;
    static final int SEAL_X = 9;
    static final int SEAL_Y = 22;
    static final int TEXT_X = 33;
    static final int TITLE_Y = 21;
    static final int SUBTITLE_Y = 31;
    static final int ROW_Y = 42;
    static final int CHIP_H = 10;
    /** Padding of the key label inside the chip, on both sides together. */
    static final int CHIP_PAD = 6;
    static final int LABEL_GAP = 3;
    /** Minimum space between the left and the right part of the bottom row. */
    static final int ROW_GAP = 8;
    static final int RIGHT_PAD = 6;
    /** Remaining-time line: 1 px high, 4 px above the bottom edge. */
    static final int LINE_FROM_BOTTOM = 4;
    static final int SCREEN_MARGIN = 4;

    /**
     * @param width       card width
     * @param textRoom    room of title and subtitle
     * @param headerRoom  room of the header title inside the band
     * @param openChipW   width of the vote key chip, 0 without a bound key (no chip, no "Abstimmen")
     * @param actionRoom  room of "Abstimmen" right of its chip
     * @param laterX      left edge of the dismiss key chip inside the card
     * @param laterChipW  width of the dismiss key chip
     * @param showLater   the dismiss part is drawn (key bound and room left)
     */
    record Layout(int width, int textRoom, int headerRoom, int openChipW, int actionRoom, int laterX,
                  int laterChipW, boolean showLater) {
        int lineWidth() {
            return Math.max(0, width - 2 * BAND_INSET);
        }
    }

    private VoteToastLayout() {
    }

    /** Key widths are the widths of the key labels, 0 when that key is unbound. */
    static Layout compute(int openKeyW, int openW, int laterKeyW, int laterW, int screenW) {
        int openChip = openKeyW > 0 ? openKeyW + CHIP_PAD : 0;
        int left = openChip > 0 ? openChip + LABEL_GAP + Math.max(0, openW) : 0;
        int laterChip = laterKeyW > 0 ? laterKeyW + CHIP_PAD : 0;
        int right = laterChip > 0 ? laterChip + LABEL_GAP + Math.max(0, laterW) : 0;
        int gap = left > 0 && right > 0 ? ROW_GAP : 0;
        int want = Math.max(MIN_W, TEXT_X + left + gap + right + RIGHT_PAD);
        int width = Math.max(0, Math.min(want, screenW - 2 * SCREEN_MARGIN));
        int laterX = width - RIGHT_PAD - right;
        boolean showLater = right > 0 && laterX >= TEXT_X + left + gap;
        int textRoom = Math.max(0, width - TEXT_X - RIGHT_PAD);
        int actionRoom = openChip > 0 ? Math.max(0, textRoom - openChip - LABEL_GAP) : 0;
        int headerRoom = Math.max(0, width - 2 * BAND_INSET - 4);
        return new Layout(width, textRoom, headerRoom, openChip, actionRoom, laterX, laterChip, showLater);
    }

    /** Filled pixels of the remaining-time line: full when shown, empty when the toast ends. */
    static int remaining(int lineWidth, long elapsed, long duration) {
        if (duration <= 0 || lineWidth <= 0) {
            return 0;
        }
        double left = 1.0 - Math.max(0L, Math.min(duration, elapsed)) / (double) duration;
        return (int) (lineWidth * left);
    }
}
