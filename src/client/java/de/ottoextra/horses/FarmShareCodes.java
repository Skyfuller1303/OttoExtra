package de.ottoextra.horses;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Random;

/**
 * Share codes of the farm lists (docs: OttoExtra-Discordbot/web/docs/farm-lists-protokoll.md). A write code
 * derives the read code, which derives the list id; every step is a SHA-256 over UTF-8, so the service never
 * stores a code. Same scheme as the resource lists of OttoExtra Resources with own prefixes and the read
 * code prefix {@code H}.
 */
public final class FarmShareCodes {
    public static final String ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
    public static final int WRITE_LENGTH = 20;
    public static final int READ_LENGTH = 25;
    private static final String READ_PREFIX = "ottoextra-farms:read:";
    private static final String LIST_PREFIX = "ottoextra-farms:list:";
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Kind { WRITE, READ, INVALID }

    private FarmShareCodes() {
    }

    public static String generateWriteCode() {
        return generateWriteCode(RANDOM);
    }

    static String generateWriteCode(Random random) {
        StringBuilder code = new StringBuilder(WRITE_LENGTH);
        for (int index = 0; index < WRITE_LENGTH; index++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** Upper-cases the input and removes hyphens and whitespace. */
    public static String normalize(String input) {
        if (input == null) return "";
        return input.toUpperCase(Locale.ROOT).replaceAll("[-\\s]", "");
    }

    public static Kind kind(String input) {
        String code = normalize(input);
        if (code.length() == WRITE_LENGTH && code.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            return Kind.WRITE;
        }
        if (code.length() == READ_LENGTH && code.charAt(0) == 'H'
                && code.substring(1).chars().allMatch(c -> c >= '0' && c <= '9' || c >= 'A' && c <= 'F')) {
            return Kind.READ;
        }
        return Kind.INVALID;
    }

    /** {@code H} followed by the first 24 upper-case hex characters of sha256(prefix + write code). */
    public static String readCode(String writeCode) {
        return "H" + sha256Hex(READ_PREFIX + normalize(writeCode)).substring(0, 24).toUpperCase(Locale.ROOT);
    }

    /** 64 lower-case hex characters: sha256(prefix + read code). */
    public static String listId(String readCode) {
        return sha256Hex(LIST_PREFIX + normalize(readCode));
    }

    /** Groups of five characters separated by hyphens. */
    public static String display(String code) {
        String normalized = normalize(code);
        StringBuilder out = new StringBuilder(normalized.length() + normalized.length() / 5);
        for (int index = 0; index < normalized.length(); index++) {
            if (index > 0 && index % 5 == 0) out.append('-');
            out.append(normalized.charAt(index));
        }
        return out.toString();
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
