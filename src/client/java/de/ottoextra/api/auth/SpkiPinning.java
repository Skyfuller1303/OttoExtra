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
public final class SpkiPinning {
    private static final Set<String> SPKI_PINS = Set.of();
    private SpkiPinning() {
    }
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
    static String pinOf(X509Certificate certificate) throws CertificateException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getPublicKey().getEncoded());
            return "sha256/" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new CertificateException("SPKI-Hash fehlgeschlagen", e);
        }
    }
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
