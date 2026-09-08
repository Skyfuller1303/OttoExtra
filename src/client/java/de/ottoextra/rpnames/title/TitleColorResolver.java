package de.ottoextra.rpnames.title;

import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.RpNamesServices;

/** Gemeinsame Prioritätslogik für Titelfarben in allen RP-Namensanzeigen. */
public final class TitleColorResolver {

    private static final String DEFAULT_COLOR = "#a17f5f";

    private TitleColorResolver() {
    }

    public static String resolve(LocalRpProfile profile, String personalColor,
                                 String title, TitleCatalogStore catalog,
                                 TitleRegistry titles) {
        var config = RpNamesServices.config();
        return resolve(profile, personalColor, title, catalog, titles,
                config != null && config.nobilityColorForAllTitles);
    }

    static String resolve(LocalRpProfile profile, String personalColor,
                          String title, TitleCatalogStore catalog,
                          TitleRegistry titles, boolean nobilityColorForAllTitles) {
        String state = profile == null ? "" : ApiStateCategory.category(profile.apiState).orElse("");
        boolean noble = "adel".equals(state);
        String stateColor = null;
        if (catalog != null && (!noble || nobilityColorForAllTitles)) {
            stateColor = catalog.categoryColor(state).orElse(null);
        }
        String explicitTitleColor = catalog != null
                ? catalog.find(title).map(e -> e.colorOverride).orElse(null) : null;
        String catalogColor = catalog != null
                ? catalog.titleColor(title).orElse(null) : null;
        String groupColor = titles != null
                ? titles.find(title).map(r -> r.group().titleColor).orElse(null) : null;
        String fallback = catalog != null ? catalog.fallbackTitleColor() : DEFAULT_COLOR;
        if (noble && nobilityColorForAllTitles) {
            // Explicit rank colors and personal edits also win over the optional legacy behavior.
            return firstNonBlank(personalColor, explicitTitleColor, stateColor, catalogColor, groupColor, fallback);
        }
        // Known titles use their own category; an API estate only helps with unknown titles.
        return firstNonBlank(personalColor, catalogColor, groupColor, stateColor, fallback);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return DEFAULT_COLOR;
    }
}
