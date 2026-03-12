package de.czoeller.openhardwaremonitor.client

import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpResponse
import kotlin.jvm.JvmOverloads
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.serialization.json.Json

open class DefaultOpenHardwareMonitorWebClient @JvmOverloads constructor(
    endpoint: String,
    private val httpClient: HttpClient = defaultHttpClient(),
    private val json: Json = defaultJson()
) : OpenHardwareMonitorWebClient {
    private val endpointUri = OpenHardwareMonitorEndpoint.normalize(endpoint)
    private val requestFactory = OpenHardwareMonitorRequestFactory(DEFAULT_TIMEOUT)
    private val parser = OpenHardwareMonitorJsonParser(json)

    override fun fetchSnapshot(): OpenHardwareMonitorSnapshot {
        val request = requestFactory.createGetRequest(endpointUri)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("Unexpected OpenHardwareMonitor response: HTTP ${response.statusCode()}")
        }
        return OpenHardwareMonitorSnapshot(parser.parseTree(response.body()))
    }

    protected companion object {
        val DEFAULT_TIMEOUT: Duration = 2.seconds

        fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT.toJavaDuration())
            .build()

        fun defaultJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
