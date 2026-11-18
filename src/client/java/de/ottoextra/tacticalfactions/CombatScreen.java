package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.theme.InkCheckbox;
import de.ottoextra.tacticalfactions.theme.InkTextField;
import de.ottoextra.tacticalfactions.theme.OttonienTheme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Kampf tab: combat mode and the display / protection options of the same config as the settings cards. */
final class CombatScreen extends BookScreen {
    private static final String SIDE_DISPLAY = "display";
    private static final String SIDE_TABLIST = "tablist";

    private String section = SIDE_DISPLAY;
    private InkTextField allyField;
    private InkTextField hostileField;
    private InkCheckbox tablistBox;
    private final List<InkCheckbox> displayBoxes = new java.util.ArrayList<>();
    private int toggleX;
    private int toggleY;

    CombatScreen(Screen parent) {
        super(parent, Tab.KAMPF, Text.translatable("ottoextra_tacticalfactions.book.title"));
    }

    private boolean ffAvailable() { return BuildVariant.friendlyFireProtectionAvailable(); }

    @Override boolean hasLeaf() { return ffAvailable(); }

    @Override int leafHeight() { return Math.min(lfh, 112); }

    @Override
    List<SideDef> sideDefs() {
        return List.of(
                new SideDef(SIDE_DISPLAY, Text.translatable("ottoextra_tacticalfactions.kampf.side.display"),
                        OttonienTheme.TERRACOTTA),
                new SideDef(SIDE_TABLIST, Text.translatable("ottoextra_tacticalfactions.kampf.side.tablist"),
                        OttonienTheme.NEUTRAL_TAN));
    }

    @Override String activeSide() { return section; }

    @Override
    void onSide(String id) {
        section = id;
        applyVisibility();
    }

    @Override
    void initContent() {
        TacticalConfig config = TacticalFactions.config();
        displayBoxes.clear();
        int x = pageX() + 10;
        int y = pageY() + 9;
        int right = pageX() + pageW() - 10;
        toggleX = right - 32;
        toggleY = y - 1;

        int cy = y + 28;
        displayBoxes.add(page(checkbox("ottoextra_tacticalfactions.option.names", x, cy, config.showPlayerNames,
                value -> config.showPlayerNames = value)));
        cy += 14;
        displayBoxes.add(page(checkbox("ottoextra_tacticalfactions.option.colors", x, cy, config.politicalColors,
                value -> config.politicalColors = value)));
        cy += 14;
        displayBoxes.add(page(checkbox("ottoextra_tacticalfactions.option.retaliationMarkers", x, cy, config.retaliationMarkers,
                value -> config.retaliationMarkers = value)));
        cy += 14 + 8 + 11;
        // Colour rows: swatch (drawn), label (drawn) and an editable hex field.
        allyField = page(colorField(right, cy, config.allyColor, true));
        cy += 14;
        hostileField = page(colorField(right, cy, config.hostileColor, false));

        tablistBox = page(checkbox("ottoextra_tacticalfactions.option.tablistHostilesFirst", x, y + 28,
                config.tablistHostilesFirst, value -> config.tablistHostilesFirst = value));
        tablistBox.setTooltip(Tooltip.of(Text.translatable("ottoextra_tacticalfactions.option.tablistHostilesFirst.tip")));

        if (ffAvailable()) {
            int lx = lfx + 10;
            int ly = lfy + 10 + 17 + 30;
            InkCheckbox prevent = leaf(checkbox("ottoextra_tacticalfactions.option.friendlyFire", lx, ly,
                    config.preventFriendlyFire, value -> config.preventFriendlyFire = value));
            prevent.setTooltip(Tooltip.of(Text.translatable("ottoextra_tacticalfactions.option.friendlyFire.warning")));
            leaf(checkbox("ottoextra_tacticalfactions.option.neutralProtection", lx, ly + 14,
                    config.protectNeutralPlayers, value -> config.protectNeutralPlayers = value));
        }
    }

    private InkCheckbox checkbox(String key, int x, int y, boolean checked,
                                 java.util.function.Consumer<Boolean> setter) {
        return InkCheckbox.builder(Text.translatable(key), textRenderer).pos(x, y).checked(checked)
                .callback((box, value) -> {
                    setter.accept(value);
                    TacticalFactions.config().save();
                }).build();
    }

    private InkTextField colorField(int right, int y, String current, boolean ally) {
        int w = 50;
        InkTextField field = new InkTextField(textRenderer, right - w - 2, y, w, 12, Text.empty(), false);
        field.setMaxLength(7);
        field.setText(current);
        field.setChangedListener(value -> {
            String valid = TacticalConfig.validColor(value, null);
            field.setEditableColor(valid != null ? OttonienTheme.INK : OttonienTheme.WAX_TEXT);
            if (valid == null) return;
            TacticalConfig config = TacticalFactions.config();
            if (ally) config.allyColor = valid; else config.hostileColor = valid;
            config.save();
        });
        return field;
    }

    @Override
    void applyExtraVisibility() {
        boolean display = section.equals(SIDE_DISPLAY);
        boolean pageShown = pageVisible();
        displayBoxes.forEach(box -> box.visible = pageShown && display);
        if (tablistBox != null) tablistBox.visible = pageShown && !display;
        if (allyField != null) allyField.visible = pageShown && display;
        if (hostileField != null) hostileField.visible = pageShown && display;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && pageVisible() && section.equals(SIDE_DISPLAY)
                && click.x() >= toggleX && click.x() < toggleX + 31
                && click.y() >= toggleY && click.y() < toggleY + 11) {
            TacticalFactionsClient.setCombatMode(!TacticalFactions.config().combatMode);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    // ---- rendering ----

    @Override
    void drawPage(DrawContext ctx, int mouseX, int mouseY) {
        TacticalConfig config = TacticalFactions.config();
        int lx = pageX();
        int x = lx + 10;
        int y = pageY() + 9;
        int right = lx + pageW() - 10;
        ctx.enableScissor(lx, pageY(), lx + pageW(), pageY() + pageH());
        if (section.equals(SIDE_DISPLAY)) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.mode")
                    .formatted(Formatting.BOLD), x, y, OttonienTheme.INK, false);
            drawToggle(ctx, config.combatMode);
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.key_hint",
                    TacticalFactionsClient.combatKeyName()), x, y + 11, OttonienTheme.INK_MUTED, false);
            BookArt.line(ctx, x - 2, right, y + 22);
            int cy = y + 28 + 14 * 3;
            BookArt.line(ctx, x - 2, right, cy + 1);
            cy += 7;
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.colors"),
                    x, cy, OttonienTheme.INK_MUTED, false);
            cy += 11;
            drawColorRow(ctx, x, cy, "ottoextra_tacticalfactions.option.allyColor", config.allyColor);
            cy += 14;
            drawColorRow(ctx, x, cy, "ottoextra_tacticalfactions.option.hostileColor", config.hostileColor);
            cy += 14;
            BookArt.line(ctx, x - 2, right, cy + 1);
            cy += 7;
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.preview"),
                    x, cy, OttonienTheme.INK_MUTED, false);
            drawPreview(ctx, x, cy + 11, right, config);
        } else {
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.card.tablist")
                    .formatted(Formatting.BOLD), x, y, OttonienTheme.INK, false);
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.tablist_hint"),
                    x, y + 11, OttonienTheme.INK_MUTED, false);
            BookArt.line(ctx, x - 2, right, y + 22);
            int ty = y + 28 + 18;
            for (var line : textRenderer.wrapLines(
                    Text.translatable("ottoextra_tacticalfactions.option.tablistHostilesFirst.tip"), right - x)) {
                ctx.drawText(textRenderer, line, x, ty, OttonienTheme.INK_MUTED, false);
                ty += 10;
            }
            ty += 4;
            for (var line : textRenderer.wrapLines(
                    Text.translatable("ottoextra_tacticalfactions.kampf.tablist_squads"), right - x)) {
                ctx.drawText(textRenderer, line, x, ty, OttonienTheme.INK_MUTED, false);
                ty += 10;
            }
        }
        ctx.disableScissor();
    }

    private void drawToggle(DrawContext ctx, boolean on) {
        int x = toggleX;
        int y = toggleY;
        ctx.fill(x, y, x + 31, y + 11, 0xFF0E0E10);
        ctx.fill(x + 1, y + 1, x + 30, y + 10, on ? OttonienTheme.shade(OttonienTheme.GREEN, .8)
                : OttonienTheme.shade(OttonienTheme.NEUTRAL_TAN, .7));
        if (on) {
            ctx.fill(x + 21, y + 1, x + 30, y + 10, OttonienTheme.LABEL);
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.on"),
                    x + 4, y + 2, OttonienTheme.LABEL, false);
        } else {
            ctx.fill(x + 1, y + 1, x + 10, y + 10, OttonienTheme.LABEL);
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.off"),
                    x + 12, y + 2, OttonienTheme.LABEL, false);
        }
    }

    private void drawColorRow(DrawContext ctx, int x, int y, String labelKey, String hex) {
        ctx.fill(x, y, x + 11, y + 11, 0xFF0E0E10);
        ctx.fill(x + 1, y + 1, x + 10, y + 10, BookArt.parseHex(hex));
        ctx.drawText(textRenderer, Text.translatable(labelKey), x + 15, y + 2, OttonienTheme.INK, false);
    }

    private void drawPreview(DrawContext ctx, int x, int y, int right, TacticalConfig config) {
        List<Squad> squads = config.squads;
        boolean colored = config.politicalColors;
        int lines = squads.isEmpty() ? 3 : 4;
        ctx.fill(x, y, right, y + 10 * lines + 3, OttonienTheme.PREVIEW_DARK);
        int ty = y + 3;
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.preview.hostile"),
                x + 4, ty, colored ? BookArt.parseHex(config.hostileColor) : OttonienTheme.LABEL, false);
        ty += 10;
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.preview.ally"),
                x + 4, ty, colored ? BookArt.parseHex(config.allyColor) : OttonienTheme.LABEL, false);
        ty += 10;
        if (!squads.isEmpty()) {
            Squad squad = squads.get(0);
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.preview.squad",
                            squad.name), x + 4, ty,
                    colored && squad.alwaysAllied ? BookArt.parseHex(squad.color) : OttonienTheme.LABEL, false);
            ty += 10;
        }
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.preview.neutral"),
                x + 4, ty, OttonienTheme.LABEL, false);
    }

    @Override
    void drawLeaf(DrawContext ctx, int mouseX, int mouseY) {
        int x = lfx + 10;
        int y = lfy + 10;
        OttonienTheme.pictoSeal(ctx, x, y - 1);
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.kampf.warning.title")
                .formatted(Formatting.BOLD), x + 16, y + 1, OttonienTheme.WAX_TEXT, false);
        int ty = y + 17;
        for (var line : textRenderer.wrapLines(
                Text.translatable("ottoextra_tacticalfactions.kampf.warning.body"), lfw - 20)) {
            ctx.drawText(textRenderer, line, x, ty, OttonienTheme.INK, false);
            ty += 10;
        }
    }
}
