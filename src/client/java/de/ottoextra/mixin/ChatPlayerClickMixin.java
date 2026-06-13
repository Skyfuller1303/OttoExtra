package de.ottoextra.mixin;

import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen;
import net.minecraft.client.MinecraftClient;
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
 * Shift-Klick auf einen Spielernamen im Chat öffnet das RP-Personenbuch beim
 * Eintrag der Person (nur wenn in den Einstellungen aktiviert). Hängt sich an den
 * Klick-Choke-Point {@code handleClickEvent(Style, boolean)}; der Account kommt aus
 * der Insertion (Vanilla shift-click-Name) oder dem Hover-Tooltip. Chat darf nie
 * brechen — bei jedem Zweifel Vanilla-Verhalten.
 */
@Mixin(ChatScreen.class)
public abstract class ChatPlayerClickMixin {

    @Inject(method = "handleClickEvent(Lnet/minecraft/text/Style;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void ottoextra$shiftClickPlayer(Style style, boolean leftClick,
                                            CallbackInfoReturnable<Boolean> cir) {
        try {
            var win = MinecraftClient.getInstance().getWindow();
            boolean shift = InputUtil.isKeyPressed(win, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(win, GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (!RpNamesServices.openBookOnClickEnabled() || style == null || !shift) {
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
