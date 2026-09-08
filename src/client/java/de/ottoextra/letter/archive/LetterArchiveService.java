package de.ottoextra.letter.archive;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.letter.ui.WrittenLetterImport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.util.List;

/** Captures the actual book before /post consumes it; archives only Courier's matching acknowledgement. */
public final class LetterArchiveService {
    private static final java.util.Deque<Pending> pending = new java.util.ArrayDeque<>();
    private record Pending(List<String> pages, String author, String recipient, Instant date,
                           Object connection, long deadline) { }

    private LetterArchiveService() { }

    public static void onCommand(String command, String postCommand) {
        if (postCommand == null) return;
        String[] parts = command.strip().split("\\s+");
        if (parts.length != 2 || !parts[0].equalsIgnoreCase(postCommand.strip().replaceFirst("^/", ""))
                || !parts[1].matches("[A-Za-z0-9_]{1,16}")) return;
        var client = MinecraftClient.getInstance();
        if (client.player == null || !de.ottoextra.letter.LetterWriteItemGuard.isWritableLetter(client.player.getMainHandStack())) return;
        var book = client.player.getMainHandStack().get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        prune();
        if (pending.size() >= 16) pending.removeFirst();
        pending.addLast(new Pending(List.copyOf(WrittenLetterImport.toSectionPages(book)),
                book.author(), parts[1], Instant.now(), client.getNetworkHandler(),
                System.nanoTime() + 30_000_000_000L));
    }

    public static void onGameMessage(Text message, boolean overlay) {
        if (overlay) return;
        prune();
        Pending entry = pending.peekFirst();
        if (entry == null) return;
        String text = message.getString().replaceAll("§[0-9A-FK-ORa-fk-or]", "").strip();
        if (!text.startsWith("[Brief] ") && !text.startsWith("[Courier] ")) return;
        String body = text.substring(text.indexOf("] ") + 2);
        if (body.equalsIgnoreCase("Der Brief wurde " + entry.recipient + " gesendet.")) {
            pending.removeFirst();
            save("letter", entry.pages, entry.author, entry.recipient, entry.date);
        } else if (body.startsWith("Du hast keine Erlaubnis ")
                || body.equals("Bitte halte ein beschreibbares Buch in der Hand.")
                || body.equals("Dieser Brief wurde bereits gesendet.")
                || body.equalsIgnoreCase("Der Brief konnte " + entry.recipient + " nicht gesendet werden.")
                || (body.startsWith("Du benötigst ") && body.endsWith("um einen Brief zu senden."))) {
            pending.removeFirst();
        }
    }

    private static void prune() {
        Object connection = MinecraftClient.getInstance().getNetworkHandler();
        long now = System.nanoTime();
        pending.removeIf(entry -> entry.connection != connection || now > entry.deadline);
    }

    public static void exportBook(ItemStack stack) {
        var book = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (book != null) save(book.author(), WrittenLetterImport.toSectionPages(book), book.author(), "", Instant.now());
    }

    private static void save(String prefix, List<String> pages, String author, String recipient, Instant date) {
        try {
            var file = LetterTextArchive.write(OttoExtraPaths.lettersDir(), prefix, pages, author, recipient, date);
            hint(Text.translatable("ottoextra.letter.txt.saved",
                    Text.literal(file.getFileName().toString()).formatted(Formatting.GOLD),
                    Text.literal("(config/ottoextra/letters)").formatted(Formatting.GRAY))
                    .formatted(Formatting.GREEN));
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[letter] TXT archive could not be saved", e);
            hint(Text.translatable("ottoextra.letter.txt.failed").formatted(Formatting.RED));
        }
    }

    private static void hint(Text message) {
        var client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(
                Text.literal("[Brief] ").formatted(Formatting.GOLD).append(message), false);
    }

    public static void clear() { pending.clear(); }
}
