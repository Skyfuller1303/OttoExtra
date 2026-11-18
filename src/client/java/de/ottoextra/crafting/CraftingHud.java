package de.ottoextra.crafting;

import de.ottoextra.OttoExtraContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;

/**
 * Replaces the server's crafting progress action bar at custom stations with an animated icon and a
 * progress bar. Only the block character progress messages are swallowed; every other action bar
 * message, including the completion message, is shown as usual.
 */
public final class CraftingHud {

    private static final int FRAME_MS = 150; // 3 game ticks
    private static final int ICON_SIZE = 16;
    private static final int ICON_SCALE = 2;
    private static final int BAR_W = 34;
    private static final int BAR_H = 5;
    private static final int GAP = 2;
    /**
     * Vanilla draws the action bar text at scaledHeight - 68 - 4 with a 2 px background, so its box starts at
     * scaledHeight - 74 (InGameHud.renderOverlayMessage). The bar ends 3 px above that.
     */
    private static final int BAR_BOTTOM_FROM_BOTTOM = 74 + 3;

    private static final int COL_OUTLINE = 0xFF141418;
    private static final int COL_BACK = 0xFF2E2D35;
    private static final int[] FILL = {0xFFDBB451, 0xFFB58D36, 0xFF866C36};
    private static final int[] FILL_DONE = {0xFF8FD14F, 0xFF5FA83A, 0xFF3E7A2A};
    private static final int[] FILL_FAILED = {0xFFC4545A, 0xFFB24147, 0xFF6E2226};

    private static final java.util.Random RANDOM = new java.util.Random();
    private static final CraftingState STATE = new CraftingState();
    private static volatile String lastTitle = "";
    private static long lastStrikeFrame = -1;

    private CraftingHud() {
    }

    public static void register(OttoExtraContext context) {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            // Only known station screens count: sub menus such as "Mehr Suppen" must not replace the
            // station the player is crafting at.
            if (screen instanceof GenericContainerScreen && client.currentScreen == screen) {
                String title = screen.getTitle().getString();
                if (CraftingStation.forTitle(title).isPresent()) lastTitle = title;
            }
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!context.config().tweaks.craftingHud || !context.isOnOttonien()) return true;
            long now = Util.getMeasuringTimeMs();
            String plain = message.getString();
            if (!overlay) {
                var item = CraftingParser.itemName(plain);
                if (item.isPresent()) {
                    STATE.onStart(CraftingStation.forTitle(lastTitle).orElse(null), item.get(), now);
                    lastStrikeFrame = -1;
                } else if (CraftingParser.isFailure(plain) && STATE.onFailure(now)) {
                    play(context, SoundEvents.ENTITY_ITEM_BREAK.value(), 0.5f, 0.9f);
                }
                return true;
            }
            var progress = CraftingParser.progress(message);
            if (progress.isPresent()) {
                STATE.onProgress(progress.get().fraction(),
                        CraftingStation.forTitle(lastTitle).orElse(null), now);
                return false;
            }
            if (CraftingParser.isCompletion(plain)) {
                if (STATE.onComplete(now)) play(context, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.35f, 1.5f);
            } else if (CraftingParser.isFailure(plain) && STATE.onFailure(now)) {
                play(context, SoundEvents.ENTITY_ITEM_BREAK.value(), 0.5f, 0.9f);
            }
            return true;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            STATE.clear();
            lastTitle = "";
        });
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            if (!de.ottoextra.map.WorldMapOpenAnimation.hidesHudOverlays()) render(ctx, context);
        });
    }

    private static void render(DrawContext ctx, OttoExtraContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden
                || !context.config().tweaks.craftingHud || !context.isOnOttonien()) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        if (STATE.tick(now)) play(context, SoundEvents.ENTITY_ITEM_BREAK.value(), 0.5f, 0.9f);
        if (!STATE.visible(now)) return;
        float shown = STATE.advance(now);
        CraftingStation station = STATE.station();

        int barX = (ctx.getScaledWindowWidth() - BAR_W) / 2;
        int barY = ctx.getScaledWindowHeight() - BAR_BOTTOM_FROM_BOTTOM - BAR_H;

        if (station != null) {
            long tickIndex = now / FRAME_MS;
            int frame = (int) (tickIndex % station.frames());
            if (STATE.result() == CraftingState.Result.NONE && frame == station.hitFrame()
                    && tickIndex != lastStrikeFrame) {
                lastStrikeFrame = tickIndex;
                play(context, station.strikeSound().get(), station.strikeVolume(),
                        station.strikePitch() + (RANDOM.nextFloat() * 2 - 1) * station.strikePitchSpread());
            }
            int size = ICON_SIZE * ICON_SCALE;
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate((ctx.getScaledWindowWidth() - size) / 2, barY - GAP - size);
            m.scale(ICON_SCALE, ICON_SCALE);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, station.textureId(), 0, 0,
                    0f, frame * ICON_SIZE, ICON_SIZE, ICON_SIZE,
                    ICON_SIZE, ICON_SIZE * station.frames());
            m.popMatrix();
        }
        drawBar(ctx, barX, barY, shown, STATE.result());
    }

    private static void play(OttoExtraContext context, SoundEvent sound, float volume, float pitch) {
        if (!context.config().tweaks.craftingHudSounds) return;
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.ui(sound, pitch, volume));
    }

    /** 34x5 bar: 1 px outline, 32x3 inner area, fill in three shaded rows. */
    private static void drawBar(DrawContext ctx, int x, int y, float fraction, CraftingState.Result result) {
        ctx.fill(x, y, x + BAR_W, y + BAR_H, COL_OUTLINE);
        ctx.fill(x + 1, y + 1, x + BAR_W - 1, y + BAR_H - 1, COL_BACK);
        int fillW = Math.round(fraction * (BAR_W - 2));
        if (fillW <= 0) return;
        int[] rows = switch (result) {
            case SUCCESS -> FILL_DONE;
            case FAILURE -> FILL_FAILED;
            case NONE -> FILL;
        };
        for (int i = 0; i < 3; i++) {
            ctx.fill(x + 1, y + 1 + i, x + 1 + fillW, y + 2 + i, rows[i]);
        }
    }
}
