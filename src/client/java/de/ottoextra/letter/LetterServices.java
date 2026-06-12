package de.ottoextra.letter;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterSendProgress;
import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.recovery.SendProgressStore;
import de.ottoextra.letter.send.CommandSendQueue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Zentrale Letter-Dienste: Brief UND Verkündung
 * schreiben über {@code /letter}-Zeilen (gewrappte Editor-Zeilen, Leerzeilen
 * als Figure Space); Abschluss Brief = {@code /post <Empfänger>}, Verkündung =
 * konfigurierbarer Submit-Command (leer = manuell). Versand mit
 * randomisiertem Anti-Spam-Timing + Recovery-Persistenz nach jedem Command.
 */
public final class LetterServices {

    /** Leerzeilen-Platzhalter (Server schluckt echte Leerzeile). */
    public static final String EMPTY_LINE = "\u2007";

    private static SendProgressStore<LetterSendProgress> letterStore;
    private static SendProgressStore<AnnouncementSendProgress> announcementStore;

    private LetterServices() {
    }

    private static Path cacheDir() {
        return OttoExtraPaths.root().resolve(".cache").resolve("letters");
    }

    public static synchronized SendProgressStore<LetterSendProgress> letterStore() {
        if (letterStore == null) {
            letterStore = new SendProgressStore<>(
                    cacheDir().resolve("active-letter-send.json"), LetterSendProgress.class);
        }
        return letterStore;
    }

    public static synchronized SendProgressStore<AnnouncementSendProgress> announcementStore() {
        if (announcementStore == null) {
            announcementStore = new SendProgressStore<>(
                    cacheDir().resolve("active-announcement-letter-send.json"),
                    AnnouncementSendProgress.class);
        }
        return announcementStore;
    }

    // ---- /letter-Zeilenaufbau (Brief + Verkündung gemeinsam) ---------------------

    /**
     * Gewrappte {@code /letter}-Zeilen aller Seiten (Editor-Layout = gesendetes
     * Layout); Leerzeilen bleiben als Figure Space erhalten.
     * Liefert zusätzlich den Seitenindex je Command für das Seitenwechsel-Timing.
     */
    static List<String> buildLetterLines(OttoExtraConfig config, LetterDraft draft,
                                         List<Integer> pageIndexOut) {
        PageSplitter wrapper = new PageSplitter(config.letter.maxCharsPerLine,
                Integer.MAX_VALUE);
        List<String> out = new ArrayList<>();
        for (int p = 0; p < draft.pages.size(); p++) {
            for (String line : wrapper.wrapLines(draft.pages.get(p))) {
                out.add(config.letter.letterCommand + " "
                        + (line.isEmpty() ? EMPTY_LINE : line));
                if (pageIndexOut != null) {
                    pageIndexOut.add(p);
                }
            }
        }
        return out;
    }

    /** Randomisierte Delays: Zeilen-Jitter, längere Pause bei Seitenwechsel. */
    private static long[] buildDelays(OttoExtraConfig config, List<Integer> pageIndex,
                                      int totalCommands) {
        Random random = new Random();
        long[] delays = new long[totalCommands];
        for (int i = 1; i < totalCommands; i++) {
            boolean pageChange = i < pageIndex.size()
                    && !pageIndex.get(i).equals(pageIndex.get(i - 1));
            int min = pageChange ? config.letter.letterPageDelayMinMs
                    : config.letter.letterSendDelayMinMs;
            int max = pageChange ? config.letter.letterPageDelayMaxMs
                    : config.letter.letterSendDelayMaxMs;
            delays[i] = min + random.nextInt(Math.max(1, max - min));
        }
        return delays;
    }

    // ---- Brief --------------------------------------------------------------

    public static void startLetterSend(OttoExtraConfig config, LetterDraft draft,
                                       String recipient) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildLetterLines(config, draft, pageIndex);
        commands.add(config.letter.postCommand + " " + recipient);
        pageIndex.add(-1);
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.recipient = recipient;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        letterStore().save(progress);
        runLetterQueue(config, progress, 0, pageIndex);
    }

    public static void runLetterQueue(OttoExtraConfig config, LetterSendProgress progress,
                                      int startIndex, List<Integer> pageIndex) {
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            letterStore().clear();
            LetterDraftCache.clear();
            OttoExtra.LOGGER.info("[letter] Brief an {} vollständig gesendet.",
                    progress.recipient);
        }).withDelays(delays).start(startIndex);
    }

    // ---- Verkündung (über /letter, konfigurierbarer Abschluss) ----------

    /** true, wenn nach den /letter-Zeilen automatisch der Submit-Command folgt. */
    public static boolean hasSubmitCommand(OttoExtraConfig config) {
        return config.letter.announcementSubmitCommand != null
                && !config.letter.announcementSubmitCommand.isBlank();
    }

    public static void startAnnouncementSend(OttoExtraConfig config, LetterDraft draft) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildLetterLines(config, draft, pageIndex);
        if (hasSubmitCommand(config)) {
            commands.add(config.letter.announcementSubmitCommand.trim());
            pageIndex.add(-1);
        }
        AnnouncementSendProgress progress = new AnnouncementSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.pageCount = draft.pages.size();
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        announcementStore().save(progress);
        runAnnouncementQueue(config, progress, 0, pageIndex);
    }

    public static void runAnnouncementQueue(OttoExtraConfig config,
                                            AnnouncementSendProgress progress,
                                            int startIndex, List<Integer> pageIndex) {
        boolean manualSubmit = !hasSubmitCommand(config);
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            announcementStore().save(progress);
        }, () -> {
            announcementStore().clear();
            LetterDraftCache.clear();
            if (manualSubmit) {
                hint("ottoextra.letter.announcement.manualSubmit");
            }
            OttoExtra.LOGGER.info("[letter] Verkündung {} geschrieben ({} Seiten).",
                    progress.draftId, progress.pageCount);
        }).withDelays(delays).start(startIndex);
    }

    private static void hint(String key) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.translatable(key), false);
        }
    }
}
