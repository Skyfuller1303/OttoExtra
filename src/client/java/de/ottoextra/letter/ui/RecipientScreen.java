package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Empfänger-Auswahl für Briefe: Suche über das lokale
 * Personenbuch (Accounts + RP-Namen), Klick wählt, Senden startet die
 * Brief-Queue ({@code /letter}-Zeilen + {@code /post <Empfänger>}).
 */
public final class RecipientScreen extends Screen {

    private static final int ROW_H = 11;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    private final List<LocalRpProfile> filtered = new ArrayList<>();
    private TextFieldWidget searchField;
    private String selected;
    private int scroll;

    public RecipientScreen(Screen parent, OttoExtraConfig config, LetterDraft draft) {
        super(Text.translatable("ottoextra.letter.recipient.title"));
        this.parent = parent;
        this.config = config;
        this.draft = draft;
    }

    private int listX() {
        return width / 2 - 90;
    }

    private int listTop() {
        return 56;
    }

    private int listBottom() {
        return height - 40;
    }

    @Override
    protected void init() {
        searchField = new TextFieldWidget(textRenderer, listX(), 32, 180, 16,
                Text.translatable("ottoextra.rpbook.search"));
        searchField.setChangedListener(s -> {
            scroll = 0;
            refilter();
        });
        addDrawableChild(searchField);
        ButtonWidget send = ButtonWidget.builder(
                Text.translatable("ottoextra.letter.recipient.send"), b -> {
                    if (selected != null) {
                        LetterServices.startLetterSend(config, draft, selected);
                        close();
                    }
                }).dimensions(width / 2 - 102, height - 30, 100, 20).build();
        addDrawableChild(send);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(width / 2 + 2, height - 30, 100, 20).build());
        refilter();
    }

    private void refilter() {
        filtered.clear();
        if (RpNamesServices.store() == null) {
            return;
        }
        String q = searchField == null ? ""
                : searchField.getText().toLowerCase(Locale.ROOT).trim();
        for (LocalRpProfile p : RpNamesServices.store().all()) {
            if (p.accountName == null) {
                continue;
            }
            if (q.isEmpty() || p.accountName.toLowerCase(Locale.ROOT).contains(q)
                    || (p.rpName != null && p.rpName.toLowerCase(Locale.ROOT).contains(q))) {
                filtered.add(p);
            }
        }
        filtered.sort(Comparator.comparing(p -> p.accountName.toLowerCase(Locale.ROOT)));
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(vertical) * 3,
                Math.max(0, filtered.size() - visibleRows())));
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.x() >= listX() && click.x() <= listX() + 180
                && click.y() >= listTop() && click.y() <= listBottom()) {
            int idx = scroll + (int) ((click.y() - listTop()) / ROW_H);
            if (idx >= 0 && idx < filtered.size()) {
                selected = filtered.get(idx).accountName;
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 14, 0xFFE6C8A9);
        int rows = visibleRows();
        for (int r = 0; r < rows; r++) {
            int idx = scroll + r;
            if (idx >= filtered.size()) {
                break;
            }
            LocalRpProfile p = filtered.get(idx);
            int y = listTop() + r * ROW_H;
            boolean isSel = p.accountName.equals(selected);
            if (isSel) {
                ctx.fill(listX() - 2, y - 1, listX() + 182, y + ROW_H - 1, 0x337A5A3A);
            }
            String label = p.accountName + (p.hasRpName() ? " → " + p.rpName : "");
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(label, 178),
                    listX(), y, isSel ? 0xFFFFD479 : 0xFFE6C8A9);
        }
        if (selected != null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.letter.recipient.selected", selected),
                    width / 2, listBottom() + 6, 0xFFB8A88F);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
