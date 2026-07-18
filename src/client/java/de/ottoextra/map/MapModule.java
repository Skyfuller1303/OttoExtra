package de.ottoextra.map;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionsServices;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class MapModule implements OttoExtraModule {

    private static volatile boolean overlayVisible = true;
    private boolean minimapHooked = false;

    private KeyBinding toggleKey;

    @Override
    public String id() {
        return "map";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.map.enabled;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        OttoExtraConfig.Map cfg = context.config().map;

        if (!XaeroMapBridge.isWorldmapInstalled()) {
            OttoExtra.LOGGER.info("[map] Xaero World Map nicht installiert — Overlay inaktiv.");
        }

        PoliticalOverlay.setUserGroupColors(cfg.groupColors);
        PoliticalOverlay.setUserLehenColors(cfg.lehenColors);
        PoliticalOverlay.setUserFactionColors(cfg.factionColors);
        PoliticalOverlay.setGroupNameOverrides(cfg.groupNameOverrides);

        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(
                        new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                            @Override
                            public net.minecraft.util.Identifier getFabricId() {
                                return OttoExtra.id("painted_map_reload");
                            }

                            @Override
                            public void reload(net.minecraft.resource.ResourceManager manager) {
                                PaintedMapRenderer.onResourceReload();
                            }
                        });

        PaintedWorldMapHook.install(cfg,
                () -> overlayVisible && (!cfg.onlyOnOttonien || context.isOnOttonien()));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!XaeroMapBridge.isWorldmapScreen(screen)) {
                return;
            }
            if (context.isOnOttonien() && RegionsServices.data() != null) {
                RegionsServices.data().syncNow();
            }
            LehenPolygonStore.ensureLoaded();
            PoliticalOverlay.clearSelection();

            try {
                var buttons = net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen);

                int size = 20;
                int bx = screen.width - 42;
                int by = screen.height - 22;
                int bottom = -1;
                for (var w : buttons) {
                    if (w.getX() >= screen.width - 26 && w.getWidth() <= 24
                            && w.getY() + w.getHeight() > bottom) {
                        bottom = w.getY() + w.getHeight();
                        size = w.getHeight();
                        bx = w.getX() - size - 2;
                        by = w.getY() + w.getHeight() - size;
                    }
                }
                buttons.add(new PoliticalToggleButton(bx, by, size, context.config()));

                if (context.config().map.showCalibrationArrows) {
                    int ns = 12;
                    int px0 = bx - 3 * ns - 6;
                    int py0 = by + size - 2 * ns;
                    buttons.add(new MapNudgeButton(px0 + ns, py0 - ns, ns, 0, -1, context.config()));
                    buttons.add(new MapNudgeButton(px0, py0, ns, -1, 0, context.config()));
                    buttons.add(new MapNudgeButton(px0 + ns, py0, ns, 0, 0, context.config()));
                    buttons.add(new MapNudgeButton(px0 + 2 * ns, py0, ns, 1, 0, context.config()));
                    buttons.add(new MapNudgeButton(px0 + ns, py0 + ns, ns, 0, 1, context.config()));
                }

                buttons.removeIf(w -> {
                    String msg = w.getMessage() != null ? w.getMessage().getString() : "";
                    return msg.matches("(?i).*switch to (the )?(nether|overworld|end).*");
                });
            } catch (Throwable t) {
                OttoExtra.LOGGER.debug("[map] Politik-Button nicht einfuegbar: {}", t.toString());
            }

            double[] press = {Double.NaN, Double.NaN};
            net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.afterMouseClick(screen)
                    .register((s, click, handled) -> {
                        if (click.button() == 0) {
                            press[0] = click.x();
                            press[1] = click.y();
                        }
                        return handled;
                    });
            net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.afterMouseRelease(screen)
                    .register((s, click, handled) -> {
                        if (click.button() != 0 || Double.isNaN(press[0])) {
                            return handled;
                        }
                        double dx = click.x() - press[0];
                        double dy = click.y() - press[1];
                        press[0] = Double.NaN;
                        if (dx * dx + dy * dy > 16.0) {
                            return handled;
                        }
                        if (!overlayVisible || XaeroMapBridge.isDisabled()
                                || (cfg.onlyOnOttonien && !context.isOnOttonien())
                                || isOverButton(s, click.x(), click.y())) {
                            return handled;
                        }
                        try {
                            XaeroMapBridge.View view = XaeroMapBridge.view(s);
                            if (MapSelectionPanel.handleClick(s, view, click.x(), click.y())) {
                                return true;
                            }
                            if (PoliticalOverlay.handleClick(s, view, click.x(), click.y())) {
                                return true;
                            }
                        } catch (Throwable t) {
                            OttoExtra.LOGGER.debug("[map] Klick-Fokus-Fehler: {}", t.toString());
                        }
                        return handled;
                    });
            ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                if (!overlayVisible || XaeroMapBridge.isDisabled()) {
                    return;
                }
                if (cfg.onlyOnOttonien && !context.isOnOttonien()) {
                    return;
                }
                try {
                    XaeroMapBridge.View view = XaeroMapBridge.view(s);
                    if (view != null) {

                        MapOverlayRenderer.render(drawContext, view, cfg, mouseX, mouseY);
                        ParchmentMapOverlay.render(drawContext, view, cfg);
                        MapSelectionPanel.render(drawContext, view, cfg);
                    }
                } catch (Throwable t) {

                    OttoExtra.LOGGER.warn("[map] Overlay-Fehler: {}", t.toString());
                }
            });
        });

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (!minimapHooked) {
                    minimapHooked = de.ottoextra.map.xaero.XaeroMinimapBorders.tryRegister(
                            cfg, () -> overlayVisible && (!cfg.onlyOnOttonien || context.isOnOttonien()));
                }
            });

            net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                    (drawContext, tickCounter) -> {
                        if (!overlayVisible
                                || (cfg.onlyOnOttonien && !context.isOnOttonien())) {
                            return;
                        }
                        de.ottoextra.map.xaero.MinimapBannerOverlay.render(drawContext, cfg);
                    });
        } else {
            OttoExtra.LOGGER.info("[map] Xaero Minimap nicht installiert — Minimap-Grenzen inaktiv.");
        }

        toggleKey = new KeyBinding(
                "key.ottoextra.map_toggle",
                InputUtil.Type.KEYSYM,
                keyCode(cfg.toggleKey, GLFW.GLFW_KEY_K),
                KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {

                if (!XaeroMapBridge.isWorldmapScreen(client.currentScreen)) {
                    continue;
                }
                overlayVisible = !overlayVisible;
                OttoExtra.LOGGER.info("[map] Overlay {}", overlayVisible ? "an" : "aus");
                if (client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.translatable(
                            overlayVisible ? "ottoextra.map.overlayOn" : "ottoextra.map.overlayOff"), true);
                }
            }
        });

        OttoExtra.LOGGER.info("[map] initialisiert (Xaero-Overlay: Grenzen/Namen/Wappen).");
    }

    private static boolean isOverButton(net.minecraft.client.gui.screen.Screen screen,
                                        double mouseX, double mouseY) {
        try {
            for (var widget : net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen)) {
                if (widget.visible && widget.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {

        }
        return false;
    }

    @Override
    public void onServerJoin(OttoExtraContext context) {
        LehenPolygonStore.ensureLoaded();
    }

    private static int keyCode(String translationKey, int fallback) {
        if (translationKey == null || translationKey.isBlank()) {
            return fallback;
        }
        try {
            return InputUtil.fromTranslationKey(translationKey).getCode();
        } catch (Throwable t) {
            return fallback;
        }
    }
}
