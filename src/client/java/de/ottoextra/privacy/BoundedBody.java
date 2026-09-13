package de.ottoextra.privacy;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Rejects advertised and streamed excess before passing bytes to an accumulating subscriber. */
public final class BoundedBody {
    public static final int JSON_BYTES = 4 * 1024 * 1024;
    // The signed bootstrap includes the complete faction/region snapshot and
    // already exceeds the ordinary per-request JSON limit in production.
    public static final int BOOTSTRAP_JSON_BYTES = 16 * 1024 * 1024;
    public static final int IMAGE_BYTES = 5 * 1024 * 1024;
    private BoundedBody() { }

    public static HttpResponse.BodyHandler<byte[]> bytes(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("Positive body limit required");
        return info -> new LimitedSubscriber(HttpResponse.BodySubscribers.ofByteArray(), limit,
                info.headers().firstValueAsLong("Content-Length").orElse(-1));
    }

    public static HttpResponse.BodyHandler<String> text(int limit) {
        return info -> HttpResponse.BodySubscribers.mapping(bytes(limit).apply(info),
                body -> new String(body, StandardCharsets.UTF_8));
    }

    private static final class LimitedSubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate;
        private final long limit;
        private final boolean advertisedExcess;
        private Flow.Subscription subscription;
        private long received;
        private boolean done;

        LimitedSubscriber(HttpResponse.BodySubscriber<byte[]> delegate, long limit, long length) {
            this.delegate = delegate;
            this.limit = limit;
            this.advertisedExcess = length > limit;
        }

        public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            if (advertisedExcess) {
                subscription.cancel();
                fail();
            } else delegate.onSubscribe(subscription);
        }
        public void onNext(List<ByteBuffer> buffers) {
            if (done) return;
            for (ByteBuffer buffer : buffers) {
                received += buffer.remaining();
                if (received > limit) {
                    subscription.cancel();
                    fail();
                    return;
                }
            }
            delegate.onNext(buffers);
        }
        private void fail() { onError(new IOException("Response exceeds " + limit + " bytes")); }
        public void onError(Throwable error) {
            if (!done) { done = true; delegate.onError(error); }
        }
        public void onComplete() {
            if (!done) { done = true; delegate.onComplete(); }
        }
    }
}
