package de.ottoextra.letter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.ottoextra.config.OttoExtraPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Kleine Liste zuletzt angeschriebener Empfänger (neueste zuerst) im Briefordner. */
public final class RecentRecipients {

    public static final int MAX = 20;

    private static final Gson GSON = new Gson();

    private RecentRecipients() {
    }

    private static Path file() {
        return OttoExtraPaths.lettersDir().resolve("recent-recipients.json");
    }

    public static synchronized List<String> load() {
        try {
            Path file = file();
            if (!Files.exists(file)) {
                return List.of();
            }
            List<String> parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    new TypeToken<List<String>>() { }.getType());
            return parsed == null ? List.of() : record(parsed, null);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static synchronized void record(String name) {
        List<String> updated = record(load(), name);
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(updated), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            // Die Liste ist nur eine Komfortfunktion.
        }
    }

    /** Stellt {@code name} an den Anfang, entfernt Duplikate (ohne Groß-/Kleinschreibung), kürzt auf {@link #MAX}. */
    public static List<String> record(List<String> recent, String name) {
        List<String> out = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            out.add(name.trim());
        }
        for (String existing : recent == null ? List.<String>of() : recent) {
            if (existing == null || existing.isBlank()) {
                continue;
            }
            boolean duplicate = out.stream().anyMatch(known -> known.equalsIgnoreCase(existing.trim()));
            if (!duplicate) {
                out.add(existing.trim());
            }
        }
        return out.size() > MAX ? new ArrayList<>(out.subList(0, MAX)) : out;
    }

    /** Zuletzt benutzte zuerst (in Verlaufsreihenfolge), dann Online-Spieler, dann alphabetisch. */
    public static List<String> order(List<String> accounts, List<String> recent,
                                     Predicate<String> online) {
        List<String> recents = recent == null ? List.of() : recent;
        Comparator<String> byName = Comparator.comparing((String s) -> s.toLowerCase(Locale.ROOT));
        List<String> sorted = new ArrayList<>(accounts == null ? List.of() : accounts);
        sorted.sort(Comparator
                .comparingInt((String account) -> recentIndex(recents, account))
                .thenComparing((String account) -> !online.test(account))
                .thenComparing(byName));
        return sorted;
    }

    private static int recentIndex(List<String> recent, String account) {
        for (int i = 0; i < recent.size(); i++) {
            if (recent.get(i).equalsIgnoreCase(account)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }
}
