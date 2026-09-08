package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.LetterSendMode;
import de.ottoextra.letter.V2DraftFlow;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.send.LetterV2Planner;
import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.format.LetterFormattingSidebar;
import de.ottoextra.letter.model.LetterPlaceholder;
import de.ottoextra.letter.model.LetterOutputMode;
import de.ottoextra.letter.model.PlaceholderResolveResult;
import de.ottoextra.letter.paste.BookImportService;
import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.paste.TextNormalizer;
import de.ottoextra.letter.placeholder.LetterPlaceholderParser;
import de.ottoextra.letter.placeholder.OttoExtraRpIdentityResolver;
import de.ottoextra.letter.placeholder.PlaceholderResolveService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LetterEditorScreen extends Screen {

    private static final boolean IS_MACOS =
            Util.getOperatingSystem() == Util.OperatingSystem.OSX;

    private static final int PAPER_COLOR = 0xFFC8AC8E;
    private static final int PAPER_DARK = 0xFFB18F69;
    private static final int PAPER_LINE = 0x88643C38;
    private static final int TEXT_COLOR = 0xFF503D29;

    private static final int TEXT_COLOR_LOCKED = 0x66503D29;
    private static final int SELECTION_COLOR = 0x8888AAFF;
    private static final int PANEL_W = 184;
    private static final int PANEL_H = 250;
    private static final int TEXT_X = 28;
    private static final int TEXT_Y = 48;
    private static final int LINE_H = 12;

    private static final int BOOK_PAGE_WIDTH = 108;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    private PageSplitter splitter;
    private final PlaceholderResolveService placeholderService =
            new PlaceholderResolveService(new OttoExtraRpIdentityResolver());

    private static final int SIDEBAR_W = LetterFormattingSidebar.WIDTH;

    private int page;
    private int cursor;
    private int selAnchor = -1;
    private int firstVisibleLine;
    private int viewportPage = -1;
    private int viewportCursor = -1;
    private int viewportCursorLine = -1;
    private String status = "";
    private PreviewKey previewKey;
    private int previewBookPages = -1;
    private LetterFormattingSidebar formattingSidebar;

    private record PreviewKey(List<String> pages, String document, int documentVersion,
                              boolean imported, int lockedPages, int lockedOffset,
                              List<String> initialPages, String sendMode, String command) { }

    @Override
    public void tick() {
        super.tick();
        refreshBookPageCount();
    }

    private void refreshBookPageCount() {
        draft.repair();
        var key = new PreviewKey(List.copyOf(draft.pages), draft.meta.flowingDocument,
                draft.meta.flowingDocumentVersion, draft.meta.importedFromBook,
                draft.meta.lockedPages, draft.meta.lockedOffset,
                List.copyOf(draft.meta.courierInitialPages), config.letter.sendMode,
                config.letter.letterCommand);
        if (key.equals(previewKey)) return;
        var plan = LetterServices.previewPlan(config, draft);
        previewBookPages = plan.warnings().isEmpty() ? Math.max(1, plan.predictedPages().size()) : -1;
        previewKey = key;
    }

    private static final int HISTORY_LIMIT = 100;
    private final Deque<EditorState> undoStack = new ArrayDeque<>();
    private final Deque<EditorState> redoStack = new ArrayDeque<>();

    private boolean mouseSelecting;

    private static final String[] SUGGEST_TYPES = {"name", "title", "full", "mc"};
    private int suggestIndex = 0;

    public LetterEditorScreen(Screen parent, OttoExtraConfig config) {
        this(parent, config, null);
    }

    public LetterEditorScreen(Screen parent, OttoExtraConfig config, LetterDraft suppliedDraft) {
        super(Text.translatable("ottoextra.letter.title"));
        this.parent = parent;
        this.config = config;
        this.draft = suppliedDraft == null ? LetterDraftCache.load() : suppliedDraft;
        this.draft.repair();

        this.page = Math.min(lockedPages(), Math.max(0, draft.pages.size() - 1));
        this.cursor = text().length();
    }

    private int lockedPages() {
        return Math.max(0, Math.min(draft.meta.lockedPages, draft.pages.size()));
    }

    private boolean isLockedPage(int p) {
        return p < lockedPages();
    }

    private boolean currentLocked() {
        return isLockedPage(page);
    }

    private int lockedOffset() {
        return Math.max(0, draft.meta.lockedOffset);
    }

    private int editableStart() {
        int locked = lockedPages();
        if (page < locked) {
            return Integer.MAX_VALUE;
        }
        if (page == locked) {
            return Math.min(lockedOffset(), text().length());
        }
        return 0;
    }

    private boolean hasLockedHere() {
        return editableStart() > 0;
    }

    private int maxLinesPerPage() {
        return switch (LetterSendMode.from(config.letter.sendMode)) {
            case LEGACY -> config.letter.maxLinesPerPage;
            case PAGE -> config.letter.pageModeMaxLinesPerPage;
            case V2 -> draft != null && draft.meta.importedFromBook
                    ? LetterV2Planner.BOOK_MAX_LINES : LetterV2Planner.SERVER_PAGE_MAX_LINES;
        };
    }

    private boolean v2Mode() {
        return LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2;
    }

    private boolean flowingV2Draft() {
        return v2Mode() && lockedPages() == 0 && lockedOffset() == 0
                && !de.ottoextra.letter.calibration.LetterCalibrationService
                .isCalibrationDraft(draft);
    }

    private int panelHeight() {
        return Math.max(1, Math.min(PANEL_H, height - footerHeight() - 8));
    }

    private int footerHeight() {
        return de.ottoextra.letter.calibration.LetterCalibrationService.hasActiveCase()
                ? 56 : 34;
    }

    private int visibleLineCount() {
        return Math.max(1, Math.min(maxLinesPerPage(),
                (panelHeight() - TEXT_Y - 44) / LINE_H));
    }

    private PageSplitter splitter() {
        if (splitter == null && textRenderer != null) {
            int effectiveBudget = LetterSendMode.from(config.letter.sendMode)
                    == LetterSendMode.V2 ? 0 : config.letter.pageModeEffectiveCharBudget;
            splitter = new PageSplitter(textRenderer::getWidth, BOOK_PAGE_WIDTH,
                    maxLinesPerPage(), effectiveBudget);
        }
        return splitter;
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return Math.max(4, (height - panelHeight() - footerHeight()) / 2);
    }

    private StringBuilder pageBuilder() {
        while (draft.pages.size() <= page) {
            draft.pages.add("");
        }
        return new StringBuilder(draft.pages.get(page));
    }

    private String text() {
        draft.repair();
        if (page >= draft.pages.size()) {
            page = draft.pages.size() - 1;
        }
        return draft.pages.get(page);
    }

    private void setText(String value) {
        draft.pages.set(page, value);
        if (!flowingV2Draft()) {
            invalidateFlowingDocument();
        }
    }

    private void invalidateFlowingDocument() {
        draft.meta.flowingDocument = null;
        draft.meta.flowingDocumentVersion = 0;
    }

    private void replaceCurrentText(String value, int nextCursor, int nextAnchor,
                                    boolean reflow) {
        if (currentLocked() || !LetterEditorTextOperations.preservesLockedPrefix(
                text(), value, editableStart())) {
            return;
        }
        if (flowingV2Draft() && textRenderer != null) {
            V2DraftFlow.EditorPosition position = V2DraftFlow.replacePage(
                    draft, page, value, nextCursor, nextAnchor,
                    config.letter.letterCommand, textRenderer::getWidth, reflow);
            page = position.page();
            cursor = position.cursor();
            selAnchor = position.anchor();
            normalizeEditorPosition();
            return;
        }
        setText(value);
        cursor = Math.max(0, Math.min(nextCursor, value.length()));
        selAnchor = nextAnchor < 0 ? -1
                : Math.max(0, Math.min(nextAnchor, value.length()));
        normalizeEditorPosition();
    }

    private void normalizeEditorPosition() {
        String value = text();
        int minimum = editableStart();
        cursor = LetterEditorTextOperations.editableCursor(value, cursor, minimum, 1);
        if (selAnchor >= 0) {
            selAnchor = LetterEditorTextOperations.editableCursor(value, selAnchor, minimum, 1);
            if (selAnchor == cursor) {
                selAnchor = -1;
            }
        }
    }

    private List<int[]> lineSpans() {
        String text = text();
        List<int[]> spans = new ArrayList<>();
        int lineStart = 0;
        int lastSpace = -1;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n') {
                spans.add(new int[]{lineStart, i});
                lineStart = i + 1;
                lastSpace = -1;
                i++;
                continue;
            }
            if (c == ' ') {
                lastSpace = i;
            }

            if (isFormattingCodeAt(text, i)) {
                i += 2;
                continue;
            }
            int next = LetterEditorTextOperations.nextCharacterEnd(text, i);
            String inherited = formattingActive()
                    ? LetterFormattingCodes.activePrefixBefore(text, lineStart) : "";
            if (i > lineStart
                    && textRenderer.getWidth(inherited + text.substring(lineStart, next)) > BOOK_PAGE_WIDTH) {
                int breakAt = lastSpace > lineStart ? lastSpace : i;
                spans.add(new int[]{lineStart, breakAt});
                lineStart = lastSpace > lineStart ? breakAt + 1 : breakAt;
                lastSpace = -1;
                continue;
            }
            i = next;
        }
        spans.add(new int[]{lineStart, text.length()});
        return spans;
    }

    private boolean hasSelection() {
        return selAnchor >= 0 && selAnchor != cursor;
    }

    private int selStart() {
        return Math.min(selAnchor, cursor);
    }

    private int selEnd() {
        return Math.max(selAnchor, cursor);
    }

    private void deleteSelection() {
        if (currentLocked() || !hasSelection()) {
            return;
        }
        String t = text();
        int start = LetterEditorTextOperations.editableCursor(t, selStart(), editableStart(), -1);
        int end = LetterEditorTextOperations.editableCursor(t, selEnd(), editableStart(), 1);
        if (start >= end) {
            return;
        }
        replaceCurrentText(t.substring(0, start) + t.substring(end),
                start, -1, true);
    }

    private void insert(String value) {
        if (currentLocked()) {
            return;
        }
        normalizeEditorPosition();
        recordUndo();
        String t = text();
        int start = hasSelection() ? selStart() : cursor;
        int end = hasSelection() ? selEnd() : cursor;
        start = Math.max(editableStart(), Math.min(start, t.length()));
        end = Math.max(start, Math.min(end, t.length()));
        String inserted = t.substring(0, start) + value + t.substring(end);
        int nextCursor = start + value.length();
        if (LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2
                && value.length() > 1) {
            int locked = editableStart();
            String normalizedPrefix = LetterV2Planner.normalizeDocument(
                    inserted.substring(locked, nextCursor));
            inserted = inserted.substring(0, locked)
                    + LetterV2Planner.normalizeDocument(inserted.substring(locked));
            nextCursor = locked + normalizedPrefix.length();
        }
        // Der Server normalisiert Leerraum erst mit dem folgenden Text. Würden wir
        // hier sofort neu paginieren, würde ein gerade eingegebenes Leerzeichen am
        // Zeilen-/Wortende verschwinden und die Leertaste im V2-Editor wirkungslos.
        boolean pendingV2Space = LetterSendMode.from(config.letter.sendMode)
                == LetterSendMode.V2 && " ".equals(value);
        replaceCurrentText(inserted, nextCursor, -1, !pendingV2Space);
        if (!pendingV2Space && !flowingV2Draft()) {
            reflowOverflow();
        }
        persist();
    }

    private void reflowOverflow() {
        if (LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2) {
            reflowV2Pages();
            return;
        }
        int maxLines = maxLinesPerPage();
        for (int p = page; p < draft.pages.size(); p++) {
            String t = draft.pages.get(p);
            PageSplitter local = splitter();
            if (local == null) {
                return;
            }
            List<String> split = local.split(t);
            if (split.size() <= 1) {
                continue;
            }
            draft.pages.set(p, split.get(0));
            String overflow = String.join("\n", split.subList(1, split.size()));
            if (p + 1 < draft.pages.size()) {
                String nextText = draft.pages.get(p + 1);
                draft.pages.set(p + 1, overflow
                        + (nextText.isEmpty() ? "" : "\n" + nextText));
            } else {
                draft.pages.add(overflow);
            }

            if (p == page && cursor > split.get(0).length()) {
                cursor = cursor - split.get(0).length();
                cursor = Math.max(0, cursor - 1);
                page = p + 1;
            }
        }
    }

    private void persist() {
        if (!de.ottoextra.letter.calibration.LetterCalibrationService
                .isCalibrationDraft(draft)) {
            LetterDraftCache.save(draft);
        }
    }

    private static final class EditorState {
        private final List<String> pages;
        private final int page;
        private final int cursor;
        private final int selAnchor;
        private final String flowingDocument;
        private final int flowingDocumentVersion;

        private EditorState(List<String> pages, int page, int cursor, int selAnchor,
                            String flowingDocument, int flowingDocumentVersion) {
            this.pages = pages;
            this.page = page;
            this.cursor = cursor;
            this.selAnchor = selAnchor;
            this.flowingDocument = flowingDocument;
            this.flowingDocumentVersion = flowingDocumentVersion;
        }
    }

    private EditorState captureState() {
        return new EditorState(new ArrayList<>(draft.pages), page, cursor, selAnchor,
                draft.meta.flowingDocument, draft.meta.flowingDocumentVersion);
    }

    private void recordUndo() {
        undoStack.push(captureState());
        while (undoStack.size() > HISTORY_LIMIT) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(captureState());
        restoreState(undoStack.pop());
        status = Text.translatable("ottoextra.letter.undoDone").getString();
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(captureState());
        restoreState(redoStack.pop());
        status = Text.translatable("ottoextra.letter.redoDone").getString();
    }

    private void restoreState(EditorState state) {
        draft.pages.clear();
        draft.pages.addAll(state.pages);
        draft.repair();
        draft.meta.flowingDocument = state.flowingDocument;
        draft.meta.flowingDocumentVersion = state.flowingDocumentVersion;
        page = Math.max(0, Math.min(state.page, draft.pages.size() - 1));
        int lo = editableStart();
        int minCursor = lo == Integer.MAX_VALUE ? text().length() : lo;
        cursor = Math.max(minCursor, Math.min(state.cursor, text().length()));
        if (state.selAnchor < 0) {
            selAnchor = -1;
        } else {
            selAnchor = Math.max(minCursor, Math.min(state.selAnchor, text().length()));
            if (selAnchor == cursor) {
                selAnchor = -1;
            }
        }
        mouseSelecting = false;
        persist();
    }

    private boolean formattingActive() {
        return config.letter.formattingEnabled;
    }

    private void insertFormattingCode(String code) {
        if (!formattingActive() || code == null || code.length() != 2
                || code.charAt(0) != LetterFormattingCodes.SECTION) {
            return;
        }
        char c = Character.toLowerCase(code.charAt(1));

        if (!LetterFormattingCodes.isValidCode(c) || c == 'k') {
            return;
        }
        if (c == 'r') {
            insertFormattingResetAtCursor();
            return;
        }
        if (hasSelection()) {
            applyFormattingToSelection(code);
        } else {
            insert(code);
        }
    }

    /** Reset ist eine Einfügemarke: Er ändert nur die Formatierung ab dem Cursor. */
    private void insertFormattingResetAtCursor() {
        if (currentLocked()) {
            return;
        }
        String t = text();
        int editable = editableStart();
        int at = Math.max(editable == Integer.MAX_VALUE ? t.length() : editable,
                Math.min(cursor, t.length()));
        at = LetterEditorTextOperations.cursorBoundary(t, at, 1);
        String reset = String.valueOf(LetterFormattingCodes.SECTION) + 'r';
        recordUndo();
        selAnchor = -1;
        replaceCurrentText(t.substring(0, at) + reset + t.substring(at),
                at + reset.length(), -1, true);
        persist();
    }

    private void applyFormattingToSelection(String code) {
        if (currentLocked() || !hasSelection()) {
            return;
        }
        String t = text();
        int editable = editableStart();
        int start = Math.max(editable == Integer.MAX_VALUE ? t.length() : editable, selStart());
        int end = Math.min(t.length(), selEnd());
        if (start >= end) {
            return;
        }

        boolean forwardSelection = selAnchor <= cursor;
        String prefix = selectionFormatPrefix(code, t, start);

        String suffix = LetterFormattingCodes.resetAndRestoreAt(t, end);
        recordUndo();
        String changed = t.substring(0, start) + prefix + t.substring(start, end)
                + suffix + t.substring(end);

        int newStart = start + prefix.length();
        int newEnd = end + prefix.length();
        if (forwardSelection) {
            replaceCurrentText(changed, newEnd, newStart, true);
        } else {
            replaceCurrentText(changed, newStart, newEnd, true);
        }
        persist();
    }

    private void clearFormattingFromSelection() {
        if (currentLocked()) {
            return;
        }
        if (!hasSelection()) {
            status = Text.translatable("ottoextra.letter.fmt.selectFirst").getString();
            return;
        }

        String t = text();
        int editable = editableStart();
        int start = Math.max(editable == Integer.MAX_VALUE ? t.length() : editable, selStart());
        int end = Math.min(t.length(), selEnd());
        if (start >= end) {
            return;
        }

        if (start > editable && start < t.length()
                && t.charAt(start - 1) == LetterFormattingCodes.SECTION
                && LetterFormattingCodes.isValidCode(t.charAt(start))) {
            start--;
        }
        if (end > 0 && end < t.length()
                && t.charAt(end - 1) == LetterFormattingCodes.SECTION
                && LetterFormattingCodes.isValidCode(t.charAt(end))) {
            end++;
        }

        int replaceStart = start;
        while (replaceStart - 2 >= editable && isFormattingCodeAt(t, replaceStart - 2)) {
            replaceStart -= 2;
        }
        int replaceEnd = end;
        while (isFormattingCodeAt(t, replaceEnd)) {
            replaceEnd += 2;
        }

        String activeBefore = LetterFormattingCodes.activePrefixBefore(t, replaceStart);
        String activeAfter = LetterFormattingCodes.activePrefixBefore(t, replaceEnd);
        String plain = stripFormattingCodes(t.substring(replaceStart, replaceEnd));
        String resetBefore = activeBefore.isEmpty()
                ? "" : String.valueOf(LetterFormattingCodes.SECTION) + 'r';
        String replacement = resetBefore + plain + activeAfter;
        String changed = t.substring(0, replaceStart) + replacement + t.substring(replaceEnd);
        if (changed.equals(t)) {
            return;
        }

        boolean forwardSelection = selAnchor <= cursor;
        recordUndo();

        int newStart = replaceStart + resetBefore.length();
        int newEnd = newStart + plain.length();
        if (forwardSelection) {
            replaceCurrentText(changed, newEnd, newStart, true);
        } else {
            replaceCurrentText(changed, newStart, newEnd, true);
        }
        persist();
        status = Text.translatable("ottoextra.letter.fmt.cleared").getString();
    }

    private String stripFormattingCodes(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            if (isFormattingCodeAt(value, i)) {
                i++;
            } else {
                out.append(value.charAt(i));
            }
        }
        return out.toString();
    }

    private String selectionFormatPrefix(String code, String document, int start) {
        char chosen = Character.toLowerCase(code.charAt(1));
        if (!LetterFormattingCodes.isColor(chosen)) {
            return code;
        }
        String active = LetterFormattingCodes.activePrefixBefore(document, start);
        StringBuilder out = new StringBuilder(code);
        for (int i = 0; i + 1 < active.length(); i += 2) {
            char activeCode = Character.toLowerCase(active.charAt(i + 1));
            if (!LetterFormattingCodes.isColor(activeCode) && activeCode != 'r') {
                out.append(LetterFormattingCodes.SECTION).append(activeCode);
            }
        }
        return out.toString();
    }

    private int sidebarX() {
        return Math.min(panelX() + PANEL_W + 8, width - SIDEBAR_W - 4);
    }

    private boolean sidebarVisible() {
        return formattingSidebar != null
                && config.letter.formattingEnabled
                && config.letter.formattingSidebarVisible
                && sidebarX() > panelX() + PANEL_W;
    }

    private void reflowAllPages() {
        if (v2Mode() && (lockedPages() > 0 || lockedOffset() > 0)) {
            // Originalseiten bleiben unverändert: Courier setzt am gespeicherten Buch an.
            invalidateFlowingDocument();
            normalizeEditorPosition();
            return;
        }
        if (flowingV2Draft() && textRenderer != null) {
            applyFlowPosition(V2DraftFlow.reflow(draft, page, cursor, selAnchor,
                    config.letter.letterCommand, textRenderer::getWidth));
            persist();
            return;
        }
        PageSplitter sp = splitter();
        if (sp == null) {
            return;
        }
        invalidateFlowingDocument();
        List<String> rebuilt = new ArrayList<>();
        int locked = lockedPages();
        if (LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2 && locked == 0) {
            rebuilt.addAll(LetterV2Planner.planFlowing(config.letter.letterCommand,
                    List.of(LetterV2Planner.normalizeDocument(String.join("\n", draft.pages))),
                    textRenderer::getWidth).predictedPages());
        } else {
            for (int i = 0; i < draft.pages.size(); i++) {
                if (i < locked) {
                    rebuilt.add(draft.pages.get(i));
                } else {
                    rebuilt.addAll(sp.split(draft.pages.get(i)));
                }
            }
        }
        if (rebuilt.isEmpty()) {
            rebuilt.add("");
        }
        draft.pages.clear();
        draft.pages.addAll(rebuilt);
        page = Math.max(0, Math.min(page, draft.pages.size() - 1));
        cursor = Math.min(cursor, text().length());
        persist();
    }

    private void reflowV2Pages() {
        if (textRenderer == null || !flowingV2Draft()) {
            return;
        }
        applyFlowPosition(V2DraftFlow.reflow(draft, page, cursor, selAnchor,
                config.letter.letterCommand, textRenderer::getWidth));
    }

    private void applyFlowPosition(V2DraftFlow.EditorPosition position) {
        page = position.page();
        cursor = position.cursor();
        selAnchor = position.anchor();
    }

    @Override
    public boolean charTyped(CharInput input) {
        String chr = input.asString();
        if (chr.isEmpty() || !input.isValidChar()) {
            return super.charTyped(input);
        }
        insert(chr);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        normalizeEditorPosition();
        int key = input.key();

        int modifiers = input.modifiers();
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean option = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        boolean rawCtrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean command = IS_MACOS && (modifiers & GLFW.GLFW_MOD_SUPER) != 0;

        boolean shortcut = LetterEditorTextOperations.editorShortcut(
                IS_MACOS, rawCtrl, command);
        boolean wordJump = LetterEditorTextOperations.wordShortcut(
                IS_MACOS, rawCtrl, command, option);
        boolean macLineJump = command;
        boolean macDocumentJump = command
                && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN);

        String t = text();
        java.util.List<String> sugg = suggestions();
        if (!sugg.isEmpty() && !macDocumentJump) {
            suggestIndex = Math.min(suggestIndex, sugg.size() - 1);
            switch (key) {
                case GLFW.GLFW_KEY_DOWN -> {
                    suggestIndex = (suggestIndex + 1) % sugg.size();
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    suggestIndex = (suggestIndex - 1 + sugg.size()) % sugg.size();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB -> {
                    acceptSuggestion(sugg.get(suggestIndex));
                    return true;
                }
                default -> {
                }
            }
        }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                handleTab(shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (currentLocked()) {
                    return true;
                }
                if (hasSelection()) {
                    recordUndo();
                    deleteSelection();
                    persist();
                } else if (cursor > editableStart()) {
                    LetterEditorTextOperations.Range deletion = wordJump
                            ? LetterEditorTextOperations.previousWord(
                                    t, cursor, editableStart())
                            : new LetterEditorTextOperations.Range(
                                    LetterEditorTextOperations.previousCharacterStart(t, cursor),
                                    cursor);
                    if (!deletion.isEmpty()) {
                        recordUndo();
                        replaceCurrentText(t.substring(0, deletion.start())
                                        + t.substring(deletion.end()),
                                deletion.start(), -1, true);
                        persist();
                    }
                } else if (flowingV2Draft() && page > 0) {
                    int previousPage = page - 1;
                    if (!isLockedPage(previousPage)) {
                        String previous = draft.pages.get(previousPage);
                        int minimum = previousPage == lockedPages()
                                ? Math.min(lockedOffset(), previous.length()) : 0;
                        LetterEditorTextOperations.Range deletion = wordJump
                                ? LetterEditorTextOperations.previousWord(
                                        previous, previous.length(), minimum)
                                : new LetterEditorTextOperations.Range(
                                        LetterEditorTextOperations.previousCharacterStart(
                                                previous, previous.length()), previous.length());
                        if (!deletion.isEmpty()) {
                            recordUndo();
                            page = previousPage;
                            cursor = previous.length();
                            replaceCurrentText(previous.substring(0, deletion.start())
                                            + previous.substring(deletion.end()),
                                    deletion.start(), -1, true);
                            persist();
                        }
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (currentLocked()) {
                    return true;
                }
                if (hasSelection()) {
                    recordUndo();
                    deleteSelection();
                    persist();
                } else if (cursor < t.length()) {
                    LetterEditorTextOperations.Range deletion = wordJump
                            ? LetterEditorTextOperations.nextWord(t, cursor)
                            : new LetterEditorTextOperations.Range(cursor,
                                    LetterEditorTextOperations.nextCharacterEnd(t, cursor));
                    if (!deletion.isEmpty()) {
                        recordUndo();
                        replaceCurrentText(t.substring(0, deletion.start())
                                        + t.substring(deletion.end()),
                                deletion.start(), -1, true);
                        persist();
                    }
                } else if (flowingV2Draft() && page + 1 < draft.pages.size()) {
                    int nextPage = page + 1;
                    String next = draft.pages.get(nextPage);
                    LetterEditorTextOperations.Range deletion;
                    if (wordJump) {
                        deletion = LetterEditorTextOperations.nextWord(next, 0);
                    } else {
                        int removeAt = 0;
                        while (isFormattingCodeAt(next, removeAt)) {
                            removeAt += 2;
                        }
                        deletion = new LetterEditorTextOperations.Range(removeAt,
                                LetterEditorTextOperations.nextCharacterEnd(next, removeAt));
                    }
                    if (!deletion.isEmpty()) {
                        recordUndo();
                        page = nextPage;
                        cursor = 0;
                        replaceCurrentText(next.substring(0, deletion.start())
                                        + next.substring(deletion.end()),
                                deletion.start(), -1, true);
                        persist();
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insert("\n");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (!shift && flowingV2Draft() && cursor <= editableStart() && page > 0) {
                    page--;
                    cursor = text().length();
                    selAnchor = -1;
                } else if (macLineJump) {
                    moveCursor(lineStartOf(cursor), shift);
                } else if (wordJump) {
                    moveCursor(prevWordBoundary(cursor), shift);
                } else {
                    moveCursor(cursor - 1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (!shift && flowingV2Draft() && cursor >= t.length()
                        && page + 1 < draft.pages.size()) {
                    page++;
                    cursor = 0;
                    selAnchor = -1;
                } else if (macLineJump) {
                    moveCursor(lineEndOf(cursor), shift);
                } else if (wordJump) {
                    moveCursor(nextWordBoundary(cursor), shift);
                } else {
                    moveCursor(cursor + 1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                if (macDocumentJump) {
                    moveCursor(key == GLFW.GLFW_KEY_DOWN ? t.length() : 0, shift);
                } else {
                    moveVertical(key == GLFW.GLFW_KEY_DOWN ? 1 : -1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveCursor(lineStartOf(cursor), shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveCursor(lineEndOf(cursor), shift);
                return true;
            }
            case GLFW.GLFW_KEY_Y -> {
                if (shortcut) {
                    redo();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_Z -> {
                if (shortcut) {
                    if (shift) {
                        redo();
                    } else {
                        undo();
                    }
                    return true;
                }
            }
            case GLFW.GLFW_KEY_A -> {
                if (shortcut) {

                    int lo = editableStart();
                    selAnchor = lo == Integer.MAX_VALUE ? t.length() : lo;
                    cursor = t.length();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (shortcut && hasSelection()) {
                    client.keyboard.setClipboard(t.substring(selStart(), selEnd()));
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                if (shortcut && hasSelection()) {
                    client.keyboard.setClipboard(t.substring(selStart(), selEnd()));
                    recordUndo();
                    deleteSelection();
                    persist();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (shortcut) {
                    pasteClipboard();
                    return true;
                }
            }
            default -> {
            }
        }
        return super.keyPressed(input);
    }

    private void pasteClipboard() {
        String raw = client.keyboard.getClipboard();
        String normalized;
        if (formattingActive()) {

            String guarded = LetterFormattingCodes.sectionToAmpersand(raw);
            normalized = TextNormalizer.normalize(guarded);
            if (config.letter.formattingConvertAmpersandOnPaste) {
                normalized = LetterFormattingCodes.ampersandToSection(normalized);
            }
        } else {
            normalized = TextNormalizer.normalize(raw);
        }
        if (normalized.isEmpty()) {
            return;
        }
        int pagesBefore = draft.pages.size();
        insert(normalized);
        int pagesAfter = draft.pages.size();
        if (normalized.length() > 200 || pagesAfter > pagesBefore) {
            status = Text.translatable("ottoextra.letter.pasteSummary",
                    normalized.length(), pagesAfter - pagesBefore).getString();
        }
    }

    private String suggestPrefix() {
        String t = text();
        int end = Math.min(cursor, t.length());
        int open = t.lastIndexOf("{{", end - 1);
        if (open < 0) {
            return null;
        }
        String between = t.substring(open + 2, end);
        if (between.contains("}") || between.contains(":") || between.length() > 8) {
            return null;
        }
        return between.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private java.util.List<String> suggestions() {
        String prefix = suggestPrefix();
        if (prefix == null) {
            return java.util.List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String type : SUGGEST_TYPES) {
            if (type.startsWith(prefix)) {
                out.add(type);
            }
        }
        return out;
    }

    private void acceptSuggestion(String type) {
        String prefix = suggestPrefix();
        if (prefix == null) {
            return;
        }
        insert(type.substring(prefix.length()) + ":");
        suggestIndex = 0;
    }

    private void handleTab(boolean shift) {
        String t = text();
        LetterPlaceholder at = LetterPlaceholderParser.at(t, cursor);
        if (!shift && at != null && !currentLocked() && at.start() >= editableStart()) {
            PlaceholderResolveResult result = placeholderService.resolve(at);
            if (result.ok()) {
                recordUndo();
                String changed = placeholderService.apply(t, result);
                replaceCurrentText(changed,
                        at.start() + result.resolved().length(), -1, true);
                status = Text.translatable("ottoextra.letter.placeholderResolved",
                        result.resolved()).getString();
                persist();
            } else {
                status = Text.translatable("ottoextra.letter.placeholderUnknown",
                        at.playerName()).getString();
            }
            return;
        }
        LetterPlaceholder target = shift
                ? LetterPlaceholderParser.previous(t, cursor)
                : LetterPlaceholderParser.next(t, cursor);
        if (target != null && target.start() >= editableStart()) {
            moveCursor(target.start() + 2, false);
        }
    }

    private void moveCursor(int to, boolean shift) {
        normalizeEditorPosition();
        if (shift && selAnchor < 0) {
            selAnchor = cursor;
        }
        if (!shift) {
            selAnchor = -1;
        }
        int hi = text().length();
        int lo = editableStart();
        if (lo == Integer.MAX_VALUE) {

            cursor = hi;
            return;
        }
        int direction = Integer.compare(to, cursor);
        int boundary = LetterEditorTextOperations.cursorBoundary(text(), to, direction);
        cursor = Math.max(lo, Math.max(0, Math.min(boundary, hi)));
    }

    private void moveVertical(int dir, boolean shift) {
        List<int[]> spans = lineSpans();
        int line = lineIndexOf(cursor, spans);
        int col = cursor - spans.get(line)[0];
        int target = line + dir;
        if (target < 0 || target >= spans.size()) {
            return;
        }
        int[] span = spans.get(target);
        moveCursor(Math.min(span[0] + col, span[1]), shift);
    }

    private int lineIndexOf(int pos, List<int[]> spans) {
        for (int i = 0; i < spans.size(); i++) {
            if (pos <= spans.get(i)[1]) {
                return i;
            }
        }
        return spans.size() - 1;
    }

    private int prevWordBoundary(int pos) {
        return LetterEditorTextOperations.previousWord(text(), pos, 0).start();
    }

    private int nextWordBoundary(int pos) {
        return LetterEditorTextOperations.nextWord(text(), pos).end();
    }

    private int lineStartOf(int pos) {
        List<int[]> spans = lineSpans();
        return spans.get(lineIndexOf(pos, spans))[0];
    }

    private int lineEndOf(int pos) {
        List<int[]> spans = lineSpans();
        return spans.get(lineIndexOf(pos, spans))[1];
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (sidebarVisible() && formattingSidebar.contains(click.x(), click.y())) {

            setFocused(null);
            return formattingSidebar.mouseClicked(click.x(), click.y(), click.button());
        }
        List<int[]> spans = lineSpans();
        if (click.button() == 0 && isInsideTextArea(click.x(), click.y(), spans)) {
            moveCursor(textPositionAt(click.x(), click.y(), spans), false);

            selAnchor = cursor;
            mouseSelecting = true;
            setFocused(null);
            return true;
        }

        mouseSelecting = false;
        boolean handled = super.mouseClicked(click, doubled);
        if (handled) {

            setFocused(null);
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (mouseSelecting) {
            moveCursor(textPositionAt(click.x(), click.y(), lineSpans()), true);
            setFocused(null);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (mouseSelecting && click.button() == 0) {
            mouseSelecting = false;

            if (selAnchor == cursor) {
                selAnchor = -1;
            }
            setFocused(null);
            return true;
        }
        return super.mouseReleased(click);
    }

    private boolean isInsideTextArea(double mouseX, double mouseY, List<int[]> spans) {
        int tx = panelX() + TEXT_X;
        int ty = panelY() + TEXT_Y;
        int visibleLines = Math.max(1, Math.min(spans.size(), visibleLineCount()));
        return mouseX >= tx - 4 && mouseX <= tx + 114
                && mouseY >= ty - 2 && mouseY <= ty + visibleLines * LINE_H + 2;
    }

    private int textPositionAt(double mouseX, double mouseY, List<int[]> spans) {
        int tx = panelX() + TEXT_X;
        int ty = panelY() + TEXT_Y;
        int visibleLines = Math.max(1, Math.min(spans.size(), visibleLineCount()));
        int line = Math.max(0, Math.min((int) Math.floor((mouseY - ty) / LINE_H),
                visibleLines - 1));
        int[] span = spans.get(Math.min(spans.size() - 1, firstVisibleLine + line));
        String lineText = text().substring(span[0], span[1]);
        int col = colForX(lineText, (int) Math.round(mouseX - tx), span[0]);
        int pos = span[0] + col;
        int lo = editableStart();
        if (lo == Integer.MAX_VALUE) {
            return text().length();
        }
        return Math.max(lo, Math.min(pos, text().length()));
    }

    private int colForX(String lineText, int relX, int lineStart) {
        String inherited = formattingActive()
                ? LetterFormattingCodes.activePrefixBefore(text(), lineStart) : "";
        int i = 0;
        int lastVisibleBoundary = 0;
        while (i < lineText.length()) {
            while (isFormattingCodeAt(lineText, i)) {
                i += 2;
            }
            if (i >= lineText.length()) {

                return lastVisibleBoundary == 0 ? i : lastVisibleBoundary;
            }
            int before = i;
            int next = i + Character.charCount(lineText.codePointAt(i));
            int beforeWidth = textRenderer.getWidth(inherited + lineText.substring(0, before));
            int afterWidth = textRenderer.getWidth(inherited + lineText.substring(0, next));
            int midpoint = beforeWidth + Math.max(1, afterWidth - beforeWidth) / 2;
            if (relX < midpoint) {
                return before;
            }
            lastVisibleBoundary = next;
            i = next;
        }
        return lastVisibleBoundary;
    }

    private boolean isFormattingCodeAt(String value, int index) {
        return index >= 0 && index + 1 < value.length()
                && value.charAt(index) == LetterFormattingCodes.SECTION
                && LetterFormattingCodes.isValidCode(value.charAt(index + 1));
    }

    private int renderedWidthTo(String document, int lineStart, String lineText, int rawColumn) {
        int end = Math.max(0, Math.min(rawColumn, lineText.length()));
        String inherited = formattingActive()
                ? LetterFormattingCodes.activePrefixBefore(document, lineStart) : "";
        return textRenderer.getWidth(inherited + lineText.substring(0, end));
    }

    @Override
    protected void init() {
        reflowAllPages();
        previewKey = null;
        refreshBookPageCount();
        viewportPage = -1;
        if (config.letter.formattingEnabled) {
            formattingSidebar = new LetterFormattingSidebar(
                    this::insertFormattingCode, this::clearFormattingFromSelection);
        }
        int px = panelX();
        int py = panelY();
        int ph = panelHeight();
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> switchPage(-1))
                .dimensions(px + 8, py + ph - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> switchPage(1))
                .dimensions(px + 30, py + ph - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            if (currentLocked()) {
                return;
            }
            recordUndo();
            if (flowingV2Draft() && textRenderer != null) {
                applyFlowPosition(V2DraftFlow.insertPageAfter(draft, page,
                        config.letter.letterCommand, textRenderer::getWidth));
            } else {
                invalidateFlowingDocument();
                draft.pages.add(page + 1, "");
                switchPage(1);
            }
            persist();
        }).dimensions(px + 52, py + ph - 26, 20, 18).build());
        int deleteX = 74;
        addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> deleteCurrentPage())
                .dimensions(px + deleteX, py + ph - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.send"), b -> {

                    persist();
                    if (de.ottoextra.letter.calibration.LetterCalibrationService
                            .isCalibrationDraft(draft)) {
                        de.ottoextra.letter.calibration.LetterCalibrationService
                                .writeFromEditor(config, draft);
                        client.setScreen(null);
                        return;
                    }
                    draft.meta.mode = LetterOutputMode.BRIEF;
                    if (LetterServices.startWrite(config, draft, () -> {
                        MinecraftClient minecraft = MinecraftClient.getInstance();
                        if (minecraft.player != null && minecraft.world != null) {
                            minecraft.setScreen(new RecipientScreen(null, config, draft));
                        }
                    })) {
                        LetterServices.setPending(draft);
                        client.setScreen(null);
                    } else {
                        Text problem = LetterServices.writeItemProblem(config, draft);
                        status = problem == null
                                ? Text.translatable("ottoextra.letter.write.failed").getString()
                                : problem.getString();
                    }
                })
                .dimensions(px + PANEL_W - 64, py + ph - 26, 56, 18).build());

        ButtonWidget check = ButtonWidget.builder(Text.empty(), b -> checkPlaceholders())
                .dimensions(px, py + ph + 4, 18, 16).build();
        check.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.translatable("ottoextra.letter.checkPlaceholders")));
        addDrawableChild(check);
        int secondaryX = px + 22;
        if (config.letter.previewEnabled) {
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("ottoextra.letter.preview"), b -> {
                        persist();
                        client.setScreen(new LetterPreviewScreen(this, config, draft, page));
                    }).dimensions(secondaryX, py + ph + 4, 72, 16).build());
            secondaryX += 76;
        }
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.drafts"), b -> {
                    persist();
                    client.setScreen(new SavedDraftsScreen(this, config, draft));
                }).dimensions(secondaryX, py + ph + 4,
                        px + PANEL_W - secondaryX, 16).build());

        if (de.ottoextra.letter.calibration.LetterCalibrationService.hasActiveCase()) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("§cDebug-Testtext in diesen Brief einsetzen"), b -> {
                        recordUndo();
                        String id = de.ottoextra.letter.calibration.LetterCalibrationService
                                .populateCurrentDraft(draft);
                        page = 0;
                        selAnchor = -1;
                        splitter = null;
                        reflowAllPages();
                        page = 0;
                        cursor = text().length();
                        status = id.isBlank() ? "Kalibrierung nicht aktiv"
                                : id + " eingesetzt — jetzt Schreiben drücken";
                    }).dimensions(px, py + ph + 24, PANEL_W, 18).build());
        }
    }

    private void deleteCurrentPage() {
        if (currentLocked() || hasLockedHere()) {
            return;
        }
        recordUndo();
        if (flowingV2Draft()) {
            applyFlowPosition(V2DraftFlow.deletePage(draft, page,
                    config.letter.letterCommand, textRenderer::getWidth));
            persist();
            return;
        }
        if (draft.pages.size() <= lockedPages() + 1) {
            setText("");
            cursor = 0;
            selAnchor = -1;
            persist();
            return;
        }
        invalidateFlowingDocument();
        draft.pages.remove(page);
        page = Math.max(lockedPages(), Math.min(page, draft.pages.size() - 1));
        cursor = Math.min(cursor, text().length());
        selAnchor = -1;
        if (v2Mode()) {
            reflowV2Pages();
        }
        persist();
    }

    private static final net.minecraft.util.Identifier CHECK_ICON =
            de.ottoextra.OttoExtra.id("textures/gui/check_params.png");

    private void switchPage(int dir) {
        int target = page + dir;
        if (target < 0 || target >= draft.pages.size()) {
            return;
        }
        page = target;
        cursor = Math.min(cursor, text().length());
        selAnchor = -1;
        normalizeEditorPosition();
    }

    private void checkPlaceholders() {
        int open = 0;
        int invalid = 0;
        for (String p : draft.pages) {
            open += LetterPlaceholderParser.parse(p).size();
            invalid += LetterPlaceholderParser.countInvalidOpenings(p);
        }
        status = Text.translatable("ottoextra.letter.placeholderStatus", open, invalid).getString();
    }

    private void updateViewport(List<int[]> spans) {
        boolean changedPage = viewportPage != page;
        int cursorLine = lineIndexOf(cursor, spans);
        firstVisibleLine = LetterEditorTextOperations.firstVisibleLine(
                changedPage ? 0 : firstVisibleLine, cursorLine,
                spans.size(), visibleLineCount(), changedPage || viewportCursor != cursor
                        || viewportCursorLine != cursorLine);
        viewportPage = page;
        viewportCursor = cursor;
        viewportCursorLine = cursorLine;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        List<int[]> spans = lineSpans();
        if (isInsideTextArea(mouseX, mouseY, spans) && spans.size() > visibleLineCount()) {
            updateViewport(spans);
            firstVisibleLine = LetterEditorTextOperations.firstVisibleLine(
                    firstVisibleLine - (int) Math.signum(vertical) * 3,
                    lineIndexOf(cursor, spans), spans.size(), visibleLineCount(), false);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int ph = panelHeight();
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + ph + 2, PAPER_DARK);
        ctx.fill(px, py, px + PANEL_W, py + ph, PAPER_COLOR);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.workingPageIndicator",
                page + 1, draft.pages.size()), px + TEXT_X, py + 12,
                TEXT_COLOR, false);
        Text bookCount = previewBookPages < 0
                ? Text.translatable("ottoextra.letter.bookCountUnavailable")
                : Text.translatable("ottoextra.letter.bookCount", previewBookPages);
        ctx.drawText(textRenderer, bookCount, px + TEXT_X, py + 23, TEXT_COLOR, false);
        if (de.ottoextra.letter.calibration.LetterCalibrationService
                .isCalibrationDraft(draft)) {
            ctx.drawText(textRenderer, "§cKalibrierung · "
                            + de.ottoextra.letter.calibration.LetterCalibrationService
                            .activeSendMode() + " · nicht verändern",
                    px + TEXT_X, py + ph - 40, TEXT_COLOR_LOCKED, false);
        } else if (hasLockedHere()) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.book.locked"),
                    px + TEXT_X, py + 34, TEXT_COLOR_LOCKED, false);
        }

        String t = text();
        int lockStart = editableStart();
        List<int[]> spans = lineSpans();
        int maxLines = visibleLineCount();
        updateViewport(spans);
        for (int i = firstVisibleLine; i < Math.min(spans.size(), firstVisibleLine + maxLines); i++) {
            int[] span = spans.get(i);
            String lineText = t.substring(span[0], span[1]);
            int y = py + TEXT_Y + (i - firstVisibleLine) * LINE_H;
            ctx.fill(px + TEXT_X - 2, y + LINE_H - 2,
                    px + TEXT_X + 130, y + LINE_H - 1, PAPER_LINE);
            if (hasSelection() && selEnd() > span[0] && selStart() < span[1]) {
                int s = Math.max(selStart(), span[0]) - span[0];
                int e = Math.min(selEnd(), span[1]) - span[0];
                int x1 = px + TEXT_X + renderedWidthTo(t, span[0], lineText, s);
                int x2 = px + TEXT_X + renderedWidthTo(t, span[0], lineText, e);
                ctx.fill(x1, y - 1, x2, y + 9, SELECTION_COLOR);
            }

            int split = lockStart == Integer.MAX_VALUE
                    ? lineText.length()
                    : Math.max(0, Math.min(lockStart - span[0], lineText.length()));
            int x = px + TEXT_X;
            if (split > 0) {

                MutableText locked = desaturatedText(lineText.substring(0, split));
                ctx.drawText(textRenderer, locked, x, y, TEXT_COLOR_LOCKED, false);
                x += textRenderer.getWidth(locked);
            }
            if (split < lineText.length()) {
                String editPart = lineText.substring(split);
                String renderText = formattingActive()
                        ? LetterFormattingCodes.activePrefixBefore(t, span[0] + split) + editPart
                        : editPart;
                ctx.drawText(textRenderer, renderText, x, y, TEXT_COLOR, false);
            }

            if (lockStart != Integer.MAX_VALUE && cursor >= Math.max(span[0], lockStart)
                    && cursor <= span[1]
                    && i == lineIndexOf(cursor, spans)
                    && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = px + TEXT_X + renderedWidthTo(
                        t, span[0], lineText, cursor - span[0]);
                ctx.fill(cx, y - 1, cx + 1, y + 9, TEXT_COLOR);
            }
        }
        if (spans.size() > maxLines && !de.ottoextra.letter.calibration
                .LetterCalibrationService.isCalibrationDraft(draft)) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.editor.visibleLines",
                    firstVisibleLine + 1, Math.min(spans.size(), firstVisibleLine + maxLines),
                    spans.size()), px + TEXT_X, py + ph - 40, TEXT_COLOR, false);
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2,
                    py + ph + (de.ottoextra.letter.calibration
                            .LetterCalibrationService.hasActiveCase() ? 46 : 24),
                    0xFFE6C8A9);
        }
        renderSuggestions(ctx, px, py, spans);
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, CHECK_ICON,
                px + 1, py + ph + 4, 0f, 0f, 16, 16, 16, 16);

        if (sidebarVisible()) {
            formattingSidebar.setBounds(sidebarX(), py);
            formattingSidebar.render(ctx, textRenderer, mouseX, mouseY);
        }
    }

    private void renderSuggestions(DrawContext ctx, int px, int py, List<int[]> spans) {
        java.util.List<String> sugg = suggestions();
        if (sugg.isEmpty()) {
            return;
        }
        suggestIndex = Math.min(suggestIndex, sugg.size() - 1);
        int line = lineIndexOf(cursor, spans);
        int x = px + TEXT_X + 8;
        if (line < firstVisibleLine || line >= firstVisibleLine + visibleLineCount()) return;
        int y = py + TEXT_Y + (line - firstVisibleLine + 1) * LINE_H + 2;
        int w = 64;
        int h = sugg.size() * 11 + 4;
        ctx.fill(x - 2, y - 2, x + w + 2, y + h, 0xE0201608);
        for (int i = 0; i < sugg.size(); i++) {
            boolean sel = i == suggestIndex;
            if (sel) {
                ctx.fill(x - 1, y + i * 11 - 1, x + w + 1, y + i * 11 + 10, 0x667A5A3A);
            }
            ctx.drawText(textRenderer, sugg.get(i) + ":", x, y + i * 11,
                    sel ? 0xFFFFD479 : 0xFFE6C8A9, false);
        }
    }

    private MutableText desaturatedText(String raw) {
        MutableText out = Text.empty();
        StringBuilder buf = new StringBuilder();
        Style style = Style.EMPTY;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                if (buf.length() > 0) {
                    out.append(Text.literal(buf.toString()).setStyle(style));
                    buf.setLength(0);
                }
                style = applyCode(style, Character.toLowerCase(raw.charAt(++i)));
                continue;
            }
            buf.append(c);
        }
        if (buf.length() > 0) {
            out.append(Text.literal(buf.toString()).setStyle(style));
        }
        return out;
    }

    private static Style applyCode(Style style, char code) {
        if (code == 'r') {
            return Style.EMPTY;
        }
        Formatting f = Formatting.byCode(code);
        if (f == null) {
            return style;
        }
        if (f.isColor()) {
            Integer rgb = f.getColorValue();

            return rgb == null ? Style.EMPTY
                    : Style.EMPTY.withColor(TextColor.fromRgb(washed(rgb)));
        }
        return switch (f) {
            case BOLD -> style.withBold(true);
            case ITALIC -> style.withItalic(true);
            case UNDERLINE -> style.withUnderline(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case OBFUSCATED -> style.withObfuscated(true);
            default -> style;
        };
    }

    private static int washed(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int lum = (int) (0.3 * r + 0.59 * g + 0.11 * b);
        r += (int) ((lum - r) * 0.55);
        g += (int) ((lum - g) * 0.55);
        b += (int) ((lum - b) * 0.55);
        r += (int) ((0xC8 - r) * 0.40);
        g += (int) ((0xAC - g) * 0.40);
        b += (int) ((0x8E - b) * 0.40);
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        persist();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
