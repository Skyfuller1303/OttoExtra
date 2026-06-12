package de.ottoextra.mixin;

import de.ottoextra.chat.ChatChannelButton;
import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chat-Eingabefeld nach rechts einrücken, damit links Platz für den
 * Kanal-Button bleibt. Die
 * Command-Suggestions richten sich am Feld aus und wandern mit.
 * Mindestbreite bleibt gewahrt; ohne Ottonien/Modul bleibt Vanilla-Layout.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    /** Vanilla-Layout des Feldes (init): x=4, Breite bis screenWidth-4. */
    private static final int VANILLA_X = 4;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void ottoextra$shiftChatField(CallbackInfo ci) {
        ottoextra$applyShift();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"))
    private void ottoextra$keepShifted(CallbackInfo ci) {
        // Kanalwechsel ändert die Prefix-Breite -> Feld jedes Frame nachführen
        ottoextra$applyShift();
    }

    private void ottoextra$applyShift() {
        try {
            if (!ChatChannelState.buttonActive() || chatField == null) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            int shift = ChatChannelButton.x() + ChatChannelButton.width(client) + ChatChannelButton.GAP;
            int newX = Math.max(VANILLA_X, shift);
            int right = client.currentScreen != null ? client.currentScreen.width - VANILLA_X
                    : chatField.getX() + chatField.getWidth();
            chatField.setX(newX);
            chatField.setWidth(Math.max(60, right - newX));
        } catch (Throwable ignored) {
            // Chat darf nie brechen — Vanilla-Layout behalten
        }
    }
}
