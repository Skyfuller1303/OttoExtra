package de.ottoextra.chat;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import java.util.Optional;
public final class RpChatFormatter {
    private RpChatFormatter() {
    }
    public static Text format(Text message) {
        if (message == null || !enabled()) {
            return message;
        }
        try {
            String plain = message.getString();
            int bodyStart = findBodyStart(plain);
            if (bodyStart < 0 || bodyStart >= plain.length()) {
                return message;
            }
            StateBox syntax = new StateBox();
            return rebuild(message, bodyStart, new int[]{0}, syntax);
        } catch (Throwable ignored) {
            return message;
        }
    }
    private static boolean enabled() {
        try {
            OttoExtraConfig.Chat cfg = OttoExtraConfig.active().chat;
            return cfg != null && cfg.enabled && cfg.rpFormattingEnabled;
        } catch (Throwable ignored) {
            return false;
        }
    }
    static int findBodyStart(String plain) {
        if (plain == null || plain.isEmpty()) {
            return -1;
        }
        int bracket = plain.indexOf(']');
        int from = bracket >= 0 ? bracket + 1 : 0;
        int nestedBrackets = 0;
        int fallback = -1;
        for (int i = Math.max(0, from); i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '[') {
                nestedBrackets++;
            } else if (c == ']' && nestedBrackets > 0) {
                nestedBrackets--;
            } else if (c == ':' && nestedBrackets == 0) {
                if (fallback < 0) {
                    fallback = i;
                }
                if (i + 1 >= plain.length() || Character.isWhitespace(plain.charAt(i + 1))) {
                    return skipWhitespace(plain, i + 1);
                }
            }
        }
        return fallback >= 0 ? skipWhitespace(plain, fallback + 1) : -1;
    }
    private static int skipWhitespace(String text, int index) {
        int cursor = Math.max(0, index);
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }
    private static Text rebuild(Text node, int bodyStart, int[] flatPos, StateBox syntax) {
        TextContent content = node.getContent();
        if (content instanceof net.minecraft.text.TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            Object[] args = translated.getArgs();
            Object[] rewritten = new Object[args.length];
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                if (arg instanceof Text text) {
                    rewritten[index] = rebuild(text, bodyStart, flatPos, syntax);
                } else {
                    rewritten[index] = arg;
                    if (arg != null) {
                        flatPos[0] += arg.toString().length();
                    }
                }
            }
            MutableText copy = MutableText.of(new net.minecraft.text.TranslatableTextContent(
                    translated.getKey(), translated.getFallback(), rewritten))
                    .setStyle(safeStyle(node.getStyle()));
            for (Text sibling : node.getSiblings()) {
                copy.append(rebuild(sibling, bodyStart, flatPos, syntax));
            }
            return copy;
        }
        String own = ownText(node);
        int start = flatPos[0];
        int end = start + own.length();
        Style base = safeStyle(node.getStyle());
        MutableText copy;
        if (isVisualComponent(node)) {
            copy = MutableText.of(content).setStyle(base);
        } else if (content instanceof PlainTextContent plain) {
            copy = formatLiteral(plain.string(), start, bodyStart, base, syntax);
        } else {
            copy = MutableText.of(content).setStyle(base);
            scanUnformattedContent(own, start, bodyStart, syntax);
        }
        flatPos[0] = end;
        for (Text sibling : node.getSiblings()) {
            copy.append(rebuild(sibling, bodyStart, flatPos, syntax));
        }
        return copy;
    }
    private static MutableText formatLiteral(String text, int globalStart, int bodyStart,
                                             Style base, StateBox syntax) {
        MutableText out = Text.empty();
        if (text == null || text.isEmpty()) {
            return out.setStyle(base);
        }
        int localBodyStart = Math.max(0, bodyStart - globalStart);
        localBodyStart = Math.min(text.length(), localBodyStart);
        if (localBodyStart > 0) {
            out.append(Text.literal(text.substring(0, localBodyStart)).setStyle(base));
        }
        if (localBodyStart >= text.length()) {
            return out;
        }
        StringBuilder segment = new StringBuilder();
        RpChatSyntax.Kind currentKind = null;
        for (int index = localBodyStart; index < text.length(); index++) {
            char c = text.charAt(index);
            RpChatSyntax.Step step = RpChatSyntax.step(syntax.state, c);
            if (currentKind != null && currentKind != step.kind()) {
                appendSegment(out, segment, base, currentKind);
            }
            currentKind = step.kind();
            segment.append(c);
            syntax.state = step.after();
        }
        appendSegment(out, segment, base,
                currentKind == null ? RpChatSyntax.Kind.NORMAL : currentKind);
        return out;
    }
    private static void appendSegment(MutableText out, StringBuilder segment,
                                      Style base, RpChatSyntax.Kind kind) {
        if (segment.isEmpty()) {
            return;
        }
        OttoExtraConfig.Chat cfg = chatConfig();
        Style style = switch (kind) {
            case EMOTE -> base.withColor(parseColor(
                    cfg == null ? null : cfg.rpEmoteColor, 0xC6C6C6)).withItalic(true);
            case OOC -> base.withColor(parseColor(
                    cfg == null ? null : cfg.rpOocColor, 0xFFD45A)).withItalic(false);
            case NORMAL -> base.withColor(parseColor(
                    cfg == null ? null : cfg.rpNormalColor, 0xAAAAAA));
        };
        out.append(Text.literal(segment.toString()).setStyle(style));
        segment.setLength(0);
    }
    private static void scanUnformattedContent(String own, int globalStart, int bodyStart,
                                               StateBox syntax) {
        if (own == null || own.isEmpty()) {
            return;
        }
        int localStart = Math.max(0, bodyStart - globalStart);
        if (localStart < own.length()) {
            syntax.state = RpChatSyntax.scan(own.substring(localStart), syntax.state);
        }
    }
    private static boolean isVisualComponent(Text node) {
        if (node == null) {
            return false;
        }
        if (node.getContent() instanceof ObjectTextContent) {
            return true;
        }
        StyleSpriteSource font = safeStyle(node.getStyle()).getFont();
        return font instanceof StyleSpriteSource.Player
                || font instanceof StyleSpriteSource.Sprite;
    }
    private static String ownText(Text node) {
        StringBuilder out = new StringBuilder();
        node.getContent().visit(value -> {
            out.append(value);
            return Optional.empty();
        });
        return out.toString();
    }
    private static OttoExtraConfig.Chat chatConfig() {
        try {
            OttoExtraConfig config = OttoExtraConfig.active();
            return config == null ? null : config.chat;
        } catch (Throwable ignored) {
            return null;
        }
    }
    private static int parseColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("[0-9a-fA-F]{6}")) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
    private static Style safeStyle(Style style) {
        return style == null ? Style.EMPTY : style;
    }
    private static final class StateBox {
        private RpChatSyntax.State state = RpChatSyntax.State.normal();
    }
}
