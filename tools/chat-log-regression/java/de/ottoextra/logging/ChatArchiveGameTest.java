package de.ottoextra.logging;

import de.ottoextra.addon.ChatMessageContext;
import de.ottoextra.addon.OttoExtraAddon;
import de.ottoextra.addon.OttoExtraAddons;
import de.ottoextra.animalinfo.AnimalInfoService;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.settings.SettingsRegistry;
import de.ottoextra.mixin.ChatHudAccessor;
import de.ottoextra.rpnames.chat.ChatHistoryRefresh;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Exercises the transformed ChatHud and independent, numbered session files. */
public final class ChatArchiveGameTest implements FabricClientGameTest {
    private final String prefix = "CHAT-ARCHIVE-" + UUID.randomUUID() + " ";
    private final Path logs = FabricLoader.getInstance().getGameDir().resolve("logs");

    @Override
    public void runTest(ClientGameTestContext test) {
        CompletableFuture<Text> delayed = new CompletableFuture<>();
        Object previousAddons = field(OttoExtraAddons.class, "addons");
        OttoExtraConfig config = OttoExtraConfig.active();
        boolean previousChatLog = config.tweaks.chatLog;
        boolean previousChatEnabled = config.chat.enabled;
        boolean previousDebug = DebugLog.isEnabled();
        try {
            test.runOnClient(client -> {
                config.tweaks.chatLog = true;
                config.chat.enabled = false;
                DebugLog.setEnabled(false);
                var hud = client.inGameHud.getChatHud();
                hud.addMessage(Text.literal("[RP] <Alrik> " + prefix + "Grüße *nickt*"));
                hud.addMessage(Text.literal(prefix + "signed"), new MessageSignatureData(new byte[256]), null);
                hud.addMessage(Text.literal(prefix + "profileless"), null, MessageIndicator.system());
                Text repeated = Text.literal(prefix + "repeat");
                hud.addMessage(repeated);
                hud.addMessage(repeated);
                hud.addMessage(Text.literal(prefix + "line\r\nnext"));
                hud.addMessage(Text.literal("§a" + prefix + "formatted\\path 👋"));
            });
            expectBoth("Grüße *nickt*", 1);
            expectBoth("signed", 1);
            expectBoth("profileless", 1);
            expectBoth("repeat", 2);
            expectBoth("line\\r\\nnext", 1);
            expectBoth("formatted", 1);

            test.runOnClient(client -> {
                setField(OttoExtraAddons.class, "addons", List.of(new OttoExtraAddon() {
                    @Override
                    public void registerSettings(SettingsRegistry registry) { }

                    @Override
                    public boolean delaysIncomingChat(ChatMessageContext context) {
                        return context.original().getString().equals(prefix + "delayed");
                    }

                    @Override
                    public CompletableFuture<Text> processChatMessageAsync(ChatMessageContext context) {
                        return context.original().getString().equals(prefix + "delayed")
                                ? delayed : CompletableFuture.completedFuture(context.displayed());
                    }
                }));
                client.inGameHud.getChatHud().addMessage(Text.literal(prefix + "delayed"));
                client.inGameHud.getChatHud().addMessage(Text.literal(prefix + "following"));
            });
            // These must already be on disk while the addon is still waiting.
            expectBoth("delayed", 1);
            expectBoth("following", 1);
            test.runOnClient(client -> delayed.complete(Text.literal(prefix + "translated")));
            test.waitTicks(2);
            expectBoth("delayed", 1);
            expectBoth("following", 1);
            expectBoth("translated", 0);
            test.runOnClient(client -> {
                boolean displayed = ((ChatHudAccessor) client.inGameHud.getChatHud()).ottoextra$messages()
                        .stream().anyMatch(line -> line.content().getString().equals(prefix + "translated"));
                require(displayed, "Delayed chat was not displayed");
                setField(OttoExtraAddons.class, "addons", previousAddons);
                ChatHistoryRefresh.request();
            });
            test.waitTicks(2);
            expectBoth("delayed", 1);
            expectBoth("following", 1);

            test.runOnClient(client -> {
                var hud = client.inGameHud.getChatHud();
                int visible = ((ChatHudAccessor) hud).ottoextra$messages().size();
                String divider = "-----------------------------------------------------";
                long before = count(vanillaEntries(read(logs.resolve("latest.log"))), divider);
                long chatBefore = count(readChat(), divider);
                boolean hideBefore = config.animalInfo.hideAutomaticChat;
                Object dividerBefore = field(AnimalInfoService.class, "trailingDividerUntil");
                try {
                    config.animalInfo.hideAutomaticChat = true;
                    setField(AnimalInfoService.class, "trailingDividerUntil", Long.MAX_VALUE);
                    hud.addMessage(Text.literal(divider));
                    require(((ChatHudAccessor) hud).ottoextra$messages().size() == visible,
                            "Automatic response became visible");
                    require(count(vanillaEntries(read(logs.resolve("latest.log"))), divider) == before + 1,
                            "Hidden automatic response missing in latest.log");
                    require(count(readChat(), divider) == chatBefore + 1,
                            "Hidden automatic response missing in independent chat log");
                } finally {
                    config.animalInfo.hideAutomaticChat = hideBefore;
                    setField(AnimalInfoService.class, "trailingDividerUntil", dividerBefore);
                }
                config.tweaks.chatLog = false;
                hud.addMessage(Text.literal(prefix + "custom-file-off"));
                config.tweaks.chatLog = true;
                hud.addMessage(Text.literal(prefix + "enabled-again"));
            });
            expect(vanillaEntries(read(logs.resolve("latest.log"))), "custom-file-off", 1);
            expect(readChat(), "custom-file-off", 0);
            expectBoth("enabled-again", 1);

            // The requested files must work even when Minecraft writes no latest.log.
            test.runOnClient(client -> {
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                var root = context.getRootLogger();
                var appender = root.getAppenders().get("LatestFile");
                require(appender != null, "Minecraft latest.log appender missing");
                root.removeAppender(appender);
                try {
                    client.inGameHud.getChatHud().addMessage(Text.literal(prefix + "without-minecraft-file"));
                } finally {
                    root.addAppender(appender);
                }
            });
            expect(readChat(), "without-minecraft-file", 1);
            expect(read(logs.resolve("latest.log")), "without-minecraft-file", 0);
            require(!Files.exists(logs.resolve("ottoextra-chat.log")), "Legacy monolithic chat log still created");
            require(!read(logs.resolve("latest.log")).contains("[OttoExtra-Chat]"),
                    "Independent chat file should not be mirrored into Minecraft's logger");
            require(readChat().contains("formatted\\\\path 👋"), "Formatting/escaping changed in chat file");
            System.out.println("CHAT_SESSION_LOG_PASS: numbered logs/chat file, original chat, immediate flush, "
                    + "replay once, hidden replies, history refresh, genuine duplicates, toggle, "
                    + "works without Minecraft file logging");
        } finally {
            test.runOnClient(client -> {
                delayed.complete(Text.literal(prefix + "cleanup"));
                setField(OttoExtraAddons.class, "addons", previousAddons);
                config.tweaks.chatLog = previousChatLog;
                config.chat.enabled = previousChatEnabled;
                DebugLog.setEnabled(previousDebug);
                client.setScreen(new TitleScreen());
            });
        }
    }

    private String readChat() {
        try (var files = Files.list(logs.resolve("chat"))) {
            List<Path> sessions = files.toList();
            require(sessions.size() == 1, "Expected one chat file in this session, got " + sessions.size());
            Path file = sessions.getFirst();
            require(file.getFileName().toString().matches("ottoextra-chat-\\d{4}-\\d{2}-\\d{2}-1\\.log"),
                    "Unexpected chat filename: " + file);
            return read(file);
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private void expectBoth(String suffix, long expected) {
        String latest = read(logs.resolve("latest.log"));
        expect(vanillaEntries(latest), suffix, expected);
        expect(readChat(), suffix, expected);
    }

    private static String vanillaEntries(String contents) {
        return String.join("\n", contents.lines()
                .filter(line -> line.contains("[CHAT]")).toList());
    }

    private void expect(String contents, String suffix, long expected) {
        long actual = count(contents, prefix + suffix);
        require(actual == expected, suffix + ": expected " + expected + " log entries, got " + actual);
    }

    private static long count(String contents, String needle) {
        return contents.lines().filter(line -> line.contains(needle)).count();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private static Object field(Class<?> type, String name) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void setField(Class<?> type, String name, Object value) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
