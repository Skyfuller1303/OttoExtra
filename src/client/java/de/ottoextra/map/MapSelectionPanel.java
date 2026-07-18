package de.ottoextra.map;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Locale;

public final class MapSelectionPanel {

    private static final int WIDTH = 202;
    private static final int HEIGHT = 132;
    private static final int BG = 0xE8C8AC8E;
    private static final int BORDER = 0xFF513E2A;
    private static final int LIGHT = 0xFFE6C8A9;
    private static final int DARK = 0xFFB8926E;
    private static final int TEXT = 0xFF503D29;
    private static final int MUTED = 0xFF765B41;

    private static int lastX;
    private static int lastY;
    private static int lastW;
    private static int lastH;

    private MapSelectionPanel() {
    }

    public static void render(DrawContext ctx, XaeroMapBridge.View view, OttoExtraConfig.Map cfg) {
        LehenPolygon poly = PoliticalOverlay.selectedPolygon();
        if (poly == null || !cfg.clickInfoPanel) {
            lastW = lastH = 0;
            return;
        }
        int x = Math.max(8, cfg.parchmentMarginPx + 8);
        int y = Math.max(38, cfg.parchmentMarginPx + 34);
        int w = Math.min(WIDTH, Math.max(160, view.width() - x - 8));
        int h = HEIGHT;
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;

        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDER);
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 1, LIGHT);
        ctx.fill(x, y, x + 1, y + h, LIGHT);
        ctx.fill(x, y + h - 1, x + w, y + h, DARK);
        ctx.fill(x + w - 1, y, x + w, y + h, DARK);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Details d = details(poly, cfg);
        int tx = x + 8;
        int ty = y + 7;
        String title = trim(tr, d.regionName, w - 34);
        ctx.drawText(tr, Text.literal(title), tx, ty, TEXT, false);

        ctx.drawText(tr, "x", x + w - 13, ty, MUTED, false);
        ty += 14;

        ty = line(ctx, tr, lang("ottoextra.map.info.faction"), d.faction, tx, ty, w - 16);
        ty = line(ctx, tr, lang("ottoextra.map.info.liege"), d.leader, tx, ty, w - 16);
        ty = line(ctx, tr, lang("ottoextra.map.info.rank"), d.rank, tx, ty, w - 16);
        ty = line(ctx, tr, lang("ottoextra.map.info.group"), d.group, tx, ty, w - 16);
        ty += 2;
        ty = line(ctx, tr, lang("ottoextra.map.info.distance"), d.distance, tx, ty, w - 16);
        ty = line(ctx, tr, lang("ottoextra.map.info.walk"), d.walkTime, tx, ty, w - 16);
        line(ctx, tr, lang("ottoextra.map.info.horse"), d.horseTime, tx, ty, w - 16);

        String coords = Math.round(poly.centroidX()) + " / " + Math.round(poly.centroidZ());
        ctx.drawText(tr, coords, x + w - tr.getWidth(coords) - 7, y + h - 12, MUTED, false);
    }

    public static boolean handleClick(Screen screen, XaeroMapBridge.View view, double mouseX, double mouseY) {
        if (lastW <= 0 || mouseX < lastX || mouseX > lastX + lastW
                || mouseY < lastY || mouseY > lastY + lastH) {
            return false;
        }
        if (mouseX >= lastX + lastW - 24 && mouseY <= lastY + 24) {
            PoliticalOverlay.clearSelection();
            return true;
        }
        LehenPolygon poly = PoliticalOverlay.selectedPolygon();
        if (poly != null) {
            XaeroMapBridge.setCamera(screen, poly.centroidX(), poly.centroidZ());
        }
        return true;
    }

    private static int line(DrawContext ctx, TextRenderer tr, String label, String value,
                            int x, int y, int maxW) {
        if (value == null || value.isBlank()) {
            value = "-";
        }
        String prefix = label + ": ";
        ctx.drawText(tr, prefix, x, y, MUTED, false);
        int px = x + tr.getWidth(prefix);
        ctx.drawText(tr, trim(tr, value, Math.max(20, maxW - tr.getWidth(prefix))), px, y, TEXT, false);
        return y + 11;
    }

    private static Details details(LehenPolygon poly, OttoExtraConfig.Map cfg) {
        RegionDataService data = RegionsServices.data();
        RegionRecord region = data != null ? data.regionByName(poly.key()).orElse(null) : null;
        FactionRecord faction = data != null ? data.factionForRegion(poly.key()).orElse(null) : null;
        String regionName = region != null && region.name() != null && !region.name().isBlank()
                ? region.name() : poly.key().replace("lehen_", "Lehen ");
        String factionName = faction != null ? safe(faction.name()) : "";
        String leader = faction != null ? firstNonBlank(faction.leader_name(), faction.lord_name()) : "";
        String rank = faction != null ? safe(faction.rank_name()) : "";
        String group = safe(PoliticalOverlay.groupDisplayName(poly.key()));

        MinecraftClient client = MinecraftClient.getInstance();
        String distance = lang("ottoextra.map.info.unknown");
        String walk = "-";
        String horse = "-";
        if (client.player != null) {
            double dx = poly.centroidX() - client.player.getX();
            double dz = poly.centroidZ() - client.player.getZ();
            double blocks = Math.sqrt(dx * dx + dz * dz);
            distance = lang("ottoextra.map.info.airline", formatDistance(blocks), direction(dx, dz));
            walk = formatDuration(blocks / Math.max(0.1, cfg.walkBlocksPerSecond));
            horse = formatDuration(blocks / Math.max(0.1, cfg.horseBlocksPerSecond));
        }
        return new Details(regionName, factionName, leader, rank, group, distance, walk, horse);
    }

    private static String direction(double dx, double dz) {
        double angle = Math.atan2(dz, dx);
        int idx = (int) Math.round(angle / (Math.PI / 4.0));
        return switch (Math.floorMod(idx, 8)) {
            case 0 -> "O";
            case 1 -> "SO";
            case 2 -> "S";
            case 3 -> "SW";
            case 4 -> "W";
            case 5 -> "NW";
            case 6 -> "N";
            default -> "NO";
        };
    }

    private static String formatDistance(double blocks) {
        if (blocks >= 1000) {
            return String.format(Locale.GERMAN, "%.1f km", blocks / 1000.0);
        }
        return lang("ottoextra.map.travel.blocks", Math.round(blocks));
    }

    private static String formatDuration(double seconds) {
        long s = Math.max(0, Math.round(seconds));
        if (s < 60) {
            return "ca. " + s + " s";
        }
        long minutes = s / 60;
        if (minutes < 60) {
            return "ca. " + minutes + " min";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? "ca. " + hours + " h" : "ca. " + hours + " h " + rest + " min";
    }

    private static String trim(TextRenderer tr, String value, int maxW) {
        if (value == null) {
            return "";
        }
        return tr.trimToWidth(value, maxW);
    }

    private static String lang(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : safe(b);
    }

    private record Details(String regionName, String faction, String leader, String rank,
                           String group, String distance, String walkTime, String horseTime) {
    }
}
