package de.ottoextra.config;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.settings.OttoExtraSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Kleiner Icon-Button (Mod-Logo) unten links im Pause-Menü — öffnet die
 * OttoExtra-Einstellungen direkt. Wird per {@code ScreenEvents.AFTER_INIT}
 * eingehängt (siehe {@code OttoExtraClient}), kein Mixin nötig.
 *
 * <p>{@link PressableWidget} zeichnet den Button-Hintergrund selbst
 * ({@code renderWidget} ist final); wir liefern nur das Icon via
 * {@link #drawIcon}.</p>
 */
public final class OttoExtraMenuButton extends PressableWidget {

    /** Fabric-Mod-Icon ({@code assets/ottoextra/icon.png}), 128×128. */
    private static final Identifier ICON = OttoExtra.id("icon.png");
    private static final int ICON_SIZE = 24;
    private static final int ICON_TEX = 128;

    private final Screen parent;

    public OttoExtraMenuButton(int x, int y, int size, Screen parent) {
        super(x, y, size, size, Text.empty());
        this.parent = parent;
        setTooltip(Tooltip.of(Text.translatable("ottoextra.menu.settings")));
    }

    @Override
    public void onPress(AbstractInput input) {
        MinecraftClient.getInstance().setScreen(new OttoExtraSettingsScreen(parent));
    }

    @Override
    public void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int ix = getX() + (getWidth() - ICON_SIZE) / 2;
        int iy = getY() + (getHeight() - ICON_SIZE) / 2;
        // Volle 128er-Textur in 16px skalieren (regionGröße = textureGröße = 128).
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICON, ix, iy, 0f, 0f,
                ICON_SIZE, ICON_SIZE, ICON_TEX, ICON_TEX, ICON_TEX, ICON_TEX);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
