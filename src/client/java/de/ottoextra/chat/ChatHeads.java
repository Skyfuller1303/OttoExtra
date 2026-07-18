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

        }
        return DefaultSkinHelper.getSkinTextures(uuid);
    }
}
