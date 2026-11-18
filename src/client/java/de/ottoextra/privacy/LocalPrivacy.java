package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraConfig;

/** Local logging is a separate choice; a historical default true is not evidence of consent. */
public final class LocalPrivacy {
    private LocalPrivacy() { }
    public static boolean chatAllowed(OttoExtraConfig config, boolean onOttonien) {
        var tweaks = config.tweaks;
        return tweaks.chatLog && tweaks.chatLogDecisionVersion == 1
                && tweaks.chatLogScope.equals(tweaks.chatLogAcceptedScope)
                && ("ALL".equals(tweaks.chatLogScope) || "OTTONIEN".equals(tweaks.chatLogScope) && onOttonien);
    }
    public static void decideChat(OttoExtraConfig config, boolean allow, String shownScope) {
        var tweaks = config.tweaks;
        tweaks.chatLogDecisionVersion = 1;
        tweaks.chatLogAcceptedScope = shownScope;
        tweaks.chatLog = allow && tweaks.chatLogScope.equals(shownScope);
    }
    public static void reconcile(OttoExtraConfig config) {
        var tweaks = config.tweaks;
        if (!"ALL".equals(tweaks.chatLogScope) && !"OTTONIEN".equals(tweaks.chatLogScope)) tweaks.chatLogScope = "OTTONIEN";
        if (tweaks.chatLogDecisionVersion != 1 || !tweaks.chatLogScope.equals(tweaks.chatLogAcceptedScope)) tweaks.chatLog = false;
        // A disabled Minecraft selection is authoritative even for old configurations.
        config.resourcepack.respectUserDisable = true;
    }
}
