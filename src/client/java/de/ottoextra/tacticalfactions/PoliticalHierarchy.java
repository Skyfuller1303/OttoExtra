package de.ottoextra.tacticalfactions;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure landed/hierarchy computation, independent of Minecraft. */
final class PoliticalHierarchy {
    private PoliticalHierarchy() { }

    /**
     * Landed factions are the ones that currently hold a region ({@code current_faction} of any region).
     * The record's own region fields go stale, so they are only used when no region data is available.
     * Returns null in that fallback case; callers then use {@link #isLanded}.
     */
    static Set<String> landedIds(List<RegionRecord> regions) {
        if (regions == null || regions.isEmpty()) return null;
        Set<String> landed = new HashSet<>();
        for (RegionRecord region : regions) {
            FactionRecord holder = region != null ? region.current_faction() : null;
            if (holder != null && holder.uuid() != null && !holder.uuid().isBlank()) landed.add(holder.uuid());
        }
        return landed;
    }

    static boolean isLanded(FactionRecord faction, Set<String> landed) {
        if (faction == null) return false;
        return landed != null ? landed.contains(faction.uuid()) : faction.isLanded();
    }

    /**
     * Child uuid to liege uuid. Explicit vassal lists (older API) win over the liege name. Only
     * landed factions take part: an unlanded faction never gets a parent and never is one.
     */
    static Map<String, String> parents(List<FactionRecord> records, Set<String> landed) {
        Map<String, String> parent = new HashMap<>();
        Map<String, FactionRecord> byUuid = new HashMap<>();
        for (FactionRecord faction : records) if (faction.uuid() != null) byUuid.put(faction.uuid(), faction);
        for (FactionRecord faction : records) {
            if (faction.vassal_uuids() == null || !isLanded(faction, landed)) continue;
            for (String child : faction.vassal_uuids()) {
                if (child != null && isLanded(byUuid.get(child), landed)) parent.putIfAbsent(child, faction.uuid());
            }
        }
        for (FactionRecord faction : records) {
            if (faction.vassal_recursive_uuids() == null || !isLanded(faction, landed)) continue;
            for (String child : faction.vassal_recursive_uuids()) {
                if (child != null && !child.equals(faction.uuid()) && isLanded(byUuid.get(child), landed)) {
                    parent.putIfAbsent(child, faction.uuid());
                }
            }
        }
        addNameBasedParents(records, parent, landed);
        return parent;
    }

    /** {@code landed == null} disables the landed restriction (legacy behaviour). */
    static void addNameBasedParents(List<FactionRecord> records, Map<String, String> parent, Set<String> landed) {
        Map<String, String> factionByName = new HashMap<>();
        Map<String, String> factionByLeaderName = new HashMap<>();
        for (FactionRecord faction : records) {
            if (faction.uuid() == null || faction.uuid().isBlank()) continue;
            if (landed != null && !landed.contains(faction.uuid())) continue;
            if (faction.name() != null && !faction.name().isBlank()) {
                factionByName.putIfAbsent(normalize(faction.name()), faction.uuid());
            }
            if (faction.leader_name() != null && !faction.leader_name().isBlank()) {
                factionByLeaderName.putIfAbsent(normalize(faction.leader_name()), faction.uuid());
            }
        }
        for (FactionRecord faction : records) {
            if (faction.uuid() == null || parent.containsKey(faction.uuid()) || faction.lord_name() == null) continue;
            if (landed != null && !landed.contains(faction.uuid())) continue;
            // lord_name normally contains the liege faction (Geldern -> Holdstewik),
            // while old snapshots may still contain the leader account.
            String liege = factionByName.get(normalize(faction.lord_name()));
            if (liege == null) liege = factionByLeaderName.get(normalize(faction.lord_name()));
            if (liege != null && !liege.equals(faction.uuid())) parent.put(faction.uuid(), liege);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
