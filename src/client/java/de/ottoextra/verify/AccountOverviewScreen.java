package de.ottoextra.verify;

import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.PlayerRecord;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class AccountOverviewScreen extends Screen {

    private static final int BG = 0xF0111822;
    private static final int PANEL = 0xF01A2431;
    private static final int CARD = 0xCC222F3E;
    private static final int BORDER = 0xFF394B60;
    private static final int GOLD = 0xFFE8C982;
    private static final int TEXT = 0xFFE7EDF3;
    private static final int MUTED = 0xFF91A1B2;
    private static final int BLUE = 0xFF77C7E8;
    private static final int GREEN = 0xFF61D6A2;
    private static final int YELLOW = 0xFFF3C969;
    private static final URI ACCOUNT_URL = URI.create("https://ottoextra.dev/account");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Screen parent;
    private final AccountLinkConfig linkConfig;
    private final AccountLinkApi linkApi;
    private final OttoExtraApiClient profileApi;
    private final UUID minecraftUuid;
    private final String minecraftName;

    private final AccountOverviewState state =
            new AccountOverviewState(() -> LocalTime.now().format(TIME));

    public AccountOverviewScreen(
            Screen parent,
            AccountLinkConfig linkConfig,
            AccountLinkApi linkApi,
            OttoExtraApiClient profileApi,
            UUID minecraftUuid,
            String minecraftName) {
        super(Text.translatable("ottoextra.account.title"));
        this.parent = parent;
        this.linkConfig = linkConfig;
        this.linkApi = linkApi;
        this.profileApi = profileApi;
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
    }

    private int panelW() {
        return Math.min(width - 16, 430);
    }

    private int panelH() {
        return Math.min(height - 16, 250);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    private int panelY() {
        return (height - panelH()) / 2;
    }

    @Override
    protected void init() {
        int py = panelY();
        int ph = panelH();
        int px = panelX();
        int pw = panelW();
        int buttonY = py + ph - 27;
        int gap = 5;
        int buttonW = Math.max(68, (pw - 24 - gap * 2) / 3);

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.account.refresh"), button -> refresh())
                .dimensions(px + 7, buttonY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.account.web"), button ->
                                Util.getOperatingSystem().open(ACCOUNT_URL))
                .dimensions(px + 7 + buttonW + gap, buttonY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(px + 7 + (buttonW + gap) * 2, buttonY, buttonW, 20).build());

        refresh();
    }

    private void refresh() {
        boolean linked = linkConfig.hasDeviceToken() && !linkConfig.linkPending;
        state.refresh(linked, () -> linked
                        ? linkApi.summary().thenApply(value -> value == null ? null
                                : new AccountOverviewState.Details(value.profile(), value))
                        : profileApi.player(minecraftUuid).thenApply(value ->
                                new AccountOverviewState.Details(value, null)),
                MinecraftClientExecutor::run);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        ctx.fill(px, py, px + pw, py + ph, PANEL);
        ctx.fill(px, py, px + pw, py + 34, BG);
        ctx.fill(px, py + 33, px + pw, py + 34, BORDER);

        ctx.drawText(textRenderer, title, px + 12, py + 10, GOLD, true);
        String linkLabel = linkState();
        int stateColor = linkConfig.hasDeviceToken() && !linkConfig.linkPending ? GREEN : YELLOW;
        int stateWidth = textRenderer.getWidth(linkLabel) + 12;
        int stateX = px + pw - stateWidth - 10;
        ctx.fill(stateX, py + 7, stateX + stateWidth, py + 25, 0x553A4C5F);
        ctx.drawCenteredTextWithShadow(textRenderer, linkLabel,
                stateX + stateWidth / 2, py + 12, stateColor);

        int contentY = py + 43;
        int leftW = Math.min(132, Math.max(104, pw / 3));
        renderIdentity(ctx, px + 9, contentY, leftW, ph - 79);

        int rightX = px + 16 + leftW;
        int rightW = px + pw - 9 - rightX;
        renderProfile(ctx, rightX, contentY, rightW, ph - 79);

        String updated = state.refreshedAt.isBlank() ? ""
                : Text.translatable("ottoextra.account.updated", state.refreshedAt).getString();
        if (this.state.error != null) {
            String failed = Text.translatable("ottoextra.account.refreshFailed").getString();
            String message = updated.isBlank() ? failed : failed + " · " + updated;
            ctx.drawText(textRenderer, trim(message, pw - 20), px + 10, py + ph - 38, YELLOW, false);
        } else if (!updated.isBlank()) {
            ctx.drawText(textRenderer, updated, px + pw - textRenderer.getWidth(updated) - 10,
                    py + ph - 38, MUTED, false);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderIdentity(DrawContext ctx, int x, int y, int w, int h) {
        var account = state.account;
        boolean accountLoading = state.loading && linkConfig.hasDeviceToken() && !linkConfig.linkPending;
        card(ctx, x, y, w, h);
        int avatar = Math.min(52, w - 24);
        int ax = x + (w - avatar) / 2;
        ctx.fill(ax, y + 12, ax + avatar, y + 12 + avatar, 0xFF314255);
        String initial = minecraftName == null || minecraftName.isBlank()
                ? "?" : minecraftName.substring(0, 1).toUpperCase(Locale.ROOT);
        ctx.drawCenteredTextWithShadow(textRenderer, initial,
                ax + avatar / 2, y + 12 + avatar / 2 - 4, GOLD);

        String name = trim(minecraftName, w - 14);
        ctx.drawCenteredTextWithShadow(textRenderer, name, x + w / 2, y + 72, TEXT);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.account.minecraft"),
                x + w / 2, y + 84, MUTED);

        String discord = account != null && !account.discordName().isBlank()
                ? account.discordName()
                : accountLoading ? Text.translatable("ottoextra.account.loading").getString()
                : linkConfig.hasDeviceToken() && !linkConfig.linkPending
                ? Text.translatable("ottoextra.account.connected").getString()
                : Text.translatable("ottoextra.account.not_connected").getString();
        ctx.fill(x + 9, y + 101, x + w - 9, y + 102, 0x33FFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, trim(discord, w - 14),
                x + w / 2, y + 110,
                linkConfig.hasDeviceToken() && !linkConfig.linkPending ? BLUE : MUTED);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.account.discord"),
                x + w / 2, y + 122, MUTED);
    }

    private void renderProfile(DrawContext ctx, int x, int y, int w, int h) {
        PlayerRecord profile = state.profile;
        if (state.loading && profile == null) {
            card(ctx, x, y, w, h);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.account.loading"),
                    x + w / 2, y + h / 2 - 4, MUTED);
            return;
        }
        if (profile == null) {
            card(ctx, x, y, w, h);
            String error = state.error == null
                    ? Text.translatable("ottoextra.account.no_data").getString() : shortError(state.error);
            ctx.drawCenteredTextWithShadow(textRenderer, trim(error, w - 16),
                    x + w / 2, y + h / 2 - 4, YELLOW);
            return;
        }

        int gap = 6;
        int metricW = (w - gap) / 2;
        int metricH = 51;
        metric(ctx, x, y, metricW, metricH,
                Text.translatable("ottoextra.account.heller").getString(),
                profile.money() == null ? "–"
                        : NumberFormat.getIntegerInstance(Locale.GERMANY).format(profile.money()), GOLD);
        metric(ctx, x + metricW + gap, y, w - metricW - gap, metricH,
                Text.translatable("ottoextra.account.state").getString(),
                safe(profile.state()), BLUE);

        int detailsY = y + metricH + gap;
        int detailsH = h - metricH - gap;
        card(ctx, x, detailsY, w, detailsH);
        int labelX = x + 9;
        int valueX = x + Math.min(76, w / 3);
        int rowY = detailsY + 9;
        row(ctx, labelX, valueX, rowY, w, "ottoextra.account.rp_name",
                first(profile.rp_name(), profile.name(), profile.minecraft_name()));
        rowY += 17;
        row(ctx, labelX, valueX, rowY, w, "ottoextra.account.title_label", safe(profile.title()));
        rowY += 17;
        row(ctx, labelX, valueX, rowY, w, "ottoextra.account.rank", safe(profile.rank()));
        rowY += 17;
        row(ctx, labelX, valueX, rowY, w, "ottoextra.account.faction",
                first(profile.faction_name(), profile.faction()));
        rowY += 17;
        row(ctx, labelX, valueX, rowY, w, "ottoextra.account.permissions",
                profile.permissions() == null || profile.permissions().isEmpty()
                        ? "–" : String.join(", ", profile.permissions()));
    }

    private void metric(
            DrawContext ctx, int x, int y, int w, int h, String label, String value, int color) {
        card(ctx, x, y, w, h);
        ctx.drawText(textRenderer, label, x + 9, y + 8, MUTED, false);
        ctx.drawText(textRenderer, trim(value, w - 18), x + 9, y + 26, color, true);
    }

    private void row(
            DrawContext ctx, int labelX, int valueX, int y, int cardW, String key, String value) {
        ctx.drawText(textRenderer, Text.translatable(key), labelX, y, MUTED, false);
        ctx.drawText(textRenderer, trim(value, labelX + cardW - valueX - 10),
                valueX, y, TEXT, false);
    }

    private static void card(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, CARD);
        ctx.fill(x, y, x + w, y + 1, BORDER);
        ctx.fill(x, y, x + 1, y + h, BORDER);
    }

    private String linkState() {
        if (linkConfig.linkPending) {
            return Text.translatable("ottoextra.account.pending").getString();
        }
        if (linkConfig.hasDeviceToken()) {
            return Text.translatable("ottoextra.account.linked").getString();
        }
        return Text.translatable("ottoextra.account.unlinked").getString();
    }

    private String trim(String value, int width) {
        return textRenderer.trimToWidth(safe(value), Math.max(8, width));
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "–";
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "–" : value;
    }

    private static String shortError(Throwable error) {
        if (error == null) {
            return "";
        }
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        message = message.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim();
        return message.length() > 100 ? message.substring(0, 100) : message;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        state.invalidate();
        super.removed();
    }

    private static final class MinecraftClientExecutor {
        private static void run(Runnable action) {
            net.minecraft.client.MinecraftClient.getInstance().execute(action);
        }
    }
}
