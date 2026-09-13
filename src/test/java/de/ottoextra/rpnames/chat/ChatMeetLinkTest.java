package de.ottoextra.rpnames.chat;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMeetLinkTest {
    @Test void unknownSpanRetainsTheAccountWithoutExposingTheOldIdentityTooltip() {
        var oldHover = new HoverEvent.ShowText(Text.literal("Freiherr Alrik"));
        var click = new ClickEvent.SuggestCommand("/msg PlayerOne ");
        var original = Style.EMPTY.withColor(0xAABBCC).withHoverEvent(oldHover).withClickEvent(click);
        var marked = ChatMeetLink.decorate(original, "PlayerOne", "Unbekannt");
        assertEquals("PlayerOne", ChatMeetLink.account(marked));
        assertEquals(original.getColor(), marked.getColor());
        assertSame(click, marked.getClickEvent());
        assertNotEquals(oldHover, marked.getHoverEvent());
        var content = (TranslatableTextContent) ((HoverEvent.ShowText) marked.getHoverEvent()).value().getContent();
        assertArrayEquals(new Object[]{"Unbekannt"}, content.getArgs());
        assertSame(oldHover, original.getHoverEvent());
    }

    @Test void arbitraryChatInsertionsAndMatchingLiteralTooltipsAreNotMeetLinks() {
        assertNull(ChatMeetLink.account(Style.EMPTY.withInsertion("PlayerOne")));
        assertNull(ChatMeetLink.account(Style.EMPTY.withInsertion("PlayerOne").withHoverEvent(
                new HoverEvent.ShowText(Text.literal("ottoextra.meet.chat.hint")))));
        assertNull(ChatMeetLink.account(null));
    }

    @Test void customUnknownPlaceholderKeepsTheSameIdentity() {
        var style = ChatMeetLink.decorate(Style.EMPTY, "PlayerTwo", "???");
        assertEquals("PlayerTwo", ChatMeetLink.account(style));
        var content = (TranslatableTextContent) ((HoverEvent.ShowText) style.getHoverEvent()).value().getContent();
        assertArrayEquals(new Object[]{"???"}, content.getArgs());
    }

    @Test void invalidAccountCannotBecomeAnInteractiveIdentity() {
        for (String account : new String[]{null, "", "xy", "/msg Other", "Player\nOther", "MoreThan16LettersHere"}) {
            assertSame(Style.EMPTY, ChatMeetLink.decorate(Style.EMPTY, account, "Unbekannt"));
        }
        var valid = ChatMeetLink.decorate(Style.EMPTY, "PlayerOne", "Unbekannt");
        assertNull(ChatMeetLink.account(valid.withInsertion("/some command")));
    }
}
