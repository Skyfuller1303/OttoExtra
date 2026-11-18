package de.ottoextra.crafting.werkstatt;

import java.util.ArrayList;
import java.util.List;

/**
 * One job: a recipe button of the station, how to reach it, how often to craft it and with which
 * fuel. Plain fields so it can be stored as JSON.
 */
public final class WerkstattJob {
    public enum Status { WAITING, RUNNING, PAUSED, STOPPED, DONE }

    /** One click on the way from the station's start page to the recipe page. */
    public static final class PathStep {
        public int slot;
        public String name = "";

        public PathStep() {
        }

        public PathStep(int slot, String name) {
            this.slot = slot;
            this.name = name == null ? "" : name;
        }
    }

    public String name = "";
    public String itemId = "";
    /** Item model component of the button, so the icon looks like on the server; may be empty. */
    public String itemModel = "";
    /** Stack size of the button, i.e. items per craft. */
    public int outputCount = 1;
    public List<String> lore = new ArrayList<>();
    public List<PathStep> path = new ArrayList<>();
    /** Slot of the recipe on its page; only a hint, the recipe is found by name and lore. */
    public int slot = -1;
    /** Station id ({@link WerkstattText#stationId}); the job only runs there. Old files: Zeugtisch. */
    public String station = "zeugtisch";
    public int quantity = 1;
    public int done;
    /** Crafts the server reported as failed (or without any result); they do not count as done. */
    public int failed;
    /** Display name of the chosen fuel ("Stock", "Steinkohle", "Torf"); empty = first fire button. */
    public String fuel = "";
    public Status status = Status.WAITING;
    public StopReason stopReason;

    private transient RecipeLore parsed;

    public RecipeLore recipe() {
        if (parsed == null) {
            parsed = RecipeLore.parse(lore);
        }
        return parsed;
    }

    /** Identity of the recipe for "Gemerkte Rezepte": name plus ingredient block. */
    public String recipeKey() {
        RecipeLore r = recipe();
        return WerkstattText.normalize(name) + "|" + (r.known() ? r.groups().toString() : "?");
    }

    /** Passes made so far, successful or failed. */
    public int attempts() {
        return done + failed;
    }

    /** True when every planned pass was made; the quantity counts passes, failed ones included. */
    public boolean complete() {
        return attempts() >= quantity;
    }


    /** Passes still to make. Failures are normal and never made up. */
    public int remaining() {
        return Math.max(0, quantity - attempts());
    }

    /** Readable path: page names and the slot, e.g. "Zeugschmieden > Slot 11". */
    public String pathText() {
        StringBuilder b = new StringBuilder();
        for (PathStep step : path) {
            if (b.length() > 0) {
                b.append(" > ");
            }
            b.append(step.name);
        }
        if (b.length() > 0) {
            b.append(" > ");
        }
        return b.append("Slot ").append(slot).toString();
    }

    /** A new waiting job for the same recipe, with the same quantity and fuel. */
    public WerkstattJob copyFresh() {
        WerkstattJob j = new WerkstattJob();
        j.name = name;
        j.itemId = itemId;
        j.itemModel = itemModel;
        j.outputCount = outputCount;
        j.lore = new ArrayList<>(lore);
        j.path = new ArrayList<>();
        for (PathStep s : path) {
            j.path.add(new PathStep(s.slot, s.name));
        }
        j.slot = slot;
        j.station = station;
        j.quantity = Math.max(1, quantity);
        j.fuel = fuel;
        return j;
    }

    /** Repairs a job read from a file. */
    void sanitize() {
        if (name == null) name = "";
        if (itemId == null) itemId = "";
        if (itemModel == null) itemModel = "";
        if (lore == null) lore = new ArrayList<>();
        if (path == null) path = new ArrayList<>();
        path.removeIf(s -> s == null);
        for (PathStep s : path) {
            if (s.name == null) s.name = "";
        }
        if (fuel == null) fuel = "";
        if (station == null || station.isBlank()) station = "zeugtisch";
        if (status == null) {
            status = Status.WAITING;
        } else if (status == Status.RUNNING || status == Status.PAUSED) {
            // A job never keeps running across a restart; it waits for a click to resume.
            status = Status.STOPPED;
            if (stopReason == null) stopReason = StopReason.DISCONNECTED;
        }
        quantity = Math.max(1, Math.min(CraftPlan.MAX_CRAFTS, quantity));
        done = Math.max(0, Math.min(quantity, done));
        failed = Math.max(0, failed);
        outputCount = Math.max(1, outputCount);
        parsed = null;
    }
}
