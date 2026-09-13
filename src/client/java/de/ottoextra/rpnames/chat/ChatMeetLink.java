package de.ottoextra.rpnames.chat;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.regex.Pattern;

/** Keeps the real account attached to our unknown-name span without showing it in the tooltip. */
public final class ChatMeetLink {
    private static final String HINT = "ottoextra.meet.chat.hint";
    private static final Pattern ACCOUNT = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private ChatMeetLink() { }

    public static Style decorate(Style style, String account, String visibleName) {
        if (account == null || !ACCOUNT.matcher(account).matches()) return style;
        return style.withInsertion(account).withHoverEvent(new HoverEvent.ShowText(
                Text.translatable(HINT, visibleName)));
    }

    public static String account(Style style) {
        if (style == null || !(style.getHoverEvent() instanceof HoverEvent.ShowText hover)
                || !(hover.value().getContent() instanceof TranslatableTextContent content)
                || !HINT.equals(content.getKey())) return null;
        String account = style.getInsertion();
        return account != null && ACCOUNT.matcher(account).matches() ? account : null;
    }
}
