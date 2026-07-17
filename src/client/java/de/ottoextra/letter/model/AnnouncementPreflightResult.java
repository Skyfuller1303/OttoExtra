package de.ottoextra.letter.model;
import java.util.List;
public record AnnouncementPreflightResult(String draftId, String totalChecksum,
                                          List<AnnouncementPageCheck> pages) {
    public boolean ok() {
        return pages.stream().allMatch(AnnouncementPageCheck::ok);
    }
}
