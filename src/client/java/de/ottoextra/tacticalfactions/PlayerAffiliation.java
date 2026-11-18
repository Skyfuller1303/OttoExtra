package de.ottoextra.tacticalfactions;

public record PlayerAffiliation(String uuid, String accountName, String apiFactionId, String customFactionId) {
    public PlayerAffiliation(String uuid, String accountName, String factionId) {
        this(uuid, accountName, factionId, null);
    }

    /** A local membership deliberately takes precedence over the API faction. */
    public String factionId() {
        return customFactionId != null ? customFactionId : apiFactionId;
    }
}
