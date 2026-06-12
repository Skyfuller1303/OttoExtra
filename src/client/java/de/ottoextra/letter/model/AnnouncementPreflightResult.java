package de.ottoextra.letter.model;

import java.util.List;

/** Gesamtbefund des Verkündungs-Preflights. */
public record AnnouncementPreflightResult(String draftId, String totalChecksum,
                                          List<AnnouncementPageCheck> pages) {
    public boolean ok() {
        return pages.stream().allMatch(AnnouncementPageCheck::ok);
    }
}
