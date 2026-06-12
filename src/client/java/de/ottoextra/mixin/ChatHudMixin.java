package de.ottoextra.mixin;

import de.ottoextra.rpnames.RpNamesServices;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * RP-Namen: EIN Hook für Lernen UND Ersetzen (der 1.20.4-Bestand hatte zwei
 * konkurrierende Mixins auf dieselbe Methode).
 *
 * <p>Ziel: {@code ChatHud.addMessage(Text)}; ModifyVariable at HEAD, argsOnly.
 * Fallback-Plan: Service liefert bei jedem Fehler das Original zurück; das
 * Mixin selbst enthält keine Logik.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"), argsOnly = true)
    private Text ottoextra$rewriteRpNames(Text message) {
        return RpNamesServices.processChatMessage(message);
    }
}
