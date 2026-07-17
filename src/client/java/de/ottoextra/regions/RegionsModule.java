package de.ottoextra.regions;
import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
public final class RegionsModule implements OttoExtraModule {
    private KeyBinding menuKey;
    private boolean menuKeyNameResolved = false;
    @Override
    public String id() {
        return "regions";
    }
    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.regions.enabled;
    }
    @Override
    public void onInitializeClient(OttoExtraContext context) {
        RegionMessageService.init(context.config());
        RegionsServices.init(context.api());
        RegionNotificationOverlay.configure(context.config().regions);
        HudRenderCallback.EVENT.register(RegionNotificationOverlay::render);
        int code = keyCode(context.config().regions.menuKey, GLFW.GLFW_KEY_L);
        menuKey = new KeyBinding(
                "key.ottoextra.region_menu",
                InputUtil.Type.KEYSYM,
                code,
                KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(menuKey);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!menuKeyNameResolved) {
                menuKeyNameResolved = true;
                try {
                    RegionNotificationOverlay.setMenuKeyName(
                            menuKey.getBoundKeyLocalizedText().getString());
                } catch (Throwable ignored) {
                }
            }
            while (menuKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(RegionInfoScreen.current(null));
                }
            }
        });
        OttoExtra.LOGGER.info("[regions] initialisiert (Paket-Abfang + HUD + Lehen-Menue).");
    }
    @Override
    public void onServerJoin(OttoExtraContext context) {
        RegionDataService data = RegionsServices.data();
        if (data != null) {
            data.onServerJoin();
        }
    }
    @Override
    public void onDisconnect(OttoExtraContext context) {
        RegionMessageService.reset();
        RegionNotificationOverlay.clear();
        RegionDataService data = RegionsServices.data();
        if (data != null) {
            data.onDisconnect();
        }
    }
    @Override
    public void onClientStop(OttoExtraContext context) {
        RegionsServices.shutdown();
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
