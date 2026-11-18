package de.ottoextra.resources.xaero;

import de.ottoextra.resources.OttoExtraResourcesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

/** Adds the resource entries to the context menu of an empty world map spot. */
public final class ResourceWorldmapMenu {
    private static java.lang.reflect.Field dimensionField;

    private ResourceWorldmapMenu() { }

    /** Dimension of the last right click, read by name so no Minecraft type enters the mixin. */
    private static Object clickedDimension(Object guiMap) throws ReflectiveOperationException {
        if (dimensionField == null || dimensionField.getDeclaringClass() != guiMap.getClass()) {
            dimensionField = guiMap.getClass().getDeclaredField("rightClickDim");
            dimensionField.setAccessible(true);
        }
        return dimensionField.get(guiMap);
    }

    /** Called from the GuiMap mixin; never lets an error escape into Xaero's menu code. */
    public static void add(Object guiMap, ArrayList<RightClickOption> options, int x, int z) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (options == null || client.world == null
                    || !client.world.getRegistryKey().equals(clickedDimension(guiMap))
                    || !(guiMap instanceof IRightClickableElement target)) return;
            if (OttoExtraResourcesClient.isFeatureAvailable()) {
                options.add(new RightClickOption(Text.translatable("ottoextra.resources.map.create_here")
                        .getString(), options.size(), target) {
                    @Override public void onAction(Screen screen) { OttoExtraResourcesClient.openCreateAt(screen, x, z); }
                });
            }
        } catch (Throwable error) {
            // The map's own menu must keep working.
        }
    }
}
