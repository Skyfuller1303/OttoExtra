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
 * Optionale Spielerköpfe im Chat (unfokussiert): zeichnet das Wappen links neben
 * dem Namen. Hängt am ChatHud-Backend {@code Hud}, das je Zeile {@code text(y,
 * opacity, OrderedText)} im skalierten Pose zeichnet. Einrückung kommt aus
 * {@link ChatHeads#withHeadIndent}; Backend {@code Interactable} (offener Chat)
 * deckt {@link ChatHudHeadInteractableMixin} ab.
 */
@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$Hud")
public abstract class ChatHudHeadMixin {

    @Shadow
    @Final
    private DrawContext context;

    @Inject(method = "text(IFLnet/minecraft/text/OrderedText;)Z", at = @At("HEAD"))
    private void ottoextra$drawHead(int y, float opacity, OrderedText text,
                                    CallbackInfoReturnable<Boolean> cir) {
        ChatHeads.drawHead(context, y, opacity, text);
    }
}
