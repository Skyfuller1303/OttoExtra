package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.settings.OttoExtraSettingsScreen;
import de.ottoextra.config.settings.SettingsRegistry;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.map.PaintedMapRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/** Test-only checks on the real client/render thread. */
final class AuditFixClientChecks {
    static void gpu(ClientGameTestContext ctx) {
        ctx.runOnClient(c -> {
            try {
                var resize = PaintedMapRenderer.class.getDeclaredMethod("ensureCopyTexture", int.class, int.class);
                var invalidate = PaintedMapRenderer.class.getDeclaredMethod("invalidateGpu");
                resize.setAccessible(true); invalidate.setAccessible(true);
                invalidate.invoke(null);
                try {
                    for (int i = 0; i < 16; i++) {
                        resize.invoke(null, 32 + i, 32);
                        GlTexture previous = field(PaintedMapRenderer.class, null, "copyTexture");
                        GpuTextureView view = field(PaintedMapRenderer.class, null, "copyView");
                        check(GL11.glIsTexture(previous.getGlId()), "Expected real allocated GL texture");
                        resize.invoke(null, 64 + i, 32);
                        check(previous.isClosed() && view.isClosed(), "Resize must close texture and view");
                        check(AuditFixClientChecks.<Integer>field(GlTexture.class, previous, "refCount") == 0,
                                "Old texture retains a view reference");
                    }
                    GlTexture last = field(PaintedMapRenderer.class, null, "copyTexture");
                    GpuTextureView view = field(PaintedMapRenderer.class, null, "copyView");
                    invalidate.invoke(null);
                    check(last.isClosed() && view.isClosed(), "Invalidation must close both resources");
                    check(!GL11.glIsTexture(last.getGlId()), "GPU texture was not deleted");
                    invalidate.invoke(null); // Idempotent cleanup.
                } finally { invalidate.invoke(null); }
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
    }

    static void backupStatus(ClientGameTestContext ctx) {
        ctx.runOnClient(c -> {
            var screen = new OttoExtraSettingsScreen(null);
            c.setScreen(screen);
            try {
                SettingsRegistry registry = field(OttoExtraSettingsScreen.class, screen, "registry");
                var select = OttoExtraSettingsScreen.class.getDeclaredMethod("selectModuleTab", int.class);
                select.setAccessible(true); select.invoke(screen, registry.modules().size());
                ButtonWidget create = screen.children().stream().filter(e -> e instanceof ButtonWidget)
                        .map(e -> (ButtonWidget)e).filter(b -> b.getMessage().equals(Text.translatable("ottoextra.settings.backups.create")))
                        .findFirst().orElseThrow();
                create.onPress(new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0));
                String status = field(OttoExtraSettingsScreen.class, screen, "statusMessage");
                check(status.equals(Text.translatable("ottoextra.settings.backups.created").getString()),
                        "Backup feedback lost or action failed: " + status);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        ctx.waitTick();
        ctx.runOnClient(c -> check(!AuditFixClientChecks.<String>field(OttoExtraSettingsScreen.class, c.currentScreen, "statusMessage").isBlank(),
                "Feedback disappeared before render"));
    }

    static void bookCount(ClientGameTestContext ctx, OttoExtraConfig config, LetterDraft draft) {
        ctx.runOnClient(c -> {
            int count = field(LetterEditorScreen.class, c.currentScreen, "previewBookPages");
            check(count == Math.max(1, LetterServices.previewPages(config, draft).size()), "Editor/preview page count mismatch");
            Object key = field(LetterEditorScreen.class, c.currentScreen, "previewKey");
            c.currentScreen.tick();
            check(key == field(LetterEditorScreen.class, c.currentScreen, "previewKey"), "Unchanged document recomputed preview");
            for (Text label : new Text[]{Text.translatable("ottoextra.letter.workingPageIndicator", 12, 12),
                    Text.translatable("ottoextra.letter.bookCount", count), Text.translatable("ottoextra.letter.book.locked")}) {
                check(c.textRenderer.getWidth(label) <= 148, "Header too wide: " + label.getString());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Class<?> type, Object target, String name) {
        try { var f = type.getDeclaredField(name); f.setAccessible(true); return (T) f.get(target); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
