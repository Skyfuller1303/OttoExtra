package de.ottoextra.mixin;

import de.ottoextra.chat.ChatMessagePipeline;
import de.ottoextra.rpnames.chat.ChatHistoryRefresh;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyArgs(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHudLine;<init>(ILnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"))
    private void ottoextra$rewriteChatLine(Args args) {
        Text original = args.get(1);
        MessageSignatureData signature = args.get(2);
        MessageIndicator indicator = args.get(3);
        Text displayed = ChatMessagePipeline.formatIncoming(original, signature, indicator);
        args.set(1, ChatHistoryRefresh.remember(original, displayed));
    }
}
