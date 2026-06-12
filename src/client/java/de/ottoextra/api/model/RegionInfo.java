package de.ottoextra.api.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Statusinformationen zu einer Region (Spielbarkeit, Hinweise, Banner-Vorschlag).
 *
 * <p>{@code npc_village} liefert die API uneinheitlich: mal als String, mal als
 * Objekt (NpcVillage-Record). Deshalb als rohes {@link JsonElement} mit
 * tolerantem Accessor {@link #npcVillageLabel()} — sonst reisst ein einzelnes
 * Objekt den gesamten Bootstrap-Parse ab.</p>
 */
public record RegionInfo(
        String region_id,
        String name,
        Boolean playable,
        String status_label,
        String note,
        JsonElement npc_village,
        String suggested_banner_path,
        Integer player_gathering
) {
    /** NPC-Dorf-Name, egal ob die API String oder Objekt liefert; sonst "". */
    public String npcVillageLabel() {
        if (npc_village == null || npc_village.isJsonNull()) {
            return "";
        }
        if (npc_village.isJsonPrimitive()) {
            return npc_village.getAsString();
        }
        if (npc_village.isJsonObject()) {
            JsonObject obj = npc_village.getAsJsonObject();
            for (String key : new String[]{"name", "label", "village", "id"}) {
                JsonElement e = obj.get(key);
                if (e != null && e.isJsonPrimitive()) {
                    return e.getAsString();
                }
            }
        }
        return "";
    }
}
