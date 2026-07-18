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

public final class LetterServices {

    public static final String EMPTY_LINE = "\u2007";

    static final int BOOK_PAGE_WIDTH = 108;

    private static java.util.function.ToIntFunction<String> textWidth() {
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        return tr != null ? tr::getWidth : (s -> s.length() * 6);
    }

    private static SendProgressStore<LetterSendProgress> letterStore;
    private static SendProgressStore<AnnouncementSendProgress> announcementStore;

    private static volatile boolean sending = false;
    private static volatile boolean sendingAnnouncement = false;

    private LetterServices() {
    }

    public static boolean isSending() {
        return sending;
    }

    public static boolean isSendingAnnouncement() {
        return sendingAnnouncement;
    }

    public static void clearSendingState() {
        sending = false;
    }

    public record PendingLetter(LetterDraft draft, long createdAtMs) {
    }

    private static final long PENDING_TTL_MS = 10 * 60 * 1000L;
    private static volatile PendingLetter pending;

    public static void setPending(LetterDraft draft) {
        pending = new PendingLetter(draft, System.currentTimeMillis());
    }

    public static boolean hasPending() {
        PendingLetter p = pending;
        return p != null
                && System.currentTimeMillis() - p.createdAtMs() <= PENDING_TTL_MS;
    }

    public static LetterDraft pendingDraft() {
        return hasPending() ? pending.draft() : null;
    }

    public static void consumePending() {
        pending = null;
        LetterDraftCache.clear();
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

    static List<String> buildLetterLines(OttoExtraConfig config, LetterDraft draft,
                                         List<Integer> pageIndexOut) {
        PageSplitter wrapper = new PageSplitter(textWidth(), BOOK_PAGE_WIDTH, Integer.MAX_VALUE);
        List<String> out = new ArrayList<>();
        for (int p = 0; p < draft.pages.size(); p++) {
            for (String line : wrapper.wrapLines(draft.pages.get(p))) {
                out.add(config.letter.letterCommand + " "
                        + (line.isEmpty() ? EMPTY_LINE : normalizeFormattingCodes(line)));
                if (pageIndexOut != null) {
                    pageIndexOut.add(p);
                }
            }
        }
        return out;
    }

    static final int BOOK_MAX_LINES = 14;
    static final int BOOK_MAX_EFFECTIVE_CHARS = 256;

    static final int SAFE_CHAT_MESSAGE_LIMIT = 256;

    static boolean pageMode(OttoExtraConfig config) {
        return "PAGE".equalsIgnoreCase(config.letter.sendMode);
    }

    static int effectiveBookLength(String page) {
        int count = 0;
        for (int i = 0; i < page.length(); i++) {
            count += page.charAt(i) == '\n' ? 2 : 1;
        }
        return count;
    }

    static int lineCount(String page) {
        return page.isEmpty() ? 1 : page.split("\n", -1).length;
    }

    static String normalizeFormattingCodes(String raw) {
        return de.ottoextra.letter.format.LetterFormattingCodes.sectionToAmpersand(raw);
    }

    static String encodePagePayload(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\n') {
                out.append("\\n");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static List<String> buildLetterPages(OttoExtraConfig config, LetterDraft draft,
                                         List<Integer> pageIndexOut) {

        PageSplitter wrapper = new PageSplitter(textWidth(), BOOK_PAGE_WIDTH,
                config.letter.pageModeMaxLinesPerPage, config.letter.pageModeEffectiveCharBudget);
        String prefix = config.letter.letterCommand + " ";
        int payloadLimit = SAFE_CHAT_MESSAGE_LIMIT - prefix.length();
        List<String> out = new ArrayList<>();
        int pageNo = 0;
        for (String draftPage : draft.pages) {
            for (String rawPage : wrapper.split(draftPage)) {
                if (rawPage.isBlank()) {
                    continue;
                }
                String normalized = normalizeFormattingCodes(rawPage);
                String encoded = encodePagePayload(normalized);
                if (encoded.length() > payloadLimit
                        || effectiveBookLength(normalized) > BOOK_MAX_EFFECTIVE_CHARS
                        || lineCount(normalized) > BOOK_MAX_LINES) {

                    OttoExtra.LOGGER.warn("[letter] Teilseite zu lang (Buch={}, Zeilen={}, "
                            + "Payload={}) — uebersprungen.", effectiveBookLength(normalized),
                            lineCount(normalized), encoded.length());
                    hint("ottoextra.letter.page.tooLong");
                    continue;
                }
                out.add(prefix + encoded);
                if (pageIndexOut != null) {
                    pageIndexOut.add(pageNo);
                }
                pageNo++;
            }
        }
        return out;
    }

    private static List<String> buildSendCommands(OttoExtraConfig config, LetterDraft draft,
                                                  List<Integer> pageIndexOut) {
        return pageMode(config)
                ? buildLetterPages(config, draft, pageIndexOut)
                : buildLetterLines(config, draft, pageIndexOut);
    }

    private static long[] buildDelays(OttoExtraConfig config, List<Integer> pageIndex,
                                      int totalCommands) {
        if (pageMode(config)) {
            long d = Math.max(200L, config.letter.pageModeSendDelayMs);
            long[] flat = new long[totalCommands];
            for (int i = 1; i < totalCommands; i++) {
                flat[i] = d;
            }
            return flat;
        }
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

    public static void startWrite(OttoExtraConfig config, LetterDraft draft) {
        List<Integer> pageIndex = new ArrayList<>();

        boolean append = draft.meta.lockedPages > 0 || draft.meta.lockedOffset > 0;
        LetterDraft toWrite = unlockedPart(draft);
        List<String> commands = append
                ? buildLetterLines(config, toWrite, pageIndex)
                : buildSendCommands(config, toWrite, pageIndex);
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        letterStore().save(progress);

        LetterDraftCache.clear();
        long[] delays = buildDelays(config, pageIndex, commands.size());
        sending = true;
        sendingAnnouncement = false;
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            sending = false;
            letterStore().clear();
            OttoExtra.LOGGER.info("[letter] Brief {} geschrieben ({} Befehle).",
                    progress.draftId, progress.pendingCommands.size());
        }).withDelays(delays).start(0);
    }

    public static void sendPost(OttoExtraConfig config, String recipient) {
        sendCommand(config.letter.postCommand + " " + recipient);
        OttoExtra.LOGGER.info("[letter] Brief an {} zugestellt.", recipient);
    }

    public static boolean sendAnnounceSubmit(OttoExtraConfig config) {
        if (!hasSubmitCommand(config)) {
            hint("ottoextra.letter.announcement.manualSubmit");
            return false;
        }
        sendCommand(config.letter.announcementSubmitCommand.trim());
        OttoExtra.LOGGER.info("[letter] Verkündung ausgelöst.");
        return true;
    }

    private static LetterDraft unlockedPart(LetterDraft draft) {
        int locked = Math.max(0, draft.meta.lockedPages);
        int offset = Math.max(0, draft.meta.lockedOffset);
        if (locked <= 0 && offset <= 0) {
            return draft;
        }
        LetterDraft sub = new LetterDraft();
        sub.meta.draftId = draft.meta.draftId;
        sub.meta.mode = draft.meta.mode;
        sub.pages = new ArrayList<>();
        if (locked < draft.pages.size()) {
            String continuePage = draft.pages.get(locked);
            sub.pages.add(offset < continuePage.length()
                    ? continuePage.substring(offset) : "");
            for (int i = locked + 1; i < draft.pages.size(); i++) {
                sub.pages.add(draft.pages.get(i));
            }
        }
        return sub;
    }

    private static void sendCommand(String command) {
        var nh = net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler();
        if (nh != null) {
            nh.sendChatCommand(command);
        }
    }

    public static void startLetterSend(OttoExtraConfig config, LetterDraft draft,
                                       String recipient) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildSendCommands(config, draft, pageIndex);
        commands.add(config.letter.postCommand + " " + recipient);
        pageIndex.add(-1);
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.recipient = recipient;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        letterStore().save(progress);

        LetterDraftCache.clear();
        runLetterQueue(config, progress, 0, pageIndex);
    }

    public static void runLetterQueue(OttoExtraConfig config, LetterSendProgress progress,
                                      int startIndex, List<Integer> pageIndex) {
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        sending = true;
        sendingAnnouncement = false;
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            sending = false;
            letterStore().clear();
            OttoExtra.LOGGER.info("[letter] Brief an {} vollständig gesendet.",
                    progress.recipient);
        }).withDelays(delays).start(startIndex);
    }

    public static boolean hasSubmitCommand(OttoExtraConfig config) {
        return config.letter.announcementSubmitCommand != null
                && !config.letter.announcementSubmitCommand.isBlank();
    }

    public static void startAnnouncementSend(OttoExtraConfig config, LetterDraft draft) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildSendCommands(config, draft, pageIndex);
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
        LetterDraftCache.clear();
        runAnnouncementQueue(config, progress, 0, pageIndex);
    }

    public static void runAnnouncementQueue(OttoExtraConfig config,
                                            AnnouncementSendProgress progress,
                                            int startIndex, List<Integer> pageIndex) {
        boolean manualSubmit = !hasSubmitCommand(config);
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        sending = true;
        sendingAnnouncement = true;
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            announcementStore().save(progress);
        }, () -> {
            sending = false;
            announcementStore().clear();

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
