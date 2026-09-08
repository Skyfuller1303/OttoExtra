package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.announcement.AnnouncementLetterPreflightService;
import de.ottoextra.letter.send.LetterV2Planner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class AnnouncementPreflightScreen extends Screen {

    private final Screen parent;
    private final OttoExtraConfig config;
    private final Flow flow;

    public AnnouncementPreflightScreen(Screen parent, OttoExtraConfig config, LetterDraft draft) {
        super(Text.translatable("ottoextra.letter.preflight.title"));
        this.parent = parent;
        this.config = config;
        this.flow = new Flow(config, draft);
    }

    /** The preflight actions can run without a Minecraft screen or network. */
    static final class Flow {
        private final OttoExtraConfig config;
        private final LetterDraft draft;
        private final Function<LetterDraft, List<String>> preparedPagesProvider;
        private final List<String> preparedPages;
        private AnnouncementLetterPreflightService.Result result;
        private boolean planSendable;

        Flow(OttoExtraConfig config, LetterDraft draft) {
            this(config, draft, LetterServices::snapshotPreparedPages);
        }

        Flow(OttoExtraConfig config, LetterDraft source,
             Function<LetterDraft, List<String>> preparedPagesProvider) {
            this.config = config;
            this.draft = copyDraft(source);
            this.preparedPagesProvider = preparedPagesProvider;
            List<String> snapshot = preparedPagesProvider.apply(source);
            this.preparedPages = snapshot == null ? null : List.copyOf(snapshot);
            draft.pages = new ArrayList<>(preparedPages == null
                    ? LetterServices.previewPages(config, source)
                    : LetterV2Planner.displayPages(preparedPages));
            recheck();
        }

        private static LetterDraft copyDraft(LetterDraft source) {
            LetterDraft copy = new LetterDraft();
            copy.pages = new ArrayList<>(source.pages);
            copy.meta.draftId = source.meta.draftId;
            copy.meta.name = source.meta.name;
            copy.meta.mode = source.meta.mode;
            copy.meta.importedFromBook = source.meta.importedFromBook;
            copy.meta.pagesOneToOne = source.meta.pagesOneToOne;
            copy.meta.updatedAtMs = source.meta.updatedAtMs;
            copy.meta.lockedPages = source.meta.lockedPages;
            copy.meta.lockedOffset = source.meta.lockedOffset;
            copy.meta.courierInitialPages = new ArrayList<>(source.meta.courierInitialPages);
            copy.meta.flowingDocument = source.meta.flowingDocument;
            copy.meta.flowingDocumentVersion = source.meta.flowingDocumentVersion;
            return copy;
        }

        private AnnouncementLetterPreflightService service() {
            return new AnnouncementLetterPreflightService(
                    config.letter.announcementSafeLinesPerPage,
                    config.letter.announcementSafeCharsPerLine,
                    config.letter.announcementHardLinesPerPage,
                    config.letter.announcementHardCharsPerLine);
        }

        private void recheck() {
            if (preparedPages != null) {
                planSendable = !preparedPages.isEmpty()
                        && preparedPages.equals(preparedPagesProvider.apply(draft))
                        && draft.pages.equals(LetterV2Planner.displayPages(preparedPages));
                result = service().check(draft.meta.draftId, draft.pages);
                return;
            }
            var plan = LetterServices.buildPreflightAnnouncementPlan(config, draft);
            planSendable = !plan.commands().isEmpty() && plan.warnings().isEmpty();
            var sourceCheck = service().check(draft.meta.draftId, draft.pages);
            result = sourceCheck.canSend()
                    ? service().check(draft.meta.draftId, plan.predictedPages()) : sourceCheck;
        }

        boolean confirm(java.util.function.BiPredicate<OttoExtraConfig, LetterDraft> start) {
            recheck();
            return planSendable && result.canSend() && start.test(config, draft);
        }

        void optimize() {
            if (!canOptimize()) {
                return;
            }
            draft.pages = new ArrayList<>(service().optimize(draft.pages));
            recheck();
        }

        boolean canOptimize() {
            return preparedPages == null;
        }
    }

    @Override
    protected void init() {
        int cx = width / 2;
        ButtonWidget send = ButtonWidget.builder(
                Text.translatable("ottoextra.letter.preflight.send"), b -> {

                    if (LetterServices.isSending()) {
                        if (client != null && client.player != null) {
                            client.player.sendMessage(
                                    Text.translatable("ottoextra.letter.write.wait"), false);
                        }
                        return;
                    }

                    if (!flow.confirm(LetterServices::tryStartAnnouncementSend)) return;
                    LetterServices.consumePending();
                    MinecraftClient.getInstance().setScreen(null);
                }).dimensions(cx - 156, height - 30, 100, 20).build();
        send.active = flow.planSendable && flow.result.canSend();
        addDrawableChild(send);
        if (config.letter.announcementAutoOptimizeEnabled) {
            ButtonWidget optimize = ButtonWidget.builder(
                    Text.translatable("ottoextra.letter.preflight.optimize"), b -> {
                        flow.optimize();
                        clearAndInit();
                    }).dimensions(cx - 52, height - 30, 104, 20).build();
            optimize.active = flow.canOptimize();
            if (!optimize.active) {
                optimize.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        "Der Brief ist bereits geschrieben und wird mit diesem Inhalt verkündet.")));
            }
            addDrawableChild(optimize);
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(cx + 56, height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 12, 0xFFE6C8A9);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.letter.preflight.summary2",
                        flow.result.pageCount(), flow.result.totalLines(), flow.result.totalCharacters()),
                width / 2, 24, 0xFFB8A88F);
        if (!LetterServices.hasSubmitCommand(config)) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.letter.preflight.manualSubmitHint"),
                    width / 2, 36, 0xFFB8A88F);
        }
        int y = 50;
        for (var page : flow.result.pages()) {
            if (y > height - 44) {
                ctx.drawCenteredTextWithShadow(textRenderer, "…", width / 2, y, 0xFFB8A88F);
                break;
            }
            int color = !page.ok() ? 0xFFCC8888 : page.clean() ? 0xFF9ACD78 : 0xFFE8C87A;
            String mark = !page.ok() ? "✘" : page.clean() ? "✔" : "!";
            String head = mark + " Seite " + (page.pageIndex() + 1) + ": "
                    + page.usedLines() + "/" + config.letter.announcementSafeLinesPerPage
                    + " Zeilen · längste " + page.maxLineLength() + "/"
                    + config.letter.announcementSafeCharsPerLine;
            ctx.drawTextWithShadow(textRenderer, head, width / 2 - 150, y, color);
            y += 11;
            for (String b : page.blockers()) {
                ctx.drawTextWithShadow(textRenderer, "   • " + b, width / 2 - 150, y, 0xFFCC8888);
                y += 10;
            }
            for (String w : page.warnings()) {
                ctx.drawTextWithShadow(textRenderer, "   • " + w, width / 2 - 150, y, 0xFFE8C87A);
                y += 10;
            }
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
