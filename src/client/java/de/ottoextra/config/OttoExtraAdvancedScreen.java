package de.ottoextra.config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
public final class OttoExtraAdvancedScreen extends Screen {
    private record Field(String labelKey, Supplier<String> get, Consumer<String> set) {
    }
    private final Screen parent;
    private final OttoExtraConfig config;
    private final List<Field> fields;
    private final boolean withPreview;
    private final List<TextFieldWidget> inputs = new ArrayList<>();
    private final List<Integer> baseYs = new ArrayList<>();
    private int scrollOffset = 0;
    private OttoExtraAdvancedScreen(Screen parent, OttoExtraConfig config,
                                    Text title, List<Field> fields, boolean withPreview) {
        super(title);
        this.parent = parent;
        this.config = config;
        this.fields = fields;
        this.withPreview = withPreview;
    }
    public static OttoExtraAdvancedScreen regions(Screen parent, OttoExtraConfig c) {
        List<Field> f = new ArrayList<>();
        f.add(intField("ottoextra.adv.maxTextWidth", () -> c.regions.maxTextWidth, v -> c.regions.maxTextWidth = v, 50, 800));
        f.add(intField("ottoextra.adv.minToastWidth", () -> c.regions.minToastWidth, v -> c.regions.minToastWidth = v, 50, 800));
        f.add(intField("ottoextra.adv.maxToastWidth", () -> c.regions.maxToastWidth, v -> c.regions.maxToastWidth = v, 50, 1000));
        f.add(intField("ottoextra.adv.screenTopMargin", () -> c.regions.screenTopMargin, v -> c.regions.screenTopMargin = v, 0, 200));
        f.add(intField("ottoextra.adv.iconSize", () -> c.regions.iconSize, v -> c.regions.iconSize = v, 8, 64));
        f.add(intField("ottoextra.adv.iconGap", () -> c.regions.iconGap, v -> c.regions.iconGap = v, 0, 32));
        f.add(intField("ottoextra.adv.paddingLeft", () -> c.regions.paddingLeft, v -> c.regions.paddingLeft = v, 0, 40));
        f.add(intField("ottoextra.adv.paddingRight", () -> c.regions.paddingRight, v -> c.regions.paddingRight = v, 0, 40));
        f.add(intField("ottoextra.adv.paddingTop", () -> c.regions.paddingTop, v -> c.regions.paddingTop = v, 0, 40));
        f.add(intField("ottoextra.adv.paddingBottom", () -> c.regions.paddingBottom, v -> c.regions.paddingBottom = v, 0, 40));
        f.add(floatField("ottoextra.adv.titleScale", () -> c.regions.titleScale, v -> c.regions.titleScale = v));
        f.add(floatField("ottoextra.adv.regionScale", () -> c.regions.regionScale, v -> c.regions.regionScale = v));
        f.add(floatField("ottoextra.adv.hierarchyScale", () -> c.regions.hierarchyScale, v -> c.regions.hierarchyScale = v));
        f.add(floatField("ottoextra.adv.baseTextScale", () -> c.regions.baseTextScale, v -> c.regions.baseTextScale = v));
        f.add(floatField("ottoextra.adv.hintScale", () -> c.regions.hintScale, v -> c.regions.hintScale = v));
        return new OttoExtraAdvancedScreen(parent, c,
                Text.translatable("ottoextra.config.advanced.regions"), f, true);
    }
    public static OttoExtraAdvancedScreen resourcepack(Screen parent, OttoExtraConfig c) {
        List<Field> f = new ArrayList<>();
        f.add(new Field("ottoextra.adv.manifestUrl",
                () -> c.resourcepack.manifestUrl, v -> c.resourcepack.manifestUrl = v.trim()));
        f.add(new Field("ottoextra.adv.assetName",
                () -> c.resourcepack.assetName, v -> c.resourcepack.assetName = v.trim()));
        f.add(intField("ottoextra.adv.maxSizeMb",
                () -> (int) (c.resourcepack.maxSizeBytes / (1024 * 1024)),
                v -> c.resourcepack.maxSizeBytes = v * 1024L * 1024L, 1, 512));
        f.add(intField("ottoextra.adv.connectTimeoutMs",
                () -> c.resourcepack.connectTimeoutMs, v -> c.resourcepack.connectTimeoutMs = v, 1000, 120_000));
        f.add(intField("ottoextra.adv.requestTimeoutMs",
                () -> c.resourcepack.requestTimeoutMs, v -> c.resourcepack.requestTimeoutMs = v, 1000, 300_000));
        return new OttoExtraAdvancedScreen(parent, c,
                Text.translatable("ottoextra.config.advanced.resourcepack"), f, false);
    }
    public static OttoExtraAdvancedScreen nametags(Screen parent, OttoExtraConfig c) {
        List<Field> f = new ArrayList<>();
        f.add(floatField("ottoextra.adv.tagTitleScale",
                () -> c.nametags.titleScale, v -> c.nametags.titleScale = v));
        f.add(floatField("ottoextra.adv.tagNameScale",
                () -> c.nametags.nameScale, v -> c.nametags.nameScale = v));
        f.add(floatField("ottoextra.adv.tagAccountScale",
                () -> c.nametags.accountScale, v -> c.nametags.accountScale = v));
        f.add(intField("ottoextra.adv.tagLineSpacing",
                () -> c.nametags.lineSpacing, v -> c.nametags.lineSpacing = v, 4, 30));
        f.add(new Field("ottoextra.adv.tagAccountColor",
                () -> c.nametags.accountColor, v -> c.nametags.accountColor = v.trim()));
        return new OttoExtraAdvancedScreen(parent, c,
                Text.translatable("ottoextra.config.advanced.nametags"), f, false);
    }
    public static OttoExtraAdvancedScreen map(Screen parent, OttoExtraConfig c) {
        List<Field> f = new ArrayList<>();
        f.add(doubleField("ottoextra.adv.nameMinScale",
                () -> c.map.nameMinScale, v -> c.map.nameMinScale = v, 0.005, 4));
        f.add(doubleField("ottoextra.adv.bannerMinScale",
                () -> c.map.bannerMinScale, v -> c.map.bannerMinScale = v, 0.005, 8));
        f.add(floatField("ottoextra.adv.labelScale",
                () -> c.map.labelScale, v -> c.map.labelScale = v));
        f.add(doubleField("ottoextra.adv.politicalMaxScale",
                () -> c.map.politicalMaxScale, v -> c.map.politicalMaxScale = v, 0.01, 8));
        f.add(doubleField("ottoextra.adv.labelZoomA",
                () -> c.map.labelZoomA, v -> c.map.labelZoomA = v, 0.005, 8));
        f.add(doubleField("ottoextra.adv.labelZoomB",
                () -> c.map.labelZoomB, v -> c.map.labelZoomB = v, 0.005, 8));
        f.add(floatField("ottoextra.adv.factionScaleA",
                () -> c.map.factionScaleA, v -> c.map.factionScaleA = v));
        f.add(floatField("ottoextra.adv.factionScaleB",
                () -> c.map.factionScaleB, v -> c.map.factionScaleB = v));
        f.add(floatField("ottoextra.adv.lehenScaleA",
                () -> c.map.lehenScaleA, v -> c.map.lehenScaleA = v));
        f.add(floatField("ottoextra.adv.lehenScaleB",
                () -> c.map.lehenScaleB, v -> c.map.lehenScaleB = v));
        f.add(intField("ottoextra.adv.bannerSizeA",
                () -> c.map.bannerSizeA, v -> c.map.bannerSizeA = v, 4, 64));
        f.add(intField("ottoextra.adv.bannerSizeB",
                () -> c.map.bannerSizeB, v -> c.map.bannerSizeB = v, 4, 96));
        f.add(intField("ottoextra.adv.borderWidthPx",
                () -> c.map.borderWidthPx, v -> c.map.borderWidthPx = v, 1, 4));
        f.add(new Field("ottoextra.adv.borderColor",
                () -> c.map.borderColor, v -> c.map.borderColor = v.trim()));
        f.add(intField("ottoextra.adv.dashLengthPx",
                () -> c.map.dashLengthPx, v -> c.map.dashLengthPx = v, 2, 64));
        f.add(intField("ottoextra.adv.dashGapPx",
                () -> c.map.dashGapPx, v -> c.map.dashGapPx = v, 0, 64));
        f.add(intField("ottoextra.adv.minimapBannerSize",
                () -> c.map.minimapBannerSize, v -> c.map.minimapBannerSize = v, 8, 64));
        f.add(intField("ottoextra.adv.minimapBannerOffsetX",
                () -> c.map.minimapBannerOffsetX, v -> c.map.minimapBannerOffsetX = v, -200, 200));
        f.add(intField("ottoextra.adv.minimapBannerOffsetY",
                () -> c.map.minimapBannerOffsetY, v -> c.map.minimapBannerOffsetY = v, -200, 200));
        f.add(intField("ottoextra.adv.minimapBannerOffsetXRound",
                () -> c.map.minimapBannerOffsetXRound, v -> c.map.minimapBannerOffsetXRound = v, -200, 200));
        f.add(intField("ottoextra.adv.minimapBannerOffsetYRound",
                () -> c.map.minimapBannerOffsetYRound, v -> c.map.minimapBannerOffsetYRound = v, -200, 200));
        f.add(intField("ottoextra.adv.nameHudWidth",
                () -> c.map.nameHudWidth, v -> c.map.nameHudWidth = v, 20, 400));
        f.add(intField("ottoextra.adv.stateHudWidth",
                () -> c.map.stateHudWidth, v -> c.map.stateHudWidth = v, 20, 400));
        f.add(floatField("ottoextra.adv.nameHudScale",
                () -> c.map.nameHudScale, v -> c.map.nameHudScale = v));
        f.add(floatField("ottoextra.adv.stateHudScale",
                () -> c.map.stateHudScale, v -> c.map.stateHudScale = v));
        f.add(new Field("ottoextra.adv.nameHudColor",
                () -> c.map.nameHudColor, v -> c.map.nameHudColor = v.trim()));
        f.add(new Field("ottoextra.adv.stateHudColor",
                () -> c.map.stateHudColor, v -> c.map.stateHudColor = v.trim()));
        return new OttoExtraAdvancedScreen(parent, c,
                Text.translatable("ottoextra.config.advanced.map"), f, false);
    }
    private static Field doubleField(String key, Supplier<Double> get, Consumer<Double> set,
                                     double min, double max) {
        return new Field(key, () -> String.format(java.util.Locale.ROOT, "%.3f", get.get()), raw -> {
            try {
                double v = Double.parseDouble(raw.trim().replace(',', '.'));
                if (v >= min && v <= max) {
                    set.accept(v);
                }
            } catch (NumberFormatException ignored) {
            }
        });
    }
    private static Field floatField(String key, Supplier<Float> get, Consumer<Float> set) {
        return new Field(key, () -> String.format(java.util.Locale.ROOT, "%.2f", get.get()), raw -> {
            try {
                float v = Float.parseFloat(raw.trim().replace(',', '.'));
                if (v >= 0.3f && v <= 3f) {
                    set.accept(v);
                }
            } catch (NumberFormatException ignored) {
            }
        });
    }
    private static Field intField(String key, Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
        return new Field(key, () -> Integer.toString(get.get()), raw -> {
            try {
                int v = Integer.parseInt(raw.trim());
                if (v >= min && v <= max) {
                    set.accept(v);
                }
            } catch (NumberFormatException ignored) {
            }
        });
    }
    private int panelX() {
        return Math.max(8, (width - panelW()) / 2);
    }
    private int panelY() {
        return Math.max(8, (height - panelH()) / 2);
    }
    private int panelW() {
        return Math.min(width - 16, 420);
    }
    private int panelH() {
        int cols = fields.size() > 6 ? 2 : 1;
        int rows = (fields.size() + cols - 1) / cols;
        return Math.min(height - 16, 26 + rows * 22 + 34);
    }
    private int viewTop() {
        return panelY() + 24;
    }
    private int viewBottom() {
        return panelY() + panelH() - 30;
    }
    private int maxScroll() {
        int cols = fields.size() > 6 ? 2 : 1;
        int rows = (fields.size() + cols - 1) / cols;
        int contentBottom = panelY() + 26 + rows * 22;
        return Math.max(0, contentBottom - viewBottom());
    }
    private void applyScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
        for (int i = 0; i < inputs.size(); i++) {
            TextFieldWidget w = inputs.get(i);
            int y = baseYs.get(i) - scrollOffset;
            w.setY(y);
            w.visible = y >= viewTop() - 2 && y + 18 <= viewBottom() + 4;
        }
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll() > 0
                && mouseX >= panelX() && mouseX <= panelX() + panelW()
                && mouseY >= viewTop() && mouseY <= viewBottom()) {
            scrollOffset -= (int) Math.signum(vertical) * 22;
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    @Override
    protected void init() {
        inputs.clear();
        baseYs.clear();
        int cols = fields.size() > 6 ? 2 : 1;
        int colW = (panelW() - 24) / cols;
        int fieldW = 70;
        int rowH = 22;
        int startY = panelY() + 26;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = panelX() + 12 + col * colW;
            int y = startY + row * rowH;
            boolean wide = cols == 1;
            int fw = wide ? Math.min(220, colW - 4) : fieldW;
            TextFieldWidget input = new TextFieldWidget(textRenderer,
                    x + colW - fw - 6, y, fw, 18, Text.translatable(field.labelKey()));
            input.setMaxLength(256);
            input.setText(field.get().get());
            inputs.add(input);
            baseYs.add(y);
            addDrawableChild(input);
        }
        applyScroll();
        int bw = 100;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> saveAndClose())
                .dimensions(panelX() + panelW() - bw - 8, panelY() + panelH() - 26, bw, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> client.setScreen(parent))
                .dimensions(panelX() + 8, panelY() + panelH() - 26, bw, 18).build());
        if (withPreview) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.config.preview"), b -> {
                applyFields();
                OttoExtraConfigScreen.triggerPreview();
            }).dimensions(panelX() + (panelW() - bw) / 2, panelY() + panelH() - 26, bw, 18).build());
        }
    }
    private void applyFields() {
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).set().accept(inputs.get(i).getText());
        }
        config.repair();
    }
    private void saveAndClose() {
        applyFields();
        config.save();
        client.setScreen(parent);
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();
        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, OttoExtraConfigScreen.COL_BORDER);
        ctx.fill(px, py, px + pw, py + ph, OttoExtraConfigScreen.COL_PANEL);
        ctx.fill(px, py, px + pw, py + 1, OttoExtraConfigScreen.COL_INNER_TL);
        ctx.fill(px, py, px + 1, py + ph, OttoExtraConfigScreen.COL_INNER_TL);
        ctx.fill(px, py + ph - 1, px + pw, py + ph, OttoExtraConfigScreen.COL_INNER_BR);
        ctx.fill(px + pw - 1, py, px + pw, py + ph, OttoExtraConfigScreen.COL_INNER_BR);
        ctx.drawText(textRenderer, title, px + 8, py + 9, OttoExtraConfigScreen.COL_TITLE, false);
        int cols = fields.size() > 6 ? 2 : 1;
        int colW = (pw - 24) / cols;
        int rowH = 22;
        int startY = py + 26;
        for (int i = 0; i < fields.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = px + 12 + col * colW;
            int y = startY + row * rowH + 5 - scrollOffset;
            if (y < viewTop() || y + 9 > viewBottom() + 4) {
                continue;
            }
            ctx.drawText(textRenderer,
                    textRenderer.trimToWidth(Text.translatable(fields.get(i).labelKey()).getString(), colW - 84),
                    x, y, OttoExtraConfigScreen.COL_TEXT, false);
        }
        int ms = maxScroll();
        if (ms > 0) {
            int trackX = px + pw - 5;
            int trackTop = viewTop();
            int trackH = viewBottom() - trackTop;
            ctx.fill(trackX, trackTop, trackX + 3, trackTop + trackH, OttoExtraConfigScreen.COL_INNER_BR);
            int thumbH = Math.max(12, trackH * trackH / (trackH + ms));
            int thumbY = trackTop + (trackH - thumbH) * scrollOffset / ms;
            ctx.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, OttoExtraConfigScreen.COL_BORDER);
        }
        super.render(ctx, mouseX, mouseY, delta);
        if (withPreview) {
            de.ottoextra.regions.RegionNotificationOverlay.render(ctx, null);
        }
    }
    @Override
    public void close() {
        client.setScreen(parent);
    }
}
