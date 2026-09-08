package de.ottoextra.verify;

public record AccountLinkCredentials(String endpointUrl, String deviceToken, int requestTimeoutSeconds) {

    public static AccountLinkCredentials load() {
        AccountLinkConfig config = AccountLinkConfig.load();
        return new AccountLinkCredentials(
                config.endpointUrl,
                config.deviceToken == null ? "" : config.deviceToken,
                config.requestTimeoutSeconds);
    }

    public boolean linked() {
        return deviceToken != null && !deviceToken.isBlank();
    }
}
