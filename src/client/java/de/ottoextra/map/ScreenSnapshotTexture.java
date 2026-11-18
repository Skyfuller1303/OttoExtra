package de.ottoextra.map;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

/**
 * A GPU-side copy of the main framebuffer color attachment, registered as a normal texture so it can
 * be drawn through {@code DrawContext}. Created once per use; no CPU readback. Render thread only.
 */
final class ScreenSnapshotTexture extends AbstractTexture {

    private final Identifier id;

    private ScreenSnapshotTexture(Identifier id) {
        this.id = id;
    }

    /** Copies the current main framebuffer into a new registered texture; throws on any GPU API failure. */
    static ScreenSnapshotTexture capture(MinecraftClient client, Identifier id) {
        Framebuffer fb = client.getFramebuffer();
        int w = fb.textureWidth;
        int h = fb.textureHeight;
        if (w <= 0 || h <= 0) throw new IllegalStateException("Framebuffer leer: " + w + "x" + h);

        ScreenSnapshotTexture tex = new ScreenSnapshotTexture(id);
        try {
            var device = RenderSystem.getDevice();
            tex.glTexture = device.createTexture(
                    () -> "ottoextra worldmap open snapshot",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8, w, h, 1, 1);
            tex.glTextureView = device.createTextureView(tex.glTexture);
            tex.sampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(fb.getColorAttachment(), tex.glTexture, 0, 0, 0, 0, 0, w, h);
            client.getTextureManager().registerTexture(id, tex);
        } catch (RuntimeException e) {
            tex.close();
            throw e;
        }
        return tex;
    }

    /**
     * Copies the current main framebuffer into this texture again. Returns false if the framebuffer
     * size changed, in which case the texture keeps its old content.
     */
    boolean refresh(MinecraftClient client) {
        Framebuffer fb = client.getFramebuffer();
        if (glTexture == null || fb.textureWidth != glTexture.getWidth(0)
                || fb.textureHeight != glTexture.getHeight(0)) {
            return false;
        }
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(fb.getColorAttachment(),
                glTexture, 0, 0, 0, 0, 0, fb.textureWidth, fb.textureHeight);
        return true;
    }

    Identifier id() {
        return id;
    }

    /** Unregisters and frees the GPU texture (TextureManager closes the texture on destroy). */
    void release(MinecraftClient client) {
        client.getTextureManager().destroyTexture(id);
        close();
    }
}
