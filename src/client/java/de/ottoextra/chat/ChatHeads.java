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
    public static final String INDENT = "   ";

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

    /** Namenszone (lowercase) -> autoritative Sprecher-UUID aus dem Chat-Hover.
     *  Vorrangig vor der RP-Namens-Heuristik, damit der richtige Kopf erscheint. */
    private static final Map<String, UUID> ZONE_UUID = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, UUID> e) {
                    return size() > 256;
                }
            });

    private ChatHeads() {
    }

    /** Sprecher-UUID einer Namenszone merken (aus dem Hover in processChatMessage). */
    public static void mapSpeaker(String zone, UUID uuid) {
        if (zone != null && !zone.isBlank() && uuid != null) {
            ZONE_UUID.put(zoneKey(zone), uuid);
        }
    }

    private static String zoneKey(String zone) {
        return zone.toLowerCase(java.util.Locale.ROOT).trim();
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
        if (message == null || !enabled()) {
            return message;
        }
        String flat = message.getString();
        if (flat.indexOf(']') < 0 || !hasHead(flat)) {
            return message;
        }
        return insertIndent(message, INDENT, new boolean[]{false});
    }

    /** Wird für diese Zeile ein Kopf gezeichnet? (autoritative Sprecher-UUID aus
     *  dem Hover ODER RP-Namens-Heuristik). */
    private static boolean hasHead(String flat) {
        String zone = nameZone(flat);
        if (zone != null && ZONE_UUID.containsKey(zoneKey(zone))) {
            return true;
        }
        return profileForPlain(flat) != null;
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
            // Vorrang: autoritative Sprecher-UUID aus dem Hover (richtiger Kopf),
            // sonst RP-Namens-Heuristik als Fallback.
            String zone = nameZone(flat);
            UUID uuid = zone != null ? ZONE_UUID.get(zoneKey(zone)) : null;
            SkinTextures skin;
            if (uuid != null) {
                skin = skinForUuid(uuid, null);
            } else {
                LocalRpProfile profile = profileForPlain(flat);
                skin = profile != null ? skinFor(profile) : null;
            }
            if (skin == null) {
                return;
            }
            // Kopf NUR in die tatsächliche Leerzeichen-Lücke nach dem Kanal "]"
            // zeichnen (zentriert). Ist die Lücke zu klein (alter Client ohne
            // Einrückung / Format ohne Leerzeichen), gar kein Kopf statt Overlap.
            var tr = MinecraftClient.getInstance().textRenderer;
            int br = flat.indexOf(']');
            int x;
            if (br < 0) {
                x = 0;
            } else {
                int ws = br + 1;
                while (ws < flat.length() && flat.charAt(ws) == ' ') {
                    ws++;
                }
                int gapW = tr.getWidth(flat.substring(br + 1, ws));
                if (gapW < HEAD_SIZE) {
                    return;
                }
                x = tr.getWidth(flat.substring(0, br + 1)) + Math.max(0, (gapW - HEAD_SIZE) / 2);
            }
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
    public static String nameZone(String flat) {
        int br = flat.indexOf(']');
        int colon = flat.indexOf(':', br + 1);
        if (br < 0 || colon <= br) {
            return null;
        }
        String zone = flat.substring(br + 1, colon).trim();
        return zone.isEmpty() ? null : zone;
    }

    /** Skin-Textur (Kopf) für ein lokales Profil (Identität = dessen UUID). */
    public static SkinTextures skinFor(LocalRpProfile profile) {
        return profile == null ? null : skinForUuid(parseUuid(profile.uuid), profile.accountName);
    }

    /**
     * Skin-Textur (Kopf) strikt für eine UUID. Online: Skin aus der Tabliste
     * (Treffer per UUID) und signierten Skin im persistenten {@link SkinCache}
     * merken. Offline: über den Skin-Provider mit dem gecachten GameProfile
     * (echte Textur-Property) — lädt asynchron, sonst Default. {@code account}
     * nur als Fallback, wenn keine UUID vorliegt.
     */
    public static SkinTextures skinForUuid(UUID uuid, String account) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                var gp = e.getProfile();
                if (gp == null) {
                    continue;
                }
                boolean match = uuid != null && gp.id() != null
                        ? gp.id().equals(uuid)
                        : (account != null && gp.name() != null
                                && gp.name().equalsIgnoreCase(account));
                if (match) {
                    SkinCache.remember(gp);
                    return e.getSkinTextures();
                }
            }
        }
        if (uuid == null) {
            return null;
        }
        // Offline: zuerst der LOKAL gecachte Skin (eigenes PNG, kein Mojang),
        // sonst der Provider (gecachte Property), zuletzt Default.
        SkinTextures local = SkinCache.localSkin(uuid);
        if (local != null) {
            return local;
        }
        try {
            SkinTextures s = mc.getSkinProvider()
                    .supplySkinTextures(SkinCache.profileFor(uuid, account), false).get();
            if (s != null) {
                return s;
            }
        } catch (Throwable ignored) {
            // Fallback unten
        }
        return DefaultSkinHelper.getSkinTextures(uuid);
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
