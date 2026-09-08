package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterWriteItemGuard;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.WeakHashMap;

/** Bind book actions to the item used before Courier's temporary reading copy arrives. */
public final class LetterBookActions {
    private static final long OPEN_TIMEOUT_NS = 5_000_000_000L;
    private static final Map<Screen, BookUse> openedBooks = new WeakHashMap<>();
    private static BookUse lastUse;
    private static PendingActions pending;

    private record BookUse(ItemStack stack, Hand hand, int slot, String player, long time) {
        boolean matches(PlayerEntity current) {
            return current != null && player.equals(current.getUuidAsString())
                    && current.getInventory().getSelectedSlot() == slot
                    && ItemStack.areEqual(stack, current.getMainHandStack());
        }
    }

    private record PendingActions(Screen screen, OttoExtraConfig config, int y, BookUse use, long time) { }

    private LetterBookActions() { }

    public static void noteUse(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        lastUse = stack.isOf(Items.WRITTEN_BOOK)
                ? new BookUse(stack.copy(), hand, player.getInventory().getSelectedSlot(),
                player.getUuidAsString(), System.nanoTime()) : null;
    }

    public static void afterInit(Screen screen, OttoExtraConfig config, int y) {
        BookUse use = openedBooks.get(screen);
        if (use == null) {
            use = lastUse;
            lastUse = null;
            if (use == null || System.nanoTime() - use.time() > OPEN_TIMEOUT_NS
                    || use.hand() != Hand.MAIN_HAND || !LetterWriteItemGuard.isWritableLetter(use.stack())) {
                return;
            }
            openedBooks.put(screen, use);
        }
        pending = new PendingActions(screen, config, y, use, System.nanoTime());
    }

    public static void tick(MinecraftClient client) {
        PendingActions actions = pending;
        if (actions == null) {
            return;
        }
        if (client.currentScreen != actions.screen()
                || System.nanoTime() - actions.time() > OPEN_TIMEOUT_NS) {
            pending = null;
            return;
        }
        // Paper sends a temporary stack, opens it, then restores the real stack.
        // Wait until restoration; never infer sent state or identity from that copy.
        if (!actions.use().matches(client.player)) {
            return;
        }
        pending = null;
        int center = actions.screen().width / 2;
        var buttons = Screens.getButtons(actions.screen());
        buttons.add(ButtonWidget.builder(Text.translatable("ottoextra.letter.book.edit"), button -> {
            if (actions.use().matches(client.player)) {
                WrittenLetterImport.editInEditor(actions.config(), client.player.getMainHandStack());
            }
        }).dimensions(center - 100, actions.y(), 98, 20).build());
        buttons.add(ButtonWidget.builder(Text.translatable("ottoextra.letter.book.send"), button ->
                LetterActionPrompt.onSendHeld(actions.config(), actions.screen(), actions.use().stack()))
                .dimensions(center + 2, actions.y(), 98, 20).build());
    }

    public static void clear() {
        lastUse = null;
        pending = null;
        openedBooks.clear();
    }
}
