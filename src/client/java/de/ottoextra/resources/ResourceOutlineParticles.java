package de.ottoextra.resources;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/**
 * Shows a zone outline in the world with client-side particles (END_ROD: a small white spark that
 * stays visible in daylight and hangs almost still). Nothing is sent to the server.
 */
final class ResourceOutlineParticles {
    /** Ticks between two emissions. */
    static final int INTERVAL_TICKS = 10;
    /** Only outline points this close to the player are emitted. */
    static final double RANGE = 96.0;

    private ResourceOutlineParticles() { }

    static void emit(ClientWorld world, double playerX, double playerZ, ResourceZone zone) {
        double[] points = ResourceOutline.of(zone);
        double y = ResourceOutline.height(zone);
        double limit = RANGE * RANGE;
        for (int index = 0; index < points.length; index += 2) {
            double dx = points[index] - playerX;
            double dz = points[index + 1] - playerZ;
            if (dx * dx + dz * dz > limit) continue;
            world.addParticleClient(ParticleTypes.END_ROD, points[index], y, points[index + 1], 0.0, 0.0, 0.0);
        }
    }
}
