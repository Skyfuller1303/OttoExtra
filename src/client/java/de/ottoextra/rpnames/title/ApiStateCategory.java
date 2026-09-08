package de.ottoextra.rpnames.title;

import java.util.Optional;

/** Ordnet die von der Spieler-API gelieferten Standvarianten Titelkategorien zu. */
public final class ApiStateCategory {

    private ApiStateCategory() {
    }

    public static Optional<String> category(String apiState) {
        return switch (TitleRegistry.normalize(apiState)) {
            case "teamler", "teamlerin" -> Optional.of("system");
            case "adliger", "adlige" -> Optional.of("adel");
            case "kleriker", "klerikerin" -> Optional.of("klerus");
            case "gemeiner", "gemeine" -> Optional.of("allgemein");
            default -> Optional.empty();
        };
    }

    public static boolean isKnown(String apiState) {
        return category(apiState).isPresent();
    }
}
