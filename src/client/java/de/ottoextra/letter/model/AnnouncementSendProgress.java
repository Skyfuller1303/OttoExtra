package de.ottoextra.letter.model;

import java.util.ArrayList;
import java.util.List;

public final class AnnouncementSendProgress {
    public String draftId = "";
    public String totalChecksum = "";
    public int pageCount = 0;
    public List<String> pendingCommands = new ArrayList<>();
    public int sentCommands = 0;
    public long startedAtMs = 0L;
    public long updatedAtMs = 0L;
}
