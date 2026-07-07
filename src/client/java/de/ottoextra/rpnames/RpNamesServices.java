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

    /**
     * Anzeige-Form eines (Server-)Titels aus dem Katalog: {@code title} ist der
     * fixe Standardwert, die <b>Varianten</b> bestimmen die angezeigte Form.
     * Siehe {@link de.ottoextra.rpnames.title.TitleCatalogStore#displayForm}.
     * Roh-Titel, wenn kein Katalog vorhanden/kein Treffer.
     */
    public static String canonicalTitle(String raw) {
        var c = catalog;
        return c == null ? raw : c.displayForm(raw);
    }

    /** Katalog-Default-Namensfarbe (Fallback), nie null. */
    private static String catalogDefaultNameColor() {
        var cat = catalog;
        return cat != null ? cat.defaultNameColor() : "#c7a87f";
    }

    private static String titleNameColor(String title) {
        var cat = catalog;
        return cat != null ? cat.titleNameColor(title).orElse(null) : null;
    }

    /** Hat der Titel „Farbe überschreibt" gesetzt? Dann schlägt die Katalogfarbe
     *  (Titel- und Namensfarbe) den Personen-Override. */
    public static boolean titleOverridesColor(String title) {
        var cat = catalog;
        return cat != null && cat.overridesColor(title);
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * Farbkette RP-Name: Personen-Override → Titel-Namensfarbe → globale
     * RP-Namensfarbe → Katalog-Default. Einheitlich für Chat/Tab/Nametag.
     */
    public static String rpNameColor(String personOverride, String title) {
        String global = config != null ? config.globalRpNameColor : null;
        if (titleOverridesColor(title)) {
            return firstNonBlank(titleNameColor(title), personOverride, global,
                    catalogDefaultNameColor());
        }
        return firstNonBlank(personOverride, titleNameColor(title), global,
                catalogDefaultNameColor());
    }

    /**
     * Farbkette Spieler-/Accountname: Personen-Override → Titel-Namensfarbe →
     * globale Spieler-Namensfarbe → Katalog-Default.
     */
    public static String playerNameColor(String personOverride, String title) {
        String global = config != null ? config.globalPlayerNameColor : null;
        if (titleOverridesColor(title)) {
            return firstNonBlank(titleNameColor(title), personOverride, global,
                    catalogDefaultNameColor());
        }
        return firstNonBlank(personOverride, titleNameColor(title), global,
                catalogDefaultNameColor());
    }

    /** Modul initialisiert + auf Ottonien (Server-Gate)? */
    public static boolean isActive() {
        return active && store != null && config != null && config.enabled;
    }

    /** Shift-Klick (Chat/Spieler) öffnet das RP-Personenbuch? */
    public static boolean openBookOnClickEnabled() {
        return isActive() && config.openBookOnClick;
    }

    /** Proaktives Kennenlernen aktiv? */
    public static boolean proactiveMeetEnabled() {
        return isActive() && config.proactiveMeet;
    }

    /** Accounts, die im RP-Chat geredet haben und noch unbekannt sind ("!"). */
    private static final java.util.Set<String> pendingMeet =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Markiert einen Account als "im RP-Chat gehört" (Kennenlern-Marker). */
    public static void markHeardInRpChat(String account) {
        if (account != null && !account.isBlank()) {
            pendingMeet.add(account.toLowerCase(java.util.Locale.ROOT));
        }
    }

    /** Bekommt der Account ein "!" überm Kopf (gehört + noch unbekannt)? */
    public static boolean isPendingMeet(String account) {
        if (account == null || store == null
                || !pendingMeet.contains(account.toLowerCase(java.util.Locale.ROOT))) {
            return false;
        }
        de.ottoextra.rpnames.model.LocalRpProfile p = store.findByName(account).orElse(null);
        return p != null && !p.hasRpName();
    }

    /** Vom Server vorgeschlagene Identität (RP-Name + Titel) für die Kennenlern-GUI. */
    public record MeetSuggestion(String rpName, String title) {
    }

    /** Aus dem Server-Hover gelesene Vorschläge (NICHT gespeichert, nur Prefill). */
    private static final java.util.Map<String, MeetSuggestion> suggestions =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Merkt einen Server-Vorschlag (RP-Name/Titel) für einen noch unbekannten Account. */
    public static void suggestIdentity(String account, String rpName, String title) {
        if (account == null || account.isBlank() || store == null) {
            return;
        }
        boolean hasName = rpName != null && !rpName.isBlank();
        boolean hasTitle = title != null && !title.isBlank();
        if (!hasName && !hasTitle) {
            return;
        }
        suggestions.put(account.toLowerCase(java.util.Locale.ROOT),
                new MeetSuggestion(hasName ? rpName.trim() : "", hasTitle ? title.trim() : ""));
    }

    /** Server-Vorschlag für einen Account (oder null). */
    public static MeetSuggestion meetSuggestion(String account) {
        return account == null ? null
                : suggestions.get(account.toLowerCase(java.util.Locale.ROOT));
    }

    /** Sprecher-Account aus einer RP-Chat-Zeile ableiten (letztes bekanntes Wort
     *  zwischen {@code ]} und {@code :}); null, wenn nicht ermittelbar. */
    public static String speakerAccount(String plain) {
        if (store == null || plain == null) {
            return null;
        }
        int br = plain.indexOf(']');
        int colon = plain.indexOf(':', br + 1);
        if (br < 0 || colon < 0) {
            return null;
        }
        String[] words = plain.substring(br + 1, colon).trim().split("\\s+");
        for (int i = words.length - 1; i >= 0; i--) {
            if (!words[i].isBlank() && store.findByName(words[i]).isPresent()) {
                return words[i];
            }
        }
        return null;
    }

    /**
     * Aktueller Server-Titel eines Online-Spielers aus der Tabliste (Original-
     * Displayname, vor unserer Anpassung). null, wenn offline/nicht ermittelbar.
     */
    public static String serverTitleFor(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            return null;
        }
        for (net.minecraft.client.network.PlayerListEntry entry
                : mc.getNetworkHandler().getPlayerList()) {
            var p = entry.getProfile();
            if (p == null || p.name() == null || !account.equalsIgnoreCase(p.name())) {
                continue;
            }
            net.minecraft.text.Text raw =
                    ((de.ottoextra.mixin.PlayerListEntryAccessor) (Object) entry)
                            .ottoextra$rawDisplayName();
            if (raw == null) {
                return null;
            }
            String flat = raw.getString();
            int idx = flat.indexOf(p.name());
            return idx > 0 ? flat.substring(0, idx).trim() : "";
        }
        return null;
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
        // Hover-Debug läuft VOR dem Aktiv-Gate, damit auch bei inaktivem
        // Modul sichtbar ist, was der Server schickt.
        de.ottoextra.rpnames.chat.HoverDebug.dump(message);
        if (!isActive() || message == null) {
            return message;
        }
        try {
            String plain = message.getString();
            OttoChatChannel channel = OttoChatChannel.fromMessage(plain);
            // Proaktives Kennenlernen: unbekannte RP-Sprecher als "gehört" markieren
            if (channel.isRpSpeak() && proactiveMeetEnabled()) {
                markHeardInRpChat(speakerAccount(plain));
            }
            // Bei JEDER Nachricht Titel gegen die lokale Datei abgleichen
            // (updateTitleIfChanged trifft nur echte Accounts; nur locked schützt).
            // RP-Namen lernen nur auf den dafür vorgesehenen Kanälen.
            // Proaktiv: RP-Namen NICHT automatisch lernen (nur manuell via Kennenlernen).
            boolean learn = channel.shouldLearn() && !proactiveMeetEnabled();
            boolean meet = proactiveMeetEnabled();
            // Server-Format (2026-07): RP-Kanäle hovern Titel+ACCOUNT (sichtbar
            // ist der RP-Name), OOC-Kanäle hovern Titel+RP-NAME.
            java.util.List<HoverIdentityParser.ParsedIdentity> ids =
                    hoverParser.parseMessage(message, channel.isRpSpeak());
            de.ottoextra.rpnames.chat.HoverDebug.logParsed(ids);
            for (HoverIdentityParser.ParsedIdentity id : ids) {
                // ROH-Titel speichern; die Anzeige-Form (Varianten-Override) wird
                // live beim Rendern aufgelöst.
                if (learn) {
                    store.learnIdentity(id.accountName(), id.rpName(), id.title(),
                            id.titleGroup(), RpNameSource.LEARNED_FROM_HOVER);
                }
                // Proaktiv: Server-Identität nur als Vorschlag merken (Prefill GUI).
                // Account kommt aus dem Hover — zuverlässiger als speakerAccount
                // (Plaintext trägt in RP-Kanälen den RP-Namen, nicht den Account).
                if (meet) {
                    suggestIdentity(id.accountName(), id.rpName(), id.title());
                    if (channel.isRpSpeak()) {
                        markHeardInRpChat(id.accountName());
                    }
                }
                store.updateTitleIfChanged(id.accountName(), null, id.title());
            }
            Text result;
            if (!channel.shouldReplace(config)) {
                // OOC: RP-Name bleibt, aber den Titel trotzdem voranstellen
                result = channel.isOoc() ? rewriter.rewriteTitleOnly(message, config) : message;
            } else {
                result = rewriter.rewrite(message, config);
            }
            return result;
        } catch (Throwable t) {
            return message;
        }
    }

}
