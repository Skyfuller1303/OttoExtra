package de.ottoextra.crafting.werkstatt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-station panel state for the session, free of Minecraft: whether the panel is open, the chosen
 * tab, and whether the station ever showed fire buttons (only then the fuel parts are shown).
 * Unknown stations start closed, on the Auftrag tab and without fire.
 */
final class StationMemory {
    private final Map<String, Boolean> open = new HashMap<>();
    private final Map<String, WerkstattPanel.Tab> tabs = new HashMap<>();
    private final Set<String> fire = new HashSet<>();

    private static String key(String station) {
        return station == null || station.isBlank() ? "zeugtisch" : station;
    }

    boolean isOpen(String station) {
        return open.getOrDefault(key(station), false);
    }

    void setOpen(String station, boolean value) {
        open.put(key(station), value);
    }

    void toggle(String station) {
        setOpen(station, !isOpen(station));
    }

    WerkstattPanel.Tab tab(String station) {
        return tabs.getOrDefault(key(station), WerkstattPanel.Tab.JOB);
    }

    void setTab(String station, WerkstattPanel.Tab tab) {
        tabs.put(key(station), tab == null ? WerkstattPanel.Tab.JOB : tab);
    }

    boolean hasFire(String station) {
        return fire.contains(key(station));
    }

    void markFire(String station) {
        fire.add(key(station));
    }
}
