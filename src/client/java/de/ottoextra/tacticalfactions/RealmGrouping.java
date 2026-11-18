package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.PoliticalStateService.FactionView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Pure grouping of faction views into realms (everything below one top liege). */
final class RealmGrouping {
    private RealmGrouping() { }

    /** A top liege with all vassals (direct and indirect) in the order of the input list. */
    record Realm(FactionView root, List<FactionView> vassals, int total) {
        // total counts all vassals of the realm, even when a filter hides some of them.
    }

    /**
     * Groups by {@link FactionView#rootId()}. Realms keep the order in which their root first
     * appears; vassals keep their input order. If a root is missing from the input, the first
     * member seen takes its place so no entry is dropped.
     */
    static List<Realm> group(List<FactionView> views) {
        Map<String, FactionView> roots = new LinkedHashMap<>();
        Map<String, List<FactionView>> members = new LinkedHashMap<>();
        if (views != null) for (FactionView view : views) {
            String rootId = view.rootId() != null ? view.rootId() : view.id();
            members.computeIfAbsent(rootId, key -> new ArrayList<>());
            if (view.id().equals(rootId)) {
                roots.put(rootId, view);
            } else {
                members.get(rootId).add(view);
            }
        }
        List<Realm> result = new ArrayList<>();
        for (Map.Entry<String, List<FactionView>> entry : members.entrySet()) {
            FactionView root = roots.get(entry.getKey());
            List<FactionView> vassals = entry.getValue();
            if (root == null) {
                if (vassals.isEmpty()) continue;
                root = vassals.get(0);
                vassals = vassals.subList(1, vassals.size());
            }
            result.add(new Realm(root, List.copyOf(vassals), vassals.size()));
        }
        return result;
    }

    /**
     * Keeps the realms whose root or any vassal passes {@code keep}. Only passing vassals are
     * kept; the root stays as the header even if only a vassal matched.
     */
    static List<Realm> filter(List<Realm> realms, Predicate<FactionView> keep) {
        List<Realm> result = new ArrayList<>();
        for (Realm realm : realms) {
            List<FactionView> vassals = realm.vassals().stream().filter(keep).toList();
            if (keep.test(realm.root()) || !vassals.isEmpty()) {
                result.add(new Realm(realm.root(), vassals, realm.total()));
            }
        }
        return result;
    }

    /** An unlanded API faction without any vassals: shown outside the realms ("Ohne Land"). */
    static boolean isLoose(Realm realm) {
        return !realm.root().custom() && !realm.root().landed() && realm.total() == 0;
    }

    static int totalVassals(List<Realm> realms) {
        int total = 0;
        for (Realm realm : realms) total += realm.total();
        return total;
    }
}
