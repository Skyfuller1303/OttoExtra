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

/**
 * Prüft Ed25519-Antwortsignaturen der v2-API.
 *
 * <p>Schützt gegen gefälschte API-Server (DNS-Spoofing, kaputte Proxys):
 * Antworten ohne gültige Signatur werden verworfen, sobald
 * {@code api.requireSignatures} aktiv ist. Eingebettet werden ausschliesslich
 * <b>Public</b> Keys — die dürfen öffentlich sein.</p>
 */
public final class ResponseVerifier {

    public static final String SIGNATURE_HEADER = "X-OE-Signature";
    public static final String KEY_ID_HEADER = "X-OE-Key-Id";

    /**
     * Public Keys des Servers (base64 X.509 SubjectPublicKeyInfo), Key-Id → Key.
     * Nur Public Keys — dürfen öffentlich sein. Immer zwei Keys ausliefern
     * (aktiv + Rotations-Reserve), sonst sperrt eine Key-Rotation alle
     * Clients ohne Mod-Update aus.
     */
    private static final Map<String, String> PUBLIC_KEYS = Map.of(
            "k1", "MCowBQYDK2VwAyEA7+zdLKmnLJSdUCDUlOTHH9YN7ns5z/fKSwpedEqmJ1E=",
            "k2", "MCowBQYDK2VwAyEAGyXZMtbMa108e2CWWOTizr8p4MnWipQBjOlzfWuKr/I=");

    private final Map<String, String> keysById;
    private final BooleanSupplier requireSignatures;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile boolean warnedNoKeys;

    /** Produktiv: einkompilierte Keys, Pflicht-Flag live aus der Config. */
    public ResponseVerifier(BooleanSupplier requireSignatures) {
        this(PUBLIC_KEYS, requireSignatures);
    }

    /** Für Tests: eigene Keys injizierbar. */
    public ResponseVerifier(Map<String, String> keysById, BooleanSupplier requireSignatures) {
        this.keysById = Map.copyOf(keysById);
        this.requireSignatures = requireSignatures;
    }

    public enum Result {
        /** Signatur vorhanden und gültig. */
        VALID,
        /** Kein Signatur-Header (Übergangsphase, Server noch nicht umgestellt). */
        MISSING,
        /** Signatur falsch, Body manipuliert oder Key-Id unbekannt. */
        INVALID
    }

    /** Prüft die Signatur über die exakt empfangenen Body-Bytes — VOR jedem Parsen. */
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

    /** Akzeptanz-Entscheidung inkl. Übergangsregel. */
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
