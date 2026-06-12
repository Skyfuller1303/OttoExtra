package de.ottoextra.letter.placeholder;

import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;

import java.util.Optional;

/**
 * Resolver-Kette: OttoExtra-RP-Store (lokal gelernt +
 * API-Import) → Tablist-Accountnamen. Keine harte Dependency auf Fremdmods —
 * der RP-Store enthält bereits die importierten OttoChat-Daten.
 */
public final class OttoExtraRpIdentityResolver implements RpIdentityResolver {

    @Override
    public Optional<String> rpName(String playerName) {
        return profile(playerName).filter(LocalRpProfile::hasRpName).map(p -> p.rpName);
    }

    @Override
    public Optional<String> title(String playerName) {
        return profile(playerName).filter(LocalRpProfile::hasTitle).map(p -> p.title);
    }

    @Override
    public boolean accountKnown(String playerName) {
        return profile(playerName).isPresent();
    }

    private Optional<LocalRpProfile> profile(String playerName) {
        if (RpNamesServices.store() == null || playerName == null || playerName.isBlank()) {
            return Optional.empty();
        }
        return RpNamesServices.store().findByName(playerName.trim());
    }
}
