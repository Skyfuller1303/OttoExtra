package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.theme.OttonienTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Drawing helpers shared by the book screens: blue-grey binding, arms, tags, pennants, heads. */
final class BookArt {
    /** Dark to light leather ramp of the blue-grey Politik-Buch binding. */
    static final int[] BOOK = {0xFF141A21, 0xFF222B36, 0xFF313C49, 0xFF3B4756, 0xFF4F5C6B};
    private static final int[] GOLD = {0xFF47391D, 0xFF866C36, 0xFFB58D36, 0xFFDBB451};

    static final int INHERIT_COLOR = 0xFF7C8C9C;
    static final int LOCAL_TAG = 0xFF866C36;
    static final int ONLINE_DARK = 0xFF2F5426;
    static final int ONLINE_LIGHT = 0xFF86C25A;
    static final int[] CROWN = {0xFFB58D36, 0xFFDBB451};

    private static final Map<String, SkinTextures> DEFAULT_SKINS = new HashMap<>();

    private BookArt() { }

    // ---- binding ----

    /** Same construction as {@link OttonienTheme#redBook} with the blue-grey leather ramp. */
    static void blueBook(DrawContext ctx, int bx, int by, int bw, int bh) {
        OttonienTheme.rect(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, BOOK[0]);
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + bh, BOOK[2]);
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + 1, BOOK[3]);
        OttonienTheme.rect(ctx, bx, by, bx + 1, by + bh, BOOK[3]);
        OttonienTheme.rect(ctx, bx, by + bh - 1, bx + bw, by + bh, BOOK[1]);
        OttonienTheme.rect(ctx, bx, by, bx + 7, by + bh, BOOK[1]);
        for (int i = by + 10; i < by + bh - 8; i += 22) {
            OttonienTheme.rect(ctx, bx + 1, i, bx + 6, i + 2, BOOK[3]);
            OttonienTheme.rect(ctx, bx + 1, i + 2, bx + 6, i + 3, BOOK[0]);
        }
        OttonienTheme.rect(ctx, bx + 9, by + 2, bx + bw - 2, by + 3, BOOK[1]);
        OttonienTheme.rect(ctx, bx + 9, by + bh - 3, bx + bw - 2, by + bh - 2, BOOK[3]);
        int[][] studs = {{bx + bw - 6, by + 3}, {bx + bw - 6, by + bh - 6}};
        for (int[] s : studs) {
            OttonienTheme.rect(ctx, s[0], s[1], s[0] + 3, s[1] + 3, GOLD[1]);
            ctx.fill(s[0], s[1], s[0] + 1, s[1] + 1, GOLD[3]);
            ctx.fill(s[0] + 2, s[1] + 2, s[0] + 3, s[1] + 3, GOLD[0]);
        }
    }

    // ---- colours ----

    static int relationColor(Relation relation) {
        if (relation == null) return OttonienTheme.NEUTRAL_TAN;
        return switch (relation) {
            case ALLIED -> OttonienTheme.FRIEND_BLUE;
            case HOSTILE -> OttonienTheme.FOE_RED;
            case INHERIT -> INHERIT_COLOR;
            case NEUTRAL -> OttonienTheme.NEUTRAL_TAN;
        };
    }

    static String shortRelationKey(Relation relation) {
        return "ottoextra_tacticalfactions.relation."
                + (relation == null ? "neutral" : relation.name().toLowerCase(Locale.ROOT)) + ".short";
    }

    /** Parses #RRGGBB into an opaque ARGB colour; falls back to grey. */
    static int parseHex(String hex) {
        try {
            if (hex != null && hex.matches("#[0-9a-fA-F]{6}")) {
                return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0xFF888888;
    }

    // ---- arms ----

    /** Small heater shield: outline, two-colour field, one simple ordinary (0..3). */
    static void shield(DrawContext ctx, int x, int y, int s, int c1, int c2, int kind) {
        int outline = OttonienTheme.shade(c1, .35);
        for (int r = 0; r < s; r++) {
            int inset = r < s * .55 ? 0 : (int) ((r - s * .55) * 1.1);
            if (inset * 2 >= s) break;
            ctx.fill(x + inset, y + r, x + s - inset, y + r + 1, outline);
            if (r > 0 && r < s - 1 && inset * 2 + 2 < s) {
                int x0 = x + inset + 1;
                int x1 = x + s - inset - 1;
                switch (kind) {
                    case 0 -> {
                        int mid = Math.max(x0, Math.min(x1, x + s / 2));
                        ctx.fill(x0, y + r, mid, y + r + 1, c1);
                        ctx.fill(mid, y + r, x1, y + r + 1, c2);
                    }
                    case 1 -> ctx.fill(x0, y + r, x1, y + r + 1,
                            r > s * .38 && r < s * .62 ? c2 : c1);
                    case 2 -> {
                        int a = Math.max(x0, Math.min(x1, x + (int) (s * .36)));
                        int b = Math.max(x0, Math.min(x1, x + (int) (s * .64) + 1));
                        ctx.fill(x0, y + r, a, y + r + 1, c1);
                        ctx.fill(a, y + r, b, y + r + 1, c2);
                        ctx.fill(b, y + r, x1, y + r + 1, c1);
                    }
                    default -> {
                        int mid = Math.max(x0, Math.min(x1, x + s / 2));
                        boolean top = r < s * .5;
                        ctx.fill(x0, y + r, mid, y + r + 1, top ? c2 : c1);
                        ctx.fill(mid, y + r, x1, y + r + 1, top ? c1 : c2);
                    }
                }
            }
        }
        ctx.fill(x + 1, y + 1, x + 2, y + 2, OttonienTheme.shade(c1, 1.25));
    }

    /** Fallback arms from a base colour and a stable key (same entry always looks the same). */
    static void fallbackShield(DrawContext ctx, int x, int y, int s, int base, String key) {
        int hash = key == null ? 0 : key.hashCode();
        int kind = Math.floorMod(hash, 4);
        int c1 = base == 0xFF777777 ? OttonienTheme.PICTO : base;
        int c2 = switch (Math.floorMod(hash >>> 3, 3)) {
            case 0 -> 0xFFC9A04A;
            case 1 -> 0xFFD9D2C2;
            default -> 0xFF2E2E33;
        };
        shield(ctx, x, y, s, c1, c2, kind);
    }

    /** Real banner texture when present, otherwise the drawn shield. */
    static void arms(DrawContext ctx, Identifier banner, int x, int y, int s, int base, String key) {
        if (banner != null) {
            ctx.fill(x, y, x + s, y + s, OttonienTheme.shade(base, .35));
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, x + 1, y + 1, 0f, 0f,
                    s - 2, s - 2, s - 2, s - 2);
            return;
        }
        fallbackShield(ctx, x, y, s, base, key);
    }

    // ---- small widgets ----

    /** Flat label tag (1 px darker border, filled, centered text) right-aligned at {@code xRight}; returns its width. */
    static int tag(DrawContext ctx, TextRenderer font, int xRight, int y, Text label, int color) {
        int w = font.getWidth(label) + 8;
        ctx.fill(xRight - w, y, xRight, y + 11, OttonienTheme.shade(color, .6));
        ctx.fill(xRight - w + 1, y + 1, xRight - 1, y + 10, color);
        ctx.drawText(font, label, xRight - w + 4, y + 2, OttonienTheme.LABEL, false);
        return w;
    }

    /** Collapse arrow: open points down, closed points right. */
    static void arrow(DrawContext ctx, int x, int y, boolean open) {
        for (int i = 0; i < 3; i++) {
            if (open) {
                ctx.fill(x + i, y + i, x + 7 - i, y + 1 + i, OttonienTheme.PICTO);
            } else {
                ctx.fill(x + i, y + i, x + 1 + i, y + 7 - i, OttonienTheme.PICTO);
            }
        }
    }

    static void triangleUp(DrawContext ctx, int x, int y, int color) {
        for (int i = 0; i < 3; i++) ctx.fill(x + 2 - i, y + i, x + 3 + i, y + i + 1, color);
    }

    static void triangleDown(DrawContext ctx, int x, int y, int color) {
        for (int i = 0; i < 3; i++) ctx.fill(x + i, y + i, x + 5 - i, y + i + 1, color);
    }

    static void cross(DrawContext ctx, int x, int y, int color) {
        for (int i = 0; i < 5; i++) {
            ctx.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            ctx.fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, color);
        }
    }

    static void line(DrawContext ctx, int x0, int x1, int y) {
        ctx.fill(x0, y, x1, y + 1, OttonienTheme.PA_S);
    }

    /** Relation / option chip: active in colour, inactive on parchment with a coloured stripe. */
    static void chip(DrawContext ctx, TextRenderer font, int x, int y, int w, Text label,
                     int color, boolean on, boolean stripe) {
        OttonienTheme.chip(ctx, x, y, w, 11, color, on);
        if (!on && stripe) ctx.fill(x + 1, y + 1, x + 3, y + 10, color);
        int tx = x + (w - font.getWidth(label)) / 2 + (!on && stripe ? 1 : 0);
        ctx.drawText(font, label, tx, y + 2, on ? OttonienTheme.LABEL : OttonienTheme.INK_MUTED, false);
    }

    /** Small gold crown marking a squad leader, above a head at (x, y). */
    static void crown(DrawContext ctx, int x, int y) {
        ctx.fill(x, y - 3, x + 1, y, CROWN[1]);
        ctx.fill(x + 2, y - 2, x + 3, y, CROWN[1]);
        ctx.fill(x + 4, y - 3, x + 5, y, CROWN[1]);
        ctx.fill(x, y - 1, x + 5, y + 1, CROWN[0]);
    }

    static void onlineDot(DrawContext ctx, int x, int y, boolean online) {
        ctx.fill(x, y, x + 4, y + 4, online ? ONLINE_DARK : OttonienTheme.PA_S);
        if (online) ctx.fill(x + 1, y + 1, x + 3, y + 3, ONLINE_LIGHT);
    }

    static String ellipsize(TextRenderer font, String value, int maxWidth) {
        if (value == null) return "";
        if (font.getWidth(value) <= maxWidth) return value;
        return font.trimToWidth(value, Math.max(0, maxWidth - font.getWidth("…"))) + "…";
    }

    // ---- heads ----

    /**
     * Skin of a player: the real skin if the player is in the tab list, otherwise the default skin
     * of the account name. Never triggers a lookup beyond the already-loaded tab list.
     */
    static SkinTextures skinFor(String uuid, String account) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client != null ? client.getNetworkHandler() : null;
        if (handler != null) {
            PlayerListEntry entry = null;
            if (uuid != null && !uuid.isBlank()) {
                try {
                    entry = handler.getPlayerListEntry(UUID.fromString(uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (entry == null && account != null && !account.isBlank()) {
                entry = handler.getPlayerListEntry(account);
            }
            if (entry != null) return entry.getSkinTextures();
        }
        String key = account == null ? "" : account.toLowerCase(Locale.ROOT);
        return DEFAULT_SKINS.computeIfAbsent(key,
                name -> DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(name)));
    }

    static void head(DrawContext ctx, SkinTextures skin, int x, int y, int size) {
        PlayerSkinDrawer.draw(ctx, skin, x, y, size);
    }
}
