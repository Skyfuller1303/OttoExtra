package de.ottoextra.rpnames.tablist;

import com.mojang.authlib.GameProfile;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.regex.Pattern;

public final class TablistNameFormatter {

    private static final String UNKNOWN_COLOR = "#8A8A8A";
    private static final Pattern OBJECT_TEXT_TOKEN = Pattern.compile(
            "\\[[a-z0-9_.-]+:[^\\]\\r\\n]+@[^\\]\\r\\n]+\\]\\s*",
            Pattern.CASE_INSENSITIVE);

    private TablistNameFormatter() {
    }

    public static Text format(GameProfile gameProfile, Text original) {
        if (!RpNamesServices.isActive() || gameProfile == null) {
            return null;
        }

        OttoExtraConfig.RpNames cfg = RpNamesServices.config();
        boolean showTitles = cfg.tablistShowTitle;
        String account = gameProfile.name();
        Text base = original != null ? original : Text.literal(account);
        LocalRpProfile profile = RpNamesServices.store()
                .find(gameProfile.id() != null ? gameProfile.id().toString() : null, account)
                .orElse(null);

        if (profile != null && !profile.showInTablist) {
            return null;
        }

        if (profile == null) {
            boolean unknown = cfg.tablistEnabled && RpNamesServices.proactiveMeetEnabled();
            String replacement = unknown ? RpNamesServices.unknownDisplay(account) : account;
            String color = unknown && !RpNamesServices.unknownShowsAccount()
                    ? UNKNOWN_COLOR : RpNamesServices.playerNameColor(null, null);

            // Bei aktiver Titelanzeige bleibt auch für unbekannte Spieler der
            // originale Servertitel sichtbar. Nur bei ausgeschalteter Option
            // wird er entfernt; Wappen und sonstige Rich-Text-Komponenten bleiben.
            if (!showTitles) {
                LocalRpProfile placeholder = new LocalRpProfile();
                placeholder.accountName = account;
                Text stripped = replaceServerTitleAndName(base, account, null,
                        replacement, color, placeholder, false);
                if (stripped != null) {
                    return stripped;
                }
            }

            // Wenn weder RP-Name noch Farbe geändert werden müssen, die originale
            // Server-Komponente unverändert lassen. So bleiben Titel/Wappen exakt erhalten.
            if (!cfg.tablistEnabled && showTitles) {
                return null;
            }

            NameRewriteState state = new NameRewriteState(account, replacement, color);
            MutableText rewritten = rewriteNameOnly(base, state);
            return state.replaced ? rewritten : null;
        }

        boolean knownForDisplay = RpNamesServices.isKnownForDisplay(profile);
        String replacement;
        String color;
        if (cfg.tablistEnabled && knownForDisplay) {
            replacement = profile.rpName;
            color = RpNamesServices.rpNameColor(profile.colors.tabNameColor, profile.title);
        } else if (cfg.tablistEnabled) {
            replacement = RpNamesServices.unknownDisplay(account);
            color = RpNamesServices.unknownShowsAccount()
                    ? RpNamesServices.playerNameColor(profile.colors.tabNameColor, profile.title)
                    : UNKNOWN_COLOR;
        } else {
            replacement = account;
            color = RpNamesServices.playerNameColor(profile.colors.tabNameColor, profile.title);
        }

        boolean hasRenderableLocalTitle = hasRenderableTitle(profile);

        // Unbekannte behalten bei aktiver Titelanzeige den originalen Servertitel.
        // Ein lokaler Titel ersetzt ihn nur bei bekannten Personen; bei
        // ausgeschalteter Titeloption wird er für alle entfernt.
        if (!showTitles || (knownForDisplay && hasRenderableLocalTitle)) {
            boolean includeLocalTitle = knownForDisplay && showTitles && hasRenderableLocalTitle;
            Text titled = replaceServerTitleAndName(base, account, profile.rpName,
                    replacement, color, profile, includeLocalTitle);
            if (titled != null) {
                return titled;
            }
        }

        // Kein lokaler Titel vorhanden: den aktuellen Servertitel unverändert behalten
        // und ausschließlich den Namen ersetzen. Das ist der entscheidende Fallback,
        // damit „Titel in Tabliste“ AN nicht versehentlich alle Titel entfernt.
        NameRewriteState accountState = new NameRewriteState(account, replacement, color);
        MutableText rewritten = rewriteNameOnly(base, accountState);
        if (accountState.replaced) {
            return rewritten;
        }

        if (profile.hasRpName()) {
            NameRewriteState rpState = new NameRewriteState(profile.rpName, replacement, color);
            MutableText byRpName = rewriteNameOnly(base, rpState);
            if (rpState.replaced) {
                return byRpName;
            }
        }

        return null;
    }

    private static boolean hasRenderableTitle(LocalRpProfile profile) {
        if (profile == null || !profile.hasTitle()) {
            return false;
        }
        String clean = cleanObjectDebugTokens(profile.title);
        String shown = cleanObjectDebugTokens(RpNamesServices.displayTitle(profile));
        return !shown.isBlank();
    }

    private static final class TabRewriteState {
        private final String account;
        private final String rpName;
        private final String replacement;
        private final String color;
        private final LocalRpProfile profile;
        private final boolean includeTitle;
        private boolean nameWritten;
        private String pendingLeadingWhitespace = "";

        private TabRewriteState(String account, String rpName, String replacement,
                                String color, LocalRpProfile profile, boolean includeTitle) {
            this.account = account;
            this.rpName = rpName;
            this.replacement = replacement;
            this.color = color;
            this.profile = profile;
            this.includeTitle = includeTitle;
        }
    }

    private static final class NameRewriteState {
        private final String needle;
        private final String replacement;
        private final String color;
        private boolean replaced;

        private NameRewriteState(String needle, String replacement, String color) {
            this.needle = needle;
            this.replacement = replacement;
            this.color = color;
        }
    }

    private record NameMatch(int index, int length) {
    }

    private static Text replaceServerTitleAndName(Text base, String account,
                                                  String rpName, String replacement,
                                                  String color,
                                                  LocalRpProfile profile,
                                                  boolean includeTitle) {
        TabRewriteState state = new TabRewriteState(account, rpName, replacement, color, profile,
                includeTitle);
        MutableText rewritten = rewriteTitleAndName(base, state);
        return state.nameWritten ? rewritten : null;
    }

    private static MutableText rewriteTitleAndName(Text node, TabRewriteState state) {
        if (node == null) {
            return Text.empty();
        }

        TextContent content = node.getContent();
        Style style = safeStyle(node.getStyle());
        MutableText copy;

        if (content instanceof ObjectTextContent) {
            copy = MutableText.of(content).setStyle(style);
        } else if (content instanceof TranslatableTextContent translated) {
            Object[] args = translated.getArgs();
            Object[] rewrittenArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                rewrittenArgs[i] = rewriteTitleArgument(args[i], state, style);
            }
            copy = MutableText.of(new TranslatableTextContent(
                    translated.getKey(), translated.getFallback(), rewrittenArgs)).setStyle(style);
        } else if (content instanceof PlainTextContent plain) {
            copy = rewriteTitleLiteral(plain.string(), style, state);
        } else {

            copy = MutableText.of(content).setStyle(style);
        }

        for (Text sibling : node.getSiblings()) {
            copy.append(rewriteTitleAndName(sibling, state));
        }
        return copy;
    }

    private static Object rewriteTitleArgument(Object argument, TabRewriteState state, Style parentStyle) {
        if (state.nameWritten || argument == null) {
            return argument;
        }
        if (argument instanceof Text text) {
            if (containsName(text, state.account, state.rpName)) {
                return rewriteTitleAndName(text, state);
            }
            if (containsObjectComponent(text)) {
                return text.copy();
            }
            rememberWhitespace(textWithoutObjectComponents(text), state);
            return Text.empty();
        }
        if (argument instanceof String string) {
            return rewriteTitleArgumentString(string, state, parentStyle);
        }
        return argument;
    }

    private static Object rewriteTitleArgumentString(String text, TabRewriteState state,
                                                     Style parentStyle) {
        NameMatch match = findName(text, state.account, state.rpName);
        if (match == null) {
            rememberWhitespace(text, state);
            return "";
        }
        return buildTitleAndNameReplacement(text, match, parentStyle, state);
    }

    private static MutableText rewriteTitleLiteral(String text, Style style, TabRewriteState state) {
        if (state.nameWritten) {
            return Text.literal(text).setStyle(style);
        }
        NameMatch match = findName(text, state.account, state.rpName);
        if (match == null) {
            rememberWhitespace(text, state);
            return Text.empty().setStyle(style);
        }
        return buildTitleAndNameReplacement(text, match, style, state);
    }

    private static MutableText buildTitleAndNameReplacement(String text, NameMatch match,
                                                            Style baseStyle,
                                                            TabRewriteState state) {
        MutableText out = Text.empty().setStyle(baseStyle);
        String before = text.substring(0, match.index());
        String leading = leadingWhitespace(before);
        if (leading.isEmpty()) {
            leading = state.pendingLeadingWhitespace;
        }
        if (!leading.isEmpty()) {
            out.append(Text.literal(leading).setStyle(baseStyle));
        }
        if (state.includeTitle) {
            out.append(titlePrefix(state.profile, baseStyle));
        }
        out.append(colored(state.replacement, state.color, baseStyle));
        int end = match.index() + match.length();
        if (end < text.length()) {
            out.append(Text.literal(text.substring(end)).setStyle(baseStyle));
        }
        state.nameWritten = true;
        state.pendingLeadingWhitespace = "";
        return out;
    }

    private static void rememberWhitespace(String text, TabRewriteState state) {
        String leading = leadingWhitespace(text);
        if (!leading.isEmpty()) {
            state.pendingLeadingWhitespace = leading;
        }
    }

    private static MutableText rewriteNameOnly(Text node, NameRewriteState state) {
        if (node == null) {
            return Text.empty();
        }
        TextContent content = node.getContent();
        Style style = safeStyle(node.getStyle());
        MutableText copy;

        if (content instanceof ObjectTextContent) {
            copy = MutableText.of(content).setStyle(style);
        } else if (content instanceof TranslatableTextContent translated) {
            Object[] args = translated.getArgs();
            Object[] rewrittenArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                rewrittenArgs[i] = rewriteNameArgument(args[i], state, style);
            }
            copy = MutableText.of(new TranslatableTextContent(
                    translated.getKey(), translated.getFallback(), rewrittenArgs)).setStyle(style);
        } else if (content instanceof PlainTextContent plain) {
            copy = replaceNameInLiteral(plain.string(), style, state);
        } else {
            copy = MutableText.of(content).setStyle(style);
        }

        for (Text sibling : node.getSiblings()) {
            copy.append(rewriteNameOnly(sibling, state));
        }
        return copy;
    }

    private static Object rewriteNameArgument(Object argument, NameRewriteState state,
                                              Style parentStyle) {
        if (argument instanceof Text text) {
            return rewriteNameOnly(text, state);
        }
        if (argument instanceof String string) {
            int index = indexOfIgnoreEmpty(string, state.needle);
            if (index < 0) {
                return argument;
            }
            state.replaced = true;
            MutableText out = Text.empty().setStyle(parentStyle);
            if (index > 0) {
                out.append(Text.literal(string.substring(0, index)).setStyle(parentStyle));
            }
            out.append(colored(state.replacement, state.color, parentStyle));
            int end = index + state.needle.length();
            if (end < string.length()) {
                out.append(Text.literal(string.substring(end)).setStyle(parentStyle));
            }
            return out;
        }
        return argument;
    }

    private static MutableText replaceNameInLiteral(String text, Style style,
                                                    NameRewriteState state) {
        int index = indexOfIgnoreEmpty(text, state.needle);
        if (index < 0) {
            return Text.literal(text).setStyle(style);
        }
        state.replaced = true;
        MutableText out = Text.empty().setStyle(style);
        if (index > 0) {
            out.append(Text.literal(text.substring(0, index)).setStyle(style));
        }
        out.append(colored(state.replacement, state.color, style));
        int end = index + state.needle.length();
        if (end < text.length()) {
            out.append(Text.literal(text.substring(end)).setStyle(style));
        }
        return out;
    }

    private static int indexOfIgnoreEmpty(String text, String needle) {
        return text == null || needle == null || needle.isBlank() ? -1 : text.indexOf(needle);
    }

    private static NameMatch findName(String text, String account, String rpName) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int accountIndex = account == null || account.isBlank() ? -1 : text.indexOf(account);
        int rpIndex = rpName == null || rpName.isBlank() ? -1 : text.indexOf(rpName);
        if (accountIndex < 0 && rpIndex < 0) {
            return null;
        }
        if (accountIndex >= 0 && (rpIndex < 0 || accountIndex <= rpIndex)) {
            return new NameMatch(accountIndex, account.length());
        }
        return new NameMatch(rpIndex, rpName.length());
    }

    private static boolean containsName(Text text, String account, String rpName) {
        String flat = textWithoutObjectComponents(text);
        return (account != null && !account.isBlank() && flat.contains(account))
                || (rpName != null && !rpName.isBlank() && flat.contains(rpName));
    }

    private static boolean containsObjectComponent(Text node) {
        if (node == null) {
            return false;
        }
        TextContent content = node.getContent();
        if (content instanceof ObjectTextContent) {
            return true;
        }
        if (content instanceof TranslatableTextContent translated) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Text text && containsObjectComponent(text)) {
                    return true;
                }
            }
        }
        for (Text sibling : node.getSiblings()) {
            if (containsObjectComponent(sibling)) {
                return true;
            }
        }
        return false;
    }

    private static String leadingWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return text.substring(0, i);
    }

    private static MutableText titlePrefix(LocalRpProfile profile, Style baseStyle) {
        var catalog = RpNamesServices.catalog();
        String cleanTitle = cleanObjectDebugTokens(profile.title);
        String catalogColor = catalog != null
                ? catalog.titleColor(cleanTitle).orElse(null) : null;
        String groupColor = RpNamesServices.titles().find(cleanTitle)
                .map(r -> r.group().titleColor).orElse(null);
        String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";
        String shown = cleanObjectDebugTokens(RpNamesServices.displayTitle(profile));
        String personal = profile.colors.tabTitleColor;
        String titleColor = firstNonBlank(personal,
                firstNonBlank(catalogColor, firstNonBlank(groupColor, fallback)));
        return colored(shown + " ", titleColor, baseStyle);
    }

    public static String extractServerTitle(Text display, String accountName) {
        if (display == null || accountName == null || accountName.isBlank()) {
            return "";
        }
        String flat = textWithoutObjectComponents(display);
        int idx = flat.indexOf(accountName);
        if (idx <= 0) {
            return "";
        }
        return cleanObjectDebugTokens(flat.substring(0, idx));
    }

    public static String cleanObjectDebugTokens(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return OBJECT_TEXT_TOKEN.matcher(value)
                .replaceAll("")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String textWithoutObjectComponents(Text node) {
        StringBuilder out = new StringBuilder();
        appendTextWithoutObjects(node, out);
        return cleanObjectDebugTokens(out.toString());
    }

    private static void appendTextWithoutObjects(Text node, StringBuilder out) {
        if (node == null) {
            return;
        }
        TextContent content = node.getContent();
        if (content instanceof ObjectTextContent) {

        } else if (content instanceof TranslatableTextContent translated) {

            translated.visit(s -> {
                out.append(s);
                return java.util.Optional.empty();
            });
        } else if (content instanceof PlainTextContent plain) {
            out.append(plain.string());
        } else if (content != null) {
            content.visit(s -> {
                out.append(s);
                return java.util.Optional.empty();
            });
        }
        for (Text sibling : node.getSiblings()) {
            appendTextWithoutObjects(sibling, out);
        }
    }

    private static MutableText colored(String text, String hex, Style baseStyle) {
        Style style = safeStyle(baseStyle);
        TextColor color = de.ottoextra.rpnames.chat.ChatNameRewriter.parseColor(hex);
        if (color != null) {
            style = style.withColor(color);
        }
        return Text.literal(text).setStyle(style);
    }

    private static Style safeStyle(Style style) {
        return style == null ? Style.EMPTY : style;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
