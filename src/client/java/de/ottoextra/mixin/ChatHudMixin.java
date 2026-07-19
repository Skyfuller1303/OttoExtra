package de.ottoextra.mixin;

import de.ottoextra.chat.ChatChannelFormatter;
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
        displayed = ChatChannelFormatter.format(displayed);
        // Normale OttoExtra-Nachrichten zuerst formatieren. Addons dürfen danach
        // gezielt ihre eigenen Übersetzungs-/Hover-Stile darüberlegen.
        displayed = RpChatFormatter.format(displayed);
        displayed = de.ottoextra.addon.OttoExtraAddons.processChatMessage(displayed);
        return de.ottoextra.rpnames.chat.ChatHistoryRefresh.remember(message, displayed);
    }
}
