package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.calibration.LetterCalibrationService;
import de.ottoextra.letter.calibration.LetterCalibrationSession;
import de.ottoextra.logging.DebugLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

public final class LetterCalibrationScreen extends Screen {
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 250;
    private final OttoExtraConfig config;
    private int shownPage;

    public LetterCalibrationScreen(OttoExtraConfig config) {
        super(Text.literal("OttoExtra Briefkalibrierung"));
        this.config = config;
    }

    public static void open(OttoExtraConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        DebugLog.setEnabled(true);
        LetterCalibrationService.activeOrCreate(config);
        client.setScreen(new LetterCalibrationScreen(config));
    }

    private int x() {
        return (width - PANEL_W) / 2;
    }

    private int y() {
        return Math.max(8, (height - PANEL_H) / 2);
    }

    @Override
    protected void init() {
        LetterCalibrationSession session = LetterCalibrationService.activeOrCreate(config);
        LetterCalibrationSession.CaseResult result = LetterCalibrationService.current();
        int x = x();
        int y = y();
        addDrawableChild(ButtonWidget.builder(Text.literal("◀ Test"), b -> {
            LetterCalibrationService.move(-1);
            shownPage = 0;
            client.setScreen(new LetterCalibrationScreen(config));
        }).dimensions(x + 12, y + 198, 58, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Test ▶"), b -> {
            LetterCalibrationService.move(1);
            shownPage = 0;
            client.setScreen(new LetterCalibrationScreen(config));
        }).dimensions(x + 74, y + 198, 58, 18).build());
        if (result != null && result.sourcePages.size() > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Seite ▶"), b ->
                    shownPage = (shownPage + 1) % result.sourcePages.size())
                    .dimensions(x + 136, y + 198, 50, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Vorschau"), b -> {
            LetterDraft draft = new LetterDraft();
            draft.pages = new java.util.ArrayList<>(result.sourcePages);
            client.setScreen(new LetterPreviewScreen(this, config, draft, shownPage));
        }).dimensions(x + 190, y + 198, 56, 18).build());
        ButtonWidget send = ButtonWidget.builder(Text.literal("/letter senden"), b -> {
            if (LetterCalibrationService.sendCurrent(config)) {
                client.setScreen(null);
            }
        }).dimensions(x + 250, y + 198, 68, 18).build();
        send.active = result != null && ("PENDING".equals(result.status)
                || result.status.startsWith("SENDING_") && result.commands.isEmpty());
        addDrawableChild(send);

        addDrawableChild(ButtonWidget.builder(Text.literal("Ordner öffnen"), b ->
                Util.getOperatingSystem().open(
                        LetterCalibrationService.activeDirectory().toFile()))
                .dimensions(x + 12, y + 220, 92, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Messreihe beenden"), b -> {
            LetterCalibrationService.finish();
            close();
        }).dimensions(x + 108, y + 220, 112, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(x + 224, y + 220, 94, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = x();
        int y = y();
        ctx.fill(x - 2, y - 2, x + PANEL_W + 2, y + PANEL_H + 2, 0xFF4A382A);
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFFC8AC8E);
        LetterCalibrationSession session = LetterCalibrationService.active();
        LetterCalibrationSession.CaseResult result = LetterCalibrationService.current();
        if (session == null || result == null) {
            return;
        }
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, y + 10,
                0xFF503D29);
        ctx.drawText(textRenderer, "Test " + (session.currentCase + 1) + "/"
                + session.cases.size() + ": " + result.id + " — " + result.purpose,
                x + 12, y + 29, 0xFF503D29, false);
        ctx.drawText(textRenderer, "Status: " + result.status, x + 12, y + 42,
                statusColor(result.status), false);
        ctx.drawText(textRenderer, "Frischen Brief halten; es wird niemals /post gesendet.",
                x + 12, y + 57, 0xFF765B41, false);
        shownPage = Math.max(0, Math.min(shownPage, result.sourcePages.size() - 1));
        ctx.fill(x + 11, y + 73, x + PANEL_W - 11, y + 190, 0xFFBFA17E);
        ctx.drawText(textRenderer, "Sollseite " + (shownPage + 1) + "/"
                + result.sourcePages.size(), x + 18, y + 79, 0xFF765B41, false);
        var lines = textRenderer.wrapLines(
                Text.literal(result.sourcePages.get(shownPage)), PANEL_W - 38);
        for (int i = 0; i < Math.min(10, lines.size()); i++) {
            ctx.drawText(textRenderer, lines.get(i), x + 18, y + 94 + i * 9,
                    0xFF503D29, false);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static int statusColor(String status) {
        if ("CAPTURED".equals(status)) {
            return 0xFF227722;
        }
        if (status.startsWith("SENT") || status.startsWith("SENDING")) {
            return 0xFFAA6600;
        }
        return 0xFF503D29;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
