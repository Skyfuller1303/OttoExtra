package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.UUID;

/**
 * Skin-Helfer (Kopf) für GUIs (z. B. Brief-Empfängerliste): löst den Skin
 * strikt zu einer UUID auf — Tabliste, persistenter {@link SkinCache}, sonst
 * Default. Reine Lese-Logik, nie ein Crash.
 *
 * <p>Die früheren „Spielerköpfe im Chat" wurden entfernt (Konflikt mit den
 * serverseitigen Kopf-Token wie {@code [name head]}).</p>
 */
public final class ChatHeads {

    private ChatHeads() {
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
}
