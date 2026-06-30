package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Rendering + Geometrie des Kanal-Buttons links unten im Chat-Screen
 *: wirkt wie ein Prefix vor dem
 * Eingabefeld, ist aber reines UI. RP-Kanäle warm (Pergament/Gold),
 * OOC kühl (Blau), Hover aufgehellt.
 */
public final class ChatChannelButton {

    public static final int MARGIN_LEFT = 4; // wie der Vanilla-Textstart im Chatfeld
    public static final int GAP = 4;
    private static final int HEIGHT = 12;

    // Kanalfarbe je Kanal (siehe channelColor), Hover aufgehellt; Offtopic-Marker
    // "!!!" flasht hart zwischen #c6505e und #d1bf59 (Signal: öffentlicher Kanal!)
    private static final int BANG_COLOR_A = 0xFFC6505E;
    private static final int BANG_COLOR_B = 0xFFD1BF59;
    private static final long BANG_FLASH_MS = 500;

    private ChatChannelButton() {
    }

    /** Textbreite des aktuellen Kanal-Prefixes (inkl. Offtopic-"!!!"). */
    public static int width(MinecraftClient client) {
        var channel = ChatChannelState.current();
        int w = client.textRenderer.getWidth(channel.label);
        String bang = bangText();
        if (channel == ChatChannelState.ChatChannel.OFFTOPIC && !bang.isEmpty()) {
            w += client.textRenderer.getWidth(bang);
        }
        return w;
    }

    /** "!"-Marker je Config (deaktivierbar, Anzahl 1-3). */
    private static String bangText() {
        var cfg = ChatChannelState.chatConfig();
        if (cfg == null || !cfg.offtopicBangEnabled) {
            return "";
        }
        int count = Math.max(1, Math.min(3, cfg.offtopicBangCount));
        return "!".repeat(count);
    }

    public static int x() {
        return MARGIN_LEFT;
    }

    public static int y(int screenHeight) {
        return screenHeight - 14; // Klickfläche über die Chatzeilen-Höhe
    }

    public static boolean contains(MinecraftClient client, int screenHeight, double mx, double my) {
        int x = x();
        int y = y(screenHeight);
        return mx >= x && mx <= x + width(client) && my >= y && my <= y + HEIGHT + 2;
    }

    /** Prefix als reiner Text in der Eingabezeile (Hintergrund liefert der Chat). */
    public static void render(DrawContext ctx, MinecraftClient client, int screenHeight,
                              int mouseX, int mouseY) {
        var channel = ChatChannelState.current();
        boolean hovered = contains(client, screenHeight, mouseX, mouseY);
        int base = channelColor(channel);
        int color = hovered ? brighten(base) : base;
        int x = x();
        int y = screenHeight - 12;
        String bang = bangText();
        if (channel == ChatChannelState.ChatChannel.OFFTOPIC && !bang.isEmpty()) {
            ctx.drawText(client.textRenderer, bang, x, y, bangColor(), true);
            x += client.textRenderer.getWidth(bang);
        }
        ctx.drawText(client.textRenderer, channel.label, x, y, color, true);
    }

    /** Grundfarbe des Kanal-Prefix je Kanal. */
    private static int channelColor(ChatChannelState.ChatChannel channel) {
        return switch (channel) {
            case SPRECHEN -> 0xFFDFC8A7;
            case FLUESTERN -> 0xFF768491;
            case MURMELN -> 0xFF58666F;
            case RUFEN -> 0xFFD2BF6A;
            case BRUELLEN -> 0xFFFCF47E;
            case OFFTOPIC -> 0xFFB4BEC6;
            case HILFE -> 0xFFB53764;
        };
    }

    /** Hover: jeden Kanal um +0x28 pro Kanal aufhellen (geklemmt). */
    private static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 0x28);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 0x28);
        int b = Math.min(255, (argb & 0xFF) + 0x28);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Harter Flash zwischen den beiden Signal-Farben (kein Fade). */
    private static int bangColor() {
        boolean a = (System.currentTimeMillis() / BANG_FLASH_MS) % 2 == 0;
        return a ? BANG_COLOR_A : BANG_COLOR_B;
    }
}
