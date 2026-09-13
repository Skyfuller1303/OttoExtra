package de.ottoextra.chat;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatP2RegressionTest {
    @Test
    void deepNestingMakesProgressAndNeverReturnsAnOversizedRemainder() {
        String message = "(".repeat(300);
        List<String> parts = LongChatSplitter.split(message, 256, " >");
        assertTrue(parts.size() <= 3, "parts=" + parts.size());
        assertTrue(parts.stream().allMatch(part -> part.length() <= 256));
        assertEquals(message, String.join("", parts).replace(" >", ""));
    }

    @Test
    void emojiAtChunkBoundariesAndInsideLongMarkersStaysIntact() {
        for (boolean rp : List.of(true, false)) {
            for (int prefix = 251; prefix <= 256; prefix++) {
                String message = "x".repeat(prefix) + "😀" + "abc";
                List<String> parts = LongChatSplitter.split(message, 256, " >", rp);
                assertEquals(message, String.join("", parts).replace(" >", ""));
                parts.forEach(part -> assertValidUnicode(part, 256));
            }
            for (String marker : List.of("x".repeat(6) + "😀", "x".repeat(5) + "😀x")) {
                LongChatSplitter.split("😀".repeat(12), 8, marker, rp)
                        .forEach(part -> assertValidUnicode(part, 8));
            }
        }
    }

    @Test
    void legitimateRepeatedSuffixesPreserveTextAndStyle() {
        for (String body : List.of("*nickt*??", "*lacht* hahaha", "(1+1)=22")) {
            Text message = Text.literal("[RP] Alice: ").append(Text.literal(body).styled(s -> s.withBold(true)));
            assertSame(message, RpChatCorruptionRepair.repair(message, null, null));
        }
    }

    private static void assertValidUnicode(String value, int limit) {
        assertTrue(value.length() <= limit);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                assertTrue(++i < value.length() && Character.isLowSurrogate(value.charAt(i)));
            } else {
                assertFalse(Character.isLowSurrogate(ch));
            }
        }
    }
}
