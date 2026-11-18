package de.ottoextra.tacticalfactions;

import de.ottoextra.OttoExtraGate;
import de.ottoextra.addon.NametagContext;
import de.ottoextra.addon.NametagPresentation;
import de.ottoextra.addon.OttoExtraAddon;
import de.ottoextra.addon.RpBookPlayerContext;
import de.ottoextra.addon.RpBookPlayerField;
import de.ottoextra.config.settings.SettingsRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;

public final class TacticalFactionsAddon implements OttoExtraAddon {
    @Override
    public void registerSettings(SettingsRegistry registry) {
        addSettings(registry, TacticalFactions.config(), true);
    }

    @Override
    public void registerDefaultSettings(SettingsRegistry registry) {
        addSettings(registry, new TacticalConfig(), false);
    }

    private static void addSettings(SettingsRegistry registry, TacticalConfig config, boolean persist) {
        Runnable save = persist ? config::save : () -> { };
        var module = registry.module("tacticalfactions", "ottoextra_tacticalfactions.module",
                "ottoextra_tacticalfactions.module.desc");
        var base = SettingsRegistry.tab(module, "ottoextra.set.tab.base");
        List<SettingsRegistry.Option> combatOptions = new ArrayList<>();
        combatOptions.add(SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.combat", "tactical.combatMode",
                () -> config.combatMode, value -> { config.combatMode = value; save.run(); }));
        combatOptions.add(SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.names", "tactical.showPlayerNames",
                () -> config.showPlayerNames, value -> { config.showPlayerNames = value; save.run(); }));
        combatOptions.add(SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.colors", "tactical.politicalColors",
                () -> config.politicalColors, value -> { config.politicalColors = value; save.run(); }));
        if (BuildVariant.friendlyFireProtectionAvailable()) {
            combatOptions.add(SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.friendlyFire",
                            "tactical.preventFriendlyFire", () -> config.preventFriendlyFire,
                            value -> { config.preventFriendlyFire = value; save.run(); })
                    .tooltip("ottoextra_tacticalfactions.option.friendlyFire.warning"));
            combatOptions.add(SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.neutralProtection",
                    "tactical.protectNeutralPlayers", () -> config.protectNeutralPlayers,
                    value -> { config.protectNeutralPlayers = value; save.run(); }));
        }
        combatOptions.add(SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.retaliationMarkers",
                "tactical.retaliationMarkers", () -> config.retaliationMarkers,
                value -> { config.retaliationMarkers = value; save.run(); }));
        combatOptions.add(SettingsRegistry.Option.action("ottoextra_tacticalfactions.option.book", "tactical.openBook", () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new PoliticsScreen(client.currentScreen));
        }));
        SettingsRegistry.card(base, "ottoextra_tacticalfactions.card.combat",
                BuildVariant.friendlyFireProtectionAvailable()
                        ? "ottoextra_tacticalfactions.card.combat.desc.protection"
                        : "ottoextra_tacticalfactions.card.combat.desc",
                combatOptions.toArray(SettingsRegistry.Option[]::new));
        SettingsRegistry.card(base, "ottoextra_tacticalfactions.card.colors",
                "ottoextra_tacticalfactions.card.colors.desc",
                SettingsRegistry.Option.color("ottoextra_tacticalfactions.option.allyColor", "tactical.allyColor",
                        () -> config.allyColor, value -> { config.allyColor = value; save.run(); }),
                SettingsRegistry.Option.color("ottoextra_tacticalfactions.option.hostileColor", "tactical.hostileColor",
                        () -> config.hostileColor, value -> { config.hostileColor = value; save.run(); }));
        SettingsRegistry.card(base, "ottoextra_tacticalfactions.card.tablist",
                "ottoextra_tacticalfactions.card.tablist.desc",
                SettingsRegistry.Option.bool("ottoextra_tacticalfactions.option.tablistHostilesFirst",
                                "tactical.tablistHostilesFirst",
                                () -> config.tablistHostilesFirst,
                                value -> { config.tablistHostilesFirst = value; save.run(); })
                        .tooltip("ottoextra_tacticalfactions.option.tablistHostilesFirst.tip"));
    }

    @Override
    public List<de.ottoextra.ui.hudlayout.HudLayoutElement> hudLayoutElements() {
        return List.of(new SquadHudLayoutElement());
    }

    @Override
    public NametagPresentation processNametag(NametagContext context, NametagPresentation current) {
        TacticalConfig config = TacticalFactions.config();
        MinecraftClient client = MinecraftClient.getInstance();
        if (!config.combatMode || !context.onlinePlayer() || !OttoExtraGate.isOnOttonien(client)) return current;

        Resolution resolution = TacticalFactions.politics().resolve(context.playerUuid(), context.accountName());
        Text name = current.name() != null ? current.name() : context.vanillaName();
        name = withoutRepeatedTitle(name, current.title());
        Text account = current.account();
        if (config.showPlayerNames) {
            if (name == null || !name.getString().equalsIgnoreCase(context.accountName())) {
                account = Text.literal(context.accountName());
            } else {
                account = null;
            }
        }
        if (config.politicalColors && resolution.relation() != Relation.NEUTRAL) {
            String squadHex = TacticalFactions.politics().squadColor(
                    resolution, context.playerUuid(), context.accountName());
            String color = squadHex != null ? squadHex
                    : resolution.relation() == Relation.ALLIED ? config.allyColor : config.hostileColor;
            name = colored(name, color);
            if (account != null) account = colored(account, color);
        }
        name = RetaliationMarkers.decorate(context.playerUuid(), name);
        return new NametagPresentation(true, true, current.title(), name, account, true);
    }

    static Text withoutRepeatedTitle(Text name, Text title) {
        if (name == null || title == null) return name;
        String value = name.getString().trim();
        String titleValue = title.getString().trim();
        if (value.isEmpty() || titleValue.isEmpty()) return name;
        String clean = value;
        if (startsWithToken(value, titleValue)) {
            clean = value.substring(titleValue.length()).replaceFirst("^[\\s|:,-]+", "").trim();
        } else if (endsWithToken(value, titleValue)) {
            clean = value.substring(0, value.length() - titleValue.length())
                    .replaceFirst("[\\s|:,-]+$", "").trim();
        }
        return clean.equals(value) || clean.isEmpty() ? name
                : Text.literal(clean).setStyle(name.getStyle());
    }

    private static boolean startsWithToken(String value, String token) {
        return value.regionMatches(true, 0, token, 0, token.length())
                && value.length() > token.length()
                && isSeparator(value.charAt(token.length()));
    }

    private static boolean endsWithToken(String value, String token) {
        int start = value.length() - token.length();
        return start > 0 && value.regionMatches(true, start, token, 0, token.length())
                && isSeparator(value.charAt(start - 1));
    }

    private static boolean isSeparator(char value) {
        return Character.isWhitespace(value) || value == '|' || value == ':' || value == ',' || value == '-';
    }

    @Override
    public List<RpBookPlayerField> rpBookPlayerFields(RpBookPlayerContext context) {
        Resolution resolution = TacticalFactions.politics().resolve(context.playerUuid(), context.accountName());
        return List.of(
                new RpBookPlayerField(Text.translatable("ottoextra_tacticalfactions.player_field"),
                        Text.translatable(resolution.relation().translationKey()),
                        () -> MinecraftClient.getInstance().setScreen(new PlayerRelationScreen(
                                context.parent(), context.playerUuid(), context.accountName()))),
                new RpBookPlayerField(Text.empty(),
                        Text.translatable("ottoextra_tacticalfactions.book.button"),
                        () -> MinecraftClient.getInstance().setScreen(new PoliticsScreen(context.parent()))));
    }

    static Text sourceText(String source) {
        if (source != null && source.startsWith("inherited:")) {
            return Text.translatable("ottoextra_tacticalfactions.source.inherited",
                    source.substring("inherited:".length()));
        }
        if (source != null && source.startsWith("squad:")) {
            return Text.translatable("ottoextra_tacticalfactions.source.squad",
                    source.substring("squad:".length()));
        }
        return Text.translatable("ottoextra_tacticalfactions.source." +
                (source == null || source.isBlank() ? "default" : source));
    }

    private static Text colored(Text text, String hex) {
        if (text == null || hex == null) return text;
        try {
            int rgb = Integer.parseInt(hex.substring(1), 16);
            return text.copy().setStyle(text.getStyle().withColor(TextColor.fromRgb(rgb)));
        } catch (RuntimeException ignored) {
            return text;
        }
    }
}
