package de.ottoextra.mixin;

import de.ottoextra.chat.RpChatFormatter;
import de.ottoextra.rpnames.RpNamesServices;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text ottoextra$rewriteRpNames(Text message) {
        Text displayed = RpNamesServices.processChatMessage(message);
        displayed = RpChatFormatter.format(displayed);
        return de.ottoextra.rpnames.chat.ChatHistoryRefresh.remember(message, displayed);
    }
}
