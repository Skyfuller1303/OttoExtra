package de.ottoextra.nametags;
import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
public final class NametagModule implements OttoExtraModule {
    private KeyBinding toggleKey;
    @Override
    public String id() {
        return "nametags";
    }
    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.nametags.enabled;
    }
    @Override
    public void onInitializeClient(OttoExtraContext context) {
        OttoExtraConfig config = context.config();
        NametagService.init(config.nametags);
        toggleKey = new KeyBinding(
                "key.ottoextra.nametag_mode",
                InputUtil.Type.KEYSYM,
                keyCode(config.nametags.toggleKey, GLFW.GLFW_KEY_N),
                KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                config.nametags.mode = config.nametags.mode.next();
                config.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable("ottoextra.nametags.toggled",
                            Text.translatable(config.nametags.mode.translationKey())), true);
                }
            }
        });
        OttoExtra.LOGGER.info("[nametags] initialisiert — Modus {}.", config.nametags.mode);
    }
    private static int keyCode(String name, int fallback) {
        try {
            return InputUtil.fromTranslationKey(name).getCode();
        } catch (Throwable t) {
            return fallback;
        }
    }
}
