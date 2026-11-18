package de.ottoextra.tacticalfactions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A named combat squad ("Trupp"). Purely local; the only live information the client ever derives
 * from it is whether a member is present in the tab list.
 */
public final class Squad {
    public String id;
    public String name = "";
    public String color = "#55C46A";
    public List<SquadMember> members = new ArrayList<>();
    public boolean alwaysAllied = true;
    public boolean tablistTop = true;
    public boolean friendlyFireProtection = false;
    public boolean showInHud = true;

    public Squad() { }

    public static Squad create(String name, String color) {
        Squad squad = new Squad();
        squad.id = "squad:" + UUID.randomUUID();
        squad.name = name;
        squad.color = color;
        return squad;
    }

    public Squad copy() {
        Squad copy = new Squad();
        copy.id = id;
        copy.name = name;
        copy.color = color;
        copy.alwaysAllied = alwaysAllied;
        copy.tablistTop = tablistTop;
        copy.friendlyFireProtection = friendlyFireProtection;
        copy.showInHud = showInHud;
        copy.members = new ArrayList<>();
        if (members != null) members.forEach(member -> copy.members.add(member.copy()));
        return copy;
    }

    public boolean contains(String uuid, String account) {
        if (members == null) return false;
        for (SquadMember member : members) {
            if (member != null && member.matches(uuid, account)) return true;
        }
        return false;
    }

    /** Index of the first squad (list order) containing the player, or -1. The first squad always wins. */
    public static int indexOfFirst(List<Squad> squads, String uuid, String account) {
        if (squads == null) return -1;
        for (int i = 0; i < squads.size(); i++) {
            Squad squad = squads.get(i);
            if (squad != null && squad.contains(uuid, account)) return i;
        }
        return -1;
    }

    public static Squad firstContaining(List<Squad> squads, String uuid, String account) {
        int index = indexOfFirst(squads, uuid, account);
        return index < 0 ? null : squads.get(index);
    }

    /**
     * Number of members that are online. {@code onlineKeys} holds lower-case account names and
     * lower-case UUID strings of everyone currently present in the tab list.
     */
    public int onlineCount(Set<String> onlineKeys) {
        int count = 0;
        if (members == null || onlineKeys == null) return 0;
        for (SquadMember member : members) {
            if (member != null && member.isOnline(onlineKeys)) count++;
        }
        return count;
    }
}
