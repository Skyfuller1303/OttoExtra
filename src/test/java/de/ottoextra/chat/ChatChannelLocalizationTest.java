package de.ottoextra.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatChannelLocalizationTest {
    @Test
    void everyServerLabelHasIndependentTranslationKey() {
        assertTrue(Arrays.stream(ChatChannelState.ChatChannel.values())
                .allMatch(channel -> channel.translationKey.startsWith("ottoextra.chat.channel.")));
        assertEquals("[Flüstern]", ChatChannelState.ChatChannel.FLUESTERN.label);
        assertEquals("ottoextra.chat.channel.fluestern",
                ChatChannelState.ChatChannel.FLUESTERN.translationKey);
    }
}
