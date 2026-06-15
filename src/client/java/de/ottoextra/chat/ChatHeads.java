package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hilfen für die optionalen Spielerköpfe im Chat: Sprecher einer Zeile auflösen,
 * Skin (Kopf) liefern, Platz zwischen Kanal und Titel schaffen und den Kopf dort
 * zeichnen. Reine Lese-/Zeichen-Logik — bei jedem Zweifel kein Kopf, nie Crash.
 */
public final class ChatHeads {

    /** Quadratische Kopfgröße in px (eine Chat-Zeile ist ~9px hoch). */
    public static final int HEAD_SIZE = 8;
    /** Einrückung (Leerzeichen) nach dem Kanal — schafft Platz fürs Wappen. */
    public static final String INDENT = "  ";

    /** Negativ-Sentinel im Cache (Zone ohne Treffer). */
    private static final LocalRpProfile NONE = new LocalRpProfile();
    /** zone ("Titel Name") -> Profil bzw. NONE; klein gehalten, Zugriffs-LRU. */
    private static final Map<String, LocalRpProfile> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, LocalRpProfile> e) {
                    return size() > 128;
                }
            });

    private ChatHeads() {
    }

    /** Spielerköpfe im Chat aktiv? */
    public static boolean enabled() {
        OttoExtraConfig.Chat cfg = chatConfig();
        return cfg != null && cfg.enabled && cfg.showPlayerHeads;
    }

    private static OttoExtraConfig.Chat chatConfig() {
        OttoExtraConfig c = OttoExtraConfig.active();
        return c != null ? c.chat : null;
    }

    /** Plain-String einer Chat-Zeile (OrderedText -> String). */
    public static String plain(OrderedText line) {
        if (line == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    /**
     * Fügt einer Chat-Nachricht hinter dem Kanal ("[…]") eine kleine Einrückung
     * ein, wenn ein Kopf gezeichnet wird — so steht das Wappen zwischen Kanal und
     * Titel. Styling bleibt erhalten. Ohne Kanal/Treffer unverändert.
     */
    public static Text withHeadIndent(Text message) {
        if (message == null || !enabled() || profileForPlain(message.getString()) == null) {
            return message;
        }
        String flat = message.getString();
        if (flat.indexOf(']') < 0) {
            return message;
        }
        return insertIndent(message, INDENT, new boolean[]{false});
    }

    /**
     * Zeichnet den Kopf des Sprechers einer Chat-Zeile in die Lücke hinter dem
     * Kanal (x = Breite von "[Kanal]"), im bereits skalierten Pose des
     * ChatHud-Backends. No-op ohne Treffer/Skin. Nie ein Crash.
     */
    public static void drawHead(net.minecraft.client.gui.DrawContext ctx, int y, float opacity,
                                OrderedText line) {
        try {
            if (ctx == null || !enabled()) {
                return;
            }
            String flat = plain(line);
            LocalRpProfile profile = profileForPlain(flat);
            if (profile == null) {
                return;
            }
            SkinTextures skin = skinFor(profile);
            if (skin == null) {
                return;
            }
            int br = flat.indexOf(']');
            int x = br >= 0
                    ? MinecraftClient.getInstance().textRenderer.getWidth(flat.substring(0, br + 1)) + 1
                    : 0;
            int color = net.minecraft.util.math.ColorHelper.withAlpha(opacity, 0xFFFFFF);
            net.minecraft.client.gui.PlayerSkinDrawer.draw(ctx, skin, x, y, HEAD_SIZE, color);
        } catch (Throwable ignored) {
            // Chat darf nie crashen
        }
    }

    /** Sprecher-Profil zu einer Chat-Zeile, oder null. */
    public static LocalRpProfile profileForLine(OrderedText line) {
        return profileForPlain(plain(line));
    }

    /**
     * Sprecher-Profil zu einer Chat-Zeile: nimmt die Namenszone zwischen "]" und
     * ":" und löst sie über Account ODER RP-Name auf (so greift auch der eigene
     * Spieler, dessen Account nicht im sichtbaren Text steht). Folgezeilen ohne
     * "]…:" liefern null (kein doppelter Kopf). Auflösung wird je Zone gecacht.
     */
    public static LocalRpProfile profileForPlain(String flat) {
        if (flat == null || RpNamesServices.store() == null) {
            return null;
        }
        String zone = nameZone(flat);
        if (zone == null) {
            return null;
        }
        LocalRpProfile cached = CACHE.get(zone);
        if (cached != null) {
            return cached == NONE ? null : cached;
        }
        LocalRpProfile resolved = RpNamesServices.findProfileByAnyName(zone);
        CACHE.put(zone, resolved == null ? NONE : resolved);
        return resolved;
    }

    /** "Titel Name" zwischen erstem "]" und dem folgenden ":", oder null. */
    private static String nameZone(String flat) {
        int br = flat.indexOf(']');
        int colon = flat.indexOf(':', br + 1);
        if (br < 0 || colon <= br) {
            return null;
        }
        String zone = flat.substring(br + 1, colon).trim();
        return zone.isEmpty() ? null : zone;
    }

    /** Skin-Textur (Kopf) für ein Profil: Online-Tabliste bevorzugt, sonst
     *  Default-Skin aus der UUID. Null, wenn nicht ermittelbar. */
    public static SkinTextures skinFor(LocalRpProfile profile) {
        if (profile == null) {
            return null;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null && profile.accountName != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                var gp = e.getProfile();
                if (gp != null && gp.name() != null
                        && gp.name().equalsIgnoreCase(profile.accountName)) {
                    return e.getSkinTextures();
                }
            }
        }
        UUID uuid = parseUuid(profile.uuid);
        return uuid != null ? DefaultSkinHelper.getSkinTextures(uuid) : null;
    }

    /** Einrückung {@code indent} einmalig hinter dem ersten "]" einfügen,
     *  Styling der Komponenten erhalten. */
    private static MutableText insertIndent(Text node, String indent, boolean[] done) {
        StringBuilder sb = new StringBuilder();
        node.getContent().visit(s -> {
            sb.append(s);
            return java.util.Optional.empty();
        });
        String own = sb.toString();
        Style style = node.getStyle();
        MutableText copy;
        int idx = done[0] ? -1 : own.indexOf(']');
        if (idx >= 0) {
            copy = Text.empty().setStyle(style);
            copy.append(Text.literal(own.substring(0, idx + 1)).setStyle(style));
            copy.append(Text.literal(indent));
            if (idx + 1 < own.length()) {
                copy.append(Text.literal(own.substring(idx + 1)).setStyle(style));
            }
            done[0] = true;
        } else {
            copy = MutableText.of(node.getContent()).setStyle(style);
        }
        for (Text sibling : node.getSiblings()) {
            copy.append(insertIndent(sibling, indent, done));
        }
        return copy;
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
