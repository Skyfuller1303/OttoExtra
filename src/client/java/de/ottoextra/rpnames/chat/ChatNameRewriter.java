package de.ottoextra.rpnames.chat;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Optional;

/**
 * Ersetzt Accountnamen im Chat durch Titel + RP-Name.
 *
 * <p>Primär: Komponenten-Rebuild — nur Knoten, deren eigener Text exakt dem
 * Accountnamen eines bekannten Profils entspricht, werden ersetzt; Style und
 * HoverEvent des Knotens bleiben erhalten. Fallback für reine Textzeilen:
 * Sprecherbereich zwischen {@code ]} und erstem {@code :}. Bei unklarem
 * Format bleibt die Originalnachricht unverändert.</p>
 */
public final class ChatNameRewriter {

    private final LocalRpIdentityStore store;
    private final TitleRegistry titles;

    public ChatNameRewriter(LocalRpIdentityStore store, TitleRegistry titles) {
        this.store = store;
        this.titles = titles;
    }

    public Text rewrite(Text message, OttoExtraConfig.RpNames cfg) {
        try {
            boolean[] replaced = {false};
            Text rebuilt = rebuild(message, cfg, replaced);
            if (replaced[0]) {
                return rebuilt;
            }
            return plainFallback(message, cfg).orElse(message);
        } catch (Throwable t) {
            return message; // Chat darf nie brechen
        }
    }

    // ---- Komponenten-Rebuild ---------------------------------------------------

    private Text rebuild(Text node, OttoExtraConfig.RpNames cfg, boolean[] replaced) {
        String own = ownText(node).trim();
        MutableText copy;
        LocalRpProfile profile = own.isEmpty() ? null : store.findByName(own).orElse(null);
        if (profile != null && profile.showInChat
                && (profile.hasRpName() || cfg.showUnknownAsUnknown)) {
            copy = displayName(profile, node.getStyle());
            replaced[0] = true;
        } else {
            copy = MutableText.of(node.getContent()).setStyle(node.getStyle());
        }
        for (Text sibling : node.getSiblings()) {
            copy.append(rebuild(sibling, cfg, replaced));
        }
        return copy;
    }

    /**
     * Titel + RP-Name als gefärbte Komponente; Basis-Style (inkl. Hover) bleibt.
     * Farbkette: Spieler-Override → Titelkatalog (Override/Kategorie)
     * → Titelgruppe (Legacy) → Standard-Namensfarbe.
     */
    private MutableText displayName(LocalRpProfile profile, Style baseStyle) {
        MutableText out = Text.empty().setStyle(baseStyle == null ? Style.EMPTY : baseStyle);
        var catalog = de.ottoextra.rpnames.RpNamesServices.catalog();
        if (profile.hasTitle()) {
            String groupTitleColor = null;
            Optional<TitleRegistry.ResolvedTitle> resolved = titles.find(profile.title);
            if (resolved.isPresent()) {
                groupTitleColor = resolved.get().group().titleColor;
            }
            String catalogColor = catalog != null
                    ? catalog.titleColor(profile.title).orElse(null) : null;
            String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";
            String titleColor = firstNonBlank(profile.colors.chatTitleColor,
                    firstNonBlank(catalogColor, firstNonBlank(groupTitleColor, fallback)));
            out.append(colored(profile.title + " ", titleColor));
        }
        // Namen: rangunabhängig Standardfarbe #c7a87f, nur Spieler-Override schlägt;
        // Unbekannt -> Accountname (#c7a87f) oder Platzhalter (grau), einstellbar
        String defaultName = catalog != null ? catalog.defaultNameColor() : "#c7a87f";
        String name;
        String nameColor;
        if (profile.hasRpName()) {
            name = profile.rpName;
            nameColor = firstNonBlank(profile.colors.chatNameColor, defaultName);
        } else {
            name = de.ottoextra.rpnames.RpNamesServices.unknownDisplay(profile.accountName);
            nameColor = de.ottoextra.rpnames.RpNamesServices.unknownShowsAccount()
                    ? defaultName : "#8A8A8A";
        }
        out.append(colored(name, nameColor));
        return out;
    }

    private static MutableText colored(String text, String hex) {
        MutableText t = Text.literal(text);
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
            return TextColor.fromRgb(Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String ownText(Text node) {
        StringBuilder sb = new StringBuilder();
        node.getContent().visit(s -> {
            sb.append(s);
            return Optional.empty();
        });
        return sb.toString();
    }

    // ---- Plain-Fallback ([Kanal] Name: Nachricht) -------------------------------

    private Optional<Text> plainFallback(Text message, OttoExtraConfig.RpNames cfg) {
        String plain = message.getString();
        if (plain.isEmpty() || plain.charAt(0) != '[') {
            return Optional.empty();
        }
        int bracketEnd = plain.indexOf(']');
        if (bracketEnd < 0) {
            return Optional.empty();
        }
        int colon = plain.indexOf(':', bracketEnd);
        if (colon < 0) {
            return Optional.empty();
        }
        String speaker = plain.substring(bracketEnd + 1, colon).trim();
        LocalRpProfile profile = store.findByName(speaker).orElse(null);
        if (profile == null || !profile.showInChat
                || (!profile.hasRpName() && !cfg.showUnknownAsUnknown)) {
            return Optional.empty();
        }
        MutableText out = Text.literal(plain.substring(0, bracketEnd + 1) + " ");
        out.append(displayName(profile, Style.EMPTY));
        out.append(Text.literal(plain.substring(colon)));
        return Optional.of(out);
    }
}
