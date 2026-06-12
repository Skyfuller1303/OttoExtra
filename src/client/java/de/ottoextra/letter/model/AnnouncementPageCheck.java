package de.ottoextra.letter.model;

import java.util.List;

/**
 * Preflight-Befund einer Verkündungsseite: Probleme blockieren
 * den Versand, bis der Nutzer sie behebt.
 */
public record AnnouncementPageCheck(int pageIndex, int characters, int bytes,
                                    int chunkCount, String checksum,
                                    List<String> problems) {
    public boolean ok() {
        return problems.isEmpty();
    }
}
