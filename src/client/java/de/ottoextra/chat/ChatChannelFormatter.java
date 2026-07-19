package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

import java.util.Optional;

/** Wendet die lokal konfigurierten Farben eines Chatkanals auf Chatzeilen an. */
public final class ChatChannelFormatter {

    private ChatChannelFormatter() {
    }

    public static Text format(Text message) {
        if (message == null) return null;
        try {
            OttoExtraConfig.Chat config = OttoExtraConfig.active().chat;
            if (config == null || !config.enabled) return message;

            String plain = message.getString();
            int bodyStart = RpChatFormatter.findBodyStart(plain);
            ChatChannelState.ChatChannel channel = channelForLine(plain, bodyStart);
            if (channel == null) return message;

            OttoExtraConfig.ChannelColors colors = config.channelColors(key(channel));
            Integer labelColor = parseOptionalColor(colors.labelColor);
            Integer messageColor = parseOptionalColor(colors.messageColor);
            if (labelColor == null && messageColor == null) return message;

            int labelStart = plain.indexOf(channel.label);
            int labelEnd = labelStart < 0 ? -1 : labelStart + channel.label.length();
            return rebuild(message, new int[]{0}, Style.EMPTY, labelStart, labelEnd,
                    bodyStart, labelColor, messageColor);
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

    private static Text rebuild(Text node, int[] flatPos, Style parentStyle,
                                int labelStart, int labelEnd, int bodyStart,
                                Integer labelColor, Integer messageColor) {
        TextContent content = node.getContent();
        Style effective = safeStyle(node.getStyle()).withParent(safeStyle(parentStyle));
        MutableText copy;

        if (content instanceof net.minecraft.text.TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            Object[] args = translated.getArgs();
            Object[] rewritten = new Object[args.length];
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                if (arg instanceof Text text) {
                    rewritten[index] = rebuild(text, flatPos, effective, labelStart, labelEnd,
                            bodyStart, labelColor, messageColor);
                } else {
                    rewritten[index] = arg;
                    if (arg != null) flatPos[0] += arg.toString().length();
                }
            }
            copy = MutableText.of(new net.minecraft.text.TranslatableTextContent(
                    translated.getKey(), translated.getFallback(), rewritten)).setStyle(effective);
        } else if (content instanceof PlainTextContent plain) {
            copy = colorLiteral(plain.string(), flatPos[0], effective, labelStart, labelEnd,
                    bodyStart, labelColor, messageColor);
            flatPos[0] += plain.string().length();
        } else {
            copy = MutableText.of(content).setStyle(effective);
            flatPos[0] += ownText(node).length();
        }

        for (Text sibling : node.getSiblings()) {
            copy.append(rebuild(sibling, flatPos, effective, labelStart, labelEnd,
                    bodyStart, labelColor, messageColor));
        }
        return copy;
    }

    private static MutableText colorLiteral(String value, int globalStart, Style base,
                                            int labelStart, int labelEnd, int bodyStart,
                                            Integer labelColor, Integer messageColor) {
        MutableText out = Text.empty();
        if (value == null || value.isEmpty()) return out.setStyle(base);

        StringBuilder run = new StringBuilder();
        Integer current = null;
        boolean initialized = false;
        for (int index = 0; index < value.length(); index++) {
            int position = globalStart + index;
            Integer next = position >= labelStart && position < labelEnd
                    ? labelColor : position >= bodyStart ? messageColor : null;
            if (initialized && !java.util.Objects.equals(current, next)) {
                append(out, run, base, current);
            }
            initialized = true;
            current = next;
            run.append(value.charAt(index));
        }
        append(out, run, base, current);
        return out;
    }

    private static void append(MutableText out, StringBuilder run, Style base, Integer color) {
        if (run.isEmpty()) return;
        out.append(Text.literal(run.toString()).setStyle(
                color == null ? base : base.withColor(color)));
        run.setLength(0);
    }

    private static String ownText(Text node) {
        StringBuilder out = new StringBuilder();
        node.getContent().visit(value -> {
            out.append(value);
            return Optional.empty();
        });
        return out.toString();
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
