package de.ottoextra.tweaks.litematica;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Tunes Fachwerk note blocks toward the state projected by Litematica. */
public final class FachwerkAutoTuneFeature {

    private static final int CLICK_COOLDOWN_TICKS = 1;
    private static final int UPDATE_TIMEOUT_TICKS = 40;
    private static final int MAX_CLICKS = 320;
    private static final double MAX_DISTANCE_SQUARED = 36.0;

    private final OttoExtraConfig rootConfig;
    private final KeyBinding toggleKey;

    private BlockPos targetPos;
    private BlockHitResult hitResult;
    private Hand hand;
    private BlockState expectedState;
    private BlockState stateBeforeClick;
    private Tuning initialTuning;
    private boolean backwards;
    private boolean waitingForUpdate;
    private boolean sendingClick;
    private int cooldownTicks;
    private int updateWaitTicks;
    private int clickCount;

    private FachwerkAutoTuneFeature(OttoExtraConfig rootConfig) {
        this.rootConfig = rootConfig;
        this.toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.tweaks.fachwerk_auto_tune",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC));
    }

    public static void register(OttoExtraConfig rootConfig) {
        FachwerkAutoTuneFeature feature = new FachwerkAutoTuneFeature(rootConfig);
        UseBlockCallback.EVENT.register(feature::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(feature::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> feature.clear());
    }

    private ActionResult onUseBlock(net.minecraft.entity.player.PlayerEntity player,
                                    net.minecraft.world.World world,
                                    Hand usedHand,
                                    BlockHitResult hit) {
        if (!world.isClient() || sendingClick || !config().fachwerkAutoTune
                || !isStemmeisen(player.getStackInHand(usedHand))) {
            return ActionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState actual = world.getBlockState(pos);
        BlockState expected = FachwerkSchematicAccess.blockState(pos);
        if (!isNoteBlock(actual) || !isKnownFachwerk(expected)) {
            return ActionResult.PASS;
        }

        if (sameTuning(actual, expected)) {
            clear();
            player.sendMessage(Text.translatable(
                    "ottoextra.tweaks.litematica.fachwerkAutoTune.alreadyDone"), true);
            return ActionResult.FAIL;
        }

        targetPos = pos.toImmutable();
        hitResult = hit;
        hand = usedHand;
        expectedState = expected;
        initialTuning = Tuning.from(actual);
        backwards = usedHand == Hand.MAIN_HAND
                && FachwerkBlockNameResolver.isBackwardShorter(
                actual.get(NoteBlock.INSTRUMENT).asString(), actual.get(NoteBlock.NOTE),
                expected.get(NoteBlock.INSTRUMENT).asString(), expected.get(NoteBlock.NOTE));
        waitingForUpdate = false;
        cooldownTicks = 0;
        updateWaitTicks = 0;
        clickCount = 0;
        player.sendMessage(Text.translatable(
                "ottoextra.tweaks.litematica.fachwerkAutoTune.started"), true);

        // Suppress the initiating click; all tuning clicks are paced by tick().
        return ActionResult.FAIL;
    }

    private void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            config().fachwerkAutoTune = !config().fachwerkAutoTune;
            rootConfig.save();
            clear();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable(
                        config().fachwerkAutoTune
                                ? "ottoextra.tweaks.litematica.fachwerkAutoTune.toggledOn"
                                : "ottoextra.tweaks.litematica.fachwerkAutoTune.toggledOff"), true);
            }
        }

        if (targetPos == null) {
            return;
        }
        if (!canContinue(client)) {
            abort(client, "ottoextra.tweaks.litematica.fachwerkAutoTune.aborted");
            return;
        }

        BlockState actual = client.world.getBlockState(targetPos);
        if (!isNoteBlock(actual)) {
            abort(client, "ottoextra.tweaks.litematica.fachwerkAutoTune.aborted");
            return;
        }
        if (sameTuning(actual, expectedState)) {
            int usedClicks = clickCount;
            clear();
            client.player.sendMessage(Text.translatable(
                    "ottoextra.tweaks.litematica.fachwerkAutoTune.finished", usedClicks), true);
            return;
        }

        if (waitingForUpdate) {
            if (!sameTuning(actual, stateBeforeClick)) {
                waitingForUpdate = false;
                updateWaitTicks = 0;
                cooldownTicks = CLICK_COOLDOWN_TICKS;
                if (clickCount > 1 && initialTuning.equals(Tuning.from(actual))) {
                    abort(client, "ottoextra.tweaks.litematica.fachwerkAutoTune.notReachable");
                }
            } else if (++updateWaitTicks > UPDATE_TIMEOUT_TICKS) {
                abort(client, "ottoextra.tweaks.litematica.fachwerkAutoTune.timeout");
            }
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        if (clickCount >= MAX_CLICKS) {
            abort(client, "ottoextra.tweaks.litematica.fachwerkAutoTune.notReachable");
            return;
        }

        stateBeforeClick = actual;
        sendingClick = true;
        try {
            if (backwards) {
                client.interactionManager.attackBlock(targetPos, hitResult.getSide());
                client.player.swingHand(Hand.MAIN_HAND);
            } else {
                client.interactionManager.interactBlock(client.player, hand, hitResult);
            }
        } finally {
            sendingClick = false;
        }
        clickCount++;
        waitingForUpdate = true;
        updateWaitTicks = 0;
    }

    private boolean canContinue(MinecraftClient client) {
        return config().fachwerkAutoTune
                && client.player != null
                && client.world != null
                && client.interactionManager != null
                && client.currentScreen == null
                && isStemmeisen(client.player.getStackInHand(hand))
                && client.player.squaredDistanceTo(Vec3d.ofCenter(targetPos))
                <= MAX_DISTANCE_SQUARED;
    }

    private void abort(MinecraftClient client, String messageKey) {
        clear();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(messageKey), true);
        }
    }

    private void clear() {
        targetPos = null;
        hitResult = null;
        hand = null;
        expectedState = null;
        stateBeforeClick = null;
        initialTuning = null;
        backwards = false;
        waitingForUpdate = false;
        sendingClick = false;
        cooldownTicks = 0;
        updateWaitTicks = 0;
        clickCount = 0;
    }

    private OttoExtraConfig.Tweaks.Litematica config() {
        return rootConfig.tweaks.litematica;
    }

    private static boolean isKnownFachwerk(BlockState state) {
        if (!isNoteBlock(state)) {
            return false;
        }
        return FachwerkBlockNameResolver.translationKey(
                state.get(NoteBlock.INSTRUMENT).asString(),
                state.get(NoteBlock.NOTE),
                state.get(NoteBlock.POWERED)) != null;
    }

    private static boolean isNoteBlock(BlockState state) {
        return state != null && state.isOf(Blocks.NOTE_BLOCK);
    }

    private static boolean sameTuning(BlockState left, BlockState right) {
        return isNoteBlock(left) && isNoteBlock(right)
                && left.get(NoteBlock.INSTRUMENT) == right.get(NoteBlock.INSTRUMENT)
                && left.get(NoteBlock.NOTE).equals(right.get(NoteBlock.NOTE));
    }

    private static boolean isStemmeisen(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && isStemmeisenName(stack.getName().getString());
    }

    static boolean isStemmeisenName(String name) {
        return name != null && name.trim().toLowerCase(Locale.ROOT).contains("stemmeisen");
    }

    private record Tuning(Object instrument, int note) {
        private static Tuning from(BlockState state) {
            return new Tuning(state.get(NoteBlock.INSTRUMENT), state.get(NoteBlock.NOTE));
        }
    }
}
