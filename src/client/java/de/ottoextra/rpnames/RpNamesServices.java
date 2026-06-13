package de.ottoextra.rpnames;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.chat.ChatNameRewriter;
import de.ottoextra.rpnames.chat.HoverIdentityParser;
import de.ottoextra.rpnames.chat.OttoChatChannel;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.text.Text;

/**
 * Statischer Zugriffspunkt des RP-Namen-Moduls (Muster: RegionsServices) —
 * Mixins und Renderer greifen hierüber zu. Lebenszyklus: {@link RpNamesModule}.
 */
public final class RpNamesServices {

    private static volatile LocalRpIdentityStore store;
    private static volatile TitleRegistry titles;
    private static volatile de.ottoextra.rpnames.title.TitleCatalogStore catalog;
    private static volatile HoverIdentityParser hoverParser;
    private static volatile ChatNameRewriter rewriter;
    private static volatile OttoExtraConfig.RpNames config;
    private static volatile boolean active = false;

    private RpNamesServices() {
    }

    static void init(OttoExtraConfig.RpNames cfg) {
        config = cfg;
        store = new LocalRpIdentityStore();
        titles = new TitleRegistry();
        store.load();
        titles.load();
        catalog = new de.ottoextra.rpnames.title.TitleCatalogStore();
        catalog.load();
        hoverParser = new HoverIdentityParser(titles);
        rewriter = new ChatNameRewriter(store, titles);
    }

    static void setActive(boolean value) {
        active = value;
    }

    static void shutdown() {
        LocalRpIdentityStore s = store;
        if (s != null) {
            s.shutdown();
        }
        active = false;
    }

    public static LocalRpIdentityStore store() {
        return store;
    }

    public static TitleRegistry titles() {
        return titles;
    }

    public static de.ottoextra.rpnames.title.TitleCatalogStore catalog() {
        return catalog;
    }

    public static OttoExtraConfig.RpNames config() {
        return config;
    }

    /** Modul initialisiert + auf Ottonien (Server-Gate)? */
    public static boolean isActive() {
        return active && store != null && config != null && config.enabled;
    }

    /** Shift-Klick (Chat/Spieler) öffnet das RP-Personenbuch? */
    public static boolean openBookOnClickEnabled() {
        return isActive() && config.openBookOnClick;
    }

    /**
     * Profil per Account- ODER RP-Name finden (für Chat-Klick: sichtbarer Name
     * kann beides sein). Account-Treffer hat Vorrang.
     */
    public static de.ottoextra.rpnames.model.LocalRpProfile findProfileByAnyName(String name) {
        if (store == null || name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        var byAccount = store.findByName(trimmed);
        if (byAccount.isPresent()) {
            return byAccount.get();
        }
        // Klick-Style trägt oft "Titel RP-Name" (aus dem Hover) — daher auch
        // Suffix-Treffer auf rpName/Account zulassen, längster Treffer gewinnt.
        String low = trimmed.toLowerCase(java.util.Locale.ROOT);
        de.ottoextra.rpnames.model.LocalRpProfile best = null;
        int bestLen = 0;
        for (de.ottoextra.rpnames.model.LocalRpProfile p : store.all()) {
            int len = matchLen(low, p.rpName);
            if (len == 0) {
                len = matchLen(low, p.accountName);
            }
            if (len > bestLen) {
                bestLen = len;
                best = p;
            }
        }
        return best;
    }

    /** Länge des Treffers, wenn {@code candidate} gleich {@code low} ist oder als
     *  letztes Wort darin endet (" name"); sonst 0. */
    private static int matchLen(String low, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return 0;
        }
        String c = candidate.trim().toLowerCase(java.util.Locale.ROOT);
        return low.equals(c) || low.endsWith(" " + c) ? c.length() : 0;
    }

    /**
     * Anzeige für unbekannten RP-Namen: Accountname oder
     * konfigurierter Platzhalter ("Unbekannt", "???", ...). Gilt einheitlich
     * für Chat, Tabliste und Namensschild.
     */
    public static String unknownDisplay(String accountName) {
        OttoExtraConfig.RpNames cfg = config;
        if (cfg == null) {
            return accountName;
        }
        if (cfg.unknownShowAccount && accountName != null && !accountName.isBlank()) {
            return accountName;
        }
        return cfg.unknownPlaceholder == null || cfg.unknownPlaceholder.isBlank()
                ? "Unbekannt" : cfg.unknownPlaceholder;
    }

    /** Zeigt die Unbekannt-Anzeige gerade den Accountnamen? (Farbwahl) */
    public static boolean unknownShowsAccount() {
        return config != null && config.unknownShowAccount;
    }

    /**
     * ChatHud-Hook: erst lernen (RP-Kanäle + Hilfe), dann je Kanal-Policy
     * ersetzen. Gibt die ggf. umgebaute Nachricht zurück; bei jedem Zweifel
     * das Original.
     */
    public static Text processChatMessage(Text message) {
        if (!isActive() || message == null) {
            return message;
        }
        try {
            String plain = message.getString();
            OttoChatChannel channel = OttoChatChannel.fromMessage(plain);
            if (channel.shouldLearn()) {
                for (HoverIdentityParser.ParsedIdentity id : hoverParser.parseMessage(message)) {
                    store.learnIdentity(id.accountName(), id.rpName(), id.title(),
                            id.titleGroup(), RpNameSource.LEARNED_FROM_HOVER);
                }
            }
            if (!channel.shouldReplace(config)) {
                return message;
            }
            return rewriter.rewrite(message, config);
        } catch (Throwable t) {
            return message;
        }
    }
}
