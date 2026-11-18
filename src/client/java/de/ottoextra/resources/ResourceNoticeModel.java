package de.ottoextra.resources;

/**
 * What a zone notice shows, without any drawing: the state, its colour ramp, the status label key,
 * percentage, bar fraction and the time until the zone is full. Ramps and measures follow
 * gen_zonen_toast_hud.py (the animal toast and activity HUD of the core mod). Pure, so it can be
 * tested without the game.
 */
record ResourceNoticeModel(Kind kind, int percent, float fraction, int secondsUntilFull) {
    /** Ten minutes or less until full counts as "soon" (rust ramp), like the HUD's time colour. */
    static final int SOON_SECONDS = 10 * 60;

    static final int[] READY = {0xFF2F5426, 0xFF5E9A3E, 0xFF86C25A};
    static final int[] WAIT = {0xFF96752C, 0xFFCD9F3D, 0xFFDBB451};
    static final int[] SOON = {0xFF7A3A1E, 0xFFC0602E, 0xFFE08A4A};

    enum Kind {
        /** Fully recovered. */
        READY("ready"),
        /** A mine past its partial stage: can be mined while it still recovers. */
        HARVESTABLE("harvestable"),
        RECOVERING("recovering"),
        /** Ten minutes or less until full. */
        SOON("soon"),
        /** Timer started after mining (also the notice when leaving the zone). */
        STARTED("started"),
        RESTARTED("restarted"),
        /** Manually set back to full. */
        RESET("reset"),
        /** Blocks were broken; the timer waits for the confirm key. */
        PENDING("pending"),
        /** Left the zone after mining; confirming is still possible for a minute. */
        PENDING_OUTSIDE("pending_outside");

        final String key;

        Kind(String key) {
            this.key = key;
        }
    }

    /** State of a zone as the player meets it. */
    static ResourceNoticeModel of(String status, int percent, int secondsUntilFull, boolean harvestable) {
        int shown = Math.max(0, Math.min(100, percent));
        Kind kind;
        if ("READY".equals(status) || shown >= 100) {
            kind = Kind.READY;
            shown = 100;
        } else if (harvestable) {
            kind = Kind.HARVESTABLE;
        } else if (secondsUntilFull > 0 && secondsUntilFull <= SOON_SECONDS) {
            kind = Kind.SOON;
        } else {
            kind = Kind.RECOVERING;
        }
        return new ResourceNoticeModel(kind, shown, shown / 100f, kind == Kind.READY ? 0 : Math.max(0, secondsUntilFull));
    }

    /** A notice of a fixed kind (started, reset, pending, ...) with the zone's current values. */
    static ResourceNoticeModel event(Kind kind, int percent, int secondsUntilFull) {
        int shown = Math.max(0, Math.min(100, percent));
        return new ResourceNoticeModel(kind, shown, shown / 100f, shown >= 100 ? 0 : Math.max(0, secondsUntilFull));
    }

    /** Colour ramp (dark, mid, light) of the status stripe, band and bar. */
    int[] ramp() {
        return switch (kind) {
            case READY, HARVESTABLE, RESET -> READY;
            case RECOVERING -> WAIT;
            case SOON, STARTED, RESTARTED, PENDING, PENDING_OUTSIDE -> SOON;
        };
    }

    /** Translation key of the status label. */
    String statusKey() {
        return "ottoextra.resources.notice." + kind.key;
    }

    /** True while the zone still recovers, so "voll in X" is shown. */
    boolean showsTime() {
        return secondsUntilFull > 0 && percent < 100;
    }

    /** Minutes until full, rounded up; at least one while there is time left. */
    int minutesUntilFull() {
        return secondsUntilFull <= 0 ? 0 : Math.max(1, (secondsUntilFull + 59) / 60);
    }

    /** "8 min" below an hour, "1:12 h" from an hour on (as in the sketches). */
    String clock() {
        return ResourceBook.clockText(minutesUntilFull());
    }
}
