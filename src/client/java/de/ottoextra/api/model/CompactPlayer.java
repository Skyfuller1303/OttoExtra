package de.ottoextra.api.model;

import com.google.gson.annotations.SerializedName;

public record CompactPlayer(
        @SerializedName("entity_key") String entityKey,
        String uuid,
        String name,
        @SerializedName("minecraft_name") String minecraftName,
        @SerializedName("rp_name") String rpName,
        String title,
        String rank,
        String state,
        String faction,
        @SerializedName("faction_name") String factionName
) {
}
