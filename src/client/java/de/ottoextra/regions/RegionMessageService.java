package de.ottoextra.regions;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt Region-Betreten-Meldungen ("Du betrittst ...") und pflegt den
 * aktuellen Regionskontext. Wird von {@code ClientPlayNetworkHandlerMixin}
 * aufgerufen — die gesamte Logik liegt hier, der Mixin bleibt dünn.
 *
 * <p>Statischer Service, weil der Mixin keinen einfachen Zugriff auf den
 * {@code OttoExtraContext} hat (Muster wie im Legacy-OttoRegions).</p>
 */
public final class RegionMessageService {

    // Identisch zur OttoRegions-Vorlage (CASE_INSENSITIVE | UNICODE_CASE).
    private static final Pattern ENTER_PATTERN =
            Pattern.compile("du\\s+betrittst", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern PARSE_PATTERN =
            Pattern.compile("du\\s+betrittst\\s+(.+?)(?:\\s*\\((.+?)\\))?(?:\\s+in\\s+.+)?\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final long GLOBAL_COOLDOWN_MS = 1_800L;
    private static final long SAME_MESSAGE_COOLDOWN_MS = 12_000L;

    private static volatile OttoExtraConfig config;
    private static volatile RegionState current;

    private static long lastGlobalMs = 0;
    private static long lastSameMs = 0;
    private static String lastRaw = null;

    private RegionMessageService() {
    }

    public static void init(OttoExtraConfig cfg) {
        config = cfg;
    }

    /** Bei Disconnect: flüchtigen Zustand verwerfen. */
    public static void reset() {
        current = null;
        lastGlobalMs = 0;
        lastSameMs = 0;
        lastRaw = null;
    }

    public static RegionState current() {
        return current;
    }

    // ---- Erkennung -------------------------------------------------------

    public static boolean isRegionEnter(Text text) {
        return text != null && isRegionEnter(text.getString());
    }

    public static boolean isRegionEnter(String plain) {
        return plain != null && !plain.isBlank() && ENTER_PATTERN.matcher(plain).find();
    }

    /** Ob die Vanilla-Meldung unterdrückt werden soll (nur bei Config-Wunsch). */
    public static boolean shouldHide(Text text) {
        return config != null && config.regions.hideOriginalActionbar && isRegionEnter(text);
    }

    // ---- Verarbeitung ----------------------------------------------------

    /**
     * Verarbeitet eine eingehende Meldung. Mehrere Pakete (GameMessage/Overlay/
     * Title/Subtitle) feuern fast gleichzeitig für ein Betreten — der globale
     * Cooldown dedupliziert diesen Burst.
     */
    public static void handle(Text content, String sourceTag) {
        if (content == null) {
            return;
        }
        String plain = content.getString();
        if (!isRegionEnter(plain)) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean same = plain.equals(lastRaw);
        if (same && now - lastSameMs < SAME_MESSAGE_COOLDOWN_MS) {
            return;
        }
        if (now - lastGlobalMs < GLOBAL_COOLDOWN_MS) {
            return;
        }

        RegionInfo info = parse(plain);
        if (info == null) {
            return;
        }
        lastGlobalMs = now;
        lastSameMs = now;
        lastRaw = plain;
        current = new RegionState(info.regionName(), info.hierarchyLine(), plain, now);

        OttoExtra.LOGGER.info("[regions] Betreten: {}{} (Quelle: {})",
                info.regionName(),
                info.hierarchyLine().isBlank() ? "" : " (" + info.hierarchyLine() + ")",
                sourceTag);

        RegionNotificationOverlay.show(info.regionName(), info.hierarchyLine());
        prefetch(info.regionName());
        if (config != null && config.regions.playEnterSound) {
            playEnterSound();
        }
    }

    /** Region-Detail + Wappen im Hintergrund vorladen, damit HUD/Menü sofort liefern. */
    private static void prefetch(String name) {
        RegionDataService data = RegionsServices.data();
        BannerTextureService banners = RegionsServices.banners();
        if (data == null) {
            return;
        }
        data.requestRegionDetail(name);
        if (banners != null) {
            data.factionForRegion(name).ifPresent(banners::bannerFor);
        }
    }

    private static RegionInfo parse(String plain) {
        Matcher m = PARSE_PATTERN.matcher(plain);
        if (!m.find()) {
            return null;
        }
        String region = m.group(1) == null ? "" : m.group(1).trim();
        String hierarchy = m.group(2) == null ? "" : m.group(2).trim();
        return new RegionInfo(region, hierarchy);
    }

    /**
     * Region-Betreten-Klang (OttoRegions-Original): Noteblock-Hat (Pitch 0.55,
     * Vol 1.1) + Amethyst-Chime (Pitch 0.35, Vol 1.0) gleichzeitig.
     */
    public static void playEnterSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            try {
                client.getSoundManager().play(PositionedSoundInstance.ui(
                        SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.55f, 1.1f), 0);
                client.getSoundManager().play(PositionedSoundInstance.ui(
                        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 1.0f), 0);
            } catch (Throwable ignored) {
                // Sound ist optional; niemals den Client-Thread gefährden.
            }
        });
    }
}
