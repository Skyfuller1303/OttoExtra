package de.ottoextra.map;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.map.xaero.MinimapBannerOverlay;
import de.ottoextra.regions.RegionMessageService;
import de.ottoextra.regions.RegionState;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.OrderedText;

import java.lang.reflect.Field;
import java.util.List;

public final class MinimapHeightUiChecks {
    private static final String MESSAGE = "Gefolge in Gefolge XYZ";
    private MinimapHeightUiChecks() { }

    public static void run(ClientGameTestContext ctx) {
        LehenPolygonStore.ensureLoaded();
        ctx.waitFor(c -> LehenPolygonStore.isLoaded());
        var polygons = LehenPolygonStore.polygons();
        var region = RegionMessageService.current();
        String snapshot = ctx.computeOnClient(c -> OttoExtraConfig.active().snapshotJson());
        int scale = ctx.computeOnClient(c -> c.options.getGuiScale().getValue());
        Field polygonField = field(LehenPolygonStore.class, "polygons");
        Field regionField = field(RegionMessageService.class, "current");
        try {
            ctx.runOnClient(c -> c.options.getGuiScale().setValue(3));
            ctx.getInput().resizeWindow(1920, 1080); ctx.waitTick();
            ctx.runOnClient(c -> {
                try {
                    double x = c.player.getX(), z = c.player.getZ();
                    polygonField.set(null, List.of(new LehenPolygon("ottoextra-ui-minimap-fixture",
                            new double[]{x - 16, x + 16, x + 16, x - 16},
                            new double[]{z - 16, z - 16, z + 16, z + 16}, x, z,
                            x - 16, z - 16, x + 16, z + 16, true)));
                    regionField.set(null, new RegionState("Kallberg Kloster", MESSAGE, "", System.currentTimeMillis()));
                } catch (IllegalAccessException e) { throw new RuntimeException(e); }
                var config = OttoExtraConfig.active();
                config.map.minimapBanner = false;
                config.map.minimapBannerShowName = config.map.minimapBannerShowState = true;
                config.map.minimapBannerShowFaction = false;
                config.map.minimapLiegeTop = false;
                config.map.nameHudWidth = 150;
                config.map.nameHudHeight = 0;
                config.map.nameHudScale = 1F;
                config.map.nameHudX = 50; config.map.nameHudY = 50;
                config.map.nameHudFromRight = config.map.nameHudFromBottom = false;
                config.map.stateHudWidth = 91;
                config.map.stateHudHeight = 0;
                config.map.stateHudScale = 1F;
                config.map.stateHudX = c.getWindow().getScaledWidth() - 100 - 91;
                config.map.stateHudY = c.getWindow().getScaledHeight() - 100 - 10;
                config.map.stateHudFromRight = config.map.stateHudFromBottom = true;
                c.getToastManager().clear();
                c.setScreen(new MinimapBannerEditScreen(null, config));
                var before = state(c);
                require(before.height() == 10 && MinimapBannerOverlay.textLines(c.textRenderer, before).size() == 1,
                        "Existing layouts must remain single-line");
            });
            ctx.waitTick(); ctx.takeScreenshot("minimap-height-before");
            ctx.runOnClient(c -> {
                var before = state(c);
                resize(c, before, 0, 10);
                var after = state(c);
                require(after.height() == 20 && after.width() == before.width(), "Vertical drag must change height only");
                require(after.scale() == before.scale(), "Height resize changed the font scale");
                require(after.x() == before.x() && after.y() == before.y(), "Anchored resize moved the opposite corner");
                var lines = MinimapBannerOverlay.textLines(c.textRenderer, after);
                require(lines.size() == 2 && String.join(" ", lines.stream().map(MinimapHeightUiChecks::plain).toList()).equals(MESSAGE),
                        "Two-line layout lost part of the hierarchy text");
                for (var line : lines) require(c.textRenderer.getWidth(line) <= after.width(), "Wrapped line exceeds width");
            });
            ctx.waitTick(); ctx.takeScreenshot("minimap-height-two-lines");
            ctx.runOnClient(c -> {
                var before = state(c);
                var screen = (MinimapBannerEditScreen) c.currentScreen;
                screen.mouseScrolled(before.x() + 5, before.y() + 5, 0, 1);
                require(OttoExtraConfig.active().map.stateHudScale > before.scale(), "Scroll must adjust font size");
                require(OttoExtraConfig.active().map.stateHudHeight == 20, "Font scaling overwrote text box height");
                screen.mouseScrolled(before.x() + 5, before.y() + 5, 0, -1);
                var start = state(c);
                screen.mouseClicked(click(start.x() + 3, start.y() + 3), false);
                screen.mouseDragged(click(start.x() + 23, start.y() + 18), 20, 15);
                screen.mouseReleased(click(start.x() + 23, start.y() + 18));
                var moved = state(c);
                require(moved.x() == start.x() + 20 && moved.y() == start.y() + 15, "Two-line block did not move as a whole");
                require(moved.height() == 20, "Moving changed the height");
                c.setScreen(null);
                c.setScreen(new MinimapBannerEditScreen(null, OttoExtraConfig.active()));
                require(state(c).height() == 20, "Reopening discarded the height");
            });
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
            ctx.runOnClient(c -> {
                var e = state(c);
                require(e.x() >= 0 && e.y() >= 0 && e.x() + e.width() <= c.getWindow().getScaledWidth()
                        && e.y() + e.height() <= c.getWindow().getScaledHeight(), "Resized window clips the text box");
                require(MinimapBannerOverlay.textLines(c.textRenderer, e).size() == 2, "Window resize lost wrapping");
            });
            ctx.takeScreenshot("minimap-height-compact");
            ctx.runOnClient(c -> {
                resize(c, state(c), -20, 0);
                require(state(c).width() == 71 && state(c).height() == 20, "Horizontal resize changed height");
                resize(c, state(c), 0, -1000);
                require(state(c).height() == 10 && Math.abs(state(c).scale() - 1F) < 0.0001F,
                        "Minimum height must retain one line");
                var cfg = OttoExtraConfig.active().map;
                cfg.stateHudX = cfg.stateHudY = 0;
                cfg.stateHudFromRight = cfg.stateHudFromBottom = true;
                int widthBefore = state(c).width();
                resize(c, state(c), -20, 0);
                require(state(c).width() == widthBefore - 20, "Handle at screen edge is not reachable");
            });
        } finally {
            ctx.runOnClient(c -> {
                c.setScreen(null);
                try { polygonField.set(null, polygons); regionField.set(null, region); }
                catch (IllegalAccessException e) { throw new RuntimeException(e); }
                OttoExtraConfig.active().restoreFrom(snapshot);
                OttoExtraConfig.active().save();
                c.options.getGuiScale().setValue(scale);
            });
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
        }
    }

    private static void resize(MinecraftClient client, MinimapBannerOverlay.Element e, int dx, int dy) {
        var screen = (MinimapBannerEditScreen) client.currentScreen;
        int x = Math.min(screen.width - 9, e.x() + e.width() + 1) + 3;
        int y = Math.min(screen.height - 9, e.y() + e.height() + 1) + 3;
        require(screen.mouseClicked(click(x, y), false), "Resize handle did not accept click");
        require(screen.mouseDragged(click(x + dx, y + dy), dx, dy), "Resize handle did not accept drag");
        screen.mouseReleased(click(x + dx, y + dy));
    }

    private static MinimapBannerOverlay.Element state(MinecraftClient c) {
        return MinimapBannerOverlay.computeElements(c, OttoExtraConfig.active().map, null).stream()
                .filter(e -> e.kind() == MinimapBannerOverlay.Kind.STATE).findFirst().orElseThrow();
    }

    private static String plain(OrderedText text) {
        var result = new StringBuilder();
        text.accept((i, style, cp) -> { result.appendCodePoint(cp); return true; });
        return result.toString();
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static Click click(int x, int y) { return new Click(x, y, new MouseInput(0, 0)); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
