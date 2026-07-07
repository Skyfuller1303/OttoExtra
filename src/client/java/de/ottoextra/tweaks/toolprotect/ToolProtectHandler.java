package de.ottoextra.tweaks.toolprotect;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Werkzeugschutz: Fällt die Haltbarkeit eines Werkzeugs unter eine Schwelle
 * (verbleibende Nutzungen), werden Links- und Rechtsklick-Interaktionen
 * clientseitig abgebrochen und eine Actionbar-Nachricht angezeigt — das
 * Werkzeug geht nicht mehr versehentlich kaputt.
 *
 * <p>Ausnahmen von der Blockade:</p>
 * <ul>
 *   <li><b>UI-Blöcke</b> (Kiste, Werkbank, Amboss, Kartentisch, ...): der Klick
 *       öffnet nur ein Menü und verbraucht keine Haltbarkeit — bleibt erlaubt.
 *       Erkennung über Config-Liste + Heuristik (Screen-Factory/Container).</li>
 *   <li><b>Waffen</b> (Schwerter, Bögen, Armbrüste, Dreizack, Streitkolben,
 *       Schilde): funktionieren immer weiter, es gibt nur einen Actionbar-Hinweis,
 *       dass sie bald kaputtgehen.</li>
 * </ul>
 *
 * <p>Zusätzlich gibt es eine <b>einmalige</b> Actionbar-Warnung, wenn die
 * Haltbarkeit des gehaltenen Items <b>oder getragener Rüstung</b> unter einen
 * Prozentsatz (Default 10 %) fällt — einmal pro Unterschreitung, zurückgesetzt
 * beim Item-Wechsel oder wenn die Haltbarkeit wieder über der Schwelle liegt
 * (Reparatur).</p>
 */
public final class ToolProtectHandler {

    /** Actionbar-Throttle für die Blockier-Nachricht (Ticks). */
    private static final int MSG_COOLDOWN_TICKS = 20;

    /** Überwachte Slots für die Prozent-Warnung (Hände + Rüstung). */
    private static final EquipmentSlot[] WATCHED_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private int msgCooldown = 0;

    // Warn-einmalig-Zustand je überwachtem Slot (Item + war-bereits-unter-Schwelle)
    private final Item[] lastItem = new Item[WATCHED_SLOTS.length];
    private final boolean[] lastBelow = new boolean[WATCHED_SLOTS.length];

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
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) {
                return ActionResult.PASS;
            }
            if (h.isUiBlock(player, world, hitResult.getBlockPos())) {
                return ActionResult.PASS;
            }
            return h.guard(player, player.getStackInHand(hand));
        });
        UseItemCallback.EVENT.register((player, world, hand) ->
                world.isClient() ? h.guard(player, player.getStackInHand(hand)) : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                world.isClient() ? h.guard(player, player.getStackInHand(hand)) : ActionResult.PASS);

        ClientTickEvents.END_CLIENT_TICK.register(h::tick);
    }

    private OttoExtraConfig.Tweaks.ToolProtect cfg() {
        return config.tweaks.toolProtect;
    }

    /**
     * Blockiert die Interaktion (FAIL), wenn das Item fast kaputt ist.
     * Waffen werden nie blockiert — nur ein Actionbar-Hinweis.
     */
    private ActionResult guard(PlayerEntity player, ItemStack stack) {
        OttoExtraConfig.Tweaks.ToolProtect tp = cfg();
        if (!tp.enabled || !isNearlyBroken(stack, tp.blockAtUses)) {
            return ActionResult.PASS;
        }
        if (isWeapon(stack)) {
            if (msgCooldown <= 0) {
                msgCooldown = MSG_COOLDOWN_TICKS;
                player.sendMessage(Text.translatable("ottoextra.tweaks.toolprotect.weapon",
                                stack.getName().copy().formatted(Formatting.YELLOW))
                        .formatted(Formatting.GOLD), true);
            }
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

    /**
     * Öffnet der angeklickte Block ein UI? Dann bleibt der Rechtsklick trotz
     * fast kaputtem Werkzeug erlaubt — er verbraucht keine Haltbarkeit.
     * Beim Schleichen greift die Ausnahme nicht: das Block-UI wird dann
     * übersprungen und stattdessen das Item selbst benutzt/platziert.
     */
    private boolean isUiBlock(PlayerEntity player, World world, BlockPos pos) {
        if (player.shouldCancelInteraction()) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        java.util.List<String> uiBlocks = cfg().uiBlocks;
        if (uiBlocks != null && uiBlocks.contains(id)) {
            return true;
        }
        return state.createScreenHandlerFactory(world, pos) != null
                || world.getBlockEntity(pos) instanceof NamedScreenHandlerFactory;
    }

    /** Waffen (und Schilde) nie blockieren — sie müssen im Kampf funktionieren. */
    private static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return stack.isIn(ItemTags.SWORDS)
                || item instanceof RangedWeaponItem
                || item instanceof TridentItem
                || item instanceof MaceItem
                || item instanceof ShieldItem;
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
        for (int i = 0; i < WATCHED_SLOTS.length; i++) {
            ItemStack stack = client.player.getEquippedStack(WATCHED_SLOTS[i]);
            boolean below = isBelowPercent(stack, tp.warnBelowPercent);
            Item item = stack.isEmpty() ? null : stack.getItem();
            if (item != lastItem[i]) {
                // Item-Wechsel: Zustand neu aufsetzen; ist das Item bereits unter der
                // Schwelle, sofort warnen (Throttle verhindert Hotbar-Scroll-Spam)
                lastItem[i] = item;
                lastBelow[i] = below;
                if (below) {
                    warn(client, stack, tp);
                }
                continue;
            }
            if (below && !lastBelow[i]) {
                warn(client, stack, tp);
            }
            lastBelow[i] = below;
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
