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

public final class PackInstaller {

    public static final String PACK_ID_PREFIX = "file/";

    private static boolean pendingActivation = false;
    private static boolean pendingPriorityTop = true;
    private static boolean pendingReload;
    private static boolean pendingRespectDisable;
    private static boolean pendingPreviouslyInstalled;

    private PackInstaller() {
    }

    public static String packId() {
        return PACK_ID_PREFIX + OttoExtraPaths.serverPackFileName();
    }

    public static void install(Path temp) throws IOException {
        Path target = OttoExtraPaths.serverPackFile();
        Files.createDirectories(target.getParent());
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void requestActivation(boolean priorityTop) {
        requestActivation(priorityTop, false, false);
    }

    public static synchronized void requestActivation(boolean priorityTop, boolean respectDisable,
                                                       boolean previouslyInstalled) {
        pendingPriorityTop = priorityTop;
        pendingRespectDisable = respectDisable;
        pendingPreviouslyInstalled = previouslyInstalled;
        pendingActivation = true;
    }

    public static synchronized void requestReload() {
        pendingReload = true;
    }

    public static synchronized void clearPending() {
        pendingActivation = false;
        pendingReload = false;
    }

    private record Pending(boolean activate, boolean replaced, boolean priorityTop,
                           boolean respectDisable, boolean previouslyInstalled) { }

    private static synchronized Pending takePending() {
        Pending result = new Pending(pendingActivation, pendingReload, pendingPriorityTop,
                pendingRespectDisable, pendingPreviouslyInstalled);
        clearPending();
        return result;
    }

    private static synchronized boolean hasPending() {
        return pendingActivation || pendingReload;
    }

    record Decision(boolean activate, boolean reload) { }

    static Decision decide(boolean requested, boolean replaced, boolean respectDisable,
                            boolean previouslyInstalled, boolean inManager, boolean inOptions) {
        boolean activate = requested && !(respectDisable && previouslyInstalled && !inOptions);
        boolean active = inManager && inOptions;
        boolean changeSelection = activate && !active;
        return new Decision(changeSelection, changeSelection || replaced && (active || activate));
    }

    static boolean usablePack(Path file) {
        if (!Files.isRegularFile(file)) return false;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file.toFile())) {
            var metadata = zip.getEntry("pack.mcmeta");
            return metadata != null && !metadata.isDirectory() && metadata.getSize() > 0;
        } catch (IOException error) {
            return false;
        }
    }

    public static void tick(MinecraftClient client) {
        if (!hasPending() || client == null) {
            return;
        }

        try {
            if (client.getOverlay() != null) {
                return;
            }
        } catch (Throwable ignored) {

        }
        Pending pending = takePending();

        String id = packId();
        try {
            if (!Files.exists(OttoExtraPaths.serverPackFile())) {
                return;
            }
            ResourcePackManager rpm = client.getResourcePackManager();
            rpm.scanPacks();

            boolean inManager = rpm.getEnabledIds().contains(id);
            boolean inOptions = client.options.resourcePacks.contains(id);
            Decision decision = decide(pending.activate(), pending.replaced(), pending.respectDisable(),
                    pending.previouslyInstalled(), inManager, inOptions);
            // Minecraft's persisted selection is authoritative after installation.
            PackState state = PackState.load();
            state.enabled = inOptions || decision.activate();
            state.save();
            if (decision.activate()) applyEnabled(client, id, pending.priorityTop());
            if (!decision.reload()) return;
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

    private static void applyEnabled(MinecraftClient client, String id, boolean priorityTop) {
        ResourcePackManager rpm = client.getResourcePackManager();
        LinkedHashSet<String> enabled = new LinkedHashSet<>(rpm.getEnabledIds());
        enabled.remove(id);
        if (priorityTop) {
            enabled.add(id);
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
