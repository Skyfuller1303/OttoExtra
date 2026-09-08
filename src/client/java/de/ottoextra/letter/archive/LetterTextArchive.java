package de.ottoextra.letter.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** UTF-8 text with a readable header, section formatting and form-feed page breaks. */
public final class LetterTextArchive {
    private static final String HEADER = "# OttoExtra Letter TXT 1\n";
    public static final int MAX_BYTES = 262_144;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());

    private LetterTextArchive() { }

    public static String encode(List<String> pages, String author, String recipient, Instant date) {
        return HEADER + "From: " + oneLine(author) + "\nTo: " + oneLine(recipient)
                + "\nDate: " + date + "\n\n"
                + String.join("\n\f\n", pages.stream().map(page -> page.replace("\\", "\\\\")
                .replace("\f", "\\f")).toList());
    }

    public static List<String> decode(String text) throws IOException {
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        boolean archive = text.startsWith(HEADER);
        if (text.startsWith("# OttoExtra Letter TXT") && !archive) throw new IOException("Unknown TXT version");
        if (archive) {
            int start = text.indexOf("\n\n");
            if (start < 0) throw new IOException("Missing TXT header separator");
            text = text.substring(start + 2);
        }
        List<String> pages = new ArrayList<>();
        for (String page : text.split(archive ? "\n\f\n" : "\f", -1)) {
            if (archive) {
                StringBuilder decoded = new StringBuilder();
                for (int i = 0; i < page.length(); i++) {
                    char c = page.charAt(i);
                    if (c == '\\') {
                        if (++i == page.length()) throw new IOException("Incomplete TXT escape");
                        c = page.charAt(i);
                        if (c != '\\' && c != 'f') throw new IOException("Invalid TXT escape");
                        if (c == 'f') c = '\f';
                    }
                    decoded.append(c);
                }
                page = decoded.toString();
            }
            if (page.length() > 32767) throw new IOException("TXT page too large");
            pages.add(page);
        }
        if (pages.size() > 100) throw new IOException("Too many TXT pages");
        return List.copyOf(pages);
    }

    public static List<String> read(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("TXT exceeds 256 KiB");
            // REPORT malformed UTF-8 rather than silently damaging imported letters.
            return decode(StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString());
        }
    }

    public static Path write(Path directory, String prefix, List<String> pages,
                             String author, String recipient, Instant date) throws IOException {
        byte[] bytes = encode(pages, author, recipient, date).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("TXT exceeds 256 KiB");
        decode(new String(bytes, StandardCharsets.UTF_8));
        Files.createDirectories(directory);
        String stem = safeName(prefix) + "_" + DATE.format(date);
        for (int i = 0; i < 10000; i++) {
            Path file = directory.resolve(stem + (i == 0 ? "" : "_" + i) + ".txt");
            try {
                Files.write(file, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return file;
            } catch (FileAlreadyExistsException ignored) { }
        }
        throw new IOException("No free TXT filename");
    }

    private static String safeName(String name) {
        String clean = oneLine(name).replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .replaceAll("[^A-Za-z0-9_-]", "_");
        return clean.isBlank() ? "unknown" : clean.substring(0, Math.min(48, clean.length()));
    }

    private static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("[\\p{Cntrl}]", " ");
    }
}
