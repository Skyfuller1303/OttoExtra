package de.ottoextra.config;

import de.ottoextra.map.PoliticalOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MapGroupColorsScreen extends Screen {

    private static final int ROW_H = 20;

    private final Screen parent;
    private final OttoExtraConfig config;

    private final List<String> groupNames = new ArrayList<>();
    private final List<TextFieldWidget> hexFields = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private TextFieldWidget searchField;
    private int scroll = 0;

    public MapGroupColorsScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.map.groupColors.title"));
        this.parent = parent;
        this.config = config;
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
        return Math.min(height - 16, 300);
    }

    private int listTop() {
        return panelY() + 48;
    }

    private int listBottom() {
        return panelY() + panelH() - 28;
    }

    @Override
    protected void init() {
        groupNames.clear();
        hexFields.clear();
        baseY.clear();

        searchField = new TextFieldWidget(textRenderer, panelX() + 8, panelY() + 24,
                panelW() - 16, 16, Text.translatable("ottoextra.rpbook.search"));
        searchField.setSuggestion(Text.translatable("ottoextra.rpbook.search").getString());
        searchField.setChangedListener(s -> {
            searchField.setSuggestion(s.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.search").getString() : "");
            scroll = 0;
            rebuildRows();
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(panelX() + panelW() - 60, panelY() + panelH() - 24, 52, 16).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.map.groupColors.adopt"), b -> {
            for (Map.Entry<String, Integer> e : PoliticalOverlay.groupTintOverview().entrySet()) {
                config.map.groupColors.putIfAbsent(e.getKey(),
                        String.format("#%06X", e.getValue() & 0xFFFFFF));
            }
            pushColors();
            rebuildRows();
        }).dimensions(panelX() + 8, panelY() + panelH() - 24, 150, 16).build());

        rebuildRows();
    }

    private void rebuildRows() {
        for (TextFieldWidget f : hexFields) {
            remove(f);
        }
        groupNames.clear();
        hexFields.clear();
        baseY.clear();

        String q = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        Map<String, Integer> overview = PoliticalOverlay.groupTintOverview();
        int x = panelX() + 8;
        int y = listTop();
        for (Map.Entry<String, Integer> e : overview.entrySet()) {
            String name = e.getKey();
            if (!q.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            groupNames.add(name);
            TextFieldWidget f = new TextFieldWidget(textRenderer,
                    x + panelW() - 16 - 70, y, 56, 14, Text.empty());
            f.setMaxLength(7);
            String existing = config.map.groupColors.get(name);
            f.setText(existing == null ? "" : existing);
            f.setChangedListener(s -> applyColor(name, s));
            hexFields.add(f);
            baseY.add(y);
            addDrawableChild(f);
            y += ROW_H;
        }
        applyScroll();
    }

    private void applyColor(String name, String raw) {
        String s = raw == null ? "" : raw.trim().replace("#", "");
        if (s.isEmpty()) {
            if (config.map.groupColors.remove(name) != null) {
                pushColors();
            }
            return;
        }
        if (s.length() == 6 && s.matches("[0-9a-fA-F]{6}")) {
            config.map.groupColors.put(name, "#" + s.toUpperCase(Locale.ROOT));
            pushColors();
        }
    }

    private void pushColors() {
        config.save();
        PoliticalOverlay.setUserGroupColors(config.map.groupColors);
    }

    private int maxScroll() {
        int rows = groupNames.size();
        int visible = Math.max(1, (listBottom() - listTop()) / ROW_H);
        return Math.max(0, (rows - visible) * ROW_H);
    }

    private void applyScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        for (int i = 0; i < hexFields.size(); i++) {
            TextFieldWidget f = hexFields.get(i);
            int y = baseY.get(i) - scroll;
            f.setY(y);
            f.visible = y >= listTop() - 2 && y + 14 <= listBottom() + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseY >= listTop() && mouseY <= listBottom()) {
            scroll -= (int) Math.signum(vertical) * ROW_H;
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, OttoExtraConfigScreen.COL_BORDER);
        ctx.fill(px, py, px + pw, py + ph, OttoExtraConfigScreen.COL_BG);
        ctx.drawText(textRenderer, getTitle(), px + 8, py + 8, OttoExtraConfigScreen.COL_TITLE, false);

        Map<String, Integer> overview = PoliticalOverlay.groupTintOverview();
        for (int i = 0; i < groupNames.size(); i++) {
            int y = baseY.get(i) - scroll;
            if (y < listTop() - 2 || y + 14 > listBottom() + 2) {
                continue;
            }
            String name = groupNames.get(i);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(name, pw - 110),
                    px + 8, y + 3, OttoExtraConfigScreen.COL_TEXT, false);
            Integer tint = overview.get(name);
            int rgb = tint == null ? 0xFF888888 : 0xFF000000 | (tint & 0xFFFFFF);
            int sx = px + pw - 16 - 70 - 16;
            ctx.fill(sx - 1, y + 1, sx + 13, y + 15, OttoExtraConfigScreen.COL_BORDER);
            ctx.fill(sx, y + 2, sx + 12, y + 14, rgb);
        }
        if (maxScroll() > 0) {
            int trackTop = listTop();
            int trackH = listBottom() - trackTop;
            int barH = Math.max(12, trackH * trackH / (trackH + maxScroll()));
            int barY = trackTop + (trackH - barH) * scroll / maxScroll();
            ctx.fill(px + pw - 6, barY, px + pw - 3, barY + barH, OttoExtraConfigScreen.COL_BORDER);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
