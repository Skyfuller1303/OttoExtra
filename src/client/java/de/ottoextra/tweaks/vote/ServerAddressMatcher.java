package de.ottoextra.tweaks.vote;

import java.net.IDN;
import java.util.List;
import java.util.Locale;

public final class ServerAddressMatcher {

    private ServerAddressMatcher() {
    }

    public static boolean matches(String address, List<String> configuredAddresses) {
        String normalizedAddress = normalize(address);
        if (normalizedAddress.isEmpty() || configuredAddresses == null) {
            return false;
        }
        return configuredAddresses.stream()
                .map(ServerAddressMatcher::normalize)
                .anyMatch(normalizedAddress::equals);
    }

    public static String normalize(String address) {
        if (address == null) {
            return "";
        }

        String value = address.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.contains("://") || containsInvalidSeparator(value)) {
            return "";
        }

        String host = stripPort(value);
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty()) {
            return "";
        }

        try {
            return IDN.toASCII(host);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static boolean containsInvalidSeparator(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character)
                    || character == '/'
                    || character == '?'
                    || character == '#') {
                return true;
            }
        }
        return false;
    }

    private static String stripPort(String value) {
        if (value.startsWith("[")) {
            int closingBracket = value.indexOf(']');
            if (closingBracket < 0) {
                return "";
            }
            return value.substring(1, closingBracket);
        }

        int colon = value.lastIndexOf(':');
        if (colon <= 0 || value.indexOf(':') != colon) {
            return value;
        }

        String port = value.substring(colon + 1);
        if (port.isEmpty() || !port.chars().allMatch(Character::isDigit)) {
            return value;
        }
        return value.substring(0, colon);
    }
}
