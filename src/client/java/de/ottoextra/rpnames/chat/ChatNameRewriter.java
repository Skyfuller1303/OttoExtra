package de.ottoextra.rpnames.chat;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextContent;
import java.util.Locale;
import java.util.Optional;
public final class ChatNameRewriter {
    private final LocalRpIdentityStore store;
    private final TitleRegistry titles;
    public ChatNameRewriter(LocalRpIdentityStore store, TitleRegistry titles) {
        this.store = store;
        this.titles = titles;
    }
    public Text rewrite(Text message, OttoExtraConfig.RpNames cfg) {
        return rewrite(message, cfg, null);
    }
    public Text rewrite(Text message, OttoExtraConfig.RpNames cfg, String forcedAccount) {
        try {
            SpeakerBounds bounds = SpeakerBounds.from(message);
            LocalRpProfile forced = forcedAccount == null || forcedAccount.isBlank()
                    ? null : store.findByName(forcedAccount).orElse(null);
            SpeakerInfo si = findSpeaker(message, cfg, bounds, forced, false);
            if (si == null) {
                return message;
            }
            return collapseSpaces(rebuild(message, si, new int[]{0}, false),
                    new boolean[]{false});
        } catch (Throwable t) {
            return message;
        }
    }
    public Text rewriteTitleOnly(Text message, OttoExtraConfig.RpNames cfg) {
        try {
            SpeakerBounds bounds = SpeakerBounds.from(message);
            SpeakerInfo si = findSpeaker(message, cfg, bounds, null, true);
            if (si == null || !ownTitleApplies(si.profile())) {
                return message;
            }
            return collapseSpaces(rebuild(message, si, new int[]{0}, true),
                    new boolean[]{false});
        } catch (Throwable t) {
            return message;
        }
    }
    private record SpeakerBounds(int start, int end) {
        static SpeakerBounds from(Text message) {
            if (message == null) {
                return null;
            }
            String plain = message.getString();
            int bracket = plain.indexOf(']');
            int colon = bracket >= 0 ? separatorColon(plain, bracket + 1) : -1;
            if (bracket < 0 || colon < 0 || colon <= bracket + 1) {
                return null;
            }
            int start = bracket + 1;
            while (start < colon && Character.isWhitespace(plain.charAt(start))) {
                start++;
            }
            int end = colon;
            while (end > start && Character.isWhitespace(plain.charAt(end - 1))) {
                end--;
            }
            return end > start ? new SpeakerBounds(start, end) : null;
        }
        private static int separatorColon(String plain, int from) {
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
                        return i;
                    }
                }
            }
            return fallback;
        }
    }
    private record ProfileMatch(LocalRpProfile profile, String visibleIdentity,
                                int localStart, int localEnd, int score) {
    }
    private record SpeakerInfo(Text node, LocalRpProfile profile, String visibleIdentity,
                               int nodeStart, int replaceStart, int replaceEnd,
                               int titleZoneStart) {
    }
    private static final class BestSpeaker {
        private SpeakerInfo info;
        private int score = Integer.MIN_VALUE;
        void offer(SpeakerInfo candidate, int candidateScore) {
            if (candidate == null) {
                return;
            }
            if (info == null || candidateScore > score
                    || (candidateScore == score
                    && candidate.replaceEnd() > info.replaceEnd())) {
                info = candidate;
                score = candidateScore;
            }
        }
    }
    private SpeakerInfo findSpeaker(Text message, OttoExtraConfig.RpNames cfg,
                                    SpeakerBounds bounds, LocalRpProfile forced,
                                    boolean titleOnly) {
        BestSpeaker best = new BestSpeaker();
        findSpeakerNode(message, cfg, bounds, forced, titleOnly, new int[]{0}, best);
        return best.info;
    }
    private void findSpeakerNode(Text node, OttoExtraConfig.RpNames cfg,
                                 SpeakerBounds bounds, LocalRpProfile forced,
                                 boolean titleOnly, int[] flatPos, BestSpeaker best) {
        if (node == null) {
            return;
        }
        TextContent content = node.getContent();
        if (content instanceof net.minecraft.text.TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Text text) {
                    findSpeakerNode(text, cfg, bounds, forced, titleOnly, flatPos, best);
                } else if (argument != null) {
                    flatPos[0] += argument.toString().length();
                }
            }
            for (Text sibling : node.getSiblings()) {
                findSpeakerNode(sibling, cfg, bounds, forced, titleOnly, flatPos, best);
            }
            return;
        }
        String own = ownText(node);
        int nodeStart = flatPos[0];
        int nodeEnd = nodeStart + own.length();
        if (content instanceof PlainTextContent && !isVisualComponent(node)
                && !own.isBlank() && overlapsSpeaker(nodeStart, nodeEnd, bounds)) {
            int localFrom = bounds == null ? 0 : Math.max(0, bounds.start() - nodeStart);
            int localTo = bounds == null ? own.length() : Math.min(own.length(), bounds.end() - nodeStart);
            if (localFrom < localTo) {
                String inSpeaker = own.substring(localFrom, localTo);
                ProfileMatch match = findProfileInSpeakerText(inSpeaker, cfg, titleOnly);
                LocalRpProfile hover = profileFromHover(node);
                if (hover != null && profileAllowed(hover, cfg, titleOnly)) {
                    String identity = identityInside(inSpeaker, hover);
                    int identityAt = identityOffset(inSpeaker, identity);
                    int rs = identityAt >= 0 ? nodeStart + localFrom + identityAt
                            : nodeStart + localFrom;
                    int re = identityAt >= 0 ? rs + identity.length()
                            : nodeStart + localTo;
                    best.offer(new SpeakerInfo(node, hover, identity,
                                    nodeStart, rs, re, titleZoneStart(bounds, nodeStart)),
                            10_000 + re);
                }
                if (match != null && profileAllowed(match.profile(), cfg, titleOnly)) {
                    int rs = nodeStart + localFrom + match.localStart();
                    int re = nodeStart + localFrom + match.localEnd();
                    best.offer(new SpeakerInfo(node, match.profile(), match.visibleIdentity(),
                                    nodeStart, rs, re, titleZoneStart(bounds, nodeStart)),
                            8_000 + match.score() + re);
                }
                if (forced != null && profileAllowed(forced, cfg, titleOnly)) {
                    String identity = identityInside(inSpeaker, forced);
                    int identityAt = identityOffset(inSpeaker, identity);
                    int rs;
                    int re;
                    int score;
                    if (identityAt >= 0) {
                        rs = nodeStart + localFrom + identityAt;
                        re = rs + identity.length();
                        score = 30_000 + identity.length();
                    } else {
                        rs = nodeStart + localFrom;
                        re = nodeStart + localTo;
                        score = 25_000 + re;
                    }
                    best.offer(new SpeakerInfo(node, forced, identity,
                                    nodeStart, rs, re, titleZoneStart(bounds, nodeStart)),
                            score);
                }
            }
        }
        flatPos[0] = nodeEnd;
        for (Text sibling : node.getSiblings()) {
            findSpeakerNode(sibling, cfg, bounds, forced, titleOnly, flatPos, best);
        }
    }
    private static int titleZoneStart(SpeakerBounds bounds, int nodeStart) {
        return bounds != null ? bounds.start() : nodeStart;
    }
    private static boolean overlapsSpeaker(int start, int end, SpeakerBounds bounds) {
        return bounds == null || (start < bounds.end() && end > bounds.start());
    }
    private boolean profileAllowed(LocalRpProfile p, OttoExtraConfig.RpNames cfg,
                                   boolean titleOnly) {
        if (p == null || !p.showInChat) {
            return false;
        }
        if (titleOnly) {
            return ownTitleApplies(p);
        }
        return de.ottoextra.rpnames.RpNamesServices.isKnownForDisplay(p)
                || (cfg != null && cfg.showUnknownAsUnknown)
                || de.ottoextra.rpnames.RpNamesServices.proactiveMeetEnabled();
    }
    private LocalRpProfile profileFromHover(Text node) {
        try {
            Style style = node.getStyle();
            if (style == null
                    || !(style.getHoverEvent() instanceof net.minecraft.text.HoverEvent.ShowText shown)) {
                return null;
            }
            String flat = shown.value().getString();
            int nl = flat.indexOf('\n');
            String firstLine = (nl >= 0 ? flat.substring(0, nl) : flat).trim();
            if (firstLine.isEmpty()) {
                return null;
            }
            int sp = firstLine.lastIndexOf(' ');
            String account = sp >= 0 ? firstLine.substring(sp + 1) : firstLine;
            return store.findByName(account).orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }
    private Text rebuild(Text node, SpeakerInfo si, int[] flatPos, boolean titleOnly) {
        TextContent content = node.getContent();
        if (content instanceof net.minecraft.text.TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            Object[] arguments = translated.getArgs();
            Object[] rewrittenArguments = new Object[arguments.length];
            for (int index = 0; index < arguments.length; index++) {
                Object argument = arguments[index];
                if (argument instanceof Text text) {
                    rewrittenArguments[index] = rebuild(text, si, flatPos, titleOnly);
                } else {
                    rewrittenArguments[index] = argument;
                    if (argument != null) {
                        flatPos[0] += argument.toString().length();
                    }
                }
            }
            MutableText translatedCopy = MutableText.of(
                    new net.minecraft.text.TranslatableTextContent(
                            translated.getKey(), translated.getFallback(), rewrittenArguments))
                    .setStyle(safeStyle(node.getStyle()));
            for (Text sibling : node.getSiblings()) {
                translatedCopy.append(rebuild(sibling, si, flatPos, titleOnly));
            }
            return translatedCopy;
        }
        String own = ownText(node);
        int start = flatPos[0];
        int end = start + own.length();
        Style style = safeStyle(node.getStyle());
        MutableText copy;
        boolean known = de.ottoextra.rpnames.RpNamesServices
                .isKnownForDisplay(si.profile());
        boolean ownTitle = ownTitleApplies(si.profile());
        boolean renderOurTitle = known && ownTitle;
        boolean blankServerTitle = !known || ownTitle;
        if (isVisualComponent(node)) {
            copy = MutableText.of(content).setStyle(style);
        } else if (node == si.node()) {
            copy = rebuildSpeakerNode(own, start, style, si, titleOnly,
                    renderOurTitle, known);
        } else if (content instanceof PlainTextContent && blankServerTitle
                && !own.trim().isEmpty()
                && start >= si.titleZoneStart() && end <= si.replaceStart()) {
            copy = Text.literal(" ").setStyle(style);
        } else {
            copy = MutableText.of(content).setStyle(style);
        }
        flatPos[0] = end;
        for (Text sibling : node.getSiblings()) {
            copy.append(rebuild(sibling, si, flatPos, titleOnly));
        }
        return copy;
    }
    private MutableText rebuildSpeakerNode(String own, int nodeStart, Style style,
                                           SpeakerInfo si, boolean titleOnly,
                                           boolean renderOurTitle, boolean known) {
        int localStart = Math.max(0, Math.min(own.length(), si.replaceStart() - nodeStart));
        int localEnd = Math.max(localStart,
                Math.min(own.length(), si.replaceEnd() - nodeStart));
        boolean stripServerTitleInSameNode = (!known || ownTitleApplies(si.profile()))
                && localStart > 0
                && !own.substring(0, localStart).isBlank();
        if (stripServerTitleInSameNode) {
            int leadingWhitespace = 0;
            while (leadingWhitespace < localStart
                    && Character.isWhitespace(own.charAt(leadingWhitespace))) {
                leadingWhitespace++;
            }
            localStart = leadingWhitespace;
        }
        MutableText out = Text.empty().setStyle(style);
        if (localStart > 0) {
            out.append(Text.literal(own.substring(0, localStart)));
        } else if (nodeStart > si.titleZoneStart()) {
            out.append(Text.literal(" "));
        }
        if (titleOnly) {
            MutableText title = titleComponent(si.profile());
            if (title != null) {
                out.append(title);
            }
            String visible = si.visibleIdentity() == null || si.visibleIdentity().isBlank()
                    ? own.substring(localStart, localEnd) : si.visibleIdentity();
            out.append(Text.literal(visible));
        } else if (!known) {
            out.append(Text.literal(de.ottoextra.rpnames.RpNamesServices
                    .unknownChatDisplay(si.profile().accountName)));
        } else {
            out.append(displayName(si.profile(), Style.EMPTY, renderOurTitle));
        }
        if (localEnd < own.length()) {
            out.append(Text.literal(own.substring(localEnd)));
        }
        return out;
    }
    private Text collapseSpaces(Text node, boolean[] lastSpace) {
        TextContent content = node.getContent();
        Style style = safeStyle(node.getStyle());
        if (content instanceof net.minecraft.text.TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            Object[] arguments = translated.getArgs();
            Object[] collapsedArguments = new Object[arguments.length];
            for (int index = 0; index < arguments.length; index++) {
                Object argument = arguments[index];
                collapsedArguments[index] = argument instanceof Text text
                        ? collapseSpaces(text, lastSpace)
                        : argument;
            }
            MutableText translatedCopy = MutableText.of(
                    new net.minecraft.text.TranslatableTextContent(
                            translated.getKey(), translated.getFallback(), collapsedArguments))
                    .setStyle(style);
            for (Text sibling : node.getSiblings()) {
                translatedCopy.append(collapseSpaces(sibling, lastSpace));
            }
            return translatedCopy;
        }
        MutableText copy;
        if (isVisualComponent(node)) {
            copy = MutableText.of(content).setStyle(style);
            lastSpace[0] = false;
        } else if (content instanceof PlainTextContent plain) {
            String own = plain.string();
            StringBuilder sb = new StringBuilder(own.length());
            for (int i = 0; i < own.length(); i++) {
                char c = own.charAt(i);
                if (c == ' ') {
                    if (lastSpace[0]) {
                        continue;
                    }
                    lastSpace[0] = true;
                } else {
                    lastSpace[0] = false;
                }
                sb.append(c);
            }
            copy = Text.literal(sb.toString()).setStyle(style);
        } else {
            copy = MutableText.of(content).setStyle(style);
            String own = ownText(node);
            if (!own.isEmpty()) {
                lastSpace[0] = Character.isWhitespace(own.charAt(own.length() - 1));
            }
        }
        for (Text sibling : node.getSiblings()) {
            copy.append(collapseSpaces(sibling, lastSpace));
        }
        return copy;
    }
    private static boolean isVisualComponent(Text node) {
        if (node == null) {
            return false;
        }
        if (node.getContent() instanceof ObjectTextContent) {
            return true;
        }
        Style style = safeStyle(node.getStyle());
        StyleSpriteSource font = style.getFont();
        return font instanceof StyleSpriteSource.Player
                || font instanceof StyleSpriteSource.Sprite;
    }
    private MutableText displayName(LocalRpProfile profile, Style baseStyle) {
        return displayName(profile, baseStyle, true);
    }
    private boolean ownTitleApplies(LocalRpProfile profile) {
        return profile != null
                && de.ottoextra.rpnames.RpNamesServices.isKnownForDisplay(profile)
                && profile.hasTitle();
    }
    private MutableText titleComponent(LocalRpProfile profile) {
        if (!ownTitleApplies(profile)) {
            return null;
        }
        var catalog = de.ottoextra.rpnames.RpNamesServices.catalog();
        String groupTitleColor = null;
        Optional<TitleRegistry.ResolvedTitle> resolved = titles.find(profile.title);
        if (resolved.isPresent()) {
            groupTitleColor = resolved.get().group().titleColor;
        }
        String catalogColor = catalog != null
                ? catalog.titleColor(profile.title).orElse(null) : null;
        String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";
        String pers = profile.colors.chatTitleColor;
        String titleColor = firstNonBlank(pers,
                firstNonBlank(catalogColor, firstNonBlank(groupTitleColor, fallback)));
        return colored(de.ottoextra.rpnames.RpNamesServices
                .canonicalTitle(profile.title) + " ", titleColor);
    }
    private MutableText displayName(LocalRpProfile profile, Style baseStyle,
                                    boolean includeTitle) {
        MutableText out = Text.empty().setStyle(baseStyle == null ? Style.EMPTY : baseStyle);
        if (includeTitle) {
            MutableText title = titleComponent(profile);
            if (title != null) {
                out.append(title);
            }
        }
        String name = profile.rpName;
        String nameColor = de.ottoextra.rpnames.RpNamesServices.rpNameColor(
                profile.colors.chatNameColor, profile.title);
        out.append(colored(name, nameColor));
        return out;
    }
    private static MutableText colored(String text, String hex) {
        MutableText t = Text.literal(text == null ? "" : text);
        TextColor color = parseColor(hex);
        if (color != null) {
            t.setStyle(Style.EMPTY.withColor(color));
        }
        return t;
    }
    public static TextColor parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            return TextColor.fromRgb(Integer.parseInt(
                    hex.replace("#", "").trim(), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
    private ProfileMatch findProfileInSpeakerText(String speaker,
                                                  OttoExtraConfig.RpNames cfg,
                                                  boolean titleOnly) {
        if (speaker == null || speaker.isBlank()) {
            return null;
        }
        ProfileMatch best = null;
        for (LocalRpProfile p : store.all()) {
            if (!profileAllowed(p, cfg, titleOnly)) {
                continue;
            }
            ProfileMatch candidate = matchIdentity(speaker, p);
            if (candidate != null && (best == null || candidate.score() > best.score())) {
                best = candidate;
            }
        }
        return best;
    }
    private static ProfileMatch matchIdentity(String speaker, LocalRpProfile profile) {
        ProfileMatch best = null;
        String account = profile.accountName == null ? "" : profile.accountName.trim();
        if (!account.isEmpty()) {
            ProfileMatch m = identityMatch(speaker, account, profile, true);
            if (m != null) {
                best = m;
            }
        }
        if (profile.hasRpName()) {
            String rp = profile.rpName.trim();
            ProfileMatch m = identityMatch(speaker, rp, profile, false);
            if (m != null && (best == null || m.score() > best.score())) {
                best = m;
            }
        }
        return best;
    }
    private static ProfileMatch identityMatch(String speaker, String identity,
                                               LocalRpProfile profile, boolean account) {
        int at = tokenIndex(speaker.toLowerCase(Locale.ROOT),
                identity.toLowerCase(Locale.ROOT));
        if (at < 0) {
            return null;
        }
        String low = speaker.toLowerCase(Locale.ROOT);
        String id = identity.toLowerCase(Locale.ROOT);
        int score;
        if (low.equals(id)) {
            score = account ? 4_000 : 3_900;
        } else if (at + id.length() == low.length()) {
            score = account ? 3_000 : 2_900;
        } else {
            score = account ? 2_000 : 1_900;
        }
        return new ProfileMatch(profile, identity, at, at + identity.length(),
                score + identity.length());
    }
    private static int tokenIndex(String text, String token) {
        int from = 0;
        while (from <= text.length() - token.length()) {
            int at = text.indexOf(token, from);
            if (at < 0) {
                return -1;
            }
            int before = at - 1;
            int after = at + token.length();
            boolean left = before < 0 || !isIdentityChar(text.charAt(before));
            boolean right = after >= text.length() || !isIdentityChar(text.charAt(after));
            if (left && right) {
                return at;
            }
            from = at + 1;
        }
        return -1;
    }
    private static boolean isIdentityChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    private static String identityInside(String text, LocalRpProfile profile) {
        ProfileMatch match = matchIdentity(text, profile);
        return match == null ? null : match.visibleIdentity();
    }
    private static int identityOffset(String text, String identity) {
        if (text == null || identity == null || identity.isBlank()) {
            return -1;
        }
        return tokenIndex(text.toLowerCase(Locale.ROOT),
                identity.toLowerCase(Locale.ROOT));
    }
    private static String ownText(Text node) {
        StringBuilder sb = new StringBuilder();
        node.getContent().visit(s -> {
            sb.append(s);
            return Optional.empty();
        });
        return sb.toString();
    }
    private static Style safeStyle(Style style) {
        return style == null ? Style.EMPTY : style;
    }
}
