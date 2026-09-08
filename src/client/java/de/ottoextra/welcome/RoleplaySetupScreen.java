package de.ottoextra.welcome;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.function.Consumer;

/** Optional first-run display settings, kept as a draft until the user applies them. */
final class RoleplaySetupScreen extends Screen {
    private static final int GOLD = WelcomeStyle.GOLD;
    private static final int MUTED = WelcomeStyle.MUTED;
    private static final int GAP = 6;
    private final Screen background;
    private final Screen previous;
    private final Consumer<RoleplayChoices> onFinish;
    private final EnumMap<RoleplayPreset, RoleplayChoices> drafts = new EnumMap<>(RoleplayPreset.class);
    private final EnumMap<RoleplayChoices.Option, ToggleButton> toggles = new EnumMap<>(RoleplayChoices.Option.class);
    private RoleplayPreset selected;
    private RoleplayChoices.Group group = RoleplayChoices.Group.CHAT;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int rowHeight;
    private boolean finished;

    RoleplaySetupScreen(Screen background, Screen previous, Consumer<RoleplayChoices> onFinish) {
        super(Text.translatable("ottoextra.setup.rp.title"));
        this.background = background;
        this.previous = previous;
        this.onFinish = onFinish;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(560, width - 16);
        panelHeight = Math.min(326, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        toggles.clear();
        int buttonY = top + panelHeight - 32;
        rowHeight = Math.min(40, (buttonY - 12 - (top + 82)) / 5);

        if (selected == null) {
            for (RoleplayPreset preset : RoleplayPreset.values()) {
                int x = columnX(preset.ordinal(), 3);
                button(x + 8, buttonY - 38, columnWidth(3) - 16,
                        Text.translatable(preset.translationKey("name")), true, b -> {
                            selected = preset;
                            drafts.computeIfAbsent(preset, RoleplayChoices::new);
                            group = RoleplayChoices.Group.CHAT;
                            clearAndInit();
                        }).setTooltip(Tooltip.of(Text.translatable(preset.translationKey("description"))));
            }
        } else {
            for (RoleplayChoices.Group tab : RoleplayChoices.Group.values()) {
                button(columnX(tab.ordinal(), 3), top + 54, columnWidth(3),
                        Text.translatable(tab.translationKey()), tab == group, b -> {
                            group = tab;
                            clearAndInit();
                        });
            }
            int y = top + 82;
            for (RoleplayChoices.Option option : RoleplayChoices.Option.values()) {
                if (option.group != group) continue;
                ToggleButton toggle = addDrawableChild(new ToggleButton(y, option));
                toggles.put(option, toggle);
                y += rowHeight;
            }
            refreshToggles();
        }

        int count = selected == null ? 2 : 3;
        button(columnX(0, count), buttonY, columnWidth(count),
                Text.translatable("ottoextra.consent.back"), false, b -> {
                    if (selected == null) {
                        if (client != null) client.setScreen(previous);
                    } else {
                        selected = null;
                        clearAndInit();
                    }
                });
        button(columnX(1, count), buttonY, columnWidth(count),
                Text.translatable("ottoextra.setup.rp.skip"), false, b -> finish(null))
                .setTooltip(Tooltip.of(Text.translatable("ottoextra.setup.rp.skip_hint")));
        if (selected != null) {
            button(columnX(2, count), buttonY, columnWidth(count),
                    Text.translatable("ottoextra.setup.rp.apply"), true, b -> finish(choices()));
        }
    }

    private int columnWidth(int count) { return (panelWidth - 24 - GAP * (count - 1)) / count; }
    private int columnX(int index, int count) { return left + 12 + index * (columnWidth(count) + GAP); }
    private RoleplayChoices choices() { return drafts.get(selected); }

    private WelcomeStyle.Button button(int x, int y, int w, Text label, boolean accent, ButtonWidget.PressAction action) {
        return addDrawableChild(new WelcomeStyle.Button(x, y, w, 20, label, accent, action));
    }

    private void refreshToggles() {
        toggles.forEach((option, button) -> {
            button.active = choices().isAvailable(option);
            button.setMessage(Text.translatable("ottoextra.setup.rp.option_state",
                    Text.translatable(option.translationKey()), stateText(option)));
        });
    }

    private Text stateText(RoleplayChoices.Option option) {
        return Text.translatable(choices().isOn(option) ? "ottoextra.settings.on" : "ottoextra.settings.off");
    }

    private void finish(RoleplayChoices choices) {
        if (finished) return;
        finished = true;
        onFinish.accept(choices);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (background != null) background.render(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, 0xB0000000);
        WelcomeStyle.panel(ctx, left, top, panelWidth, panelHeight);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, top + 13, 0xFFFFFFFF);
        Text subtitle = selected == null ? Text.translatable("ottoextra.setup.rp.choose")
                : Text.translatable("ottoextra.setup.rp.customize", Text.translatable(selected.translationKey("name")));
        ctx.drawCenteredTextWithShadow(textRenderer, subtitle, width / 2, top + 31, MUTED);

        if (selected == null) {
            for (RoleplayPreset preset : RoleplayPreset.values()) {
                int x = columnX(preset.ordinal(), 3);
                int w = columnWidth(3);
                int cardTop = top + 54;
                int cardBottom = top + panelHeight - 44;
                ctx.fill(x, cardTop, x + w, cardBottom, WelcomeStyle.CARD);
                ctx.fill(x, cardTop, x + w, cardTop + 1, WelcomeStyle.BORDER);
                ctx.drawTextWithShadow(textRenderer, Text.literal("0" + (preset.ordinal() + 1)), x + 8, cardTop + 8, MUTED);
                ctx.drawTextWithShadow(textRenderer, Text.translatable(preset.translationKey("name")), x + 8, cardTop + 24, GOLD);
                drawWrapped(ctx, Text.translatable(preset.translationKey("description")), x + 8,
                        cardTop + 38, w - 16, cardBottom - 32, WelcomeStyle.TEXT);
                if (cardBottom - cardTop >= 180) {
                    ctx.fill(x + 8, cardTop + 108, x + w - 8, cardTop + 147, WelcomeStyle.INSET);
                    ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.setup.rp.example"),
                            x + 14, cardTop + 115, MUTED);
                    ctx.drawTextWithShadow(textRenderer, Text.translatable(preset.translationKey("example")),
                            x + 14, cardTop + 130, GOLD);
                }
            }
        } else {
            int hintTop = top + 82 + toggles.size() * rowHeight + 8;
            int hintBottom = top + panelHeight - 48;
            drawWrapped(ctx, Text.translatable(group.translationKey() + ".hint"), left + 16,
                    hintTop, panelWidth - 32, hintBottom, MUTED);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawWrapped(DrawContext ctx, Text text, int x, int y, int w, int bottom, int color) {
        for (OrderedText line : textRenderer.wrapLines(text, w)) {
            if (y + 9 > bottom) break;
            ctx.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 10;
        }
    }

    private final class ToggleButton extends WelcomeStyle.Button {
        private final RoleplayChoices.Option option;

        ToggleButton(int y, RoleplayChoices.Option option) {
            super(left + 12, y, panelWidth - 24, rowHeight - 2,
                    net.minecraft.text.Text.translatable(option.translationKey()), false, b -> {
                        choices().toggle(option);
                        refreshToggles();
                    });
            this.option = option;
            setTooltip(Tooltip.of(net.minecraft.text.Text.translatable(option.translationKey() + ".hint")));
        }

        @Override
        protected void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
            drawSurface(ctx);
            int labelY = getY() + (getHeight() >= 32 ? 6 : (getHeight() - 9) / 2);
            int labelWidth = getWidth() - 70;
            ctx.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(net.minecraft.text.Text.translatable(option.translationKey()).getString(), labelWidth),
                    getX() + 8, labelY, active ? WelcomeStyle.TEXT : 0xFF6F829C);
            if (getHeight() >= 32) {
                ctx.drawTextWithShadow(textRenderer,
                        textRenderer.trimToWidth(net.minecraft.text.Text.translatable(option.translationKey() + ".hint").getString(), labelWidth),
                        getX() + 8, labelY + 13, active ? MUTED : 0xFF6F829C);
            }
            int pillX = getX() + getWidth() - 49;
            int pillY = getY() + (getHeight() - 14) / 2;
            boolean on = choices().isOn(option);
            ctx.fill(pillX, pillY, pillX + 42, pillY + 14,
                    !active ? 0xFF202A36 : on ? 0xFF5A4930 : 0xFF202D3B);
            ctx.drawCenteredTextWithShadow(textRenderer, stateText(option), pillX + 21, pillY + 3,
                    !active ? 0xFF6F829C : on ? 0xFFFFE2AD : MUTED);
        }
    }

    @Override
    public void close() {
        finish(null);
    }
}
