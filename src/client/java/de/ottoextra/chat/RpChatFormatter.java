package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RpChatFormatter {

    public static final String ROLE_ACCENT = "accent";
    public static final String ROLE_MUTED = "muted";
    public static final String ROLE_DECORATION = "decoration";
    public static final String ROLE_ERROR = "error";
    public static final String ROLE_SUCCESS = "success";
    public static final String ROLE_INACTIVE = "inactive";
    public static final String ROLE_DISABLED = "disabled";
    public static final String ROLE_BODY = "body";

    private RpChatFormatter() {
    }

    public static Text format(Text message) {
        if (message == null || !enabled()) {
            return message;
        }

        try {
            String plain = message.getString();
            int bodyStart = findBodyStart(plain);
            if (bodyStart < 0 || bodyStart >= plain.length()
                    || !isRpChannelLine(plain, bodyStart)) {
                return message;
            }

            StateBox syntax = new StateBox();
            return rebuild(message, bodyStart, new int[]{0}, syntax, Style.EMPTY);
        } catch (Throwable ignored) {
            return message;
        }
    }

    /**
     * Formatiert einen reinen RP-Nachrichteninhalt nach den zentralen
     * OttoExtra-Vorgaben. Addons verwenden diese Methode insbesondere fuer
     * Hover-Lore und lokal eingeblendete Chattexte, damit sie keine eigenen
     * Farben oder Syntaxregeln mitbringen muessen.
     */
    public static Text formatContent(Text content) {
        if (content == null || !enabled()) {
            return content;
        }
        try {
            return rebuild(content, 0, new int[]{0}, new StateBox(), Style.EMPTY);
        } catch (Throwable ignored) {
            return content;
        }
    }

    /**
     * Zentrale Darstellungsvorgabe fuer Chat-Addons. Addons benennen nur die
     * semantische Rolle; konkrete Farben bleiben Eigentum von OttoExtra.
     */
    public static Text stylePresentation(Text content, String role) {
        if (content == null) {
            return null;
        }
        MutableText copy = content.copy();
        copy.setStyle(presentationStyle(copy.getStyle(), role));
        return copy;
    }

    /** Liefert einen zentral von OttoExtra gestylten Basisstil. */
    public static Style presentationStyle(Style base, String role) {
        Style safe = safeStyle(base);
        return safe.withColor(presentationColor(role));
    }

    /**
     * Zentrale RGB-Farbe fuer semantische Chat-Rollen. Die String-API ist
     * absichtlich stabil, damit optionale Addons sie versionsdynamisch nutzen
     * koennen, ohne gegen eine bestimmte OttoExtra-Version gelinkt zu sein.
     */
    public static int presentationColor(String role) {
        return switch (role == null ? "" : role) {
            case ROLE_ACCENT -> 0xFFAA00;
            case ROLE_MUTED -> 0xAAAAAA;
            case ROLE_DECORATION -> 0x555555;
            case ROLE_ERROR -> 0xFF5555;
            case ROLE_SUCCESS -> 0x55FF55;
            case ROLE_INACTIVE -> 0x888888;
            case ROLE_DISABLED -> 0xAA5555;
            case ROLE_BODY -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    /** Wie {@link #presentationColor(String)}, jedoch fuer GUI-Zeichenaufrufe. */
    public static int presentationArgb(String role) {
        return 0xFF000000 | presentationColor(role);
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

    static boolean isRpChannelLine(String plain, int bodyStart) {
        if (plain == null || plain.isEmpty() || bodyStart <= 0) {
            return false;
        }

        String header = plain.substring(0, Math.min(bodyStart, plain.length()));
        for (ChatChannelState.ChatChannel channel : ChatChannelState.ChatChannel.values()) {
            if (!channel.rp && header.contains(channel.label)) {
                return false;
            }
        }
        for (ChatChannelState.ChatChannel channel : ChatChannelState.ChatChannel.values()) {
            if (channel.rp && header.contains(channel.label)) {
                return true;
            }
        }
        return false;
    }

    private static int skipWhitespace(String text, int index) {
        int cursor = Math.max(0, index);
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static Text rebuild(Text node, int bodyStart, int[] flatPos, StateBox syntax,
                                Style parentStyle) {
        TextContent content = node.getContent();
        Style effectiveStyle = safeStyle(node.getStyle()).withParent(safeStyle(parentStyle));

        if (content instanceof net.minecraft.text.TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            Object[] args = translated.getArgs();
            Object[] rewritten = new Object[args.length];
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                if (arg instanceof Text text) {
                    rewritten[index] = rebuild(text, bodyStart, flatPos, syntax, effectiveStyle);
                } else {
                    rewritten[index] = arg;
                    if (arg != null) {
                        flatPos[0] += arg.toString().length();
                    }
                }
            }

            MutableText copy = MutableText.of(new net.minecraft.text.TranslatableTextContent(
                            translated.getKey(), translated.getFallback(), rewritten))
                    .setStyle(effectiveStyle);
            for (Text sibling : node.getSiblings()) {
                copy.append(rebuild(sibling, bodyStart, flatPos, syntax, effectiveStyle));
            }
            return copy;
        }

        String own = ownText(node);
        int start = flatPos[0];
        int end = start + own.length();
        Style base = effectiveStyle;
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
            copy.append(rebuild(sibling, bodyStart, flatPos, syntax, effectiveStyle));
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
                appendSegment(out, segment, base, currentKind, syntax);
            }

            if (c == '§' && index + 1 < text.length()) {
                appendSegment(out, segment, base,
                        currentKind == null ? RpChatSyntax.Kind.NORMAL : currentKind, syntax);
                applyFormattingCode(syntax, Formatting.byCode(text.charAt(++index)));
                currentKind = null;
                continue;
            }

            currentKind = step.kind();
            segment.append(c);
            syntax.state = step.after();
        }
        appendSegment(out, segment, base,
                currentKind == null ? RpChatSyntax.Kind.NORMAL : currentKind, syntax);
        return out;
    }

    private static void appendSegment(MutableText out, StringBuilder segment,
                                      Style base, RpChatSyntax.Kind kind, StateBox syntax) {
        if (segment.isEmpty()) {
            return;
        }
        Style formattedBase = base;
        for (Formatting code : syntax.formattingCodes) {
            formattedBase = formattedBase.withFormatting(code);
        }
        OttoExtraConfig.Chat cfg = chatConfig();
        Style style = switch (kind) {
            case EMOTE -> formattedBase.withColor(parseColor(
                    cfg == null ? null : cfg.rpEmoteColor, 0xC6C6C6))
                    .withItalic(cfg == null || cfg.rpEmoteItalic);
            case OOC -> formattedBase.withColor(parseColor(
                    cfg == null ? null : cfg.rpOocColor, 0xB4BEC6))
                    .withItalic(cfg != null && cfg.rpOocItalic);
            case NORMAL -> formattedBase;
        };
        out.append(Text.literal(segment.toString()).setStyle(style));
        segment.setLength(0);
    }

    private static void applyFormattingCode(StateBox syntax, Formatting code) {
        if (code == null) {
            return;
        }
        if (code == Formatting.RESET) {
            syntax.formattingCodes.clear();
            return;
        }
        if (code.isColor()) {
            syntax.formattingCodes.removeIf(Formatting::isColor);
        }
        syntax.formattingCodes.add(code);
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
        private final List<Formatting> formattingCodes = new ArrayList<>();
    }
}
