package de.ottoextra.regions;

import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.map.PoliticalOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class RegionsServices {

    private static volatile RegionDataService data;
    private static volatile BannerTextureService banners;

    private RegionsServices() {
    }

    public static synchronized void init(OttoExtraApiClient api,
                                         int activityRefreshSeconds) {
        if (data != null && banners != null) {
            return;
        }
        RegionDataService service = new RegionDataService(api, activityRefreshSeconds);
        BannerTextureService bannerService = new BannerTextureService(api);
        // Runs on a service thread under the data lock. PoliticalOverlay caches are read on the
        // render thread without synchronization, so invalidate them there.
        service.setChangeListener(() -> {
            bannerService.invalidateRefresh();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.execute(PoliticalOverlay::invalidateGroups);
        });
        data = service;
        banners = bannerService;
    }

    public static void onServerJoin() {
        RegionDataService d = data;
        if (d != null) {
            d.onServerJoin();
        }
        BannerTextureService b = banners;
        if (b != null) {
            b.onServerJoin();
        }
    }

    public static void onDisconnect() {
        RegionDataService d = data;
        if (d != null) {
            d.onDisconnect();
        }
    }

    public static synchronized void shutdown() {
        RegionDataService d = data;
        if (d != null) {
            d.shutdown();
        }
        data = null;
        banners = null;
    }

    public static RegionDataService data() {
        return data;
    }

    public static BannerTextureService banners() {
        return banners;
    }

    /** Manual map data reload; reports start and result through {@code out} on the client thread. */
    public static void refreshWithFeedback(Consumer<Text> out) {
        RegionDataService service = data;
        if (service == null) {
            out.accept(Text.translatable("ottoextra.regions.refresh.unavailable"));
            return;
        }
        out.accept(Text.translatable("ottoextra.regions.refresh.start"));
        BannerTextureService bannerService = banners;
        if (bannerService != null) bannerService.invalidateRefresh();
        MinecraftClient client = MinecraftClient.getInstance();
        service.forceRefresh().whenComplete((ignored, error) -> client.execute(() -> {
            if (error == null) {
                out.accept(Text.translatable("ottoextra.regions.refresh.done",
                        service.allFactions().size(), service.allRegions().size()));
            } else {
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                out.accept(Text.translatable("ottoextra.regions.refresh.failed",
                        String.valueOf(cause.getMessage())));
            }
        }));
    }

    /** Same as {@link #refreshWithFeedback(Consumer)}, answering in the player's chat. */
    public static void refreshToChat() {
        MinecraftClient client = MinecraftClient.getInstance();
        refreshWithFeedback(text -> {
            if (client.player != null) client.player.sendMessage(text, false);
        });
    }

    /** Aktuelle Eigennamen aus dem API-Bootstrap fuer Uebersetzungsfilter. */
    public static Set<String> translationProtectedNames() {
        RegionDataService service = data;
        if (service == null) return Set.of();
        Set<String> names = new LinkedHashSet<>();
        service.allFactions().forEach(faction -> {
            addName(names, faction.name());
            addName(names, faction.region_name());
            addName(names, faction.leader_name());
            addName(names, faction.lord_name());
            addName(names, faction.banner_name());
            if (faction.vassal_names() != null) {
                faction.vassal_names().forEach(value -> addName(names, value));
            }
        });
        service.allRegions().forEach(region -> {
            addName(names, region.name());
            addName(names, region.name_override());
            addName(names, region.original_name());
        });
        return Set.copyOf(names);
    }

    private static void addName(Set<String> names, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.length() < 2 || "-".equals(clean)
                || "Unbekannt".equalsIgnoreCase(clean)
                || "Unknown".equalsIgnoreCase(clean)
                || "Ungelandet".equalsIgnoreCase(clean)) return;
        names.add(clean);
    }
}
