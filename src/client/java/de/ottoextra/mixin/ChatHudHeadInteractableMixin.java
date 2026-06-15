package de.ottoextra.mixin;

import de.ottoextra.chat.ChatHeads;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wie {@link ChatHudHeadMixin}, aber für das Backend {@code Interactable}
 * (geöffneter Chat-Bildschirm). Eigenes Mixin, da {@code context} ein anderes
 * Feld als in {@code Hud} ist (ein gemeinsames @Shadow wäre nicht eindeutig
 * remapbar).
 */
@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$Interactable")
public abstract class ChatHudHeadInteractableMixin {

    @Shadow
    @Final
    private DrawContext context;

    @Inject(method = "text(IFLnet/minecraft/text/OrderedText;)Z", at = @At("HEAD"))
    private void ottoextra$drawHead(int y, float opacity, OrderedText text,
                                    CallbackInfoReturnable<Boolean> cir) {
        ChatHeads.drawHead(context, y, opacity, text);
    }
}
