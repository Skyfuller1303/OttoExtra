package de.ottoextra.mixin;

import de.ottoextra.regions.RegionMessageService;
import de.ottoextra.playerlist.PlayerListDiagnostics;
import de.ottoextra.tweaks.combat.CombatModeHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    private static final String FORCE_MAIN_THREAD =
            "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread("
                    + "Lnet/minecraft/network/packet/Packet;"
                    + "Lnet/minecraft/network/listener/PacketListener;"
                    + "Lnet/minecraft/network/PacketApplyBatcher;)V";

    @Inject(method = "onPlayerRemove", at = @At(
            value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void ottoextra$beforePlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        PlayerListDiagnostics.removal((ClientPlayNetworkHandler) (Object) this, packet, "before");
    }

    @Inject(method = "onPlayerRemove", at = @At("TAIL"))
    private void ottoextra$afterPlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        PlayerListDiagnostics.removal((ClientPlayNetworkHandler) (Object) this, packet, "after");
    }

    @Inject(method = "onPlayerList", at = @At(
            value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void ottoextra$beforePlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        PlayerListDiagnostics.update((ClientPlayNetworkHandler) (Object) this, packet, "before");
    }

    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void ottoextra$afterPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        PlayerListDiagnostics.update((ClientPlayNetworkHandler) (Object) this, packet, "after");
    }

    @Inject(method = "onGameMessage", at = @At(
            value = "INVOKE",
            target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void ottoextra$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.content();
        if (packet.overlay()) CombatModeHud.onActionBar(content);
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, packet.overlay() ? "ActionBar" : "GameMessage");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onOverlayMessage", at = @At(
            value = "INVOKE",
            target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void ottoextra$onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.text();
        CombatModeHud.onActionBar(content);
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, "ActionBar");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onTitle", at = @At(
            value = "INVOKE",
            target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void ottoextra$onTitle(TitleS2CPacket packet, CallbackInfo ci) {
        Text content = packet.text();
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, "Title");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onSubtitle", at = @At(
            value = "INVOKE",
            target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void ottoextra$onSubtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        Text content = packet.text();
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, "Subtitle");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }
}
