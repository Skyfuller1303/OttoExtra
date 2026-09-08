package de.ottoextra.animalinfo;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AnimalInfoResponseCollector {

    private static final int MAX_BLOCK_LINES = 20;
    private static final Pattern HEADER = Pattern.compile(
            "^(Tierinfo|Pferdeinfo)\\s*[-–—:]\\s*(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FIELD = Pattern.compile("^([^:]+):\\s*(.*)$");
    private static final Pattern DIVIDER = Pattern.compile("^-{5,}$");

    private State state = State.IDLE;
    private int blockLines;
    private String animalName = "";
    private String gender = "";
    private String mast = "";
    private String breedingReady = "";
    private String shearable = "";
    private String milkable = "";
    private String jumpHeight = "";
    private String speed = "";
    private String life = "";
    private boolean finishOnBreedingReady;

    void start() {
        resetValues();
        state = State.WAITING;
    }

    void reset() {
        resetValues();
        state = State.IDLE;
    }

    Result accept(String rawLine) {
        if (state == State.IDLE || rawLine == null) {
            return Result.pass();
        }
        String line = rawLine.trim();

        if (state == State.WAITING) {
            if (isDivider(line)) {
                state = State.LEADING_DIVIDER;
                return Result.consumed(null);
            }
            return acceptHeader(line) ? Result.consumed(null) : Result.pass();
        }

        if (state == State.LEADING_DIVIDER) {
            if (isDivider(line)) {
                return Result.consumed(null);
            }
            if (acceptHeader(line)) {
                return Result.consumed(null);
            }
            resetValues();
            state = State.WAITING;
            return Result.pass();
        }

        if (isDivider(line)) {
            AnimalInfoSnapshot snapshot = snapshot();
            reset();
            return Result.consumed(snapshot.complete() ? snapshot : null);
        }

        if (++blockLines > MAX_BLOCK_LINES) {
            reset();
            return Result.consumed(null);
        }

        Matcher field = FIELD.matcher(line);
        if (field.matches()) {
            String key = normalizeKey(field.group(1));
            String value = field.group(2).trim();
            if ("geschlecht".equals(key) || "gender".equals(key)) {
                gender = value;
            } else if ("leben".equals(key) || "life".equals(key)
                    || "health".equals(key)) {
                life = value;
            } else if ("mast".equals(key) || "feeding".equals(key)) {
                mast = value;
            } else if ("paarungsbereit".equals(key)
                    || "zuchtbereit".equals(key)
                    || "breeding ready".equals(key)
                    || "ready to breed".equals(key)) {
                breedingReady = value;
                if (finishOnBreedingReady) {
                    AnimalInfoSnapshot snapshot = snapshot();
                    reset();
                    return Result.consumed(snapshot.complete() ? snapshot : null, true);
                }
            } else if ("scherbar".equals(key) || "shearable".equals(key)) {
                shearable = value;
            } else if ("melkbar".equals(key) || "milkable".equals(key)) {
                milkable = value;
            } else if ("sprunghöhe".equals(key) || "sprunghoehe".equals(key)
                    || "jump height".equals(key)) {
                jumpHeight = value;
            } else if ("geschwindigkeit".equals(key) || "speed".equals(key)) {
                speed = value;
            }
        }
        return Result.consumed(null);
    }

    private boolean acceptHeader(String line) {
        Matcher header = HEADER.matcher(line);
        if (!header.matches()) {
            return false;
        }
        finishOnBreedingReady = "pferdeinfo".equalsIgnoreCase(header.group(1));
        animalName = header.group(2).trim();
        state = State.COLLECTING;
        blockLines = 0;
        return true;
    }

    private static boolean isDivider(String line) {
        return DIVIDER.matcher(line).matches();
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void resetValues() {
        blockLines = 0;
        animalName = "";
        gender = "";
        mast = "";
        breedingReady = "";
        shearable = "";
        milkable = "";
        jumpHeight = "";
        speed = "";
        life = "";
        finishOnBreedingReady = false;
    }

    private AnimalInfoSnapshot snapshot() {
        return new AnimalInfoSnapshot(
                animalName, gender, mast, breedingReady, shearable, milkable,
                jumpHeight, speed, life);
    }

    private enum State {
        IDLE,
        WAITING,
        LEADING_DIVIDER,
        COLLECTING
    }

    record Result(boolean consumed, AnimalInfoSnapshot completed,
                  boolean expectsTrailingDivider) {
        static Result pass() {
            return new Result(false, null, false);
        }

        static Result consumed(AnimalInfoSnapshot completed) {
            return consumed(completed, false);
        }

        static Result consumed(AnimalInfoSnapshot completed,
                               boolean expectsTrailingDivider) {
            return new Result(true, completed, expectsTrailingDivider);
        }
    }
}
