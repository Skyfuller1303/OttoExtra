package de.ottoextra.tacticalfactions;

import de.ottoextra.OttoExtraGate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RetaliationMarkers {
    static final long DURATION_MS = 30_000L;
    private static final Map<UUID, Long> MARKED_UNTIL = new ConcurrentHashMap<>();

    private RetaliationMarkers() { }

    public static void onEntityDamage(EntityDamageS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        TacticalConfig config = TacticalFactions.config();
        if (packet == null || client == null || client.player == null || client.world == null
                || packet.entityId() != client.player.getId() || !config.combatMode
                || !config.retaliationMarkers || !OttoExtraGate.isOnOttonien(client)) return;
        DamageSource source = packet.createDamageSource(client.world);
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity player) || player == client.player
                || client.getNetworkHandler() == null
                || client.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) return;
        Relation relation = TacticalFactions.politics()
                .resolve(player.getUuid(), player.getName().getString()).relation();
        if (relation == Relation.ALLIED || relation == Relation.NEUTRAL) mark(player.getUuid());
    }

    static void mark(UUID uuid) {
        mark(uuid, System.currentTimeMillis());
    }

    static void mark(UUID uuid, long now) {
        if (uuid != null) MARKED_UNTIL.put(uuid, now + DURATION_MS);
    }

    public static boolean isMarked(UUID uuid) {
        return isMarked(uuid, System.currentTimeMillis());
    }

    static boolean isMarked(UUID uuid, long now) {
        if (uuid == null) return false;
        Long until = MARKED_UNTIL.get(uuid);
        if (until == null) return false;
        if (until <= now) {
            MARKED_UNTIL.remove(uuid, until);
            return false;
        }
        return true;
    }

    public static Text decorate(UUID uuid, Text name) {
        if (name == null || !TacticalFactions.config().retaliationMarkers || !isMarked(uuid)) return name;
        return Text.literal("! ").formatted(Formatting.RED).append(name);
    }

    public static void clear() {
        MARKED_UNTIL.clear();
    }
}
