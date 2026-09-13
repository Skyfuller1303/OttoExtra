package de.ottoextra.api.model;

/** A server-owned farm, including empty farms and explicit sharing permissions. */
public record AnimalBreedingFarm(String nameKey, String name, int horseCount,
                                 String ownerUuid, boolean legacy, String role, boolean canManage) {
    public AnimalBreedingFarm {
        nameKey = nameKey == null ? "" : nameKey.trim();
        name = name == null ? "" : name.trim();
        ownerUuid = ownerUuid == null ? "" : ownerUuid.trim();
        role = role == null ? "" : role.trim();
        horseCount = Math.max(0, horseCount);
        if (nameKey.isBlank() || !validName(name)) throw new IllegalArgumentException("Ungültiger Hof");
    }

    public boolean canEdit() {
        return "owner".equals(role) || "editor".equals(role) || "admin".equals(role);
    }

    public static boolean validName(String value) {
        if (value == null) return false;
        String name = value.trim();
        return !name.isEmpty() && name.codePointCount(0, name.length()) <= 64
                && name.codePoints().noneMatch(c -> c < 32 || c == 127 || c == '/' || c == '\\');
    }
}
