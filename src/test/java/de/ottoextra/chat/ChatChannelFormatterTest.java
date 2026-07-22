package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.chat.ChatSpeakerClassifier.SpeakerKind;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatChannelFormatterTest {
    private Field activeField;
    private Object previous;
    private OttoExtraConfig config;

    @BeforeEach
    void setUp() throws Exception {
        activeField = OttoExtraConfig.class.getDeclaredField("active");
        activeField.setAccessible(true);
        previous = activeField.get(null);
        config = new OttoExtraConfig();
        activeField.set(null, config);
    }

    @AfterEach
    void tearDown() throws Exception {
        activeField.set(null, previous);
    }

    @Test
    void colorsChannelLabelAndBodyButPreservesSpeakerStyle() {
        var colors = config.chat.channelColors("fluestern");
        colors.labelColor = "#123456";
        colors.messageColor = "#654321";
        Text original = Text.literal("[Flüstern] Mönch Matthias: leise Worte")
                .formatted(Formatting.GRAY);

        Text formatted = ChatChannelFormatter.format(original);

        assertEquals(0x123456, styleFor(formatted, "[Flüstern]").getColor().getRgb());
        assertEquals(Formatting.GRAY.getColorValue(),
                styleFor(formatted, "Mönch Matthias").getColor().getRgb());
        assertEquals(0x654321, styleFor(formatted, "leise Worte").getColor().getRgb());
    }

    @Test
    void channelDefaultsColorBodyButPreserveServerLabelAndSpeaker() {
        Text original = Text.literal("[Hilfe] Matthias: Kann jemand helfen?")
                .formatted(Formatting.YELLOW);

        Text formatted = ChatChannelFormatter.format(original);

        assertEquals(Formatting.YELLOW.getColorValue(),
                styleFor(formatted, "[Hilfe]").getColor().getRgb());
        assertEquals(Formatting.YELLOW.getColorValue(),
                styleFor(formatted, "Matthias").getColor().getRgb());
        assertEquals(0xB53764,
                styleFor(formatted, "Kann jemand helfen?").getColor().getRgb());
    }

    @Test
    void npcSpeakerAndBodyReceiveReadableRpColors() {
        Style originalStyle = Style.EMPTY
                .withColor(Formatting.DARK_GRAY)
                .withBold(true)
                .withInsertion("npc-speaker")
                .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(Text.literal("NPC")));
        Text original = Text.literal("[Sprechen] Bote Hinz: Eine Nachricht")
                .setStyle(originalStyle);

        Text formatted = ChatChannelFormatter.format(original, SpeakerKind.NPC);

        Style channel = styleFor(formatted, "[Sprechen]");
        Style speaker = styleFor(formatted, "Bote Hinz");
        Style body = styleFor(formatted, "Eine Nachricht");
        assertEquals(Formatting.DARK_GRAY.getColorValue(), channel.getColor().getRgb());
        assertEquals(0xC7A87F, speaker.getColor().getRgb());
        assertEquals(0xDFC8A7, body.getColor().getRgb());
        assertTrue(speaker.isBold());
        assertEquals("npc-speaker", speaker.getInsertion());
        assertEquals(originalStyle.getHoverEvent(), speaker.getHoverEvent());
    }

    @Test
    void npcMessageColorOverridesChannelBodyColor() {
        config.chat.channelColors("sprechen").messageColor = "#123456";
        config.chat.npcMessageColor = "#654321";
        Text original = Text.literal("[Sprechen] Bote Hinz: Eine Nachricht")
                .formatted(Formatting.GRAY);

        Text formatted = ChatChannelFormatter.format(original, SpeakerKind.NPC);

        assertEquals(0x654321, styleFor(formatted, "Eine Nachricht").getColor().getRgb());
    }

    @Test
    void redenNpcUsesSameGenericRpFallback() {
        Text original = Text.literal("[Reden] Krämerin Ada: Frische Ware")
                .formatted(Formatting.GRAY);

        Text formatted = ChatChannelFormatter.format(original, SpeakerKind.NPC);

        assertEquals(0xC7A87F, styleFor(formatted, "Krämerin Ada").getColor().getRgb());
        assertEquals(0xDFC8A7, styleFor(formatted, "Frische Ware").getColor().getRgb());
    }

    @Test
    void playerUnknownAndNonRpChannelsOnlyReceiveChannelBodyColor() {
        Text player = Text.literal("[Sprechen] Spieler: Hallo").formatted(Formatting.GRAY);
        Text unknown = Text.literal("[Sprechen] Unklar: Hallo").formatted(Formatting.GRAY);
        Text helpNpc = Text.literal("[Hilfe] Bote Hinz: Antwort").formatted(Formatting.GRAY);

        Text playerFormatted = ChatChannelFormatter.format(player, SpeakerKind.PLAYER);
        Text unknownFormatted = ChatChannelFormatter.format(unknown, SpeakerKind.UNKNOWN);
        Text helpFormatted = ChatChannelFormatter.format(helpNpc, SpeakerKind.NPC);

        assertEquals(Formatting.GRAY.getColorValue(),
                styleFor(playerFormatted, "Spieler").getColor().getRgb());
        assertEquals(Formatting.GRAY.getColorValue(),
                styleFor(unknownFormatted, "Unklar").getColor().getRgb());
        assertEquals(Formatting.GRAY.getColorValue(),
                styleFor(helpFormatted, "Bote Hinz").getColor().getRgb());
        assertEquals(0xDFC8A7, styleFor(playerFormatted, "Hallo").getColor().getRgb());
        assertEquals(0xDFC8A7, styleFor(unknownFormatted, "Hallo").getColor().getRgb());
        assertEquals(0xB53764, styleFor(helpFormatted, "Antwort").getColor().getRgb());
    }

    @Test
    void configuredNpcNameColorOverridesLegacyServerGray() {
        config.chat.npcNameColor = "#123456";
        Text original = Text.literal(
                "§8[Sprechen] §7Stallmeister Ermenoldus: §aBringe mir 32 Stroh.");

        Text formatted = ChatChannelFormatter.format(original, SpeakerKind.NPC);

        assertEquals("[Sprechen] Stallmeister Ermenoldus: Bringe mir 32 Stroh.",
                formatted.getString());
        assertEquals(Formatting.DARK_GRAY.getColorValue(),
                styleFor(formatted, "[Sprechen]").getColor().getRgb());
        assertEquals(0x123456,
                styleFor(formatted, "Stallmeister Ermenoldus").getColor().getRgb());
        assertEquals(0xDFC8A7,
                styleFor(formatted, "Bringe mir 32 Stroh.").getColor().getRgb());
    }

    @Test
    void arbitraryTranslatableNpcStructureReceivesConfiguredColor() {
        config.chat.npcNameColor = "#345678";
        Text original = Text.translatableWithFallback(
                "ottoextra.test.npc-line",
                "[Sprechen] %s: %s",
                Text.literal("Krämerin Ada").formatted(Formatting.DARK_GRAY),
                Text.literal("Frische Ware").formatted(Formatting.GRAY));

        Text formatted = ChatChannelFormatter.format(original, SpeakerKind.NPC);

        assertEquals(0x345678, styleFor(formatted, "Krämerin Ada").getColor().getRgb());
        assertEquals(0xDFC8A7, styleFor(formatted, "Frische Ware").getColor().getRgb());
    }

    @Test
    void incomingSystemNpcUsesSamePipelineAsHistory() {
        config.chat.npcNameColor = "#456789";
        Text original = Text.literal("§8[Sprechen] §7Stallmeister Ermenoldus: §aHallo");

        Text incoming = ChatMessagePipeline.formatIncoming(
                original, null, net.minecraft.client.gui.hud.MessageIndicator.system());
        Text history = ChatMessagePipeline.formatHistory(
                original, null, net.minecraft.client.gui.hud.MessageIndicator.system());

        assertEquals(0x456789,
                styleFor(incoming, "Stallmeister Ermenoldus").getColor().getRgb());
        assertEquals(0x456789,
                styleFor(history, "Stallmeister Ermenoldus").getColor().getRgb());
        assertEquals(0xDFC8A7, styleFor(incoming, "Hallo").getColor().getRgb());
        assertEquals(incoming, history);
    }

    @Test
    void emoteAndOocColorsOverrideNpcMessageColor() {
        config.chat.npcMessageColor = "#102030";
        config.chat.rpEmoteColor = "#405060";
        config.chat.rpOocColor = "#708090";
        Text original = Text.literal("[Sprechen] Bote Hinz: normal *emote* (ooc)")
                .formatted(Formatting.GRAY);

        Text formatted = RpChatFormatter.format(
                ChatChannelFormatter.format(original, SpeakerKind.NPC));

        assertEquals(0x102030, styleFor(formatted, "normal ").getColor().getRgb());
        assertEquals(0x405060, styleFor(formatted, "*emote*").getColor().getRgb());
        assertEquals(0x708090, styleFor(formatted, "(ooc)").getColor().getRgb());
    }

    @Test
    void emoteAndOocItalicsAreConfiguredIndependently() {
        config.chat.rpEmoteItalic = false;
        config.chat.rpOocItalic = true;

        Text formatted = RpChatFormatter.formatContent(
                Text.literal("*Emote* (OOC)"));

        assertFalse(styleFor(formatted, "Emote").isItalic());
        assertTrue(styleFor(formatted, "OOC").isItalic());
    }

    private static Style styleFor(Text text, String needle) {
        return text.visit((style, segment) -> segment.contains(needle)
                ? Optional.of(style) : Optional.empty(), Style.EMPTY).orElseThrow();
    }
}
