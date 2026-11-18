package de.ottoextra.letter.placeholder;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignatureExpanderTest {

    private static final RpIdentityResolver KNOWN = new RpIdentityResolver() {
        @Override
        public Optional<String> rpName(String playerName) {
            return Optional.of("Ansgar");
        }

        @Override
        public Optional<String> title(String playerName) {
            return Optional.of("Ritter");
        }

        @Override
        public boolean accountKnown(String playerName) {
            return true;
        }
    };

    private static final RpIdentityResolver UNKNOWN = new RpIdentityResolver() {
        @Override
        public Optional<String> rpName(String playerName) {
            return Optional.empty();
        }

        @Override
        public Optional<String> title(String playerName) {
            return Optional.empty();
        }

        @Override
        public boolean accountKnown(String playerName) {
            return false;
        }
    };

    @Test
    void defaultSignatureResolvesOwnFullNameAndResetsFormatting() {
        assertEquals("§oHochachtungsvoll,\n§lRitter Ansgar§r",
                SignatureExpander.expand(SignatureExpander.DEFAULT_SIGNATURE, "Mattis", KNOWN));
    }

    @Test
    void unknownIdentityFallsBackToResolvableTabPlaceholder() {
        assertEquals("§oHochachtungsvoll,\n§l{{full:Mattis}}§r",
                SignatureExpander.expand(SignatureExpander.DEFAULT_SIGNATURE, "Mattis", UNKNOWN));
    }

    @Test
    void plainSignatureWithoutFormattingGetsNoReset() {
        assertEquals("Gruß {{mc}}", SignatureExpander.expand("Gruß {{mc}}", "", KNOWN));
        assertEquals("Gruß Mattis", SignatureExpander.expand("Gruß {{ MC }}", "Mattis", KNOWN));
    }
}
