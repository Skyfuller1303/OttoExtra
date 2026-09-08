package de.ottoextra.letter.send;

import java.util.List;

public record LetterCommandPlan(List<PlannedCommand> entries,
                                List<String> predictedPages,
                                List<String> warnings) {

    public LetterCommandPlan {
        entries = List.copyOf(entries);
        predictedPages = List.copyOf(predictedPages);
        warnings = List.copyOf(warnings);
    }

    public List<String> commands() {
        return entries.stream().map(PlannedCommand::command).toList();
    }

    public List<String> transports() {
        return entries.stream().map(entry -> entry.transport().name()).toList();
    }

    public boolean sendable() {
        return warnings.isEmpty() && !entries.isEmpty();
    }

    public enum Transport {
        LEGACY_LINE,
        LEGACY_EMPTY,
        PAGE,
        V2_CONTENT,
        V2_BOUNDARY_PADDING,
        V2_PAGE_BLOCK,
        V2_LEGACY_LINE,
        V2_LEGACY_EMPTY
    }

    public record PlannedCommand(String command, Transport transport, int predictedPage) {
    }
}
