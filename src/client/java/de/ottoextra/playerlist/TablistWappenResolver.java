package de.ottoextra.playerlist;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionNameKeys;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.object.AtlasTextObjectContents;

import java.util.Locale;

/** Liest das live vom Server gesetzte Wappen aus dem Rich-Text-Tablistennamen. */
final class TablistWappenResolver {
    private static final String WAPPEN_PREFIX = "wappen_";

    private TablistWappenResolver() {
    }

    static FactionRecord resolve(Text tablistName, RegionDataService regions) {
        if (tablistName == null || regions == null) {
            return null;
        }

        FactionRecord own = resolveContent(tablistName, regions);
        if (own != null) {
            return own;
        }
        for (Text sibling : tablistName.getSiblings()) {
            FactionRecord faction = resolve(sibling, regions);
            if (faction != null) {
                return faction;
            }
        }
        return null;
    }

    private static FactionRecord resolveContent(Text text, RegionDataService regions) {
        if (!(text.getContent() instanceof ObjectTextContent object)
                || !(object.contents() instanceof AtlasTextObjectContents atlas)) {
            return null;
        }

        String path = atlas.sprite().getPath();
        String stem = fileStem(path).toLowerCase(Locale.ROOT);
        if (!stem.startsWith(WAPPEN_PREFIX)) {
            return null;
        }
        String wappenKey = stem.substring(WAPPEN_PREFIX.length());

        FactionRecord direct = landed(regions.factionByKey(wappenKey).orElse(null));
        if (direct != null) {
            return direct;
        }
        direct = landed(regions.factionByKey(stem).orElse(null));
        if (direct != null) {
            return direct;
        }

        // Viele Sprite-IDs tragen nach dem Lehensnamen noch Farbangaben,
        // beispielsweise wappen_aehrstein_gruen_golden.
        String normalizedWappen = RegionNameKeys.normalize(wappenKey);
        FactionRecord best = null;
        int bestLength = -1;
        for (FactionRecord faction : regions.allFactions()) {
            if (!faction.isLanded()) {
                continue;
            }
            String factionName = RegionNameKeys.normalize(faction.name());
            if (!factionName.isEmpty() && normalizedWappen.startsWith(factionName)
                    && factionName.length() > bestLength) {
                best = faction;
                bestLength = factionName.length();
            }
        }
        if (best != null) {
            return best;
        }

        // Letzter sicherer Fallback: Das API-Medienbild und das Tab-Wappen
        // haben denselben Dateinamen. Mehrdeutige Standardwappen nicht raten.
        FactionRecord bannerMatch = null;
        for (FactionRecord faction : regions.allFactions()) {
            if (!faction.isLanded() || faction.effectiveBannerPath() == null) {
                continue;
            }
            String banner = RegionNameKeys.normalize(
                    stripWappenPrefix(fileStem(faction.effectiveBannerPath())));
            if (!normalizedWappen.equals(banner)) {
                continue;
            }
            if (bannerMatch != null && !bannerMatch.uuid().equals(faction.uuid())) {
                return null;
            }
            bannerMatch = faction;
        }
        return bannerMatch;
    }

    private static FactionRecord landed(FactionRecord faction) {
        return faction != null && faction.isLanded() ? faction : null;
    }

    private static String fileStem(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String stripWappenPrefix(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith(WAPPEN_PREFIX)
                ? value.substring(WAPPEN_PREFIX.length()) : value;
    }
}
