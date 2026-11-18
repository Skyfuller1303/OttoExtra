package de.ottoextra.crafting.werkstatt;

/** Why a job stopped. The translation key is {@code ottoextra.werkstatt.stop.<name in lower case>}. */
public enum StopReason {
    USER,
    COMMAND,
    KEY,
    ESCAPE,
    MOVED,
    DAMAGE,
    DIED,
    OTHER_GUI,
    UNFOCUSED,
    MISSING_MATERIAL,
    MISSING_FUEL,
    NO_FIRE_BUTTON,
    INVENTORY_FULL,
    SERVER_REFUSED,
    NO_START,
    RESULT_TIMEOUT,
    TOO_MANY_FAILURES,
    WRONG_STATION,
    STATION_NOT_OPENED,
    NOT_IN_REACH,
    PAGE_TIMEOUT,
    PATH_NOT_FOUND,
    RECIPE_NOT_FOUND,
    CURSOR_BUSY,
    FIRE_CLOSED,
    DISCONNECTED,
    DISABLED;

    public String translationKey() {
        return "ottoextra.werkstatt.stop." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
