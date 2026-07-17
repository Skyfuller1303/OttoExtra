package de.ottoextra.rpnames.inspect;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rein clientseitiger, rollenspielorientierter Untersuchen-Modus.
 *
 * <p>Solange die konfigurierte Taste gehalten wird, aktiviert sich eine
 * Untersuchungslinse. Ein Ziel wird zuerst mehrere Sekunden lang gemustert.
 * Erst danach werden Informationen angezeigt, die der Client wirklich kennt:
 * lokale RP-Personendaten, sichtbare Item-Komponenten, Schildtexte,
 * Ruestungsstaender-Ausrüstung und offen sichtbare Blockzustaende.</p>
 */
public final class InspectMode {

    private static final long TARGET_STABILITY_MS = 180L;
    private static final long TARGET_LOST_GRACE_MS = 240L;
    private static final long LENS_IN_MS = 180L;
    private static final long LENS_OUT_MS = 140L;
    private static final double ITEM_RAY_PADDING = 0.32;
    private static final int MAX_RENDER_LINES = 10;
    private static final int PROGRESS_SEGMENTS = 36;
    private static final List<String> ROLEPLAY_PHRASE_KEYS = List.of(
            "ottoextra.inspect.thought.1",
            "ottoextra.inspect.thought.2",
            "ottoextra.inspect.thought.3",
            "ottoextra.inspect.thought.4",
            "ottoextra.inspect.thought.5",
            "ottoextra.inspect.thought.6");

    private static final Identifier LENS_TEXTURE =
            Identifier.of("ottoextra", "textures/gui/inspect_lens.png");

    private static KeyBinding inspectKey;
    private static OttoExtraConfig.RpNames activeConfig;

    private static InspectionTarget target;
    private static String targetKey = "";
    private static long targetSince;
    private static long lastTargetSeen;

    private static InspectionTarget candidate;
    private static String candidateKey = "";
    private static long candidateSince;

    private static boolean visualRequested;

    // Ein RP-Gedanke wird einmal pro gedrueckter ALT-Phase gewaehlt.
    // Zielwechsel beim Laufen duerfen den Satz nicht hektisch wechseln.
    private static boolean inspectKeyHeld;
    private static int activeRoleplayPhraseIndex = -1;
    private static int previousRoleplayPhraseIndex = -1;
    private static float transitionFrom;
    private static float transitionTo;
    private static long transitionStartedNanos = System.nanoTime();

    private InspectMode() {
    }

    public static void register(OttoExtraConfig.RpNames config) {
        activeConfig = config;
        inspectKey = new KeyBinding(
                "key.ottoextra.inspect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(inspectKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> update(client, config));
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context, config));
    }

    private static void update(MinecraftClient client, OttoExtraConfig.RpNames config) {
        // Der Hauptschalter deaktiviert das gesamte Modul sofort: Taste, HUD,
        // Zoom, Post-Effect und alle Zielinformationen.
        if (!config.inspectEnabled) {
            resetRoleplayPhraseSession();
            disableImmediately();
            clearTarget();
            return;
        }

        boolean keyPressed = inspectKey != null && inspectKey.isPressed();
        updateRoleplayPhraseSession(keyPressed);

        boolean allowed = keyPressed
                && client.currentScreen == null
                && client.player != null
                && client.world != null
                && RpNamesServices.isActive();

        setVisualRequested(allowed);
        if (!allowed) {
            clearTarget();
            return;
        }

        long now = System.currentTimeMillis();
        InspectionTarget resolved = resolveTarget(client, config);

        if (resolved == null) {
            clearCandidate();
            if (target != null && now - lastTargetSeen > TARGET_LOST_GRACE_MS) {
                clearCommittedTarget();
            }
            return;
        }

        if (target != null && resolved.key().equals(targetKey)) {
            // Sichtbare Informationen duerfen sich aktualisieren, ohne die
            // bereits investierte Untersuchungszeit zu verlieren.
            target = resolved;
            lastTargetSeen = now;
            clearCandidate();
            return;
        }

        if (!resolved.key().equals(candidateKey)) {
            candidate = resolved;
            candidateKey = resolved.key();
            candidateSince = now;
        } else {
            candidate = resolved;
        }

        if (now - candidateSince >= TARGET_STABILITY_MS) {
            target = candidate;
            targetKey = candidateKey;
            targetSince = candidateSince;
            lastTargetSeen = now;
            clearCandidate();
        }
    }

    private static InspectionTarget resolveTarget(MinecraftClient client, OttoExtraConfig.RpNames config) {
        if (client.player == null || client.world == null) {
            return null;
        }

        if (client.crosshairTarget instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            double maxDistanceSq = config.inspectMaxDistance * config.inspectMaxDistance;
            if (client.player.squaredDistanceTo(entity) > maxDistanceSq) {
                return null;
            }

            if (entity instanceof PlayerEntity player && player != client.player) {
                return playerTarget(player, config);
            }
            if (entity instanceof ItemEntity itemEntity && !itemEntity.getStack().isEmpty()) {
                return itemTarget("item:" + itemEntity.getUuidAsString(), itemEntity.getStack(), null);
            }
            if (entity instanceof ItemFrameEntity frame) {
                return itemFrameTarget(frame);
            }
            if (entity instanceof ArmorStandEntity armorStand) {
                return armorStandTarget(armorStand);
            }
            return entityTarget(entity);
        }

        // Gedroppte Items werden vom normalen Fadenkreuz nicht in jeder
        // Minecraft-Situation als Treffer gemeldet. Darum folgt ein kleiner,
        // rein lokaler Sichtstrahl mit Sichtschutzpruefung.
        ItemEntity lookedAtItem = findLookedAtItem(client, config.inspectMaxDistance);
        if (lookedAtItem != null) {
            return itemTarget("item:" + lookedAtItem.getUuidAsString(), lookedAtItem.getStack(), null);
        }

        if (client.crosshairTarget instanceof BlockHitResult blockHit) {
            BlockState state = client.world.getBlockState(blockHit.getBlockPos());
            if (state.isAir()) {
                return null;
            }

            BlockEntity blockEntity = client.world.getBlockEntity(blockHit.getBlockPos());
            if (blockEntity instanceof SignBlockEntity sign) {
                return signTarget(client, blockHit, state, sign);
            }
            if (blockEntity instanceof LecternBlockEntity lectern && lectern.hasBook()) {
                return itemTarget(
                        "lectern:" + blockHit.getBlockPos().asLong() + ":" + itemIdentity(lectern.getBook()),
                        lectern.getBook(),
                        Text.translatable("ottoextra.inspect.book.lectern", lectern.getCurrentPage() + 1));
            }
            if (blockEntity instanceof BannerBlockEntity banner) {
                return bannerTarget(blockHit, state, banner);
            }
            return blockTarget(blockHit, state);
        }

        return null;
    }

    private static InspectionTarget playerTarget(PlayerEntity player, OttoExtraConfig.RpNames config) {
        String account = player.getGameProfile().name();
        String uuid = player.getUuidAsString();
        LocalRpProfile profile = RpNamesServices.store() == null
                ? null
                : RpNamesServices.store().find(uuid, account).orElse(null);
        boolean known = profile != null && RpNamesServices.isKnownForDisplay(profile);

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(known ? profile.displayRpName() : config.unknownPlaceholder));
        if (known && profile.hasTitle()) {
            lines.add(Text.literal(RpNamesServices.canonicalTitle(profile.title)));
        }
        if (known && config.inspectShowAccount) {
            lines.add(Text.literal("(" + account + ")"));
        }

        if (config.inspectShowPlayerHands) {
            addPlayerHandLines(lines, player);
        }
        if (config.inspectShowPlayerArmor) {
            addPlayerArmorLines(lines, player);
        }

        return new InspectionTarget(
                "player:" + uuid,
                TargetKind.PLAYER,
                trimLines(lines),
                known);
    }

    private static void addPlayerHandLines(List<Text> lines, PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        boolean mainEmpty = mainHand == null || mainHand.isEmpty();
        boolean offEmpty = offHand == null || offHand.isEmpty();

        if (mainEmpty && offEmpty) {
            lines.add(Text.translatable("ottoextra.inspect.player.hands_empty"));
            return;
        }
        if (!mainEmpty) {
            lines.add(Text.translatable(
                    "ottoextra.inspect.player.mainhand",
                    mainHand.getName()));
        }
        if (!offEmpty) {
            lines.add(Text.translatable(
                    "ottoextra.inspect.player.offhand",
                    offHand.getName()));
        }
    }

    private static void addPlayerArmorLines(List<Text> lines, PlayerEntity player) {
        addPlayerArmorLine(lines, player, EquipmentSlot.HEAD, "ottoextra.inspect.slot.head");
        addPlayerArmorLine(lines, player, EquipmentSlot.CHEST, "ottoextra.inspect.slot.chest");
        addPlayerArmorLine(lines, player, EquipmentSlot.LEGS, "ottoextra.inspect.slot.legs");
        addPlayerArmorLine(lines, player, EquipmentSlot.FEET, "ottoextra.inspect.slot.feet");
    }

    private static void addPlayerArmorLine(List<Text> lines, PlayerEntity player,
                                           EquipmentSlot slot, String slotKey) {
        ItemStack equipped = player.getEquippedStack(slot);
        if (equipped == null || equipped.isEmpty()) {
            return;
        }
        lines.add(Text.translatable(
                "ottoextra.inspect.player.armor",
                Text.translatable(slotKey),
                equipped.getName()));
    }

    private static InspectionTarget itemFrameTarget(ItemFrameEntity frame) {
        ItemStack held = frame.getHeldItemStack();
        if (held.isEmpty()) {
            return new InspectionTarget(
                    "frame:" + frame.getUuidAsString() + ":empty",
                    TargetKind.ITEM_FRAME,
                    List.of(
                            frame.getDisplayName(),
                            Text.translatable("ottoextra.inspect.frame.empty")),
                    true);
        }
        return itemTarget(
                "frame:" + frame.getUuidAsString() + ":" + itemIdentity(held),
                held,
                Text.translatable("ottoextra.inspect.frame.displayed"));
    }

    private static InspectionTarget itemTarget(String key, ItemStack stack, Text contextLine) {
        ItemStack snapshot = stack.copy();
        List<Text> lines = new ArrayList<>();
        lines.add(snapshot.getName().copy());
        if (contextLine != null && !contextLine.getString().isBlank()) {
            lines.add(contextLine);
        }
        lines.addAll(ItemInspectionDescriptions.details(snapshot));
        return new InspectionTarget(
                key + ":" + itemIdentity(snapshot),
                TargetKind.ITEM,
                trimLines(lines),
                true);
    }

    private static InspectionTarget armorStandTarget(ArmorStandEntity armorStand) {
        List<Text> lines = new ArrayList<>();
        lines.add(armorStand.getDisplayName());

        addEquipmentLine(lines, armorStand, EquipmentSlot.HEAD, "ottoextra.inspect.slot.head");
        addEquipmentLine(lines, armorStand, EquipmentSlot.CHEST, "ottoextra.inspect.slot.chest");
        addEquipmentLine(lines, armorStand, EquipmentSlot.LEGS, "ottoextra.inspect.slot.legs");
        addEquipmentLine(lines, armorStand, EquipmentSlot.FEET, "ottoextra.inspect.slot.feet");
        addEquipmentLine(lines, armorStand, EquipmentSlot.MAINHAND, "ottoextra.inspect.slot.mainhand");
        addEquipmentLine(lines, armorStand, EquipmentSlot.OFFHAND, "ottoextra.inspect.slot.offhand");

        if (lines.size() == 1) {
            lines.add(Text.translatable("ottoextra.inspect.armor_stand.empty"));
        }
        return new InspectionTarget(
                "armor_stand:" + armorStand.getUuidAsString(),
                TargetKind.ARMOR_STAND,
                trimLines(lines),
                true);
    }

    private static void addEquipmentLine(List<Text> lines, ArmorStandEntity stand,
                                         EquipmentSlot slot, String slotKey) {
        ItemStack equipped = stand.getEquippedStack(slot);
        if (equipped == null || equipped.isEmpty()) {
            return;
        }
        lines.add(Text.translatable(
                "ottoextra.inspect.armor_stand.equipped",
                Text.translatable(slotKey),
                equipped.getName()));
    }

    private static InspectionTarget entityTarget(Entity entity) {
        List<Text> lines = new ArrayList<>();
        lines.add(entity.getDisplayName());

        if (entity instanceof LivingEntity living) {
            if (living.isBaby()) {
                lines.add(Text.translatable("ottoextra.inspect.entity.young"));
            }
            if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
                lines.add(Text.translatable("ottoextra.inspect.entity.tamed"));
            } else if (entity instanceof AbstractHorseEntity horse && horse.isTame()) {
                lines.add(Text.translatable("ottoextra.inspect.entity.tamed"));
            }
        }

        if (lines.size() == 1) {
            lines.add(Text.translatable("ottoextra.inspect.entity.visible"));
        }
        return new InspectionTarget(
                "entity:" + entity.getUuidAsString(),
                TargetKind.ENTITY,
                trimLines(lines),
                true);
    }

    private static InspectionTarget signTarget(MinecraftClient client, BlockHitResult hit,
                                                BlockState state, SignBlockEntity sign) {
        boolean front = client.player == null || sign.isPlayerFacingFront(client.player);
        SignText visibleText = sign.getText(front);
        List<Text> lines = new ArrayList<>();
        lines.add(state.getBlock().getName());
        lines.add(Text.translatable(front
                ? "ottoextra.inspect.sign.front"
                : "ottoextra.inspect.sign.back"));

        int textLines = 0;
        for (Text message : visibleText.getMessages(false)) {
            if (message == null || message.getString().isBlank()) {
                continue;
            }
            lines.add(quote(shortenPlain(message.getString(), 54)));
            textLines++;
        }
        if (textLines == 0) {
            lines.add(Text.translatable("ottoextra.inspect.sign.empty"));
        }
        if (visibleText.isGlowing()) {
            lines.add(Text.translatable("ottoextra.inspect.sign.glowing"));
        }
        if (sign.isWaxed()) {
            lines.add(Text.translatable("ottoextra.inspect.sign.waxed"));
        }

        return new InspectionTarget(
                "sign:" + hit.getBlockPos().asLong() + ":" + front,
                TargetKind.SIGN,
                trimLines(lines),
                true);
    }

    private static InspectionTarget bannerTarget(BlockHitResult hit, BlockState state, BannerBlockEntity banner) {
        ItemStack pickStack = banner.getPickStack();
        List<Text> lines = new ArrayList<>();
        lines.add(pickStack.isEmpty() ? state.getBlock().getName() : pickStack.getName());
        lines.add(Text.translatable("ottoextra.inspect.banner.placed"));
        if (!pickStack.isEmpty()) {
            lines.addAll(ItemInspectionDescriptions.details(pickStack));
        }
        return new InspectionTarget(
                "banner:" + hit.getBlockPos().asLong() + ":" + itemIdentity(pickStack),
                TargetKind.BANNER,
                trimLines(lines),
                true);
    }

    private static InspectionTarget blockTarget(BlockHitResult hit, BlockState state) {
        List<Text> lines = new ArrayList<>();
        lines.add(state.getBlock().getName());

        Identifier id = Registries.BLOCK.getId(state.getBlock());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (hasUsefulGenericDescription(path)) {
            lines.add(ItemInspectionDescriptions.describe(state));
        }
        addVisibleBlockState(lines, state, path);

        return new InspectionTarget(
                "block:" + hit.getBlockPos().asLong() + ":" + state.getBlock().getTranslationKey(),
                TargetKind.BLOCK,
                trimLines(lines),
                true);
    }

    private static boolean hasUsefulGenericDescription(String path) {
        return containsAny(path,
                "door", "trapdoor", "gate", "chest", "barrel", "shulker_box",
                "furnace", "smoker", "campfire", "crafting_table", "smithing_table",
                "anvil", "grindstone", "stonecutter", "loom", "cartography_table",
                "flower", "sapling", "crop", "mushroom", "ore", "torch", "lantern",
                "candle", "lamp");
    }

    /** Zeigt nur Blockzustaende, die der Client offen kennt und die sichtbar sind. */
    private static void addVisibleBlockState(List<Text> lines, BlockState state, String path) {
        String lit = propertyValue(state, "lit");
        if (lit != null && containsAny(path, "candle", "campfire", "furnace", "smoker", "lantern")) {
            lines.add(Text.translatable(Boolean.parseBoolean(lit)
                    ? "ottoextra.inspect.block.lit"
                    : "ottoextra.inspect.block.unlit"));
        }

        String open = propertyValue(state, "open");
        if (open != null && containsAny(path, "door", "trapdoor", "gate")) {
            lines.add(Text.translatable(Boolean.parseBoolean(open)
                    ? "ottoextra.inspect.block.open"
                    : "ottoextra.inspect.block.closed"));
        }

        String candles = propertyValue(state, "candles");
        if (candles != null && path.contains("candle")) {
            lines.add(Text.translatable("ottoextra.inspect.block.candles", candles));
        }

        String honey = propertyValue(state, "honey_level");
        if (honey != null && containsAny(path, "beehive", "bee_nest")) {
            lines.add(Text.translatable("ottoextra.inspect.block.honey", honey));
        }

        String age = propertyValue(state, "age");
        int maxAge = expectedMaxAge(path);
        if (age != null && maxAge > 0) {
            try {
                int currentAge = Integer.parseInt(age);
                lines.add(Text.translatable(currentAge >= maxAge
                        ? "ottoextra.inspect.block.mature"
                        : "ottoextra.inspect.block.growing"));
            } catch (NumberFormatException ignored) {
                // Nicht jeder modded AGE-Wert ist numerisch. Dann wird nichts
                // behauptet, statt eine falsche Reifestufe anzuzeigen.
            }
        }
    }

    private static String propertyValue(BlockState state, String propertyName) {
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
            if (entry.getKey().getName().equals(propertyName)) {
                return String.valueOf(entry.getValue());
            }
        }
        return null;
    }

    private static int expectedMaxAge(String path) {
        if (containsAny(path, "wheat", "carrots", "potatoes", "stem")) {
            return 7;
        }
        if (containsAny(path, "beetroots", "nether_wart", "sweet_berry_bush")) {
            return 3;
        }
        if (path.contains("cocoa")) {
            return 2;
        }
        return -1;
    }

    private static String itemIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + ":" + stack.getName().getString();
    }

    private static ItemEntity findLookedAtItem(MinecraftClient client, double maxDistance) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return null;
        }

        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f).normalize();
        Vec3d end = start.add(direction.multiply(maxDistance));
        Box searchBox = player.getBoundingBox()
                .stretch(direction.multiply(maxDistance))
                .expand(1.0);

        double obstructionDistanceSq = maxDistance * maxDistance;
        if (client.crosshairTarget != null) {
            obstructionDistanceSq = Math.min(obstructionDistanceSq,
                    start.squaredDistanceTo(client.crosshairTarget.getPos()) + 0.01);
        }

        ItemEntity best = null;
        double bestDistanceSq = obstructionDistanceSq;
        List<ItemEntity> candidates = client.world.getEntitiesByClass(
                ItemEntity.class,
                searchBox,
                item -> !item.isRemoved() && !item.getStack().isEmpty());

        for (ItemEntity item : candidates) {
            Optional<Vec3d> intersection = item.getBoundingBox()
                    .expand(ITEM_RAY_PADDING)
                    .raycast(start, end);
            if (intersection.isEmpty()) {
                continue;
            }
            double distanceSq = start.squaredDistanceTo(intersection.get());
            if (distanceSq <= bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = item;
            }
        }
        return best;
    }

    private static List<Text> trimLines(List<Text> lines) {
        List<Text> cleaned = new ArrayList<>();
        for (Text line : lines) {
            if (line != null && !line.getString().isBlank()) {
                cleaned.add(line);
            }
            if (cleaned.size() >= MAX_RENDER_LINES) {
                break;
            }
        }
        return List.copyOf(cleaned);
    }

    private static Text quote(String value) {
        return Text.literal("„" + value + "“");
    }

    private static String shortenPlain(String value, int maxLength) {
        String clean = value == null ? "" : value.strip();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, Math.max(1, maxLength - 1)) + "…";
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static void clearTarget() {
        clearCommittedTarget();
        clearCandidate();
    }

    private static void clearCommittedTarget() {
        target = null;
        targetKey = "";
        targetSince = 0L;
        lastTargetSeen = 0L;
    }

    private static void clearCandidate() {
        candidate = null;
        candidateKey = "";
        candidateSince = 0L;
    }

    private static void updateRoleplayPhraseSession(boolean keyPressed) {
        if (!keyPressed) {
            inspectKeyHeld = false;
            activeRoleplayPhraseIndex = -1;
            return;
        }
        if (inspectKeyHeld) {
            return;
        }

        inspectKeyHeld = true;
        int size = ROLEPLAY_PHRASE_KEYS.size();
        if (size <= 0) {
            activeRoleplayPhraseIndex = -1;
            return;
        }

        int next = ThreadLocalRandom.current().nextInt(size);
        if (size > 1 && next == previousRoleplayPhraseIndex) {
            next = (next + 1 + ThreadLocalRandom.current().nextInt(size - 1)) % size;
        }
        activeRoleplayPhraseIndex = next;
        previousRoleplayPhraseIndex = next;
    }

    private static void resetRoleplayPhraseSession() {
        inspectKeyHeld = false;
        activeRoleplayPhraseIndex = -1;
    }

    private static void disableImmediately() {
        visualRequested = false;
        transitionFrom = 0.0f;
        transitionTo = 0.0f;
        transitionStartedNanos = System.nanoTime();
    }

    private static void setVisualRequested(boolean requested) {
        if (visualRequested == requested) {
            return;
        }
        long now = System.nanoTime();
        transitionFrom = calculateVisualStrength(now);
        transitionTo = requested ? 1.0f : 0.0f;
        transitionStartedNanos = now;
        visualRequested = requested;
    }

    /** Aktuelle weich interpolierte Staerke der Untersuchungslinse (0–1). */
    public static float visualStrength() {
        return calculateVisualStrength(System.nanoTime());
    }

    private static float calculateVisualStrength(long nowNanos) {
        long durationMs = transitionTo > transitionFrom ? LENS_IN_MS : LENS_OUT_MS;
        float elapsedMs = (nowNanos - transitionStartedNanos) / 1_000_000.0f;
        float t = Math.max(0.0f, Math.min(1.0f, elapsedMs / Math.max(1L, durationMs)));
        float eased = t * t * (3.0f - 2.0f * t);
        return transitionFrom + (transitionTo - transitionFrom) * eased;
    }

    /**
     * Zielverhaeltnis fuer den sanften Zoom der Untersuchungslinse.
     *
     * <p>Die Einstellung bleibt in Grad angegeben: Bei einem Referenz-FOV von 70
     * entsprechen beispielsweise 9 Grad einem Zielverhaeltnis von 61/70. Der
     * Renderer veraendert damit erst die bereits fertige Projektionsmatrix. So
     * greift OttoExtra nicht mehr in die FOV-Berechnung anderer Zoom-Mods ein.</p>
     */
    public static float fovZoomMultiplier() {
        OttoExtraConfig.RpNames config = activeConfig;
        if (config == null || !config.inspectEnabled || !config.inspectZoomEnabled) {
            return 1.0f;
        }

        final float referenceFov = 70.0f;
        float reductionDegrees = visualStrength()
                * Math.max(0.0f, (float) config.inspectZoomDegrees);
        float multiplier = (referenceFov - reductionDegrees) / referenceFov;
        return Math.max(0.10f, Math.min(1.0f, multiplier));
    }

    /** Wird vom Welt-Renderer fuer den leichten Rand-Blur abgefragt. */
    public static boolean edgeBlurActive() {
        OttoExtraConfig.RpNames config = activeConfig;
        return config != null
                && config.inspectEnabled
                && config.inspectEdgeBlurEnabled
                && visualStrength() > 0.12f;
    }

    private static long revealDelayMs(OttoExtraConfig.RpNames config) {
        // Der gesamte RP-Untersuchungsvorgang dauert hoechstens zwei Sekunden.
        double seconds = Math.max(0.5, Math.min(2.0, config.inspectRevealSeconds));
        return Math.round(seconds * 1000.0);
    }

    private static long roleplayPhraseDurationMs(OttoExtraConfig.RpNames config, long revealDelay) {
        double seconds = Math.max(0.5, Math.min(2.0, config.inspectRoleplayPhraseSeconds));
        return Math.min(revealDelay, Math.round(seconds * 1000.0));
    }

    private static void render(DrawContext context, OttoExtraConfig.RpNames config) {
        MinecraftClient client = MinecraftClient.getInstance();
        float strength = visualStrength();
        if (!config.inspectEnabled || strength <= 0.01f || client.currentScreen != null
                || client.player == null || !RpNamesServices.isActive()) {
            return;
        }

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;
        drawLensIcon(context, centerX, centerY, strength);

        // Beim Ausblenden bleibt nur die Linse kurz sichtbar; Zielinformationen
        // verschwinden sofort nach dem Loslassen der Taste.
        if (!visualRequested) {
            return;
        }

        // Ein neu erfasstes Ziel hat Vorrang vor dem zuvor untersuchten Ziel.
        // Dadurch bleiben beim Wechsel niemals kurz die alten Informationen
        // sichtbar; die RP-Untersuchung beginnt unmittelbar von vorn.
        boolean examiningCandidate = candidate != null;
        InspectionTarget current = examiningCandidate ? candidate : target;
        if (current == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long start = examiningCandidate ? candidateSince : targetSince;
        long observedFor = Math.max(0L, now - start);
        long revealDelay = revealDelayMs(config);
        float progress = Math.max(0.0f, Math.min(1.0f, observedFor / (float) revealDelay));
        drawProgressRing(context, centerX, centerY, progress, strength);

        boolean revealed = !examiningCandidate && target != null && observedFor >= revealDelay;
        List<Text> lines;
        if (!revealed) {
            lines = List.of(investigatingText(
                    current, start, observedFor, now, config, revealDelay));
        } else {
            lines = current.lines();
        }
        drawInfoBox(context, client, config, current, lines, centerX, centerY, revealed);
    }

    private static Text investigatingText(InspectionTarget current, long startedAt,
                                           long observedFor, long now,
                                           OttoExtraConfig.RpNames config,
                                           long revealDelay) {
        int dots = 1 + (int) ((now / 350L) % 3L);
        if (!config.inspectRoleplayPhrasesEnabled
                || observedFor > roleplayPhraseDurationMs(config, revealDelay)) {
            return Text.translatable("ottoextra.inspect.investigating")
                    .copy()
                    .append(".".repeat(dots));
        }

        // Der Satz bleibt fuer die komplette gedrueckte ALT-Phase stabil.
        // Beim Laufen und schnellen Zielwechseln wird er daher nicht neu
        // ausgewuerfelt. Erst Loslassen und erneutes Druecken waehlt neu.
        int index = activeRoleplayPhraseIndex;
        if (index < 0 || index >= ROLEPLAY_PHRASE_KEYS.size()) {
            index = 0;
        }
        return Text.translatable(ROLEPLAY_PHRASE_KEYS.get(index))
                .copy()
                .append(".".repeat(dots));
    }

    private static void drawInfoBox(DrawContext context, MinecraftClient client,
                                    OttoExtraConfig.RpNames config, InspectionTarget current,
                                    List<Text> lines, int centerX, int centerY, boolean revealed) {
        int paddingX = 10;
        int paddingY = 7;
        int lineHeight = 11;
        int maxTextWidth = 0;
        for (Text line : lines) {
            maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(line));
        }

        int boxWidth = Math.max(104, maxTextWidth + paddingX * 2);
        int boxHeight = lines.size() * lineHeight + paddingY * 2 - 1;
        int desiredTop = centerY + Math.max(38, config.inspectOffsetY);
        int maxTop = client.getWindow().getScaledHeight() - boxHeight - 6;
        int top = Math.max(6, Math.min(desiredTop, maxTop));
        int left = centerX - boxWidth / 2;

        boolean unknownPlayer = current.kind() == TargetKind.PLAYER && !current.known();
        int background = revealed ? 0xCC241C16 : 0xC91D1915;
        if (unknownPlayer) {
            background = 0xCC1B1714;
        }
        int border = borderColor(current.kind(), revealed, unknownPlayer);

        context.fill(left, top, left + boxWidth, top + boxHeight, background);
        context.fill(left, top, left + boxWidth, top + 1, border);
        context.fill(left, top + boxHeight - 1, left + boxWidth, top + boxHeight, border);
        context.fill(left, top, left + 1, top + boxHeight, border);
        context.fill(left + boxWidth - 1, top, left + boxWidth, top + boxHeight, border);

        int y = top + paddingY;
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int color;
            if (!revealed) {
                color = 0xFFD8C6A6;
            } else if (i == 0) {
                color = unknownPlayer ? 0xFFB9ACA0 : 0xFFE7D3B0;
            } else if (line.getString().startsWith("(")) {
                color = 0xFF8F8175;
            } else {
                color = secondaryColor(current.kind());
            }
            context.drawCenteredTextWithShadow(client.textRenderer, line, centerX, y, color);
            y += lineHeight;
        }
    }

    private static int borderColor(TargetKind kind, boolean revealed, boolean unknownPlayer) {
        if (!revealed) {
            return 0xFF7F694E;
        }
        if (unknownPlayer) {
            return 0xFF67584B;
        }
        return switch (kind) {
            case PLAYER -> 0xFF9B7952;
            case ITEM, ITEM_FRAME, ARMOR_STAND -> 0xFF8B765A;
            case SIGN, BANNER -> 0xFFA08455;
            case ENTITY -> 0xFF7D765E;
            case BLOCK -> 0xFF716B57;
        };
    }

    private static int secondaryColor(TargetKind kind) {
        return switch (kind) {
            case ITEM, ITEM_FRAME, ARMOR_STAND -> 0xFFD0B88E;
            case SIGN, BANNER -> 0xFFD7BB82;
            case PLAYER -> 0xFFC6A77B;
            case ENTITY -> 0xFFC3B992;
            case BLOCK -> 0xFFBDB18D;
        };
    }

    private static void drawLensIcon(DrawContext context, int centerX, int centerY, float strength) {
        if (strength <= 0.08f) {
            return;
        }
        int size = 24 + Math.round(6.0f * strength);
        int x = centerX - size / 2;
        int y = centerY - size / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LENS_TEXTURE,
                x, y, 0.0f, 0.0f, size, size, 64, 64, 64, 64);
    }

    /** Dezenter kreisfoermiger Fortschritt statt eines technischen Ladebalkens. */
    private static void drawProgressRing(DrawContext context, int centerX, int centerY,
                                         float progress, float strength) {
        if (strength <= 0.08f) {
            return;
        }
        int completed = Math.round(PROGRESS_SEGMENTS * progress);
        int radius = 18 + Math.round(2.0f * strength);
        int color = progress >= 1.0f ? 0xFFE3C892 : 0xFFD2B17B;

        for (int i = 0; i < completed; i++) {
            double angle = -Math.PI / 2.0 + (2.0 * Math.PI * i / PROGRESS_SEGMENTS);
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            context.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    private enum TargetKind {
        PLAYER,
        ITEM,
        ITEM_FRAME,
        ARMOR_STAND,
        ENTITY,
        SIGN,
        BANNER,
        BLOCK
    }

    private record InspectionTarget(
            String key,
            TargetKind kind,
            List<Text> lines,
            boolean known) {

        private InspectionTarget {
            lines = List.copyOf(lines);
        }
    }
}
