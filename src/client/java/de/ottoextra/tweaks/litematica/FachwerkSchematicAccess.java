package de.ottoextra.tweaks.litematica;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.lang.reflect.Method;

/** Lazy optional access to Litematica's projected schematic world. */
final class FachwerkSchematicAccess {

    private static boolean resolved;
    private static Method schematicWorldGetter;

    private FachwerkSchematicAccess() {
    }

    static BlockState blockState(BlockPos pos) {
        Method getter = getter();
        if (getter == null) {
            return null;
        }
        try {
            Object world = getter.invoke(null);
            return world instanceof BlockView blockView ? blockView.getBlockState(pos) : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static synchronized Method getter() {
        if (resolved) {
            return schematicWorldGetter;
        }
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded("litematica")) {
            return null;
        }
        try {
            Class<?> handler = Class.forName(
                    "fi.dy.masa.litematica.world.SchematicWorldHandler",
                    false,
                    FachwerkSchematicAccess.class.getClassLoader());
            schematicWorldGetter = handler.getMethod("getSchematicWorld");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            schematicWorldGetter = null;
        }
        return schematicWorldGetter;
    }
}
