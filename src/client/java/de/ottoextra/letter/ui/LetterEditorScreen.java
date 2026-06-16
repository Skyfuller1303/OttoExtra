package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.format.LetterFormattingSidebar;
import de.ottoextra.letter.model.LetterPlaceholder;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Brief-/Verkündungs-Editor: Papier-Look, Seiten mit
 * Wortumbruch, Cursor + Shift-Selektion, Ctrl+C/X/V (Paste mit automatischer
 * Seitenerstellung, NIE kürzen), Buchimport, Tab/Shift-Tab für RP-Platzhalter
 * ({@code {{name:...}}} usw. — nie automatische Ersetzung beim Tippen).
 * Senden öffnet den Modusdialog (Brief vs. Verkündung).
 */
public final class LetterEditorScreen extends Screen {

    // Papier-Palette (aus OttoLetter portiert)
    private static final int PAPER_COLOR = 0xFFC8AC8E;
    private static final int PAPER_DARK = 0xFFB18F69;
    private static final int PAPER_LINE = 0x88643C38;
    private static final int TEXT_COLOR = 0xFF503D29;
    /** Reduzierte Deckkraft für bereits geschriebene (gesperrte) Seiten. */
    private static final int TEXT_COLOR_LOCKED = 0x66503D29;
    private static final int SELECTION_COLOR = 0x8888AAFF;
    private static final int PANEL_W = 184;
    private static final int PANEL_H = 250;
    private static final int TEXT_X = 28;
    private static final int TEXT_Y = 30;
    private static final int LINE_H = 12;
    /** Zeilenbreite des Buchs in Pixeln (WYSIWYG-Umbruch). 108 = 18 Ziffern/Zeile,
     *  gemessen an der Buchansicht; schmaler als Vanilla-114, damit das Buch die
     *  Zeilen nicht nochmal umbricht. */
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
    private String status = "";
    private LetterFormattingSidebar formattingSidebar;
    /** Platzhalter-Typen für die {{-Vorschlagsliste. */
    private static final String[] SUGGEST_TYPES = {"name", "title", "full", "mc"};
    private int suggestIndex = 0;

    public LetterEditorScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.letter.title"));
        this.parent = parent;
        this.config = config;
        this.draft = LetterDraftCache.load();
        this.draft.repair();
        // Beim Bearbeiten eines beschriebenen Briefs auf der ersten neuen
        // (editierbaren) Seite starten, nicht im gesperrten Altbestand.
        this.page = Math.min(lockedPages(), Math.max(0, draft.pages.size() - 1));
        this.cursor = text().length();
    }

    /** Anzahl gesperrter (bereits geschriebener) Seiten, geklemmt. */
    private int lockedPages() {
        return Math.max(0, Math.min(draft.meta.lockedPages, draft.pages.size()));
    }

    /** Ist Seite {@code p} gesperrt (read-only, bereits geschrieben)? */
    private boolean isLockedPage(int p) {
        return p < lockedPages();
    }

    /** Ist die aktuell sichtbare Seite vollständig gesperrt? */
    private boolean currentLocked() {
        return isLockedPage(page);
    }

    /** Gesperrte führende Zeichen auf der Fortsetzungs-Seite. */
    private int lockedOffset() {
        return Math.max(0, draft.meta.lockedOffset);
    }

    /**
     * Erste editierbare Zeichenposition auf der aktuellen Seite:
     * {@code MAX_VALUE} = ganze Seite gesperrt; auf der Fortsetzungs-Seite
     * {@link #lockedOffset()}; sonst 0.
     */
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

    /** Gibt es auf der aktuellen Seite gesperrten (bereits geschriebenen) Text? */
    private boolean hasLockedHere() {
        return editableStart() > 0;
    }

    /** PAGE-Modus nutzt volle Buchseiten (14 Zeilen); LEGACY bleibt bei der
     *  konfigurierten Zeilenzahl. */
    private int maxLinesPerPage() {
        return "PAGE".equalsIgnoreCase(config.letter.sendMode)
                ? config.letter.pageModeMaxLinesPerPage : config.letter.maxLinesPerPage;
    }

    /**
     * Pixelbasierter Seiten-Splitter wie das echte Buch — lazy, da
     * {@code textRenderer} erst nach {@code init()} gesetzt ist.
     */
    private PageSplitter splitter() {
        if (splitter == null && textRenderer != null) {
            splitter = new PageSplitter(textRenderer::getWidth, BOOK_PAGE_WIDTH,
                    maxLinesPerPage(), config.letter.pageModeEffectiveCharBudget);
        }
        return splitter;
    }

    // ---- Layout/Helpers --------------------------------------------------------

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return Math.max(4, (height - PANEL_H) / 2);
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
    }

    /** Zeilen der aktuellen Seite mit Original-Offsets [start, end]. */
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
            // Umbruch nach Pixelbreite wie das echte Buch (ein einzelnes Zeichen
            // passt immer -> kein Endlos-Loop).
            if (i > lineStart
                    && textRenderer.getWidth(text.substring(lineStart, i + 1)) > BOOK_PAGE_WIDTH) {
                int breakAt = lastSpace > lineStart ? lastSpace : i;
                spans.add(new int[]{lineStart, breakAt});
                lineStart = lastSpace > lineStart ? breakAt + 1 : breakAt;
                lastSpace = -1;
                continue;
            }
            i++;
        }
        spans.add(new int[]{lineStart, text.length()});
        return spans;
    }

    // ---- Editing ----------------------------------------------------------------

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
        if (!hasSelection()) {
            return;
        }
        String t = text();
        setText(t.substring(0, selStart()) + t.substring(selEnd()));
        cursor = selStart();
        selAnchor = -1;
    }

    private void insert(String value) {
        if (currentLocked()) {
            return; // gesperrte (bereits geschriebene) Seite ist read-only
        }
        deleteSelection();
        String t = text();
        cursor = Math.max(0, Math.min(cursor, t.length()));
        setText(t.substring(0, cursor) + value + t.substring(cursor));
        cursor += value.length();
        reflowOverflow();
        persist();
    }

    /** Seitenüberlauf in Folgeseiten schieben (Auto-Pages, nie kürzen). */
    private void reflowOverflow() {
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
            // Cursor folgt, wenn er hinter dem Schnitt lag
            if (p == page && cursor > split.get(0).length()) {
                cursor = cursor - split.get(0).length();
                cursor = Math.max(0, cursor - 1); // getrenntes \n
                page = p + 1;
            }
        }
    }

    private void persist() {
        LetterDraftCache.save(draft);
    }

    // ---- Formatierung ----------------------------------------------------------

    /** Formatierungshilfe (Sidebar + Live-Vorschau) aktiv? */
    private boolean formattingActive() {
        return config.letter.formattingEnabled;
    }

    /** Einzelnen {@code §x}-Code an der Cursorposition einfügen (von der Sidebar). */
    private void insertFormattingCode(String code) {
        if (!formattingActive() || code == null || code.length() != 2
                || code.charAt(0) != LetterFormattingCodes.SECTION) {
            return;
        }
        char c = Character.toLowerCase(code.charAt(1));
        // Magic-Code §k bewusst nicht einfügbar (macht Text unlesbar).
        if (!LetterFormattingCodes.isValidCode(c) || c == 'k') {
            return;
        }
        insert(code);
    }

    /** Sichtbare X-Position der Sidebar (rechts neben dem Brief, in den Screen geklemmt). */
    private int sidebarX() {
        return Math.min(panelX() + PANEL_W + 8, width - SIDEBAR_W - 4);
    }

    /** Sidebar überhaupt zeigen? (genug Platz rechts, Feature + Toggle an) */
    private boolean sidebarVisible() {
        return formattingSidebar != null
                && config.letter.formattingEnabled
                && config.letter.formattingSidebarVisible
                && sidebarX() > panelX() + PANEL_W; // sonst überlappt es den Brief
    }

    /** Alle Seiten beim Öffnen gegen die aktuellen Limits re-paginieren
     *  (Zeichen-Budget + Zeilen). Splittet überlange Seiten, ohne kurze zu mergen. */
    private void reflowAllPages() {
        PageSplitter sp = splitter();
        if (sp == null) {
            return;
        }
        List<String> rebuilt = new ArrayList<>();
        int locked = lockedPages();
        for (int i = 0; i < draft.pages.size(); i++) {
            if (i < locked) {
                rebuilt.add(draft.pages.get(i)); // gesperrte Seiten 1:1 behalten
            } else {
                rebuilt.addAll(sp.split(draft.pages.get(i)));
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

    // ---- Input ----------------------------------------------------------------

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
        int key = input.key();
        boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        String t = text();
        java.util.List<String> sugg = suggestions();
        if (!sugg.isEmpty()) {
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
                    return true; // read-only
                }
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor > editableStart()) {
                    setText(t.substring(0, cursor - 1) + t.substring(cursor));
                    cursor--;
                }
                persist();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (currentLocked()) {
                    return true; // read-only
                }
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor < t.length()) {
                    setText(t.substring(0, cursor) + t.substring(cursor + 1));
                }
                persist();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insert("\n");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveCursor(cursor - 1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCursor(cursor + 1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                moveVertical(key == GLFW.GLFW_KEY_DOWN ? 1 : -1, shift);
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
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    // Nur den editierbaren Teil markieren (gesperrten Prefix auslassen)
                    int lo = editableStart();
                    selAnchor = lo == Integer.MAX_VALUE ? t.length() : lo;
                    cursor = t.length();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl && hasSelection()) {
                    client.keyboard.setClipboard(t.substring(selStart(), selEnd()));
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl && hasSelection()) {
                    client.keyboard.setClipboard(t.substring(selStart(), selEnd()));
                    deleteSelection();
                    persist();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    pasteClipboard();
                    return true;
                }
            }
            default -> {
            }
        }
        return super.keyPressed(input);
    }

    /** Strg+V: beliebig langer Text, Auto-Pages, Zusammenfassung. */
    private void pasteClipboard() {
        String raw = client.keyboard.getClipboard();
        String normalized;
        if (formattingActive()) {
            // TextNormalizer entfernt §-Codes — daher zuerst § -> & schützen,
            // dann normalisieren, danach (optional) zurück zu § für die Vorschau.
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

    /** Getippter Typ-Prefix nach "{{" vor dem Cursor, null = kein Vorschlagskontext. */
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

    /** Auswahl übernehmen: fehlenden Typ-Rest + ":" einfügen. */
    private void acceptSuggestion(String type) {
        String prefix = suggestPrefix();
        if (prefix == null) {
            return;
        }
        insert(type.substring(prefix.length()) + ":");
        suggestIndex = 0;
    }

    /** Tab: Platzhalter am Cursor auflösen, sonst zum nächsten/vorherigen springen. */
    private void handleTab(boolean shift) {
        String t = text();
        LetterPlaceholder at = LetterPlaceholderParser.at(t, cursor);
        if (!shift && at != null && !currentLocked()) {
            PlaceholderResolveResult result = placeholderService.resolve(at);
            if (result.ok()) {
                setText(placeholderService.apply(t, result));
                cursor = at.start() + result.resolved().length();
                selAnchor = -1;
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
        if (target != null) {
            cursor = target.start() + 2;
            selAnchor = -1;
        }
    }

    private void moveCursor(int to, boolean shift) {
        if (shift && selAnchor < 0) {
            selAnchor = cursor;
        }
        if (!shift) {
            selAnchor = -1;
        }
        int hi = text().length();
        int lo = editableStart();
        if (lo == Integer.MAX_VALUE) {
            // Ganze Seite gesperrt: Cursor ans Ende, kein Editieren.
            cursor = hi;
            return;
        }
        cursor = Math.max(lo, Math.max(0, Math.min(to, hi)));
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
            return formattingSidebar.mouseClicked(click.x(), click.y(), click.button());
        }
        int tx = panelX() + TEXT_X;
        int ty = panelY() + TEXT_Y;
        List<int[]> spans = lineSpans();
        if (click.button() == 0 && click.x() >= tx - 4 && click.x() <= tx + 132
                && click.y() >= ty - 2 && click.y() <= ty + spans.size() * LINE_H + 2) {
            int line = Math.max(0, Math.min((int) ((click.y() - ty) / LINE_H), spans.size() - 1));
            int[] span = spans.get(line);
            String lineText = text().substring(span[0], span[1]);
            int col = colForX(lineText, (int) (click.x() - tx));
            moveCursor(span[0] + col, false);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private int colForX(String lineText, int relX) {
        int best = lineText.length();
        for (int i = 0; i <= lineText.length(); i++) {
            if (textRenderer.getWidth(lineText.substring(0, i)) > relX) {
                best = Math.max(0, i - 1);
                break;
            }
        }
        return best;
    }

    // ---- Buttons -----------------------------------------------------------------

    @Override
    protected void init() {
        reflowAllPages();
        if (config.letter.formattingEnabled) {
            formattingSidebar = new LetterFormattingSidebar(this::insertFormattingCode);
        }
        int px = panelX();
        int py = panelY();
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> switchPage(-1))
                .dimensions(px + 8, py + PANEL_H - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> switchPage(1))
                .dimensions(px + 30, py + PANEL_H - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            if (currentLocked()) {
                return; // nicht in den gesperrten Altbestand einfügen
            }
            draft.pages.add(page + 1, "");
            switchPage(1);
            persist();
        }).dimensions(px + 52, py + PANEL_H - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> {
            if (!currentLocked() && draft.pages.size() > lockedPages() + 1) {
                draft.pages.remove(page);
                page = Math.max(lockedPages(), Math.min(page, draft.pages.size() - 1));
                cursor = Math.min(cursor, text().length());
                persist();
            }
        }).dimensions(px + 74, py + PANEL_H - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.send"), b -> {
                    // „Schreiben": Brief sofort via /letter schreiben (Buch
                    // entsteht), Editor schließen, dann entscheidet der Spieler im
                    // Chat-Prompt über den Abschluss (Verschicken/Verkünden/Schließen).
                    persist();
                    LetterServices.startWrite(config, draft);
                    client.setScreen(null);
                    LetterActionPrompt.show(config, draft);
                })
                .dimensions(px + PANEL_W - 64, py + PANEL_H - 26, 56, 18).build());
        // Sekundärleiste: Parameter-Prüfen als Icon + Entwürfe daneben
        ButtonWidget check = ButtonWidget.builder(Text.empty(), b -> checkPlaceholders())
                .dimensions(px, py + PANEL_H + 4, 18, 16).build();
        check.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.translatable("ottoextra.letter.checkPlaceholders")));
        addDrawableChild(check);
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.drafts"), b -> {
                    persist();
                    client.setScreen(new SavedDraftsScreen(this, config, draft));
                }).dimensions(px + 22, py + PANEL_H + 4, PANEL_W - 22, 16).build());
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

    // ---- Render -----------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, PAPER_DARK);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, PAPER_COLOR);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.pageIndicator",
                page + 1, draft.pages.size()), px + TEXT_X, py + 12, TEXT_COLOR, false);
        if (hasLockedHere()) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.book.locked"),
                    px + TEXT_X + 44, py + 12, TEXT_COLOR_LOCKED, false);
        }

        String t = text();
        int lockStart = editableStart(); // MAX_VALUE = ganze Seite gesperrt
        List<int[]> spans = lineSpans();
        int maxLines = maxLinesPerPage();
        for (int i = 0; i < Math.min(spans.size(), maxLines); i++) {
            int[] span = spans.get(i);
            String lineText = t.substring(span[0], span[1]);
            int y = py + TEXT_Y + i * LINE_H;
            ctx.fill(px + TEXT_X - 2, y + LINE_H - 2, px + TEXT_X + 130, y + LINE_H - 1, PAPER_LINE);
            // Selektion hinterlegen
            if (hasSelection() && selEnd() > span[0] && selStart() < span[1]) {
                int s = Math.max(selStart(), span[0]) - span[0];
                int e = Math.min(selEnd(), span[1]) - span[0];
                int x1 = px + TEXT_X + textRenderer.getWidth(lineText.substring(0, s));
                int x2 = px + TEXT_X + textRenderer.getWidth(lineText.substring(0, e));
                ctx.fill(x1, y - 1, x2, y + 9, SELECTION_COLOR);
            }
            // Zeile an der Lock-Grenze splitten: gesperrter Teil faded (ohne
            // §-Codes, sonst überschreiben Farbcodes die Deckkraft), editierbarer
            // Teil mit normaler Live-Formatierung.
            int split = lockStart == Integer.MAX_VALUE
                    ? lineText.length()
                    : Math.max(0, Math.min(lockStart - span[0], lineText.length()));
            int x = px + TEXT_X;
            if (split > 0) {
                // Gesperrten Text mit Original-Farben/Formatierung zeigen, aber
                // entsättigt + verblasst — so sieht man, ob/wie vorher formatiert war.
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
            // Cursor (nur im editierbaren Bereich)
            if (lockStart != Integer.MAX_VALUE && cursor >= Math.max(span[0], lockStart)
                    && cursor <= span[1]
                    && i == lineIndexOf(cursor, spans)
                    && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = px + TEXT_X + textRenderer.getWidth(
                        lineText.substring(0, cursor - span[0]));
                ctx.fill(cx, y - 1, cx + 1, y + 9, TEXT_COLOR);
            }
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2,
                    py + PANEL_H + 24, 0xFFE6C8A9);
        }
        renderSuggestions(ctx, px, py, spans);
        super.render(ctx, mouseX, mouseY, delta);
        // Icon über dem (leeren) Parameter-Prüfen-Button
        ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, CHECK_ICON,
                px + 1, py + PANEL_H + 4, 0f, 0f, 16, 16, 16, 16);
        // Formatierungs-Sidebar zuletzt (oben), inkl. Tooltips
        if (sidebarVisible()) {
            formattingSidebar.setBounds(sidebarX(), py);
            formattingSidebar.render(ctx, textRenderer, mouseX, mouseY);
        }
    }

    /** Vorschlags-Popup unter der Cursorzeile: {{name|title|full|mc -> Typ + ":". */
    private void renderSuggestions(DrawContext ctx, int px, int py, List<int[]> spans) {
        java.util.List<String> sugg = suggestions();
        if (sugg.isEmpty()) {
            return;
        }
        suggestIndex = Math.min(suggestIndex, sugg.size() - 1);
        int line = lineIndexOf(cursor, spans);
        int x = px + TEXT_X + 8;
        int y = py + TEXT_Y + (line + 1) * LINE_H + 2;
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

    /**
     * §-codierten Text in ein gestyltes {@link MutableText} mit ENTSÄTTIGTEN
     * Farben übersetzen — Farben/Formatierung des alten Buchinhalts bleiben
     * erkennbar, wirken aber verblasst. Vanilla-Semantik: Farbcode setzt die
     * Formatierung zurück, §r setzt alles zurück, Formatcodes addieren.
     */
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
            // Farbcode setzt die Formatierung zurück (Vanilla-Verhalten)
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

    /** Farbe entsättigen (Richtung Graustufe) und Richtung Papierfarbe verblassen. */
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
