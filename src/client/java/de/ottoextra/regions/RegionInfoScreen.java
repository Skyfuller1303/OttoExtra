package de.ottoextra.regions;

import de.ottoextra.api.model.CapabilityEntry;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionCapabilities;
import de.ottoextra.api.model.RegionInfo;
import de.ottoextra.api.model.RegionRecord;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public final class RegionInfoScreen extends Screen {

    private enum Tab { OVERVIEW, PLAYERS, VASSALS }

    private static final int COL_PANEL = 0xE0182230;
    private static final int COL_SIDEBAR = 0xE61B2735;
    private static final int COL_BORDER = 0xFF344459;
    private static final int COL_TITLE = 0xFFF2E7C9;
    private static final int COL_BODY = 0xFFCBD6E0;
    private static final int COL_MUTED = 0xFF8C9BAA;
    private static final int COL_RANK = 0xFF9BC7DC;
    private static final int COL_CARD = 0x66101820;

    private final Screen parent;
    private final String regionName;

    private Tab tab = Tab.OVERVIEW;
    private double scroll = 0;
    private int contentHeight = 0;

    private RegionRecord region;
    private FactionRecord faction;
    private List<PlayerRecord> players;
    private boolean playersLoading = false;
    private long playerRequestGeneration;
    private final ViewRequests requests = new ViewRequests();
    private List<FactionRecord> vassals = List.of();

    private ButtonWidget tabOverview;
    private ButtonWidget tabPlayers;
    private ButtonWidget tabVassals;
    private final List<ButtonWidget> rowButtons = new ArrayList<>();

    /** Applies async results only on the client executor and to the current view lifetime. */
    static final class ViewRequests {
        private long generation;

        void invalidate() {
            generation++;
        }

        <T> void whenComplete(CompletableFuture<T> request, Executor clientExecutor,
                              BooleanSupplier currentView, BiConsumer<T, Throwable> apply) {
            long requestGeneration = generation;
            request.whenComplete((value, error) -> clientExecutor.execute(() -> {
                if (generation == requestGeneration && currentView.getAsBoolean()) {
                    apply.accept(value, error);
                }
            }));
        }
    }

    record DataSnapshot(RegionRecord region, FactionRecord faction, List<FactionRecord> vassals) {
        static DataSnapshot read(RegionDataService data, String name) {
            FactionRecord faction = data.factionForRegion(name).orElse(null);
            return new DataSnapshot(data.regionByName(name).orElse(null), faction,
                    faction == null ? List.of() : data.vassalsOf(faction));
        }
    }

    public RegionInfoScreen(String regionName, Screen parent) {
        super(Text.translatable("ottoextra.regions.screen.title"));
        this.regionName = regionName == null ? "" : regionName;
        this.parent = parent;
    }

    public static RegionInfoScreen current(Screen parent) {
        RegionState state = RegionMessageService.current();
        return new RegionInfoScreen(state != null ? state.regionName() : "", parent);
    }

    private int panelX() {
        return Math.max(8, (width - panelW()) / 2);
    }

    private int panelY() {
        return Math.max(8, (height - panelH()) / 2);
    }

    private int panelW() {
        return Math.min(width - 16, 360);
    }

    private int panelH() {
        return Math.min(height - 16, 220);
    }

    private int sidebarW() {
        return 104;
    }

    private int contentX() {
        return panelX() + sidebarW() + 8;
    }

    private int contentY() {
        return panelY() + 8;
    }

    private int contentW() {
        return panelX() + panelW() - 8 - contentX();
    }

    private int contentH() {
        return panelH() - 16;
    }

    @Override
    protected void init() {
        loadData();

        int bx = panelX() + 6;
        int bw = sidebarW() - 12;
        int by = panelY() + 78;

        tabOverview = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.regions.tab.overview"), b -> switchTab(Tab.OVERVIEW))
                .dimensions(bx, by, bw, 18).build());
        by += 21;
        tabPlayers = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.regions.tab.players", playerCountLabel()), b -> switchTab(Tab.PLAYERS))
                .dimensions(bx, by, bw, 18).build());
        by += 21;
        tabVassals = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.regions.tab.vassals", vassals.size()), b -> switchTab(Tab.VASSALS))
                .dimensions(bx, by, bw, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(bx, panelY() + panelH() - 26, bw, 18).build());

        updateTabState();
        rebuildRowButtons();
        loadPlayers();
        RegionDataService data = RegionsServices.data();
        if (data != null && !regionName.isBlank()) {
            requests.whenComplete(data.requestRegionDetailAsync(regionName), client::execute,
                    () -> client.currentScreen == this && RegionsServices.data() == data,
                    (ignored, error) -> {
                        if (error != null) return;
                        if (loadData()) refreshDataWidgets();
                        loadPlayers();
                    });
        }
    }

    private boolean loadData() {
        RegionDataService data = RegionsServices.data();
        if (data == null || regionName.isBlank()) {
            return false;
        }
        DataSnapshot snapshot = DataSnapshot.read(data, regionName);
        boolean changed = !Objects.equals(region, snapshot.region())
                || !Objects.equals(faction, snapshot.faction()) || !vassals.equals(snapshot.vassals());
        region = snapshot.region();
        FactionRecord refreshedFaction = snapshot.faction();
        if (!Objects.equals(faction, refreshedFaction)) {
            playerRequestGeneration++;
            players = null;
            playersLoading = false;
        }
        faction = refreshedFaction;
        vassals = snapshot.vassals();
        return changed;
    }

    private void switchTab(Tab newTab) {
        tab = newTab;
        scroll = 0;
        updateTabState();
        loadPlayers();
        rebuildRowButtons();
    }

    private void loadPlayers() {
        if (tab != Tab.PLAYERS || players != null || playersLoading || faction == null) return;
        RegionDataService data = RegionsServices.data();
        if (data == null) return;
        FactionRecord requestedFaction = faction;
        long requestGeneration = ++playerRequestGeneration;
        playersLoading = true;
        requests.whenComplete(data.factionPlayers(requestedFaction), client::execute,
                () -> client.currentScreen == this && RegionsServices.data() == data
                        && playerRequestGeneration == requestGeneration
                        && Objects.equals(faction, requestedFaction),
                (list, error) -> {
                    players = error == null && list != null ? list : List.of();
                    playersLoading = false;
                    refreshDataWidgets();
                });
    }

    private void refreshDataWidgets() {
        tabPlayers.setMessage(Text.translatable("ottoextra.regions.tab.players", playerCountLabel()));
        tabVassals.setMessage(Text.translatable("ottoextra.regions.tab.vassals", vassals.size()));
        scroll = 0;
        contentHeight = 0;
        rebuildRowButtons();
    }

    private void updateTabState() {
        tabOverview.active = tab != Tab.OVERVIEW;
        tabPlayers.active = tab != Tab.PLAYERS;
        tabVassals.active = tab != Tab.VASSALS;
    }

    private void rebuildRowButtons() {
        rowButtons.forEach(this::remove);
        rowButtons.clear();
        if (tab != Tab.VASSALS) {
            return;
        }
        int rowH = 22;
        int y = contentY() + 14 - (int) scroll;
        for (FactionRecord vassal : vassals) {
            if (y + rowH > contentY() && y < contentY() + contentH()) {
                String target = vassal.region_name() != null && !vassal.region_name().isBlank()
                        ? vassal.region_name() : vassal.name();
                ButtonWidget btn = ButtonWidget.builder(
                                Text.translatable("ottoextra.regions.more"), b ->
                                        client.setScreen(new RegionInfoScreen(target, this)))
                        .dimensions(contentX() + contentW() - 62, y + 2, 60, 16)
                        .build();
                rowButtons.add(btn);
                addDrawableChild(btn);
            }
            y += rowH;
        }
    }

    private String playerCountLabel() {
        if (players != null) {
            return Integer.toString(players.size());
        }
        return faction != null ? Integer.toString(faction.player_count()) : "?";
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, COL_BORDER);
        ctx.fill(px, py, px + pw, py + ph, COL_PANEL);
        ctx.fill(px, py, px + sidebarW(), py + ph, COL_SIDEBAR);
        ctx.fill(px + sidebarW(), py, px + sidebarW() + 1, py + ph, COL_BORDER);

        renderSidebarHeader(ctx);

        ctx.enableScissor(contentX(), contentY(), contentX() + contentW(), contentY() + contentH());
        switch (tab) {
            case OVERVIEW -> renderOverview(ctx);
            case PLAYERS -> renderPlayers(ctx);
            case VASSALS -> renderVassals(ctx);
        }
        ctx.disableScissor();

        if (contentHeight > contentH()) {
            int track = contentH();
            int thumb = Math.max(12, track * track / contentHeight);
            int maxScroll = contentHeight - contentH();
            int thumbY = contentY() + (int) ((track - thumb) * (scroll / maxScroll));
            int sx = contentX() + contentW() + 2;
            ctx.fill(sx, contentY(), sx + 2, contentY() + track, 0x33FFFFFF);
            ctx.fill(sx, thumbY, sx + 2, thumbY + thumb, 0xAA9BC7DC);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSidebarHeader(DrawContext ctx) {
        int px = panelX();
        int py = panelY();
        int cx = px + sidebarW() / 2;

        Identifier banner = faction != null && RegionsServices.banners() != null
                ? RegionsServices.banners().bannerFor(faction).orElse(null) : null;
        int bSize = 48;
        int bx = cx - bSize / 2;
        int byy = py + 8;
        ctx.fill(bx - 2, byy - 2, bx + bSize + 2, byy + bSize + 2, COL_BORDER);
        if (banner != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, bx, byy,
                    0f, 0f, bSize, bSize, bSize, bSize);
        } else {
            ctx.fill(bx, byy, bx + bSize, byy + bSize, 0xFF101820);
            ctx.drawCenteredTextWithShadow(textRenderer, "?", cx, byy + bSize / 2 - 4, COL_MUTED);
        }

        String rank = faction != null && faction.rank_name() != null && !faction.rank_name().isBlank()
                ? faction.rank_name()
                : Text.translatable("ottoextra.regions.rank.fallback").getString();
        ctx.drawCenteredTextWithShadow(textRenderer, rank, cx, py + 60, COL_RANK);
        String name = regionName.isBlank()
                ? Text.translatable("ottoextra.regions.none").getString() : regionName;
        ctx.drawCenteredTextWithShadow(textRenderer,
                textRenderer.trimToWidth(name, sidebarW() - 8), cx, py + 70, COL_TITLE);
    }

    private void renderOverview(DrawContext ctx) {
        int x = contentX();
        int y = contentY() - (int) scroll;
        int w = contentW() - 6;
        int line = textRenderer.fontHeight + 2;
        int startY = y;

        if (faction == null && region == null) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.regions.no_details"),
                    x, y, COL_MUTED, true);
            contentHeight = line;
            return;
        }

        String description = resolveDescription();
        if (!description.isBlank()) {
            Text translatedDescription = translatedRegionText(description);
            for (OrderedText l : textRenderer.wrapLines(translatedDescription, w)) {
                ctx.drawText(textRenderer, l, x, y, COL_BODY, true);
                y += line;
            }
            y += 4;
        }

        String lead = leadSentence();
        for (OrderedText l : textRenderer.wrapLines(translatedRegionText(lead), w)) {
            ctx.drawText(textRenderer, l, x, y, COL_BODY, true);
            y += line;
        }
        y += 6;

        RegionCapabilities caps = capabilities();
        if (caps != null) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.regions.properties"),
                    x, y, COL_TITLE, true);
            y += line + 2;
            y = renderPropertyCard(ctx, x, y, w, caps.agriculture(),
                    firstMappedDescription(caps, "agriculture"));
            y = renderPropertyCard(ctx, x, y, w, caps.mines(),
                    firstMappedDescription(caps, "mines"));
            y = renderPropertyCard(ctx, x, y, w, caps.bonus(),
                    firstMappedDescription(caps, "bonus"));
        }

        contentHeight = y + (int) scroll - startY - (contentY() - startY) + 4;
        contentHeight = Math.max(contentHeight, y - (contentY() - (int) scroll)) ;
    }

    private int renderPropertyCard(DrawContext ctx, int x, int y, int w, String title, String body) {
        if (title == null || title.isBlank()) {
            return y;
        }
        int line = textRenderer.fontHeight + 2;
        List<OrderedText> bodyLines = body == null || body.isBlank()
                ? List.of() : textRenderer.wrapLines(translatedRegionText(body), w - 10);
        int cardH = 6 + line + bodyLines.size() * line;
        ctx.fill(x, y, x + w, y + cardH, COL_CARD);
        ctx.fill(x, y, x + 2, y + cardH, COL_RANK);
        ctx.drawText(textRenderer, title, x + 6, y + 4, COL_TITLE, true);
        int by = y + 4 + line;
        for (OrderedText l : bodyLines) {
            ctx.drawText(textRenderer, l, x + 6, by, COL_MUTED, true);
            by += line;
        }
        return y + cardH + 4;
    }

    private static Text translatedRegionText(String text) {
        return de.ottoextra.addon.OttoExtraAddons.processDisplayText(
                "REGION", Text.literal(text));
    }

    private void renderPlayers(DrawContext ctx) {
        int x = contentX();
        int y = contentY() - (int) scroll;
        int w = contentW() - 6;
        int line = textRenderer.fontHeight + 2;

        if (playersLoading) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.regions.loading"), x, y, COL_MUTED, true);
            contentHeight = line;
            return;
        }
        List<PlayerRecord> list = players;
        if (list == null || list.isEmpty()) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.regions.no_players"), x, y, COL_MUTED, true);
            contentHeight = line;
            return;
        }

        int rowH = 20;
        for (PlayerRecord p : list) {
            String display = (p.title() != null && !p.title().isBlank() ? p.title() + " " : "")
                    + (p.name() != null ? p.name() : "?");
            String rank = p.rank() != null && !p.rank().isBlank() ? p.rank() : "-";
            String money = p.money() == null ? "–"
                    : Text.translatable("ottoextra.regions.money", p.money()).getString();

            ctx.drawText(textRenderer, textRenderer.trimToWidth(display, w - 110), x, y + 2, COL_BODY, true);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(rank, 56), x + w - 108, y + 2, COL_MUTED, true);
            ctx.drawText(textRenderer, money, x + w - 50, y + 2, COL_RANK, true);
            ctx.fill(x, y + rowH - 2, x + w, y + rowH - 1, 0x22FFFFFF);
            y += rowH;
        }
        contentHeight = list.size() * rowH;
    }

    private void renderVassals(DrawContext ctx) {
        int x = contentX();
        int y = contentY() - (int) scroll;
        int w = contentW() - 6;
        int line = textRenderer.fontHeight + 2;

        if (vassals.isEmpty()) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.regions.no_vassals"), x, y, COL_MUTED, true);
            contentHeight = line;
            return;
        }
        ctx.drawText(textRenderer, Text.translatable("ottoextra.regions.tab.vassals", vassals.size()),
                x, y, COL_TITLE, true);
        y += 14;

        int rowH = 22;
        for (FactionRecord vassal : vassals) {
            Identifier banner = RegionsServices.banners() != null
                    ? RegionsServices.banners().bannerFor(vassal).orElse(null) : null;
            if (banner != null) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, x, y + 2,
                        0f, 0f, 16, 16, 16, 16);
            } else {
                ctx.fill(x, y + 2, x + 16, y + 18, 0xFF101820);
            }
            String name = vassal.region_name() != null && !vassal.region_name().isBlank()
                    ? vassal.region_name() : String.valueOf(vassal.name());
            ctx.drawText(textRenderer, textRenderer.trimToWidth(name, w - 150), x + 20, y + 2, COL_BODY, true);
            ctx.drawText(textRenderer,
                    Text.translatable("ottoextra.regions.residents", vassal.player_count()).getString(),
                    x + 20, y + 12, COL_MUTED, true);
            y += rowH;
        }
        contentHeight = 14 + vassals.size() * rowH;
    }

    private RegionCapabilities capabilities() {
        if (region != null && region.region_capabilities() != null) {
            return region.region_capabilities();
        }
        return faction != null ? faction.region_capabilities() : null;
    }

    private String resolveDescription() {
        RegionCapabilities caps = capabilities();
        if (caps != null && caps.region_info() != null) {
            RegionInfo info = caps.region_info();
            if (info.note() != null && !info.note().isBlank()) {
                return info.note();
            }
        }
        if (region != null && region.region_info() != null
                && region.region_info().note() != null && !region.region_info().note().isBlank()) {
            return region.region_info().note();
        }
        if (faction != null && faction.description() != null) {
            return faction.description();
        }
        return "";
    }

    private String leadSentence() {
        String rank = faction != null && faction.rank_name() != null && !faction.rank_name().isBlank()
                ? faction.rank_name()
                : Text.translatable("ottoextra.regions.rank.fallback").getString();
        int count = faction != null ? faction.player_count() : 0;
        String leader = faction != null ? faction.leader_name() : null;
        if (leader != null && !leader.isBlank()) {
            return Text.translatable("ottoextra.regions.lead.full", rank, leader, count).getString();
        }
        return Text.translatable("ottoextra.regions.lead.short", rank, count).getString();
    }

    private String firstMappedDescription(RegionCapabilities caps, String which) {
        if (caps.mapped() == null) {
            return "";
        }
        List<CapabilityEntry> list = switch (which) {
            case "agriculture" -> caps.mapped().agriculture();
            case "mines" -> caps.mapped().mines();
            case "bonus" -> caps.mapped().bonus();
            default -> null;
        };
        if (list == null || list.isEmpty()) {
            return "";
        }
        CapabilityEntry first = list.get(0);
        return first.description() != null ? first.description() : "";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (contentHeight > contentH()) {
            scroll = Math.max(0, Math.min(contentHeight - contentH(), scroll - vertical * 12));
            if (tab == Tab.VASSALS) {
                rebuildRowButtons();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void removed() {
        requests.invalidate();
        playerRequestGeneration++;
        playersLoading = false;
        super.removed();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
