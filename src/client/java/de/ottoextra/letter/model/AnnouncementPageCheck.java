package de.ottoextra.letter.model;

import java.util.List;

public record AnnouncementPageCheck(int pageIndex, int characters, int bytes,
                                    int chunkCount, String checksum,
                                    List<String> problems) {
    public boolean ok() {
        return problems.isEmpty();
    }
}
