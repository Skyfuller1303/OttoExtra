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
    void emoteInsideOocUsesEmoteStyleThenReturnsToOoc() {
        RpChatSyntax.State ooc = RpChatSyntax.scan("((OOC ");
        RpChatSyntax.Step openingEmote = RpChatSyntax.step(ooc, '*');
        RpChatSyntax.Step emoteText = RpChatSyntax.step(openingEmote.after(), 'E');
        RpChatSyntax.Step closingEmote = RpChatSyntax.step(emoteText.after(), '*');
        RpChatSyntax.Step resumedOoc = RpChatSyntax.step(closingEmote.after(), ' ');

        assertEquals(RpChatSyntax.Kind.EMOTE, openingEmote.kind());
        assertEquals(RpChatSyntax.Kind.EMOTE, emoteText.kind());
        assertEquals(RpChatSyntax.Kind.EMOTE, closingEmote.kind());
        assertEquals(RpChatSyntax.Kind.OOC, resumedOoc.kind());
    }

    @Test
    void oocInsideEmoteUsesOocStyleThenReturnsToEmote() {
        RpChatSyntax.State emote = RpChatSyntax.scan("*Emote ");
        RpChatSyntax.Step openingOoc = RpChatSyntax.step(emote, '(');
        RpChatSyntax.Step oocText = RpChatSyntax.step(openingOoc.after(), 'O');
        RpChatSyntax.Step closingOoc = RpChatSyntax.step(oocText.after(), ')');
        RpChatSyntax.Step resumedEmote = RpChatSyntax.step(closingOoc.after(), ' ');

        assertEquals(RpChatSyntax.Kind.OOC, openingOoc.kind());
        assertEquals(RpChatSyntax.Kind.OOC, oocText.kind());
        assertEquals(RpChatSyntax.Kind.OOC, closingOoc.kind());
        assertEquals(RpChatSyntax.Kind.EMOTE, resumedEmote.kind());
    }

    @Test
    void emoteInsideDoubleOocNeverChangesVisibleText() {
        Text original = Text.empty()
                .append(Text.literal("(( ja sollte gehen "))
                .append(Text.literal("*nicht*").styled(style -> style.withItalic(true)))
                .append(Text.literal(" ))"));

        Text formatted = RpChatFormatter.formatContent(original);

        assertEquals("(( ja sollte gehen *nicht* ))", formatted.getString());
        assertEquals(original.getString(), formatted.getString());
    }

    @Test
    void styleHardeningRejectsDuplicatedNestedSegment() {
        Text original = Text.literal("(( ja sollte gehen *nicht* ))");
        Text corrupted = Text.literal("( ja sollte gehen *nicht* )*nicht* )");

        Text result = RpChatFormatter.keepOnlyIfTextPreserved(original, corrupted);

        assertTrue(result == original);
        assertEquals("(( ja sollte gehen *nicht* ))", result.getString());
    }

    @Test
    void nameRewriteHardeningAllowsHeaderChangeButRejectsBodyCorruption() {
        Text original = Text.literal(
                "[Sprechen] Matthias: (( ja sollte gehen *nicht* ))");
        Text safeRename = Text.literal(
                "[Sprechen] Ordenspriester Matthias: (( ja sollte gehen *nicht* ))");
        Text corrupted = Text.literal(
                "[Sprechen] Ordenspriester Matthias: ( ja sollte gehen *nicht* )*nicht* )");

        assertTrue(ChatMessagePipeline.keepOnlyIfBodyPreserved(original, safeRename)
                == safeRename);
        assertTrue(ChatMessagePipeline.keepOnlyIfBodyPreserved(original, corrupted)
                == original);
    }

    @Test
    void preservesAmbiguousNestedEmoteWithoutProtocolProof() {
        Text corrupted = Text.literal(
                "[Sprechen] Ordenspriester Matthias: (dann schauen wir ob ooc "
                        + "*emotes erkannt werden*)*emotes erkannt werden*)");

        Text repaired = RpChatCorruptionRepair.repair(corrupted, null, null);

        assertTrue(repaired == corrupted);
    }

    @Test
    void preservesAmbiguousShortSuffixAndNormalOoc() {
        Text corrupted = Text.literal("[Sprechen] Prior Tabris: (*test*)*test*)");
        Text normal = Text.literal("[Sprechen] Prior Tabris: ((*test*))");

        assertTrue(RpChatCorruptionRepair.repair(corrupted, null, null) == corrupted);
        assertTrue(RpChatCorruptionRepair.repair(normal, null, null) == normal);
    }

    @Test
    void preservesAmbiguousMultipleEmotesAndInverseNesting() {
        Text multiple = Text.literal("[Sprechen] Matthias: "
                + "(erstes *Emote* und zweites *Emote*)*Emote* und zweites *Emote*)");
        Text inverse = Text.literal("[Sprechen] Matthias: "
                + "*Emote mit (OOC) darin*(OOC) darin*");

        assertTrue(RpChatCorruptionRepair.repair(multiple, null, null) == multiple);
        assertTrue(RpChatCorruptionRepair.repair(inverse, null, null) == inverse);
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
