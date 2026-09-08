package de.ottoextra.letter;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.ui.WrittenLetterImport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import de.ottoextra.letter.recovery.LetterSendContext;
import java.util.ArrayList;

import java.util.Locale;

/** Verhindert, dass CourierNew beim Schreiben auf anderes Briefmaterial ausweicht. */
public final class LetterWriteItemGuard {

    enum Result {
        ALLOW,
        HOLD_STARTED_LETTER,
        HOLD_SINGLE_SUPPLY,
        REMOVE_EXTRA_SUPPLIES
    }

    private LetterWriteItemGuard() {
    }

    static Result check(OttoExtraConfig config, LetterDraft draft) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Result.ALLOW;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        String triggerName = config.letter.triggerItemName;
        boolean heldSupply = matchesTrigger(mainHand, triggerName);
        int heldSupplyCount = heldSupply ? mainHand.getCount() : 0;
        int matchingSupplyCount = 0;
        if (heldSupply) {
            var inventory = client.player.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack candidate = inventory.getStack(slot);
                if (ItemStack.areItemsAndComponentsEqual(candidate, mainHand)) {
                    matchingSupplyCount += candidate.getCount();
                }
            }
        }

        boolean continuation = draft.meta.importedFromBook
                || draft.meta.lockedPages > 0 || draft.meta.lockedOffset > 0;
        if (continuation) {
            boolean matchingStartedLetter = isWritableLetter(mainHand) && WrittenLetterImport.hasPages(
                    mainHand, draft.meta.courierInitialPages);
            return decide(true, matchingStartedLetter, heldSupplyCount);
        }
        return decide(false, false, heldSupplyCount, matchingSupplyCount);
    }

    /** Courier identifies writable letters by their title and absence of the sent marker. */
    public static boolean isWritableLetter(ItemStack stack) {
        if (stack == null || !stack.isOf(Items.WRITTEN_BOOK)
                || stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) == null) {
            return false;
        }
        String title = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).title().raw();
        if (!title.contains("§7Brief von §6")) {
            return false;
        }
        var lore = stack.get(DataComponentTypes.LORE);
        return lore == null || lore.lines().stream().noneMatch(line ->
                WrittenLetterImport.toSection(line).contains("§8Zu "));
    }

    static LetterSendContext capture(OttoExtraConfig config, LetterDraft draft) {
        MinecraftClient client = MinecraftClient.getInstance();
        LetterSendContext context = new LetterSendContext();
        context.serverAddress = serverAddress(client);
        context.playerUuid = client.player.getUuidAsString();
        context.selectedSlot = client.player.getInventory().getSelectedSlot();
        context.letterCommand = config.letter.letterCommand.trim();
        context.draft = draft;
        ItemStack held = client.player.getMainHandStack();
        if (isWritableLetter(held)) {
            context.writtenExpected = true;
            context.expectedPages = new ArrayList<>(WrittenLetterImport.toSectionPages(
                    held.get(DataComponentTypes.WRITTEN_BOOK_CONTENT)));
        }
        return context;
    }

    static boolean matches(OttoExtraConfig config, LetterSendContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (context == null || client.player == null
                || !context.matches(serverAddress(client), client.player.getUuidAsString(),
                client.player.getInventory().getSelectedSlot())) {
            return false;
        }
        if (context.submitted) {
            return true;
        }
        ItemStack held = client.player.getMainHandStack();
        if (!context.writtenExpected) {
            return held.getCount() == 1 && matchesTrigger(held, config.letter.triggerItemName);
        }
        return isWritableLetter(held) && context.matchesPages(WrittenLetterImport.toSectionPages(
                held.get(DataComponentTypes.WRITTEN_BOOK_CONTENT)));
    }

    private static String serverAddress(MinecraftClient client) {
        return client.getCurrentServerEntry() == null ? "" : client.getCurrentServerEntry().address;
    }

    static Result decide(boolean continuation, boolean matchingStartedLetter,
                          int heldSupplyCount) {
        return decide(continuation, matchingStartedLetter, heldSupplyCount, heldSupplyCount);
    }

    static Result decide(boolean continuation, boolean matchingStartedLetter,
                         int heldSupplyCount, int matchingSupplyCount) {
        if (continuation) {
            return matchingStartedLetter
                    ? Result.ALLOW : Result.HOLD_STARTED_LETTER;
        }
        if (heldSupplyCount < 1) {
            return Result.HOLD_SINGLE_SUPPLY;
        }
        return heldSupplyCount > 1 || matchingSupplyCount > 1
                ? Result.REMOVE_EXTRA_SUPPLIES : Result.ALLOW;
    }

    private static boolean matchesTrigger(ItemStack stack, String triggerName) {
        if (stack == null || stack.isEmpty() || triggerName == null
                || triggerName.isBlank()) {
            return false;
        }
        String shown = stack.getName().getString().replace("\"", "")
                .replace("'", "").trim().toLowerCase(Locale.ROOT);
        String trigger = triggerName.trim().toLowerCase(Locale.ROOT);
        return shown.equals(trigger) || shown.contains(trigger);
    }
}
