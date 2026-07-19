package de.ottoextra.chat;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpChatSyntaxTest {
    @Test
    void nestedOocInsideEmoteRestoresEmoteState() {
        RpChatSyntax.State state = RpChatSyntax.State.normal();
        state = RpChatSyntax.scan("*blickt (kurz (OOC)) wieder auf*", state);

        assertFalse(state.emote());
        assertEquals(0, state.oocDepth());
    }

    @Test
    void nonRpSplitDoesNotInventClosersOrOpeners() {
        List<String> parts = LongChatSender.split(
                "Offtopic (mit absichtlich offener Klammer und sehr viel Text dahinter",
                28, " >", false);

        assertTrue(parts.size() > 1);
        assertTrue(parts.getFirst().endsWith(" >"));
        assertFalse(parts.get(1).startsWith("("));
    }

    @Test
    void formatterRecognizesOnlyRpChannelBody() {
        String rp = "[Sprechen] Mönch Matthias: Text *Emote*";
        int rpBody = RpChatFormatter.findBodyStart(rp);
        assertTrue(rpBody > 0);
        assertTrue(RpChatFormatter.isRpChannelLine(rp, rpBody));

        String offTopic = "[Offtopic] Matthias: Text *unverändert*";
        int offTopicBody = RpChatFormatter.findBodyStart(offTopic);
        assertTrue(offTopicBody > 0);
        assertFalse(RpChatFormatter.isRpChannelLine(offTopic, offTopicBody));
    }

    @Test
    void chatAddonPresentationColorsAreOwnedByOttoExtra() {
        Text accent = RpChatFormatter.stylePresentation(
                Text.literal("Original"), RpChatFormatter.ROLE_ACCENT);
        Style muted = RpChatFormatter.presentationStyle(
                Style.EMPTY, RpChatFormatter.ROLE_MUTED);

        assertEquals(0xFFAA00, accent.getStyle().getColor().getRgb());
        assertEquals(0xAAAAAA, muted.getColor().getRgb());
        assertEquals(0xFF55FF55,
                RpChatFormatter.presentationArgb(RpChatFormatter.ROLE_SUCCESS));
    }
}
