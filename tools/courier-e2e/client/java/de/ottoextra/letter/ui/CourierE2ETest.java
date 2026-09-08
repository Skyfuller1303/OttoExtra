package de.ottoextra.letter.ui;

import com.google.gson.*;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.*;
import de.ottoextra.letter.calibration.*;
import de.ottoextra.letter.send.LetterV2Planner;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

/** Runs only with tools/courier-e2e/client.init.gradle; never included in release JARs. */
public final class CourierE2ETest implements FabricClientGameTest {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final List<Map<String, Object>> results = new ArrayList<>();
    private final String runId = Instant.now().toString().replace(':', '-');
    private final Path reports = Path.of(System.getProperty("courier.e2e.reportDir"));
    private ClientGameTestContext ctx;
    private OttoExtraConfig config;
    private Map<String, Object> current;

    @Override
    public void runTest(ClientGameTestContext context) {
        ctx = context;
        try {
            Files.createDirectories(reports);
            ctx.runOnClient(c -> {
                config = OttoExtraConfig.active();
                config.letter.sendMode = "V2";
                config.letter.announcementAutoOptimizeEnabled = true;
                clear();
                c.setScreen(new TitleScreen());
            });
            join();
            var corpus = new ArrayList<>(LetterCalibrationCorpus.v2());
            corpus.addAll(LetterCalibrationCorpus.v2Refine());
            corpus.addAll(LetterCalibrationCorpus.v2Final());
            corpus.addAll(LetterCalibrationCorpus.all());
            corpus.add(new LetterCalibrationCorpus.TestCase("U01", "Supplementary Unicode", "Grüße 😀 🕊 Ende."));
            for (var sample : corpus) test(sample.id(), sample.purpose(), () -> writeCase(sample));
            test("G01", "Kein Schreibmaterial", () -> guard(0));
            test("G02", "Mehrere Schreibmaterialien", () -> guard(2));
            test("G03", "Aktiven Slot während Schreiben wechseln", this::slotInterruption);
            test("G04", "Server lehnt Schreibbefehl ab", this::permissionFailure);
            test("G05", "Zusätzliches Material rechts vom aktiven Slot bleibt erhalten", () -> {
                api("POST", "/supply&slot=5&amount=1", null); pause(300);
                LetterDraft draft = editor("Einzelnes Pergament trotz Vorrat");
                List<String> expected = preview();
                click("ottoextra.letter.send"); written(draft, expected);
                JsonArray inventory = api("GET", "/state", null).getAsJsonArray("inventory");
                check(java.util.stream.StreamSupport.stream(inventory.spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .anyMatch(item -> item.get("slot").getAsInt() == 5
                                && item.get("type").getAsString().equals("PAPER")
                                && item.get("amount").getAsInt() == 1), "Spare supply was consumed");
            });
            test("G06", "Paralleler Schreibstart wird abgewiesen", () -> {
                LetterDraft draft = editor("Nur ein Brief");
                LetterDraft second = LetterDraft.empty(); second.pages.set(0, "Konkurrierender Brief");
                ctx.runOnClient(c -> {
                    c.currentScreen.children().stream().filter(e -> e instanceof ButtonWidget).map(e -> (ButtonWidget)e)
                            .filter(b -> b.getMessage().getString().equals(Text.translatable("ottoextra.letter.send").getString()))
                            .findFirst().orElseThrow().onPress(new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0));
                    check(!LetterServices.startWrite(config, second), "Concurrent write accepted");
                });
                written(draft, List.of("Nur ein Brief")); check(commands().size() == 1, "Duplicate write");
            });
            test("G07", "Vorrat im Hauptinventar erlaubt Mehrseitenbrief", () -> writeWithReserve(2, 12, ""));
            test("G08", "Vorrat in Nebenhand erlaubt Schreiben aus letztem Hotbar-Slot", () -> writeWithReserve(8, 40, ""));
            test("G09", "Früheres Material: genauer Hinweis und Schreiben nach Slotwahl", () -> {
                arrangeReserve(2, 0, 1, "");
                LetterDraft draft = editor("Einzelnes Pergament und Vorrat weiter links");
                List<String> expected = preview(); click("ottoextra.letter.send"); pause(400);
                assertGuardStatus("ottoextra.letter.write.earlierSupply");
                screenshot("courier-earlier-supply-hint");
                api("POST", "/slot&slot=0", null); pause(300);
                click("ottoextra.letter.send"); written(draft, expected);
                check(inventoryItem(2).get("amount").getAsInt() == 1, "Other supply was consumed");
            });
            test("G10", "Courier entfernt früheres Material auch bei anderem Modell", () -> {
                arrangeReserve(2, 0, 1, "model");
                editor("Modellabweichung"); click("ottoextra.letter.send"); pause(400);
                assertGuardStatus("ottoextra.letter.write.earlierSupply");
                // Test-only oracle: bypass OttoExtra to confirm why this order is unsafe.
                ctx.runOnClient(c -> c.getNetworkHandler().sendChatCommand("letter Serverreferenz"));
                await(() -> !commands().isEmpty(), 5000, "Reference command did not arrive"); pause(300);
                JsonObject state = api("GET", "/state", null);
                check(state.getAsJsonObject("hand").get("type").getAsString().equals("PAPER"), "Courier unexpectedly consumed the held supply");
                check(inventoryItem(0).get("type").getAsString().equals("WRITTEN_BOOK"), "Earlier-slot diversion was not reproduced");
            });
            test("G11", "Andere Lore weiter links blockiert nicht", () -> writeWithReserve(2, 0, "lore"));
            test("G12", "Anderer Name weiter links blockiert nicht", () -> writeWithReserve(2, 0, "name"));
            test("G13", "Leere Hand leiht kein Schreibmaterial aus dem Inventar", () -> {
                api("POST", "/slot&slot=2", null); pause(300);
                editor("Kein Material in der Hand"); click("ottoextra.letter.send"); pause(400);
                assertGuardStatus("ottoextra.letter.write.holdSupply", config.letter.triggerItemName);
            });
            test("I01", "Import, gesperrter Alttext, Unicode, Scrollen, Weiterführen", this::importAppend);
            test("R01-runtime", "Disconnect und echte Wiederaufnahme ohne doppelte Befehle", this::recovery);
            for (String variant : List.of("server", "player", "missing", "book", "restart", "discard"))
                test("R-" + variant, "Wiederaufnahme: " + variant, () -> recoveryGuard(variant));
            test("P01", "Empfängerdialog, Selbstversand und Brieftaube", this::post);
            test("RP01", "Geschriebenen Brief schließen und über Buchbutton verschicken", this::reopenWrittenPost);
            test("RP02", "Vorhandenen Brief nach erneutem Login verschicken", this::reopenPostAfterLogin);
            test("RP03", "Versandbefehl verwendet gehaltenen Brief statt alten Entwurf", this::reopenPostCommand);
            test("RP04", "Empfangene Briefe, normale Bücher und Nebenhand nicht versandbereit", this::unavailableBookPost);
            test("RP05", "Slotwechsel im erneut geöffneten Versanddialog stoppt Zustellung", this::reopenedPostSlotGuard);
            test("RP06", "Ausgetauschtes Buch vor Buttonklick wird nicht übernommen", this::reopenPostBookGuard);
            test("RP07", "Laufendes Schreiben wird durch erneutes Öffnen nicht ersetzt", this::reopenPostWhileWriting);
            test("RP08", "Nach abgelehntem Versand erneut öffnen und zustellen", this::reopenRejectedPost);
            test("RP09", "Anderes angezeigtes Buch bietet keinen Versand der Haupthand an", this::reopenDisplayedBookGuard);
            test("P02", "Post ohne Berechtigung", () -> submitDenied(false, false));
            test("P03", "Post ohne Geld", () -> submitDenied(false, true));
            test("A01", "Verkündung aus frischem Entwurf", () -> announce(false));
            test("A02", "Verkündung mit bereits geschriebenem Brief", () -> announce(true));
            test("A03", "Verkündung ohne Berechtigung", () -> submitDenied(true, false));
            test("A04", "Verkündung ohne Geld", () -> submitDenied(true, true));
            test("A05", "Verkündung nach Auto-Optimieren", this::optimizedAnnouncement);
            test("A06", "Verkündungs-Cooldown bewahrt zweiten Brief", this::announcementCooldown);
            test("E01", "Editor Undo, Redo und Speichern", this::editorHistory);
            test("E02", "Manuelle Seite über Plus-Button", () -> {
                LetterDraft draft = editor("§cErste Seite§r"); click("+"); paste("Zweite Seite");
                check(ctx.computeOnClient(c -> V2DraftFlow.sourcePages(draft).size() == 2), "Manual page break missing");
                List<String> predicted = preview(); click("ottoextra.letter.send"); written(draft, predicted);
            });
            test("E03", "Unicode tippen, Cursor bewegen und löschen", () -> {
                LetterDraft draft = editor("Start ");
                "😀X".codePoints().forEach(ctx.getInput()::typeChar);
                key(GLFW.GLFW_KEY_LEFT, 0); key(GLFW.GLFW_KEY_BACKSPACE, 0);
                check(ctx.computeOnClient(c -> String.join("\n", V2DraftFlow.sourcePages(draft))).equals("StartX"), "Emoji deletion split surrogate or deleted wrong character");
                List<String> predicted = preview(); click("ottoextra.letter.send"); written(draft, predicted);
            });
            test("UI01", "Alle Editorbuttons erreichbar, Größenwechsel und echte Mausklicks", () -> {
                LetterDraft draft = editor("Bildschirmprüfung\nZweite Zeile.");
                editorBounds(); screenshot("courier-editor-1280x720");
                ctx.getInput().resizeWindow(1920, 1080); ctx.waitTick(); editorBounds();
                screenshot("courier-editor-1920x1080");
                ctx.getInput().resizeWindow(1280, 720); ctx.waitTick(); editorBounds();
                List<String> predicted = preview(); click("ottoextra.letter.send"); written(draft, predicted);
            });
            test("FIX-CHAT-OFF", "Langchat ohne RP-Formatierung und vollständiger Eingabeverlauf", () -> auditLongChat(false, "s", true));
            test("FIX-CHAT-OOC", "Langchat im OOC-Kanal ohne synthetische Klammern", () -> auditLongChat(true, "o", false));
            test("FIX-CHAT-RP", "Langchat mit aktivierter RP-Syntax", () -> auditLongChat(true, "s", false));
            test("FIX-RECONNECT", "Langchat-Abbruch bei echtem Disconnect/Reconnect", this::auditChatReconnect);
            test("FIX-GPU", "Kopiertexturen und Views beim Größenwechsel wirklich freigegeben", () -> AuditFixClientChecks.gpu(ctx));
            test("FIX-STATUS", "Rückmeldung nach Backup-Aktion bleibt sichtbar", () -> {
                AuditFixClientChecks.backupStatus(ctx);
                screenshot("audit-backup-status");
            });
            test("FIX-PAGES", "Buchseitenzähler, Cache und Fehlerstatus im Editor", () -> {
                LetterDraft draft = editor(LetterCalibrationCorpus.byId("V207").pages().getFirst());
                ctx.waitTick();
                AuditFixClientChecks.bookCount(ctx, config, draft);
                screenshot("audit-working-and-book-pages");
                paste(" Zusatz"); ctx.waitTick(); AuditFixClientChecks.bookCount(ctx, config, draft);
                key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL); ctx.waitTick(); AuditFixClientChecks.bookCount(ctx, config, draft);
                List<String> predicted = preview(); click("ottoextra.letter.send"); written(draft, predicted);
                editor("Unzulässig: \\n Ende"); ctx.waitTick();
                check(ctx.computeOnClient(c -> CourierE2ETest.<Integer>field(c.currentScreen, "previewBookPages")) == -1,
                        "Unsafe plan must not show a valid page count");
            });
            test("UI-ANIMAL-ICONS", "16px Tierstatus-Icons, kompakte Skalierung und Layout-Editor", () -> {
                de.ottoextra.animalinfo.AnimalInfoUiChecks.run(ctx);
                check(commands().isEmpty(), "Icon rendering emitted server commands");
            });
            test("UI-ANIMAL-NAMETAGS", "Tier-Nametags: Geschlecht, Farbe, Render-State und Einstellungen", () -> {
                de.ottoextra.horses.AnimalNametagUiChecks.run(ctx);
                check(commands().isEmpty(), "Nametag rendering emitted server commands");
            });
            test("UI-ANIMAL-OVERVIEW", "Tierartenfilter, kompakte Tierzeilen und Esel speichern", () -> {
                de.ottoextra.horses.AnimalOverviewUiChecks.run(ctx);
                check(commands().isEmpty(), "Overview emitted server commands");
            });
            test("UI-MINIMAP-HEIGHT", "Minimap: Texthöhe, Zeilenumbruch und Größenänderung per Maus", () -> {
                de.ottoextra.map.MinimapHeightUiChecks.run(ctx);
                check(commands().isEmpty(), "Minimap layout emitted server commands");
            });
            test("UI-INVENTORY", "Inventar und Rezeptbuch wiederholt öffnen, umschalten und skalieren", () -> {
                InventoryUiChecks.run(ctx);
                check(commands().isEmpty(), "Inventory test emitted server commands");
            });
            test("UI-ANIMAL-GLOW", "Tier-Glow: Geschlechtsfarben, Finder-Vorrang, Menü und Tastenkürzel", () -> {
                de.ottoextra.horses.AnimalGlowUiChecks.run(ctx);
                check(commands().isEmpty(), "Animal glow emitted server commands");
            });
            check(!results.isEmpty(), "No tests selected");
            String requested = System.getProperty("courier.e2e.filter", "");
            if (!requested.isBlank()) for (String id : requested.split(","))
                check(results.stream().anyMatch(r -> id.equals(r.get("id"))), "Unknown test filter: " + id);
            save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ctx.runOnClient(c -> { clear(); c.disconnect(new TitleScreen(), false); c.setScreen(new TitleScreen()); });
            ctx.waitFor(c -> c.world == null);
        }
        long failed = results.stream().filter(r -> "FAIL".equals(r.get("status"))).count();
        if (failed != 0) throw new AssertionError(failed + " Courier E2E cases failed; see " + reports);
    }

    private void join() {
        ctx.runOnClient(c -> ConnectScreen.connect(c.currentScreen, c,
                ServerAddress.parse("127.0.0.1:25579"), new ServerInfo("Courier E2E", "127.0.0.1:25579",
                ServerInfo.ServerType.OTHER), false, null));
        ctx.waitFor(c -> c.player != null && c.world != null && c.getNetworkHandler() != null, 1200);
        pause(1500);
        ctx.runOnClient(c -> { if (!(c.currentScreen instanceof RecoveryPromptScreen)) c.setScreen(null); });
    }

    private void test(String id, String purpose, Runnable body) {
        String filter = System.getProperty("courier.e2e.filter", "");
        if (!filter.isBlank() && Arrays.stream(filter.split(",")).noneMatch(id::equals)) return;
        current = new LinkedHashMap<>();
        current.put("id", id); current.put("purpose", purpose); current.put("started", Instant.now().toString());
        System.out.println("COURIER_E2E START " + id + " " + purpose);
        long start = System.currentTimeMillis();
        try {
            reset(1);
            body.run();
            current.put("status", "PASS");
        } catch (Throwable e) {
            current.put("status", "FAIL"); current.put("error", e.toString());
            e.printStackTrace();
            try { ctx.takeScreenshot("courier-" + id + "-failure"); } catch (Throwable ignored) { }
        } finally {
            current.put("durationMs", System.currentTimeMillis() - start);
            try { current.put("server", api("GET", "/state", null)); } catch (Throwable e) { current.put("serverError", e.toString()); }
            results.add(current); save();
            System.out.println("COURIER_E2E " + current.get("status") + " " + id);
        }
    }

    private void clear() {
        de.ottoextra.chat.LongChatSender.cancel();
        LetterServices.clearSendingState(); LetterServices.letterStore().clear();
        LetterServices.announcementStore().clear(); LetterDraftCache.clear();
    }

    private List<String> chatMessages() {
        List<String> messages = new ArrayList<>();
        api("GET", "/state", null).getAsJsonArray("chatMessages").forEach(e -> messages.add(e.getAsString()));
        return messages;
    }

    private void auditLongChat(boolean formatting, String channel, boolean history) {
        String text = "Start (" + "word ".repeat(80) + "end)";
        boolean rp = formatting && channel.equals("s");
        List<String> expected = de.ottoextra.chat.LongChatSplitter.split(text, 256, " >", rp);
        ctx.runOnClient(c -> {
            config.chat.enabled = true; config.chat.longChatEnabled = true;
            config.chat.longChatChunk = 256; config.chat.longChatMarker = " >";
            config.chat.longChatDelayMs = 500; config.chat.rpFormattingEnabled = formatting;
            de.ottoextra.chat.ChatChannelState.handleOutgoingCommand(channel);
            int before = c.inGameHud.getChatHud().getMessageHistory().size();
            c.setScreen(new net.minecraft.client.gui.screen.ChatScreen("", false));
            ((net.minecraft.client.gui.screen.ChatScreen)c.currentScreen).sendMessage(text, history);
            check(c.inGameHud.getChatHud().getMessageHistory().size() == before + (history ? 1 : 0), "Wrong history count");
            c.setScreen(new net.minecraft.client.gui.screen.ChatScreen("", false));
            if (history) {
                c.currentScreen.keyPressed(new KeyInput(GLFW.GLFW_KEY_UP, 0, 0));
                net.minecraft.client.gui.widget.TextFieldWidget input = field(c.currentScreen, "chatField");
                check(input.getText().equals(text), "Arrow-up must restore the whole long message");
            }
            c.setScreen(null);
        });
        await(() -> chatMessages().size() >= expected.size(), 10_000, "Long chat did not arrive on server");
        check(chatMessages().equals(expected), "Server chat differs: " + chatMessages() + " != " + expected);
    }

    private void auditChatReconnect() {
        ctx.runOnClient(c -> {
            de.ottoextra.chat.LongChatSender.configureMs(5000);
            de.ottoextra.chat.LongChatSender.enqueue(c.getNetworkHandler(), List.of("before-disconnect", "must-not-resume"));
        });
        await(() -> chatMessages().size() == 1, 10_000, "First chat chunk missing");
        ctx.runOnClient(c -> { c.disconnect(new TitleScreen(), false); c.setScreen(new TitleScreen()); });
        ctx.waitFor(c -> c.world == null); join(); pause(5500);
        check(chatMessages().equals(List.of("before-disconnect")), "Old chat resumed after reconnect: " + chatMessages());
        ctx.runOnClient(c -> {
            de.ottoextra.chat.LongChatSender.configureMs(500);
            de.ottoextra.chat.LongChatSender.enqueue(c.getNetworkHandler(), List.of("fresh-connection"));
        });
        await(() -> chatMessages().size() == 2, 5000, "Fresh connection cannot send");
        check(chatMessages().getLast().equals("fresh-connection"), "Wrong fresh chat");
    }

    private void reset(int supplies) {
        ctx.runOnClient(c -> { clear(); c.setScreen(null); fastTiming(); });
        api("POST", "/reset&supplies=" + supplies + "&slot=0", null);
        pause(500);
    }

    private void fastTiming() {
        config.letter.pageModeSendDelayMs = 500;
        config.letter.letterSendDelayMinMs = config.letter.letterSendDelayMaxMs = 500;
        config.letter.letterPageDelayMinMs = config.letter.letterPageDelayMaxMs = 500;
    }

    private LetterDraft editor(String text) {
        ctx.runOnClient(c -> c.setScreen(new LetterEditorScreen(null, config, LetterDraft.empty())));
        editorBounds();
        paste(text);
        LetterDraft draft = ctx.computeOnClient(c -> field(c.currentScreen, "draft"));
        String actual = ctx.computeOnClient(c -> String.join("\n", V2DraftFlow.sourcePages(draft)));
        String expected = de.ottoextra.letter.format.LetterFormattingCodes.ampersandToSection(text);
        expected = expected.replace('\u00a0', ' ').replace('\u2007', ' ').replace('\u202f', ' ')
                .replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ");
        expected = String.join("\n", Arrays.stream(expected.split("\n", -1)).map(line -> line.trim().replaceAll(" +", " ")).toList());
        check(actual.equals(expected), "Paste content mismatch: " + actual + " != " + expected);
        return draft;
    }

    private void paste(String text) {
        ctx.runOnClient(c -> { c.keyboard.setClipboard(text);
            c.currentScreen.keyPressed(new KeyInput(GLFW.GLFW_KEY_V, 0, GLFW.GLFW_MOD_CONTROL)); });
    }

    private void key(int key, int mods) {
        ctx.runOnClient(c -> c.currentScreen.keyPressed(new KeyInput(key, 0, mods)));
    }

    private void click(String key) {
        double[] position = ctx.computeOnClient(c -> {
            ButtonWidget button = c.currentScreen.children().stream().filter(e -> e instanceof ButtonWidget)
                    .map(e -> (ButtonWidget)e).filter(b -> b.getMessage().getString().equals(Text.translatable(key).getString()))
                    .findFirst().orElseThrow();
            check(button.active && button.visible, "Button disabled/hidden: " + key);
            check(button.getX() >= 0 && button.getY() >= 0 && button.getX() + button.getWidth() <= c.currentScreen.width
                    && button.getY() + button.getHeight() <= c.currentScreen.height, "Button outside window: " + key);
            var window = c.getWindow();
            return new double[] {(button.getX() + button.getWidth()/2.0) * window.getWidth()/window.getScaledWidth(),
                    (button.getY() + button.getHeight()/2.0) * window.getHeight()/window.getScaledHeight()};
        });
        ctx.getInput().setCursorPos(position[0], position[1]);
        ctx.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT); ctx.waitTick();
    }

    private void editorBounds() { ctx.runOnClient(c -> {
        for (var child : c.currentScreen.children()) if (child instanceof ButtonWidget b && b.visible)
            check(b.getX() >= 0 && b.getY() >= 0 && b.getX()+b.getWidth() <= c.currentScreen.width
                    && b.getY()+b.getHeight() <= c.currentScreen.height, "Clipped editor control: " + b.getMessage().getString());
    }); }

    private void screenshot(String name) { ctx.runOnClient(c -> c.getToastManager().clear()); pause(200); ctx.takeScreenshot(name); }

    private void writeCase(LetterCalibrationCorpus.TestCase sample) {
        if (sample.id().equals("V201")) ctx.runOnClient(c -> {
            config.letter.pageModeSendDelayMs = 1200;
            config.letter.letterPageDelayMinMs = 3000; config.letter.letterPageDelayMaxMs = 5200;
        });
        current.put("sourcePages", sample.pages());
        LetterDraft draft = editor(String.join("\n", sample.pages()));
        if (sample.pages().stream().anyMatch(s -> s.contains("\\n"))) {
            click("ottoextra.letter.send"); pause(500);
            check(!ctx.computeOnClient(c -> LetterServices.isSending()), "Unsafe literal \\n was queued");
            check(commands().isEmpty(), "Unsafe literal \\n reached server");
            return;
        }
        List<String> preview = preview();
        current.put("preview", preview);
        click("ottoextra.letter.send");
        written(draft, preview);
        if (sample.id().equals("V204")) screenshot("courier-V204-written");
    }

    private List<String> preview() {
        click("ottoextra.letter.preview");
        check(ctx.computeOnClient(c -> c.currentScreen instanceof LetterPreviewScreen), "Preview screen did not open");
        List<String> pages = ctx.computeOnClient(c -> List.copyOf(CourierE2ETest.<List<String>>field(c.currentScreen, "pages")));
        click("gui.done");
        return pages;
    }

    private void written(LetterDraft draft, List<String> preview) {
        pause(100);
        await(() -> !ctx.computeOnClient(c -> LetterServices.isSending()), 90_000, "Write never completed");
        check(ctx.computeOnClient(c -> LetterServices.hasPreparedLetter(config, draft)), "Queue stopped without prepared book");
        List<String> raw = rawPages();
        check(!raw.isEmpty(), "Server has no written book");
        equalPages(preview, LetterV2Planner.displayPages(raw));
        List<String> expected = ctx.computeOnClient(c -> LetterServices.snapshotPreparedPages(draft));
        equalPages(expected, raw);
        check(ctx.computeOnClient(c -> LetterServices.letterStore().load() == null), "Completed write retained recovery state");
    }

    private void guard(int count) {
        reset(count); editor("Dieser Brief darf nicht starten."); click("ottoextra.letter.send"); pause(700);
        assertGuardStatus(count == 0 ? "ottoextra.letter.write.holdSupply" : "ottoextra.letter.write.extraSupplies",
                config.letter.triggerItemName);
    }

    private void assertGuardStatus(String key, Object... args) {
        check(commands().isEmpty(), "Guard emitted command");
        check(!ctx.computeOnClient(c -> LetterServices.isSending()), "Guard left queue active");
        ctx.runOnClient(c -> {
            check(c.currentScreen instanceof LetterEditorScreen, "Guard closed editor");
            String message = field(c.currentScreen, "status");
            check(Text.translatable(key, args).getString().equals(message), "Wrong editor rejection: " + message);
            check(c.textRenderer.getWidth(message) <= c.currentScreen.width - 8, "Rejection is clipped horizontally");
        });
    }

    private JsonObject inventoryItem(int slot) {
        return java.util.stream.StreamSupport.stream(api("GET", "/state", null)
                        .getAsJsonArray("inventory").spliterator(), false)
                .map(JsonElement::getAsJsonObject).filter(item -> item.get("slot").getAsInt() == slot)
                .findFirst().orElseThrow(() -> new AssertionError("Inventory slot is empty: " + slot));
    }

    private void arrangeReserve(int selectedSlot, int reserveSlot, int amount, String variant) {
        api("POST", "/reset&supplies=1&slot=" + selectedSlot, null);
        api("POST", "/supply&slot=" + reserveSlot + "&amount=" + amount + "&variant=" + variant, null);
        pause(500);
        ctx.runOnClient(c -> {
            check(c.player.getInventory().getSelectedSlot() == selectedSlot, "Wrong held slot");
            check(c.player.getMainHandStack().getCount() == 1, "Main hand must contain exactly one supply");
        });
    }

    private void writeWithReserve(int selectedSlot, int reserveSlot, String variant) {
        arrangeReserve(selectedSlot, reserveSlot, 16, variant);
        JsonObject before = inventoryItem(reserveSlot).deepCopy();
        LetterDraft draft = editor(LetterCalibrationCorpus.byId("V207").pages().getFirst());
        List<String> expected = preview();
        click("ottoextra.letter.send"); written(draft, expected);
        check(before.equals(inventoryItem(reserveSlot)), "Writing changed reserve material");
        check(api("GET", "/state", null).get("slot").getAsInt() == selectedSlot, "Writing switched the active slot");
    }

    private void slotInterruption() {
        ctx.runOnClient(c -> { config.letter.pageModeSendDelayMs = 2500; config.letter.letterPageDelayMinMs = config.letter.letterPageDelayMaxMs = 2500; });
        editor(LetterCalibrationCorpus.byId("V207").pages().getFirst()); click("ottoextra.letter.send");
        await(() -> !commands().isEmpty(), 10_000, "First command missing");
        api("POST", "/slot&slot=1", null); pause(3200);
        check(!ctx.computeOnClient(c -> LetterServices.isSending()), "Changed slot did not stop queue");
        check(commands().size() == 1, "Changed slot wrote additional commands");
        check(ctx.computeOnClient(c -> LetterServices.letterStore().load() != null), "Interrupted draft lost");
    }

    private void permissionFailure() {
        api("POST", "/permissions&letter=false", null);
        editor(LetterCalibrationCorpus.byId("V207").pages().getFirst()); click("ottoextra.letter.send");
        pause(2200);
        check(commands().size() == 1, "Denied first command did not stop subsequent writes");
        check(rawPages().isEmpty(), "Permission denied but server created book");
        check(ctx.computeOnClient(c -> LetterServices.letterStore().load() != null), "Denied write lost draft recovery");
    }

    private void importAppend() {
        List<String> original = List.of("§6Alte Seite", String.join("\n", java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(i -> "Alt " + i).toList()));
        api("POST", "/book", Map.of("pages", original, "title", "§7Brief von §6CourierAudit", "author", "CourierAudit")); pause(500);
        ctx.runOnClient(c -> WrittenLetterImport.editInEditor(config, c.player.getMainHandStack()));
        check(ctx.computeOnClient(c -> c.currentScreen instanceof LetterEditorScreen), "Book import failed");
        LetterDraft draft = ctx.computeOnClient(c -> field(c.currentScreen, "draft"));
        List<String> locked = List.copyOf(draft.pages);
        key(GLFW.GLFW_KEY_BACKSPACE, 0);
        check(locked.equals(draft.pages), "Backspace changed locked content");
        paste("Anhang eins\nAnhang zwei\nAnhang drei 😀\n§cRot§r.");
        pause(250); editorBounds(); screenshot("courier-import-12-lines");
        check(ctx.computeOnClient(c -> CourierE2ETest.<Integer>field(c.currentScreen, "firstVisibleLine") > 0), "Imported append caret did not scroll into view");
        List<String> predicted = preview();
        click("ottoextra.letter.send"); written(draft, predicted);
        check(rawPages().getFirst().equals(original.getFirst()), "Earlier page changed");
    }

    private void recovery() {
        ctx.runOnClient(c -> { config.letter.pageModeSendDelayMs = 5000; config.letter.letterPageDelayMinMs = config.letter.letterPageDelayMaxMs = 5000; });
        LetterDraft draft = editor(LetterCalibrationCorpus.byId("V207").pages().getFirst());
        List<String> predicted = preview(); click("ottoextra.letter.send");
        await(() -> commands().size() == 1, 10_000, "First command missing"); pause(300);
        var progress = ctx.computeOnClient(c -> LetterServices.letterStore().load());
        check(progress != null && progress.sentCommands == 1, "Progress checkpoint missing");
        ctx.runOnClient(c -> { c.disconnect(new TitleScreen(), false); c.setScreen(new TitleScreen()); });
        ctx.waitFor(c -> c.world == null); pause(500); join();
        await(() -> ctx.computeOnClient(c -> c.currentScreen instanceof RecoveryPromptScreen), 10_000, "Recovery dialog did not reopen");
        ctx.runOnClient(c -> fastTiming()); click("ottoextra.letter.recovery.resume");
        written(draft, predicted);
        check(commands().size() == progress.pendingCommands.size(), "Recovery duplicated/skipped commands");
    }

    private void post() {
        LetterDraft draft = editor("§6E2E Post§r\nGrüße an mich selbst.");
        List<String> predicted = preview(); click("ottoextra.letter.send"); written(draft, predicted);
        check(ctx.computeOnClient(c -> c.currentScreen instanceof RecipientScreen), "Recipient dialog missing");
        ctx.runOnClient(c -> setField(c.currentScreen, "selected", "CourierAudit"));
        click("ottoextra.letter.recipient.send");
        await(() -> ctx.computeOnClient(c -> c.player.getMainHandStack().isEmpty()), 10_000, "Post did not consume book");
        check(api("GET", "/state", null).getAsJsonArray("outgoing").size() == 1, "Post not recorded in Courier queue");
        await(() -> ctx.computeOnClient(c -> { for (var entity : c.world.getEntities()) {
            if (entity instanceof net.minecraft.entity.passive.BatEntity) {
                c.interactionManager.interactEntity(c.player, entity, net.minecraft.util.Hand.MAIN_HAND); return true;
            }
        } return false; }), 20_000, "Courier bat missing");
        await(() -> !rawPages().isEmpty(), 15_000, "Delivered book missing");
        equalPages(predicted, LetterV2Planner.displayPages(rawPages()));
        check(api("GET", "/state", null).getAsJsonArray("outgoing").isEmpty(), "Received book remained queued");
        check(!ctx.computeOnClient(c -> WrittenLetterImport.isOwn(c.player.getMainHandStack())), "Delivered letter remains editable");
    }

    private void openHeldBook() {
        ctx.runOnClient(c -> {
            c.setScreen(null);
            c.interactionManager.interactItem(c.player, net.minecraft.util.Hand.MAIN_HAND);
        });
        ctx.waitFor(c -> c.currentScreen instanceof net.minecraft.client.gui.screen.ingame.BookScreen);
        pause(150);
    }

    private boolean hasBookSendButton() {
        return ctx.computeOnClient(c -> c.currentScreen != null && c.currentScreen.children().stream()
                .filter(child -> child instanceof ButtonWidget).map(child -> (ButtonWidget) child)
                .anyMatch(button -> button.getMessage().getString().equals(Text.translatable("ottoextra.letter.book.send").getString())));
    }

    private void openBookRecipients() {
        openHeldBook();
        check(hasBookSendButton(), "Book send button missing");
        ctx.runOnClient(c -> {
            var buttons = c.currentScreen.children().stream().filter(child -> child instanceof ButtonWidget)
                    .map(child -> (ButtonWidget) child).filter(button -> button.visible).toList();
            for (var button : buttons) {
                check(button.getY() >= 0 && button.getY() + button.getHeight() <= c.currentScreen.height, "Book action outside screen");
                check(button.getX() >= 0 && button.getX() + button.getWidth() <= c.currentScreen.width, "Book action outside width");
            }
        });
        click("ottoextra.letter.book.send");
        check(ctx.computeOnClient(c -> c.currentScreen instanceof RecipientScreen), "Book button did not open recipients");
        check(!ctx.computeOnClient(c -> LetterServices.isSending()), "Reopening started a write queue");
    }

    private void submitReopenedPost() {
        ctx.runOnClient(c -> setField(c.currentScreen, "selected", "CourierAudit"));
        click("ottoextra.letter.recipient.send");
        await(() -> ctx.computeOnClient(c -> c.player.getMainHandStack().isEmpty()), 10_000, "Reopened post did not consume book");
        check(api("GET", "/state", null).getAsJsonArray("outgoing").size() == 1, "Reopened post was not queued once");
    }

    private void installUnsentBook(List<String> pages) {
        api("POST", "/book", Map.of("pages", pages)); pause(300);
    }

    private void reopenWrittenPost() {
        LetterDraft draft = editor("§6Brief später versenden§r\nGrüße 😀");
        List<String> predicted = preview(); click("ottoextra.letter.send"); written(draft, predicted);
        int writes = commands().size();
        click("gui.back"); openHeldBook(); screenshot("courier-book-send-button");
        click("ottoextra.letter.book.send");
        check(ctx.computeOnClient(c -> c.currentScreen instanceof RecipientScreen), "Cannot reopen freshly written letter");
        click("gui.back");
        check(ctx.computeOnClient(c -> c.currentScreen instanceof net.minecraft.client.gui.screen.ingame.BookScreen), "Back lost book view");
        click("ottoextra.letter.book.send");
        check(commands().size() == writes, "Reopening rewrote the letter");
        equalPages(predicted, LetterV2Planner.displayPages(rawPages()));
        submitReopenedPost();
        check(commands().size() == writes + 1, "Reopened post sent duplicate commands");
    }

    private void reopenPostAfterLogin() {
        List<String> original = List.of("§6Vorhandener Brief§r", "Zweite Seite 😀\n§cRot§r Ende");
        installUnsentBook(original);
        LetterDraft cached = LetterDraft.empty(); cached.pages.set(0, "Unabhängiger Entwurf bleibt erhalten");
        ctx.runOnClient(c -> {
            LetterDraftCache.save(cached);
            c.disconnect(new TitleScreen(), false); c.setScreen(new TitleScreen());
        });
        ctx.waitFor(c -> c.world == null); join();
        check(ctx.computeOnClient(c -> LetterServices.pendingDraft() == null), "Pending survived disconnect");
        openBookRecipients();
        check(commands().isEmpty(), "Existing letter was rewritten after login");
        equalPages(original, rawPages());
        check(ctx.computeOnClient(c -> LetterDraftCache.load().pages.equals(cached.pages)), "Reopening overwrote draft cache");
        submitReopenedPost();
        check(commands().equals(List.of("/post CourierAudit")), "Unexpected reconnect delivery commands: " + commands());
    }

    private void reopenPostCommand() {
        List<String> original = List.of("Gehaltenes Buch hat Vorrang."); installUnsentBook(original);
        LetterDraft stale = LetterDraft.empty(); stale.pages.set(0, "Dieser alte Entwurf darf nicht geschrieben werden");
        ctx.runOnClient(c -> {
            LetterServices.setPending(stale);
            c.getNetworkHandler().sendChatCommand("ottoextra letter send");
        });
        pause(300);
        check(ctx.computeOnClient(c -> c.currentScreen instanceof RecipientScreen), "Client command did not reopen held book");
        check(commands().isEmpty(), "Client command wrote stale text or reached server");
        equalPages(original, rawPages()); submitReopenedPost();
    }

    private void unavailableBookPost() {
        for (Map<String, Object> options : List.<Map<String, Object>>of(
                Map.of("pages", List.of("Empfangener Brief"), "sent", true),
                Map.of("pages", List.of("Normales Buch"), "title", "Reisetagebuch"))) {
            api("POST", "/book", options); pause(300); openHeldBook();
            check(!hasBookSendButton(), "Ineligible book exposes delivery");
            check(ctx.computeOnClient(c -> LetterServices.prepareHeldLetter(config) == null), "Ineligible book was prepared");
        }
        api("POST", "/book&slot=40", Map.of("pages", List.of("Brief in Nebenhand")));
        api("POST", "/slot&slot=1", null); pause(300);
        check(ctx.computeOnClient(c -> LetterServices.prepareHeldLetter(config) == null), "Offhand book was adopted");
        check(commands().isEmpty(), "Negative book case sent commands");
    }

    private void reopenedPostSlotGuard() {
        installUnsentBook(List.of("Slotgebundene Zustellung")); openBookRecipients();
        api("POST", "/slot&slot=1", null); pause(300);
        ctx.runOnClient(c -> setField(c.currentScreen, "selected", "CourierAudit"));
        click("ottoextra.letter.recipient.send"); pause(300);
        check(commands().isEmpty(), "Slot change bypassed delivery guard");
        check(ctx.computeOnClient(c -> c.currentScreen instanceof RecipientScreen), "Failed delivery closed recipients");
    }

    private void reopenPostBookGuard() {
        installUnsentBook(List.of("Erster Brief")); openHeldBook();
        installUnsentBook(List.of("Anderer Brief im selben Slot"));
        click("ottoextra.letter.book.send");
        check(ctx.computeOnClient(c -> !(c.currentScreen instanceof RecipientScreen)), "Old book button adopted replacement book");
        check(commands().isEmpty(), "Stale book button sent commands");
        openBookRecipients();
        installUnsentBook(List.of("Noch ein anderer Brief"));
        ctx.runOnClient(c -> setField(c.currentScreen, "selected", "CourierAudit"));
        click("ottoextra.letter.recipient.send"); pause(300);
        check(commands().isEmpty(), "Changed book content bypassed post guard");
    }

    private void reopenPostWhileWriting() {
        ctx.runOnClient(c -> { config.letter.pageModeSendDelayMs = 3000; config.letter.letterPageDelayMinMs = config.letter.letterPageDelayMaxMs = 3000; });
        LetterDraft draft = editor(LetterCalibrationCorpus.byId("V207").pages().getFirst());
        List<String> predicted = preview(); click("ottoextra.letter.send");
        await(() -> commands().size() == 1, 10_000, "First write missing"); pause(200);
        check(ctx.computeOnClient(c -> LetterServices.isSending()), "Expected active write");
        check(ctx.computeOnClient(c -> LetterServices.prepareHeldLetter(config) == null), "Active write was replaced");
        written(draft, predicted);
    }

    private void reopenRejectedPost() {
        List<String> original = List.of("Nach Ablehnung erneut zustellen"); installUnsentBook(original);
        api("POST", "/permissions&post=false", null); openBookRecipients();
        ctx.runOnClient(c -> setField(c.currentScreen, "selected", "CourierAudit"));
        click("ottoextra.letter.recipient.send"); pause(400);
        equalPages(original, rawPages());
        check(api("GET", "/state", null).getAsJsonArray("outgoing").isEmpty(), "Rejected post was queued");
        api("POST", "/permissions&post=true", null); openBookRecipients(); submitReopenedPost();
        check(commands().equals(List.of("/post CourierAudit", "/post CourierAudit")), "Retry rewrote book or duplicated post");
    }

    private void reopenDisplayedBookGuard() {
        installUnsentBook(List.of("Brief in der Haupthand"));
        api("POST", "/book&slot=40", Map.of("pages", List.of("Anderes Buch in der Nebenhand"), "sent", true));
        pause(300);
        ctx.runOnClient(c -> c.interactionManager.interactItem(c.player, net.minecraft.util.Hand.OFF_HAND));
        ctx.waitFor(c -> c.currentScreen instanceof net.minecraft.client.gui.screen.ingame.BookScreen);
        check(!hasBookSendButton(), "Displayed offhand book offers delivery of a different main-hand letter");
        check(commands().isEmpty(), "Reading another book sent commands");
        openBookRecipients();
        click("gui.back"); click("ottoextra.letter.book.edit");
        check(ctx.computeOnClient(c -> c.currentScreen instanceof LetterEditorScreen), "Book edit action no longer opens editor");
        LetterDraft imported = ctx.computeOnClient(c -> field(c.currentScreen, "draft"));
        equalPages(rawPages(), imported.meta.courierInitialPages);
    }

    private void announce(boolean prepared) {
        LetterDraft draft = editor("§6E2E Verkündung§r\nAlle sind willkommen.");
        List<String> predicted = preview();
        if (prepared) { click("ottoextra.letter.send"); written(draft, predicted); }
        int before = commands().size();
        ctx.runOnClient(c -> c.setScreen(new AnnouncementPreflightScreen(null, config, draft)));
        check(ctx.computeOnClient(c -> c.currentScreen.children().stream().filter(e -> e instanceof ButtonWidget)
                .map(e -> (ButtonWidget)e).filter(b -> b.getMessage().getString().equals(Text.translatable("ottoextra.letter.preflight.optimize").getString()))
                .findFirst().orElseThrow().active != prepared), "Prepared optimize state wrong");
        screenshot("courier-announcement-" + prepared);
        click("ottoextra.letter.preflight.send");
        await(() -> commands().stream().anyMatch(s -> s.contains("verkünden")), 30_000, "Announcement command missing");
        pause(2000);
        check(ctx.computeOnClient(c -> c.player.getMainHandStack().isEmpty()), "Announcement rejected by server");
        check(api("GET", "/state", null).get("announcements_count").getAsInt() == 1, "Announcement not persisted");
        JsonObject state = api("GET", "/state", null);
        check(state.get("announcements_persisted_count").getAsInt() == 1, "Announcement YAML not persisted");
        equalPages(predicted, LetterV2Planner.displayPages(strings(state.getAsJsonArray("announcements").get(0)
                .getAsJsonObject().getAsJsonObject("letter").getAsJsonArray("rawPages"))));
        if (prepared) check(commands().size() == before + 1, "Prepared announcement rewrote book");
    }

    private void recoveryGuard(String variant) {
        ctx.runOnClient(c -> { config.letter.pageModeSendDelayMs = 5000; config.letter.letterPageDelayMinMs = config.letter.letterPageDelayMaxMs = 5000; });
        LetterDraft draft = editor(LetterCalibrationCorpus.byId("V207").pages().getFirst());
        click("ottoextra.letter.send");
        await(() -> commands().size() == 1, 10_000, "First command missing"); pause(300);
        ctx.runOnClient(c -> LetterServices.clearSendingState());
        var progress = ctx.computeOnClient(c -> LetterServices.letterStore().load());
        check(progress != null, "No progress fixture");
        check(ctx.computeOnClient(c -> progress.context.matches(c.getCurrentServerEntry().address,
                c.player.getUuidAsString(), c.player.getInventory().getSelectedSlot())
                && WrittenLetterImport.isOwn(c.player.getMainHandStack())
                && progress.context.matchesPages(WrittenLetterImport.toSectionPages(c.player.getMainHandStack().get(DataComponentTypes.WRITTEN_BOOK_CONTENT)))), "Recovery fixture was already invalid");
        int before = commands().size();
        if (variant.equals("server")) progress.context.serverAddress = "wrong.example:25579";
        if (variant.equals("player")) progress.context.playerUuid = UUID.randomUUID().toString();
        if (variant.equals("missing")) progress.context = null;
        if (variant.equals("book")) { api("POST", "/book", Map.of("pages", List.of("Manipuliert"))); pause(300); }
        ctx.runOnClient(c -> c.setScreen(new RecoveryPromptScreen(config, progress, null)));
        String action = variant.equals("restart") || variant.equals("discard") ? variant : "resume";
        click("ottoextra.letter.recovery." + action); pause(300);
        check(commands().size() == before, "Invalid recovery emitted commands");
        if (action.equals("resume")) {
            check(ctx.computeOnClient(c -> c.currentScreen instanceof RecoveryPromptScreen), "Invalid resume closed recovery");
            check(ctx.computeOnClient(c -> LetterServices.letterStore().load() != null), "Invalid resume erased progress");
        } else {
            check(ctx.computeOnClient(c -> LetterServices.letterStore().load() == null), "Recovery action did not clear progress");
            if (action.equals("restart")) check(ctx.computeOnClient(c -> c.currentScreen instanceof LetterEditorScreen), "Restart did not reopen editor");
        }
    }

    private void submitDenied(boolean announcement, boolean money) {
        LetterDraft draft = editor("Ablehnung bewahrt den Brief."); List<String> predicted = preview();
        click("ottoextra.letter.send"); written(draft, predicted);
        List<String> before = rawPages();
        if (money) api("POST", "/economy&balance=0", null);
        else api("POST", "/permissions&" + (announcement ? "announce" : "post") + "=false", null);
        if (announcement) {
            ctx.runOnClient(c -> c.setScreen(new AnnouncementPreflightScreen(null, config, draft)));
            click("ottoextra.letter.preflight.send");
        } else {
            ctx.runOnClient(c -> setField(c.currentScreen, "selected", "CourierAudit"));
            click("ottoextra.letter.recipient.send");
        }
        pause(1000); equalPages(before, rawPages());
        JsonObject state = api("GET", "/state", null);
        check(state.getAsJsonArray("outgoing").isEmpty() && state.get("announcements_count").getAsInt() == 0, "Rejected submission persisted");
    }

    private void editorHistory() {
        LetterDraft draft = editor("Erste Fassung 😀");
        paste(" Zweite Fassung"); key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL);
        check(ctx.computeOnClient(c -> String.join("\n", V2DraftFlow.sourcePages(draft))).equals("Erste Fassung 😀"), "Undo lost original text");
        key(GLFW.GLFW_KEY_Y, GLFW.GLFW_MOD_CONTROL);
        check(ctx.computeOnClient(c -> String.join("\n", V2DraftFlow.sourcePages(draft))).equals("Erste Fassung 😀 Zweite Fassung"), "Redo failed");
        ctx.runOnClient(c -> c.currentScreen.close());
        ctx.runOnClient(c -> c.setScreen(new LetterEditorScreen(null, config)));
        LetterDraft restored = ctx.computeOnClient(c -> field(c.currentScreen, "draft"));
        List<String> predicted = preview(); click("ottoextra.letter.send"); written(restored, predicted);
    }

    private void optimizedAnnouncement() {
        LetterDraft draft = editor("§6Einladung§r\nDies ist eine längere Zeile mit mehreren Wörtern. Herzlich willkommen.");
        ctx.runOnClient(c -> c.setScreen(new AnnouncementPreflightScreen(null, config, draft)));
        click("ottoextra.letter.preflight.optimize");
        LetterDraft optimized = ctx.computeOnClient(c -> field(field(c.currentScreen, "flow"), "draft"));
        List<String> expected = ctx.computeOnClient(c -> LetterServices.buildPreflightAnnouncementPlan(config, optimized).predictedPages());
        current.put("optimizedPages", expected); click("ottoextra.letter.preflight.send");
        await(() -> api("GET", "/state", null).get("announcements_persisted_count").getAsInt() == 1, 30_000, "Optimized announcement missing");
        JsonObject state = api("GET", "/state", null);
        equalPages(expected, LetterV2Planner.displayPages(strings(state.getAsJsonArray("announcements").get(0)
                .getAsJsonObject().getAsJsonObject("letter").getAsJsonArray("rawPages"))));
    }

    private void announcementCooldown() {
        announce(false);
        ctx.runOnClient(c -> { clear(); c.setScreen(null); });
        api("POST", "/supply&slot=0&amount=1", null); pause(300);
        LetterDraft draft = editor("Zweite Verkündung"); List<String> expected = preview();
        click("ottoextra.letter.send"); written(draft, expected);
        ctx.runOnClient(c -> c.setScreen(new AnnouncementPreflightScreen(null, config, draft)));
        click("ottoextra.letter.preflight.send"); pause(1200);
        JsonObject state = api("GET", "/state", null);
        check(state.get("announcements_persisted_count").getAsInt() == 1, "Cooldown accepted another announcement");
        equalPages(expected, LetterV2Planner.displayPages(rawPages()));
        check(state.get("balance").getAsDouble() == 980, "Cooldown charged again");
    }

    private List<String> rawPages() { return strings(api("GET", "/state", null).getAsJsonObject("hand").getAsJsonArray("rawPages")); }
    private List<String> commands() { return strings(api("GET", "/state", null).getAsJsonArray("commands")); }
    private static List<String> strings(JsonArray array) { if (array == null) return List.of(); return java.util.stream.StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsString).toList(); }
    private JsonObject api(String method, String path, Object body) {
        try {
            String endpoint = path.replaceFirst("&", "?");
            endpoint += (endpoint.contains("?") ? "&" : "?") + "player=CourierAudit";
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:25580" + endpoint)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.toJson(body))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200, endpoint + ": " + response.statusCode() + " " + response.body());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private void pause(long millis) { long until = System.nanoTime() + millis * 1_000_000; ctx.waitFor(c -> System.nanoTime() >= until, 1_000_000); }
    private void await(BooleanSupplier condition, long timeout, String message) { long until = System.currentTimeMillis() + timeout;
        while (!condition.getAsBoolean()) { check(System.currentTimeMillis() < until, message); pause(100); } }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void equalPages(List<String> expected, List<String> actual) {
        check(expected != null && expected.size() == actual.size(), "Page count mismatch: " + expected + " != " + actual);
        for (int i = 0; i < expected.size(); i++) check(LetterMarkupSemantics.runs(expected.get(i)).equals(LetterMarkupSemantics.runs(actual.get(i))), "Page " + i + " mismatch: " + expected.get(i) + " != " + actual.get(i));
    }
    @SuppressWarnings("unchecked") private static <T> T field(Object object, String name) { try { var f = object.getClass().getDeclaredField(name); f.setAccessible(true); return (T)f.get(object); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); } }
    private static void setField(Object object, String name, Object value) { try { var f = object.getClass().getDeclaredField(name); f.setAccessible(true); f.set(object, value); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); } }
    private void save() { try { String json = JSON.toJson(results); Files.writeString(reports.resolve("results.json"), json); Files.writeString(reports.resolve("run-" + runId + ".json"), json); } catch (Exception e) { throw new RuntimeException(e); } }
}
