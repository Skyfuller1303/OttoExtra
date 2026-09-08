package de.ottoextra.welcome;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.text.Text;

/**
 * Verpflichtender Erststart-Assistent. Jede Online-Entscheidung beginnt bei
 * "nicht erlaubt"; das Abschließen ist auch ohne Zustimmung möglich.
 */
public final class WelcomeScreen extends Screen {

    private static final int MAX_PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 326;
    private static final int LAST_PAGE = 4;

    private final Screen parent;
    private int page;
    private boolean resourcePacks;
    private boolean externalImages;
    private boolean updateChecks;
    private boolean communityRpNameUploads;
    private ScrollableTextWidget body;
    private int bodyPage = -1;
    private RoleplaySetupScreen roleplaySetup;

    WelcomeScreen(Screen parent) {
        super(Text.translatable("ottoextra.consent.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int panelTop = (height - panelHeight()) / 2;
        int buttonY = panelTop + panelHeight() - 32;
        int panelWidth = panelWidth();

        // Preserve the reading position on resize, but start each new page at the top.
        double scrollY = body != null && bodyPage == page ? body.getScrollY() : 0;
        Text content = Text.empty().append(Text.translatable("ottoextra.consent.page." + page + ".body")
                .styled(style -> style.withColor(WelcomeStyle.TEXT)));
        if (page > 0) {
            content = content.copy().append("\n\n")
                    .append(Text.translatable("ottoextra.consent.page." + page + ".details")
                            .styled(style -> style.withColor(WelcomeStyle.MUTED)));
        }
        int bodyTop = panelTop + 80;
        int bodyBottom = buttonY - 12;
        body = addDrawableChild(new ScrollableTextWidget(centerX - panelWidth / 2 + 18,
                bodyTop, panelWidth - 44, bodyBottom - bodyTop, content, textRenderer) {
            @Override
            protected void drawBox(DrawContext context) {
                // The surrounding welcome panel supplies the background.
            }
        });
        body.setScrollY(scrollY);
        bodyPage = page;

        if (page == 0) {
            addDrawableChild(new WelcomeStyle.Button(centerX - 60, buttonY, 120, 20,
                    Text.translatable("ottoextra.consent.next"), true, button -> advanceIntro()));
            return;
        }

        int choicesX = centerX - panelWidth / 2 + 12;
        int choiceWidth = (panelWidth - 36) / 3;
        addDrawableChild(new WelcomeStyle.Button(choicesX, buttonY, choiceWidth, 20,
                Text.translatable("ottoextra.consent.back"), false, button -> {
                    page--;
                    clearAndInit();
                }));
        // Both decisions have equal emphasis; the gold hover/focus is shared.
        addDrawableChild(new WelcomeStyle.Button(choicesX + choiceWidth + 6, buttonY, choiceWidth, 20,
                Text.translatable("ottoextra.consent.deny"), false, button -> decideAndAdvance(false)))
                .setTooltip(Tooltip.of(Text.translatable("ottoextra.consent.deny_next")));
        addDrawableChild(new WelcomeStyle.Button(choicesX + 2 * (choiceWidth + 6), buttonY, choiceWidth, 20,
                Text.translatable("ottoextra.consent.allow"), false, button -> decideAndAdvance(true)))
                .setTooltip(Tooltip.of(Text.translatable("ottoextra.consent.accept_next")));
    }

    private void advanceIntro() {
        page = 1;
        clearAndInit();
    }

    private void decideAndAdvance(boolean accepted) {
        setCurrentChoice(accepted);
        if (page < LAST_PAGE) {
            page++;
            clearAndInit();
            return;
        }
        if (client != null) {
            if (roleplaySetup == null) roleplaySetup = new RoleplaySetupScreen(parent, this, choices -> {
                WelcomeScreenManager.accept(
                        resourcePacks, externalImages, updateChecks, communityRpNameUploads, choices);
                client.setScreen(parent);
            });
            client.setScreen(roleplaySetup);
        }
    }

    private void setCurrentChoice(boolean value) {
        switch (page) {
            case 1 -> resourcePacks = value;
            case 2 -> externalImages = value;
            case 3 -> updateChecks = value;
            case 4 -> communityRpNameUploads = value;
            default -> {
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.render(ctx, mouseX, mouseY, delta);
        }
        ctx.fill(0, 0, width, height, 0xB0000000);

        int panelWidth = panelWidth();
        int left = width / 2 - panelWidth / 2;
        int top = (height - panelHeight()) / 2;
        WelcomeStyle.panel(ctx, left, top, panelWidth, panelHeight());

        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2,
                top + 13, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.consent.progress", page + 1, LAST_PAGE + 1),
                width / 2, top + 31, WelcomeStyle.MUTED);

        Text heading = Text.translatable("ottoextra.consent.page." + page + ".heading");
        ctx.drawCenteredTextWithShadow(textRenderer, heading, width / 2,
                top + 61, WelcomeStyle.GOLD);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, width - 16);
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, height - 16);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        // Erst "Assistent abschließen" speichert eine informierte Entscheidung.
    }
}
