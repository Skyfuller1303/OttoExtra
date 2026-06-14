package de.ottoextra.mixin;

import de.ottoextra.chat.ChatChannelButton;
import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        // Langer Chat: Tipplänge über Vanilla-256 anheben
        try {
            var cfg = de.ottoextra.config.OttoExtraConfig.active().chat;
            if (cfg != null && cfg.enabled && cfg.longChatEnabled && chatField != null) {
                chatField.setMaxLength(Math.max(256, cfg.longChatMaxInput));
            }
        } catch (Throwable ignored) {
            // Layout/Eingabe darf nie brechen
        }
    }

    /**
     * Lange Nachricht splitten BEVOR Vanilla {@code sendMessage} auf 256 kürzt.
     * Volltext kommt hier ungekürzt an (chatField-MaxLength angehoben).
     */
    @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void ottoextra$splitLongMessage(String message, boolean addToHistory, CallbackInfo ci) {
        try {
            var cfg = de.ottoextra.config.OttoExtraConfig.active().chat;
            if (cfg == null || !cfg.enabled || !cfg.longChatEnabled || message == null) {
                return;
            }
            String text = message.trim();
            if (text.startsWith("/") || text.length() <= cfg.longChatChunk) {
                return;
            }
            de.ottoextra.chat.LongChatSender.configureMs(cfg.longChatDelayMs);
            de.ottoextra.chat.LongChatSender.enqueue(
                    de.ottoextra.chat.LongChatSender.split(text, cfg.longChatChunk, cfg.longChatMarker));
            ci.cancel();
        } catch (Throwable ignored) {
            // Chat darf nie brechen -> Vanilla senden lassen
        }
    }

    /** Shift+Tab wechselt im Chat den Kanal durch (inkl. OOC). */
    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyInput;)Z",
            at = @At("HEAD"), cancellable = true)
    private void ottoextra$shiftTabChannel(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() == GLFW.GLFW_KEY_TAB
                && (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0
                && ChatChannelState.shiftTabCycleEnabled()) {
            // immer abfangen -> kein Vanilla-Autofill der Spielernamen bei Shift+Tab
            try {
                ChatChannelState.cycleAllChannels();
            } catch (Throwable ignored) {
                // Chat darf nie brechen
            }
            cir.setReturnValue(true);
        }
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
