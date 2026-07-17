package de.ottoextra.nametags;
public enum NameTagMode {
    NORMAL("ottoextra.nametags.mode.normal"),
    REALISTIC("ottoextra.nametags.mode.realistic"),
    HIDE_ALL("ottoextra.nametags.mode.hide_all");
    private final String translationKey;
    NameTagMode(String translationKey) {
        this.translationKey = translationKey;
    }
    public String translationKey() {
        return translationKey;
    }
    public NameTagMode next() {
        NameTagMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
