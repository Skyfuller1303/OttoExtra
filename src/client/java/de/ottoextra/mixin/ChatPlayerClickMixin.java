package de.ottoextra.mixin;

import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-Linksklick auf einen Spielernamen im Chat öffnet das RP-Personenbuch
 * beim Eintrag der Person (nur wenn aktiviert). Eigene Style-Auflösung mit
 * {@code insert(false)} — Vanilla würde bei Shift nur Styles MIT Insertion
 * liefern; Ottonien-Namen haben keine. Account aus Insertion oder Hover.
 * Chat darf nie brechen.
 */
@Mixin(ChatScreen.class)
public abstract class ChatPlayerClickMixin {

    @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void ottoextra$shiftClickPlayer(Click click, boolean doubled,
                                            CallbackInfoReturnable<Boolean> cir) {
        try {
            if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || !RpNamesServices.openBookOnClickEnabled()) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean shift = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (!shift) {
                return;
            }
            // Style unter dem Cursor auflösen (insert=false -> auch ohne Insertion)
            DrawnTextConsumer.ClickHandler handler = new DrawnTextConsumer.ClickHandler(
                    mc.textRenderer, (int) click.x(), (int) click.y()).insert(false);
            mc.inGameHud.getChatHud().render(handler, mc.getWindow().getScaledHeight(),
                    mc.inGameHud.getTicks(), true);
            Style style = handler.getStyle();
            if (style == null) {
                return;
            }
            String account = null;
            if (style.getInsertion() != null && !style.getInsertion().isBlank()) {
                account = style.getInsertion().trim();
            } else if (style.getHoverEvent() instanceof HoverEvent.ShowText shown) {
                String first = shown.value().getString();
                int nl = first.indexOf('\n');
                if (nl >= 0) {
                    first = first.substring(0, nl);
                }
                first = first.trim();
                if (RpNamesServices.findProfileByAnyName(first) != null) {
                    account = first;
                }
            }
            if (account == null) {
                return; // kein Spielername -> Vanilla
            }
            RpNamesPeopleBookScreen.openFor(null, account, null);
            cir.setReturnValue(true);
        } catch (Throwable ignored) {
            // niemals den Chat brechen
        }
    }
}
