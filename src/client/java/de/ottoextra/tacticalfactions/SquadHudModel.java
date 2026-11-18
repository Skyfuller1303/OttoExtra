package de.ottoextra.tacticalfactions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Pure row model of the squad HUD. Online means only "listed in the tab list": the input is a set
 * of lower-case names and UUIDs, nothing else about a player is ever consulted.
 */
final class SquadHudModel {
    private SquadHudModel() { }

    /**
     * A squad header (member == null, more == 0), a member line, or a "+n more" line
     * (member == null, more &gt; 0). Header lines carry the online count and size of the whole squad.
     */
    record Line(Squad squad, SquadMember member, boolean online, int onlineCount, int total, int more) {
        boolean header() { return member == null && more == 0; }

        boolean moreRow() { return member == null && more > 0; }
    }

    static List<Line> build(List<Squad> squads, Set<String> onlineKeys, boolean showMembers, boolean onlyOnline) {
        return build(squads, onlineKeys, showMembers, onlyOnline, 0);
    }

    /** {@code maxMembers} 0 means unlimited; it applies to each squad separately. */
    static List<Line> build(List<Squad> squads, Set<String> onlineKeys, boolean showMembers,
                            boolean onlyOnline, int maxMembers) {
        List<Line> lines = new ArrayList<>();
        if (squads == null) return lines;
        for (Squad squad : squads) {
            if (squad == null || !squad.showInHud || squad.members == null || squad.members.isEmpty()) continue;
            int online = squad.onlineCount(onlineKeys);
            lines.add(new Line(squad, null, false, online, squad.members.size(), 0));
            if (!showMembers) continue;
            List<SquadMember> candidates = new ArrayList<>();
            for (SquadMember member : squad.members) {
                if (!onlyOnline || member.isOnline(onlineKeys)) candidates.add(member);
            }
            int hidden = 0;
            if (maxMembers > 0 && candidates.size() > maxMembers) {
                hidden = candidates.size() - maxMembers;
                candidates = truncate(squad, candidates, onlineKeys, maxMembers);
            }
            for (SquadMember member : candidates) {
                lines.add(new Line(squad, member, member.isOnline(onlineKeys), online, squad.members.size(), 0));
            }
            if (hidden > 0) lines.add(new Line(squad, null, false, online, squad.members.size(), hidden));
        }
        return lines;
    }

    /** Online members first (list order kept inside each group); a leader is never cut. */
    private static List<SquadMember> truncate(Squad squad, List<SquadMember> candidates, Set<String> onlineKeys,
                                              int max) {
        List<SquadMember> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparing((SquadMember m) -> !m.isOnline(onlineKeys)));
        List<SquadMember> kept = new ArrayList<>(ordered.subList(0, max));
        SquadMember leader = null;
        for (SquadMember member : candidates) if (member.leader) { leader = member; break; }
        if (leader != null && !kept.contains(leader)) {
            kept.set(kept.size() - 1, leader);
        }
        kept.sort(Comparator.comparing((SquadMember m) -> !m.isOnline(onlineKeys))
                .thenComparingInt(candidates::indexOf));
        return kept;
    }

    static int totalOnline(List<Line> lines) {
        int sum = 0;
        for (Line line : lines) if (line.header()) sum += line.onlineCount();
        return sum;
    }

    static int totalMembers(List<Line> lines) {
        int sum = 0;
        for (Line line : lines) if (line.header()) sum += line.total();
        return sum;
    }
}
