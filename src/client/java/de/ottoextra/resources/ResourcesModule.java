package de.ottoextra.resources;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.ottoextra.OttoExtraGate;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.regions.RegionMessageService;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.Normalizer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ResourcesModule {
    private static final long NOTICE_COOLDOWN_MILLIS = 600_000L;
    private static final long CONFIRM_OUTSIDE_GRACE_MILLIS = 60_000L;
    private static final Map<String, String> GARDEN_PLANTS = Map.of(
            "minecraft:brown_mushroom", "ottoextra:garden/steinpilzhut",
            "minecraft:red_mushroom", "ottoextra:garden/fliegenpilzhut",
            "minecraft:warped_fungus", "ottoextra:garden/hoehlenpilzhut",
            "minecraft:red_tulip", "ottoextra:garden/weinrautenblueten",
            "minecraft:bubble_coral", "ottoextra:garden/nesselblaetter",
            "minecraft:oxeye_daisy", "ottoextra:garden/baldrianblueten",
            "minecraft:pink_tulip", "ottoextra:garden/ringelblumenblueten");

    private static final long REBUILD_INTERVAL_MILLIS = 1_000L;
    private static final int NEARBY_COMMAND_RADIUS = 192;
    private static final long SHOW_MILLIS = 30_000L;

    private final ResourcesConfig config = ResourcesConfig.active();
    private final ResourceRepository repository = new ResourceRepository(new ResourceStore(
            FabricLoader.getInstance().getConfigDir().resolve("ottoextra-resources")));
    private final ResourceSync sync = new ResourceSync(repository, config,
            ResourcesModule::sharingAllowed, this::invalidate);
    private final ResourceHud hud = new ResourceHud(config);
    private final ResourceNotice notice = new ResourceNotice(config, hud);
    /** Zones whose timer was started while the player stood inside; announced again on leaving. */
    private final Set<String> startedInside = new HashSet<>();
    private final ResourceProposals proposals = new ResourceProposals();
    private int outlineTicks;
    private ResourceZone editorOutline;
    private String editorOutlineDimension = "";
    private String showId = "";
    private long showUntil;
    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Long> noticeCooldowns = new HashMap<>();
    private final Set<String> insideResources = new HashSet<>();
    private final Map<String, Boolean> readyStates = new HashMap<>();
    private volatile List<ResourceSnapshot> snapshots = List.of();
    private volatile List<ResourceSnapshot> cache = List.of();
    private volatile boolean snapshotsDirty = true;
    private long nextRebuild;
    private String currentTerritory = "";
    private Supplier<Screen> screenRequest;
    private KeyBinding confirmKey;
    private KeyBinding hudKey;
    private KeyBinding overviewKey;
    private String pendingToastResourceId;
    private ResourceSnapshot navigationTarget;
    private boolean minimapHooked;
    private boolean worldmapHooked;
    private final ResourceActionbarTracker actionbarTracker = new ResourceActionbarTracker();
    private final ResourcePickupTracker pickupTracker = new ResourcePickupTracker();
    private ResourceStatistics statistics;
    private boolean openStatisticsRequested;
    private String statisticsDimension;
    private long statisticsWorldSince;
    private static final List<String> STATISTICS_TYPES = List.of("IRON_DEPOSIT", "COAL_DEPOSIT",
            "SALT_DEPOSIT", "MONASTERY_GARDEN", "PEAT_BOG", "CLAY_PIT");

    void register() {
        confirmKey = new KeyBinding("key.ottoextra.resources.confirm", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R, KeyBinding.Category.MISC);
        hudKey = new KeyBinding("key.ottoextra.resources.hud", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC);
        overviewKey = new KeyBinding("key.ottoextra.resources.overview", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(confirmKey);
        KeyBindingHelper.registerKeyBinding(hudKey);
        KeyBindingHelper.registerKeyBinding(overviewKey);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveStatistics());
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()) onInteraction(MinecraftClient.getInstance(), pos,
                    world.getBlockState(pos), "BLOCK_BREAK");
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient()) onInteraction(MinecraftClient.getInstance(), hit.getBlockPos(),
                    world.getBlockState(hit.getBlockPos()), "BLOCK_INTERACT");
            return ActionResult.PASS;
        });
        ResourceMapOverlay.register(this::mapResources, ResourceIcons::iconFor, this::navigateTo,
                parent -> MinecraftClient.getInstance().setScreen(
                        new ResourceMapFilterScreen(parent, config)));
        hud.register();
        notice.register();
        registerCommands();
    }

    void onServerJoin() {
        resetStatisticsTracking();
        statisticsDimension = null;
        ResourceIcons.clear();
        invalidate();
    }

    void onDisconnect() {
        saveStatistics();
        statistics = null;
        openStatisticsRequested = false;
        statisticsDimension = null;
        resetStatisticsTracking();
        clearNavigation();
        snapshots = List.of(); cache = List.of();
        hud.clear();
        pending.clear();
        noticeCooldowns.clear();
        insideResources.clear();
        readyStates.clear();
        currentTerritory = "";
        proposals.discard();
        showId = "";
        pendingToastResourceId = null;
        startedInside.clear();
        notice.clear();
        invalidate();
    }

    private void tick(MinecraftClient client) {
        // The global map renderer must be ready before any harvest confirmation.
        if (client.world != null && !worldmapHooked && FabricLoader.getInstance().isModLoaded("xaeroworldmap")) {
            try {
                worldmapHooked = de.ottoextra.resources.xaero.ResourceWorldmapIcons.register(
                        this::mapResources, ResourceIcons::iconFor, this::navigateTo);
            } catch (LinkageError error) {
                worldmapHooked = true;
                OttoExtraResources.LOGGER.warn("Xaero-Worldmap-API nicht kompatibel; Ressourcen-Overlay bleibt aktiv: {}", error.toString());
            }
        }
        if (statistics != null) statistics.tick(System.currentTimeMillis());
        if (openStatisticsRequested) {
            openStatisticsRequested = false;
            if (client.player != null) openStatistics(null);
        }
        if (screenRequest != null) {
            Supplier<Screen> request = screenRequest;
            screenRequest = null;
            if (client.player != null) client.setScreen(request.get());
        }
        if (!available() || client.player == null || client.world == null) {
            resetStatisticsTracking();
            if (statistics != null) statistics.finishRun();
            hud.clear();
            return;
        }
        updateStatisticsDimension(client);
        // AttackBlockCallback only fires at the start; keep context fresh during slow block mining.
        if (client.currentScreen == null && client.options.attackKey.isPressed()
                && client.crosshairTarget instanceof BlockHitResult hit) {
            trackStatisticsInteraction(client, hit.getBlockPos(),
                    client.world.getBlockState(hit.getBlockPos()), "BLOCK_BREAK");
        }
        if (!minimapHooked && FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            minimapHooked = de.ottoextra.resources.xaero.ResourceMinimapIcons.register(
                    this::mapResources, ResourceIcons::iconFor, () -> config.showMinimapStatusIndicator);
        }
        long now = System.currentTimeMillis();
        noticeCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
        if (snapshotsDirty || now >= nextRebuild) rebuild(client, now);
        finishNavigationInTargetZone(client);
        updateEnteredResources(client);
        updatePendingConfirmationWindow(client, now);
        clearStalePendingToast();
        while (confirmKey.wasPressed()) confirm(client);
        while (hudKey.wasPressed()) toggleHud(client);
        while (overviewKey.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new ResourceBookScreen(null, this, ResourceBookScreen.View.LIST));
        }
        sync.tick(client, now);
        if (proposals.expireIfDue(now)) {
            message(client, Text.translatable("ottoextra.resources.proposal.expired").formatted(Formatting.GRAY));
        }
        if (++outlineTicks >= ResourceOutlineParticles.INTERVAL_TICKS) {
            outlineTicks = 0;
            emitOutlines(client, now);
        }
    }

    /** Re-emits the particle outlines of everything that is currently being shown. */
    private void emitOutlines(MinecraftClient client, long now) {
        if (!config.showOutlineParticles || client.world == null || client.player == null) return;
        double px = client.player.getX();
        double pz = client.player.getZ();
        String dimension = dimension(client);
        if (editorOutline != null && editorOutlineDimension.equals(dimension)) {
            ResourceOutlineParticles.emit(client.world, px, pz, editorOutline);
        }
        ResourceProposals.Proposal proposal = proposals.current(now);
        if (proposal != null && proposal.dimension().equals(dimension)) {
            ResourceOutlineParticles.emit(client.world, px, pz, proposal.zone());
        }
        if (!showId.isEmpty()) {
            if (now >= showUntil) {
                showId = "";
            } else {
                ResourceSnapshot shown = fresh(showId);
                if (shown != null && shown.dimension().equals(dimension)) {
                    ResourceOutlineParticles.emit(client.world, px, pz, shown.zone());
                }
            }
        }
    }

    /** The editor reports the zone it currently edits; null when it closes. */
    void setEditorOutline(ResourceZone zone, String dimension) {
        editorOutline = zone;
        editorOutlineDimension = dimension == null ? "" : dimension;
    }

    /** Toggles the particle outline of the zone the player stands in, else the nearest one, for 30 s. */
    private void toggleShow(MinecraftClient client) {
        if (!available() || client.player == null) return;
        if (!showId.isEmpty()) {
            showId = "";
            message(client, Text.translatable("ottoextra.resources.show.off").formatted(Formatting.GRAY));
            return;
        }
        String dimension = dimension(client);
        BlockPos here = client.player.getBlockPos();
        List<ResourceSnapshot> candidates = cache.stream().filter(r -> r.dimension().equals(dimension)).toList();
        List<ResourceSnapshot> inside = candidates.stream().filter(r -> r.zone().contains(here)).toList();
        ResourceSnapshot target = ResourceOverlap.nearest(inside.isEmpty() ? candidates : inside,
                here.getX(), here.getY(), here.getZ());
        if (target == null) {
            message(client, Text.translatable("ottoextra.resources.show.none").formatted(Formatting.RED));
            return;
        }
        showId = target.id();
        showUntil = System.currentTimeMillis() + SHOW_MILLIS;
        message(client, Text.translatable(config.showOutlineParticles ? "ottoextra.resources.show.on"
                : "ottoextra.resources.show.disabled", target.name()).formatted(Formatting.GRAY));
    }

    /** Marks the snapshots stale so the next tick rebuilds them. */
    void invalidate() {
        snapshotsDirty = true;
    }

    private void rebuild(MinecraftClient client, long now) {
        snapshotsDirty = false;
        nextRebuild = now + REBUILD_INTERVAL_MILLIS;
        List<ResourceSnapshot> all = repository.snapshots(now);
        snapshots = all;
        cache = all.stream().filter(resource -> !resource.markerOnly()).toList();
        if (client.player == null || client.world == null) return;
        String dimension = dimension(client);
        double x = client.player.getX();
        double z = client.player.getZ();
        currentTerritory = ResourceTerritories.nameAt(dimension, x, z);
        playReadySound(client, ResourceHudModel.build(all, dimension, currentTerritory,
                config.hudShowAll, x, z, Integer.MAX_VALUE).shown());
        if (config.hudEnabled) {
            hud.update(client, ResourceHudModel.build(all, dimension, currentTerritory,
                    config.hudShowAll, x, z, config.hudMaxRows), currentTerritory);
        } else {
            hud.clear();
        }
    }

    /** One sound when an entry of the HUD set went from regenerating to ready while watched. */
    private void playReadySound(MinecraftClient client, List<ResourceSnapshot> watched) {
        boolean play = false;
        Set<String> ids = new HashSet<>();
        for (ResourceSnapshot resource : watched) {
            boolean ready = "READY".equals(resource.status());
            Boolean before = readyStates.put(resource.id(), ready);
            ids.add(resource.id());
            if (before != null && !before && ready) play = true;
        }
        readyStates.keySet().retainAll(ids);
        if (play && config.readySound) {
            client.getSoundManager().play(PositionedSoundInstance.ui(
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 0.9f));
        }
    }

    private void toggleHud(MinecraftClient client) {
        config.hudEnabled = !config.hudEnabled;
        config.save();
        invalidate();
        message(client, Text.translatable(config.hudEnabled ? "ottoextra.resources.hud.on"
                : "ottoextra.resources.hud.off").formatted(Formatting.GRAY));
    }

    void navigateTo(ResourceSnapshot resource) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            message(client, Text.literal("Xaero's Minimap wird für die Navigation benötigt.")
                    .formatted(Formatting.RED));
            return;
        }
        try {
            if (!de.ottoextra.resources.xaero.ResourceWaypointNavigation.navigate(resource)) {
                message(client, Text.literal("Navigation konnte nicht gestartet werden.")
                        .formatted(Formatting.RED));
                return;
            }
            navigationTarget = resource;
            message(client, Text.literal("Navigation gestartet: " + resource.name())
                    .formatted(Formatting.AQUA));
        } catch (Throwable error) {
            navigationTarget = null;
            message(client, Text.literal("Xaero-Navigation ist nicht verfügbar.")
                    .formatted(Formatting.RED));
        }
    }

    private void finishNavigationInTargetZone(MinecraftClient client) {
        ResourceSnapshot target = navigationTarget;
        if (target == null || !target.dimension().equals(dimension(client))
                || !target.zone().contains(client.player.getBlockPos())) return;
        clearNavigation();
        message(client, Text.literal("Ziel erreicht: " + target.name())
                .formatted(Formatting.GREEN));
    }

    private void clearNavigation() {
        if (navigationTarget != null && FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            try {
                de.ottoextra.resources.xaero.ResourceWaypointNavigation.clear();
            } catch (Throwable ignored) {
            }
        }
        navigationTarget = null;
    }

    void onInteraction(MinecraftClient client, BlockPos pos, BlockState state, String interaction) {
        if (!available() || client.player == null || client.world == null) return;
        trackStatisticsInteraction(client, pos, state, interaction);
        String dimension = dimension(client);
        for (ResourceSnapshot resource : nearestMatch(pos, r -> r.dimension().equals(dimension)
                && relevant(r.type(), state, interaction))) {
            if (!resource.writable()) return;
            boolean startable = canStartHarvest(resource.type(), resource.status(),
                    resource.regenerationPercentage());
            boolean forceRestart = canForceRestart(resource.type(), resource.status());
            if (config.autoStartOnBreak && interaction.equals("BLOCK_BREAK") && startable) {
                completeStart(client, resource, false);
                return;
            }
            if (!startable && !forceRestart || !startable && config.autoStartOnBreak) {
                if (!noticeCooldowns.containsKey(resource.id())) {
                    noticeCooldowns.put(resource.id(), System.currentTimeMillis() + NOTICE_COOLDOWN_MILLIS);
                    message(client, Text.translatable(regeneratingStatusKey(resource.type(),
                                    resource.regenerationPercentage()), resource.name(),
                            resource.regenerationPercentage(), remaining(regenerationCountdown(resource)))
                            .formatted(Formatting.YELLOW));
                }
                return;
            }
            if (pending.containsKey(resource.id())) return;
            pending.put(resource.id(), new Pending(resource, pos.toImmutable(), forceRestart, 0L));
            if (config.hintsEnabled) {
                pendingResourceToast(resource, forceRestart, false);
            }
            return;
        }
    }

    /** Entries around a position matching the filter; overlaps resolve to the nearest centre. */
    private List<ResourceSnapshot> nearestMatch(BlockPos pos,
                                                java.util.function.Predicate<ResourceSnapshot> filter) {
        List<ResourceSnapshot> matches = cache.stream()
                .filter(resource -> resource.zone().contains(pos)).filter(filter).toList();
        ResourceSnapshot best = ResourceOverlap.nearest(matches, pos.getX(), pos.getY(), pos.getZ());
        return best == null ? List.of() : List.of(best);
    }

    private void confirm(MinecraftClient client) {
        boolean runFinished = false;
        if (available() && client.player != null) {
            if (statistics != null) runFinished = statistics.finishRun();
            resetStatisticsTracking();
        }
        if (!available() || client.player == null || pending.isEmpty()) {
            message(client, Text.translatable(runFinished ? "ottoextra.resources.stats.run_finished"
                    : "ottoextra.resources.no_pending").formatted(Formatting.GRAY));
            return;
        }
        long now = System.currentTimeMillis();
        BlockPos playerPos = client.player.getBlockPos();
        Pending selected = pending.values().stream().filter(value -> sameDimension(value.resource, client)
                && confirmationAllowed(value.resource.zone().contains(playerPos),
                        value.outsideSinceMillis, now))
                .findFirst().orElse(null);
        if (selected == null) {
            message(client, Text.translatable(runFinished ? "ottoextra.resources.stats.run_finished"
                    : "ottoextra.resources.no_pending").formatted(Formatting.GRAY));
            return;
        }
        pending.remove(selected.resource.id());
        clearPendingToast(selected.resource.id());
        completeStart(client, selected.resource, selected.forceRestart);
    }

    private void start(MinecraftClient client) {
        ResourceSnapshot resource = currentResource(client);
        if (resource == null) return;
        if (!canStartHarvest(resource.type(), resource.status(),
                resource.regenerationPercentage())) {
            message(client, Text.translatable(regeneratingStatusKey(resource.type(),
                            resource.regenerationPercentage()), resource.name(),
                    resource.regenerationPercentage(), remaining(regenerationCountdown(resource)))
                    .formatted(Formatting.YELLOW));
            return;
        }
        pending.remove(resource.id());
        clearPendingToast(resource.id());
        completeStart(client, resource, false);
    }

    private void completeStart(MinecraftClient client, ResourceSnapshot selected, boolean forceRestart) {
        if (!startEntry(selected.id())) {
            message(client, Text.translatable("ottoextra.resources.readonly").formatted(Formatting.RED));
            return;
        }
        ResourceSnapshot resource = fresh(selected.id());
        if (resource == null) return;
        if (insideResources.contains(resource.id())) startedInside.add(resource.id());
        resourceToast(resource, ResourceNoticeModel.event(forceRestart ? ResourceNoticeModel.Kind.RESTARTED
                : ResourceNoticeModel.Kind.STARTED, resource.regenerationPercentage(), resource.secondsUntilFull()), null);
    }

    private void reset(MinecraftClient client) {
        ResourceSnapshot selected = currentResource(client);
        if (selected == null) return;
        if (!resetEntry(selected.id())) {
            message(client, Text.translatable("ottoextra.resources.readonly").formatted(Formatting.RED));
            return;
        }
        pending.remove(selected.id());
        clearPendingToast(selected.id());
        ResourceSnapshot resource = fresh(selected.id());
        startedInside.remove(selected.id());
        if (resource != null) {
            resourceToast(resource, ResourceNoticeModel.event(ResourceNoticeModel.Kind.RESET, 100, 0), null);
        }
    }

    // ---- entry actions shared by commands, keys and screens -----------------------------

    /** Starts (or restarts) the timer of an entry; false for unknown or read-only entries. */
    boolean startEntry(String id) {
        ResourceList list = repository.listOfZone(id);
        if (list == null) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        String by = config.shareStartedBy && list.shared() && client.getSession() != null
                ? client.getSession().getUsername() : null;
        return changed(list, repository.startTimer(list, id, by, System.currentTimeMillis()), id);
    }

    boolean resetEntry(String id) {
        ResourceList list = repository.listOfZone(id);
        return list != null && changed(list, repository.resetTimer(list, id, System.currentTimeMillis()), id);
    }

    boolean shiftEntry(String id, long deltaMillis) {
        ResourceList list = repository.listOfZone(id);
        return list != null && changed(list, repository.shiftTimer(list, id, deltaMillis,
                System.currentTimeMillis()), id);
    }

    boolean deleteEntry(String id) {
        ResourceList list = repository.listOfZone(id);
        return list != null && changed(list, repository.deleteZone(list, id, System.currentTimeMillis()), id);
    }

    boolean updateEntry(ZoneRecord record) {
        ResourceList list = repository.listOfZone(record.id());
        return list != null && changed(list, repository.updateZone(list, record,
                System.currentTimeMillis()), record.id());
    }

    private boolean changed(ResourceList list, boolean success, String id) {
        if (!success) return false;
        // A manual change must not sound like a timer that ran out.
        readyStates.remove(id);
        pending.remove(id);
        if (list.shared()) sync.syncSoon(list.localId);
        rebuild(MinecraftClient.getInstance(), System.currentTimeMillis());
        return true;
    }

    private ResourceSnapshot fresh(String id) {
        for (ResourceSnapshot snapshot : snapshots) if (snapshot.id().equals(id)) return snapshot;
        return null;
    }

    // ---- commands ----------------------------------------------------------------------

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            var resources = ClientCommandManager.literal("resources")
                    .executes(ctx -> { openScreen(() -> new ResourceBookScreen(null, this, ResourceBookScreen.View.LIST)); return 1; })
                    .then(ClientCommandManager.literal("stats").executes(ctx -> { openStatisticsRequested = true; return 1; }))
                    .then(ClientCommandManager.literal("overview").executes(ctx -> { openScreen(() -> new ResourceBookScreen(null, this, ResourceBookScreen.View.LIST)); return 1; }))
                    .then(ClientCommandManager.literal("lists").executes(ctx -> { openScreen(() -> new ResourceBookScreen(null, this, ResourceBookScreen.View.LIST)); return 1; }))
                    .then(ClientCommandManager.literal("hud").executes(ctx -> { toggleHud(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("nearby").executes(ctx -> { nearby(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("status").executes(ctx -> { status(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("confirm").executes(ctx -> { confirm(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("start").executes(ctx -> { start(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("reset").executes(ctx -> { reset(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("map").executes(ctx -> { toggleMap(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("bestaetigen").executes(ctx -> { confirmProposal(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("verwerfen").executes(ctx -> { discardProposal(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("bearbeiten").executes(ctx -> { editProposal(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("radius")
                            .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1, 64))
                                    .executes(ctx -> { proposalRadius(MinecraftClient.getInstance(), IntegerArgumentType.getInteger(ctx, "n")); return 1; })))
                    .then(ClientCommandManager.literal("zeigen").executes(ctx -> { toggleShow(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("teilen").executes(ctx -> { shareActive(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("beitreten")
                            .then(ClientCommandManager.argument("code", StringArgumentType.greedyString())
                                    .executes(ctx -> { join(MinecraftClient.getInstance(), StringArgumentType.getString(ctx, "code")); return 1; })))
                    .then(ClientCommandManager.literal("verlassen").executes(ctx -> { leaveActive(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("code-erneuern").executes(ctx -> { renewActive(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("export")
                            .executes(ctx -> { exportActive(MinecraftClient.getInstance(), ""); return 1; })
                            .then(ClientCommandManager.argument("datei", StringArgumentType.word())
                                    .executes(ctx -> { exportActive(MinecraftClient.getInstance(), StringArgumentType.getString(ctx, "datei")); return 1; })))
                    .then(ClientCommandManager.literal("import")
                            .executes(ctx -> { importList(MinecraftClient.getInstance(), ""); return 1; })
                            .then(ClientCommandManager.argument("datei", StringArgumentType.word())
                                    .executes(ctx -> { importList(MinecraftClient.getInstance(), StringArgumentType.getString(ctx, "datei")); return 1; })))
                    .then(ClientCommandManager.literal("create")
                            .then(ClientCommandManager.literal("point")
                                    .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                            .suggests((context, builder) -> suggestTypes(builder))
                                            .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                    .executes(ctx -> create(MinecraftClient.getInstance(), StringArgumentType.getString(ctx, "type"), StringArgumentType.getString(ctx, "name")))))));
            var poiCreate = ClientCommandManager.literal("create")
                    .then(ClientCommandManager.argument("selection", StringArgumentType.greedyString())
                            .executes(ctx -> createPoi(MinecraftClient.getInstance(),
                                    StringArgumentType.getString(ctx, "selection"))));
            dispatcher.register(ClientCommandManager.literal("ottoextra")
                    .then(resources)
                    .then(ClientCommandManager.literal("resource")
                            .then(ClientCommandManager.literal("poi").then(poiCreate))));
            dispatcher.register(ClientCommandManager.literal("ores")
                    .then(ClientCommandManager.literal("stats").executes(ctx -> { openStatisticsRequested = true; return 1; }))
                    .then(ClientCommandManager.literal("overview").executes(ctx -> { openScreen(() -> new ResourceBookScreen(null, this, ResourceBookScreen.View.LIST)); return 1; }))
                    .then(ClientCommandManager.literal("confirm").executes(ctx -> { confirm(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("start").executes(ctx -> { start(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("reset").executes(ctx -> { reset(MinecraftClient.getInstance()); return 1; }))
                    .then(ClientCommandManager.literal("map").executes(ctx -> { toggleMap(MinecraftClient.getInstance()); return 1; })));
        });
    }

    /** Screens opened from a command are opened on the next tick, after the chat screen closed. */
    void openScreen(Supplier<Screen> screen) {
        screenRequest = screen;
    }

    private CompletableFuture<Suggestions> suggestTypes(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().trim().toLowerCase(Locale.ROOT);
        for (String type : ResourcePolicy.TYPES) {
            if (remaining.isEmpty() || type.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(type);
            }
        }
        return builder.buildFuture();
    }

    private void toggleMap(MinecraftClient client) {
        boolean visible = ResourceMapOverlay.toggle();
        message(client, Text.translatable("ottoextra.resources.toggle",
                Text.translatable("ottoextra.resources.filter.resources"),
                Text.translatable(visible ? "ottoextra.resources.on" : "ottoextra.resources.off"))
                .formatted(visible ? Formatting.GREEN : Formatting.GRAY));
    }

    // ---- creating entries --------------------------------------------------------------

    /**
     * Command: starts a proposal for a point of radius 3 at the player position. Nothing is saved
     * until the player confirms in chat; see {@link ResourceProposals}.
     */
    int create(MinecraftClient client, String rawType, String rawName) {
        if (!available() || client.player == null || client.world == null) return 0;
        String type = rawType.toUpperCase(Locale.ROOT);
        String name = rawName.trim();
        if (!ResourcePolicy.known(type) || name.isEmpty() || name.length() > ResourceWire.NAME_MAX) {
            message(client, Text.translatable("ottoextra.resources.create.invalid").formatted(Formatting.RED));
            return 0;
        }
        BlockPos pos = client.player.getBlockPos();
        ResourceProposals.Proposal proposal = proposals.create(type, name, dimension(client),
                pos.getX(), pos.getY(), pos.getZ(), System.currentTimeMillis());
        printProposal(client, proposal);
        return 1;
    }

    /** One chat line with the summary and clickable client commands. */
    private void printProposal(MinecraftClient client, ResourceProposals.Proposal proposal) {
        MutableText line = Text.translatable("ottoextra.resources.proposal.line", proposal.name(),
                ResourceBookScreen.typeName(proposal.type()), proposal.radius()).formatted(Formatting.YELLOW);
        line.append(Text.literal(" "));
        line.append(chatButton("ottoextra.resources.proposal.save", "/ottoextra resources bestaetigen",
                Formatting.GREEN, "ottoextra.resources.proposal.save.hover"));
        line.append(Text.literal(" "));
        line.append(chatButton("ottoextra.resources.proposal.discard", "/ottoextra resources verwerfen",
                Formatting.RED, "ottoextra.resources.proposal.discard.hover"));
        line.append(Text.literal(" "));
        line.append(chatButton("ottoextra.resources.proposal.edit", "/ottoextra resources bearbeiten",
                Formatting.GOLD, "ottoextra.resources.proposal.edit.hover"));
        line.append(Text.literal(" "));
        line.append(chatButton("ottoextra.resources.proposal.smaller", "/ottoextra resources radius "
                + Math.max(1, proposal.radius() - 1), Formatting.AQUA, "ottoextra.resources.proposal.smaller.hover"));
        line.append(Text.literal(" "));
        line.append(chatButton("ottoextra.resources.proposal.larger", "/ottoextra resources radius "
                + Math.min(64, proposal.radius() + 1), Formatting.AQUA, "ottoextra.resources.proposal.larger.hover"));
        message(client, line);
    }

    private static MutableText chatButton(String labelKey, String command, Formatting color, String hoverKey) {
        return Text.translatable(labelKey).styled(style -> style.withColor(color)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Text.translatable(hoverKey))));
    }

    private void noProposal(MinecraftClient client) {
        message(client, Text.translatable("ottoextra.resources.proposal.none").formatted(Formatting.RED));
    }

    void confirmProposal(MinecraftClient client) {
        ResourceProposals.Proposal proposal = proposals.confirm(System.currentTimeMillis());
        if (proposal == null) { noProposal(client); return; }
        createEntry(client, activeList(), "", proposal.name(), proposal.type(), proposal.zone(), null, null, "", null);
    }

    void discardProposal(MinecraftClient client) {
        if (proposals.discard()) {
            message(client, Text.translatable("ottoextra.resources.proposal.discarded").formatted(Formatting.GRAY));
        } else {
            noProposal(client);
        }
    }

    void proposalRadius(MinecraftClient client, int radius) {
        ResourceProposals.Proposal proposal = proposals.adjustRadius(radius, System.currentTimeMillis());
        if (proposal == null) { noProposal(client); return; }
        printProposal(client, proposal);
    }

    void editProposal(MinecraftClient client) {
        ResourceProposals.Proposal proposal = proposals.beginEdit(System.currentTimeMillis());
        if (proposal == null) { noProposal(client); return; }
        openScreen(() -> ResourceEditScreen.forProposal(null, this, proposal));
    }

    /** Called by the editor when it closes after working on the pending proposal. */
    void finishProposalEdit(MinecraftClient client, boolean saved) {
        long now = System.currentTimeMillis();
        proposals.endEdit(saved, now);
        ResourceProposals.Proposal proposal = proposals.current(now);
        if (!saved && proposal != null) printProposal(client, proposal);
    }

    /**
     * Creates an entry from the editor.
     *
     * @param list      target list, or null to create a new list named {@code newListName}
     * @param territory Lehen to store; null detects it from the zone centre
     */
    int createEntry(MinecraftClient client, ResourceList list, String newListName, String name,
                    String type, ResourceZone zone, String territory, Integer customMinutes,
                    String note, String markerSymbol) {
        if (!available() || client.player == null || client.world == null) return 0;
        return submitCreate(client, list, newListName, name, type, zone, territory, customMinutes, note,
                markerSymbol);
    }


    /** POI marker: {@code <name> [x y z]}; without coordinates the player position is used. */
    int createPoi(MinecraftClient client, String rawSelection) {
        if (!available() || client.player == null || client.world == null) return 0;
        String selection = rawSelection == null ? "" : rawSelection.trim();
        String[] parts = selection.split("\\s+");
        BlockPos position = client.player.getBlockPos();
        String name = selection;
        if (parts.length >= 4) {
            try {
                int x = Integer.parseInt(parts[parts.length - 3]);
                int y = Integer.parseInt(parts[parts.length - 2]);
                int z = Integer.parseInt(parts[parts.length - 1]);
                position = new BlockPos(x, y, z);
                name = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 3));
            } catch (NumberFormatException ignored) {
                // The last words are part of the name.
            }
        }
        if (name.isBlank() || name.length() > ResourceWire.NAME_MAX
                || Math.abs(position.getX()) > 30_000_000 || Math.abs(position.getZ()) > 30_000_000
                || Math.abs(position.getY()) > 2048) {
            message(client, Text.translatable("ottoextra.resources.create.invalid").formatted(Formatting.RED));
            return 0;
        }
        return submitCreate(client, activeList(), "", name, ResourcePolicy.MARKER,
                ResourceZone.point(position.getX(), position.getY(), position.getZ(), 3), null, null, "", null);
    }

    private int submitCreate(MinecraftClient client, ResourceList list, String newListName,
                             String name, String type, ResourceZone zone, String givenTerritory,
                             Integer customMinutes, String note, String markerSymbol) {
        long now = System.currentTimeMillis();
        String dimension = dimension(client);
        String territory = givenTerritory != null ? givenTerritory : ResourceTerritories.nameAt(dimension,
                ResourceMapOverlay.centerX(zone), ResourceMapOverlay.centerZ(zone));
        ResourceList target = list;
        if (target == null) {
            String listName = newListName == null || newListName.isBlank()
                    ? (territory.isEmpty() ? Text.translatable("ottoextra.resources.list.default").getString()
                            : territory) : newListName;
            target = repository.createList(listName);
        }
        if (!target.writable()) {
            message(client, Text.translatable("ottoextra.resources.readonly").formatted(Formatting.RED));
            return 0;
        }
        ZoneRecord record = repository.addZone(target, name, type, dimension, territory, zone, customMinutes, note,
                markerSymbol, now);
        if (record == null) {
            message(client, Text.translatable("ottoextra.resources.create.full").formatted(Formatting.RED));
            return 0;
        }
        setActive(target);
        if (target.shared()) sync.syncSoon(target.localId);
        rebuild(client, now);
        message(client, Text.translatable("ottoextra.resources.create.done", record.name(), target.name)
                .formatted(Formatting.GREEN));
        return 1;
    }

    private void nearby(MinecraftClient client) {
        if (!available() || client.player == null) return;
        String dimension = dimension(client);
        double px = client.player.getX();
        double pz = client.player.getZ();
        double limit = (double) NEARBY_COMMAND_RADIUS * NEARBY_COMMAND_RADIUS;
        List<ResourceSnapshot> near = cache.stream().filter(resource -> resource.dimension().equals(dimension)
                && distanceSquared(resource, px, pz) <= limit)
                .sorted(java.util.Comparator.comparingDouble(resource -> distanceSquared(resource, px, pz)))
                .toList();
        if (near.isEmpty()) { message(client, Text.translatable("ottoextra.resources.nearby.empty").formatted(Formatting.GRAY)); return; }
        message(client, Text.translatable("ottoextra.resources.nearby.title").formatted(Formatting.GOLD));
        for (ResourceSnapshot resource : near) message(client, Text.literal("• " + resource.name() + " — "
                + resource.regenerationPercentage() + " % — " + remaining(resource.secondsUntilFull()))
                .formatted(Formatting.GRAY));
    }

    static double distanceSquared(ResourceSnapshot resource, double x, double z) {
        double dx = ResourceMapOverlay.centerX(resource.zone()) - x;
        double dz = ResourceMapOverlay.centerZ(resource.zone()) - z;
        return dx * dx + dz * dz;
    }

    private void status(MinecraftClient client) {
        ResourceSnapshot resource = currentResource(client);
        if (resource == null) return;
        message(client, Text.translatable(statusMessageKey(resource.type(),
                        resource.regenerationPercentage()), resource.name(),
                resource.regenerationPercentage(), remaining(regenerationCountdown(resource)))
                .formatted(Formatting.GOLD));
    }

    private ResourceSnapshot currentResource(MinecraftClient client) {
        if (!available() || client.player == null) return null;
        BlockPos here = client.player.getBlockPos();
        ResourceSnapshot resource = ResourceOverlap.nearest(cache.stream().filter(r -> sameDimension(r, client)
                && r.zone().contains(here)).toList(), here.getX(), here.getY(), here.getZ());
        if (resource == null) {
            message(client, Text.translatable("ottoextra.resources.status.none")
                    .formatted(Formatting.GRAY));
        }
        return resource;
    }

    // ---- lists, sharing, text ----------------------------------------------------------

    ResourceRepository repository() { return repository; }
    ResourcesConfig config() { return config; }
    ResourceSync sync() { return sync; }
    List<ResourceSnapshot> snapshots() { return snapshots; }
    String currentTerritory() { return currentTerritory; }

    /** Online sharing needs the sharing setting and the core consent for online services. */
    static boolean sharingAllowed() {
        return ResourcesConfig.active().sharingEnabled && ConsentPolicy.allowed(
                OttoExtraConfig.active(), ConsentPolicy.Purpose.ONLINE_SERVICES);
    }

    ResourceList activeList() {
        ResourceList list = repository.list(config.activeListId);
        if (list != null) return list;
        List<ResourceList> all = repository.lists();
        return all.isEmpty() ? null : all.getFirst();
    }

    void setActive(ResourceList list) {
        if (list == null || list.localId.equals(config.activeListId)) return;
        config.activeListId = list.localId;
        config.save();
    }

    /** Explains why sharing is not possible right now; returns true when it is. */
    boolean sharingReady(MinecraftClient client) {
        if (!config.sharingEnabled) {
            message(client, Text.translatable("ottoextra.resources.share.disabled").formatted(Formatting.RED));
            return false;
        }
        if (!sharingAllowed()) {
            message(client, Text.translatable("ottoextra.resources.share.consent").formatted(Formatting.RED));
            return false;
        }
        return true;
    }

    private void noList(MinecraftClient client) {
        message(client, Text.translatable("ottoextra.resources.list.none").formatted(Formatting.RED));
    }

    void shareActive(MinecraftClient client) {
        if (!sharingReady(client)) return;
        ResourceList list = activeList();
        if (list == null) { noList(client); return; }
        shareList(list);
    }

    void shareList(ResourceList list) {
        if (!list.shared()) repository.share(list);
        sync.syncSoon(list.localId);
        setActive(list);
        openScreen(() -> new ResourceBookScreen(null, this, ResourceBookScreen.View.SHARE));
    }

    void renewActive(MinecraftClient client) {
        if (!sharingReady(client)) return;
        ResourceList list = activeList();
        if (list == null) { noList(client); return; }
        renewList(client, list);
    }

    void renewList(MinecraftClient client, ResourceList list) {
        if (!repository.renewCode(list, System.currentTimeMillis())) {
            message(client, Text.translatable("ottoextra.resources.share.not_writer").formatted(Formatting.RED));
            return;
        }
        sync.syncSoon(list.localId);
        setActive(list);
        openScreen(() -> new ResourceBookScreen(null, this, ResourceBookScreen.View.SHARE));
    }

    void leaveActive(MinecraftClient client) {
        ResourceList list = activeList();
        if (list == null) { noList(client); return; }
        if (!list.shared()) {
            message(client, Text.translatable("ottoextra.resources.share.not_shared").formatted(Formatting.RED));
            return;
        }
        removeList(client, list);
    }

    void removeList(MinecraftClient client, ResourceList list) {
        repository.remove(list);
        sync.forget(list.localId);
        invalidate();
        message(client, Text.translatable("ottoextra.resources.list.removed", list.name).formatted(Formatting.GRAY));
    }

    void join(MinecraftClient client, String rawCode) {
        if (!sharingReady(client)) return;
        ResourceCodes.Kind kind = ResourceCodes.kind(rawCode);
        if (kind == ResourceCodes.Kind.INVALID) {
            message(client, Text.translatable("ottoextra.resources.share.code_invalid").formatted(Formatting.RED));
            return;
        }
        String normalized = ResourceCodes.normalize(rawCode);
        String write = kind == ResourceCodes.Kind.WRITE ? normalized : null;
        String read = write != null ? ResourceCodes.readCode(write) : normalized;
        String shareId = ResourceCodes.listId(read);
        ResourceList existing = repository.listByShareId(shareId);
        ResourceList list;
        if (existing != null) {
            if (write != null && existing.writeCode == null) repository.upgrade(existing, write);
            else {
                message(client, Text.translatable("ottoextra.resources.share.already").formatted(Formatting.YELLOW));
                return;
            }
            list = existing;
        } else {
            list = repository.join(Text.translatable("ottoextra.resources.list.shared",
                    shareId.substring(0, 4)).getString(), write, read);
        }
        setActive(list);
        sync.syncSoon(list.localId);
        message(client, Text.translatable(write != null ? "ottoextra.resources.share.joined_write"
                : "ottoextra.resources.share.joined_read", list.name).formatted(Formatting.GREEN));
    }

    /** Copies the active list as text (no argument) or writes it to a file. */
    void exportActive(MinecraftClient client, String fileName) {
        ResourceList list = activeList();
        if (list == null) { noList(client); return; }
        exportList(client, list, fileName);
    }

    void exportList(MinecraftClient client, ResourceList list, String fileName) {
        String text = ResourceWire.exportText(list.name, list.zones.values());
        if (fileName == null || fileName.isBlank()) {
            client.keyboard.setClipboard(text);
            message(client, Text.translatable("ottoextra.resources.text.copied", list.liveZoneCount())
                    .formatted(Formatting.GREEN));
            return;
        }
        Path file = exportFile(fileName);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
            message(client, Text.translatable("ottoextra.resources.text.exported", file.getFileName().toString())
                    .formatted(Formatting.GREEN));
        } catch (java.io.IOException error) {
            message(client, Text.translatable("ottoextra.resources.text.file_failed").formatted(Formatting.RED));
        }
    }

    /** Reads a list from the clipboard (no argument) or an export file into a new list. */
    void importList(MinecraftClient client, String fileName) {
        String text;
        if (fileName == null || fileName.isBlank()) {
            text = client.keyboard.getClipboard();
        } else {
            try {
                text = Files.readString(exportFile(fileName), StandardCharsets.UTF_8);
            } catch (java.io.IOException error) {
                message(client, Text.translatable("ottoextra.resources.text.file_failed").formatted(Formatting.RED));
                return;
            }
        }
        ResourceWire.ImportedList imported;
        try {
            imported = ResourceWire.importText(text, ResourceWire::newId, System.currentTimeMillis());
        } catch (IllegalArgumentException error) {
            message(client, Text.translatable("ottoextra.resources.text.invalid").formatted(Formatting.RED));
            return;
        }
        ResourceList list = repository.importZones(imported.name().isEmpty()
                ? Text.translatable("ottoextra.resources.list.imported").getString()
                : imported.name(), imported.zones());
        setActive(list);
        invalidate();
        message(client, Text.translatable("ottoextra.resources.text.imported", imported.zones().size(),
                imported.skipped()).formatted(Formatting.GREEN));
    }

    private Path exportFile(String fileName) {
        String safe = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.length() > 60) safe = safe.substring(0, 60);
        if (safe.isEmpty() || safe.startsWith(".")) safe = "liste";
        return repository.directory().resolve("export").resolve(safe.endsWith(".txt") ? safe : safe + ".txt");
    }

    private void updateEnteredResources(MinecraftClient client) {
        Set<String> current = new HashSet<>();
        BlockPos pos = client.player.getBlockPos();
        for (ResourceSnapshot resource : cache) {
            if (!sameDimension(resource, client) || !resource.zone().contains(pos)) continue;
            current.add(resource.id());
            if (!insideResources.contains(resource.id())) {
                if (config.hintsEnabled) {
                    ResourceNoticeModel model = zoneModel(resource);
                    boolean startable = model.kind() == ResourceNoticeModel.Kind.READY
                            || model.kind() == ResourceNoticeModel.Kind.HARVESTABLE;
                    resourceToast(resource, model, startable && resource.writable() && !config.autoStartOnBreak
                            ? keyHint("ottoextra.resources.notice.hint_after") : null);
                }
            }
        }
        // Leaving a zone whose timer was started during the visit: remind that it runs (toast J).
        for (String left : insideResources) {
            if (current.contains(left) || !startedInside.remove(left)) continue;
            if (config.hintsEnabled && config.leaveNotice && !pending.containsKey(left)) {
                ResourceSnapshot resource = fresh(left);
                if (resource != null && !"READY".equals(resource.status())) {
                    resourceToast(resource, ResourceNoticeModel.event(ResourceNoticeModel.Kind.STARTED,
                            resource.regenerationPercentage(), resource.secondsUntilFull()), null);
                }
            }
        }
        insideResources.retainAll(current);
        insideResources.addAll(current);
    }

    private void updatePendingConfirmationWindow(MinecraftClient client, long now) {
        BlockPos playerPos = client.player.getBlockPos();
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Pending> entry = iterator.next();
            Pending value = entry.getValue();
            if (!sameDimension(value.resource, client)) {
                iterator.remove();
                continue;
            }
            boolean inZone = value.resource.zone().contains(playerPos);
            if (inZone) {
                if (value.outsideSinceMillis > 0L) {
                    entry.setValue(value.withOutsideSinceMillis(0L));
                }
                continue;
            }
            if (value.outsideSinceMillis == 0L) {
                entry.setValue(value.withOutsideSinceMillis(now));
                if (config.hintsEnabled) {
                    pendingResourceToast(value.resource, value.forceRestart, true);
                }
            } else if (!confirmationAllowed(false, value.outsideSinceMillis, now)) {
                iterator.remove();
            }
        }
    }

    static boolean confirmationAllowed(boolean inZone, long outsideSinceMillis, long now) {
        return inZone || outsideSinceMillis > 0L && now >= outsideSinceMillis
                && now - outsideSinceMillis <= CONFIRM_OUTSIDE_GRACE_MILLIS;
    }

    List<ResourceSnapshot> cachedResources() { return cache; }
    List<ResourceSnapshot> mapResources() {
        return snapshots.stream().filter(config::showsOnMap).toList();
    }

    boolean available() {
        return config.enabled && OttoExtraGate.isOnOttonien(MinecraftClient.getInstance());
    }

    private ResourceStatistics statistics() {
        if (statistics == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return null;
            statistics = new ResourceStatistics(FabricLoader.getInstance().getConfigDir()
                    .resolve("ottoextra-resources-statistics").resolve(client.player.getUuid() + ".json"));
        }
        return statistics;
    }

    void openStatistics(Screen parent) {
        ResourceStatistics current = statistics();
        if (current != null) MinecraftClient.getInstance().setScreen(new ResourceStatisticsScreen(parent, current));
    }

    private void saveStatistics() {
        if (statistics != null) statistics.finishRun();
        if (statistics != null && !statistics.save()) {
            OttoExtraResources.LOGGER.warn("Ressourcenstatistik konnte nicht gespeichert werden: {}",
                    statistics.storageError());
        }
    }

    private void updateStatisticsDimension(MinecraftClient client) {
        String current = dimension(client);
        if (!current.equals(statisticsDimension)) {
            resetStatisticsTracking();
            if (statistics != null) statistics.finishRun();
            statisticsDimension = current;
            statisticsWorldSince = System.currentTimeMillis();
        }
    }

    private void trackStatisticsInteraction(MinecraftClient client, BlockPos pos,
                                            BlockState state, String interaction) {
        if (client.player.isSpectator()) return;
        updateStatisticsDimension(client);
        String currentDimension = dimension(client);
        for (ResourceSnapshot resource : nearestMatch(pos, r -> r.dimension().equals(currentDimension)
                && relevant(r.type(), state, interaction)
                && (interaction.equals("BLOCK_BREAK") || r.type().equals("APIARY")))) {
            {
                trackStatisticsTarget(new ResourceActionbarTracker.Target(resource.id(), resource.name(),
                        resource.type(), currentDimension, statisticsRegion(resource.territoryName())), System.currentTimeMillis());
                return;
            }
        }
        // Known resource blocks can also be tracked before a zone has been created or synchronized.
        if (!interaction.equals("BLOCK_BREAK")) return;
        for (String type : STATISTICS_TYPES) {
            if (relevant(type, state, interaction)) {
                String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
                trackStatisticsTarget(new ResourceActionbarTracker.Target("block:" + blockId,
                        state.getBlock().getName().getString(), type, currentDimension, statisticsRegion(null)), System.currentTimeMillis());
                return;
            }
        }
    }

    private void trackStatisticsTarget(ResourceActionbarTracker.Target target, long now) {
        var previous = actionbarTracker.context(target.dimension(), now);
        if (previous == null || !previous.id().equals(target.id()) || !previous.region().equals(target.region())) {
            pickupTracker.reset();
        }
        actionbarTracker.interact(target, now);
        statistics().interact(target, now);
    }

    private void resetStatisticsTracking() {
        actionbarTracker.reset();
        pickupTracker.reset();
    }

    private String statisticsRegion(String territoryName) {
        if (territoryName != null && !territoryName.isBlank()) return territoryName;
        var region = RegionMessageService.current();
        // Never carry a stale HUD region across a connection or dimension change.
        return region != null && region.updatedAtMs() >= statisticsWorldSince ? region.regionName() : "";
    }

    void onActionbar(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!available() || client.player == null || client.world == null) {
            resetStatisticsTracking();
            return;
        }
        updateStatisticsDimension(client);
        if (RegionMessageService.isRegionEnter(message)) return;
        long now = System.currentTimeMillis();
        var observation = actionbarTracker.receive(message.getString(), dimension(client), now);
        if (observation != null) pickupTracker.actionbar(statistics(), observation, now);
    }

    void onItemPickup(ItemPickupAnimationS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!available() || client.player == null || client.world == null
                || client.player.isSpectator() || packet.getCollectorEntityId() != client.player.getId()
                || packet.getStackAmount() <= 0) return;
        updateStatisticsDimension(client);
        long now = System.currentTimeMillis();
        var target = actionbarTracker.context(dimension(client), now);
        if (target == null || !(client.world.getEntityById(packet.getEntityId()) instanceof ItemEntity entity)) return;
        ItemStack stack = entity.getStack();
        if (stack.isEmpty() || trackingItemKey(target.type(), Registries.ITEM.getId(stack.getItem()).toString(),
                stack.getName().getString()) == null) return;
        pickupTracker.pickup(statistics(), target, packet.getEntityId(), now);
    }

    static boolean relevant(String type, BlockState state, String interaction) {
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        return relevant(type, id, interaction);
    }

    static boolean relevant(String type, String id, String interaction) {
        return switch (type) {
            case "IRON_DEPOSIT", "IRON_MINE" -> id.equals("minecraft:iron_ore") || id.equals("minecraft:deepslate_iron_ore");
            case "COAL_DEPOSIT", "COAL_MINE" -> id.equals("minecraft:coal_ore") || id.equals("minecraft:deepslate_coal_ore");
            case "SALT_DEPOSIT", "SALT_MINE" -> id.equals("minecraft:lapis_ore")
                    || id.equals("minecraft:deepslate_lapis_ore")
                    || id.equals("ottoextra:salt_block") || id.equals("ottonien:salt_block");
            case "APIARY" -> interaction.equals("BLOCK_INTERACT");
            case "MONASTERY_GARDEN" -> interaction.equals("BLOCK_BREAK")
                    && GARDEN_PLANTS.containsKey(id);
            case "PEAT_BOG" -> interaction.equals("BLOCK_BREAK") && (id.equals("minecraft:tuff")
                    || id.equals("minecraft:mud") || id.equals("minecraft:muddy_mangrove_roots")
                    || id.equals("ottonien:peat"));
            case "CLAY_PIT" -> interaction.equals("BLOCK_BREAK") && id.equals("minecraft:clay");
            default -> false;
        };
    }

    static boolean trackedItem(String type, String itemId) {
        return trackingItemKey(type, itemId, "") != null;
    }

    static String trackingItemKey(String type, String itemId, String itemName) {
        return switch (type) {
            case "IRON_DEPOSIT", "IRON_MINE" -> itemId.equals("minecraft:raw_iron")
                    ? itemId : null;
            case "COAL_DEPOSIT", "COAL_MINE", "PEAT_BOG" ->
                    itemId.equals("minecraft:charcoal") ? itemId : null;
            case "SALT_DEPOSIT", "SALT_MINE" -> itemId.equals("minecraft:amethyst_shard")
                    ? itemId : null;
            case "APIARY" -> itemId.equals("minecraft:honeycomb") ? itemId : null;
            case "CLAY_PIT" -> itemId.equals("minecraft:clay_ball") ? itemId : null;
            case "MONASTERY_GARDEN" -> gardenTrackingKey(itemId, itemName);
            default -> null;
        };
    }

    private static String gardenTrackingKey(String itemId, String itemName) {
        String plantKey = GARDEN_PLANTS.get(itemId);
        if (plantKey != null) return plantKey;
        if (!itemId.equals("minecraft:amethyst_shard")) return null;
        String normalized = Normalizer.normalize(itemName.toLowerCase(Locale.GERMAN),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", "");
        return switch (normalized) {
            case "steinpilzhut" -> "ottoextra:garden/steinpilzhut";
            case "hohlenpilzhut" -> "ottoextra:garden/hoehlenpilzhut";
            case "fliegenpilzhut" -> "ottoextra:garden/fliegenpilzhut";
            case "ringelblumenbluten" -> "ottoextra:garden/ringelblumenblueten";
            case "nesselblatter" -> "ottoextra:garden/nesselblaetter";
            case "weinrautenbluten" -> "ottoextra:garden/weinrautenblueten";
            case "baldrianbluten" -> "ottoextra:garden/baldrianblueten";
            default -> null;
        };
    }

    private static boolean sameDimension(ResourceSnapshot resource, MinecraftClient client) { return client.world != null && resource.dimension().equals(dimension(client)); }
    private static String dimension(MinecraftClient client) { return client.world == null ? "" : client.world.getRegistryKey().getValue().toString(); }
    private static void message(MinecraftClient client, Text text) { if (client.player != null) client.player.sendMessage(text, false); }
    /** State of a zone for its notice: ready, can be mined, recovering or ready soon. */
    static ResourceNoticeModel zoneModel(ResourceSnapshot resource) {
        boolean harvestable = !"READY".equals(resource.status())
                && canStartHarvest(resource.type(), resource.status(), resource.regenerationPercentage());
        return ResourceNoticeModel.of(resource.status(), resource.regenerationPercentage(),
                resource.secondsUntilFull(), harvestable);
    }
    private ResourceNotice.Content noticeContent(ResourceSnapshot resource, ResourceNoticeModel model, String hint) {
        String gefolge = resource.territoryName() == null ? "" : resource.territoryName();
        return new ResourceNotice.Content(resource.id(), model, resource.name(),
                ResourceMapOverlay.detailLabel(resource), gefolge, resourceIcon(resource),
                ResourceNotice.seal(repository.listOfZone(resource.id())), hint);
    }
    /** Hint with the key actually bound to "confirm", e.g. "[R] Abbau bestätigen"; null when unbound. */
    private String keyHint(String key) {
        if (confirmKey == null || confirmKey.isUnbound()) return null;
        return Text.translatable(key, confirmKey.getBoundKeyLocalizedText().getString()).getString();
    }
    private void resourceToast(ResourceSnapshot resource, ResourceNoticeModel model, String hint) {
        notice.show(noticeContent(resource, model, hint));
    }
    private void pendingResourceToast(ResourceSnapshot resource, boolean forceRestart, boolean outside) {
        pendingToastResourceId = resource.id();
        ResourceNoticeModel model = ResourceNoticeModel.event(outside ? ResourceNoticeModel.Kind.PENDING_OUTSIDE
                : ResourceNoticeModel.Kind.PENDING, resource.regenerationPercentage(), resource.secondsUntilFull());
        String hint = keyHint(outside ? "ottoextra.resources.notice.hint_outside"
                : forceRestart ? "ottoextra.resources.notice.hint_restart" : "ottoextra.resources.notice.hint_confirm");
        notice.showSticky(noticeContent(resource, model, hint));
    }
    private void clearStalePendingToast() {
        if (pendingToastResourceId != null && !pending.containsKey(pendingToastResourceId)) {
            clearPendingToast(pendingToastResourceId);
        }
    }
    private void clearPendingToast(String resourceId) {
        if (resourceId.equals(pendingToastResourceId)) {
            pendingToastResourceId = null;
            notice.clear(resourceId);
        }
    }
    private Identifier resourceIcon(ResourceSnapshot resource) {
        return ResourceIcons.iconFor(resource);
    }
    static Identifier bundledResourceIcon(String type) {
        // Markers show the default symbol (marker_violett) of symbol set 2.
        Identifier icon = ResourcePolicy.isMarker(type)
                ? Identifier.of("ottoextra_resources", MarkerSymbols.texturePath(MarkerSymbols.DEFAULT))
                : Identifier.of("ottoextra_resources", "textures/gui/resources/" + resourceIconName(type));
        return MinecraftClient.getInstance().getResourceManager().getResource(icon).isPresent()
                ? icon : null;
    }
    public static String resourceIconName(String type) {
        return switch (type) {
            case "IRON_DEPOSIT", "IRON_MINE" -> "eisenerz.png";
            case "SALT_DEPOSIT", "SALT_MINE" -> "salzkristall.png";
            case "COAL_DEPOSIT", "COAL_MINE" -> "steinkohle.png";
            case "CLAY_PIT" -> "tonklumpen.png";
            case "APIARY" -> "honig.png";
            case "MONASTERY_GARDEN" -> "rosenkranz.png";
            case "PEAT_BOG" -> "torf.png";
            default -> type.toLowerCase(Locale.ROOT) + ".png";
        };
    }
    static String resourceTypeLabelKey(String type) {
        if (type.endsWith("_DEPOSIT")) return "ottoextra.resources.type.deposit";
        if (type.endsWith("_MINE")) return "ottoextra.resources.type.mine";
        return switch (type) {
            case "CLAY_PIT" -> "ottoextra.resources.type.pit";
            case "MONASTERY_GARDEN" -> "ottoextra.resources.type.monastery_garden";
            case "PEAT_BOG" -> "ottoextra.resources.type.peat_bog";
            case "APIARY" -> "ottoextra.resources.type.apiary";
            default -> "ottoextra.resources.type.resource";
        };
    }
    static String resourceMaterialKey(String type) {
        if (type.startsWith("IRON_")) return "ottoextra.resources.material.iron";
        if (type.startsWith("COAL_")) return "ottoextra.resources.material.coal";
        if (type.startsWith("SALT_")) return "ottoextra.resources.material.salt";
        return switch (type) {
            case "CLAY_PIT" -> "ottoextra.resources.material.clay";
            case "MONASTERY_GARDEN" -> "ottoextra.resources.material.herbs";
            case "PEAT_BOG" -> "ottoextra.resources.material.peat";
            case "APIARY" -> "ottoextra.resources.material.honey";
            default -> "ottoextra.resources.material.resource";
        };
    }
    static boolean isMine(String type) {
        return type != null && type.endsWith("_MINE");
    }
    static boolean canStartHarvest(String type, String status, int percentage) {
        return "READY".equals(status) || isMine(type)
                && "REGENERATING".equals(status) && percentage >= 70;
    }
    static boolean canForceRestart(String type, String status) {
        return "REGENERATING".equals(status) && type != null
                && (type.endsWith("_MINE") || type.endsWith("_DEPOSIT"));
    }
    static int regenerationCountdown(ResourceSnapshot resource) {
        return isMine(resource.type()) && resource.regenerationPercentage() < 70
                && resource.secondsUntilNextStage() >= 0
                ? resource.secondsUntilNextStage() : resource.secondsUntilFull();
    }
    static String countdownTargetKey(String type, int percentage) {
        return isMine(type) && percentage < 70
                ? "ottoextra.resources.stage.partial" : "ottoextra.resources.stage.full";
    }
    static String enteredMessageKey(String type, int percentage) {
        if (!isMine(type)) return "ottoextra.resources.entered.regenerating";
        return percentage >= 70 ? "ottoextra.resources.entered.mine_partial"
                : "ottoextra.resources.entered.mine_regenerating";
    }
    static String regeneratingStatusKey(String type, int percentage) {
        if (!isMine(type)) return "ottoextra.resources.regenerating";
        return percentage >= 70 ? "ottoextra.resources.regenerating.mine_partial"
                : "ottoextra.resources.regenerating.mine";
    }
    static String statusMessageKey(String type, int percentage) {
        if (!isMine(type)) return "ottoextra.resources.status";
        return percentage >= 70 ? "ottoextra.resources.status.mine_partial"
                : "ottoextra.resources.status.mine_regenerating";
    }
    static String resourceToastLabel(ResourceSnapshot resource) {
        String type = resource.type();
        Text material = resource.materialLabel() == null || resource.materialLabel().isBlank()
                ? Text.translatable(resourceMaterialKey(type)) : Text.literal(resource.materialLabel());
        Text label = resource.typeLabel() == null || resource.typeLabel().isBlank()
                ? Text.translatable(resourceTypeLabelKey(type)) : Text.literal(resource.typeLabel());
        return Text.translatable("ottoextra.resources.type.display",
                material, label).getString();
    }
    static String remaining(int seconds) {
        if (seconds < 0) return "–";
        if (seconds == 0) return "0 min";
        int minutes = Math.max(1, (seconds + 59) / 60);
        int hours = minutes / 60;
        int rest = minutes % 60;
        return hours > 0 ? hours + " h " + rest + " min" : minutes + " min";
    }
    private record Pending(ResourceSnapshot resource, BlockPos interactionPos,
                           boolean forceRestart, long outsideSinceMillis) {
        private Pending withOutsideSinceMillis(long value) {
            return new Pending(resource, interactionPos, forceRestart, value);
        }
    }
}
