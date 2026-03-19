package de.czoeller.openhardwaremonitor.client

import java.io.IOException
import java.net.URI
import java.net.ServerSocket
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.util.concurrent.CountDownLatch
import java.util.Optional
import javax.net.ssl.SSLSession
import kotlin.time.Duration.Companion.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class OpenHardwareMonitorWebClientTest {
    @Test
    fun parsesRealSampleAndExtractsMetrics() {
        val json = requireNotNull(
            OpenHardwareMonitorWebClientTest::class.java.getResource("/ohm/sample-data.json")
        ).readText()
        val client = DefaultOpenHardwareMonitorWebClient(
            endpoint = "http://localhost:8085",
            httpClient = FakeHttpClient(json)
        )

        val snapshot = client.fetchSnapshot()
        val metrics = snapshot.metrics()

        assertEquals(2, metrics.cpuLoadPercent)
        assertEquals(50, metrics.memoryLoadPercent)
        assertEquals(59, metrics.cpuTempC)
        assertEquals(1, metrics.gpuLoadPercent)
        assertEquals(44, metrics.gpuTempC)
        assertFalse(snapshot.sensors().any { it.rawValue == "Value" })
    }

    @Test
    fun appliesCustomKotlinTimeoutToRequest() {
        val httpClient = FakeHttpClient("{}")
        val client = DefaultOpenHardwareMonitorWebClient(
            endpoint = "http://localhost:8085",
            timeout = 5.seconds,
            httpClient = httpClient
        )

        client.fetchSnapshot()

        assertEquals(java.time.Duration.ofSeconds(5), httpClient.lastRequest?.timeout()?.orElseThrow())
    }

    @Test
    fun httpClientParsesRealSampleAndNormalizesEndpoint() {
        val json = requireNotNull(
            OpenHardwareMonitorWebClientTest::class.java.getResource("/ohm/sample-data.json")
        ).readText()
        val httpClient = FakeHttpClient(json)
        val client = HttpOpenHardwareMonitorClient(
            endpoint = " http://localhost:8085/ ",
            httpClient = httpClient
        )

        val snapshot = client.fetchSnapshot()

        assertEquals("/data.json", httpClient.lastRequest?.uri()?.path)
        assertEquals(59, snapshot.metrics().cpuTempC)
    }

    @Test
    fun httpClientThrowsOnNonSuccessStatus() {
        val client = HttpOpenHardwareMonitorClient(
            endpoint = "http://localhost:8085",
            httpClient = FakeHttpClient("{}", statusCode = 503)
        )

        val exception = assertFailsWith<IOException> {
            client.fetchSnapshot()
        }

        assertEquals("Unexpected OpenHardwareMonitor response: HTTP 503", exception.message)
    }

    @Test
    fun discoversFirstReachableEndpoint() {
        ServerSocket(0).use { serverSocket ->
            val accepted = CountDownLatch(1)
            val acceptThread = Thread {
                serverSocket.accept().use {
                    accepted.countDown()
                }
            }
            acceptThread.start()

            val endpoint = OpenHardwareMonitorEndpointDiscovery.discover(
                listOf(
                    "   ",
                    "http://127.0.0.1:${serverSocket.localPort}/",
                    "http://127.0.0.1:1/"
                )
            )

            accepted.await()
            acceptThread.join(1_000)
            assertEquals("http://127.0.0.1:${serverSocket.localPort}/", endpoint)
        }
    }

    @Test
    fun discoveryFailureIncludesAttemptedEndpoints() {
        val exception = assertFailsWith<OpenHardwareMonitorEndpointDiscoveryException> {
            OpenHardwareMonitorEndpointDiscovery.discover(
                listOf(
                    "http://127.0.0.1:1/",
                    "http://"
                )
            )
        }

        assertEquals(2, exception.attempts.size)
        assertEquals("http://127.0.0.1:1/", exception.attempts[0].endpoint)
        assertEquals("http://", exception.attempts[1].endpoint)
        assertTrue(exception.message!!.contains("http://127.0.0.1:1/"))
        assertTrue(exception.message!!.contains("http://"))
        assertIs<OpenHardwareMonitorEndpointAttempt>(exception.attempts.first())
    }
}

private class FakeHttpClient(
    private val body: String,
    private val statusCode: Int = 200
) : HttpClient() {
    var lastRequest: HttpRequest? = null

    override fun <T : Any?> send(request: HttpRequest?, responseBodyHandler: BodyHandler<T>?): HttpResponse<T> {
        lastRequest = request
        @Suppress("UNCHECKED_CAST")
        return FakeHttpResponse(body as T, statusCode)
    }

    override fun <T : Any?> sendAsync(
        request: HttpRequest?,
        responseBodyHandler: BodyHandler<T>?
    ) = throw UnsupportedOperationException()

    override fun <T : Any?> sendAsync(
        request: HttpRequest?,
        responseBodyHandler: BodyHandler<T>?,
        pushPromiseHandler: HttpResponse.PushPromiseHandler<T>?
    ) = throw UnsupportedOperationException()

    override fun cookieHandler(): Optional<java.net.CookieHandler> = Optional.empty()
    override fun connectTimeout(): Optional<java.time.Duration> = Optional.empty()
    override fun followRedirects(): HttpClient.Redirect = HttpClient.Redirect.NEVER
    override fun proxy(): Optional<java.net.ProxySelector> = Optional.empty()
    override fun sslContext(): javax.net.ssl.SSLContext = throw UnsupportedOperationException()
    override fun sslParameters(): javax.net.ssl.SSLParameters = javax.net.ssl.SSLParameters()
    override fun authenticator(): Optional<java.net.Authenticator> = Optional.empty()
    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
    override fun executor(): Optional<java.util.concurrent.Executor> = Optional.empty()
}

private class FakeHttpResponse<T>(
    private val bodyValue: T,
    private val responseCode: Int
) : HttpResponse<T> {
    override fun statusCode(): Int = responseCode
    override fun request(): HttpRequest = HttpRequest.newBuilder(URI.create("http://localhost")).build()
    override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()
    override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
    override fun body(): T = bodyValue
    override fun sslSession(): Optional<SSLSession> = Optional.empty()
    override fun uri(): URI = URI.create("http://localhost")
    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
}
