package de.ottoextra.tacticalfactions;

import java.util.UUID;

public final class CustomPoliticalEntity {
    public String id;
    public PoliticalEntityType type = PoliticalEntityType.FIEF;
    public String name = "";
    public String description = "";
    public String parentId;
    public String bannerFile;

    public CustomPoliticalEntity() { }

    public CustomPoliticalEntity(String id, PoliticalEntityType type, String name, String description,
                                 String parentId, String bannerFile) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.bannerFile = bannerFile;
    }

    public static CustomPoliticalEntity create() {
        return new CustomPoliticalEntity("custom:" + UUID.randomUUID(), PoliticalEntityType.FIEF,
                "", "", null, null);
    }

    public CustomPoliticalEntity copy() {
        return new CustomPoliticalEntity(id, type, name, description, parentId, bannerFile);
    }
}
