package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.UUID;

public final class ChatHeads {

    private ChatHeads() {
    }

    public static SkinTextures skinForUuid(UUID uuid, String account) {
        MinecraftClient mc = MinecraftClient.getInstance();
        UUID resolvedUuid = uuid;
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
                    resolvedUuid = gp.id();
                    break;
                }
            }
        }
        if (resolvedUuid == null) {
            return null;
        }

        // SkinProvider/PlayerListEntry can initiate downloads whose lifecycle we cannot revoke.
        // All OttoExtra-triggered skin requests use the consent-bound cache instead.
        SkinCache.requestPng(resolvedUuid);
        SkinTextures local = SkinCache.localSkin(resolvedUuid);
        if (local != null) {
            return local;
        }
        return DefaultSkinHelper.getSkinTextures(resolvedUuid);
    }
}
