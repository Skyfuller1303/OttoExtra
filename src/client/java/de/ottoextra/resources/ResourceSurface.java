package de.ottoextra.resources;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

/** Surface height lookup in loaded client chunks. */
final class ResourceSurface {
    private ResourceSurface() { }

    /** Height of the top block at a column, or null when its chunk is not loaded. */
    static Integer yAt(ClientWorld world, int worldX, int worldZ) {
        if (world == null) return null;
        WorldChunk chunk = world.getChunkManager().getWorldChunk(worldX >> 4, worldZ >> 4);
        if (chunk == null) return null;
        int top = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, worldX & 15, worldZ & 15) - 1;
        return top < world.getBottomY() ? null : top;
    }
}
