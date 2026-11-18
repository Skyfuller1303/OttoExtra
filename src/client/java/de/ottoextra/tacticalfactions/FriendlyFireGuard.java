package de.ottoextra.tacticalfactions;

import de.ottoextra.OttoExtraGate;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;

public final class FriendlyFireGuard {
    private static long lastWarning;

    private FriendlyFireGuard() { }

    static void register() {
        if (!BuildVariant.friendlyFireProtectionAvailable()) return;
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                shouldBlock(entity, FriendlyFirePolicy.WeaponKind.MELEE) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            FriendlyFirePolicy.WeaponKind weapon = weapon(stack);
            MinecraftClient client = MinecraftClient.getInstance();
            Entity target = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
            return shouldBlock(target, weapon) ? ActionResult.FAIL : ActionResult.PASS;
        });
    }

    private static FriendlyFirePolicy.WeaponKind weapon(ItemStack stack) {
        Item item = stack != null ? stack.getItem() : null;
        if (item instanceof BowItem) return FriendlyFirePolicy.WeaponKind.BOW;
        if (item instanceof CrossbowItem) return FriendlyFirePolicy.WeaponKind.CROSSBOW;
        if (item instanceof TridentItem) return FriendlyFirePolicy.WeaponKind.TRIDENT;
        if (item instanceof ThrowablePotionItem) {
            var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null) {
                for (var effect : contents.getEffects()) {
                    if (effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                        return FriendlyFirePolicy.WeaponKind.HARMFUL_POTION;
                    }
                }
            }
        }
        return FriendlyFirePolicy.WeaponKind.OTHER;
    }

    public static boolean shouldBlockRelease(ItemStack stack) {
        FriendlyFirePolicy.WeaponKind weapon = weapon(stack);
        if (weapon != FriendlyFirePolicy.WeaponKind.BOW
                && weapon != FriendlyFirePolicy.WeaponKind.TRIDENT) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        Entity target = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
        return shouldBlock(target, weapon);
    }

    private static boolean shouldBlock(Entity entity, FriendlyFirePolicy.WeaponKind weapon) {
        if (!BuildVariant.friendlyFireProtectionAvailable()) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        TacticalConfig config = TacticalFactions.config();
        boolean online = entity instanceof PlayerEntity player && client.getNetworkHandler() != null
                && client.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null;
        Relation relation = online ? TacticalFactions.politics()
                .resolve(entity.getUuid(), entity.getName().getString()).relation() : Relation.NEUTRAL;
        // Members of a squad with its protection box ticked are protected even when the global
        // switch is off; the squad flag is only ever honoured in this build variant.
        Squad squad = online ? TacticalFactions.politics()
                .squadFor(entity.getUuid(), entity.getName().getString()) : null;
        boolean squadProtected = squad != null && squad.friendlyFireProtection;
        if (squadProtected) relation = Relation.ALLIED;
        boolean block = FriendlyFirePolicy.blocks(config.combatMode,
                OttoExtraGate.isOnOttonien(client), config.preventFriendlyFire || squadProtected,
                config.protectNeutralPlayers, online && RetaliationMarkers.isMarked(entity.getUuid()), relation,
                online ? FriendlyFirePolicy.TargetKind.ONLINE_PLAYER
                        : entity instanceof PlayerEntity ? FriendlyFirePolicy.TargetKind.NPC
                        : FriendlyFirePolicy.TargetKind.OTHER,
                weapon);
        if (block) warn(client, relation);
        return block;
    }

    private static void warn(MinecraftClient client, Relation relation) {
        long now = System.currentTimeMillis();
        if (now - lastWarning < 1000L) return;
        lastWarning = now;
        if (client.player != null) client.player.sendMessage(
                Text.translatable(relation == Relation.NEUTRAL
                        ? "ottoextra_tacticalfactions.friendly_fire.neutral"
                        : "ottoextra_tacticalfactions.friendly_fire"), true);
    }
}
