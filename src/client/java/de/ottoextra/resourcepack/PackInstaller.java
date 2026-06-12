package de.ottoextra.resourcepack;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;

/**
 * Legt das verifizierte ZIP in den resourcepacks-Ordner und aktiviert es.
 *
 * <p><b>Kritisch (Stabilität):</b> Während des Client-Starts fasst OttoExtra
 * <i>nichts</i> am Render-/ResourcePack-/Options-System an. Datei-Download/-Move
 * + State laufen off-thread auf Disk; die eigentliche Aktivierung
 * (scanPacks + setEnabledProfiles + options.write + reloadResources) wird per
 * {@link #requestActivation(boolean)} nur <i>vorgemerkt</i> und erst an einem
 * sicheren Zeitpunkt (Titelscreen, kein Lade-Overlay) über {@link #tick(MinecraftClient)}
 * ausgeführt.</p>
 *
 * <p>Grund: Manipuliert ein Mod ResourcePacks/options während des Startups, kann das
 * in Launchern mit Relaunch-Mechanik (Modrinth/Theseus) einen Neustart auslösen, der
 * dann mit {@code GLFW_NOT_INITIALIZED} abstürzt. Zur Laufzeit (Titelscreen) ist
 * dieselbe Aktion unproblematisch.</p>
 */
public final class PackInstaller {

    public static final String PACK_ID_PREFIX = "file/";

    private static volatile boolean pendingActivation = false;
    private static volatile boolean pendingPriorityTop = true;

    private PackInstaller() {
    }

    public static String packId() {
        return PACK_ID_PREFIX + OttoExtraPaths.serverPackFileName();
    }

    /** Verschiebt die Temp-Datei atomar in den resourcepacks-Ordner. Kein Render-/Options-Zugriff. */
    public static void install(Path temp) throws IOException {
        Path target = OttoExtraPaths.serverPackFile();
        Files.createDirectories(target.getParent());
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Merkt eine Aktivierung vor. Führt NICHTS sofort aus — die eigentliche Arbeit
     * passiert in {@link #tick(MinecraftClient)} am Titelscreen.
     */
    public static void requestActivation(boolean priorityTop) {
        pendingPriorityTop = priorityTop;
        pendingActivation = true;
    }

    public static void clearPending() {
        pendingActivation = false;
    }

    /**
     * Pro Client-Tick aufgerufen. Führt eine vorgemerkte Aktivierung aus, sobald der
     * Client sicher ist (an einem Screen, kein Lade-Overlay). Aktiviert nur, wenn nötig,
     * und reloadet nur dann.
     */
    public static void tick(MinecraftClient client) {
        if (!pendingActivation || client == null) {
            return;
        }
        // Noch im Startup/Splash? -> warten (genau das wollen wir NICHT beim Start tun).
        if (client.currentScreen == null) {
            return;
        }
        try {
            if (client.getOverlay() != null) {
                return; // Lade-Overlay aktiv -> warten
            }
        } catch (Throwable ignored) {
            // getOverlay nicht verfügbar -> fortfahren
        }
        pendingActivation = false;

        String id = packId();
        try {
            if (!Files.exists(OttoExtraPaths.serverPackFile())) {
                return;
            }
            ResourcePackManager rpm = client.getResourcePackManager();
            rpm.scanPacks();

            boolean inManager = rpm.getEnabledIds().contains(id);
            boolean inOptions = client.options.resourcePacks.contains(id);
            if (inManager && inOptions) {
                OttoExtra.LOGGER.info("[resourcepack] Bereits aktiv: {} (kein Reload).", id);
                return;
            }

            applyEnabled(client, id, pendingPriorityTop);
            client.reloadResources().whenComplete((v, t) -> {
                if (t != null) {
                    OttoExtra.LOGGER.error("[resourcepack] Reload fehlgeschlagen — Rollback.", t);
                    rollback(client, id);
                } else {
                    OttoExtra.LOGGER.info("[resourcepack] Server-Look aktiv: {}", id);
                }
            });
        } catch (Throwable t) {
            OttoExtra.LOGGER.error("[resourcepack] Aktivierung fehlgeschlagen — Rollback.", t);
            rollback(client, id);
        }
    }

    /** Fügt den Pack in Manager-Profile + Vanilla-Optionen ein (Priorität beachtet). */
    private static void applyEnabled(MinecraftClient client, String id, boolean priorityTop) {
        ResourcePackManager rpm = client.getResourcePackManager();
        LinkedHashSet<String> enabled = new LinkedHashSet<>(rpm.getEnabledIds());
        enabled.remove(id);
        if (priorityTop) {
            enabled.add(id); // zuletzt angewandt = höchste Priorität
        } else {
            LinkedHashSet<String> reordered = new LinkedHashSet<>();
            reordered.add(id);
            reordered.addAll(enabled);
            enabled = reordered;
        }
        rpm.setEnabledProfiles(enabled);
        if (!client.options.resourcePacks.contains(id)) {
            client.options.resourcePacks.add(id);
        }
        client.options.write();
    }

    private static void rollback(MinecraftClient client, String id) {
        try {
            ResourcePackManager rpm = client.getResourcePackManager();
            LinkedHashSet<String> enabled = new LinkedHashSet<>(rpm.getEnabledIds());
            if (enabled.remove(id)) {
                rpm.setEnabledProfiles(enabled);
            }
            client.options.resourcePacks.remove(id);
            client.options.write();
        } catch (Throwable t) {
            OttoExtra.LOGGER.warn("[resourcepack] Rollback unvollstaendig: {}", t.toString());
        }
    }
}
