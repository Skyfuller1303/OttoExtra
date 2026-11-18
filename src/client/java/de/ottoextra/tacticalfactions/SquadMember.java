package de.ottoextra.tacticalfactions;

import java.util.Locale;

/** One member of a {@link Squad}: a Minecraft account name, optionally with its UUID. */
public final class SquadMember {
    public String account;
    public String uuid;
    public boolean leader;

    public SquadMember() { }

    public SquadMember(String account, String uuid, boolean leader) {
        this.account = account;
        this.uuid = uuid;
        this.leader = leader;
    }

    public SquadMember copy() {
        return new SquadMember(account, uuid, leader);
    }

    /** Online means present in the tab list; {@code onlineKeys} holds lower-case names and UUIDs. */
    public boolean isOnline(java.util.Set<String> onlineKeys) {
        if (onlineKeys == null) return false;
        if (account != null && onlineKeys.contains(account.toLowerCase(Locale.ROOT))) return true;
        return uuid != null && onlineKeys.contains(uuid.toLowerCase(Locale.ROOT));
    }

    /** A member matches by UUID when both sides know it, otherwise by account name (case-insensitive). */
    public boolean matches(String otherUuid, String otherAccount) {
        if (uuid != null && otherUuid != null && uuid.equalsIgnoreCase(otherUuid.trim())) return true;
        return account != null && otherAccount != null
                && account.toLowerCase(Locale.ROOT).equals(otherAccount.trim().toLowerCase(Locale.ROOT));
    }
}
