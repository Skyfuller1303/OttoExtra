package de.ottoextra.tweaks.toolprotect;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/**
 * Werkzeugschutz: Fällt die Haltbarkeit eines Werkzeugs unter eine Schwelle
 * (verbleibende Nutzungen), werden Links- und Rechtsklick-Interaktionen
 * clientseitig abgebrochen und eine Actionbar-Nachricht angezeigt — das
 * Werkzeug geht nicht mehr versehentlich kaputt.
 *
 * <p>Zusätzlich gibt es eine <b>einmalige</b> Actionbar-Warnung, wenn die
 * Haltbarkeit des gehaltenen Items unter einen Prozentsatz (Default 10 %)
 * fällt — einmal pro Unterschreitung, zurückgesetzt beim Item-Wechsel oder
 * wenn die Haltbarkeit wieder über der Schwelle liegt (Reparatur).</p>
 */
public final class ToolProtectHandler {

    /** Actionbar-Throttle für die Blockier-Nachricht (Ticks). */
    private static final int MSG_COOLDOWN_TICKS = 20;

    private int msgCooldown = 0;

    // Warn-einmalig-Zustand je Hand (Item + war-bereits-unter-Schwelle)
    private final Item[] lastItem = new Item[2];
    private final boolean[] lastBelow = new boolean[2];

    private final OttoExtraConfig config;

    private ToolProtectHandler(OttoExtraConfig config) {
        this.config = config;
    }

    public static void register(OttoExtraConfig config) {
        ToolProtectHandler h = new ToolProtectHandler(config);

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                world.isClient() ? h.guard(player, player.getMainHandStack()) : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                world.isClient() ? h.guard(player, player.getMainHandStack()) : ActionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                world.isClient() ? h.guard(player, player.getStackInHand(hand)) : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                world.isClient() ? h.guard(player, player.getStackInHand(hand)) : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                world.isClient() ? h.guard(player, player.getStackInHand(hand)) : ActionResult.PASS);

        ClientTickEvents.END_CLIENT_TICK.register(h::tick);
    }

    private OttoExtraConfig.Tweaks.ToolProtect cfg() {
        return config.tweaks.toolProtect;
    }

    /** Blockiert die Interaktion (FAIL), wenn das Item fast kaputt ist. */
    private ActionResult guard(PlayerEntity player, ItemStack stack) {
        OttoExtraConfig.Tweaks.ToolProtect tp = cfg();
        if (!tp.enabled || !isNearlyBroken(stack, tp.blockAtUses)) {
            return ActionResult.PASS;
        }
        if (msgCooldown <= 0) {
            msgCooldown = MSG_COOLDOWN_TICKS;
            player.sendMessage(Text.translatable("ottoextra.tweaks.toolprotect.blocked",
                            stack.getName().copy().formatted(Formatting.YELLOW))
                    .formatted(Formatting.RED), true);
        }
        return ActionResult.FAIL;
    }

    /** Einmalige Warnung beim Unterschreiten der Prozent-Schwelle + Throttle-Tick. */
    private void tick(MinecraftClient client) {
        if (msgCooldown > 0) {
            msgCooldown--;
        }
        OttoExtraConfig.Tweaks.ToolProtect tp = cfg();
        if (!tp.enabled || client.player == null) {
            return;
        }
        for (Hand hand : Hand.values()) {
            int slot = hand.ordinal();
            ItemStack stack = client.player.getStackInHand(hand);
            boolean below = isBelowPercent(stack, tp.warnBelowPercent);
            Item item = stack.isEmpty() ? null : stack.getItem();
            if (item != lastItem[slot]) {
                // Item-Wechsel: Zustand neu aufsetzen; ist das Item bereits unter der
                // Schwelle, sofort warnen (Throttle verhindert Hotbar-Scroll-Spam)
                lastItem[slot] = item;
                lastBelow[slot] = below;
                if (below) {
                    warn(client, stack, tp);
                }
                continue;
            }
            if (below && !lastBelow[slot]) {
                warn(client, stack, tp);
            }
            lastBelow[slot] = below;
        }
    }

    /** Einmalige Haltbarkeits-Warnung (Actionbar, gethrottlet). */
    private void warn(MinecraftClient client, ItemStack stack, OttoExtraConfig.Tweaks.ToolProtect tp) {
        if (msgCooldown > 0 || client.player == null) {
            return;
        }
        msgCooldown = MSG_COOLDOWN_TICKS;
        client.player.sendMessage(Text.translatable("ottoextra.tweaks.toolprotect.warn",
                        stack.getName().copy().formatted(Formatting.YELLOW),
                        tp.warnBelowPercent)
                .formatted(Formatting.GOLD), true);
    }

    /** Verbleibende Nutzungen ≤ Schwelle? (nur beschädigbare Items) */
    private static boolean isNearlyBroken(ItemStack stack, int blockAtUses) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return false;
        }
        int remaining = stack.getMaxDamage() - stack.getDamage();
        return remaining <= Math.max(1, blockAtUses);
    }

    /** Verbleibende Haltbarkeit unter dem Prozentsatz? (nur beschädigbare Items) */
    private static boolean isBelowPercent(ItemStack stack, int percent) {
        if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return false;
        }
        int remaining = stack.getMaxDamage() - stack.getDamage();
        return remaining * 100 < stack.getMaxDamage() * Math.max(1, percent);
    }
}
