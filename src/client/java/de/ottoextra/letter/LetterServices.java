package de.ottoextra.letter;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterSendProgress;
import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.recovery.SendProgressStore;
import de.ottoextra.letter.send.CommandSendQueue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Zentrale Letter-Dienste: Brief UND Verkündung
 * schreiben über {@code /letter}-Zeilen (gewrappte Editor-Zeilen, Leerzeilen
 * als Figure Space); Abschluss Brief = {@code /post <Empfänger>}, Verkündung =
 * konfigurierbarer Submit-Command (leer = manuell). Versand mit
 * randomisiertem Anti-Spam-Timing + Recovery-Persistenz nach jedem Command.
 */
public final class LetterServices {

    /** Leerzeilen-Platzhalter (Server schluckt echte Leerzeile). */
    public static final String EMPTY_LINE = "\u2007";

    /** Zeilenbreite des Buchs in Pixeln (WYSIWYG-Umbruch). 108 = 18 Ziffern/Zeile,
     *  passend zur Buchansicht (schmaler als Vanilla-114). */
    static final int BOOK_PAGE_WIDTH = 108;

    /** Breitenmesser des Client-Fonts (Pixel-Umbruch); Fallback Zeichenzahl. */
    private static java.util.function.ToIntFunction<String> textWidth() {
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        return tr != null ? tr::getWidth : (s -> s.length() * 6);
    }

    private static SendProgressStore<LetterSendProgress> letterStore;
    private static SendProgressStore<AnnouncementSendProgress> announcementStore;

    /** Läuft gerade ein Versand? (für Actionbar-Status im LetterModule-Tick) */
    private static volatile boolean sending = false;
    private static volatile boolean sendingAnnouncement = false;

    private LetterServices() {
    }

    /** Versand aktiv (Brief oder Verkündung)? */
    public static boolean isSending() {
        return sending;
    }

    /** Ist der laufende Versand eine Verkündung (true) oder ein Brief (false)? */
    public static boolean isSendingAnnouncement() {
        return sendingAnnouncement;
    }

    /** Versand-Status zurücksetzen (Disconnect-Abbruch). */
    public static void clearSendingState() {
        sending = false;
    }

    // ---- Pending-Aktion nach dem Schreiben (Chat-Prompt statt Auto-Dialog) ----

    /** Vorbereiteter, fertig geschriebener Entwurf; wartet auf die
     *  Spieler-Aktion aus dem Chat-Prompt (Verschicken/Verkünden/Schließen). */
    public record PendingLetter(LetterDraft draft, long createdAtMs) {
    }

    /** Gültigkeitsdauer der Pending-Aktion: nach Ablauf ist der alte Chat-Button
     *  wirkungslos (verhindert versehentlichen Spätversand). */
    private static final long PENDING_TTL_MS = 10 * 60 * 1000L;
    private static volatile PendingLetter pending;

    /** Merkt den geschriebenen Entwurf vor; ab jetzt entscheidet der Spieler. */
    public static void setPending(LetterDraft draft) {
        pending = new PendingLetter(draft, System.currentTimeMillis());
    }

    /** Vorbereiteter Brief vorhanden und noch nicht abgelaufen? */
    public static boolean hasPending() {
        PendingLetter p = pending;
        return p != null
                && System.currentTimeMillis() - p.createdAtMs() <= PENDING_TTL_MS;
    }

    /** Entwurf des vorbereiteten Briefs, oder {@code null} (keiner/abgelaufen). */
    public static LetterDraft pendingDraft() {
        return hasPending() ? pending.draft() : null;
    }

    /** Pending-Aktion verbrauchen (nach erfolgreichem Senden/Verkünden). */
    public static void consumePending() {
        pending = null;
    }

    private static Path cacheDir() {
        return OttoExtraPaths.root().resolve(".cache").resolve("letters");
    }

    public static synchronized SendProgressStore<LetterSendProgress> letterStore() {
        if (letterStore == null) {
            letterStore = new SendProgressStore<>(
                    cacheDir().resolve("active-letter-send.json"), LetterSendProgress.class);
        }
        return letterStore;
    }

    public static synchronized SendProgressStore<AnnouncementSendProgress> announcementStore() {
        if (announcementStore == null) {
            announcementStore = new SendProgressStore<>(
                    cacheDir().resolve("active-announcement-letter-send.json"),
                    AnnouncementSendProgress.class);
        }
        return announcementStore;
    }

    // ---- /letter-Zeilenaufbau (Brief + Verkündung gemeinsam) ---------------------

    /**
     * Gewrappte {@code /letter}-Zeilen aller Seiten (Editor-Layout = gesendetes
     * Layout); Leerzeilen bleiben als Figure Space erhalten.
     * Liefert zusätzlich den Seitenindex je Command für das Seitenwechsel-Timing.
     */
    static List<String> buildLetterLines(OttoExtraConfig config, LetterDraft draft,
                                         List<Integer> pageIndexOut) {
        PageSplitter wrapper = new PageSplitter(textWidth(), BOOK_PAGE_WIDTH, Integer.MAX_VALUE);
        List<String> out = new ArrayList<>();
        for (int p = 0; p < draft.pages.size(); p++) {
            for (String line : wrapper.wrapLines(draft.pages.get(p))) {
                out.add(config.letter.letterCommand + " "
                        + (line.isEmpty() ? EMPTY_LINE : normalizeFormattingCodes(line)));
                if (pageIndexOut != null) {
                    pageIndexOut.add(p);
                }
            }
        }
        return out;
    }

    // ---- PAGE-Modus: eine /letter-Nachricht pro Buchseite ----------------------

    /** Minecraft-Buchlimits (Java-Edition). */
    static final int BOOK_MAX_LINES = 14;
    static final int BOOK_MAX_EFFECTIVE_CHARS = 256;
    /** Sicheres Chat-/Command-Limit; Prefix "letter " zählt mit. */
    static final int SAFE_CHAT_MESSAGE_LIMIT = 256;

    /** PAGE-Modus aktiv? (sonst LEGACY zeilenweise). */
    static boolean pageMode(OttoExtraConfig config) {
        return "PAGE".equalsIgnoreCase(config.letter.sendMode);
    }

    /** Effektive Buchzeichen: Zeilenumbruch zählt als 2. */
    static int effectiveBookLength(String page) {
        int count = 0;
        for (int i = 0; i < page.length(); i++) {
            count += page.charAt(i) == '\n' ? 2 : 1;
        }
        return count;
    }

    /** Zeilenanzahl einer Seite (leere Seite = 1). */
    static int lineCount(String page) {
        return page.isEmpty() ? 1 : page.split("\n", -1).length;
    }

    /** Gültige §-Formatcodes -> &-Codes (einmalig, vor der Längenprüfung). */
    static String normalizeFormattingCodes(String raw) {
        return de.ottoextra.letter.format.LetterFormattingCodes.sectionToAmpersand(raw);
    }

    /** Payload-Kodierung: {@code \} -> {@code \\}, Zeilenumbruch -> {@code \n}. */
    static String encodePagePayload(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\n') {
                out.append("\\n");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * PAGE-Modus: pro nicht-leerer Buchseite genau ein {@code /letter}-Command mit
     * der komplett kodierten Seite ({@code \n} zwischen den sichtbaren Zeilen,
     * Leerzeilen bleiben erhalten). Validiert Buch- und Payload-Limits; eine zu
     * lange Seite wird NICHT still gekürzt, sondern mit Warnung übersprungen
     * (Editor-Seiten liegen normal innerhalb der Limits).
     */
    static List<String> buildLetterPages(OttoExtraConfig config, LetterDraft draft,
                                         List<Integer> pageIndexOut) {
        // Jede Editor-Seite gegen Buch-/Payload-Limits re-paginieren (Zeichen-Budget
        // + 14 Zeilen). Überlange Seiten werden auf Folgeseiten aufgeteilt, NICHT
        // gekürzt — greift auch für alte (zu lang gespeicherte) Entwürfe.
        PageSplitter wrapper = new PageSplitter(textWidth(), BOOK_PAGE_WIDTH,
                config.letter.pageModeMaxLinesPerPage, config.letter.pageModeEffectiveCharBudget);
        String prefix = config.letter.letterCommand + " ";
        int payloadLimit = SAFE_CHAT_MESSAGE_LIMIT - prefix.length();
        List<String> out = new ArrayList<>();
        int pageNo = 0;
        for (String draftPage : draft.pages) {
            for (String rawPage : wrapper.split(draftPage)) {
                if (rawPage.isBlank()) {
                    continue; // leere (Teil-)Seite überspringen
                }
                String normalized = normalizeFormattingCodes(rawPage);
                String encoded = encodePagePayload(normalized);
                if (encoded.length() > payloadLimit
                        || effectiveBookLength(normalized) > BOOK_MAX_EFFECTIVE_CHARS
                        || lineCount(normalized) > BOOK_MAX_LINES) {
                    // Sollte durch das Budget nicht passieren — Sicherheitsnetz.
                    OttoExtra.LOGGER.warn("[letter] Teilseite zu lang (Buch={}, Zeilen={}, "
                            + "Payload={}) — uebersprungen.", effectiveBookLength(normalized),
                            lineCount(normalized), encoded.length());
                    hint("ottoextra.letter.page.tooLong");
                    continue;
                }
                out.add(prefix + encoded);
                if (pageIndexOut != null) {
                    pageIndexOut.add(pageNo);
                }
                pageNo++;
            }
        }
        return out;
    }

    /** Versand-Commands je nach Modus (PAGE = seitenweise, LEGACY = zeilenweise). */
    private static List<String> buildSendCommands(OttoExtraConfig config, LetterDraft draft,
                                                  List<Integer> pageIndexOut) {
        return pageMode(config)
                ? buildLetterPages(config, draft, pageIndexOut)
                : buildLetterLines(config, draft, pageIndexOut);
    }

    /** Randomisierte Delays: Zeilen-Jitter, längere Pause bei Seitenwechsel.
     *  Im PAGE-Modus flacher fester Seiten-Delay. */
    private static long[] buildDelays(OttoExtraConfig config, List<Integer> pageIndex,
                                      int totalCommands) {
        if (pageMode(config)) {
            long d = Math.max(200L, config.letter.pageModeSendDelayMs);
            long[] flat = new long[totalCommands];
            for (int i = 1; i < totalCommands; i++) {
                flat[i] = d;
            }
            return flat;
        }
        Random random = new Random();
        long[] delays = new long[totalCommands];
        for (int i = 1; i < totalCommands; i++) {
            boolean pageChange = i < pageIndex.size()
                    && !pageIndex.get(i).equals(pageIndex.get(i - 1));
            int min = pageChange ? config.letter.letterPageDelayMinMs
                    : config.letter.letterSendDelayMinMs;
            int max = pageChange ? config.letter.letterPageDelayMaxMs
                    : config.letter.letterSendDelayMaxMs;
            delays[i] = min + random.nextInt(Math.max(1, max - min));
        }
        return delays;
    }

    // ---- Schreiben (nur /letter, ohne Abschluss) ----------------------------

    /**
     * Schreibt den Brief mit {@code /letter}-Zeilen — erzeugt das beschriebene
     * Buch, OHNE {@code /post} oder Verkündungs-Submit. Der Spieler entscheidet
     * danach im Chat-Prompt, was mit dem geschriebenen Brief geschehen soll
     * ({@link #sendPost} oder {@link #sendAnnounceSubmit}).
     */
    public static void startWrite(OttoExtraConfig config, LetterDraft draft) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildSendCommands(config, draft, pageIndex);
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        letterStore().save(progress);
        // Arbeits-Entwurf leeren (Versand nutzt den eigenen Progress-Store).
        LetterDraftCache.clear();
        long[] delays = buildDelays(config, pageIndex, commands.size());
        sending = true;
        sendingAnnouncement = false;
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            sending = false;
            letterStore().clear();
            OttoExtra.LOGGER.info("[letter] Brief {} geschrieben ({} Befehle).",
                    progress.draftId, progress.pendingCommands.size());
        }).withDelays(delays).start(0);
    }

    /** Brief an Empfänger zustellen — Abschluss nach dem Schreiben ({@code /post}). */
    public static void sendPost(OttoExtraConfig config, String recipient) {
        sendCommand(config.letter.postCommand + " " + recipient);
        OttoExtra.LOGGER.info("[letter] Brief an {} zugestellt.", recipient);
    }

    /**
     * Verkündung auslösen — Abschluss nach dem Schreiben (Submit-Command, z. B.
     * {@code verkünden}). Liefert {@code false}, wenn kein Submit-Command
     * konfiguriert ist (dann manuell auszuführen).
     */
    public static boolean sendAnnounceSubmit(OttoExtraConfig config) {
        if (!hasSubmitCommand(config)) {
            hint("ottoextra.letter.announcement.manualSubmit");
            return false;
        }
        sendCommand(config.letter.announcementSubmitCommand.trim());
        OttoExtra.LOGGER.info("[letter] Verkündung ausgelöst.");
        return true;
    }

    /** Einzelnen Chat-Command senden (Abschluss-Commands, kein Queue-Timing). */
    private static void sendCommand(String command) {
        var nh = net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler();
        if (nh != null) {
            nh.sendChatCommand(command);
        }
    }

    // ---- Brief (gebündelt: schreiben + posten in einem Schwung) --------------
    // Hinweis: Wird im aktuellen Chat-Prompt-Flow nicht mehr genutzt
    // (Schreiben/Abschluss sind getrennt), bleibt für Recovery/Fallback.

    public static void startLetterSend(OttoExtraConfig config, LetterDraft draft,
                                       String recipient) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildSendCommands(config, draft, pageIndex);
        commands.add(config.letter.postCommand + " " + recipient);
        pageIndex.add(-1);
        LetterSendProgress progress = new LetterSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.recipient = recipient;
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        letterStore().save(progress);
        // Arbeits-Entwurf sofort leeren: nächster Editor-Aufruf startet leer.
        // Der laufende Versand nutzt den eigenen Progress-Store, nicht den Cache.
        LetterDraftCache.clear();
        runLetterQueue(config, progress, 0, pageIndex);
    }

    public static void runLetterQueue(OttoExtraConfig config, LetterSendProgress progress,
                                      int startIndex, List<Integer> pageIndex) {
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        sending = true;
        sendingAnnouncement = false;
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            letterStore().save(progress);
        }, () -> {
            sending = false;
            letterStore().clear();
            OttoExtra.LOGGER.info("[letter] Brief an {} vollständig gesendet.",
                    progress.recipient);
        }).withDelays(delays).start(startIndex);
    }

    // ---- Verkündung (über /letter, konfigurierbarer Abschluss) ----------

    /** true, wenn nach den /letter-Zeilen automatisch der Submit-Command folgt. */
    public static boolean hasSubmitCommand(OttoExtraConfig config) {
        return config.letter.announcementSubmitCommand != null
                && !config.letter.announcementSubmitCommand.isBlank();
    }

    public static void startAnnouncementSend(OttoExtraConfig config, LetterDraft draft) {
        List<Integer> pageIndex = new ArrayList<>();
        List<String> commands = buildSendCommands(config, draft, pageIndex);
        if (hasSubmitCommand(config)) {
            commands.add(config.letter.announcementSubmitCommand.trim());
            pageIndex.add(-1);
        }
        AnnouncementSendProgress progress = new AnnouncementSendProgress();
        progress.draftId = draft.meta.draftId;
        progress.pageCount = draft.pages.size();
        progress.pendingCommands = commands;
        progress.startedAtMs = System.currentTimeMillis();
        announcementStore().save(progress);
        LetterDraftCache.clear(); // Arbeits-Entwurf leeren (siehe startLetterSend)
        runAnnouncementQueue(config, progress, 0, pageIndex);
    }

    public static void runAnnouncementQueue(OttoExtraConfig config,
                                            AnnouncementSendProgress progress,
                                            int startIndex, List<Integer> pageIndex) {
        boolean manualSubmit = !hasSubmitCommand(config);
        long[] delays = buildDelays(config, pageIndex != null ? pageIndex : List.of(),
                progress.pendingCommands.size());
        sending = true;
        sendingAnnouncement = true;
        new CommandSendQueue(progress.pendingCommands, sent -> {
            progress.sentCommands = sent;
            progress.updatedAtMs = System.currentTimeMillis();
            announcementStore().save(progress);
        }, () -> {
            sending = false;
            announcementStore().clear();
            // Arbeits-Entwurf wurde bereits beim Start geleert
            if (manualSubmit) {
                hint("ottoextra.letter.announcement.manualSubmit");
            }
            OttoExtra.LOGGER.info("[letter] Verkündung {} geschrieben ({} Seiten).",
                    progress.draftId, progress.pageCount);
        }).withDelays(delays).start(startIndex);
    }

    private static void hint(String key) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.translatable(key), false);
        }
    }
}
