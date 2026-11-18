package de.ottoextra.horses;

import de.ottoextra.horses.FarmShareWire.AnimalRec;
import de.ottoextra.horses.FarmShareWire.FarmRec;
import de.ottoextra.horses.FarmShareWire.Meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One shared farm list (docs/core-farms.md, "Höfe teilen"): a named group of farms plus the farm membership,
 * note and label of their animals. Stored inside the {@link AnimalBookStore} file of the server.
 *
 * <p>A list is either <b>owned</b> (created here: its farms are the user's own register farms, linked by the
 * local {@link AnimalBookStore.Farm#id()}, and the shared state is rebuilt from the register and the local
 * notes) or <b>joined</b> by code (its farms and animals exist only in this list). A joined list without
 * write code is read-only. The write code is a password; it is stored only locally.
 *
 * <p>All mutation happens on the client thread.
 */
public final class FarmShareList {
    /** Tombstones that still have to be written under the id the list had before "Codes erneuern". */
    public record Retired(String listId, String writeCode, Map<String, FarmRec> farms,
                          Map<String, AnimalRec> animals) {
    }

    String localId = "";
    String writeCode;
    String readCode = "";
    String listId = "";
    boolean owned;
    String name = "";
    String note = "";
    String icon = "haus";
    String color = "gruen";
    long metaUpdatedAt;
    boolean metaDirty;
    /** {@code since} of the next poll: server time of the last answer minus the overlap. */
    long since;
    /** Owned lists: local farm ids ({@link AnimalBookStore.Farm#id()}) that belong to the list. */
    Set<String> members = new LinkedHashSet<>();
    Map<String, FarmRec> farms = new LinkedHashMap<>();
    Map<String, AnimalRec> animals = new LinkedHashMap<>();
    Set<String> dirtyFarms = new LinkedHashSet<>();
    Set<String> dirtyAnimals = new LinkedHashSet<>();
    List<Retired> retired = new ArrayList<>();

    FarmShareList() {
    }

    /** Repairs collections that are missing in an older or edited file. */
    void sanitize() {
        if (localId == null) localId = "";
        if (readCode == null) readCode = "";
        if (listId == null) listId = "";
        if (name == null) name = "";
        if (note == null) note = "";
        if (!FarmSymbols.SHAPES.contains(icon)) icon = FarmSymbols.shape(FarmSymbols.DEFAULT);
        if (!FarmSymbols.COLORS.contains(color)) color = FarmSymbols.color(FarmSymbols.DEFAULT);
        if (members == null) members = new LinkedHashSet<>();
        if (farms == null) farms = new LinkedHashMap<>();
        if (animals == null) animals = new LinkedHashMap<>();
        if (dirtyFarms == null) dirtyFarms = new LinkedHashSet<>();
        if (dirtyAnimals == null) dirtyAnimals = new LinkedHashSet<>();
        if (retired == null) retired = new ArrayList<>();
        farms.values().removeIf(farm -> FarmShareWire.problem(farm) != null);
        animals.values().removeIf(animal -> FarmShareWire.problem(animal) != null);
        if (writeCode != null && FarmShareCodes.kind(writeCode) != FarmShareCodes.Kind.WRITE) writeCode = null;
    }

    /** Times from the future (the clock was changed) are clamped to now instead of dropped. */
    void clampFuture(long now) {
        farms.replaceAll((id, farm) -> farm.updatedAt() > now ? farm.withUpdatedAt(now) : farm);
        animals.replaceAll((uuid, animal) -> animal.updatedAt() > now ? animal.withUpdatedAt(now) : animal);
        if (metaUpdatedAt > now) metaUpdatedAt = now;
    }

    /** True when the list has a valid pair of codes and may contact the service. */
    boolean valid() {
        return FarmShareCodes.kind(readCode) == FarmShareCodes.Kind.READ
                && listId.equals(FarmShareCodes.listId(readCode))
                && (writeCode == null || readCode.equals(FarmShareCodes.readCode(writeCode)));
    }

    public String localId() { return localId; }
    public String name() { return name; }
    public String note() { return note; }
    public String symbol() { return FarmSymbols.key(icon, color); }
    public boolean owned() { return owned; }
    /** Read-only: joined with the read code only. */
    public boolean readOnly() { return writeCode == null; }
    public String writeCode() { return writeCode; }
    public String readCode() { return readCode; }
    public String listId() { return listId; }
    public Set<String> members() { return Set.copyOf(members); }

    /** Farms of the list without tombstones, by name. */
    public List<FarmRec> liveFarms() {
        List<FarmRec> result = new ArrayList<>();
        for (FarmRec farm : farms.values()) if (!farm.deleted()) result.add(farm);
        result.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return result;
    }

    public FarmRec farm(String id) {
        FarmRec farm = id == null ? null : farms.get(id);
        return farm == null || farm.deleted() ? null : farm;
    }

    /** Animals of one shared farm without tombstones. */
    public List<AnimalRec> animalsOf(String farmId) {
        List<AnimalRec> result = new ArrayList<>();
        for (AnimalRec animal : animals.values()) {
            if (!animal.deleted() && farmId != null && farmId.equals(animal.farmId())) result.add(animal);
        }
        return result;
    }

    public Meta meta() {
        return new Meta(name, note, icon, color, metaUpdatedAt);
    }

    void applyMeta(Meta meta) {
        name = meta.name();
        note = meta.note();
        icon = meta.icon();
        color = meta.color();
        metaUpdatedAt = meta.updatedAt();
    }

    /** True when the list has changes that still have to be uploaded. */
    boolean pending() {
        return metaDirty || !dirtyFarms.isEmpty() || !dirtyAnimals.isEmpty() || !retired.isEmpty();
    }
}
