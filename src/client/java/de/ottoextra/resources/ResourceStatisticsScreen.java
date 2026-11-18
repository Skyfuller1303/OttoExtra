package de.ottoextra.resources;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

final class ResourceStatisticsScreen extends Screen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final int[] COLUMNS = {0, 22, 51, 61, 72, 84, 100};
    private static final String[] HEADERS = {"time", "mine", "attempts", "successes", "failures", "rate"};
    private static final int LIST_TOP = 127;
    private final Screen parent;
    private final ResourceStatistics statistics;
    private String resourceId;
    private String selectedRunId;
    private ResourceStatistics.RunSummary selectedRun;
    private int page;
    private int pageCount;
    private int rowsPerPage;
    private int rowHeight;
    private int panelWidth;
    private int left;
    private long shownRevision;
    private List<ResourceStatistics.Row> rows = List.of();
    private List<ResourceStatistics.RunSummary> runs = List.of();
    private ResourceStatistics.Totals totals;

    ResourceStatisticsScreen(Screen parent, ResourceStatistics statistics) {
        super(Text.translatable("ottoextra.resources.stats.runs"));
        this.parent = parent;
        this.statistics = statistics;
    }

    @Override
    protected void init() {
        clearChildren();
        shownRevision = statistics.revision();
        runs = statistics.runs(resourceId);
        selectedRun = runs.stream().filter(run -> run.id().equals(selectedRunId)).findFirst().orElse(null);
        rows = selectedRun == null ? List.of() : statistics.rowsForRun(selectedRun.id(), resourceId);
        totals = selectedRun == null ? statistics.totals(resourceId) : selectedRun.totals();
        rowHeight = selectedRun == null ? 24 : 38;
        rowsPerPage = Math.max(1, (height - 44 - LIST_TOP) / rowHeight);
        int rowCount = selectedRun == null ? runs.size() : rows.size();
        pageCount = Math.max(1, (rowCount + rowsPerPage - 1) / rowsPerPage);
        page = Math.clamp(page, 0, pageCount - 1);
        panelWidth = Math.min(860, width - 24);
        left = (width - panelWidth) / 2;

        var resources = statistics.rows(null);
        String resourceName = resourceId == null ? Text.translatable("ottoextra.resources.stats.all").getString()
                : resources.stream().filter(row -> row.resourceId().equals(resourceId))
                        .map(ResourceStatistics.Row::resourceName).findFirst().orElse(resourceId);
        Text filterLabel = Text.translatable("ottoextra.resources.stats.filter", resourceName);
        int filterWidth = selectedRun == null ? panelWidth : panelWidth - 110;
        addDrawableChild(tooltip(button(left, 84, filterWidth, 20, trim(filterLabel, filterWidth - 12), button -> {
            List<String> ids = resources.stream().map(ResourceStatistics.Row::resourceId).distinct().toList();
            int current = resourceId == null ? -1 : ids.indexOf(resourceId);
            resourceId = current + 1 < ids.size() ? ids.get(current + 1) : null;
            selectedRunId = null;
            page = 0;
            init();
        }), filterLabel));
        if (selectedRun != null) {
            addDrawableChild(button(left + panelWidth - 106, 84, 106, 20,
                    Text.translatable("ottoextra.resources.stats.runs"), button -> {
                        selectedRunId = null;
                        page = 0;
                        init();
                    }));
        }

        for (int index = page * rowsPerPage; index < Math.min(rowCount, (page + 1) * rowsPerPage); index++) {
            int y = LIST_TOP + (index - page * rowsPerPage) * rowHeight;
            if (selectedRun == null) {
                var run = runs.get(index);
                addDrawableChild(tooltip(button(left, y, panelWidth, 22, Text.empty(), button -> {
                    selectedRunId = run.id();
                    page = 0;
                    init();
                }), runTooltip(run)));
            } else {
                var row = rows.get(index);
                Text rawMessage = ResourceStatistics.PICKUP_MESSAGE.equals(row.message())
                        ? Text.translatable("ottoextra.resources.stats.pickup") : Text.literal(row.message());
                Text message = Text.literal(row.count() + " × ").append(rawMessage);
                addDrawableChild(tooltip(button(left, y + 11, panelWidth - 110, 20, trim(message, panelWidth - 122),
                        button -> { }), rawMessage));
                ButtonWidget classify = addDrawableChild(tooltip(button(left + panelWidth - 106, y + 11, 106, 20,
                        Text.translatable(row.outcome().translationKey()), button -> {
                            statistics.classify(row.message(), row.outcome().next());
                            init();
                        }), Text.translatable(row.pickupConfirmed()
                                ? "ottoextra.resources.stats.pickup_confirmed" : "ottoextra.resources.stats.classify")));
                classify.active = !row.pickupConfirmed();
            }
        }

        ButtonWidget previous = addDrawableChild(button(left, height - 30, 36, 20, Text.literal("‹"), button -> {
            page--;
            init();
        }));
        previous.active = page > 0;
        ButtonWidget next = addDrawableChild(button(left + panelWidth - 36, height - 30, 36, 20, Text.literal("›"),
                button -> {
                    page++;
                    init();
                }));
        next.active = page + 1 < pageCount;
        addDrawableChild(button(width / 2 - 60, height - 30, 120, 20, Text.translatable("gui.done"),
                button -> close()));
    }

    /** Slate button without text shadow (the book's style). */
    private static ButtonWidget button(int x, int y, int w, int h, Text label, ButtonWidget.PressAction action) {
        return new ResourceBook.Button(x, y, w, h, label, ResourceBook.Tone.SLATE, action);
    }

    private static ButtonWidget tooltip(ButtonWidget button, Text text) {
        button.setTooltip(Tooltip.of(text));
        return button;
    }

    @Override
    public void tick() {
        if (shownRevision != statistics.revision()) init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xEE10151D);
        super.render(context, mouseX, mouseY, delta);
        centered(context, selectedRun == null ? title : runName(selectedRun), 18, 0xFFE4C477);
        centered(context, Text.translatable("ottoextra.resources.stats.results", totals.successes(), totals.failures()), 39, 0xFFF2F2F2);
        centered(context, Text.translatable("ottoextra.resources.stats.rate", totals.attempts(), rate(totals)), 52, 0xFF9FC7A8);
        centered(context, Text.translatable("ottoextra.resources.stats.unclassified", totals.unknown(), totals.ignored()), 65, 0xFF9AA6B5);
        if (selectedRun == null) {
            for (int column = 0; column < HEADERS.length; column++) {
                cell(context, Text.translatable("ottoextra.resources.stats.column." + HEADERS[column]
                        + (panelWidth < 560 ? ".short" : "")), column, 113, 0xFF9AA6B5);
            }
        } else {
            centered(context, Text.translatable("ottoextra.resources.stats.hint"), 111, 0xFF9AA6B5);
        }
        int rowCount = selectedRun == null ? runs.size() : rows.size();
        if (rowCount == 0) centered(context, Text.translatable("ottoextra.resources.stats.empty"), 141, 0xFF9AA6B5);
        for (int index = page * rowsPerPage; index < Math.min(rowCount, (page + 1) * rowsPerPage); index++) {
            int y = LIST_TOP + (index - page * rowsPerPage) * rowHeight;
            if (selectedRun == null) {
                var run = runs.get(index);
                cell(context, Text.literal(isLegacy(run) ? "–" : date(run.startedAt())), 0, y + 7, 0xFFBDC6D2);
                Text name = runName(run).copy().append(run.active() ? " *" : "");
                cell(context, name, 1, y + 7, run.active() ? 0xFFE4C477 : 0xFFF2F2F2);
                cell(context, Text.literal(Long.toString(run.totals().attempts())), 2, y + 7, 0xFFF2F2F2);
                cell(context, Text.literal(Long.toString(run.totals().successes())), 3, y + 7, 0xFF9FC7A8);
                cell(context, Text.literal(Long.toString(run.totals().failures())), 4, y + 7, 0xFFE1A099);
                cell(context, Text.literal(rate(run.totals())), 5, y + 7, 0xFFF2F2F2);
            } else {
                context.drawText(textRenderer, trim(Text.literal(rows.get(index).resourceName()), panelWidth),
                        left, y, 0xFFE4C477, false);
            }
        }
        centered(context, statistics.storageError() == null
                ? Text.translatable("ottoextra.resources.stats.page", page + 1, pageCount)
                : Text.translatable("ottoextra.resources.stats.storage_error"), height - 44,
                statistics.storageError() == null ? 0xFF9AA6B5 : 0xFFFF7777);
    }

    private void cell(DrawContext context, Text text, int column, int y, int color) {
        int x = left + panelWidth * COLUMNS[column] / 100 + 5;
        int end = left + panelWidth * COLUMNS[column + 1] / 100 - 5;
        Text clipped = trim(text, end - x);
        if (column >= 2) x = end - textRenderer.getWidth(clipped);
        context.drawText(textRenderer, clipped, x, y, color, false);
    }

    private Text runTooltip(ResourceStatistics.RunSummary run) {
        String regionName = run.region().isBlank()
                ? Text.translatable("ottoextra.resources.stats.unknown_region").getString() : run.region();
        String end = run.active() ? Text.translatable("ottoextra.resources.stats.active").getString() : date(run.endedAt());
        Text details = isLegacy(run) ? Text.translatable("ottoextra.resources.stats.legacy_hint")
                : Text.translatable("ottoextra.resources.stats.run_details", run.resourceName(), date(run.startedAt()),
                        end, regionName, run.totals().unknown(), run.totals().ignored());
        return details.copy().append("\n").append(Text.translatable("ottoextra.resources.stats.results",
                        run.totals().successes(), run.totals().failures()))
                .append("\n").append(Text.translatable("ottoextra.resources.stats.rate", run.totals().attempts(), rate(run.totals())));
    }

    private static boolean isLegacy(ResourceStatistics.RunSummary run) {
        return ResourceStatistics.LEGACY_RUN_ID.equals(run.id());
    }

    private static Text runName(ResourceStatistics.RunSummary run) {
        return isLegacy(run) ? Text.translatable("ottoextra.resources.stats.legacy") : Text.literal(run.resourceName());
    }

    private static String date(long timestamp) {
        return DATE.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }

    private static String rate(ResourceStatistics.Totals totals) {
        return totals.attempts() == 0 ? "–" : String.format(Locale.ROOT, "%.1f %%", totals.successRate());
    }

    private void centered(DrawContext context, Text text, int y, int color) {
        Text shown = trim(text, width - 16);
        context.drawText(textRenderer, shown, (width - textRenderer.getWidth(shown)) / 2, y, color, false);
    }

    private Text trim(Text text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        return Text.literal(textRenderer.trimToWidth(text.getString(), Math.max(0, maxWidth - textRenderer.getWidth("…"))) + "…");
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        statistics.save();
        if (client != null) client.setScreen(parent);
    }
}
