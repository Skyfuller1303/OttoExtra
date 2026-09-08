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
import java.util.List;
import java.util.Objects;

/** Verhindert, dass CourierNew beim Schreiben auf anderes Briefmaterial ausweicht. */
public final class LetterWriteItemGuard {

    enum Result {
        ALLOW,
        HOLD_STARTED_LETTER,
        HOLD_SINGLE_SUPPLY,
        REMOVE_EXTRA_SUPPLIES,
        HOLD_FIRST_SUPPLY
    }

    private LetterWriteItemGuard() {
    }

    static Result check(OttoExtraConfig config, LetterDraft draft) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Result.ALLOW;
        }

        ItemStack mainHand = client.player.getMainHandStack();

        boolean continuation = draft.meta.importedFromBook
                || draft.meta.lockedPages > 0 || draft.meta.lockedOffset > 0;
        if (continuation) {
            boolean matchingStartedLetter = isWritableLetter(mainHand) && WrittenLetterImport.hasPages(
                    mainHand, draft.meta.courierInitialPages);
            return decide(true, matchingStartedLetter, 0);
        }
        return checkFreshSupply(client, config.letter.triggerItemName);
    }

    private static Result checkFreshSupply(MinecraftClient client, String triggerName) {
        var inventory = client.player.getInventory();
        ItemStack held = client.player.getMainHandStack();
        int count = matchesTrigger(held, triggerName) ? held.getCount() : 0;
        boolean earlierSupply = false;
        if (count == 1) {
            // Courier removes the first matching item starting at slot 0. Since
            // the held slot is in the hotbar, later inventory/offhand slots cannot
            // divert consumption. Only earlier hotbar slots can keep the hand full.
            for (int slot = 0; slot < inventory.getSelectedSlot(); slot++) {
                if (sameCourierSupply(inventory.getStack(slot), held)) {
                    earlierSupply = true;
                    break;
                }
            }
        }
        return decide(false, false, count, earlierSupply);
    }

    private static boolean sameCourierSupply(ItemStack candidate, ItemStack held) {
        // The bundled ItemUtil.isEqual(..., false) compares type, damage, legacy
        // display name and lore, but ignores item model, PDC and other components.
        return !candidate.isEmpty() && candidate.isOf(held.getItem())
                && candidate.getDamage() == held.getDamage()
                && Objects.equals(customName(candidate), customName(held))
                && lore(candidate).equals(lore(held));
    }

    private static String customName(ItemStack stack) {
        var name = stack.get(DataComponentTypes.CUSTOM_NAME);
        return name == null ? null : WrittenLetterImport.toSection(name);
    }

    private static List<String> lore(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        return lore == null ? List.of()
                : lore.lines().stream().map(WrittenLetterImport::toSection).toList();
    }

    /** Courier identifies writable letters by their title and absence of the sent marker. */
    public static boolean isWritableLetter(ItemStack stack) {
        if (!isLetter(stack)) return false;
        var lore = stack.get(DataComponentTypes.LORE);
        return lore == null || lore.lines().stream().noneMatch(line ->
                WrittenLetterImport.toSection(line).contains("§8Zu "));
    }

    public static boolean isLetter(ItemStack stack) {
        if (stack == null || !stack.isOf(Items.WRITTEN_BOOK)
                || stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) == null) {
            return false;
        }
        String title = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).title().raw();
        return title.contains("§7Brief von §6");
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
            return checkFreshSupply(client, config.letter.triggerItemName) == Result.ALLOW;
        }
        return isWritableLetter(held) && context.matchesPages(WrittenLetterImport.toSectionPages(
                held.get(DataComponentTypes.WRITTEN_BOOK_CONTENT)));
    }

    private static String serverAddress(MinecraftClient client) {
        return client.getCurrentServerEntry() == null ? "" : client.getCurrentServerEntry().address;
    }

    static Result decide(boolean continuation, boolean matchingStartedLetter,
                          int heldSupplyCount) {
        return decide(continuation, matchingStartedLetter, heldSupplyCount, false);
    }

    static Result decide(boolean continuation, boolean matchingStartedLetter,
                         int heldSupplyCount, boolean earlierSupply) {
        if (continuation) {
            return matchingStartedLetter
                    ? Result.ALLOW : Result.HOLD_STARTED_LETTER;
        }
        if (heldSupplyCount < 1) {
            return Result.HOLD_SINGLE_SUPPLY;
        }
        if (heldSupplyCount > 1) {
            return Result.REMOVE_EXTRA_SUPPLIES;
        }
        return earlierSupply ? Result.HOLD_FIRST_SUPPLY : Result.ALLOW;
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
