package de.ottoextra.tacticalfactions;

import com.mojang.authlib.GameProfile;
import de.ottoextra.OttoExtraGate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Overlay listing the squads and their members. Look mirrors OttoExtra's region activity HUD.
 * The only live data is presence in the tab list; no health, position or any other player state.
 */
public final class SquadActivityHud {
    private static final int MARGIN = 4;
    private static final int MIN_PANEL_WIDTH = 104;
    private static final int MAX_PANEL_WIDTH = 158;
    private static final int HEADER_HEIGHT = 15;
    private static final int SQUAD_ROW = 14;
    private static final int MEMBER_ROW = 11;
    private static final int FOOTER_HEIGHT = 14;
    private static final int PAD_X = 4;
    /** Left edge of a header row up to where its name starts (3px colour stripe + gap). */
    private static final int STRIPE_LEAD = 3 + 3 + 4;
    private static final int HEAD = 8;
    private static final int REFRESH_TICKS = 10;

    private static final int COL_PANEL = 0x961E1E24;
    private static final int COL_OUTLINE = 0xD7141418;
    private static final int COL_BEVEL_LIGHT = 0xD7514F59;
    private static final int COL_BEVEL_DARK = 0xD72E2D35;
    private static final int COL_ROW_BG = 0x503B3A45;
    private static final int COL_DIVIDER_DARK = 0xC8141418;
    private static final int COL_DIVIDER_LIGHT = 0xC83B3A45;
    private static final int COL_TITLE = 0xFFD6C194;
    private static final int COL_NAME = 0xFFF0EBE0;
    private static final int COL_DIM = 0xFF8F8C96;
    private static final int COL_ONLINE = 0xFF86C25A;
    private static final int COL_ONLINE_DARK = 0xFF2F5426;
    private static final float SMALL_SCALE = 0.75f;

    record Prepared(SquadHudModel.Line line, SkinTextures skin, String name, int color) { }

    private static final Set<String> ONLINE = new HashSet<>();
    private static List<Prepared> prepared = List.of();
    private static int totalOnline;
    private static int totalMembers;
    private static int ticks = REFRESH_TICKS;

    private SquadActivityHud() { }

    /** Forces a rebuild on the next tick, e.g. after a setting changed. */
    public static void invalidate() {
        ticks = REFRESH_TICKS;
    }

    /** Client tick: recomputes the tab-list presence and the rows at most every 10 ticks. */
    public static void tick(MinecraftClient client) {
        TacticalConfig config = TacticalFactions.config();
        if (!config.squadHudEnabled || client.player == null) {
            prepared = List.of();
            ticks = REFRESH_TICKS;
            return;
        }
        if (++ticks < REFRESH_TICKS) return;
        ticks = 0;
        ONLINE.clear();
        if (client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                GameProfile profile = entry.getProfile();
                if (profile == null) continue;
                if (profile.name() != null) ONLINE.add(profile.name().toLowerCase(Locale.ROOT));
                if (profile.id() != null) ONLINE.add(profile.id().toString().toLowerCase(Locale.ROOT));
            }
        }
        List<SquadHudModel.Line> lines = SquadHudModel.build(config.squads, ONLINE,
                config.squadHudMembers, config.squadHudOnlyOnline, config.squadHudMaxMembers);
        List<Prepared> result = new ArrayList<>(lines.size());
        for (SquadHudModel.Line line : lines) {
            if (line.header()) {
                result.add(new Prepared(line, null, line.squad().name, BookArt.parseHex(line.squad().color)));
            } else if (line.moreRow()) {
                result.add(new Prepared(line, null, Text.translatable("ottoextra_tacticalfactions.squad_hud.more",
                        line.more()).getString(), 0));
            } else {
                SquadMember member = line.member();
                UUID uuid = null;
                try {
                    if (member.uuid != null) uuid = UUID.fromString(member.uuid);
                } catch (IllegalArgumentException ignored) {
                }
                String name = TacticalFactions.politics().tablistDisplayName(uuid, member.account);
                result.add(new Prepared(line, BookArt.skinFor(member.uuid, member.account),
                        name == null ? member.account : name, 0));
            }
        }
        prepared = result;
        totalOnline = SquadHudModel.totalOnline(lines);
        totalMembers = SquadHudModel.totalMembers(lines);
    }

    public static void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        TacticalConfig config = TacticalFactions.config();
        MinecraftClient client = MinecraftClient.getInstance();
        List<Prepared> lines = prepared;
        if (!config.squadHudEnabled || lines.isEmpty() || client == null || client.player == null
                || client.options.hudHidden || client.debugHudEntryList.isF3Enabled()
                || !OttoExtraGate.isOnOttonien(client)) {
            return;
        }
        TextRenderer font = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        float scale = HudPlacement.clampScale(config.squadHudScale);
        Panel panel = layout(font, lines, totalOnline, totalMembers, Math.round(screenH / scale));
        if (panel.count() == 0) return;
        int boxW = HudPlacement.scaled(panel.width(), scale);
        int boxH = HudPlacement.scaled(panel.height(), scale);
        HudCorner corner = config.squadHudCorner;
        // Without HudFlow, our own offset wins and only the untouched default avoids OttoExtra's activity HUD.
        if (!flowActive() && config.squadHudOffsetX == 0 && config.squadHudOffsetY == 0) {
            corner = effectiveCorner(corner);
        }
        int[] pos = HudPlacement.position(corner, config.squadHudOffsetX, config.squadHudOffsetY,
                screenW, screenH, boxW, boxH, MARGIN);
        int[] flowed = flow(pos[0], pos[1], boxW, boxH, corner.bottom());
        if (flowed != null) pos = flowed;
        draw(ctx, font, lines, panel, pos[0], pos[1], scale, totalOnline, totalMembers);
    }

    /** Computed size and number of rows that fit on screen. */
    record Panel(int width, int height, int count) { }

    static int margin() { return MARGIN; }

    static Panel layout(TextRenderer font, List<Prepared> lines, int online, int members, int screenH) {
        int maxHeight = screenH - 2 * MARGIN - HEADER_HEIGHT - FOOTER_HEIGHT;
        int used = 0;
        int count = 0;
        for (Prepared p : lines) {
            int h = p.line().header() ? SQUAD_ROW : MEMBER_ROW;
            if (used + h > maxHeight) break;
            used += h;
            count++;
        }
        String title = Text.translatable("ottoextra_tacticalfactions.squad_hud.title").getString();
        String footer = Text.translatable("ottoextra_tacticalfactions.squad_hud.footer", online, members).getString();
        int width = Math.max(MIN_PANEL_WIDTH, font.getWidth(title) + 2 * PAD_X);
        width = Math.max(width, Math.round(font.getWidth(footer) * SMALL_SCALE) + 2 * PAD_X);
        for (int i = 0; i < count; i++) {
            Prepared p = lines.get(i);
            int lead = p.line().header() ? STRIPE_LEAD + font.getWidth(countText(p)) + 4
                    : PAD_X + 6 + HEAD + 4;
            width = Math.max(width, lead + Math.min(100, font.getWidth(p.name())) + PAD_X);
        }
        return new Panel(Math.min(MAX_PANEL_WIDTH, width), HEADER_HEIGHT + used + FOOTER_HEIGHT, count);
    }

    /** Draws the panel with its top-left corner at (x, y). Shared with the position editor. */
    static void draw(DrawContext ctx, TextRenderer font, List<Prepared> lines, Panel panel, int x, int y,
                     float scale, int online, int members) {
        String title = Text.translatable("ottoextra_tacticalfactions.squad_hud.title").getString();
        String footer = Text.translatable("ottoextra_tacticalfactions.squad_hud.footer", online, members).getString();
        int width = panel.width();
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        try {
            drawPanel(ctx, 0, 0, width, panel.height());
            ctx.drawTextWithShadow(font, title, PAD_X, 3, COL_TITLE);
            drawDivider(ctx, 2, HEADER_HEIGHT - 3, width - 4);
            int rowY = HEADER_HEIGHT;
            for (int i = 0; i < panel.count(); i++) {
                Prepared p = lines.get(i);
                if (p.line().header()) {
                    drawSquadRow(ctx, font, p, rowY, width);
                    rowY += SQUAD_ROW;
                } else {
                    drawMemberRow(ctx, font, p, rowY, width);
                    rowY += MEMBER_ROW;
                }
            }
            drawDivider(ctx, 2, rowY, width - 4);
            m.pushMatrix();
            m.translate(PAD_X, rowY + 4);
            m.scale(SMALL_SCALE, SMALL_SCALE);
            ctx.drawText(font, footer, 0, 0, COL_DIM, false);
            m.popMatrix();
        } finally {
            m.popMatrix();
        }
    }

    /** What the HUD currently shows, or sample rows when there is nothing real to show. */
    static List<Prepared> currentOrSample() {
        return prepared.isEmpty() ? sample() : prepared;
    }

    static int currentOnline() { return prepared.isEmpty() ? 3 : totalOnline; }

    static int currentMembers() { return prepared.isEmpty() ? 5 : totalMembers; }

    private static List<Prepared> sample() {
        Squad a = Squad.create("Vorhut", "#55C46A");
        a.members.add(new SquadMember("Alice_1", null, true));
        a.members.add(new SquadMember("Bob_2", null, false));
        Squad b = Squad.create("Hafenwache", "#4FA3C7");
        b.members.add(new SquadMember("Cara_3", null, false));
        b.members.add(new SquadMember("Dan_4", null, false));
        b.members.add(new SquadMember("Eve_5", null, false));
        Set<String> on = Set.of("alice_1", "bob_2", "cara_3");
        List<Prepared> result = new ArrayList<>();
        for (SquadHudModel.Line line : SquadHudModel.build(List.of(a, b), on, true, false)) {
            result.add(line.header()
                    ? new Prepared(line, null, line.squad().name, BookArt.parseHex(line.squad().color))
                    : new Prepared(line, BookArt.skinFor(null, line.member().account),
                            line.member().account, 0));
        }
        return result;
    }

    /**
     * OttoExtra's activity HUD sits in the middle of the left edge by default. If the user moved it
     * into the same corner as ours, we use the opposite vertical edge instead of drawing on top of it.
     */
    /** True if OttoExtra's HudFlow is available and enabled; false with an older or disabled OttoExtra. */
    static boolean flowActive() {
        try {
            return de.ottoextra.ui.hudlayout.HudFlow.isEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Asks HudFlow where to draw so we do not overlap other HUDs; null means draw at the given spot. */
    static int[] flow(int x, int y, int w, int h, boolean pushUp) {
        try {
            return de.ottoextra.ui.hudlayout.HudFlow.place("tacticalfactions:squad_hud", x, y, w, h, pushUp,
                    de.ottoextra.ui.hudlayout.HudFlow.PRIORITY_ADDON);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static HudCorner effectiveCorner(HudCorner wanted) {
        try {
            var regions = de.ottoextra.config.OttoExtraConfig.active().regions;
            if (regions != null && regions.enabled && regions.activityHudEnabled
                    && wanted.name().equalsIgnoreCase(regions.activityHudPosition)) {
                return wanted.flipVertical();
            }
        } catch (Throwable ignored) {
            // OttoExtra config unavailable: keep the chosen corner.
        }
        return wanted;
    }

    private static String countText(Prepared p) {
        return p.line().onlineCount() + "/" + p.line().total();
    }

    private static void drawSquadRow(DrawContext ctx, TextRenderer font, Prepared p, int y, int width) {
        drawRoundedFlat(ctx, 3, y, width - 6, SQUAD_ROW - 1, COL_ROW_BG);
        // Squad colour as a stripe along the left edge of the header row (lit top, shaded bottom).
        int c = p.color();
        ctx.fill(3, y + 1, 6, y + SQUAD_ROW - 2, c);
        ctx.fill(4, y, 6, y + 1, c);
        ctx.fill(5, y + 1, 6, y + SQUAD_ROW - 2, OttonienShade.dark(c));
        ctx.fill(4, y + SQUAD_ROW - 2, 6, y + SQUAD_ROW - 1, OttonienShade.dark(c));
        int base = (SQUAD_ROW - font.fontHeight) / 2 + y;
        String count = countText(p);
        int countW = font.getWidth(count);
        int nameX = STRIPE_LEAD;
        ctx.drawTextWithShadow(font, font.trimToWidth(p.name(), Math.max(24, width - nameX - countW - 2 * PAD_X)),
                nameX, base + 1, COL_NAME);
        ctx.drawTextWithShadow(font, count, width - PAD_X - countW, base + 1,
                p.line().onlineCount() > 0 ? COL_ONLINE : COL_DIM);
    }

    private static void drawMemberRow(DrawContext ctx, TextRenderer font, Prepared p, int y, int width) {
        if (p.line().moreRow()) {
            int moreX = PAD_X + 6 + HEAD + 4;
            ctx.drawTextWithShadow(font, font.trimToWidth(p.name(), Math.max(24, width - moreX - PAD_X)),
                    moreX, y + (MEMBER_ROW - font.fontHeight) / 2 + 1, COL_DIM);
            return;
        }
        boolean online = p.line().online();
        int headX = PAD_X + 6;
        int headY = y + 1;
        BookArt.head(ctx, p.skin(), headX, headY, HEAD);
        if (!online) ctx.fill(headX, headY, headX + HEAD, headY + HEAD, 0x99000000);
        if (p.line().member().leader) BookArt.crown(ctx, headX, headY);
        if (online) {
            ctx.fill(headX + HEAD - 2, headY + HEAD - 2, headX + HEAD + 1, headY + HEAD + 1, COL_ONLINE_DARK);
            ctx.fill(headX + HEAD - 1, headY + HEAD - 1, headX + HEAD, headY + HEAD, COL_ONLINE);
        }
        int nameX = headX + HEAD + 4;
        ctx.drawTextWithShadow(font, font.trimToWidth(p.name(), Math.max(24, width - nameX - PAD_X)),
                nameX, y + (MEMBER_ROW - font.fontHeight) / 2 + 1, online ? COL_NAME : COL_DIM);
    }

    private static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        int r = x + w;
        int b = y + h;
        ctx.fill(x + 1, y, r - 1, y + 1, COL_OUTLINE);
        ctx.fill(x + 1, b - 1, r - 1, b, COL_OUTLINE);
        ctx.fill(x, y + 1, x + 1, b - 1, COL_OUTLINE);
        ctx.fill(r - 1, y + 1, r, b - 1, COL_OUTLINE);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, COL_OUTLINE);
        ctx.fill(r - 2, y + 1, r - 1, y + 2, COL_OUTLINE);
        ctx.fill(x + 1, b - 2, x + 2, b - 1, COL_OUTLINE);
        ctx.fill(r - 2, b - 2, r - 1, b - 1, COL_OUTLINE);
        ctx.fill(x + 2, y + 1, r - 2, y + 2, COL_BEVEL_LIGHT);
        ctx.fill(x + 1, y + 2, x + 2, b - 2, COL_BEVEL_LIGHT);
        ctx.fill(x + 2, b - 2, r - 2, b - 1, COL_BEVEL_DARK);
        ctx.fill(r - 2, y + 2, r - 1, b - 2, COL_BEVEL_DARK);
        ctx.fill(x + 2, y + 2, r - 2, b - 2, COL_PANEL);
    }

    private static void drawRoundedFlat(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void drawDivider(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, COL_DIVIDER_DARK);
        ctx.fill(x, y + 1, x + w, y + 2, COL_DIVIDER_LIGHT);
    }

    private static final class OttonienShade {
        static int dark(int argb) {
            return de.ottoextra.tacticalfactions.theme.OttonienTheme.shade(argb, .6);
        }
    }
}
