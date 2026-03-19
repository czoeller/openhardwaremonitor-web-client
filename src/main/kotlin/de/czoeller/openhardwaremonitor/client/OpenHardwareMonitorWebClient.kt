package de.czoeller.openhardwaremonitor.client

import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.Socket
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import kotlinx.serialization.json.Json

interface OpenHardwareMonitorWebClient {
    @Throws(IOException::class, InterruptedException::class)
    fun fetchSnapshot(): OpenHardwareMonitorSnapshot
}

class HttpOpenHardwareMonitorClient @JvmOverloads constructor(
    endpoint: String,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build(),
    json: Json = Json { ignoreUnknownKeys = true }
) : OpenHardwareMonitorWebClient {
    companion object {
        private val DEFAULT_DISCOVERY_ENDPOINTS = listOf(
            "http://localhost:8085/",
            "http://127.0.0.1:8085/"
        )

        @JvmStatic
        @JvmOverloads
        fun discover(
            candidates: List<String> = DEFAULT_DISCOVERY_ENDPOINTS,
            httpClient: HttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build(),
            json: Json = Json { ignoreUnknownKeys = true }
        ): HttpOpenHardwareMonitorClient {
            val endpoint = OpenHardwareMonitorEndpointDiscovery.discover(candidates)
            return HttpOpenHardwareMonitorClient(endpoint, httpClient, json)
        }
    }

    private val endpointUri = OpenHardwareMonitorEndpoint.normalize(endpoint)
    private val parser = OpenHardwareMonitorJsonParser(json)

    override fun fetchSnapshot(): OpenHardwareMonitorSnapshot {
        val request = HttpRequest.newBuilder(endpointUri)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("Unexpected OpenHardwareMonitor response: HTTP ${response.statusCode()}")
        }
        return OpenHardwareMonitorSnapshot(parser.parseTree(response.body()))
    }
}

data class OpenHardwareMonitorEndpointAttempt(
    val endpoint: String,
    val error: String
)

class OpenHardwareMonitorEndpointDiscoveryException(
    val attempts: List<OpenHardwareMonitorEndpointAttempt>
) : IOException(
    buildString {
        append("OpenHardwareMonitor discovery failed")
        if (attempts.isNotEmpty()) {
            append(". Tried: ")
            append(
                attempts.joinToString(" | ") { attempt ->
                    "${attempt.endpoint} -> ${attempt.error}"
                }
            )
        }
    }
)

object OpenHardwareMonitorEndpointDiscovery {
    @JvmStatic
    fun defaultCandidates(): List<String> = listOf(
        "http://localhost:8085/",
        "http://127.0.0.1:8085/"
    )

    @JvmStatic
    fun discover(candidates: List<String> = defaultCandidates()): String {
        val attempts = mutableListOf<OpenHardwareMonitorEndpointAttempt>()

        candidates
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { endpoint ->
                val failure = getReachabilityFailure(endpoint) ?: return endpoint
                attempts += OpenHardwareMonitorEndpointAttempt(endpoint, failure)
            }

        throw OpenHardwareMonitorEndpointDiscoveryException(attempts)
    }

    private fun getReachabilityFailure(endpoint: String): String? {
        return try {
            val uri = URI.create(endpoint)
            val host = uri.host ?: return "missing host"
            val port = if (uri.port > 0) uri.port else 80

            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
            }
            null
        } catch (ex: Exception) {
            ex.message ?: ex.javaClass.simpleName
        }
    }
}
