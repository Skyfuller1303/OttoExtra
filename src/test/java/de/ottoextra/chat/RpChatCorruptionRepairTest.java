package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.object.PlayerTextObjectContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RpChatCorruptionRepairTest {
    private static final String HEADER = "[Sprechen] Ritter Tobias \"Der Würfelmeister\": ";
    private static final String CORE = "*guckt auf matthias seinen Bart* Zufall? Ich denke nicht";
    private static final String CLEAN = "( " + CORE + " )";
    private static final String BROKEN = CLEAN + CORE + " )";
    private static final MessageSignatureData SIGNATURE = new MessageSignatureData(new byte[256]);
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
    void screenshotDuplicateIsRemovedOnlyWithMatchingOriginal() {
        for (String signed : List.of(CLEAN, "(( " + CORE + " ))", "((" + CORE + "))")) {
            Text original = Text.literal(HEADER + BROKEN);
            Text repaired = repair(original, signed);
            assertEquals(HEADER + CLEAN, repaired.getString(), signed);
            assertEquals(HEADER + BROKEN, original.getString(), "Received text must remain intact");
            assertSame(repaired, repair(repaired, signed), "Repair must be idempotent");
        }
    }

    @Test
    void repairsShortAndMultipleNestedEmotesWithOriginalProof() {
        for (String core : List.of("*test*", "erstes *Emote* und zweites *Emote*",
                "*blickt (kurz) auf* hahaha?? 22 😀")) {
            String clean = "(" + core + ")";
            Text original = Text.literal(HEADER + clean + clean.substring(clean.indexOf('*')));
            assertEquals(HEADER + clean, repair(original, "(" + clean + ")").getString());
        }
    }

    @Test
    void keepsIntentionalRepetitionsIncludingMalformedOocIfTheyAreTheOriginal() {
        for (String body : List.of("*nickt*??", "*lacht* hahaha", "(1+1)=22",
                BROKEN, "(*test*)*test*)", "(*winkt* *winkt*)", "(*test*)(*test*)")) {
            Text original = Text.literal(HEADER + body).styled(style -> style.withBold(true));
            assertSame(original, repair(original, body), body);
        }
    }

    @Test
    void doesNotRestoreTranslatedFilteredOrUnrelatedOriginals() {
        Text original = Text.literal(HEADER + BROKEN);
        for (String signed : List.of("(*looks at his beard* Coincidence?)", "(gefiltert)",
                "(*guckt auf matthias seinen Bart* Zufall? Ich denke schon)", CORE)) {
            assertSame(original, repair(original, signed), signed);
        }
        Text partialDuplicate = Text.literal(HEADER + CLEAN + "*guckt auf matthias seinen Bart*)");
        assertSame(partialDuplicate, repair(partialDuplicate, CLEAN));
    }

    @Test
    void requiresSignatureAndVanillaOriginalMetadata() {
        Text original = Text.literal(HEADER + BROKEN);
        assertSame(original, RpChatCorruptionRepair.repair(original, null, MessageIndicator.modified(CLEAN)));
        assertSame(original, RpChatCorruptionRepair.repair(original, SIGNATURE, null));
        assertSame(original, RpChatCorruptionRepair.repair(original, SIGNATURE, MessageIndicator.system()));
        assertSame(original, RpChatCorruptionRepair.repair(original, SIGNATURE, MessageIndicator.notSecure()));
        MessageIndicator custom = new MessageIndicator(0, MessageIndicator.Icon.CHAT_MODIFIED,
                Text.literal("Custom tooltip\n" + CLEAN), "Modified");
        assertSame(original, RpChatCorruptionRepair.repair(original, SIGNATURE, custom));
    }

    @Test
    void leavesOtherChannelsAndDisabledFormattingUntouched() {
        for (String header : List.of("[Offtopic] Tobias: ", "[Hilfe] Tobias: ", "<Tobias> ",
                "System: ", "[Unbekannter Kanal] Tobias: ")) {
            Text original = Text.literal(header + BROKEN);
            assertSame(original, repair(original, CLEAN));
        }
        Text original = Text.literal(HEADER + BROKEN);
        config.chat.rpFormattingEnabled = false;
        assertSame(original, repair(original, CLEAN));
        config.chat.rpFormattingEnabled = true;
        config.chat.enabled = false;
        assertSame(original, repair(original, CLEAN));
    }

    @Test
    void preservesStructuredHeadHoverAndStyledBodyAcrossComponentBoundaries() {
        Text head = MutableText.of(new ObjectTextContent(new PlayerTextObjectContents(
                ProfileComponent.ofDynamic("TestTobias"), true)));
        Style nameStyle = Style.EMPTY.withColor(0xD6B16E)
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Spielerprofil")))
                .withInsertion("TestTobias");
        Style bodyStyle = Style.EMPTY.withColor(0xABABAB).withItalic(true);
        for (int split = 0; split <= BROKEN.length(); split++) {
            Text original = Text.translatable("%s", Text.literal("[Sprechen] ").append(head)
                    .append(Text.literal(" Tobias: ").setStyle(nameStyle))
                    .append(Text.literal(BROKEN.substring(0, split)).setStyle(bodyStyle))
                    .append(Text.translatable("%s", Text.literal(BROKEN.substring(split))
                            .setStyle(bodyStyle))));
            Text repaired = repair(original, CLEAN);
            assertEquals("[Sprechen] " + head.getString() + " Tobias: " + CLEAN, repaired.getString());
            assertEquals("TestTobias", ChatSpeakerClassifier.speakerComponentAccount(repaired));
            assertEquals(nameStyle, styleFor(repaired, "Tobias"));
            assertEquals(bodyStyle, styleFor(repaired, "guckt"));
            assertTrue(original.getString().endsWith(BROKEN));
        }
    }

    @Test
    void incomingAndHistoryRepairBeforeStylingWithoutChangingOriginalMetadata() {
        config.chat.rpEmoteColor = "#123456";
        config.chat.rpOocColor = "#654321";
        Text original = Text.literal(HEADER + BROKEN);
        MessageIndicator indicator = MessageIndicator.modified("(( " + CORE + " ))");
        Text incoming = ChatMessagePipeline.formatIncoming(original, SIGNATURE, indicator);
        Text history = ChatMessagePipeline.formatHistory(original, SIGNATURE, indicator);
        assertEquals(HEADER + CLEAN, incoming.getString());
        assertEquals(incoming, history);
        assertEquals(0x123456, styleFor(incoming, "guckt").getColor().getRgb());
        assertTrue(styleFor(incoming, "guckt").isItalic());
        assertEquals(0x654321, styleFor(incoming, "Zufall").getColor().getRgb());
        assertFalse(styleFor(incoming, "Zufall").isItalic());
        assertEquals(HEADER + BROKEN, original.getString());
        assertTrue(indicator.text().getString().endsWith("(( " + CORE + " ))"));
    }

    private static Text repair(Text message, String signedContent) {
        return RpChatCorruptionRepair.repair(message, SIGNATURE, MessageIndicator.modified(signedContent));
    }

    private static Style styleFor(Text text, String needle) {
        int target = text.getString().indexOf(needle);
        assertTrue(target >= 0, needle);
        int[] position = {0};
        return text.visit((style, part) -> {
            int start = position[0];
            position[0] += part.length();
            return target >= start && target < position[0] ? Optional.of(style) : Optional.empty();
        }, Style.EMPTY).orElseThrow();
    }
}
