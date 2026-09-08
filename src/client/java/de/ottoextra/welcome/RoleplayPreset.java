package de.ottoextra.welcome;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.nametags.NameTagMode;

/** Local display presets; applying one leaves online consent and player data intact. */
enum RoleplayPreset {
    COMFORT("comfort", NameTagMode.NORMAL),
    BALANCED("balanced", NameTagMode.REALISTIC),
    IMMERSIVE("immersive", NameTagMode.HIDE_ALL);

    private final String key;
    private final NameTagMode nametagMode;

    RoleplayPreset(String key, NameTagMode nametagMode) {
        this.key = "ottoextra.setup.rp." + key;
        this.nametagMode = nametagMode;
    }

    String translationKey(String part) {
        return key + "." + part;
    }

    void apply(OttoExtraConfig config) {
        boolean immersive = this == IMMERSIVE;
        boolean comfort = this == COMFORT;
        OttoExtraConfig.RpNames names = config.rpnames;
        names.enabled = true;
        names.showInSprechen = true;
        names.showInReden = true;
        names.showInRufen = true;
        names.showInBruellen = true;
        names.showInFluestern = true;
        names.showInMurmeln = true;
        names.showInHilfe = false;
        names.showInOfftopic = false;
        names.showInOoc = false;
        names.showInAllChannels = false;
        names.tablistEnabled = !comfort;
        names.tablistShowTitle = !immersive;
        names.showUnknownAsUnknown = immersive;
        names.unknownShowAccount = !immersive;
        // Proactive meeting deliberately hides even API-imported identities.
        names.proactiveMeet = immersive;
        names.inspectShowAccount = comfort;

        OttoExtraConfig.Nametags tags = config.nametags;
        tags.enabled = true;
        tags.mode = nametagMode;
        tags.showTitle = !comfort;
        tags.showRpName = !comfort;
        tags.showPlayerName = comfort;
    }
}
