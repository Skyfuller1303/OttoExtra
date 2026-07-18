package de.ottoextra.config;

import de.ottoextra.config.OttoExtraConfig.RegionTheme;
import de.ottoextra.regions.RegionNotificationOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RegionThemeScreen extends Screen {

    private static final int COL_LABEL = 0xFFFFFFFF;
    private static final int COL_HEADER = 0xFFFFFF99;
    private static final int COL_DESC = 0xFFA0A0A0;
    private static final int COL_SWATCH_BORDER = 0xFF000000;

    private static final String[] SLOTS =
            {"bg", "borderOut", "borderTl", "borderBr", "title", "region", "hierarchy", "hint"};
    private static final String[] LIGHT_HEX =
            {"#C8AC8E", "#513E2A", "#E6C8A9", "#B8926E", "#503D29", "#503D29", "#7A5A3A", "#6A4D33"};
    private static final String[] DARK_HEX =
            {"#212D3B", "#0A0A0A", "#344459", "#191F22", "#FFFFFF", "#FFFFFF", "#9BC7DC", "#9D9D9D"};

    private final Screen parent;
    private final OttoExtraConfig config;

    private String selected;

    private static final class Row {
        ClickableWidget widget;
        int baseY;
        Text label;
        int colorSlot = -1;
        boolean header;
        Supplier<String> swatchHex;
    }

    private final List<Row> rows = new ArrayList<>();
    private int scroll;

    public RegionThemeScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.regions.themes.title"));
        this.parent = parent;
        this.config = config;
        this.selected = config.regions.theme == null ? "light" : config.regions.theme;
    }

    private int panelX() {
        return Math.max(8, (width - panelW()) / 2);
    }

    private int panelY() {
        return 28;
    }

    private int panelW() {
        return Math.min(width - 16, 460);
    }

    private int panelH() {
        return Math.min(height - 40, 320);
    }

    private int editorTop() {
        return panelY() + 30;
    }

    private int editorBottom() {
        return panelY() + panelH() - 30;
    }

    @Override
    protected void init() {
        rows.clear();
        int px = panelX();
        int py = panelY();
        int listX = px + 8;
        int y = py + 30;

        for (String key : themeKeys()) {
            String label = displayName(key) + (key.equalsIgnoreCase(config.regions.theme) ? " ✓" : "");
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> activate(key))
                    .dimensions(listX, y, 116, 16).build());
            y += 18;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.regions.themes.add"),
                b -> addCustom()).dimensions(listX, y + 4, 116, 16).build());

        int ex = px + 132;
        int ew = panelW() - 140;
        RegionTheme custom = customByName(selected);
        if (custom != null) {
            buildEditor(custom, ex, ew);
            addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.regions.themes.delete"),
                    b -> deleteCustom(custom)).dimensions(ex, py + panelH() - 24, 80, 16).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.regions.themes.duplicate"),
                    b -> duplicateBuiltin(selected)).dimensions(ex, py + 34, 140, 16).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(px + panelW() - 60, py + panelH() - 24, 52, 16).build());

        applyScroll();
        RegionNotificationOverlay.holdPreview("Sankt Aegidius", "Abtei in Grafschaft Holdstewik");
    }

    private void buildEditor(RegionTheme t, int ex, int ew) {
        int[] ry = {editorTop()};
        int fieldW = 60;
        int fieldX = ex + ew - fieldW;

        TextFieldWidget nameField = field(ex + 40, ry[0], ew - 40, t.name, 24, s -> renameCustom(t, s));
        rowWidget(nameField, ry[0], Text.translatable("ottoextra.regions.themes.name"));
        ry[0] += 20;

        header(ex, ry, "ottoextra.regions.themes.colors");
        for (int i = 0; i < SLOTS.length; i++) {
            final int slot = i;
            TextFieldWidget f = field(fieldX, ry[0], fieldW, getSlot(t, slot), 7, s -> applyColor(t, slot, s));
            Row r = rowWidget(f, ry[0], Text.literal(SLOTS[slot]));
            r.colorSlot = slot;
            r.swatchHex = () -> getSlot(t, slot);
            ry[0] += 18;
        }

        header(ex, ry, "ottoextra.regions.themes.fonts");
        floatRow(t, ry, fieldX, fieldW, "ottoextra.adv.baseTextScale", () -> t.baseTextScale, v -> t.baseTextScale = v);
        floatRow(t, ry, fieldX, fieldW, "ottoextra.adv.titleScale", () -> t.titleScale, v -> t.titleScale = v);
        floatRow(t, ry, fieldX, fieldW, "ottoextra.adv.regionScale", () -> t.regionScale, v -> t.regionScale = v);
        floatRow(t, ry, fieldX, fieldW, "ottoextra.adv.hierarchyScale", () -> t.hierarchyScale, v -> t.hierarchyScale = v);
        floatRow(t, ry, fieldX, fieldW, "ottoextra.adv.hintScale", () -> t.hintScale, v -> t.hintScale = v);

        header(ex, ry, "ottoextra.regions.themes.visibility");
        toggleRow(ex, ew, ry, "ottoextra.config.regions.show.banner", () -> t.showBanner, v -> t.showBanner = v);
        toggleRow(ex, ew, ry, "ottoextra.config.regions.show.title", () -> t.showEnteredTitle, v -> t.showEnteredTitle = v);
        toggleRow(ex, ew, ry, "ottoextra.config.regions.show.hierarchy", () -> t.showHierarchy, v -> t.showHierarchy = v);
        toggleRow(ex, ew, ry, "ottoextra.config.regions.show.hint", () -> t.showHint, v -> t.showHint = v);

        header(ex, ry, "ottoextra.regions.themes.spacing");
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.maxTextWidth", () -> t.maxTextWidth, v -> t.maxTextWidth = v, 50, 800);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.minToastWidth", () -> t.minToastWidth, v -> t.minToastWidth = v, 50, 800);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.maxToastWidth", () -> t.maxToastWidth, v -> t.maxToastWidth = v, 50, 1000);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.screenTopMargin", () -> t.screenTopMargin, v -> t.screenTopMargin = v, 0, 200);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.iconSize", () -> t.iconSize, v -> t.iconSize = v, 8, 64);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.iconGap", () -> t.iconGap, v -> t.iconGap = v, 0, 32);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.paddingLeft", () -> t.paddingLeft, v -> t.paddingLeft = v, 0, 40);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.paddingRight", () -> t.paddingRight, v -> t.paddingRight = v, 0, 40);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.paddingTop", () -> t.paddingTop, v -> t.paddingTop = v, 0, 40);
        intRow(t, ry, fieldX, fieldW, "ottoextra.adv.paddingBottom", () -> t.paddingBottom, v -> t.paddingBottom = v, 0, 40);
    }

    private void header(int ex, int[] ry, String key) {
        ry[0] += 8;
        Row r = new Row();
        r.header = true;
        r.baseY = ry[0];
        r.label = Text.translatable(key);
        rows.add(r);
        ry[0] += 16;
    }

    private void floatRow(RegionTheme t, int[] ry, int fx, int fw, String key,
                          Supplier<Float> get, Consumer<Float> set) {
        TextFieldWidget f = field(fx, ry[0], fw, fmt(get.get()), 6, s -> {
            try {
                float v = Float.parseFloat(s.trim().replace(',', '.'));
                set.accept(Math.max(0.1f, Math.min(3f, v)));
                config.save();
            } catch (NumberFormatException ignored) {

            }
        });
        rowWidget(f, ry[0], Text.translatable(key));
        ry[0] += 18;
    }

    private void intRow(RegionTheme t, int[] ry, int fx, int fw, String key,
                        Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
        TextFieldWidget f = field(fx, ry[0], fw, String.valueOf(get.get()), 5, s -> {
            try {
                int v = Integer.parseInt(s.trim());
                set.accept(Math.max(min, Math.min(max, v)));
                config.save();
            } catch (NumberFormatException ignored) {

            }
        });
        rowWidget(f, ry[0], Text.translatable(key));
        ry[0] += 18;
    }

    private void toggleRow(int ex, int ew, int[] ry, String key,
                           BooleanSupplier get, Consumer<Boolean> set) {
        ButtonWidget[] ref = new ButtonWidget[1];
        ref[0] = ButtonWidget.builder(toggleLabel(key, get.getAsBoolean()), b -> {
            boolean nv = !get.getAsBoolean();
            set.accept(nv);
            config.save();
            ref[0].setMessage(toggleLabel(key, nv));
        }).dimensions(ex, ry[0], ew, 16).build();
        addDrawableChild(ref[0]);
        rowWidget(ref[0], ry[0], null);
        ry[0] += 18;
    }

    private Text toggleLabel(String key, boolean on) {
        return Text.translatable(key).copy().append(": ")
                .append(Text.translatable(on ? "ottoextra.config.on" : "ottoextra.config.off"));
    }

    private TextFieldWidget field(int x, int y, int w, String value, int maxLen,
                                  Consumer<String> onChange) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 14, Text.empty());
        f.setMaxLength(maxLen);
        f.setText(value == null ? "" : value);
        f.setChangedListener(onChange);
        addDrawableChild(f);
        return f;
    }

    private Row rowWidget(ClickableWidget w, int baseY, Text label) {
        Row r = new Row();
        r.widget = w;
        r.baseY = baseY;
        r.label = label;
        rows.add(r);
        return r;
    }

    private static String fmt(float v) {
        return String.valueOf(v);
    }

    private int contentBottomY() {
        int max = editorTop();
        for (Row r : rows) {
            max = Math.max(max, r.baseY + 18);
        }
        return max;
    }

    private int maxScroll() {
        return Math.max(0, contentBottomY() - editorBottom());
    }

    private void applyScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        for (Row r : rows) {
            if (r.widget == null) {
                continue;
            }
            int y = r.baseY - scroll;
            r.widget.setY(y);
            r.widget.visible = y >= editorTop() - 2 && y + 16 <= editorBottom() + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= panelX() + 124 && mouseY >= editorTop() && mouseY <= editorBottom()) {
            scroll -= (int) Math.signum(vertical) * 16;
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private List<String> themeKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("light");
        keys.add("dark");
        if (config.regions.customThemes != null) {
            for (RegionTheme t : config.regions.customThemes) {
                if (t != null && t.name != null && !t.name.isBlank()) {
                    keys.add(t.name);
                }
            }
        }
        return keys;
    }

    private String displayName(String key) {
        if ("light".equalsIgnoreCase(key)) {
            return Text.translatable("ottoextra.regions.theme.light").getString();
        }
        if ("dark".equalsIgnoreCase(key)) {
            return Text.translatable("ottoextra.regions.theme.dark").getString();
        }
        return key;
    }

    private RegionTheme customByName(String name) {
        if (name == null || config.regions.customThemes == null) {
            return null;
        }
        for (RegionTheme t : config.regions.customThemes) {
            if (t != null && name.equalsIgnoreCase(t.name)) {
                return t;
            }
        }
        return null;
    }

    private void activate(String key) {
        selected = key;
        config.regions.theme = key;
        config.save();
        scroll = 0;
        clearAndInit();
    }

    private void addCustom() {
        RegionTheme t = fromHex(LIGHT_HEX);
        t.name = uniqueName("Custom");
        config.regions.customThemes.add(t);
        config.regions.theme = t.name;
        selected = t.name;
        config.save();
        scroll = 0;
        clearAndInit();
    }

    private void duplicateBuiltin(String key) {
        RegionTheme t = fromHex("dark".equalsIgnoreCase(key) ? DARK_HEX : LIGHT_HEX);
        t.name = uniqueName(displayName(key) + " Kopie");
        config.regions.customThemes.add(t);
        config.regions.theme = t.name;
        selected = t.name;
        config.save();
        scroll = 0;
        clearAndInit();
    }

    private void deleteCustom(RegionTheme t) {
        config.regions.customThemes.remove(t);
        if (t.name != null && t.name.equalsIgnoreCase(config.regions.theme)) {
            config.regions.theme = "light";
        }
        selected = config.regions.theme;
        config.save();
        scroll = 0;
        clearAndInit();
    }

    private void renameCustom(RegionTheme t, String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || "light".equalsIgnoreCase(name) || "dark".equalsIgnoreCase(name)) {
            return;
        }
        boolean wasActive = t.name != null && t.name.equalsIgnoreCase(config.regions.theme);
        t.name = name;
        if (wasActive) {
            config.regions.theme = name;
        }
        selected = name;
        config.save();
    }

    private void applyColor(RegionTheme t, int slot, String raw) {
        String s = raw == null ? "" : raw.trim().replace("#", "");
        if (s.length() == 6 && s.matches("[0-9a-fA-F]{6}")) {
            setSlot(t, slot, "#" + s.toUpperCase(Locale.ROOT));
            config.save();
        }
    }

    private String uniqueName(String base) {
        String name = base;
        int n = 1;
        while (customByName(name) != null) {
            name = base + " " + (++n);
        }
        return name;
    }

    private static RegionTheme fromHex(String[] hex) {
        RegionTheme t = new RegionTheme();
        for (int i = 0; i < SLOTS.length; i++) {
            setSlot(t, i, hex[i]);
        }
        return t;
    }

    private static String getSlot(RegionTheme t, int i) {
        return switch (i) {
            case 0 -> t.bg;
            case 1 -> t.borderOut;
            case 2 -> t.borderTl;
            case 3 -> t.borderBr;
            case 4 -> t.title;
            case 5 -> t.region;
            case 6 -> t.hierarchy;
            default -> t.hint;
        };
    }

    private static void setSlot(RegionTheme t, int i, String v) {
        switch (i) {
            case 0 -> t.bg = v;
            case 1 -> t.borderOut = v;
            case 2 -> t.borderTl = v;
            case 3 -> t.borderBr = v;
            case 4 -> t.title = v;
            case 5 -> t.region = v;
            case 6 -> t.hierarchy = v;
            default -> t.hint = v;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int px = panelX();
        int py = panelY();
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 12, COL_LABEL);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.regions.themes.list"),
                px + 8, py + 18, COL_HEADER);

        RegionTheme custom = customByName(selected);
        if (custom == null) {
            ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.regions.themes.builtin"),
                    px + 132, py + 18, COL_DESC);
        } else {
            int top = editorTop();
            int bottom = editorBottom();
            ctx.enableScissor(px + 124, top - 2, px + panelW(), bottom + 2);
            for (Row r : rows) {
                int y = r.baseY - scroll;
                if (y < top - 12 || y > bottom + 2) {
                    continue;
                }
                if (r.header) {
                    ctx.drawTextWithShadow(textRenderer, r.label, px + 132, y, COL_HEADER);
                    ctx.fill(px + 132, y + 10, px + panelW() - 8, y + 11, 0x33FFFFFF);
                } else if (r.label != null) {
                    int lx = r.colorSlot >= 0 ? px + 132 + 16 : px + 132;
                    ctx.drawTextWithShadow(textRenderer, r.label, lx, y + 3, COL_LABEL);
                    if (r.colorSlot >= 0 && r.swatchHex != null) {
                        int rgb = 0xFF000000 | (parseHex(r.swatchHex.get()) & 0xFFFFFF);
                        int sx = px + 132;
                        ctx.fill(sx - 1, y, sx + 13, y + 14, COL_SWATCH_BORDER);
                        ctx.fill(sx, y + 1, sx + 12, y + 13, rgb);
                    }
                }
            }
            ctx.disableScissor();
        }

        RegionNotificationOverlay.render(ctx, null);
    }

    private static int parseHex(String s) {
        if (s == null) {
            return 0x888888;
        }
        String h = s.replace("#", "").trim();
        return h.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(h, 16) : 0x888888;
    }

    @Override
    public void close() {
        RegionNotificationOverlay.clear();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
