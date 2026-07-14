package de.ottoextra.letter.placeholder;

import java.util.Optional;

public interface RpIdentityResolver {

    Optional<String> rpName(String playerName);

    Optional<String> title(String playerName);

    boolean accountKnown(String playerName);
}
