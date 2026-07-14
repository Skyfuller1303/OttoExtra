package de.ottoextra.mixin;

import de.ottoextra.regions.RegionMessageService;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void ottoextra$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.content();
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, packet.overlay() ? "ActionBar" : "GameMessage");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void ottoextra$onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.text();
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, "ActionBar");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onTitle", at = @At("HEAD"), cancellable = true)
    private void ottoextra$onTitle(TitleS2CPacket packet, CallbackInfo ci) {
        Text content = packet.text();
        if (RegionMessageService.isRegionEnter(content)) {
            RegionMessageService.handle(content, "Title");
            if (RegionMessageService.shouldHide(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onSubtitle", at = @At("HEAD"), cancellable = true)
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
