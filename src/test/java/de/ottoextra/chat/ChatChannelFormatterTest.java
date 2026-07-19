package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
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
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void emptyOverridesPreserveTheServerTextUntouched() {
        Text original = Text.literal("[Hilfe] Matthias: Kann jemand helfen?")
                .formatted(Formatting.YELLOW);

        assertSame(original, ChatChannelFormatter.format(original));
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
