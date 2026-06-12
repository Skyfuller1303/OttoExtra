package de.ottoextra.api.auth;

import de.ottoextra.OttoExtra;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Set;

/**
 * Optionales SPKI-Pinning für die v2-API.
 *
 * <p>Pinnt den SHA-256-Hash des SubjectPublicKeyInfo des Leaf-Zertifikats —
 * nicht das Zertifikat selbst, damit Renewals mit gleichem Key-Pair
 * (z. B. Let's Encrypt) weiterlaufen. Erst Standard-TLS-Validierung, dann
 * Pin-Check. Ohne einkompilierte Pins bleibt Pinning inaktiv und die
 * Ed25519-Antwortsignatur ist die Verteidigungslinie.</p>
 */
public final class SpkiPinning {

    /**
     * SPKI-SHA-256-Pins ("sha256/&lt;base64&gt;").
     * TODO Server-Rollout: IMMER zwei Pins einkompilieren
     * (aktiver Key + offline gelagerter Backup-Key) — sonst sperrt
     * ein Key-Wechsel alle Clients aus. Hinter Cloudflare/Proxy: Pinning
     * weglassen und auf Antwortsignaturen stützen.
     */
    private static final Set<String> SPKI_PINS = Set.of();

    private SpkiPinning() {
    }

    /**
     * SSLContext mit Pin-Prüfung, oder {@code null} wenn Pinning aus ist
     * (Flag aus, keine Pins einkompiliert oder Initialisierung fehlgeschlagen)
     * — dann nutzt der HttpClient den System-Truststore.
     */
    public static SSLContext pinnedContextOrNull(boolean enabled) {
        if (!enabled || SPKI_PINS.isEmpty()) {
            return null;
        }
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
            X509ExtendedTrustManager standard = null;
            for (TrustManager tm : factory.getTrustManagers()) {
                if (tm instanceof X509ExtendedTrustManager extended) {
                    standard = extended;
                    break;
                }
            }
            if (standard == null) {
                OttoExtra.LOGGER.warn("[api] kein System-TrustManager gefunden — Pinning inaktiv");
                return null;
            }
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new PinnedTrustManager(standard, SPKI_PINS)}, null);
            return context;
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[api] SPKI-Pinning konnte nicht initialisiert werden ({}) — Standard-TLS",
                    e.getClass().getSimpleName());
            return null;
        }
    }

    /** Berechnet den Pin eines Zertifikats ("sha256/<base64(SHA-256(SPKI))>"). */
    static String pinOf(X509Certificate certificate) throws CertificateException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getPublicKey().getEncoded());
            return "sha256/" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new CertificateException("SPKI-Hash fehlgeschlagen", e);
        }
    }

    /** Erst Standard-Validierung, dann SPKI-Pin des Leaf gegen die Pin-Liste. */
    static final class PinnedTrustManager extends X509ExtendedTrustManager {

        private final X509ExtendedTrustManager delegate;
        private final Set<String> pins;

        PinnedTrustManager(X509ExtendedTrustManager delegate, Set<String> pins) {
            this.delegate = delegate;
            this.pins = pins;
        }

        private void checkPin(X509Certificate[] chain) throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException("Leere Zertifikatskette");
            }
            String pin = pinOf(chain[0]);
            if (!pins.contains(pin)) {
                // Möglicher MITM — Verbindung abbrechen, Aufrufer geht in den Offline-Modus
                throw new CertificateException("SPKI-Pin stimmt nicht überein");
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            delegate.checkServerTrusted(chain, authType, socket);
            checkPin(chain);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            delegate.checkServerTrusted(chain, authType, engine);
            checkPin(chain);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkServerTrusted(chain, authType);
            checkPin(chain);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            delegate.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            delegate.checkClientTrusted(chain, authType, engine);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }
}
