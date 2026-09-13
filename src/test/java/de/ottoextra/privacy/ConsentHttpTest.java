package de.ottoextra.privacy;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConsentHttpTest {
    private static ConsentPermit permit() { return new ConsentPermit.Gate().capture(() -> true); }

    @Test void rejectedRedirectDoesNotContactTarget() throws Exception {
        HttpServer destination = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpServer source = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger contacts = new AtomicInteger();
        destination.createContext("/", exchange -> {
            contacts.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        destination.start();
        source.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + destination.getAddress().getPort() + "/secret");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        source.start();
        try (HttpClient http = HttpClient.newHttpClient()) {
            URI uri = URI.create("http://127.0.0.1:" + source.getAddress().getPort() + "/");
            HttpRequest request = HttpRequest.newBuilder(uri).header("Authorization", "Bearer test-only").build();
            assertThrows(CompletionException.class, () -> ConsentHttp.download(http, request, permit(),
                    target -> ConsentHttp.sameOrigin(target, uri)).join());
            assertEquals(0, contacts.get());
            // Auth/JSON requests do not follow redirects at all.
            assertEquals(302, ConsentHttp.send(http, request, HttpResponse.BodyHandlers.ofString(), permit()).join().statusCode());
            assertEquals(0, contacts.get());
        } finally { source.stop(0); destination.stop(0); }
    }

    @Test void deniedPermissionAndAutomaticRedirectClientMakeNoContact() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger contacts = new AtomicInteger();
        server.createContext("/", exchange -> { contacts.incrementAndGet(); exchange.close(); });
        server.start();
        try (HttpClient http = HttpClient.newHttpClient();
             HttpClient redirecting = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort())).build();
            assertThrows(RuntimeException.class, () -> ConsentHttp.send(http, request, HttpResponse.BodyHandlers.ofString(),
                    new ConsentPermit.Gate().capture(() -> false)).join());
            assertThrows(RuntimeException.class, () -> ConsentHttp.send(redirecting, request,
                    HttpResponse.BodyHandlers.ofString(), permit()).join());
            assertEquals(0, contacts.get());
        } finally { server.stop(0); }
    }

    @Test void skinUrlsCannotChooseArbitraryHostsOrCredentials() {
        assertEquals(URI.create("https://textures.minecraft.net/texture/abc123"),
                ConsentHttp.skinTarget("http://textures.minecraft.net/texture/abc123"));
        for (String value : new String[]{"https://evil.invalid/texture/a", "https://textures.minecraft.net.evil.invalid/texture/a",
                "https://user@textures.minecraft.net/texture/a", "https://textures.minecraft.net:444/texture/a",
                "https://textures.minecraft.net/texture/a?secret=1", "https://textures.minecraft.net/texture/../private"}) {
            assertThrows(IllegalArgumentException.class, () -> ConsentHttp.skinTarget(value));
        }
    }

    @Test void packDestinationsAreExactOriginsOrDisclosedGithubCdn() {
        URI github = URI.create("https://api.github.com/repos/owner/repo/releases/latest");
        assertTrue(ConsentHttp.packTarget(URI.create("https://release-assets.githubusercontent.com/asset"), github));
        for (String value : new String[]{"http://github.com/asset", "https://github.com.evil.invalid/asset",
                "https://other.invalid/pack.zip", "https://user@github.com/asset", "https://github.com:444/asset"}) {
            assertFalse(ConsentHttp.packTarget(URI.create(value), github));
        }
        URI custom = URI.create("https://pack.example.invalid/manifest.json");
        assertTrue(ConsentHttp.packTarget(URI.create("https://pack.example.invalid/pack.zip"), custom));
        assertFalse(ConsentHttp.packTarget(URI.create("https://github.com/asset"), custom));
        assertFalse(ConsentHttp.secureEndpoint(URI.create("http://public.example.invalid/")));
    }
}
