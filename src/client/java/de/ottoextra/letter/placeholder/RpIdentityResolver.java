package de.ottoextra.letter.placeholder;

import java.util.Optional;

/**
 * Identitätsauflösung für Platzhalter — abstrahiert,
 * damit die Kette (RP-Store → PlayerDirectory → Fallback) testbar bleibt
 * und keine harte Abhängigkeit auf andere Mods entsteht.
 */
public interface RpIdentityResolver {

    Optional<String> rpName(String playerName);

    Optional<String> title(String playerName);

    /** Existiert der Account überhaupt (PlayerDirectory)? */
    boolean accountKnown(String playerName);
}
