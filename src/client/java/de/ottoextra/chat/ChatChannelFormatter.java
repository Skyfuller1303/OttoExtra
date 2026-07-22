package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.chat.OttoChatChannel;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;

/** Wendet die lokal konfigurierten Farben eines Chatkanals auf Chatzeilen an. */
public final class ChatChannelFormatter {

    private static final int NPC_NAME_FALLBACK = 0xC7A87F;
    private static final int NPC_MESSAGE_FALLBACK = 0xDFC8A7;

    private ChatChannelFormatter() {
    }

    public static Text format(Text message) {
        return format(message, ChatSpeakerClassifier.SpeakerKind.UNKNOWN);
    }

    public static Text format(Text message, ChatSpeakerClassifier.SpeakerKind speakerKind) {
        if (message == null) return null;
        try {
            OttoExtraConfig.Chat config = OttoExtraConfig.active().chat;
            if (config == null || !config.enabled) return message;

            String plain = TextVisitFactory.removeFormattingCodes(message);
            int bodyStart = RpChatFormatter.findBodyStart(plain);
            ChatChannelState.ChatChannel channel = channelForLine(plain, bodyStart);
            OttoChatChannel rpChannel = OttoChatChannel.fromMessage(plain);
            boolean npc = speakerKind == ChatSpeakerClassifier.SpeakerKind.NPC
                    && ((channel != null && channel.rp) || rpChannel.isRpSpeak());
            if (channel == null && !npc) return message;

            String colorKey = channel != null ? key(channel) : key(rpChannel);
            OttoExtraConfig.ChannelColors colors = config.channelColors(colorKey);
            Integer labelColor = parseOptionalColor(colors.labelColor);
            Integer messageColor = parseOptionalColor(colors.messageColor);
            Integer speakerColor = null;
            if (npc) {
                speakerColor = parseOptionalColor(config.npcNameColor);
                if (speakerColor == null) speakerColor = NPC_NAME_FALLBACK;
                messageColor = parseOptionalColor(config.npcMessageColor);
                if (messageColor == null) messageColor = NPC_MESSAGE_FALLBACK;
            }
            if (labelColor == null && messageColor == null && speakerColor == null) return message;

            int channelEnd = plain.indexOf(']') + 1;
            int labelStart = channel != null ? plain.indexOf(channel.label)
                    : channelEnd > 0 ? 0 : -1;
            int labelEnd = channel != null && labelStart >= 0
                    ? labelStart + channel.label.length()
                    : channelEnd > 0 ? channelEnd : -1;
            int separator = separatorBeforeBody(plain, bodyStart);
            int speakerStart = npc ? skipWhitespace(plain, channelEnd) : -1;
            int speakerEnd = npc ? trimWhitespaceBefore(plain, separator) : -1;
            if (npc && (channelEnd <= 0 || separator < 0 || speakerStart >= speakerEnd)) {
                return message;
            }

            return rebuild(message, labelStart, labelEnd,
                    speakerStart, speakerEnd, bodyStart,
                    labelColor, speakerColor, messageColor);
        } catch (Throwable ignored) {
            return message;
        }
    }

    static ChatChannelState.ChatChannel channelForLine(String plain, int bodyStart) {
        if (plain == null || bodyStart <= 0) return null;
        String header = plain.substring(0, Math.min(bodyStart, plain.length()));
        for (ChatChannelState.ChatChannel channel : ChatChannelState.ChatChannel.values()) {
            if (header.contains(channel.label)) return channel;
        }
        return null;
    }

    public static int configuredLabelColor(ChatChannelState.ChatChannel channel, int fallback) {
        try {
            String value = OttoExtraConfig.active().chat.channelColors(key(channel)).labelColor;
            Integer parsed = parseOptionalColor(value);
            return parsed == null ? fallback : (0xFF000000 | parsed);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Text rebuild(Text message,
                                int labelStart, int labelEnd,
                                int speakerStart, int speakerEnd, int bodyStart,
                                Integer labelColor, Integer speakerColor, Integer messageColor) {
        MutableText out = Text.empty();
        StringBuilder run = new StringBuilder();
        Style[] runStyle = {null};
        int[] flatPos = {0};
        boolean[] visited = {false};

        TextVisitFactory.visitFormatted(message, Style.EMPTY, (index, originalStyle, codePoint) -> {
            Integer color = colorAt(flatPos[0], labelStart, labelEnd,
                    speakerStart, speakerEnd, bodyStart,
                    labelColor, speakerColor, messageColor);
            Style style = safeStyle(originalStyle);
            if (color != null) {
                style = style.withColor(color);
            }
            if (runStyle[0] != null && !runStyle[0].equals(style)) {
                append(out, run, runStyle[0]);
            }
            runStyle[0] = style;
            run.appendCodePoint(codePoint);
            flatPos[0] += Character.charCount(codePoint);
            visited[0] = true;
            return true;
        });
        append(out, run, runStyle[0]);
        return visited[0] ? out : message;
    }

    private static Integer colorAt(int position,
                                   int labelStart, int labelEnd,
                                   int speakerStart, int speakerEnd, int bodyStart,
                                   Integer labelColor, Integer speakerColor,
                                   Integer messageColor) {
        if (labelColor != null && position >= labelStart && position < labelEnd) {
            return labelColor;
        }
        if (speakerColor != null && position >= speakerStart && position < speakerEnd) {
            return speakerColor;
        }
        return messageColor != null && position >= bodyStart ? messageColor : null;
    }

    private static void append(MutableText out, StringBuilder run, Style style) {
        if (run.isEmpty()) return;
        out.append(Text.literal(run.toString()).setStyle(safeStyle(style)));
        run.setLength(0);
    }

    private static int separatorBeforeBody(String plain, int bodyStart) {
        if (plain == null || bodyStart <= 0 || bodyStart > plain.length()) return -1;
        int cursor = bodyStart - 1;
        while (cursor >= 0 && Character.isWhitespace(plain.charAt(cursor))) cursor--;
        return cursor >= 0 && plain.charAt(cursor) == ':' ? cursor : -1;
    }

    private static int skipWhitespace(String plain, int index) {
        int cursor = Math.max(0, index);
        while (cursor < plain.length() && Character.isWhitespace(plain.charAt(cursor))) cursor++;
        return cursor;
    }

    private static int trimWhitespaceBefore(String plain, int index) {
        int cursor = Math.min(index, plain.length());
        while (cursor > 0 && Character.isWhitespace(plain.charAt(cursor - 1))) cursor--;
        return cursor;
    }

    private static String key(ChatChannelState.ChatChannel channel) {
        return switch (channel) {
            case SPRECHEN -> "sprechen";
            case FLUESTERN -> "fluestern";
            case MURMELN -> "murmeln";
            case RUFEN -> "rufen";
            case BRUELLEN -> "bruellen";
            case OFFTOPIC -> "offtopic";
            case HILFE -> "hilfe";
        };
    }

    private static String key(OttoChatChannel channel) {
        return switch (channel) {
            case SPRECHEN, REDEN -> "sprechen";
            case FLUESTERN -> "fluestern";
            case MURMELN -> "murmeln";
            case RUFEN -> "rufen";
            case BRUELLEN -> "bruellen";
            default -> "sprechen";
        };
    }

    private static Integer parseOptionalColor(String value) {
        if (value == null || value.isBlank()) return null;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("[0-9a-fA-F]{6}")) return null;
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Style safeStyle(Style style) {
        return style == null ? Style.EMPTY : style;
    }
}
