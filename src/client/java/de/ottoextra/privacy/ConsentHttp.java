package de.ottoextra.privacy;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/** No implicit redirects: a new recipient must be checked before the next request starts. */
public final class ConsentHttp {
    private static final java.util.concurrent.ScheduledThreadPoolExecutor TIMER = timer();
    private static java.util.concurrent.ScheduledThreadPoolExecutor timer() {
        var timer = new java.util.concurrent.ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "ottoextra-http-deadline");
            thread.setDaemon(true);
            return thread;
        });
        timer.setRemoveOnCancelPolicy(true);
        return timer;
    }
    private ConsentHttp() { }

    private static long timeoutMillis(HttpRequest request) {
        return Math.max(1, Math.min(120_000, request.timeout()
                .orElse(java.time.Duration.ofSeconds(30)).toMillis()));
    }

    public static <T> CompletableFuture<HttpResponse<T>> send(HttpClient client, HttpRequest request,
            HttpResponse.BodyHandler<T> handler, ConsentPermit permit) {
        if (!secureEndpoint(request.uri())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("HTTPS endpoint required"));
        }
        if (client.followRedirects() != HttpClient.Redirect.NEVER) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Automatic redirects are disabled"));
        }
        var source = permit.start(() -> client.sendAsync(request, handler));
        CompletableFuture<HttpResponse<T>> bounded = new CompletableFuture<>();
        var deadline = TIMER.schedule(() -> {
            if (bounded.completeExceptionally(new java.net.http.HttpTimeoutException("Response deadline exceeded")))
                source.cancel(true);
        }, timeoutMillis(request), java.util.concurrent.TimeUnit.MILLISECONDS);
        source.whenComplete((response, error) -> {
            if (error != null) bounded.completeExceptionally(error);
            else if (!bounded.complete(response) && response.body() instanceof InputStream body) {
                try { body.close(); } catch (java.io.IOException ignored) { }
            }
        });
        bounded.whenComplete((response, error) -> {
            deadline.cancel(false);
            if (bounded.isCancelled()) source.cancel(true);
        });
        return bounded.thenApply(response -> {
            try {
                permit.require();
                return response;
            } catch (RuntimeException revoked) {
                if (response.body() instanceof InputStream body) {
                    try { body.close(); } catch (java.io.IOException ignored) { }
                }
                throw revoked;
            }
        });
    }

    public static CompletableFuture<HttpResponse<InputStream>> download(HttpClient client,
            HttpRequest request, ConsentPermit permit, Predicate<URI> targets) {
        return download(client, request, permit, targets, 0,
                System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis(request)));
    }

    private static CompletableFuture<HttpResponse<InputStream>> download(HttpClient client,
            HttpRequest request, ConsentPermit permit, Predicate<URI> targets, int redirects, long deadlineNanos) {
        if (!targets.test(request.uri()) || redirects > 5 || System.nanoTime() >= deadlineNanos) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Download target not allowed"));
        }
        return send(client, request, HttpResponse.BodyHandlers.ofInputStream(), permit).thenCompose(response -> {
            int status = response.statusCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                try (InputStream ignored = response.body()) {
                    URI next = request.uri().resolve(response.headers().firstValue("Location").orElseThrow());
                    HttpRequest.Builder builder = HttpRequest.newBuilder(next).GET();
                    builder.timeout(java.time.Duration.ofNanos(Math.max(1, deadlineNanos - System.nanoTime())));
                    builder.header("User-Agent", "OttoExtra-download");
                    return download(client, builder.build(), permit, targets, redirects + 1, deadlineNanos);
                } catch (Exception error) { return CompletableFuture.failedFuture(error); }
            }
            InputStream guarded = deadlineStream(permit.guard(response.body()), deadlineNanos);
            return CompletableFuture.completedFuture(new HttpResponse<InputStream>() {
                public int statusCode() { return response.statusCode(); }
                public HttpRequest request() { return response.request(); }
                public java.util.Optional<HttpResponse<InputStream>> previousResponse() { return java.util.Optional.empty(); }
                public java.net.http.HttpHeaders headers() { return response.headers(); }
                public InputStream body() { return guarded; }
                public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return response.sslSession(); }
                public URI uri() { return response.uri(); }
                public HttpClient.Version version() { return response.version(); }
            });
        });
    }

    private static InputStream deadlineStream(InputStream input, long deadlineNanos) {
        return new java.io.FilterInputStream(input) {
            private volatile boolean expired;
            private final java.util.concurrent.ScheduledFuture<?> timeout = TIMER.schedule(() -> {
                expired = true;
                try { input.close(); } catch (java.io.IOException ignored) { }
            }, Math.max(0, deadlineNanos - System.nanoTime()), java.util.concurrent.TimeUnit.NANOSECONDS);
            private void check() throws java.io.IOException {
                if (expired || System.nanoTime() >= deadlineNanos)
                    throw new java.net.http.HttpTimeoutException("Download body deadline exceeded");
            }
            @Override public int read() throws java.io.IOException {
                check(); int value = in.read(); check(); return value;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws java.io.IOException {
                check(); int count = in.read(bytes, offset, length); check(); return count;
            }
            @Override public void close() throws java.io.IOException {
                timeout.cancel(false); super.close();
            }
        };
    }

    public static boolean sameOrigin(URI target, URI source) {
        return target != null && source != null && target.getHost() != null && source.getHost() != null
                && target.getUserInfo() == null && target.getFragment() == null
                && source.getScheme() != null && source.getScheme().equalsIgnoreCase(target.getScheme())
                && source.getHost().equalsIgnoreCase(target.getHost()) && port(source) == port(target);
    }

    private static int port(URI uri) { return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80; }

    /** Plain HTTP is restricted to explicit loopback development endpoints; never resolve arbitrary hosts. */
    public static boolean secureEndpoint(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) return false;
        String host = uri.getHost();
        boolean loopback = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "[::1]".equals(host) || "::1".equals(host);
        return "https".equalsIgnoreCase(uri.getScheme()) || loopback && "http".equalsIgnoreCase(uri.getScheme());
    }

    public static boolean packTarget(URI target, URI source) {
        if (source == null || target == null || target.getHost() == null || !"https".equalsIgnoreCase(target.getScheme()) || target.getUserInfo() != null
                || target.getFragment() != null || target.getPort() != -1 && target.getPort() != 443) return false;
        if (sameOrigin(target, source)) return true;
        boolean github = "api.github.com".equalsIgnoreCase(source.getHost()) || "github.com".equalsIgnoreCase(source.getHost());
        return github && java.util.Set.of("github.com", "api.github.com", "release-assets.githubusercontent.com",
                "objects.githubusercontent.com", "codeload.github.com").contains(target.getHost().toLowerCase(java.util.Locale.ROOT));
    }

    public static URI skinTarget(String value) {
        URI uri = URI.create(value);
        if (!"textures.minecraft.net".equalsIgnoreCase(uri.getHost()) || uri.getUserInfo() != null
                || uri.getPort() != -1 || uri.getQuery() != null || uri.getFragment() != null
                || !uri.getPath().matches("/texture/[a-zA-Z0-9]+")
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Skin source not allowed");
        }
        return URI.create("https://textures.minecraft.net" + uri.getPath());
    }
}
