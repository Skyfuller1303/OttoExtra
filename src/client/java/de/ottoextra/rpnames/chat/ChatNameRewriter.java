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
            // Sprecher (Account-Knoten mit bekanntem Profil) suchen
            SpeakerInfo si = findSpeaker(message, cfg, new StringBuilder(), false);
            if (si != null) {
                return collapseSpaces(rebuild(message, si, new int[]{0}, false), new boolean[]{false});
            }
            return plainFallback(message, cfg).orElse(message);
        } catch (Throwable t) {
            return message; // Chat darf nie brechen
        }
    }

    /**
     * Nur den Titel voranstellen (OOC-Kanäle): RP-Name/Account bleibt unverändert,
     * lediglich der lokale Titel (mit Override-Farbe) wird vor dem Sprecher ergänzt.
     */
    public Text rewriteTitleOnly(Text message, OttoExtraConfig.RpNames cfg) {
        try {
            SpeakerInfo si = findSpeaker(message, cfg, new StringBuilder(), true);
            if (si != null && si.profile().hasTitle()) {
                return collapseSpaces(rebuild(message, si, new int[]{0}, true), new boolean[]{false});
            }
            return message;
        } catch (Throwable t) {
            return message;
        }
    }

    /** Gefundener Sprecher + Position seines Account-Knotens im Flach-Text. */
    private record SpeakerInfo(Text node, LocalRpProfile profile, int accountStart,
                               int titleZoneStart) {
    }

    /**
     * Erster Knoten, dessen Eigentext einem bekannten Profil entspricht (Sprecher).
     * {@code flat} = Pre-Order-Konkatenation der Eigentexte davor → liefert die
     * Startposition + den Beginn der Server-Titel-Zone (nach der letzten {@code ]}).
     */
    private SpeakerInfo findSpeaker(Text node, OttoExtraConfig.RpNames cfg, StringBuilder flat,
                                    boolean titleOnly) {
        String own = ownText(node);
        String trimmed = own.trim();
        if (!trimmed.isEmpty()) {
            LocalRpProfile p = store.findByName(trimmed).orElse(null);
            boolean match = p != null && p.showInChat && (titleOnly
                    ? p.hasTitle()
                    : (p.hasRpName() || cfg.showUnknownAsUnknown));
            if (match) {
                int bracket = flat.lastIndexOf("]");
                return new SpeakerInfo(node, p, flat.length(), bracket >= 0 ? bracket + 1 : 0);
            }
        }
        flat.append(own);
        for (Text sibling : node.getSiblings()) {
            SpeakerInfo r = findSpeaker(sibling, cfg, flat, titleOnly);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    // ---- Komponenten-Rebuild ---------------------------------------------------

    /**
     * Baut die Nachricht neu: Account-Knoten → unser Titel (Override-Farbe) + RP-Name;
     * den Server-Titel davor (Titel-Zone zwischen {@code ]} und Sprecher) blanken wir,
     * damit kein doppelter Titel entsteht und unsere Titelfarbe greift. {@code flatPos}
     * spiegelt die Positionen aus {@link #findSpeaker}.
     */
    private Text rebuild(Text node, SpeakerInfo si, int[] flatPos, boolean titleOnly) {
        String own = ownText(node);
        int start = flatPos[0];
        MutableText copy;
        boolean known = si.profile().hasRpName();
        // Titel nur bei bekannten Personen rendern; Server-Titel bei Unbekannten
        // (oder wenn wir einen eigenen rendern) ausblenden -> kein Titel für Fremde.
        boolean renderOurTitle = known && si.profile().hasTitle();
        boolean blankServerTitle = !known || si.profile().hasTitle();
        if (node == si.node()) {
            if (titleOnly) {
                // nur Titel voranstellen, Original-Name (Account/Nick) behalten
                MutableText out = Text.empty().setStyle(node.getStyle());
                MutableText title = titleComponent(si.profile());
                if (title != null) {
                    out.append(title);
                }
                out.append(MutableText.of(node.getContent()).setStyle(node.getStyle()));
                copy = out;
            } else {
                copy = displayName(si.profile(), node.getStyle(), renderOurTitle);
            }
        } else if (blankServerTitle && !own.trim().isEmpty()
                && start >= si.titleZoneStart() && start + own.length() <= si.accountStart()) {
            // Server-Titel-Zone leeren (auch im OOC-/titleOnly-Modus -> kein Doppeltitel)
            copy = Text.empty().setStyle(node.getStyle());
        } else {
            copy = MutableText.of(node.getContent()).setStyle(node.getStyle());
        }
        flatPos[0] += own.length();
        for (Text sibling : node.getSiblings()) {
            copy.append(rebuild(sibling, si, flatPos, titleOnly));
        }
        return copy;
    }

    /**
     * Kollabiert aufeinanderfolgende Leerzeichen (auch über Knotengrenzen) auf
     * eins — entfernt die Lücke, die beim Leeren der Server-Titel-Zone entsteht.
     * Style/Hover bleiben erhalten ({@link #rebuild} hat bereits literalisiert).
     */
    private Text collapseSpaces(Text node, boolean[] lastSpace) {
        String own = ownText(node);
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
        MutableText copy = Text.literal(sb.toString())
                .setStyle(node.getStyle() == null ? Style.EMPTY : node.getStyle());
        for (Text sibling : node.getSiblings()) {
            copy.append(collapseSpaces(sibling, lastSpace));
        }
        return copy;
    }

    /**
     * Titel + RP-Name als gefärbte Komponente; Basis-Style (inkl. Hover) bleibt.
     * Farbkette: Spieler-Override → Titelkatalog (Override/Kategorie)
     * → Titelgruppe (Legacy) → Standard-Namensfarbe.
     */
    private MutableText displayName(LocalRpProfile profile, Style baseStyle) {
        return displayName(profile, baseStyle, true);
    }

    /** Gefärbter Titel-Prefix ("Titel ") nach Farbkette, oder null wenn kein Titel. */
    private MutableText titleComponent(LocalRpProfile profile) {
        if (!profile.hasTitle()) {
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
        String titleColor = firstNonBlank(profile.colors.chatTitleColor,
                firstNonBlank(catalogColor, firstNonBlank(groupTitleColor, fallback)));
        // Angezeigten Titel auf den Katalog-Kanon abbilden (umbenannte Titel
        // greifen so auch im Chat).
        return colored(de.ottoextra.rpnames.RpNamesServices.canonicalTitle(profile.title) + " ",
                titleColor);
    }

    private MutableText displayName(LocalRpProfile profile, Style baseStyle, boolean includeTitle) {
        MutableText out = Text.empty().setStyle(baseStyle == null ? Style.EMPTY : baseStyle);
        if (includeTitle) {
            MutableText title = titleComponent(profile);
            if (title != null) {
                out.append(title);
            }
        }
        // Namen-Farbkette: Personen-Override -> Titel-Namensfarbe -> global -> Katalog.
        // Unbekannt -> Accountname (Spieler-Farbkette) oder Platzhalter (grau).
        String name;
        String nameColor;
        if (profile.hasRpName()) {
            name = profile.rpName;
            nameColor = de.ottoextra.rpnames.RpNamesServices.rpNameColor(
                    profile.colors.chatNameColor, profile.title);
        } else {
            name = de.ottoextra.rpnames.RpNamesServices.unknownDisplay(profile.accountName);
            nameColor = de.ottoextra.rpnames.RpNamesServices.unknownShowsAccount()
                    ? de.ottoextra.rpnames.RpNamesServices.playerNameColor(null, profile.title)
                    : "#8A8A8A";
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
