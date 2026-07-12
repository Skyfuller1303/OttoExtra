package de.ottoextra.letter.announcement;

import de.ottoextra.letter.model.AnnouncementPageCheck;
import de.ottoextra.letter.model.AnnouncementPreflightResult;
import de.ottoextra.letter.placeholder.LetterPlaceholderParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AnnouncementPreflightService {

    public static final int DISCORD_MESSAGE_LIMIT = 2000;
    public static final int MAX_CHUNKS_PER_PAGE = 20;

    private final AnnouncementCommandBuilder builder;

    public AnnouncementPreflightService(AnnouncementCommandBuilder builder) {
        this.builder = builder;
    }

    public AnnouncementPreflightResult check(String draftId, List<String> pageTexts) {
        List<AnnouncementPageCheck> checks = new ArrayList<>();
        for (int i = 0; i < pageTexts.size(); i++) {
            String text = pageTexts.get(i);
            List<String> problems = new ArrayList<>();
            int chars = text.length();
            int bytes = text.getBytes(StandardCharsets.UTF_8).length;
            if (text.isBlank()) {
                problems.add("Seite ist leer");
            }
            int unresolved = LetterPlaceholderParser.parse(text).size();
            if (unresolved > 0) {
                problems.add(unresolved + " Platzhalter nicht aufgelöst");
            }
            int invalid = LetterPlaceholderParser.countInvalidOpenings(text);
            if (invalid > 0) {
                problems.add(invalid + " ungültige/offene Platzhalter-Klammern");
            }
            if (chars > DISCORD_MESSAGE_LIMIT) {
                problems.add("über Discord-Limit (" + chars + "/" + DISCORD_MESSAGE_LIMIT
                        + " Zeichen) — Seite teilen");
            }
            int chunkCount = builder.chunkPage(draftId, i + 1, text).size();
            if (chunkCount > MAX_CHUNKS_PER_PAGE) {
                problems.add("zu viele Chunks (" + chunkCount + ")");
            }
            checks.add(new AnnouncementPageCheck(i, chars, bytes, chunkCount,
                    AnnouncementCommandBuilder.checksum(text), problems));
        }
        return new AnnouncementPreflightResult(draftId,
                builder.totalChecksum(pageTexts), checks);
    }
}
