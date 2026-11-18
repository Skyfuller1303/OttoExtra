package de.ottoextra.resources;

import de.ottoextra.addon.OttoExtraAddon;
import de.ottoextra.config.settings.SettingsRegistry;
import de.ottoextra.config.settings.SettingsRegistry.Option;
import de.ottoextra.ui.hudlayout.HudLayoutElement;

import java.util.List;

public final class OttoExtraResourcesAddon implements OttoExtraAddon {
    /** The timer panel in OttoExtra's "HUD anordnen" editor (position and scale). */
    @Override
    public List<HudLayoutElement> hudLayoutElements() {
        return List.of(new ResourceHudLayoutElement());
    }

    @Override
    public void registerSettings(SettingsRegistry registry) {
        build(registry, ResourcesConfig.active(), true);
    }

    @Override
    public void registerDefaultSettings(SettingsRegistry registry) {
        build(registry, new ResourcesConfig(), false);
    }

    private void build(SettingsRegistry registry, ResourcesConfig config, boolean persist) {
        Runnable save = persist ? config::save : () -> { };
        var module = registry.module("resources", "ottoextra.module.resources",
                "ottoextra.set.resources.desc");
        var base = SettingsRegistry.tab(module, "ottoextra.set.tab.base");
        SettingsRegistry.card(base, "ottoextra.set.resources.card",
                "ottoextra.set.resources.card.desc",
                Option.bool("ottoextra.config.resources.enabled", "resources.enabled",
                        () -> config.enabled, value -> {
                            config.enabled = value;
                            save.run();
                        }),
                Option.bool("ottoextra.config.resources.hints", "resources.hintsEnabled",
                        () -> config.hintsEnabled, value -> {
                            config.hintsEnabled = value;
                            save.run();
                        }),
                Option.cycle("ottoextra.config.resources.notice_style", "resources.noticeStyle",
                        () -> config.noticeStyle, value -> {
                            config.noticeStyle = value;
                            save.run();
                        }, ResourceNotice.STYLE_COMPACT, ResourceNotice.STYLE_DETAILED),
                Option.bool("ottoextra.config.resources.leave_notice", "resources.leaveNotice",
                        () -> config.leaveNotice, value -> {
                            config.leaveNotice = value;
                            save.run();
                        }),
                Option.bool("ottoextra.config.resources.autostart", "resources.autoStartOnBreak",
                        () -> config.autoStartOnBreak, value -> {
                            config.autoStartOnBreak = value;
                            save.run();
                        }),
                Option.bool("ottoextra.config.resources.outline", "resources.showOutlineParticles",
                        () -> config.showOutlineParticles, value -> {
                            config.showOutlineParticles = value;
                            save.run();
                        }),
                Option.bool("ottoextra.config.resources.sound", "resources.readySound",
                        () -> config.readySound, value -> {
                            config.readySound = value;
                            save.run();
                        }));
        SettingsRegistry.card(base, "ottoextra.set.resources.hud.card",
                "ottoextra.set.resources.hud.desc",
                Option.bool("ottoextra.config.resources.hud", "resources.hudEnabled",
                        () -> config.hudEnabled, value -> {
                            config.hudEnabled = value;
                            save.run();
                        }),
                Option.bool("ottoextra.config.resources.hud_all", "resources.hudShowAll",
                        () -> config.hudShowAll, value -> {
                            config.hudShowAll = value;
                            save.run();
                        }),
                Option.cycle("ottoextra.config.resources.hud_style", "resources.hudStyle",
                        () -> config.hudStyle, value -> {
                            config.hudStyle = value;
                            save.run();
                        }, ResourceHud.STYLE_FULL, ResourceHud.STYLE_COMPACT),
                Option.intVal("ottoextra.config.resources.hud_rows", "resources.hudMaxRows",
                        () -> config.hudMaxRows, value -> {
                            config.hudMaxRows = Math.clamp(value, 1, 30);
                            save.run();
                        }, 1, 30));
        SettingsRegistry.card(base, "ottoextra.set.resources.share.card",
                "ottoextra.set.resources.share.desc",
                Option.bool("ottoextra.config.resources.share", "resources.sharingEnabled",
                        () -> config.sharingEnabled, value -> {
                            config.sharingEnabled = value;
                            save.run();
                        }),
                Option.bool("ottoextra.config.resources.share_by", "resources.shareStartedBy",
                        () -> config.shareStartedBy, value -> {
                            config.shareStartedBy = value;
                            save.run();
                        }),
                Option.string("ottoextra.config.resources.share_url", "resources.shareBaseUrl",
                        () -> config.shareBaseUrl, value -> {
                            config.shareBaseUrl = ResourceShareApi.baseUri(value) == null
                                    ? ResourceShareApi.DEFAULT_BASE_URL : value;
                            save.run();
                        }));
        SettingsRegistry.card(base, "ottoextra.set.resources.minimap.card",
                "ottoextra.set.resources.minimap.desc",
                Option.bool("ottoextra.config.resources.minimap_indicator", "resources.showMinimapStatusIndicator",
                        () -> config.showMinimapStatusIndicator, value -> {
                            config.showMinimapStatusIndicator = value;
                            save.run();
                        }));
    }
}
