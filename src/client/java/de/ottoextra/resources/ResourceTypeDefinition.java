package de.ottoextra.resources;

record ResourceTypeDefinition(
        String type,
        String name,
        String materialLabel,
        String iconPath,
        boolean markerOnly) {
}
