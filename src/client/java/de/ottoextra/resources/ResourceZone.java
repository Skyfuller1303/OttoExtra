package de.ottoextra.resources;

import net.minecraft.util.math.BlockPos;

public record ResourceZone(
        Type type,
        int x,
        int y,
        int z,
        int radius,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ) {

    public enum Type { POINT, CUBOID }

    public static ResourceZone point(int x, int y, int z, int radius) {
        if (radius < 1 || radius > 64) throw new IllegalArgumentException("radius");
        return new ResourceZone(Type.POINT, x, y, z, radius, 0, 0, 0, 0, 0, 0);
    }

    public static ResourceZone cuboid(BlockPos first, BlockPos second) {
        return new ResourceZone(Type.CUBOID, 0, 0, 0, 0,
                Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()), Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }

    /** Extent of a cuboid in blocks (corners included). */
    public int sizeX() { return maxX - minX + 1; }
    public int sizeY() { return maxY - minY + 1; }
    public int sizeZ() { return maxZ - minZ + 1; }

    /**
     * Point at the centre of a cuboid with radius ceil(max(dx, dz) / 2), limited to 1..64.
     * A point returns itself.
     */
    public ResourceZone asPoint() {
        if (type == Type.POINT) return this;
        int cx = (int) Math.floorDiv((long) minX + maxX, 2L);
        int cy = (int) Math.floorDiv((long) minY + maxY, 2L);
        int cz = (int) Math.floorDiv((long) minZ + maxZ, 2L);
        int radius = Math.clamp(((long) Math.max(sizeX(), sizeZ()) + 1) / 2, 1, 64);
        return point(cx, cy, cz, radius);
    }

    public boolean contains(BlockPos pos) {
        return contains(pos, 0);
    }

    public boolean contains(BlockPos pos, int padding) {
        if (padding < 0) throw new IllegalArgumentException("padding");
        if (type == Type.POINT) {
            long dx = (long) pos.getX() - x;
            long dy = (long) pos.getY() - y;
            long dz = (long) pos.getZ() - z;
            long paddedRadius = (long) radius + padding;
            return dx * dx + dy * dy + dz * dz <= paddedRadius * paddedRadius;
        }
        return (long) pos.getX() >= (long) minX - padding
                && (long) pos.getX() <= (long) maxX + padding
                && (long) pos.getY() >= (long) minY - padding
                && (long) pos.getY() <= (long) maxY + padding
                && (long) pos.getZ() >= (long) minZ - padding
                && (long) pos.getZ() <= (long) maxZ + padding;
    }
}
