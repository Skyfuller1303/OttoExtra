package de.ottoextra.nametags;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.addon.NametagContext;
import de.ottoextra.addon.NametagPresentation;
import de.ottoextra.addon.OttoExtraAddons;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.chat.ChatNameRewriter;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.title.TitleColorResolver;
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

    private record PlayerIdentity(java.util.UUID uuid, String account) { }

    // Render-thread only. Identity is captured from the actual entity, never a label.
    private static final java.util.Map<net.minecraft.client.render.entity.state.EntityRenderState, PlayerIdentity>
            PLAYER_BY_STATE = new java.util.WeakHashMap<>();

    private NametagService() { }

    public static void rememberPlayer(net.minecraft.client.render.entity.state.EntityRenderState state,
                                      Entity entity) {
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            rememberIdentity(state, player.getUuid(), player.getGameProfile().name());
        } else {
            rememberIdentity(state, null, null);
        }
    }

    /** Legacy account update; a label alone can never establish player identity. */
    @Deprecated
    public static void rememberAccount(net.minecraft.client.render.entity.state.EntityRenderState state,
                                       String account) {
        PlayerIdentity identity = PLAYER_BY_STATE.get(state);
        if (identity != null) rememberIdentity(state, identity.uuid(), account);
    }

    static void rememberIdentity(net.minecraft.client.render.entity.state.EntityRenderState state,
                                  java.util.UUID uuid, String account) {
        if (state == null) return;
        if (uuid == null || account == null || account.isBlank()) {
            PLAYER_BY_STATE.remove(state);
        } else {
            PLAYER_BY_STATE.put(state, new PlayerIdentity(uuid, account));
        }
    }

    public static String accountFor(net.minecraft.client.render.entity.state.EntityRenderState state) {
        PlayerIdentity identity = PLAYER_BY_STATE.get(state);
        return identity == null ? null : identity.account();
    }

    public static java.util.UUID playerUuidFor(net.minecraft.client.render.entity.state.EntityRenderState state) {
        PlayerIdentity identity = PLAYER_BY_STATE.get(state);
        return identity == null ? null : identity.uuid();
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
        return shouldRender(entity, true);
    }

    /** Includes Vanilla's initial decision so addons can explicitly override it. */
    public static boolean shouldRender(Entity entity, boolean vanillaVisible) {
        if (!isOnlinePlayer(entity)) return vanillaVisible;
        boolean baseVisible = baseShouldRender(entity, vanillaVisible);
        String account = entity.getName() != null ? entity.getName().getString() : "";
        NametagContext context = context(entity.getUuid(), account, entity.getName(), baseVisible);
        NametagPresentation result = OttoExtraAddons.processNametag(context,
                NametagPresentation.vanilla(baseVisible, entity.getName()));
        return result.forceVisible() ? result.visible() : baseVisible && result.visible();
    }

    private static boolean baseShouldRender(Entity entity, boolean vanillaVisible) {
        OttoExtraConfig.Nametags cfg = config();
        if (cfg == null || !cfg.enabled) {
            return vanillaVisible;
        }
        // PlayerEntityRenderer wird auf modernen Versionen auch fuer
        // PlayerLike-NPCs verwendet. RP-Sichtbarkeitsregeln duerfen jedoch nur
        // echte, in der Spielerliste gefuehrte Spieler beeinflussen.
        if (!isOnlinePlayer(entity)) {
            return vanillaVisible;
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

    /** Echte Online-Spieler von Tieren, sonstigen Entities und Fake-Player-NPCs trennen. */
    public static boolean isOnlinePlayer(Entity entity) {
        if (!(entity instanceof net.minecraft.entity.player.PlayerEntity player)) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return false;
        }
        return player == client.player
                || client.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null;
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
        return linesFor(uuidFor(accountName), accountName, vanillaName);
    }

    public static Lines linesFor(java.util.UUID uuid, String accountName, Text vanillaName) {
        Lines base = baseLinesFor(uuid, accountName, vanillaName);
        NametagPresentation initial = base == null
                ? NametagPresentation.vanilla(true, vanillaName)
                : new NametagPresentation(true, false, base.title(), base.name(), base.account(), true);
        NametagContext context = context(uuid, accountName, vanillaName, true);
        NametagPresentation result = OttoExtraAddons.processNametag(context, initial);
        if (!result.custom()) return null;
        Text name = result.name() != null ? result.name()
                : (vanillaName != null ? vanillaName : Text.literal(accountName == null ? "" : accountName));
        name = de.ottoextra.ailments.ConditionsDisplay.decorate(uuid, name);
        return new Lines(result.title(), name, result.account());
    }

    private static Lines baseLinesFor(java.util.UUID uuid, String accountName, Text vanillaName) {
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
        LocalRpProfile profile = RpNamesServices.displayProfile(
                uuid != null ? uuid.toString() : null, accountName).orElse(null);

        // Das persönliche Ausblenden bleibt bindend. NametagLabelRenderer ruft
        // shouldRender(...) zusätzlich auf und unterdrückt dadurch auch Vanilla.
        if (profile != null && !profile.showInNametag) {
            debugOnce(accountName, "showInNametag=false");
            return null;
        }

        boolean knownForDisplay = profile != null
                && RpNamesServices.isKnownForNametag(profile);

        /*
         * Proaktives Kennenlernen:
         *
         * Ein fehlendes Profil, ein vergessenes Profil oder ein lediglich von
         * der API importierter Name ist noch NICHT bekannt. In diesem Fall muss
         * OttoExtra selbst "Unbekannt" zeichnen. Ein null-Rückgabewert würde
         * Vanilla weiterzeichnen lassen und dadurch den Minecraft-Namen zeigen.
         */
        if (!knownForDisplay) {
            Text name;
            boolean nameIsAccount = false;

            if (cfg.showRpName) {
                String shown = RpNamesServices.unknownNametagDisplay(accountName);
                name = colored(shown, firstNonBlank(
                        profile != null ? profile.colors.nametagNameColor : null,
                        "#8A8A8A"));
            } else if (cfg.showPlayerName) {
                nameIsAccount = true;
                name = colored(accountName,
                        accountColor(cfg, RpNamesServices.playerNameColor(
                                profile != null ? profile.colors.nametagNameColor : null,
                                profile != null ? profile.title : null)));
            } else {
                name = Text.empty();
            }

            Text account = null;
            if (cfg.showPlayerName && !nameIsAccount
                    && RpNamesServices.unknownNametagAccountLineEnabled()) {
                account = colored(accountName,
                        accountColor(cfg, RpNamesServices.playerNameColor(
                                profile != null ? profile.colors.nametagNameColor : null,
                                profile != null ? profile.title : null)));
            }

            // Auch bei vollständig deaktivierten Zeilen einen leeren eigenen
            // Renderdatensatz zurückgeben, damit Vanilla den Accountnamen nicht
            // als Fallback einblendet.
            debugOnce(accountName, "unbekannt -> Platzhalter");
            return new Lines(null, name, account);
        }

        boolean hasRp = profile.hasRpName() && cfg.showRpName;
        boolean hasTitle = profile.hasTitle() && cfg.showTitle;
        // Unsere Farbe gewinnt immer — Server-/Team-Farbe (z. B. Kampf-Rot) wird
        // bewusst NICHT mehr übernommen, damit der RP-Look konsistent bleibt.
        Text title = null;
        if (hasTitle) {
            // Angezeigten Titel auf den Katalog-Kanon abbilden (umbenannte Titel
            // greifen so auch am Namensschild).
            String titleColor = TitleColorResolver.resolve(profile,
                    profile.colors.nametagTitleColor, profile.title,
                    catalog, RpNamesServices.titles());
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

    private static NametagContext context(java.util.UUID uuid, String account, Text vanilla,
                                           boolean baseVisible) {
        OttoExtraConfig.Nametags cfg = config();
        return new NametagContext(uuid, account, vanilla, true,
                cfg != null && cfg.enabled, baseVisible);
    }

    private static java.util.UUID uuidFor(String account) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null || account == null) return null;
        for (var entry : client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().name() != null
                    && account.equalsIgnoreCase(entry.getProfile().name())) {
                return entry.getProfile().id();
            }
        }
        return null;
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
            DebugLog.debug("[nametags] {} -> Vanilla ({})", key, reason);
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
        return RpNamesServices.displayProfile(entity.getUuidAsString(), name).orElse(null);
    }
}
