package de.ottoextra.animalinfo;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AnimalInfoResponseCollector {

    private static final int MAX_BLOCK_LINES = 64;
    private static final Pattern HEADER = Pattern.compile(
            "^(Tierinfo|Pferdeinfo)\\s*[-–—:]\\s*(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SHORT_HEADER = Pattern.compile("^[-–—]\\s+(.+?)\\s*$");
    private static final Pattern FIELD = Pattern.compile("^([^:：]+)[:：]\\s*(.*)$");
    private static final Pattern DIVIDER = Pattern.compile("^[-–—─━]{5,}$");
    private static final Pattern FORMATTING = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern KEY_DECORATION = Pattern.compile("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$");

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

    /** Parses both individual chat lines and a block sent as one message. */
    Result acceptMessage(String rawMessage) {
        if (rawMessage == null) return Result.pass();
        boolean consumed = false;
        boolean unrelated = false;
        boolean expectsTrailingDivider = false;
        AnimalInfoSnapshot completed = null;
        for (String line : rawMessage.split("\\R", -1)) {
            if (expectsTrailingDivider && isDivider(line)) {
                expectsTrailingDivider = false;
                consumed = true;
                continue;
            }
            Result result = accept(line);
            consumed |= result.consumed();
            unrelated |= !result.consumed() && !line.isBlank();
            if (result.completed() != null) completed = result.completed();
            expectsTrailingDivider |= result.expectsTrailingDivider();
        }
        // Hiding is message-wide: preserve the whole payload if it contains chat.
        return new Result(consumed && !unrelated, completed, expectsTrailingDivider);
    }

    Result accept(String rawLine) {
        if (state == State.IDLE || rawLine == null) {
            return Result.pass();
        }
        String line = normalizeLine(rawLine);

        if (state == State.WAITING) {
            if (isDivider(line)) {
                state = State.LEADING_DIVIDER;
                return Result.consumed(null);
            }
            return acceptHeader(line) ? Result.consumed(null) : Result.pass();
        }

        if (state == State.LEADING_DIVIDER) {
            if (line.isEmpty() || isDivider(line)) {
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

        // A fresh header must never inherit stats from a truncated response.
        if (HEADER.matcher(line).matches()) {
            acceptHeader(line);
            return Result.consumed(null);
        }

        if (++blockLines > MAX_BLOCK_LINES) {
            reset();
            return Result.pass();
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
            return Result.consumed(null);
        }
        // Ordinary chat can arrive between response lines; leave it visible.
        return line.isEmpty() ? Result.consumed(null) : Result.pass();
    }

    private boolean acceptHeader(String line) {
        Matcher header = HEADER.matcher(line);
        boolean framed = state == State.LEADING_DIVIDER;
        String name;
        boolean horseHeader = false;
        if (header.matches()) {
            name = header.group(2);
            horseHeader = "pferdeinfo".equalsIgnoreCase(header.group(1));
        } else {
            // The short server header is only meaningful inside a framed block.
            Matcher shortHeader = SHORT_HEADER.matcher(line);
            if (!framed || !shortHeader.matches() || line.contains(":") || line.contains("：")) {
                return false;
            }
            name = shortHeader.group(1);
        }
        resetValues();
        finishOnBreedingReady = horseHeader && !framed;
        animalName = name.trim();
        state = State.COLLECTING;
        blockLines = 0;
        return true;
    }

    static boolean isDivider(String line) {
        return line != null && DIVIDER.matcher(normalizeLine(line)).matches();
    }

    private static String normalizeKey(String value) {
        return KEY_DECORATION.matcher(value).replaceAll("").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeLine(String line) {
        return WHITESPACE.matcher(FORMATTING.matcher(line).replaceAll("")).replaceAll(" ").trim();
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
