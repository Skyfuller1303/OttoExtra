package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.text.Text;

/** The same information is available when granting a permission later in Settings. */
public final class ConsentDetailsScreen extends Screen {
    private final Screen parent;
    private final OttoExtraConfig config;
    private final ConsentPolicy.Purpose purpose;
    private final String shownScope;

    private ConsentDetailsScreen(Screen parent, OttoExtraConfig config, ConsentPolicy.Purpose purpose) {
        super(Text.translatable("ottoextra.consent.title"));
        this.parent = parent;
        this.config = config;
        this.purpose = purpose;
        this.shownScope = ConsentPolicy.scope(config, purpose);
    }

    public static void change(OttoExtraConfig config, ConsentPolicy.Purpose purpose, boolean allow) {
        if (!allow) {
            ConsentPolicy.decide(config, purpose, false, ConsentPolicy.scope(config, purpose));
            config.save();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.setScreen(new ConsentDetailsScreen(client.currentScreen, config, purpose));
    }

    public static int page(ConsentPolicy.Purpose purpose) {
        return switch (purpose) {
            case RESOURCE_PACKS -> 1;
            case EXTERNAL_IMAGES -> 2;
            case UPDATE_CHECKS -> 3;
            case COMMUNITY_UPLOADS -> 4;
            case ONLINE_SERVICES -> 5;
        };
    }

    public static Text information(OttoExtraConfig config, ConsentPolicy.Purpose purpose) {
        return purposeInformation(config, purpose).copy().append("\n\n")
                .append(Text.translatable("ottoextra.consent.revoke.details"));
    }

    /** The bulk decision exposes the same purpose and source details as individual decisions. */
    public static Text overview(OttoExtraConfig config) {
        var text = Text.translatable("ottoextra.consent.page.0.body").copy()
                .append("\n\n").append(Text.translatable("ottoextra.consent.all.summary"));
        for (var purpose : ConsentPolicy.Purpose.values()) {
            text.append("\n\n").append(Text.translatable("ottoextra.consent.page." + page(purpose) + ".heading")
                    .styled(style -> style.withBold(true))).append("\n")
                    .append(purposeInformation(config, purpose));
        }
        return text.append("\n\n").append(Text.translatable("ottoextra.consent.revoke.details"));
    }

    private static Text purposeInformation(OttoExtraConfig config, ConsentPolicy.Purpose purpose) {
        Text details = switch (purpose) {
            case RESOURCE_PACKS -> Text.translatable("ottoextra.consent.pack.actual",
                    config.resourcepack.effectiveSource(), config.resourcepack.assetName,
                    config.resourcepack.maxSizeBytes,
                    Text.translatable(config.resourcepack.autoEnable ? "ottoextra.settings.on" : "ottoextra.settings.off"));
            case EXTERNAL_IMAGES -> Text.translatable("ottoextra.consent.images.actual", ConsentPolicy.apiSource(config));
            case ONLINE_SERVICES -> Text.translatable("ottoextra.consent.online.actual", ConsentPolicy.apiSource(config));
            case COMMUNITY_UPLOADS -> ConsentPolicy.COMMUNITY_UPLOADS_AVAILABLE
                    ? Text.translatable("ottoextra.consent.upload.actual", ConsentPolicy.apiSource(config))
                    : Text.translatable("ottoextra.consent.upload.unavailable");
            case UPDATE_CHECKS -> Text.translatable("ottoextra.consent.page.3.details");
        };
        return Text.empty().append(Text.translatable("ottoextra.consent.page." + page(purpose) + ".body"))
                .append("\n\n").append(details);
    }

    @Override protected void init() {
        int panelWidth = Math.min(550, width - 32);
        int left = (width - panelWidth) / 2;
        int top = 44;
        addDrawableChild(new ScrollableTextWidget(left, top, panelWidth,
                Math.max(30, height - 104), information(config, purpose), textRenderer));
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.consent.deny"), button -> {
            ConsentPolicy.decide(config, purpose, false, shownScope);
            config.save();
            close();
        }).dimensions(left, height - 44, panelWidth / 2 - 4, 20).build());
        ButtonWidget allow = addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.consent.allow"), button -> {
            ConsentPolicy.decide(config, purpose, true, shownScope);
            config.save();
            close();
        }).dimensions(left + panelWidth / 2 + 4, height - 44, panelWidth / 2 - 4, 20).build());
        allow.active = purpose != ConsentPolicy.Purpose.COMMUNITY_UPLOADS || ConsentPolicy.COMMUNITY_UPLOADS_AVAILABLE;
    }

    @Override public void close() { if (client != null) client.setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF0202020);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
