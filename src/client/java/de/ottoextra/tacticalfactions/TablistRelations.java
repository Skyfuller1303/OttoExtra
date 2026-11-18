package de.ottoextra.tacticalfactions;

import com.mojang.authlib.GameProfile;
import de.ottoextra.OttoExtraGate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client-only relation grouping and coloring for the vanilla player list. */
public final class TablistRelations {
    private TablistRelations() {
    }

    public static List<PlayerListEntry> groupEntries(List<PlayerListEntry> entries) {
        if (entries == null || entries.size() < 2 || !active()) return entries;
        boolean hostilesFirst = TacticalFactions.config().tablistHostilesFirst;
        Map<PlayerListEntry, Integer> ranks = new IdentityHashMap<>();
        List<PlayerListEntry> grouped = new ArrayList<>(entries);
        grouped.sort((left, right) -> Integer.compare(
                ranks.computeIfAbsent(left, entry -> groupRank(topSquadIndex(entry), resolve(entry), hostilesFirst)),
                ranks.computeIfAbsent(right, entry -> groupRank(topSquadIndex(entry), resolve(entry), hostilesFirst))));
        return grouped;
    }

    /** Squad members of a tablist-top squad come first (in squad order), everyone else by relation. */
    static int groupRank(int topSquadIndex, Relation relation, boolean hostilesFirst) {
        return topSquadIndex >= 0 ? topSquadIndex - SQUAD_RANK_BASE : relationRank(relation, hostilesFirst);
    }

    private static final int SQUAD_RANK_BASE = 1000;

    private static int topSquadIndex(PlayerListEntry entry) {
        try {
            GameProfile profile = entry != null ? entry.getProfile() : null;
            return profile == null ? -1
                    : TacticalFactions.politics().tablistSquadIndex(profile.id(), profile.name());
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    public static Text colorName(GameProfile profile, Text original) {
        try {
            TacticalConfig config = TacticalFactions.config();
            if (profile == null || !active() || !config.politicalColors) return original;
            Resolution resolution = TacticalFactions.politics()
                    .resolveForDisplay(profile.id(), profile.name());
            Relation relation = resolution.relation();
            if (relation != Relation.ALLIED && relation != Relation.HOSTILE) return original;

            Text base = original != null ? original : Text.literal(profile.name());
            String squadHex = squadColor(resolution, profile);
            TextColor color = parseColor(squadHex != null ? squadHex
                    : relation == Relation.ALLIED ? config.allyColor : config.hostileColor);
            if (color == null) return original;
            List<String> candidates = new ArrayList<>();
            addCandidate(candidates, TacticalFactions.politics()
                    .tablistDisplayName(profile.id(), profile.name()));
            addCandidate(candidates, profile.name());
            Text colored = colorVisibleName(base, candidates, color);
            if (colored != base) return colored;
            String fallback = lastVisibleText(base);
            return fallback == null ? original
                    : colorVisibleName(base, List.of(fallback), color);
        } catch (RuntimeException ignored) {
            return original;
        }
    }

    private static String squadColor(Resolution resolution, GameProfile profile) {
        return TacticalFactions.politics().squadColor(resolution, profile.id(), profile.name());
    }

    static int relationRank(Relation relation, boolean hostilesFirst) {
        Relation resolved = relation == null || relation == Relation.INHERIT
                ? Relation.NEUTRAL : relation;
        if (resolved == Relation.NEUTRAL) return 2;
        if (hostilesFirst) return resolved == Relation.HOSTILE ? 0 : 1;
        return resolved == Relation.ALLIED ? 0 : 1;
    }

    static Text colorVisibleName(Text original, List<String> candidates, TextColor color) {
        if (original == null || color == null) return original;
        NameColorState state = new NameColorState(candidates, color);
        MutableText rewritten = rewrite(original, state);
        return state.replaced ? rewritten : original;
    }

    private static MutableText rewrite(Text node, NameColorState state) {
        TextContent content = node.getContent();
        Style style = safeStyle(node.getStyle());
        MutableText copy;
        if (content instanceof ObjectTextContent) {
            copy = MutableText.of(content).setStyle(style);
        } else if (content instanceof TranslatableTextContent translated) {
            Object[] arguments = translated.getArgs();
            Object[] rewrittenArguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                rewrittenArguments[i] = rewriteArgument(arguments[i], state, style);
            }
            copy = MutableText.of(new TranslatableTextContent(
                    translated.getKey(), translated.getFallback(), rewrittenArguments)).setStyle(style);
        } else if (content instanceof PlainTextContent plain) {
            copy = rewriteLiteral(plain.string(), style, state);
        } else {
            copy = MutableText.of(content).setStyle(style);
        }
        for (Text sibling : node.getSiblings()) copy.append(rewrite(sibling, state));
        return copy;
    }

    private static Object rewriteArgument(Object argument, NameColorState state, Style style) {
        if (argument instanceof Text text) return rewrite(text, state);
        if (argument instanceof String value) {
            Match match = findMatch(value, state);
            return match == null ? argument : rewriteMatch(value, style, state, match);
        }
        return argument;
    }

    private static MutableText rewriteLiteral(String value, Style style, NameColorState state) {
        Match match = findMatch(value, state);
        return match == null ? Text.literal(value).setStyle(style)
                : rewriteMatch(value, style, state, match);
    }

    private static MutableText rewriteMatch(String value, Style style,
                                            NameColorState state, Match match) {
        MutableText result = Text.empty().setStyle(style);
        if (match.index > 0) result.append(Text.literal(value.substring(0, match.index)).setStyle(style));
        int end = match.index + match.value.length();
        result.append(Text.literal(value.substring(match.index, end))
                .setStyle(style.withColor(state.color)));
        if (end < value.length()) result.append(Text.literal(value.substring(end)).setStyle(style));
        state.replaced = true;
        return result;
    }

    private static Match findMatch(String text, NameColorState state) {
        if (state.replaced || text == null || text.isEmpty()) return null;
        Match best = null;
        for (String candidate : state.candidates) {
            if (candidate == null || candidate.isBlank()) continue;
            int index = text.indexOf(candidate);
            if (index >= 0 && (best == null || index < best.index
                    || index == best.index && candidate.length() > best.value.length())) {
                best = new Match(index, candidate);
            }
        }
        return best;
    }

    private static void addCandidate(List<String> candidates, String value) {
        if (value == null || value.isBlank()) return;
        String clean = value.trim();
        if (!candidates.contains(clean)) candidates.add(clean);
    }

    private static String lastVisibleText(Text text) {
        LastText state = new LastText();
        collectLastVisibleText(text, state);
        return state.value;
    }

    private static void collectLastVisibleText(Text node, LastText state) {
        if (node == null) return;
        TextContent content = node.getContent();
        if (content instanceof PlainTextContent plain) {
            rememberVisibleText(plain.string(), state);
        } else if (content instanceof TranslatableTextContent translated) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Text text) collectLastVisibleText(text, state);
                else if (argument instanceof String value) rememberVisibleText(value, state);
            }
        }
        for (Text sibling : node.getSiblings()) collectLastVisibleText(sibling, state);
    }

    private static void rememberVisibleText(String value, LastText state) {
        if (value == null || value.isBlank()) return;
        String clean = value.trim();
        if (clean.codePoints().anyMatch(Character::isLetterOrDigit)) state.value = clean;
    }

    private static Relation resolve(PlayerListEntry entry) {
        try {
            GameProfile profile = entry != null ? entry.getProfile() : null;
            UUID uuid = profile != null ? profile.id() : null;
            String account = profile != null ? profile.name() : null;
            return TacticalFactions.politics().resolveForDisplay(uuid, account).relation();
        } catch (RuntimeException ignored) {
            return Relation.NEUTRAL;
        }
    }

    private static boolean active() {
        try {
            return TacticalFactions.config().combatMode
                    && OttoExtraGate.isOnOttonien(MinecraftClient.getInstance());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static TextColor parseColor(String hex) {
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) return null;
        try {
            return TextColor.fromRgb(Integer.parseInt(hex.substring(1), 16));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Style safeStyle(Style style) {
        return style == null ? Style.EMPTY : style;
    }

    private static final class NameColorState {
        private final List<String> candidates;
        private final TextColor color;
        private boolean replaced;

        private NameColorState(List<String> candidates, TextColor color) {
            this.candidates = candidates == null ? List.of() : candidates;
            this.color = color;
        }
    }

    private static final class LastText {
        private String value;
    }

    private record Match(int index, String value) {
    }
}
