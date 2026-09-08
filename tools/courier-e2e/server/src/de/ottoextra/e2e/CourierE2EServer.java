package de.ottoextra.e2e;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Test-only loopback controller. Courier itself is loaded from the unchanged supplied JAR. */
public final class CourierE2EServer extends JavaPlugin implements Listener {
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final Map<UUID, PermissionAttachment> permissions = new HashMap<>();
    private final Set<UUID> initializedPlayers = new HashSet<>();
    private final Map<String, Double> balances = new HashMap<>();
    private final List<Map<String, Object>> commandEvents = new ArrayList<>();
    private final List<Map<String, Object>> chatEvents = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> interactionEvents = new ArrayList<>();
    private final Path reports = Path.of("reports");
    private final String runId = Instant.now().toString().replaceAll("[:.]", "-")
            + "-" + UUID.randomUUID().toString().substring(0, 8);
    private HttpServer http;
    private long sequence;
    private boolean platformReady;

    @Override public void onEnable() {
        try {
            Files.createDirectories(reports);
            // Load the real JDA interface before Courier verifies its Announcement class.
            // No Discord connection is constructed; external publication stays disabled.
            Class.forName("net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel", true, getClassLoader());
            configureCourierMaterial();
            registerEconomy();
            getServer().getPluginManager().registerEvents(this, this);
            http = HttpServer.create(new InetSocketAddress("127.0.0.1", 25580), 0);
            http.createContext("/", this::handleHttp);
            http.setExecutor(Executors.newCachedThreadPool(task -> {
                Thread thread = new Thread(task, "courier-e2e-http");
                thread.setDaemon(true);
                return thread;
            }));
            http.start();
            getLogger().info("Courier E2E controller bound to http://127.0.0.1:25580");
        } catch (Exception error) {
            throw new IllegalStateException("Cannot initialize isolated Courier E2E harness", error);
        }
    }

    @Override public void onDisable() {
        if (http != null) http.stop(0);
        Bukkit.getServicesManager().unregisterAll(this);
    }

    private static ItemStack supply(int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        var meta = item.getItemMeta();
        meta.setDisplayName("Pergament und Feder");
        item.setItemMeta(meta);
        return item;
    }

    private void configureCourierMaterial() throws Exception {
        var file = Path.of("plugins", "Courier", "config.yml").toFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("letter-item.MythicItem", null);
        config.set("letter-item.ItemStack", supply(1));
        config.set("discord-announcements", false);
        config.set("announce-cooldown-in-seconds", 3600);
        config.set("book-reading.shuffle-word-percentage-per-permission.default", 0);
        config.set("book-reading.replace-letter-percentage-permission", null);
        config.set("book-reading.replace-letter-percentage-per-permission.default", 0);
        config.save(file);
    }

    private void registerEconomy() {
        Economy economy = (Economy) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{Economy.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("toString")) return "CourierE2E deterministic economy";
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    String account = args != null && args.length > 0
                            ? args[0] instanceof OfflinePlayer player ? player.getUniqueId().toString()
                            : String.valueOf(args[0]) : "";
                    double balance = balances.getOrDefault(account, 1000.0);
                    double amount = args != null && args.length > 0 && args[args.length - 1] instanceof Number number
                            ? number.doubleValue() : 0.0;
                    return switch (name) {
                        case "isEnabled", "hasAccount", "createPlayerAccount" -> true;
                        case "hasBankSupport" -> false;
                        case "getName" -> "CourierE2E deterministic economy";
                        case "fractionalDigits" -> 2;
                        case "format" -> String.format(java.util.Locale.ROOT, "%.2f test coins", amount);
                        case "currencyNamePlural", "currencyNameSingular" -> "test coins";
                        case "getBalance" -> balance;
                        case "has" -> balance >= amount;
                        case "withdrawPlayer" -> {
                            boolean allowed = balance >= amount;
                            double next = allowed ? balance - amount : balance;
                            balances.put(account, next);
                            yield new EconomyResponse(allowed ? amount : 0, next,
                                    allowed ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                                    allowed ? null : "Insufficient test balance");
                        }
                        case "depositPlayer" -> {
                            balances.put(account, balance + amount);
                            yield new EconomyResponse(amount, balance + amount, EconomyResponse.ResponseType.SUCCESS, null);
                        }
                        case "getBanks" -> List.of();
                        default -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                "Bank operation outside E2E scope");
                    };
                });
        Bukkit.getServicesManager().register(Economy.class, economy, this, ServicePriority.Highest);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PermissionAttachment old = permissions.remove(player.getUniqueId());
        if (initializedPlayers.add(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) reset(player, Map.of());
            }, 5L);
        } else {
            setPermissions(player, Map.of(), true);
            if (old != null) old.getPermissions().forEach(permissions.get(player.getUniqueId())::setPermission);
            player.recalculatePermissions();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        var entry = new LinkedHashMap<String, Object>();
        entry.put("runId", runId);
        entry.put("sequence", ++sequence);
        entry.put("at", Instant.now().toString());
        entry.put("tick", Bukkit.getCurrentTick());
        entry.put("player", event.getPlayer().getName());
        entry.put("command", event.getMessage());
        entry.put("cancelled", event.isCancelled());
        commandEvents.add(entry);
        appendReport("commands.jsonl", entry);
        Bukkit.getScheduler().runTaskLater(this, () -> archive("after-command", event.getPlayer()), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        chatEvents.add(Map.of("player", event.getPlayer().getName(), "message", event.getMessage(),
                "cancelled", event.isCancelled(), "at", Instant.now().toString()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteraction(PlayerInteractEntityEvent event) {
        var entry = new LinkedHashMap<String, Object>();
        entry.put("runId", runId);
        entry.put("sequence", ++sequence);
        entry.put("at", Instant.now().toString());
        entry.put("player", event.getPlayer().getName());
        entry.put("entity", event.getRightClicked().getUniqueId().toString());
        entry.put("type", event.getRightClicked().getType().name());
        entry.put("hand", event.getHand().name());
        entry.put("cancelled", event.isCancelled());
        interactionEvents.add(entry);
        appendReport("interactions.jsonl", entry);
        Bukkit.getScheduler().runTaskLater(this, () -> archive("after-interaction", event.getPlayer()), 2L);
    }

    private void handleHttp(HttpExchange exchange) {
        try {
            Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
            byte[] bytes = exchange.getRequestBody().readNBytes(1_048_577);
            if (bytes.length > 1_048_576) throw new IllegalArgumentException("Request too large");
            String body = new String(bytes, StandardCharsets.UTF_8);
            String path = exchange.getRequestURI().getPath();
            boolean read = path.equals("/health") || path.equals("/state") || path.equals("/outgoing")
                    || path.equals("/announcements_count");
            if (!read && !exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, Map.of("error", "POST required"));
                return;
            }
            Object result = Bukkit.getScheduler().callSyncMethod(this,
                    () -> route(path, query, body)).get(15, TimeUnit.SECONDS);
            send(exchange, 200, result);
        } catch (Exception error) {
            getLogger().warning(error.toString());
            try { send(exchange, 400, Map.of("error", error.toString())); } catch (Exception ignored) { }
        } finally {
            exchange.close();
        }
    }

    private Object route(String path, Map<String, String> query, String body) throws Exception {
        if (path.equals("/health")) {
            Plugin courier = Bukkit.getPluginManager().getPlugin("Courier");
            return Map.of("ok", courier != null && courier.isEnabled(), "paper", Bukkit.getVersion(),
                    "courier", courier == null ? "missing" : courier.getDescription().getVersion(),
                    "players", Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                    "economy", "deterministic test provider", "http", "127.0.0.1:25580");
        }
        if (path.equals("/announcements_count")) {
            var persisted = persistedAnnouncements();
            return Map.of("count", announcements().size(), "persistedCount", persisted.size(),
                    "announcements", persisted);
        }
        if (path.equals("/stop")) {
            Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 5L);
            return Map.of("ok", true);
        }
        Player player = findPlayer(query.get("player"));
        return switch (path) {
            case "/state" -> state(player);
            case "/outgoing" -> {
                var letters = outgoing(player);
                yield Map.of("player", player.getName(), "letters", letters, "count", letters.size());
            }
            case "/reset" -> { reset(player, query); yield state(player); }
            case "/slot" -> {
                player.getInventory().setHeldItemSlot(bounded(query, "slot", 0, 0, 8));
                player.updateInventory();
                archive("slot", player);
                yield state(player);
            }
            case "/supply" -> {
                int slot = bounded(query, "slot", player.getInventory().getHeldItemSlot(), 0, 40);
                ItemStack item = supply(bounded(query, "amount", 1, 1, 64));
                var meta = item.getItemMeta();
                switch (query.getOrDefault("variant", "")) {
                    case "" -> { }
                    case "model" -> meta.setCustomModelData(123);
                    case "lore" -> meta.setLore(List.of("Anderes Schreibmaterial"));
                    case "name" -> meta.setDisplayName("Anderes Pergament");
                    default -> throw new IllegalArgumentException("Unknown supply variant");
                }
                item.setItemMeta(meta);
                player.getInventory().setItem(slot, item);
                player.updateInventory();
                yield state(player);
            }
            case "/book" -> { installBook(player, body, query); yield state(player); }
            case "/permissions" -> { setPermissions(player, query, false); yield state(player); }
            case "/economy" -> {
                balances.put(player.getUniqueId().toString(), Double.parseDouble(query.getOrDefault("balance", "1000")));
                yield state(player);
            }
            case "/archive" -> { archive(query.getOrDefault("label", "manual"), player); yield state(player); }
            case "/deliver" -> {
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity.getCustomName() != null && entity.getCustomName().contains("Brieftaube")) {
                        entity.teleport(player.getLocation().add(0.8, 1.0, 0));
                    }
                }
                yield state(player);
            }
            case "/disconnect" -> { player.kickPlayer("Courier E2E intentional disconnect"); yield Map.of("ok", true); }
            default -> throw new IllegalArgumentException("Unknown endpoint " + path);
        };
    }

    private Player findPlayer(String name) {
        Player player = name == null ? Bukkit.getOnlinePlayers().stream().findFirst().orElse(null)
                : Bukkit.getPlayerExact(name);
        if (player == null) throw new IllegalArgumentException("Player not online: " + name);
        return player;
    }

    private void reset(Player player, Map<String, String> options) {
        if (!platformReady) {
            var world = player.getWorld();
            for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) {
                world.getBlockAt(x, 64, z).setType(Material.STONE, false);
                for (int y = 65; y <= 70; y++) world.getBlockAt(x, y, z).setType(Material.AIR, false);
            }
            world.setSpawnLocation(0, 65, 0);
            platformReady = true;
        }
        player.closeInventory();
        player.setOp(false);
        player.setGameMode(GameMode.CREATIVE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.teleport(new Location(player.getWorld(), 0.5, 65, 0.5, 0, 0));
        player.getInventory().clear();
        player.getInventory().setHeldItemSlot(bounded(options, "slot", 0, 0, 8));
        int amount = bounded(options, "supplies", 1, 0, 64);
        if (amount > 0) player.getInventory().setItemInMainHand(supply(amount));
        setPermissions(player, Map.of(), true);
        balances.put(player.getUniqueId().toString(), 1000.0);
        for (Entity entity : player.getWorld().getEntities()) {
            if (!(entity instanceof Player) && entity.getCustomName() != null
                    && entity.getCustomName().contains("Brieftaube")) entity.remove();
        }
        try {
            courierClass("de.ottonien.courier.letter.Outgoing").getMethod("reset", UUID.class)
                    .invoke(null, player.getUniqueId());
            Class<?> announcementClass = courierClass("de.ottonien.courier.announce.Announcements");
            Field listField = announcementClass.getDeclaredField("announcements");
            listField.setAccessible(true);
            ((List<?>) listField.get(null)).clear();
            Plugin courier = Bukkit.getPluginManager().getPlugin("Courier");
            Object announcementConfig = courierClass("de.ottonien.courier.Config")
                    .getMethod("getAnnouncementsConfig", courier.getClass()).invoke(null, courier);
            YamlConfiguration yaml = (YamlConfiguration) announcementConfig.getClass().getMethod("getConfig").invoke(announcementConfig);
            yaml.set("Announcements", null);
            announcementConfig.getClass().getMethod("saveConfig").invoke(announcementConfig);
            clearCooldown(announcementClass, null, "Announcement_" + player.getUniqueId());
            Object executor = Bukkit.getPluginCommand("courier:unread").getExecutor();
            clearCooldown(executor.getClass(), executor, player.getUniqueId().toString());
            Object couriers = courierClass("de.ottonien.courier.courier.Courier").getMethod("getCouriers").invoke(null);
            ((Map<?, ?>) couriers).clear();
        } catch (Exception error) { getLogger().warning("Reset outgoing: " + error); }
        commandEvents.clear();
        chatEvents.clear();
        interactionEvents.clear();
        player.updateInventory();
        archive("reset", player);
    }

    private void clearCooldown(Class<?> owner, Object instance, String key) throws Exception {
        Field field = owner.getDeclaredField("coolDownManager");
        field.setAccessible(true);
        Object manager = field.get(instance);
        manager.getClass().getMethod("removeCoolDowns", String.class).invoke(manager, key);
    }

    private void setPermissions(Player player, Map<String, String> options, boolean defaults) {
        PermissionAttachment attachment = permissions.computeIfAbsent(player.getUniqueId(), ignored -> player.addAttachment(this));
        for (String key : List.of("letter", "post", "announce", "unread", "announcements")) {
            if (defaults || options.containsKey(key)) {
                attachment.setPermission("courier." + (key.equals("post") ? "post.one" : key),
                        Boolean.parseBoolean(options.getOrDefault(key, "true")));
            }
        }
        attachment.setPermission("skills.reading.6", true);
        player.recalculatePermissions();
    }

    private void installBook(Player player, String body, Map<String, String> query) {
        var parsed = JsonParser.parseString(body);
        JsonObject options = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        JsonArray pages = parsed.isJsonArray() ? parsed.getAsJsonArray() : options.getAsJsonArray("pages");
        if (pages == null) throw new IllegalArgumentException("Expected JSON pages array or {pages:[...]}");
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(options.has("title") ? options.get("title").getAsString() : "§7Brief von §6" + player.getName());
        meta.setAuthor(options.has("author") ? options.get("author").getAsString() : player.getName());
        meta.setPages(pages.asList().stream().map(value -> value.getAsString()).toList());
        meta.setLore(options.has("sent") && options.get("sent").getAsBoolean()
                ? List.of("E2E import fixture", "§8Zu " + player.getName()) : List.of("E2E import fixture"));
        meta.setItemModel(NamespacedKey.fromString("ottonien:item/brief"));
        book.setItemMeta(meta);
        player.getInventory().setItem(bounded(query, "slot", player.getInventory().getHeldItemSlot(), 0, 40), book);
        player.updateInventory();
        archive("book-fixture", player);
    }

    private Map<String, Object> state(Player player) throws Exception {
        var result = new LinkedHashMap<String, Object>();
        result.put("runId", runId);
        result.put("at", Instant.now().toString());
        result.put("tick", Bukkit.getCurrentTick());
        result.put("player", player.getName());
        result.put("uuid", player.getUniqueId().toString());
        result.put("slot", player.getInventory().getHeldItemSlot());
        var hand = item(player.getInventory().getItemInMainHand());
        hand.put("slot", player.getInventory().getHeldItemSlot());
        result.put("hand", hand);
        result.put("permissions", Map.of("letter", player.hasPermission("courier.letter"),
                "post", player.hasPermission("courier.post.one"), "announce", player.hasPermission("courier.announce")));
        result.put("balance", balances.getOrDefault(player.getUniqueId().toString(), 1000.0));
        result.put("location", Map.of("x", player.getX(), "y", player.getY(), "z", player.getZ()));
        var inventory = new ArrayList<Map<String, Object>>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                var entry = new LinkedHashMap<>(item(stack));
                entry.put("slot", slot);
                inventory.add(entry);
            }
        }
        result.put("inventory", inventory);
        result.put("commands", commandEvents.stream().filter(entry -> player.getName().equals(entry.get("player")))
                .map(entry -> entry.get("command")).toList());
        result.put("chatMessages", chatEvents.stream().filter(entry -> player.getName().equals(entry.get("player")))
                .map(entry -> entry.get("message")).toList());
        result.put("commandEvents", List.copyOf(commandEvents));
        result.put("interactions", List.copyOf(interactionEvents));
        result.put("outgoing", outgoing(player));
        result.put("announcements_count", announcements().size());
        var persisted = persistedAnnouncements();
        result.put("announcements", persisted);
        result.put("announcements_persisted_count", persisted.size());
        result.put("couriers", player.getWorld().getEntities().stream()
                .filter(entity -> entity.getCustomName() != null && entity.getCustomName().contains("Brieftaube"))
                .map(entity -> Map.of("id", entity.getEntityId(), "entityId", entity.getEntityId(), "uuid", entity.getUniqueId().toString(),
                        "type", entity.getType().name(), "x", entity.getX(), "y", entity.getY(), "z", entity.getZ())).toList());
        return result;
    }

    private Map<String, Object> item(ItemStack stack) {
        var result = new LinkedHashMap<String, Object>();
        result.put("type", stack == null ? "AIR" : stack.getType().name());
        result.put("amount", stack == null ? 0 : stack.getAmount());
        result.put("rawPages", List.of());
        result.put("title", "");
        result.put("author", "");
        result.put("lore", List.of());
        if (stack != null && stack.hasItemMeta()) {
            var meta = stack.getItemMeta();
            result.put("name", meta.hasDisplayName() ? meta.getDisplayName() : "");
            result.put("lore", meta.hasLore() ? meta.getLore() : List.of());
            result.put("itemModel", meta.hasItemModel() ? String.valueOf(meta.getItemModel()) : "");
            if (meta instanceof BookMeta book) {
                result.put("title", book.getTitle());
                result.put("author", book.getAuthor());
                result.put("rawPages", book.getPages());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked") private List<Map<String, Object>> outgoing(Player player) throws Exception {
        Map<UUID, List<ItemStack>> map = (Map<UUID, List<ItemStack>>) courierClass("de.ottonien.courier.letter.Outgoing")
                .getMethod("getOutgoing").invoke(null);
        return new ArrayList<>(map.getOrDefault(player.getUniqueId(), List.of())).stream().map(this::item).toList();
    }

    private List<?> announcements() throws Exception {
        Field field = courierClass("de.ottonien.courier.announce.Announcements").getDeclaredField("announcements");
        field.setAccessible(true);
        return List.copyOf((List<?>) field.get(null));
    }

    /** Read disk afresh: a Courier failure after save can leave YAML ahead of its in-memory list. */
    private List<Map<String, Object>> persistedAnnouncements() throws Exception {
        Plugin courier = Bukkit.getPluginManager().getPlugin("Courier");
        Path file = courier.getDataFolder().toPath().resolve("announcements.yml");
        if (!Files.exists(file)) return List.of();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file.toFile());
        var section = yaml.getConfigurationSection("Announcements");
        if (section == null) return List.of();
        var records = new ArrayList<Map<String, Object>>();
        for (String key : section.getKeys(false).stream().sorted().toList()) {
            var entry = new LinkedHashMap<String, Object>();
            entry.put("uuid", key);
            entry.put("player", section.getString(key + ".Player", ""));
            entry.put("created", section.getLong(key + ".Created"));
            entry.put("announced", section.getBoolean(key + ".Announced"));
            entry.put("letter", item(section.getItemStack(key + ".Letter")));
            records.add(entry);
        }
        return records;
    }

    private Class<?> courierClass(String name) throws ClassNotFoundException {
        Plugin courier = Bukkit.getPluginManager().getPlugin("Courier");
        return Class.forName(name, true, courier.getClass().getClassLoader());
    }

    private void archive(String label, Player player) {
        try {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("label", label);
            payload.put("state", state(player));
            String safe = label.replaceAll("[^A-Za-z0-9_-]", "_");
            Files.writeString(reports.resolve(String.format("%s-%06d-%s.json", runId, ++sequence, safe)),
                    JSON.toJson(payload), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (Exception error) { getLogger().warning("Archive: " + error); }
    }

    private void appendReport(String file, Object payload) {
        try {
            Files.writeString(reports.resolve(file), new Gson().toJson(payload) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception error) { getLogger().warning("Report: " + error); }
    }

    private static void send(HttpExchange exchange, int status, Object value) throws Exception {
        byte[] body = JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static Map<String, String> query(String value) {
        var result = new HashMap<String, String>();
        if (value != null) for (String entry : value.split("&")) {
            String[] parts = entry.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }

    private static int bounded(Map<String, String> options, String key, int fallback, int min, int max) {
        int value = Integer.parseInt(options.getOrDefault(key, String.valueOf(fallback)));
        if (value < min || value > max) throw new IllegalArgumentException(key + " outside " + min + ".." + max);
        return value;
    }
}
