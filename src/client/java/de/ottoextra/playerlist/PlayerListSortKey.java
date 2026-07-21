package de.ottoextra.playerlist;

import java.util.UUID;

record PlayerListSortKey(
        boolean known,
        String lehen,
        int roleOrder,
        int titleGroupPriority,
        String titleGroup,
        int titleOrder,
        String title,
        String visibleName,
        String accountName,
        UUID uuid
) {
    static int compare(PlayerListSortKey left, PlayerListSortKey right) {
        int result = Boolean.compare(!left.known, !right.known);
        if (result != 0 || !left.known) {
            return result;
        }
        result = left.lehen.compareTo(right.lehen);
        if (result != 0) return result;
        result = Integer.compare(left.roleOrder, right.roleOrder);
        if (result != 0) return result;
        result = Integer.compare(right.titleGroupPriority, left.titleGroupPriority);
        if (result != 0) return result;
        result = left.titleGroup.compareTo(right.titleGroup);
        if (result != 0) return result;
        result = Integer.compare(left.titleOrder, right.titleOrder);
        if (result != 0) return result;
        result = left.title.compareTo(right.title);
        if (result != 0) return result;
        result = left.visibleName.compareTo(right.visibleName);
        if (result != 0) return result;
        result = left.accountName.compareTo(right.accountName);
        if (result != 0) return result;
        return left.uuid.compareTo(right.uuid);
    }
}
