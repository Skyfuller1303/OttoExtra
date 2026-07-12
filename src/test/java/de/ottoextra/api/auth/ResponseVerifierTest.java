package de.ottoextra.api.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseVerifierTest {

    private static KeyPair keyPair;
    private static Map<String, String> keys;

    @BeforeAll
    static void generateKeys() throws Exception {
        keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

        keys = Map.of("k1", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }

    private static byte[] sign(byte[] body) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(body);
        return signer.sign();
    }

    private static HttpHeaders headers(String sigB64, String keyId) {
        Map<String, List<String>> map = sigB64 == null
                ? Map.of()
                : Map.of(ResponseVerifier.SIGNATURE_HEADER, List.of(sigB64),
                        ResponseVerifier.KEY_ID_HEADER, List.of(keyId));
        return HttpHeaders.of(map, (a, b) -> true);
    }

    @Test
    void validSignatureAccepted() throws Exception {
        byte[] body = "{\"regions\":[]}".getBytes(StandardCharsets.UTF_8);
        String sig = Base64.getEncoder().encodeToString(sign(body));
        ResponseVerifier verifier = new ResponseVerifier(keys, () -> true);
        assertEquals(ResponseVerifier.Result.VALID, verifier.check(headers(sig, "k1"), body));
        assertTrue(verifier.accept(headers(sig, "k1"), body));
    }

    @Test
    void tamperedBodyRejected() throws Exception {
        byte[] body = "{\"regions\":[]}".getBytes(StandardCharsets.UTF_8);
        String sig = Base64.getEncoder().encodeToString(sign(body));
        byte[] tampered = body.clone();
        tampered[1] ^= 1;
        ResponseVerifier verifier = new ResponseVerifier(keys, () -> false);
        assertEquals(ResponseVerifier.Result.INVALID, verifier.check(headers(sig, "k1"), tampered));
        assertFalse(verifier.accept(headers(sig, "k1"), tampered));
    }

    @Test
    void unknownKeyIdRejected() throws Exception {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        String sig = Base64.getEncoder().encodeToString(sign(body));
        ResponseVerifier verifier = new ResponseVerifier(keys, () -> false);
        assertEquals(ResponseVerifier.Result.INVALID, verifier.check(headers(sig, "k9"), body));
        assertFalse(verifier.accept(headers(sig, "k9"), body));
    }

    @Test
    void missingHeaderFollowsRequireFlag() {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        ResponseVerifier lenient = new ResponseVerifier(keys, () -> false);
        ResponseVerifier strict = new ResponseVerifier(keys, () -> true);
        assertEquals(ResponseVerifier.Result.MISSING, lenient.check(headers(null, null), body));
        assertTrue(lenient.accept(headers(null, null), body));
        assertFalse(strict.accept(headers(null, null), body));
    }

    @Test
    void embeddedProductionKeysAreParseable() {

        ResponseVerifier production = new ResponseVerifier(() -> true);
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        String wrongSig = Base64.getEncoder().encodeToString(new byte[64]);
        assertEquals(ResponseVerifier.Result.INVALID, production.check(headers(wrongSig, "k1"), body));
        assertEquals(ResponseVerifier.Result.INVALID, production.check(headers(wrongSig, "k2"), body));
        assertEquals(ResponseVerifier.Result.INVALID, production.check(headers(wrongSig, "k3"), body));
    }

    @Test
    void garbageSignatureRejected() {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        ResponseVerifier verifier = new ResponseVerifier(keys, () -> false);
        assertEquals(ResponseVerifier.Result.INVALID,
                verifier.check(headers("nicht-base64!!", "k1"), body));
    }
}
