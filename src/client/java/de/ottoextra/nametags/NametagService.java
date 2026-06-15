package de.ottoextra.nametags;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.chat.ChatNameRewriter;
import de.ottoextra.rpnames.model.LocalRpProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Namensschild-Logik (Port aus OttoNames):
 * <ul>
 *   <li>Sichtbarkeit: {@link NameTagMode} — REALISTIC = Drei-Punkt-Sichtlinien-
 *       Raycast (Augen/Oberkörper/Mitte), HIDE_ALL = aus; pro Spieler
 *       {@code showInNametag} aus dem RP-Store.</li>
 *   <li>Inhalt: Titel-Zeile ÜBER dem Namen + RP-Name statt Accountname,
 *       Farbkette wie Chat/Tab (Override → Katalog → Gruppe; Name #c7a87f).
 *       Einzeln schaltbar: showTitle / showRpName / showPlayerName.</li>
 * </ul>
 * Mixins rufen nur hierher; jede Exception fällt auf Vanilla zurück.
 */
public final class NametagService {

    /** Label-Zeilen (Titel / RP-Name / Accountname); title und account dürfen null sein. */
    public record Lines(Text title, Text name, Text account) {
    }

    private static volatile OttoExtraConfig.Nametags config;

    /**
     * Accountname je RenderState — NICHT in {@code state.playerName} schreiben:
     * Vanilla rendert playerName als eigene Label-Zeile, sobald es gesetzt ist
     * (Doppel-Nametag-Bug). Render-Thread only; WeakHashMap folgt dem
     * State-Pooling.
     */
    private static final java.util.Map<net.minecraft.client.render.entity.state.EntityRenderState, String>
            ACCOUNT_BY_STATE = new java.util.WeakHashMap<>();

    private NametagService() {
    }

    public static void rememberAccount(net.minecraft.client.render.entity.state.EntityRenderState state,
                                       String account) {
        if (state != null && account != null && !account.isBlank()) {
            ACCOUNT_BY_STATE.put(state, account);
        }
    }

    public static String accountFor(net.minecraft.client.render.entity.state.EntityRenderState state) {
        if (state == null) {
            return null;
        }
        String remembered = ACCOUNT_BY_STATE.get(state);
        if (remembered != null) {
            return remembered;
        }
        if (state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState p
                && p.playerName != null) {
            return p.playerName.getString();
        }
        return state.displayName != null ? state.displayName.getString() : null;
    }

    public static void init(OttoExtraConfig.Nametags cfg) {
        config = cfg;
    }

    /**
     * Aktive Nametag-Config. Fallback auf {@link OttoExtraConfig#active()},
     * falls das Modul beim Start deaktiviert war (init nie lief) — sonst
     * bliebe ein späteres Aktivieren im Settings-GUI bis zum Neustart wirkungslos.
     */
    public static OttoExtraConfig.Nametags config() {
        OttoExtraConfig.Nametags c = config;
        if (c == null) {
            c = OttoExtraConfig.active().nametags;
            config = c;
        }
        return c;
    }

    // ---- Sichtbarkeit ---------------------------------------------------------

    /** Nametag dieses Spielers überhaupt rendern? (Modus + Profil-Flag) */
    public static boolean shouldRender(Entity entity) {
        OttoExtraConfig.Nametags cfg = config();
        if (cfg == null || !cfg.enabled) {
            return true;
        }
        try {
            LocalRpProfile profile = profileFor(entity);
            if (profile != null && !profile.showInNametag) {
                return false;
            }
            return switch (cfg.mode) {
                case NORMAL -> true;
                case HIDE_ALL -> false;
                case REALISTIC -> hasLineOfSight(entity);
            };
        } catch (Throwable t) {
            return true;
        }
    }

    /** Drei-Punkt-Sichtlinie (Augen, Oberkörper 70 %, Box-Mitte) wie OttoNames. */
    private static boolean hasLineOfSight(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || entity == null) {
            return true;
        }
        Entity viewer = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
        if (viewer == null || viewer == entity) {
            return true;
        }
        if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() == entity) {
            return true;
        }
        Vec3d start = viewer.getCameraPosVec(1.0f);
        Box box = entity.getBoundingBox();
        Vec3d center = box.getCenter();
        Vec3d eye = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
        Vec3d upperBody = new Vec3d(entity.getX(),
                box.minY + (box.maxY - box.minY) * 0.7, entity.getZ());
        Vec3d lowerBody = new Vec3d(entity.getX(),
                box.minY + (box.maxY - box.minY) * 0.3, entity.getZ());
        // Ein sichtbarer Körperpunkt reicht (Kopf ODER Körper sichtbar -> Nametag)
        return isUnobstructed(viewer, start, eye)
                || isUnobstructed(viewer, start, center)
                || isUnobstructed(viewer, start, upperBody)
                || isUnobstructed(viewer, start, lowerBody);
    }

    private static boolean isUnobstructed(Entity viewer, Vec3d start, Vec3d end) {
        HitResult hit = viewer.getEntityWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS;
    }

    // ---- Inhalt -----------------------------------------------------------------

    /**
     * Label-Zeilen für einen Spieler-Accountnamen; null = Vanilla rendern
     * lassen (Modul aus, kein Profil/keine Änderung nötig).
     */
    public static Lines linesFor(String accountName, Text vanillaName) {
        OttoExtraConfig.Nametags cfg = config();
        if (cfg == null || !cfg.enabled || accountName == null || accountName.isBlank()) {
            debugOnce("null-acc", "cfg=" + (cfg != null && cfg.enabled) + " acc=" + accountName);
            return null;
        }
        if (!RpNamesServices.isActive()) {
            debugOnce(accountName, "rpnames inaktiv");
            return null;
        }
        var catalog = RpNamesServices.catalog();
        LocalRpProfile profile = RpNamesServices.store().findByName(accountName).orElse(null);
        if (profile == null || !profile.showInNametag) {
            // Ausgeblendet -> Vanilla. Unbekannt (kein Profil) aber Name-Anzeige an:
            // Accountname trotzdem in Spieler-Farbkette (nie ungefärbt), ohne Titel/RP.
            if (profile == null && cfg.showPlayerName) {
                debugOnce(accountName, "kein Profil -> Account in Standardfarbe");
                return new Lines(null, colored(accountName,
                        accountColor(cfg, RpNamesServices.playerNameColor(null, null))), null);
            }
            debugOnce(accountName, profile == null ? "kein Profil" : "showInNametag=false");
            return null;
        }
        boolean hasRp = profile.hasRpName() && cfg.showRpName;
        // Unbekannte (ohne RP-Namen) bekommen keinen Titel im Namensschild
        boolean hasTitle = profile.hasTitle() && cfg.showTitle && profile.hasRpName();
        // Unsere Farbe gewinnt immer — Server-/Team-Farbe (z. B. Kampf-Rot) wird
        // bewusst NICHT mehr übernommen, damit der RP-Look konsistent bleibt.
        Text title = null;
        if (hasTitle) {
            String catalogColor = catalog != null
                    ? catalog.titleColor(profile.title).orElse(null) : null;
            String groupColor = RpNamesServices.titles().find(profile.title)
                    .map(r -> r.group().titleColor).orElse(null);
            String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";
            // Angezeigten Titel auf den Katalog-Kanon abbilden (umbenannte Titel
            // greifen so auch am Namensschild).
            String pers = profile.colors.nametagTitleColor;
            // „Farbe überschreibt": Katalogfarbe schlägt den Personen-Override.
            String titleColor = RpNamesServices.titleOverridesColor(profile.title)
                    ? firstNonBlank(catalogColor, firstNonBlank(pers, firstNonBlank(groupColor, fallback)))
                    : firstNonBlank(pers, firstNonBlank(catalogColor, firstNonBlank(groupColor, fallback)));
            title = colored(RpNamesServices.canonicalTitle(profile.title), titleColor);
        }
        // Proaktives Kennenlernen: Marker ist jetzt ein 3D-Ausrufezeichen über dem
        // Kopf (MeetMarkerRenderer) — kein "!" mehr im Namensschild.
        Text name;
        boolean nameIsAccount = false;
        if (hasRp) {
            name = colored(profile.rpName,
                    RpNamesServices.rpNameColor(profile.colors.nametagNameColor, profile.title));
        } else if (cfg.showRpName) {
            // RP-Name unbekannt: Accountname oder Platzhalter ("???"), einstellbar
            String shown = RpNamesServices.unknownDisplay(accountName);
            nameIsAccount = RpNamesServices.unknownShowsAccount();
            name = colored(shown, nameIsAccount
                    ? RpNamesServices.playerNameColor(null, profile.title) : "#8A8A8A");
        } else if (cfg.showPlayerName) {
            nameIsAccount = true;
            name = colored(accountName,
                    accountColor(cfg, RpNamesServices.playerNameColor(null, profile.title)));
        } else {
            name = Text.empty();
        }
        // Accountname als dritte Zeile darunter (nicht doppeln, wenn die
        // Namenszeile schon den Accountnamen zeigt)
        Text account = null;
        if (cfg.showPlayerName && !nameIsAccount) {
            account = colored(accountName,
                    accountColor(cfg, RpNamesServices.playerNameColor(null, profile.title)));
        }
        if (title == null && account == null && name.getString().isEmpty()) {
            debugOnce(accountName, "alle Zeilen deaktiviert -> Vanilla");
            return null;
        }
        debugOnce(accountName, "ersetzt (rp=" + hasRp + ", titel=" + hasTitle + ")");
        return new Lines(title, name, account);
    }

    /** Konfigurierte Accountnamen-Farbe, Fallback Standard-Namensfarbe. */
    private static String accountColor(OttoExtraConfig.Nametags cfg, String fallback) {
        return cfg.accountColor != null && !cfg.accountColor.isBlank()
                ? cfg.accountColor : fallback;
    }

    /** Einmaliges Debug-Log pro Schlüssel (Diagnose, kein Spam). */
    private static final java.util.Set<String> DEBUG_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void debugOnce(String key, String reason) {
        if (DEBUG_LOGGED.add(key)) {
            de.ottoextra.OttoExtra.LOGGER.info("[nametags] {} -> Vanilla ({})", key, reason);
        }
    }

    private static MutableText colored(String s, String hex) {
        MutableText t = Text.literal(s);
        TextColor c = ChatNameRewriter.parseColor(hex);
        if (c != null) {
            t.setStyle(Style.EMPTY.withColor(c));
        }
        return t;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static LocalRpProfile profileFor(Entity entity) {
        if (!RpNamesServices.isActive()) {
            return null;
        }
        String name = entity.getName() != null ? entity.getName().getString() : null;
        if (name == null || name.isBlank()) {
            return null;
        }
        return RpNamesServices.store().findByName(name).orElse(null);
    }
}
