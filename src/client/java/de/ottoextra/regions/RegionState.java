package de.ottoextra.regions;
public record RegionState(String regionName, String hierarchyLine, String rawMessage, long updatedAtMs) {
    public boolean hasHierarchy() {
        return hierarchyLine != null && !hierarchyLine.isBlank();
    }
}
