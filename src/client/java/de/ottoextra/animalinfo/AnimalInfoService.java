package de.ottoextra.animalinfo;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.horses.HorseRegistry;
import de.ottoextra.logging.DebugLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.Util;
import net.minecraft.util.hit.EntityHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.function.BooleanSupplier;

public final class AnimalInfoService {

    /** Begrenzt schnelle Zielwechsel, ohne eine bereits laufende Antwort zu ueberholen. */
    private static final long GLOBAL_QUERY_COOLDOWN_MS = 350L;
    private static final long BLOCK_INTERACTION_GUARD_MS = 750L;
    private static final long ANIMAL_INTERACTION_REFRESH_MS = 500L;
    private static final long FAILED_TARGET_BACKOFF_MS = 15_000L;
    private static final long TRAILING_DIVIDER_GRACE_MS = 1_000L;
    private static final int MAX_CACHE_ENTRIES = 128;

    private static final AnimalInfoResponseCollector RESPONSE =
            new AnimalInfoResponseCollector();
    private static final Map<UUID, CachedInfo> CACHE = new HashMap<>();
    private static final Map<UUID, Long> RETRY_AFTER = new HashMap<>();
    private static final Map<UUID, Long> REFRESH_AFTER = new HashMap<>();

    private static OttoExtraConfig.AnimalInfo config;
    private static BooleanSupplier onOttonien = () -> false;

    private static UUID focusedId;
    private static long focusedSince;
    private static boolean queriedThisFocus;
    private static long lastQueryAt;
    private static long blockInteractionGuardUntil;
    private static long trailingDividerUntil;
    private static PendingRequest pending;
    private static ClientPlayNetworkHandler sessionNetworkHandler;
    private static ClientWorld sessionWorld;
    private static UUID sessionPlayerId;
    private static AnimalInfoRequestPlan.CleanupPhase cleanupPhase =
            AnimalInfoRequestPlan.CleanupPhase.NONE;
    private static AnimalInfoRequestPlan.Attempt nextAttempt =
            AnimalInfoRequestPlan.Attempt.PRIMARY;

    private AnimalInfoService() {
    }

    public static void init(OttoExtraConfig.AnimalInfo cfg,
                            BooleanSupplier serverGate) {
        config = cfg;
        onOttonien = serverGate == null ? () -> false : serverGate;
        reset();
    }

    public static void tick(MinecraftClient client) {
        long now = now();
        if (AnimalInfoRequestPlan.mountedEquineBlocksQuery(
                client != null && client.player != null
                        && client.player.getVehicle() instanceof AbstractHorseEntity)) {
            interruptMountedInspection();
            return;
        }
        runScheduledSneakCleanup(client);
        pruneExpired(now);
        expirePending(now);

        if (!hasInspectionSession(client)) {
            clearQueryState();
            return;
        }
        refreshSession(client);
        if (!canInspect(client)) {
            clearFocus();
            AnimalInfoOverlay.hide();
            return;
        }

        AnimalEntity target = targetedAnimal(client);
        if (target == null) {
            clearFocus();
            AnimalInfoOverlay.hide();
            return;
        }
        if (!AnimalInfoOverlay.mayInspect(target, config)) {
            clearFocus();
            AnimalInfoOverlay.clear();
            return;
        }

        UUID targetId = target.getUuid();
        if (!targetId.equals(focusedId)) {
            boolean switchingAnimal = focusedId != null;
            focusedId = targetId;
            focusedSince = now;
            queriedThisFocus = false;
            nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
            if (switchingAnimal) {
                AnimalInfoOverlay.switchTarget();
            } else {
                AnimalInfoOverlay.hide();
            }
            DebugLog.debug(
                    "[animalinfo] Ziel {} (Typ {}, Name '{}', Pferd {}, angeleint {}).",
                    targetId, target.getType().toString(), target.getName().getString(),
                    target instanceof AbstractHorseEntity, target.isLeashed());
        }

        if (AnimalInfoRequestPlan.heldLeadBlocksQuery(
                client.player.getMainHandStack().isOf(Items.LEAD))) {
            showDatabaseHorseInfo(target);
            return;
        }

        CachedInfo cached = CACHE.get(targetId);
        Long refreshAfter = REFRESH_AFTER.get(targetId);
        boolean refreshDue = refreshAfter != null && refreshAfter <= now;
        if (cached != null) {
            if (cached.expiresAt() >= now && !refreshDue) {
                if (AnimalInfoOverlay.enabledFor(target, cached.snapshot(), config)) {
                    AnimalInfoOverlay.show(cached.snapshot(), target);
                } else {
                    AnimalInfoOverlay.clear();
                }
                queriedThisFocus = refreshAfter == null;
                return;
            }
            CACHE.remove(targetId);
            if (refreshDue) {
                queriedThisFocus = false;
            }
        }

        Long retryAfter = RETRY_AFTER.get(targetId);
        if (retryAfter != null) {
            if (target instanceof AbstractHorseEntity) {
                showDatabaseHorseInfo(target);
            }
            return;
        }

        PlayerInput input = client.player.input.playerInput;
        if (AnimalInfoRequestPlan.movementBlocksQuery(
                input, client.player.isSneaking(), client.player.isSprinting())
                || AnimalInfoRequestPlan.ridingBlocksQuery(client.player.hasVehicle())) {
            focusedSince = now;
            return;
        }
        if (heldItemBlocksQuery(client, target)) {
            // The synthetic click would feed, leash, shear, milk, name or saddle the animal.
            focusedSince = now;
            return;
        }

        if (queriedThisFocus || pending != null
                || now - focusedSince < config.hoverDelayMs
                || now - lastQueryAt < GLOBAL_QUERY_COOLDOWN_MS
                || now < blockInteractionGuardUntil
                || refreshAfter != null && now < refreshAfter
                || client.options.useKey.isPressed()
                || client.options.attackKey.isPressed()) {
            return;
        }

        boolean horseTarget = target instanceof AbstractHorseEntity;
        request(client, target, now,
                AnimalInfoRequestPlan.attemptForTarget(nextAttempt, horseTarget));
    }

    /**
     * Avoids the synthetic main-hand interaction entirely. A real use-key
     * interaction still follows Minecraft's normal path and can leash the animal.
     */
    private static void showDatabaseHorseInfo(AnimalEntity target) {
        if (!(target instanceof AbstractHorseEntity)) {
            AnimalInfoOverlay.clear();
            return;
        }
        HorseRegistry.requestDatabaseRecords();
        AnimalInfoSnapshot stored = HorseRegistry.databaseSnapshot(target.getUuid())
                .filter(AnimalInfoSnapshot::complete)
                .or(() -> HorseRegistry.observation(target.getUuid())
                        .map(AnimalInfoObservation::snapshot)
                        .filter(AnimalInfoSnapshot::complete))
                .orElse(null);
        if (stored != null && AnimalInfoOverlay.enabledFor(target, stored, config)) {
            AnimalInfoOverlay.show(stored, target);
        } else {
            AnimalInfoOverlay.clear();
        }
    }

    /**
     * Verarbeitet ausschließlich die erwartete Antwort einer von OttoExtra
     * gestarteten Tierinfo-Abfrage. Ohne laufende Anfrage bleiben manuelle
     * Tierinfos vollständig unangetastet.
     */
    public static boolean handleGameMessage(Text message) {
        if (message == null) {
            return false;
        }
        long now = now();
        PendingRequest request = pending;
        if (request == null) {
            if (now <= trailingDividerUntil && isOnlyDivider(message.getString())) {
                trailingDividerUntil = 0L;
                return config != null && config.hideAutomaticChat;
            }
            if (now > trailingDividerUntil) {
                trailingDividerUntil = 0L;
            }
            return false;
        }

        if (now - request.sentAt() > responseTimeoutMs()) {
            expirePending(now);
            return false;
        }

        String rawMessage = message.getString();
        if (AnimalInfoRequestPlan.isLeashedHorseRejection(rawMessage)) {
            DebugLog.debug(
                    "[animalinfo] Aufsteige-Ablehnung fuer {} ueber Versuch {} erkannt.",
                    request.targetId(), request.attempt());
            handleLeashedHorseRejection(request, now);
            return config != null && config.hideAutomaticChat;
        }

        AnimalInfoResponseCollector.Result result = RESPONSE.acceptMessage(rawMessage);
        AnimalInfoSnapshot completed = result.completed();

        if (completed != null) {
            if (result.expectsTrailingDivider()) {
                trailingDividerUntil = now + TRAILING_DIVIDER_GRACE_MS;
            }
            cache(request.targetId(), completed, now);
            AnimalLiveStore.current().observe(request.targetId(), completed, System.currentTimeMillis());
            publishObservation(request.targetId(), completed, Instant.now());
            pending = null;
            DebugLog.debug(
                    "[animalinfo] Antwort fuer {} vollstaendig (Tier '{}').",
                    request.targetId(), completed.animalName());
            if (request.targetId().equals(focusedId)) {
                nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
                AnimalEntity target = targetedAnimal(MinecraftClient.getInstance());
                if (target != null && request.targetId().equals(target.getUuid())) {
                    if (AnimalInfoOverlay.enabledFor(target, completed, config)) {
                        AnimalInfoOverlay.show(completed, target);
                    } else {
                        AnimalInfoOverlay.clear();
                    }
                }
            }
        }

        return result.consumed() && config != null && config.hideAutomaticChat;
    }

    static void publishObservation(UUID targetId, AnimalInfoSnapshot snapshot,
                                   Instant observedAt) {
        try {
            AnimalInfoEvents.OBSERVED.invoker().onObserved(
                    new AnimalInfoObservation(targetId, snapshot, observedAt));
        } catch (Throwable error) {
            OttoExtra.LOGGER.warn(
                    "[animalinfo] Beobachtungs-Listener fehlgeschlagen: {}",
                    error.toString());
        }
    }

    public static void reset() {
        sessionNetworkHandler = null;
        sessionWorld = null;
        sessionPlayerId = null;
        clearQueryState();
    }

    private static void clearQueryState() {
        focusedId = null;
        focusedSince = 0L;
        queriedThisFocus = false;
        lastQueryAt = 0L;
        blockInteractionGuardUntil = 0L;
        trailingDividerUntil = 0L;
        pending = null;
        cleanupPhase = AnimalInfoRequestPlan.CleanupPhase.NONE;
        nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
        CACHE.clear();
        RETRY_AFTER.clear();
        REFRESH_AFTER.clear();
        RESPONSE.reset();
        AnimalInfoOverlay.clear();
    }

    private static void request(MinecraftClient client, AnimalEntity target, long now,
                                AnimalInfoRequestPlan.Attempt attempt) {
        if (client.getNetworkHandler() == null || client.player == null) {
            return;
        }
        if (AnimalInfoRequestPlan.heldLeadBlocksQuery(
                client.player.getMainHandStack().isOf(Items.LEAD))) {
            return;
        }
        if (heldItemBlocksQuery(client, target)) {
            return;
        }
        UUID targetId = target.getUuid();
        int selectedSlot = client.player.getInventory().getSelectedSlot();
        PlayerInput neutralInput = AnimalInfoRequestPlan.neutralInput(
                client.player.input.playerInput);
        PlayerInput assertedSneakInput = AnimalInfoRequestPlan.assertedSneakInput(
                client.player.input.playerInput);
        boolean interactionSent = false;

        try {
            DebugLog.debug(
                    "[animalinfo] Sende {} fuer {} (Typ {}, Name '{}', Slot {}, Item {}).",
                    attempt, targetId, target.getType().toString(),
                    target.getName().getString(), selectedSlot,
                    client.player.getMainHandStack().getItem().toString());
            for (AnimalInfoRequestPlan.Step step : AnimalInfoRequestPlan.steps(attempt)) {
                switch (step) {
                    case SYNC_SELECTED_SLOT -> {
                        // Der Rohpaket-Pfad umgeht den privaten Vanilla-Slotabgleich.
                        // Ohne dieses Paket kann der Server noch das vorige Item
                        // (insbesondere eine Leine) in der Haupthand annehmen.
                        client.getNetworkHandler().sendPacket(
                                new UpdateSelectedSlotC2SPacket(selectedSlot));
                    }
                    case NEUTRAL_INPUT -> client.getNetworkHandler().sendPacket(
                            new PlayerInputC2SPacket(neutralInput));
                    case STOP_SPRINTING -> client.getNetworkHandler().sendPacket(
                            new ClientCommandC2SPacket(client.player,
                                    ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    case ASSERT_SNEAK_INPUT -> client.getNetworkHandler().sendPacket(
                            new PlayerInputC2SPacket(assertedSneakInput));
                    case SNEAK_INTERACTION -> {
                        pending = new PendingRequest(targetId, now, attempt);
                        RESPONSE.start();
                        client.getNetworkHandler().sendPacket(
                                PlayerInteractEntityC2SPacket.interact(
                                        target, true, Hand.MAIN_HAND));
                        interactionSent = true;
                    }
                }
            }
        } catch (RuntimeException error) {
            if (interactionSent) {
                bestEffortMovementCleanup(client, neutralInput);
            } else {
                cancelPending();
                putBounded(RETRY_AFTER, targetId, now + FAILED_TARGET_BACKOFF_MS);
            }
            OttoExtra.LOGGER.warn(
                    "[animalinfo] Sichere Tierabfrage unvollständig: {}",
                    error.toString());
        }

        if (interactionSent) {
            queriedThisFocus = true;
            lastQueryAt = now;
            nextAttempt = attempt;
            REFRESH_AFTER.remove(targetId);
            cleanupPhase = AnimalInfoRequestPlan.CleanupPhase.ASSERT_SNEAK;
        }
    }

    /**
     * The automatic click is a real interaction on the server. With one of these items in either hand it
     * could do something to the animal, so no query is sent until the player holds something else.
     */
    private static boolean heldItemBlocksQuery(MinecraftClient client, AnimalEntity target) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            if (stack.isEmpty()) {
                continue;
            }
            if (AnimalInfoRequestPlan.heldItemBlocksQuery(
                    target.isBreedingItem(stack),
                    stack.isOf(Items.LEAD),
                    stack.isOf(Items.SHEARS),
                    stack.isOf(Items.BUCKET) || stack.isOf(Items.WATER_BUCKET)
                            || stack.isOf(Items.MILK_BUCKET),
                    stack.isOf(Items.NAME_TAG),
                    stack.isOf(Items.SADDLE) || stack.isOf(Items.BOWL)
                            || stack.isOf(Items.GLASS_BOTTLE))) {
                return true;
            }
        }
        return false;
    }

    private static void bestEffortMovementCleanup(MinecraftClient client,
                                                   PlayerInput neutralInput) {
        try {
            client.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(neutralInput));
        } catch (RuntimeException ignored) {
            // Verbindung kann während der Anfrage geschlossen worden sein.
        }
        try {
            client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                    client.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        } catch (RuntimeException ignored) {
            // Verbindung kann während der Anfrage geschlossen worden sein.
        }
    }

    /**
     * Erzeugt dieselbe Input-Flanke wie ein kurzer echter Sneak-Tastendruck.
     * Manche Server-Protokollbrücken übernehmen den Sneak-Wert des
     * Interaktionspakets erst nach einem folgenden true/false-Inputwechsel.
     */
    private static void runScheduledSneakCleanup(MinecraftClient client) {
        AnimalInfoRequestPlan.CleanupPhase phase = cleanupPhase;
        if (phase == AnimalInfoRequestPlan.CleanupPhase.NONE) {
            return;
        }
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            cleanupPhase = AnimalInfoRequestPlan.CleanupPhase.NONE;
            return;
        }

        PlayerInput actualInput = client.player.input.playerInput;
        if (actualInput == null || actualInput.sneak()) {
            // Echter Sneak-Input besitzt Vorrang und erzeugt seine eigene Freigabe.
            cleanupPhase = AnimalInfoRequestPlan.CleanupPhase.NONE;
            return;
        }
        if (actualInput.sprint() || client.player.isSprinting()) {
            return;
        }

        PlayerInput cleanupInput = phase == AnimalInfoRequestPlan.CleanupPhase.ASSERT_SNEAK
                ? AnimalInfoRequestPlan.assertedSneakInput(actualInput)
                : AnimalInfoRequestPlan.neutralInput(actualInput);
        try {
            client.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(cleanupInput));
            client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                    client.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            cleanupPhase = phase.next();
        } catch (RuntimeException error) {
            cleanupPhase = AnimalInfoRequestPlan.CleanupPhase.NONE;
            OttoExtra.LOGGER.warn(
                    "[animalinfo] Sneak-Bereinigung fehlgeschlagen: {}",
                    error.toString());
        }
    }

    /** Verhindert automatische Tierklicks unmittelbar nach einer Blockinteraktion. */
    static void onBlockInteraction() {
        long until = now() + BLOCK_INTERACTION_GUARD_MS;
        blockInteractionGuardUntil = Math.max(blockInteractionGuardUntil, until);
    }

    /** Markiert angezeigte Tierdaten nach Füttern, Melken oder ähnlichen Aktionen als veraltet. */
    static void onAnimalInteraction(AnimalEntity animal) {
        if (animal == null) {
            return;
        }
        long now = now();
        UUID targetId = animal.getUuid();
        putBounded(REFRESH_AFTER, targetId, now + ANIMAL_INTERACTION_REFRESH_MS);
        RETRY_AFTER.remove(targetId);
        if (targetId.equals(focusedId)) {
            queriedThisFocus = false;
            nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
        }
    }

    private static boolean hasInspectionSession(MinecraftClient client) {
        return config != null && config.enabled && onOttonien.getAsBoolean()
                && client != null && client.player != null && client.world != null
                && client.getNetworkHandler() != null;
    }

    private static void refreshSession(MinecraftClient client) {
        UUID playerId = client.player.getUuid();
        if (sessionNetworkHandler == client.getNetworkHandler()
                && sessionWorld == client.world
                && playerId.equals(sessionPlayerId)) {
            return;
        }
        clearQueryState();
        sessionNetworkHandler = client.getNetworkHandler();
        sessionWorld = client.world;
        sessionPlayerId = playerId;
    }

    private static boolean canInspect(MinecraftClient client) {
        return client.currentScreen == null
                && !client.player.isSpectator() && !client.player.isUsingItem();
    }

    private static AnimalEntity targetedAnimal(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return null;
        }
        Entity entity = hit.getEntity();
        if (AnimalInfoRequestPlan.ownMountHidden(config != null && config.hideOwnMount,
                client.player != null && entity != null && client.player.isConnectedThroughVehicle(entity))) {
            return null;
        }
        return entity instanceof AnimalEntity animal && animal.isAlive() ? animal : null;
    }

    private static void clearFocus() {
        focusedId = null;
        focusedSince = 0L;
        queriedThisFocus = false;
        nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
    }

    /** Unterbricht auch die noch ausstehende Sneak-Flanke beim Aufsteigen. */
    private static void interruptMountedInspection() {
        cleanupPhase = AnimalInfoRequestPlan.CleanupPhase.NONE;
        cancelPending();
        clearFocus();
        AnimalInfoOverlay.hide();
    }

    private static void expirePending(long now) {
        PendingRequest request = pending;
        if (request != null && now - request.sentAt() > responseTimeoutMs()) {
            cancelPending();
            if (request.targetId().equals(focusedId)
                    && request.attempt() == AnimalInfoRequestPlan.Attempt.PRIMARY) {
                queriedThisFocus = false;
                focusedSince = now;
                nextAttempt = request.attempt().afterTimeout();
                DebugLog.debug(
                        "[animalinfo] Primaerabfrage ohne Antwort; Sneak-Fallback folgt.");
            } else {
                putBounded(RETRY_AFTER, request.targetId(),
                        now + FAILED_TARGET_BACKOFF_MS);
                nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
                DebugLog.debug(
                        "[animalinfo] {} fuer {} ohne Antwort; Ziel-Backoff und Datenbank-Fallback.",
                        request.attempt(), request.targetId());
                showDatabaseHorseFallback(request.targetId());
            }
        }
    }

    /**
     * Eine unmittelbare Aufsteige-Ablehnung ist bereits die Antwort auf den
     * synthetischen Klick. Der Primärpfad wechselt sofort auf den echten
     * Sneak-Input; scheitert auch dieser, blockiert nur dieses Pferd und eine
     * vorhandene Datenbank-/Beobachtungskopie hält den Toast nutzbar.
     */
    private static void handleLeashedHorseRejection(PendingRequest request, long now) {
        cancelPending();
        if (request.targetId().equals(focusedId)
                && request.attempt() == AnimalInfoRequestPlan.Attempt.PRIMARY) {
            queriedThisFocus = false;
            focusedSince = now;
            nextAttempt = AnimalInfoRequestPlan.Attempt.FALLBACK;
        } else {
            putBounded(RETRY_AFTER, request.targetId(),
                    now + FAILED_TARGET_BACKOFF_MS);
            nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
        }
        showDatabaseHorseFallback(request.targetId());
    }

    private static void showDatabaseHorseFallback(UUID targetId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        AnimalEntity target = targetedAnimal(client);
        if (target instanceof AbstractHorseEntity
                && targetId.equals(target.getUuid())) {
            showDatabaseHorseInfo(target);
        }
    }

    private static void cancelPending() {
        pending = null;
        trailingDividerUntil = 0L;
        RESPONSE.reset();
    }

    private static void pruneExpired(long now) {
        CACHE.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        RETRY_AFTER.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) {
                return false;
            }
            if (entry.getKey().equals(focusedId)) {
                queriedThisFocus = false;
                focusedSince = now;
                nextAttempt = AnimalInfoRequestPlan.Attempt.PRIMARY;
            }
            return true;
        });
        REFRESH_AFTER.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) {
                return false;
            }
            UUID targetId = entry.getKey();
            CACHE.remove(targetId);
            if (targetId.equals(focusedId)) {
                queriedThisFocus = false;
            }
            return true;
        });
    }

    private static void putBounded(Map<UUID, Long> map, UUID targetId, long value) {
        if (map.size() >= MAX_CACHE_ENTRIES && !map.containsKey(targetId)) {
            map.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .ifPresent(map::remove);
        }
        map.put(targetId, value);
    }

    private static long now() {
        return Util.getMeasuringTimeMs();
    }

    private static long responseTimeoutMs() {
        return config == null ? 3_000L : Math.max(1_000L, config.responseTimeoutMs);
    }

    private static boolean isOnlyDivider(String message) {
        boolean found = false;
        for (String line : message.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!AnimalInfoResponseCollector.isDivider(trimmed)) {
                return false;
            }
            found = true;
        }
        return found;
    }

    private static void cache(UUID targetId, AnimalInfoSnapshot snapshot, long now) {
        if (CACHE.size() >= MAX_CACHE_ENTRIES && !CACHE.containsKey(targetId)) {
            UUID oldest = CACHE.entrySet().stream()
                    .min(Map.Entry.comparingByValue(
                            java.util.Comparator.comparingLong(CachedInfo::expiresAt)))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest != null) {
                CACHE.remove(oldest);
            }
        }
        long duration = config == null ? 5_000L : Math.max(1_000L, config.cacheDurationMs);
        RETRY_AFTER.remove(targetId);
        CACHE.put(targetId, new CachedInfo(snapshot, now + duration));
    }

    private record PendingRequest(UUID targetId, long sentAt,
                                  AnimalInfoRequestPlan.Attempt attempt) {
    }

    private record CachedInfo(AnimalInfoSnapshot snapshot, long expiresAt) {
    }
}
