package de.ottoextra.letter.recovery;

import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.model.LetterSendProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SendProgressStoreTest {
    @TempDir Path directory;

    @Test
    void preservesDraftAndRecoveryContextAcrossRestart() {
        Path file = directory.resolve("progress.json");
        SendProgressStore<LetterSendProgress> store = new SendProgressStore<>(file, LetterSendProgress.class);
        LetterSendProgress original = new LetterSendProgress();
        original.pendingCommands = List.of("letter Gruß", "post Alice");
        original.sentCommands = 1;
        original.context = new LetterSendContext();
        original.context.serverAddress = "example.org";
        original.context.playerUuid = "player-one";
        original.context.selectedSlot = 2;
        original.context.draft = LetterDraft.empty();
        original.context.draft.pages = List.of("Mein vollständiger Entwurf");
        store.save(original);

        LetterSendProgress recovered = new SendProgressStore<>(file, LetterSendProgress.class).load();
        assertNotNull(recovered);
        assertEquals(1, recovered.sentCommands);
        assertTrue(recovered.context.matches("example.org", "player-one", 2));
        assertEquals(original.context.draft.pages, recovered.context.draft.pages);
    }

    @Test
    void syntacticallyValidButNullCommandListCannotCrashRecoveryTick() throws Exception {
        Path file = directory.resolve("progress.json");
        Files.writeString(file, "{\"pendingCommands\":null,\"sentCommands\":0}");
        assertNull(new SendProgressStore<>(file, LetterSendProgress.class).load());
    }
}
