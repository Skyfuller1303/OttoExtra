package de.ottoextra.playerlist;

import de.ottoextra.logging.DebugLog;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;

import java.util.UUID;

/** Opt-in packet evidence for entries that disappear only after reconnecting. */
public final class PlayerListDiagnostics {
    private PlayerListDiagnostics() {
    }

    public static void removal(ClientPlayNetworkHandler handler, PlayerRemoveS2CPacket packet,
                               String phase) {
        if (!DebugLog.isLogging()) {
            return;
        }
        for (UUID uuid : packet.profileIds()) {
            logState(handler, uuid, null, "REMOVE " + phase);
        }
    }

    public static void update(ClientPlayNetworkHandler handler, PlayerListS2CPacket packet,
                              String phase) {
        if (!DebugLog.isLogging()) {
            return;
        }
        var actions = packet.getActions();
        if (!actions.contains(PlayerListS2CPacket.Action.ADD_PLAYER)
                && !actions.contains(PlayerListS2CPacket.Action.UPDATE_LISTED)
                && !actions.contains(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME)) {
            return;
        }
        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            String packetState = actions + " " + phase
                    + (actions.contains(PlayerListS2CPacket.Action.UPDATE_LISTED)
                    ? " listed=" + entry.listed() : "")
                    + (actions.contains(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME)
                    ? " display=" + (entry.displayName() == null ? "null" : "present") : "");
            logState(handler, entry.profileId(),
                    entry.profile() == null ? null : entry.profile().name(), packetState);
        }
    }

    private static void logState(ClientPlayNetworkHandler handler, UUID uuid, String packetName,
                                 String packetState) {
        PlayerListEntry current = handler.getPlayerListEntry(uuid);
        String account = current != null ? current.getProfile().name() : packetName;
        int listedMatches = 0;
        int orphanedMatches = 0;
        for (PlayerListEntry listed : handler.getListedPlayerListEntries()) {
            if (uuid.equals(listed.getProfile().id())) {
                listedMatches++;
                if (listed != current) {
                    orphanedMatches++;
                }
                if (account == null) {
                    account = listed.getProfile().name();
                }
            }
        }
        DebugLog.debug("[playerlist-packet] connection={} {} uuid={} account={} tracked={} "
                        + "listedMatches={} orphanedMatches={} totalTracked={} totalListed={}",
                Integer.toHexString(System.identityHashCode(handler)), packetState,
                uuid, account == null ? "?" : account, current != null,
                listedMatches, orphanedMatches, handler.getPlayerList().size(),
                handler.getListedPlayerListEntries().size());
    }
}
