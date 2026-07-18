package de.ottoextra.regions;

import de.ottoextra.api.OttoExtraApiClient;

public final class RegionsServices {

    private static volatile RegionDataService data;
    private static volatile BannerTextureService banners;

    private RegionsServices() {
    }

    static void init(OttoExtraApiClient api) {
        data = new RegionDataService(api);
        banners = new BannerTextureService(api);
    }

    static void shutdown() {
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
}
