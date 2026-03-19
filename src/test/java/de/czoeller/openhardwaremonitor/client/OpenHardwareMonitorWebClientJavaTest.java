package de.czoeller.openhardwaremonitor.client;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenHardwareMonitorWebClientJavaTest {
    @Test
    void javaCanUseJvmOverloadsConstructorAndFetchSnapshot() throws IOException {
        FakeHttpClient httpClient = new FakeHttpClient(sampleJson());
        HttpOpenHardwareMonitorClient client = new HttpOpenHardwareMonitorClient("http://localhost:8085", httpClient);

        OpenHardwareMonitorSnapshot snapshot = client.fetchSnapshot();

        assertNotNull(httpClient.lastRequest);
        assertEquals("/data.json", httpClient.lastRequest.uri().getPath());
        assertEquals(59, snapshot.metrics().getCpuTempC());
    }

    @Test
    void javaCanUseJvmStaticDiscoverFactory() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            Thread acceptThread = new Thread(() -> {
                try {
                    serverSocket.accept().close();
                } catch (IOException ignored) {
                }
            });
            acceptThread.start();

            FakeHttpClient httpClient = new FakeHttpClient(sampleJson());
            HttpOpenHardwareMonitorClient client = HttpOpenHardwareMonitorClient.discover(
                List.of("http://127.0.0.1:" + serverSocket.getLocalPort() + "/"),
                httpClient
            );

            OpenHardwareMonitorSnapshot snapshot = client.fetchSnapshot();

            acceptThread.join(1000);
            assertNotNull(httpClient.lastRequest);
            assertEquals("/data.json", httpClient.lastRequest.uri().getPath());
            assertEquals(44, snapshot.metrics().getGpuTempC());
        }
    }

    @Test
    void javaCanCallJvmStaticDiscoveryHelpers() {
        List<String> defaults = OpenHardwareMonitorEndpointDiscovery.defaultCandidates();

        assertFalse(defaults.isEmpty());
        assertTrue(defaults.contains("http://localhost:8085/"));
        assertTrue(defaults.contains("http://127.0.0.1:8085/"));
    }

    private static String sampleJson() throws IOException {
        try (InputStream stream = OpenHardwareMonitorWebClientTest.class.getResourceAsStream("/ohm/sample-data.json")) {
            if (stream == null) {
                throw new IOException("Missing test resource: /ohm/sample-data.json");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class FakeHttpClient extends HttpClient {
        private final String body;
        private final int statusCode;
        private HttpRequest lastRequest;

        private FakeHttpClient(String body) {
            this(body, 200);
        }

        private FakeHttpClient(String body, int statusCode) {
            this.body = body;
            this.statusCode = statusCode;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            lastRequest = request;
            @SuppressWarnings("unchecked")
            T responseBody = (T) body;
            return new FakeHttpResponse<>(responseBody, statusCode);
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<java.net.Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private static final class FakeHttpResponse<T> implements HttpResponse<T> {
        private final T body;
        private final int statusCode;

        private FakeHttpResponse(T body, int statusCode) {
            this.body = body;
            this.statusCode = statusCode;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("http://localhost")).build();
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://localhost");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
