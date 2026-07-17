package de.ottoextra.letter.model;
import java.util.ArrayList;
import java.util.List;
public final class LetterSendProgress {
    public String draftId = "";
    public List<String> pendingCommands = new ArrayList<>();
    public int sentCommands = 0;
    public String recipient = "";
    public long startedAtMs = 0L;
    public long updatedAtMs = 0L;
}
