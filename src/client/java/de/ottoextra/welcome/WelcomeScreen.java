package de.ottoextra.welcome;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.text.Text;

/**
 * Einwilligungsassistent. Jede neue Online-Entscheidung beginnt bei
 * "nicht erlaubt"; das Abschließen ist auch ohne Zustimmung möglich.
 */
public final class WelcomeScreen extends Screen {

    private static final int MAX_PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 326;
    private static final int LAST_PAGE = 5;

    private final Screen parent;
    private final Runnable backToIntro;
    private int page;
    private boolean resourcePacks;
    private boolean externalImages;
    private boolean updateChecks;
    private boolean communityRpNameUploads;
    private boolean onlineServices;
    private final java.util.Map<de.ottoextra.privacy.ConsentPolicy.Purpose, String> shownScopes =
            new java.util.EnumMap<>(de.ottoextra.privacy.ConsentPolicy.Purpose.class);
    private ScrollableTextWidget body;
    private int bodyPage = -1;
    private RoleplaySetupScreen roleplaySetup;

    WelcomeScreen(Screen parent) {
        this(parent, null);
    }

    WelcomeScreen(Screen parent, Runnable backToIntro) {
        super(Text.translatable("ottoextra.consent.title"));
        this.parent = parent;
        this.backToIntro = backToIntro;
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
        if (page == 0) {
            var config = de.ottoextra.config.OttoExtraConfig.active();
            for (var purpose : de.ottoextra.privacy.ConsentPolicy.Purpose.values()) {
                shownScopes.put(purpose, de.ottoextra.privacy.ConsentPolicy.scope(config, purpose));
            }
            content = de.ottoextra.privacy.ConsentDetailsScreen.overview(config)
                    .copy().styled(style -> style.withColor(WelcomeStyle.TEXT));
        } else {
            var purpose = java.util.Arrays.stream(de.ottoextra.privacy.ConsentPolicy.Purpose.values())
                    .filter(value -> de.ottoextra.privacy.ConsentDetailsScreen.page(value) == page).findFirst().orElseThrow();
            var config = de.ottoextra.config.OttoExtraConfig.active();
            shownScopes.put(purpose, de.ottoextra.privacy.ConsentPolicy.scope(config, purpose));
            content = de.ottoextra.privacy.ConsentDetailsScreen.information(config, purpose)
                    .copy().styled(style -> style.withColor(WelcomeStyle.TEXT));
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
            int choicesX = centerX - panelWidth / 2 + 12;
            if (backToIntro != null) {
                addDrawableChild(new WelcomeStyle.Button(choicesX, panelTop + 30, 58, 20,
                        Text.translatable("ottoextra.consent.back"), false, button -> close()));
            }
            int choiceWidth = (panelWidth - 36) / 3;
            addDrawableChild(new WelcomeStyle.Button(choicesX, buttonY, choiceWidth, 20,
                    Text.translatable("ottoextra.consent.all.deny"), false, button -> finishAll(false)));
            addDrawableChild(new WelcomeStyle.Button(choicesX + choiceWidth + 6, buttonY, choiceWidth, 20,
                    Text.translatable(panelWidth < 400 ? "ottoextra.consent.individual.short"
                            : "ottoextra.consent.individual"), false, button -> advanceIntro()));
            addDrawableChild(new WelcomeStyle.Button(choicesX + 2 * (choiceWidth + 6), buttonY, choiceWidth, 20,
                    Text.translatable("ottoextra.consent.all.allow"), false, button -> finishAll(true)));
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
        var allow = addDrawableChild(new WelcomeStyle.Button(choicesX + 2 * (choiceWidth + 6), buttonY, choiceWidth, 20,
                Text.translatable("ottoextra.consent.allow"), false, button -> decideAndAdvance(true)));
        allow.setTooltip(Tooltip.of(Text.translatable("ottoextra.consent.accept_next")));
        allow.active = page != 4 || de.ottoextra.privacy.ConsentPolicy.COMMUNITY_UPLOADS_AVAILABLE;
    }

    private void advanceIntro() {
        page = 1;
        clearAndInit();
    }

    private void finishAll(boolean allow) {
        if (client == null) return;
        // Use exactly the sources shown on this page; do not silently accept later changes.
        // Bulk decisions preserve the user's existing RP/display choices and finish immediately.
        WelcomeScreenManager.accept(allow, allow, allow, allow, allow, shownScopes, null);
        client.setScreen(parent);
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
                        resourcePacks, externalImages, updateChecks, communityRpNameUploads, onlineServices, shownScopes, choices);
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
            case 5 -> onlineServices = value;
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
        return backToIntro != null;
    }

    @Override
    public void close() {
        if (backToIntro != null) backToIntro.run();
    }
}
