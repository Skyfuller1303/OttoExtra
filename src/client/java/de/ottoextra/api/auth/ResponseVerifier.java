package de.ottoextra.api.auth;

import de.ottoextra.OttoExtra;

import java.net.http.HttpHeaders;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class ResponseVerifier {

    public static final String SIGNATURE_HEADER = "X-OE-Signature";
    public static final String KEY_ID_HEADER = "X-OE-Key-Id";

    private static final Map<String, String> PUBLIC_KEYS = Map.of(
            "k1", "MCowBQYDK2VwAyEAlsJDi7fJIJnSpmp6ztPP86GjAlxe/CcLAPuFqBlYnX0=");

    private final Map<String, String> keysById;
    private final BooleanSupplier requireSignatures;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile boolean warnedNoKeys;

    public ResponseVerifier(BooleanSupplier requireSignatures) {
        this(PUBLIC_KEYS, requireSignatures);
    }

    public ResponseVerifier(Map<String, String> keysById, BooleanSupplier requireSignatures) {
        this.keysById = Map.copyOf(keysById);
        this.requireSignatures = requireSignatures;
    }

    static Map<String, String> productionKeys() {
        return PUBLIC_KEYS;
    }

    public enum Result {

        VALID,

        MISSING,

        INVALID
    }

    public Result check(HttpHeaders headers, byte[] rawBody) {
        String sig = headers.firstValue(SIGNATURE_HEADER).orElse(null);
        String keyId = headers.firstValue(KEY_ID_HEADER).orElse(null);
        if (sig == null || keyId == null) {
            return Result.MISSING;
        }
        String publicKeyB64 = keysById.get(keyId);
        if (publicKeyB64 == null) {
            OttoExtra.LOGGER.warn("[api] unbekannte Signatur-Key-Id '{}' — Mod-Update nötig?", keyId);
            return Result.INVALID;
        }
        try {
            PublicKey key = keyCache.computeIfAbsent(keyId, id -> parseKey(publicKeyB64));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(rawBody == null ? new byte[0] : rawBody);
            return verifier.verify(Base64.getDecoder().decode(sig)) ? Result.VALID : Result.INVALID;
        } catch (Exception e) {
            return Result.INVALID;
        }
    }

    public boolean accept(HttpHeaders headers, byte[] rawBody) {
        Result result = check(headers, rawBody);
        return switch (result) {
            case VALID -> true;
            case INVALID -> false;
            case MISSING -> {
                boolean require = requireSignatures.getAsBoolean();
                if (require && keysById.isEmpty() && !warnedNoKeys) {
                    warnedNoKeys = true;
                    OttoExtra.LOGGER.warn(
                            "[api] requireSignatures aktiv, aber keine Public Keys einkompiliert — alle Antworten werden verworfen");
                }
                yield !require;
            }
        };
    }

    private static PublicKey parseKey(String base64Spki) {
        try {
            KeyFactory factory = KeyFactory.getInstance("Ed25519");
            return factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64Spki)));
        } catch (Exception e) {
            throw new IllegalStateException("Ungültiger eingebetteter Public Key", e);
        }
    }
}
