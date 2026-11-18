package de.ottoextra.tacticalfactions;

public final class TacticalFactions {
    private static final TacticalConfig CONFIG = TacticalConfig.active();
    private static final PoliticalStateService POLITICS = new PoliticalStateService(CONFIG);

    private TacticalFactions() { }
    public static TacticalConfig config() { return CONFIG; }
    public static PoliticalStateService politics() { return POLITICS; }
}
