package de.ottoextra.map;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.map.xaero.MinimapBannerOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.lib.client.gui.IScreenBase;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.util.List;

public final class MinimapBannerEditScreen extends Screen implements IScreenBase {

    private final Screen parent;
    private final OttoExtraConfig config;

    private MinimapBannerOverlay.Kind dragging;
    private MinimapBannerOverlay.Kind resizing;
    private double dragOffX;
    private double dragOffY;
    private int curX;
    private int curY;
    private double resizeStartX;
    private double resizeStartY;
    private float resizeStartScale;
    private int resizeStartSize;
    private int resizeStartWidth;

    public MinimapBannerEditScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.map.bannerEdit.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 80, height - 28, 76, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.map.bannerEdit.reset"), b -> {
            MinimapBannerOverlay.resetPositions(config.map);
            config.save();
        }).dimensions(width / 2 + 4, height - 28, 76, 18).build());
    }

    private List<MinimapBannerOverlay.Element> elements() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return List.of();
        }
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        List<MinimapBannerOverlay.Element> list =
                MinimapBannerOverlay.computeElements(client, config.map, session);
        if (dragging != null) {

            list = list.stream().map(e -> e.kind() == dragging
                    ? new MinimapBannerOverlay.Element(e.kind(), curX, curY, e.width(), e.height(),
                            e.banner(), e.iconSize(), e.text(), e.scale())
                    : e).toList();
        }
        return list;
    }

    private int[] resizeHandle(MinimapBannerOverlay.Element e) {
        int s = 8;
        return new int[]{e.x() + e.width() + 1, e.y() + e.height() + 1, s};
    }

    private MinimapBannerOverlay.Element ottoHoveredElement(double mx, double my) {
        for (var e : elements()) {
            if (mx >= e.x() - 2 && mx <= e.x() + e.width() + 2
                    && my >= e.y() - 2 && my <= e.y() + e.height() + 2) {
                return e;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {

            for (var e : elements()) {
                int[] hnd = resizeHandle(e);
                if (click.x() >= hnd[0] && click.x() <= hnd[0] + hnd[2]
                        && click.y() >= hnd[1] && click.y() <= hnd[1] + hnd[2]) {
                    resizing = e.kind();
                    resizeStartX = click.x();
                    resizeStartY = click.y();
                    resizeStartScale = switch (e.kind()) {
                        case NAME -> config.map.nameHudScale;
                        case FACTION -> config.map.factionHudScale;
                        default -> config.map.stateHudScale;
                    };
                    resizeStartSize = config.map.minimapBannerSize;
                    resizeStartWidth = switch (e.kind()) {
                        case NAME -> config.map.nameHudWidth;
                        case FACTION -> config.map.factionHudWidth;
                        default -> config.map.stateHudWidth;
                    };
                    return true;
                }
            }
            for (var e : elements()) {
                if (click.x() >= e.x() - 2 && click.x() <= e.x() + e.width() + 2
                        && click.y() >= e.y() - 2 && click.y() <= e.y() + e.height() + 2) {
                    dragging = e.kind();
                    dragOffX = click.x() - e.x();
                    dragOffY = click.y() - e.y();
                    curX = e.x();
                    curY = e.y();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (resizing != null) {

            double dx = click.x() - resizeStartX;
            double dy = click.y() - resizeStartY;
            switch (resizing) {
                case BANNER -> config.map.minimapBannerSize =
                        Math.max(8, Math.min(64, resizeStartSize + (int) Math.round((dx + dy) / 2.0)));
                case NAME -> {
                    config.map.nameHudWidth =
                            Math.max(20, Math.min(400, resizeStartWidth + (int) Math.round(dx)));
                    config.map.nameHudScale =
                            MinimapBannerOverlay.clampScale(resizeStartScale + (float) (dy * 0.02));
                }
                case STATE -> {
                    config.map.stateHudWidth =
                            Math.max(20, Math.min(400, resizeStartWidth + (int) Math.round(dx)));
                    config.map.stateHudScale =
                            MinimapBannerOverlay.clampScale(resizeStartScale + (float) (dy * 0.02));
                }
                case FACTION -> {
                    config.map.factionHudWidth =
                            Math.max(20, Math.min(400, resizeStartWidth + (int) Math.round(dx)));
                    config.map.factionHudScale =
                            MinimapBannerOverlay.clampScale(resizeStartScale + (float) (dy * 0.02));
                }
            }
            return true;
        }
        if (dragging != null) {
            var element = elements().stream()
                    .filter(e -> e.kind() == dragging).findFirst().orElse(null);
            int w = element != null ? element.width() : 40;
            int h = element != null ? element.height() : 10;
            curX = Math.max(0, Math.min((int) Math.round(click.x() - dragOffX), width - w));
            curY = Math.max(0, Math.min((int) Math.round(click.y() - dragOffY), height - h));
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (resizing != null) {
            resizing = null;
            config.save();
            return true;
        }
        if (dragging != null) {
            var element = elements().stream()
                    .filter(e -> e.kind() == dragging).findFirst().orElse(null);
            if (element != null) {
                MinimapBannerOverlay.savePosition(config.map, dragging,
                        curX, curY, element.width(), element.height(), width, height);
                config.save();
            }
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.map.bannerEdit.hint"), width / 2, 10, 0xFFE6C8A9);
        List<MinimapBannerOverlay.Element> list = elements();
        if (list.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.map.bannerEdit.noRegion"),
                    width / 2, height / 2, 0xFFCC8888);
            return;
        }
        var hovered = ottoHoveredElement(mouseX, mouseY);
        for (var e : list) {
            boolean active = e.kind() == dragging;
            int col = active ? 0xFFFFD479 : 0xFFB08D57;
            ctx.fill(e.x() - 2, e.y() - 2, e.x() + e.width() + 2, e.y() - 1, col);
            ctx.fill(e.x() - 2, e.y() + e.height() + 1, e.x() + e.width() + 2,
                    e.y() + e.height() + 2, col);
            ctx.fill(e.x() - 2, e.y() - 1, e.x() - 1, e.y() + e.height() + 1, col);
            ctx.fill(e.x() + e.width() + 1, e.y() - 1, e.x() + e.width() + 2,
                    e.y() + e.height() + 1, col);
            MinimapBannerOverlay.draw(ctx, client, e, config.map);

            int[] hnd = resizeHandle(e);
            boolean hov = (mouseX >= hnd[0] && mouseX <= hnd[0] + hnd[2]
                    && mouseY >= hnd[1] && mouseY <= hnd[1] + hnd[2])
                    || resizing == e.kind();
            int hcol = hov ? 0xFFFFD479 : 0xFFB08D57;
            ctx.fill(hnd[0], hnd[1], hnd[0] + hnd[2], hnd[1] + hnd[2],
                    hov ? 0xCC7A5A3A : 0x99000000);

            for (int i = 1; i < hnd[2] - 1; i++) {
                ctx.fill(hnd[0] + i, hnd[1] + i, hnd[0] + i + 1, hnd[1] + i + 1, hcol);
            }
            ctx.fill(hnd[0] + 1, hnd[1] + 2, hnd[0] + 3, hnd[1] + 3, hcol);
            ctx.fill(hnd[0] + 2, hnd[1] + 1, hnd[0] + 3, hnd[1] + 3, hcol);
            ctx.fill(hnd[0] + hnd[2] - 3, hnd[1] + hnd[2] - 3, hnd[0] + hnd[2] - 1,
                    hnd[1] + hnd[2] - 2, hcol);
            ctx.fill(hnd[0] + hnd[2] - 3, hnd[1] + hnd[2] - 3, hnd[0] + hnd[2] - 2,
                    hnd[1] + hnd[2] - 1, hcol);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldSkipWorldRender() {
        return false;
    }

    @Override
    public Screen getEscape() {
        return parent;
    }

    @Override
    public void onDropdownOpen(DropDownWidget widget) {
    }

    @Override
    public void onDropdownClosed(DropDownWidget widget) {
    }
}
