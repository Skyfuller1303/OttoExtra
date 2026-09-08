package de.ottoextra.letter.calibration;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.send.CommandSendQueue;
import de.ottoextra.letter.send.LetterCommandPlan;
import de.ottoextra.letter.send.LetterV2Planner;
import de.ottoextra.letter.ui.WrittenLetterImport;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class LetterCalibrationService {
    public static final String SUITE_LEGACY = "LEGACY_16";
    public static final String SUITE_V2 = "V2_HYBRID_8";
    public static final String SUITE_V2_REFINE = "V2_REFINE_6";
    public static final String SUITE_V2_FINAL = "V2_FINAL_8";
    public static final String SUITE_V2_BOUNDARY = "V2_BOUNDARY_15";
    private static LetterCalibrationStore store;
    private static LetterCalibrationSession active;
    private static CommandSendQueue queue;
    private static String captureCandidate = "";
    private static long captureCandidateSince;

    private LetterCalibrationService() {
    }

    public static synchronized boolean isSending() {
        return queue != null;
    }

    public static synchronized LetterCalibrationSession activeOrCreate(OttoExtraConfig config) {
        requireDebug();
        String suiteId = active == null ? store().loadSelectedSuite() : active.suiteId;
        if (active == null) {
            String id = store().loadActiveSessionId(suiteId);
            active = id.isBlank() ? null : store().load(id);
            recoverInterruptedSend();
        }
        if (active == null || active.completed) {
            active = create(config, suiteId);
            store().saveActiveSessionId(active.suiteId, active.sessionId);
            store().save(active);
        }
        return active;
    }

    private static synchronized LetterCalibrationSession activeOrCreate(
            OttoExtraConfig config, String suiteId) {
        requireDebug();
        if (active != null && !suiteId.equals(active.suiteId)) {
            restoreMode(config, active);
            store().save(active);
            active = null;
        }
        if (active == null) {
            String id = store().loadActiveSessionId(suiteId);
            active = id.isBlank() ? null : store().load(id);
            recoverInterruptedSend();
        }
        if (active == null || active.completed || !suiteId.equals(active.suiteId)) {
            active = create(config, suiteId);
            store().saveActiveSessionId(active.suiteId, active.sessionId);
            store().save(active);
        }
        return active;
    }

    public static synchronized LetterCalibrationSession active() {
        if (active == null) {
            String suiteId = store().loadSelectedSuite();
            String id = store().loadActiveSessionId(suiteId);
            active = id.isBlank() ? null : store().load(id);
            recoverInterruptedSend();
        }
        return active;
    }

    public static synchronized LetterCalibrationSession.CaseResult current() {
        LetterCalibrationSession session = active();
        if (session == null || session.cases.isEmpty()) {
            return null;
        }
        session.currentCase = Math.max(0, Math.min(session.currentCase,
                session.cases.size() - 1));
        return session.cases.get(session.currentCase);
    }

    public static synchronized void move(int direction) {
        LetterCalibrationSession session = active();
        if (session == null) {
            return;
        }
        session.currentCase = Math.max(0, Math.min(session.cases.size() - 1,
                session.currentCase + direction));
        store().save(session);
    }

    public static synchronized boolean sendCurrent(OttoExtraConfig config) {
        requireDebug();
        LetterCalibrationSession session = activeOrCreate(config);
        LetterCalibrationSession.CaseResult result = current();
        MinecraftClient client = MinecraftClient.getInstance();
        if (result == null || client.player == null || client.getNetworkHandler() == null) {
            return false;
        }
        LetterDraft draft = new LetterDraft();
        draft.meta.draftId = "OE-" + result.id;
        draft.pages = new ArrayList<>(result.sourcePages);
        return sendDraft(config, session, result, draft);
    }

    public static synchronized boolean writeFromEditor(OttoExtraConfig config,
                                                       LetterDraft draft) {
        requireDebug();
        LetterCalibrationSession session = activeOrCreate(config);
        LetterCalibrationSession.CaseResult result = current();
        if (result == null || !isCalibrationDraft(draft)) {
            return false;
        }
        if (!LetterBoundaryCalibration.isBoundarySuite(session.suiteId)) {
            result.sourcePages = new ArrayList<>(draft.pages);
            result.sourceRuns = runPages(result.sourcePages);
        }
        return sendDraft(config, session, result, draft);
    }

    private static boolean sendDraft(OttoExtraConfig config, LetterCalibrationSession session,
                                     LetterCalibrationSession.CaseResult result,
                                     LetterDraft draft) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null || queue != null
                || LetterServices.isSending()) {
            return false;
        }
        LetterCommandPlan plan = LetterBoundaryCalibration.isBoundarySuite(session.suiteId)
                ? LetterBoundaryCalibration.rawPlan(config.letter.letterCommand, result)
                : LetterServices.buildCalibrationPlan(config, draft);
        List<String> commands = plan.commands();
        if (!plan.sendable() || commands.stream().anyMatch(c -> !safe(config, c))) {
            client.player.sendMessage(Text.literal(
                    "§c[OttoExtra] Kalibrierung blockiert: unsicherer Command."), false);
            return false;
        }
        result.commands = new ArrayList<>(commands);
        result.transports = new ArrayList<>(plan.transports());
        result.warnings = new ArrayList<>(plan.warnings());
        if (LetterBoundaryCalibration.isBoundarySuite(session.suiteId)) {
            result.commandCount = commands.size();
        }
        result.intendedPages = isV2Suite(session.suiteId)
                ? new ArrayList<>(plan.predictedPages()) : decodeIntendedPages(config, commands);
        result.differences = isV2Suite(session.suiteId)
                ? new ArrayList<>()
                : prefixed("CLIENT_", LetterCalibrationComparator.compare(
                result.sourcePages, result.intendedPages));
        result.status = "SENDING";
        captureCandidate = "";
        captureCandidateSince = 0L;
        result.sentAt = System.currentTimeMillis();
        store().save(session);
        DebugLog.debug("[letter-calibration] {} Quelle={} Commands={}", result.id,
                result.sourcePages, commands);
        long[] delays = new long[commands.size()];
        for (int i = 1; i < delays.length; i++) {
            delays[i] = Math.max(200L, config.letter.pageModeSendDelayMs);
        }
        queue = new CommandSendQueue(commands, sent -> {
            synchronized (LetterCalibrationService.class) {
                result.status = "SENDING_" + sent + "_OF_" + commands.size();
                store().save(session);
            }
        }, () -> {
            synchronized (LetterCalibrationService.class) {
                result.status = "SENT";
                result.sendCompletedAt = System.currentTimeMillis();
                queue = null;
                store().save(session);
            }
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§a[OttoExtra] " + result.id
                        + " beschrieben und gespeichert. Der Serverbrief wird automatisch erfasst."),
                        false);
            }
        }).withDelays(delays).onInterrupted(LetterCalibrationService::interruptSending);
        queue.start(0);
        return true;
    }

    public static synchronized boolean isCalibrationDraft(LetterDraft draft) {
        LetterCalibrationSession.CaseResult result = current();
        return draft != null && draft.meta != null && result != null
                && ("OE-" + result.id).equals(draft.meta.draftId);
    }

    public static synchronized boolean openNumbered(OttoExtraConfig config, int number) {
        return openNumbered(config, number, SUITE_LEGACY);
    }

    public static synchronized boolean openV2Numbered(OttoExtraConfig config, int number) {
        return openNumbered(config, number, SUITE_V2);
    }

    public static synchronized boolean openV2RefineNumbered(OttoExtraConfig config, int number) {
        return openNumbered(config, number, SUITE_V2_REFINE);
    }

    public static synchronized boolean openV2FinalNumbered(OttoExtraConfig config, int number) {
        return openNumbered(config, number, SUITE_V2_FINAL);
    }

    public static synchronized boolean openV2BoundaryNumbered(OttoExtraConfig config,
                                                               int number) {
        return openNumbered(config, number, SUITE_V2_BOUNDARY);
    }

    private static boolean openNumbered(OttoExtraConfig config, int number, String suiteId) {
        DebugLog.setEnabled(true);
        requireDebug();
        List<LetterCalibrationCorpus.TestCase> corpus = corpus(suiteId);
        if (number < 1 || number > corpus.size()) {
            feedback("§c[OttoExtra] Testnummer muss zwischen 1 und "
                    + corpus.size() + " liegen.");
            return false;
        }
        LetterCalibrationSession session = activeOrCreate(config, suiteId);
        captureHeldStructured(config, false);
        int index = number - 1;
        if (index > 0 && !captured(session.cases.get(index - 1))) {
            feedback("§c[OttoExtra] Bitte den fertigen Brief von Test " + index
                    + " in der Hand halten und den Command erneut ausführen.");
            return false;
        }
        String forcedMode = isV2Suite(suiteId) ? "V2" : "PAGE";
        config.letter.sendMode = forcedMode;
        config.save();
        session.sendMode = forcedMode;
        session.maxLinesPerPage = isV2Suite(suiteId)
                ? LetterV2Planner.BOOK_MAX_LINES : config.letter.pageModeMaxLinesPerPage;
        session.currentCase = index;
        LetterCalibrationSession.CaseResult result = session.cases.get(index);
        result.status = "PENDING";
        result.commands = new ArrayList<>();
        result.transports = new ArrayList<>();
        result.warnings = new ArrayList<>();
        result.intendedPages = new ArrayList<>();
        result.observedPages = new ArrayList<>();
        result.observedRuns = new ArrayList<>();
        result.differences = new ArrayList<>();
        result.observedPageCount = 0;
        result.screenshot = "";
        if (LetterBoundaryCalibration.isBoundarySuite(suiteId)) {
            LetterBoundaryCalibration.prepare(session, index, textWidth());
        } else {
            result.sourcePages = new ArrayList<>(corpus.get(index).pages());
            result.sourceRuns = runPages(result.sourcePages);
        }
        store().save(session);
        feedback("§a[OttoExtra] Test " + number
                + " gewählt; " + forcedMode
                + " ist aktiv. Jetzt Pergament und Feder öffnen und "
                + "den Debug-Testtext-Button drücken.");
        return true;
    }

    public static synchronized String populateCurrentDraft(LetterDraft draft) {
        requireDebug();
        LetterCalibrationSession.CaseResult result = current();
        if (draft == null || result == null) {
            return "";
        }
        draft.repair();
        draft.meta.draftId = "OE-" + result.id;
        draft.meta.lockedPages = 0;
        draft.meta.lockedOffset = 0;
        draft.pages = new ArrayList<>(result.sourcePages);
        result.sourcePages = new ArrayList<>(draft.pages);
        result.sourceRuns = runPages(result.sourcePages);
        result.status = "PENDING";
        store().save(active());
        DebugLog.debug("[letter-calibration] {} in geöffneten Briefeditor eingesetzt.",
                result.id);
        return result.id;
    }

    public static synchronized boolean hasActiveCase() {
        return DebugLog.isEnabled() && active() != null && !active().completed
                && current() != null;
    }

    public static synchronized String activeSendMode() {
        LetterCalibrationSession session = active();
        return session == null || session.sendMode == null || session.sendMode.isBlank()
                ? "?" : session.sendMode;
    }

    static boolean safe(OttoExtraConfig config, String command) {
        String letter = cleanCommand(config.letter.letterCommand);
        String post = cleanCommand(config.letter.postCommand);
        String announce = cleanCommand(config.letter.announcementSubmitCommand);
        String root = command == null ? "" : command.strip().split("\\s+", 2)[0]
                .toLowerCase(Locale.ROOT);
        return command != null && !letter.isBlank() && root.equals(letter)
                && !root.equals(post) && !root.equals(announce)
                && command.indexOf('§') < 0;
    }

    private static String cleanCommand(String command) {
        if (command == null) {
            return "";
        }
        return command.strip().replaceFirst("^/", "").split("\\s+", 2)[0]
                .toLowerCase(Locale.ROOT);
    }

    public static synchronized boolean canCaptureHeldBook() {
        return matchingHeldResult() != null;
    }

    public static synchronized boolean captureHeldBook(Screen bookScreen) {
        requireDebug();
        LetterCalibrationSession.CaseResult result = matchingHeldResult();
        if (result == null) {
            feedback("§c[OttoExtra] Das gehaltene Buch gehört nicht zum aktuellen Test.");
            return false;
        }
        LetterCalibrationSession session = active();
        boolean newlyCaptured = "SENT".equals(result.status);
        if (newlyCaptured) {
            captureResultData(session, result);
        }
        result.status = "CAPTURED";
        LetterBoundaryCalibration.recordObservation(session, result);
        if (newlyCaptured) {
            advanceAfterCapture(OttoExtraConfig.active(), session, result);
        }
        Path screenshot = store().screenshot(session.sessionId, result.id);
        result.screenshot = "screenshots/" + result.id + ".png";
        store().save(session);
        takeScreenshot(screenshot);
        feedback("§a[OttoExtra] " + result.id + " erfasst: "
                + result.differences.size() + " Abweichung(en)." +
                (session.completed ? " Messreihe vollständig." : ""));
        DebugLog.debug("[letter-calibration] {} erfasst: {}", result.id,
                result.differences);
        return true;
    }

    public static synchronized void tick(OttoExtraConfig config) {
        if (!DebugLog.isEnabled()) {
            return;
        }
        LetterCalibrationSession.CaseResult result = current();
        if (result == null || !"SENT".equals(result.status)
                || System.currentTimeMillis() - result.sendCompletedAt < 1500L) {
            return;
        }
        LetterCalibrationSession.CaseResult held = matchingHeldResult();
        if (held != result) {
            return;
        }
        ItemStack stack = WrittenLetterImport.heldWrittenBookStack();
        WrittenBookContentComponent book = stack == null ? null
                : stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (book == null) {
            return;
        }
        String candidate = result.id + "\u0000" + String.join("\u0000",
                WrittenLetterImport.toSectionPages(book));
        if (!candidate.equals(captureCandidate)) {
            captureCandidate = candidate;
            captureCandidateSince = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - captureCandidateSince < 1000L) {
            return;
        }
        captureHeldStructured(config, true);
    }

    private static boolean captureHeldStructured(OttoExtraConfig config, boolean notify) {
        LetterCalibrationSession session = active();
        LetterCalibrationSession.CaseResult result = matchingHeldResult();
        if (session == null || result == null || !"SENT".equals(result.status)) {
            return false;
        }
        captureResultData(session, result);
        result.status = "CAPTURED_NO_SCREENSHOT";
        LetterBoundaryCalibration.recordObservation(session, result);
        captureCandidate = "";
        captureCandidateSince = 0L;
        advanceAfterCapture(config, session, result);
        store().save(session);
        if (notify) {
            feedback("§a[OttoExtra] " + result.id + " automatisch gespeichert. "
                    + (session.completed ? "Messreihe vollständig."
                    : "Jetzt " + nextCommand(session) + " verwenden."));
        }
        return true;
    }

    private static void captureResultData(LetterCalibrationSession session,
                                          LetterCalibrationSession.CaseResult result) {
        ItemStack stack = WrittenLetterImport.heldWrittenBookStack();
        WrittenBookContentComponent book = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        result.observedPages = new ArrayList<>(WrittenLetterImport.toSectionPages(book));
        result.observedRuns = runPages(result.observedPages);
        result.observedPageCount = result.observedPages.size();
        List<String> differences = result.differences.stream()
                .filter(value -> !value.startsWith("SERVER_")).collect(
                        java.util.stream.Collectors.toCollection(ArrayList::new));
        differences.addAll(prefixed("SERVER_", LetterCalibrationComparator.compare(
                result.intendedPages.isEmpty() ? result.sourcePages : result.intendedPages,
                result.observedPages)));
        result.differences = differences;
        result.capturedAt = System.currentTimeMillis();
        DebugLog.debug("[letter-calibration] {} automatisch erfasst: {}", result.id,
                result.differences);
    }

    private static LetterCalibrationSession.CaseResult matchingHeldResult() {
        LetterCalibrationSession session = active();
        ItemStack stack = WrittenLetterImport.heldWrittenBookStack();
        WrittenBookContentComponent book = stack == null ? null
                : stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (session == null || book == null) {
            return null;
        }
        List<String> plainPages = WrittenLetterImport.toSectionPages(book).stream()
                .map(LetterMarkupSemantics::plain).toList();
        for (LetterCalibrationSession.CaseResult candidate : session.cases) {
            if (("SENT".equals(candidate.status)
                    || "CAPTURED_NO_SCREENSHOT".equals(candidate.status))
                    && plainPages.stream().anyMatch(page -> page.contains("OE-" + candidate.id))) {
                return candidate;
            }
        }
        return null;
    }

    private static void advanceAfterCapture(OttoExtraConfig config,
                                            LetterCalibrationSession session,
                                            LetterCalibrationSession.CaseResult result) {
        int index = session.cases.indexOf(result);
        if (index + 1 < session.cases.size()) {
            session.currentCase = index + 1;
        }
        if (session.cases.stream().allMatch(LetterCalibrationService::captured)) {
            session.completed = true;
            restoreMode(config, session);
            store().clearActiveSession(session.suiteId);
        }
    }

    private static boolean captured(LetterCalibrationSession.CaseResult result) {
        return result != null && result.status != null && result.status.startsWith("CAPTURED");
    }

    private static void takeScreenshot(Path target) {
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), (NativeImage image) -> {
            try (image) {
                Files.createDirectories(target.getParent());
                image.writeTo(target);
            } catch (Exception e) {
                OttoExtra.LOGGER.warn("[letter-calibration] Screenshot fehlgeschlagen: {}",
                        e.toString());
            }
        });
    }

    public static synchronized void finish() {
        LetterCalibrationSession session = active();
        if (session != null) {
            session.completed = true;
            restoreMode(OttoExtraConfig.active(), session);
            store().save(session);
        }
        if (queue != null) {
            queue.cancel();
            queue = null;
        }
        if (session != null) {
            store().clearActiveSession(session.suiteId);
        }
        active = null;
    }

    public static synchronized void interruptSending() {
        if (queue != null) {
            queue.cancel();
            queue = null;
        }
        LetterCalibrationSession.CaseResult result = current();
        if (result != null && result.status.startsWith("SENDING")) {
            result.status = "PENDING";
            store().save(active);
        }
    }

    public static Path activeDirectory() {
        LetterCalibrationSession session = active();
        return session == null ? OttoExtraPaths.letterCalibrationDir()
                : store().sessionDir(session.sessionId);
    }

    private static LetterCalibrationSession create(OttoExtraConfig config, String suiteId) {
        MinecraftClient client = MinecraftClient.getInstance();
        LetterCalibrationSession session = new LetterCalibrationSession();
        session.suiteId = suiteId;
        session.sessionId = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 4);
        session.startedAt = System.currentTimeMillis();
        session.modVersion = FabricLoader.getInstance().getModContainer("ottoextra")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        session.serverAddress = client.getCurrentServerEntry() == null ? "unknown"
                : client.getCurrentServerEntry().address;
        session.guiScale = client.getWindow().getScaleFactor();
        session.language = client.getLanguageManager().getLanguage();
        session.sendMode = config.letter.sendMode;
        session.previousSendMode = config.letter.sendMode;
        session.maxLinesPerPage = isV2Suite(suiteId)
                ? LetterV2Planner.BOOK_MAX_LINES
                : "PAGE".equalsIgnoreCase(config.letter.sendMode)
                ? config.letter.pageModeMaxLinesPerPage : config.letter.maxLinesPerPage;
        session.effectiveCharBudget = isV2Suite(suiteId)
                ? 0
                : config.letter.pageModeEffectiveCharBudget;
        session.renderedWidthBudget = isV2Suite(suiteId)
                && !LetterBoundaryCalibration.isBoundarySuite(suiteId)
                ? LetterV2Planner.BOOK_MAX_RENDERED_WIDTH : 0;
        for (LetterCalibrationCorpus.TestCase test : corpus(suiteId)) {
            LetterCalibrationSession.CaseResult result =
                    new LetterCalibrationSession.CaseResult();
            result.id = test.id();
            result.purpose = test.purpose();
            result.sourcePages = new ArrayList<>(test.pages());
            result.sourceRuns = runPages(test.pages());
            session.cases.add(result);
        }
        return session;
    }

    private static List<LetterCalibrationCorpus.TestCase> corpus(String suiteId) {
        if (SUITE_V2_BOUNDARY.equals(suiteId)) {
            return LetterCalibrationCorpus.v2Boundary();
        }
        if (SUITE_V2_FINAL.equals(suiteId)) {
            return LetterCalibrationCorpus.v2Final();
        }
        if (SUITE_V2_REFINE.equals(suiteId)) {
            return LetterCalibrationCorpus.v2Refine();
        }
        return SUITE_V2.equals(suiteId) ? LetterCalibrationCorpus.v2()
                : LetterCalibrationCorpus.all();
    }

    private static String nextCommand(LetterCalibrationSession session) {
        String base = SUITE_V2_BOUNDARY.equals(session.suiteId)
                ? "/ottoextra letter calibrate v2 boundary "
                : SUITE_V2_FINAL.equals(session.suiteId)
                ? "/ottoextra letter calibrate v2 final "
                : SUITE_V2_REFINE.equals(session.suiteId)
                ? "/ottoextra letter calibrate v2 refine "
                : SUITE_V2.equals(session.suiteId)
                ? "/ottoextra letter calibrate v2 " : "/ottoextra letter calibrate ";
        return base + (session.currentCase + 1);
    }

    private static boolean isV2Suite(String suiteId) {
        return SUITE_V2.equals(suiteId) || SUITE_V2_REFINE.equals(suiteId)
                || SUITE_V2_FINAL.equals(suiteId) || SUITE_V2_BOUNDARY.equals(suiteId);
    }

    private static java.util.function.ToIntFunction<String> textWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.textRenderer != null
                ? client.textRenderer::getWidth : text -> text.length() * 6;
    }

    private static void requireDebug() {
        if (!DebugLog.isEnabled()) {
            throw new IllegalStateException("Briefkalibrierung benötigt /ottoextra debug on");
        }
    }

    private static List<String> decodeIntendedPages(OttoExtraConfig config,
                                                     List<String> commands) {
        List<String> pages = new ArrayList<>();
        boolean pageMode = "PAGE".equalsIgnoreCase(config.letter.sendMode);
        for (String command : commands) {
            int space = command.indexOf(' ');
            String payload = space < 0 ? "" : command.substring(space + 1);
            pages.add(pageMode ? LetterWireCodec.decodePagePayload(payload)
                    : de.ottoextra.letter.format.LetterFormattingCodes
                    .ampersandToSection(payload));
        }
        return pages;
    }

    private static List<String> prefixed(String prefix, List<String> values) {
        return values.stream().map(value -> prefix + value).toList();
    }

    private static List<List<LetterMarkupSemantics.Run>> runPages(List<String> pages) {
        List<List<LetterMarkupSemantics.Run>> runs = new ArrayList<>();
        for (String page : pages) {
            runs.add(new ArrayList<>(LetterMarkupSemantics.runs(page)));
        }
        return runs;
    }

    private static void restoreMode(OttoExtraConfig config,
                                    LetterCalibrationSession session) {
        if (config == null || session.previousSendMode == null
                || session.previousSendMode.isBlank()) {
            return;
        }
        config.letter.sendMode = session.previousSendMode;
        config.save();
    }

    private static void feedback(String value) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(value), false);
        }
    }

    private static synchronized LetterCalibrationStore store() {
        if (store == null) {
            store = new LetterCalibrationStore(OttoExtraPaths.letterCalibrationDir());
        }
        return store;
    }

    private static void recoverInterruptedSend() {
        if (active == null || queue != null) {
            return;
        }
        for (LetterCalibrationSession.CaseResult result : active.cases) {
            if (result.status != null && result.status.startsWith("SENDING")) {
                result.status = "PENDING";
            }
        }
    }
}
