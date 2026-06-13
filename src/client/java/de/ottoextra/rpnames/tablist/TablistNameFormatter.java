package de.ottoextra.rpnames.tablist;

import com.mojang.authlib.GameProfile;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * Tablisten-Anzeigename: es wird NUR der Accountname im
 * Original-Displaynamen durch den RP-Namen ersetzt — Server-Präfixe (z. B.
 * live gesetzte Titel) bleiben unangetastet, ein spontaner Titelwechsel auf
 * dem Server ist damit sofort sichtbar.
 *
 * <p>Optional ({@code tablistShowTitle}) wird der lokal bekannte Titel
 * vorangestellt, aber nur, wenn der Server-Text ihn nicht ohnehin enthält.
 * Namensfarbe: Spieler-Override, sonst Standard {@code defaultNameColor}
 * (#c7a87f) für alle Ränge. Liefert null, wenn nichts ersetzt werden soll.</p>
 */
public final class TablistNameFormatter {

    private static final String UNKNOWN_COLOR = "#8A8A8A";

    private TablistNameFormatter() {
    }

    public static Text format(GameProfile gameProfile, Text original) {
        if (!RpNamesServices.isActive() || gameProfile == null) {
            return null;
        }
        OttoExtraConfig.RpNames cfg = RpNamesServices.config();
        if (!cfg.tablistEnabled && !cfg.tablistTitlesAlways) {
            return null;
        }
        LocalRpProfile profile = RpNamesServices.store()
                .find(gameProfile.id() != null ? gameProfile.id().toString() : null,
                        gameProfile.name())
                .orElse(null);
        String account = gameProfile.name();
        var catalogRef = RpNamesServices.catalog();
        String defaultName = catalogRef != null ? catalogRef.defaultNameColor() : "#c7a87f";

        if (profile == null || !profile.showInTablist) {
            // Kein lokaler Eintrag/ausgeblendet: Spielername trotzdem in
            // Standardfarbe (#c7a87f) — kein API-Leak, nur Färbung
            Text base = original != null ? original : Text.literal(account);
            boolean[] tinted = {false};
            MutableText t = rebuildReplacing(base, account, account, defaultName, tinted);
            return tinted[0] ? t : null;
        }

        // RP-Namen aus: nur Titel voranstellen (mit Override-Farbe)
        if (!cfg.tablistEnabled) {
            Text base = original != null ? original : Text.literal(account);
            if (profile.hasTitle()) {
                // Eigenen Titel (Override-Farbe) + Accountname rendern, Server-Titel
                // ersetzen -> Titelfarbe greift auch ohne RP-Namen
                String nameColor = firstNonBlank(profile.colors.tabNameColor, defaultName);
                return Text.empty().append(titlePrefix(profile))
                        .append(colored(account, nameColor));
            }
            boolean[] tinted = {false};
            MutableText coloredBase = rebuildReplacing(base, account, account, defaultName, tinted);
            return tinted[0] ? coloredBase : null;
        }
        String replacement;
        String color;
        if (profile.hasRpName()) {
            var catalog = RpNamesServices.catalog();
            replacement = profile.rpName;
            color = firstNonBlank(profile.colors.tabNameColor,
                    catalog != null ? catalog.defaultNameColor() : "#c7a87f");
        } else {
            // Unbekannt: Accountname (#c7a87f) oder Platzhalter (grau), einstellbar
            replacement = RpNamesServices.unknownDisplay(account);
            color = RpNamesServices.unknownShowsAccount() ? defaultName : UNKNOWN_COLOR;
        }

        Text base = original != null ? original : Text.literal(account);
        // Eigener Titel vorhanden: Titel (Override-Farbe) + Name komplett selbst
        // rendern und den Server-Titel ersetzen -> unsere Titelfarbe greift im Tab.
        if (cfg.tablistShowTitle && profile.hasTitle()) {
            return Text.empty().append(titlePrefix(profile))
                    .append(colored(replacement, color));
        }
        boolean[] replaced = {false};
        MutableText rebuilt = rebuildReplacing(base, account, replacement, color, replaced);
        if (!replaced[0] && profile.hasRpName()) {
            // Server zeigt bereits den RP-Namen -> diesen einfärben
            rebuilt = rebuildReplacing(base, profile.rpName, profile.rpName, color, replaced);
        }
        if (!replaced[0]) {
            return null; // weder Account- noch RP-Name im Original -> nichts erfinden
        }
        return rebuilt;
    }

    /**
     * Zeigt der Server bereits einen Titel/Prefix vor dem Namen? Erkannt am
     * nicht-leeren Text VOR dem ersten Vorkommen von Account- bzw. RP-Name im
     * Original. So wird ein abweichender (importierter) Titel nicht zusätzlich
     * vorangestellt.
     */
    private static boolean serverShowsTitle(Text base, String account, String rpName) {
        if (base == null) {
            return false;
        }
        String flat = base.getString();
        int idx = account != null ? flat.indexOf(account) : -1;
        if (idx < 0 && rpName != null && !rpName.isBlank()) {
            idx = flat.indexOf(rpName);
        }
        if (idx <= 0) {
            return false; // Name nicht gefunden oder steht ganz vorne -> kein Prefix
        }
        return !flat.substring(0, idx).trim().isEmpty();
    }

    /** Gefärbter Titel-Prefix nach Farbkette (Override -> Katalog -> Gruppe). */
    private static MutableText titlePrefix(LocalRpProfile profile) {
        var catalog = RpNamesServices.catalog();
        String catalogColor = catalog != null
                ? catalog.titleColor(profile.title).orElse(null) : null;
        String groupColor = RpNamesServices.titles().find(profile.title)
                .map(r -> r.group().titleColor).orElse(null);
        String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";
        return colored(profile.title + " ",
                firstNonBlank(profile.colors.tabTitleColor,
                        firstNonBlank(catalogColor, firstNonBlank(groupColor, fallback))));
    }

    /**
     * Komponentenbaum kopieren; Vorkommen von {@code account} im eigenen Text
     * eines Knotens werden (Substring-genau) durch den gefärbten RP-Namen
     * ersetzt, Prefix/Suffix behalten den Knoten-Style.
     */
    private static MutableText rebuildReplacing(Text node, String account,
                                                String replacement, String color,
                                                boolean[] replaced) {
        StringBuilder sb = new StringBuilder();
        node.getContent().visit(s -> {
            sb.append(s);
            return java.util.Optional.empty();
        });
        String own = sb.toString();
        Style style = node.getStyle();
        MutableText copy;
        int idx = own.indexOf(account);
        if (idx >= 0) {
            copy = Text.empty().setStyle(style);
            if (idx > 0) {
                copy.append(Text.literal(own.substring(0, idx)));
            }
            copy.append(colored(replacement, color));
            int end = idx + account.length();
            if (end < own.length()) {
                copy.append(Text.literal(own.substring(end)));
            }
            replaced[0] = true;
        } else {
            copy = MutableText.of(node.getContent()).setStyle(style);
        }
        for (Text sibling : node.getSiblings()) {
            copy.append(rebuildReplacing(sibling, account, replacement, color, replaced));
        }
        return copy;
    }

    private static MutableText colored(String text, String hex) {
        MutableText t = Text.literal(text);
        TextColor color = de.ottoextra.rpnames.chat.ChatNameRewriter.parseColor(hex);
        if (color != null) {
            t.setStyle(Style.EMPTY.withColor(color));
        }
        return t;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
