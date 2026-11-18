package de.ottoextra.ailments;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read access to known conditions, for the name tag, the own HUD and addons (for example a squad HUD
 * of Tactical Factions): {@code Conditions.of(uuid)}. Own conditions come from the server menu, those
 * of others from the shared state; both lists are immutable and empty if nothing is known.
 */
public final class Conditions {

    /** A shared state is dropped this long after the server last updated it (server rule: 20 min). */
    static final long REMOTE_MAX_AGE_MS = 20 * 60_000L;

    private record Entry(List<Condition> conditions, long updatedAt) {
    }

    private static volatile List<Condition> own;
    private static volatile long ownReadAt;
    private static volatile UUID ownUuid;
    private static final Map<UUID, Entry> REMOTE = new ConcurrentHashMap<>();
    private static final Set<UUID> WATCHED = ConcurrentHashMap.newKeySet();

    private Conditions() {
    }

    /** Conditions of that player; own ones are returned for the local player. */
    public static List<Condition> of(UUID uuid) {
        if (uuid == null) return List.of();
        if (uuid.equals(ownUuid) && own != null) return own;
        Entry e = REMOTE.get(uuid);
        if (e == null) return List.of();
        if (e.updatedAt() > 0 && System.currentTimeMillis() - e.updatedAt() > REMOTE_MAX_AGE_MS) {
            REMOTE.remove(uuid);
            return List.of();
        }
        return e.conditions();
    }

    /** Own conditions, or null if the menu has not been read yet in this session. */
    public static List<Condition> own() {
        return own;
    }

    public static long ownReadAt() {
        return ownReadAt;
    }

    /** True if the player has at least one condition of the given kind. */
    public static boolean has(UUID uuid, String kind) {
        for (Condition c : of(uuid)) {
            if (c.kind().equals(kind)) return true;
        }
        return false;
    }

    /** Asks for the state of these players too (besides the visible ones); for example squad members. */
    public static void watch(Collection<UUID> uuids) {
        WATCHED.clear();
        if (uuids != null) WATCHED.addAll(uuids);
    }

    static Set<UUID> watched() {
        return WATCHED;
    }

    /** @return true if the stored list changed */
    static boolean setOwn(UUID uuid, List<Condition> conditions, long now) {
        List<Condition> copy = List.copyOf(conditions);
        boolean changed = !copy.equals(own);
        own = copy;
        ownReadAt = now;
        ownUuid = uuid;
        return changed;
    }

    static void putRemote(UUID uuid, List<Condition> conditions, long updatedAt) {
        if (conditions.isEmpty()) REMOTE.remove(uuid);
        else REMOTE.put(uuid, new Entry(List.copyOf(conditions), updatedAt));
    }

    static void removeRemote(UUID uuid) {
        REMOTE.remove(uuid);
    }

    static void clearAll() {
        own = null;
        ownReadAt = 0L;
        ownUuid = null;
        REMOTE.clear();
    }
}
