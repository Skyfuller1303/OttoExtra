package de.ottoextra.letter;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterSendProgress;
import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.recovery.SendProgressStore;
import de.ottoextra.letter.recovery.LetterSendContext;
import de.ottoextra.letter.send.ActiveCommandSendQueue;
import de.ottoextra.letter.send.LetterCommandPlan;
import de.ottoextra.letter.send.LetterV2Planner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LetterServices {

    public static final String EMPTY_LINE = LetterV2Planner.EMPTY_LINE;

    static final int BOOK_PAGE_WIDTH = 108;

    private static java.util.function.ToIntFunction<String> textWidth() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        var tr = client != null ? client.textRenderer : null;
        return tr != null ? tr::getWidth : (s -> s.length() * 6);
    }

    private static SendProgressStore<LetterSendProgress> letterStore;
    private static SendProgressStore<AnnouncementSendProgress> announcementStore;

    private static final ActiveCommandSendQueue activeSend = new ActiveCommandSendQueue();
    private static LetterSendContext preparedLetter;

    private LetterServices() {
    }

    public static boolean isSending() {
        return activeSend.isSending();
    }

    public static boolean isSendingAnnouncement() {
        return activeSend.isAnnouncement();
    }

    public static void clearSendingState() {
        activeSend.cancel();
        preparedLetter = null;
        pending = null;
    }

    private static boolean canStartSend() {
        if (isSending() || de.ottoextra.letter.calibration.LetterCalibrationService.isSending()) {
            hint("ottoextra.letter.write.wait");
            return false;
        }
        var client = net.minecraft.client.MinecraftClient.getInstance();
        return client.player != null && client.getNetworkHandler() != null;
    }

    private static boolean checkWriteItem(OttoExtraConfig config, LetterDraft draft) {
        draft.repair();
        LetterWriteItemGuard.Result result = LetterWriteItemGuard.check(config, draft);
        switch (result) {
            case HOLD_STARTED_LETTER -> hint("ottoextra.letter.write.holdStarted");
            case HOLD_SINGLE_SUPPLY -> hint("ottoextra.letter.write.holdSupply", config.letter.triggerItemName);
            case REMOVE_EXTRA_SUPPLIES -> hint("ottoextra.letter.write.extraSupplies", config.letter.triggerItemName);
            default -> { }
        }
        return result == LetterWriteItemGuard.Result.ALLOW;
    }

    private static boolean canContinue(OttoExtraConfig config, LetterSendContext context) {
        if (LetterWriteItemGuard.matches(config, context)) {
            return true;
        }
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(
                    "Briefversand angehalten: Server, Spieler, aktiver Slot oder Briefinhalt stimmt nicht mehr. "
                            + "Der Entwurf bleibt gespeichert."), false);
        }
        return false;
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
        return LetterSendMode.from(config.letter.sendMode) == LetterSendMode.PAGE;
    }

    static boolean v2Mode(OttoExtraConfig config) {
        return LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2;
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

    private enum SendPurpose {
        LETTER,
        ANNOUNCEMENT,
        CALIBRATION
    }

    private static LetterCommandPlan buildPlan(OttoExtraConfig config, LetterDraft draft,
                                               SendPurpose purpose) {
        return buildPlan(config, draft, purpose, List.of());
    }

    private static LetterCommandPlan buildPlan(OttoExtraConfig config, LetterDraft draft,
                                               SendPurpose purpose,
                                               List<String> initialServerPages) {
        // Editor layout/timing modes do not change the server's Courier 2.1.0 protocol.
        List<String> sources = purpose != SendPurpose.CALIBRATION && v2Mode(config)
                ? V2DraftFlow.sourcePages(draft) : draft.pages;
        return LetterV2Planner.planDocument(config.letter.letterCommand, sources,
                textWidth(), initialServerPages);
    }

    private static List<String> buildSendCommands(OttoExtraConfig config, LetterDraft draft,
                                                  List<Integer> pageIndexOut) {
        LetterCommandPlan plan = buildPlan(config, draft, SendPurpose.LETTER);
        if (pageIndexOut != null) {
            plan.entries().forEach(entry -> pageIndexOut.add(entry.predictedPage()));
        }
        return new ArrayList<>(plan.commands());
    }

    /** Erzeugt ausschließlich die beschreibenden /letter-Befehle, niemals eine Zustellung. */
    public static List<String> buildCalibrationCommands(OttoExtraConfig config,
                                                        LetterDraft draft) {
        return buildCalibrationPlan(config, draft).commands();
    }

    public static LetterCommandPlan buildCalibrationPlan(OttoExtraConfig config,
                                                          LetterDraft draft) {
        return buildPlan(config, draft, SendPurpose.CALIBRATION);
    }

    static LetterCommandPlan buildAnnouncementPlanForTest(OttoExtraConfig config,
                                                           LetterDraft draft) {
        return buildPlan(config, draft, SendPurpose.ANNOUNCEMENT);
    }

    public static List<String> previewPages(OttoExtraConfig config, LetterDraft draft) {
        return previewPlan(config, draft).predictedPages();
    }

    /** Shared prediction, including warnings, for the editor count and book preview. */
    public static LetterCommandPlan previewPlan(OttoExtraConfig config, LetterDraft draft) {
        if (draft.meta.importedFromBook && draft.meta.lockedPages >= 0) {
            LetterDraft remaining = unlockedPart(draft);
            return LetterV2Planner.planDocument(config.letter.letterCommand, remaining.pages,
                    textWidth(), draft.meta.courierInitialPages);
        }
        return buildPlan(config, draft, SendPurpose.LETTER);
    }

    private static long[] buildDelays(OttoExtraConfig config, List<Integer> pageIndex,
                                      int totalCommands) {
        return buildDelays(config, pageIndex, totalCommands,
                LetterSendMode.from(config.letter.sendMode));
    }

    private static long[] buildDelays(OttoExtraConfig config, List<Integer> pageIndex,
                                      int totalCommands, LetterSendMode mode) {
        if (mode == LetterSendMode.PAGE || mode == LetterSendMode.V2) {
            long d = Math.max(200L, config.letter.pageModeSendDelayMs);
            long[] flat = new long[totalCommands];
            for (int i = 1; i < totalCommands; i++) {
                boolean pageChange = i < pageIndex.size()
                        && !pageIndex.get(i).equals(pageIndex.get(i - 1));
                if (mode == LetterSendMode.V2 && pageChange) {
                    int min = config.letter.letterPageDelayMinMs;
                    int max = config.letter.letterPageDelayMaxMs;
                    flat[i] = min + new Random().nextInt(Math.max(1, max - min));
                } else {
                    flat[i] = d;
                }
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

    public static boolean startWrite(OttoExtraConfig config, LetterDraft draft) {
        return startWrite(config, draft, SendPurpose.LETTER, null);
    }

    public static boolean startWrite(OttoExtraConfig config, LetterDraft draft,
                                     Runnable onComplete) {
        return startWrite(config, draft, SendPurpose.LETTER, onComplete);
    }

    public static boolean startAnnouncementWrite(OttoExtraConfig config, LetterDraft draft) {
        return startWrite(config, draft, SendPurpose.ANNOUNCEMENT, null);
    }

    private static boolean startWrite(OttoExtraConfig config, LetterDraft draft,
                                      SendPurpose purpose, Runnable onComplete) {
        if (!canStartSend()) {
            return false;
        }
        if (!checkWriteItem(config, draft)) {
            return false;
        }
        List<Integer> pageIndex = new ArrayList<>();

        boolean append = draft.meta.lockedPages > 0 || draft.meta.lockedOffset > 0;
        LetterDraft toWrite = unlockedPart(draft);
        List<String> initialPages = draft.meta.courierInitialPages == null
                ? List.of() : draft.meta.courierInitialPages;
        LetterCommandPlan plan = append
                ? LetterV2Planner.planDocument(config.letter.letterCommand, toWrite.pages,
                textWidth(), initialPages)
                : buildPlan(config, toWrite, purpose);
        debugPlan(plan);
        if (!plan.warnings().isEmpty()) {
            hintForWarnings(plan.warnings());
            return false;
        }
        List<String> commands = new ArrayList<>(plan.commands());
        plan.entries().forEach(entry -> pageIndex.add(entry.predictedPage()));
        if (commands.isEmpty()) {
            return false;
        }
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        progress.context = LetterWriteItemGuard.capture(config, draft);
        letterStore().save(progress);

        LetterDraftCache.save(draft);
        preparedLetter = null;
        LetterSendMode effectiveMode = LetterSendMode.from(config.letter.sendMode);
        long[] delays = buildDelays(config, pageIndex, commands.size(), effectiveMode);
        activeSend.start(progress.pendingCommands, delays, 0,
                purpose == SendPurpose.ANNOUNCEMENT, sent -> {
            progress.sentCommands = sent;
            progress.context.sent(progress.pendingCommands.get(sent - 1));
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            letterStore().clear();
            clearWrittenDraftCache(progress.context);
            preparedLetter = progress.context;
            DebugLog.debug("[letter] {} {} geschrieben ({} Befehle).",
                    purpose == SendPurpose.ANNOUNCEMENT ? "Verkündung" : "Brief",
                    progress.draftId, progress.pendingCommands.size());
            if (onComplete != null) {
                onComplete.run();
            }
        }, () -> canContinue(config, progress.context));
        return true;
    }

    public static boolean sendPost(OttoExtraConfig config, String recipient) {
        if (!validRecipient(recipient) || !canStartSend() || !canContinue(config, preparedLetter)) {
            return false;
        }
        sendCommand(config.letter.postCommand + " " + recipient);
        preparedLetter = null;
        DebugLog.debug("[letter] Zustellbefehl für {} gesendet.", recipient);
        return true;
    }

    static boolean validRecipient(String recipient) {
        return recipient != null && recipient.matches("[A-Za-z0-9_]{1,16}");
    }

    private static void clearWrittenDraftCache(LetterSendContext context) {
        // /post and /verkünden have no ACK here; permission/money errors must retain the draft.
        if (context.submitted) {
            return;
        }
        LetterDraft cached = LetterDraftCache.load();
        LetterDraft written = context.draft;
        if (written != null && cached.meta.draftId.equals(written.meta.draftId)
                && cached.meta.updatedAtMs == written.meta.updatedAtMs
                && cached.pages.equals(written.pages)) {
            LetterDraftCache.clear();
        }
    }

    public static boolean sendAnnounceSubmit(OttoExtraConfig config) {
        if (!hasSubmitCommand(config)) {
            hint("ottoextra.letter.announcement.manualSubmit");
            return false;
        }
        if (!canStartSend() || !canContinue(config, preparedLetter)) {
            return false;
        }
        sendCommand(config.letter.announcementSubmitCommand.trim());
        preparedLetter = null;
        DebugLog.debug("[letter] Verkündung ausgelöst.");
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
        if (!validRecipient(recipient) || !canStartSend() || !checkWriteItem(config, draft)) {
            return;
        }
        List<Integer> pageIndex = new ArrayList<>();
        boolean append = draft.meta.lockedPages > 0 || draft.meta.lockedOffset > 0;
        LetterDraft remaining = unlockedPart(draft);
        LetterCommandPlan plan = append
                ? LetterV2Planner.planDocument(config.letter.letterCommand, remaining.pages,
                textWidth(), draft.meta.courierInitialPages)
                : buildPlan(config, draft, SendPurpose.LETTER);
        debugPlan(plan);
        if (!plan.warnings().isEmpty()) {
            hintForWarnings(plan.warnings());
            return;
        }
        List<String> commands = new ArrayList<>(plan.commands());
        if (commands.isEmpty()) {
            return;
        }
        plan.entries().forEach(entry -> pageIndex.add(entry.predictedPage()));
        commands.add(config.letter.postCommand + " " + recipient);
        pageIndex.add(-1);
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.recipient = recipient;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        progress.context = LetterWriteItemGuard.capture(config, draft);
        letterStore().save(progress);

        LetterDraftCache.save(draft);
        runLetterQueue(config, progress, 0, pageIndex);
    }

    public static boolean runLetterQueue(OttoExtraConfig config, LetterSendProgress progress,
                                      int startIndex, List<Integer> pageIndex) {
        if (!canStartSend() || progress == null
                || !LetterSendContext.validCommands(progress.pendingCommands, startIndex)
                || startIndex != progress.sentCommands || !canContinue(config, progress.context)) {
            return false;
        }
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        preparedLetter = null;
        return activeSend.start(progress.pendingCommands, delays, startIndex, false, sent -> {
            progress.sentCommands = sent;
            progress.context.sent(progress.pendingCommands.get(sent - 1));
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            letterStore().clear();
            clearWrittenDraftCache(progress.context);
            preparedLetter = progress.context.submitted ? null : progress.context;
            DebugLog.debug("[letter] Briefbefehle an {} gesendet.",
                    progress.recipient);
        }, () -> canContinue(config, progress.context));
    }

    public static boolean hasSubmitCommand(OttoExtraConfig config) {
        return config.letter.announcementSubmitCommand != null
                && !config.letter.announcementSubmitCommand.isBlank();
    }

    /** Plans exactly the pages shown/optimized in the announcement preflight. */
    public static LetterCommandPlan buildPreflightAnnouncementPlan(OttoExtraConfig config, LetterDraft draft) {
        if (draft.meta.lockedPages > 0 || draft.meta.lockedOffset > 0) {
            return LetterV2Planner.planDocument(config.letter.letterCommand, unlockedPart(draft).pages,
                    textWidth(), draft.meta.courierInitialPages);
        }
        return LetterV2Planner.planDocument(config.letter.letterCommand, draft.pages, textWidth());
    }

    public static void startAnnouncementSend(OttoExtraConfig config, LetterDraft draft) {
        tryStartAnnouncementSend(config, draft);
    }

    /** Raw pages of the already written letter, or null; does not access Minecraft. */
    public static List<String> snapshotPreparedPages(LetterDraft draft) {
        if (preparedLetter == null || preparedLetter.draft == null || draft == null
                || draft.meta == null || preparedLetter.draft.meta == null
                || !java.util.Objects.equals(preparedLetter.draft.meta.draftId, draft.meta.draftId)
                || preparedLetter.expectedPages == null) {
            return null;
        }
        return List.copyOf(preparedLetter.expectedPages);
    }

    public static boolean hasPreparedLetter(OttoExtraConfig config, LetterDraft draft) {
        return snapshotPreparedPages(draft) != null && LetterWriteItemGuard.matches(config, preparedLetter);
    }

    public static boolean tryStartAnnouncementSend(OttoExtraConfig config, LetterDraft draft) {
        if (!canStartSend()) {
            return false;
        }
        List<String> preparedPages = snapshotPreparedPages(draft);
        if (preparedPages != null) {
            return LetterV2Planner.displayPages(preparedPages).equals(draft.pages)
                    && sendAnnounceSubmit(config);
        }
        if (!checkWriteItem(config, draft)) {
            return false;
        }
        List<Integer> pageIndex = new ArrayList<>();
        LetterCommandPlan plan = buildPreflightAnnouncementPlan(config, draft);
        debugPlan(plan);
        if (!plan.warnings().isEmpty()) {
            hintForWarnings(plan.warnings());
            return false;
        }
        List<String> commands = new ArrayList<>(plan.commands());
        if (commands.isEmpty()) {
            return false;
        }
        plan.entries().forEach(entry -> pageIndex.add(entry.predictedPage()));
        if (hasSubmitCommand(config)) {
            commands.add(config.letter.announcementSubmitCommand.trim());
            pageIndex.add(-1);
        }
        AnnouncementSendProgress progress = new AnnouncementSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.pageCount = plan.predictedPages().size();
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        progress.context = LetterWriteItemGuard.capture(config, draft);
        announcementStore().save(progress);
        LetterDraftCache.save(draft);
        return runAnnouncementQueue(config, progress, 0, pageIndex);
    }

    public static boolean runAnnouncementQueue(OttoExtraConfig config,
                                            AnnouncementSendProgress progress,
                                            int startIndex, List<Integer> pageIndex) {
        if (!canStartSend() || progress == null
                || !LetterSendContext.validCommands(progress.pendingCommands, startIndex)
                || startIndex != progress.sentCommands || !canContinue(config, progress.context)) {
            return false;
        }
        boolean manualSubmit = !hasSubmitCommand(config);
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        preparedLetter = null;
        return activeSend.start(progress.pendingCommands, delays, startIndex, true, sent -> {
            progress.sentCommands = sent;
            progress.context.sent(progress.pendingCommands.get(sent - 1));
            progress.updatedAtMs = System.currentTimeMillis();
            announcementStore().save(progress);
        }, () -> {
            announcementStore().clear();
            clearWrittenDraftCache(progress.context);
            preparedLetter = progress.context.submitted ? null : progress.context;

            if (manualSubmit) {
                hint("ottoextra.letter.announcement.manualSubmit");
            }
            DebugLog.debug("[letter] Verkündung {} geschrieben ({} Seiten).",
                    progress.draftId, progress.pageCount);
        }, () -> canContinue(config, progress.context));
    }

    private static void hint(String key, Object... args) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.translatable(key, args), false);
        }
    }

    private static void hintForWarnings(List<String> warnings) {
        if (warnings.contains(LetterV2Planner.VISIBLE_PADDING_WARNING)) {
            hint("ottoextra.letter.v2.visiblePadding");
        } else if (warnings.contains(LetterV2Planner.PAGE_CONTENT_WARNING)) {
            hint("ottoextra.letter.v2.pageUnsendable");
        } else {
            hint("ottoextra.letter.v2.literalNewline");
        }
    }

    private static void debugPlan(LetterCommandPlan plan) {
        if (plan == null || !DebugLog.isLogging()) {
            return;
        }
        DebugLog.debug("[letter-v2] Plan: {} sichtbare Seiten, {} Befehle, Warnungen {}.",
                plan.predictedPages().size(), plan.entries().size(), plan.warnings());
        for (int i = 0; i < plan.entries().size(); i++) {
            LetterCommandPlan.PlannedCommand entry = plan.entries().get(i);
            DebugLog.debug("[letter-v2] #{} {} -> Serverseite {}, Befehlslänge {}.",
                    i + 1, entry.transport(), entry.predictedPage(), entry.command().length());
        }
    }
}
